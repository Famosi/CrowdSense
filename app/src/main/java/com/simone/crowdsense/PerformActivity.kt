package com.simone.crowdsense

import android.app.Activity
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_perform.*
import android.hardware.SensorManager
import android.hardware.SensorEvent
import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.os.Handler
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import pl.bclogic.pulsator4droid.library.PulsatorLayout


class PerformActivity : AppCompatActivity(), SensorEventListener {

    private var mSensorManager: SensorManager? = null
    private var mSensor: Sensor? = null

    private var mProgressBarStatus = 0
    private var mProgressBar: ProgressBar? = null
    private var mHandler = Handler()

    private var mActivityPerformImg : ImageView? = null

    private var mPulsator: PulsatorLayout? = null

    private var sensorArrayPerform: MutableList<Float> = mutableListOf()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perform)

        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        // Get an instance of the sensor service, and use that to get an instance of
        // a particular sensor.
        if (intent.hasExtra("type_to_perform")) {
            when (intent.getStringExtra("type_to_perform")) {

                "light" -> {
                    mActivityPerformImg = findViewById(R.id.type_to_perform_img)
                    mActivityPerformImg!!.background = getDrawable(R.drawable.ic_light)
                    mSensor = mSensorManager!!.getDefaultSensor(Sensor.TYPE_LIGHT)
                }

                "temperature" -> {
                    mActivityPerformImg = findViewById(R.id.type_to_perform_img)
                    mActivityPerformImg!!.background = getDrawable(R.drawable.ic_temperature)
                    if (mSensorManager!!.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) == null) {
                        sensor_val_txt.text = "Can't get " + intent.getStringExtra("type_to_perform").capitalize()
                    }
                }
            }
        }

        mProgressBar = findViewById(R.id.progress_bar)
        mProgressBarStatus = 0

        mPulsator = findViewById(R.id.pulsator)
        mPulsator!!.start()


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
                        val lenght = sensorArrayPerform.count()

                        val value = getMedia(lenght.toFloat()).toInt()

                        val returnIntent = Intent()

                        returnIntent.putExtra("result", "" + value)
                        setResult(Activity.RESULT_OK, returnIntent)
                        finish()
                    }
                }
            }
        }.start()


    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Do something here if sensor accuracy changes.
    }

    override fun onSensorChanged(event: SensorEvent) {
        val sensor_value = event.values[0]
        sensor_val_txt.text = "" + sensor_value
        sensorArrayPerform.add(sensor_value)
    }

    override fun onResume() {
        // Register a listener for the sensor.
        super.onResume()
        mSensorManager!!.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        //unregister the sensor when the activity pauses.
        super.onPause()
        mSensorManager!!.unregisterListener(this)
    }

    fun getMedia(len : Float) : Float{
        var sum : Float = 0.toFloat()
        for (item in sensorArrayPerform) {
            sum += item
        }
        return sum / len
    }

}
