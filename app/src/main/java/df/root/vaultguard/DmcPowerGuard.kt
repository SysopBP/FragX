package df.root.vaultguard

import android.content.Context
import android.os.Binder
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import df.root.vaultguard.DmcVaultPolicy
import df.root.vaultguard.DmcPowerDecision
import java.util.concurrent.atomic.AtomicBoolean

/** Runs only inside system_server. No cached status can authorize a power action. */
internal object DmcPowerGuard {
    private const val TAG = "FragXVaultPowerGuard"
    private const val TIMEOUT_MS = 5_000L
    private val busy = AtomicBoolean(false)
    private val bypass = ThreadLocal<Boolean>()
    @Volatile private var activeDecision: DmcPowerDecision? = null
    fun cancelPending() { activeDecision?.cancel() }
    fun isEmergency(reason: String?) = reason?.startsWith("thermal") == true || reason == "battery"
    // Shared with boot/setup recovery so vault read-modify-write operations cannot overlap.
    val vaultLock = Any()
    fun isBusy() = busy.get()
    fun isResuming() = bypass.get() == true

    fun verifyVault(): ByteArray = synchronized(vaultLock) {
        check(Process.myUid() == 1000) { "DMC verification requires System Server UID 1000" }
        val api = Class.forName("com.samsung.android.service.vaultkeeper.VaultKeeperManager")
        val manager = api.getMethod("getInstance", String::class.java).invoke(null, "DMC")
            ?: error("DMC VaultKeeper unavailable")
        val read = api.getMethod("read", Int::class.javaPrimitiveType)
        val write = api.getMethod("write", Int::class.javaPrimitiveType, ByteArray::class.java)
        val vault = object : DmcVaultPolicy.Vault {
            override fun read() = read.invoke(manager, 1) as? ByteArray
            override fun write(record: ByteArray) = write.invoke(manager, 1, record) as Int
        }
        val verified = DmcVaultPolicy.enforce(vault)
        // Even an already-correct record gets a second read immediately before approval.
        val finalRead = vault.read()
        DmcVaultPolicy.validate(finalRead)
        check(verified.contentEquals(finalRead) && DmcVaultPolicy.matchesTarget(finalRead)) {
            "DMC changed during power-action verification"
        }
        finalRead!!
    }

    fun intercept(context: Context, action: String, resume: () -> Unit) {
        if (!busy.compareAndSet(false, true)) {
            Log.w(TAG, "DMC_POWER_DUPLICATE action=$action")
            return
        }
        val main = Handler(Looper.getMainLooper())
        val deadline = SystemClock.elapsedRealtime() + TIMEOUT_MS
        val decision = DmcPowerDecision(deadline)
        activeDecision = decision
        fun message(text: String) { runCatching { Toast.makeText(context, text, Toast.LENGTH_LONG).show() } }
        main.post { message("VaultGuard: checking before $action…") }
        val timeout = Runnable {
            if (decision.cancel()) {
                Log.e(TAG, "DMC_POWER_CANCELLED action=$action reason=timeout")
                message("VaultGuard cancelled $action: VaultGuard verification timed out. Check VaultGuard status before retrying.")
                // Keep busy while a stuck vault call is still running; never queue more workers.
            }
        }
        main.postDelayed(timeout, TIMEOUT_MS)
        try {
            Thread({
                val identity = Binder.clearCallingIdentity()
                val result = try { runCatching { verifyVault() } }
                    finally { Binder.restoreCallingIdentity(identity) }
                main.post {
                    try {
                        if (SystemClock.elapsedRealtime() >= deadline) timeout.run()
                        if (!decision.complete(SystemClock.elapsedRealtime())) return@post
                        main.removeCallbacks(timeout)
                        result.fold(onSuccess = {
                            Log.i(TAG, "DMC_POWER_VERIFIED action=$action uid=${Process.myUid()} pid=${Process.myPid()} lock=${it[0]} maintenance=${it[1]} at=${it[2]}")
                            bypass.set(true)
                            try { resume() } finally { bypass.remove() }
                        }, onFailure = {
                            Log.e(TAG, "DMC_POWER_CANCELLED action=$action reason=verification_failed", it)
                            message("VaultGuard cancelled $action: Samsung Recovery Protection could not be verified. Open VaultGuard status and retry.")
                        })
                    } catch (error: Throwable) {
                        Log.e(TAG, "DMC_POWER_RESUME_FAILED action=$action", error)
                        message("VaultGuard could not continue $action. Please retry from the power menu.")
                    } finally { activeDecision = null; busy.set(false) }
                }
            }, "FragX-Vault-PowerGuard").apply { isDaemon = true }.start()
        } catch (error: Throwable) {
            main.removeCallbacks(timeout)
            decision.cancel()
            activeDecision = null
            busy.set(false)
            Log.e(TAG, "DMC_POWER_CANCELLED action=$action reason=worker_failed", error)
            main.post { message("VaultGuard cancelled $action: verification could not start.") }
        }
    }
}
