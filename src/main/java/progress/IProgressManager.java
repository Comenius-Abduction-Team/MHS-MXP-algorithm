package progress;

public interface IProgressManager {

    void updateProgress(int depth, double time);

    void updateProgress(double newPercentage, String message);

}
