Docktimizer
=======

Short Description
----------------

This project implements a multi-objective optimization model for
scaling Virtual Machines and Docker containers as described in [1]


### License

Copyright 2015 National ICT Australia Limited

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.


### Software Dependencies, Versions and Licenses

* lp_solve, 5.5, Lesser GPL 2.1, http://lpsolve.sourceforge.net/5.5/
* Hibernate ORM, 4.3.9.Final, Lesser GPL 2.1, http://hibernate.org/orm/
* Apache Commons Langs, 3.3.2, Apache 2.0, https://commons.apache.org/proper/commons-lang/
* Apache Commons IO, 2.4, Apache 2.0, https://commons.apache.org/proper/commons-io/
* jUnit, 4.11, Eclipse Public License 1.0, http://junit.org
* Hamcrest, 1.3, BSD, http://hamcrest.org
* Mockito, 1.10.19, MIT, http://mockito.org
* jclouds, 1.9.1, Apache 2.0, https://jclouds.apache.org
* [REMOVED] MySQL Connector Java, 5.1.25, GPL 2, http://www.mysql.com
* Spotify Docker Client, 2.7.7, Apache 2.0, https://github.com/spotify/docker-client
* Jersey, 1.19, CDDL 1.1 & GPL 2, https://jersey.java.net/index.html
* jetcd, 0.3.0, Apache 2.0, https://github.com/justinsb/jetcd
* Async Http Client, 1.9.15, Apache 2.0, https://github.com/AsyncHttpClient/async-http-client
* SLF4J, 1.7.12, MIT, http://www.slf4j.org
* WireMock, 1.54, Apache 2.0, http://wiremock.org
* Java ILP, 1.0, Lesser GPL, http://javailp.sourceforge.net
* MariaDB Connector/J, 1.2.3, Lesser GPL, https://mariadb.org


### Installation    

* setup database
    * create database "dockerplacement";
    * or start a MYSQL Database using Docker:

 ```
 docker
   run
   --detach
   -v /home/core/datadir:/var/lib/mysql
   --env MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}
   --env MYSQL_USER=${MYSQL_USER}
   --env MYSQL_PASSWORD=${MYSQL_PASSWORD}
   --env MYSQL_DATABASE=${MYSQL_DATABASE}
   --name ${MYSQL_CONTAINER_NAME}
   --publish ${MYSQL_PORT}:3306
   mysql:5.7;
 ```

 * start the HAPRoxy using Docker:

 ```
  docker run --name ha -p 3001:3000
      -p 1936:1936
      -p 8082:8081
      -d bonomat/haproxy-updater
 ```


* If you want to use CPLEX
    * Copy CPLEX library into lib/ AND in src/main/resources/natives


* build without tests:

```
mvn clean install -Dmaven.test.skip=true
```

* build with tests:

```
mvn clean install
```

* run debug mode

```
java -jar -Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=5005,suspend=y target/DockerPlacement.jar
```

* run normal

```
 java -jar target/DockerPlacement.jar
```

* or use provided run script

```
./run.sh
```

* run with the dockerfile with:

```
docker run  --name docker
    --link mysql:mysql
    --link ha:ha
    -p 8088:8088 -p 5005:5005  
    -v /home/core/lib:/local/git/lib:ro
    -v /home/core/properties:/local/git/properties  
    -v /home/core/lib/natives:/local/git/src/main/resources/natives:ro
    -it --rm  bonomat/docker-placement bash
```

* used variables are
    * ``` DOCKER_PROPERTY_FILE=/path/to/docker-properties, see example: src/main/resources/docker-config/docker-swarm.properties.example ```


    * ``` OPENSTACK_PROPERTY_FILE/path/to/cloud-properties, see example: src/main/resources/cloud-config/cloud-config.properties.example```


    * ``` MYSQL_PROPERTY_FILE=properties/mysql.properties, see example: src/main/resources/mysql-config/mysql.properties.example```


    * ``` LOADBALANCER_PROPERTY_FILE=properties/mysql.properties, see example: src/main/resources/ha-proxy/ha-proxy.properties.example```


### Package Structure

The project contains the following main packages:

* au.csiro.data61.docktimizer.controller:
This package contains the main controllers and has the following classes
    * HAProxyLoadBalancerController: is used to update the HA Proxy
    * JavaDockerController: is used to control the Docker containers on the Backend VMs
    * OpenStackCloudController: is used to control the VM instances, i.e., starting/stoping VMs
    * MysqlDatabaseController: is used for saving/storing data in a mysql database

* au.csiro.data61.docktimizer.exception:
    * a collection of custom exceptions

* au.csiro.data61.docktimizer.helper:    
    * HAProxyConfigGenerator: is used to generate the HAProxy config file
    * NativeLibraryLoader: is used to load the solver libaries
    * MILPSolver: is used to solve the optimization model, it will either use CPlex or
    falls back on an open-source solver

* au.csiro.data61.docktimizer.hibernate:    
    * just some helpers for hibernate

* au.csiro.data61.docktimizer.interfaces:
    * the interfaces for the controller   

* au.csiro.data61.docktimizer.models:
    * models used within the optimization model

* au.csiro.data61.docktimizer.placement:
    That's the core package, containing the optimization model itself:
    * DockerPlacement: assemblies the optimization model
    * DockerPlacementService: fills in the data to the model and processes the results

* au.csiro.data61.docktimizer.service:
    * DockerPlacementRESTApi: contains a RESTful API for controlling the docker placment tool

* au.csiro.data61.docktimizer.testClient:
    * Contains some test client tools

* au.csiro.data61.docktimizer:
    * DockerPlacementServer: starts up a HTTP server and publishes the RESTful API


[1] P. Hoenisch, I. Weber, S. Schulte, L. Zhu, and A. Fekete, “Four-fold Auto-scaling on a Contemporary Deployment
Platform using Docker Containers (accepted for publication),” in 13th International Conference on Service Oriented
Computing (ICSOC 2015), Goa, India, 2015, pp. NN-NN.
