echo off
call start PDBTExecInitedSrv.bat
call start PDBTExecInitedWrk.bat 4 localhost 7890 2
REM call start PDBTExecInitedWrk.bat 4 localhost 7890 2
echo Verify all 3 servers are started before you press any key...
pause
echo on
java -cp .\dist\popt4jlib.jar parallel.distributed.PDBTExecInitedCltTest2
