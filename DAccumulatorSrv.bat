REM DAccumulatorSrv [port(7900)] [maxthreads(10000)]
java -Xmx1500m -cp ./dist/popt4jlib.jar;./dist/lib/colt.jar; parallel.distributed.DAccumulatorSrv %1 %2