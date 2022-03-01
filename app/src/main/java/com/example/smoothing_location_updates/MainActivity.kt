package com.example.smoothing_location_updates

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import android.R
import android.animation.ValueAnimator
import android.animation.TypeEvaluator

import android.graphics.BitmapFactory

import android.graphics.Bitmap
import android.os.Handler
import android.util.Log
import com.mapbox.geojson.Feature
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.extension.style.image.image
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.style
import com.mapbox.maps.plugin.animation.easeTo
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.gestures.removeOnMapClickListener
import java.lang.Exception

/*
* Smoothing puck movement of jumpy location updates
* Location Queue
* Location-jump-distance dependent animation duration
* Optional: use Mapmatching to ensure movement along road network
 */



class MainActivity : AppCompatActivity(), OnMapClickListener {

    // map
    private lateinit var mapView: MapView
    private lateinit var mapboxMap: MapboxMap

    // animation
    private lateinit var geojsonSource: GeoJsonSource
    private var currentPoint = Point.fromLngLat(-117.17282, 32.71204)
    private var animator: ValueAnimator? = null
    private var asset_speed_fast = 100
    private var asset_speed_normal = 60

    // location queue
    private lateinit var location_lon_queue: MutableList<Double>
    private lateinit var location_lat_queue: MutableList<Double>
    private var postDelayed_counter = 0


    fun init_location_queue(){  // init for location buffer queue
        location_lon_queue = mutableListOf()
        location_lat_queue = mutableListOf()
    }

    fun push_into_buffer(point: Point){
        // add location update to end of buffer queue
        location_lon_queue.add(point.longitude())
        location_lat_queue.add(point.latitude())
    }


    fun start_update_handler(){
        // Update handler to iterate through location array and push location updates to the location puck
        hupdate_buffer = Handler()

    }

    private var hupdate_buffer: Handler? = null
    private val rUpdate_buffer: Runnable = object : Runnable {
        override fun run() {

            //push a location update
            push_location_update(Point.fromLngLat(location_lon_queue[0], location_lat_queue[0]), true)
            // eliminate element from buffer queue
            location_lon_queue.removeAt(0)
            location_lat_queue.removeAt(0)
            postDelayed_counter -= 1 // reduce counter so new postdelayed can be scheduled

        }
    }

    fun push_location_update(point: Point, buffer_event: Boolean){

        var remaining_duration = 0L
        try {
            // check current animation status. If not close to end, push location update into buffer queue
            remaining_duration = animator!!.duration - animator!!.currentPlayTime
        }catch (e: Exception){
            Log.e("MainActivity animator duration: ", e.message.toString())
        }


        if(buffer_event){
            var animation_duration = animate_puck(point)
            if(location_lat_queue.size > 1) {
                hupdate_buffer!!.postDelayed(rUpdate_buffer, animation_duration)
                postDelayed_counter += 1
            }
        }else{
            // Also check if there is a buffer event in the queue. if yes, always write to buffer
            if( (remaining_duration > 200 && animator!!.currentPlayTime > 0) || location_lat_queue.size > 0){
                // Push into buffer queue and set next handler update event to remaining duration
                push_into_buffer(point)
                if(postDelayed_counter == 0) {
                    if(remaining_duration < 0){ // to ensure the location is played even if there was something taking longer
                        remaining_duration = 100
                    }
                    hupdate_buffer!!.postDelayed(rUpdate_buffer, remaining_duration)
                    postDelayed_counter += 1
                }

            }else{
                animate_puck(point)
            }
        }
    }

    fun animate_puck(point: Point): Long{
        // Compute animation duration to keep constant speed
        var speed = 0
        if(location_lat_queue.size > 0){
            // puck animation is behind location updates: speed up puck, but still stay realistic
            speed = asset_speed_fast  
        }else{
            // use actual speed of driver, or use realistic speed value
            speed = asset_speed_normal
        }


        var distance = distance_in_meter(currentPoint.latitude(), currentPoint.longitude(),
            point.latitude(), point.longitude())
        var animation_duration = distance/speed * 1000  // in ms

        // When the user clicks on the map, we want to animate the marker to that location.
        animator?.let {
            if (it.isStarted) {
                currentPoint = it.animatedValue as Point
                it.cancel()
            }
        }

        val pointEvaluator = TypeEvaluator<Point> { fraction, startValue, endValue ->
            Point.fromLngLat(
                startValue.longitude() + fraction * (endValue.longitude() - startValue.longitude()),
                startValue.latitude() + fraction * (endValue.latitude() - startValue.latitude())
            )
        }
        animator = ValueAnimator().apply {
            setObjectValues(currentPoint, point)
            setEvaluator(pointEvaluator)
            addUpdateListener {
                geojsonSource.geometry(it.animatedValue as Point)
            }
            duration = animation_duration.toLong()
            start()
        }

        currentPoint = point
        return animation_duration.toLong()
    }



    override fun onMapClick(point: Point): Boolean {
        push_location_update(point, false)
        return true
    }

    fun distance_in_meter(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val radius = 6370000
        val lat = Math.toRadians(lat2 - lat1)
        val lon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(lat / 2) * Math.sin(lat / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(
            Math.toRadians(lat2)
        ) * Math.sin(lon / 2) * Math.sin(lon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        var d = 0.0
        d = radius * c
        return Math.abs(d)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.example.smoothing_location_updates.R
            .layout.activity_main)

        mapView = findViewById(com.mapbox.maps.R.id.mapView)
        mapboxMap = mapView.getMapboxMap()

        geojsonSource = geoJsonSource("source-id") {
            feature(Feature.fromGeometry(currentPoint))
        }

        mapboxMap.loadStyle(
            style(Style.SATELLITE_STREETS) {
                +image("marker_icon") {
                    bitmap(BitmapFactory.decodeResource(resources, com.example.smoothing_location_updates.R.drawable.red_marker))
                }
                +geojsonSource
                +symbolLayer(layerId = "layer-id", sourceId = "source-id") {
                    iconImage("marker_icon")
                    iconIgnorePlacement(true)
                    iconAllowOverlap(true)
                }
            })
        mapboxMap.addOnStyleLoadedListener{
            val cameraOptions = CameraOptions.Builder()
                .center(currentPoint)
                .zoom(13.0)
                .pitch(45.0)
                .build()
            mapboxMap.easeTo(cameraOptions, null)
            // Style loaded:

            // init location buffer queue
            init_location_queue()
            // register onMapClickListener
            mapboxMap.addOnMapClickListener(this@MainActivity)
            // start update handler to simualte loac
            start_update_handler()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        animator?.cancel()
        mapboxMap.removeOnMapClickListener(this)
        try {
            hupdate_buffer!!.removeCallbacks(rUpdate_buffer)

        }catch (e: Exception){
            Log.e("MainActivity", "Error killing handlers" + e.message)
        }

    }



}