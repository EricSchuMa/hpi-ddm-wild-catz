package de.hpi.ddm.structures;

import akka.actor.ActorRef;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data @NoArgsConstructor @AllArgsConstructor
public class WorkAvailabilityMessage implements Serializable {
    private static final long serialVersionUID = 78916216448627600L;
    private ActorRef workerReference;
}
