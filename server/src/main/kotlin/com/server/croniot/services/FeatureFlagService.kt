package com.server.croniot.services

import com.server.croniot.application.DomainException
import com.server.croniot.data.repositories.FeatureFlagRepository
import croniot.models.dto.FeatureFlagDto
import croniot.models.errors.DomainError
import javax.inject.Inject

class FeatureFlagService @Inject constructor(
    private val repo: FeatureFlagRepository,
) {
    fun getAll(): List<FeatureFlagDto> =
        repo.getAll().map { FeatureFlagDto(it.name, it.enabled, it.description) }

    fun setEnabled(name: String, enabled: Boolean): FeatureFlagDto {
        val updated = repo.setEnabled(name, enabled)
        if (!updated) throw DomainException(DomainError.NotFound("feature_flag '$name'"))
        val entity = repo.getByName(name)!!
        return FeatureFlagDto(entity.name, entity.enabled, entity.description)
    }
}
