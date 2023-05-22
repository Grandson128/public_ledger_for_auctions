#!/bin/bash

if [ $# -ne 1 ];
    then echo "Need one argument: Port"
    exit
fi

mvn clean install
mvn exec:java -Dexec.mainClass=Peer.PeerBoot -Dexec.args="$1"
