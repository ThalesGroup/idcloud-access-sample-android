package com.thalesgroup.gemalto.IdCloudAccessSample.utilities

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.thalesgroup.gemalto.IdCloudAccessSample.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun <T> LiveData<T>.observeOnce(observer: (T) -> Unit) {
    observeForever(object : Observer<T> {
        override fun onChanged(value: T) {
            removeObserver(this)
            observer(value)
        }
    })
}

fun <T> LiveData<T>.observeOnce(owner: LifecycleOwner, observer: (T) -> Unit) {
    observe(
        owner,
        object : Observer<T> {
            override fun onChanged(value: T) {
                removeObserver(this)
                observer(value)
            }
        }
    )
}

inline fun Context.toast(message: () -> String) {
    Toast.makeText(this, message(), Toast.LENGTH_LONG).show()
}

inline fun Fragment.toast(message: () -> String) {
    Toast.makeText(this.context, message(), Toast.LENGTH_LONG).show()
}

inline fun View.snackbar(message: () -> String) {
    Snackbar.make(this, message(), Snackbar.LENGTH_SHORT).show()
}

fun Fragment.hideKeyboard() {
    val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(requireView().windowToken, 0)
}

fun Fragment.findNavControllerSafely(): NavController? {
    return if (isAdded) {
        findNavController()
    } else {
        null
    }
}

fun String.withDateAndTime(): String {
    val date = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault()).format(Date())
    return "$date : $this"
}

var alertDialog: AlertDialog? = null
fun FragmentActivity.getProgressDialog(): AlertDialog? {

    if (alertDialog == null) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        val customLayout: View = this.layoutInflater.inflate(R.layout.progress_dialog, null)
        builder.setView(customLayout)
        builder.setCancelable(false)
        alertDialog = builder.create()
        alertDialog?.setCanceledOnTouchOutside(false)
    }
    return alertDialog
}
