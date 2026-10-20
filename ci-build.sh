#!/bin/bash
# Runs the Gradle build; on failure, publishes the error text as a workflow annotation.
set +e
chmod +x ./gradlew
./gradlew build --console=plain > build.log 2>&1
code=$?
if [ $code -ne 0 ]; then
  msg=$(grep -E -A6 "error:|What went wrong" build.log | head -c 12000 | python3 -c 'import sys; t=sys.stdin.read(); print(t.replace("%","%25").replace("\r","%0D").replace("\n","%0A"))')
  echo "::error title=Gradle build failed::$msg"
fi
exit $code
