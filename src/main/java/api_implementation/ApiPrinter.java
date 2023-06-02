package api_implementation;

import common.IPrinter;

public class ApiPrinter implements IPrinter {

    private final MhsMxpAbductionManager abductionManager;

    public ApiPrinter(MhsMxpAbductionManager abductionManager){
        this.abductionManager = abductionManager;
    }

    @Override
    public void logInfo(String message) {
        abductionManager.appendToLog(message);
    }

    @Override
    public void logError(String message, Throwable exception) {
        abductionManager.appendToLog(message);
        abductionManager.appendToLog(exception.getMessage());
        abductionManager.setMessage(exception.getMessage());
    }

    @Override
    public void print(String message) {
        abductionManager.appendToLog(message);
    }
}
