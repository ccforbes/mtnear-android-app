package edu.uw.lho12.finalproject

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import edu.uw.lho12.finalproject.dummy.DummyContent
import kotlinx.android.synthetic.main.activity_trail_detail.*
import kotlinx.android.synthetic.main.trail_detail.view.*

/**
 * A fragment representing a single Trail detail screen.
 * This fragment is either contained in a [TrailListActivity]
 * in two-pane mode (on tablets) or a [TrailDetailActivity]
 * on handsets.
 */
class TrailDetailFragment : Fragment() {

    interface HasCollapsibleToolbar {
        fun setupToolbar()
    }

    /**
     * The dummy content this fragment is presenting.
     */
    private var item: Trail? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            if (it.containsKey(ARG_ITEM_ID)) {
                // Load the dummy content specified by the fragment
                // arguments. In a real-world scenario, use a Loader
                // to load content from a content provider.
                item = it.getParcelable(ARG_ITEM_ID)
            }
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.trail_detail, container, false)

        // Show the dummy content as text in a TextView.
        item?.let {
            rootView.detail_trail_name.text = it.name
            rootView.detail_location.text = "Location: " + it.location
            rootView.detail_length.text = "Length: " + it.length + " mi"
            rootView.detail_elevation.text = "Elevation: " + it.elevation + "ft"
            rootView.detail_difficulty.text = "Difficulty: " + it.difficulty
            rootView.rating.text = "Rating: " + it.rating + "/5"
            rootView.description.text = "Description: " + it.summary
        }

        return rootView
    }

    companion object {
        /**
         * The fragment argument representing the item ID that this fragment
         * represents.
         */
        const val ARG_ITEM_ID = "item_id"
        fun newInstance(argument: Trail) =
            TrailDetailFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_ITEM_ID, argument)
                }
            }
    }
}
