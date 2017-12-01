#!/bin/bash
java -Xmx1500m -cp ./dist/popt4jlib.jar parallel.distributed.DAccumulatorSrv $1 $2 $3

