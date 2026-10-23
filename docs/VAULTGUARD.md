# VaultGuard preview

VaultGuard replaces the Odin estimate with Samsung recovery checks. It ports the DMC policy, System Framework setup observers and experimental power-action guard from D2 Guardian. Original DMC discovery credit: @diablOw. It is not affiliated with Samsung's VaultKeeper service.

## Setup and test

1. Disable D2 in LSPosed for this test.
2. Install the PR APK, open FragX once, enable FragX in LSPosed for **System Framework**, then restart Android to reload hooks. SystemUI scope is not needed.
3. Open **VaultGuard**. Check for System Server available, screen-lock vault flag disabled, Maintenance Mode vault flag disabled, AT-command flag enabled, and a successful verification timestamp.
4. Repeat checks after changing a PIN or adding/removing biometrics. The observers schedule checks after 2 and 12 seconds. They do not inspect credential arguments or biometric templates.
5. Validate ordinary Restart/Power off and check `DMC_POWER_HOOK_INSTALLED` and `DMC_POWER_VERIFIED` in logcat before claiming guard support on this firmware.

Modern libxposed API 101/102 support and Android 14+ are required for authenticated status requests. Root alone is insufficient. Disable the module and restart Android to unload these hooks.

The PR workflow builds a development-signed APK; it may not install over a permanent-key build. Preserve needed app settings before switching signing keys. No release is published by this PR.

## Behavior and limits

Only the primary user's 32-byte DMC slot 1 record is supported. The three known boolean bytes target `[0,0,1]`; the other 29 bytes are preserved. Malformed/unknown records are rejected. Writes must succeed and match the full read-back. Correct records are not rewritten.

Green means the last authenticated System Server response verified those targets. Gray means unknown/unavailable, including missing LSPosed hooks, unsupported firmware, denied access and timeouts. The panel includes check time; status is a point-in-time snapshot, not continuous monitoring or a guarantee that Odin will physically boot. No Android PIN, password, pattern or enrollment is removed. Maintenance Mode's actual session is not started/stopped.

Boot checks and post-setup checks run in System Server; app status requests are authenticated using Android's sender UID. There is no root shell/app_process fallback. Boot/setup and power checks share a vault lock; ordinary recovery admits only one worker at a time.

The experimental power guard covers ShutdownThread.beginShutdownSequence and ActivityManagerService.restart when present. A covered action is cancelled on failed verification or a five-second timeout. Emergency battery/thermal shutdown bypasses verification. Missing hooks can leave framework power actions unguarded. Direct root kills, init/property restarts, forced hardware resets, crashes and power loss are outside coverage. **FragX's existing native `ksud soft-reboot` path remains outside this framework guard**; this PR does not change its root/bootstrap behavior.

## Review provenance

Adapted from SysopBP/SamsungLockD2-Public: DmcRecovery.kt, DmcVaultPolicy.java, DmcPowerDecision.java, DmcPowerGuard.kt and the DMC portions of GuardianXposedBridge.kt, plus their policy/decision tests. D2's keyguard, authentication, wake, power-menu UI and Guardian handoff hooks are excluded. The port adds FragX-specific authenticated messaging, branding, UI and bounded ordinary recovery workers. Retain original discovery credit to @diablOw.
