cls
@echo off
javac -cp ".;*" *.java
java -cp ".;*" GameCore
del "*.class"