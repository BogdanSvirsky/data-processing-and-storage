#!/bin/bash

mkdir secret
openssl genrsa -out secret/server_private_key.pem 8192

javac -cp .:lib/bouncycastle/* src/Main.java src/Server.java src/Client.java -d build