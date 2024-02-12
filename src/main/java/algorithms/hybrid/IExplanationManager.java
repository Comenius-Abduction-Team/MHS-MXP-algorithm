package algorithms.hybrid;

import models.Explanation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import java.util.Collection;
import java.util.List;

public interface IExplanationManager {

    void setSolver(HybridSolver solver);

    void addPossibleExplanation(Explanation explanation);

    void setPossibleExplanations(Collection<Explanation> possibleExplanations);

    List<Explanation> getPossibleExplanations();

    int getPossibleExplanationsCount();

    void addLengthOneExplanation(OWLAxiom explanation);

    void setLengthOneExplanations(Collection<OWLAxiom> lengthOneExplanations);

    List<OWLAxiom> getLengthOneExplanations();

    int getLengthOneExplanationsCount();

    void processExplanations(String message) throws OWLOntologyCreationException, OWLOntologyStorageException;

    void showExplanations() throws OWLOntologyStorageException, OWLOntologyCreationException;

    void showError(Throwable e);

    void logMessages(List<String> info, String message);

    void logExplanationsWithDepth(Integer depth, boolean timeout, boolean error, Double time);

    void logExplanationsWithLevel(Integer level, boolean timeout, boolean error, Double time);

}
