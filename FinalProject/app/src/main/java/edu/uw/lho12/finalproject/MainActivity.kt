package edu.uw.lho12.finalproject

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.util.LruCache
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.ImageLoader
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.min

object ApiType{
    var hike: Boolean = false
    var climb: Boolean = false
}

object DifficultyLevel{
    var easy: Boolean = false
    var medium: Boolean = false
    var hard: Boolean = false
}

object LocationCoordinates {
    var lat: String = ""
    var long: String = ""
}

object ResultsView {
    var map = false
    var list = false
}

const val MIN_EASY = "5.0"
const val MAX_EASY = "5.7"
const val MIN_MED = "5.8"
const val MAX_MED = "5.10"
const val MIN_HARD = "5.11"
const val MAX_HARD = "5.12"
const val MAX_DISTANCE_DEFAULT = "30"
const val MIN_LENGTH_DEFAULT = "0"

private var hikeResults = arrayListOf<Trail>()

private var climbResults = arrayListOf<Trail>()

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        // Asks for permission to use location on the initial run if it hasn't be chosen yet
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        }

        // If permission is granted, get the user's location
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            getUserLocation()
        }

        get_user_location.setOnClickListener {
            // Ask for permissions if it wasn't specified already (If granted, will get the location via the callback afterwards)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    1
                )
            } else {
                // Otherwise, just get the user's location
                getUserLocation()
            }
        }

        // Show blinking cursor whenever the EditText is clicked
        location_input.setOnClickListener {
            location_input.isCursorVisible = true
        }

        // Submit input to search for results
        search_button.setOnClickListener {

            val location = location_input.text.toString()
            var locationURL: String? = null

            if (location != "") {
                // OpenCage Geocoder API to parse location input into Lat and Long coordinates so it can be
                // used in the trail API call
                val builder = Uri.Builder()
                locationURL = builder.scheme("https")
                    .authority("api.opencagedata.com")
                    .appendPath("geocode")
                    .appendPath("v1")
                    .appendPath("json")
                    .appendQueryParameter("q", location)
                    .appendQueryParameter("key", "8778169fb7994706a74b9b1c7b499a36")
                    .build().toString()
            }

            var maxDistance = max_distance_input.text.toString()
            if (maxDistance.isEmpty()){
                maxDistance = MAX_DISTANCE_DEFAULT
            }

            var minLength = min_length_input.text.toString()
            if (minLength.isEmpty()) {
                minLength = MIN_LENGTH_DEFAULT
            }

            if (!ApiType.hike && !ApiType.climb) {
                Toast.makeText(this, "Please select an activity type", Toast.LENGTH_LONG).show()
            } else {
                generateAPIData(locationURL, maxDistance, minLength)
                Handler().postDelayed({
                    val results = arrayListOf<Trail>()
                    var intent = Intent()
                    results.addAll(hikeResults)
                    results.addAll(climbResults)
                    if (results.size == 0) {
                        // Case for when there are no results for the search query
                        Toast.makeText(this, "No Trails Found", Toast.LENGTH_LONG).show()
                    } else {
                        if (ResultsView.map && !ResultsView.list) {
                            intent = Intent(this, MapsActivity::class.java).apply {
                                putParcelableArrayListExtra("RESULTS", results)
                                putExtra(
                                    "LOCATION",
                                    LatLong(LocationCoordinates.lat, LocationCoordinates.long)
                                )
                            }
                            startActivity(intent)
                        } else if (!ResultsView.map && ResultsView.list) {
                            intent = Intent(this, TrailListActivity::class.java).apply {
                                putParcelableArrayListExtra("RESULTS", results)
                                putExtra(
                                    "LOCATION",
                                    LatLong(LocationCoordinates.lat, LocationCoordinates.long)
                                )
                            }
                            startActivity(intent)
                        } else {
                            Toast.makeText(
                                this,
                                "Please choose a method to see your results",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    hikeResults.clear()
                    climbResults.clear()
                }, 2000)
            }
        }
    }

    // Callback to get the user's location when selecting allowed after permissions is asked
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 1) {
            if(permissions[0].equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getUserLocation()
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    // Gets the lat and long coordinates of the user's current location
    fun getUserLocation() {
        location_input.setHint(R.string.current_location)
        location_input.setText(null)
        location_input.isCursorVisible = false
        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationProviderClient.lastLocation
            .addOnSuccessListener { locationPoint: Location? ->
                locationPoint?.let {
                    val pos = LatLng(it.latitude, it.longitude)
                    location_input.setHint(R.string.current_location)
                    LocationCoordinates.lat = pos.latitude.toString()
                    LocationCoordinates.long = pos.longitude.toString()
                }
            }
    }

    // Handles the checkbox selections
    fun checkBoxClicked(view: View) {
        if (view is CheckBox) {
            val checked: Boolean = view.isChecked

            when(view.id) {
                R.id.hike_checkbox -> {
                    when(checked) {
                        true -> ApiType.hike = true
                        false -> ApiType.hike = false
                    }
                }
                R.id.climb_checkbox -> {
                    when(checked) {
                        true -> ApiType.climb = true
                        false -> ApiType.climb = false
                    }
                }
                R.id.easy_checkbox -> {
                    when(checked) {
                        true -> DifficultyLevel.easy = true
                        false -> DifficultyLevel.easy = false
                    }
                }
                R.id.medium_checkbox -> {
                    when(checked) {
                        true -> DifficultyLevel.medium = true
                        false -> DifficultyLevel.medium = false
                    }
                }
                R.id.hard_checkbox -> {
                    when(checked) {
                        true -> DifficultyLevel.hard = true
                        false -> DifficultyLevel.hard = false
                    }
                }
            }
        }
    }

    fun onRadioButtonClicked(view: View) {
        if (view is RadioButton) {
            val checked = view.isChecked

            when (view.id) {
                R.id.map_radio_button -> {
                    when (checked) {
                        true -> {
                            ResultsView.map = true
                            ResultsView.list = false
                        }
                    }
                }
                R.id.list_radio_button -> {
                    when (checked) {
                        true -> {
                            ResultsView.map = false
                            ResultsView.list = true
                        }
                    }
                }
            }
        }
    }

    fun generateAPIData(url: String?, maxDistance: String, minLength: String) {
        val queue = APICaller.DataRequestQueue(applicationContext)

        if (url != null) {
            val jsonObjectRequest = JsonObjectRequest(
                Request.Method.GET, url, null,
                Response.Listener { response ->

                    // API used to convert location input into lat and long coordinates
                    val latLongJSON = response.getJSONArray("results")
                        .getJSONObject(0)
                        .getJSONObject("geometry")

                    LocationCoordinates.lat = latLongJSON.get("lat").toString()
                    LocationCoordinates.long = latLongJSON.get("lng").toString()

                    apiCallLogic(LocationCoordinates, maxDistance, minLength)
                },
                Response.ErrorListener { error -> }
            )

            queue?.add(jsonObjectRequest)
        } else {
            apiCallLogic(LocationCoordinates, maxDistance, minLength)
        }
    }

    // Logic for determining which API(s) to call
    private fun apiCallLogic(coordinates: LocationCoordinates, maxDistance: String, minLength: String) {
        if (ApiType.hike && ApiType.climb) {
            // If both hike and climb are selected, call both APIS
            val hikeUrl = generateURL(coordinates, maxDistance, minLength, "hike")
            val climbUrl = generateURL(coordinates, maxDistance, minLength, "climb")
            generateTrails(hikeUrl as String, "hike")
            generateTrails(climbUrl as String, "climb")
        } else if (ApiType.hike) {
            val hikeUrl = generateURL(coordinates, maxDistance, minLength, "hike")
            generateTrails(hikeUrl as String, "hike")
        } else if (ApiType.climb) {
            val climbUrl = generateURL(coordinates, maxDistance, minLength, "climb")
            generateTrails(climbUrl as String, "climb")
        }
    }

    fun generateURL(locationCoordinates: LocationCoordinates, maxDistance: String, minLength: String, type: String): String? {
        val builder = Uri.Builder()

        if (type == "hike") {
            // Hiking Project API to get hiking trails
            val url = builder.scheme("https")
                .authority("www.hikingproject.com")
                .appendPath("data")
                .appendPath("get-trails")
                .appendQueryParameter("lat", LocationCoordinates.lat)
                .appendQueryParameter("lon", LocationCoordinates.long)
                .appendQueryParameter("maxDistance", maxDistance)
                .appendQueryParameter("minLength", minLength)
                .appendQueryParameter("key","200643783-ee45c967bbba815f5a560164e1dd6621")
                .build().toString()
            return url
        }
        if (type == "climb") {

            var minDiff: String? = null
            var maxDiff: String? = null

            // Decode selected difficulty levels so the API can be called
            if (DifficultyLevel.easy) {
                minDiff = MIN_EASY
                maxDiff = MAX_EASY
            } else if (DifficultyLevel.medium) {
                minDiff = MIN_MED
                maxDiff = MAX_MED
            } else if (DifficultyLevel.hard) {
                minDiff = MIN_HARD
                maxDiff = MAX_HARD
            }

            // Mountain Project API to get climbing routes
            val url = builder.scheme("https")
                .authority("www.mountainproject.com")
                .appendPath("data")
                .appendPath("get-routes-for-lat-lon")
                .appendQueryParameter("lat", locationCoordinates.lat)
                .appendQueryParameter("lon", locationCoordinates.long)
                .appendQueryParameter("maxDistance", maxDistance)
                .appendQueryParameter("minDiff", minDiff)
                .appendQueryParameter("maxDiff", maxDiff)
                .appendQueryParameter("key", "200643783-a32296d693ae75fccf909d0b312a8fc7")
                .build().toString()
            return url
        }
        return null
    }

    // Call to get the trail data from either the hiking or mountain project
    fun generateTrails(url: String, type: String) {
        val queue = APICaller.DataRequestQueue(applicationContext)
        val results = listOf<Trail>()
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            Response.Listener { response ->
                val apiOutput = parseTrailAPI(response, type, DifficultyLevel)
                setResults(apiOutput, type)
            },
            Response.ErrorListener { error -> }
        )

        queue?.add(jsonObjectRequest)
    }

    // Helper method to set the API results to the appropriate list
    private fun setResults(output: List<Trail>, type: String) {
        if (type == "hike") {
            hikeResults = output as ArrayList<Trail>
        } else {
            climbResults = output as ArrayList<Trail>
        }
    }
}

class APICaller {
    companion object Caller {
        private var queue: RequestQueue? = null

        fun DataRequestQueue(context: Context): RequestQueue? {
            queue = Volley.newRequestQueue(context)
            return queue
        }

        val imageLoader: ImageLoader by lazy { //only instantiate when needed
            ImageLoader(
                queue,
                object : ImageLoader.ImageCache { //anonymous cache object
                    private val cache = LruCache<String, Bitmap>(20)
                    override fun getBitmap(url: String): Bitmap? {
                        return cache.get(url)
                    }
                    override fun putBitmap(url: String, bitmap: Bitmap) {
                        cache.put(url, bitmap)
                    }
                })
        }
    }
}
