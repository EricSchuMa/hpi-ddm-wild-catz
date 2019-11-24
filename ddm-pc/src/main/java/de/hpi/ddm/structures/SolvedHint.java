package de.hpi.ddm.structures;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
public class SolvedHint implements Serializable {
    private static final long serialVersionUID = 284023216448627600L;
    private List<Character> permutation;
}
