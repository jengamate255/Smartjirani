package com.smartjirani.core.navigation

sealed class Screen(
    val route: String,
) {
    data object Splash : Screen("splash")

    data object Login : Screen("login")

    data object Register : Screen("register")

    data object ProfileSetup : Screen("profile_setup")

    data object NeighborhoodSelection : Screen("neighborhood_selection")

    data object Feed : Screen("feed")

    data object Ujamaa : Screen("ujamaa")

    data object Baraza : Screen("baraza")

    data object Explore : Screen("explore")

    data object CreatePost : Screen("create_post")

    data object Calls : Screen("calls")

    data object Profile : Screen("profile")

    data object Notifications : Screen("notifications")

    data object PostDetail : Screen("post/{postId}") {
        fun createRoute(postId: String) = "post/$postId"
    }

    data object Comments : Screen("comments/{postId}") {
        fun createRoute(postId: String) = "comments/$postId"
    }

    data object VoiceMessage : Screen("voice_message/{userId}") {
        fun createRoute(userId: String) = "voice_message/$userId"
    }

    data object CallScreen : Screen("call/{callId}") {
        fun createRoute(callId: String) = "call/$callId"
    }

    data object ConferenceScreen : Screen("conference/{conferenceId}") {
        fun createRoute(conferenceId: String) = "conference/$conferenceId"
    }

    data object CreateConference : Screen("create_conference")

    data object MtaaMap : Screen("mtaa_map")

    data object Messages : Screen("messages")

    data object Chat : Screen("chat/{conversationId}") {
        fun createRoute(conversationId: String) = "chat/$conversationId"
    }

    data object CivicHub : Screen("civic_hub")

    data object CivicReportDetail : Screen("civic_report/{reportId}") {
        fun createRoute(reportId: String) = "civic_report/$reportId"
    }

    data object CivicMap : Screen("civic_map")

    data object CivicNewReport : Screen("civic_new_report")

    data object CivicMyReports : Screen("civic_my_reports")

    data object CivicAssistant : Screen("civic_assistant")

    data object CivicAdmin : Screen("civic_admin")

    data object DepartmentOfficerConsole : Screen("department_officer_console")
}
