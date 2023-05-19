package api_implementation;

import abduction_api.abducible.*;
import abduction_api.exception.NotSupportedException;
import models.Abducibles;
import reasoner.ILoader;

public abstract class MhsMxpAbducibleContainer
        implements AbducibleContainer {
    
    /**
     * Create an instance of the models.Abducibles class containing abducibles from this container
     * @param loader instance of reasoner.ILooader needed to construct the Abducibles instance
     */
    public abstract Abducibles exportAbducibles(ILoader loader);

    public abstract boolean isEmpty();
}
