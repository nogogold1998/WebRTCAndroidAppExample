package com.android.webrtc.example.session

import android.util.Log
import org.webrtc.*
import java.nio.charset.StandardCharsets

/**
 * [PeerConnection.Observer] implementation with default callbacks and ability to override them
 * NOTE: This class is not mandatory but simplifies work with WebRTC.
 */
class PeerConnectionObserver(
    private val onIceCandidateCallback: (IceCandidate) -> Unit = {},
    private val onTrackCallback: (RtpTransceiver?) -> Unit = {}
) : PeerConnection.Observer {
    override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
    }

    override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
    }

    override fun onIceConnectionReceivingChange(p0: Boolean) {
    }

    override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
    }

    // called when LocalIceCandidate received
    override fun onIceCandidate(iceCandidate: IceCandidate?) {
        iceCandidate ?: return
        onIceCandidateCallback(iceCandidate)
    }

    override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {
    }

    override fun onAddStream(mediaStream: MediaStream?) {
    }

    override fun onRemoveStream(p0: MediaStream?) {
    }

    override fun onDataChannel(dc: DataChannel?) {
        val TAG = "PeerConnectionObserver"
        Log.d(TAG, "onDataChannel: $dc")
        if(dc == null) return
        Log.d(TAG, "onDataChannel: ${dc.label()}, ${dc.id()}")
        dc.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(p0: Long) {
                Log.i(TAG, "onBufferedAmountChange($p0)")
            }

            override fun onStateChange() {
                Log.i(TAG, "onStateChange()")
            }

            override fun onMessage(p0: DataChannel.Buffer?) {
                Log.i(TAG, "onMessage($p0)")
                if (p0 == null) return
                val i = StandardCharsets.UTF_8.decode(p0.data).toString()
                Log.i(TAG, "onMessage: i = $i")
            }
        })
    }

    override fun onRenegotiationNeeded() {
    }

    // called when the remote track received
    override fun onTrack(transceiver: RtpTransceiver?) {
        super.onTrack(transceiver)
        onTrackCallback(transceiver)
    }

    override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {
        Log.i("PeerConnectionObserver", "onAddTrack: $p0, ${p1?.toList()}")
    }
}
