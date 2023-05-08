package algorithms.hybrid;

import api_implementation.MhsMxpAbductionManager;
import common.ApiPrinter;
import models.Explanation;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import reasoner.ILoader;
import reasoner.IReasonerManager;

public class ApiExplanationManager extends ConsoleExplanationManager {

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
                abductionManager.sendExplanation(abductionManager.getAbductionMonitor(),
                        explanation.createExplanationWrapper());
        } catch(InterruptedException ignored){}
    }

    public void processExplanations(String message) throws OWLOntologyCreationException, OWLOntologyStorageException {
        abductionManager.setMessage(message);
        showExplanations();
        abductionManager.setExplanations(finalExplanations);
    }
}
