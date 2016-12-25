echo off
REM starts a number of servers in different windows, then starts the test in its own command prompt
call start DAccumulatorSrv.bat
call start DConditionCounterLLCSrv.bat
call start PDAsynchBatchTaskExecutorSrv.bat 7980 7981 true
call start PDAsynchBatchTaskExecutorWrk.bat 4 localhost 7980 true 100000
call start PDAsynchBatchTaskExecutorWrk.bat 4 localhost 7980 true 100000
echo Verify all 5 servers are started before you press any key...
pause
echo on
java -Xmx8000m -cp .\dist\popt4jlib.jar graph.packing.DBBGASPPacker %1 %2
