package au.csiro.data61.docktimizer.testClient;

import au.csiro.data61.docktimizer.controller.MysqlDatabaseController;
import au.csiro.data61.docktimizer.models.*;
import com.google.gson.Gson;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by Philipp Hoenisch on 4/21/15.
 */
public class TestClient implements Runnable {
    private static final Logger LOG = (Logger) LoggerFactory.getLogger(TestClient.class);
    //                private static final String DOCKER_PLACEMENT_SERVER = "http://localhost:8088";
    private static final String DOCKER_PLACEMENT_SERVER = "http://128.130.172.209:8088";
    private static final String DOCKER_PLACEMENT_SERVER_ADD_INVOCATION = DOCKER_PLACEMENT_SERVER + "/addInvocations";
    private static final String DOCKER_PLACEMENT_SERVER_RUN_OPTIMIZATION = DOCKER_PLACEMENT_SERVER + "/api";
    private static final String DOCKER_PLACEMENT_SYSTEM_STATE = DOCKER_PLACEMENT_SERVER + "/state";
    private static final String DOCKER_PLACEMENT_RESTART = DOCKER_PLACEMENT_SERVER + "/restart";
    private static final String PROXY_IP = "http://128.130.172.209:8082";
    private static final int MAX_ROUNDS = 40;
    private static boolean RUNNING = true;

    public static void main(String[] args) throws Exception {
        LOG.info("Test Client started");
        Scanner scanner = new Scanner(System.in);
        LOG.info("Enter 'start' to Start");
        LOG.info("Enter 'stop' to stop");

        int app1 = 190;
        int app0 = 0;
        int app2 = 50;

        for (int k = 0; k < MAX_ROUNDS; k++) {
            if (app1 > 100 && k <= MAX_ROUNDS / 2 && k % 2 == 0) {
                app1 -= 10;
            }
            if (k >= MAX_ROUNDS * 0.75 && app1 > 40) {
                app1 -= 40;
            }

            if (k % 2 == 0 && k < 9) {
                app0 += 20;
            } else if (k % 4 == 0) {
                app0 += 15;
            }
            if (app0 >= 200) {
                app0 = 200;
            }

            LOG.info(k + ";" + app0 + ";" + app1 + ";" + app2);
        }


        Thread thread = new Thread();
        String input = "";
        while (!input.equalsIgnoreCase("stop")) {
            input = scanner.nextLine();
            if (input.equalsIgnoreCase("start")) {
                thread = new Thread(new TestClient());
                thread.start();
            }
        }
        LOG.info("stop caught-----------------------------");
        RUNNING = false;
        thread.join();
        System.exit(1);
//        updatePlacement(getInvocations());
//        updatePlacement(new String[]{
//                "app2", "app1", "app0"});
//
//        staticAndIncreseEvaluation("app0", "app1");

    }

    public void run() {
        List<String> files = new ArrayList<>();
        try {

            PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
            cm.setMaxTotal(1000);
            cm.setDefaultMaxPerRoute(1000);


            CloseableHttpClient httpClient = HttpClients.custom()
                    .setConnectionManager(cm).setMaxConnTotal(10000) // shared connection manager
                    .setConnectionManagerShared(false).build();

            int app1 = 80;

            for (int run = 0; run < 3 && RUNNING; run++) {
                final long realStart = System.currentTimeMillis();


                int app0 = 0;

                StringBuilder systemStates = new StringBuilder("");
                StringBuilder invocationAmount = new StringBuilder("");
                StringBuilder responseApp0 = new StringBuilder("");
                StringBuilder responseApp1 = new StringBuilder("");
                StringBuilder responseApp2 = new StringBuilder("");
                StringBuilder responseAppUnited = new StringBuilder("");


                AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
                for (int k = 0; k < MAX_ROUNDS && RUNNING; k++) {
                    final long startMoment = System.currentTimeMillis() - realStart;
                    String systemstateString = getSystemState(startMoment);
                    LOG.info(k + ";" + startMoment + ";" + systemstateString);
                    systemStates.append(k)
                            .append(";").append(startMoment)
                            .append(";").append(systemstateString).append("\n");

                    app0 += 5;
                    if ((k < MAX_ROUNDS / 2)) {
                        app1 += 5;
                    } else {
                        app1 -= 5;
                    }

                    int app2 = 50;

                    StringBuilder tmp = new StringBuilder("")
                            .append(k)
                            .append(";").append(startMoment)
                            .append(";").append(app0)
                            .append(";").append(app1)
                            .append(";").append(app2).append("\n");
                    LOG.info(tmp.toString());
                    invocationAmount.append(tmp);
                    String[] urisToGet = new String[app0 + app1 + app2];
                    {
                        int i = 0;
                        for (i = 0; i < app0; i++) {
                            urisToGet[i] = "http://128.130.172.209:8082/app0/";
                        }
                        i = 0;
                        for (i = 0; i < app1; i++) {
                            urisToGet[app0 + i] = "http://128.130.172.209:8082/app1/";
                        }
                        for (i = 0; i < app2; i++) {
                            urisToGet[app0 + app1 + i] = "http://128.130.172.209:8082/app2/";
                        }
                    }

                    // create a thread for each URI
                    GetThread[] threads = new GetThread[urisToGet.length];
                    List<Future<Object[]>> futures = new ArrayList<>();


                    int length = threads.length;

                    long waiting = (long) ((10.0 / threads.length) * 1000);
                    long firstShot = System.currentTimeMillis();
                    for (int i = 0; i < length; i++) {
                        final String uri = urisToGet[i];
                        HttpGet httpget = new HttpGet(uri);
                        final String replace = uri.replace(PROXY_IP, "").replace("/", "");
                        final long start = System.currentTimeMillis();
                        final int finalK = k;
                        Future<Object[]> f = asyncHttpClient.prepareGet(uri).setFollowRedirects(true).execute(
                                new AsyncCompletionHandler<Object[]>() {

                                    @Override
                                    public Object[] onCompleted(Response response) throws Exception {
                                        // Do something with the Response
                                        long stop = System.currentTimeMillis() - start;
                                        Object[] result = new Object[3];
                                        result[0] = stop;
                                        result[1] = replace;
                                        result[2] = response.getStatusCode();
                                        return result;
                                    }

                                    @Override
                                    public void onThrowable(Throwable t) {
                                        // Something wrong happened.
                                    }
                                });
                        futures.add(f);
                        Thread.sleep(waiting);

                    }
                    long last = System.currentTimeMillis();
                    LOG.info("first; " + firstShot + " last " + (last - firstShot));
                    for (Future<Object[]> future : futures) {
                        try {
                            Object[] response = future.get(10, TimeUnit.SECONDS);
                            String output = k + ";" + startMoment + ";" + response[0] + ";" + response[1] + ";" + response[2];
                            LOG.info(output);
                            String respTmp = "";
                            if (output.contains("app0")) {
                                responseApp0.append(output).append("\n");
                                respTmp = String.format("%s;%s;%s;%s;%s;%s;%s;%s", k, startMoment,
                                        response[0], response[2],
                                        "-1", "-1",
                                        "-1", "-1");
                            } else if (output.contains("app1")) {
                                responseApp1.append(output).append("\n");
                                respTmp = String.format("%s;%s;%s;%s;%s;%s;%s;%s", k, startMoment,
                                        "-1", "-1",
                                        response[0], response[2],
                                        "-1", "-1");
                            } else if (output.contains("app2")) {
                                responseApp2.append(output).append("\n");
                                respTmp = String.format("%s;%s;%s;%s;%s;%s;%s;%s", k, startMoment,
                                        "-1", "-1",
                                        "-1", "-1",
                                        response[0], response[2]);
                            }
                            responseAppUnited.append(respTmp).append("\n");
                        } catch (Exception e) {
                            //ignore
                        }

                    }

                    //wait for 5 seconds for next round
                    if (k % 2 == 0) {// each 5th round
                        List<PlannedInvocation> invocations = getInvocations(app0, app1, app2, 0);
                        updatePlacement(invocations);
                        getSystemState(startMoment);
                    }
                    if (k < 30) {
                        LOG.info("----------------------------------------------------");
                        LOG.info("next round " + k);
                        LOG.info("----------------------------------------------------");
                        Thread.sleep(30000L);
                    }
                }
                StringBuilder output = new StringBuilder("");
                output.append("----------------------------------------------------").append("\n");
                output.append("----------------------------------------------------").append("\n");
                output.append("----------------------------------------------------").append("\n");
                output.append("----------------------------------------------------").append("\n");
                output.append("Run ").append(run).append("STATS:").append("\n");
                output.append("----------------------------------------------------").append("\n");
                output.append("----------------------------------------------------").append("\n");
                output.append("----------------------------------------------------").append("\n");
                output.append("----------------------------------------------------").append("\n");
                output.append(invocationAmount.toString()).append("\n");
                output.append("----------------------------------------------------").append("\n");
                output.append("----------------------------------------------------").append("\n");
                output.append("----------------------------------------------------").append("\n");
                output.append("----------------------------------------------------").append("\n");
                output.append(systemStates.toString()).append("\n");
                output.append("----------------------------------------------------").append("\n");
                output.append("----------------------------------------------------").append("\n");
                output.append("----------------------------------------------------").append("\n");
                output.append("-------------------App0-----------------------------").append("\n");
                output.append(responseApp0.toString()).append("\n");
                output.append("-------------------App1-----------------------------").append("\n");
                output.append(responseApp1.toString()).append("\n");
                output.append("-------------------App2-----------------------------").append("\n");
                output.append(responseApp2.toString()).append("\n");
                output.append("-------------------AllA-----------------------------").append("\n");
                output.append(responseAppUnited.toString()).append("\n");
                output.append("----------------------------------------------------").append("\n");
                output.append("----------------------------------------------------").append("\n");
                output.append("----------------------------------------------------").append("\n");
                output.append("----------------------------------------------------").append("\n");
                File file = File.createTempFile("run" + run, ".csv");
                FileUtils.writeStringToFile(file, output.toString());
                LOG.info("written to file: \n" + output);
                LOG.info("written to file: \n" + file.getAbsolutePath());
                files.add(file.getAbsolutePath());
                switch (run) {
                    case 0:
                        restart(MysqlDatabaseController.DEFAULT);
                        break;
                    case 1:
                        restart(MysqlDatabaseController.ONE_ALL);
                        break;
                    case 2:
                        restart(MysqlDatabaseController.ONE_EACH);
                        break;
                    default:
                        break;

                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            LOG.info("An error occured");
        } finally {
            for (String file : files) {
                LOG.info("Files in:");
                LOG.info(file);
            }
            return;
        }
    }

    private static void restart(String param) throws IOException {
        HttpClient httpclient = HttpClients.createDefault();

        HttpGet get = new HttpGet(DOCKER_PLACEMENT_RESTART + "/" + param);
        HttpResponse response = httpclient.execute(get);
    }

    private static String getSystemState(long startMoment) throws Exception {
        SystemState serverState = getServerState();
        serverState.setTimestamp(startMoment);
//                LOG.info(serverState.getTimestamp() + "," + uri + "," + elapsedTime + "," + statusLine.getStatusCode());
        Map<String, List<String>> vmContainers = serverState.getVmContainers();
        StringBuilder result = new StringBuilder("");
        for (String vm : vmContainers.keySet()) {
            result.append(vm).append("(");
            for (String containers : vmContainers.get(vm)) {

                String output = String.format("%s", containers);
                result.append(output).append(",");
            }
            result.append("),");
        }
        return result.toString();
    }

    private static void updatePlacement(String[] apps) throws IOException {
        HttpClient httpclient = HttpClients.createDefault();
        for (final String app : apps) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        updatePlacement(app);
                    } catch (IOException e) {
                    }
                }
            }).start();

        }
    }

    /**
     * return responsetime
     */
    private static long updatePlacement(String app) throws IOException {
        HttpClient httpclient = HttpClients.createDefault();
        HttpGet get = new HttpGet(PROXY_IP + "/" + app + "/");
        long startTime = System.currentTimeMillis();
        HttpResponse response = httpclient.execute(get);
        long elapsedTime = System.currentTimeMillis() - startTime;
        StatusLine statusLine = response.getStatusLine();
        LOG.info(app + " " + statusLine);
        return elapsedTime;

    }

    private static void updatePlacement(List<PlannedInvocation> invocations) throws Exception {
        PlannedInvocations plannedInvocations = new PlannedInvocations();
        plannedInvocations.setPlannedInvocations(invocations);

        HttpPost put = new HttpPost(DOCKER_PLACEMENT_SERVER_ADD_INVOCATION);
        HttpClient httpclient = HttpClients.createDefault();

        JAXBContext context = JAXBContext.newInstance(PlannedInvocations.class);
        Marshaller m = context.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        StringWriter stringWriter = new StringWriter();
        m.marshal(plannedInvocations, stringWriter);

        String string = stringWriter.toString();

        StringEntity entity = new StringEntity(string);
        entity.setContentType("application/xml");
        put.setEntity(entity);
        HttpResponse response = httpclient.execute(put);
        StatusLine statusLine = response.getStatusLine();
        LOG.info("Status Line " + statusLine);


        //update
        HttpGet get = new HttpGet(DOCKER_PLACEMENT_SERVER_RUN_OPTIMIZATION);
        response = httpclient.execute(get);
        statusLine = response.getStatusLine();
        LOG.info("Status Line " + statusLine);

    }

    private static List<PlannedInvocation> getInvocations() {
        long tau_t = new Date().getTime();
        List<PlannedInvocation> result = new ArrayList<>();
        for (DockerContainer dockerContainer : getDockerMap()) {

            int amount = 0;

            switch (dockerContainer.getAppID()) {
                case "app0":
                    amount = 251;
                    break;
                case "app1":
                    amount = 251;
                    break;
                case "app2":
                    amount = 150;
                    break;
                case "app3":
                    amount = 0;
                    break;
            }


            PlannedInvocation plannedInvocation = new PlannedInvocation(dockerContainer.getDockerImage().getAppId(),
                    amount, tau_t);
            plannedInvocation.setDone(false);
            result.add(plannedInvocation);
        }
        return result;
    }

    private static List<PlannedInvocation> getInvocations(int app0, int app1, int app2, int app3) {
        long tau_t = new Date().getTime();
        List<PlannedInvocation> result = new ArrayList<>();
        for (DockerContainer dockerContainer : getDockerMap()) {

            int amount = 0;

            switch (dockerContainer.getAppID()) {
                case "app0":
                    amount = app0;
                    break;
                case "app1":
                    amount = app1;
                    break;
                case "app2":
                    amount = app2;
                    break;
                case "app3":
                    amount = app3;
                    break;
            }


            PlannedInvocation plannedInvocation = new PlannedInvocation(dockerContainer.getDockerImage().getAppId(),
                    amount, tau_t);
            plannedInvocation.setDone(false);
            result.add(plannedInvocation);
        }
        return result;
    }

    public static List<DockerContainer> getDockerMap() {

        List<DockerContainer> dockerList = new ArrayList<>();

        DockerConfiguration configuration = DockerConfiguration.SINGLE_CORE;

        {
            DockerImage dockerImage = MysqlDatabaseController.parseByAppId("app0");
            DockerContainer container = new DockerContainer(dockerImage, configuration);
            dockerList.add(container);
        }
        {
            DockerImage dockerImage = MysqlDatabaseController.parseByAppId("app1");
            DockerContainer container = new DockerContainer(dockerImage, configuration);
            dockerList.add(container);
        }
        {
            DockerImage dockerImage = MysqlDatabaseController.parseByAppId("app2");
            DockerContainer container = new DockerContainer(dockerImage, configuration);
            dockerList.add(container);
        }
        {
            DockerImage dockerImage = MysqlDatabaseController.parseByAppId("app3");
            DockerContainer container = new DockerContainer(dockerImage, configuration);
            dockerList.add(container);
        }

        return dockerList;
    }


    public static SystemState getServerState() throws Exception {
        HttpClient httpclient = HttpClients.createDefault();

        HttpGet get = new HttpGet(DOCKER_PLACEMENT_SYSTEM_STATE);
        HttpResponse response = httpclient.execute(get);
        StatusLine statusLine = response.getStatusLine();
        LOG.info("Status Line " + statusLine);
        Gson gson = new Gson();
        String s = EntityUtils.toString(response.getEntity());
        return gson.fromJson(s, SystemState.class);
    }

}
