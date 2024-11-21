package com.example.webrtc.client.signaling

import com.example.webrtc.client.event.WebRtcEvent
import kotlinx.coroutines.flow.Flow
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

interface Signaling {
    suspend fun getRoomExists(roomID: String): Boolean
    suspend fun getRoomList(): Flow<List<String>?>
    suspend fun terminate()
    fun sendMyStatus(peerStatus: PeerStatus)
    suspend fun getPeerStatus(): Flow<PeerStatus>
    suspend fun sendIce(ice: IceCandidate?, type: SignalType)
    suspend fun sendSdp(sdp: SessionDescription)
    fun start(roomID: String, isHost: Boolean)
    fun getEvent(): Flow<WebRtcEvent>
}