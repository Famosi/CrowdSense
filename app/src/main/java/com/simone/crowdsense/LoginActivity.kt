package com.simone.crowdsense

import android.support.v7.app.AppCompatActivity
import android.content.*
import android.os.Bundle
import android.view.View
import android.widget.EditText
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.activity_login.*
import okhttp3.*
import java.io.IOException
import okhttp3.OkHttpClient

var user_log = "simone"
const val EXTRA_MESSAGE = "com.example.CrowdSense.MESSAGE"

/**
 * A login screen that offers login via username/password.
 */
class LoginActivity : AppCompatActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val sharedPref = getSharedPreferences(
                getString(R.string.preference_file), Context.MODE_PRIVATE)

        if (!sharedPref.getBoolean("is_log", false)) {
            sign_in_btn.setOnClickListener {
                attemptLogin()
            }

            go_sign_up.setOnClickListener {
                val intent = Intent(this, SignupActivity::class.java).apply {
                    putExtra(EXTRA_MESSAGE, user_log)
                }
                startActivity(intent)
            }
        }
        else{
            goHome(getSharedPreferences(getString(R.string.preference_file), Context.MODE_PRIVATE)
                    .getString(getString(R.string.user_pref), "user"))
            finish()
        }

    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid username, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */

    private fun attemptLogin(){
        val username = findViewById<EditText>(R.id.username_field).text.toString()
        val password = findViewById<EditText>(R.id.password_field).text.toString()
        requestSignIn("http://simone.faggi.tw.cs.unibo.it/api/user/login", username, password)
    }

    private fun requestSignIn(url: String, username: String, password: String){

        val client = OkHttpClient()
        val body = FormBody.Builder()
            .add("username", username).add("password", password)
            .build()

        val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

        client.newCall(request).enqueue(object: Callback {
            override fun onResponse(call: Call?, response: Response?) {
                val body = response?.body()?.string()

                println(body)

                val gson = GsonBuilder().create()
                val Success = gson.fromJson(body, Success::class.java)

                if (Success.success == "true") {
                    //is_log = true
                    val user = gson.fromJson(body, User::class.java)

                    goHome(user.username)
                    finish()
                }
                else {
                    val err = gson.fromJson(body, Error::class.java)
                    runOnUiThread {
                        showError(err.error, err.field)
                    }
                }

            }
            override fun onFailure(call: Call?, e: IOException?) {
                println("Fail to execute request! $e")
            }
        })

    }

    private fun showError(err : String, field : String){
        var focusView : View? = null

        when(field){
            "username" -> {
                username_field.error = err
                focusView = username_field
            }
            "password" -> {
                password_field.error = err
                focusView = password_field
            }
        }
        focusView?.requestFocus()
    }

    private fun goHome(username: String){
        val sharedPref = getSharedPreferences(
                getString(R.string.preference_file), Context.MODE_PRIVATE)

        with (sharedPref.edit()) {
            println(username)
            putBoolean("is_log", true)
            putString(getString(R.string.user_pref), username)
            commit()
        }

        val intent = Intent(this, Home::class.java)
        startActivity(intent)
    }
}

class Success(val success : String)
class Error(val error : String, val field : String)
class User(val username : String)

