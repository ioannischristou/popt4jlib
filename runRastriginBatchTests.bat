REM run batch tests on std optimization test functions to allow statistical 
REM analysis of results to see whether any heuristic is clearly superior.
REM The methods compared are: GA/EA/DE/SA/PS/MC/ASD/FCG/AVD/FA
REM plus the combined methods GA-AVD/EA-AVD/DE-AVD/SA-AVD/PS-AVD
REM The test functions are:
REM Rastrigin only
REM All tests are run using 10,50,100, and 1000 dimensions (the first argument),
REM and are repeatedly run as many times as indicated in the second argument 
REM (each time changing the random seed of the run).
REM The first argument (#dimensions) is also presented as third argument to the runXXXTestR batch files
REM so that a maximum limit of 1000*(#dimensions) is imposed on the number of allowed function evaluations
REM of each run.
REM 1. Rastrigin
call runDGATestR %2 testdata\opti14\dgaRastrigin%1props.txt %1 1> testresults\opti14\dgaRastrigin%1.out 2>&1 
call runDEATestR %2 testdata\opti14\deaRastrigin%1props.txt %1 1> testresults\opti14\deaRastrigin%1.out 2>&1
call runDDETestR %2 testdata\opti14\ddeRastrigin%1props.txt %1 1> testresults\opti14\ddeRastrigin%1.out 2>&1
call runDSATestR %2 testdata\opti14\dsaRastrigin%1props.txt %1 1> testresults\opti14\dsaRastrigin%1.out 2>&1
call runDPSOTestR %2 testdata\opti14\dpsoRastrigin%1props.txt %1 1> testresults\opti14\dpsoRastrigin%1.out 2>&1
call runDMCSTestR %2 testdata\opti14\dmcsRastrigin%1props.txt %1 1> testresults\opti14\dmcsRastrigin%1.out 2>&1
call runASDTestR %2 testdata\opti14\asdRastrigin%1props.txt %1 1> testresults\opti14\asdRastrigin%1.out 2>&1
call runFCGTestR %2 testdata\opti14\fcgRastrigin%1props.txt %1 1> testresults\opti14\fcgRastrigin%1.out 2>&1
call runAVDTestR %2 testdata\opti14\avdRastrigin%1props.txt %1 1> testresults\opti14\avdRastrigin%1.out 2>&1
call runDFATestR %2 testdata\opti14\dfaRastrigin%1props.txt %1 1> testresults\opti14\dfaRastrigin%1.out 2>&1
call runDGATestR %2 testdata\opti14\dgafcgRastrigin%1props.txt %1 1> testresults\opti14\dgafcgRastrigin%1.out 2>&1
call runDEATestR %2 testdata\opti14\deafcgRastrigin%1props.txt %1 1> testresults\opti14\deafcgRastrigin%1.out 2>&1
call runDDETestR %2 testdata\opti14\ddefcgRastrigin%1props.txt %1 1> testresults\opti14\ddefcgRastrigin%1.out 2>&1
call runDSATestR %2 testdata\opti14\dsafcgRastrigin%1props.txt %1 1> testresults\opti14\dsafcgRastrigin%1.out 2>&1
call runDPSOTestR %2 testdata\opti14\dpsofcgRastrigin%1props.txt %1 1> testresults\opti14\dpsofcgRastrigin%1.out 2>&1
