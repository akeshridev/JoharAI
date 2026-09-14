package com.akeshridev.johar.map

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.akeshridev.johar.data.spatial.RanchiCoordinate
import com.akeshridev.johar.data.spatial.RanchiSpatialPlace
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import java.io.File

/**
 * Rich answer card for Johar chat. The base map is fully local; route is optional and must come
 * from a routing engine rather than being guessed from straight-line geometry.
 */
@Composable
fun RanchiMapCard(
    destination: RanchiSpatialPlace,
    modifier: Modifier = Modifier,
    origin: RanchiCoordinate? = null,
    route: List<RanchiCoordinate> = emptyList(),
) {
    val context = LocalContext.current
    val mapFile = remember { RanchiMapPackStore(context).ensureInstalled() }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(destination.name)
            Text(destination.type.replace('_', ' ').lowercase())

            if (mapFile == null) {
                Text("Ranchi offline map pack is not installed in this build.")
            } else {
                OfflineMap(
                    mapFile = mapFile,
                    destination = destination.coordinate,
                    origin = origin,
                    route = route,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { openNavigation(context, destination.coordinate) }) {
                    Text("Navigate")
                }
            }
            Text("© OpenStreetMap contributors")
        }
    }
}

@Composable
private fun OfflineMap(
    mapFile: File,
    destination: RanchiCoordinate,
    origin: RanchiCoordinate?,
    route: List<RanchiCoordinate>,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember {
        MapLibre.getInstance(context.applicationContext)
        MapView(context).apply { onCreate(null) }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    DisposableEffect(mapFile.absolutePath, destination, origin, route) {
        mapView.getMapAsync { map ->
            map.setStyle(Style.Builder().fromJson(styleJson(mapFile))) { style ->
                val pinCoordinates = buildList {
                    origin?.let(::add)
                    add(destination)
                }
                val pinFeatures = pinCoordinates.map { coordinate ->
                    Feature.fromGeometry(Point.fromLngLat(coordinate.longitude, coordinate.latitude))
                }
                style.addSource(
                    GeoJsonSource(
                        PIN_SOURCE,
                        FeatureCollection.fromFeatures(pinFeatures),
                    ),
                )
                style.addLayer(
                    CircleLayer(PIN_LAYER, PIN_SOURCE).withProperties(
                        circleColor(Color.rgb(35, 99, 235)),
                        circleRadius(7f),
                    ),
                )

                if (route.size >= 2) {
                    val line = LineString.fromLngLats(
                        route.map { Point.fromLngLat(it.longitude, it.latitude) },
                    )
                    style.addSource(GeoJsonSource(ROUTE_SOURCE, Feature.fromGeometry(line)))
                    style.addLayer(
                        LineLayer(ROUTE_LAYER, ROUTE_SOURCE).withProperties(
                            lineColor(Color.rgb(22, 101, 52)),
                            lineWidth(5f),
                        ),
                    )
                }

                val cameraPoints = buildList {
                    origin?.let { add(LatLng(it.latitude, it.longitude)) }
                    route.forEach { add(LatLng(it.latitude, it.longitude)) }
                    add(LatLng(destination.latitude, destination.longitude))
                }.distinct()

                if (cameraPoints.size > 1) {
                    val bounds = LatLngBounds.Builder().includes(cameraPoints).build()
                    map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 64))
                } else {
                    map.cameraPosition = CameraPosition.Builder()
                        .target(cameraPoints.first())
                        .zoom(13.5)
                        .build()
                }
            }
        }
        onDispose { }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
    )
}

private fun styleJson(mapFile: File): String {
    val source = "pmtiles://file://${mapFile.absolutePath}"
    return """
        {
          "version": 8,
          "name": "Johar Ranchi Offline",
          "sources": {
            "ranchi": { "type": "vector", "url": "$source" }
          },
          "layers": [
            { "id": "background", "type": "background", "paint": { "background-color": "#f6f4ed" } },
            { "id": "landcover", "type": "fill", "source": "ranchi", "source-layer": "landcover", "paint": { "fill-color": "#e7eadf", "fill-opacity": 0.7 } },
            { "id": "landuse", "type": "fill", "source": "ranchi", "source-layer": "landuse", "paint": { "fill-color": "#eeeeea", "fill-opacity": 0.5 } },
            { "id": "water", "type": "fill", "source": "ranchi", "source-layer": "water", "paint": { "fill-color": "#cfe8f3" } },
            { "id": "buildings", "type": "fill", "source": "ranchi", "source-layer": "buildings", "paint": { "fill-color": "#dedbd2", "fill-opacity": 0.65 } },
            { "id": "roads", "type": "line", "source": "ranchi", "source-layer": "roads", "paint": { "line-color": "#b0aaa0", "line-width": 1.2 } }
          ]
        }
    """.trimIndent()
}

private fun openNavigation(context: android.content.Context, destination: RanchiCoordinate) {
    val googleNavigation = Uri.parse(
        "google.navigation:q=${destination.latitude},${destination.longitude}",
    )
    val navigationIntent = Intent(Intent.ACTION_VIEW, googleNavigation)
    if (navigationIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(navigationIntent)
        return
    }

    val geo = Uri.parse("geo:${destination.latitude},${destination.longitude}?q=${destination.latitude},${destination.longitude}")
    context.startActivity(Intent(Intent.ACTION_VIEW, geo))
}

private const val PIN_SOURCE = "johar-pins"
private const val PIN_LAYER = "johar-pins-layer"
private const val ROUTE_SOURCE = "johar-route"
private const val ROUTE_LAYER = "johar-route-layer"
