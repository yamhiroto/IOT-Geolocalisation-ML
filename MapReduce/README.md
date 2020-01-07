# MAPREDUCE FROM SCRATCH

This project is an attempt to replicate the MapReduce framework from scratch using multithreading.
<u>It can be optimize way further</u> but at least the result is correct and it gives, in my opinion, a clear idea about the different steps used in such a popular paradigm.

Language used: java.

INTRO
MapReduce(lien wiki) is a paradigm developed in the 2013 in order to handle large volume of data. Its objective is to optimize execution time in distributing files on different computers to perform independent operations.

MapReduce consists in 4 main steps that are summarized in the below schema:
SPLIT(lien)
MAP
SHUFFLE
REDUCE

In this project, I've chosen to separate each step on purpose, in order to be able to measure each step running time.

![MapReduceImage](https://github.com/savoga/various_projects/blob/master/MapReduce/MapReducePic.png)

The project is structured in different programs:

Slave
The Slave is responsible for computing three steps on the machine it is launched.
Master
This is the main program used to launched the execution of splits, slave (map, shuffle, reduce) and concatenation of the results.
It has one main class Master.java where the main method is. It is where all steps are executed.
All steps are executed using multiple threads: I've chosen to create a class for each of them (although this could be factorize better):
ThreadCreateSplit, ThreadDeploySplit, ThreadMap, ThreadShuffle and ThreadReduce. The Partition class is used to split the initial file (first step). ThreadProcessBuilder is the class allowing to send linux command in ssh.

Clean
Used to clean all files on the machines that will be used for MapReduce. The clean simply loop on the machine list and remove the folder /savoga.
Deploy
This program send the Slave.jar on the different machines. It loops on the machine list and copy the file from local computer to remote ones.

SPLIT
The split step consist in two actions:
Split the initial text file "input.txt" into multiple files

There are numerous ways to split a files, I chose the most intuitive one for me:
- Spliting the initial file into words and putting them into an array
- Partitioning the array into chunks with same size
- For each chunk, create a file

Since each chunk are independent, the last part of this action is done using multiple threads.

Send the different split files to different machines
