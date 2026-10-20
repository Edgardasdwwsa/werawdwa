#!/bin/bash
# Runs the Gradle build and, on failure, reports the error text as a workflow annotation.
set +e
gradle build --console=plain > build.log 2>&1
code=$?
if [ $code -ne 0 ]; then
  msg=$(grep -A45 "What went wrong" build.log | head -c 12000 | python3 -c 'import sys,urllib.parse; t=sys.stdin.read(); print(t.replace("%","%25").replace("\r","%0D").replace("\n","%0A"))')
  echo "::error title=Gradle build failed::$msg"
fi
exit $code
