package au.csiro.data61.docktimizer.helper;

import org.apache.commons.io.IOUtils;

/**
 * <p/>
 * this is an init script to initializeAndUpdate a node in our swarm,
 * check on that node for output with:
 * journalctl -b -u oem-cloudinit.service --no-pager
 * if something failed
 * and use
 * systemctl status docker.service
 * systemctl status docker-tcp.socket
 * systemctl status docker-swarm.service
 * global cloud-init info
 * journalctl _EXE=/usr/bin/coreos-cloudinit
 */
public class FileLoader {

    public String CLOUD_INIT_DOCKER_START  ;
    public String CLOUD_INIT;
    public String DOCKER_RESIZE_SCRIPT;

    public FileLoader() {
        try {
            CLOUD_INIT = IOUtils.toString(
                    getClass().getClassLoader().getResourceAsStream("docker-config/swarm-node-cloud-config.yaml"),
                    "UTF-8"
            );
            CLOUD_INIT_DOCKER_START = IOUtils.toString(
                    getClass().getClassLoader().getResourceAsStream("docker-config/docker-units-script.template"),
                    "UTF-8"
            );
            DOCKER_RESIZE_SCRIPT = IOUtils.toString(
                    getClass().getClassLoader().getResourceAsStream("docker-config/docker-resize.sh"),
                    "UTF-8"
            );
        } catch (Exception e) {
            System.err.println("Could not load cloud-init config");
            CLOUD_INIT = "";
        }
    }
}
