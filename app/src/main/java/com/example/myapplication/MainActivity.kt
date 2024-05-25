package com.example.myapplication

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*
import kotlin.collections.ArrayList

data class Song(val title: String, val artist: String, val resourceId: Int, val imageResId: Int, val lyricResId: Int)

data class Lyric(val startTime: Long, val content: String)

class MainActivity : AppCompatActivity() {

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var seekBar: SeekBar
    private lateinit var playPauseButton: Button
    private lateinit var prevButton: Button
    private lateinit var nextButton: Button
    private lateinit var songTitleTextView: TextView
    private lateinit var artistTextView: TextView
    private lateinit var musicImageView: ImageView
    private lateinit var lyricTextView: TextView
    private lateinit var lyrics: List<Lyric>
    private var currentLyricIndex = 0

    private val songs = mutableListOf(
        Song("瞬间的永恒", "赵海洋", R.raw.my_music1, R.drawable.music_image1, R.raw.my_musiclyric1),
        Song("玫瑰少年", "旺仔小乔", R.raw.my_music3, R.drawable.music_image3, R.raw.my_musiclyric3),
        Song("嘿，那个你", "房东的猫", R.raw.my_music2, R.drawable.music_image2, R.raw.my_musiclyric2)
    )

    private var currentSongIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        songTitleTextView = findViewById(R.id.songTitleTextView)
        artistTextView = findViewById(R.id.artistTextView)
        playPauseButton = findViewById(R.id.playPauseButton)
        prevButton = findViewById(R.id.prevButton)
        nextButton = findViewById(R.id.nextButton)
        seekBar = findViewById(R.id.seekBar)
        musicImageView = findViewById(R.id.musicImageView)
        lyricTextView = findViewById(R.id.lyricTextView)

        playPauseButton.setOnClickListener { togglePlayPause() }
        prevButton.setOnClickListener { playPreviousSong() }
        nextButton.setOnClickListener { playNextSong() }

        findViewById<Button>(R.id.musicSelectionButton).setOnClickListener {
            startActivityForResult(Intent(this, MusicSelectionActivity::class.java), REQUEST_MUSIC_SELECTION)
        }

        initializeMediaPlayer()

        // Add a button to open a browser for song download
        val downloadButton: Button = findViewById(R.id.downloadButton)
        downloadButton.setOnClickListener {
            val url = "https://www.baidu.com"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        }
    }

    private fun initializeMediaPlayer() {
        mediaPlayer = MediaPlayer.create(this, songs[currentSongIndex].resourceId)
        mediaPlayer.setOnCompletionListener { playNextSong() }
        updateSongInfo() // Add this line to update song information
        updateSeekBar()
        loadLyrics(songs[currentSongIndex].lyricResId)
        startLyricTimer()
    }

    private fun togglePlayPause() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            playPauseButton.text = "Play"
        } else {
            mediaPlayer.start()
            playPauseButton.text = "Pause"
        }
    }

    private fun playPreviousSong() {
        mediaPlayer.reset()
        currentSongIndex = if (currentSongIndex - 1 < 0) songs.size - 1 else currentSongIndex - 1
        mediaPlayer = MediaPlayer.create(this, songs[currentSongIndex].resourceId)
        mediaPlayer.setOnCompletionListener { playNextSong() }
        updateSongInfo() // Add this line to update song information
        updateSeekBar()
        loadLyrics(songs[currentSongIndex].lyricResId)
        mediaPlayer.start()
        playPauseButton.text = "Pause"
    }

    private fun playNextSong() {
        mediaPlayer.reset()
        currentSongIndex = (currentSongIndex + 1) % songs.size
        mediaPlayer = MediaPlayer.create(this, songs[currentSongIndex].resourceId)
        mediaPlayer.setOnCompletionListener { playNextSong() }
        updateSongInfo() // Add this line to update song information
        updateSeekBar()
        loadLyrics(songs[currentSongIndex].lyricResId)
        mediaPlayer.start()
        playPauseButton.text = "Pause"
    }


    private fun updateSongInfo() {
        val currentSong = songs[currentSongIndex]
        songTitleTextView.text = currentSong.title
        artistTextView.text = currentSong.artist
        musicImageView.setImageResource(currentSong.imageResId)
    }

    private fun updateSeekBar() {
        seekBar.max = mediaPlayer.duration
        seekBar.progress = 0

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        val handler = android.os.Handler()
        handler.post(object : Runnable {
            override fun run() {
                try {
                    seekBar.progress = mediaPlayer.currentPosition
                    handler.postDelayed(this, 1000)
                } catch (e: Exception) {
                    seekBar.progress = 0
                }
            }
        })
    }

    private fun loadLyrics(lyricResId: Int) {
        val inputStream = resources.openRawResource(lyricResId)
        val reader = BufferedReader(InputStreamReader(inputStream, "gbk"))
        val lyricList = mutableListOf<Lyric>()

        var line: String? = reader.readLine()
        while (line != null) {
            val regex = "\\[(\\d+):(\\d+).(\\d+)](.*)".toRegex()
            val matchResult = regex.find(line)
            if (matchResult != null) {
                val minutes = matchResult.groupValues[1].toLong()
                val seconds = matchResult.groupValues[2].toLong()
                val millis = matchResult.groupValues[3].toLong()
                val content = matchResult.groupValues[4]
                val startTime = minutes * 60 * 1000 + seconds * 1000 + millis
                val lyric = Lyric(startTime, content)
                lyricList.add(lyric)
            }
            line = reader.readLine()
        }
        reader.close()

        lyricTextView.text = "" // Clear lyric view

        if (lyricList.isEmpty()) {
            lyricTextView.text = "" // Clear lyric view
        }

        lyrics = lyricList
    }


    private fun startLyricTimer() {
        Timer().scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    if (mediaPlayer.isPlaying) {
                        val currentPosition = mediaPlayer.currentPosition.toLong()
                        for (i in lyrics.indices) {
                            if (i < lyrics.size - 1 && currentPosition >= lyrics[i].startTime && currentPosition < lyrics[i + 1].startTime) {
                                currentLyricIndex = i
                                updateLyric()
                                break
                            }
                        }
                    }
                }
            }
        }, 0, 100)
    }

    private fun updateLyric() {
        lyricTextView.text = lyrics[currentLyricIndex].content
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_MUSIC_SELECTION && resultCode == RESULT_OK) {
            val musicUri = data?.getStringExtra("musicUri")
            val musicTitle = data?.getStringExtra("musicTitle")
            if (musicUri != null) {
                mediaPlayer.reset()
                mediaPlayer = MediaPlayer.create(this, Uri.parse(musicUri))
                mediaPlayer.setOnCompletionListener { playNextSong() }
                updateSongInfo() // Add this line to update song information
                loadLyrics(songs[currentSongIndex].lyricResId)
                mediaPlayer.start()
                playPauseButton.text = "Pause"
            }
            if (musicTitle != null) {
                songTitleTextView.text = musicTitle
                if (musicUri != null && musicUri.isNotEmpty()) {
                    // Clear lyric and artist view
                    lyricTextView.text = ""
                    artistTextView.text = ""
                }
            }
        }
    }

    companion object {
        private const val REQUEST_MUSIC_SELECTION = 101
    }
}
