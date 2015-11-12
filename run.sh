#!/bin/bash

setup_on=$1
building_on=$2
debug=$3

case $setup_on in
  (true)    echo "setup is on"; ./setup-dependencies.sh ;;
  (false)   echo "setup is off";;
  (*) echo "usage: run.sh true/false true/false true/false";;
esac

case $building_on in
  (true)    echo "building is on"; mvn clean install -Dmaven.test.skip=true ;;
  (false)   echo "building is off";;
  (*) echo "usage: run.sh true/false true/false true/false";;
esac


case $debug in
  (true)   echo "starting execution in debug mode"; java -jar -Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=5005,suspend=y target/DockerPlacement.jar;;
  (*)      echo "normal "; java -jar target/DockerPlacement.jar;;
esac

