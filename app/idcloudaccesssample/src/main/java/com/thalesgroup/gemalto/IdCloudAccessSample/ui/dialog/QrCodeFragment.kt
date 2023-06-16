package com.thalesgroup.gemalto.IdCloudAccessSample.ui.dialog

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import com.google.zxing.Result
import me.dm7.barcodescanner.zxing.ZXingScannerView

class QrCodeFragment : DialogFragment(), ZXingScannerView.ResultHandler {
    private val CAMERA_PERMISSION_REQUEST_CODE = 0
    private var scannerView: ZXingScannerView? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        scannerView = ZXingScannerView(activity)
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return scannerView
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    override fun onResume() {
        super.onResume()
        scannerView?.setResultHandler(this)
        if (ContextCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(
                    Manifest.permission.CAMERA
                ),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        } else {
            scannerView?.startCamera()
        }
    }

    override fun onStop() {
        super.onStop()
        scannerView?.stopCamera()
    }

    override fun handleResult(rawResult: Result) {
        findNavController().previousBackStackEntry?.savedStateHandle?.set("registrationCode", rawResult.text)
        findNavController().popBackStack()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                scannerView?.startCamera()
                return
            }
        }
    }
}
