package models;

import org.semanticweb.owlapi.model.OWLAxiom;

public class NumberedAxiom {

    private final int number;
    private final OWLAxiom axiom;

    public NumberedAxiom(int number, OWLAxiom axiom) {
        this.number = number;
        this.axiom = axiom;
    }

    public int getNumber() {
        return number;
    }

    public OWLAxiom getAxiom() {
        return axiom;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof NumberedAxiom) {
            NumberedAxiom other = (NumberedAxiom) object;
            return axiom.equals(other.getAxiom());
        }
        if (object instanceof OWLAxiom) {
            OWLAxiom other = (OWLAxiom) object;
            return axiom.equals(other);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return axiom.hashCode();
    }
}
