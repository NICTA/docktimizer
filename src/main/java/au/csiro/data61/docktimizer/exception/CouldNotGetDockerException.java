package au.csiro.data61.docktimizer.exception;

import com.spotify.docker.client.DockerException;

/**
 */
public class CouldNotGetDockerException extends Exception {
    public CouldNotGetDockerException(DockerException e) {
        super(e);
    }
}
