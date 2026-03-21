package com.scrolllight.bible.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.scrolllight.bible.ai.*
import com.scrolllight.bible.ui.home.HomeScreen
import com.scrolllight.bible.ui.plans.PlansScreen
import com.scrolllight.bible.ui.profile.ProfileScreen
import com.scrolllight.bible.ui.reading.BookContentsScreen
import com.scrolllight.bible.ui.reading.ReadingScreen
import com.scrolllight.bible.ui.reading.ReadingViewModel
import com.scrolllight.bible.ui.search.SearchScreen
import com.scrolllight.bible.ui.theme.glassBackground

// ── Routes ────────────────────────────────────────────────────────────────────

sealed class Screen(val route: String) {
    object Home         : Screen("home")
    object Plans        : Screen("plans")
    object Explore      : Screen("explore")
    object Profile      : Screen("profile")
    object BookContents : Screen("book_contents")
    object AiSettings   : Screen("ai_settings")
    object Reading      : Screen("reading/{bookId}/{chapter}") {
        fun createRoute(b: String, c: Int) = "reading/$b/$c"
    }
    object Search       : Screen("search?query={query}") {
        fun createRoute(q: String = "") = "search?query=$q"
    }
}

data class BottomNavItem(
    val screen: Screen, val label: String,
    val selectedIcon: ImageVector, val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home,    "读经",  Icons.Filled.Book,          Icons.Outlined.Book),
    BottomNavItem(Screen.Plans,   "计划",  Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    BottomNavItem(Screen.Explore, "探索",  Icons.Filled.Explore,       Icons.Outlined.Explore),
    BottomNavItem(Screen.Profile, "我的",  Icons.Filled.Person,        Icons.Outlined.Person),
)
val bottomNavRoutes = setOf(
    Screen.Home.route, Screen.Plans.route, Screen.Explore.route, Screen.Profile.route
)

// ── Main NavHost ──────────────────────────────────────────────────────────────

@Composable
fun ScrollLightNavHost() {
    val navController  = rememberNavController()
    val navBackStack   by navController.currentBackStackEntryAsState()
    val currentRoute   = navBackStack?.destination?.route
        ?.substringBefore("?")?.substringBefore("/")
    val showBottomBar  = bottomNavRoutes.any { it == currentRoute }
    val hideFloating   = currentRoute == Screen.AiSettings.route

    val aiChatVm: AiChatViewModel = hiltViewModel()
    var readingCtx by remember { mutableStateOf(AiReadingContext("", "", 0)) }

    val colors = MaterialTheme.colorScheme
    val isDark  = colors.background.luminance() < 0.15f

    // ── Key fix: AiFloatingWindowHost wraps the ENTIRE Scaffold ──────────
    // This means the floating panel is NOT constrained by paddingValues,
    // so it can truly cover the full screen height.
    val scaffoldContent: @Composable () -> Unit = {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0),   // let content manage its own insets
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                            .background(
                                if (isDark) colors.surfaceVariant.copy(alpha = 0.88f)
                                else Color.White.copy(alpha = 0.85f),
                                RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                            )
                    ) {
                        bottomNavItems.forEach { item ->
                            val selected = currentRoute == item.screen.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(item.screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState    = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector        = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor   = colors.primary,
                                    selectedTextColor   = colors.primary,
                                    indicatorColor      = colors.primary.copy(alpha = 0.12f),
                                    unselectedIconColor = colors.onSurfaceVariant,
                                    unselectedTextColor = colors.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            NavHost(
                navController      = navController,
                startDestination   = Screen.Home.route,
                modifier           = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                enterTransition    = { fadeIn(tween(220)) + slideInHorizontally { it / 5 } },
                exitTransition     = { fadeOut(tween(180)) + slideOutHorizontally { -it / 5 } },
                popEnterTransition = { fadeIn(tween(220)) + slideInHorizontally { -it / 5 } },
                popExitTransition  = { fadeOut(tween(180)) + slideOutHorizontally { it / 5 } }
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        onNavigateToBookContents = { navController.navigate(Screen.BookContents.route) },
                        onNavigateToReading = { b, c -> navController.navigate(Screen.Reading.createRoute(b, c)) },
                        onNavigateToSearch  = { navController.navigate(Screen.Search.createRoute()) }
                    )
                }
                composable(Screen.Plans.route) { PlansScreen() }
                composable(Screen.Explore.route) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Text(
                            "探索功能即将上线",
                            style = MaterialTheme.typography.headlineSmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
                composable(Screen.Profile.route) {
                    ProfileScreen(
                        onNavigateToAiSettings = { navController.navigate(Screen.AiSettings.route) }
                    )
                }
                composable(Screen.BookContents.route) {
                    BookContentsScreen(
                        onBack          = { navController.popBackStack() },
                        onSelectChapter = { b, c -> navController.navigate(Screen.Reading.createRoute(b, c)) }
                    )
                }
                composable(Screen.Reading.route) { backStack ->
                    val bookId  = backStack.arguments?.getString("bookId")  ?: "mat"
                    val chapter = backStack.arguments?.getString("chapter")?.toIntOrNull() ?: 1
                    val readingVm: ReadingViewModel = hiltViewModel()
                    val rs by readingVm.state.collectAsState()

                    LaunchedEffect(rs.book, rs.chapter, rs.selectedVerse) {
                        rs.book?.let { book ->
                            readingCtx = AiReadingContext(
                                bookId        = book.id,
                                bookName      = book.name,
                                chapter       = rs.chapter,
                                selectedVerse = rs.selectedVerse
                            )
                        }
                    }
                    ReadingScreen(
                        bookId               = bookId,
                        chapter              = chapter,
                        onBack               = { navController.popBackStack() },
                        onNavigateToSearch   = { navController.navigate(Screen.Search.createRoute()) },
                        onNavigateToContents = { navController.navigate(Screen.BookContents.route) }
                    )
                }
                composable(Screen.Search.route) { backStack ->
                    val query = backStack.arguments?.getString("query") ?: ""
                    SearchScreen(
                        initialQuery = query,
                        onBack       = { navController.popBackStack() },
                        onVerseClick = { b, c -> navController.navigate(Screen.Reading.createRoute(b, c)) }
                    )
                }
                composable(Screen.AiSettings.route) {
                    AiSettingsScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }

    if (!hideFloating) {
        AiFloatingWindowHost(
            readingContext       = readingCtx,
            onNavigateToSettings = { navController.navigate(Screen.AiSettings.route) },
            vm                   = aiChatVm,
            content              = scaffoldContent   // whole Scaffold is inside host
        )
    } else {
        scaffoldContent()
    }
}
