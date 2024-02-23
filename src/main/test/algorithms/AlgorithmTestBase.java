package algorithms;

import api_implementation.MhsMxpAbductionManager;
import api_implementation.MhsMxpExplanationConfigurator;
import api_implementation.MhsMxpSymbolAbducibleContainer;
import org.junit.jupiter.api.BeforeEach;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * The type Algorithm test base.
 */
public abstract class AlgorithmTestBase {

    /** Path to the file containing the background knowledge ontology. */
    static String ONTOLOGY_FILE;
    /** String containing the observation axioms in some OWL syntax. */
    static String OBSERVATION;
    /** Prefix to use for abducible symbols (usually a prefix from the BK ontology). */
    static String ABDUCIBLE_PREFIX;

    /** The BK ontology extracted from the file. */
    protected OWLOntology backgroundKnowledge;
    /** The observation extracted from the string. */
    protected Set<OWLAxiom> observation;

    /**
     * The Data factory.
     */
    protected OWLDataFactory dataFactory;
    /**
     * The Prefix manager.
     */
    protected PrefixManager prefixManager;

    /**
     * The Manager.
     */
    protected MhsMxpAbductionManager manager;
    /**
     * The No neg.
     */
    protected MhsMxpExplanationConfigurator noNeg;
    /**
     * The Symbol abd.
     */
    protected MhsMxpSymbolAbducibleContainer symbolAbd;

    /**
     * Instantiates a new Algorithm test base.
     *
     * @throws OWLOntologyCreationException internal OWL API error
     * @throws IOException                  in case of wrong file path
     */
    public AlgorithmTestBase() throws OWLOntologyCreationException, IOException {
        setUpInput();
        setUpHelperObjects();
        setUpAbducibles();
    }

    /** Method that should be overwritten by the test to set its BK, observation and prefix to be used in all test cases. */
    abstract void setUpInput();

    /**
     * Constructs objects necesarry to set up abducibles and run the test cases.
     *
     * @throws OWLOntologyCreationException internal OWL API error
     * @throws IOException                  in case of wrong file path
     */
    void setUpHelperObjects() throws OWLOntologyCreationException, IOException {

        backgroundKnowledge = parseOntologyFromFile(ONTOLOGY_FILE);
        observation = parseAxiomsFromString(OBSERVATION);

        dataFactory = OWLManager.createOWLOntologyManager().getOWLDataFactory();
        prefixManager = new DefaultPrefixManager(ABDUCIBLE_PREFIX);

        noNeg = new MhsMxpExplanationConfigurator();
        noNeg.allowConceptComplements(false);
        symbolAbd = new MhsMxpSymbolAbducibleContainer();

    }

    /** Method that should be overriden by the specific test to set up abducibles used in the abducible test cases. */
    abstract void setUpAbducibles();

    /**
     * Instantiates an abduction manager before each test case.
     */
    @BeforeEach
    void setUp() {
        manager = new MhsMxpAbductionManager(backgroundKnowledge,observation);
    }

    /**
     * Parses ontology from an OWL file.
     *
     * @param filepath path to the ontology file
     * @return the OWL ontology
     * @throws IOException                  in case of wrong file
     * @throws OWLOntologyCreationException internal OWL API error
     */
    public OWLOntology parseOntologyFromFile(String filepath) throws IOException, OWLOntologyCreationException {
        File file = new File(filepath);
        if (!file.exists())
            throw new IOException("File '" + filepath + "' wasn't found!");
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        return manager.loadOntologyFromOntologyDocument(file);
    }

    /**
     * Parses a set of axioms from a string in some OWL syntax.
     *
     * @param string the string
     * @return set of OWL axioms
     * @throws OWLOntologyCreationException internal OWL API error
     */
    public Set<OWLAxiom> parseAxiomsFromString(String string) throws OWLOntologyCreationException {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology observationOntology = manager.loadOntologyFromOntologyDocument(new StringDocumentSource(string));
        return observationOntology.getAxioms();
    }
}
