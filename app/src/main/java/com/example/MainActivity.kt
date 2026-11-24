package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.core.configureAppCheck
import com.example.data.DiscoveryCategory
import com.example.data.DiscoveryRecord
import com.example.data.IntelAiRepository
import com.example.data.IntelFirestoreRepository
import com.example.data.PreferencesRepository
import com.example.data.UserPreferences
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutUser
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.DiscoveryDetailSheet
import com.example.ui.screens.AssistantScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DiscoveryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.IntelDiscoveryTheme
import com.example.ui.viewmodel.IntelDiscoveryViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

object IntelRoutes {
    const val HOME = "home"
    const val DASHBOARD = "dashboard"
    const val DISCOVERY = "discovery"
    const val ASSISTANT = "assistant"
    const val SETTINGS = "settings"
}

private data class NavDestinationItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val NavDestinations = listOf(
    NavDestinationItem(
        route = IntelRoutes.HOME,
        label = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    ),
    NavDestinationItem(
        route = IntelRoutes.DASHBOARD,
        label = "Workspace",
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard
    ),
    NavDestinationItem(
        route = IntelRoutes.DISCOVERY,
        label = "Discover",
        selectedIcon = Icons.Filled.Explore,
        unselectedIcon = Icons.Outlined.Explore
    ),
    NavDestinationItem(
        route = IntelRoutes.ASSISTANT,
        label = "Assistant",
        selectedIcon = Icons.Filled.AutoAwesome,
        unselectedIcon = Icons.Outlined.AutoAwesome
    ),
    NavDestinationItem(
        route = IntelRoutes.SETTINGS,
        label = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Firebase App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            val appContext = applicationContext
            val prefsRepo = remember(appContext) { PreferencesRepository(appContext) }
            val userPrefs by prefsRepo.preferences.collectAsStateWithLifecycle(initialValue = UserPreferences())

            IntelDiscoveryTheme(themePreference = userPrefs.theme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    IntelDiscoveryRoot(prefsRepo = prefsRepo)
                }
            }
        }
    }
}

@Composable
private fun IntelDiscoveryRoot(prefsRepo: PreferencesRepository) {
    var currentUser by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = { currentUser = Firebase.auth.currentUser })
    } else {
        key(user.uid) {
            AuthenticatedIntelShell(
                user = user,
                prefsRepo = prefsRepo
            )
        }
    }
}

@Composable
private fun AuthenticatedIntelShell(
    user: FirebaseUser,
    prefsRepo: PreferencesRepository
) {
    val uid = user.uid
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: IntelDiscoveryViewModel = viewModel(
        key = uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                IntelDiscoveryViewModel(
                    firestoreRepo = IntelFirestoreRepository(db = db),
                    aiRepo = IntelAiRepository(),
                    prefsRepo = prefsRepo,
                    currentUserId = uid
                )
            }
        }
    )

    val userPrefs by viewModel.preferences.collectAsStateWithLifecycle()
    val discoveriesState by viewModel.discoveriesState.collectAsStateWithLifecycle()
    val searchesState by viewModel.searchesState.collectAsStateWithLifecycle()
    val messagesState by viewModel.messagesState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val investigationState by viewModel.investigationState.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSavingDiscovery.collectAsStateWithLifecycle()
    val assistantInput by viewModel.assistantInput.collectAsStateWithLifecycle()
    val streamingState by viewModel.assistantStreamingState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var inspectedRecord by remember { mutableStateOf<DiscoveryRecord?>(null) }
    var pendingDeleteRecord by remember { mutableStateOf<DiscoveryRecord?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.toastMessages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: IntelRoutes.HOME

    fun navigateToTab(route: String) {
        if (currentRoute == route) return
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    if (inspectedRecord != null) {
        val activeRecord = inspectedRecord!!
        DiscoveryDetailSheet(
            record = activeRecord,
            themePreference = userPrefs.theme,
            onDismiss = { inspectedRecord = null },
            onTogglePin = {
                viewModel.toggleDiscoveryPin(activeRecord)
                inspectedRecord = activeRecord.copy(pinned = !activeRecord.pinned)
            },
            onCopyBrief = {
                val text = buildString {
                    appendLine("INTELDISCOVERY: ${activeRecord.query}")
                    appendLine(activeRecord.summary)
                    if (activeRecord.verifiedFacts.isNotEmpty()) {
                        appendLine("\nVerified Facts:")
                        activeRecord.verifiedFacts.forEach { appendLine("- $it") }
                    }
                    if (activeRecord.interpretations.isNotEmpty()) {
                        appendLine("\nInterpretations:")
                        activeRecord.interpretations.forEach { appendLine("- $it") }
                    }
                    if (activeRecord.aiSuggestions.isNotEmpty()) {
                        appendLine("\nAI Suggestions:")
                        activeRecord.aiSuggestions.forEach { appendLine("- $it") }
                    }
                }
                clipboard.setText(AnnotatedString(text))
                viewModel.showToast("Brief copied to clipboard")
            },
            onAskAssistant = {
                viewModel.updateAssistantInput(
                    "Explore \"${activeRecord.query}\" further. Key summary: ${activeRecord.summary}"
                )
                inspectedRecord = null
                navigateToTab(IntelRoutes.ASSISTANT)
            },
            onDelete = {
                pendingDeleteRecord = activeRecord
                inspectedRecord = null
            }
        )
    }

    if (pendingDeleteRecord != null) {
        val target = pendingDeleteRecord!!
        ConfirmActionDialog(
            title = "Delete Discovery?",
            message = "Remove \"${target.query}\" from your Intelligence Workspace?",
            confirmLabel = "Delete",
            onConfirm = {
                pendingDeleteRecord = null
                viewModel.deleteDiscovery(target)
            },
            onDismiss = { pendingDeleteRecord = null }
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets.statusBars,
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        NavDestinations.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.testTag("nav_tab_${item.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
                    ) {
                        NavDestinations.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag("nav_rail_${item.route}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = IntelRoutes.HOME
                    ) {
                        composable(IntelRoutes.HOME) {
                            HomeScreen(
                                userDisplayName = user.displayName,
                                discoveriesState = discoveriesState,
                                searchesState = searchesState,
                                onInvestigate = { query, category ->
                                    viewModel.investigateTopic(query, category)
                                    navigateToTab(IntelRoutes.DISCOVERY)
                                },
                                onOpenDashboard = { navigateToTab(IntelRoutes.DASHBOARD) },
                                onOpenAssistant = { navigateToTab(IntelRoutes.ASSISTANT) },
                                onOpenDiscoveryRecord = { inspectedRecord = it }
                            )
                        }

                        composable(IntelRoutes.DASHBOARD) {
                            DashboardScreen(
                                userDisplayName = user.displayName,
                                discoveriesState = discoveriesState,
                                searchesState = searchesState,
                                messagesState = messagesState,
                                onInvestigateTopic = { query, category ->
                                    viewModel.investigateTopic(query, category)
                                    navigateToTab(IntelRoutes.DISCOVERY)
                                },
                                onOpenDiscoveriesTab = { navigateToTab(IntelRoutes.DISCOVERY) },
                                onOpenRecord = { inspectedRecord = it },
                                onTogglePin = { viewModel.toggleDiscoveryPin(it) },
                                onDeleteRecord = { pendingDeleteRecord = it }
                            )
                        }

                        composable(IntelRoutes.DISCOVERY) {
                            DiscoveryScreen(
                                searchQuery = searchQuery,
                                selectedCategory = selectedCategory,
                                sortOrder = sortOrder,
                                investigationState = investigationState,
                                isSaving = isSaving,
                                discoveriesState = discoveriesState,
                                searchesState = searchesState,
                                themePreference = userPrefs.theme,
                                onQueryChange = viewModel::updateSearchQuery,
                                onCategorySelected = viewModel::selectCategory,
                                onSortSelected = viewModel::selectSortOrder,
                                onInvestigate = { q, cat ->
                                    viewModel.investigateTopic(q, cat)
                                },
                                onSaveDraft = { pinned -> viewModel.saveCurrentDraft(pinned) },
                                onAskAssistant = { draft ->
                                    viewModel.prepareAssistantPromptFromDraft(draft)
                                    navigateToTab(IntelRoutes.ASSISTANT)
                                },
                                onOpenRecord = { inspectedRecord = it },
                                onTogglePin = { viewModel.toggleDiscoveryPin(it) },
                                onDeleteRecord = { pendingDeleteRecord = it },
                                onShowToast = viewModel::showToast
                            )
                        }

                        composable(IntelRoutes.ASSISTANT) {
                            AssistantScreen(
                                inputText = assistantInput,
                                activeModel = userPrefs.aiModel,
                                messagesState = messagesState,
                                streamingState = streamingState,
                                onInputChange = viewModel::updateAssistantInput,
                                onSendMessage = { prompt -> viewModel.sendAssistantMessage(prompt) },
                                onClearChat = viewModel::clearAssistantMessages,
                                onShowToast = viewModel::showToast
                            )
                        }

                        composable(IntelRoutes.SETTINGS) {
                            SettingsScreen(
                                userDisplayName = user.displayName,
                                userEmail = user.email,
                                preferences = userPrefs,
                                onThemeSelected = viewModel::setThemePreference,
                                onModelSelected = viewModel::setAiModelOption,
                                onDepthSelected = viewModel::setResponseDepth,
                                onClearSearchHistory = viewModel::clearSearchHistory,
                                onClearAssistantHistory = viewModel::clearAssistantMessages,
                                onSignOut = {
                                    signOutUser(credentialManager, scope)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
