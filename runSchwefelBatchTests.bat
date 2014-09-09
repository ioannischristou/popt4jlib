REM run batch tests on std optimization test functions to allow statistical 
REM analysis of results to see whether any heuristic is clearly superior.
REM The methods compared are: GA/EA/DE/SA/PS/MC/ASD/FCG/AVD/FA
REM plus the combined methods GA-AVD/EA-AVD/DE-AVD/SA-AVD/PS-AVD
REM The test functions are:
REM Schwefel only
REM All tests are run using 10,50,100, and 1000 dimensions (the first argument),
REM and are repeatedly run as many times as indicated in the second argument 
REM (each time changing the random seed of the run).
REM The first argument (#dimensions) is also presented as third argument to the runXXXTestR batch files
REM so that a maximum limit of 1000*(#dimensions) is imposed on the number of allowed function evaluations
REM of each run.
REM 1. Schwefel
call runDGATestR %2 testdata\opti14\dgaSchwefel%1props.txt %1 1> testresults\opti14\dgaSchwefel%1.out 2>&1 
call runDEATestR %2 testdata\opti14\deaSchwefel%1props.txt %1 1> testresults\opti14\deaSchwefel%1.out 2>&1
call runDDETestR %2 testdata\opti14\ddeSchwefel%1props.txt %1 1> testresults\opti14\ddeSchwefel%1.out 2>&1
call runDSATestR %2 testdata\opti14\dsaSchwefel%1props.txt %1 1> testresults\opti14\dsaSchwefel%1.out 2>&1
call runDPSOTestR %2 testdata\opti14\dpsoSchwefel%1props.txt %1 1> testresults\opti14\dpsoSchwefel%1.out 2>&1
call runDMCSTestR %2 testdata\opti14\dmcsSchwefel%1props.txt %1 1> testresults\opti14\dmcsSchwefel%1.out 2>&1
call runASDTestR %2 testdata\opti14\asdSchwefel%1props.txt %1 1> testresults\opti14\asdSchwefel%1.out 2>&1
call runFCGTestR %2 testdata\opti14\fcgSchwefel%1props.txt %1 1> testresults\opti14\fcgSchwefel%1.out 2>&1
call runAVDTestR %2 testdata\opti14\avdSchwefel%1props.txt %1 1> testresults\opti14\avdSchwefel%1.out 2>&1
call runDFATestR %2 testdata\opti14\dfaSchwefel%1props.txt %1 1> testresults\opti14\dfaSchwefel%1.out 2>&1
call runDGATestR %2 testdata\opti14\dgafcgSchwefel%1props.txt %1 1> testresults\opti14\dgafcgSchwefel%1.out 2>&1
call runDEATestR %2 testdata\opti14\deafcgSchwefel%1props.txt %1 1> testresults\opti14\deafcgSchwefel%1.out 2>&1
call runDDETestR %2 testdata\opti14\ddefcgSchwefel%1props.txt %1 1> testresults\opti14\ddefcgSchwefel%1.out 2>&1
call runDSATestR %2 testdata\opti14\dsafcgSchwefel%1props.txt %1 1> testresults\opti14\dsafcgSchwefel%1.out 2>&1
call runDPSOTestR %2 testdata\opti14\dpsofcgSchwefel%1props.txt %1 1> testresults\opti14\dpsofcgSchwefel%1.out 2>&1
