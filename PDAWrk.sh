#!/bin/bash
java -Xmx30000m -cp ./dist/popt4jlib.jar parallel.distributed.PDAsynchBatchTaskExecutorWrk $1 $2 $3 $4

