package com.thalesgroup.gemalto.IdCloudAccessSample.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.thalesgroup.gemalto.IdCloudAccessSample.R
import com.thalesgroup.gemalto.IdCloudAccessSample.agents.SCAAgent
import com.thalesgroup.gemalto.IdCloudAccessSample.data.DataStoreRepo
import com.thalesgroup.gemalto.IdCloudAccessSample.databinding.FragmentAuthenticationBinding
import com.thalesgroup.gemalto.IdCloudAccessSample.utilities.getProgressDialog
import com.thalesgroup.gemalto.IdCloudAccessSample.viewmodels.AuthenticationViewModel
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
class AuthenticationFragment : Fragment() {
    private var _binding: FragmentAuthenticationBinding? = null
    private val binding get() = _binding!!
    private val authenticationViewModel: AuthenticationViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by viewModels()
    var homeScreenFragment: HomeScreenFragment? = null

    @Inject
    lateinit var dataStoreRepo: DataStoreRepo

    companion object {
        private const val AUTHENTICATE_RC = 200
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // getting the instance of the parent fragment i.e HomeScreen Fragment
        val navHostFragment = parentFragment as NavHostFragment?
        homeScreenFragment = navHostFragment?.parentFragment as HomeScreenFragment

        // an observer for fetching the uri during the authentication and redirecting the uri to HomeScreenFragment
        sharedViewModel.uriResponse.observe(
            this,
            Observer {
                when (it) {
                    is UriResponse.Success -> {
                        it.successData?.let { success ->
                            homeScreenFragment?.openWebViewFragment(
                                success, AUTHENTICATE_RC
                            )
                        }
                    }
                    is UriResponse.Error -> {
                        it.errorData?.let { error ->
                            homeScreenFragment?.showAlertDialog(error)
                        }
                    }

                    is UriResponse.Exception -> {
                        it.exception?.let { exception ->
                            homeScreenFragment?.setLogs(exception)
                        }
                    }
                }

                activity?.getProgressDialog()?.cancel()
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
        sharedViewModel.init()
        SCAAgent.init(this, sharedViewModel.getMSUrl(), sharedViewModel.getTenantId())
        _binding = FragmentAuthenticationBinding.inflate(inflater, container, false)
        val view = binding.root

        // User Name
        val username = binding.userName
        val editUserName = binding.editUserName

        val userNameInStorage = sharedViewModel.getUserName()
        editUserName.visibility = if (userNameInStorage == null) View.VISIBLE else View.GONE
        username.visibility = if (userNameInStorage == null) View.GONE else View.VISIBLE
        username.text = userNameInStorage

        val authenticationType = resources.getStringArray(R.array.authentication_type_options)
        val spinner = view.findViewById<Spinner>(R.id.select_authentication_type)

        val adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_dropdown_item, authenticationType
        )
        spinner.adapter = adapter

        authenticationViewModel.getAuthenticationType()?.let { spinner.setSelection(it) }

        binding.run {
            authSignIn.setOnClickListener {
                val userName: String = userNameInStorage ?: editUserName.text.toString()
                authenticationViewModel.storeUserName(userName)
                authenticationViewModel.storeAuthenticationType(spinner.selectedItemPosition)
                val clientId = SCAAgent.getClientId()

                if (spinner.selectedItem.equals(getString(R.string.strong))) {
                    val acrValueForSCA = "sca sca=fidomob clientid=$clientId"
                    homeScreenFragment?.setLogs("acr_values: $acrValueForSCA")

                    sharedViewModel.authenticateUser(
                        acrValueForSCA, userName
                    )
                } else {
                    lifecycleScope.launch(Dispatchers.IO) {
                        runCatching {
                            sharedViewModel.stopAnalyzeRisk()
                        }.onSuccess { acrValueForRisk ->
                            withContext(Dispatchers.Main) {
                                var acrValue: String? = null
                                when (spinner.selectedItem) {
                                    getString(R.string.risk_based) ->
                                        acrValue =
                                            "rba $acrValueForRisk clientid=$clientId"
                                    getString(R.string.silent) ->
                                        acrValue =
                                            "silent $acrValueForRisk clientid=$clientId"
                                }
                                homeScreenFragment?.setLogs("Risk analysis stopped")
                                homeScreenFragment?.setLogs("acr_values: $acrValue")

                                if (acrValue != null) {
                                    sharedViewModel.authenticateUser(
                                        acrValue, userName
                                    )
                                }
                            }
                        }.onFailure {
                            withContext(Dispatchers.Main) {
                                it.message?.let { it1 -> homeScreenFragment?.setLogs(it1) }
                            }
                        }
                    }
                }
            }
        }

        return view
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
}
