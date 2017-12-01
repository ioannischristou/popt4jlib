#!/bin/bash
# create two servers, each being a client to the other one, and three workers
# servers first
source java -Xmx1500m -cp ./popt4jlib.jar parallel.distributed.PDBatchTaskExecutorSrv 7890 7891 192.168.212.175 7893 &
source java -Xmx1500m -cp ./popt4jlib.jar parallel.distributed.PDBatchTaskExecutorSrv 7892 7893 192.168.212.175 7891 &
# workers next: each worker will have 5 threads to work with 
# the first two workers attached to the first server (7890)
source java -Xmx1500m -cp ./popt4jlib.jar parallel.distributed.PDBatchTaskExecutorWrk 5 localhost 7890 &
source java -Xmx1500m -cp ./popt4jlib.jar parallel.distributed.PDBatchTaskExecutorWrk 5 localhost 7890 &
# third worker attached to the second server (7892)
source java -Xmx1500m -cp ./popt4jlib.jar parallel.distributed.PDBatchTaskExecutorWrk 5 localhost 7892 &
# finally, create a client generating work for the servers: reads a graph, computes its components
# and sends each component to the server for computing the max. graph diameter
# notice: client assumes server is to be found at default location (i.e. localhost,7890)
# in this example localhost resolving to 192.168.212.175, and sends all its tasks to the first server, 
# first server then forwards some tasks to the second server to which it is a client.
source java -Xmx1500m -cp ./popt4jlib.jar parallel.distributed.PDBatchTaskExecutorCltTest2 ./testdata/nudg_025.graph

