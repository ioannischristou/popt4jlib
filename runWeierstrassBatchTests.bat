REM run batch tests on std optimization test functions to allow statistical 
REM analysis of results to see whether any heuristic is clearly superior.
REM The methods compared are: GA/EA/DE/SA/PS/MC/ASD/FCG/AVD/FA
REM plus the combined methods GA-AVD/EA-AVD/DE-AVD/SA-AVD/PS-AVD
REM The test functions are:
REM Weierstrass only
REM All tests are run using 10,50,100, and 1000 dimensions (the first argument),
REM and are repeatedly run as many times as indicated in the second argument 
REM (each time changing the random seed of the run).
REM The first argument (#dimensions) is also presented as third argument to the runXXXTestR batch files
REM so that a maximum limit of 1000*(#dimensions) is imposed on the number of allowed function evaluations
REM of each run.
REM 1. Weierstrass
call runDGATestR %2 testdata\opti14\dgaWeierstrass%1props.txt %1 1> testresults\opti14\dgaWeierstrass%1.out 2>&1 
call runDEATestR %2 testdata\opti14\deaWeierstrass%1props.txt %1 1> testresults\opti14\deaWeierstrass%1.out 2>&1
call runDDETestR %2 testdata\opti14\ddeWeierstrass%1props.txt %1 1> testresults\opti14\ddeWeierstrass%1.out 2>&1
call runDSATestR %2 testdata\opti14\dsaWeierstrass%1props.txt %1 1> testresults\opti14\dsaWeierstrass%1.out 2>&1
call runDPSOTestR %2 testdata\opti14\dpsoWeierstrass%1props.txt %1 1> testresults\opti14\dpsoWeierstrass%1.out 2>&1
call runDMCSTestR %2 testdata\opti14\dmcsWeierstrass%1props.txt %1 1> testresults\opti14\dmcsWeierstrass%1.out 2>&1
call runASDTestR %2 testdata\opti14\asdWeierstrass%1props.txt %1 1> testresults\opti14\asdWeierstrass%1.out 2>&1
call runFCGTestR %2 testdata\opti14\fcgWeierstrass%1props.txt %1 1> testresults\opti14\fcgWeierstrass%1.out 2>&1
call runAVDTestR %2 testdata\opti14\avdWeierstrass%1props.txt %1 1> testresults\opti14\avdWeierstrass%1.out 2>&1
call runDFATestR %2 testdata\opti14\dfaWeierstrass%1props.txt %1 1> testresults\opti14\dfaWeierstrass%1.out 2>&1
call runDGATestR %2 testdata\opti14\dgafcgWeierstrass%1props.txt %1 1> testresults\opti14\dgafcgWeierstrass%1.out 2>&1
call runDEATestR %2 testdata\opti14\deafcgWeierstrass%1props.txt %1 1> testresults\opti14\deafcgWeierstrass%1.out 2>&1
call runDDETestR %2 testdata\opti14\ddefcgWeierstrass%1props.txt %1 1> testresults\opti14\ddefcgWeierstrass%1.out 2>&1
call runDSATestR %2 testdata\opti14\dsafcgWeierstrass%1props.txt %1 1> testresults\opti14\dsafcgWeierstrass%1.out 2>&1
call runDPSOTestR %2 testdata\opti14\dpsofcgWeierstrass%1props.txt %1 1> testresults\opti14\dpsofcgWeierstrass%1.out 2>&1
