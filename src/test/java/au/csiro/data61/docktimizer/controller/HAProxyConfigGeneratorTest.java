package au.csiro.data61.docktimizer.controller;

import au.csiro.data61.docktimizer.AbstractTest;
import au.csiro.data61.docktimizer.helper.HAProxyConfigGenerator;
import au.csiro.data61.docktimizer.models.DockerContainer;
import au.csiro.data61.docktimizer.models.VirtualMachine;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@Ignore
public class HAProxyConfigGeneratorTest extends AbstractTest {

    @Test
    public void testAddServers() throws Exception {
        List<DockerContainer> list = new ArrayList<>();
        list.add(validDockerContainer);
        validVirtualMachine.setDeplyoedContainers(list);
        invalidVirtualMachine.setDeplyoedContainers(list);

        List<VirtualMachine> vms = new ArrayList<>();
        vms.add(validVirtualMachine);
        vms.add(invalidVirtualMachine);

        String haProxyConfiguration = HAProxyConfigGenerator.generateConfigFile(vms);
        assertThat(haProxyConfiguration.trim(), equalTo(TEMPL_CONSTANT.trim()));
    }

    private static final String TEMPL_CONSTANT = "global\n" +
            "        log 127.0.0.1    local0\n" +
            "        log 127.0.0.1    local1 notice\n" +
            "        chroot /var/lib/haproxy\n" +
            "        stats timeout 30s\n" +
            "        user haproxy\n" +
            "        group haproxy\n" +
            "        maxconn 2048\n" +
            "        daemon\n" +
            "\n" +
            "defaults\n" +
            "        log     global\n" +
            "        mode    http\n" +
            "        option  httplog\n" +
            "        option  dontlognull\n" +
            "        timeout connect 5000\n" +
            "        timeout client  50000\n" +
            "        timeout server  50000\n" +
            "        maxconn 2000\n" +
            "        errorfile 400 /etc/haproxy/errors/400.http\n" +
            "        errorfile 403 /etc/haproxy/errors/403.http\n" +
            "        errorfile 408 /etc/haproxy/errors/408.http\n" +
            "        errorfile 500 /etc/haproxy/errors/500.http\n" +
            "        errorfile 502 /etc/haproxy/errors/502.http\n" +
            "        errorfile 503 /etc/haproxy/errors/503.http\n" +
            "        errorfile 504 /etc/haproxy/errors/504.http\n" +
            "\n" +
            "\n" +
            "listen stats *:1936\n" +
            "        stats enable\n" +
            "        stats uri /\n" +
            "        stats hide-version\n" +
            "        stats auth someuser:password\n" +
            "\n" +
            "###################### generated - config #################\n" +
            "\n" +
            "frontend localnodes\n" +
            "    bind *:8081\n" +
            "    mode http\n" +
            "    acl url_app0 path_beg /app0\n" +
            "    use_backend app0 if url_app0\n" +
            "    acl url_google path_beg /google\n" +
            "    use_backend google if url_google\n" +
            "    default_backend google\n" +
            "\n" +
            "backend google\n" +
            "    mode http\n" +
            "    balance roundrobin\n" +
            "    option forwardfor\n" +
            "    http-request set-header X-Forwarded-Port %[dst_port]\n" +
            "    http-request add-header X-Forwarded-Proto https if { ssl_fc }\n" +
            "    option httpchk HEAD / HTTP/1.1\\r\n" +
            "Host:localhost\n" +
            "    reqrep ^([^\\ :]*)\\ /google/(.*)     \\1\\ /\\2\n" +
            "    server google 23.92.30.78:80 check\n" +
            "\n" +
            "\n" +
            "############### next server #############\n" +
            "backend app0\n" +
            "    mode http\n" +
            "    balance roundrobin\n" +
            "    option forwardfor\n" +
            "    http-request set-header X-Forwarded-Port %[dst_port]\n" +
            "    http-request add-header X-Forwarded-Proto https if { ssl_fc }\n" +
            "    option httpchk HEAD / HTTP/1.1\\r\n" +
            "Host:localhost\n" +
            "    reqrep ^([^\\ :]*)\\ /app0/(.*)     \\1\\ /\\2\n" +
            "    server appapp0-1 128.130.172.211:8090 check maxconn 70\n" +
            "    server appapp0-2 999.999.999.999:8090 check maxconn 70";
}