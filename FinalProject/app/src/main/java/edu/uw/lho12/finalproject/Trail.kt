package edu.uw.lho12.finalproject

import android.os.Parcelable
import android.util.Log
import kotlinx.android.parcel.Parcelize
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

@Parcelize
data class Trail(
    val name: String?,
    val summary: String?,
    val difficulty: String?,
    val location: String?,
    val length: String?,
    val elevation: String?,
    val rating: String?,
    val coordinates: LatLong?,
    val imageUrl: String?,
    val pitches: String?,
    val type: String?,
    val url: String?
) : Parcelable

@Parcelize
data class LatLong(
    var lat: String?,
    var long: String?
) : Parcelable

const val TRAIL_TAG = "Trail"

private var jsonTrails: JSONArray? = null

fun parseTrailAPI(response: JSONObject, apiType: String, difficultyLevel: DifficultyLevel): List<Trail> {

    var allTrails = mutableListOf<Trail>()

    try {

        if (apiType == "hike") {
            jsonTrails = response.getJSONArray("trails")

            for (i in 0 until jsonTrails!!.length()) {

                val currentTrail = jsonTrails!!.getJSONObject(i)

                val singleTrail = Trail(
                    name = currentTrail.getString("name"),
                    summary = currentTrail.getString("summary"),
                    difficulty = changeDifficultyName(currentTrail.getString("difficulty")),
                    location = currentTrail.getString("location"),
                    length = currentTrail.getString("length"),
                    elevation = currentTrail.getString("ascent"),
                    rating = currentTrail.getString("stars"),
                    coordinates = LatLong(
                        lat = currentTrail.getString("latitude"),
                        long = currentTrail.getString("longitude")
                    ),
                    imageUrl = currentTrail.getString("imgSmallMed"),
                    pitches = "N/A",
                    type = "N/A",
                    url = currentTrail.getString("url")
                )
                allTrails.add(singleTrail)
            }

            // Return all the results if the user did not specify a difficulty level
            if (!difficultyLevel.easy && !difficultyLevel.medium && !difficultyLevel.hard) {
                return allTrails
            } else {
                // Otherwise filter results to only display trails based on the difficulty level indicated
                val levelList = mutableListOf<Trail>()

                if (difficultyLevel.easy) {
                    val easyList = allTrails.filter {
                        (it as Trail).difficulty == "Easy"
                    } as MutableList<Trail>
                    levelList.addAll(easyList)
                }

                if (difficultyLevel.medium) {
                    val mediumList = allTrails.filter {
                        (it as Trail).difficulty == "Medium"
                    } as MutableList<Trail>
                    levelList.addAll(mediumList)
                }

                if (difficultyLevel.hard) {
                    val hardList = allTrails.filter {
                        (it as Trail).difficulty == "Hard"
                    } as MutableList<Trail>
                    levelList.addAll(hardList)
                }
                allTrails = levelList
            }

        } else {
            jsonTrails = response.getJSONArray("routes")

            for (i in 0 until jsonTrails!!.length()) {

                val currentTrail = jsonTrails!!.getJSONObject(i)

                val singleTrail = Trail(
                    name = currentTrail.getString("name"),
                    summary = "N/A",
                    difficulty = "N/A",
                    location = currentTrail.getJSONArray("location")[1].toString() + ", " + currentTrail.getJSONArray("location")[0].toString(),
                    length = "N/A",
                    elevation = "N/A",
                    rating = currentTrail.getString("stars"),
                    coordinates = LatLong(
                        lat = currentTrail.getString("latitude"),
                        long = currentTrail.getString("longitude")
                    ),
                    imageUrl = currentTrail.getString("imgSmallMed"),
                    pitches = currentTrail.getString("pitches"),
                    type = currentTrail.getString("type"),
                    url = currentTrail.getString("url")
                )

                allTrails.add(singleTrail)
            }

        }
    } catch (e: JSONException) {
        Log.e(TRAIL_TAG, "Error parsing json", e)
    }
    return allTrails
}

// Decodes the difficulty level names to make it easier to understand
private fun changeDifficultyName(apiCodedName: String): String {
    if (apiCodedName == "green" || apiCodedName == "greenBlue") {
        return "Easy"
    } else if (apiCodedName == "blue" || apiCodedName == "blueBlack") {
        return "Medium"
    } else {
        return "Hard"
    }
}