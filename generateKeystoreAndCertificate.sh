#!/bin/sh

# Keystore password is pointless: https://gist.github.com/zach-klippenstein/4631307

if [ -f garage.jks -o -f garage.crt ]
then
  echo "Files garage.jks or garage.crt already exist!"
  echo "Remove them if you wish to make a new keystore and certificate."
  exit
fi

keytool -genkeypair -keyalg RSA -alias garage -keystore garage.jks -storepass pointlesspassword

keytool -export -alias garage -keystore garage.jks -file garage.cer -storepass pointlesspassword

echo "Now put garage.cer on a SD card and import into the Android app from the Settings screen."
