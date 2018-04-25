package com.pranay.lastknownlocation

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.android.gms.location.*
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import android.location.LocationManager
import android.R.string.cancel
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.support.v7.app.AlertDialog


/**
 * Created by Pranay on 7/16/2017.
 */
//https://developer.android.com/training/location/retrieve-current.html
class MainActivity : AppCompatActivity() {

    var TAG: String = "MainActivity"
    var FASTEST_INTERVAL: Long = 8 * 1000 // 8 SECOND
    var UPDATE_INTERVAL: Long = 2000 // 2 SECOND
    var FINE_LOCATION_REQUEST: Int = 888
    lateinit var toast: Toast

    lateinit var locationRequest: LocationRequest
    @SuppressLint("ShowToast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        toast = Toast.makeText(this, "", Toast.LENGTH_SHORT)
        if (checkPermissionAccessFineLocation()) {
            initLocationUpdate()
        }
        btnGetLocation.setOnClickListener {
            if (checkPermissionAccessFineLocation()) {
                initLocationUpdate()
            }
        }

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "GPS is Enabled in your devide", Toast.LENGTH_SHORT).show()
        } else {
            showGPSDisabledAlertToUser()
        }
    }

    private fun showGPSDisabledAlertToUser() {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setMessage("GPS está desligado no seu dispositivo, gostaria de habilitá-lo?")
                .setCancelable(false)
                .setPositiveButton("Ir para configurações e ativar o GPS",
                        DialogInterface.OnClickListener { dialog, id ->
                            val callGPSSettingIntent = Intent(
                                    android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                            startActivity(callGPSSettingIntent)
                        })
        alertDialogBuilder.setNegativeButton("Não, obrigado!",
                DialogInterface.OnClickListener { dialog, id -> dialog.cancel() })
        val alert = alertDialogBuilder.create()
        alert.show()
    }

    @SuppressLint("MissingPermission")
            //Start Location update as define intervals
    fun initLocationUpdate() {

        // Check API revision for New Location Update
        //https://developers.google.com/android/guides/releases#june_2017_-_version_110

        //init location request to start retrieving location update
        locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = UPDATE_INTERVAL
        locationRequest.fastestInterval = FASTEST_INTERVAL

        //Create LocationSettingRequest object using locationRequest
        val locationSettingBuilder: LocationSettingsRequest.Builder = LocationSettingsRequest.Builder()
        locationSettingBuilder.addLocationRequest(locationRequest)
        val locationSetting: LocationSettingsRequest = locationSettingBuilder.build()

        //Need to check whether location settings are satisfied
        val settingsClient: SettingsClient = LocationServices.getSettingsClient(this)
        settingsClient.checkLocationSettings(locationSetting)
        //More info :  // https://developers.google.com/android/reference/com/google/android/gms/location/SettingsClient


        // new Google API SDK v11 uses getFusedLocationProviderClient(this)
        val fusedLocationProviderClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                //super.onLocationResult(p0)
                if (locationResult != null) {
                    onLocationChanged(locationResult.lastLocation)
                }
            }

            override fun onLocationAvailability(p0: LocationAvailability?) {
                super.onLocationAvailability(p0)
            }
        },
                Looper.myLooper())

    }

    fun onLocationChanged(location: Location) {
        // New location has now been determined
        val msg = "Updated Location: " +
                java.lang.Double.toString(location.latitude) + "," +
                java.lang.Double.toString(location.longitude)
        tvLocationDetails.text = msg

        toast.setText(msg)
        toast.show()
    }

    @SuppressLint("MissingPermission")
            //For get Last know location
    fun getLatKnowLocation() {
        // Get last known recent location using new Google Play Services SDK (v11+)
        val locationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        locationClient.lastLocation
                .addOnSuccessListener(OnSuccessListener<Location> { location ->
                    // GPS location can be null if GPS is switched off
                    if (location != null) {
                        onLocationChanged(location)
                    }
                })
                .addOnFailureListener(OnFailureListener { e ->
                    Log.d("MainActivity", "Error trying to get last GPS location")
                    e.printStackTrace()
                })
    }

    private fun checkPermissionAccessFineLocation(): Boolean {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Already had permission for ACCESS_FINE_LOCATION")
            return true
        } else {
            Log.i(TAG, "Has NO permission for ACCESS_FINE_LOCATION")
            requestPermissions()
            return false
        }
    }

    private fun checkPermissionAccessCoarseLocation(): Boolean {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Already had permission for ACCESS_FINE_LOCATION")
            return true
        } else {
            Log.i(TAG, "Has NO permission for ACCESS_FINE_LOCATION")
            requestPermissions()
            return false
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                FINE_LOCATION_REQUEST)
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == FINE_LOCATION_REQUEST) {
            // Received permission result for Location permission.
            Log.i(TAG, "Received response for Location permission request.")

            // Check if the only required permission has been granted
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Camera permission has been granted, preview can be displayed
                Log.i(TAG, "Location permission has now been granted. Now call initLocationUpdate")
                initLocationUpdate()
            } else {
                Snackbar.make(mainLayout, R.string.rational_location_permission,
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction(getString(R.string.ok), object : View.OnClickListener {
                            override fun onClick(p0: View?) {
                                requestPermissions()
                            }
                        })
                        .show()

            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onStop() {
        if (toast != null) {
            toast.cancel()
        }
        super.onStop()
    }

    override fun onPause() {
        if (toast != null) {
            toast.cancel()
        }
        super.onPause()
    }
}
