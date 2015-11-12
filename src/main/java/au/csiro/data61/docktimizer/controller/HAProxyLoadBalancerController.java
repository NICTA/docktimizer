package au.csiro.data61.docktimizer.controller;

import au.csiro.data61.docktimizer.helper.HAProxyConfigGenerator;
import au.csiro.data61.docktimizer.interfaces.LoadBalancerController;
import au.csiro.data61.docktimizer.models.VirtualMachine;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Properties;

/**
 * A docker container is provided for this HAProxy Load Balancer.
 * https://registry.hub.docker.com/u/bonomat/haproxy-updater/
 *
 */
public class HAProxyLoadBalancerController implements LoadBalancerController {
    private static final Logger LOG = (Logger) LoggerFactory.getLogger(HAProxyLoadBalancerController.class);
    private static LoadBalancerController instance;
    private String LOAD_BALANCER_PORT;
    private String LOAD_BALANCER_PATH;
    private String LOAD_BALANCER_ADDRESS;
    public String LOAD_BALANCER_UPDATE_URL;


    private HAProxyLoadBalancerController() {
    }

    public static LoadBalancerController getInstance() {
        if (instance == null) {
            instance = new HAProxyLoadBalancerController();
        }
        return instance;
    }

    @Override
    public void initialize() {
        Properties prop = new Properties();
        try {

            String propertyFilePath= System.getenv("LOADBALANCER_PROPERTY_FILE");
            if (propertyFilePath != null) {
                prop.load(new FileInputStream(propertyFilePath));
            } else {
                prop.load(instance.getClass().getClassLoader().
                        getResourceAsStream("ha-proxy/ha-proxy.properties"));
            }


            LOAD_BALANCER_ADDRESS = prop.getProperty("LOAD_BALANCER_ADDRESS", "localhost");
            if (LOAD_BALANCER_ADDRESS.startsWith("$")) {
                LOAD_BALANCER_ADDRESS = System.getenv(LOAD_BALANCER_ADDRESS.replace("$",""));
            }
            LOAD_BALANCER_PORT = prop.getProperty("LOAD_BALANCER_PORT", "3000");
            if (LOAD_BALANCER_PORT.startsWith("$")) {
                LOAD_BALANCER_PORT = System.getenv(LOAD_BALANCER_PORT.replace("$",""));
            }
            LOAD_BALANCER_PATH = prop.getProperty("LOAD_BALANCER_PATH", "/api/files");
            LOAD_BALANCER_UPDATE_URL = String.format("http://%s:%s%s", LOAD_BALANCER_ADDRESS, LOAD_BALANCER_PORT,
                    LOAD_BALANCER_PATH);
        } catch (Exception e) {
            LOG.error("\n-------------------------------------------------------" +
                    "\n--------load balancer properties not loaded--------------" +
                    "\n---------------------------------------------------------" +
                    "\n--- /src/resources/ha-proxy.properties is missing -------" +
                    "\n---------------------------------------------------------");

        }
        LOG.info("\n--------------------------------------------------------" +
                "\n--------load balancer properties loaded------------------" +
                "\n---------------------------------------------------------");
    }

    @Override
    public int updateHAProxyConfiguration(List<VirtualMachine> virtualMachines) {
        CloseableHttpResponse response2 = null;
        try {
            CloseableHttpClient httpclient = HttpClients.createDefault();

            HttpPost httpPost = new HttpPost(LOAD_BALANCER_UPDATE_URL);
            String newConfig = HAProxyConfigGenerator.generateConfigFile(virtualMachines);
            LOG.info("New HA Proxy Config:");

            File file = File.createTempFile("new-config", ".cfg");
            FileUtils.writeStringToFile(file, newConfig);
            LOG.info("New HA Proxy File: " + file.getAbsolutePath());
            LOG.info(newConfig);

            HttpEntity httpEntity = MultipartEntityBuilder.create()
                    .addBinaryBody("file", file, ContentType.create("text/plain"), file.getName())
                    .build();

            httpPost.setEntity(httpEntity);

            response2 = httpclient.execute(httpPost);

            LOG.info("HAPRoxy Update: " + response2.getStatusLine().getReasonPhrase());
            HttpEntity entity2 = response2.getEntity();
            EntityUtils.consume(entity2);
            return response2.getStatusLine().getStatusCode();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (response2 != null) {
                    response2.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return -1;

    }


}
