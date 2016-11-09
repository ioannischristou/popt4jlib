echo off
REM running 4 distributed DDE processes for the Rosenbrock function
REM requires starting a DActiveMsgPassingCoordinatorLongLivedConnSrv and a DReducerSrv
start call DActiveMsgPassingCoordinatorLongLivedConnSrv.bat
start call DReduceSrv.bat
echo Verify distributed msg-passing server and distributed reducer server are both started and accept connections, and press enter
pause
start call runDDETest.bat testdata\ddeRosenbrock50Dprops_Proc1.txt
start call runDDETest.bat testdata\ddeRosenbrock50Dprops_Proc2.txt
start call runDDETest.bat testdata\ddeRosenbrock50Dprops_Proc3.txt
start call runDDETest.bat testdata\ddeRosenbrock50Dprops_Proc4.txt
echo on
