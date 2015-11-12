#!/bin/bash

#setup dependencies
mvn install:install-file -Dfile=./lib/natives/libcplex1260.so -DgroupId=cplex-so \
    -DartifactId=cplex-so -Dversion=1.0 -Dpackaging=so

mvn install:install-file -Dfile=./lib/jar/cplex.jar -DgroupId=cplex-so \
    -DartifactId=cplex-jar -Dversion=1.0 -Dpackaging=jar

mvn install:install-file -Dfile=./lib/jar/javailp-1.2a.jar -DgroupId=java-ilp-jar \
    -DartifactId=java-ilp-jar -Dversion=1.2a -Dpackaging=jar

mvn install:install-file -Dfile=./lib/jar/SCPSolver.jar -DgroupId=java-ilp-jar \
    -DartifactId=java-ilp-solver -Dversion=1.0 -Dpackaging=jar

mvn install:install-file -Dfile=./lib/jar/LPSOLVESolverPack.jar -DgroupId=java-ilp-jar \
    -DartifactId=java-ilp-solver-pack -Dversion=1.0 -Dpackaging=jar

mvn install:install-file -Dfile=./lib/natives/liblpsolve55j_x64.so -DgroupId=java-ilp-jar \
    -DartifactId=java-ilp-solver-pack-so-64 -Dversion=1.0 -Dpackaging=so

mvn install:install-file -Dfile=./lib/natives/liblpsolve55j.so -DgroupId=java-ilp-jar \
    -DartifactId=java-ilp-solver-pack-so -Dversion=1.0  -Dpackaging=so

mvn install:install-file -Dfile=./lib/natives/liblpsolve55.so -DgroupId=java-ilp-jar \
    -DartifactId=java-ilp-solver-pack-so-55 -Dversion=1.0 -Dpackaging=so