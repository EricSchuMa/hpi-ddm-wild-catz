package de.hpi.ddm.actors;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import com.sun.corba.se.spi.orbutil.threadpool.Work;
import de.hpi.ddm.structures.HintsMessage;
import de.hpi.ddm.structures.SolvedHint;
import de.hpi.ddm.structures.WorkAvailabilityMessage;
import de.hpi.ddm.structures.WorkPackage;

import java.util.*;

public class HintSolver extends AbstractLoggingActor {

    ////////////////////////
    // Actor Construction //
    ////////////////////////

    public static final String DEFAULT_NAME = "hintSolver";

    public static Props props(final ActorRef advancedPasswordSolver) {
        return Props.create(HintSolver.class, () -> new HintSolver(advancedPasswordSolver));
    }

    public HintSolver(final ActorRef advancedPasswordSolver) {
        this.advancedPasswordSolver = advancedPasswordSolver;
    }

    ////////////////////
    // Actor Messages //
    ////////////////////


    public Receive createReceive() {
        return receiveBuilder()
                .match(HintsMessage.class, this::handle)
                .match(WorkAvailabilityMessage.class, this::handle)
                .match(SolvedHint.class, this::handle)
                .matchAny(object -> this.log().info("Received unknown message: \"{}\"", object.toString()))
                .build();
    }

    protected void handle(HintsMessage message) {
        this.hints = message.getHints();
        this.pchars = message.getPchars();
        this.passwordSolver = message.getOwner();
        // log().info("Received Hints and trying to solve them ...");

        for (List<String> oneLetterList : message.getPermutationList()) {
            WorkPackage workPackage = new WorkPackage(this.hints, oneLetterList, this.self());
            this.workPackages.push(workPackage);
        }
    }

    protected void handle(WorkAvailabilityMessage message) {
        // send the Work Package to some worker
        if (!this.workPackages.empty()) {
            message.getWorkerReference().tell(this.workPackages.pop(), this.sender());
        }
        else {
            this.advancedPasswordSolver.tell(message, this.sender());
        }
    }

    protected void handle(SolvedHint message) {
        for (Character possibleCharacter: pchars) {
            if (!message.getPermutation().contains(possibleCharacter)) {
                solvedHints.add(possibleCharacter);
                // this.log().info("Hint is {}", possibleCharacter);
                if (solvedHints.size() == hints.length) {
                    List<Character> listOfAllHints = new ArrayList<Character>(solvedHints);
                    this.passwordSolver.tell(new SolvedHint(listOfAllHints), this.self());
                }
            }
        }
    }


    /////////////////
    // Actor State //
    /////////////////

    private String[] hints;
    private List<Character> pchars;
    private Set<Character> solvedHints = new HashSet<Character>();
    private Stack<WorkPackage> workPackages = new Stack<>();
    private ActorRef passwordSolver;
    private ActorRef advancedPasswordSolver;

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



}
