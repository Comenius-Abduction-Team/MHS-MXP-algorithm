package reasoner;

import application.Application;
import application.ExitCode;
import common.LogMessage;
import models.Abducibles;
import models.Individuals;
import models.Observation;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.knowledgeexploration.OWLKnowledgeExplorerReasoner;
import uk.ac.manchester.cs.jfact.JFactFactory;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class Loader implements ILoader {

    protected Logger logger = Logger.getLogger(Loader.class.getSimpleName());

    protected OWLOntologyManager ontologyManager;
    protected OWLReasonerFactory reasonerFactory;
    protected OWLOntology ontology;
    protected OWLKnowledgeExplorerReasoner reasoner;

    protected Observation observation;
    protected Observation negObservation;
    protected String ontologyIRI;
    protected Individuals namedIndividuals;
    protected OWLOntology originalOntology;
    protected OWLOntology initialOntology; // initial ontology without negated observation
    protected Abducibles abducibles;

    protected OWLDocumentFormat observationOntologyFormat;
    protected boolean isMultipleObservationOnInput = false;
    protected boolean isAxiomBasedAbduciblesOnInput = false;

    protected void loadReasoner(ReasonerType reasonerType) {
        try {
            ontologyManager = OWLManager.createOWLOntologyManager();
            setupOntology();
            changeReasoner(reasonerType);
            initializeReasoner();

            if (reasoner.isConsistent()) {
                logger.log(Level.INFO, LogMessage.INFO_ONTOLOGY_CONSISTENCY);
            } else {
                logger.log(Level.WARNING, LogMessage.ERROR_ONTOLOGY_CONSISTENCY);
                reasoner.dispose();

                Application.finish(ExitCode.ERROR);
            }

        } catch (OWLOntologyCreationException exception) {
            logger.log(Level.WARNING, LogMessage.ERROR_CREATING_ONTOLOGY, exception);
            Application.finish(ExitCode.ERROR);
        }
    }

    protected abstract void setupOntology() throws OWLOntologyCreationException;

    @Override
    public void changeReasoner(ReasonerType reasonerType) {
        // Note: we only use JFact for now

//        switch (reasonerType) {
//            case PELLET:
//                setOWLReasonerFactory(new OpenlletReasonerFactory());
//                break;
//
//            case HERMIT:
//                setOWLReasonerFactory(new ReasonerFactory());
//                break;
//
//            case JFACT:
//                setOWLReasonerFactory(new JFactFactory());
//                break;
//        }

        setOWLReasonerFactory(new JFactFactory());
        reasoner = (OWLKnowledgeExplorerReasoner) reasonerFactory.createReasoner(ontology);
        logger.log(Level.INFO, LogMessage.INFO_ONTOLOGY_LOADED);
    }

    @Override
    public void initializeReasoner() {
        reasoner.flush();
    }

    protected abstract void loadObservation() throws Exception;

    protected abstract void loadAbducibles();

    protected abstract void loadPrefixes();

    public Abducibles getAbducibles(){
        return abducibles;
    }

    @Override
    public Observation getObservation() {
        return observation;
    }

    @Override
    public void setObservation(OWLAxiom observation) {
        this.observation = new Observation(observation);
    }

    @Override
    public void setObservation(OWLAxiom observation, List<OWLAxiom> axiomsInMultipleObservations, OWLNamedIndividual reductionIndividual){
        this.observation = new Observation(observation, axiomsInMultipleObservations, reductionIndividual);
    }

    @Override
    public Observation getNegObservation() {
        return negObservation;
    }

    @Override
    public void setNegObservation(OWLAxiom negObservation) {
        this.negObservation = new Observation(negObservation);
    }

    @Override
    public OWLOntologyManager getOntologyManager() {
        return ontologyManager;
    }

    @Override
    public OWLOntology getOntology() {
        return ontology;
    }

    @Override
    public OWLKnowledgeExplorerReasoner getReasoner() {
        return reasoner;
    }

    @Override
    public void setOWLReasonerFactory(OWLReasonerFactory reasonerFactory) {
        this.reasonerFactory = reasonerFactory;
    }

    @Override
    public String getOntologyIRI() {
        if (ontologyIRI == null) {
            ontologyIRI = ontology.getOntologyID().getOntologyIRI().get().toString();
        }
        return ontologyIRI;
    }

    @Override
    public OWLDataFactory getDataFactory() {
        return ontologyManager.getOWLDataFactory();
    }

    @Override
    public Individuals getIndividuals() {
        return namedIndividuals;
    }

    @Override
    public void addNamedIndividual(OWLNamedIndividual namedIndividual) {
        namedIndividuals.addNamedIndividual(namedIndividual);
    }

    @Override
    public OWLOntology getOriginalOntology() {
        return originalOntology;
    }

    @Override
    public OWLOntology getInitialOntology() {
        return initialOntology;
    }


    public void setObservationOntologyFormat(OWLDocumentFormat observationOntologyFormat) {
        this.observationOntologyFormat = observationOntologyFormat;
    }

    public boolean isMultipleObservationOnInput() {
        return isMultipleObservationOnInput;
    }

    public void setMultipleObservationOnInput(boolean multipleObservationOnInput) {
        isMultipleObservationOnInput = multipleObservationOnInput;
    }

    public boolean isAxiomBasedAbduciblesOnInput() {
        return isAxiomBasedAbduciblesOnInput;
    }

    public void setAxiomBasedAbduciblesOnInput(boolean axiomBasedAbduciblesOnInput) {
        isAxiomBasedAbduciblesOnInput = axiomBasedAbduciblesOnInput;
    }
}
