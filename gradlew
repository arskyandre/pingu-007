#!/bin/sh

app_home=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)

if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    java_cmd="$JAVA_HOME/bin/java"
else
    java_cmd=java
fi

if ! command -v "$java_cmd" >/dev/null 2>&1; then
    echo "ERROR: JAVA_HOME is not set and no java command could be found." >&2
    exit 1
fi

wrapper_jar="$app_home/gradle/wrapper/gradle-wrapper.jar"
if [ ! -f "$wrapper_jar" ]; then
    echo "ERROR: missing $wrapper_jar" >&2
    exit 1
fi

exec "$java_cmd" ${JAVA_OPTS:-} ${GRADLE_OPTS:-} -classpath "$wrapper_jar" org.gradle.wrapper.GradleWrapperMain "$@"
