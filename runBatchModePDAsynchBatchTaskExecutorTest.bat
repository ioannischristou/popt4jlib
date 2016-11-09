echo off
REM starts a number of servers in different windows, then starts the test in its own command prompt
call start PDAsynchBatchTaskExecutorSrv.bat
call start DAccumulatorSrv.bat
call start DConditionCounterSrv.bat
call start PDAsynchBatchTaskExecutorWrk.bat
call start PDAsynchBatchTaskExecutorWrk.bat
echo Verify all 5 servers are started before you press any key...
pause
call PDAsynchBatchTaskExecutorCltTest.bat
