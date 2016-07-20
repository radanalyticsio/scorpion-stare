# scorpion-stare
Spark scheduler backend plug-ins for awareness of kube, openshift, oshinko, etc

build the plug-ins into local jar files:

    $ cd /path/to/scorpion-stare
    $ sbt core/package oshinko/package

using custom scorpion-stare build of spark:

    $ cd /path/to/spark
    $ ./bin/spark-shell --master "local-cluster[2,1,1024]" --jars /path/to/scorpion-stare/oshinko/target/scala-2.11/scorpion-stare-oshinko_2.11-0.0.1-SNAPSHOT.jar,/path/to/scorpion-stare/core/target/scala-2.11/scorpion-stare-core_2.11-0.0.1-SNAPSHOT.jar

The spark-shell should recognize the `OshinkoService` plug-in for `SchedulerBackendPlugin`
