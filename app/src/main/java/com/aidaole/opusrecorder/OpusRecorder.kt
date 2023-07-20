package com.aidaole.opusrecorder

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import com.aidaole.ext.logi
import java.io.File
import java.io.FileOutputStream
import kotlin.concurrent.thread

@SuppressLint("MissingPermission")
class OpusRecorder private constructor(
    val source: Int,
    val sampleRate: Int,
    val channel: Int,
    val audioFormat: Int,
    private val bufferSize: Int,
) {

    private var audioRecorder: AudioRecord = AudioRecord(
        source,
        sampleRate,
        channel,
        audioFormat,
        bufferSize
    )
    private var sourceDataFos: FileOutputStream? = null

    companion object {
        private const val TAG = "OpusRecorder"

        fun build(source: Int, simpleRate: Int, channel: Int, audioFormat: Int, ms: Int): OpusRecorder {
            val bufferSize = simpleRate / 1000.0 *
                    (if (channel == AudioFormat.CHANNEL_IN_STEREO) 2 else 1) *
                    audioFormat *
                    ms
            return OpusRecorder(source, simpleRate, channel, audioFormat, bufferSize.toInt())
        }
    }

    private val readBuffer = ByteArray(bufferSize)

    @Volatile
    private var state = State.STOP

    enum class State {
        START, STOP
    }

    fun startRecord(listener: (data: ByteArray) -> Unit, saveSourceFileName: (() -> String)? = null) {
        if (state == State.START) {
            return
        }
        state = State.START
        audioRecorder.startRecording()
        thread {
            while (state == State.START) {
                val len = audioRecorder.read(readBuffer, 0, readBuffer.size)
                "startRecord-> bufferSize: ${readBuffer.size}, readLen: $len".logi(TAG)
                val readData = readBuffer.copyOfRange(0, len)
                if (saveSourceFileName != null) {
                    saveToFile(readData, saveSourceFileName.invoke())
                }
                listener.invoke(readData)
            }
        }
    }

    private fun saveToFile(readData: ByteArray, savePath: String) {
        "saveToFile-> $savePath, len: ${readData.size}".logi(TAG)
        sourceDataFos?.let {
            it.write(readData)
            it.flush()
        } ?: run {
            sourceDataFos = makeSureFosValid(savePath)
            sourceDataFos?.run {
                write(readData)
                flush()
            }
        }
    }

    fun stopRecord() {
        state = State.STOP
        audioRecorder.stop()
        releaseStreams()
    }

    fun destroy() {
        state = State.STOP
        audioRecorder.release()
        releaseStreams()
    }

    private fun makeSureFosValid(fileName: String): FileOutputStream? {
        val file = File(fileName)
        return if (file.exists()) {
            FileOutputStream(file)
        } else {
            val succ = file.parentFile?.mkdirs()
            if (succ != true) {
                null
            } else {
                FileOutputStream(file)
            }
        }
    }

    private fun releaseStreams() {
        sourceDataFos?.close()
        sourceDataFos = null
    }

    override fun toString(): String {
        return "OpusRecorder(source=$source, sampleRate=$sampleRate, channel=$channel, audioFormat=$audioFormat, bufferSize=$bufferSize, audioRecorder=$audioRecorder)"
    }

    object SampleRate {
        const val RATE_44100 = 44100
        const val RATE_22050 = 22050
        const val RATE_16000 = 16000
        const val RATE_11025 = 11025
    }

    object FRAME_MS {
        const val MS_2_5 = 2.5
        const val MS_5 = 5
        const val MS_10 = 10
        const val MS_20 = 20
        const val MS_40 = 40
        const val MS_60 = 60
    }
}

