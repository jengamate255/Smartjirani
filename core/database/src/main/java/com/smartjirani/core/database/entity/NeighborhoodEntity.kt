package com.smartjirani.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "neighborhoods")
data class NeighborhoodEntity(
    @PrimaryKey val id: String,
    val name: String,
    val city: String,
    val region: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Double,
    val createdAt: Long,
    val updatedAt: Long,
)
