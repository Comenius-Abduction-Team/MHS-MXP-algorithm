package models;

import org.semanticweb.owlapi.model.OWLAxiom;

import java.util.*;

public class NumberedAxioms implements IAxioms{

    public static final Integer DEFAULT_INDEX = -1;

    Map<OWLAxiom, Integer> axioms = new HashMap<>();

    @Override
    public Collection<OWLAxiom> getAxioms() {
        return axioms.keySet();
    }

    @Override
    public Set<OWLAxiom> copyAsSet() {
        return new HashSet<>(axioms.keySet());
    }

    @Override
    public List<OWLAxiom> copyAsList() {
        return new ArrayList<>(axioms.keySet());
    }

    @Override
    public void add(OWLAxiom axiom) {
        if (!axioms.containsKey(axiom))
            axioms.put(axiom, DEFAULT_INDEX);
    }

    @Override
    public void addAll(Collection<OWLAxiom> axioms) {
        axioms.forEach(this::add);
    }

//    @Override
//    public void addAll(IAxioms axioms) {
//        axioms.toSet().forEach(this::add);
//    }

    public void addWithIndex(OWLAxiom axiom, Integer index) {
        if (!axioms.containsKey(axiom))
            axioms.put(axiom,index);
    }

//    @Override
//    public void set(Collection<OWLAxiom> axioms) {
//        this.axioms = new HashMap<>();
//        addAll(axioms);
//    }

    @Override
    public void remove(OWLAxiom axiom) {
        axioms.remove(axiom);
    }

    @Override
    public void removeAll(Collection<OWLAxiom> axioms) {
        this.axioms.keySet().removeAll(axioms);
    }

//    @Override
//    public void removeAll(IAxioms axioms) {
//        this.axioms.keySet().removeAll(axioms.toSet());
//    }

    public boolean contains(OWLAxiom axiom) {
        return axioms.containsKey(axiom);
    }

//    @Override
//    public boolean containsAll(Collection<OWLAxiom> axioms) {
//        return this.axioms.keySet().containsAll(axioms);
//    }

//    @Override
//    public boolean containsAll(IAxioms axioms) {
//        return this.axioms.keySet().containsAll(axioms.toSet());
//    }

//    @Override
//    public int size() {
//        return axioms.size();
//    }
//
//    @Override
//    public boolean isEmpty() {
//        return axioms.isEmpty();
//    }

//    @Override
//    public IAxioms copy() {
//        NumberedAxioms copy = new NumberedAxioms();
//        axioms.forEach(copy::addAxiom);
//        return copy;
//    }

//    @Override
//    public Stream<OWLAxiom> stream() {
//        return axioms.keySet().stream();
//    }

    public Integer getIndex(OWLAxiom axiom){
        return axioms.get(axiom);
    }

    public void setIndex(OWLAxiom axiom, Integer index){
        axioms.put(axiom,index);
    }
}
