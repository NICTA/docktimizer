package au.csiro.data61.docktimizer.helper;

import net.sf.javailp.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class MILPSolver extends NativeLibraryLoader {

    private static final Logger LOG = (Logger) LoggerFactory.getLogger(MILPSolver.class);

    /**
     *
     * solves the given problem and returns the result
     *
     * @param problem to be solved
     * @return the result of the problem
     */
    public Result solveProblem(Problem problem)  {

        SolverFactory factory;
        if (useCPLEX) {
            LOG.info("USING CPLEX-------------------------------");
            factory = new SolverFactoryCPLEX(); // use lp_solve
        } else {
            LOG.info("USING LPSolve -------------------------------");
            factory = new SolverFactoryLpSolve(); // use lp_solve
        }
        factory.setParameter(Solver.VERBOSE, 0);
        factory.setParameter(Solver.TIMEOUT, 100); // set timeout to 100 seconds

        try {
            Solver solver = factory.get();
            return solver.solve(problem);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
