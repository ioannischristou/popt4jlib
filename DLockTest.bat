REM DLockTest [numthreads(100)] [host_lockport_unlockport(localhost_7892_7893)]
java -Xmx1500m -cp ./dist/popt4jlib.jar;./dist/lib/colt.jar; parallel.distributed.DLockTest %1 %2
