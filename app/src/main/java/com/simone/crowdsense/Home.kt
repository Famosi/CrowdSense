package com.simone.crowdsense

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.Toolbar
import kotlinx.android.synthetic.main.activity_home.*
import java.io.IOException
import java.util.*
import okhttp3.*
import com.google.gson.GsonBuilder
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import java.util.concurrent.TimeUnit


var id_delete_list = mutableListOf<String?>()
var id_accept_list : MutableList<Task> = mutableListOf()

var currentPlace : Location? = null
var mLocationPermissionGranted = false


class Home : AppCompatActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        id_delete_list = mutableListOf()
        id_accept_list = mutableListOf()

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        recyclerView_main.layoutManager = LinearLayoutManager(this)

        getLocationPermission()

        getDeviceLocation(this)


        if (!running){
            Thread{
                run{
                    val intent = Intent(this, LocationService::class.java)
                    startService(intent)
                }
            }.start()
        }

    }

    override fun onResume() {
        super.onResume()

        val mSwipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.swipe_container)

        twitterRequest(twitterOauth(), Geocoder(this, Locale.getDefault()))

        mSwipeRefreshLayout.setOnRefreshListener {
            // This method performs the actual data-refresh operation.
            // The method calls setRefreshing(false) when it's finished.

            mSwipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary,
                    android.R.color.holo_green_dark,
                    android.R.color.holo_orange_dark,
                    android.R.color.holo_blue_dark)

            mSwipeRefreshLayout.isRefreshing = true
            twitterRequest(twitterOauth(), Geocoder(this, Locale.getDefault()))
            mSwipeRefreshLayout.isRefreshing = false
        }

        val sharedPref = getSharedPreferences(
                getString(R.string.preference_file), Context.MODE_PRIVATE)
        val user = sharedPref.getString(getString(R.string.user_pref), "user")

        Thread{
            run {
                getDeleteTask(user)
            }
        }.start()


    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.option_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.logout ->  {
                val sharedPref = getSharedPreferences(
                        getString(R.string.preference_file), Context.MODE_PRIVATE)

                with (sharedPref.edit()) {
                    putBoolean("is_log", false)
                    apply()
                }

                runOnUiThread {
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun twitterRequest(request: Request, geocoder: Geocoder){
        val client = OkHttpClient()
        client.newCall(request).enqueue(object: Callback {
            override fun onResponse(call: Call?, response: Response?) {
                val body = response?.body()?.string()

                println(body)

                if(!body!!.startsWith("[]", true)){
                    val gson = GsonBuilder().create()

                    val parseTweet = gson.fromJson(body, Statuses::class.java)

                    runOnUiThread {
                        recyclerView_main.adapter = MainAdapter(parseTweet.statuses, geocoder)
                    }

                }
            }

            override fun onFailure(call: Call?, e: IOException?) {
                println("Fail to execute request!")
            }
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {
            // If request is cancelled, the result arrays are empty.
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                mLocationPermissionGranted = true
            } else {
                Toast.makeText(this, "Can't get current position", Toast.LENGTH_SHORT).show()
                mLocationPermissionGranted = false
            }
            return
        }
    }


    fun getLocationPermission(){
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
            android.Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true
        } else {
            ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
        }
    }

    fun getDeleteTask(user : String){
        val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false)
                .build()

        val url = "http://simone.faggi.tw.cs.unibo.it/api/task/delete?username=$user"

        val request = Request.Builder()
                .url(url)
                .build()


        client.newCall(request).enqueue(object: Callback {
            override fun onResponse(call: Call?, response: Response?) {
                val body = response?.body()?.string()

                println(body + " DELETE")

                //BAD GATWAY CONTROL
                if (!body!!.startsWith("<", true)){
                    val tasks = Gson().fromJson(body, Array<Task>::class.java)
                    Thread{
                        run {
                            if (tasks[0].ID != "null"){
                                upadteDeleteList(tasks)
                            }
                        }
                    }.start()
                } else {
                    println("BAD GATEWAY DELETE")
                }
            }
            override fun onFailure(call: Call?, e: IOException?) {
                println("Fail to execute request delete! $e")
            }
        })
    }

    fun upadteDeleteList(tasks : Array<Task>){
        val iterator = tasks.iterator()
        while (iterator.hasNext()){
            val it = iterator.next()
            id_delete_list.add(it.ID)
        }
    }

}

@SuppressLint("MissingPermission")
fun getDeviceLocation(context: Context){
    val mFusionLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    try {
        if (mLocationPermissionGranted){
            val location = mFusionLocationProviderClient.lastLocation
            location.addOnCompleteListener{ task ->
                if (task.isSuccessful){
                    currentPlace = task.result
                }
            }
        }
    } catch(ex : IOException){
        println("getDeviceLocation: $ex")
    }
}

