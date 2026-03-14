package com.ibrahim.geoalarm

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.maps.android.compose.*
import com.ibrahim.geoalarm.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { GeoAlarmTheme { GeoAlarmApp() } }
    }
}

// ── Dark-style JSON for Google Maps ──────────────────────────────────
private val darkMapStyle = """
[
  {"elementType":"geometry","stylers":[{"color":"#1d2c4d"}]},
  {"elementType":"labels.text.fill","stylers":[{"color":"#8ec3b9"}]},
  {"elementType":"labels.text.stroke","stylers":[{"color":"#1a3646"}]},
  {"featureType":"administrative.country","elementType":"geometry.stroke","stylers":[{"color":"#4b6878"}]},
  {"featureType":"land_parcel","elementType":"labels.text.fill","stylers":[{"color":"#64779e"}]},
  {"featureType":"poi","elementType":"geometry","stylers":[{"color":"#283d6a"}]},
  {"featureType":"poi","elementType":"labels.text.fill","stylers":[{"color":"#6f9ba5"}]},
  {"featureType":"poi.park","elementType":"geometry.fill","stylers":[{"color":"#023e58"}]},
  {"featureType":"poi.park","elementType":"labels.text.fill","stylers":[{"color":"#3C7680"}]},
  {"featureType":"road","elementType":"geometry","stylers":[{"color":"#304a7d"}]},
  {"featureType":"road","elementType":"labels.text.fill","stylers":[{"color":"#98a5be"}]},
  {"featureType":"road.highway","elementType":"geometry","stylers":[{"color":"#2c6675"}]},
  {"featureType":"road.highway","elementType":"geometry.stroke","stylers":[{"color":"#255763"}]},
  {"featureType":"transit","elementType":"labels.text.fill","stylers":[{"color":"#98a5be"}]},
  {"featureType":"transit.line","elementType":"geometry.fill","stylers":[{"color":"#283d6a"}]},
  {"featureType":"water","elementType":"geometry","stylers":[{"color":"#0e1626"}]},
  {"featureType":"water","elementType":"labels.text.fill","stylers":[{"color":"#4e6d70"}]}
]
""".trimIndent()

// ── Main App Composable ──────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeoAlarmApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // ── Permission state ─────────────────
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED
            else true
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotificationPermission = permissions[Manifest.permission.POST_NOTIFICATIONS] == true
        }
    }

    // ── Map / location state ─────────────
    var currentLat by remember { mutableStateOf<Double?>(null) }
    var currentLon by remember { mutableStateOf<Double?>(null) }
    var targetLat by remember { mutableStateOf<Double?>(null) }
    var targetLon by remember { mutableStateOf<Double?>(null) }
    var selectedTarget by remember { mutableStateOf<LatLng?>(null) }
    var distanceMeters by remember { mutableStateOf<Float?>(null) }
    var isLoadingLocation by remember { mutableStateOf(false) }

    // ── Alarm state ──────────────────────
    var thresholdMeters by remember { mutableFloatStateOf(500f) }
    var isAlarmActive by remember { mutableStateOf(false) }
    var isAlarmTriggered by remember { mutableStateOf(false) }

    // ── Camera ───────────────────────────
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(41.015137, 28.979530), 12f)
    }

    // ── Distance helper ──────────────────
    fun calcDistance(cLat: Double, cLon: Double, tLat: Double, tLon: Double): Float {
        val r = FloatArray(1)
        android.location.Location.distanceBetween(cLat, cLon, tLat, tLon, r)
        return r[0]
    }
    fun updateDistance() {
        if (currentLat != null && currentLon != null && targetLat != null && targetLon != null) {
            distanceMeters = calcDistance(currentLat!!, currentLon!!, targetLat!!, targetLon!!)
        }
    }

    // ── Broadcast receiver for service updates ──
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                when (intent.action) {
                    LocationService.ACTION_DISTANCE_UPDATE -> {
                        distanceMeters = intent.getFloatExtra(LocationService.EXTRA_CURRENT_DISTANCE, 0f)
                        currentLat = intent.getDoubleExtra(LocationService.EXTRA_CURRENT_LAT, 0.0)
                        currentLon = intent.getDoubleExtra(LocationService.EXTRA_CURRENT_LON, 0.0)
                    }
                    LocationService.ACTION_ALARM_TRIGGERED -> {
                        isAlarmTriggered = true
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(LocationService.ACTION_DISTANCE_UPDATE)
            addAction(LocationService.ACTION_ALARM_TRIGGERED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        onDispose { context.unregisterReceiver(receiver) }
    }

    // ── Auto zoom to both markers ────────
    LaunchedEffect(currentLat, currentLon, targetLat, targetLon) {
        if (currentLat != null && currentLon != null && targetLat != null && targetLon != null) {
            val bounds = LatLngBounds.Builder()
                .include(LatLng(currentLat!!, currentLon!!))
                .include(LatLng(targetLat!!, targetLon!!))
                .build()
            scope.launch {
                cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 180), 700)
            }
        }
    }

    // ── Permission not granted screen ────
    if (!hasLocationPermission) {
        PermissionRequestScreen {
            val perms = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                perms.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            permissionLauncher.launch(perms.toTypedArray())
        }
        return
    }

    // ── Main content ─────────────────────
    val currentPos = if (currentLat != null && currentLon != null)
        LatLng(currentLat!!, currentLon!!) else null
    val routePoints = if (currentPos != null && selectedTarget != null)
        listOf(currentPos, selectedTarget!!) else emptyList()

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Full-screen Google Map ───────
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapStyleOptions = try {
                    MapStyleOptions(darkMapStyle)
                } catch (_: Exception) { null },
                isMyLocationEnabled = hasLocationPermission
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                mapToolbarEnabled = false
            ),
            onMapLongClick = { latLng ->
                if (!isAlarmActive) {
                    selectedTarget = latLng
                    targetLat = latLng.latitude
                    targetLon = latLng.longitude
                    updateDistance()
                }
            }
        ) {
            currentPos?.let {
                Marker(
                    state = MarkerState(position = it),
                    title = "Konumum"
                )
            }
            selectedTarget?.let {
                Marker(
                    state = MarkerState(position = it),
                    title = "Hedef"
                )
                Circle(
                    center = it,
                    radius = thresholdMeters.toDouble(),
                    fillColor = Cyan60.copy(alpha = 0.12f),
                    strokeColor = Cyan60.copy(alpha = 0.5f),
                    strokeWidth = 3f
                )
            }
            if (routePoints.size == 2) {
                Polyline(
                    points = routePoints,
                    color = Cyan60,
                    width = 6f
                )
            }
        }

        // ── Top bar — My Location FAB ────
        SmallFloatingActionButton(
            onClick = {
                isLoadingLocation = true
                val token = CancellationTokenSource()
                try {
                    fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token)
                        .addOnSuccessListener { loc ->
                            isLoadingLocation = false
                            if (loc != null) {
                                currentLat = loc.latitude
                                currentLon = loc.longitude
                                updateDistance()
                                scope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(
                                            LatLng(loc.latitude, loc.longitude), 15f
                                        ), 600
                                    )
                                }
                            }
                        }
                        .addOnFailureListener { isLoadingLocation = false }
                } catch (_: SecurityException) {
                    isLoadingLocation = false
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 52.dp, end = 16.dp),
            containerColor = DarkCard.copy(alpha = 0.9f),
            contentColor = Cyan60
        ) {
            if (isLoadingLocation) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Cyan60
                )
            } else {
                Icon(Icons.Filled.MyLocation, contentDescription = "Konumum")
            }
        }

        // ── Bottom Sheet Panel ───────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            DarkSurface.copy(alpha = 0.97f),
                            DarkSurface
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = DarkCardBorder.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                )
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Handle bar
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(TextMuted)
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(16.dp))

            // ── Status badge ─────────────
            StatusBadge(
                isAlarmActive = isAlarmActive,
                isAlarmTriggered = isAlarmTriggered
            )

            Spacer(Modifier.height(16.dp))

            // ── Info Cards Row ───────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.NearMe,
                    label = "Mesafe",
                    value = distanceMeters?.let { "${"%.0f".format(it)}m" } ?: "—",
                    accentColor = Cyan60
                )
                InfoCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.TrackChanges,
                    label = "Eşik",
                    value = "${thresholdMeters.roundToInt()}m",
                    accentColor = Blue60
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Distance threshold slider ─
            Text(
                "Alarm Mesafesi",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary
            )
            Spacer(Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("100m", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Slider(
                    value = thresholdMeters,
                    onValueChange = { thresholdMeters = it },
                    valueRange = 100f..5000f,
                    steps = 48,
                    enabled = !isAlarmActive,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Cyan60,
                        activeTrackColor = Cyan60,
                        inactiveTrackColor = DarkCard
                    )
                )
                Text("5km", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }

            Spacer(Modifier.height(12.dp))

            // ── Target info ──────────────
            AnimatedVisibility(
                visible = selectedTarget != null,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkCard)
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = Red60,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Hedef Konum", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            Text(
                                "${"%.4f".format(targetLat)}, ${"%.4f".format(targetLon)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }
                    }
                    if (!isAlarmActive) {
                        IconButton(onClick = {
                            targetLat = null
                            targetLon = null
                            selectedTarget = null
                            distanceMeters = null
                        }) {
                            Icon(Icons.Filled.Close, contentDescription = "Temizle", tint = TextMuted)
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = selectedTarget == null && !isAlarmActive,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkCard.copy(alpha = 0.5f))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Outlined.TouchApp,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Haritada uzun basarak hedef belirleyin",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Action Buttons ───────────
            if (isAlarmTriggered) {
                // Alarm arrived — show STOP
                Button(
                    onClick = {
                        AlarmHelper.stopAlarm(context)
                        context.stopService(Intent(context, LocationService::class.java))
                        isAlarmActive = false
                        isAlarmTriggered = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RedBright,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Filled.AlarmOff, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Alarmı Durdur", style = MaterialTheme.typography.titleMedium)
                }
            } else if (isAlarmActive) {
                // Tracking active — show CANCEL
                OutlinedButton(
                    onClick = {
                        context.stopService(Intent(context, LocationService::class.java))
                        isAlarmActive = false
                        isAlarmTriggered = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 2.dp,
                        brush = Brush.horizontalGradient(listOf(Red60, RedBright))
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Red60)
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Alarmı İptal Et", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                // Ready to start
                Button(
                    onClick = {
                        if (selectedTarget != null) {
                            val intent = Intent(context, LocationService::class.java).apply {
                                putExtra(LocationService.EXTRA_TARGET_LAT, targetLat)
                                putExtra(LocationService.EXTRA_TARGET_LON, targetLon)
                                putExtra(LocationService.EXTRA_THRESHOLD, thresholdMeters)
                            }
                            ContextCompat.startForegroundService(context, intent)
                            isAlarmActive = true
                            isAlarmTriggered = false
                        }
                    },
                    enabled = selectedTarget != null && currentLat != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Cyan40,
                        contentColor = Color.White,
                        disabledContainerColor = DarkCard,
                        disabledContentColor = TextMuted
                    )
                ) {
                    Icon(Icons.Filled.NotificationsActive, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Alarmı Başlat", style = MaterialTheme.typography.titleMedium)
                }
            }

            // Bottom safe area spacer
            Spacer(Modifier.height(8.dp))
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

// ── Status Badge ─────────────────────────────────────────────────────
@Composable
fun StatusBadge(isAlarmActive: Boolean, isAlarmTriggered: Boolean) {
    val text: String
    val bgColor: Color
    val textColor: Color
    val icon: androidx.compose.ui.graphics.vector.ImageVector

    when {
        isAlarmTriggered -> {
            text = "ALARM ÇALIYOR!"
            bgColor = RedBright
            textColor = Color.White
            icon = Icons.Filled.Warning
        }
        isAlarmActive -> {
            text = "Takip Ediliyor"
            bgColor = GreenBright
            textColor = DarkSurface
            icon = Icons.Filled.GpsFixed
        }
        else -> {
            text = "Hazır"
            bgColor = TextMuted
            textColor = TextSecondary
            icon = Icons.Outlined.RadioButtonUnchecked
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isAlarmTriggered) 0.4f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor.copy(alpha = if (isAlarmTriggered) pulseAlpha else 0.15f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isAlarmTriggered || isAlarmActive) bgColor else textColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (isAlarmTriggered) Color.White else textColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ── Info Card ────────────────────────────────────────────────────────
@Composable
fun InfoCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    accentColor: Color
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCard)
            .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}

// ── Permission Request Screen ────────────────────────────────────────
@Composable
fun PermissionRequestScreen(onRequestPermission: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(DarkSurface, DarkSurfaceVariant)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Cyan60.copy(alpha = 0.2f), Color.Transparent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = Cyan60,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "Konum İzni Gerekli",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            Text(
                "GeoAlarm, hedefinize yaklaştığınızda sizi bilgilendirebilmek için konum izninize ihtiyaç duyar.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = onRequestPermission,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Cyan40,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Filled.LocationOn, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("İzin Ver", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
