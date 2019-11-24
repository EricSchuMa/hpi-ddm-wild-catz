package de.hpi.ddm.actors;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import de.hpi.ddm.structures.HintsMessage;

import java.util.ArrayList;
import java.util.List;

public class HintSolver extends AbstractLoggingActor {

    ////////////////////////
    // Actor Construction //
    ////////////////////////

    public static final String  DEFAULT_NAME = "hintSolver";

    public static Props props(){
        return Props.create(HintSolver.class);
    }

    public HintSolver(){
        this.workers = new ArrayList<>();
    }

    ////////////////////
    // Actor Messages //
    ////////////////////


    public Receive createReceive(){
        return receiveBuilder()
                .match(HintsMessage.class, this::handle)
                .matchAny(object -> this.log().info("Received unknown message: \"{}\"", object.toString()))
                .build();
    }

    protected void handle(HintsMessage message) {
        this.hints = message.getHints();
        log().info("Received Hints and trying to solve them ...");
        for (String hint : this.hints) {
            log().info("Received hint: {} \n", hint);
            // do some preprocessing here
        }
    }

    /////////////////
    // Actor State //
    /////////////////

    private final List<ActorRef> workers;
    private String[] hints;

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
