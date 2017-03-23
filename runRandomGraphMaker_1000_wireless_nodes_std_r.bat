echo off
REM java -cp dist\popt4jlib.jar utils.RandomGraphMaker <numnodes> <x> <y> <r> <graphfilename> [uniform?(false)] [rndseed(0)] [jplotfile(null)] [createMWIS?(false)]
java -cp dist\popt4jlib.jar utils.RandomGraphMaker 1000 1 1 0.053 testdata\mwis_manet_1000_0.053.graph false 7 null true
java -cp dist\popt4jlib.jar utils.RandomGraphMaker 1000 1 1 0.054 testdata\mwis_manet_1000_0.054.graph false 7 null true
java -cp dist\popt4jlib.jar utils.RandomGraphMaker 1000 1 1 0.055 testdata\mwis_manet_1000_0.055.graph false 7 null true
java -cp dist\popt4jlib.jar utils.RandomGraphMaker 1000 1 1 0.06 testdata\mwis_manet_1000_0.06.graph false 7 null true
java -Xmx11000m -cp dist\popt4jlib.jar utils.RandomGraphMaker 1000 1 1 0.065 testdata\mwis_manet_1000_0.065.graph false 7 null true
java -cp dist\popt4jlib.jar utils.RandomGraphMaker 1000 1 1 0.07 testdata\mwis_manet_1000_0.07.graph false 7 null true
java -cp dist\popt4jlib.jar utils.RandomGraphMaker 1000 1 1 0.08 testdata\mwis_manet_1000_0.08.graph false 7 null true
java -cp dist\popt4jlib.jar utils.RandomGraphMaker 1000 1 1 0.09 testdata\mwis_manet_1000_0.09.graph false 7 null true
java -cp dist\popt4jlib.jar utils.RandomGraphMaker 1000 1 1 0.1 testdata\mwis_manet_1000_0.1.graph false 7 null true
java -Xmx11000m -cp dist\popt4jlib.jar utils.RandomGraphMaker 1000 1 1 0.12 testdata\mwis_manet_1000_0.12.graph false 7 null true
echo on