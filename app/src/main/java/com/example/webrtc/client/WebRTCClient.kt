package com.example.webrtc.client

import android.app.Application
import android.content.Context
import android.util.Log
import com.example.webrtc.data.WebRTCRepository
import com.example.webrtc.event.PeerConnectionEvent
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.webrtc.*

class WebRTCClient(
    private val webRTCRepository: WebRTCRepository
    //private val roomID: String
) {
    private val constraints = MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
    }

    private val rootEglBase: EglBase = EglBase.create()

    private var localAudioTrack: AudioTrack? = null

    private var localVideoTrack: VideoTrack? = null
    private val database = Firebase.firestore

    private val peerConnectionFactory by lazy { buildPeerConnectionFactory() }

    private val localVideoSource by lazy { peerConnectionFactory.createVideoSource(false) }

    private val audioSource by lazy { peerConnectionFactory.createAudioSource(MediaConstraints()) }

    private lateinit var videoCapture: CameraVideoCapturer

    private val iceServer =
        listOf(PeerConnection.IceServer.builder(ICE_SERVER_URL).createIceServer())

    private val peerConnection by lazy { buildPeerConnection() }

    private val _eventFlow = MutableSharedFlow<PeerConnectionEvent>()

    val eventFlow = _eventFlow


    fun getVideoCapture(context: Context) =
        Camera2Enumerator(context).run {
            deviceNames.find {
                isFrontFacing(it)
            }?.let {
                createCapturer(it, null)
            } ?: throw IllegalStateException()
        }

    fun initSurfaceView(view: SurfaceViewRenderer) = view.run {
        setMirror(true)
        setEnableHardwareScaler(true)
        init(rootEglBase.eglBaseContext, null)
    }

    fun startLocalView(localVideoOutput: SurfaceViewRenderer) {
        val surfaceTextureHelper =
            SurfaceTextureHelper.create(Thread.currentThread().name, rootEglBase.eglBaseContext)
        (videoCapture as VideoCapturer).initialize(
            surfaceTextureHelper,
            localVideoOutput.context,
            localVideoSource.capturerObserver
        )
        videoCapture.startCapture(320, 240, 60)
        localAudioTrack =
            peerConnectionFactory.createAudioTrack(LOCAL_TRACK_ID + "_audio", audioSource);
        localVideoTrack = peerConnectionFactory.createVideoTrack(LOCAL_TRACK_ID, localVideoSource)
        localVideoTrack?.addSink(localVideoOutput)
        val localStream = peerConnectionFactory.createLocalMediaStream(LOCAL_STREAM_ID)
        localStream.addTrack(localVideoTrack)
        localStream.addTrack(localAudioTrack)
        peerConnection?.addStream(localStream)
    }

    fun initVideoCapture(context: Application){
        videoCapture = getVideoCapture(context)
    }
    fun initPeerConnectionFactory(context: Application) {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .setFieldTrials(FIELD_TRIALS)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)
    }

    private fun buildPeerConnectionFactory() = PeerConnectionFactory.builder().apply {
        setVideoDecoderFactory(DefaultVideoDecoderFactory(rootEglBase.eglBaseContext))
        setVideoEncoderFactory(DefaultVideoEncoderFactory(rootEglBase.eglBaseContext, true, true))
        setOptions(PeerConnectionFactory.Options().apply {
            disableEncryption = true
            disableNetworkMonitor = true
        })
    }.createPeerConnectionFactory()

    private fun createPeerConnectionObserver(): PeerConnection.Observer =
        object : PeerConnection.Observer {
            override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
                Log.e("Rsupport", "onSignalingChange")
            }

            override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
                Log.e("Rsupport", "onIceConnectionChange")
            }

            override fun onIceConnectionReceivingChange(p0: Boolean) {
                Log.e("Rsupport", "onIceConnectionReceivingChange")
            }

            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
                Log.e("Rsupport", "onIceGatheringChange")
            }

            override fun onIceCandidate(p0: IceCandidate?) {
                Log.e("Rsupport", "onIceCandidate : $p0")
                CoroutineScope(Dispatchers.IO).launch {
                    p0?.let {
                        _eventFlow.emit(PeerConnectionEvent.OnIceCandidate(it))
                    }
                }
            }

            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {
                Log.e("Rsupport", "onIceCandidatesRemoved")
            }

            override fun onAddStream(p0: MediaStream?) {
                Log.e("Rsupport", "onAddStream")
                CoroutineScope(Dispatchers.IO).launch {
                    p0?.let {
                        _eventFlow.emit(PeerConnectionEvent.OnAddStream(it))
                    }
                }
            }

            override fun onRemoveStream(p0: MediaStream?) {
                Log.e("Rsupport", "onRemoveStream")
            }

            override fun onDataChannel(p0: DataChannel?) {
                Log.e("Rsupport", "onDataChannel")
            }

            override fun onRenegotiationNeeded() {
                Log.e("Rsupport", "onRenegotiationNeeded")
            }

            override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {
                Log.e("Rsupport", "onAddTrack")
            }

        }

    private fun buildPeerConnection() =
        peerConnectionFactory.createPeerConnection(iceServer, createPeerConnectionObserver())

    private fun PeerConnection.Call(roomID: String) {
        createOffer(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {
                peerConnection?.setLocalDescription(this, p0)
                val offer = hashMapOf(
                    "sdp" to p0?.description,
                    "type" to p0?.type
                )
                database.collection("calls").document(roomID).set(offer)
            }

            override fun onSetSuccess() {
                Log.e("Rsupport", "onSetSuccess")
            }

            override fun onCreateFailure(p0: String?) {
                Log.e("Rsupport", "onCreateFailure: $p0")
            }

            override fun onSetFailure(p0: String?) {

            }

        }, constraints)
    }

    fun sendIceCandidate(candidate: IceCandidate?, isJoin: Boolean, roomID: String) = runBlocking {
        webRTCRepository.sendIceCandidate(candidate, isJoin, roomID)
    }

    private fun PeerConnection.Answer(roomID: String) {
        createAnswer(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {
                Log.e("Rsupport", "setLocalDescription")
                peerConnection?.setLocalDescription(this, p0)
                val answer = hashMapOf(
                    "sdp" to p0?.description,
                    "type" to p0?.type
                )
                database.collection("calls").document(roomID).set(answer)
            }

            override fun onSetSuccess() {
                Log.e("Rsupport", "onSetSuccess")
            }

            override fun onCreateFailure(p0: String?) {
                Log.e("Rsupport", "onCreateFailure: $p0")
            }

            override fun onSetFailure(p0: String?) {

            }

        }, constraints)
    }

    fun onRemoteSessionReceived(description: SessionDescription) {
        Log.e("Rsupport", "setRemoteDescription")
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {

            }

            override fun onSetSuccess() {
                Log.e("Rsupport", "onSetSuccess")
            }

            override fun onCreateFailure(p0: String?) {
                Log.e("Rsupport", "onCreateFailure: $p0")
            }

            override fun onSetFailure(p0: String?) {

            }

        }, description)
    }

    fun addCandidate(iceCandidate: IceCandidate?) {
        peerConnection?.addIceCandidate(iceCandidate)
    }

    fun answer(roomID: String) =
        peerConnection?.Answer(roomID)

    fun call(roomID: String) =
        peerConnection?.Call(roomID)

    companion object {
        private const val LOCAL_TRACK_ID = "local_track"
        private const val LOCAL_STREAM_ID = "local_track"
        private const val FIELD_TRIALS = "WebRTC-H264HighProfile/Enabled/"
        private const val ICE_SERVER_URL = "stun:stun.l.google.com:19302"
    }
}