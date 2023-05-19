package parser;

import api_implementation.MhsMxpAbductionManager;
import common.ApiPrinter;
import common.Configuration;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import reasoner.ApiLoader;

public class ApiObservationParser extends ObservationParser {

    private final MhsMxpAbductionManager abductionManager;

    public ApiObservationParser(ApiLoader loader, MhsMxpAbductionManager abdctionManager){
        super(loader);
        this.abductionManager = abdctionManager;
        printer = new ApiPrinter(abdctionManager);
    }

    @Override
    protected void createOntologyFromObservation() throws OWLOntologyCreationException {

        OWLOntologyManager ontologyManager = OWLManager.createOWLOntologyManager();
        OWLOntology observationOntology = ontologyManager.createOntology(abductionManager.getMultiAxiomObservation());
        Configuration.OBSERVATION = observationOntology.toString();
        processAxiomsFromObservation(observationOntology);

    }

}
