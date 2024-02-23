package common;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class ConsolePrinter implements IPrinter {

    private final Logger logger;

    public ConsolePrinter(Logger logger){
        this.logger = logger;
    }

    @Override
    public void logInfo(String message) {
        logger.log(Level.INFO, message);
    }

    @Override
    public void logError(String message, Throwable exception) {
        if (exception == null)
            logger.log(Level.WARN, message);
        else
            logger.log(Level.WARN, message, exception);
    }

    @Override
    public void print(String message) {
        System.out.println(message);
    }
}
