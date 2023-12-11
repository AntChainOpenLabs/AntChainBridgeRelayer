#!/bin/bash

bin=`dirname "${BASH_SOURCE-$0}"`
CLI_HOME=`cd "$bin"; pwd`

which java > /dev/null
if [ $? -eq 1 ]; then
    echo "no java installed. "
    exit 1
fi

PORT="${1:-8088}"
java -jar ${CLI_HOME}/../lib/r-cli.jar -p $PORT