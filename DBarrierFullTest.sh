#!/bin/bash
java -cp ./popt4jlib.jar parallel.distributed.DBarrierSrv &
sleep 100
java -cp ./popt4jlib.jar parallel.distributed.DBarrierTest 0 9 1 2 10 &
java -cp ./popt4jlib.jar parallel.distributed.DBarrierTest 10 19 2 2 20 &

