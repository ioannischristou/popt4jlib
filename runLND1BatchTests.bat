REM run batch tests on std optimization test functions to allow statistical 
REM analysis of results to see whether any heuristic is clearly superior.
REM The methods compared are: GA/EA/DE/SA/PS/MC/ASD/FCG/AVD/FA
REM plus the combined methods GA-AVD/EA-AVD/DE-AVD/SA-AVD/PS-AVD
REM The test functions are:
REM LND1 only
REM All tests are run using 10,50,100, and 1000 dimensions (the first argument),
REM and are repeatedly run as many times as indicated in the second argument 
REM (each time changing the random seed of the run).
REM The first argument (#dimensions) is also presented as third argument to the runXXXTestR batch files
REM so that a maximum limit of 1000*(#dimensions) is imposed on the number of allowed function evaluations
REM of each run.
REM 1. LND1
call runDGATestR %2 testdata\opti14\dgaLND1%1props.txt %1 1> testresults\opti14\dgaLND1%1.out 2>&1 
call runDEATestR %2 testdata\opti14\deaLND1%1props.txt %1 1> testresults\opti14\deaLND1%1.out 2>&1
call runDDETestR %2 testdata\opti14\ddeLND1%1props.txt %1 1> testresults\opti14\ddeLND1%1.out 2>&1
call runDSATestR %2 testdata\opti14\dsaLND1%1props.txt %1 1> testresults\opti14\dsaLND1%1.out 2>&1
call runDPSOTestR %2 testdata\opti14\dpsoLND1%1props.txt %1 1> testresults\opti14\dpsoLND1%1.out 2>&1
call runDMCSTestR %2 testdata\opti14\dmcsLND1%1props.txt %1 1> testresults\opti14\dmcsLND1%1.out 2>&1
call runASDTestR %2 testdata\opti14\asdLND1%1props.txt %1 1> testresults\opti14\asdLND1%1.out 2>&1
call runFCGTestR %2 testdata\opti14\fcgLND1%1props.txt %1 1> testresults\opti14\fcgLND1%1.out 2>&1
call runAVDTestR %2 testdata\opti14\avdLND1%1props.txt %1 1> testresults\opti14\avdLND1%1.out 2>&1
call runDFATestR %2 testdata\opti14\dfaLND1%1props.txt %1 1> testresults\opti14\dfaLND1%1.out 2>&1
call runDGATestR %2 testdata\opti14\dgafcgLND1%1props.txt %1 1> testresults\opti14\dgafcgLND1%1.out 2>&1
call runDEATestR %2 testdata\opti14\deafcgLND1%1props.txt %1 1> testresults\opti14\deafcgLND1%1.out 2>&1
call runDDETestR %2 testdata\opti14\ddefcgLND1%1props.txt %1 1> testresults\opti14\ddefcgLND1%1.out 2>&1
call runDSATestR %2 testdata\opti14\dsafcgLND1%1props.txt %1 1> testresults\opti14\dsafcgLND1%1.out 2>&1
call runDPSOTestR %2 testdata\opti14\dpsofcgLND1%1props.txt %1 1> testresults\opti14\dpsofcgLND1%1.out 2>&1
