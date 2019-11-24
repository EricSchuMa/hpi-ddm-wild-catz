package de.hpi.ddm.structures;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data @NoArgsConstructor @AllArgsConstructor
public class PasswordMessage {
    private int id;
    private String name;
    private String pchars;
    private int plength;
    private String password;
    private String [] hints;
}
