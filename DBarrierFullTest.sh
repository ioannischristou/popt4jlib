#!/bin/bash
cmd1="java -cp ./popt4jlib.jar parallel.distributed.DBarrierSrv"
eval "${cmd1}" &
# instead of read, use sleep
sleep 100
cmd2="java -cp ./popt4jlib.jar parallel.distributed.DBarrierTest 0 9 1 2 10" 
eval "${cmd2}" &
cmd3="java -cp ./popt4jlib.jar parallel.distributed.DBarrierTest 10 19 2 2 20"
eval "${cmd3}" &

