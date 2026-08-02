package com.server.croniot.testsupport.fakes

import com.server.croniot.data.db.daos.FeatureFlagDao
import com.server.croniot.data.db.entities.FeatureFlagEntity

class FakeFeatureFlagDao : FeatureFlagDao {

    private val byName = mutableMapOf<String, FeatureFlagEntity>()

    fun seed(entity: FeatureFlagEntity) {
        byName[entity.name] = entity
    }

    override fun getAll(): List<FeatureFlagEntity> = byName.values.toList()

    override fun getByName(name: String): FeatureFlagEntity? = byName[name]

    override fun setEnabled(name: String, enabled: Boolean): Boolean {
        val existing = byName[name] ?: return false
        byName[name] = existing.copy(enabled = enabled)
        return true
    }
}
