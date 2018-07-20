package com.simone.crowdsense

import android.content.Context
import android.content.Intent
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.gson.*
import kotlinx.android.synthetic.main.task_row.view.*
import android.location.Geocoder
import android.os.Bundle
import android.support.v4.content.ContextCompat.startActivity
import java.text.SimpleDateFormat
import java.util.*

var incomingTasks = mutableListOf<Task>()


class MainAdapter(private val tweets: Array<Tweets>, private val geocoder: Geocoder) : RecyclerView.Adapter<CustomViewHolder>(){

    override fun getItemCount(): Int {
        when (tweets.isEmpty()){
            true -> return 1
            false -> return tweets.count()
        }
    }

    override fun getItemViewType(position: Int): Int {

        /*
    * 0 default
    * 1 okToDisplay
    * 2 noToDisplay
    */
        val tweet = tweets[position]

        if (tweet.full_text.startsWith("#LAM_CROWD18")) {

            val str = tweet.full_text.split("#LAM_CROWD18")[1]
            val time = tweet.created_at

            val task = Gson().fromJson(str, Task::class.java)

            val id = task.ID

            if (!id_delete_list.contains(id) && getDifference(time) <= task.duration!!.toInt()) {
                return 1
            }
        }
        return 2
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
        val layoutInflater = LayoutInflater.from(parent?.context)
        val cellForRow: View
        when (tweets.isEmpty()){
            true -> cellForRow = layoutInflater.inflate(R.layout.no_row, parent, false)
            false -> {
                if (viewType == 1){
                    cellForRow = layoutInflater.inflate(R.layout.task_row, parent, false)
                }
                else{
                    cellForRow = layoutInflater.inflate(R.layout.notoperform, parent, false)
                }
            }
        }
        return CustomViewHolder(cellForRow, parent.context)
    }

    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        val tweet = tweets[position]

        GsonBuilder().create()

        if(holder.itemViewType == 1){
            val str = tweet.full_text.split("#LAM_CROWD18")[1]

            val task = Gson().fromJson(str, Task::class.java)

            val id = task.ID

            if (!incomingTasks.any { Task -> Task.ID == id }){
                incomingTasks.add(task)
            }
            val addresses = geocoder.getFromLocation(task.lat!!.toDouble(), task.lon!!.toDouble(), 1);

            holder?.view?.taskWhat?.text = task.what!!.capitalize()
            holder?.view?.taskPlace?.text = "${addresses.get(0).getAddressLine(0)}"

            when (task.type) {
                "picture" -> holder?.view?.taskImg?.setImageResource(R.drawable.ic_camera)
                "RSSI" -> holder?.view?.taskImg?.setImageResource(R.drawable.ic_rssi)
                "noise" -> holder?.view?.taskImg?.setImageResource(R.drawable.ic_noise)
                "light" -> holder?.view?.taskImg?.setImageResource(R.drawable.ic_light)
                "temperature" -> holder?.view?.taskImg?.setImageResource(R.drawable.ic_temperature)
            }

            holder?.view.parent_layout.setOnClickListener {
                val intent = Intent(holder.context, TaskActivity::class.java)
                intent.putExtra("what", task.what.capitalize())
                intent.putExtra("issuer", task.issuer)
                intent.putExtra("place", addresses[0].getAddressLine(0))
                intent.putExtra("duration", task.duration)
                intent.putExtra("type", task.type)
                intent.putExtra("lat", task.lat)
                intent.putExtra("lon", task.lon)
                intent.putExtra("radius", task.radius)
                intent.putExtra("ID", task.ID)
                intent.putExtra("tweetID", tweet.id)
                startActivity(holder.context, intent, Bundle())
            }
        }
    }

    fun getDifference(time : String) : Int {
        val sdf = SimpleDateFormat("EEE MMM dd kk:mm:ss ZZZZ yyyy", Locale.ENGLISH)

        val d = sdf.parse(time)

        val c = Calendar.getInstance()
        c.setTime(d)

        val time = c.getTimeInMillis()/1000
        val curr = System.currentTimeMillis()/1000
        val diff = ((curr - time)/86400).toInt()   //Time difference in milliseconds

        return diff
    }

}

class CustomViewHolder(val view: View, val context: Context): RecyclerView.ViewHolder(view)