package org.fppuck.community.ui.screens

enum class AppScreen(val sidebarLabel: String? = null) {
    HOME,
    SCHEDULE,
    TEAMS,
    TEAM_DETAILS,
    GAME_DETAILS,
    STANDINGS,
    MORE("Glossary"),
    PLAYER_DETAILS,
    APPEARANCE("Appearance"),
    FAVORITES("Manage Favorites"),
    PATCH_NOTES("Patch Notes"),
    SUPPORT("Help"),
    ABOUT("About FPPuck"),
    PRIVACY("Privacy Policy"),
}
