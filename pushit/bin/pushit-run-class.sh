#!/bin/bash

if [ $# -lt 1 ];
then
  echo "USAGE: $0 classname [opts]"
  exit 1
fi

base_dir=$(dirname $0)/..

for file in $base_dir/lib/*.jar;
do
  CLASSPATH=$CLASSPATH:$file
done

if [ -z "$META_OPTS" ]; then
  PUSHIT_OPTS="-Xmx1024m -server -Dcom.sun.management.jmxremote -Dnotify.useJMX=true "
fi

if [ -z "$JAVA_HOME" ]; then
  JAVA="java"
else
  JAVA="$JAVA_HOME/bin/java"
fi

$JAVA $PUSHIT_OPTS -cp $CLASSPATH $@