package com.example.mungnyang.model

sealed class Routes(
    val route: String,
    val isRoot: Boolean = true,
    val isCamera: Boolean = false
) {

    data object Welcome: Routes("Welcome", false)
    data object Login: Routes("Login", false)

    data object Main: Routes("Main")

    data object AiHealth: Routes("AiHealth")
    data object AiCameraDogEye: Routes("AiCameraDogEye", true, true)
    data class AiCheckDogEye(val imageData: ByteArray? = null): Routes("AiCheckDogEye", true) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as AiCheckDogEye
            if (imageData != null) {
                if (other.imageData == null) return false
                if (!imageData.contentEquals(other.imageData)) return false
            } else if (other.imageData != null) return false
            return true
        }

        override fun hashCode(): Int {
            return imageData?.contentHashCode() ?: 0
        }
    }
    data object AiCameraDogSkin: Routes("AiCameraDogSkin", true, true)
    data object AiCheckDogSkin: Routes("AiCheckDogSkin", true)

    data object PetCalendar: Routes("PetCalendar")
    data object PetWalk: Routes("PetWalk")
    data object PetLog: Routes("PetLog")

    companion object {
        fun getRoutes(route: String): Routes {
            return when {
                route == Welcome.route -> Welcome
                route == Login.route -> Login
                route == AiHealth.route -> AiHealth
                route == AiCameraDogEye.route -> AiCameraDogEye
                route.startsWith("AiCheckDogEye") -> AiCheckDogEye()
                route == AiCameraDogSkin.route -> AiCameraDogSkin
                route == AiCheckDogSkin.route -> AiCheckDogSkin
                route == PetCalendar.route -> PetCalendar
                route == PetWalk.route -> PetWalk
                route == PetLog.route -> PetLog
                else -> AiHealth
            }
        }
    }
}