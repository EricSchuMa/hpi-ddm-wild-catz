package de.hpi.ddm.actors;

import akka.actor.AbstractActor;
import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;

import akka.remote.EndpointManager;
import de.hpi.ddm.structures.PasswordMessage;

import akka.japi.pf.ReceiveBuilder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

public class PasswordSolver extends AbstractLoggingActor {

    ////////////////////////
    // Actor Construction //
    ////////////////////////

    public static final String  DEFAULT_NAME = "passwordSolver";

    public static Props props(){
        return Props.create(PasswordSolver.class);
    }

    public PasswordSolver(){
        this.workers = new ArrayList<>();
        this.password = new PasswordMessage();
    }

    ////////////////////
    // Actor Messages //
    ////////////////////


    public Receive createReceive(){
        return receiveBuilder()
                .match(PasswordMessage.class, object -> this.log().info("Received Password Message"))
                .matchAny(object -> this.log().info("Received unknown message: \"{}\"", object.toString()))
                .build();
    }

    /////////////////
    // Actor State //
    /////////////////

    private final List<ActorRef> workers;
    private final PasswordMessage password;

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