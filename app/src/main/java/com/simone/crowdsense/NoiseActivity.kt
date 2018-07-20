package com.simone.crowdsense

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_perform.*
import java.io.IOException
import pl.bclogic.pulsator4droid.library.PulsatorLayout
import java.lang.Math.log10
import java.util.*


var db : Double = 0.0
var sensorArrayNoise: MutableList<Float> = mutableListOf()

class NoiseActivity : AppCompatActivity() {

    private var mRecorder : MediaRecorder? = null
    private var mEMA = 0.0

    private var mProgressBarStatus = 0
    private var mProgressBar: ProgressBar? = null
    private var mHandler = Handler()
    private var mActivityPerformImg : ImageView? = null


    private var mPulsator: PulsatorLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perform)

        sensorArrayNoise = mutableListOf()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_NOISE_CAPTURE)

        } else {
            start()
            animation()
        }
    }

    fun getMedia(len : Float) : Float{
        var sum : Float = 0.toFloat()
        for (item in sensorArrayNoise) {
            sum += item
        }
        return sum / len
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_NOISE_CAPTURE) {
            // If request is cancelled, the result arrays are empty.
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                start()
                animation()
            } else {
                sensor_val_txt.text = "Can't use noise"
            }
            return
        }
    }

    fun start(){
        if (mRecorder == null){
            mRecorder = MediaRecorder()
            mRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
            mRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            mRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            mRecorder!!.setOutputFile("/dev/null/")

            val record = RecorderTask(mRecorder!!, this, sensor_val_txt)

            val timer = Timer()
            timer.scheduleAtFixedRate(record, 0, 1000) // will update Value every 1 second

            try {
                mRecorder!!.prepare()
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            mRecorder!!.start()
            mEMA = 0.0
        }
    }

    fun stop(){
        if (mRecorder != null){
            mRecorder!!.stop()
        }
    }

    fun animation(){
        mProgressBar = findViewById(R.id.progress_bar)
        mProgressBarStatus = 0

        mPulsator = findViewById(R.id.pulsator)
        mPulsator!!.start()

        mActivityPerformImg = findViewById(R.id.type_to_perform_img)
        mActivityPerformImg!!.background = getDrawable(R.drawable.ic_noise)

        Thread {
            run {
                while (mProgressBarStatus < 100) {
                    mProgressBarStatus++
                    android.os.SystemClock.sleep(50)
                    mHandler.post {
                        run {
                            mProgressBar!!.progress = mProgressBarStatus
                        }
                    }
                }
                mHandler.post {
                    run {
                        val lenght = sensorArrayNoise.count()

                        db = 0.0

                        val value = getMedia(lenght.toFloat()).toInt()

                        val returnIntent = Intent()

                        returnIntent.putExtra("result", "" + value)
                        setResult(Activity.RESULT_OK, returnIntent)
                        stop()
                        finish()
                    }
                }
            }
        }.start()
    }


    private class RecorderTask(private val recorder: MediaRecorder, val mActivity : NoiseActivity, val sensor_txt : TextView) : TimerTask() {

        override fun run() {
            db = 20*log10(recorder!!.maxAmplitude.toDouble())

            mActivity.runOnUiThread {
                if (db.toInt() > 0){
                    sensor_txt.text = "" + db.toInt() + "dB"
                    sensorArrayNoise.add(db.toFloat())
                }
            }
        }
    }

}