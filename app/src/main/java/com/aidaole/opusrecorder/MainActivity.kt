package com.aidaole.opusrecorder

import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.MediaRecorder.AudioSource
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.aidaole.ext.logi
import com.aidaole.opusrecorder.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    private val layout by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private var recorder: OpusRecorder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout.root)
        checkAudioPermission()
        initViews()
    }

    private fun checkAudioPermission() {
        val havePermission = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECORD_AUDIO
        )
        if (havePermission != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), 1)
        }
    }

    private fun initViews() {
        layout.btnRecord.setOnClickListener {
            recorder = OpusRecorder.build(
                AudioSource.VOICE_RECOGNITION,
                OpusRecorder.SampleRate.RATE_16000,
                AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                OpusRecorder.READ_MS.MS_20
            )
            "initViews-> $recorder".logi(TAG)
            recorder?.startRecord {
                "initViews-> ${it.size}".logi(TAG)
            }
        }
        layout.btnStop.setOnClickListener {
            recorder?.stopRecord()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        recorder?.release()
    }
}