package com.thalesgroup.gemalto.IdCloudAccessSample.ui.fragments

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceClickListener
import androidx.preference.PreferenceFragmentCompat
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.thales.dis.mobile.idcloud.auth.IdCloudClient
import com.thalesgroup.gemalto.IdCloudAccessSample.R
import com.thalesgroup.gemalto.IdCloudAccessSample.agents.IDCAException
import com.thalesgroup.gemalto.IdCloudAccessSample.agents.SCAAgent
import com.thalesgroup.gemalto.IdCloudAccessSample.utilities.CLIENT_ID
import com.thalesgroup.gemalto.IdCloudAccessSample.utilities.CLIENT_SECRET
import com.thalesgroup.gemalto.IdCloudAccessSample.utilities.IDP_URL
import com.thalesgroup.gemalto.IdCloudAccessSample.utilities.MS_URL
import com.thalesgroup.gemalto.IdCloudAccessSample.utilities.ND_CLIENT_ID
import com.thalesgroup.gemalto.IdCloudAccessSample.utilities.ND_URL
import com.thalesgroup.gemalto.IdCloudAccessSample.utilities.PUSH_TOKEN
import com.thalesgroup.gemalto.IdCloudAccessSample.utilities.REDIRECT_URL
import com.thalesgroup.gemalto.IdCloudAccessSample.utilities.RISK_URL
import com.thalesgroup.gemalto.IdCloudAccessSample.utilities.TENANT_ID
import com.thalesgroup.gemalto.IdCloudAccessSample.utilities.findNavControllerSafely
import com.thalesgroup.gemalto.IdCloudAccessSample.utilities.toast
import com.thalesgroup.gemalto.IdCloudAccessSample.viewmodels.SettingViewModel
import com.thalesgroup.gemalto.IdCloudAccessSample.viewmodels.SharedViewModel
import com.thalesgroup.gemalto.IdCloudAccessSample.viewmodels.UnenrollmentResponse
import com.thalesgroup.gemalto.IdCloudAccessSample.viewmodels.UpdatePushTokenResponse
import com.thalesgroup.gemalto.d1.D1Task
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {
    private val settingViewModel: SettingViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        try {
            SCAAgent.init(this, sharedViewModel.getMSUrl(), sharedViewModel.getTenantId())
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
                            sharedViewModel.clearStorage()
                            findNavControllerSafely()?.navigate(R.id.action_settingsFragment_to_landingPageFragment)
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
                        findNavController().popBackStack()
                    }
                }
            }
        )

        settingViewModel.updatePushTokenResponse.observe(
            viewLifecycleOwner,
            Observer {
                val logList = ArrayList<String>()
                when (it) {
                    is UpdatePushTokenResponse.Success -> {
                        it.successData?.let { token ->
                            sharedViewModel.updatePreference(PUSH_TOKEN, token)
                            toast { "Refresh Push Token Successful" }
                        }
                    }

                    is UpdatePushTokenResponse.Exception -> {
                        it.exception?.let { exception ->
                            logList.add(exception.message!!)
                            findNavControllerSafely()?.previousBackStackEntry?.savedStateHandle?.set(
                                "idCloud_error",
                                bundleOf("loglist" to logList, "error" to exception.getIDCAErrorDescription())
                            )
                        }
                        findNavController().popBackStack()
                    }
                }
            }
        )

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        findPreference<Preference>(getString(R.string.pref_key_protector_fido_version))?.title = getString(R.string.pref_title_protector_fido_version, IdCloudClient.getSDKVersion())

        findPreference<Preference>(getString(R.string.pref_key_idcloud_risk_version))?.title = getString(R.string.pref_title_idcloud_risk_version, D1Task.getSDKVersions()["D1"])

        val idpUrl = sharedViewModel.getIDPUrl()
        findPreference<Preference>(getString(R.string.pref_key_idp_url))?.summary = idpUrl
        findPreference<Preference>(getString(R.string.pref_key_idp_url))?.onPreferenceClickListener =
            OnPreferenceClickListener {
                showEditDialog(context, getString(R.string.pref_title_idp_url), IDP_URL, idpUrl, it)
                true
            }

        val redirectUrl = sharedViewModel.getRedirectUrl()
        findPreference<Preference>(getString(R.string.pref_key_idp_redirect_url))?.summary = redirectUrl
        findPreference<Preference>(getString(R.string.pref_key_idp_redirect_url))?.onPreferenceClickListener =
            OnPreferenceClickListener {
                showEditDialog(context, getString(R.string.pref_title_redirect_url), REDIRECT_URL, redirectUrl, it)
                true
            }

        val clientId = sharedViewModel.getClientId()
        findPreference<Preference>(getString(R.string.pref_key_client_id))?.summary = clientId
        findPreference<Preference>(getString(R.string.pref_key_client_id))?.onPreferenceClickListener =
            OnPreferenceClickListener {
                showEditDialog(context, getString(R.string.pref_title_client_id), CLIENT_ID, clientId, it)
                true
            }

        val clientSecret = sharedViewModel.getClientSecret()
        findPreference<Preference>(getString(R.string.pref_key_client_secret))?.summary = clientSecret
        findPreference<Preference>(getString(R.string.pref_key_client_secret))?.onPreferenceClickListener =
            OnPreferenceClickListener {
                showEditDialog(context, getString(R.string.pref_title_client_secret), CLIENT_SECRET, clientSecret, it)
                true
            }

        val msUrl = sharedViewModel.getMSUrl()
        findPreference<Preference>(getString(R.string.pref_key_ms_url))?.summary = msUrl
        findPreference<Preference>(getString(R.string.pref_key_ms_url))?.onPreferenceClickListener =
            OnPreferenceClickListener {
                showEditDialog(context, getString(R.string.pref_title_ms_url), MS_URL, msUrl, it)
                true
            }

        val tenantId = sharedViewModel.getTenantId()
        findPreference<Preference>(getString(R.string.pref_key_tenant_id))?.summary = tenantId
        findPreference<Preference>(getString(R.string.pref_key_tenant_id))?.onPreferenceClickListener =
            OnPreferenceClickListener {
                showEditDialog(context, getString(R.string.pref_title_client_id), TENANT_ID, tenantId, it)
                true
            }

        val ndUrl = sharedViewModel.getNDUrl()
        findPreference<Preference>(getString(R.string.pref_key_nd_url))?.summary = ndUrl
        findPreference<Preference>(getString(R.string.pref_key_nd_url))?.onPreferenceClickListener =
            OnPreferenceClickListener {
                showEditDialog(context, getString(R.string.pref_title_nd_url), ND_URL, ndUrl, it)
                true
            }

        val ndClientId = sharedViewModel.getNDClientId()
        findPreference<Preference>(getString(R.string.pref_key_risk_client_id))?.summary = ndClientId
        findPreference<Preference>(getString(R.string.pref_key_risk_client_id))?.onPreferenceClickListener =
            OnPreferenceClickListener {
                showEditDialog(context, getString(R.string.pref_title_risk_client_id), ND_CLIENT_ID, ndClientId, it)
                true
            }

        val riskUrl = sharedViewModel.getRiskUrl()
        findPreference<Preference>(getString(R.string.pref_key_risk_url))?.summary = riskUrl
        findPreference<Preference>(getString(R.string.pref_key_risk_url))?.onPreferenceClickListener =
            OnPreferenceClickListener {
                showEditDialog(context, getString(R.string.pref_title_risk_url), RISK_URL, riskUrl, it)
                true
            }

        findPreference<Preference>(getString(R.string.pref_key_device_push_token))?.onPreferenceClickListener =
            OnPreferenceClickListener {
                FirebaseMessaging.getInstance().token.addOnCompleteListener(
                    OnCompleteListener { task ->
                        if (!task.isSuccessful) {
                            return@OnCompleteListener
                        }

                        // Get new FCM registration token
                        val token = task.result
                        settingViewModel.updatePushToken(token)
                    }
                )
                true
            }

        findPreference<Preference>(getString(R.string.pref_key_reset))?.onPreferenceClickListener =
            OnPreferenceClickListener {
                settingViewModel.unenroll()
                true
            }
    }

    private fun showEditDialog(context: Context?, title: String, key: String, currentValue: String, preference: Preference) {
        val layout = context?.let { FrameLayout(it) }
        val editText = EditText(context)
        editText.setText(currentValue)
        layout?.addView(editText)

        android.app.AlertDialog.Builder(context)
            .setTitle(title)
            .setView(layout)
            .setPositiveButton(
                android.R.string.ok,
                DialogInterface.OnClickListener { _, _ ->
                    val preferenceValue: String = editText.text.toString()
                    if (preferenceValue.isEmpty()) {
                        return@OnClickListener
                    }
                    // update to storage
                    sharedViewModel.updatePreference(key, preferenceValue)
                    preference.summary = preferenceValue
                }
            )
            .setNegativeButton(
                android.R.string.cancel,
                DialogInterface.OnClickListener { dialog, _ ->
                    dialog.dismiss()
                    return@OnClickListener
                }
            )
            .setCancelable(false)
            .show()
    }
}
