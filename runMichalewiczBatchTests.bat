REM run batch tests on std optimization test functions to allow statistical 
REM analysis of results to see whether any heuristic is clearly superior.
REM The methods compared are: GA/EA/DE/SA/PS/MC/ASD/FCG/AVD/FA
REM plus the combined methods GA-AVD/EA-AVD/DE-AVD/SA-AVD/PS-AVD
REM The test functions are:
REM Michalewicz only
REM All tests are run using 10,50,100, and 1000 dimensions (the first argument),
REM and are repeatedly run as many times as indicated in the second argument 
REM (each time changing the random seed of the run).
REM The first argument (#dimensions) is also presented as third argument to the runXXXTestR batch files
REM so that a maximum limit of 1000*(#dimensions) is imposed on the number of allowed function evaluations
REM of each run.
REM 1. Michalewicz
call runDGATestR %2 testdata\opti14\dgaMichalewicz%1props.txt %1 1> testresults\opti14\dgaMichalewicz%1.out 2>&1 
call runDEATestR %2 testdata\opti14\deaMichalewicz%1props.txt %1 1> testresults\opti14\deaMichalewicz%1.out 2>&1
call runDDETestR %2 testdata\opti14\ddeMichalewicz%1props.txt %1 1> testresults\opti14\ddeMichalewicz%1.out 2>&1
call runDSATestR %2 testdata\opti14\dsaMichalewicz%1props.txt %1 1> testresults\opti14\dsaMichalewicz%1.out 2>&1
call runDPSOTestR %2 testdata\opti14\dpsoMichalewicz%1props.txt %1 1> testresults\opti14\dpsoMichalewicz%1.out 2>&1
call runDMCSTestR %2 testdata\opti14\dmcsMichalewicz%1props.txt %1 1> testresults\opti14\dmcsMichalewicz%1.out 2>&1
call runASDTestR %2 testdata\opti14\asdMichalewicz%1props.txt %1 1> testresults\opti14\asdMichalewicz%1.out 2>&1
call runFCGTestR %2 testdata\opti14\fcgMichalewicz%1props.txt %1 1> testresults\opti14\fcgMichalewicz%1.out 2>&1
call runAVDTestR %2 testdata\opti14\avdMichalewicz%1props.txt %1 1> testresults\opti14\avdMichalewicz%1.out 2>&1
call runDFATestR %2 testdata\opti14\dfaMichalewicz%1props.txt %1 1> testresults\opti14\dfaMichalewicz%1.out 2>&1
call runDGATestR %2 testdata\opti14\dgafcgMichalewicz%1props.txt %1 1> testresults\opti14\dgafcgMichalewicz%1.out 2>&1
call runDEATestR %2 testdata\opti14\deafcgMichalewicz%1props.txt %1 1> testresults\opti14\deafcgMichalewicz%1.out 2>&1
call runDDETestR %2 testdata\opti14\ddefcgMichalewicz%1props.txt %1 1> testresults\opti14\ddefcgMichalewicz%1.out 2>&1
call runDSATestR %2 testdata\opti14\dsafcgMichalewicz%1props.txt %1 1> testresults\opti14\dsafcgMichalewicz%1.out 2>&1
call runDPSOTestR %2 testdata\opti14\dpsofcgMichalewicz%1props.txt %1 1> testresults\opti14\dpsofcgMichalewicz%1.out 2>&1
