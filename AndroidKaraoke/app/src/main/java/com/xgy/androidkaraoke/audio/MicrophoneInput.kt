package com.xgy.androidkaraoke.audio

/**
 * 麦克风输入抽象。本期 stub。
 * Sony KD-65A9F / Android 10 蓝牙麦因机型与固件差异需实机测试，不保证开箱可用。
 */
interface MicrophoneInput {
    val isAvailable: Boolean
    val isActive: Boolean
    fun start()
    fun stop()
    fun level(): Float
}

class StubMicrophoneInput : MicrophoneInput {
    override val isAvailable: Boolean = false
    override var isActive: Boolean = false
        private set

    override fun start() {
        // Stub — wire AudioRecord / BT SCO later after A9F field test.
        isActive = false
    }

    override fun stop() {
        isActive = false
    }

    override fun level(): Float = 0f
}
