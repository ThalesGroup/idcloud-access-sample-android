package com.thalesgroup.gemalto.IdCloudAccessSample.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.thalesgroup.gemalto.IdCloudAccessSample.R
import com.thalesgroup.gemalto.IdCloudAccessSample.databinding.FragmentEnrollmentBinding
import com.thalesgroup.gemalto.IdCloudAccessSample.utilities.NetworkHelper
import com.thalesgroup.gemalto.IdCloudAccessSample.utilities.getProgressDialog
import com.thalesgroup.gemalto.IdCloudAccessSample.utilities.hideKeyboard
import com.thalesgroup.gemalto.IdCloudAccessSample.utilities.snackbar
import com.thalesgroup.gemalto.IdCloudAccessSample.utilities.toast
import com.thalesgroup.gemalto.IdCloudAccessSample.viewmodels.EnrollmentResponse
import com.thalesgroup.gemalto.IdCloudAccessSample.viewmodels.EnrollmentViewModel
import com.thalesgroup.gemalto.IdCloudAccessSample.viewmodels.RiskResponse
import com.thalesgroup.gemalto.IdCloudAccessSample.viewmodels.SharedViewModel
import com.thalesgroup.gemalto.IdCloudAccessSample.viewmodels.UriResponse
import com.thalesgroup.gemalto.d1.risk.RiskParams
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class EnrollmentFragment : Fragment() {
    private var _binding: FragmentEnrollmentBinding? = null
    private val binding get() = _binding!!
    private val enrollmentViewModel: EnrollmentViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by viewModels()
    var homeScreenFragment: HomeScreenFragment? = null

    @Inject
    lateinit var networkHelper: NetworkHelper

    private var currentRC = ENROLL_RC

    private lateinit var username: String

    companion object {
        const val ENROLL_RC = 100
        const val AUTHENTICATE_RC = 200
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // getting the instance of the parent fragment i.e HomeScreen Fragment
        val navHostFragment = parentFragment as NavHostFragment?
        homeScreenFragment = navHostFragment?.parentFragment as HomeScreenFragment

        // moved observer to onCreate() of fragment instead of  onCreateView() because onCreate() will be called once throughout the fragment lifecycle but
        // onCreateView will be called multiple times when the view is created again and again and it will cause the observer to trigger multiple times
        // even when the livedata doesn't post anything. So in this case below , if it is in onCreateView() and when the webview fragment is opened at first time
        // and user press the back button then EnrollmentFragment onCreateView will be called and observer will be triggered again

        // So below case only will trigger the observer when the uri livedata will call postValue function

        // an observer for fetching the uri during register device and redirecting the uri to HomeScreenFragment
        sharedViewModel.uriResponse.observe(
            this,
            Observer {
                when (it) {
                    is UriResponse.Success -> {
                        it.successData?.let { success ->
                            homeScreenFragment?.openWebViewFragment(success, currentRC)
                        }
                    }
                    is UriResponse.Error -> {
                        it.errorData?.let { error ->
                            homeScreenFragment?.showAlertDialog(error)
                        }
                        activity?.getProgressDialog()?.cancel()
                    }

                    is UriResponse.Exception -> {
                        it.exception?.let { exception ->
                            homeScreenFragment?.setLogs(exception)
                        }
                        activity?.getProgressDialog()?.cancel()
                    }
                }
            }
        )

        enrollmentViewModel.enrollmentResponse.observe(
            this,
            Observer {
                when (it) {
                    is EnrollmentResponse.Success -> {
                        it.successData?.let { success ->
                            homeScreenFragment?.run {
                                navigateToAuthenticationFragment()
                                toast {
                                    getString(R.string.you_are_signed_in)
                                }
                                setLogs(success)
                            }
                        }
                    }
                    is EnrollmentResponse.Error -> {
                        it.failedData?.let { error ->
                            homeScreenFragment?.showAlertDialog(error)
                        }
                    }

                    is EnrollmentResponse.Exception -> {
                        it.exception?.let { exception ->
                            homeScreenFragment?.setLogs(exception)
                        }
                    }
                }
            }
        )

        sharedViewModel.riskResponse.observe(
            this,
            Observer {
                when (it) {
                    is RiskResponse.Success -> {
                        it.successData.let { log ->
                            homeScreenFragment?.setLogs(log)
                        }
                    }

                    is RiskResponse.Exception -> {
                        it.exception.let { exception ->
                            homeScreenFragment?.setLogs(exception)
                        }
                    }
                }
            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentEnrollmentBinding.inflate(inflater, container, false)
        val view = binding.root
        binding.run {
            btnRegisterDevice.setOnClickListener {
                hideKeyboard()
                if (edtUsername.text.isNullOrEmpty()) {
                    edtUsername.error = getString(R.string.enter_your_username)
                } else {
                    if (networkHelper.isNetworkConnected()) {
                        username = edtUsername.text.toString()
                        currentRC = ENROLL_RC
                        sharedViewModel.authenticateUser(getEnrollAcr(), username)
                        homeScreenFragment?.apply {
                            activity?.getProgressDialog()?.show()
                        }
                    } else {
                        view.snackbar { "No internet connection" }
                    }
                }
            }

            btnSignIn.setOnClickListener {
                hideKeyboard()
                if (edtUsername.text.isNullOrEmpty()) {
                    edtUsername.error = getString(R.string.enter_your_username)
                } else {
                    if (networkHelper.isNetworkConnected()) {
                        username = edtUsername.text.toString()
                        currentRC = AUTHENTICATE_RC
                        lifecycleScope.launch(Dispatchers.IO) {
                            runCatching { sharedViewModel.stopAnalyzeRisk() }.onSuccess { acrValueForRisk ->
                                withContext(Dispatchers.Main) {
                                    val acrValue = "${getAuthAcr()} $acrValueForRisk"
                                    homeScreenFragment?.setLogs("acr_values: $acrValue")
                                    sharedViewModel.authenticateUser(
                                        acrValue, edtUsername.text.toString()
                                    )
                                }
                            }.onFailure {
                                it.message?.let { it1 -> homeScreenFragment?.setLogs(it1) }
                            }
                        }

                        homeScreenFragment?.apply {
                            activity?.getProgressDialog()?.show()
                        }
                    } else {
                        view.snackbar { "No internet connection" }
                    }
                }
            }
        }
        return view
    }

    fun performTokenRequest(code: String, state: String) {
        enrollmentViewModel.performTokenRequest(username, code, state)
    }

    override fun onResume() {
        super.onResume()
        // Risk:: Start the Analyze
        val params = RiskParams<Fragment>(this, "LoginMobile", 1)
        sharedViewModel.startAnalyzeRisk(params)
    }

    override fun onPause() {
        super.onPause()
        // Risk:: Pause the Analyze
        sharedViewModel.pauseAnalyzeRisk()
        homeScreenFragment?.setLogs("Risk analysis paused")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getEnrollAcr(): String = "enroll sca=fidomob"

    private fun getAuthAcr(): String = "rba sca=fidomob"
}
