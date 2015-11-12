package au.csiro.data61.docktimizer.exception;

/**
 */
public class HardwareNotFoundException extends Exception {
    public HardwareNotFoundException(String flavorName) {
        super(String.format("Flavor not found %s", flavorName));
    }
}
