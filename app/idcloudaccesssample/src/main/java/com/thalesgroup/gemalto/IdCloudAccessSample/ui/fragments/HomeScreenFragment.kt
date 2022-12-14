package com.thalesgroup.gemalto.IdCloudAccessSample.ui.fragments

import android.net.Uri
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.thalesgroup.gemalto.IdCloudAccessSample.R
import com.thalesgroup.gemalto.IdCloudAccessSample.databinding.FragmentHomeScreenBinding
import com.thalesgroup.gemalto.IdCloudAccessSample.ui.dialog.IDCAAlertDialog
import com.thalesgroup.gemalto.IdCloudAccessSample.utilities.toast
import com.thalesgroup.gemalto.IdCloudAccessSample.viewmodels.HomeScreenViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeScreenFragment : Fragment() {

    private var _binding: FragmentHomeScreenBinding? = null
    private val binding get() = _binding!!
    private val homeScreenViewModel: HomeScreenViewModel by viewModels()

    var requestCode: Int? = null
    var homeHostFragment: NavHostFragment? = null
    var progressDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeScreenBinding.inflate(inflater, container, false)
        val view = binding.root

        // region Shared preference - Show Logs view
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val showLogState = sharedPreferences?.getBoolean("key_show_logs", false)
        if (showLogState == true) {
            binding.txtLogs.visibility = View.VISIBLE
            binding.txtClearLogs.visibility = View.VISIBLE
        } else {
            binding.txtLogs.visibility = View.GONE
            binding.txtClearLogs.visibility = View.GONE
        }

        // find the child fragment navHost
        homeHostFragment =
            childFragmentManager.findFragmentById(R.id.home_host_fragment) as NavHostFragment
        // setup the scrolling for text view
        binding.txtLogs.movementMethod = ScrollingMovementMethod()

        homeScreenViewModel.getLogs()?.let {
            if (it.isNotEmpty()) {
                for (i in it) {
                    binding.txtLogs.append(i + "\n\n")
                }
            }
        }

        homeScreenViewModel.loggerData.observe(
            viewLifecycleOwner,
            Observer {
                binding.txtLogs.append(it + "\n\n")
            }
        )

        // Observer the enrollment token from the webview fragment
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<String>("enrollmentToken")
            ?.observe(
                viewLifecycleOwner
            ) { result ->
                homeScreenViewModel.setLog("Enrollment Token :: $result")
                // after fetching the enrollment token , removing the value of key "enrollmentToken" from savedStatehandle to prevent
                // fetching the last value present inside the LiveData during onCreateView Method
                findNavController().currentBackStackEntry?.savedStateHandle?.remove<String>("enrollmentToken")
            }

        // Observer for the URI state from the webview fragment
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Bundle>("uri_state")
            ?.observe(
                viewLifecycleOwner
            ) { result ->
                val code = result.getString("code")
                val state = result.getString("state")
                homeScreenViewModel.setLog("Code: $code")
                homeScreenViewModel.setLog("State: $state")

                // Passing the data of code and state fetched from webview fragment to the enrollment fragment during enrollment and navigate to that fragment
                if (requestCode == EnrollmentFragment.ENROLL_RC) {
                    // finding the nested current fragment loaded inside first in the HomeScreenFragment
                    val currentFragment =
                        homeHostFragment?.childFragmentManager?.primaryNavigationFragment
                    if (currentFragment is EnrollmentFragment) {
                        if (code != null && state != null) currentFragment.performTokenRequest(
                            code, state
                        )
                    }
                }

                // after fetching the uri_state  , removing the value of key "uri_state" from savedStatehandle to prevent
                // fetching the last value present inside the LiveData during onCreateView Method
                findNavController().currentBackStackEntry?.savedStateHandle?.remove<Bundle>("uri_state")
            }

        // Observer for the Uri Error from the webview fragment
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Bundle>("uri_error")
            ?.observe(
                viewLifecycleOwner
            ) { result ->
                val error = result.getString("error")
                val logList = result.getStringArrayList("loglist")

                if (logList != null) {
                    for (data in logList) {
                        homeScreenViewModel.setLog(data)
                    }
                }
                if (error != null) {
                    showAlertDialog(error)
                }

                findNavController().currentBackStackEntry?.savedStateHandle?.remove<Bundle>(
                    "uri_error"
                )
            }

        // Observer for the idCloud Enrollment  from the webview fragment
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<ArrayList<String>>(
            "idCloud_enroll"
        )?.observe(viewLifecycleOwner) { result ->
            for (data in result) {
                homeScreenViewModel.setLog(data)
            }

            // after fetching the idCloud_enroll , removing the value of key "idCloud_enroll" from savedStatehandle to prevent
            // fetching the last value present inside the LiveData during onCreateView Method
            findNavController().currentBackStackEntry?.savedStateHandle?.remove<ArrayList<String>>(
                "idCloud_enroll"
            )
        }

        // Observer for the idCloud UnEnrollment  from the Settings fragment
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<ArrayList<String>>(
            "idCloud_unenroll"
        )?.observe(viewLifecycleOwner) { result ->
            for (data in result) {
                toast { data }
                binding.txtLogs.text = ""
                homeScreenViewModel.clearLogs()
            }

            // after fetching the idCloud_unenroll , removing the value of key "idCloud_unenroll" from savedStatehandle to prevent
            // fetching the last value present inside the LiveData during onCreateView Method
            findNavController().currentBackStackEntry?.savedStateHandle?.remove<ArrayList<String>>(
                "idCloud_unenroll"
            )
        }

        // Observer for the idCloud error  from the webview fragment
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Bundle>(
            "idCloud_error"
        )?.observe(viewLifecycleOwner) { result ->
            val error = result.getString("error")
            val logList = result.getStringArrayList("loglist")

            if (logList != null) {
                for (data in logList) {
                    homeScreenViewModel.setLog(data)
                }
            }
            if (error != null) {
                showAlertDialog(error)
            }

            // after fetching the idCloud_error , removing the value of key "idCloud_error" from savedStatehandle to prevent
            // fetching the last value present inside the LiveData during onCreateView Method
            findNavController().currentBackStackEntry?.savedStateHandle?.remove<Bundle>(
                "idCloud_error"
            )
        }

        // Observer for the risk error  from the Authentication fragment
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Bundle>(
            "risk_error"
        )?.observe(viewLifecycleOwner) { result ->
            val error = result.getString("error")
            val logList = result.getStringArrayList("loglist")

            if (logList != null) {
                for (data in logList) {
                    homeScreenViewModel.setLog(data)
                }
            }
            if (error != null) {
                showAlertDialog(error)
            }

            // after fetching the risk_error , removing the value of key "risk_error" from savedStatehandle to prevent
            // fetching the last value present inside the LiveData during onCreateView Method
            findNavController().currentBackStackEntry?.savedStateHandle?.remove<Bundle>(
                "risk_error"
            )
        }

        // Observer for the sca init Enrollment  from the webview fragment
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Bundle>(
            "sca_init_error"
        )?.observe(viewLifecycleOwner) { result ->
            val error = result.getString("error")
            val logList = result.getStringArrayList("loglist")

            if (logList != null) {
                for (data in logList) {
                    homeScreenViewModel.setLog(data)
                }
            }
            if (error != null) {
                showAlertDialog(error)
            }

            // after fetching the sca_init_error , removing the value of key "sca_init_error" from savedStatehandle to prevent
            // fetching the last value present inside the LiveData during onCreateView Method
            findNavController().currentBackStackEntry?.savedStateHandle?.remove<Bundle>(
                "sca_init_error"
            )
        }

        // Observer for the idCloud fetch  from the webview fragment
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<ArrayList<String>>(
            "idCloud_fetch"
        )?.observe(viewLifecycleOwner) { result ->
            for (data in result) {
                homeScreenViewModel.setLog(data)
            }
            toast { getString(R.string.authentication_success) }
            // after fetching the idCloud_fetch , removing the value of key "idCloud_fetch" from savedStatehandle to prevent
            // fetching the last value present inside the LiveData during onCreateView Method
            findNavController().currentBackStackEntry?.savedStateHandle?.remove<ArrayList<String>>(
                "idCloud_fetch"
            )
        }

        // Observer for the user pressed back from webview  from the webview fragment
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<ArrayList<String>>(
            "back_press_web_fragment"
        )?.observe(viewLifecycleOwner) { result ->
            for (data in result) {
                homeScreenViewModel.setLog(data)
            }
            // after fetching the back_press_web_fragment , removing the value of key "back_press_web_fragment" from savedStatehandle to prevent
            // fetching the last value present inside the LiveData during onCreateView Method
            findNavController().currentBackStackEntry?.savedStateHandle?.remove<ArrayList<String>>(
                "back_press_web_fragment"
            )
        }

        binding.imgSettings.setOnClickListener {
            findNavController().navigate(
                R.id.action_homeScreenFragment_to_settingsFragment
            )
        }

        // if username is not empty then we directly show the user enrollment fragment
        if (homeScreenViewModel.getUserName().isNullOrEmpty()) {
            navigateToEnrollmentFragment()
        } else {
            navigateToAuthenticationFragment()
        }

        binding.txtClearLogs.setOnClickListener {
            binding.txtLogs.text = ""
            homeScreenViewModel.clearLogs()
        }
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun openWebViewFragment(uri: Uri, requestCode: Int) {
        this.requestCode = requestCode
        val bundle = bundleOf("auth_request_uri" to uri.toString())
        findNavController().navigate(R.id.webViewFragment, bundle)
    }

    fun setLogs(logData: String) {
        homeScreenViewModel.setLog(logData)
    }

    fun navigateToAuthenticationFragment() {
        val currentFragment = homeHostFragment?.childFragmentManager?.primaryNavigationFragment
        if (currentFragment !is AuthenticationFragment) {
            homeHostFragment?.navController?.navigate(R.id.authenticationFragment)
        }
    }

    fun navigateToEnrollmentFragment() {
        val currentFragment = homeHostFragment?.childFragmentManager?.primaryNavigationFragment
        if (currentFragment !is EnrollmentFragment) {
            homeHostFragment?.navController?.navigate(R.id.enrollmentFragment)
        }
    }

    fun showAlertDialog(message: String, title: String = "Error") {
        activity?.let { IDCAAlertDialog.getAlertDialog(it, message, title) }
    }
}
