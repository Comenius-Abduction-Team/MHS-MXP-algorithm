package algorithms;

import abduction_api.manager.ExplanationWrapper;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.io.IOException;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExtractingModel92Test extends AlgorithmTestBase {

    public ExtractingModel92Test() throws OWLOntologyCreationException, IOException {
        super();
    }

    @Override
    void setUpInput() {
        ONTOLOGY_FILE = "files/testExtractingModel9_2.owl";

        OBSERVATION =
                "Prefix: o: <http://www.co-ode.org/ontologies/ont.owl#>"
                        + " Class: o:A Class: o:C Class: o:E"
                        + " Individual: o:a Types: o:A and o:C and o:E";

        ABDUCIBLE_PREFIX = "http://www.co-ode.org/ontologies/ont.owl#";
    }

    @Override
    void setUpAbducibles() {
        OWLClass D = dataFactory.getOWLClass(":D", prefixManager);
        OWLClass E = dataFactory.getOWLClass(":E", prefixManager);
        OWLClass F = dataFactory.getOWLClass(":F", prefixManager);

        symbolAbd.addSymbol(D);
        symbolAbd.addSymbol(E);
        symbolAbd.addSymbol(F);
    }

    @Test
    void defaultMode() {

        manager.solveAbduction();

        Collection<ExplanationWrapper> explanations = manager.getExplanations();
        assertEquals(4, explanations.size());
        //String expected = "[{¬B(a),¬F(a),¬D(a)}, {E(a),¬D(a),A(a)}, {E(a),¬D(a),¬B(a)}, {E(a),C(a),¬B(a)}, {¬D(a),A(a),¬F(a)}, {¬F(a),A(a),C(a)}, {¬F(a),¬B(a),C(a)}]";
//        String expected = "[{E(a),¬D(a)}, {¬F(a),¬D(a)}, {C(a),E(a)}, {¬F(a),C(a)}]";
//        assertEquals(expected, explanations.toString());

    }

    @Test
    void mhs() {

        manager.setPureMhs(true);

        manager.solveAbduction();

        Collection<ExplanationWrapper> explanations = manager.getExplanations();
        assertEquals(4, explanations.size());

    }

    @Test
    void noNeg() {

        manager.setExplanationConfigurator(noNeg);

        manager.solveAbduction();

        Collection<ExplanationWrapper> explanations = manager.getExplanations();
        assertEquals(1, explanations.size());

    }

    @Test
    void mhsNoNeg() {

        manager.setExplanationConfigurator(noNeg);
        manager.setPureMhs(true);

        manager.solveAbduction();

        Collection<ExplanationWrapper> explanations = manager.getExplanations();
        assertEquals(1, explanations.size());

    }

    @Test
    void symbolAbd() {

        manager.setAbducibleContainer(symbolAbd);

        manager.solveAbduction();

        Collection<ExplanationWrapper> explanations = manager.getExplanations();
        assertEquals(2, explanations.size());

    }

    @Test
    void mhsSymbolAbd() {

        manager.setAbducibleContainer(symbolAbd);
        manager.setPureMhs(true);

        manager.solveAbduction();

        Collection<ExplanationWrapper> explanations = manager.getExplanations();
        assertEquals(2, explanations.size());

    }

    void symbolAbdNoNeg() {

        manager.setAbducibleContainer(symbolAbd);
        manager.setExplanationConfigurator(noNeg);

        manager.solveAbduction();

        Collection<ExplanationWrapper> explanations = manager.getExplanations();
        assertEquals(0, explanations.size());

    }

    @Test
    void mhsSymbolAbdNoNeg() {

        manager.setAbducibleContainer(symbolAbd);
        manager.setExplanationConfigurator(noNeg);
        manager.setPureMhs(true);

        manager.solveAbduction();

        Collection<ExplanationWrapper> explanations = manager.getExplanations();
        assertEquals(0, explanations.size());

    }

}
