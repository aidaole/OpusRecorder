package com.aidaole.opusrecorder

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import com.aidaole.ext.logi
import com.theeasiestway.opus.Constants
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
    private val opus: OpusHelper
) {

    private var audioRecorder: AudioRecord = AudioRecord(
        source, sampleRate, channel, audioFormat, bufferSize
    )
    private var sourceDataFos: FileOutputStream? = null
    private var opusDataFos: FileOutputStream? = null
    private var opusDecDataFos: FileOutputStream? = null

    private fun releaseStreams() {
        sourceDataFos?.close()
        sourceDataFos = null
        opusDataFos?.close()
        opusDataFos = null
        opusDecDataFos?.close()
        opusDecDataFos = null
    }

    companion object {
        private const val TAG = "OpusRecorder"

        fun build(source: Int, simpleRate: Int, channel: Int, audioFormat: Int, ms: Int): OpusRecorder {
            // 采样率(16000) * 通道数(2) * 帧大小(2byte) = 每秒的bufferSize
            // 每秒的bufferSize / 1000 * MS = 想读取ms数的bufferSize， MS只能是(2.5, 5, 10, 20, 40, 60)
            val channelCount = if (channel == AudioFormat.CHANNEL_IN_STEREO) 2 else 1
            val audioFrameSize = when (audioFormat) {
                AudioFormat.ENCODING_PCM_16BIT -> 2
                AudioFormat.ENCODING_PCM_8BIT -> 1
                AudioFormat.ENCODING_PCM_32BIT -> 4
                else -> throw java.lang.IllegalArgumentException("audioFormat参数错误")
            }
            val bufferSize = simpleRate / 1000.0 * channelCount * audioFrameSize * ms
            // opus 支持的 framesize取值（2880，2560，1920，1280，960，640，480,320，240,160,120）
            val opusFrameSize = bufferSize.toInt().div(audioFormat * channelCount)
            val opusHelper = OpusHelper(
                simpleRate.toOpusSample(),
                channel.toOpusChannel(),
                Constants.Application.audio(),
                opusFrameSize.toOpusFrameSize(),
            )
            "build-> channel:$channelCount, audioFrameSize:${audioFrameSize}, recorder bufferSize: $bufferSize, opusFrameSize:${opusFrameSize}".logi(
                TAG
            )
            return OpusRecorder(source, simpleRate, channel, audioFormat, bufferSize.toInt(), opusHelper)
        }
    }

    private val readBuffer = ByteArray(bufferSize)

    @Volatile
    private var state = State.STOP

    enum class State {
        START, STOP
    }

    fun startRecord(
        listener: (data: ByteArray) -> Unit,
        sourceFileName: (() -> String)? = null,
        opusSourceFileName: (() -> String)? = null,
        opusDecSourceFileName: (() -> String)? = null,
    ) {
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
                // 保存录音原文件
                if (sourceFileName != null) {
                    sourceDataFos?.let {
                        fosWriteToFile(it, readData)
                    } ?: run {
                        sourceDataFos = makeSureFosValid(sourceFileName.invoke())
                        fosWriteToFile(sourceDataFos!!, readData)
                    }
                }
                // 保存pcm文件
                if (opusSourceFileName != null) {
                    val opusData = opus.encode(readData.copyOf())
                    "startRecord-> opusSize: ${opusData?.size}".logi(TAG)
                    opusDataFos?.let {
                        fosWriteToFile(it, opusData)
                    } ?: run {
                        opusDataFos = makeSureFosValid(opusSourceFileName.invoke())
                        fosWriteToFile(opusDataFos!!, opusData)
                    }
                    // 保存pcm decode文件
                    if (opusDecSourceFileName != null && opusData != null) {
                        val decodePcm = opus.decode(opusData.copyOf())
                        "startRecord-> decodeSize: ${decodePcm?.size}".logi(TAG)
                        opusDecDataFos?.let {
                            fosWriteToFile(it, decodePcm)
                        } ?: run {
                            opusDecDataFos = makeSureFosValid(opusDecSourceFileName.invoke())
                            fosWriteToFile(opusDataFos!!, decodePcm)
                        }
                    }
                }
                listener.invoke(readData)
            }
        }
    }

    private fun fosWriteToFile(fos: FileOutputStream, readData: ByteArray?) {
        fos.run {
            readData?.let {
                fos.write(readData)
            }
            fos.flush()
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
            if (file.parentFile?.exists() != true) {
                val succ = file.parentFile?.mkdirs()
                if (!succ!!) {
                    return null
                }
            }
            FileOutputStream(file)
        }
    }

    override fun toString(): String {
        return "OpusRecorder(source=$source, sampleRate=$sampleRate, channel=$channel, audioFormat=$audioFormat, bufferSize=$bufferSize, opus=$opus)"
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

private fun Int.toOpusFrameSize(): Constants.FrameSize {
    return Constants.FrameSize.fromValue(this)
}

private fun Int.toOpusChannel(): Constants.Channels {
    return when (this) {
        AudioFormat.CHANNEL_IN_STEREO -> {
            Constants.Channels.stereo()
        }
        AudioFormat.CHANNEL_IN_MONO -> {
            Constants.Channels.mono()
        }
        else -> {
            throw java.lang.IllegalArgumentException()
        }
    }
}

fun Int.toOpusSample(): Constants.SampleRate {
    return when (this) {
        OpusRecorder.SampleRate.RATE_44100 -> {
            Constants.SampleRate._48000()
        }
        OpusRecorder.SampleRate.RATE_22050 -> {
            Constants.SampleRate._24000()
        }
        OpusRecorder.SampleRate.RATE_16000 -> {
            Constants.SampleRate._16000()
        }
        OpusRecorder.SampleRate.RATE_11025 -> {
            Constants.SampleRate._12000()
        }
        else -> {
            throw java.lang.IllegalArgumentException("参数错误")
        }
    }
}
