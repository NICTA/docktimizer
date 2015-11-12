package au.csiro.data61.docktimizer.helper;

import au.csiro.data61.docktimizer.testClient.DockerPlacementServiceTest;
import net.sf.javailp.*;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 */
public class ManualLibLoader {

    private static final Logger LOG = (Logger) LoggerFactory.getLogger(DockerPlacementServiceTest.class);


    /**
     * List of native libraries you put in src/main/resources
     */
    public static final String[] NATIVE_LIB_FILENAMES = {
            "natives/libcplex1260.so",
            "natives/libcplex1260_x64.so",
            "natives/liblpsolve55.so",
            "natives/liblpsolve55j.so",
            "natives/liblpsolve55j_x64.so",
    };

    /**
     * Extract native libraries to the current directory.
     * This example needs Apache Commons IO (https://commons.apache.org/proper/commons-io/)
     */
    public static void extractNativeResources() {
        for (String filename : NATIVE_LIB_FILENAMES) {
            final InputStream in = ManualLibLoader.class.getResourceAsStream(filename);

            if (in != null) {
                try {
                    LOG.info("Extracting " + filename);
                    File destination = File.createTempFile(filename, ".so");
                    FileUtils.copyInputStreamToFile(in, destination);
                    System.load(destination.getAbsolutePath());
                } catch (IOException e) {
                    LOG.error("Can't extract " + filename);
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Delete native libraries in the current directory
     */
    public static void removeNativeResources() {
        for (String filename : NATIVE_LIB_FILENAMES) {
            File file = new File(filename);
            file.delete();
        }
    }

    public static void main(String[] args) throws Exception {
        boolean deleteNativesOnExit;    // Delete natives on exit

        extractNativeResources();
        deleteNativesOnExit = true;


        try {
            SolverFactory factory = new SolverFactoryCPLEX();
            factory.setParameter(Solver.VERBOSE, 0);
            factory.setParameter(Solver.TIMEOUT, 100); // set timeout to 100 seconds
            Problem problem = new Problem();

            Linear linear = new Linear();
            linear.add(5, "x");
            linear.add(10, "y");

            problem.setObjective(linear, OptType.MIN);

            linear = new Linear();
            linear.add(3, "x");
            linear.add(1, "y");

            problem.add(linear, ">=", 8);

            linear = new Linear();
            linear.add(4, "y");

            problem.add(linear, ">=", 4);

            linear = new Linear();
            linear.add(2, "x");

            problem.add(linear, "<=", 2);
            Solver solver = factory.get();
            Result result = solver.solve(problem);
            Number objective = result.getObjective();
            Number x = result.get("x");
            Number y = result.get("y");
            LOG.info(objective.doubleValue() + " is(55.0)");
            LOG.info(x.doubleValue() + "is(1.0)");
            LOG.info(y.doubleValue() + "is(5.0)");
            LOG.info("Success");
        } catch (Exception e) {
            LOG.error("FAILED", e);
        }

        // Delete the native libraries we extracted
        if (deleteNativesOnExit) {
            removeNativeResources();
        }
    }
}
