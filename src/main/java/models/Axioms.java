package models;

import common.StringFactory;
import org.semanticweb.owlapi.model.OWLAxiom;

import java.util.*;

public class Axioms implements IAxioms {

    private final Set<OWLAxiom> owlAxioms = new HashSet<>();

    public Axioms() {}

    public Axioms(Collection<OWLAxiom> owlAxioms) {
        this.owlAxioms.addAll(owlAxioms);
    }

    @Override
    public Collection<OWLAxiom> getAxioms() {
        return Collections.unmodifiableSet(owlAxioms);
    }

    @Override
    public Set<OWLAxiom> copyAsSet() {
        return new HashSet<>(owlAxioms);
    }

    @Override
    public List<OWLAxiom> copyAsList() {
        return new ArrayList<>(owlAxioms);
    }

    @Override
    public void addAll(Collection<OWLAxiom> literals){
        owlAxioms.addAll(literals);
    }

    @Override
    public void add(OWLAxiom axiom){
        owlAxioms.add(axiom);
    }

    @Override
    public void remove(OWLAxiom axiom) {owlAxioms.remove(axiom); }

    @Override
    public void removeAll(Collection<OWLAxiom> axioms){
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
