package net.lyrex.audio

import mu.KotlinLogging
import java.io.ByteArrayInputStream
import java.util.concurrent.TimeUnit

import javax.sound.sampled.AudioSystem
import javax.sound.sampled.LineEvent

private val logger = KotlinLogging.logger {}

class AudioPlayer {
    private class AudioPlayerRunner(private val audioBytes: ByteArray) : Runnable {
        var isPlaying = false

        override fun run() {
            logger.info { "AudioPlaybackRunner executed" }

            ByteArrayInputStream(audioBytes).use { stream ->
                AudioSystem.getAudioInputStream(stream).use { ais ->
                    // if we were interrupted previously, go to the interrupted frame and start playback from there
                    if (_interruptedAtFrame != 0L) {
                        logger.info { "Resuming audio playback at frame $_interruptedAtFrame" }
                        val bytesToSkip = _interruptedAtFrame * ais.frameLength
                        ais.skip(bytesToSkip)
                    }

                    logger.info { "Initializing audio playback" }
                    AudioSystem.getClip().use { clip ->
                        // add listener to know when we stopped playback
                        clip.addLineListener { e ->
                            if (e.type == LineEvent.Type.START) {
                                isPlaying = true
                            }

                            if (e.type == LineEvent.Type.STOP) {
                                synchronized(_lock) {
                                    logger.debug { "audio playback ended" }

                                    isPlaying = false
                                }
                            }

                            synchronized(_lock) {
                                _lock.notifyAll()
                            }
                        }

                        try {
                            logger.debug { "starting audio playback" }
                            clip.open(ais)
                            clip.start()

                            // wait until the clip is done
                            do {
                                synchronized(_lock) {
                                    _lock.wait();
                                }
                            } while (clip.isRunning);

                            clip.drain();
                            clip.stop();
                        } catch (e: InterruptedException) {
                            _interruptedAtFrame = clip.longFramePosition
                        }
                    }
                }
            }
        }

        private val _lock = Object()
        private var _interruptedAtFrame: Long = 0L
    }

    private val lock = Object()
    private var _audioThread: Thread? = null
    private var paused = false

    fun play(audioBytes: ByteArray) {
        synchronized(lock) {
            if (_audioThread != null) {
                stop()
                paused = false
            }

            _audioThread = Thread(AudioPlayerRunner(audioBytes))
            _audioThread!!.start()
        }
    }

    fun resume() {
        if (paused && _audioThread != null) {
            _audioThread!!.run()
            paused = false
        }
    }

    fun pause() {
        synchronized(lock) {
            _audioThread?.interrupt()
            _audioThread?.join()
            paused = true
        }
    }

    fun stop() {
        synchronized(lock) {
            _audioThread?.interrupt()
            _audioThread?.join()
            _audioThread = null
            paused = false
        }
    }

    fun waitUntilPlayingIsOver(timeoutSeconds: Int = -1) {
        // todo(tobias): this should not busy wait. use a thread or some kind of async (kotlin coroutines, hello?)
        var waitedMilliseconds = 0

        while (isPlaying) {
            TimeUnit.MILLISECONDS.sleep(100)
            waitedMilliseconds += 100

            if (timeoutSeconds > 0 && (waitedMilliseconds / 1000) > timeoutSeconds) {
                return
            }
        }
    }

    val isPlaying: Boolean
        get() {
            var threadNotNull: Boolean
            var threadIsAlive: Boolean
            var threadNotInterrupted: Boolean

            synchronized(lock) {
                threadNotNull = _audioThread != null
                threadIsAlive = threadNotNull && _audioThread!!.isAlive
                threadNotInterrupted = threadNotNull && !_audioThread!!.isInterrupted
            }

            logger.trace { "isThreadNotNull: $threadNotNull" }
            logger.trace { "threadIsAlive: $threadIsAlive" }
            logger.trace { "threadNotInterrupted: $threadNotInterrupted" }

            return threadNotNull && threadIsAlive && threadNotInterrupted
        }
}
