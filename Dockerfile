FROM jamesdbloom/docker-java7-maven

WORKDIR /local/git
RUN export JAVA_DEBUG_OPTS='-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005'

ADD pom.xml ./
ADD src/ src/
ADD lib/ lib/

ADD setup-dependencies.sh setup-dependencies.sh
ADD run.sh run.sh


# Define Openstack and Docker Property File in ClassPath
ENV OPENSTACK_PROPERTY_FILE="properties/cloud-config.properties"
ENV DOCKER_PROPERTY_FILE="properties/docker-swarm.properties"
ENV MYSQL_PROPERTY_FILE="properties/mysql.properties"
ENV LOADBALANCER_PROPERTY_FILE=properties/ha-proxy.properties

RUN ssh-keygen -q -t rsa -N '' -f /root/.ssh/id_rsa


RUN ["/bin/bash", "./setup-dependencies.sh"]
RUN ["cat", "/root/.ssh/id_rsa.pub"]

# How to RUN:
# docker run  --name docker
#        --link mysql:mysql \
#        --link ha:ha \
#        -p 8088:8088 -p 5005:5005 \
#        -v /home/core/lib:/local/git/lib:ro \
#        -v /home/core/properties:/local/git/properties  \
#        -v /home/core/lib/natives:/local/git/src/main/resources/natives:ro \
#        -it --rm  bonomat/docker-placement bash


#RUN ["/usr/bin/mvn", "clean", "install", "-Dmaven.test.skip=true"]

EXPOSE 5005 8088
CMD ["/bin/bash"]

