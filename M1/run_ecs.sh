#!/bin/bash
pkill java
rm -rf ~/logs
rm -rf ~/M1/logs
./zookeeper.sh stop
./zookeeper.sh start
../zookeeper*/bin/zkCli.sh deleteall /kvclients
java -cp m2-ecs.jar app_kvECS.ui.Application ./src/app_kvECS/ecs.config $1
./zookeeper.sh stop
