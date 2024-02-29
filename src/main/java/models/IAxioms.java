package models;

import org.semanticweb.owlapi.model.OWLAxiom;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface IAxioms {

    Collection<OWLAxiom> getAxioms();

    void add(OWLAxiom axiom);

    void addAll(Collection<OWLAxiom> axioms);

    void remove(OWLAxiom axiom);

    void removeAll(Collection<OWLAxiom> axioms);

    int size();

}
