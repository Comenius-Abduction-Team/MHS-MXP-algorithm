package progress;

import common.Configuration;

public abstract class ProgressManager implements IProgressManager {

    protected double currentPercentage = 0;
    protected String message;

    @Override
    public void updateProgress(int depth, double time) {
        updateProgressAccordingToCorrectFactor(depth, time);
        processProgress();
    }

    @Override
    public void updateProgress(double newPercentage, String message) {
        currentPercentage = newPercentage;
        this.message = message;
        processProgress();
    }

    protected void updateProgressAccordingToCorrectFactor(int depth, double time){

        if (Configuration.DEPTH != null && Configuration.DEPTH > 0 && Configuration.DEPTH < Integer.MAX_VALUE){
            updateProgressAccordingToDepthLimit(depth);
        }
        else if (Configuration.TIMEOUT != null){
            updateProgressAccordingToTimeLimit(time);
            updateMessageAccordingToTimeLimit(time);
            return;
        }
        updateMessageAccordingToDepth(depth);
    }

    abstract protected void processProgress();

    private void updateProgressAccordingToDepthLimit(int depth){
        double remainingPercentage = 99 - currentPercentage;
        int maxDepth = Configuration.DEPTH;
        double percentageToFill = remainingPercentage / Math.pow(3, maxDepth - depth - 1);
        increaseProgress(percentageToFill);
    }

    private void updateMessageAccordingToDepth(int depth){
        message = "Finished tree depth: " + depth;
    }

    protected void updateProgressAccordingToTimeLimit(double time){
        currentPercentage = time / (double) Configuration.TIMEOUT * 99;
    }

    private void updateMessageAccordingToTimeLimit(double time){
        message = "Seconds left until time-out: " + (Configuration.TIMEOUT - time);
    }

    protected void increaseProgress(double percentageToAdd){
        currentPercentage += percentageToAdd;
    }

}
