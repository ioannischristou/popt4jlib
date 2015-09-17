README File for the popt4jlib Parallel Optimization For Java Library.

Author: Ioannis T. Christou

License material is to be found in the license sub-directory.

1. Brief Introduction

popt4jlib is a library of Java classes for solving in parallel and/or
distributed computing environments numerical optimization problems 
(continuous and discrete). It makes heavy use of the multi-threading
capabilities of the Java programming language and builds heavily on it. The
algorithms implemented were selected according to their "fitness" in 
parallel machines.
The library source code compiles under JDK 1.4 or any higher version 
therefore the java.util.concurrent packages of JDK5 are not used at all. 
Java binaries (jar as well as class files) are provided both for JRE 1.4 as 
well as JRE 1.7, and the entire project is provided both as a NetBeans 7.4
project as well as a JBuilder X project. Several packages containing 
functionality that can be used independently of the main optimization
packages are also provided, namely the parallel, parallel.distributed,
graph, and graph.* sub-packages.

2. Project Structure

The main structure of the project consists of the following sub-directories:
- ROOT popt4jlib directory: this is the directory where this README.txt 
file is located. It contains the popt4jlib.jar file containing also the
javadoc for the project compiled with JDK 1.4

-- src: This is the sub-directory containing all the Java source code for
the project.

-- classes: Contains all source classes compiled with JBuilder X under JDK 1.4

-- doc: Contains all the javadocs compiled with JBuilder X under JDK 1.4

-- build: Contains all source classes compiled with NetBeans 7.4 under JDK 1.7

-- dist: Contains the distribution release (java jar and javadocs) 
compiled with NetBeans 7.4 under JDK 1.7.

-- license: Contains license material.

-- testdata: Contains a number of text-files providing parameters required
by several of the test-classes in order to run several tests showcasing the
functionality of the library; it also contains several graphs used in 
graph-related problems.

-- testresults: directory where results from some tests go when appropriate
batch files contained in the ROOT-level directory are executed.

3. WHAT TO DO NEXT

Read the HOW_TO_RUN_YOUR_FIRST_EXAMPLE.txt file in the top-level directory 
of the project; then start reading the javadocs of the project, and the 
code itself!.


