package parser;

import common.Configuration;
import common.ConsolePrinter;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.*;
import reasoner.Loader;

import org.apache.log4j.Logger;

public class ConsoleObservationParser extends ObservationParser {

    public ConsoleObservationParser(Loader loader) {
        super(loader);
        printer = new ConsolePrinter(Logger.getRootLogger());
    }

    @Override
    protected void createOntologyFromObservation() throws OWLOntologyCreationException, OWLOntologyStorageException {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology observationOntology = manager.loadOntologyFromOntologyDocument(new StringDocumentSource(Configuration.OBSERVATION));

        StringDocumentTarget documentTarget = new StringDocumentTarget();
        observationOntology.saveOntology(documentTarget);

        //variable "format" - used in PrefixesParser
        OWLDocumentFormat format = manager.getOntologyFormat(observationOntology);
        loader.setObservationOntologyFormat(format);

        processAxiomsFromObservation(observationOntology);
    }
}

