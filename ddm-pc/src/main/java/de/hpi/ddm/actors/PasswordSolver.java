package de.hpi.ddm.actors;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;

import de.hpi.ddm.structures.HintsMessage;
import de.hpi.ddm.structures.PasswordMessage;

public class PasswordSolver extends AbstractLoggingActor {

    ////////////////////////
    // Actor Construction //
    ////////////////////////

    public static final String  DEFAULT_NAME = "passwordSolver";

    public static Props props(final ActorRef hintSolver){
        return Props.create(PasswordSolver.class, () -> new PasswordSolver(hintSolver));
    }

    public PasswordSolver(final ActorRef hintSolver){
        this.hintSolver = hintSolver;
    }

    ////////////////////
    // Actor Messages //
    ////////////////////


    public Receive createReceive(){
        return receiveBuilder()
                .match(PasswordMessage.class, this::handle)
                .matchAny(object -> this.log().info("Received unknown message: \"{}\"", object.toString()))
                .build();
    }

    protected void handle(PasswordMessage message) {
        this.password = message;
        log().info("Received Password and trying to solve password ...");
        HintsMessage hints = new HintsMessage(message.getHints());
        this.hintSolver.tell(hints, this.sender());

    }

    /////////////////
    // Actor State //
    /////////////////

    private final ActorRef hintSolver;
    private PasswordMessage password;

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