package com.example.budgetmanager.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.budgetmanager.data.sms.SmsProcessor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var smsProcessor: SmsProcessor

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val pendingResult = goAsync()
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            
            scope.launch {
                try {
                    for (message in messages) {
                        val sender = message.displayOriginatingAddress ?: "Unknown"
                        val body = message.displayMessageBody
                        val timestamp = message.timestampMillis
                        Log.d("BudgetDebug", "SmsReceiver: Processing message from $sender")
                        smsProcessor.processSms(sender, body, timestamp)
                    }
                } catch (e: Exception) {
                    Log.e("BudgetDebug", "SmsReceiver: Error processing SMS", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
