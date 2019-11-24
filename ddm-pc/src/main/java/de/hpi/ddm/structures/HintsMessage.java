package de.hpi.ddm.structures;

import akka.actor.ActorRef;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Data @NoArgsConstructor @AllArgsConstructor
public class HintsMessage implements Serializable {
    private static final long serialVersionUID = 172844816448627600L;
    private String[] hints;
    private ArrayList<List<String>> permutationList;
    private List<Character> pchars;
    private ActorRef owner;
}
