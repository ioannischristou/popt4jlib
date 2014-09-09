REM run batch tests on std optimization test functions to allow statistical 
REM analysis of results to see whether any heuristic is clearly superior.
REM The methods compared are: GA/EA/DE/SA/PS/MC/ASD/FCG/AVD/FA
REM plus the combined methods GA-AVD/EA-AVD/DE-AVD/SA-AVD/PS-AVD
REM The test functions are:
REM Norm2 only
REM All tests are run using 10,50,100, and 1000 dimensions (the first argument),
REM and are repeatedly run as many times as indicated in the second argument 
REM (each time changing the random seed of the run).
REM The first argument (#dimensions) is also presented as third argument to the runXXXTestR batch files
REM so that a maximum limit of 1000*(#dimensions) is imposed on the number of allowed function evaluations
REM of each run.
REM 1. Norm2
call runDGATestR %2 testdata\opti14\dgaNorm2%1props.txt %1 1> testresults\opti14\dgaNorm2%1.out 2>&1 
call runDEATestR %2 testdata\opti14\deaNorm2%1props.txt %1 1> testresults\opti14\deaNorm2%1.out 2>&1
call runDDETestR %2 testdata\opti14\ddeNorm2%1props.txt %1 1> testresults\opti14\ddeNorm2%1.out 2>&1
call runDSATestR %2 testdata\opti14\dsaNorm2%1props.txt %1 1> testresults\opti14\dsaNorm2%1.out 2>&1
call runDPSOTestR %2 testdata\opti14\dpsoNorm2%1props.txt %1 1> testresults\opti14\dpsoNorm2%1.out 2>&1
call runDMCSTestR %2 testdata\opti14\dmcsNorm2%1props.txt %1 1> testresults\opti14\dmcsNorm2%1.out 2>&1
call runASDTestR %2 testdata\opti14\asdNorm2%1props.txt %1 1> testresults\opti14\asdNorm2%1.out 2>&1
call runFCGTestR %2 testdata\opti14\fcgNorm2%1props.txt %1 1> testresults\opti14\fcgNorm2%1.out 2>&1
call runAVDTestR %2 testdata\opti14\avdNorm2%1props.txt %1 1> testresults\opti14\avdNorm2%1.out 2>&1
call runDFATestR %2 testdata\opti14\dfaNorm2%1props.txt %1 1> testresults\opti14\dfaNorm2%1.out 2>&1
call runDGATestR %2 testdata\opti14\dgafcgNorm2%1props.txt %1 1> testresults\opti14\dgafcgNorm2%1.out 2>&1
call runDEATestR %2 testdata\opti14\deafcgNorm2%1props.txt %1 1> testresults\opti14\deafcgNorm2%1.out 2>&1
call runDDETestR %2 testdata\opti14\ddefcgNorm2%1props.txt %1 1> testresults\opti14\ddefcgNorm2%1.out 2>&1
call runDSATestR %2 testdata\opti14\dsafcgNorm2%1props.txt %1 1> testresults\opti14\dsafcgNorm2%1.out 2>&1
call runDPSOTestR %2 testdata\opti14\dpsofcgNorm2%1props.txt %1 1> testresults\opti14\dpsofcgNorm2%1.out 2>&1
