package com.thalesgroup.gemalto.IdCloudAccessSample.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.thalesgroup.gemalto.IdCloudAccessSample.R
import com.thalesgroup.gemalto.IdCloudAccessSample.agents.IDCAException
import com.thalesgroup.gemalto.IdCloudAccessSample.agents.SCAAgent
import com.thalesgroup.gemalto.IdCloudAccessSample.databinding.FragmentLandingPageBinding
import com.thalesgroup.gemalto.IdCloudAccessSample.utilities.getProgressDialog
import com.thalesgroup.gemalto.IdCloudAccessSample.utilities.toast
import com.thalesgroup.gemalto.IdCloudAccessSample.viewmodels.SharedViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LandingPageFragment : Fragment() {
    private var _binding: FragmentLandingPageBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: SharedViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        sharedViewModel.init()
        SCAAgent.init(this, sharedViewModel.getMSUrl(), sharedViewModel.getTenantId())

        if (SCAAgent.isEnrolled()) {
            val bundle = bundleOf("registration" to "completed")
            findNavController().navigate(R.id.action_landingPageFragment_to_homeScreenFragment, bundle)
        }

        _binding = FragmentLandingPageBinding.inflate(inflater, container, false)
        val view = binding.root

        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<String>(
            "registrationCode"
        )?.observe(viewLifecycleOwner) { registrationCode ->
            lifecycleScope.launch(Dispatchers.Main) {
                runCatching {
                    SCAAgent.enroll(registrationCode, sharedViewModel.getPushToken())
                }.onSuccess {
                    activity?.getProgressDialog()?.cancel()
                    toast {
                        getString(R.string.you_are_signed_in)
                    }
                    val bundle = bundleOf("registration" to "completed")
                    findNavController().navigate(R.id.action_landingPageFragment_to_homeScreenFragment, bundle)
                }.onFailure {
                    activity?.getProgressDialog()?.cancel()
                    if (it is IDCAException) {
                        toast { "Registration Failed: " + it.getIDCAErrorDescription() }
                    }
                }
            }
        }

        binding.topView.imgSettings.setOnClickListener {
            findNavController().navigate(
                R.id.action_landingPageFragment_to_settingsFragment
            )
        }

        binding.run {
            btnQRCode.setOnClickListener {
                activity?.getProgressDialog()?.show()

                // show QR Codes
                findNavController().navigate(R.id.action_landingPageFragment_to_qrCodeFragment)
            }

            btnNo.setOnClickListener {
                findNavController().navigate(R.id.action_landingPageFragment_to_homeScreenFragment)
            }
        }

        return view
    }
}
