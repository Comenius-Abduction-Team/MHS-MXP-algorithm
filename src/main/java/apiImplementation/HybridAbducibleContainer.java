package apiImplementation;

import abduction_api.abducibles.*;
import abduction_api.exception.NotSupportedException;
import models.Abducibles;
import reasoner.ILoader;

public abstract class HybridAbducibleContainer
        implements AbducibleContainer, RoleAbducibleConfigurator, ComplexConceptAbducibleConfigurator {
    
    private boolean conceptComplements = true, roles = false, loops = true; 
    
    /**
     * Create an instance of the models.Abducibles class containing abducibles from this container
     * @param loader instance of reasoner.ILooader needed to construct the Abducibles instance
     */
    public abstract Abducibles exportAbducibles(ILoader loader);

    @Override
    public void allowConceptComplements() throws NotSupportedException {
        conceptComplements = true;
    }

    @Override
    public void allowConceptComplements(Boolean allowConceptComplements) throws NotSupportedException {
        conceptComplements = allowConceptComplements;
    }

    @Override
    public boolean areConceptComplementsAllowed() throws NotSupportedException {
        return conceptComplements;
    }

    @Override
    public void allowRoleAssertions() throws NotSupportedException {
        roles = true;
    }

    @Override
    public void allowRoleAssertions(Boolean allowRoleAssertions) throws NotSupportedException {
        roles = allowRoleAssertions;
    }

    @Override
    public boolean areRoleAssertionsAllowed() throws NotSupportedException {
        return roles;
    }

    @Override
    public void allowLoops() {
        loops = true;
    }

    @Override
    public void allowLoops(Boolean allowLoops) throws NotSupportedException {
        loops = allowLoops;
    }

    @Override
    public boolean areLoopsAllowed() throws NotSupportedException {
        return loops;
    }

    public abstract boolean isEmpty();
}
