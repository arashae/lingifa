#!/usr/bin/env bash
set -uo pipefail
result=0
if gradle --no-daemon connectedDebugAndroidTest --stacktrace; then
  result=0
else
  result=$?
  adb shell dumpsys input_method
fi
mkdir -p app/build/review-ui-checks
# Public MediaStore pictures survive the test runner uninstalling its APK.
if ! adb pull /sdcard/Pictures/LinguaFaReview app/build/review-ui-checks; then
  result=1
fi
if [ "$(find app/build/review-ui-checks -name '*.png' | wc -l)" -lt 4 ]; then
  echo "Expected four review verification screenshots."
  result=1
fi
exit "$result"
