#! /bin/sh
_JAVA_OPTIONS="-Xms8G -Xmx8G" java -XX:+FlightRecorder  -XX:StartFlightRecording=duration=60s,filename=myrecording.jfr -Djava.util.Arrays.useLegacyMergeSort=true -cp target:lib/ECLA.jar:lib/DTNConsoleConnection.jar:lib/gson-2.8.5.jar:lib/commons-math3-3.2.jar core.DTNSim $*
