package algorithms;

import abduction_api.manager.ExplanationWrapper;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.io.IOException;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FamilyTest extends AlgorithmTestBase {

    public FamilyTest() throws OWLOntologyCreationException, IOException {
        super();
    }

    @Override
    void setUpInput() {
        ONTOLOGY_FILE = "files/family2.owl";

        OBSERVATION =
                "Prefix: prefix1: <http://www.semanticweb.org/chrumka/ontologies/2020/4/untitled-ontology-13#>"
                + "Class: prefix1:Father Class: prefix1:Mother "
                + "Individual: prefix1:jack Types: prefix1:Father Individual: prefix1:jane Types: prefix1:Mother";

        ABDUCIBLE_PREFIX = "http://www.semanticweb.org/chrumka/ontologies/2020/4/untitled-ontology-13#";
    }

    @Override
    void setUpAbducibles() {
        OWLClass grandfather = dataFactory.getOWLClass(":Grandfather", prefixManager);
        OWLClass grandmother = dataFactory.getOWLClass(":Grandmother", prefixManager);

        symbolAbd.addSymbol(grandfather);
        symbolAbd.addSymbol(grandmother);
    }

//    @Test
//    void defaultMode() {
//
//        manager.solveAbduction();
//
//        Collection<ExplanationWrapper> explanations = manager.getExplanations();
//
//        assertEquals(7, explanations.size());
//
//    }
//
//    @Test
//    void mhs() {
//
//        manager.setPureMhs(true);
//
//        manager.solveAbduction();
//
//        Collection<ExplanationWrapper> explanations = manager.getExplanations();
//        assertEquals(7, explanations.size());
//
//    }

    @Test
    void noNeg() {

        manager.setExplanationConfigurator(noNeg);

        manager.solveAbduction();

        Collection<ExplanationWrapper> explanations = manager.getExplanations();
        System.out.println(explanations);
        assertEquals(4, explanations.size());

    }

    @Test
    void mhsNoNeg() {

        manager.setExplanationConfigurator(noNeg);
        manager.setPureMhs(true);

        manager.solveAbduction();

        Collection<ExplanationWrapper> explanations = manager.getExplanations();
        assertEquals(4, explanations.size());

    }

    @Test
    void symbolAbd() {

        manager.setAbducibleContainer(symbolAbd);

        manager.solveAbduction();

        Collection<ExplanationWrapper> explanations = manager.getExplanations();
        assertEquals(1, explanations.size());

    }

    @Test
    void mhsSymbolAbd() {

        manager.setAbducibleContainer(symbolAbd);
        manager.setPureMhs(true);

        manager.solveAbduction();

        Collection<ExplanationWrapper> explanations = manager.getExplanations();
        assertEquals(1, explanations.size());

    }

    void symbolAbdNoNeg() {

        manager.setAbducibleContainer(symbolAbd);
        manager.setExplanationConfigurator(noNeg);

        manager.solveAbduction();

        Collection<ExplanationWrapper> explanations = manager.getExplanations();
        assertEquals(1, explanations.size());

    }

    @Test
    void mhsSymbolAbdNoNeg() {

        manager.setAbducibleContainer(symbolAbd);
        manager.setExplanationConfigurator(noNeg);
        manager.setPureMhs(true);

        manager.solveAbduction();

        Collection<ExplanationWrapper> explanations = manager.getExplanations();
        assertEquals(1, explanations.size());

    }

}
