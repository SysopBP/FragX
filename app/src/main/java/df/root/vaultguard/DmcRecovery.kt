package df.root.vaultguard

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** System Server recovery only. No root shell or app_process fallback. */
object DmcRecovery {
    data class Status(
        val systemServer: Boolean? = null,
        val screenLock: Int? = null,
        val maintenance: Int? = null,
        val atCommand: Int? = null,
        val downloadAllowed: Boolean? = null,
        val verified: Boolean = false,
        val detail: String = "Enable FragX in LSPosed System Framework and restart Android",
        val checkedAt: Long = 0L
    ) {
        val targetsMet: Boolean get() = verified && systemServer == true && screenLock == 0 &&
            maintenance == 0 && atCommand == 1 && downloadAllowed == true

        fun report(): String {
            fun flag(value: Int?) = when(value) { 0 -> "Disabled"; 1 -> "Enabled"; else -> "Unknown" }
            return buildString {
                appendLine("Samsung recovery checks\nAutomatic System Server recovery: ${if(targetsMet) "Verified" else "Not verified"}")
                appendLine("System Server: ${when(systemServer) { true -> "Available"; false -> "Unavailable"; null -> if(checkedAt == 0L) "Checking" else "Not verified" }}")
                appendLine("Screen lock (vault): ${flag(screenLock)}  •  Target: Disabled")
                appendLine("Maintenance Mode (vault): ${flag(maintenance)}  •  Target: Disabled")
                appendLine("Download Mode AT Command: ${flag(atCommand)}  •  Target: Enabled")
                appendLine("Download Mode: ${when(downloadAllowed) { true -> "Allowed"; false -> "Blocked"; null -> "Unknown" }}  •  Target: Allowed")
                appendLine()
                appendLine(detail)
                if (checkedAt > 0L) appendLine("Checked: ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(checkedAt))}")
                append("VaultGuard runs automatically inside Xposed System Server. Samsung recovery vault flags are separate from Android credentials and the Android lock screen.")
            }
        }
    }

    private val worker = Executors.newSingleThreadScheduledExecutor()
    private val main = Handler(Looper.getMainLooper())
    private val busy = AtomicBoolean(false)
    @Volatile private var lastAttempt = -30_000L
    private const val PREFS = "fragx_vaultguard_v1"
    private const val TAG = "FragXVaultRecovery"

    private fun prefs(context: Context) = context.createDeviceProtectedStorageContext()
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun bootCount(context: Context) = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)

    fun status(context: Context): Status {
        val p = prefs(context)
        if (p.getInt("boot", -2) != bootCount(context)) return Status()
        fun value(key: String) = p.getInt(key, -1).takeIf { it in 0..1 }
        return Status(value("system_server")?.let { it == 1 }, value("lock"), value("maint"), value("at"),
            value("download")?.let { it == 1 }, p.getBoolean("verified", false),
            p.getString("detail", null) ?: "Enable FragX in LSPosed System Framework and restart Android", p.getLong("checked", 0L))
    }

    /** Callback always completes promptly even if another boot source already owns the check. */
    fun request(context: Context, source: String, retry: Int = 0, force: Boolean = false, actionResult: ((Status) -> Unit)? = null, done: (() -> Unit)? = null) {
        val app = context.applicationContext
        if (!busy.compareAndSet(false, true)) { actionResult?.let { main.post { it(Status(detail = "Another VaultGuard check is in progress; retry shortly")) } }; done?.invoke(); return }
        if (!force && retry == 0 && SystemClock.elapsedRealtime() - lastAttempt < 10_000L) {
            busy.set(false); done?.invoke(); return
        }
        worker.execute {
            var success = false
            var outcome = Status(detail = "VaultGuard verification failed")
            try {
                lastAttempt = SystemClock.elapsedRealtime()
                val result = if (Process.myUid() / 100000 != 0) {
                    Status(detail = "VaultGuard supports the primary user only")
                } else {
                    if (android.os.Build.VERSION.SDK_INT < 34) {
                        Status(detail = "System Server recovery requires Android 14 or newer")
                    } else {
                        val reply = java.util.concurrent.ArrayBlockingQueue<Status>(1)
                        val receiver = object : android.content.BroadcastReceiver() {
                            override fun onReceive(context: Context, intent: android.content.Intent) {
                                if (sentFromUid != 1000) return
                                val verified = intent.getBooleanExtra("verified", false)
                                fun flag(key: String) = intent.getIntExtra(key, -1).takeIf { it in 0..1 }
                                reply.offer(Status(true, flag("lock"), flag("maint"), flag("at"),
                                    flag("download")?.let { it == 1 }, verified,
                                    intent.getStringExtra("detail") ?: "System Server response"))
                            }
                        }
                        val action = "com.sysopbp.fragx.action.DMC_RESULT." + java.util.UUID.randomUUID()
                        app.registerReceiver(receiver, android.content.IntentFilter(action), Context.RECEIVER_EXPORTED)
                        try {
                            app.sendBroadcast(android.content.Intent("com.sysopbp.fragx.action.DMC_REQUEST")
                                .setPackage("android").putExtra("reply", action), null,
                                android.app.BroadcastOptions.makeBasic().setShareIdentityEnabled(true).toBundle())
                            reply.poll(6, TimeUnit.SECONDS)
                                ?: Status(detail = "Xposed System Server did not respond; enable FragX for System Framework and reboot. No root fallback.")
                        } finally { app.unregisterReceiver(receiver) }
                    }
                }
                val stamped = result.copy(checkedAt = System.currentTimeMillis())
                outcome = stamped
                success = stamped.targetsMet
                prefs(app).edit().putInt("boot", bootCount(app))
                    .putInt("system_server", stamped.systemServer?.let { if(it) 1 else 0 } ?: -1)
                    .putInt("lock", stamped.screenLock ?: -1).putInt("maint", stamped.maintenance ?: -1)
                    .putInt("at", stamped.atCommand ?: -1)
                    .putInt("download", stamped.downloadAllowed?.let { if(it) 1 else 0 } ?: -1)
                    .putBoolean("verified", stamped.verified).putString("detail", stamped.detail)
                    .putLong("checked", stamped.checkedAt).commit()
                Log.i(TAG, "DMC_AUTO_CHECK source=$source retry=$retry targetsMet=$success detail=${stamped.detail}")
            } catch (error: Exception) {
                prefs(app).edit().putInt("boot", bootCount(app)).putBoolean("verified", false)
                    .putInt("system_server", -1).putInt("lock", -1).putInt("maint", -1).putInt("at", -1).putInt("download", -1)
                    .putString("detail", "System Server unavailable: ${error.message}")
                    .putLong("checked", System.currentTimeMillis()).commit()
                Log.w(TAG, "DMC_AUTO_CHECK_FAILED source=$source", error)
            } finally {
                busy.set(false)
                actionResult?.let { callback -> val result = outcome; main.post { callback(result) } }
                done?.invoke()
            }
            if (!success && retry < 3) worker.schedule({ request(app, source, retry + 1) },
                longArrayOf(5, 15, 30)[retry], TimeUnit.SECONDS)
        }
    }

    @JvmStatic
    fun refreshForUi(context: Context, done: Runnable) {
        // Match D2 status refresh: retain throttling and the 5/15/30-second retry schedule.
        request(context, "fragx_ui", done = { main.post { done.run() } })
    }

    /** One forced check; never authorize recovery using the cached status. */
    fun verifyForAction(context: Context, done: (Status) -> Unit) {
        request(context, "recovery_action", retry = 3, force = true, actionResult = done)
    }

    fun refresh(context: Context, done: (Status) -> Unit) {
        request(context, "status_dialog") { main.post { done(status(context)) } }
    }
}
