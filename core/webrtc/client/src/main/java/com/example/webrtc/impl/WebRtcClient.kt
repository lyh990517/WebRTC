package com.example.webrtc.impl

import com.example.firestore.Signaling
import com.example.manager.LocalResourceController
import com.example.manager.WebRtcController
import com.example.model.RoomStatus
import com.example.webrtc.api.WebRtcClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class WebRtcClient @Inject constructor(
    private val webRtcScope: CoroutineScope,
    private val eventHandler: EventHandler,
    private val webRtcController: WebRtcController,
    private val localResourceController: LocalResourceController,
    private val signaling: Signaling
) : WebRtcClient {
    override fun connect(roomID: String, isHost: Boolean) {
        webRtcScope.launch {
            eventHandler.start()

            webRtcController.connect(roomID, isHost)

            localResourceController.startCapture()

            signaling.start(roomID)
        }
    }

    override fun toggleVoice() {
        localResourceController.toggleVoice()
    }

    override fun toggleVideo() {
        localResourceController.toggleVideo()
    }

    override suspend fun getRoomStatus(roomID: String): RoomStatus =
        signaling.getRoomStatus(roomID).first()

    override fun disconnect() {
        webRtcScope.cancel()
        localResourceController.dispose()
        webRtcController.closeConnection()
    }
}