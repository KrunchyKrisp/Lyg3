#!/bin/bash
#SBATCH -p main # queue name
#SBATCH -N1  # number of computers (1 computer for OpenMP or thread processing)
#SBATCH -c8 # number of cores for 1 computer
iconv -f utf8 -t US-ASCII//TRANSLIT Main.java > Main2.java
rm Main.java
mv Main2.java Main.java
javac Main.java
java Main
