package de.hpi.ddm.structures;

import akka.actor.ActorRef;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
public class WorkPackage implements Serializable {
    private static final long serialVersionUID = 50316216437387600L;
    private String[] targets;
    private List<String> permutations;
    private ActorRef owner;
}
