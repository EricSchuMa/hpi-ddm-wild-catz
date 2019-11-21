package de.hpi.ddm.actors;

import java.io.*;
import java.util.ArrayList;

import akka.util.Timeout;
import com.esotericsoftware.kryo.Kryo;
import scala.concurrent.Await;
import scala.concurrent.Future;

import akka.NotUsed;
import akka.actor.*;
import akka.serialization.*;
import akka.protobuf.*;
import akka.stream.*;
import akka.stream.scaladsl.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import scala.concurrent.duration.Duration;

public class LargeMessageProxy extends AbstractLoggingActor {

	////////////////////////
	// Actor Construction //
	////////////////////////

	public static final String DEFAULT_NAME = "largeMessageProxy";
	private final ArrayList alist = new ArrayList();
	
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
	public static class StreamMessage<T> implements Serializable {
		private static final long serialVersionUID = 4057807743872319842L;
		private Future<SourceRef<Byte>> futureStreamRef;
		private ActorRef receiver;
	}

	@Data @NoArgsConstructor @AllArgsConstructor
	public static class serializedByteMessage implements Serializable {
		private static final long serialVersionUID = 2237807743872319842L;
		private byte[] bytes;
		private int serializerID;
		private String manifest;
	}

	@Data @NoArgsConstructor @AllArgsConstructor
	public static class StreamReceived implements Serializable {
		private static final long serialVersionUID =  2237802243872319232L;
		private ActorRef receiver;
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
				.match(StreamReceived.class, this::handle)
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

		 ByteString fromArray = ByteString.copyFrom(bos.toByteArray());

		// Implement a Source and materialize it into a SourceRef
		final Materializer materializer = ActorMaterializer.create(system);
		final Source<Byte, NotUsed> source = akka.stream.javadsl.Source.from(fromArray).asScala();  // Create source from iterable
		Future<SourceRef<Byte>> stream_ref = source.runWith(StreamRefs.sourceRef(), materializer);

		StreamMessage newStreamMessage = new StreamMessage(stream_ref, message.getReceiver());

		// Send the SourceRef over Artery's remoting
		receiverProxy.tell(newStreamMessage, this.sender());
	}

	private void handle(StreamMessage streamMessage) throws Exception {
		akka.actor.ActorSystem system = this.context().system(); // the belonging actor system
		final Materializer materializer = ActorMaterializer.create(system);

		// Wait to receive the source reference from Future
		Timeout timeout = new Timeout(Duration.create(1000, "seconds"));

		Future<SourceRef<Byte>> streamref = streamMessage.getFutureStreamRef();
		SourceRef<Byte> result = Await.result(streamref, timeout.duration());

		// make sure the arrayList is empty
		this.alist.clear();
		ActorRef thisActor = this.getSelf();
		result.getSource().runForeach(alist::add, materializer).thenRun(new Runnable() {
			@Override
			public void run() {
				StreamReceived message = new StreamReceived(streamMessage.getReceiver());
				thisActor.tell(message, thisActor);
			}
		});
	}

	private void handle(StreamReceived message) throws IOException, ClassNotFoundException {
		byte[] myArray = new byte[alist.size()];
		for (int i = 0; i < alist.size(); i++) {
			myArray[i] = (byte) alist.get(i);
		}

		ByteArrayInputStream bis = new ByteArrayInputStream(myArray);
		ObjectInputStream ois = new ObjectInputStream(bis);

		serializedByteMessage my_message = (serializedByteMessage) ois.readObject();

		akka.actor.ActorSystem system = this.context().system(); // the belonging actor system
		Serialization serialization = SerializationExtension.get(system);

		message.getReceiver().tell(serialization.deserialize(my_message.getBytes(), my_message.getSerializerID(), my_message.getManifest()).get(), this.sender());
	}
}
