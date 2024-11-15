package com.example.webrtc.impl

import com.example.event.EventBus.eventFlow
import com.example.event.WebRtcEvent
import com.example.firestore.Signaling
import com.example.manager.WebRtcController
import com.example.model.Candidate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class EventHandler @Inject constructor(
    private val webRtcScope: CoroutineScope,
    private val webRtcController: WebRtcController,
    private val signaling: Signaling
) {
    fun start() {
        webRtcScope.launch {
            eventFlow.collect { event ->
                when (event) {
                    is WebRtcEvent.Host -> handleHostEvent(event)
                    is WebRtcEvent.Guest -> handleGuestEvent(event)
                }
            }
        }
    }

    private fun handleGuestEvent(event: WebRtcEvent.Guest) {
        when (event) {
            is WebRtcEvent.Guest.ReceiveOffer -> {
                webRtcController.setRemoteDescription(event.sdp)
            }

            is WebRtcEvent.Guest.SendAnswer -> {
                webRtcController.createAnswer(event.roomId)
            }

            is WebRtcEvent.Guest.SendIceToHost -> {
                signaling.sendIceCandidateToRoom(
                    candidate = event.ice,
                    type = Candidate.ANSWER,
                    roomId = event.roomId
                )
            }

            is WebRtcEvent.Guest.SendSdpToHost -> {
                signaling.sendSdpToRoom(
                    sdp = event.sdp,
                    roomId = event.roomId
                )
            }

            is WebRtcEvent.Guest.SetLocalIce -> {
                webRtcController.addIceCandidate(event.ice)
            }

            is WebRtcEvent.Guest.SetLocalSdp -> {
                webRtcController.setLocalDescription(event.sdp, event.observer)
            }

            is WebRtcEvent.Guest.SetRemoteIce -> {
                webRtcController.addIceCandidate(event.ice)
            }
        }
    }

    private fun handleHostEvent(event: WebRtcEvent.Host) {
        when (event) {
            is WebRtcEvent.Host.SendOffer -> {
                webRtcController.createOffer(event.roomId)
            }

            is WebRtcEvent.Host.ReceiveAnswer -> {
                webRtcController.setRemoteDescription(event.sdp)
            }

            is WebRtcEvent.Host.SendIceToGuest -> {
                signaling.sendIceCandidateToRoom(
                    candidate = event.ice,
                    type = Candidate.OFFER,
                    roomId = event.roomId
                )
            }

            is WebRtcEvent.Host.SendSdpToGuest -> {
                signaling.sendSdpToRoom(
                    sdp = event.sdp,
                    roomId = event.roomId
                )
            }

            is WebRtcEvent.Host.SetLocalIce -> {
                webRtcController.addIceCandidate(event.ice)
            }

            is WebRtcEvent.Host.SetLocalSdp -> {
                webRtcController.setLocalDescription(event.sdp, event.observer)
            }

            is WebRtcEvent.Host.SetRemoteIce -> {
                webRtcController.addIceCandidate(event.ice)
            }
        }
    }
}