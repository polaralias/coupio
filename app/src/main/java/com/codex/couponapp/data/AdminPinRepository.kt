package com.polaralias.coupio.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.adminPinDataStore by preferencesDataStore(name = "admin_pin")

class AdminPinRepository(private val context: Context) {
    private object Keys {
        val salt = stringPreferencesKey("pin_salt")
        val hash = stringPreferencesKey("pin_hash")
    }

    val hasPin: Flow<Boolean> = context.adminPinDataStore.data
        .map { preferences -> preferences[Keys.hash] != null && preferences[Keys.salt] != null }
        .distinctUntilChanged()

    suspend fun setPin(pin: String) {
        require(pin.length >= 4 && pin.all(Char::isDigit)) { "PIN must be at least 4 digits." }
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        val hash = derive(pin, salt)
        context.adminPinDataStore.edit { preferences ->
            preferences[Keys.salt] = salt.encoded()
            preferences[Keys.hash] = hash.encoded()
        }
    }

    suspend fun verifyPin(pin: String): Boolean {
        val preferences = context.adminPinDataStore.data.first()
        val salt = preferences[Keys.salt]?.decoded() ?: return false
        val storedHash = preferences[Keys.hash]?.decoded() ?: return false
        val candidate = derive(pin, salt)
        return MessageDigest.isEqual(storedHash, candidate)
    }

    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val keySpec = PBEKeySpec(pin.toCharArray(), salt, 120_000, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(keySpec).encoded
    }
}

private fun ByteArray.encoded(): String = Base64.getEncoder().encodeToString(this)
private fun String.decoded(): ByteArray = Base64.getDecoder().decode(this)
