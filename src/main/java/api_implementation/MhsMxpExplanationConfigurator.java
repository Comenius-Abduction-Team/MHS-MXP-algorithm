package api_implementation;

import abduction_api.abducible.ComplexConceptExplanationConfigurator;
import abduction_api.abducible.RoleExplanationConfigurator;
import abduction_api.exception.NotSupportedException;

public class MhsMxpExplanationConfigurator implements RoleExplanationConfigurator, ComplexConceptExplanationConfigurator {

    private boolean conceptComplements, roles, loops;

    public MhsMxpExplanationConfigurator(){
        setDefaultConfiguration();
    }

    @Override
    public void allowConceptComplements(Boolean allowConceptComplements) {
        conceptComplements = allowConceptComplements;
    }

    @Override
    public boolean areConceptComplementsAllowed() {
        return conceptComplements;
    }

    @Override
    public boolean getDefaultConceptComplementsAllowed() {
        return true;
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
    public boolean getDefaultRoleAssertionsAllowed() throws NotSupportedException {
        return false;
    }

    @Override
    public void allowLoops(Boolean allowLoops) throws NotSupportedException {
        loops = allowLoops;
    }

    @Override
    public boolean areLoopsAllowed() {
        return loops;
    }

    @Override
    public boolean getDefaultLoopsAllowed() {
        return true;
    }

    @Override
    public void setDefaultConfiguration() {
        allowConceptComplements(getDefaultConceptComplementsAllowed());
        allowRoleAssertions(getDefaultRoleAssertionsAllowed());
        allowLoops(getDefaultLoopsAllowed());
    }
}
