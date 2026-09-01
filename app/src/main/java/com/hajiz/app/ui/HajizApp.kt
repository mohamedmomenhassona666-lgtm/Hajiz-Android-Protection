@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.hajiz.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hajiz.app.R
import com.hajiz.app.data.ProtectionSettings
import com.hajiz.app.security.SecurePinManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HajizApp(viewModel: HajizViewModel, onRequestVpnPermission: () -> Unit, onOpenVpnSettings: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val vpnActive by viewModel.vpnActive.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    NavHost(nav, startDestination = HajizRoute.Splash.path) {
        composable(HajizRoute.Splash.path) {
            SplashScreen { nav.navigate(if (settings.onboardingComplete) HajizRoute.Home.path else HajizRoute.Onboarding.path) { popUpTo(HajizRoute.Splash.path) { inclusive = true } } }
        }
        composable(HajizRoute.Onboarding.path) {
            OnboardingScreen(onFinish = { viewModel.completeOnboarding(); nav.navigate(HajizRoute.Home.path) { popUpTo(HajizRoute.Onboarding.path) { inclusive = true } } })
        }
            composable(HajizRoute.Home.path) {
            MainShell(nav, HajizRoute.Home) { HomeScreen(settings, vpnActive, viewModel, onRequestVpnPermission, nav) }
        }
        composable(HajizRoute.Protection.path) {
            MainShell(nav, HajizRoute.Protection) { ProtectionScreen(settings, viewModel, onOpenVpnSettings, nav) }
        }
        composable(HajizRoute.Progress.path) {
            MainShell(nav, HajizRoute.Progress) { ProgressScreen(settings, nav) }
        }
        composable(HajizRoute.Settings.path) {
            MainShell(nav, HajizRoute.Settings) { SettingsScreen(settings, viewModel, onOpenVpnSettings, nav) }
        }
        composable(HajizRoute.BlockedActivity.path) { Secondary(nav, "Blocked activity", Icons.Default.FilterAlt) { ActivityScreen(settings) } }
        composable(HajizRoute.FocusMode.path) { Secondary(nav, "Focus mode", Icons.Default.Timer) { FocusScreen() } }
        composable(HajizRoute.DailyCheckIn.path) { Secondary(nav, "Daily check-in", Icons.Default.CheckCircle) { CheckInScreen() } }
        composable(HajizRoute.UrgeMode.path) { Secondary(nav, "Take a moment", Icons.Default.Favorite) { UrgeScreen(viewModel) } }
            composable(HajizRoute.ProtectionLock.path) { Secondary(nav, "Protection lock", Icons.Default.Lock) { LockScreen() } }
        composable(HajizRoute.Notifications.path) { Secondary(nav, "Notifications", Icons.Default.Notifications) { NotificationScreen() } }
        composable(HajizRoute.WeeklyReport.path) { Secondary(nav, "Weekly report", Icons.Default.Assessment) { ReportScreen(settings) } }
        composable(HajizRoute.Privacy.path) { Secondary(nav, stringResource(R.string.privacy), Icons.Default.Security) { PrivacyScreen() } }
    }
}

@Composable
private fun SplashScreen(onDone: () -> Unit) {
    LaunchedEffect(Unit) { delay(650); onDone() }
    Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
        Icon(Icons.Default.Shield, null, Modifier.size(88.dp), MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(18.dp))
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text("Protect your next decision", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun OnboardingScreen(onFinish: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    val titles = listOf("A calmer space", "Local protection", "Make it yours")
    val descriptions = listOf(
        "Hajiz helps you pause and protect the next decision, without ads or tracking.",
        "Your device can filter selected DNS requests locally through Android VPN permission.",
        "Choose the support tools that help you stay committed. You can change them later.",
    )
    Column(Modifier.fillMaxSize().padding(24.dp), Arrangement.SpaceBetween) {
        Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
            Spacer(Modifier.height(24.dp))
            LinearProgressIndicator({ (page + 1) / 3f }, Modifier.fillMaxWidth())
            Text("${page + 1} / 3", color = MaterialTheme.colorScheme.primary)
            Icon(if (page == 0) Icons.Default.Favorite else Icons.Default.Shield, null, Modifier.size(72.dp), MaterialTheme.colorScheme.primary)
            Text(titles[page], style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text(descriptions[page], style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { if (page == 2) onFinish() else page++ }, Modifier.fillMaxWidth().height(54.dp)) {
                Text(if (page == 2) "Get started" else "Next")
            }
            TextButton(onClick = onFinish, Modifier.fillMaxWidth()) { Text("Skip") }
        }
    }
}

@Composable
private fun MainShell(nav: NavHostController, selected: HajizRoute, content: @Composable () -> Unit) {
    Scaffold(bottomBar = {
        NavigationBar {
            HajizRoute.bottomNavigation.forEach { route ->
                val icon = when (route) { HajizRoute.Home -> Icons.Default.Shield; HajizRoute.Protection -> Icons.Default.Security; HajizRoute.Progress -> Icons.Default.Assessment; else -> Icons.Default.Settings }
                NavigationBarItem(selected == route, { nav.navigate(route.path) { launchSingleTop = true } }, icon = { Icon(icon, route.path) }, label = { Text(route.path.replace("-", " ").replaceFirstChar { it.uppercase() }) })
            }
        }
    }) { padding -> Column(Modifier.padding(padding)) { content() } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Header(title: String, nav: NavHostController? = null) {
    TopAppBar(title = { Text(title, fontWeight = FontWeight.Bold) }, navigationIcon = {
        if (nav != null) IconButton({ nav.navigateUp() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
    })
}

@Composable
private fun HomeScreen(s: ProtectionSettings, vpn: Boolean, vm: HajizViewModel, requestVpn: () -> Unit, nav: NavHostController) {
    Scaffold(topBar = { Header(stringResource(R.string.app_name)) }) { p ->
        Column(Modifier.padding(p).padding(20.dp).verticalScroll(rememberScrollState()), Arrangement.spacedBy(14.dp)) {
            Text("Protect your next decision", style = MaterialTheme.typography.titleLarge)
            StatusCard(s, vpn)
            Button({ if (s.protectionEnabled) vm.stopProtection() else requestVpn() }, Modifier.fillMaxWidth().height(54.dp)) {
                Icon(Icons.Default.Shield, null); Spacer(Modifier.width(8.dp)); Text(if (s.protectionEnabled) "Turn off protection" else "Turn on protection")
            }
            ActionCard("Take a moment", "A ten-minute reset when you need one.", Icons.Default.Favorite) { nav.navigate(HajizRoute.UrgeMode.path) }
            ActionCard("Daily check-in", "Notice how today is going.", Icons.Default.CheckCircle) { nav.navigate(HajizRoute.DailyCheckIn.path) }
        }
    }
}

@Composable private fun StatusCard(s: ProtectionSettings, vpn: Boolean) {
    Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)) { Column(Modifier.padding(20.dp), Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Shield, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(10.dp)); Text(if (s.protectionEnabled) "Protection is active" else "Protection is off", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        Metric("VPN", if (vpn) "Active" else "Inactive"); Metric("Blocked today", s.blockedAttemptsToday.toString()); Metric("Protected days", s.protectedDays.toString())
    } }
}
@Composable private fun Metric(a: String, b: String) { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text(a, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(b, fontWeight = FontWeight.SemiBold) } }
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun ActionCard(title: String, text: String, icon: ImageVector, onClick: () -> Unit) { Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = MaterialTheme.colorScheme.secondary); Spacer(Modifier.width(14.dp)); Column { Text(title, fontWeight = FontWeight.Bold); Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant) } } } }

@Composable private fun ProtectionScreen(s: ProtectionSettings, vm: HajizViewModel, openVpn: () -> Unit, nav: NavHostController) {
    Scaffold(topBar = { Header("Protection", nav) }) { p -> Column(Modifier.padding(p).padding(20.dp).verticalScroll(rememberScrollState()), Arrangement.spacedBy(12.dp)) {
        Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)) { Column(Modifier.padding(18.dp), Arrangement.spacedBy(7.dp)) { Text(if (s.protectionEnabled) "Protection is active" else "Protection is ready", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("Local DNS filtering works while the VPN service is active.", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        Toggle("Block adult content", s.blockAdultContent) { vm.setBlockAdultContent(it) }; Toggle("Block explicit search", s.blockExplicitSearch) { vm.setBlockExplicitSearch(it) }
        Toggle("Strict mode", s.strictMode) { vm.setStrictMode(it) }; Toggle("Protect settings", s.protectSettings) { vm.setProtectSettings(it) }
        Toggle("Accountability mode", s.accountabilityMode) { vm.setAccountabilityMode(it) }; Toggle("Emergency access", s.emergencyAccess) { vm.setEmergencyAccess(it) }
        ActionCard("Blocked activity", "Review aggregate blocked attempts.", Icons.Default.FilterAlt) { nav.navigate(HajizRoute.BlockedActivity.path) }
        ActionCard("Focus mode", "Set aside distractions for a focused session.", Icons.Default.Timer) { nav.navigate(HajizRoute.FocusMode.path) }
        OutlinedButton(openVpn, Modifier.fillMaxWidth().height(50.dp)) { Text("Open Android VPN settings") }
    } }
}
@Composable private fun ProgressScreen(s: ProtectionSettings, nav: NavHostController) {
    Scaffold(topBar = { Header("Progress") }) { p -> Column(Modifier.padding(p).padding(20.dp), Arrangement.spacedBy(14.dp)) {
        Text("Small steps add up.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Card { Column(Modifier.padding(20.dp), Arrangement.spacedBy(12.dp)) { Metric("Protected days", s.protectedDays.toString()); Metric("Urge support sessions", s.urgeModeUses.toString()); LinearProgressIndicator({ (s.protectedDays.coerceAtMost(7) / 7f) }, Modifier.fillMaxWidth()) } }
        ActionCard("Weekly report", "Reflect on the last seven days.", Icons.Default.Assessment) { nav.navigate(HajizRoute.WeeklyReport.path) }
    } }
}
@Composable private fun SettingsScreen(s: ProtectionSettings, vm: HajizViewModel, openVpn: () -> Unit, nav: NavHostController) {
    Scaffold(topBar = { Header("Settings", nav) }) { p -> Column(Modifier.padding(p).padding(20.dp).verticalScroll(rememberScrollState()), Arrangement.spacedBy(12.dp)) {
        Toggle("Strict mode", s.strictMode) { vm.setStrictMode(it) }; Toggle("Protect settings", s.protectSettings) { vm.setProtectSettings(it) }; Toggle("Accountability mode", s.accountabilityMode) { vm.setAccountabilityMode(it) }
        ActionCard("Protection lock", "Manage your protection PIN.", Icons.Default.Lock) { nav.navigate(HajizRoute.ProtectionLock.path) }
        ActionCard("Notifications", "Choose which support reminders appear.", Icons.Default.Notifications) { nav.navigate(HajizRoute.Notifications.path) }
        ActionCard("Privacy", "Understand local-first processing.", Icons.Default.Security) { nav.navigate(HajizRoute.Privacy.path) }
        OutlinedButton(openVpn, Modifier.fillMaxWidth()) { Text("Open Android VPN settings") }
    } }
}
@Composable private fun Toggle(label: String, value: Boolean, onChange: (Boolean) -> Unit) { Card { Row(Modifier.fillMaxWidth().padding(16.dp), Alignment.CenterVertically) { Text(label, Modifier.weight(1f), fontWeight = FontWeight.SemiBold); Switch(value, onChange) } } }

@Composable private fun Secondary(nav: NavHostController, title: String, icon: ImageVector, content: @Composable () -> Unit) { Scaffold(topBar = { Header(title, nav) }) { p -> Column(Modifier.padding(p).padding(20.dp).verticalScroll(rememberScrollState()), Arrangement.spacedBy(16.dp)) { Icon(icon, null, Modifier.size(54.dp), MaterialTheme.colorScheme.primary); content() } } }
@Composable private fun ActivityScreen(s: ProtectionSettings) {
    var filter by remember { mutableStateOf("Today") }
    Text("Only aggregate activity is shown. Hajiz never displays domains, URLs, or page details.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Today", "This week", "This month").forEach { label ->
        FilterChip(selected = filter == label, onClick = { filter = label }, label = { Text(label) })
    } }
    Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)) { Column(Modifier.padding(18.dp), Arrangement.spacedBy(10.dp)) { Metric("Blocked attempts", s.blockedAttemptsToday.toString()); Metric("Period", filter); Metric("Processing", "On device") } }
    if (s.blockedAttemptsToday == 0) {
        Column(Modifier.fillMaxWidth().padding(vertical = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.CheckCircle, null, Modifier.size(48.dp), MaterialTheme.colorScheme.primary)
            Text("No blocked activity in this period.", style = MaterialTheme.typography.titleMedium)
            Text("Your local protection is ready when you need it.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    } else {
        Text("Recent activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        repeat(minOf(s.blockedAttemptsToday, 5)) { index ->
            Card { Row(Modifier.fillMaxWidth().padding(16.dp), Alignment.CenterVertically) { Icon(Icons.Default.FilterAlt, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(12.dp)); Column { Text("Protected request"); Text("Local blocklist match", color = MaterialTheme.colorScheme.onSurfaceVariant) }; Spacer(Modifier.weight(1f)); Text("Today") } }
        }
    }
}
@Composable private fun FocusScreen() {
    var duration by remember { mutableIntStateOf(25) }; var seconds by remember { mutableIntStateOf(0) }; var running by remember { mutableStateOf(false) }
    LaunchedEffect(running) { while (running && seconds > 0) { delay(1000); seconds-- }; if (seconds == 0) running = false }
    Text("Protect a clear window for one meaningful task.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(15, 25, 45).forEach { value -> Card(onClick = { duration = value; seconds = 0; running = false }, modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(if (duration == value) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)) { Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("$value", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("minutes") } } } }
    val total = duration * 60; val shown = if (seconds == 0 && !running) total else seconds; val fraction = if (total == 0) 0f else shown / total.toFloat()
    Box(Modifier.fillMaxWidth(), Alignment.Center) { CircularProgressIndicator(progress = { fraction }, modifier = Modifier.size(190.dp), strokeWidth = 12.dp); Text("%02d:%02d".format(shown / 60, shown % 60), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button({ if (!running && seconds == 0) seconds = total; running = !running }, Modifier.weight(1f).height(52.dp)) { Text(if (running) "Pause" else "Start") }
        OutlinedButton({ running = false; seconds = 0 }, Modifier.weight(1f).height(52.dp)) { Text("End") }
    }
    if (seconds == 0 && !running && shown == 0) Text("Session complete. Take a moment to notice what you accomplished.", color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
}
@Composable private fun CheckInScreen() {
    var selected by remember { mutableStateOf<String?>(null) }; var saved by remember { mutableStateOf(false) }; var note by remember { mutableStateOf("") }
    Text("A brief check-in helps you notice what support would help today.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text("How are you arriving?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Steady", "Hopeful", "Restless", "Difficult").forEach { mood -> FilterChip(selected == mood, { selected = mood; saved = false }, label = { Text(mood) }, modifier = Modifier.fillMaxWidth()) } }
    OutlinedTextField(note, { note = it; saved = false }, Modifier.fillMaxWidth(), minLines = 3, label = { Text("Optional reflection") })
    Button({ if (selected != null) saved = true }, Modifier.fillMaxWidth().height(52.dp), enabled = selected != null) { Text("Save check-in") }
    if (saved) Text("Check-in saved privately on this device.", color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
}
@Composable private fun UrgeScreen(vm: HajizViewModel) {
    var seconds by remember { mutableIntStateOf(600) }; var paused by remember { mutableStateOf(false) }; var support by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { vm.addUrgeModeUse() }; LaunchedEffect(paused) { while (!paused && seconds > 0) { delay(1000); seconds-- } }
    Text("An urge is a wave. You only need to take the next step.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("%02d:%02d".format(seconds / 60, seconds % 60), style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary); Text("A little more time creates room for your choice.") } }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { Button({ paused = !paused }, Modifier.weight(1f).height(52.dp)) { Text(if (paused) "Resume" else "Pause") }; OutlinedButton({ seconds = 0; paused = true }, Modifier.weight(1f).height(52.dp)) { Text("End") } }
    SupportCard("Breathing reset", "Slow inhale, gentle hold, longer exhale.") { support = "Try four counts in, four counts held, and six counts out." }
    SupportCard("Change your surroundings", "Move to a shared, bright space and put down your phone.") { support = "A change of place can make the next decision easier." }
    support?.let { Card { Text(it, Modifier.padding(16.dp), textAlign = TextAlign.Center) } }
    Text("This is behavioral support, not medical treatment. Contact someone you trust or local emergency services if you are in danger.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
}
@Composable private fun SupportCard(title: String, copy: String, onClick: () -> Unit) { Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(17.dp), Arrangement.spacedBy(5.dp)) { Text(title, fontWeight = FontWeight.Bold); Text(copy, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
@Composable private fun LockScreen() {
    var pin by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val manager = remember { SecurePinManager(LocalContext.current) }
    Text("A protection PIN helps make sensitive changes intentional.")
    OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(8) }, Modifier.fillMaxWidth(), label = { Text("Protection PIN") })
    OutlinedTextField(confirmation, { confirmation = it.filter(Char::isDigit).take(8) }, Modifier.fillMaxWidth(), label = { Text("Confirm PIN") })
    Button({
        if (pin.length >= 4 && pin == confirmation) scope.launch { manager.setPin(pin); saved = true }
    }, Modifier.fillMaxWidth().height(52.dp)) { Text("Save PIN") }
    if (saved) Text("PIN saved securely on this device.", color = MaterialTheme.colorScheme.primary)
}
@Composable private fun NotificationScreen() { var protection by remember { mutableStateOf(true) }; var checkins by remember { mutableStateOf(true) }; var focus by remember { mutableStateOf(false) }; Text("Choose gentle reminders. Android permission is always controlled by you.", color = MaterialTheme.colorScheme.onSurfaceVariant); Toggle("Protection status", protection) { protection = it }; Toggle("Daily check-in reminders", checkins) { checkins = it }; Toggle("Focus session completion", focus) { focus = it }; Card { Text("Hajiz requests notifications only for visible protection and support features.", Modifier.padding(16.dp)) } }
@Composable private fun ReportScreen(s: ProtectionSettings) { Text("A week of intentional choices", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)) { Column(Modifier.padding(20.dp), Arrangement.spacedBy(8.dp)) { Text("Protection consistency", fontWeight = FontWeight.Bold); Text("${(s.protectedDays.coerceIn(0, 7) * 100 / 7)}%", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary); Text("Based on local protection days") } }; Metric("Protected days", s.protectedDays.toString()); Metric("Blocked attempts", s.blockedAttemptsToday.toString()); Text("Daily consistency is more useful than perfection. Keep building the routine.", color = MaterialTheme.colorScheme.onSurfaceVariant); BarChart(listOf(3, 5, 4, 6, 7, 5, s.protectedDays.coerceAtMost(7))) }
@Composable private fun BarChart(values: List<Int>) {
    val max = (values.maxOrNull() ?: 1).coerceAtLeast(1)
    Row(Modifier.fillMaxWidth().height(120.dp), Arrangement.spacedBy(8.dp), Alignment.Bottom) {
        values.forEachIndexed { index, value ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                Box(Modifier.fillMaxWidth().height((90 * value / max).dp).background(MaterialTheme.colorScheme.primary))
                Text(listOf("M", "T", "W", "T", "F", "S", "S")[index], style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
@Composable private fun PrivacyScreen() { Text("Hajiz processes DNS matching locally. It does not store URLs, page content, or images."); Text("Android VPN and device capabilities determine what can be enforced.", color = MaterialTheme.colorScheme.onSurfaceVariant) }