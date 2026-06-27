package com.server.croniot.data.repositories

import com.server.croniot.data.db.daos.FeatureFlagDao
import com.server.croniot.data.db.entities.FeatureFlagEntity
import javax.inject.Inject

class FeatureFlagRepository @Inject constructor(
    private val featureFlagDao: FeatureFlagDao,
) {
    fun getAll(): List<FeatureFlagEntity> = featureFlagDao.getAll()
    fun getByName(name: String): FeatureFlagEntity? = featureFlagDao.getByName(name)
    fun setEnabled(name: String, enabled: Boolean): Boolean = featureFlagDao.setEnabled(name, enabled)
}
