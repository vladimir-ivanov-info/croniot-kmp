package com.croniot.client.domain.usecases

import Outcome
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.croniot.client.domain.models.FeatureFlagError
import com.croniot.testing.fakes.FakeFeatureFlagRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class FetchFeatureFlagsUseCaseTest {

    @Test
    fun `WHEN repository fetch succeeds THEN it returns Ok`() = runTest {
        val repository = FakeFeatureFlagRepository(fetchAndCacheOutcome = Outcome.Ok(Unit))
        val useCase = FetchFeatureFlagsUseCase(repository)

        val result = useCase()

        assertThat(result).isEqualTo(Outcome.Ok(Unit))
    }

    @Test
    fun `WHEN repository returns a network error THEN it propagates that error`() = runTest {
        val repository = FakeFeatureFlagRepository(fetchAndCacheOutcome = Outcome.Err(FeatureFlagError.Network))
        val useCase = FetchFeatureFlagsUseCase(repository)

        val result = useCase()

        assertThat(result).isEqualTo(Outcome.Err(FeatureFlagError.Network))
    }

    @Test
    fun `WHEN repository returns an unknown error THEN it propagates that error`() = runTest {
        val repository = FakeFeatureFlagRepository(fetchAndCacheOutcome = Outcome.Err(FeatureFlagError.Unknown))
        val useCase = FetchFeatureFlagsUseCase(repository)

        val result = useCase()

        assertThat(result).isEqualTo(Outcome.Err(FeatureFlagError.Unknown))
    }

    @Test
    fun `WHEN the use case is invoked THEN it calls fetchAndCache exactly once`() = runTest {
        val repository = FakeFeatureFlagRepository()
        val useCase = FetchFeatureFlagsUseCase(repository)

        useCase()

        assertThat(repository.fetchAndCacheCalls).isEqualTo(1)
    }
}
