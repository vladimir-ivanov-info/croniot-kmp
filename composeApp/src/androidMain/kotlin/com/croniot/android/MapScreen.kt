package com.croniot.android

import android.content.Context
import android.graphics.Color
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.croniot.android.presentation.device.sensors.ViewModelSensors
import org.koin.java.KoinJavaComponent.get
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.geojson.Point
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection

//TOOD experimental

private val viewModelSensors: ViewModelSensors = get(ViewModelSensors::class.java)
lateinit var mapLibre: MapLibreMap
private val markersList = mutableListOf<MarkerOptions>()

@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    savedLocations: List<Point> = emptyList() // Use an empty list if you don't have locations yet
) {
    LaunchedEffect(Unit) {
        viewModelSensors.listenToMapUpdates()
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = rememberMapViewWithLifecycle(context, lifecycleOwner)

    // Load the map style and add saved locations
    LaunchedEffect(mapView) {
       // mapView.setRenderMode(MapView.RENDER_MODE_SOFTWARE)

        mapView.getMapAsync { map ->
            mapLibre = map
            map.setMaxZoomPreference(17.5) // Maximum zoom level to prevent black screen
            map.setStyle("asset://style.json") { style ->
               // addSavedLocationsToMap(style, savedLocations)

            }
            val newYorkCity = LatLng(40.7167, -74.0000) // Latitude and longitude of NYC
            val cameraPosition = CameraPosition.Builder()
                .target(newYorkCity) // Center map on New York City
                .zoom(17.5) // Adjust zoom level, try between 10 to 12 for city-level zoom. Proved: 14 is max
                .build()
            map.cameraPosition = cameraPosition
        }
    }

    val gps = viewModelSensors.gps.collectAsState()
    val gpsString = gps.value
    if(gpsString.isNotEmpty()){
        val dataStr = gpsString.split(",")

        val latitude = dataStr[0].toDouble()
        val  longitude = dataStr[1].toDouble()
        val dateTime = dataStr[2]
        val point = Point.fromLngLat(longitude, latitude)
        val data = Feature.fromGeometry(point)

        var fatureCollection = FeatureCollection.fromFeature(data)

        if(mapLibre != null){
            addMarkersToMap(fatureCollection, mapLibre, context, dateTime)
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier
    )
}

@Composable
fun rememberMapViewWithLifecycle(
    context: Context,
    lifecycleOwner: LifecycleOwner
): MapView {
    val mapView = remember {
        MapView(context)
    }

    DisposableEffect(lifecycleOwner) {
        val lifecycle = lifecycleOwner.lifecycle
        val observer = object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                mapView.onStart()
            }

            override fun onStart(owner: LifecycleOwner) {
                mapView.onStart()
            }

            override fun onResume(owner: LifecycleOwner) {
                mapView.onResume()
            }

            override fun onPause(owner: LifecycleOwner) {
                mapView.onPause()
            }

            override fun onStop(owner: LifecycleOwner) {
                mapView.onStop()
            }

            override fun onDestroy(owner: LifecycleOwner) {
                mapView.onDestroy()
            }
        }

        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }
    return mapView
}

private fun addMarkersToMap(data: FeatureCollection, maplibreMap: MapLibreMap, context: Context, message: String) {
    val bounds = mutableListOf<LatLng>()

    // Get bitmaps for marker icon
    val infoIconDrawable = ResourcesCompat.getDrawable(
        context.resources,
        // Intentionally specify package name
        // This makes copy from another project easier
        R.drawable.ic_map_marker,
        context.theme
    )!!

    val bitmapBlue = infoIconDrawable.toBitmap()
    val bitmapRed = infoIconDrawable
        .mutate()
        .apply { setTint(Color.RED) }
        .toBitmap()

    // Add symbol for each point feature
    data.features()?.forEach { feature ->
        val geometry = feature.geometry()?.toJson() ?: return@forEach
        val point = Point.fromJson(geometry) ?: return@forEach
        val latLng = LatLng(point.latitude(), point.longitude())
        bounds.add(latLng)

        // Contents in InfoWindow of each marker
        val title = feature.getStringProperty("title")
        val epochTime = feature.getNumberProperty("time")
        //val dateString = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(epochTime)
       // val dateString = "abc"
        val dateString = message

        // If magnitude > 6.0, show marker with red icon. If not, show blue icon instead
        val mag = feature.getNumberProperty("mag")
        //val icon = IconFactory.getInstance(context)
        //    .fromBitmap(if (mag.toFloat() > 6.0) bitmapRed else bitmapBlue)

        val icon = IconFactory.getInstance(context).fromBitmap(bitmapBlue)

        // Use MarkerOptions and addMarker() to add a new marker in map
        val markerOptions = MarkerOptions()
            .position(latLng)
            .title(dateString)
            .snippet(title)

            .icon(icon)
        maplibreMap.addMarker(markerOptions)

        markersList.add(markerOptions)
        if (markersList.size > 50) {
            // Remove the oldest marker from the map
            val oldestMarker = markersList.removeAt(0)
            maplibreMap.removeMarker(oldestMarker.marker)
        }
    }

    // Move camera to newly added annotations
   /* maplibreMap.getCameraForLatLngBounds(LatLngBounds.fromLatLngs(bounds))?.let {
        val newCameraPosition = CameraPosition.Builder()
            .target(it.target)
           // .zoom(it.zoom - 0.5)
            .zoom(it.zoom - 0.5)
            .build()
        maplibreMap.cameraPosition = newCameraPosition
    }*/
}