REM run batch tests on std optimization test functions to allow statistical 
REM analysis of results to see whether any heuristic is clearly superior.
REM The methods compared are: GA/EA/DE/SA/PS/MC/ASD/FCG/AVD/FA
REM plus the combined methods GA-AVD/EA-AVD/DE-AVD/SA-AVD/PS-AVD
REM The test functions are:
REM Ackley only
REM All tests are run using 10,50,100, and 1000 dimensions (the first argument),
REM and are repeatedly run as many times as indicated in the second argument 
REM (each time changing the random seed of the run).
REM The first argument (#dimensions) is also presented as third argument to the runXXXTestR batch files
REM so that a maximum limit of 1000*(#dimensions) is imposed on the number of allowed function evaluations
REM of each run.
REM 1. Ackley
call runDGATestR %2 testdata\opti14\dgaAckley%1props.txt %1 1> testresults\opti14\dgaAckley%1.out 2>&1 
call runDEATestR %2 testdata\opti14\deaAckley%1props.txt %1 1> testresults\opti14\deaAckley%1.out 2>&1
call runDDETestR %2 testdata\opti14\ddeAckley%1props.txt %1 1> testresults\opti14\ddeAckley%1.out 2>&1
call runDSATestR %2 testdata\opti14\dsaAckley%1props.txt %1 1> testresults\opti14\dsaAckley%1.out 2>&1
call runDPSOTestR %2 testdata\opti14\dpsoAckley%1props.txt %1 1> testresults\opti14\dpsoAckley%1.out 2>&1
call runDMCSTestR %2 testdata\opti14\dmcsAckley%1props.txt %1 1> testresults\opti14\dmcsAckley%1.out 2>&1
call runASDTestR %2 testdata\opti14\asdAckley%1props.txt %1 1> testresults\opti14\asdAckley%1.out 2>&1
call runFCGTestR %2 testdata\opti14\fcgAckley%1props.txt %1 1> testresults\opti14\fcgAckley%1.out 2>&1
call runAVDTestR %2 testdata\opti14\avdAckley%1props.txt %1 1> testresults\opti14\avdAckley%1.out 2>&1
call runDFATestR %2 testdata\opti14\dfaAckley%1props.txt %1 1> testresults\opti14\dfaAckley%1.out 2>&1
call runDGATestR %2 testdata\opti14\dgafcgAckley%1props.txt %1 1> testresults\opti14\dgafcgAckley%1.out 2>&1
call runDEATestR %2 testdata\opti14\deafcgAckley%1props.txt %1 1> testresults\opti14\deafcgAckley%1.out 2>&1
call runDDETestR %2 testdata\opti14\ddefcgAckley%1props.txt %1 1> testresults\opti14\ddefcgAckley%1.out 2>&1
call runDSATestR %2 testdata\opti14\dsafcgAckley%1props.txt %1 1> testresults\opti14\dsafcgAckley%1.out 2>&1
call runDPSOTestR %2 testdata\opti14\dpsofcgAckley%1props.txt %1 1> testresults\opti14\dpsofcgAckley%1.out 2>&1
