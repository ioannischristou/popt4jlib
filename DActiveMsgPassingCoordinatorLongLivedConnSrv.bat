echo off
REM java parallel.distributed.DActiveMsgPassingCoordinatorLongLivedConnSrv [port(7895)]
java -Xmx1500m -cp dist\popt4jlib.jar;dist\lib\colt.jar parallel.distributed.DActiveMsgPassingCoordinatorLongLivedConnSrv
