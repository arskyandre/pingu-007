#!/bin/bash
clear
javac -cp ".:*" *.java
java -cp ".:*" GameCore
rm -f *.class