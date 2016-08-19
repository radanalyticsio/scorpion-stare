# scorpion-stare
Spark scheduler backend plug-ins for awareness of kube, openshift, oshinko, etc

build a plug-in into local (uber-)jar files:

    $ cd /path/to/scorpion-stare
    $ sbt oshinko/assembly

using custom scorpion-stare build of spark:

    $ cd /path/to/spark
    $ ./bin/spark-shell --master "local-cluster[2,1,1024]" --jars /path/to/scorpion-stare/oshinko/target/scala-2.11/scorpion_stare.jar

The spark-shell should recognize the `OshinkoService` plug-in for `WorkerScaleoutService`
