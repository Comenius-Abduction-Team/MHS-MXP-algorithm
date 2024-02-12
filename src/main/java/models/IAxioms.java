package models;

import org.semanticweb.owlapi.model.OWLAxiom;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface IAxioms {

    Set<OWLAxiom> getAxiomSet();

    List<OWLAxiom> getAxiomList();

    void addAxioms(Collection<OWLAxiom> axioms);

    void addAxiom(OWLAxiom axiom);

    void removeAxiom(OWLAxiom axiom);

    void removeAxioms(Collection<OWLAxiom> axioms);

    boolean contains(OWLAxiom axiom);

}
