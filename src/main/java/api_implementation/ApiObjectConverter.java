package api_implementation;

import abduction_api.abducible.*;
import abduction_api.exception.CommonException;
import abduction_api.exception.NotSupportedException;

public class ApiObjectConverter {

    static MhsMxpExplanationConfigurator attemptConfiguratorConversion(ExplanationConfigurator configurator){

        MhsMxpExplanationConfigurator newConfigurator = new MhsMxpExplanationConfigurator();
        copyComplexConceptConfiguration(configurator, newConfigurator);
        copyRoleConfiguration(configurator, newConfigurator);
        return newConfigurator;

    }

    private static void copyRoleConfiguration(ExplanationConfigurator oldConfigurator, MhsMxpExplanationConfigurator newConfigurator) {
        try {
            RoleExplanationConfigurator convertedConfigurator = (RoleExplanationConfigurator) oldConfigurator;
            newConfigurator.allowRoleAssertions(convertedConfigurator.areRoleAssertionsAllowed());
            newConfigurator.allowLoops(convertedConfigurator.areLoopsAllowed());
        } catch(NotSupportedException e){
            throw new CommonException("Explanation configurator type not compatible with abduction manager!");
        }
    }

    private static void copyComplexConceptConfiguration(ExplanationConfigurator oldConfigurator,
                                                 MhsMxpExplanationConfigurator newConfigurator) {

        ComplexConceptExplanationConfigurator convertedConfigurator = (ComplexConceptExplanationConfigurator) oldConfigurator;

        try{
            if (!convertedConfigurator.areComplexConceptsAllowed())
                throw new CommonException(
                        "Explanation configurator type not compatible with abduction manager! - MHS-MXP can't disallow complex concepts");
        } catch(NotSupportedException ignored){}

        try {
            newConfigurator.allowConceptComplements(convertedConfigurator.areConceptComplementsAllowed());
        } catch(NotSupportedException e){
            throw new CommonException("Explanation configurator type not compatible with abduction manager!");
        }
    }

    static MhsMxpSymbolAbducibleContainer convertSymbolAbducibles(AbducibleContainer abducibles) {
        SymbolAbducibleContainer symbolAbducibles = (SymbolAbducibleContainer) abducibles;
        return new MhsMxpSymbolAbducibleContainer(symbolAbducibles.getSymbols());
    }

    static MhsMxpAxiomAbducibleContainer convertAxiomAbducibles(AbducibleContainer abducibles) {
        AxiomAbducibleContainer symbolAbducibles = (AxiomAbducibleContainer) abducibles;
        return new MhsMxpAxiomAbducibleContainer(symbolAbducibles.getAxioms());
    }

}
