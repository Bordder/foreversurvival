#!/bin/sh
#
# Minimal Gradle wrapper startup script for POSIX shells.
#

APP_HOME=$(cd -P "$(dirname "$0")" > /dev/null && pwd)
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

if ! command -v "$JAVACMD" > /dev/null 2>&1 && [ ! -x "$JAVACMD" ] ; then
    echo "ERROR: no Java found. Install JDK 17 or set JAVA_HOME." >&2
    exit 1
fi

if [ ! -f "$CLASSPATH" ] ; then
    echo "ERROR: gradle/wrapper/gradle-wrapper.jar is missing." >&2
    echo "It is a binary file and has to be generated once:" >&2
    echo "    gradle wrapper --gradle-version 7.4.1" >&2
    echo "Or build without the wrapper:" >&2
    echo "    gradle build" >&2
    exit 1
fi

exec "$JAVACMD" -Xmx64m -Xms64m \
    "-Dorg.gradle.appname=gradlew" \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain "$@"
