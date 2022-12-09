package com.thalesgroup.gemalto.IdCloudAccessSample.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceClickListener
import androidx.preference.PreferenceFragmentCompat
import com.thales.dis.mobile.idcloud.auth.IdCloudClient
import com.thalesgroup.gemalto.IdCloudAccessSample.R
import com.thalesgroup.gemalto.IdCloudAccessSample.agents.IDCAException
import com.thalesgroup.gemalto.IdCloudAccessSample.agents.SCAAgent
import com.thalesgroup.gemalto.IdCloudAccessSample.utilities.findNavControllerSafely
import com.thalesgroup.gemalto.IdCloudAccessSample.viewmodels.SettingViewModel
import com.thalesgroup.gemalto.IdCloudAccessSample.viewmodels.UnenrollmentResponse
import com.thalesgroup.gemalto.d1.D1Task
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {
    private val settingViewModel: SettingViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        try {
            SCAAgent.init(this)
        } catch (ex: IDCAException) {
            val logList = ArrayList<String>()
            logList.add(ex.message!!)
            findNavController().previousBackStackEntry?.savedStateHandle?.set(
                "sca_init_error",
                bundleOf("loglist" to logList, "error" to ex.getIDCAErrorDescription())
            )

            // popping the Settings fragment from the stack
            findNavController().popBackStack()
        }

        settingViewModel.unenrollResponse.observe(
            viewLifecycleOwner,
            Observer {
                val logList = ArrayList<String>()
                when (it) {
                    is UnenrollmentResponse.Success -> {

                        it.successData?.let {
                            logList.add("Reset successful")
                            findNavControllerSafely()?.previousBackStackEntry?.savedStateHandle?.set(
                                "idCloud_unenroll",
                                logList
                            )
                        }
                    }

                    is UnenrollmentResponse.Exception -> {
                        it.exception?.let { exception ->
                            logList.add(exception.message!!)
                            findNavControllerSafely()?.previousBackStackEntry?.savedStateHandle?.set(
                                "idCloud_error",
                                bundleOf("loglist" to logList, "error" to exception.getIDCAErrorDescription())
                            )
                        }
                    }
                }

                findNavController().popBackStack()
            }
        )

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        findPreference<Preference>(getString(R.string.pref_key_protector_fido_version))?.title = getString(R.string.pref_title_protector_fido_version, IdCloudClient.getSDKVersion())

        findPreference<Preference>(getString(R.string.pref_key_idcloud_risk_version))?.title = getString(R.string.pref_title_idcloud_risk_version, D1Task.getSDKVersions()["D1"])

        findPreference<Preference>(getString(R.string.pref_key_reset))?.onPreferenceClickListener =
            OnPreferenceClickListener {
                settingViewModel.unenroll()
                true
            }
    }
}
