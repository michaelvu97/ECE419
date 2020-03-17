#!/bin/bash

# Preamble script for testing

./zookeeper.sh stop
./zookeeper.sh start
../zookeeper*/bin/zkCli.sh deleteall /kvclients