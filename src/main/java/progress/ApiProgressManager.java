package progress;

import api_implementation.MhsMxpAbductionManager;

public class ApiProgressManager extends ProgressManager {

    MhsMxpAbductionManager abductionManager;

    public ApiProgressManager(MhsMxpAbductionManager abductionManager){
        this.abductionManager = abductionManager;
    }

    protected void processProgress(){
        try {
            if (abductionManager.isMultithread())
                abductionManager.updateProgress(abductionManager.getAbductionMonitor(), currentPercentage, message);
        } catch(InterruptedException ignored) {}
    }



}
