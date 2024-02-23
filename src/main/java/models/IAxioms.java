package models;

import org.semanticweb.owlapi.model.OWLAxiom;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface IAxioms {

    Collection<OWLAxiom> getAxioms();

    Set<OWLAxiom> copyAsSet();

    List<OWLAxiom> copyAsList();

    void addAll(Collection<OWLAxiom> axioms);

    void add(OWLAxiom axiom);

    void remove(OWLAxiom axiom);

    void removeAll(Collection<OWLAxiom> axioms);

    boolean contains(OWLAxiom axiom);

}
