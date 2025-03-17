package com.williamfq.data.converters

import androidx.room.TypeConverter
import com.google.gson.GsonBuilder
import com.williamfq.domain.model.EncryptionType
import com.williamfq.domain.model.MediaType
import com.williamfq.domain.model.PrivacyLevel
import com.williamfq.domain.model.ProcessingStatus
import com.williamfq.domain.model.StoryCategory
import timber.log.Timber

class StoryConverters {
    private val gson by lazy {
        GsonBuilder()
            .serializeNulls()
            .setLenient()
            .create()
    }

    @TypeConverter
    fun fromMediaType(value: MediaType?): String =
        value?.name ?: MediaType.TEXT.name

    @TypeConverter
    fun toMediaType(value: String?): MediaType = try {
        value?.let { MediaType.valueOf(it.uppercase()) } ?: MediaType.TEXT
    } catch (e: IllegalArgumentException) {
        Timber.e(e, "Error converting MediaType: $value")
        MediaType.TEXT
    }

    @TypeConverter
    fun fromEncryptionType(value: EncryptionType?): String? =
        value?.name

    @TypeConverter
    fun toEncryptionType(value: String?): EncryptionType? = try {
        value?.let { EncryptionType.valueOf(it.uppercase()) }
    } catch (e: IllegalArgumentException) {
        Timber.e(e, "Error converting EncryptionType: $value")
        null
    }

    @TypeConverter
    fun fromPrivacyLevel(value: PrivacyLevel?): String =
        value?.name ?: PrivacyLevel.PUBLIC.name

    @TypeConverter
    fun toPrivacyLevel(value: String?): PrivacyLevel = try {
        value?.let { PrivacyLevel.valueOf(it.uppercase()) } ?: PrivacyLevel.PUBLIC
    } catch (e: IllegalArgumentException) {
        Timber.e(e, "Error converting PrivacyLevel: $value")
        PrivacyLevel.PUBLIC
    }

    @TypeConverter
    fun fromStoryCategory(value: StoryCategory?): String =
        value?.name ?: StoryCategory.GENERAL.name

    @TypeConverter
    fun toStoryCategory(value: String?): StoryCategory = try {
        value?.let { StoryCategory.valueOf(it.uppercase()) } ?: StoryCategory.GENERAL
    } catch (e: IllegalArgumentException) {
        Timber.e(e, "Error converting StoryCategory: $value")
        StoryCategory.GENERAL
    }

    @TypeConverter
    fun fromProcessingStatus(value: ProcessingStatus?): String =
        value?.name ?: ProcessingStatus.COMPLETED.name

    @TypeConverter
    fun toProcessingStatus(value: String?): ProcessingStatus = try {
        value?.let { ProcessingStatus.valueOf(it.uppercase()) } ?: ProcessingStatus.COMPLETED
    } catch (e: IllegalArgumentException) {
        Timber.e(e, "Error converting ProcessingStatus: $value")
        ProcessingStatus.COMPLETED
    }
}