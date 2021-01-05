echo off
REM runsSTnormOptDBatchTest.bat <Kr> <Ko> <L> <mu> <sigma> <h> <p> <p2>(0) <serverport>(7891) <numworkerthreads>(8) [qnot(0)] [epst(0.01)] [epss(0.1)] [bsize(8)]
REM T530 WLAN IP (home):
REM Compaq cq 58 IP (home): 
REM T530 IP (work): 10.100.209.143
REM DeLL OPTIPLEX 790 IP (work): 10.100.208.98
REM DeLL OPTIPLEX 790 (2) IP (work): 10.100.209.187
REM DeLL OPTIPLEX 755 IP (work): 10.100.208.99
REM Compaq cq 58 IP (work): 10.100.209.226
REM starts one worker and one server in different windows, then starts the test in its own command prompt
REM java <jvmargs> -cp <classpath> parallel.distributed.PDBTExecSingleCltWrkInitSrv [workers_port(7890)] [clients_port(7891)]
call start PDBTExecSingleCltWrkInitSrv.bat 7890 %9 
echo Verify server has started before you press any key...
pause
REM java <jvm_args> -cp <classpath> parallel.distributed.PDBTExecInitedWrk [num_threads(10)] [serverhost(localhost)] [serverport(7890)] [dbglvl(0)]
call start PDBTExecInitedWrk.bat %10 localhost 7890 1
echo Verify all workers are started before you press any key...
pause
echo on
REM java -Xmx8000m -cp .\dist\popt4jlib.jar tests.sic.sST.norm.sSTCnormOpt <Kr> <Ko> <L> <mu> <sigma> <h> <p> [p2(0)] [pdsrvhost(localhost)] [pdsrvport(7891)] [qnot(0)] [epst(0.01)] [epss(0.1)] [bsize(8)]
@echo off
for /f "tokens=1-11*" %%a in ("%*") do (
    set Kr=%%a
    set Ko=%%b
    set L=%%c
    set mu=%%d
    set sigma=%%e
    set h=%%f
    set p=%%g
    set p2=%%h
    set srvport=%%i
    set numworkerthreads=%%j
    set prest=%%k
)
@echo on
REM java -Xmx2000m -cp .\dist\popt4jlib.jar tests.sic.sST.norm.sSTCnormOpt %1 %2 %3 %4 %5 %6 %7 localhost %8 %10 %11 
java -Xmx2000m -cp .\dist\popt4jlib.jar tests.sic.sST.norm.sSTCnormOpt %Kr% %Ko% %L% %mu% %sigma% %h% %p% %p2% localhost %srvport% %prest%
