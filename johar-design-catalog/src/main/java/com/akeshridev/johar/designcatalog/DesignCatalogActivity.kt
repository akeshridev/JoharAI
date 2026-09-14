package com.akeshridev.johar.designcatalog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.akeshridev.johar.designsystem.JoharCardAction
import com.akeshridev.johar.designsystem.JoharComparisonCard
import com.akeshridev.johar.designsystem.JoharComparisonItem
import com.akeshridev.johar.designsystem.JoharConstraintSummary
import com.akeshridev.johar.designsystem.JoharInfoCard
import com.akeshridev.johar.designsystem.JoharInfoTone
import com.akeshridev.johar.designsystem.JoharItineraryCard
import com.akeshridev.johar.designsystem.JoharItineraryCardModel
import com.akeshridev.johar.designsystem.JoharItineraryStop
import com.akeshridev.johar.designsystem.JoharLocalPickCard
import com.akeshridev.johar.designsystem.JoharMapPreviewCard
import com.akeshridev.johar.designsystem.JoharPlaceCard
import com.akeshridev.johar.designsystem.JoharPlaceCardModel
import com.akeshridev.johar.designsystem.JoharPlaceCarousel
import com.akeshridev.johar.designsystem.JoharPreferenceChips
import com.akeshridev.johar.designsystem.JoharRouteCard
import com.akeshridev.johar.designsystem.JoharRouteCardModel
import com.akeshridev.johar.designsystem.JoharRouteStop
import com.akeshridev.johar.designsystem.JoharSourceRow
import com.akeshridev.johar.designsystem.JoharSuggestionCard
import com.akeshridev.johar.designsystem.JoharTheme
import com.akeshridev.johar.designsystem.JoharUtilityCard

class DesignCatalogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JoharTheme {
                DesignCatalogScreen()
            }
        }
    }
}

@Composable
private fun DesignCatalogScreen() {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text("Johar Design Catalog", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Mock Ranchi data • Design-system playground",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                )
            }

            CatalogSection("Place card") { JoharPlaceCard(model = MockJoharData.rockGarden) }

            CatalogSection("Nearby carousel", horizontalPadding = false) {
                Column(Modifier.padding(start = 20.dp)) {
                    JoharPlaceCarousel(
                        title = "Near you",
                        subtitle = "Within about 20 min drive",
                        places = MockJoharData.nearbyPlaces,
                    )
                }
            }

            CatalogSection("Route card") { JoharRouteCard(model = MockJoharData.airportRoute) }
            CatalogSection("Itinerary") { JoharItineraryCard(model = MockJoharData.familyDay) }

            CatalogSection("Map preview") {
                JoharMapPreviewCard(
                    title = "Birsa Munda Airport",
                    subtitle = "Hinoo, Ranchi",
                    markerLabel = "Airport",
                    routeSummary = "Lalpur → Doranda → Hinoo → Airport",
                    action = JoharCardAction("expand-map", "Expand Map", primary = true),
                )
            }

            CatalogSection("Local discovery") {
                JoharLocalPickCard(
                    title = "Dhuska + Ghugni",
                    subtitle = "A classic Jharkhand snack combo",
                    detail = "Great when you want something that feels more local than a generic cafe stop.",
                    action = JoharCardAction("find-local", "Find Nearby", primary = true),
                )
            }

            CatalogSection("Utility") {
                JoharUtilityCard(
                    title = "Petrol Pump",
                    subtitle = "Hinoo Main Road",
                    metadata = "1.8 km • On your route",
                    actions = listOf(
                        JoharCardAction("directions", "Directions"),
                        JoharCardAction("add-stop", "Add Stop", primary = true),
                    ),
                )
            }

            CatalogSection("Comparison") {
                JoharComparisonCard(
                    leftTitle = "Rock Garden",
                    rightTitle = "Biodiversity Park",
                    rows = listOf(
                        JoharComparisonItem("Kids", "Good", "Great"),
                        JoharComparisonItem("Walking", "Medium", "Low–Medium"),
                        JoharComparisonItem("Nature", "Yes", "Yes"),
                        JoharComparisonItem("Views", "Yes", "—"),
                    ),
                    recommendation = "Biodiversity Park for younger kids.",
                )
            }

            CatalogSection("Trust states") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    JoharInfoCard(
                        title = "Dhuska near Lalpur",
                        text = "I know Dhuska is commonly available in Ranchi, but I don't have a confirmed Lalpur vendor yet.",
                        tone = JoharInfoTone.NOT_CONFIRMED,
                    )
                    JoharInfoCard(
                        title = "Pahari Mandir",
                        text = "Today's opening status was recently verified.",
                        tone = JoharInfoTone.LIVE,
                    )
                }
            }

            CatalogSection("Clarification chips") {
                JoharPreferenceChips(
                    title = "What matters most?",
                    options = listOf("Pure Veg", "Family", "Under ₹500", "Parking"),
                )
            }

            CatalogSection("Constraint summary") {
                JoharConstraintSummary(
                    constraints = listOf("Family trip", "Low walking", "Vegetarian", "Parking preferred"),
                )
            }

            CatalogSection("Source row") {
                JoharSourceRow(sourceName = "Ranchi District official source", verified = true)
            }

            CatalogSection("Suggestions") {
                JoharSuggestionCard(
                    suggestions = listOf(
                        "Mujhe tourist wali Ranchi nahi, local wali Ranchi dikhao",
                        "Kids bore ho rahe hain, nearby kya hai?",
                        "Lalpur se airport ja raha hu, raste me lunch kaha karu?",
                    ),
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CatalogSection(
    title: String,
    horizontalPadding: Boolean = true,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = if (horizontalPadding) Modifier.padding(horizontal = 20.dp) else Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (horizontalPadding) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        } else {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            content()
        }
    }
}

private object MockJoharData {
    val rockGarden = JoharPlaceCardModel(
        name = "Rock Garden",
        area = "Morabadi, Ranchi",
        description = "Good for family time, views and a relaxed evening.",
        tags = listOf("Family", "Kids", "Views"),
        metadata = listOf("3.2 km", "12 min", "Walking: Medium"),
        actions = listOf(
            JoharCardAction("map", "View on Map"),
            JoharCardAction("navigate", "Navigate", primary = true),
        ),
    )

    val nearbyPlaces = listOf(
        rockGarden,
        JoharPlaceCardModel(
            name = "Nakshatra Van",
            area = "Ranchi",
            description = "Easy outdoor stop for a family walk.",
            tags = listOf("Park", "Family"),
            metadata = listOf("14 min"),
        ),
        JoharPlaceCardModel(
            name = "Oxygen Park",
            area = "Morabadi, Ranchi",
            description = "Quick outdoor break with kids.",
            tags = listOf("Outdoor", "Kids"),
            metadata = listOf("17 min"),
        ),
    )

    val airportRoute = JoharRouteCardModel(
        title = "Lalpur → Birsa Munda Airport",
        durationLabel = "28 min",
        distanceLabel = "8.1 km",
        stops = listOf(
            JoharRouteStop("Lalpur", "Start"),
            JoharRouteStop("Fuel stop", "On route", "+2 min"),
            JoharRouteStop("Veg lunch — Hinoo", "Family friendly", "+4 min"),
            JoharRouteStop("Birsa Munda Airport", "Destination"),
        ),
        status = listOf("Offline route", "Live traffic unavailable"),
        actions = listOf(
            JoharCardAction("route", "View Route"),
            JoharCardAction("navigate", "Navigate", primary = true),
        ),
    )

    val familyDay = JoharItineraryCardModel(
        title = "Family Day in Ranchi",
        summary = "4 stops • relaxed pace",
        stops = listOf(
            JoharItineraryStop("9:00", "Nakshatra Van", "Easy morning walk"),
            JoharItineraryStop("11:00", "Rock Garden", "Views + family photos"),
            JoharItineraryStop("3:00", "Biodiversity Park", "Kids + nature"),
            JoharItineraryStop("5:30", "Jagannath Mandir", "Peaceful evening stop"),
        ),
        actions = listOf(
            JoharCardAction("trip-map", "View Trip Map"),
            JoharCardAction("start-trip", "Start Trip", primary = true),
        ),
    )
}
