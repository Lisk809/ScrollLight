package com.scrolllight.bible.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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

sealed class Screen(val route: String) {
    object Home         : Screen("home")
    object Plans        : Screen("plans")
    object Explore      : Screen("explore")
    object Profile      : Screen("profile")
    object BookContents : Screen("book_contents")
    object AiSettings   : Screen("ai_settings")
    object Reading      : Screen("reading/{bookId}/{chapter}") {
        fun createRoute(bookId: String, chapter: Int) = "reading/$bookId/$chapter"
    }
    object Search       : Screen("search?query={query}") {
        fun createRoute(query: String = "") = "search?query=$query"
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
val bottomNavRoutes = setOf(Screen.Home.route, Screen.Plans.route, Screen.Explore.route, Screen.Profile.route)

@Composable
fun ScrollLightNavHost() {
    val navController = rememberNavController()
    val navBackStack  by navController.currentBackStackEntryAsState()
    val currentRoute  = navBackStack?.destination?.route?.substringBefore("?")?.substringBefore("/")
    val showBottomBar = bottomNavRoutes.any { it == currentRoute }
    val hideFloating  = currentRoute == Screen.AiSettings.route

    val aiChatVm: AiChatViewModel = hiltViewModel()
    var readingCtx by remember { mutableStateOf(AiReadingContext("", "", 0)) }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = androidx.compose.ui.unit.dp(3)) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick  = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true; restoreState = true
                                }
                            },
                            icon  = { Icon(if (selected) item.selectedIcon else item.unselectedIcon, item.label) },
                            label = { Text(item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor   = MaterialTheme.colorScheme.primary,
                                selectedTextColor   = MaterialTheme.colorScheme.primary,
                                indicatorColor      = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        val navContent: @Composable () -> Unit = {
            NavHost(
                navController      = navController,
                startDestination   = Screen.Home.route,
                modifier           = Modifier.padding(paddingValues),
                enterTransition    = { fadeIn() + slideInHorizontally { it / 4 } },
                exitTransition     = { fadeOut() + slideOutHorizontally { -it / 4 } },
                popEnterTransition = { fadeIn() + slideInHorizontally { -it / 4 } },
                popExitTransition  = { fadeOut() + slideOutHorizontally { it / 4 } }
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        onNavigateToBookContents = { navController.navigate(Screen.BookContents.route) },
                        onNavigateToReading      = { b, c -> navController.navigate(Screen.Reading.createRoute(b, c)) },
                        onNavigateToSearch       = { navController.navigate(Screen.Search.createRoute()) }
                    )
                }
                composable(Screen.Plans.route)   { PlansScreen() }
                composable(Screen.Explore.route) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.padding(),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) { Text("探索（即将上线）", style = MaterialTheme.typography.headlineSmall) }
                }
                composable(Screen.Profile.route) {
                    ProfileScreen(onNavigateToAiSettings = { navController.navigate(Screen.AiSettings.route) })
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
                    val readingState by readingVm.state.collectAsState()
                    LaunchedEffect(readingState.book, readingState.chapter, readingState.selectedVerse) {
                        readingState.book?.let { book ->
                            readingCtx = AiReadingContext(
                                bookId = book.id, bookName = book.name,
                                chapter = readingState.chapter, selectedVerse = readingState.selectedVerse
                            )
                        }
                    }
                    ReadingScreen(
                        bookId               = bookId, chapter = chapter,
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

        if (!hideFloating) {
            AiFloatingWindowHost(
                readingContext       = readingCtx,
                onNavigateToSettings = { navController.navigate(Screen.AiSettings.route) },
                vm                   = aiChatVm,
                content              = navContent
            )
        } else {
            navContent()
        }
    }
}
