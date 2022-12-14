package com.thalesgroup.gemalto.IdCloudAccessSample.ui.dialog

import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.thalesgroup.gemalto.IdCloudAccessSample.R

object IDCAAlertDialog {
    var dialog: AlertDialog? = null
    fun getAlertDialog(activity: FragmentActivity, message: String, title: String) {
        if (dialog?.isShowing != true) {
            dialog = MaterialAlertDialogBuilder(activity).setTitle(title).setMessage(message)
                .setPositiveButton(R.string.ok) { dialog, which ->
                    /* dialog.cancel()*/
                    dialog.dismiss()
                }.show()
        }
    }
}
