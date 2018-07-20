package com.simone.crowdsense

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.telephony.PhoneStateListener
import android.telephony.CellSignalStrengthGsm
import android.telephony.CellInfoGsm
import android.content.Context.TELEPHONY_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.telephony.TelephonyManager
import kotlinx.android.synthetic.main.activity_perform.*
import android.telephony.SignalStrength
import android.system.Os.listen
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.simone.crowdsense.R.id.sensor_val_txt
import pl.bclogic.pulsator4droid.library.PulsatorLayout


var mTelephonyManager: TelephonyManager? = null
var mPhoneStatelistener: MyPhoneStateListener? = null
var mSignalStrength = 0
var sensorArrayRssi: MutableList<Float> = mutableListOf()

//TODO: MEDIA

class RssiActivity : AppCompatActivity() {

    private var mProgressBarStatus = 0
    private var mProgressBar: ProgressBar? = null
    private var mHandler = Handler()
    private var mActivityPerformImg : ImageView? = null

    private var mPulsator: PulsatorLayout? = null


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_perform)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 1)

        } else {
            getRssi()

            mProgressBar = findViewById(R.id.progress_bar)
            mProgressBarStatus = 0

            sensor_val_txt.text = "-113"

            mPulsator = findViewById(R.id.pulsator)
            mPulsator!!.start()

            mActivityPerformImg = findViewById(R.id.type_to_perform_img)
            mActivityPerformImg!!.background = getDrawable(R.drawable.ic_rssi)

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
                            val lenght = sensorArrayNoise.count() - 1

                            val value = mSignalStrength

                            sensor_val_txt.text = mSignalStrength.toString()

                            val returnIntent = Intent()

                            returnIntent.putExtra("result", "" + value)
                            setResult(Activity.RESULT_OK, returnIntent)
                            finish()
                        }
                    }
                }
            }.start()
        }

    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 1) {
            // If request is cancelled, the result arrays are empty.
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                val bundle = Bundle()
                onCreate(bundle)
            } else {
                sensor_val_txt.text = "Can't use RSSI"
            }
            return
        }
    }

    fun getRssi(){
        mPhoneStatelistener = MyPhoneStateListener()
        mTelephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        mTelephonyManager!!.listen(mPhoneStatelistener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
    }
}

class MyPhoneStateListener : PhoneStateListener() {

    override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
        super.onSignalStrengthsChanged(signalStrength)
        mSignalStrength = signalStrength.gsmSignalStrength
        mSignalStrength = 2 * mSignalStrength - 113 // -> dBm
    }
}