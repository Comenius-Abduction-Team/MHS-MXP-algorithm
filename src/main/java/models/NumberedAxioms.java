package models;

import org.semanticweb.owlapi.model.OWLAxiom;

import java.util.*;
import java.util.stream.Collectors;

public class NumberedAxioms implements IAxioms{

    private int max = 0;
    private List<NumberedAxiom> axioms;

    public NumberedAxioms(Collection<OWLAxiom> axioms){
        this.axioms = new ArrayList<>();
        addAxioms(axioms);
    }

    @Override
    public Set<OWLAxiom> getAxiomSet() {
        return axioms.stream().map(NumberedAxiom::getAxiom).collect(Collectors.toSet());
    }

    @Override
    public List<OWLAxiom> getAxiomList() {
        return axioms.stream().map(NumberedAxiom::getAxiom).collect(Collectors.toList());
    }

    @Override
    public void addAxioms(Collection<OWLAxiom> axioms) {
        axioms.forEach(this::addAxiom);
    }

    @Override
    public void addAxiom(OWLAxiom axiom) {
        max += 1;
        axioms.add(new NumberedAxiom(max,axiom));
    }

    @Override
    public void removeAxiom(OWLAxiom axiom) {
        removeAxioms(Collections.singletonList(axiom));
    }

    @Override
    public void removeAxioms(Collection<OWLAxiom> axioms) {
        this.axioms = this.axioms.stream().filter(axiom -> axioms.contains(axiom.getAxiom()))
                                          .collect(Collectors.toList());
    }

    @Override
    public boolean contains(OWLAxiom axiom) {
        return axioms.stream().anyMatch(numbered -> numbered.getAxiom().equals(axiom));
    }
}
