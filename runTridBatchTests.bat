REM run batch tests on std optimization test functions to allow statistical 
REM analysis of results to see whether any heuristic is clearly superior.
REM The methods compared are: GA/EA/DE/SA/PS/MC/ASD/FCG/AVD/FA
REM plus the combined methods GA-AVD/EA-AVD/DE-AVD/SA-AVD/PS-AVD
REM The test functions are:
REM Trid only
REM All tests are run using 10,50,100, and 1000 dimensions (the first argument),
REM and are repeatedly run as many times as indicated in the second argument 
REM (each time changing the random seed of the run).
REM The first argument (#dimensions) is also presented as third argument to the runXXXTestR batch files
REM so that a maximum limit of 1000*(#dimensions) is imposed on the number of allowed function evaluations
REM of each run.
REM 1. Trid
call runDGATestR %2 testdata\opti14\dgaTrid%1props.txt %1 1> testresults\opti14\dgaTrid%1.out 2>&1 
call runDEATestR %2 testdata\opti14\deaTrid%1props.txt %1 1> testresults\opti14\deaTrid%1.out 2>&1
call runDDETestR %2 testdata\opti14\ddeTrid%1props.txt %1 1> testresults\opti14\ddeTrid%1.out 2>&1
call runDSATestR %2 testdata\opti14\dsaTrid%1props.txt %1 1> testresults\opti14\dsaTrid%1.out 2>&1
call runDPSOTestR %2 testdata\opti14\dpsoTrid%1props.txt %1 1> testresults\opti14\dpsoTrid%1.out 2>&1
call runDMCSTestR %2 testdata\opti14\dmcsTrid%1props.txt %1 1> testresults\opti14\dmcsTrid%1.out 2>&1
call runASDTestR %2 testdata\opti14\asdTrid%1props.txt %1 1> testresults\opti14\asdTrid%1.out 2>&1
call runFCGTestR %2 testdata\opti14\fcgTrid%1props.txt %1 1> testresults\opti14\fcgTrid%1.out 2>&1
call runAVDTestR %2 testdata\opti14\avdTrid%1props.txt %1 1> testresults\opti14\avdTrid%1.out 2>&1
call runDFATestR %2 testdata\opti14\dfaTrid%1props.txt %1 1> testresults\opti14\dfaTrid%1.out 2>&1
call runDGATestR %2 testdata\opti14\dgafcgTrid%1props.txt %1 1> testresults\opti14\dgafcgTrid%1.out 2>&1
call runDEATestR %2 testdata\opti14\deafcgTrid%1props.txt %1 1> testresults\opti14\deafcgTrid%1.out 2>&1
call runDDETestR %2 testdata\opti14\ddefcgTrid%1props.txt %1 1> testresults\opti14\ddefcgTrid%1.out 2>&1
call runDSATestR %2 testdata\opti14\dsafcgTrid%1props.txt %1 1> testresults\opti14\dsafcgTrid%1.out 2>&1
call runDPSOTestR %2 testdata\opti14\dpsofcgTrid%1props.txt %1 1> testresults\opti14\dpsofcgTrid%1.out 2>&1
