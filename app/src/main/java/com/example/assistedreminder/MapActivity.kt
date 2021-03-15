package com.example.assistedreminder

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlin.random.Random

const val GEOFENCE_RADIUS = 200
const val GEOFENCE_ID = "REMINDER_GEOFENCE_ID"
const val GEOFENCE_EXPIRATION = 10 * 24 * 60 * 60 * 1000 // 10 days
const val GEOFENCE_DWELL_DELAY = 10 * 1000 // 10 secs // 2 minutes
const val GEOFENCE_LOCATION_REQUEST_CODE = 12345
const val CAMERA_ZOOM_LEVEL = 20f
const val LOCATION_REQUEST_CODE = 123
private val TAG: String = MapActivity::class.java.simpleName

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var geofencingClient: GeofencingClient
    private var marker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.set_location_activity)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        geofencingClient = LocationServices.getGeofencingClient(this)

        findViewById<Button>(R.id.setLocationButton).setOnClickListener {
            if (marker != null) {

                // send location to previous layout
                val intent = Intent()
                intent.putExtra("location", "${marker?.position?.latitude.toString()} ${marker?.position?.longitude.toString()}")
                setResult(Activity.RESULT_OK, intent)
                finish()

            } else {
                Toast.makeText(
                    this,
                    "Choose location",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = true

        if (!isLocationPermissionGranted()) {
            val permissions = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
            ActivityCompat.requestPermissions(
                this,
                permissions.toTypedArray(),
                LOCATION_REQUEST_CODE
            )
        } else {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            this.map.isMyLocationEnabled = true

            // Zoom to last known location
            fusedLocationClient.lastLocation.addOnSuccessListener {
                if (it != null) {
                    with(map) {
                        val latLng = LatLng(it.latitude, it.longitude)
                        moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, CAMERA_ZOOM_LEVEL))
                    }
                } else {
                    with(map) {
                        moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(65.01355297927051, 25.464019811372978),
                                CAMERA_ZOOM_LEVEL
                            )
                        )
                    }
                }
            }
        }

        // init point click listeners
        map.setOnPoiClickListener { point -> setMapLocation(map, point.latLng, point.name) }
        map.setOnMapLongClickListener { latlng -> setMapLocation(map, latlng) }
    }

    // set location to the activity variable marker
    private fun setMapLocation(
        map: GoogleMap,
        latlng: LatLng,
        title: String = "Selected location"
    ) {
        if (marker != null) {
            marker?.position = latlng
            marker?.title = title
        } else {
            marker = map.addMarker(
                MarkerOptions()
                    .position(latlng)
                    .title(title)
            )
            marker?.showInfoWindow()
        }
    }

    private fun isLocationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            applicationContext, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == GEOFENCE_LOCATION_REQUEST_CODE) {
            if (permissions.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    this,
                    "This application needs background location to work on Android 10 and higher",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (
                grantResults.isNotEmpty() && (
                        grantResults[0] == PackageManager.PERMISSION_GRANTED ||
                                grantResults[1] == PackageManager.PERMISSION_GRANTED)
            ) {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
                map.isMyLocationEnabled = true
                onMapReady(map)
            } else {
                Toast.makeText(
                    this,
                    "The app needs location permission to function",
                    Toast.LENGTH_LONG
                ).show()
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (grantResults.isNotEmpty() && grantResults[2] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(
                        this,
                        "This application needs background location to work on Android 10 and higher",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}