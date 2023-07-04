echo off
REM runsSTnbinHeurOptDBatchTest.bat <Kr> <Ko> <L> <lambda> <p_l> <h> <p> <serverport>(7891) <numworkerthreads>(8) [epst(0.01)] [deltat(0.1)] [bsize(24)] [dbglvl(0)] [run4ZeroOrderCost(true)]
REM T530 WLAN IP (home):
REM Compaq cq 58 IP (home): 
REM T530 IP (work): 10.100.209.143
REM DeLL OPTIPLEX 790 IP (work): 10.100.208.98
REM DeLL OPTIPLEX 790 (2) IP (work): 10.100.209.187
REM DeLL OPTIPLEX 755 IP (work): 10.100.208.99
REM Compaq cq 58 IP (work): 10.100.209.226
REM starts one worker and one server in different windows, then starts the test in its own command prompt
REM java <jvmargs> -cp <classpath> parallel.distributed.PDBTExecSingleCltWrkInitSrv [workers_port(7890)] [clients_port(7891)]
@echo off
for /f "tokens=1-9*" %%a in ("%*") do (
    set Kr=%%a
    set Ko=%%b
    set L=%%c
    set lambda=%%d
    set pl=%%e
    set h=%%f
    set p=%%g
    set srvport=%%h
    set numworkerthreads=%%i
    set prest=%%j
)
@echo on
call start PDBTExecSingleCltWrkInitSrv.bat 7890 %srvport%
echo Verify server has started before you press any key...
pause
REM java <jvm_args> -cp <classpath> parallel.distributed.PDBTExecInitedWrk [num_threads(10)] [serverhost(localhost)] [serverport(7890)] [dbglvl(0)]
call start PDBTExecInitedWrk.bat %numworkerthreads% localhost 7890 1
echo Verify all workers are started before you press any key...
pause
echo on
REM java -Xmx8000m -cp .\dist\popt4jlib.jar tests.sic.sST.nbin.sSTCnbinHeurOpt <Kr> <Ko> <L> <lambda> <p_l> <h> <p> [pdsrvhost(localhost)] [pdsrvport(7891)] [epst(0.01)] [deltat(0.1)] [bsize(24)] [dbglvl(0)] [run4zeroordercost(true)]
java -Xmx8000m -cp .\dist\popt4jlib.jar tests.sic.sST.nbin.sSTCnbinHeurOpt %Kr% %Ko% %L% %lambda% %pl% %h% %p% localhost %srvport% %prest%
