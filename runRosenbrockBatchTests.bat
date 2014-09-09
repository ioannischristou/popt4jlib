REM run batch tests on std optimization test functions to allow statistical 
REM analysis of results to see whether any heuristic is clearly superior.
REM The methods compared are: GA/EA/DE/SA/PS/MC/ASD/FCG/AVD/FA
REM plus the combined methods GA-AVD/EA-AVD/DE-AVD/SA-AVD/PS-AVD
REM The test functions are:
REM Rosenbrock only
REM All tests are run using 10,50,100, and 1000 dimensions (the first argument),
REM and are repeatedly run as many times as indicated in the second argument 
REM (each time changing the random seed of the run).
REM The first argument (#dimensions) is also presented as third argument to the runXXXTestR batch files
REM so that a maximum limit of 1000*(#dimensions) is imposed on the number of allowed function evaluations
REM of each run.
REM 1. Rosenbrock
call runDGATestR %2 testdata\opti14\dgaRosenbrock%1props.txt %1 1> testresults\opti14\dgaRosenbrock%1.out 2>&1 
call runDEATestR %2 testdata\opti14\deaRosenbrock%1props.txt %1 1> testresults\opti14\deaRosenbrock%1.out 2>&1
call runDDETestR %2 testdata\opti14\ddeRosenbrock%1props.txt %1 1> testresults\opti14\ddeRosenbrock%1.out 2>&1
call runDSATestR %2 testdata\opti14\dsaRosenbrock%1props.txt %1 1> testresults\opti14\dsaRosenbrock%1.out 2>&1
call runDPSOTestR %2 testdata\opti14\dpsoRosenbrock%1props.txt %1 1> testresults\opti14\dpsoRosenbrock%1.out 2>&1
call runDMCSTestR %2 testdata\opti14\dmcsRosenbrock%1props.txt %1 1> testresults\opti14\dmcsRosenbrock%1.out 2>&1
call runASDTestR %2 testdata\opti14\asdRosenbrock%1props.txt %1 1> testresults\opti14\asdRosenbrock%1.out 2>&1
call runFCGTestR %2 testdata\opti14\fcgRosenbrock%1props.txt %1 1> testresults\opti14\fcgRosenbrock%1.out 2>&1
call runAVDTestR %2 testdata\opti14\avdRosenbrock%1props.txt %1 1> testresults\opti14\avdRosenbrock%1.out 2>&1
call runDFATestR %2 testdata\opti14\dfaRosenbrock%1props.txt %1 1> testresults\opti14\dfaRosenbrock%1.out 2>&1
call runDGATestR %2 testdata\opti14\dgafcgRosenbrock%1props.txt %1 1> testresults\opti14\dgafcgRosenbrock%1.out 2>&1
call runDEATestR %2 testdata\opti14\deafcgRosenbrock%1props.txt %1 1> testresults\opti14\deafcgRosenbrock%1.out 2>&1
call runDDETestR %2 testdata\opti14\ddefcgRosenbrock%1props.txt %1 1> testresults\opti14\ddefcgRosenbrock%1.out 2>&1
call runDSATestR %2 testdata\opti14\dsafcgRosenbrock%1props.txt %1 1> testresults\opti14\dsafcgRosenbrock%1.out 2>&1
call runDPSOTestR %2 testdata\opti14\dpsofcgRosenbrock%1props.txt %1 1> testresults\opti14\dpsofcgRosenbrock%1.out 2>&1
