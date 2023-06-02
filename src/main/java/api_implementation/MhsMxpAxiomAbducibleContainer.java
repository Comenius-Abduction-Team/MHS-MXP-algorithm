package api_implementation;

import abduction_api.abducible.*;
import abduction_api.exception.AxiomAbducibleException;
import models.Abducibles;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import reasoner.ILoader;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class MhsMxpAxiomAbducibleContainer extends MhsMxpAbducibleContainer implements AxiomAbducibleContainer {

    Set<OWLAxiom> axioms = new HashSet<>();

    public MhsMxpAxiomAbducibleContainer(){}

    public MhsMxpAxiomAbducibleContainer(Collection<OWLAxiom> axioms) throws AxiomAbducibleException {
        addAxioms(axioms);
    }

    @Override
    public void setAxioms(Collection<OWLAxiom> axioms) throws AxiomAbducibleException {
        Set<OWLAxiom> newAxioms = new HashSet<>();
        axioms.forEach(axiom -> addAxiomToSet(axiom, newAxioms));
        this.axioms = newAxioms;
    }

    private void addAxiomToSet(OWLAxiom axiom, Set<OWLAxiom> set) {
        AxiomType<?> type = axiom.getAxiomType();
        if (
                type == AxiomType.CLASS_ASSERTION
                        || type == AxiomType.NEGATIVE_OBJECT_PROPERTY_ASSERTION
                        || type == AxiomType.OBJECT_PROPERTY_ASSERTION
        )
            set.add(axiom);
        else
            throw new AxiomAbducibleException(axiom);
    }

    @Override
    public Set<OWLAxiom> getAxioms() {
        return axioms;
    }

    @Override
    public void addAxiom(OWLAxiom axiom) throws AxiomAbducibleException {
        addAxiomToSet(axiom, axioms);
    }

    @Override
    public void addAxioms(Collection<OWLAxiom> axioms) throws AxiomAbducibleException {
        Set<OWLAxiom> newAxioms = new HashSet<>();
        axioms.forEach(axiom -> addAxiomToSet(axiom, newAxioms));
        this.axioms.addAll(newAxioms);
    }

    @Override
    public Abducibles exportAbducibles(ILoader loader) {
        return new Abducibles(loader, axioms);
    }

    @Override
    public boolean isEmpty(){
        return axioms.isEmpty();
    }

    @Override
    public void clear() {
        axioms.clear();
    }
}
