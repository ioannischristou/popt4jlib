@echo off
REM ARGS ARE: <filename> <k> [weightsfilename(null)] [use_weights(true)] [numthreads(1)] [rndseed(-1)] 
REM           [init_method(0=rnd<def>|1=KMeans++|2=KMeans||] [num_tries(1)] [num_iters(-1)] [vectors_are_sparse(false)]
REM           [project_on_empty(false)] [KMeans||_rounds(k/2)] [dbglvl(0)]
java -Xmx13000m -cp ./dist/popt4jlib.jar tests.GMeansMTClustererTest %1 %2 %3 %4 %5 %6 %7 %8 %9 