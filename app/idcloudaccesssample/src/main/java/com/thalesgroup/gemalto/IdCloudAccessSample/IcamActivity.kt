package com.thalesgroup.gemalto.IdCloudAccessSample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.thalesgroup.gemalto.IdCloudAccessSample.databinding.ActivityIcamBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class IcamActivity : AppCompatActivity() {
    private lateinit var binding: ActivityIcamBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIcamBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
    }
}
