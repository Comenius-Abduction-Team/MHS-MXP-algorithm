package algorithms.hybrid;

import models.Explanation;
import models.Axioms;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

class Conflict {

    private Axioms axioms;
    private List<Explanation> explanations;

    Conflict() {
        this.axioms = new Axioms();
        this.explanations = new LinkedList<>();
    }

    Conflict(Axioms axioms, List<Explanation> explanations) {
        this.axioms = axioms;
        this.explanations = explanations;
    }

    Conflict(Conflict conflict) {
        this.axioms = new Axioms();
        this.axioms.getAxiomSet().addAll(conflict.getAxioms().getAxiomSet());

        this.explanations = new LinkedList<>();
        this.explanations.addAll(conflict.getExplanations());
    }

    Axioms getAxioms() {
        if (axioms == null) {
            axioms = new Axioms(new HashSet<>());
        }
        return axioms;
    }

    List<Explanation> getExplanations() {
        if (explanations == null) {
            explanations = new LinkedList<>();
        }
        return explanations;
    }
}
