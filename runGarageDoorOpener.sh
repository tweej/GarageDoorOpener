#!/bin/sh

BASEDIR=$(dirname $0)
cd "$BASEDIR"

exec java -Djava.security.properties=GarageDoorOpener.properties -jar "$BASEDIR"/build/libs/GarageDoorOpener-1.0.jar 
