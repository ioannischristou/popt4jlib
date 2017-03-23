echo off
REM runBatchModeDSPPFinderFwdSrchTest.bat <graphfilename> <srcnodeid> <destnodeid> <#paths> <distr_min_size>
REM starts a number of servers in different windows, then starts the test in its own command prompt
call start PDBTExecInitedSrv.bat
call start PDBTExecInitedWrk.bat 4
REM call start PDBTExecInitedWrk.bat 4
echo Verify all servers are started before you press any key...
pause
echo on
call runDSPPFinderFwdSrch.bat %1 %2 %3 %4 true localhost 7891 %5
