package algorithms.hybrid;

import models.Explanation;
import models.Axioms;
import models.IAxioms;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

class Conflict {

    private IAxioms axioms;
    private List<Explanation> explanations;

    Conflict() {
        this.axioms = new Axioms();
        this.explanations = new LinkedList<>();
    }

    Conflict(IAxioms axioms, List<Explanation> explanations) {
        this.axioms = axioms;
        this.explanations = explanations;
    }

    Conflict(Conflict conflict) {
        this.axioms = new Axioms();
        this.axioms.addAll(conflict.getAxioms().getAxioms());

        this.explanations = new LinkedList<>();
        this.explanations.addAll(conflict.getExplanations());
    }

    IAxioms getAxioms() {
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
