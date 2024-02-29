package algorithms.hst;

import org.semanticweb.owlapi.model.OWLAxiom;

import java.util.*;

public class NumberedAxiomsSingleMap implements INumberedAxioms{

    public static final Integer DEFAULT_INDEX = -100;

    Map<OWLAxiom, Integer> axiomToIndex = new HashMap<>();

    public NumberedAxiomsSingleMap(Collection<OWLAxiom> owlAxioms) {
        addAll(owlAxioms);
    }

    @Override
    public Collection<OWLAxiom> getAxioms() {
        return Collections.unmodifiableSet(axiomToIndex.keySet());
    }

    @Override
    public void add(OWLAxiom axiom) {
        axiomToIndex.put(axiom, DEFAULT_INDEX);
    }

    @Override
    public void addAll(Collection<OWLAxiom> axioms) {
        axioms.forEach(this::add);
    }

    @Override
    public void remove(OWLAxiom axiom) {
        axiomToIndex.remove(axiom);
    }

    @Override
    public void removeAll(Collection<OWLAxiom> axioms) {
        axioms.forEach(this::remove);
    }

    public boolean contains(OWLAxiom axiom) {
        return axiomToIndex.containsKey(axiom);
    }

    public Integer getIndex(OWLAxiom axiom){
        return axiomToIndex.get(axiom);
    }

    public void addWithIndex(OWLAxiom axiom, Integer index){
        axiomToIndex.put(axiom,index);
    }

    @Override
    public String toString() {
        return axiomToIndex.toString();
    }

    public OWLAxiom getAxiomByIndex(int index){
        for (OWLAxiom axiom : axiomToIndex.keySet()){
            if (axiomToIndex.get(axiom).equals(index))
                return axiom;
        }
        return null;
    }

    @Override
    public boolean shouldBeIndexed(OWLAxiom axiom) {
        return (DEFAULT_INDEX.equals(getIndex(axiom)));
    }

    @Override
    public int size() {
        return axiomToIndex.size();
    }
}
