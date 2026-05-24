package com.server.croniot.data.db.daos

import com.server.croniot.data.db.entities.FeatureFlagEntity

interface FeatureFlagDao {
    fun getAll(): List<FeatureFlagEntity>
    fun getByName(name: String): FeatureFlagEntity?
    fun setEnabled(name: String, enabled: Boolean): Boolean
}
