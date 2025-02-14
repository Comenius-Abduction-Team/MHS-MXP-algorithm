package api_implementation;

import abduction_api.monitor.Percentage;
import progress.ProgressManager;

public class ApiProgressManager extends ProgressManager {

    MhsMxpAbductionManager abductionManager;

    public ApiProgressManager(MhsMxpAbductionManager abductionManager){
        this.abductionManager = abductionManager;
    }

    @Override
    protected void processProgress() {
        int percentage = (int) Math.round(currentPercentage);
        try {
            if (abductionManager.isMultithread())
                abductionManager.updateProgress(new Percentage(percentage), message);
        } catch(InterruptedException ignored) {}
    }

}
