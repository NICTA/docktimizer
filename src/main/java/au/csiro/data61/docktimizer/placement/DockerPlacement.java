package au.csiro.data61.docktimizer.placement;

import au.csiro.data61.docktimizer.controller.ControllerHandler;
import au.csiro.data61.docktimizer.controller.MysqlDatabaseController;
import au.csiro.data61.docktimizer.helper.MILPSolver;
import au.csiro.data61.docktimizer.models.DockerContainer;
import au.csiro.data61.docktimizer.models.VMType;
import au.csiro.data61.docktimizer.models.VirtualMachine;
import net.sf.javailp.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 */
public class DockerPlacement {

    private static final Logger LOG = (Logger) LoggerFactory.getLogger(DockerPlacement.class);


    private static final double OMEGA_F_C_VALUE = 0.0001;
    private static final double OMEGA_DEPLOY_D_VALUE = 0.01;
    private static final long CONSTANT_M = 10000;
    private static final long BTU = 60 * 60 * 1000;
    private static DockerPlacement instance;

    private Map<DockerContainer, List<DockerContainer>> dockerMap;
    private Map<VMType, List<VirtualMachine>> vmMap;

    private ControllerHandler controllerHandler;
    private long TAU_T;
    private Map<DockerContainer, Integer> invocationMap;


    private DockerPlacement() {
    }

    public static DockerPlacement getInstance() {
        if (instance == null) {
            instance = new DockerPlacement();
        }
        return instance;
    }

    public Result solvePlacement(long tau_t) {
        TAU_T = tau_t;

        initialize(TAU_T);

        Problem problem = generateProblem();

        //whoop whoop
        MILPSolver milpSolver = new MILPSolver();
        Result result = milpSolver.solveProblem(problem);


        if (result == null) {
            LOG.info("\n-----------------------------\n");
            Collection<Object> variables = problem.getVariables();
            int i = 0;
            for (Object variable : variables) {
                LOG.info(i + ": " + variable);
                i++;
            }


            LOG.info("\n-----------------------------\n");
            LOG.info(problem.getConstraints().toString());
            LOG.info("\n-----------------------------\n");
            LOG.info(problem.getObjective().toString());
        }
        return result;
    }

    private Problem generateProblem() {

        Problem problem = new Problem();

        //insert minimization problem here
        addObjective(problem);

        //add constraints
        addConstraint_2(problem);
        addConstraint_3(problem);
        addConstraint_4(problem);
        addConstraint_5(problem);
        addConstraint_6(problem);
        addConstraint_7(problem);
        addConstraint_8(problem);
        addConstraint_9(problem);
        addConstraint_10(problem);
        addConstraint_11(problem);
        addConstraint_12(problem);
        addConstraint_13(problem);
        addConstraint_14(problem);
        addConstraint_15(problem);
        addConstraint_16(problem);
        addConstraint_17(problem);
        switch (MysqlDatabaseController.BASELINE_TYPE) {
            case MysqlDatabaseController.ONE_ALL:
                addConstrainOneForAll(problem);
                break;
            case MysqlDatabaseController.ONE_EACH:
                addConstrainOneEach(problem);
                break;
            default:
                break;
        }
        return problem;
    }

    //makes only quad cores leasable
    private void addConstrainOneForAll(Problem problem) {
        for (VMType vmType : vmMap.keySet()) {
            for (VirtualMachine vm : vmMap.get(vmType)) {
                if (vm.getVmType().cores != 4) {
                    String decisionVariableY = getDecisionVariableY(vm);
                    Linear linear = new Linear();
                    linear.add(1, decisionVariableY);
                    problem.add(linear, "=", 0);
                }
            }
        }
    }

    private void addConstrainOneEach(Problem problem) {
        for (VMType vmType : vmMap.keySet()) {
            for (VirtualMachine vm : vmMap.get(vmType)) {
                Linear linear = new Linear();
                for (DockerContainer dockerContainerType : dockerMap.keySet()) {
                    for (DockerContainer dockerContainer : dockerMap.get(dockerContainerType)) {
                        String decisionVariableX = getDecisionVariableX(dockerContainer, vm);
                        linear.add(1, decisionVariableX);
                    }
                }
                if (vm.getVmType().cores != 1) {
                    String decisionVariableY = getDecisionVariableY(vm);
                    Linear linear2 = new Linear();
                    linear2.add(1, decisionVariableY);
                    problem.add(linear2, "=", 0);
                }
                problem.add(linear, Operator.LE, 1);
            }
        }
    }

    private void initialize(long tau_t) {
        LOG.info("initialize and update");
        controllerHandler = ControllerHandler.getInstance();
        controllerHandler.initializeAndUpdate(tau_t);

        invocationMap = new HashMap<>();
        vmMap = controllerHandler.getVMMap(false);
        dockerMap = controllerHandler.getDockerMap();
        for (DockerContainer dockerContainer : dockerMap.keySet()) {
            int invocations = controllerHandler.getInvocations(dockerContainer);
            invocationMap.put(dockerContainer, invocations);
        }
    }

    private void addObjective(Problem problem) {
        final Linear linear = new Linear();
        //term 1
        for (VMType vmType : vmMap.keySet()) {
            String gamma = getGammaVariable(vmType);
            linear.add(vmType.price, gamma);
        }

        //term 2
        for (VMType vmType : vmMap.keySet()) {
            for (VirtualMachine vm : vmMap.get(vmType)) {
                for (DockerContainer dockerContainerType : dockerMap.keySet()) {
                    for (DockerContainer dockerContainer : dockerMap.get(dockerContainerType)) {
                        if (!vm.isDockerDeployed(dockerContainer)) {
                            String decisionVariableX = getDecisionVariableX(dockerContainer, vm);
                            linear.add(dockerContainerType.getDeployCost() * OMEGA_DEPLOY_D_VALUE
                                    , decisionVariableX);
                        }
                    }
                }
            }
        }

        //term 3
        for (VMType vmType : vmMap.keySet()) {
            for (VirtualMachine vm : vmMap.get(vmType)) {

                String fValueC = getHelperVariableF(vm);
                linear.add(OMEGA_F_C_VALUE, fValueC);
                problem.setVarUpperBound(fValueC, Double.MAX_VALUE);
                problem.setVarLowerBound(fValueC, Double.MIN_VALUE);
            }
        }

        //term 4
        for (VMType vmType : vmMap.keySet()) {
            for (VirtualMachine vm : vmMap.get(vmType)) {

                for (DockerContainer dockerContainerType : dockerMap.keySet()) {
                    for (DockerContainer dockerContainer : dockerMap.get(dockerContainerType)) {
                        String decisionVariableX = getDecisionVariableX(dockerContainer, vm);
                        double sICDT = 1;
                        linear.add(sICDT, decisionVariableX);
                    }
                }

            }
        }

        problem.setObjective(linear, OptType.MIN);
    }

    /**
     * @param problem adds constraint for resource requirement per docker container and VM
     */
    private void addConstraint_2(Problem problem) {

        for (VMType vmType : vmMap.keySet()) {
            for (VirtualMachine vm : vmMap.get(vmType)) {
                Linear linear = new Linear();
                for (DockerContainer dockerContainerType : dockerMap.keySet()) {
                    for (DockerContainer dockerContainer : dockerMap.get(dockerContainerType)) {
                        String decisionVariableX = getDecisionVariableX(dockerContainer, vm);
                        double requiredCPUCores = dockerContainer.getContainerConfiguration().cores;
                        linear.add(requiredCPUCores, decisionVariableX);
                    }
                }
                linear.add(-vm.getVmType().cores, getHelperVariableG(vm));
                problem.add(linear, "<=", 0);

            }
        }
    }

    /**
     * @param problem adds constraint for computing the free resources for a VM (or wasted resource)
     */
    private void addConstraint_3(Problem problem) {

        for (VMType vmType : vmMap.keySet()) {
            for (VirtualMachine vm : vmMap.get(vmType)) {
                Linear linear = new Linear();
                String helperVariableG = getHelperVariableG(vm);
                double sCPUVM = vm.getVmType().cores;
                linear.add(sCPUVM, helperVariableG);

                for (DockerContainer dockerContainerType : dockerMap.keySet()) {
                    for (DockerContainer dockerContainer : dockerMap.get(dockerContainerType)) {
                        String decisionVariableX = getDecisionVariableX(dockerContainer, vm);
                        linear.add(-1 * dockerContainer.getContainerConfiguration().cores, decisionVariableX);
                    }
                }
                String helperVariableF = getHelperVariableF(vm);
                linear.add(-1, helperVariableF);
                problem.add(linear, "<=", 0);
            }
        }
    }

    /**
     * @param problem adds the constraint to compute how many
     *                containers are needed for a particular container type
     */
    private void addConstraint_4(Problem problem) {
        for (DockerContainer dockerContainerType : dockerMap.keySet()) {
            Linear linear = new Linear();
            for (DockerContainer dockerContainer : dockerMap.get(dockerContainerType)) {
                for (VMType vmType : vmMap.keySet()) {
                    for (VirtualMachine vm : vmMap.get(vmType)) {
                        String decisionVariableX = getDecisionVariableX(dockerContainer, vm);
                        double sICDT = dockerContainer.getAmountOfPossibleInvocations();
                        linear.add(sICDT, decisionVariableX);
                    }
                }
            }
            problem.add(linear, ">=", invocationMap.get(dockerContainerType));
        }
    }

    /**
     * @param problem adds the constraint that on each VM only 1 specific container of a
     *                container type should be placed at the same time the most, but can be 0 aswell
     */
    private void addConstraint_5(Problem problem) {
        for (DockerContainer dockerContainerType : dockerMap.keySet()) {

            for (VMType vmType : vmMap.keySet()) {
                for (VirtualMachine vm : vmMap.get(vmType)) {
                    Linear linear = new Linear();
                    for (DockerContainer dockerContainer : dockerMap.get(dockerContainerType)) {

                        String decisionVariableX = getDecisionVariableX(dockerContainer, vm);
                        linear.add(1, decisionVariableX);
                    }
                    problem.add(linear, "<=", 1);
                }
            }
        }
    }

    /**
     * @param problem adds constraint to sum up the amount of VMs to be leased
     */

    private void addConstraint_6(Problem problem) {
        for (VMType vmType : vmMap.keySet()) {
            Linear linear = new Linear();
            String gamma = getGammaVariable(vmType);
            for (VirtualMachine vm : vmMap.get(vmType)) {
                String variableY = getDecisionVariableY(vm);
                linear.add(1, variableY);
            }
            linear.add(-1, gamma);
            problem.add(linear, "<=", 0);
        }

    }

    /**
     * @param problem adds constraint to g >= y
     */
    private void addConstraint_7(Problem problem) {
        for (VMType vmType : vmMap.keySet()) {
            for (VirtualMachine vm : vmMap.get(vmType)) {
                String helperVariableG = getHelperVariableG(vm);
                String decisionVariableY = getDecisionVariableY(vm);
                Linear linear = new Linear();
                linear.add(1, helperVariableG);
                linear.add(-1, decisionVariableY);
                problem.add(linear, ">=", 0);
            }
        }
    }

    /**
     * @param problem adds constraint to g >= betta
     */
    private void addConstraint_8(Problem problem) {
        for (VMType vmType : vmMap.keySet()) {
            for (VirtualMachine vm : vmMap.get(vmType)) {
                String helperVariableG = getHelperVariableG(vm);
                Linear linear = new Linear();
                linear.add(1, helperVariableG);
                problem.add(linear, ">=", vm.isRunning() ? 1 : 0);
            }
        }
    }

    private void addConstraint_9(Problem problem) {
        for (VMType vmType : vmMap.keySet()) {
            for (VirtualMachine vm : vmMap.get(vmType)) {
                String helperVariableG = getHelperVariableG(vm);
                String decisionVariableY = getDecisionVariableY(vm);
                Linear linear = new Linear();
                linear.add(1, helperVariableG);
                linear.add(-1, decisionVariableY);
                problem.add(linear, "<=", vm.isRunning() ? 1 : 0);
            }
        }
    }

    private void addConstraint_10(Problem problem) {
        for (DockerContainer dockerContainerType : dockerMap.keySet()) {

            for (VMType vmType : vmMap.keySet()) {
                for (VirtualMachine vm : vmMap.get(vmType)) {
                    Linear linear = new Linear();

                    for (DockerContainer dockerContainer : dockerMap.get(dockerContainerType)) {
                        String decisionVariableX = getDecisionVariableX(dockerContainer, vm);
                        linear.add(1, decisionVariableX);
                    }
                    String helperVariableG = getHelperVariableG(vm);
                    linear.add(-CONSTANT_M, helperVariableG);
                    problem.add(linear, "<=", 0);
                }
            }
        }
    }

    /**
     * @param problem adds the constraint which computes the remaining leasing duration
     *                needs to be long enough, or the vm needs to be leased another round
     */
    private void addConstraint_11(Problem problem) {
        for (VMType vmType : vmMap.keySet()) {
            for (VirtualMachine vm : vmMap.get(vmType)) {
                for (DockerContainer dockerContainerType : dockerMap.keySet()) {
                    for (DockerContainer dockerContainer : dockerMap.get(dockerContainerType)) {
                        String decisionVariableX = getDecisionVariableX(dockerContainer, vm);
                        Linear linear = new Linear();
                        String decisionVariableY = getDecisionVariableY(vm);
                        linear.add(vm.getVmType().leasingDuration, decisionVariableY);
                        long remaining = 0;
                        if (vm.isRunning()) {
                            remaining = vm.getToBeTerminatedAt() - TAU_T;
                            if (remaining < 0) {
                                remaining = 1;
                            }
                        }
                        linear.add(remaining, decisionVariableX);

                        linear.add(-30000L, decisionVariableX);
                        problem.add(linear, ">=", 0);
                    }
                }
            }
        }
    }

    /**
     * defines the limits for decision variable X
     *
     * @param problem to add the variable
     */

    private void addConstraint_12(Problem problem) {
        for (VMType vmType : vmMap.keySet()) {
            for (VirtualMachine vm : vmMap.get(vmType)) {
                for (DockerContainer dockerContainerType : dockerMap.keySet()) {
                    for (DockerContainer dockerContainer : dockerMap.get(dockerContainerType)) {
                        String variableX = getDecisionVariableX(dockerContainer, vm);
                        Linear linear = new Linear();
                        linear.add(1, variableX);
                        problem.add(linear, ">=", 0);
                        problem.add(linear, "<=", 1);
                        problem.setVarType(variableX, VarType.INT);
                    }
                }
            }
        }
    }

    /**
     * defines the limits for decision variable Y
     *
     * @param problem to add the variable
     */
    private void addConstraint_13(Problem problem) {
        for (VMType vmType : vmMap.keySet()) {
            for (VirtualMachine vm : vmMap.get(vmType)) {
                String variableY = getDecisionVariableY(vm);
                Linear linear = new Linear();
                linear.add(1, variableY);
                problem.add(linear, ">=", 0);
                problem.add(linear, "<=", 1);
                problem.setVarType(variableY, VarType.INT);
            }
        }
    }

    /**
     * defines the limits for helper variable g
     *
     * @param problem to add the variable
     */
    private void addConstraint_14(Problem problem) {
        for (VMType vmType : vmMap.keySet()) {
            for (VirtualMachine vm : vmMap.get(vmType)) {
                String variableY = getHelperVariableG(vm);
                Linear linear = new Linear();
                linear.add(1, variableY);
                problem.add(linear, ">=", 0);
                problem.add(linear, "<=", 1);
                problem.setVarType(variableY, VarType.INT);
            }
        }
    }

    /**
     * defines the limits for helper variable betta
     *
     * @param problem to add the variable
     */
    private void addConstraint_15(Problem problem) {
        // this is just a placeholder, the real value will be used directly
    }

    /**
     * @param problem the problem to add the
     *                amount of incoming requests for a particular container
     *                at a certain point of time
     */
    private void addConstraint_16(Problem problem) {

        for (DockerContainer dockerContainerType : dockerMap.keySet()) {
            int i_d = invocationMap.get(dockerContainerType);

            Linear linear = new Linear();
            String helperVariableI = getHelperVariableI(dockerContainerType);
            linear.add(1, helperVariableI);
            problem.add(linear, "=", i_d);
        }
    }

    /**
     * @param problem to add the variable for how many invocations are possible at a certain docker container
     */
    private void addConstraint_17(Problem problem) {
        //this is just a placeholder
    }


    /**
     * *************** Variable Methods *************************
     */


    /**
     * decision variable defining if a VM has to be leased or not
     *
     * @param vm defines the virtual machine
     * @return a string presentation
     */
    public String getDecisionVariableY(VirtualMachine vm) {
        return "y_(" + vm.getName() + ",t)";
    }

    /**
     * @param dockerContainer defines the wanted docker container including configuration
     * @param vm              defines the target vm
     * @return decision variable x for a container configuration and vm
     */
    public String getDecisionVariableX(DockerContainer dockerContainer, VirtualMachine vm) {
        return "x_(" + dockerContainer.getName() + "," + vm.getName() + ",t)";
    }

    /**
     * helper variable defining if a VM has to be leased or is already running
     *
     * @param vm defines the virtual machine
     * @return a string presentation
     */
    private String getHelperVariableG(VirtualMachine vm) {
        return "g_(" + vm.getName() + ",t)";
    }

    /**
     * @param dockerContainer specifies the target container
     * @return variable name for amount of invocation
     */
    @Deprecated
    private String getHelperVariableI(DockerContainer dockerContainer) {
        return "i_(" + dockerContainer.getAppID() + ",t)";
    }


    /**
     * @param vm defines a specific VM
     * @return the variable for free (wasted) resources in terms of CPU at a point of time
     */
    private String getHelperVariableF(VirtualMachine vm) {
        return "f_(CPU," + vm.getName() + ",t)";
    }

    /**
     * gamma is the amount of all leased VMs
     *
     * @param vmType defines specific VMType
     * @return gamma variable for a particular VM Type
     */
    private String getGammaVariable(VMType vmType) {
        return "gamma_" + vmType.getReadableString();
    }


    /**
     * decision variable defining if a VM has to be leased or not
     *
     * @param vm defines the virtual machine
     * @return a string presentation
     */
    public String getHelperVariableBeta(VirtualMachine vm) {
        return "beta_(" + vm.getName() + ",t)";
    }

    public void setControllerHandler(ControllerHandler controllerHandler) {
        this.controllerHandler = controllerHandler;
    }
}
