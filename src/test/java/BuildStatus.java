public class BuildStatus {

    private int numberOfBuilds;
    private int nextBuildNumber;

    public BuildStatus(int numberOfBuilds, int nextBuildNumber) {
        this.numberOfBuilds = numberOfBuilds;
        this.nextBuildNumber = nextBuildNumber;
    }

    public int getNumberOfBuilds() {
        return numberOfBuilds;
    }

    public int getNextBuildNumber() {
        return nextBuildNumber;
    }
}
