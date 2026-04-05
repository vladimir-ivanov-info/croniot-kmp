package com.croniot.client.data.source.local.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.croniot.client.data.source.local.database.entities.TaskHistoryCacheEntity

@Dao
interface TaskHistoryCacheDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entities: List<TaskHistoryCacheEntity>)

    @Query(
        """
        SELECT * FROM task_history_cache
        WHERE deviceUuid = :deviceUuid
          AND (
              :beforeMillis IS NULL
              OR timeStampMillis < :beforeMillis
              OR (
                  timeStampMillis = :beforeMillis
                  AND (CASE WHEN stateInfoId IS NOT NULL THEN stateInfoId ELSE -id END) < :beforeId
              )
          )
        ORDER BY timeStampMillis DESC,
                 (CASE WHEN stateInfoId IS NOT NULL THEN stateInfoId ELSE -id END) DESC
        LIMIT :limit
        """,
    )
    suspend fun getPage(
        deviceUuid: String,
        limit: Int,
        beforeMillis: Long?,
        beforeId: Long,
    ): List<TaskHistoryCacheEntity>

    @Query(
        """
        SELECT COUNT(*) FROM task_history_cache
        WHERE deviceUuid = :deviceUuid
          AND (
              :beforeMillis IS NULL
              OR timeStampMillis < :beforeMillis
              OR (
                  timeStampMillis = :beforeMillis
                  AND (CASE WHEN stateInfoId IS NOT NULL THEN stateInfoId ELSE -id END) < :beforeId
              )
          )
        """,
    )
    suspend fun count(
        deviceUuid: String,
        beforeMillis: Long?,
        beforeId: Long,
    ): Int

    // --- Filtered queries (taskTypeUids non-empty, optionally with date range) ---

    @Query(
        """
        SELECT * FROM task_history_cache
        WHERE deviceUuid = :deviceUuid
          AND taskTypeUid IN (:taskTypeUids)
          AND (:dateFromMillis IS NULL OR timeStampMillis >= :dateFromMillis)
          AND (:dateToMillis IS NULL OR timeStampMillis <= :dateToMillis)
          AND (
              :beforeMillis IS NULL
              OR timeStampMillis < :beforeMillis
              OR (
                  timeStampMillis = :beforeMillis
                  AND (CASE WHEN stateInfoId IS NOT NULL THEN stateInfoId ELSE -id END) < :beforeId
              )
          )
        ORDER BY timeStampMillis DESC,
                 (CASE WHEN stateInfoId IS NOT NULL THEN stateInfoId ELSE -id END) DESC
        LIMIT :limit
        """,
    )
    suspend fun getPageFilteredByTaskTypes(
        deviceUuid: String,
        limit: Int,
        beforeMillis: Long?,
        beforeId: Long,
        taskTypeUids: List<Long>,
        dateFromMillis: Long?,
        dateToMillis: Long?,
    ): List<TaskHistoryCacheEntity>

    @Query(
        """
        SELECT COUNT(*) FROM task_history_cache
        WHERE deviceUuid = :deviceUuid
          AND taskTypeUid IN (:taskTypeUids)
          AND (:dateFromMillis IS NULL OR timeStampMillis >= :dateFromMillis)
          AND (:dateToMillis IS NULL OR timeStampMillis <= :dateToMillis)
          AND (
              :beforeMillis IS NULL
              OR timeStampMillis < :beforeMillis
              OR (
                  timeStampMillis = :beforeMillis
                  AND (CASE WHEN stateInfoId IS NOT NULL THEN stateInfoId ELSE -id END) < :beforeId
              )
          )
        """,
    )
    suspend fun countFilteredByTaskTypes(
        deviceUuid: String,
        beforeMillis: Long?,
        beforeId: Long,
        taskTypeUids: List<Long>,
        dateFromMillis: Long?,
        dateToMillis: Long?,
    ): Int

    // --- Filtered queries (date range only, no taskType filter) ---

    @Query(
        """
        SELECT * FROM task_history_cache
        WHERE deviceUuid = :deviceUuid
          AND (:dateFromMillis IS NULL OR timeStampMillis >= :dateFromMillis)
          AND (:dateToMillis IS NULL OR timeStampMillis <= :dateToMillis)
          AND (
              :beforeMillis IS NULL
              OR timeStampMillis < :beforeMillis
              OR (
                  timeStampMillis = :beforeMillis
                  AND (CASE WHEN stateInfoId IS NOT NULL THEN stateInfoId ELSE -id END) < :beforeId
              )
          )
        ORDER BY timeStampMillis DESC,
                 (CASE WHEN stateInfoId IS NOT NULL THEN stateInfoId ELSE -id END) DESC
        LIMIT :limit
        """,
    )
    suspend fun getPageFilteredByDates(
        deviceUuid: String,
        limit: Int,
        beforeMillis: Long?,
        beforeId: Long,
        dateFromMillis: Long?,
        dateToMillis: Long?,
    ): List<TaskHistoryCacheEntity>

    @Query(
        """
        SELECT COUNT(*) FROM task_history_cache
        WHERE deviceUuid = :deviceUuid
          AND (:dateFromMillis IS NULL OR timeStampMillis >= :dateFromMillis)
          AND (:dateToMillis IS NULL OR timeStampMillis <= :dateToMillis)
          AND (
              :beforeMillis IS NULL
              OR timeStampMillis < :beforeMillis
              OR (
                  timeStampMillis = :beforeMillis
                  AND (CASE WHEN stateInfoId IS NOT NULL THEN stateInfoId ELSE -id END) < :beforeId
              )
          )
        """,
    )
    suspend fun countFilteredByDates(
        deviceUuid: String,
        beforeMillis: Long?,
        beforeId: Long,
        dateFromMillis: Long?,
        dateToMillis: Long?,
    ): Int
}
