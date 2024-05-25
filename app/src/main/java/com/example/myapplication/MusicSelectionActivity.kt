package com.example.myapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MusicSelectionActivity : AppCompatActivity() {

    private lateinit var musicListView: ListView
    private lateinit var musicList: MutableList<String>
    private lateinit var musicUriList: MutableList<Uri>

    companion object {
        private const val READ_EXTERNAL_STORAGE_PERMISSION_CODE = 101
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_selection)

        musicListView = findViewById(R.id.musicListView)
        val selectMusicButton: Button = findViewById(R.id.selectMusicButton)

        musicList = mutableListOf()
        musicUriList = mutableListOf()

        selectMusicButton.setOnClickListener {
            checkStoragePermissionAndLoadMusicList()
        }
    }

    private fun checkStoragePermissionAndLoadMusicList() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                READ_EXTERNAL_STORAGE_PERMISSION_CODE
            )
        } else {
            loadMusicList()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun loadMusicList() {
        val selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0"
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION
        )
        val cursor: Cursor? = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            null
        )

        if (cursor != null) {
            musicList.clear()
            musicUriList.clear()
            while (cursor.moveToNext()) {
                val name =
                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME))
                val artist =
                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST))
                val uri = Uri.withAppendedPath(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media._ID))
                )
                musicList.add("$name - $artist")
                musicUriList.add(uri)
            }
            cursor.close()

            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, musicList)
            musicListView.adapter = adapter

            musicListView.onItemClickListener =
                AdapterView.OnItemClickListener { _, _, position, _ ->
                    val selectedMusicUri = musicUriList[position]
                    val intent = Intent().apply {
                        putExtra("musicUri", selectedMusicUri.toString())
                        putExtra("musicTitle", musicList[position])
                    }
                    setResult(RESULT_OK, intent)
                    finish()
                }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == READ_EXTERNAL_STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadMusicList()
            } else {
                Toast.makeText(
                    this,
                    "Permission denied to read your External storage",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
