echo off
REM java <jvmargs> -cp <classpath> parallel.distributed.PDBTExecSingleCltWrkInitSrv [workers_port(7890)] [clients_port(7891)]
java -Xmx1500m -cp ./dist/popt4jlib.jar parallel.distributed.PDBTExecSingleCltWrkInitSrv %1 %2
