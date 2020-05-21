package edu.uw.lho12.finalproject

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import android.view.Menu
import android.view.MenuItem

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_trail_list.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var location: LatLong
    private lateinit var trails: ArrayList<Trail>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        trails = intent.getParcelableArrayListExtra("RESULTS")
        location = intent.getParcelableExtra("LOCATION")


        // Button to change to map view
        fab.setOnClickListener { view ->
            val intent = Intent(this, TrailListActivity::class.java).apply {
                val listResults = trails
                putParcelableArrayListExtra("RESULTS", listResults as java.util.ArrayList<out Parcelable>)
                putExtra("LOCATION", location)
            }
            startActivity(intent)

        }

        setSupportActionBar(toolbar)
        toolbar.title = title
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        for (i in 0 until trails.size) {
            val lat = trails[i]!!.coordinates!!.lat!!.toDouble()
            val long = trails[i]!!.coordinates!!.long!!.toDouble()
            if (trails[i].pitches == "N/A") { // Add hike marker
                mMap.addMarker(MarkerOptions()
                    .position(LatLng(lat, long))
                    .title(trails[i].name)
                    .snippet("Rating: " + trails[i].rating + "/5")
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.tree))
                )
            } else { // Add climb marker
                mMap.addMarker(MarkerOptions()
                    .position(LatLng(lat, long))
                    .title(trails[i].name)
                    .snippet("Rating: " + trails[i].rating + "/5")
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.mountain))
                )
            }
        }

        mMap.moveCamera(CameraUpdateFactory.newLatLng(LatLng(location.lat!!.toDouble(), location.long!!.toDouble())))
        mMap.moveCamera(CameraUpdateFactory.zoomTo(9F))

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {

            R.id.action_search -> {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
