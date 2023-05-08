package common;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ConsolePrinter extends Printer {

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
            logger.log(Level.WARNING, message);
        else
            logger.log(Level.WARNING, message, exception);
    }

    @Override
    public void print(String message) {
        System.out.println(message);
    }
}
