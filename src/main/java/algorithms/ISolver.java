package algorithms;

import models.Explanation;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import reasoner.ILoader;
import reasoner.IReasonerManager;

import java.util.Collection;


public interface ISolver {

    void solve(ILoader loader, IReasonerManager reasonerManager) throws OWLOntologyStorageException, OWLOntologyCreationException;

    Collection<Explanation> getExplanations();

}
