echo off
REM java <jvm_args> -cp <classpath> parallel.distributed.PDBTExecInitedWrk [num_threads(10)] [serverhost(localhost)] [serverport(7890)]
java -Xmx2000m -cp ./dist/popt4jlib.jar; parallel.distributed.PDBTExecInitedWrk %1 %2 %3