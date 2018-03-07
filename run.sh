#!/bin/bash


# to install dependencies run this manually
#     cd $PWD/jnetpcap/linux/jnetpcap-1.4.r1425
#     mvn install:install-file -Dfile=jnetpcap.jar -DgroupId=org.jnetpcap -DartifactId=jnetpcap -Dversion=1.4.1 -Dpackaging=jar

set -e
mvn package
cp $PWD/target/CICFlowMeter*.jar $PWD/dist/lib/CICFlowMeter-3.0.jar
cd $PWD/dist/bin/ && ./CICFlowMeter $1 out
