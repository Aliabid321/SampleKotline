package com.example.milliondollaridea

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.DatabaseUtils
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.format.DateFormat
import android.util.Log
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.Float.max
import java.lang.Float.min
import java.text.SimpleDateFormat
import java.util.*


class ItemDetaildActivity : AppCompatActivity(), View.OnClickListener {
    var img: Int = 0
    lateinit var image: LinearLayout
    lateinit var selected: ImageView
    lateinit var btnopengry: Button;
    lateinit var btnsave: Button;
    lateinit var btnshare: Button;
    private val pickImage = 100
    private var imageUri: Uri? = null
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private var scaleFactor = 1.0f
    private lateinit var main: View
    private lateinit var b: Bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_item_detaild)
        init()
        image = findViewById(R.id.image_bg)
        main = findViewById(R.id.image_bg)

        selected = findViewById(R.id.myZoomageView)
        var bundle: Bundle? = intent.extras
        val int: Intent
        var message = bundle?.get("image").toString()// 1
        image.setBackgroundResource(Integer.parseInt(message))
        scaleGestureDetector = ScaleGestureDetector(this, ScaleListener())
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.btnopengalary -> {
                opengallery()
            }
            R.id.btndsave -> {
                screenshot()

            }
            R.id.btnshare -> {
                screenshot()
            }
        }
    }

    private fun init() {
        btnopengry = findViewById(R.id.btnopengalary)
        btnopengry.setOnClickListener(this)
        btnsave = findViewById(R.id.btndsave)
        btnsave.setOnClickListener(this)
        btnshare = findViewById(R.id.btnshare)
    }

    private fun opengallery() {
        val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
        startActivityForResult(gallery, pickImage)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == pickImage) {
            imageUri = data?.data
            selected.setImageURI(imageUri)
        }
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(scaleGestureDetector: ScaleGestureDetector): Boolean {
            scaleFactor *= scaleGestureDetector.scaleFactor
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                scaleFactor = max(0.1f, min(scaleFactor, 10.0f))
            }
            selected.scaleX = scaleFactor
            selected.scaleY = scaleFactor
            return true
        }
    }

    fun screenshot() {
        b = Screenshot.takeScreenshotOfRootView(image)
        selected.setImageBitmap(b)
        main.setBackgroundColor(Color.parseColor("#999999"))
        save()
    }

    companion object Screenshot {
        private fun takeScreenshot(view: View): Bitmap {
            view.isDrawingCacheEnabled = true
            view.buildDrawingCache(true)
            val b = Bitmap.createBitmap(view.drawingCache)
            view.isDrawingCacheEnabled = false
            return b
        }

        fun takeScreenshotOfRootView(v: View): Bitmap {
            return takeScreenshot(v.rootView)
        }

    }
    fun save() {
        lifecycleScope.launch {
            val bitmap = b
            saveImageToGallery(
                this@ItemDetaildActivity,
                bitmap,
                "img_${System.currentTimeMillis()}.jpg"
            )?.let {
                printMediaStoreEntry(it)
            }
        }
    }


//    suspend fun loadBitmap( resId: Int): Bitmap = withContext(Dispatchers.IO) {
//        BitmapFactory.decodeResource(resources, resId)
//    }


    private suspend fun saveImageToGallery(
        context: Context,
        bitmap: Bitmap,
        imageName: String
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val exifDateFormatter = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, imageName)
                put(MediaStore.Images.Media.DESCRIPTION, imageName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            // Insert file into MediaStore
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            val galleryFileUri = contentResolver.insert(collection, values)
                ?: return@withContext null

            // Save file to uri from MediaStore
            contentResolver.openOutputStream(galleryFileUri).use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, it)
            }

            // Add exif data
            contentResolver.openFileDescriptor(galleryFileUri, "rw")?.use {
                // set Exif attribute so MediaStore.Images.Media.DATE_TAKEN will be set
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    ExifInterface(it.fileDescriptor)
                        .apply {
                            setAttribute(
                                ExifInterface.TAG_DATETIME_ORIGINAL,
                                exifDateFormatter.format(Date())
                            )
                            saveAttributes()
                        }
                } else {
                    TODO("VERSION.SDK_INT < N")
                }
            }

            // Now that we're finished, release the "pending" status, and allow other apps to view the image.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(galleryFileUri, values, null, null)
            }
            return@withContext galleryFileUri
        } catch (ex: Exception) {
            Log.e("MSTEST", "Saving progress pic to gallery failed", ex)
            return@withContext null
        }
    }


    suspend fun printMediaStoreEntry(imageUri: Uri) = withContext(Dispatchers.IO) {

        contentResolver.query(
            imageUri,
            null,
            null,
            null,
            null
        ).use { cursor ->
            val imgInfo = DatabaseUtils.dumpCursorToString(cursor)
            Log.d("MSTEST", imgInfo)
        }
    }
}

