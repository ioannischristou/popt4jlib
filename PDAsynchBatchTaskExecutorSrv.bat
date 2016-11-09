REM PDAsynchBatchTaskExecutorSrv [workers_port(7980)] [clients_port(7981)]
java -Xmx1500m -cp ./dist/popt4jlib.jar;./dist/lib/colt.jar; parallel.distributed.PDAsynchBatchTaskExecutorSrv %1 %2