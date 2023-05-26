package api_implementation;

import abduction_api.abducible.*;
import models.Abducibles;
import reasoner.ILoader;

public abstract class MhsMxpAbducibleContainer
        implements AbducibleContainer {
    
    /**
     * Create an instance of the models.Abducibles class containing abducibles from this container
     * @param loader instance of reasoner.ILooader needed to construct the Abducibles instance
     */
    public abstract Abducibles exportAbducibles(ILoader loader);

}
