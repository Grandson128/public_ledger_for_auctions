#!/bin/bash

if [ $# -ne 2 ];
    then echo "Need two argument: IP, Port"
    exit
fi

mvn clean install
mvn exec:java -Dexec.mainClass=Peer.Peer -Dexec.args="$1 $2"
