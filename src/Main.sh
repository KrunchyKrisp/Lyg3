#!/bin/bash
#SBATCH -p main # queue name
#SBATCH -N1  # number of computers (1 computer for OpenMP or thread processing)
#SBATCH -c8 # number of cores for 1 computer
javac Main.java
java Main
