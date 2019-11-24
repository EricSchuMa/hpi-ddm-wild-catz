package de.hpi.ddm.structures;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data @NoArgsConstructor @AllArgsConstructor
public class FinalResult implements Serializable {
    private static final long serialVersionUID = -52216216448627600L;
    private String password;
    private int id;
}
