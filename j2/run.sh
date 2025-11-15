#!/bin/bash

mkdir build

javac src/Main.java src/SynchronizedList.java -d build &&
java -cp build Main