package common;

public abstract class Printer {

    public abstract void logInfo(String message);

    public abstract void logError(String message, Throwable exception);

    public abstract void print(String message);

}
