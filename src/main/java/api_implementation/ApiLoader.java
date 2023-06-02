package api_implementation;

import abduction_api.abducible.AxiomAbducibleContainer;
import models.Abducibles;
import models.Individuals;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import parser.IObservationParser;
import parser.PrefixesParser;
import reasoner.Loader;
import reasoner.ReasonerType;

import java.util.stream.Collectors;

public class ApiLoader extends Loader {

    private final MhsMxpAbductionManager abductionManager;

    public ApiLoader(MhsMxpAbductionManager abductionManager){
        this.abductionManager = abductionManager;
        printer = new ApiPrinter(abductionManager);
    }

    @Override
    public void initialize(ReasonerType reasonerType) throws Exception {
        loadReasoner(reasonerType);
        loadObservation();
        loadAbducibles();
    }

    @Override
    protected void setupOntology() throws OWLOntologyCreationException {

        ontology = this.abductionManager.getBackgroundKnowledge();
        ontologyManager = ontology.getOWLOntologyManager();

        observationOntologyFormat = ontology.getFormat();
        ontologyIRI = ontology.getOntologyID().getOntologyIRI().toString();

        originalOntology = ontologyManager.createOntology();
        copyOntology(ontology, originalOntology);

        initialOntology = ontologyManager.createOntology();
        copyOntology(ontology, initialOntology);
    }

    private void copyOntology(OWLOntology oldOntology, OWLOntology newOntology){
        ontologyManager.addAxioms(newOntology, oldOntology.getAxioms());
    }

    @Override
    protected void loadObservation() throws Exception {
        namedIndividuals = new Individuals();
        IObservationParser observationParser = new ApiObservationParser(this, abductionManager);
        observationParser.parse();
    }

    @Override
    protected void loadAbducibles() {
        MhsMxpAbducibleContainer container = abductionManager.getAbducibleContainer();

        if (container == null || container.isEmpty()) {
            abducibles = new Abducibles(this);
            return;
        }

        if (container instanceof AxiomAbducibleContainer)
            isAxiomBasedAbduciblesOnInput = true;

        abducibles = container.exportAbducibles(this);

        if (container instanceof MhsMxpSymbolAbducibleContainer) {
            MhsMxpSymbolAbducibleContainer converted = (MhsMxpSymbolAbducibleContainer) container;
            if (converted.getIndividuals().isEmpty()) {
                abducibles.addIndividuals(ontology.individualsInSignature().collect(Collectors.toList()));
            }
        }
    }
}
