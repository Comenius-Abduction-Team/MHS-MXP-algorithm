package api_implementation;

import abduction_api.abducibles.*;
import abduction_api.exception.AxiomAbducibleException;
import models.Abducibles;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import reasoner.ILoader;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class MhsMxpAxiomAbducibleContainer
        extends MhsMxpAbducibleContainer
        implements AxiomAbducibleContainer {

    Set<OWLAxiom> axioms = new HashSet<>();

    MhsMxpAxiomAbducibleContainer(){}

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
            throw new AxiomAbducibleException(axiom);
    }

    @Override
    public void addAxioms(Collection<OWLAxiom> axioms) throws AxiomAbducibleException {
        axioms.forEach(this::addAxiom);
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
