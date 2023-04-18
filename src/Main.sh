#!/bin/bash
#SBATCH -p main # queue name
#SBATCH -N1  # number of computers (1 computer for OpenMP or thread processing)
#SBATCH -c8 # number of cores for 1 computer
javac Main.java
java Main 1 10000000 64 false
java Main 2 10000000 64 false
java Main 4 10000000 64 false
java Main 8 10000000 64 false
