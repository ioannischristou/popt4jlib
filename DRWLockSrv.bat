echo off
REM java <jvm_args> -cp <classpath> parallel.distributed.DRWLockSrv [serverport(7897)]
java -Xmx128m -cp ./dist/popt4jlib.jar; parallel.distributed.DRWLockSrv %1