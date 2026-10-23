package df.root.vaultguard

import android.content.Context
import android.content.Intent
import android.util.Log
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
import java.util.concurrent.atomic.AtomicBoolean

/** VaultGuard System Framework hooks, adapted from D2 Guardian. */
class VaultGuardModule : XposedModule() {
    companion object {
        private const val TAG = "FragXVaultGuard"
        private val dmcLock = DmcPowerGuard.vaultLock
        private val recoveryBusy = AtomicBoolean(false)
        private val bootReadySent = AtomicBoolean(false)
        @Volatile private var softRestartGuardInstalled = false
    }

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        log(
            Log.INFO,
            TAG,
            "VAULTGUARD_LOADED api=$apiVersion framework=$frameworkName mode=vaultguard"
        )
    }

    override fun onSystemServerStarting(param: SystemServerStartingParam) {
        log(
            Log.INFO,
            TAG,
            "VAULTGUARD_SYSTEM_SERVER_START uid=${android.os.Process.myUid()}"
        )
        installBootReadyHook(param.classLoader)
        installSecuritySetupObservers(param.classLoader)
        installPowerActionGuard(param.classLoader)
    }

    /** Guard the confirmed shutdown sequence, before Android starts tearing down services. */
    private fun installPowerActionGuard(classLoader: ClassLoader) {
        runCatching {
            val owner = Class.forName("com.android.server.power.ShutdownThread", false, classLoader)
            val begin = owner.getDeclaredMethod("beginShutdownSequence", Context::class.java).apply { isAccessible = true }
            val reasonField = owner.getDeclaredField("mReason").apply { isAccessible = true }
            val rebootField = owner.getDeclaredField("mReboot").apply { isAccessible = true }
            // Do not let another request overwrite ShutdownThread's static action while verifying.
            owner.declaredMethods.filter { it.name in setOf("shutdown", "reboot", "rebootSafeMode") && it.returnType == Void.TYPE }.forEach { method ->
                hook(method).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                    val entryReason = chain.args.filterIsInstance<String>().firstOrNull()
                    if (DmcPowerGuard.isEmergency(entryReason)) {
                        DmcPowerGuard.cancelPending()
                        chain.proceed()
                    } else if (DmcPowerGuard.isBusy() && !DmcPowerGuard.isResuming()) {
                        Log.w(TAG, "DMC_POWER_DUPLICATE entry=${method.name}")
                        null
                    } else chain.proceed()
                }
            }
            hook(begin).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                val context = chain.args[0] as Context
                val reason = reasonField.get(null) as? String
                // Battery/thermal emergency shutdown must remain immediate.
                if (DmcPowerGuard.isResuming() || DmcPowerGuard.isEmergency(reason)) {
                    if (DmcPowerGuard.isEmergency(reason)) DmcPowerGuard.cancelPending()
                    chain.proceed()
                } else {
                    val reboot = rebootField.getBoolean(null)
                    val action = if (reboot) "restart (${reason ?: "normal"})" else "power off"
                    DmcPowerGuard.intercept(context, action) {
                        check(reasonField.get(null) == reason && rebootField.getBoolean(null) == reboot) { "Power request changed during verification" }
                        begin.invoke(null, context)
                    }
                    null
                }
            }
            Log.i(TAG, "DMC_POWER_HOOK_INSTALLED target=ShutdownThread#beginShutdownSequence")
        }.onFailure { Log.e(TAG, "DMC_POWER_HOOK_UNAVAILABLE target=ShutdownThread", it) }

        // Android's orderly userspace restart. Direct root process kills bypass this method.
        runCatching {
            val owner = Class.forName("com.android.server.am.ActivityManagerService", false, classLoader)
            val restart = owner.getDeclaredMethod("restart").apply { isAccessible = true }
            val contextField = owner.getDeclaredField("mContext").apply { isAccessible = true }
            hook(restart).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                if (DmcPowerGuard.isResuming()) chain.proceed() else {
                    val target = chain.thisObject
                    val context = contextField.get(target) as Context
                    // Preserve AMS's caller permission before deferring onto a trusted thread.
                    context.enforceCallingOrSelfPermission("android.permission.SET_ACTIVITY_WATCHER", "VaultGuard guarded userspace restart")
                    DmcPowerGuard.intercept(context, "soft restart") { restart.invoke(target) }
                    null
                }
            }
            softRestartGuardInstalled = true
            Log.i(TAG, "DMC_POWER_HOOK_INSTALLED target=ActivityManagerService#restart")
        }.onFailure { Log.e(TAG, "DMC_POWER_HOOK_UNAVAILABLE target=ActivityManagerService#restart", it) }
    }

    private fun installBootReadyHook(classLoader: ClassLoader) {
        val className = "com.android.server.am.ActivityManagerService"
        val owner = runCatching {
            Class.forName(className, false, classLoader)
        }.getOrElse {
            log(Log.WARN, TAG, "VAULTGUARD_CLASS_MISSING target=$className", it)
            return
        }

        val methods = owner.declaredMethods
            .filter { it.name == "systemReady" }
            .distinctBy { it.toGenericString() }

        if (methods.isEmpty()) {
            log(Log.WARN, TAG, "VAULTGUARD_HOOK_UNAVAILABLE target=$className#systemReady")
            return
        }

        methods.forEach { method ->
            try {
                method.isAccessible = true
                hook(method)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept { chain ->
                        // Android/system_server always gets control first. Recovery starts
                        // only after the real systemReady invocation returns successfully.
                        val result = chain.proceed()
                        signalBootReady()
                        result
                    }

                log(
                    Log.INFO,
                    TAG,
                    "VAULTGUARD_HOOK_INSTALLED target=${owner.name}#${method.name}"
                )
            } catch (t: Throwable) {
                // Fail open: a bridge problem must never intentionally block boot.
                log(
                    Log.WARN,
                    TAG,
                    "VAULTGUARD_HOOK_FAILED target=${owner.name}#${method.name}",
                    t
                )
            }
        }
    }


    private val securityRefreshHandler by lazy { android.os.Handler(android.os.Looper.getMainLooper()) }
    private val securityRefreshLock = Any()
    private val securityRefresh = Runnable { refreshAfterSecuritySetup() }
    private val securityFollowUp = Runnable { refreshAfterSecuritySetup() }

    /** Observe committed setup operations only; never inspect credentials or template contents. */
    private fun installSecuritySetupObservers(classLoader: ClassLoader) {
        val targets = mapOf(
            "com.android.server.locksettings.LockSettingsService" to setOf("setLockCredential", "setLockCredentialWithToken"),
            "com.android.server.biometrics.sensors.fingerprint.FingerprintUtils" to setOf("addBiometricForUser", "removeBiometricForUser"),
            "com.android.server.biometrics.sensors.face.FaceUtils" to setOf("addBiometricForUser", "removeBiometricForUser")
        )
        targets.forEach { (className, names) ->
            runCatching {
                val owner = Class.forName(className, false, classLoader)
                val methods = owner.declaredMethods.filter { it.name in names && !it.isBridge && !it.isSynthetic }
                if (methods.isEmpty()) Log.w(TAG, "DMC_SETUP_OBSERVER_UNAVAILABLE target=$className")
                methods.forEach { method ->
                    runCatching {
                        method.isAccessible = true
                        hook(method).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                            val result = chain.proceed()
                            // A rejected credential change returns false; exceptions never reach here.
                            if (result != false) runCatching {
                                synchronized(securityRefreshLock) {
                                    securityRefreshHandler.removeCallbacks(securityRefresh)
                                    securityRefreshHandler.removeCallbacks(securityFollowUp)
                                    securityRefreshHandler.postDelayed(securityRefresh, 2_000L)
                                    // Samsung can update DMC after the framework call returns.
                                    securityRefreshHandler.postDelayed(securityFollowUp, 12_000L)
                                }
                                Log.i(TAG, "DMC_SECURITY_SETUP_CHANGED source=$className#${method.name}")
                            }.onFailure { Log.w(TAG, "DMC_SETUP_REFRESH_SCHEDULE_FAILED", it) }
                            result
                        }
                        Log.i(TAG, "DMC_SETUP_OBSERVER_INSTALLED target=$className#${method.name}")
                    }.onFailure { Log.w(TAG, "DMC_SETUP_OBSERVER_FAILED target=$className#${method.name}", it) }
                }
            }.onFailure { Log.w(TAG, "DMC_SETUP_CLASS_UNAVAILABLE target=$className", it) }
        }
    }

    private fun refreshAfterSecuritySetup() {
        runCatching {
            val app = Class.forName("android.app.ActivityThread")
                .getDeclaredMethod("currentApplication").invoke(null) as? Context
                ?: error("system_server application unavailable")
            // Recover even while the FragX app is stopped.
            scheduleDmcRecovery(app, delay = 0L)

        }.onFailure { Log.w(TAG, "DMC_SETUP_REFRESH_FAILED", it) }
    }

    private fun installDmcReceiver(app: Context) {
        if (android.os.Build.VERSION.SDK_INT < 34) return
        app.registerReceiver(object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val appUid = runCatching { app.packageManager.getPackageUid("com.sysopbp.fragx", 0) }.getOrNull()
                val sender = sentFromUid
                if (sender < 0 || sender / 100000 != 0 || sender != appUid) return
                val callerPackage = "com.sysopbp.fragx"
                if (intent.action == "com.sysopbp.fragx.action.DMC_SOFT_RESTART") {
                    if (!softRestartGuardInstalled) {
                        Log.e(TAG, "DMC_SOFT_RESTART_CANCELLED reason=guard_unavailable")
                        runCatching { android.widget.Toast.makeText(app, "VaultGuard soft restart cancelled: recovery guard unavailable.", android.widget.Toast.LENGTH_LONG).show() }
                        return
                    }
                    runCatching {
                        val manager = Class.forName("android.app.ActivityManager").getDeclaredMethod("getService").invoke(null)
                        val restart = manager.javaClass.getMethod("restart")
                        val identity = android.os.Binder.clearCallingIdentity()
                        try { restart.invoke(manager) } finally { android.os.Binder.restoreCallingIdentity(identity) }
                    }.onFailure { Log.e(TAG, "DMC_SOFT_RESTART_REQUEST_FAILED", it) }
                    return
                }
                val reply = intent.getStringExtra("reply") ?: return
                if (!reply.startsWith("com.sysopbp.fragx.action.DMC_RESULT.")) return
                scheduleDmcRecovery(app, reply, 0L, callerPackage)
            }
        }, android.content.IntentFilter("com.sysopbp.fragx.action.DMC_REQUEST").apply {
            addAction("com.sysopbp.fragx.action.DMC_SOFT_RESTART")
        }, Context.RECEIVER_EXPORTED)
        Log.i(TAG, "DMC_SYSTEM_SERVER_RECEIVER_READY uid=${android.os.Process.myUid()} pid=${android.os.Process.myPid()}")
    }

    /** One guarded recovery attempt in system_server; validate before writing and verify read-back. */
    private fun scheduleDmcRecovery(systemContext: Context, reply: String? = null, delay: Long = 10_000L, replyPackage: String = "com.sysopbp.fragx") {
        runCatching {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                runCatching {
                    if (!recoveryBusy.compareAndSet(false, true)) return@postDelayed
                    Thread({
                        var verifiedRecord: ByteArray? = null
                        var detail = "System Server recovery failed"
                        val identity = android.os.Binder.clearCallingIdentity()
                        try {
                            val uid = android.os.Process.myUid()
                            val pid = android.os.Process.myPid()
                            Log.i(TAG, "DMC_SYSTEM_SERVER_RECOVERY_START uid=$uid pid=$pid mode=guarded_recovery")
                            check(uid == 1000) { "Probe requires system_server UID 1000" }
                            synchronized(dmcLock) {
                            val api = Class.forName("com.samsung.android.service.vaultkeeper.VaultKeeperManager")
                            val manager = api.getMethod("getInstance", String::class.java).invoke(null, "DMC")
                                ?: error("VaultKeeper manager unavailable")
                            val data = api.getMethod("read", Int::class.javaPrimitiveType).invoke(manager, 1) as? ByteArray
                            if (data == null) {
                                detail = "System Server DMC read returned no record; access or initialization unverified"
                                Log.w(TAG, "DMC_SYSTEM_SERVER_RECOVERY_NO_RECORD uid=$uid pid=$pid access_or_initialization_unverified")
                            } else {
                                df.root.vaultguard.DmcVaultPolicy.validate(data)
                                val readMethod = api.getMethod("read", Int::class.javaPrimitiveType)
                                val writeMethod = api.getMethod("write", Int::class.javaPrimitiveType, ByteArray::class.java)
                                val vault = object : df.root.vaultguard.DmcVaultPolicy.Vault {
                                    override fun read(): ByteArray? = readMethod.invoke(manager, 1) as? ByteArray
                                    override fun write(record: ByteArray): Int = writeMethod.invoke(manager, 1, record) as Int
                                }
                                val verified = df.root.vaultguard.DmcVaultPolicy.enforce(vault)
                                verifiedRecord = verified
                                detail = "Targets verified inside System Server UID $uid PID $pid"
                                Log.i(TAG, "DMC_SYSTEM_SERVER_RECOVERY_VERIFIED screenLock=${verified[0]} maintenanceMode=${verified[1]} downloadAtCommand=${verified[2]} downloadAllowed=${df.root.vaultguard.DmcVaultPolicy.downloadAllowed(verified)}")
                                Log.i(TAG, "DMC_SYSTEM_SERVER_RECOVERY_INITIAL_READ uid=$uid pid=$pid length=${data.size} screenLock=${data[0]} maintenanceMode=${data[1]} downloadAtCommand=${data[2]} downloadAllowed=${df.root.vaultguard.DmcVaultPolicy.downloadAllowed(data)} targetsMet=${df.root.vaultguard.DmcVaultPolicy.matchesTarget(data)} mode=guarded_recovery")
                            }
                            }
                        } catch (error: Throwable) {
                            val cause = (error as? java.lang.reflect.InvocationTargetException)?.targetException ?: error
                            detail = "${cause.javaClass.simpleName}: ${cause.message}"
                            Log.w(TAG, "DMC_SYSTEM_SERVER_RECOVERY_FAILED mode=guarded_recovery", cause)
                        } finally {
                            android.os.Binder.restoreCallingIdentity(identity)
                            recoveryBusy.set(false)
                            if (reply != null && android.os.Build.VERSION.SDK_INT >= 34) runCatching {
                                val data = verifiedRecord
                                systemContext.sendBroadcast(Intent(reply).setPackage(replyPackage)
                                    .putExtra("verified", data != null).putExtra("detail", detail)
                                    .putExtra("lock", data?.get(0)?.toInt() ?: -1)
                                    .putExtra("maint", data?.get(1)?.toInt() ?: -1)
                                    .putExtra("at", data?.get(2)?.toInt() ?: -1)
                                    .putExtra("download", if (data != null) 1 else -1), null,
                                    android.app.BroadcastOptions.makeBasic().setShareIdentityEnabled(true).toBundle())
                            }.onFailure { Log.w(TAG, "DMC_SYSTEM_SERVER_RESULT_FAILED", it) }
                        }
                    }, "FragX-Vault-Recovery").apply { isDaemon = true }.start()
                }.onFailure { recoveryBusy.set(false); Log.w(TAG, "DMC_SYSTEM_SERVER_RECOVERY_THREAD_FAILED", it) }
            }, delay)
        }.onFailure { Log.w(TAG, "DMC_SYSTEM_SERVER_RECOVERY_SCHEDULE_FAILED", it) }
    }

    private fun signalBootReady() {
        if (!bootReadySent.compareAndSet(false, true)) {
            log(Log.DEBUG, TAG, "VAULTGUARD_BOOT_READY_DEDUPED")
            return
        }

        runCatching {
            val activityThread = Class.forName("android.app.ActivityThread")
            val app = activityThread
                .getDeclaredMethod("currentApplication")
                .invoke(null) as? Context
                ?: error("system_server application unavailable")

            installDmcReceiver(app)
            scheduleDmcRecovery(app)
        }.onFailure {
            // No retry loop or heartbeat. If signalling fails, leave Android alone.
            log(Log.WARN, TAG, "VAULTGUARD_BOOT_READY_SIGNAL_FAILED", it)
        }
    }
}
