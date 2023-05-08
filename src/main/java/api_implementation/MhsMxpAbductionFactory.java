package api_implementation;

import abduction_api.abducibles.AxiomAbducibleContainer;
import abduction_api.abducibles.SymbolAbducibleContainer;
import abduction_api.exception.AxiomAbducibleException;
import abduction_api.exception.InvalidObservationException;
import abduction_api.exception.SymbolAbducibleException;
import abduction_api.factories.AbductionFactory;
import abduction_api.manager.AbductionManager;
import abduction_api.manager.MultiObservationManager;
import abduction_api.manager.ThreadAbductionManager;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;

import java.util.Collection;
import java.util.HashSet;

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
    public AbductionManager getAbductionManager(OWLOntology backgroundKnowledge, OWLAxiom observation)
            throws InvalidObservationException {
        return new MhsMxpAbductionManager(backgroundKnowledge, observation);
    }

    @Override
    public MultiObservationManager getMultiObservationAbductionManager() {
        return new MhsMxpAbductionManager();
    }

    @Override
    public MultiObservationManager getMultiObservationAbductionManager(
            OWLOntology backgroundKnowledge, Collection<OWLAxiom> observation)
            throws InvalidObservationException {
        return new MhsMxpAbductionManager(backgroundKnowledge, new HashSet<>(observation));
    }

    @Override
    public ThreadAbductionManager getThreadAbductionManager() {
        return new MhsMxpAbductionManager();
    }

    @Override
    public ThreadAbductionManager getThreadAbductionManager(OWLOntology backgroundKnowledge, OWLAxiom observation)
            throws InvalidObservationException {
        return new MhsMxpAbductionManager(backgroundKnowledge, observation);
    }

    @Override
    public AxiomAbducibleContainer getAxiomAbducibleContainer() {
        return new MhsMxpAxiomAbducibleContainer();
    }

    @Override
    public AxiomAbducibleContainer getAxiomAbducibleContainer(Collection<OWLAxiom> axioms)
            throws AxiomAbducibleException {
        return new MhsMxpAxiomAbducibleContainer(axioms);
    }

    @Override
    public SymbolAbducibleContainer getSymbolAbducibleContainer() {
        return new MhsMxpSymbolAbducibleContainer();
    }

    @Override
    public SymbolAbducibleContainer getSymbolAbducibleContainer(Collection<OWLEntity> symbols)
            throws SymbolAbducibleException {
        return new MhsMxpSymbolAbducibleContainer(symbols);
    }
}
