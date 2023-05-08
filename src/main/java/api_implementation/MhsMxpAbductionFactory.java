package api_implementation;

import abduction_api.abducibles.AxiomAbducibleContainer;
import abduction_api.abducibles.SymbolAbducibleContainer;
import abduction_api.factories.AbductionFactory;
import abduction_api.manager.AbductionManager;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;

import java.util.Set;

public class MhsMxpAbductionFactory implements AbductionFactory{

    private static final MhsMxpAbductionFactory instance = new MhsMxpAbductionFactory();

    private MhsMxpAbductionFactory(){}

    public static MhsMxpAbductionFactory getFactory(){
        return instance;
    }

    @Override
    public AbductionManager getAbductionManager() {
        return new MhsMxpAbductionManager();
    }

    @Override
    public AbductionManager getAbductionManagerWithInput(OWLOntology owlOntology, OWLAxiom owlAxiom) {
        AbductionManager manager = new MhsMxpAbductionManager();
        manager.setBackgroundKnowledge(owlOntology);
        manager.setObservation(owlAxiom);
        return manager;
    }

    @Override
    public AbductionManager getAbductionManagerWithSymbolAbducibles(Set<OWLEntity> symbols) {
        AbductionManager manager = new MhsMxpAbductionManager();
        SymbolAbducibleContainer container = new MhsMxpSymbolAbducibleContainer();
        container.addSymbols(symbols);
        manager.setAbducibleContainer(container);
        return manager;
    }

    @Override
    public AbductionManager getAbductionManagerWithAxiomAbducibles(Set<OWLAxiom> axioms) {
        AbductionManager manager = new MhsMxpAbductionManager();
        AxiomAbducibleContainer container = new MhsMxpAxiomAbducibleContainer();
        container.addAxioms(axioms);
        manager.setAbducibleContainer(container);
        return manager;
    }

    @Override
    public AxiomAbducibleContainer getAxiomAbducibleContainer() {
        return new MhsMxpAxiomAbducibleContainer();
    }

    @Override
    public SymbolAbducibleContainer getSymbolAbducibleContainer() {
        return new MhsMxpSymbolAbducibleContainer();
    }
}
