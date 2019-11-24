package de.hpi.ddm.actors;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.Terminated;
import de.hpi.ddm.structures.FinalResult;
import de.hpi.ddm.structures.PasswordMessage;
import de.hpi.ddm.structures.WorkAvailabilityMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class Master extends AbstractLoggingActor {

	////////////////////////
	// Actor Construction //
	////////////////////////
	
	public static final String DEFAULT_NAME = "master";

	public static Props props(final ActorRef reader, final ActorRef collector, final ActorRef passwordSolver) {
		return Props.create(Master.class, () -> new Master(reader, collector, passwordSolver));
	}

	public Master(final ActorRef reader, final ActorRef collector, final ActorRef passwordSolver) {
		this.reader = reader;
		this.collector = collector;
		this.passwordSolver = passwordSolver;
		this.workers = new ArrayList<>();
		this.hintSequences = new ArrayList<>();
	}

	////////////////////
	// Actor Messages //
	////////////////////

	@Data
	public static class StartMessage implements Serializable {
		private static final long serialVersionUID = -50374816448627600L;
	}
	
	@Data @NoArgsConstructor @AllArgsConstructor
	public static class BatchMessage implements Serializable {
		private static final long serialVersionUID = 8343040942748609598L;
		private List<String[]> lines;
	}

	@Data
	public static class RegistrationMessage implements Serializable {
		private static final long serialVersionUID = 3303081601659723997L;
	}
	
	/////////////////
	// Actor State //
	/////////////////

	private final ActorRef reader;
	private final ActorRef collector;
	private final ActorRef passwordSolver;
	private final List<ActorRef> workers;
	private ArrayList<List<String>> hintSequences;

	private long startTime;
	
	/////////////////////
	// Actor Lifecycle //
	/////////////////////

	@Override
	public void preStart() {
		Reaper.watchWithDefaultReaper(this);
	}

	////////////////////
	// Actor Behavior //
	////////////////////

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(StartMessage.class, this::handle)
				.match(BatchMessage.class, this::handle)
				.match(Terminated.class, this::handle)
				.match(RegistrationMessage.class, this::handle)
				.match(WorkAvailabilityMessage.class, this::handle)
				.match(FinalResult.class, this::handle)
				.matchAny(object -> this.log().info("Received unknown message: \"{}\"", object.toString()))
				.build();
	}

	protected void handle(StartMessage message) {
		this.startTime = System.currentTimeMillis();
		
		this.reader.tell(new Reader.ReadMessage(), this.self());
	}
	
	protected void handle(BatchMessage message) {
		
		///////////////////////////////////////////////////////////////////////////////////////////////////////
		// The input file is read in batches for two reasons: /////////////////////////////////////////////////
		// 1. If we distribute the batches early, we might not need to hold the entire input data in memory. //
		// 2. If we process the batches early, we can achieve latency hiding. /////////////////////////////////
		///////////////////////////////////////////////////////////////////////////////////////////////////////
		///////////////////////////////////////////////////////////////////////////////////////////////////////

		if (message.getLines().isEmpty()) {
			this.collector.tell(new Collector.PrintMessage(), this.self());
			//this.terminate();
			return;
		}

		String[] first_line = message.lines.get(0);
		int id = Integer.parseInt(first_line[0]);
		String name = first_line[1];
		String pchars = first_line[2];
		int plen = Integer.parseInt(first_line[3]);
		String password = first_line[4];
		String[] hints = Arrays.copyOfRange(first_line, 5, first_line.length);

		// Calculate the permutation list
		if (this.hintSequences.isEmpty()) {
			for (int i = 0; i < pchars.length(); i++) {
				String letters_temp = pchars;
				//Generate string without one character
				String temp_string = letters_temp.replace(Character.toString(pchars.charAt(i)), "");
				//Give the string to permutation function which find all the given string permutations, hashes and compares it
				List<String> list = new ArrayList<>();
				heapPermutation(temp_string.toCharArray(), pchars.length()-1, 0, list);
				this.hintSequences.add(list);
			}
		}

		PasswordMessage password1 = new PasswordMessage(id,
				name,
				pchars,
				plen,
				password,
				hints,
				this.hintSequences);

		this.passwordSolver.tell(password1, this.self());

		for (String[] line : message.getLines())
			System.out.println(Arrays.toString(line));
		
		this.collector.tell(new Collector.CollectMessage("Processed batch of size " + message.getLines().size()), this.self());
		this.reader.tell(new Reader.ReadMessage(), this.self());
	}
	
	protected void terminate() {
		this.reader.tell(PoisonPill.getInstance(), ActorRef.noSender());
		this.collector.tell(PoisonPill.getInstance(), ActorRef.noSender());
		
		for (ActorRef worker : this.workers) {
			this.context().unwatch(worker);
			worker.tell(PoisonPill.getInstance(), ActorRef.noSender());
		}
		
		this.self().tell(PoisonPill.getInstance(), ActorRef.noSender());
		
		long executionTime = System.currentTimeMillis() - this.startTime;
		this.log().info("Algorithm finished in {} ms", executionTime);
	}

	protected void handle(RegistrationMessage message) {
		this.context().watch(this.sender());
		this.workers.add(this.sender());
//		this.log().info("Registered {}", this.sender());
	}
	
	protected void handle(Terminated message) {
		this.context().unwatch(message.getActor());
		this.workers.remove(message.getActor());
//		this.log().info("Unregistered {}", message.getActor());
	}

	protected void handle(WorkAvailabilityMessage message) {
		this.passwordSolver.tell(message, this.self());
	}

	protected void handle(FinalResult message) {
		System.out.println(message.getPassword());
	}

	private void heapPermutation(char[] a, int size, int n, List<String> l) {
		// If size is 1, store the obtained permutation
		if (size == 1)
			l.add(new String(a));

		for (int i = 0; i < size; i++) {
			heapPermutation(a, size - 1, n, l);

			// If size is odd, swap first and last element
			if (size % 2 == 1) {
				char temp = a[0];
				a[0] = a[size - 1];
				a[size - 1] = temp;
			}

			// If size is even, swap i-th and last element
			else {
				char temp = a[i];
				a[i] = a[size - 1];
				a[size - 1] = temp;
			}
		}
	}
}
