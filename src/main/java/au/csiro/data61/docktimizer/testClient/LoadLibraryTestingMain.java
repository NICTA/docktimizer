package au.csiro.data61.docktimizer.testClient;

import au.csiro.data61.docktimizer.helper.MILPSolver;
import net.sf.javailp.Linear;
import net.sf.javailp.OptType;
import net.sf.javailp.Problem;
import net.sf.javailp.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class LoadLibraryTestingMain {

    private static final Logger LOG = (Logger) LoggerFactory.getLogger(LoadLibraryTestingMain.class);


    public static void main(String[] args) throws Exception {


        try {
            MILPSolver milpSolver = new MILPSolver();
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

            Result result = milpSolver.solveProblem(problem);
            Number objective = result.getObjective();
            Number x = result.get("x");
            Number y = result.get("y");
            LOG.info(objective.doubleValue() + " is(55.0)");
            LOG.info(x.doubleValue() + "is(1.0)");
            LOG.info(y.doubleValue() + "is(5.0)");
            LOG.info("Success");
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("FAILED");
        }

    }
}
