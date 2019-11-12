package de.hpi.ddm.actors;

import java.io.Serializable;

import akka.serialization.*;
import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
	public static class BytesMessage<T> implements Serializable {
		private static final long serialVersionUID = 4057807743872319842L;
		private T bytes;
		private ActorRef sender;
		private ActorRef receiver;
	}

	@Data @NoArgsConstructor @AllArgsConstructor
	public static class serializedByteMessage implements Serializable {
		private static final long serialVersionUID = 2237807743872319842L;
		private byte[] bytes;
		private ActorRef sender;
		private ActorRef receiver;
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
				.match(BytesMessage.class, this::handle)
				.match(serializedByteMessage.class, this::handle)
				.matchAny(object -> this.log().info("Received unknown message: \"{}\"", object.toString()))
				.build();
	}

	private void handle(LargeMessage<?> message) {
		ActorRef receiver = message.getReceiver();
		ActorSelection receiverProxy = this.context().actorSelection(receiver.path().child(DEFAULT_NAME));
		// Serialize the object and send its bytes batch-wise (make sure to use artery's side channel then).
		akka.actor.ActorSystem system = this.context().system();
		Serialization serialization = SerializationExtension.get(system);
		byte[] serializedByteArray = serialization.serialize(message.getMessage()).get();
		int serializerId = serialization.findSerializerFor(message.getMessage()).identifier();
		String manifest = Serializers.manifestFor(serialization.findSerializerFor(message.getMessage()), message.getMessage());

		serializedByteMessage myserializedMessage = new serializedByteMessage(serializedByteArray, this.sender(), message.getReceiver(),
				serializerId,
				manifest);

		// send the serialized byte Message via Artery's side chanel
		receiverProxy.tell(myserializedMessage, this.sender());
	}

	private void handle(serializedByteMessage message) {
		// Setup for deserialization
		akka.actor.ActorSystem system = this.context().system();
		Serialization serialization = SerializationExtension.get(system);

		// Forward the deserialized Message
		message.getReceiver().tell(serialization.deserialize(message.getBytes(), message.getSerializerID(),
				message.getManifest()).get(), message.getSender());
	}

	private void handle(BytesMessage<?> message) {
		// Reassemble the message content, deserialize it and/or load the content from some local location before forwarding its content.
		message.getReceiver().tell(message.getBytes(), message.getSender());
	}
}
