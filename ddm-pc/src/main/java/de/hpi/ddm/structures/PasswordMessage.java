package de.hpi.ddm.structures;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


@Data @NoArgsConstructor @AllArgsConstructor
public class PasswordMessage implements Serializable {
    private static final long serialVersionUID = -50316216448627600L;
    private int id;
    private String name;
    private String pchars;
    private int plength;
    private String password;
    private String [] hints;
    private ArrayList<List<String>> permutationList;
}