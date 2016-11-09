echo off
REM java tests.DDETest <paramsfile> [randomseed] [maxfunctionevaluations]
java -Xmx1500m -cp ./dist/popt4jlib.jar;./lib/colt.jar tests.DDETest %1 %2 %3