echo off
REM start 3 DLockSrv's (one central, and two connecting to it) and a DAccumulatorSrv on the localhost then start 5 DLockTest processes connecting to all 3 DLockSrv's
REM start central lock server
start call DLockSrv.bat 5001 5002
REM start 2 more lock servers
start call DLockSrv.bat 5003 5004 localhost_5001_5002
start call DLockSrv.bat 5005 5006 localhost_5001_5002
REM start the accumulator server
start call DAccumulatorSrv.bat
echo Verify the DLockSrv and DAccumulatorSrv servers have started, and press any key
pause
start call DLockTest.bat 300 localhost_5001_5002
start call DLockTest.bat 200 localhost_5003_5004
start call DLockTest.bat 200 localhost_5005_5006
start call DLockTest.bat 100 localhost_5001_5002
start call DLockTest.bat 50 localhost_5003_5004
