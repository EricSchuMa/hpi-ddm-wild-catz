package de.hpi.ddm.structures;

import akka.actor.ActorRef;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
public class HintedPasswordMessage implements Serializable{
    private static final long serialVersionUID = 172284516448627600L;
    private String pchars;
    private int plength;
    private String password;
    private List<Character> hints;
}
