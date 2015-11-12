package au.csiro.data61.docktimizer.exception;

/**
 */
public class CouldNotStartDockerException extends Exception {
    public CouldNotStartDockerException(Exception e) {
        super(e);
    }
}
