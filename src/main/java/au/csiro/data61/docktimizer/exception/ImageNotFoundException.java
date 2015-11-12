package au.csiro.data61.docktimizer.exception;

/**
 */
public class ImageNotFoundException extends Exception {
    public ImageNotFoundException(String imageID) {
        super(String.format("Image not found %s", imageID));

    }
}
