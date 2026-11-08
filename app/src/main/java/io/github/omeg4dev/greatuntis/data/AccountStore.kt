package io.github.omeg4dev.greatuntis.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import io.github.omeg4dev.greatuntis.model.Account
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

@Serializable
data class AccountCache(
    val host: String,
    val school: String,
    val user: String,
    val key: String,
    val schoolNumber: String? = null,
) {
    fun toModel(): Account = Account(
        host = host,
        school = school,
        user = user,
        key = key,
        schoolNumber = schoolNumber,
    )

    companion object {
        fun fromModel(a: Account): AccountCache = AccountCache(
            host = a.host,
            school = a.school,
            user = a.user,
            key = a.key,
            schoolNumber = a.schoolNumber,
        )
    }
}

class AccountStore(private val context: Context) {
    private val keyAlias = "greatuntis_account"
    private val accountFile = File(context.filesDir, "account.bin")
    private val json = Json { ignoreUnknownKeys = true }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!keyStore.containsAlias(keyAlias)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore"
            )
            val spec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }
        val entry = keyStore.getEntry(keyAlias, null) as KeyStore.SecretKeyEntry
        return entry.secretKey
    }

    fun save(account: Account) {
        val key = getOrCreateKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv // 12 bytes
        val plaintext = json.encodeToString(AccountCache.fromModel(account)).toByteArray(Charsets.UTF_8)
        val ciphertext = cipher.doFinal(plaintext)

        val tempFile = File(context.filesDir, "account.bin.tmp")
        try {
            FileOutputStream(tempFile).use { fos ->
                fos.write(iv)
                fos.write(ciphertext)
                fos.fd.sync()
            }
            if (!tempFile.renameTo(accountFile)) {
                accountFile.delete()
                if (!tempFile.renameTo(accountFile)) {
                    tempFile.delete()
                }
            }
        } catch (e: Exception) {
            tempFile.delete()
            throw e
        }
    }

    fun load(): Account? {
        if (!accountFile.exists()) return null
        return try {
            val bytes = accountFile.readBytes()
            if (bytes.size < 12 + 16) {
                throw IllegalStateException("Account file too short")
            }
            val iv = bytes.copyOfRange(0, 12)
            val ciphertext = bytes.copyOfRange(12, bytes.size)

            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val entry = keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry
                ?: throw IllegalStateException("Key not found in KeyStore")

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, entry.secretKey, GCMParameterSpec(128, iv))
            val plaintext = cipher.doFinal(ciphertext)
            json.decodeFromString<AccountCache>(String(plaintext, Charsets.UTF_8)).toModel()
        } catch (e: Throwable) {
            accountFile.delete()
            null
        }
    }

    fun clear() {
        accountFile.delete()
    }
}
