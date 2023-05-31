package api_implementation;

import abduction_api.abducible.SymbolAbducibleContainer;
import abduction_api.exception.SymbolAbducibleException;
import models.Abducibles;
import org.semanticweb.owlapi.model.*;
import reasoner.ILoader;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class MhsMxpSymbolAbducibleContainer extends MhsMxpAbducibleContainer
        implements SymbolAbducibleContainer{

    public MhsMxpSymbolAbducibleContainer(){}

    public MhsMxpSymbolAbducibleContainer(Collection<OWLEntity> symbols) throws SymbolAbducibleException {
        addSymbols(symbols);
    }

    private Set<OWLNamedIndividual> individuals = new HashSet<>();
    private Set<OWLClass> classes = new HashSet<>();
    private Set<OWLObjectProperty> roles = new HashSet<>();

    @Override
    public void setSymbols(Collection<OWLEntity> collection) throws SymbolAbducibleException {

        Set<OWLClass> classes = new HashSet<>();
        Set<OWLNamedIndividual> individuals = new HashSet<>();
        Set<OWLObjectProperty> roles = new HashSet<>();

        collection.forEach(entity -> addEntityToCorrectSet(entity, individuals, classes, roles));

        this.individuals = individuals;
        this.classes = classes;
        this.roles = roles;

    }

    @Override
    public Set<OWLEntity> getSymbols() {
        Set<OWLEntity> symbols = new HashSet<>(classes);
        symbols.addAll(individuals);
        symbols.addAll(roles);
        return symbols;
    }

    @Override
    public void addSymbol(OWLEntity symbol) throws SymbolAbducibleException {
        addEntityToCorrectSet(symbol, individuals, classes, roles);
    }

    private void addEntityToCorrectSet(
            OWLEntity symbol,
            Set<OWLNamedIndividual> individuals,
            Set<OWLClass> classes,
            Set<OWLObjectProperty> roles)
    {
        EntityType<?> type = symbol.getEntityType();
        if (type == EntityType.CLASS){
            classes.add((OWLClass) symbol);
        }
        else if (type == EntityType.NAMED_INDIVIDUAL){
            individuals.add((OWLNamedIndividual) symbol);
        }
        else if (type == EntityType.OBJECT_PROPERTY){
            roles.add((OWLObjectProperty) symbol);
        }
        else throw new SymbolAbducibleException(symbol);
    }

    @Override
    public void addSymbols(Collection<OWLEntity> symbols) throws SymbolAbducibleException {
        symbols.forEach(this::addSymbol);
    }

    @Override
    public Abducibles exportAbducibles(ILoader loader) {
        return new Abducibles(loader, individuals, classes, roles);
    }

    @Override
    public boolean isEmpty(){
        return classes.isEmpty() && roles.isEmpty();
    }

    @Override
    public void clear() {
        individuals.clear();
        classes.clear();
        roles.clear();
    }

    public Set<OWLNamedIndividual> getIndividuals() {
        return individuals;
    }

    public Set<OWLClass> getClasses() {
        return classes;
    }

    public Set<OWLObjectProperty> getRoles() {
        return roles;
    }
}
