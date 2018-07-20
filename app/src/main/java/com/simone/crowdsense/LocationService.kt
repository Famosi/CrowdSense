package com.simone.crowdsense

import android.app.*
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.Location.distanceBetween
import android.location.LocationManager
import android.os.Build
import android.support.v4.content.ContextCompat
import android.widget.Toast
import android.os.Handler
import android.os.Parcelable
import android.os.Looper.getMainLooper
import android.provider.Settings
import android.provider.Settings.Global.getString
import android.support.v4.app.NotificationCompat
import java.util.*
import android.support.v4.app.NotificationManagerCompat
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.activity_home.*
import okhttp3.*
import java.io.IOException
import java.sql.SQLTransactionRollbackException
import java.util.concurrent.TimeUnit


const val CHANNEL_ID = "CHANNEL_ID"
private var id = 0
var running = false

var myIncomingTweets = mutableListOf<Task>()
var numberOfTasks = 0
var update = true
var lastTask : MutableList<String?> = mutableListOf()




class LocationService : IntentService("LocationService") {

    private var timer : Timer? = null

    override fun onCreate() {
        super.onCreate()
    }

    override fun onHandleIntent(intent: Intent?) {
        running = true
        getPosition()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return Service.START_STICKY
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private fun getPosition() {
        Toast.makeText(this, "service start", Toast.LENGTH_SHORT).show()
        if (timer != null) {
            timer!!.cancel()
        }
        timer = Timer()

        val user = getSharedPreferences(getString(R.string.preference_file), Context.MODE_PRIVATE)
                .getString(getString(R.string.user_pref), "username")

        timer!!.scheduleAtFixedRate(LocationTimer(this, user), 0, 60000 * 15)
    }


    private class LocationTimer(val context: Context, val user: String) : TimerTask() {

        override fun run() {
            if (ContextCompat.checkSelfPermission(context,
                            android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED){

                if (currentPlace == null){
                    getDeviceLocation(context)
                }

                if (id_accept_list.count() == 0){
                    getAcceptTasks(user)
                } else {
                    okToPerform(id_accept_list)
                }
            }

            twitterRequest(twitterOauth())

            if (myIncomingTweets.count() > numberOfTasks){
                createNotificationChannel()
                val mBuilder = setNotificationHome()
                val notificationManager = NotificationManagerCompat.from(context)
                numberOfTasks = myIncomingTweets.count()
                id +=1
                notificationManager.notify(id, mBuilder.build())
            }
        }

        fun sendNotification(task: Task){
            createNotificationChannel()
            val mBuilder = setNotification(task)
            val notificationManager = NotificationManagerCompat.from(context)

            //Send notification only one time
            if (!lastTask.contains(task.ID)){
                id = id + 1
                lastTask.add(task.ID)
                notificationManager.notify(id, mBuilder.build())
            }
        }

        fun setNotification(task : Task): NotificationCompat.Builder {

            val address = Geocoder(context, Locale.getDefault()).getFromLocation(task.lat!!.toDouble(), task.lon!!.toDouble(), 1)

            val intent = Intent(context, TaskActivity::class.java)
                    .putExtra("what", task.what!!.capitalize())
                    .putExtra("issuer", task.issuer)
                    .putExtra("duration", task.duration )
                    .putExtra("type", task.type)
                    .putExtra("lat", task.lat)
                    .putExtra("lon", task.lon)
                    .putExtra("place", address[0].getAddressLine(0))
                    .putExtra("radius", task.radius)
                    .putExtra("ID", task.ID)

            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)

            val mBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.logo_round)
                    .setContentTitle(task.what + "")
                    .setContentText("You can perform a new task!")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)

            return mBuilder
        }

        fun setNotificationHome() : NotificationCompat.Builder{
            val intent = Intent(context, TaskActivity::class.java)

            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)

            val mBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.logo_round)
                    .setContentTitle("New task")
                    .setContentText("New task was posted")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)

            return mBuilder
        }

        fun createNotificationChannel(){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                val name = R.string.channel_name.toString()
                val description = R.string.channel_description.toString()
                val importance = NotificationManager.IMPORTANCE_DEFAULT
                val channel = NotificationChannel(CHANNEL_ID, name, importance)
                channel.description = description
                val notificationManager = context.getSystemService(NotificationManager::class.java)
                notificationManager.createNotificationChannel(channel)
            }
        }

        fun distanceToPerform(tasks : Array<Task>?) {
            val iterator = tasks!!.iterator()
            while (iterator.hasNext()){
                val it = iterator.next()

                id_accept_list.add(it)

                val lat = it.lat!!.toDouble()
                val lon = it.lon!!.toDouble()
                val tempLocation = Location(LocationManager.GPS_PROVIDER)
                val curLocation = Location(LocationManager.GPS_PROVIDER)
                tempLocation.latitude = lat
                tempLocation.longitude = lon
                curLocation.latitude = lat
                curLocation.longitude = lon
                if (currentPlace != null){
                    if(currentPlace!!.distanceTo(tempLocation) <= it.radius!!.toDouble()){
                        println("OK TO PERFORM")
                        sendNotification(it)
                    }
                    println("CAN'T PERFORM")
                }
            }
        }

        fun okToPerform(tasks : MutableList<Task>){
            val iterator = tasks.iterator()
            while (iterator.hasNext()){

                val it = iterator.next()

                val lat = it.lat!!.toDouble()
                val lon = it.lon!!.toDouble()
                val tempLocation = Location(LocationManager.GPS_PROVIDER)
                tempLocation.latitude = lat
                tempLocation.longitude = lon
                if (currentPlace != null){
                    if(currentPlace!!.distanceTo(tempLocation) <= it.radius!!.toDouble()){
                        println("OK TO PERFORM")
                        sendNotification(it)
                    }
                    println("CAN'T PERFORM")
                }
            }
        }

        fun getAcceptTasks(user : String){
            val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(false)
                    .build()

            val url = "http://simone.faggi.tw.cs.unibo.it/api/task/accept?username=$user"

            val request = Request.Builder()
                    .url(url)
                    .build()


            client.newCall(request).enqueue(object: Callback {
                override fun onResponse(call: Call?, response: Response?) {
                    val body = response?.body()?.string()

                    println(body + " ACCEPT service")

                    if (!body!!.startsWith("<", true) && !body.startsWith("[]", true)){
                        val tasks = Gson().fromJson(body, Array<Task>::class.java)
                        Thread{
                            run {
                                if (tasks[0].ID != "null"){
                                    distanceToPerform(tasks)
                                }
                            }
                        }.start()
                    } else {
                        println("BAD GATEWAY")
                    }
                }
                override fun onFailure(call: Call?, e: IOException?) {
                    println("Fail to execute request! $e")
                }
            })
        }

        fun twitterRequest(request: Request){
            val client = OkHttpClient()

            client.newCall(request).enqueue(object: Callback {
                override fun onResponse(call: Call?, response: Response?) {
                    val body = response?.body()?.string()

                    println(body)

                    if (!body!!.startsWith("[]", true)){

                        val gson = GsonBuilder().create()

                        val tweets = gson.fromJson(body, Statuses::class.java)

                        for (tweet in tweets.statuses){
                            if (tweet.full_text.startsWith("#LAM_CROWD18")){
                                val str = tweet.full_text.split("#LAM_CROWD18")[1]
                                val task = Gson().fromJson(str, Task::class.java)
                                val id = task.ID
                                if (!myIncomingTweets.any { Task -> Task.ID == id }){
                                    myIncomingTweets.add(task)
                                }
                            }
                        }
                        if (update){
                            numberOfTasks = myIncomingTweets.count()
                            update = false
                        }
                    }
                }
                override fun onFailure(call: Call?, e: IOException?) {
                    println("Fail to execute request!")
                }
            })
        }
    }
}