package de.hpi.ddm.structures;

import akka.actor.ActorRef;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data @NoArgsConstructor @AllArgsConstructor
public class HintsMessage implements Serializable {
    private static final long serialVersionUID = 172844816448627600L;
    private String[] hints;
}
