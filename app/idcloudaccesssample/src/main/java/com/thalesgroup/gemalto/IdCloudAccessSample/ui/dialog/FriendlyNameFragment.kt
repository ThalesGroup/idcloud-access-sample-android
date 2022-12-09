package com.thalesgroup.gemalto.IdCloudAccessSample.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.thales.dis.mobile.idcloud.auth.ui.AuthenticatorDescriptionCallback
import com.thales.dis.mobile.idcloud.auth.ui.AuthenticatorDescriptor
import com.thalesgroup.gemalto.IdCloudAccessSample.R

class FriendlyNameFragment : DialogFragment() {

    private var authenticatorDescriptionCallback: AuthenticatorDescriptionCallback? = null

    companion object {
        fun newInstance(authenticatorDescriptionCallback: AuthenticatorDescriptionCallback): FriendlyNameFragment? {
            val fragment = FriendlyNameFragment()

            fragment.authenticatorDescriptionCallback = authenticatorDescriptionCallback

            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val editText = EditText(requireContext())
        val builder = AlertDialog.Builder(requireContext())
            .setMessage(getString(R.string.input_friendly_name))
            .setView(editText)
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                authenticatorDescriptionCallback?.onAuthenticatorDescriptionProvided(
                    AuthenticatorDescriptor(editText.text.toString())
                )
                dismiss()
            }

        return builder.create()
    }
}
