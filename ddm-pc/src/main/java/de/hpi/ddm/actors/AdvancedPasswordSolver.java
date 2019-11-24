package de.hpi.ddm.actors;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import de.hpi.ddm.structures.*;
import de.hpi.ddm.utils.GFG;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class AdvancedPasswordSolver extends AbstractLoggingActor {

    ////////////////////////
    // Actor Construction //
    ////////////////////////

    public static final String  DEFAULT_NAME = "advancedPasswordSolver";

    public static Props props(){
        return Props.create(AdvancedPasswordSolver.class, AdvancedPasswordSolver::new);
    }

    public AdvancedPasswordSolver(){

    }

    ////////////////////
    // Actor Messages //
    ////////////////////


    public Receive createReceive(){
        return receiveBuilder()
                .match(HintedPasswordMessage.class, this::handle)
                .match(WorkAvailabilityMessage.class, this::handle)
                .match(SolvedHint.class, this::handle)
                .matchAny(object -> this.log().info("Received unknown message: \"{}\"", object.toString()))
                .build();
    }

    protected void handle(HintedPasswordMessage message) {
        passwordSolver = this.sender();
        //log().info("Received hinted Password");
        GFG combinationCalculator = new GFG();
        List<Character> availableCharacterSet = new ArrayList<>();
        for (char possibleCharacter: message.getPchars().toCharArray()) {
            if (!message.getHints().contains(possibleCharacter)) {
                availableCharacterSet.add(possibleCharacter);
            }
        }
        List<String> possiblePasswords = new ArrayList<>();
        int plen = message.getPlength();
        combinationCalculator.printAllKLength(availableCharacterSet, plen, possiblePasswords);
        //log().info("Passwords {}", possiblePasswords.toString());

        this.possiblePasswords = possiblePasswords;
        this.password = message.getPassword();
    }

    protected void handle(WorkAvailabilityMessage message) {
        String [] target = {password};
        WorkPackage workPackage = new WorkPackage(target, possiblePasswords, this.self());
        message.getWorkerReference().tell(workPackage, this.self());
    }

    protected void handle(SolvedHint message) {
        List<Character> result = message.getPermutation();

        StringBuilder sb = new StringBuilder();
        // Appends characters one by one
        for (Character ch : result) {
            sb.append(ch);
        }
        // convert in string
        String result_string = sb.toString();

        passwordSolver.tell(new FinalResult(result_string, 0), this.self());
    }

    /////////////////
    // Actor State //
    /////////////////

    private List<String> possiblePasswords;
    private String password;
    private ActorRef passwordSolver;
    private boolean ready = false;

}
