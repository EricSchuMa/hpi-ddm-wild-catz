package de.hpi.ddm.actors;

import java.io.*;
import java.util.Arrays;
import java.util.List;

import akka.NotUsed;
import akka.actor.*;
import akka.serialization.*;
import akka.stream.*;
import akka.stream.javadsl.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.ArrayUtils;

public class LargeMessageProxy extends AbstractLoggingActor {

	////////////////////////
	// Actor Construction //
	////////////////////////

	public static final String DEFAULT_NAME = "largeMessageProxy";

	public static Props props() {
		return Props.create(LargeMessageProxy.class);
	}

	////////////////////
	// Actor Messages //
	////////////////////

	@Data @NoArgsConstructor @AllArgsConstructor
	public static class LargeMessage<T> implements Serializable {
		private static final long serialVersionUID = 2940665245810221108L;
		private T message;
		private ActorRef receiver;
	}

	@Data @NoArgsConstructor @AllArgsConstructor
	public static class StreamMessage implements Serializable {
		private static final long serialVersionUID = 4057807743872319842L;
		private SourceRef<List<Byte>> futureStreamRef;
		private ActorRef receiver;
		private int length;
	}

	@Data @NoArgsConstructor @AllArgsConstructor
	public static class serializedByteMessage implements Serializable {
		private static final long serialVersionUID = 2237807743872319842L;
		private byte[] bytes;
		private int serializerID;
		private String manifest;
	}
	/////////////////
	// Actor State //
	/////////////////

	/////////////////////
	// Actor Lifecycle //
	/////////////////////

	////////////////////
	// Actor Behavior //
	////////////////////

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(LargeMessage.class, this::handle)
				.match(StreamMessage.class, this::handle)
				.matchAny(object -> this.log().info("Received unknown message: \"{}\"", object.toString()))
				.build();
	}

	private void handle(LargeMessage<?> message) throws Exception {
		ActorRef receiver = message.getReceiver();
		ActorSelection receiverProxy = this.context().actorSelection(receiver.path().child(DEFAULT_NAME));

		// Serialize the object and save it as a serializedByteMessage
		akka.actor.ActorSystem system = this.context().system(); // the belonging actor system
		Serialization serialization = SerializationExtension.get(system);

		// serialize the actual message with it's belonging serializer
		byte[] serializedByteArray = serialization.serialize(message.getMessage()).get();
		int serializerId = serialization.findSerializerFor(message.getMessage()).identifier();
		String manifest = Serializers.manifestFor(serialization.findSerializerFor(message.getMessage()),
				message.getMessage());

		// write serialized message as an object for easy reconstruction
		serializedByteMessage my_message = new serializedByteMessage(serializedByteArray, serializerId, manifest);

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream oos;
		try {
			oos = new ObjectOutputStream(bos);
			oos.writeObject(my_message);
			oos.flush();
			oos.close();
			bos.close();

		} catch (IOException e) {
			log().error("Error while serializing data");
		}

		byte [] rawByteArray = bos.toByteArray();

		// Implement a Source and materialize it into a SourceRef
		final Materializer materializer = ActorMaterializer.create(system);
		final Source<List<Byte>, NotUsed> source = Source.from(Arrays.asList(ArrayUtils.toObject(rawByteArray))).grouped(10000);  // Create source from iterable
		SourceRef<List<Byte>> stream_ref = source.runWith(StreamRefs.sourceRef(), materializer);

		StreamMessage newStreamMessage = new StreamMessage(stream_ref, message.getReceiver(), rawByteArray.length);

		// Send the SourceRef over Artery's remoting
		receiverProxy.tell(newStreamMessage, this.sender());
	}

	private void handle(StreamMessage streamMessage) throws IOException, ClassNotFoundException, Exception {
		akka.actor.ActorSystem system = this.context().system(); // the belonging actor system
		final Materializer materializer = ActorMaterializer.create(system);

		SourceRef<List<Byte>> streamref = streamMessage.getFutureStreamRef();

		// make sure the arrayList is empty
		ActorRef thisActor = this.getSelf();
		streamref.getSource().runWith(Sink.seq(), materializer).whenComplete(
				(byteList, exception) -> {
						byte[] serializedByteArray = new byte[streamMessage.getLength()];
						int index = 0;
						for (List<Byte> bytes : byteList) {
							for (Byte aByte : bytes) {
								serializedByteArray[index] = aByte;
								index++;
							}
						}

						// deserialization and sending
						try {
							ByteArrayInputStream bis = new ByteArrayInputStream(serializedByteArray);
							ObjectInputStream ois = new ObjectInputStream(bis);

							serializedByteMessage my_message = (serializedByteMessage) ois.readObject();

							Serialization serialization = SerializationExtension.get(system);

							streamMessage.getReceiver().tell(serialization.deserialize(my_message.getBytes(),
									my_message.getSerializerID(), my_message.getManifest()).get(), this.sender());
						} catch (IOException | ClassNotFoundException e) {
							e.printStackTrace();
						}
					});
	}
}
