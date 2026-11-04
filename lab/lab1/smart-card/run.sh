#!/usr/bin/env bash
# Compile the Lab 1 applets and run apdu/baiN.scr on the jCardSim simulator, explaining every step.
# Usage: ./run.sh        run all exercises (Bai 0 to 4)
#        ./run.sh 3      run Bai 3 only
#        ./run.sh -i 0   install and select Bai 0, then type your own APDUs
set -euo pipefail
cd "$(dirname "$0")"

MODE=""
if [ "${1:-}" = "-i" ]; then
    MODE=-i
    shift
    if [ $# -ne 1 ]; then
        echo "Usage: ./run.sh -i <bai number>, for example ./run.sh -i 0"
        exit 1
    fi
fi

JAR=.lib/jcardsim-2.2.2.jar
SHA1=9dfb322303390ffb0016d06ba252b43666381c02

echo "[1/3] Simulator"
if [ ! -f "$JAR" ]; then
    echo "  Downloading jCardSim 2.2.2 (Java Card simulator + Java Card 2.2.2 API) into .lib/"
    mkdir -p .lib
    curl -fsSL -o "$JAR" https://repo1.maven.org/maven2/com/licel/jcardsim/2.2.2/jcardsim-2.2.2.jar
else
    echo "  Using the cached $JAR"
fi
echo "$SHA1  $JAR" | sha1sum -c --quiet
echo "  SHA-1 matches Maven Central: $SHA1"

echo
echo "[2/3] Compiling against the Java Card API in the jCardSim jar"
SOURCES=$(find src tools -name '*.java' | sort)
for f in $SOURCES; do echo "  $f"; done
rm -rf .build
javac -nowarn -cp "$JAR" -d .build $SOURCES
echo "  Class files written to .build/"

echo
echo "[3/3] Running the APDU scripts on a fresh simulated card per exercise"
echo "  jcardsim.cfg tells the simulator which applet class belongs to which AID."
for n in ${1:-0 1 2 3 4}; do
    java -cp "$JAR:.build" StepRunner jcardsim.cfg "apdu/bai$n.scr" $MODE
done
