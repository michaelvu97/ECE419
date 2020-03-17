#!/bin/bash
# ssh -o StrictHostKeyChecking=no $2 nohup java -cp ~/M1/m2-server.jar app_kvServer.KVServer $3 $4 $5 $6 $7 $8 $9> /dev/null &
ssh -o StrictHostKeyChecking=no $2 nohup java -cp ~/M1/m2-server.jar app_kvServer.KVServer $3 $2 $4 $5 $6 $7 $8 > logs/kvserver$3.log
# ssh -n <host> nohup java -jar <path>/ms2-server.jar 50000 ERROR &