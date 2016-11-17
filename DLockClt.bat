echo off
if not exist C:\temp\NUL mkdir C:\temp
REM java -cp ./dist/popt4jlib.jar utils.DLockClientJFrame 10.100.209.240
java -cp ./dist/popt4jlib.jar utils.DLockClientJFrame %1