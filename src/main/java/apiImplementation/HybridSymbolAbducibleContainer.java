package apiImplementation;

import abductionapi.abducibles.SymbolAbducibleContainer;
import abductionapi.exception.SymbolAbducibleException;
import models.Abducibles;
import org.semanticweb.owlapi.model.*;
import reasoner.ILoader;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HybridSymbolAbducibleContainer extends HybridAbducibleContainer
        implements SymbolAbducibleContainer{

    HybridSymbolAbducibleContainer(){}

    Set<OWLClass> classes = new HashSet<>();
    Set<OWLNamedIndividual> individuals = new HashSet<>();
    Set<OWLObjectProperty> roles = new HashSet<>();

    @Override
    public void addSymbol(OWLEntity symbol) throws SymbolAbducibleException {
        EntityType<?> type = symbol.getEntityType();
        if (type == EntityType.CLASS){
            classes.add((OWLClass)symbol);
        }
        else if (type == EntityType.NAMED_INDIVIDUAL){
            individuals.add((OWLNamedIndividual)symbol);
        }
        else if (type == EntityType.OBJECT_PROPERTY){
            roles.add((OWLObjectProperty)symbol);
        }
        else throw new SymbolAbducibleException("symbol " + symbol + " of type: " + type);
    }

    @Override
    public void addSymbols(Set<OWLEntity> symbols) throws SymbolAbducibleException {
        symbols.forEach(this::addSymbol);
    }

    @Override
    public void addSymbols(List<OWLEntity> symbols) throws SymbolAbducibleException {
        new HashSet<>(symbols).forEach(this::addSymbol);
    }

    @Override
    public Abducibles exportAbducibles(ILoader loader) {
        return new Abducibles(loader, individuals, classes, roles);
    }

    @Override
    public boolean isEmpty(){
        return individuals.isEmpty() || (classes.isEmpty() && roles.isEmpty());
    }
}