package au.csiro.data61.docktimizer.controller;

import au.csiro.data61.docktimizer.helper.VMTypeComparator;
import au.csiro.data61.docktimizer.hibernate.EntityManagerHelper;
import au.csiro.data61.docktimizer.interfaces.DatabaseController;
import au.csiro.data61.docktimizer.models.*;
import au.csiro.data61.docktimizer.placement.DockerPlacementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 */
public class MysqlDatabaseController implements DatabaseController {
    private static final Logger LOG = (Logger) LoggerFactory.getLogger(MysqlDatabaseController.class);

    private static MysqlDatabaseController instance;

    public static final String ONE_ALL = "1ALL";
    public static final String DEFAULT = "DEFAULT";
    public static final String ONE_EACH = "1EACH";

    //Chose which evaluation you are runnging...
    //    public static String BASELINE_TYPE = "DEFAULT";
    //    public static String BASELINE_TYPE = "1EACH";
    public static String BASELINE_TYPE = ONE_EACH;

    protected static Integer V = 3; // type = cores
    protected static Integer K = 4; // how many of each core
    protected static Integer D = 3; // how many docker types
    protected static Integer C = 4; // different configurations of each docker type
    private EntityManagerHelper entityManager;

    private SortedMap<VMType, List<VirtualMachine>> vmMap;
    private Map<DockerContainer, List<DockerContainer>> dockerMap;
    private Map<String, Integer> invocationMap;
    private List<DockerImage> imageList;
//    private static Session session;

    private MysqlDatabaseController() {
        entityManager = EntityManagerHelper.getInstance();
        switch (BASELINE_TYPE) {
            case ONE_EACH:
                V = 3;
                K = 12;
                D = 3;
                C = 4;
                return;
            case ONE_ALL:
                V = 3;
                K = 4;
                D = 3;
                C = 4;
                return;
            default:
                V = 3;
                K = 4;
                D = 3;
                C = 4;
                break;
        }
    }

    public static MysqlDatabaseController getInstance() {
        if (instance == null) {
            instance = new MysqlDatabaseController();
        }
        return instance;
    }

    @Override
    public synchronized void initializeAndUpdate(long tau_t) {

        entityManager.getEntityManager().getTransaction().begin();


        vmMap = new TreeMap<>(new VMTypeComparator());
        dockerMap = new HashMap<>();
        invocationMap = new HashMap<>();
        imageList = new ArrayList<>();


        //setup list of VMs
        List<VirtualMachine> virtualMachineList = updateVMMap();
        if (virtualMachineList.isEmpty()) {
            initializeVirtualMachines();
        }
        LOG.info("Loading images...");
        List<DockerImage> tmpImageList = entityManager.getEntityManager().createQuery("From DockerImage ").getResultList();
        if (tmpImageList.isEmpty()) {
            initializeDockerImages();
        } else {
            this.imageList = tmpImageList;
        }
        LOG.info("Found images " + imageList.size());


        //setup DockerContainers
        LOG.info("Loading docker container types...");
        List<DockerContainer> containerList = updateDockerMap();
        if (containerList.isEmpty()) {
            initializeDockerContainer();
        }
        LOG.info("Found docker container types " + dockerMap.keySet().size());


        LOG.info("Loading planned invocations ...");
        long startTime = tau_t - 60 * 1000;
        long endTime = tau_t + DockerPlacementService.SLEEP_TIME;
        List<PlannedInvocation> plannedInvocations = entityManager.getEntityManager().createQuery("FROM PlannedInvocation AS pi WHERE pi.done=:done")
                .setParameter("done", false)
                .getResultList();

        LOG.info("Found planned invocations ..." + plannedInvocations.size());
        for (PlannedInvocation plannedInvocation : plannedInvocations) {
            Integer amount = invocationMap.get(plannedInvocation.getAppId());

            if (amount == null) {
                amount = 0;
            }
            amount += plannedInvocation.getAmount();
            invocationMap.put(plannedInvocation.getAppId(), amount);
            LOG.info("Found planned invocations " + plannedInvocation.getAppId() + " amount: " +
                    plannedInvocation.getAmount());
        }
        entityManager.getEntityManager().getTransaction().commit();
    }

    private List<DockerContainer> updateDockerMap() {
        List<DockerContainer> containerList = entityManager.getEntityManager().createQuery("From DockerContainer ").getResultList();
        for (DockerContainer dockerContainer : containerList) {
            List<DockerContainer> tmp = dockerMap.get(dockerContainer);
            if (tmp == null) {
                tmp = new ArrayList<>();
            }

            tmp.add(dockerContainer);
            dockerMap.put(dockerContainer, tmp);
        }
        return containerList;
    }

    private List<VirtualMachine> updateVMMap() {

        vmMap = new TreeMap<>();

        List<VirtualMachine> virtualMachineList = entityManager.getEntityManager().createQuery("From VirtualMachine ")
                .getResultList();
        for (VirtualMachine next : virtualMachineList) {
            List<VirtualMachine> tmp = vmMap.get(next.getVmType());
            if (tmp == null) {
                tmp = new ArrayList<>();
            }
            switch (BASELINE_TYPE) {
                case ONE_EACH:
                    //only one VM per container, this means we only use dual cores
                    if (next.getVmType().cores != VMType.M1_SMALL.cores) {
                        continue;
                    }
                    break;
                default:
                    break;
            }

            if (!tmp.contains(next)) {
                tmp.add(next);
            }
            vmMap.put(next.getVmType(), tmp);
        }
        return virtualMachineList;
    }

    private void initializeDockerImages() {
        for (int c = 0; c < D; c++) {
            DockerImage dockerImage = parseByAppId("app" + c);
            entityManager.getEntityManager().persist(parseByAppId("app" + c));
            imageList.add(dockerImage);
        }
    }

    public static DockerImage parseByImageName(String imageFullName) {
        if (imageFullName.contains("bonomat")) {
            return new DockerImage("app" + 0, "bonomat", "nodejs-hello-world", 8090, 3000);
        }
        if (imageFullName.contains("kaihofstetter")) {
            return new DockerImage("app" + 1, "gjong", "apache-joomla", 8091, 80);

        }
        if (imageFullName.contains("gjong")) {
            return new DockerImage("app" + 2, "bonomat", "zero-to-wordpress-sqlite", 8082, 80);
        }
        if (imageFullName.contains("xhoussemx")) {
            return new DockerImage("app" + 3, "bonomat", "nodejs-hello-world", 8092, 3000);
        }
        return null;
    }


    public static DockerImage parseByAppId(String appId) {
        if (appId.contains("app0")) {
            return parseByImageName("bonomat");
        }
        if (appId.contains("app1")) {
            return parseByImageName("kaihofstetter");
        }
        if (appId.contains("app2")) {
            return parseByImageName("gjong");
        }
        if (appId.contains("app3")) {
            return parseByImageName("xhoussemx");
        }
        return parseByImageName("bonomat");
    }

    private void initializeDockerContainer() {


        for (DockerImage dockerImage : imageList) {
            List<DockerContainer> dockerList = new ArrayList<>();
            for (int j = 0; j < C; j++) {
                DockerConfiguration configuration = null;
                switch (j) {
                    case 0:
                        configuration = DockerConfiguration.SINGLE_CORE;
                        break;
                    case 1:
                        configuration = DockerConfiguration.DUAL_CORE;
                        break;
                    case 2:
                        configuration = DockerConfiguration.QUAD_CORE;
                        break;
                    case 3:
                        configuration = DockerConfiguration.HEXA_CORE;
                        break;

                }

                DockerContainer container = new DockerContainer(dockerImage, configuration);
                entityManager.getEntityManager().persist(container);
                dockerList.add(container);
            }
            dockerMap.put(dockerList.get(0), dockerList);
        }
    }

    private void initializeVirtualMachines() {
        for (int type = 0; type < V; type++) {
            List<VirtualMachine> vmList = new ArrayList<>();
            VMType vmType = VMType.M1_MICRO;
            for (int j = 0; j < K; j++) {
                VirtualMachine vm;
                switch (type) {
                    case 0:
                        vmType = VMType.M1_SMALL;
                        break;
                    case 1:
                        vmType = VMType.M1_MEDIUM;
                        break;
                    case 2:
                        vmType = VMType.M1_LARGE;
                        break;
                    default:
                        vmType = VMType.M1_SMALL;
                        break;
                }
                vm = new VirtualMachine("k" + j + "v" + vmType.cores, vmType, j);
                vmList.add(vm);
                entityManager.getEntityManager().persist(vm);
            }
            vmMap.put(vmType, vmList);
        }
    }

    @Override
    public Map<DockerContainer, List<DockerContainer>> getDockerMap() {
        return dockerMap;
    }

    @Override
    public SortedMap<VMType, List<VirtualMachine>> getVmMap(boolean update) {
        if (update) {
            updateVMMap();
        }
        return vmMap;
    }

    @Override
    public int getInvocations(DockerContainer dockerContainerType) {
        Integer amount = invocationMap.get(dockerContainerType.getAppID());
        return amount == null ? 0 : amount;
    }

    @Override
    public void save(PlannedInvocation plannedInvocation) {
        entityManager.getEntityManager().getTransaction().begin();
        entityManager.getEntityManager().persist(plannedInvocation);
        entityManager.getEntityManager().getTransaction().commit();
    }

    @Override
    public synchronized void update(VirtualMachine virtualMachine) {
        entityManager.getEntityManager().getTransaction().begin();
        virtualMachine = entityManager.getEntityManager().merge(virtualMachine);
        entityManager.getEntityManager().getTransaction().commit();
    }

    @Override
    public List<PlannedInvocation> getInvocations() {
        List done = entityManager.getEntityManager().createQuery("FROM PlannedInvocation AS pi WHERE pi.done=:done")
                .setParameter("done", false)
                .getResultList();
        return done;
    }

    @Override
    public DockerContainer getDocker(String appID) {
        DockerContainer dockerContainer = (DockerContainer) entityManager.getEntityManager().createQuery("" +
                "FROM DockerContainer AS d WHERE d.dockerImage.appId=:appID")
                .setParameter("appID", appID).getSingleResult();
        return dockerContainer;
    }

    @Override
    public void close() {
        entityManager.getEntityManager().close();
    }


    @Override
    public void restart() {
        switch (BASELINE_TYPE) {
            case "1EACH":
                V = 1;
                K = 12;
                D = 3;
                C = 4;
                return;
            case "1ALL":
                V = 1;
                K = 4;
                D = 3;
                C = 4;
                return;
            default:
                V = 4;
                K = 4;
                D = 3;
                C = 4;
                break;
        }
    }
}
