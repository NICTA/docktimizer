package au.csiro.data61.docktimizer.testClient;

import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 */
public class GetThread extends Thread {

    private static final Logger LOG = (Logger) LoggerFactory.getLogger(GetThread.class);


    private final CloseableHttpClient httpClient;
    private final HttpContext context;
    private final HttpGet httpget;
    private final String uri;
    private final long startMoment;

    public GetThread(long startMoment, String uri, CloseableHttpClient httpClient, HttpGet httpget) {
        this.startMoment = startMoment;
        this.uri = uri;
        this.httpClient = httpClient;
        this.context = HttpClientContext.create();
        this.httpget = httpget;
    }

    @Override
    public void run() {
        try {
            long startTime = System.currentTimeMillis();
            CloseableHttpResponse response = httpClient.execute(httpget, context);
            StatusLine statusLine = response.getStatusLine();
            long elapsedTime = System.currentTimeMillis() - startTime;
            try {
                LOG.info(startMoment + ";" + uri + ";" + elapsedTime + ";" + statusLine.getStatusCode());

            } finally {
                response.close();
            }
        } catch (ClientProtocolException ex) {
            // Handle protocol errors
            LOG.info(startMoment + ";" + uri + ";" + -1 + ";" + 500);
        } catch (IOException ex) {
            ex.printStackTrace();
            LOG.error("IO Excepection received", ex);
            // Handle I/O errors
        }
    }

}