package progress;

public class ConsoleProgressManager extends ProgressManager {

    final static private int BAR_LENGTH = 50;
    final static private char BAR_BOUNDS_SYMBOL = '|';
    final static private char BAR_FILL_SYMBOL = '-';

    private int currentProgressBlocks = 0;

    @Override
    protected void processProgress() {
        StringBuilder builder = buildProgressBar(currentProgressBlocks, message);
        System.out.println(builder);
    }

    private int percentageToBlocks(double percentage) {
        return (int) Math.ceil(BAR_LENGTH * percentage * 0.01);
    }

    private StringBuilder buildProgressBar(int progress, String message) {
        StringBuilder builder = new StringBuilder(Character.toString(BAR_BOUNDS_SYMBOL));
        for (int i = 0; i < BAR_LENGTH; i++) {
            if (i < progress)
                builder.append(BAR_FILL_SYMBOL);
            else
                builder.append(' ');
        }
        builder.append(BAR_BOUNDS_SYMBOL).append("   ").append(message).append(" ");
        return builder;
    }

    @Override
    public void updateProgress(double newPercentage, String message) {
        currentProgressBlocks = percentageToBlocks(newPercentage);
        super.updateProgress(newPercentage, message);
    }

    @Override
    protected void updateProgressAccordingToTimeLimit(double time){
        super.updateProgressAccordingToTimeLimit(time);
        currentProgressBlocks = percentageToBlocks(currentPercentage);
    }

    @Override
    protected void increaseProgress(double percentageToAdd){
        currentPercentage += percentageToAdd;
        currentProgressBlocks = percentageToBlocks(currentPercentage);
    }
}
