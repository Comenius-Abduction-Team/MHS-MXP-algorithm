package models;

import common.StringFactory;
import org.semanticweb.owlapi.model.OWLAxiom;

import java.util.*;

public class Axioms {

    private final Set<OWLAxiom> owlAxioms = new HashSet<>();

    public Axioms() {}

    public Axioms(Collection<OWLAxiom> owlAxioms) {
        this.owlAxioms.addAll(owlAxioms);
    }

    public Set<OWLAxiom> getAxiomSet() {
        return owlAxioms;
    }

    public List<OWLAxiom> getAxiomList() {
        return new ArrayList<>(owlAxioms);
    }

    public void addAxioms(Collection<OWLAxiom> literals){
        owlAxioms.addAll(literals);
    }

    public void addAxiom(OWLAxiom axiom){
        owlAxioms.add(axiom);
    }

    public void removeAxiom(OWLAxiom axiom) {owlAxioms.remove(axiom); }

    public void removeAxioms(Collection<OWLAxiom> axioms){
        owlAxioms.removeAll(axioms);
    }

    public boolean contains(OWLAxiom axiom) { return owlAxioms.contains(axiom); }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (OWLAxiom owlAxiom : owlAxioms) {
            result.append(StringFactory.getRepresentation(owlAxiom)).append(";");
        }
        return result.toString();
    }
}
