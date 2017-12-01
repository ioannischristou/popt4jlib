#!/bin/bash
cmd_acc="source ./DAccSrv.sh"
eval "${cmd_acc}" &>/dev/null &!

cmd_dcond="source ./DCondSrv.sh"
eval "${cmd_dcond}" &>/dev/null &!

cmd_pdasrv="source ./PDASrv.sh 7980 7981 true"
eval "${cmd_pdasrv}" &>/dev/null &!

cmd_pdawrk="source ./PDAWrk.sh 32 localhost 7980 true"
eval "${cmd_pdawrk}" &>/dev/null &!

read -n1 -r -p "Verify servers" key

cmd_dbbgasppacker="java -Xmx5000m -cp ./dist/popt4jlib.jar graph.packing.DBBGASPPacker testdata/frb100-40.mis.graph testdata/dbbgasp_props_4_deepthought.txt"
eval "${cmd_dbbgasppacker}" &>dbbgasppacker.out &!

