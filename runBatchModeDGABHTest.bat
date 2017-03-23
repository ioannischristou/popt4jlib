echo off
REM starts a number of servers in different windows, then starts the test in its own command prompt
call start PDBTExecInitedSrv.bat
call start PDBTExecInitedWrk.bat
call start PDBTExecInitedWrk.bat
echo Verify all 3 servers are started before you press any key...
pause
echo on
call runDGABHTest.bat testdata\dgabhMichalewiczprops_distributed.txt