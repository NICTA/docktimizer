package au.csiro.data61.docktimizer.exception;

import org.jclouds.compute.RunNodesException;

/**
 */
public class NodeStartingException extends Exception {
    public NodeStartingException(RunNodesException e) {
        super(e);
    }
}
