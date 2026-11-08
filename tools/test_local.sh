#!/usr/bin/env bash
# Executes source validation and all pure-Java tests without Minecraft dependencies.
# This is NOT a substitute for `gradle build` or a game startup test.
set -euo pipefail
cd "$(dirname "$0")/.."
export LANG=C.UTF-8
export LC_ALL=C.UTF-8
python3 tools/verify_assets.py
mkdir -p build/smoke-classes
javac -encoding UTF-8 -d build/smoke-classes \
  src/main/java/tw/zonezero/survivordogtags/game/DogTagProgression.java \
  src/main/java/tw/zonezero/survivordogtags/game/DogTagDamageMath.java \
  src/main/java/tw/zonezero/survivordogtags/game/DogTagUpgradePlan.java \
  src/main/java/tw/zonezero/survivordogtags/game/DogTagPaymentPlanner.java \
  src/main/java/tw/zonezero/survivordogtags/game/DogTagShotLedger.java \
  src/main/java/tw/zonezero/survivordogtags/game/DogTagProfileSafety.java \
  src/main/java/tw/zonezero/survivordogtags/game/DogTagKillMath.java \
  src/main/java/tw/zonezero/survivordogtags/game/DogTagShiftRouting.java \
  tests/*.java
for test in ProgressionSmokeTest UpgradePlanSmokeTest ShotLedgerSmokeTest \
            PaymentPlannerSmokeTest PaymentPlannerFuzzTest ProfileSafetySmokeTest KillMathSmokeTest ShiftRoutingSmokeTest DamageMathSmokeTest; do
  java -cp build/smoke-classes "$test"
done
