#!/bin/sh

# Keystore password is pointless: https://gist.github.com/zach-klippenstein/4631307

if [ -f garage.jks -o -f garage.crt ]
then
  echo "Files garage.jks or garage.crt already exist!"
  echo "Remove them if you wish to make a new keystore and certificate."
  exit
fi

while true
do
  stty -echo
  read -p "Enter password for secret key: " pass
  stty echo
  echo "" # newline
  [ -z "$pass" ] || break # check not empty
done

keytool -genkeypair -keyalg RSA -alias garage -keystore garage.jks -storepass pointlesspassword -keypass "$pass"

keytool -export -alias garage -keystore garage.jks -file garage.cer -storepass pointlesspassword -keypass "$pass"

echo "Now put garage.cer on a SD card and import into the Android app from the Settings screen."
