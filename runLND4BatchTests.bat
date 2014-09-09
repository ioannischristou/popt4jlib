REM run batch tests on std optimization test functions to allow statistical 
REM analysis of results to see whether any heuristic is clearly superior.
REM The methods compared are: GA/EA/DE/SA/PS/MC/ASD/FCG/AVD/FA
REM plus the combined methods GA-AVD/EA-AVD/DE-AVD/SA-AVD/PS-AVD
REM The test functions are:
REM LND4 only
REM All tests are run using 10,50,100, and 1000 dimensions (the first argument),
REM and are repeatedly run as many times as indicated in the second argument 
REM (each time changing the random seed of the run).
REM The first argument (#dimensions) is also presented as third argument to the runXXXTestR batch files
REM so that a maximum limit of 1000*(#dimensions) is imposed on the number of allowed function evaluations
REM of each run.
REM 1. LND4
call runDGATestR %2 testdata\opti14\dgaLND4%1props.txt %1 1> testresults\opti14\dgaLND4%1.out 2>&1 
call runDEATestR %2 testdata\opti14\deaLND4%1props.txt %1 1> testresults\opti14\deaLND4%1.out 2>&1
call runDDETestR %2 testdata\opti14\ddeLND4%1props.txt %1 1> testresults\opti14\ddeLND4%1.out 2>&1
call runDSATestR %2 testdata\opti14\dsaLND4%1props.txt %1 1> testresults\opti14\dsaLND4%1.out 2>&1
call runDPSOTestR %2 testdata\opti14\dpsoLND4%1props.txt %1 1> testresults\opti14\dpsoLND4%1.out 2>&1
call runDMCSTestR %2 testdata\opti14\dmcsLND4%1props.txt %1 1> testresults\opti14\dmcsLND4%1.out 2>&1
call runASDTestR %2 testdata\opti14\asdLND4%1props.txt %1 1> testresults\opti14\asdLND4%1.out 2>&1
call runFCGTestR %2 testdata\opti14\fcgLND4%1props.txt %1 1> testresults\opti14\fcgLND4%1.out 2>&1
call runAVDTestR %2 testdata\opti14\avdLND4%1props.txt %1 1> testresults\opti14\avdLND4%1.out 2>&1
call runDFATestR %2 testdata\opti14\dfaLND4%1props.txt %1 1> testresults\opti14\dfaLND4%1.out 2>&1
call runDGATestR %2 testdata\opti14\dgafcgLND4%1props.txt %1 1> testresults\opti14\dgafcgLND4%1.out 2>&1
call runDEATestR %2 testdata\opti14\deafcgLND4%1props.txt %1 1> testresults\opti14\deafcgLND4%1.out 2>&1
call runDDETestR %2 testdata\opti14\ddefcgLND4%1props.txt %1 1> testresults\opti14\ddefcgLND4%1.out 2>&1
call runDSATestR %2 testdata\opti14\dsafcgLND4%1props.txt %1 1> testresults\opti14\dsafcgLND4%1.out 2>&1
call runDPSOTestR %2 testdata\opti14\dpsofcgLND4%1props.txt %1 1> testresults\opti14\dpsofcgLND4%1.out 2>&1
