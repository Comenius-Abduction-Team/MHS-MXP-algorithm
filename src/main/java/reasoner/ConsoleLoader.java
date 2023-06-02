package reasoner;

import common.Configuration;
import common.ConsolePrinter;
import models.Individuals;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import parser.*;

import java.io.File;
import java.util.logging.Logger;

public class ConsoleLoader extends Loader {

    public ConsoleLoader(){
        super();
        Logger logger = Logger.getLogger(Loader.class.getSimpleName());
        printer = new ConsolePrinter(logger);
    }

    @Override
    public void initialize(ReasonerType reasonerType) throws Exception {
        loadReasoner(reasonerType);
        loadObservation();
        loadPrefixes();
        loadAbducibles();
    }

    @Override
    protected void setupOntology() throws OWLOntologyCreationException {
        ontology = ontologyManager.loadOntologyFromOntologyDocument(new File(Configuration.INPUT_ONT_FILE));
        originalOntology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(new File(Configuration.INPUT_ONT_FILE));
        initialOntology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(new File(Configuration.INPUT_ONT_FILE));
    }

    @Override
    protected void loadObservation() throws Exception {
        namedIndividuals = new Individuals();

        IObservationParser observationParser = new ConsoleObservationParser(this);
        observationParser.parse();
    }

    @Override
    protected void loadAbducibles(){
        AbduciblesParser abduciblesParser = new AbduciblesParser(this);
        abducibles = abduciblesParser.parse();
    }

}
