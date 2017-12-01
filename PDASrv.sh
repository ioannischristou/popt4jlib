#!/bin/bash
java -Xmx3000m -cp ./dist/popt4jlib.jar parallel.distributed.PDAsynchBatchTaskExecutorSrv $1 $2 $3 $4

