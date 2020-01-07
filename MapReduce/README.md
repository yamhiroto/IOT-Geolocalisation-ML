# MAPREDUCE FROM SCRATCH

This project is an attempt to replicate the MapReduce framework from scratch using multithreading. The example will be a simple wordcount.
Please note that this project __can be optimize way further__ but at least the result is correct and it gives, in my opinion, a clear idea about the different steps used in such a popular algorithm.

Language used: java.

## Intro

[MapReduce](https://en.wikipedia.org/wiki/MapReduce) is a paradigm created and first used by Google in 2004 in order to handle large volume of data. Its objective is to optimize execution time in distributing files on different computers to perform independent operations.

MapReduce consists in 4 main steps that are summarized in the below schema:
<!-- TOC -->
- [SPLIT](#split)
- [MAP](#map)
- [SHUFFLE](#shuffle)
- [REDUCE](#reduce)
<!-- /TOC -->

## Project structure

In this project, I've chosen to separate each step on purpose, in order to be able to measure each step running time.

The project is structured in different programs:

Slave
The Slave is responsible for computing three steps on the machine it is launched.
Master
This is the main program used to launched the execution of splits, slave (map, shuffle, reduce) and concatenation of the results.
It has one main class Master.java where the main method is. It is where all steps are executed.
All steps are executed using multiple threads: I've chosen to create a class for each of them (although this could be factorize better):
_ThreadCreateSplit_, _ThreadDeploySplit_, _ThreadMap_, _ThreadShuffle_ and _ThreadReduce_. The _Partition_ class is used to split the initial file (first step). _ThreadProcessBuilder_ is the class allowing to send linux command in ssh.

Clean
Used to clean all files on the machines that will be used for MapReduce. The clean simply loop on the machine list and remove the folder /savoga.
Deploy
This program send the Slave.jar on the different machines. It loops on the machine list and copy the file from local computer to remote ones.

## User initialization

Here are the parameters the user should be aware before starting the program:
- The static paths should be changed with the corresponding value (the Slave needs to be rebuild and deployed)
- Depending of the ssh connection rapidity, one can also amend the time specified in the *sleep* methods
- The input.txt is the text file used for the wordcount
- The number of split files. At the moment, **the program doesn't handle a number of split files superior to the number of machines**.
- A script is used to look for a folder remotely ```fileSearch.sh``` (see [remarks](#remarks) section)

## Step details

![MapReduceImage](https://github.com/savoga/various_projects/blob/master/MapReduce/MapReducePic.png)

#### SPLIT

Folder ```\splits```

The split step consist in two actions:

__Split the initial text file "input.txt" into multiple files__

There are numerous ways to split a files, I chose the most intuitive one for me:
- Spliting the initial file into words and putting them into an array
- Partitioning the array into chunks with same size
- For each chunk, create a file

Since each chunk are independent, the last part of this action is done using multiple threads.

__Send the different split files to different machines__

For this action, I use a process builder with command *scp*.

#### MAP

Folder ```\maps```

The map is done by the Slave. For each split file, a new file with prefix *UM* is created. Each *UM* file contains a word from the split file associated with a value 1. 

#### SHUFFLE

Folder ```\shuffles``` and ```\shufflesreceived```

The shuffle step does 2 actions:

__Group words for each map file__

For each line of a map file, a file is created with the tuple [word, 1]. If the file already exists, it is appended with a new line. Finding the hashcode is done in order to name a file according to the word it contains. This will allow us to consolidate files with the same hashcode (and thus the same containing word). To find the hashcode I use ```.hashCode```.

The name of a file is also composed of the machine hostname ```.getLocalHost()```.

__Send the different shuffle files to different machines__

The rule for determining on which machine the shuffle file is sent is summarized in following line: 

```int machineIndex = Math.abs(Integer.parseInt(hashCode) % nbMachines);```

#### REDUCE

Folder ```\reduces```

The reduce step is the addition of all values of same tuples [word 1]. We thus end up with a file having [word n] where *n* is the number of times the word appears in the initial split file.

#### EXTRA STEP: CONCATENATION

In order to assess the correct result, a last step is done to retrieve all reduce files from remote machines and concatenate them locally.


Finally, all steps are timed in the *main*

## LIMITS AND IMPROVEMENTS

Many improvements can be brought to this MapReduce from scratch. 

First of all, the program doesn't handle a number of split files that is superior to the number of machine used.

When a ssh connection is too slow, 

Several functions are used in master/slave, thus the code can be factorize further in sharing function thanks to Maven dependencies.

## REMARKS

Several times during the 
