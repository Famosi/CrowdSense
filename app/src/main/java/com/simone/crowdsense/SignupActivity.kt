package com.simone.crowdsense

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.EditText
import com.google.gson.GsonBuilder
import com.simone.crowdsense.R.layout.activity_signup
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_signup.*
import okhttp3.*
import java.io.IOException

class SignupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(activity_signup)

        sign_up_btn.setOnClickListener {
            attemptSignup()
        }
    }

    private fun attemptSignup() {
        val username = findViewById<EditText>(R.id.username_sup_field).text.toString()
        val password = findViewById<EditText>(R.id.password_sup_field).text.toString()
        val confirm_psw = findViewById<EditText>(R.id.confirm_psw_field).text.toString()
        requestSignUp("http://simone.faggi.tw.cs.unibo.it/api/user/signup", username, password, confirm_psw)
    }

    private fun requestSignUp(url: String, username: String, password: String, confirm_psw : String){
        val client = OkHttpClient()
        val JSON = MediaType.parse("application/json; charset=utf-8")
        val body = FormBody.Builder()
                .add("username", username).add("password", password).add("password_confirmation", confirm_psw)
                .build()

        val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

        client.newCall(request).enqueue(object: Callback {
            override fun onResponse(call: Call?, response: Response?) {
                val body = response?.body()?.string()

                val gson = GsonBuilder().create()
                val Success = gson.fromJson(body, Success::class.java)

                if (Success.success == "true") {
                    //is_log = true
                    val user = gson.fromJson(body, User::class.java)
                    user_log = user.username
                    goHome()
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

    private fun goHome(){
        val intent = Intent(this, Home::class.java).apply {
            putExtra(EXTRA_MESSAGE, user_log)
        }
        startActivity(intent)
    }

    private fun showError(err : String, field : String){
        var focusView : View? = null
        println(field)
        when(field){
            "username" -> {
                username_sup_field.error = err
                focusView = username_field
            }
            "password" -> {
                password_sup_field.error = err
                focusView = password_field
            }
            "confirm_psw" -> {
                confirm_psw_field.error = err
                focusView = confirm_psw_field
            }
        }
        focusView?.requestFocus()
    }

}

