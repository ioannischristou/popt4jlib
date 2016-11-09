REM DLockSrv [lockport(7892)] [unlockport(7893)] [otherhost_otherlockport_otherunlockport]
java -Xmx1500m -cp ./dist/popt4jlib.jar;./dist/lib/colt.jar; parallel.distributed.DLockSrv %1 %2 %3
