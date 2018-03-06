#!/bin/bash
set -e
mvn package
cp $PWD/target/CICFlowMeter*.jar $PWD/dist/lib/CICFlowMeter-3.0.jar
echo "Cleaning previous output and running..."
cd $PWD/dist/bin/ && rm out/* && ./CICFlowMeter nmap.pcap out
