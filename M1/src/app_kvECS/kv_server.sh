ssh -o StrickHostKeyChecking=no -n -f $1@$2 "sh -c 'cd /ece419/lab1/ECE419/M1; nohup ./run_server $3 $4 $5 $6 $7 $8 $9> /dev/null & '"
