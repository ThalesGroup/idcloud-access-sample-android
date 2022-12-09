package com.thalesgroup.gemalto.IdCloudAccessSample.ui.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.thalesgroup.gemalto.IdCloudAccessSample.agents.IDCAException
import com.thalesgroup.gemalto.IdCloudAccessSample.agents.SCAAgent
import com.thalesgroup.gemalto.IdCloudAccessSample.databinding.FragmentWebViewBinding
import com.thalesgroup.gemalto.IdCloudAccessSample.utilities.findNavControllerSafely
import com.thalesgroup.gemalto.IdCloudAccessSample.utilities.getProgressDialog
import com.thalesgroup.gemalto.IdCloudAccessSample.viewmodels.WebViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WebViewFragment : Fragment() {
    private var _binding: FragmentWebViewBinding? = null
    private val binding get() = _binding!!
    private val webViewModel: WebViewModel by viewModels()

    // Injected the scaAgent in Fragment rather than viewmodel because scaAgent is scoped with Activity to provide activity in SCAAgent constructor
    // and viewmodel shouldn't depend on the activity as both activity and viewmodel have different lifecycle

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentWebViewBinding.inflate(inflater, container, false)
        val view = binding.root

        try {
            SCAAgent.init(this)
        } catch (ex: IDCAException) {
            val logList = ArrayList<String>()
            logList.add(ex.message!!)
            findNavController().previousBackStackEntry?.savedStateHandle?.set(
                "sca_init_error",
                bundleOf("loglist" to logList, "error" to ex.getIDCAErrorDescription())
            )

            // popping the Webview fragment from the stack
            findNavController().popBackStack()
        }

        // observing the token when received through redirect url's
        webViewModel.enrollmentToken.observe(
            viewLifecycleOwner,
            Observer { enrollmentToken ->
                // sending the enrollment token back to the Home Fragment just for the logs
                findNavController().previousBackStackEntry?.savedStateHandle?.set(
                    "enrollmentToken",
                    enrollmentToken
                )

                lifecycleScope.launch(Dispatchers.Main) {
                    enroll(enrollmentToken)
                }
            }
        )

        // observing the Uri state and redirecting data back to Home Fragment
        webViewModel.uriState.observe(
            viewLifecycleOwner,
            Observer {
                findNavController().previousBackStackEntry?.savedStateHandle?.set(
                    "uri_state", bundleOf("code" to it.code, "state" to it.state)
                )
                // popping the Webview fragment from the stack
                findNavController().popBackStack()
            }
        )
        // observing the Uri error and redirecting data back to Home Fragment
        webViewModel.uriError.observe(
            viewLifecycleOwner,
            Observer {
                val logList = ArrayList<String>()
                logList.add(it.ex.message!!)
                findNavController().previousBackStackEntry?.savedStateHandle?.set(
                    "uri_error",
                    bundleOf("loglist" to logList, "error" to it.ex.getIDCAErrorDescription())
                )
                findNavController().popBackStack()
            }
        )

        // loading the redirecting url back to the webview
        webViewModel.redirectUrl.observe(
            viewLifecycleOwner,
            Observer {
                binding.webView.loadUrl(it)
            }
        )

        webViewModel.fetch.observe(
            viewLifecycleOwner,
            Observer {
                fetch()
            }
        )

        webViewModel.webPageFinished.observe(
            viewLifecycleOwner,
            Observer {
                if (activity?.getProgressDialog()?.isShowing == true) {
                    activity?.getProgressDialog()?.cancel()
                }
            }
        )

        val uri = arguments?.getString("auth_request_uri")

        binding.webView.apply {
            webViewClient = webViewModel.getIDCAUserAgent()
            settings.loadsImagesAutomatically = true
            settings.javaScriptEnabled = true
            scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
            uri?.let { loadUrl(it) }
        }
        return view
    }

    private suspend fun enroll(registrationCode: String) {
        val logList = ArrayList<String>()
        logList.add("Enrollment started")

        runCatching {
            SCAAgent.enroll(registrationCode)
        }.onSuccess {
            logList.add(it)
            findNavController().previousBackStackEntry?.savedStateHandle?.set(
                "idCloud_enroll",
                logList
            )
        }.onFailure {
            if (it is IDCAException) {
                logList.add(it.message!!)
                findNavControllerSafely()?.previousBackStackEntry?.savedStateHandle?.set(
                    "idCloud_error",
                    bundleOf("loglist" to logList, "error" to it.getIDCAErrorDescription())
                )
            }

            // popping the Webview fragment from the stack on error
            findNavControllerSafely()?.popBackStack()
        }
    }

    private fun fetch() {
        val logList = ArrayList<String>()
        logList.add("Authentication started")
        lifecycleScope.launch(Dispatchers.Main) {
            runCatching {
                SCAAgent.fetch()
            }.onSuccess {
                logList.add("Authentication completed")
            }.onFailure {
                if (it is IDCAException) {
                    logList.add(it.message!!)
                    findNavController().previousBackStackEntry?.savedStateHandle?.set(
                        "idCloud_fetch_error",
                        bundleOf("loglist" to logList, "error" to it.getIDCAErrorDescription())
                    )
                }
                findNavController().popBackStack()
            }
        }

        findNavController().previousBackStackEntry?.savedStateHandle?.set(
            "idCloud_fetch",
            logList
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // function to intercept the back press button
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val logList = ArrayList<String>()
                    findNavController().previousBackStackEntry?.savedStateHandle?.set(
                        "back_press_web_fragment",
                        logList
                    )
                    findNavController().popBackStack()
                }
            }
        )
    }
}
