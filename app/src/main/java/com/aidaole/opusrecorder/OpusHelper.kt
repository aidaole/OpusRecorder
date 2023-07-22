package com.aidaole.opusrecorder

import com.theeasiestway.opus.Constants
import com.theeasiestway.opus.Opus

class OpusHelper(
    val sampleRate: Constants.SampleRate,
    val channel: Constants.Channels,
    val mode: Constants.Application,
    val frameSize: Constants.FrameSize
) {

    private var opus: Opus = Opus()

    init {
        opus.encoderInit(sampleRate, channel, mode)
        opus.decoderInit(sampleRate, channel)
    }

    fun encode(data: ByteArray): ByteArray? {
        return opus.encode(data, frameSize)
    }

    fun decode(data: ByteArray): ByteArray? {
        return opus.decode(data, frameSize)
    }

    override fun toString(): String {
        return "OpusHelper(sampleRate=$sampleRate, channel=$channel, mode=$mode, frameSize=$frameSize)"
    }
}