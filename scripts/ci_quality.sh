#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

# Explicit task-local filters: adaptive policies live in design-system, not app.
# --continue retains independent behavior reports when a source policy fails.
bash ./gradlew \
  :ci-lint:test \
  :app:lintDebug -Pbili.ciChecks=true \
  :app:testDebugUnitTest \
    --tests 'com.android.purebilibili.feature.settings.AppUpdateCheckerTest' \
    --tests 'com.android.purebilibili.core.ui.performance.AppDisplayModePreferenceTest' \
    --tests 'com.android.purebilibili.core.ui.performance.RuntimeVisualGuardTrackerTest' \
    --tests 'com.android.purebilibili.core.store.SerializedSettingsCacheWriterTest' \
    --tests 'com.android.purebilibili.core.store.AppNavigationSettingsMappingPolicyTest.gestureRotationAndTranslationPreferencesAreIndependent' \
    --tests 'com.android.purebilibili.core.store.AppNavigationSettingsMappingPolicyTest.populatedPreferences_mapToNavigationSettingsCorrectly' \
    --tests 'com.android.purebilibili.core.store.ThemeSelectionMigrationPolicyTest' \
  :design-system:testDebugUnitTest \
    --tests 'com.android.purebilibili.core.ui.adaptive.RuntimeVisualGuardPolicyTest' \
    --tests 'com.android.purebilibili.core.ui.adaptive.RuntimeVisualGuardSignalPolicyTest' \
  --continue --no-daemon --console=plain "$@"
