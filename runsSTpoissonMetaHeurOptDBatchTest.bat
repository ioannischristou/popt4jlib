echo off
REM runsSTpoissonMetaHeurOptDBatchTest.bat <meta-heur-name> <Kr> <Ko> <L> <lambda> <h> <p> [p2(0)] [useRnQT(false)] [dbglvl(0)]
REM T530 WLAN IP (home):
REM Compaq cq 58 IP (home): 
REM T530 IP (work): 10.100.209.143
REM DeLL OPTIPLEX 790 IP (work): 10.100.208.98
REM DeLL OPTIPLEX 790 (2) IP (work): 10.100.209.187
REM DeLL OPTIPLEX 755 IP (work): 10.100.208.99
REM Compaq cq 58 IP (work): 10.100.209.226
REM starts one worker and one server in different windows, then starts the test in its own command prompt
REM java <jvmargs> -cp <classpath> parallel.distributed.PDBTExecInitedSrv [workers_port(7890)] [clients_port(7891)] [dbglvl(2)]
call start PDBTExecInitedSrv.bat 7890 7891 2
echo Verify server has started before you press any key...
pause
REM java <jvm_args> -cp <classpath> parallel.distributed.PDBTExecInitedWrk [num_threads(24)] [serverhost(localhost)] [serverport(7890)] [dbglvl(2)]
call start PDBTExecInitedWrk.bat 24 localhost 7890 2
echo Verify all workers are started before you press any key...
pause
echo on
REM java -Xmx8000m -cp .\dist\popt4jlib.jar tests.sic.sST.poisson.sSTCpoissonMetaHeurOpt <name> <Kr> <Ko> <L> <lambda> <h> <p> [p2(0)] [useRnQT(false)] [dbglvl(0)]
java -Xmx8000m -cp .\dist\popt4jlib.jar tests.sic.sST.poisson.sSTCpoissonMetaHeurOpt %*
