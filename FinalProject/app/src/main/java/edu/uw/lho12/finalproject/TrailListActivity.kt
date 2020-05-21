package edu.uw.lho12.finalproject

import android.app.DownloadManager
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.NetworkImageView
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.android.PolyUtil
import edu.uw.lho12.finalproject.APICaller.Caller.imageLoader

import edu.uw.lho12.finalproject.dummy.DummyContent
import kotlinx.android.synthetic.main.activity_trail_list.*
import kotlinx.android.synthetic.main.trail_list_content.view.*
import kotlinx.android.synthetic.main.trail_list.*
import org.json.JSONObject
import org.w3c.dom.Text
import java.util.ArrayList

/**
 * An activity representing a list of Pings. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a [TrailDetailActivity] representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
class TrailListActivity : AppCompatActivity(), OnMapReadyCallback {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private var twoPane: Boolean = false
    private var trailList = listOf<Trail>()
    private var location: LatLong? = null
    private var googleMap: GoogleMap? = null
    private var currTrail: Trail? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trail_list)

        trailList = intent.getParcelableArrayListExtra("RESULTS")
        location = intent.getParcelableExtra("LOCATION")

        setSupportActionBar(toolbar)
        toolbar.title = title

        fab.setOnClickListener { view ->
            val intent = Intent(this, MapsActivity::class.java).apply {
                val listResults = trailList
                putParcelableArrayListExtra("RESULTS", listResults as ArrayList<out Parcelable>)
                putExtra("LOCATION", location)
            }
            startActivity(intent)

        }

        if (trail_detail_container != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            twoPane = true
        }

        setupRecyclerView(trail_list)
    }

    private fun setupRecyclerView(recyclerView: RecyclerView) {
        if (!twoPane) {
            recyclerView.layoutManager = GridLayoutManager(this, 2)
        }
        recyclerView.adapter = SimpleItemRecyclerViewAdapter(this, trailList, twoPane)
    }

    inner class SimpleItemRecyclerViewAdapter(
        private val parentActivity: TrailListActivity,
        private val values: List<Trail>,
        private val twoPane: Boolean
    ) :
        RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder>() {

        private val onClickListener: View.OnClickListener

        init {
            onClickListener = View.OnClickListener { v ->
                val trail = v.tag as Trail
                currTrail = trail
                if (twoPane) {
                    val fragment = TrailDetailFragment.newInstance(trail)
                    val mapFragment = SupportMapFragment.newInstance()
                    parentActivity.supportFragmentManager.beginTransaction().run {
                        replace(R.id.trail_detail_container, mapFragment)
                        addToBackStack(null)
                        commit()
                    }
                    mapFragment.getMapAsync(parentActivity)
                    /*parentActivity.supportFragmentManager
                        .beginTransaction().run {
                            replace(R.id.trail_detail_container, fragment)
                            addToBackStack(null)
                            commit()
                        }*/

                } else {
                    val intent = Intent(v.context, TrailDetailActivity::class.java).apply {
                        putExtra(TrailDetailFragment.ARG_ITEM_ID, trail)
                    }
                    v.context.startActivity(intent)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.trail_list_content, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = values[position]
            holder.trailImage.setImageUrl(item.imageUrl, imageLoader)
            holder.trailName.text = item.name
            holder.location.text = "Location: " + item.location
            if (item.length != null) {
                holder.length.text = "Length: " + item.length + " mi"
            }
            if (item.difficulty != null) {
                holder.difficulty.text = "Difficulty: " + item.difficulty
            }

            with(holder.itemView) {
                tag = item
                setOnClickListener(onClickListener)
            }
        }

        override fun getItemCount() = values.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val trailImage: NetworkImageView = view.trail_image
            val trailName: TextView = view.trail_name
            val location: TextView = view.location
            val length: TextView = view.length
            val difficulty: TextView = view.difficulty
        }
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

    override fun onMapReady(p0: GoogleMap?) {
        googleMap = p0
        val latLoc = location!!.lat!!.toDouble()
        val longLoc = location!!.long!!.toDouble()
        val lat = currTrail!!.coordinates!!.lat!!.toDouble()
        val long = currTrail!!.coordinates!!.long!!.toDouble()
        this.googleMap!!.addMarker(MarkerOptions().position(LatLng(latLoc, longLoc)).title("Current Location"))
        if (currTrail!!.pitches == "N/A") { // Add hike marker
            googleMap!!.addMarker(
                MarkerOptions()
                    .position(LatLng(lat, long))
                    .title(currTrail!!.name)
                    .snippet(currTrail!!.summary)
                //.icon(BitmapDescriptorFactory.fromResource(R.drawable.tree))
            )
        } else { // Add climb marker
            googleMap!!.addMarker(
                MarkerOptions()
                    .position(LatLng(lat, long))
                    .title(currTrail!!.name)
                //.icon(BitmapDescriptorFactory.fromResource(R.drawable.mountain))
            )
        }

        googleMap!!.moveCamera(CameraUpdateFactory.newLatLng(LatLng(lat, long)))
        googleMap!!.moveCamera(CameraUpdateFactory.zoomTo(9F))
        val origin = location!!.lat!! + "," + location!!.long!!
        val destination = currTrail!!.coordinates!!.lat!! + "," + currTrail!!.coordinates!!.long!!
        val path: MutableList<List<LatLng>> = ArrayList()
        val key = getResources().getString(R.string.google_routes_key);
        val builder = Uri.Builder()
        val urlDirections = builder.scheme("https")
            .authority("maps.googleapis.com")
            .appendPath("maps")
            .appendPath("api")
            .appendPath("directions")
            .appendPath("json")
            .appendQueryParameter("origin", origin)
            .appendQueryParameter("destination", destination)
            .appendQueryParameter("key",key)
            .build().toString()
        val directionsRequest = object : StringRequest(Request.Method.GET, urlDirections, Response.Listener<String> {
                response ->
            val jsonResponse = JSONObject(response)
            // Get routes
            val routes = jsonResponse.getJSONArray("routes")
            val legs = routes.getJSONObject(0).getJSONArray("legs")
            val steps = legs.getJSONObject(0).getJSONArray("steps")
            for (i in 0 until steps.length()) {
                val points = steps.getJSONObject(i).getJSONObject("polyline").getString("points")
                path.add(PolyUtil.decode(points))
            }
            for (i in 0 until path.size) {
                this.googleMap!!.addPolyline(PolylineOptions().addAll(path[i]).color(Color.RED))
            }
        }, Response.ErrorListener {
                _ ->
        }){}
        val requestQueue = Volley.newRequestQueue(this)
        requestQueue.add(directionsRequest)
    }
}