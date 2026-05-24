package com.server.croniot.data.db.daos

import com.server.croniot.data.db.entities.FeatureFlagEntity
import com.server.croniot.jooq.tables.FeatureFlag.Companion.FEATURE_FLAG
import org.jooq.DSLContext
import org.jooq.impl.DSL.currentOffsetDateTime
import javax.inject.Inject

class FeatureFlagJooqDaoImpl @Inject constructor(
    private val dsl: DSLContext,
) : FeatureFlagDao {

    override fun getAll(): List<FeatureFlagEntity> =
        dsl.selectFrom(FEATURE_FLAG)
            .orderBy(FEATURE_FLAG.NAME.asc())
            .fetch { rec ->
                FeatureFlagEntity(
                    name = rec.name!!,
                    enabled = rec.enabled!!,
                    description = rec.description,
                    updatedAt = rec.updatedAt!!,
                )
            }

    override fun getByName(name: String): FeatureFlagEntity? =
        dsl.selectFrom(FEATURE_FLAG)
            .where(FEATURE_FLAG.NAME.eq(name))
            .fetchOne()
            ?.let { rec ->
                FeatureFlagEntity(
                    name = rec.name!!,
                    enabled = rec.enabled!!,
                    description = rec.description,
                    updatedAt = rec.updatedAt!!,
                )
            }

    override fun setEnabled(name: String, enabled: Boolean): Boolean =
        dsl.update(FEATURE_FLAG)
            .set(FEATURE_FLAG.ENABLED, enabled)
            .set(FEATURE_FLAG.UPDATED_AT, currentOffsetDateTime())
            .where(FEATURE_FLAG.NAME.eq(name))
            .execute() > 0
}
