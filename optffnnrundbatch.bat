REM invoke as C:\path\to\popt4jlib> optffnnrundbatch.bat <params_file>
REM starts 3 workers and one server in different windows, then starts the OptFFNNRun class in its own command prompt
REM java <jvmargs> -cp <classpath> parallel.distributed.PDBTExecSingleCltWrkInitSrv [workers_port(7890)] [clients_port(7891)]
call start PDBTExecInitedSrv.bat 7890 7891
echo Verify server has started before you press any key...
pause
REM java <jvm_args> -cp <classpath> parallel.distributed.PDBTExecInitedWrk [num_threads(10)] [serverhost(localhost)] [serverport(7890)] [dbglvl(0)]
call start PDBTExecInitedWrk.bat 8 localhost 7890 2
call start PDBTExecInitedWrk.bat 8 localhost 7890 2
call start PDBTExecInitedWrk.bat 8 localhost 7890 2
echo Verify all 3 workers are started before you press any key...
pause
REM java <jvm_args> -cp <classpath> popt4jlib.neural.OptFFNNRun <paramsfile>
echo on
java -Xmx4g -cp ./dist/popt4jlib.jar popt4jlib.neural.OptFFNNRun %1
