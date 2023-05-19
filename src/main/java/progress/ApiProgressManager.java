package progress;

import abduction_api.monitor.Percentage;
import api_implementation.MhsMxpAbductionManager;

public class ApiProgressManager extends ProgressManager {

    MhsMxpAbductionManager abductionManager;

    public ApiProgressManager(MhsMxpAbductionManager abductionManager){
        this.abductionManager = abductionManager;
    }

    protected void processProgress() {
        int percentage = (int) Math.round(currentPercentage);
        try {
            if (abductionManager.isMultithread())
                abductionManager.updateProgress(abductionManager.getAbductionMonitor(), new Percentage(percentage), message);
        } catch(InterruptedException ignored) {}
    }

}
