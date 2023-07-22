package com.aidaole.opusrecorder

import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.MediaRecorder.AudioSource
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.annotation.RequiresApi
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

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout.root)
        checkAudioPermission()
        recorder = OpusRecorder.build(
            AudioSource.VOICE_RECOGNITION,
            OpusRecorder.SampleRate.RATE_16000,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            OpusRecorder.FRAME_MS.MS_60
        )
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
            "initViews-> $recorder".logi(TAG)
            recorder?.startRecord({
            },
                {
                    "${this@MainActivity.getExternalFilesDir(Environment.DIRECTORY_MUSIC)}/audio/source.pcm".also {
                        "sourcePath-> $it".logi(TAG)
                    }
                },
                {
                    "${this@MainActivity.getExternalFilesDir(Environment.DIRECTORY_MUSIC)}/audio/opus_source.pcm".also {
                        "opusPath-> $it".logi(TAG)
                    }
                }, {
                    "${this@MainActivity.getExternalFilesDir(Environment.DIRECTORY_MUSIC)}/audio/opus_dec_source.pcm".also {
                        "opus_dec_source-> $it".logi(TAG)
                    }
                }
            )
        }
        layout.btnStop.setOnClickListener {
            recorder?.stopRecord()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        recorder?.destroy()
    }
}