#!/bin/sh

BASEDIR=$(dirname $0)
cd "$BASEDIR"

exec java -Djava.security.properties=GarageDoorOpener.properties -jar "$BASEDIR"/build/libs/GarageDoorOpener-0.9.jar 
