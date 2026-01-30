package com.suvojeet.suvmusic.listentogether

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ListenTogetherActionReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var client: ListenTogetherClient

    override fun onReceive(context: Context, intent: Intent) {
        val notifId = intent.getIntExtra(ListenTogetherClient.EXTRA_NOTIFICATION_ID, 0)
        
        when (intent.action) {
            ListenTogetherClient.ACTION_APPROVE_JOIN -> {
                val userId = intent.getStringExtra(ListenTogetherClient.EXTRA_USER_ID) ?: return
                client.approveJoin(userId)
            }
            ListenTogetherClient.ACTION_REJECT_JOIN -> {
                val userId = intent.getStringExtra(ListenTogetherClient.EXTRA_USER_ID) ?: return
                client.rejectJoin(userId)
            }
            ListenTogetherClient.ACTION_APPROVE_SUGGESTION -> {
                val suggestionId = intent.getStringExtra(ListenTogetherClient.EXTRA_SUGGESTION_ID) ?: return
                client.approveSuggestion(suggestionId)
            }
            ListenTogetherClient.ACTION_REJECT_SUGGESTION -> {
                val suggestionId = intent.getStringExtra(ListenTogetherClient.EXTRA_SUGGESTION_ID) ?: return
                client.rejectSuggestion(suggestionId)
            }
        }
        
        // Cancel the notification after action
        if (notifId != 0) {
            androidx.core.app.NotificationManagerCompat.from(context).cancel(notifId)
        }
    }
}
