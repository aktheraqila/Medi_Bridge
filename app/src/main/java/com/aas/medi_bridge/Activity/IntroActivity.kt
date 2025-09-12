package com.aas.medi_bridge.Activity

import android.content.Intent
import android.os.Bundle

import com.aas.medi_bridge.databinding.ActivityIntroBinding

class IntroActivity : BaseActivity() {
    private lateinit var binding: ActivityIntroBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.apply {
            // Patient button - navigate to MainActivity (patient flow)
            patientbutton.setOnClickListener {
                val intent = Intent(this@IntroActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }


        }

    }
}