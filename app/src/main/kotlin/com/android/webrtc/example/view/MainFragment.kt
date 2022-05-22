package com.android.webrtc.example.view

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.DialogTitle
import androidx.core.content.ContentProviderCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.android.webrtc.example.R
import com.android.webrtc.example.databinding.FragmentFirstBinding
import com.android.webrtc.example.ioc.ServiceLocator
import com.android.webrtc.example.ioc.ServiceLocator.webRtcSessionManager
import com.android.webrtc.example.service.MediaForegroundService
import com.android.webrtc.example.signaling.WebRTCSessionState
import kotlinx.coroutines.flow.collect
import org.webrtc.ScreenCapturerAndroid
import javax.security.auth.login.LoginException

/**
 * Fragment which subscribes for the session state and handles it.
 * From this fragment user can start [SessionFragment] if session is in [WebRTCSessionState.Ready]
 */
class MainFragment : Fragment() {

    companion object {
        private const val TAG = "MainFragment"
    }

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!

    // getting session manager just to init it via lazy block
    private val webRtcSessionManager = ServiceLocator.webRtcSessionManager
    private val mediaProjectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            Log.i(TAG, "onStop: MediaProjection stopped!")
            Toast.makeText(requireContext(), "MediaProjection stopped!", Toast.LENGTH_SHORT)
                .show()
        }
    }
    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val context = requireContext()
                ContextCompat.startForegroundService(context, MediaForegroundService.getIntent(context))
                webRtcSessionManager.videoCapturer =
                    ScreenCapturerAndroid(result.data, mediaProjectionCallback)
            } else {
                Log.i(TAG, "Screen capture permission denied!")
                Toast.makeText(requireContext(),
                    "Screen capture permission denied!",
                    Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.startSessionButton.setOnClickListener {
            findNavController().navigate(R.id.action_start_session)
        }
        lifecycleScope.launchWhenStarted {
            ServiceLocator.signalingClient.sessionStateFlow.collect { state ->
                when (state) {
                    WebRTCSessionState.Offline -> handleOfflineState()
                    WebRTCSessionState.Impossible -> handleImpossibleState()
                    WebRTCSessionState.Ready -> handleReadyState()
                    WebRTCSessionState.Creating -> handleCreatingState()
                    WebRTCSessionState.Active -> handleActiveState()
                }
            }
        }
        // NOTE: Don't do like this in production! Always check for the result
        // Here we are just assuming that user will allow access
        requireActivity().requestPermissions(arrayOf(Manifest.permission.CAMERA), 0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startForResult.launch(ServiceLocator.mediaProjectionManager.createScreenCaptureIntent())
    }

    // state handling is just changing message in textview and text/visibility of the button
    private fun handleOfflineState() {
        binding.sessionInfoTextView.setText(R.string.session_offline)
        binding.startSessionButton.apply {
            setText(R.string.button_start_session)
            isEnabled = false
        }
    }

    private fun handleImpossibleState() {
        binding.sessionInfoTextView.setText(R.string.session_impossible)
        binding.startSessionButton.apply {
            setText(R.string.button_start_session)
            isEnabled = false
        }
    }

    private fun handleReadyState() {
        binding.sessionInfoTextView.setText(R.string.session_ready)
        binding.startSessionButton.apply {
            setText(R.string.button_start_session)
            isEnabled = true
        }
    }

    private fun handleCreatingState() {
        binding.sessionInfoTextView.setText(R.string.session_creating)
        binding.startSessionButton.apply {
            setText(R.string.button_join_session)
            isEnabled = true
        }
    }

    private fun handleActiveState() {
        binding.sessionInfoTextView.setText(R.string.session_active)
        binding.startSessionButton.apply {
            setText(R.string.button_join_session)
            isEnabled = false
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
