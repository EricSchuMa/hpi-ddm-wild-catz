package de.hpi.ddm.actors;

import akka.NotUsed;
import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.pattern.Patterns;
import akka.pattern.Patterns.*;

import akka.util.Timeout;
import de.hpi.ddm.structures.*;
import scala.concurrent.Await;
import scala.concurrent.Future;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class PasswordSolver extends AbstractLoggingActor {

    ////////////////////////
    // Actor Construction //
    ////////////////////////

    public static final String  DEFAULT_NAME = "passwordSolver";

    public static Props props(final ActorRef hintSolver, final ActorRef advancedPasswordSolver){
        return Props.create(PasswordSolver.class, () -> new PasswordSolver(hintSolver, advancedPasswordSolver));
    }

    public PasswordSolver(final ActorRef hintSolver, final ActorRef advancedPasswordSolver){
        this.hintSolver = hintSolver;
        this.advancedPasswordSolver = advancedPasswordSolver;
    }

    ////////////////////
    // Actor Messages //
    ////////////////////


    public Receive createReceive(){
        return receiveBuilder()
                .match(PasswordMessage.class, this::handle)
                .match(WorkAvailabilityMessage.class, this::handle)
                .match(SolvedHint.class, this::handle)
                .match(FinalResult.class, this::handle)
                .matchAny(object -> this.log().info("Received unknown message: \"{}\"", object.toString()))
                .build();
    }

    protected void handle(PasswordMessage message) {
        this.password = message;
        this.master = this.sender();
        log().info("Received Password and trying to solve password ...");

        List<Character> characterList = new ArrayList<>();
        for (char ch : message.getPchars().toCharArray()) {
            characterList.add(ch);
        }

        HintsMessage hints = new HintsMessage(message.getHints(), message.getPermutationList(), characterList, this.self());
        this.hintSolver.tell(hints, this.sender());
    }

    protected void handle(WorkAvailabilityMessage message) {
        if (!hintsSolved){
            hintSolver.tell(message, this.self());
        }
        else {
            if (!passwordSolved)
                advancedPasswordSolver.tell(message, this.self());
            else {
                log().info("Dropping Worker");
            }
        }
    }

    protected void handle(SolvedHint message) throws Exception {
        hintsSolved = true;
        log().info("Received hints {} \n", message.getPermutation().toString());
        HintedPasswordMessage hintedPassword = new HintedPasswordMessage(password.getPchars(),
                password.getPlength(), password.getPassword(), message.getPermutation());
        this.advancedPasswordSolver.tell(hintedPassword, this.self());
    }

    protected void handle(FinalResult message) {
        if (passwordSolved){

        }
        else{
            passwordSolved = true;
            message.setId(password.getId());
            master.tell(message, this.self());
        }
    }

    /////////////////
    // Actor State //
    /////////////////

    private final ActorRef hintSolver;
    private final ActorRef advancedPasswordSolver;
    private ActorRef master;
    private PasswordMessage password;
    private boolean hintsSolved = false;
    private boolean passwordSolved = false;

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