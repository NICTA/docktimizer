package au.csiro.data61.docktimizer.placement;

import au.csiro.data61.docktimizer.helper.MILPSolver;
import net.sf.javailp.Linear;
import net.sf.javailp.OptType;
import net.sf.javailp.Problem;
import net.sf.javailp.Result;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 */
public class DockerPlacementTest {

    /**
     * Min 5 x + 10 y
     * Subject to
     * 3 x + 1 y >= 8
     * 4y >= 4
     * 2x <= 2
     */
    @Test
    public void test1() {
        MILPSolver placement = new MILPSolver();
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

        Result result = placement.solveProblem(problem);
        Number objective = result.getObjective();
        Number x = result.get("x");
        Number y = result.get("y");
        assertThat(objective.doubleValue(), is(55.0));
        assertThat(x.doubleValue(), is(1.0));
        assertThat(y.doubleValue(), is(5.0));
    }

}
