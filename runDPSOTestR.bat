@ECHO OFF
SET L=(
IF %1 LSS 1 GOTO END
SET /A I = 1
SET L=%L% %I%
:CONT
IF "%I%" == "%1" GOTO END
SET /A I = %I% + 1
SET L=%L% %I%
IF NOT "%I%" == "%1" GOTO CONT
:END
SET L=%L% )
REM ECHO %L%

SET /A N = %3 * 1000

FOR %%I IN %L% DO CALL runDPSOTest.bat %2 %%I %N%