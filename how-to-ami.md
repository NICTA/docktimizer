* add you public key in the last line of the cloud-config script below
* create a micro instance running CoreOS with version > 717.3. e.g., ami-2b2e6911
* insert the cloud-config script below as user init script
* ensure the security group is set correctly, e.g., open at least ports 8080,8090,8091,8082
* start the instance and wait until it is running
* create an AMI from this instance
* copy the AMI ID into the "cloud-config.properties" 


```
 
#cloud-config
write_files:
  - path: /etc/systemd/system/docker.service.d/50-insecure-registry.conf
    content: |
        [Service]
        Environment=DOCKER_OPTS='--label="vm=$private_ipv4"'
coreos:
  #update strategy
  update:
    reboot-strategy: off # etcd-lock would also be possible, for now, don't reboot
  #enable listening on port 2375
  etcd:
    # generate a new token for each unique cluster from https://discovery.etcd.io/new?size=3
    # specify the intial size of your cluster with ?size=X
    discovery: https://discovery.etcd.io/cc4656462d995c7e526d85ef692e3c4f
    # multi-region and multi-cloud deployments need to use $public_ipv4
    addr: $private_ipv4:4001
    peer-addr: $private_ipv4:7001
  units:
    - name: etcd.service
      command: start
    - name: fleet.service
      command: start
    - name: docker-tcp.socket
      command: start
      enable: true
      content: |
        [Unit]
        Description=Docker Socket for the API

        [Socket]
        ListenStream=2375
        BindIPv6Only=both
        Service=docker.service

        [Install]
        WantedBy=sockets.target
    - name: docker-startup.service
      command: start
      content: |
        [Unit]
        Description=Connect to docker swarm
        After=docker.service

        [Service]
        ExecStart=/usr/bin/echo \"Starting Docker Containers...\"
    - name: example-docker1-startup.service
      command: start
      content: |
        [Unit]
        Description=Just a docker container
        After=docker.service

        [Service]
        Restart=false
        ExecStart=/usr/bin/docker run  --name initDocker1 -p 8090:3000 --rm  bonomat/nodejs-hello-world 
    - name: example-docker2-startup.service
      command: start
      content: |
        [Unit]
        Description=Just a docker container
        After=docker.service

        [Service]
        Restart=false
        ExecStart=/usr/bin/docker run  --name initDocker2 -p 8082:80 --rm  bonomat/zero-to-wordpress-sqlite 
    - name: example-docker-3-startup.service
      command: start
      content: |
        [Unit]
        Description=Just a docker container
        After=docker.service

        [Service]
        Restart=false
        ExecStart=/usr/bin/docker run  --name initDocker3 -p 8091:80 --rm  gjong/apache-joomla 

ssh_authorized_keys:  # include one or more SSH public keys
  - "ssh-rsa HERE_GOES_YOUR_PUBLIC_KEY"

 ```