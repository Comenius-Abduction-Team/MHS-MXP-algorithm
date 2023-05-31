package reasoner;

import abduction_api.abducible.AxiomAbducibleContainer;
import api_implementation.MhsMxpAbducibleContainer;
import api_implementation.MhsMxpAbductionManager;
import api_implementation.MhsMxpSymbolAbducibleContainer;
import common.ApiPrinter;
import models.Abducibles;
import models.Individuals;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import parser.ApiObservationParser;
import parser.IObservationParser;
import parser.PrefixesParser;

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
    protected void loadPrefixes() {
        PrefixesParser prefixesParser = new PrefixesParser(observationOntologyFormat);
        prefixesParser.parse();
    }

    @Override
    protected void loadAbducibles(){
        MhsMxpAbducibleContainer container = abductionManager.getAbducibleContainer();

        if (container == null || container.isEmpty()){
            abducibles = new Abducibles(this);
            return;
        }

        if (abductionManager.getAbducibleContainer() instanceof AxiomAbducibleContainer)
            isAxiomBasedAbduciblesOnInput = true;

        if (abductionManager.getAbducibleContainer() instanceof MhsMxpSymbolAbducibleContainer){
            MhsMxpSymbolAbducibleContainer converted = (MhsMxpSymbolAbducibleContainer) container;
            if (converted.getIndividuals().isEmpty())
                getOntology().getIndividualsInSignature().forEach(converted::addSymbol);
        }

        abducibles = abductionManager.getAbducibleContainer().exportAbducibles(this);
    }
}
