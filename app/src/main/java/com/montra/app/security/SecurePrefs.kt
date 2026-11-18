package com.montra.app.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * App-lock storage. The PIN itself is never stored. We keep a random salt and
 * HMAC-SHA256(device-bound Keystore key, PBKDF2-HMAC-SHA256(PIN, salt)). Because the HMAC key lives in
 * the Android Keystore and can't be exported, a copied preferences file can't be brute-forced elsewhere.
 * Failed attempts and lockout times are persisted so restarting the app doesn't reset them.
 */
class SecurePrefs(context: Context) {
    private val sp = context.getSharedPreferences("montra_secure", Context.MODE_PRIVATE)

    val hasPin: Boolean get() = sp.contains(KEY_HASH) && sp.contains(KEY_SALT)

    /** True when the stored PIN follows the 6-digit rule (older installs may hold a 4-digit PIN). */
    val pinIsStrong: Boolean get() = hasPin && sp.getBoolean(KEY_STRONG, false)

    var lockEnabled: Boolean
        get() = hasPin && sp.getBoolean(KEY_LOCK, false)
        set(value) = sp.edit().putBoolean(KEY_LOCK, value).apply()

    /** Blocks screenshots and hides the app in the recent-apps switcher. */
    var secureScreen: Boolean
        get() = sp.getBoolean(KEY_SECURE_SCREEN, true)
        set(value) = sp.edit().putBoolean(KEY_SECURE_SCREEN, value).apply()

    var failures: Int
        get() = sp.getInt(KEY_FAILURES, 0)
        private set(value) = sp.edit().putInt(KEY_FAILURES, value).apply()

    var lockoutUntil: Long
        get() = sp.getLong(KEY_LOCKOUT_UNTIL, 0L)
        private set(value) = sp.edit().putLong(KEY_LOCKOUT_UNTIL, value).apply()

    private var lastAttemptAt: Long
        get() = sp.getLong(KEY_LAST_ATTEMPT, 0L)
        set(value) = sp.edit().putLong(KEY_LAST_ATTEMPT, value).apply()

    /** Wall-clock time that never runs backwards past the last attempt (rolling the clock back doesn't help). */
    fun trustedNow(): Long = maxOf(System.currentTimeMillis(), lastAttemptAt)

    fun lockoutRemainingMillis(): Long = (lockoutUntil - trustedNow()).coerceAtLeast(0L)

    fun setPin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val derived = pbkdf2(pin, salt, ITERATIONS)
        val sealed = hmac(derived)
        sp.edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_HASH, Base64.encodeToString(sealed ?: derived, Base64.NO_WRAP))
            .putInt(KEY_ITER, ITERATIONS)
            .putBoolean(KEY_KEYSTORE, sealed != null)
            .putBoolean(KEY_STRONG, pin.length == PinPolicy.LENGTH)
            .putBoolean(KEY_LOCK, true)
            .putInt(KEY_FAILURES, 0)
            .putLong(KEY_LOCKOUT_UNTIL, 0L)
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        val salt = sp.getString(KEY_SALT, null) ?: return false
        val stored = sp.getString(KEY_HASH, null) ?: return false
        val iterations = sp.getInt(KEY_ITER, LEGACY_ITERATIONS)
        val derived = pbkdf2(pin, Base64.decode(salt, Base64.NO_WRAP), iterations)
        val actual = if (sp.getBoolean(KEY_KEYSTORE, false)) hmac(derived) ?: return false else derived
        return MessageDigest.isEqual(Base64.decode(stored, Base64.NO_WRAP), actual)
    }

    fun clearPin() {
        sp.edit()
            .remove(KEY_HASH).remove(KEY_SALT).remove(KEY_ITER).remove(KEY_KEYSTORE).remove(KEY_STRONG)
            .putBoolean(KEY_LOCK, false)
            .putInt(KEY_FAILURES, 0)
            .putLong(KEY_LOCKOUT_UNTIL, 0L)
            .apply()
    }

    fun recordSuccess() {
        failures = 0
        lockoutUntil = 0L
    }

    /** Counts a wrong PIN and returns the lockout (ms) it triggered, 0 if none. */
    fun recordFailure(): Long {
        val now = trustedNow()
        val count = failures + 1
        val ms = LockoutPolicy.lockoutMillis(count)
        sp.edit()
            .putInt(KEY_FAILURES, count)
            .putLong(KEY_LAST_ATTEMPT, now)
            .putLong(KEY_LOCKOUT_UNTIL, if (ms > 0) now + ms else 0L)
            .apply()
        return ms
    }

    private fun pbkdf2(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    private fun keystoreKey(): SecretKey? = try {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KS_ALIAS, null) as? SecretKey) ?: KeyGenerator
            .getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, "AndroidKeyStore")
            .apply { init(KeyGenParameterSpec.Builder(KS_ALIAS, KeyProperties.PURPOSE_SIGN).build()) }
            .generateKey()
    } catch (e: Exception) {
        null
    }

    /** HMAC with the Keystore key, or null when the Keystore isn't usable on this device. */
    private fun hmac(data: ByteArray): ByteArray? {
        val key = keystoreKey() ?: return null
        return try {
            Mac.getInstance("HmacSHA256").run {
                init(key)
                doFinal(data)
            }
        } catch (e: Exception) {
            null
        }
    }

    private companion object {
        const val KEY_HASH = "pin_hash"
        const val KEY_SALT = "pin_salt"
        const val KEY_ITER = "pin_iter"
        const val KEY_KEYSTORE = "pin_keystore"
        const val KEY_STRONG = "pin_6digit"
        const val KEY_LOCK = "lock_enabled"
        const val KEY_SECURE_SCREEN = "secure_screen"
        const val KEY_FAILURES = "pin_failures"
        const val KEY_LOCKOUT_UNTIL = "pin_lockout_until"
        const val KEY_LAST_ATTEMPT = "pin_last_attempt"
        const val KS_ALIAS = "montra_pin_hmac"
        const val ITERATIONS = 150_000
        const val LEGACY_ITERATIONS = 120_000
    }
}
