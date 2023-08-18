package algorithms.hybrid;

import api_implementation.ApiPrinter;
import api_implementation.MhsMxpAbductionManager;
import models.Explanation;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import reasoner.ILoader;
import reasoner.IReasonerManager;

public class ApiExplanationManager extends ExplanationManager {

    private final MhsMxpAbductionManager abductionManager;

    public ApiExplanationManager(ILoader loader, IReasonerManager reasonerManager, MhsMxpAbductionManager abductionManager) {
        super(loader, reasonerManager);
        this.abductionManager = abductionManager;
        printer = new ApiPrinter(abductionManager);
    }

    public void addPossibleExplanation(Explanation explanation) {
        possibleExplanations.add(explanation);
        try {
            if (abductionManager.isMultithread())
                abductionManager.sendExplanation(explanation.createExplanationWrapper());
        } catch(InterruptedException ignored){}
    }

    public void processExplanations(String message) throws OWLOntologyCreationException, OWLOntologyStorageException {
        if (! (message == null))
            abductionManager.setMessage(message);
        showExplanations();
        abductionManager.setExplanations(finalExplanations);
    }
}
