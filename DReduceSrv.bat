echo off
REM java parallel.distributed.DReduceSrv [port(7901)]
java -Xmx1500m -cp ./dist/popt4jlib.jar;./lib/colt.jar parallel.distributed.DReduceSrv %1
