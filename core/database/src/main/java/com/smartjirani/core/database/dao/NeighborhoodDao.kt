package com.smartjirani.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.smartjirani.core.database.entity.NeighborhoodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NeighborhoodDao {
    @Query("SELECT * FROM neighborhoods")
    fun getAllNeighborhoods(): Flow<List<NeighborhoodEntity>>

    @Query("SELECT * FROM neighborhoods WHERE id = :neighborhoodId")
    fun getNeighborhoodById(neighborhoodId: String): Flow<NeighborhoodEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNeighborhoods(neighborhoods: List<NeighborhoodEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNeighborhood(neighborhood: NeighborhoodEntity)
}
