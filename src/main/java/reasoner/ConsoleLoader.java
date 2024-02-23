package reasoner;

import common.Configuration;
import common.ConsolePrinter;
import models.Individuals;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import parser.*;

import java.io.File;
import org.apache.log4j.Logger;

public class ConsoleLoader extends Loader {

    public ConsoleLoader(){
        super();
        Logger logger = Logger.getRootLogger();
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
        File ontologyFile = new File(Configuration.INPUT_ONT_FILE);
        ontology = ontologyManager.loadOntologyFromOntologyDocument(ontologyFile);
        originalOntology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(ontologyFile);
        initialOntology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(ontologyFile);
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
