package common;

public interface IPrinter {

    void logInfo(String message);

    void logError(String message, Throwable exception);

    void print(String message);

}
