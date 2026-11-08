# DAA Assignment 4

## Build and test

    mvn test

## Run the experiment

    mvn -q compile exec:java -Dexec.args="results"

or without the exec plugin:

    mvn -q compile
    java -Xmx2g -cp target/classes city.Experiment results
