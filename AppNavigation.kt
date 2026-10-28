// What this file is / where it fits:
// This is the app's "map of screens". It lists every screen (Home, Setup, Toss, Players,
// Scoring, Innings break, Summary, History, Match detail) and says which button press leads
// to which screen. It also decides which screen to reopen if the app was closed mid-match.
//
// Jargon used below:
//  - Composable: a Kotlin function that draws a piece of the screen (marked @Composable).
//  - Route: a text "address" for a screen, like "scoring/1", used to move between screens.
//  - Back stack: the pile of screens the user has visited; pressing Back removes the top one.
//  - NavHost: the container that shows whichever screen's route is currently on top.

package com.waleedahmedja.score247.navigation

import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.waleedahmedja.score247.data.model.MatchPhase
import com.waleedahmedja.score247.data.model.Routes
import com.waleedahmedja.score247.ui.screens.AboutScreen
import com.waleedahmedja.score247.ui.screens.DocumentScreen
import com.waleedahmedja.score247.ui.screens.HistoryScreen
import com.waleedahmedja.score247.ui.screens.HomeScreen
import com.waleedahmedja.score247.ui.screens.InningsBreakScreen
import com.waleedahmedja.score247.ui.screens.MatchDetailScreen
import com.waleedahmedja.score247.ui.screens.PlayersScreen
import com.waleedahmedja.score247.ui.screens.ScoringScreen
import com.waleedahmedja.score247.ui.screens.SetupScreen
import com.waleedahmedja.score247.ui.screens.SummaryScreen
import com.waleedahmedja.score247.ui.screens.TossScreen
import com.waleedahmedja.score247.ui.theme.Ink50
import com.waleedahmedja.score247.viewmodel.MatchViewModel

/**
 * Works out which screen a match should reopen on, based on how far the match had got
 * (its "phase"). Returning null means "just stay on the Home screen".
 */
private fun resumeRoute(phase: MatchPhase): String? = when (phase) {
    MatchPhase.SETUP, MatchPhase.COMPLETED -> null
    MatchPhase.TOSS                        -> Routes.TOSS
    MatchPhase.SETUP_INNINGS_1             -> Routes.PLAYERS
    MatchPhase.INNINGS_1                   -> Routes.scoring(1)
    // SETUP_INNINGS_2 is an old leftover phase: that screen no longer exists, because the
    // "Start chase" button now does its job. Both leftover phases go to the innings break screen.
    MatchPhase.INNINGS_BREAK,
    MatchPhase.SETUP_INNINGS_2             -> Routes.INNINGS_BREAK
    MatchPhase.INNINGS_2                   -> Routes.scoring(2)
}

/**
 * The top-level composable of the whole app: it owns the screen map described above.
 * "vm" is the ViewModel, i.e. the object that holds the match data shared by every screen.
 */
@Composable
fun AppNavigation(vm: MatchViewModel) {
    // "collectAsStateWithLifecycle" = watch a live stream of data (a StateFlow) and redraw the
    // screen automatically whenever it changes, pausing while the app is in the background.
    // "ready" turns true once the saved match has been loaded from the phone's storage.
    val ready by vm.ready.collectAsStateWithLifecycle()
    val state by vm.state.collectAsStateWithLifecycle()

    // While the saved match is still loading, just paint a plain background-coloured box (the
    // splash screen is still covering it, so the user never actually sees this).
    if (!ready) {
        Box(Modifier.fillMaxSize().background(Ink50))
        return
    }

    // The "navigation controller": the remote control used to move between screens.
    // "remember" keeps it alive across redraws instead of recreating it each time.
    val nav = rememberNavController()

    // The map ALWAYS starts at HOME so HOME is guaranteed to be at the bottom of the back stack.
    // If a match was in progress, jump to its screen once, right after the map exists.
    // (LaunchedEffect(Unit) = "run this block one time when this screen first appears".)
    LaunchedEffect(Unit) {
        resumeRoute(state.phase)?.let { nav.navigate(it) { launchSingleTop = true } }
    }

    // The screen map. Each composable(route) { ... } block says "when the route is X, show screen Y".
    // The lambdas like onNewMatch = { ... } are "what to do when that button is tapped".
    // launchSingleTop = don't stack a second copy if that screen is already on top.
    NavHost(navController = nav, startDestination = Routes.HOME) {

        // HOME: the start screen. Buttons: new match, resume the match in progress, see the
        // summary of a finished match, and open the history list.
        composable(Routes.HOME) {
            HomeScreen(
                vm         = vm,
                onNewMatch = { nav.navigate(Routes.SETUP) { launchSingleTop = true } },
                onResume   = { nav.navigate(resumeRoute(state.phase) ?: Routes.SETUP) { launchSingleTop = true } },
                onSummary  = { nav.navigate(Routes.SUMMARY) { launchSingleTop = true } },
                onHistory  = { nav.navigate(Routes.HISTORY) { launchSingleTop = true } },
                onAbout    = { nav.navigate(Routes.ABOUT) { launchSingleTop = true } },
            )
        }

        // SETUP: team names, overs, rules. "Done" moves on to the toss and removes SETUP from the
        // back stack (popUpTo ... inclusive) so pressing Back from the toss can't return to it.
        composable(Routes.SETUP) {
            SetupScreen(
                vm     = vm,
                onBack = { nav.popBackStack() },
                onDone = { nav.navigate(Routes.TOSS) { popUpTo(Routes.SETUP) { inclusive = true } } },
            )
        }

        // TOSS: who won the coin toss and what they chose. Back jumps straight home (not just one
        // step). When done, TOSS is removed from the back stack and the player-entry screen opens.
        composable(Routes.TOSS) {
            TossScreen(
                vm     = vm,
                onBack = { nav.popBackStack(Routes.HOME, inclusive = false) },
                onDone = { nav.navigate(Routes.PLAYERS) { popUpTo(Routes.TOSS) { inclusive = true } } },
            )
        }

        // PLAYERS: enter the squads once, then go to live scoring for innings 1.
        composable(Routes.PLAYERS) {
            PlayersScreen(
                vm     = vm,
                onBack = { nav.popBackStack(Routes.HOME, inclusive = false) },
                onDone = { nav.navigate(Routes.scoring(1)) { popUpTo(Routes.PLAYERS) { inclusive = true } } },
            )
        }

        // SCORING: the live ball-by-ball screen. It receives which innings (1 or 2) from the route
        // ("{innings}" is a blank filled in with 1 or 2); if missing, assume innings 1.
        //
        // NOTE: popUpTo must use the route PATTERN (Routes.SCORING = "scoring/{innings}"), not a
        // concrete "scoring/1". The concrete form matches nothing, so the scoring screen used to
        // stay on the back stack and pressing Back from the summary bounced the user into it.
        composable(Routes.SCORING, listOf(navArgument("innings") { type = NavType.IntType })) { entry ->
            val n = entry.arguments?.getInt("innings") ?: 1
            ScoringScreen(n, vm) {
                // When the innings ends: after innings 1 go to the break screen, after innings 2
                // to the summary.
                val next = if (n == 1) Routes.INNINGS_BREAK else Routes.SUMMARY
                nav.navigate(next) { popUpTo(Routes.SCORING) { inclusive = true } }
            }
        }

        // INNINGS BREAK: pause between innings. "Continue" starts the chase (innings 2);
        // "Undo" goes back into innings 1 to fix the last ball.
        composable(Routes.INNINGS_BREAK) {
            InningsBreakScreen(
                vm         = vm,
                onContinue = { nav.navigate(Routes.scoring(2)) { popUpTo(Routes.INNINGS_BREAK) { inclusive = true } } },
                onUndo     = { nav.navigate(Routes.scoring(1)) { popUpTo(Routes.INNINGS_BREAK) { inclusive = true } } },
            )
        }

        // SUMMARY: the final result. "New match" wipes the whole back stack (popUpTo(0)) and
        // returns Home; "Undo" goes back into innings 2 to fix the last ball.
        composable(Routes.SUMMARY) {
            SummaryScreen(
                vm         = vm,
                onNewMatch = { nav.navigate(Routes.HOME) { popUpTo(0) { inclusive = true }; launchSingleTop = true } },
                onUndo     = { nav.navigate(Routes.scoring(2)) { popUpTo(Routes.SUMMARY) { inclusive = true } } },
            )
        }

        // HISTORY: list of past matches and the player leaderboard. Tapping a match opens its
        // detail screen, passing along its id.
        composable(Routes.HISTORY) {
            HistoryScreen(
                vm          = vm,
                onBack      = { nav.popBackStack() },
                onOpenMatch = { id -> nav.navigate(Routes.matchDetail(id)) { launchSingleTop = true } },
            )
        }

        // MATCH DETAIL: scorecard of one past match. The id (a long number) comes from the route.
        composable(Routes.MATCH_DETAIL, listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            val id = entry.arguments?.getLong("id") ?: 0L
            MatchDetailScreen(matchId = id, vm = vm, onBack = { nav.popBackStack() })
        }

        // ABOUT: app info, link to GitHub, and the list of legal documents.
        composable(Routes.ABOUT) {
            AboutScreen(
                onBack    = { nav.popBackStack() },
                onOpenDoc = { doc -> nav.navigate(Routes.doc(doc.id)) { launchSingleTop = true } },
            )
        }

        // DOCUMENT: one legal document (Privacy, Terms, ...), chosen by the id in the route.
        composable(Routes.DOC, listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            DocumentScreen(docId = entry.arguments?.getString("id") ?: "", onBack = { nav.popBackStack() })
        }
    }
}
