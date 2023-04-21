package apiImplementation;

import abductionapi.abducibles.*;
import abductionapi.exception.AxiomAbducibleException;
import models.Abducibles;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import reasoner.ILoader;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HybridAxiomAbducibleContainer
        extends HybridAbducibleContainer
        implements AxiomAbducibleContainer {

    Set<OWLAxiom> axioms = new HashSet<>();

    HybridAxiomAbducibleContainer(){}

    @Override
    public void addAxiom(OWLAxiom axiom) throws AxiomAbducibleException {
        AxiomType<?> type = axiom.getAxiomType();
        if (
                type == AxiomType.CLASS_ASSERTION
                || type == AxiomType.NEGATIVE_OBJECT_PROPERTY_ASSERTION
                || type == AxiomType.OBJECT_PROPERTY_ASSERTION
        )
            axioms.add(axiom);
        else
            throw new AxiomAbducibleException("axiom " + axiom + " of type: " + type);
    }

    @Override
    public void addAxioms(Set<OWLAxiom> axioms) throws AxiomAbducibleException {
        axioms.forEach(this::addAxiom);
    }

    @Override
    public void addAxioms(List<OWLAxiom> axioms) throws AxiomAbducibleException {
        new HashSet<>(axioms).forEach(this::addAxiom);
    }

    @Override
    public Abducibles exportAbducibles(ILoader loader) {
        return new Abducibles(loader, axioms);
    }

    @Override
    public boolean isEmpty(){
        return axioms.isEmpty();
    }
}
