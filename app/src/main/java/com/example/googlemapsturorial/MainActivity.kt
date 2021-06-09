package com.example.googlemapsturorial

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class MainActivity : AppCompatActivity(), OnMapReadyCallback, LocationListener {
    private lateinit var mMap: GoogleMap
    private var locationManager: LocationManager? = null
    private val tagInfo = "MainActivity"
    private var currentPositionMarker: Marker? = null
    private val permissionArray = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    private var pegmanImgResource: Int = 0
    private var locationArray: ArrayList<LatLng> = ArrayList()
    private var lineArray: ArrayList<Polyline> = ArrayList()
    private var speedArray: ArrayList<String> = ArrayList()
    private var here: LatLng = LatLng(-34.0, 151.0)
    private lateinit var speedText: TextView
    private lateinit var distanceText: TextView
    private var roadDistance = 0f
    var filepath: String =
        Environment.getExternalStorageDirectory().toString() + "/file.txt"

    fun createfile() {
        var fileContent = "location\t\tspeed\n"
        for (i in 0..speedArray.size - 1) {
            fileContent += locationArray[i].toString() + "\t" + speedArray[i] + "\n"
        }
        var myExternalFile = File(getExternalFilesDir(filepath), "fileName.txt")
        try {
            val fileOutPutStream = FileOutputStream(myExternalFile)
            fileOutPutStream.write(fileContent.toByteArray())
            fileOutPutStream.close()
            Log.d(tagInfo, myExternalFile.path + " created")
            Toast.makeText(this@MainActivity, myExternalFile.path + " created", Toast.LENGTH_LONG)
                .show()
        } catch (e: IOException) {
            Log.d(tagInfo, e.toString())
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pegmanImgResource = resources.getIdentifier("@drawable/pegman", null, this.packageName)
        distanceText = findViewById(R.id.text_distance)
        distanceText.text = "$roadDistance\nKM"
        speedText = findViewById(R.id.text_speed)
        speedText.text = getString(R.string.speed_text).format(0.0f)
        val myButton: Button = findViewById(R.id.button1)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        //enable permissions
        requestPermissions(permissionArray, 1000)
        locationManagerEnabler()
        myButton.setOnClickListener {
            //zapisz do pliku
            if (locationArray.size > 0)
                createfile()
            else
                Toast.makeText(this@MainActivity, "Empty", Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun locationManagerEnabler() {
        locationManager = this.getSystemService(LOCATION_SERVICE) as LocationManager
        if (locationManager != null) {
            Log.d(tagInfo, "\nUpdating Location...")
            locationManager!!.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, this)
        } else Log.d(tagInfo, "\nlocation manager not working ")
    }

    override fun onLocationChanged(location: Location) {
        if (location != null) {
            val before = here
            here = LatLng(location.latitude, location.longitude)
            if (currentPositionMarker != null) {
                val distance = FloatArray(1)
                Location.distanceBetween(
                    here.latitude,
                    here.longitude,
                    before.latitude,
                    before.longitude,
                    distance
                )
                if (currentPositionMarker!!.position != here && distance[0] > 0.5) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(here))
                    roadDistance += distance[0] / 1000
                    val tempDistance = (roadDistance * 1000).toInt().toFloat() / 1000
                    distanceText.text = ("$tempDistance\nKM")
                    locationArray.add(here)
                    lineArray.add(
                        mMap.addPolyline(
                            PolylineOptions()
                                .color(Color.RED)
                                .add(
                                    locationArray[locationArray.size - 1],
                                    locationArray[locationArray.size - 2]
                                )
                        )
                    )
                    currentPositionMarker!!.position = here

                    location.speed *= 3.6f
                    val tempSpeed = getString(R.string.speed_text).format(location.speed)
                    speedText.text = tempSpeed
                    speedArray.add(tempSpeed.replace("\n", ""))
                }
            } else {
                currentPositionMarker = mMap.addMarker(
                    MarkerOptions()
                        .position(here).title("I'm here")
                        .icon(BitmapDescriptorFactory.fromResource(pegmanImgResource))
                )
                locationArray.add(here)
            }
        } else
            Log.d(tagInfo, "Location Not Found")
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.mapType = GoogleMap.MAP_TYPE_HYBRID
        if (checkSelfPermission(permissionArray[0]) != PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
            mMap.isBuildingsEnabled = true
        }
        with(mMap.uiSettings) {
            isZoomControlsEnabled = true
            isCompassEnabled = true
            isMyLocationButtonEnabled = true
            isTiltGesturesEnabled = true
        }
    }
}