echo off
REM java -cp <classpath> graph.packing.BBGASPPacker <graphfile> <propsfile>
java -Xmx5800m -cp ./dist/popt4jlib.jar graph.packing.BBGASPPacker %1 %2 100000