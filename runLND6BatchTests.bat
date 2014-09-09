REM run batch tests on std optimization test functions to allow statistical 
REM analysis of results to see whether any heuristic is clearly superior.
REM The methods compared are: GA/EA/DE/SA/PS/MC/ASD/FCG/AVD/FA
REM plus the combined methods GA-AVD/EA-AVD/DE-AVD/SA-AVD/PS-AVD
REM The test functions are:
REM LND6 only
REM All tests are run using 10,50,100, and 1000 dimensions (the first argument),
REM and are repeatedly run as many times as indicated in the second argument 
REM (each time changing the random seed of the run).
REM The first argument (#dimensions) is also presented as third argument to the runXXXTestR batch files
REM so that a maximum limit of 1000*(#dimensions) is imposed on the number of allowed function evaluations
REM of each run.
REM 1. LND6
call runDGATestR %2 testdata\opti14\dgaLND6%1props.txt %1 1> testresults\opti14\dgaLND6%1.out 2>&1 
call runDEATestR %2 testdata\opti14\deaLND6%1props.txt %1 1> testresults\opti14\deaLND6%1.out 2>&1
call runDDETestR %2 testdata\opti14\ddeLND6%1props.txt %1 1> testresults\opti14\ddeLND6%1.out 2>&1
call runDSATestR %2 testdata\opti14\dsaLND6%1props.txt %1 1> testresults\opti14\dsaLND6%1.out 2>&1
call runDPSOTestR %2 testdata\opti14\dpsoLND6%1props.txt %1 1> testresults\opti14\dpsoLND6%1.out 2>&1
call runDMCSTestR %2 testdata\opti14\dmcsLND6%1props.txt %1 1> testresults\opti14\dmcsLND6%1.out 2>&1
call runASDTestR %2 testdata\opti14\asdLND6%1props.txt %1 1> testresults\opti14\asdLND6%1.out 2>&1
call runFCGTestR %2 testdata\opti14\fcgLND6%1props.txt %1 1> testresults\opti14\fcgLND6%1.out 2>&1
call runAVDTestR %2 testdata\opti14\avdLND6%1props.txt %1 1> testresults\opti14\avdLND6%1.out 2>&1
call runDFATestR %2 testdata\opti14\dfaLND6%1props.txt %1 1> testresults\opti14\dfaLND6%1.out 2>&1
call runDGATestR %2 testdata\opti14\dgafcgLND6%1props.txt %1 1> testresults\opti14\dgafcgLND6%1.out 2>&1
call runDEATestR %2 testdata\opti14\deafcgLND6%1props.txt %1 1> testresults\opti14\deafcgLND6%1.out 2>&1
call runDDETestR %2 testdata\opti14\ddefcgLND6%1props.txt %1 1> testresults\opti14\ddefcgLND6%1.out 2>&1
call runDSATestR %2 testdata\opti14\dsafcgLND6%1props.txt %1 1> testresults\opti14\dsafcgLND6%1.out 2>&1
call runDPSOTestR %2 testdata\opti14\dpsofcgLND6%1props.txt %1 1> testresults\opti14\dpsofcgLND6%1.out 2>&1
