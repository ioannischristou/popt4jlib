@echo off
REM ARGS ARE: <filename> <k> <numthreads> [rndseed(-1)] [init_method(0=rnd<def>|1=KMeans++|2=KMeans||] [num_iters(-1)]
java -Xmx13000m -cp ./dist/popt4jlib.jar tests.GMeansMTClustererTest %1 %2 %3 %4 %5 %6