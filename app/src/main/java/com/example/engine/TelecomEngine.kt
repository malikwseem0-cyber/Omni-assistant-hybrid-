package com.example.engine

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.telephony.SmsManager
import android.util.Log

data class ContactEntry(
    val name: String,
    val phoneNumber: String
)

class TelecomEngine(private val context: Context) {

    fun searchContact(nameQuery: String): List<ContactEntry> {
        val results = mutableListOf<ContactEntry>()
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$nameQuery%")

        try {
            val cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            cursor?.use {
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (it.moveToNext()) {
                    val name = if (nameIdx != -1) it.getString(nameIdx) else ""
                    val number = if (numberIdx != -1) it.getString(numberIdx) else ""
                    if (name.isNotEmpty() && number.isNotEmpty()) {
                        results.add(ContactEntry(name, number))
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Contacts permission not granted", e)
        } catch (_: Exception) {}

        return results
    }

    fun makeCall(phoneNumber: String, directCall: Boolean = false): Boolean {
        return try {
            val action = if (directCall) Intent.ACTION_CALL else Intent.ACTION_DIAL
            val intent = Intent(action, Uri.parse("tel:${Uri.encode(phoneNumber)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            // Fallback to dialer
            try {
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phoneNumber)}")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(dialIntent)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    fun sendSms(phoneNumber: String, message: String): Pair<Boolean, String> {
        return try {
            val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Pair(true, "SMS sent to $phoneNumber")
        } catch (e: SecurityException) {
            // Fallback to SMS app intent
            try {
                val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(phoneNumber)}")).apply {
                    putExtra("sms_body", message)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(smsIntent)
                Pair(true, "Opening SMS app with draft for $phoneNumber")
            } catch (ex: Exception) {
                Pair(false, "Failed to send SMS: ${e.localizedMessage}")
            }
        } catch (e: Exception) {
            Pair(false, "SMS error: ${e.localizedMessage}")
        }
    }

    companion object {
        private const val TAG = "TelecomEngine"
    }
}
