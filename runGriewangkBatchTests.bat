REM run batch tests on std optimization test functions to allow statistical 
REM analysis of results to see whether any heuristic is clearly superior.
REM The methods compared are: GA/EA/DE/SA/PS/MC/ASD/FCG/AVD/FA
REM plus the combined methods GA-AVD/EA-AVD/DE-AVD/SA-AVD/PS-AVD
REM The test functions are:
REM Griewangk only
REM All tests are run using 10,50,100, and 1000 dimensions (the first argument),
REM and are repeatedly run as many times as indicated in the second argument 
REM (each time changing the random seed of the run).
REM The first argument (#dimensions) is also presented as third argument to the runXXXTestR batch files
REM so that a maximum limit of 1000*(#dimensions) is imposed on the number of allowed function evaluations
REM of each run.
REM 1. Griewangk
call runDGATestR %2 testdata\opti14\dgaGriewangk%1props.txt %1 1> testresults\opti14\dgaGriewangk%1.out 2>&1 
call runDEATestR %2 testdata\opti14\deaGriewangk%1props.txt %1 1> testresults\opti14\deaGriewangk%1.out 2>&1
call runDDETestR %2 testdata\opti14\ddeGriewangk%1props.txt %1 1> testresults\opti14\ddeGriewangk%1.out 2>&1
call runDSATestR %2 testdata\opti14\dsaGriewangk%1props.txt %1 1> testresults\opti14\dsaGriewangk%1.out 2>&1
call runDPSOTestR %2 testdata\opti14\dpsoGriewangk%1props.txt %1 1> testresults\opti14\dpsoGriewangk%1.out 2>&1
call runDMCSTestR %2 testdata\opti14\dmcsGriewangk%1props.txt %1 1> testresults\opti14\dmcsGriewangk%1.out 2>&1
call runASDTestR %2 testdata\opti14\asdGriewangk%1props.txt %1 1> testresults\opti14\asdGriewangk%1.out 2>&1
call runFCGTestR %2 testdata\opti14\fcgGriewangk%1props.txt %1 1> testresults\opti14\fcgGriewangk%1.out 2>&1
call runAVDTestR %2 testdata\opti14\avdGriewangk%1props.txt %1 1> testresults\opti14\avdGriewangk%1.out 2>&1
call runDFATestR %2 testdata\opti14\dfaGriewangk%1props.txt %1 1> testresults\opti14\dfaGriewangk%1.out 2>&1
call runDGATestR %2 testdata\opti14\dgafcgGriewangk%1props.txt %1 1> testresults\opti14\dgafcgGriewangk%1.out 2>&1
call runDEATestR %2 testdata\opti14\deafcgGriewangk%1props.txt %1 1> testresults\opti14\deafcgGriewangk%1.out 2>&1
call runDDETestR %2 testdata\opti14\ddefcgGriewangk%1props.txt %1 1> testresults\opti14\ddefcgGriewangk%1.out 2>&1
call runDSATestR %2 testdata\opti14\dsafcgGriewangk%1props.txt %1 1> testresults\opti14\dsafcgGriewangk%1.out 2>&1
call runDPSOTestR %2 testdata\opti14\dpsofcgGriewangk%1props.txt %1 1> testresults\opti14\dpsofcgGriewangk%1.out 2>&1
