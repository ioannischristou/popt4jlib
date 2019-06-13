echo off
REM runRnQTnormOptDBatchTest.bat <Kr> <Ko> <L> <mi> <sigma> <h> <p> <serverport>(7891) <numworkerthreads>(8) [epsq(0.1)] [minq(0)] [epst(0.01)] [epss(1.e-4)] [bsize(8)]
REM T530 WLAN IP (home):
REM Compaq cq 58 IP (home): 
REM T530 IP (work): 10.100.209.143
REM DeLL OPTIPLEX 790 IP (work): 10.100.208.98
REM DeLL OPTIPLEX 790 (2) IP (work): 10.100.209.187
REM DeLL OPTIPLEX 755 IP (work): 10.100.208.99
REM Compaq cq 58 IP (work): 10.100.209.226
REM starts one worker and one server in different windows, then starts the test in its own command prompt
REM java <jvmargs> -cp <classpath> parallel.distributed.PDBTExecSingleCltWrkInitSrv [workers_port(7890)] [clients_port(7891)]
call start PDBTExecSingleCltWrkInitSrv.bat 7890 %8 
echo Verify server has started before you press any key...
pause
REM java <jvm_args> -cp <classpath> parallel.distributed.PDBTExecInitedWrk [num_threads(10)] [serverhost(localhost)] [serverport(7890)] [dbglvl(0)]
call start PDBTExecInitedWrk.bat %9 localhost 7890 1
echo Verify all workers are started before you press any key...
pause
echo on
REM java -Xmx8000m -cp .\dist\popt4jlib.jar tests.sic.rnqt.norm.RnQTCnormOpt <Kr> <Ko> <L> <mi> <sigma> <h> <p> [pdsrvhost(localhost)] [pdsrvport(7891)] [epsq(0.1)] [minq(0)] [epst(0.01)] [epss(1.e-4)] [bsize(8)]
@echo off
for /f "tokens=1-9*" %%a in ("%*") do (
    set p1=%%a
    set p2=%%b
    set p3=%%c
    set p4=%%d
    set p5=%%e
    set p6=%%f
    set p7=%%g
    set p8=%%h
    set p9=%%i
    set prest=%%j
)
@echo on
REM java -Xmx2000m -cp .\dist\popt4jlib.jar tests.sic.rnqt.norm.RnQTCnormOpt %1 %2 %3 %4 %5 %6 %7 localhost %8 %10 %11 %12 %13 %14
java -Xmx2000m -cp .\dist\popt4jlib.jar tests.sic.rnqt.norm.RnQTCnormOpt %p1% %p2% %p3% %p4% %p5% %p6% %p7% localhost %p8% %prest%