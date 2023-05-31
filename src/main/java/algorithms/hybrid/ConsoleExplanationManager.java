package algorithms.hybrid;

import common.ConsolePrinter;
import models.Explanation;
import org.semanticweb.owlapi.model.*;
import reasoner.ILoader;
import reasoner.IReasonerManager;

public class ConsoleExplanationManager extends ExplanationManager {

    public ConsoleExplanationManager(ILoader loader, IReasonerManager reasonerManager){
        super(loader, reasonerManager);
        printer = new ConsolePrinter(null);
    }

    @Override
    public void addPossibleExplanation(Explanation explanation) {
        possibleExplanations.add(explanation);
    }

    @Override
    public void processExplanations(String message) throws OWLOntologyCreationException, OWLOntologyStorageException {
        try{
            showExplanations();
        } catch (OWLOntologyCreationException | OWLOntologyStorageException e) {
            e.printStackTrace();
        }
        showMessages(solver.getInfo(), message);

        if (message != null){
            printer.print('\n' + message);
        }
    }

}
