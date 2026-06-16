package com.example.budgetmanager.importsms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.example.budgetmanager.data.sms.SmsProcessor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class ScanResult(val messagesScanned: Int, val transactionsImported: Int)

@Singleton
class SmsInboxScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val smsProcessor: SmsProcessor
) {
    fun hasReadSmsPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED

    /** Reads inbox messages received on/after [startMillis] and feeds them through the processor. */
    suspend fun scanSince(startMillis: Long): ScanResult = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )
        val selection = "${Telephony.Sms.DATE} >= ?"
        val args = arrayOf(startMillis.toString())

        var scanned = 0
        var imported = 0
        resolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            projection,
            selection,
            args,
            "${Telephony.Sms.DATE} ASC"
        )?.use { cursor ->
            val addressIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
            while (cursor.moveToNext()) {
                val sender = cursor.getString(addressIdx) ?: "Unknown"
                val body = cursor.getString(bodyIdx) ?: continue
                val date = cursor.getLong(dateIdx)
                scanned++
                // Dedup by SMS hash inside the processor keeps re-scans idempotent.
                // useAi = false: a bulk inbox scan must not fire a burst of cloud-model calls.
                if (smsProcessor.processSms(sender, body, date, useAi = false)) imported++
            }
        }
        ScanResult(messagesScanned = scanned, transactionsImported = imported)
    }
}
