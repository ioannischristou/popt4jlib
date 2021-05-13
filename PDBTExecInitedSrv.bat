REM PDBTExecInitedSrv [workers_port(7890)] [clients_port(7891)] [dbglvl(0)]
java -Xmx1500m -cp ./dist/popt4jlib.jar;./dist/lib/colt.jar; parallel.distributed.PDBTExecInitedSrv %*