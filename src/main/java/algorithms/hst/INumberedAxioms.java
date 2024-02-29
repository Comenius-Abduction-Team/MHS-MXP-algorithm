package algorithms.hst;

import models.IAxioms;
import org.semanticweb.owlapi.model.OWLAxiom;

public interface INumberedAxioms extends IAxioms {

    boolean shouldBeIndexed(OWLAxiom axiom);

    void addWithIndex(OWLAxiom axiom, Integer index);

    OWLAxiom getAxiomByIndex(int index);

}
