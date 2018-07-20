package com.simone.crowdsense

import android.app.Activity
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_task.*
import android.provider.MediaStore
import android.graphics.Bitmap
import android.R.attr.data
import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.os.Environment
import java.io.FileOutputStream
import java.io.IOException
import android.os.Environment.DIRECTORY_PICTURES
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.view.View
import android.widget.ImageView
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Tasks
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.activity_perform.*
import okhttp3.*
import java.text.SimpleDateFormat
import java.util.*


val REQUEST_LIGHT_TEMP_CAPTURE = 1
val REQUEST_IMAGE_CAPTURE = 2
val REQUEST_NOISE_CAPTURE = 3
val REQUEST_RSSI_CAPTURE = 4
val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 5

var place: LatLng? = null

class TaskActivity : AppCompatActivity(), OnMapReadyCallback {

    private var mMap: GoogleMap? = null
    var photoFile : File? = null
    var mCurrentPhotoPath : String? = null
    var map : Map<String, String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task)


        map =  getIncomingIntent()

        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        if (map!!["type"] != null && okToPerform(map!!["lat"], map!!["lon"], map!!["radius"])){
            perform_btn.setOnClickListener {
                if(map!!["type"] == "RSSI"){
                    val intent = Intent(this, RssiActivity::class.java).apply {
                        putExtra("type_to_perform", map!!["type"])
                    }
                    startActivityForResult(intent, REQUEST_RSSI_CAPTURE)
                } else if (map!!["type"] == "picture"){
                    takePhoto()
                } else if (map!!["type"] == "noise"){
                    val intent = Intent(this, NoiseActivity::class.java).apply {
                        putExtra("type_to_perform", map!!["type"])
                    }
                    startActivityForResult(intent, REQUEST_NOISE_CAPTURE)
                }
                else{
                    val intent = Intent(this, PerformActivity::class.java).apply {
                        putExtra("type_to_perform", map!!["type"])
                    }
                    startActivityForResult(intent, REQUEST_LIGHT_TEMP_CAPTURE)
                }
            }
        } else {
            perform_btn.setOnClickListener {
                Toast.makeText(this, "Can't perform now", Toast.LENGTH_SHORT).show()
            }
        }

        delete_btn.setOnClickListener{
            val task = Task(map!!["ID"], map!!["issuer"], map!!["type"],  map!!["lat"],  map!!["lon"], map!!["radius"], map!!["duration"], map!!["what"])

            if (!id_delete_list.contains(task.ID)){
                Toast.makeText(this, "Delete", Toast.LENGTH_SHORT).show()
                id_delete_list.add(task.ID)
                if (id_accept_list.any{Task -> Task.ID == map!!["ID"]}){
                    for (item in id_accept_list){
                        if(item.ID == map!!["ID"]){
                            id_accept_list.remove(item)
                        }
                    }
                }
                postDeleteRequest(task)
                finish()

            } else {
                Toast.makeText(this, "Already delete", Toast.LENGTH_SHORT).show()
            }
        }

        accept_btn.setOnClickListener{
            val task = Task(map!!["ID"], map!!["issuer"], map!!["type"],  map!!["lat"],  map!!["lon"], map!!["radius"], map!!["duration"], map!!["what"])

            println(getSharedPreferences(getString(R.string.preference_file), Context.MODE_PRIVATE)
                    .getString(getString(R.string.user_pref), "username"))
            if (id_accept_list.any { Task -> Task.ID == map!!["ID"] } == false){
                Toast.makeText(this, "Accept", Toast.LENGTH_SHORT).show()
                id_accept_list.add(task)
                postAcceptRequest(task)
                finish()
            } else {
                Toast.makeText(this, "Already accept", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun getIncomingIntent() : Map<String, String>? {
        if (intent.hasExtra("what") && intent.hasExtra("place") && intent.hasExtra("duration")
                && intent.hasExtra("type") && intent.hasExtra("ID") && intent.hasExtra("radius")){
            val map = mapOf<String, String>("what" to intent.getStringExtra("what"),
                                    "issuer" to intent.getStringExtra("issuer"),
                                    "lat" to intent.getStringExtra("lat"),
                                    "lon" to intent.getStringExtra("lon"),
                                    "place" to intent.getStringExtra("place"),
                                    "radius" to intent.getStringExtra("radius"),
                                    "duration" to intent.getStringExtra("duration"),
                                    "type" to intent.getStringExtra("type"),
                                    "ID" to intent.getStringExtra("ID"))

            what_txt.text = map["what"]
            address_txt.text = map["place"]

            description_txt.text = "Perform the task and send us your rilevation.\n"+
                    "Thanks for your cooperation!\n" +
                    "\n" +
                    "CrowdSense Team."

            when(map["type"]){
                "picture" -> address_txt.setCompoundDrawablesWithIntrinsicBounds(null, null, getDrawable(R.drawable.ic_camera), null)
                "RSSI" -> address_txt.setCompoundDrawablesWithIntrinsicBounds(null, null, getDrawable(R.drawable.ic_rssi), null)
                "noise" -> address_txt.setCompoundDrawablesWithIntrinsicBounds(null, null, getDrawable(R.drawable.ic_noise), null)
                "light" -> address_txt.setCompoundDrawablesWithIntrinsicBounds(null, null, getDrawable(R.drawable.ic_light), null)
                "temperature" -> address_txt.setCompoundDrawablesWithIntrinsicBounds(null, null, getDrawable(R.drawable.ic_temperature), null)
            }

            return map
        }

        return null
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if (mLocationPermissionGranted && currentPlace != null){
            mMap!!.isMyLocationEnabled = true
        }

        if (intent.hasExtra("lat")  && intent.hasExtra("lon") && intent.hasExtra("radius")){
            val lat = intent.getStringExtra("lat").toDouble()
            val lon = intent.getStringExtra("lon").toDouble()
            val radius = intent.getStringExtra("radius").toDouble()

            place = LatLng(lat, lon)
            mMap!!.addMarker(MarkerOptions().position(place!!))
            mMap!!.addCircle(CircleOptions().center(place).radius(radius).strokeWidth(0f).fillColor(R.color.colorPrimary))
            mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(place, 12.5f))

        } else {
            // Add a marker in Sydney, Australia, and move the camera.
            val sydney = LatLng(-34.0, 151.0)
            mMap!!.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
            mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, 12.0f))
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_LIGHT_TEMP_CAPTURE) {
            if(resultCode == Activity.RESULT_OK){
                if(data!!.hasExtra("result")){
                    val result = data.getStringExtra("result")
                    Toast.makeText(this, result, Toast.LENGTH_LONG).show()
                    val t = Thread{
                        run {
                            if (intent.hasExtra("tweetID")) {
                                twitterOauthPost(result + " lx", intent.getStringExtra("tweetID"), intent.getStringExtra("issuer"))
                            }
                        }
                    }
                    t.start()
                }
            }
        }
        else if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (resultCode == Activity.RESULT_OK){
                //setPic()
                val t = Thread{
                    run {
                        if (intent.hasExtra("tweetID")){
                            twitterPostPhoto(photoFile!!, intent.getStringExtra("tweetID"), intent.getStringExtra("issuer"))
                        }
                    }
                }
                t.start()
            }
        }
        else if (requestCode == REQUEST_NOISE_CAPTURE){
            if (resultCode == Activity.RESULT_OK){
                if(data!!.hasExtra("result")){
                    val result = data.getStringExtra("result")
                    Toast.makeText(this, result, Toast.LENGTH_LONG).show()
                    val t = Thread{
                        run {
                            twitterOauthPost(result + " dB", intent.getStringExtra("tweetID"), intent.getStringExtra("issuer"))
                        }
                    }
                    t.start()
                }
            }
        } else if (requestCode == REQUEST_RSSI_CAPTURE){
            if (resultCode == Activity.RESULT_OK){
                if(data!!.hasExtra("result")){
                    val result = data.getStringExtra("result")
                    Toast.makeText(this, result, Toast.LENGTH_LONG).show()
                    val t = Thread{
                        run {
                            twitterOauthPost(result + " dBm", intent.getStringExtra("tweetID"), intent.getStringExtra("issuer"))
                        }
                    }
                    t.start()
                }
            }
        }
    }

    fun takePhoto(){
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            try {
                photoFile = createImageFile()
            } catch (ex : IOException){
                println("Error creating FILE!")
            }
            if (photoFile != null){
                val photoURI = FileProvider.getUriForFile(this, "com.simone.crowdsense.fileprovider", photoFile)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }
/*
    private fun galleryAddPic() {
        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        val f = File(mCurrentPhotoPath)
        val contentUri = Uri.fromFile(f)
        mediaScanIntent.data = contentUri
        this.sendBroadcast(mediaScanIntent)
    }

    private fun setPic() {
        // Get the dimensions of the View
        val targetW = photo_img.getWidth()
        val targetH = photo_img.getHeight()

        // Get the dimensions of the bitmap
        val bmOptions = BitmapFactory.Options()
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions)
        val photoW = bmOptions.outWidth;
        val photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        val scaleFactor = Math.min(photoW/targetW, photoH/targetH)

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        val bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions)
        photo_img.setImageBitmap(bitmap)
    }
*/
    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
                imageFileName, /* prefix */
                ".jpg", /* suffix */
                storageDir      /* directory */
        )

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.absolutePath
        return image
    }

    fun postAcceptRequest(task : Task){
        val client = OkHttpClient()

        val user = getSharedPreferences(getString(R.string.preference_file), Context.MODE_PRIVATE)
                .getString(getString(R.string.user_pref), "username")

        val body_post = FormBody.Builder()
                .add("username", user)
                .add("id", task.ID.toString())
                .add("issuer", task.issuer.toString())
                .add("type", task.type.toString())
                .add("lat", task.lat.toString())
                .add("lon", task.lon.toString())
                .add("radius", task.radius.toString())
                .add("duration", task.duration.toString())
                .add("what", task.what.toString())
                .build()

        val request = Request.Builder()
                .url("http://simone.faggi.tw.cs.unibo.it/api/task/accept")
                .post(body_post)
                .build()

        client.newCall(request).enqueue(object: Callback {
            override fun onResponse(call: Call?, response: Response?) {
                val body = response?.body()?.string()
                println(body)
            }
            override fun onFailure(call: Call?, e: IOException?) {
                println("Fail to execute request! $e")
            }
        })
    }

    fun postDeleteRequest(task : Task){
        val client = OkHttpClient()
        val user = getSharedPreferences(getString(R.string.preference_file), Context.MODE_PRIVATE)
                .getString(getString(R.string.user_pref), "username")

        val body_post = FormBody.Builder()
                .add("username", user)
                .add("id", task.ID.toString())
                .build()

        val request = Request.Builder()
                .url("http://simone.faggi.tw.cs.unibo.it/api/task/delete")
                .post(body_post)
                .build()

        client.newCall(request).enqueue(object: Callback {
            override fun onResponse(call: Call?, response: Response?) {
                val body = response?.body()?.string()
                println(body)
            }
            override fun onFailure(call: Call?, e: IOException?) {
                println("Fail to execute request! $e")
            }
        })
    }

    fun okToPerform(lat : String?, lon : String?, radius: String?) : Boolean{
        val tempLocation = Location(LocationManager.GPS_PROVIDER)
        tempLocation.latitude = lat!!.toDouble()
        tempLocation.longitude = lon!!.toDouble()
        if (currentPlace != null){
            if(currentPlace!!.distanceTo(tempLocation) <= radius!!.toDouble()){
                println("OK TO PERFORM")
                return true
            }
        }
        return false
    }

}
