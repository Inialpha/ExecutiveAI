package com.inialpha.executiveai.data.remote.google

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

/**
 * Used immediately after [com.inialpha.executiveai.data.auth.GoogleAuthManager] obtains an
 * access token, to identify *which* Google account the user just granted access to — Android's
 * AuthorizationClient does not directly hand back the account's email address, so we resolve it
 * ourselves. Requires only the standard "openid email profile" scopes which are implicitly
 * available once any scope has been granted for the account.
 */
interface UserInfoApi {
    @GET("oauth2/v3/userinfo")
    suspend fun getUserInfo(@Header("Authorization") bearerToken: String): Response<UserInfoDto>

    companion object {
        const val BASE_URL = "https://www.googleapis.com/"
    }
}

@Serializable
data class UserInfoDto(
    @SerialName("sub") val sub: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("picture") val picture: String? = null,
)
