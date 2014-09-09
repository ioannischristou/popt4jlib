REM run batch tests on std optimization test functions to allow statistical 
REM analysis of results to see whether any heuristic is clearly superior.
REM The methods compared are: GA/EA/DE/SA/PS/MC/ASD/FCG/AVD/FA
REM plus the combined methods GA-AVD/EA-AVD/DE-AVD/SA-AVD/PS-AVD
REM The test functions are:
REM TridNonlinear only
REM All tests are run using 10,50,100, and 1000 dimensions (the first argument),
REM and are repeatedly run as many times as indicated in the second argument 
REM (each time changing the random seed of the run).
REM The first argument (#dimensions) is also presented as third argument to the runXXXTestR batch files
REM so that a maximum limit of 1000*(#dimensions) is imposed on the number of allowed function evaluations
REM of each run.
REM 1. TridNonlinear
call runDGATestR %2 testdata\opti14\dgaTridNonlinear%1props.txt %1 1> testresults\opti14\dgaTridNonlinear%1.out 2>&1 
call runDEATestR %2 testdata\opti14\deaTridNonlinear%1props.txt %1 1> testresults\opti14\deaTridNonlinear%1.out 2>&1
call runDDETestR %2 testdata\opti14\ddeTridNonlinear%1props.txt %1 1> testresults\opti14\ddeTridNonlinear%1.out 2>&1
call runDSATestR %2 testdata\opti14\dsaTridNonlinear%1props.txt %1 1> testresults\opti14\dsaTridNonlinear%1.out 2>&1
call runDPSOTestR %2 testdata\opti14\dpsoTridNonlinear%1props.txt %1 1> testresults\opti14\dpsoTridNonlinear%1.out 2>&1
call runDMCSTestR %2 testdata\opti14\dmcsTridNonlinear%1props.txt %1 1> testresults\opti14\dmcsTridNonlinear%1.out 2>&1
call runASDTestR %2 testdata\opti14\asdTridNonlinear%1props.txt %1 1> testresults\opti14\asdTridNonlinear%1.out 2>&1
call runFCGTestR %2 testdata\opti14\fcgTridNonlinear%1props.txt %1 1> testresults\opti14\fcgTridNonlinear%1.out 2>&1
call runAVDTestR %2 testdata\opti14\avdTridNonlinear%1props.txt %1 1> testresults\opti14\avdTridNonlinear%1.out 2>&1
call runDFATestR %2 testdata\opti14\dfaTridNonlinear%1props.txt %1 1> testresults\opti14\dfaTridNonlinear%1.out 2>&1
call runDGATestR %2 testdata\opti14\dgafcgTridNonlinear%1props.txt %1 1> testresults\opti14\dgafcgTridNonlinear%1.out 2>&1
call runDEATestR %2 testdata\opti14\deafcgTridNonlinear%1props.txt %1 1> testresults\opti14\deafcgTridNonlinear%1.out 2>&1
call runDDETestR %2 testdata\opti14\ddefcgTridNonlinear%1props.txt %1 1> testresults\opti14\ddefcgTridNonlinear%1.out 2>&1
call runDSATestR %2 testdata\opti14\dsafcgTridNonlinear%1props.txt %1 1> testresults\opti14\dsafcgTridNonlinear%1.out 2>&1
call runDPSOTestR %2 testdata\opti14\dpsofcgTridNonlinear%1props.txt %1 1> testresults\opti14\dpsofcgTridNonlinear%1.out 2>&1
