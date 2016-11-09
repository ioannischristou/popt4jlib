REM run batch tests on std optimization test functions to allow statistical 
REM analysis of results to see whether any heuristic is clearly superior.
REM The methods compared are: GA/EA/DE/SA/PS/MC/ASD/FCG/AVD/FA
REM plus the combined methods GA-AVD/EA-AVD/DE-AVD/SA-AVD/PS-AVD
REM The test functions are:
REM DropWave only
REM All tests are run using 10,50,100, and 1000 dimensions (the first argument),
REM and are repeatedly run as many times as indicated in the second argument 
REM (each time changing the random seed of the run).
REM The first argument (#dimensions) is also presented as third argument to the runXXXTestR batch files
REM so that a maximum limit of 1000*(#dimensions) is imposed on the number of allowed function evaluations
REM of each run.
REM 1. DropWave
REM call runDGATestR %2 testdata\opti14\dgaDropWave%1props.txt %1 1> testresults\opti14\dgaDropWave%1.out 2>&1 
REM call runDEATestR %2 testdata\opti14\deaDropWave%1props.txt %1 1> testresults\opti14\deaDropWave%1.out 2>&1
REM call runDDETestR %2 testdata\opti14\ddeDropWave%1props.txt %1 1> testresults\opti14\ddeDropWave%1.out 2>&1
REM call runDSATestR %2 testdata\opti14\dsaDropWave%1props.txt %1 1> testresults\opti14\dsaDropWave%1.out 2>&1
REM call runDPSOTestR %2 testdata\opti14\dpsoDropWave%1props.txt %1 1> testresults\opti14\dpsoDropWave%1.out 2>&1
REM call runDMCSTestR %2 testdata\opti14\dmcsDropWave%1props.txt %1 1> testresults\opti14\dmcsDropWave%1.out 2>&1
REM call runASDTestR %2 testdata\opti14\asdDropWave%1props.txt %1 1> testresults\opti14\asdDropWave%1.out 2>&1
call runFCGTestR %2 testdata\opti14\fcgDropWave%1props.txt %1 1> testresults\opti14\fcgDropWave%1.out 2>&1
call runAVDTestR %2 testdata\opti14\avdDropWave%1props.txt %1 1> testresults\opti14\avdDropWave%1.out 2>&1
call runDFATestR %2 testdata\opti14\dfaDropWave%1props.txt %1 1> testresults\opti14\dfaDropWave%1.out 2>&1
call runDGATestR %2 testdata\opti14\dgafcgDropWave%1props.txt %1 1> testresults\opti14\dgafcgDropWave%1.out 2>&1
call runDEATestR %2 testdata\opti14\deafcgDropWave%1props.txt %1 1> testresults\opti14\deafcgDropWave%1.out 2>&1
call runDDETestR %2 testdata\opti14\ddefcgDropWave%1props.txt %1 1> testresults\opti14\ddefcgDropWave%1.out 2>&1
call runDSATestR %2 testdata\opti14\dsafcgDropWave%1props.txt %1 1> testresults\opti14\dsafcgDropWave%1.out 2>&1
call runDPSOTestR %2 testdata\opti14\dpsofcgDropWave%1props.txt %1 1> testresults\opti14\dpsofcgDropWave%1.out 2>&1
