package com.simone.crowdsense

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class RestartServiceBroadcast : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        context.startService(Intent(context, LocationService::class.java))
    }

}
