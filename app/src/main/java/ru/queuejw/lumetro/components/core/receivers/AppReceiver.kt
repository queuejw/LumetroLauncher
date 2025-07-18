package ru.queuejw.lumetro.components.core.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.ContextCompat

class AppReceiver(
    private val onAppInstalled: (String) -> Unit,
    private val onAppRemoved: (String) -> Unit,
    private val onAppChanged: (String) -> Unit,
) : BroadcastReceiver() {

    private var tag = "AppReceiver"

    private fun checkAction(packageName: String?, action: String?) {
        action?.let { action ->
            when (action) {
                Intent.ACTION_PACKAGE_ADDED -> {
                    packageName?.let {
                        Log.d(tag, "ACTION_PACKAGE_ADDED: $it")
                        onAppInstalled(it)
                    }
                }

                Intent.ACTION_PACKAGE_REMOVED -> {
                    packageName?.let {
                        Log.d(tag, "ACTION_PACKAGE_REMOVED: $it")
                        onAppRemoved(it)
                    }
                }

                Intent.ACTION_PACKAGE_CHANGED -> {
                    packageName?.let {
                        Log.d(tag, "ACTION_PACKAGE_CHANGED: $it")
                        onAppChanged(it)
                    }
                }
            }
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(tag, "onReceive")
        intent?.let {
            checkAction(it.data?.encodedSchemeSpecificPart, it.action)
        }
    }

    companion object {
        fun register(context: Context, receiver: AppReceiver) {
            IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_CHANGED)
                addDataScheme("package")
            }.also {
                ContextCompat.registerReceiver(
                    context,
                    receiver,
                    it,
                    ContextCompat.RECEIVER_EXPORTED
                )
            }
        }

        fun unregister(context: Context, receiver: AppReceiver) {
            context.unregisterReceiver(receiver)
        }
    }
}