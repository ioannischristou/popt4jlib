echo off
REM java tests.DDE2Test <paramsfile> [randomseed] [maxfunctionevaluations]
java -Xmx1500m -cp ./dist/popt4jlib.jar tests.DDE2Test %1 %2 %3