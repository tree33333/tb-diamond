#!/bin/bash
export LANG=zh_CN.GBK

if [ $# -lt 1 ];
then
	echo "USAGE: $0 server.properties"
	exit 1
fi
LOGFILE=$(dirname $0)/../logs/pushit.log

nohup sh $(dirname $0)/pushit-run-class.sh com.taobao.pushit.server.PushitStartup $@ 2>&1 >>$LOGFILE &
tail $LOGFILE -f
