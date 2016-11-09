@echo off
REM ARGS ARE: <filename> <k> <numthreads>
java -Xmx1500m -cp ./popt4jlib.jar; tests.GMeansMTClustererTest %1 %2 %3