package com.simone.crowdsense

import android.app.Activity
import android.database.Cursor
import android.location.Geocoder
import android.media.Image
import android.net.Uri
import android.provider.ContactsContract
import android.provider.MediaStore
import com.google.android.gms.common.internal.service.Common
import com.google.android.gms.common.util.IOUtils
import com.google.gson.GsonBuilder
import okhttp3.*
import java.io.IOException
import java.sql.Timestamp
import java.security.SecureRandom
import java.net.URLEncoder
import org.apache.commons.codec.binary.Base64
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.SignatureException
import java.util.Formatter
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.apache.commons.codec.binary.Hex
import java.nio.charset.StandardCharsets
import oauth.signpost.OAuthConsumer
import oauth.signpost.http.HttpRequest
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer
import com.google.gson.Gson
import com.simone.crowdsense.R.id.recyclerView_main
import com.twitter.sdk.android.core.models.Tweet
import kotlinx.android.synthetic.main.activity_home.*
import oauth.signpost.OAuth.OUT_OF_BAND
import oauth.signpost.OAuthProvider
import oauth.signpost.basic.DefaultOAuthConsumer
import oauth.signpost.basic.DefaultOAuthProvider
import oauth.signpost.http.HttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.util.EntityUtils
import twitter4j.*
import twitter4j.auth.AccessToken
import twitter4j.auth.OAuthAuthorization
import twitter4j.conf.ConfigurationBuilder
import twitter4j.media.ImageUpload
import twitter4j.media.ImageUploadFactory
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

var lam_id = "983897673655730178"
var simone_id = "985771825425780736"

object HmacSha1Signature {
    private val HMAC_SHA1_ALGORITHM = "HmacSHA1"

    private fun toHexString(bytes: ByteArray): String {
        val formatter = Formatter()

        for (b in bytes) {
            formatter.format("%02x", b)
        }

        return formatter.toString()
    }

    @Throws(SignatureException::class, NoSuchAlgorithmException::class, InvalidKeyException::class)
    fun calculateRFC2104HMAC(data: String, key: String): String {
        val signingKey = SecretKeySpec(key.toByteArray(), HMAC_SHA1_ALGORITHM)
        val mac = Mac.getInstance(HMAC_SHA1_ALGORITHM)
        mac.init(signingKey)
        return toHexString(mac.doFinal(data.toByteArray()))
    }

}

fun nonceGenerator(): String {
    val secureRandom = SecureRandom()
    val stringBuilder = StringBuilder()
    for (i in 0..14) {
        stringBuilder.append(secureRandom.nextInt(10))
    }
    return stringBuilder.toString()
}

fun twitterOauth(): Request{

    var timestamp = Timestamp(System.currentTimeMillis())
    val ts = URLEncoder.encode((timestamp.getTime()/1000).toString(), "ASCII")
    val nonce = URLEncoder.encode(nonceGenerator(), "ASCII")

    val baseUrl = "https://api.twitter.com/1.1/statuses/user_timeline.json"
    val user_id = URLEncoder.encode(simone_id, "ASCII")

    val tweet_mode = URLEncoder.encode("extended", "ASCII")

    val params = "oauth_consumer_key=lWjUOd4cZMQjqCbKw4PwY7i4u&oauth_nonce=$nonce&oauth_signature_method=HMAC-SHA1&oauth_timestamp=$ts&oauth_token=985771825425780736-LRyqxRacUVoF51x7hW73CfBpkXpGfHd&oauth_version=1.0&tweet_mode=$tweet_mode&user_id=$user_id"
    val paramsEncode = URLEncoder.encode(params, "ASCII")
    val baseUrlEncode = URLEncoder.encode(baseUrl, "ASCII")

    val baseParams = "GET&$baseUrlEncode&$paramsEncode"
    var signingKey = URLEncoder.encode("tX6buaz7KFV2GWkvGtMrZccW3GMUXu4VdkxsTbMb1pDD3FCl2t", "ASCII") + "&" + URLEncoder.encode("ln9kzDDEARF2up4B5sYhAYyILAZdnnTQM9zbwQfNrt3bs", "ASCII")

    var hmac = HmacSha1Signature.calculateRFC2104HMAC(baseParams, signingKey)
    val bytes = Hex.decodeHex(hmac.toCharArray())

    val base64 = Base64.encodeBase64(bytes)
    hmac = URLEncoder.encode(base64.toString(StandardCharsets.UTF_8), "ASCII")

    var url = "$baseUrl?user_id=$user_id&tweet_mode=$tweet_mode"
    val request = Request.Builder()
            .header("Authorization", "OAuth oauth_consumer_key=\"lWjUOd4cZMQjqCbKw4PwY7i4u\", oauth_token=\"985771825425780736-LRyqxRacUVoF51x7hW73CfBpkXpGfHd\", oauth_signature_method=\"HMAC-SHA1\", oauth_timestamp=\"$ts\", oauth_nonce=\"$nonce\", oauth_version=\"1.0\", oauth_signature=\"$hmac\"")
            .url(url)
            .build()

    return request
}


fun twitterOauthPost(result : String, id : String){

    val consumer = CommonsHttpOAuthConsumer(
            "XgedeBPLyC48g4uZBwfJsjsWB",
            "kgVwDnry5mQUDbriGaKfuc9vw2Es3QwAWXngI9nl2JVy1UIXDB")

    consumer.setTokenWithSecret("1017016558466555910-7TT68vdgfgnqt3CQqguEZljmlHtUNV", "kZyZERAYkGU7b0bS58VBds2Abb4koBtyylYPtFhz4j9f2")

    val ids = id.toLong()
    val resultEncode = URLEncoder.encode(result, "ASCII")

    val httpPost = HttpPost(
            "https://api.twitter.com/1.1/statuses/update.json?status=%40Simone43663738%20$resultEncode&in_reply_to_status_id=$ids")

    consumer.sign(httpPost)

    val httpClient = DefaultHttpClient()
    val httpResponse = httpClient.execute(httpPost)

    val statusCode = httpResponse.statusLine.statusCode
    val mexCode = httpResponse.statusLine.reasonPhrase

    println(statusCode)
    println(mexCode)
    println(EntityUtils.toString(httpResponse.entity))
}

fun twitterPostPhoto(photo : File, id : String){

     val conf =  ConfigurationBuilder()
                                        .setOAuthConsumerKey("XgedeBPLyC48g4uZBwfJsjsWB")
                                        .setOAuthConsumerSecret("kgVwDnry5mQUDbriGaKfuc9vw2Es3QwAWXngI9nl2JVy1UIXDB")
                                        .setOAuthAccessToken("1017016558466555910-7TT68vdgfgnqt3CQqguEZljmlHtUNV")
                                        .setOAuthAccessTokenSecret("kZyZERAYkGU7b0bS58VBds2Abb4koBtyylYPtFhz4j9f2").build()

    val auth = OAuthAuthorization(conf)

    //val upload =  ImageUploadFactory(conf).getInstance(auth)

    val twitter = TwitterFactory(conf).instance

    val status = StatusUpdate("@Simone43663738")
    status.setMedia(photo) // set the image to be uploaded here.
    status.inReplyToStatusId = id.toLong()

    twitter.updateStatus(status)

    //val url = upload.upload(photo, "@Simone43663738 id:" + id)

}



class Tweets(val id: String, val full_text: String, val created_at : String)

class Task(val ID: String?, val issuer: String?, val type: String?, val lat: String?,
           val lon: String?, val radius: String?, val duration: String?, val what: String?)