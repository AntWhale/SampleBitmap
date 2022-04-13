package com.boo.sample.samplebitmap

import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.boo.sample.samplebitmap.databinding.ActivityMainBinding
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        initViewModel()
        setUpLifeCycleOwner()

        setContentView(binding.root)
    }

    private fun initViewModel(){
        binding.viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
    }

    private fun setUpLifeCycleOwner(){
        binding.lifecycleOwner = this
    }

    fun imgSaveOnClick(view: View){
        val bitmap = drawBitmap()

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            saveImageOnAboveAndroidQ(bitmap)
            Toast.makeText(baseContext, "이미지 저장이 완료되었습니다.", Toast.LENGTH_SHORT).show()
        } else {
            val writePermission = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

            if(writePermission == PackageManager.PERMISSION_GRANTED){
                saveImageOnUnderAndroidQ(bitmap)
                Toast.makeText(baseContext, "이미지 저장이 완료되었습니다.", Toast.LENGTH_SHORT).show()
            } else {
                val requestExternalStorageCode = 1

                val permissionStorage = arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                )

                ActivityCompat.requestPermissions(this, permissionStorage, requestExternalStorageCode)
            }
        }
    }

    private fun drawBitmap():Bitmap {
        //기기 해상도를 가져옴
        val backgroundWidth = resources.displayMetrics.widthPixels
        val backgroundHeight = resources.displayMetrics.heightPixels

        val totalBitmap = Bitmap.createBitmap(backgroundWidth, backgroundHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(totalBitmap)    //캔버스에 비트맵을 Mapping

        val bgColor = binding.viewModel?.background?.value
        if(bgColor != null){
            val color = ContextCompat.getColor(baseContext, bgColor)
            canvas.drawColor(color)
        }

        val imageView = binding.iv
        val imageViewBitmap = Bitmap.createBitmap(imageView.width, imageView.height, Bitmap.Config.ARGB_8888)
        val imageViewCanvas = Canvas(imageViewBitmap)
        imageView.draw(imageViewCanvas)     //imageViewCanvas를 통해서 imageView를 그린다

        //이미지가 그려질 곳 계산
        val imageViewLeft = ((backgroundWidth - imageView.width) / 2).toFloat()
        val imageViewTop =  ((backgroundHeight - imageView.height) / 2).toFloat()

        canvas.drawBitmap(imageViewBitmap, imageViewLeft, imageViewTop, null)

        val textView = binding.tv
        if(textView.length() > 0){
            val textViewBitmap = Bitmap.createBitmap(textView.width, textView.height, Bitmap.Config.ARGB_8888)
            val textViewCanvas = Canvas(textViewBitmap)
            textView.draw(textViewCanvas)

            val marginTop = (20 * resources.displayMetrics.density).toInt()
            val textViewLeft = ((backgroundWidth - textView.width) / 2).toFloat()
            val textViewTop = imageViewTop + imageView.height + marginTop

            canvas.drawBitmap(textViewBitmap, textViewLeft, textViewTop, null)
        }
        return totalBitmap
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveImageOnAboveAndroidQ(bitmap: Bitmap){
        val fileName = System.currentTimeMillis().toString() + ".png"

        //ContentValues는 ContentResolver가 처리할 수 있는 값을 저장해둘 목적으로 사용한다
        val contentValues = ContentValues()
        contentValues.apply{
            put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/ImageSave")        //경로설정
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)                 //파일이름을 put해준다.
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.IS_PENDING, 1)                          //다른 곳에서 이 데이터를 요구하면 무시하라는 의미로, 해당 저장소를 독점
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        try {
            if(uri != null){
                val image = contentResolver.openFileDescriptor(uri, "w", null)      //write 모드로 file을 open한다

                if(image != null){
                    val fos = FileOutputStream(image.fileDescriptor)
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                    fos.close()

                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    contentResolver.update(uri, contentValues, null, null)
                }
            }
        }catch (e: FileNotFoundException){
            e.printStackTrace()
        }catch (e: IOException){
            e.printStackTrace()
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun saveImageOnUnderAndroidQ(bitmap: Bitmap){
        val fileName = System.currentTimeMillis().toString() + ".png"
        val externalStorage = Environment.getExternalStorageDirectory().absolutePath
        val path = "$externalStorage/DCIM/imageSave"
        val dir = File(path)

        if(dir.exists().not()) dir.mkdirs()

        try{
            val fileItem = File("$dir/$fileName")
            fileItem.createNewFile()

            val fos = FileOutputStream(fileItem)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)

            fos.close()

            sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(fileItem)))
        } catch (e: FileNotFoundException){
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
}