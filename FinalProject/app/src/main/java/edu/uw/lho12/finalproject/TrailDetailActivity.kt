package edu.uw.lho12.finalproject

import android.content.Intent
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.view.MenuItem
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import edu.uw.lho12.finalproject.APICaller.Caller.imageLoader
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_trail_detail.*

/**
 * An activity representing a single Trail detail screen. This
 * activity is only used on narrow width devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a [TrailListActivity].
 */
class TrailDetailActivity : AppCompatActivity(), TrailDetailFragment.HasCollapsibleToolbar, OnMapReadyCallback {

    private var googleMap: GoogleMap? = null
    private var trailName: String? = ""
    private var currTrail: Trail? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trail_detail)
        setSupportActionBar(detail_toolbar)

        val trail = intent.getParcelableExtra<Trail>(TrailDetailFragment.ARG_ITEM_ID)
        currTrail = trail
        trailName = trail.name

        //detail_trail_image.setImageUrl(trail.imageUrl, imageLoader)

        fab.setOnClickListener { view ->
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "Do you want to check this place out? " + trail.url)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, null)
            startActivity(shareIntent)
        }

        // Show the Up button in the action bar.
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // savedInstanceState is non-null when there is fragment state
        // saved from previous configurations of this activity
        // (e.g. when rotating the screen from portrait to landscape).
        // In this case, the fragment will automatically be re-added
        // to its container so we don't need to manually add it.
        // For more information, see the Fragments API guide at:
        //
        // http://developer.android.com/guide/components/fragments.html
        //
        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.
            val fragment = TrailDetailFragment.newInstance(trail)
            val mapFragment = SupportMapFragment.newInstance()
            supportFragmentManager.beginTransaction()
                .add(R.id.trail_detail_container, fragment)
                .add(R.id.detail_map, mapFragment)
                .commit()
            mapFragment.getMapAsync(this)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        when (item.itemId) {
            android.R.id.home -> {
                // This ID represents the Home or Up button. In the case of this
                // activity, the Up button is shown. For
                // more details, see the Navigation pattern on Android Design:
                //
                // http://developer.android.com/design/patterns/navigation.html#up-vs-back

                navigateUpTo(Intent(this, TrailListActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    override fun setupToolbar() {
        toolbar_layout.title = ""
    }

    override fun onMapReady(p0: GoogleMap?) {
        googleMap = p0
        val lat = currTrail!!.coordinates!!.lat!!.toDouble()
        val long = currTrail!!.coordinates!!.long!!.toDouble()
        if (currTrail!!.pitches == "N/A") { // Add hike marker
            googleMap!!.addMarker(
                MarkerOptions()
                    .position(LatLng(lat, long))
                    .title(currTrail!!.name)
            )
        } else { // Add climb marker
            googleMap!!.addMarker(
                MarkerOptions()
                    .position(LatLng(lat, long))
                    .title(currTrail!!.name)
            )
        }

        googleMap!!.moveCamera(CameraUpdateFactory.newLatLng(LatLng(lat, long)))
        googleMap!!.moveCamera(CameraUpdateFactory.zoomTo(9F))
    }
}
