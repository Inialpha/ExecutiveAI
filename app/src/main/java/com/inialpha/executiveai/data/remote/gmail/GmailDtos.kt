package com.inialpha.executiveai.data.remote.gmail

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GmailMessageListResponseDto(
    @SerialName("messages") val messages: List<GmailMessageRefDto> = emptyList(),
    @SerialName("nextPageToken") val nextPageToken: String? = null,
    @SerialName("resultSizeEstimate") val resultSizeEstimate: Int = 0,
)

@Serializable
data class GmailMessageRefDto(
    @SerialName("id") val id: String,
    @SerialName("threadId") val threadId: String,
)

@Serializable
data class GmailMessageDto(
    @SerialName("id") val id: String,
    @SerialName("threadId") val threadId: String,
    @SerialName("labelIds") val labelIds: List<String> = emptyList(),
    @SerialName("snippet") val snippet: String = "",
    @SerialName("internalDate") val internalDate: String? = null,
    @SerialName("payload") val payload: GmailPayloadDto? = null,
)

@Serializable
data class GmailPayloadDto(
    @SerialName("mimeType") val mimeType: String? = null,
    @SerialName("headers") val headers: List<GmailHeaderDto> = emptyList(),
    @SerialName("body") val body: GmailBodyDto? = null,
    @SerialName("parts") val parts: List<GmailPayloadDto> = emptyList(),
)

@Serializable
data class GmailHeaderDto(
    @SerialName("name") val name: String,
    @SerialName("value") val value: String,
)

@Serializable
data class GmailBodyDto(
    @SerialName("size") val size: Int = 0,
    @SerialName("data") val data: String? = null,
)
