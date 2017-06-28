echo off
REM runBatchModeDBBGASPTest.bat <graph_file> <params_file> [maxnodesallowed(+Inf)]
REM T530 WLAN IP (home):
REM Compaq cq 58 IP (home): 
REM T530 IP (work): 10.100.208.248
REM DeLL OPTIPLEX 790 IP (work): 10.100.208.98
REM DeLL OPTIPLEX 790 (2) IP (work): 10.100.209.187
REM DeLL OPTIPLEX 755 IP (work): 10.100.208.99
REM Compaq cq 58 IP (work): 10.100.209.226
REM starts a number of servers in different windows, then starts the test in its own command prompt
REM DAccumulatorSrv.bat [port(7900)] [maxthreads(10000)] [notificationsport(9900)]
call start DAccumulatorSrv.bat
REM DConditionCounterLLCSrv.bat [port(7899)]
call start DConditionCounterLLCSrv.bat
REM PDAsynchBatchTaskExecutorSrv.bat [workers_port(7980)] [clients_port(7981)] [send_init_cmd(false)] [other_host,other_port()]
call start PDAsynchBatchTaskExecutorSrv.bat 7980 7981 true
REM PDAsynchBatchTaskExecutorWrk [num_threads(10)] [pdasrvhost(localhost)] [pdasrvport(7980)] [run_init_cmd(false)] 
call start PDAsynchBatchTaskExecutorWrk.bat 3 localhost 7980 true
call start PDAsynchBatchTaskExecutorWrk.bat 3 localhost 7980 true
call start PDAsynchBatchTaskExecutorWrk.bat 3 localhost 7980 true
REM call start PDAsynchBatchTaskExecutorWrk.bat 8 localhost 7980 true
echo Verify all servers are started before you press any key...
pause
echo on
REM java -Xmx8000m -cp .\dist\popt4jlib.jar graph.packing.DBBGASPPacker <graph_file> <params_file> [maxnodesallowed(Integer.MAX_VALUE)]
java -Xmx5000m -cp .\dist\popt4jlib.jar graph.packing.DBBGASPPacker %1 %2 %3
