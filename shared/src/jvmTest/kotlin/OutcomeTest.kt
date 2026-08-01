import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

/**
 * Outcome.kt lives in the shared module's root package but was previously only exercised from
 * client:domain's test tree (19 tests there). Kover attributes coverage per-module based on where
 * the test lives, not just where the class is compiled — so shared showed 0% for this file despite
 * being thoroughly tested in practice. These are direct, dedicated tests to fix that attribution gap.
 */
class OutcomeTest {

    @Test
    fun `WHEN the Outcome is Ok THEN map transforms its value`() {
        val result = Outcome.Ok(2).map { it * 10 }

        assertThat(result).isEqualTo(Outcome.Ok(20))
    }

    @Test
    fun `WHEN the Outcome is Err THEN map is a no-op`() {
        val result: Outcome<Int, String> = Outcome.Err("boom")

        assertThat(result.map { it * 10 }).isEqualTo(Outcome.Err("boom"))
    }

    @Test
    fun `WHEN the Outcome is Err THEN mapError transforms its value`() {
        val result: Outcome<Int, String> = Outcome.Err("boom")

        assertThat(result.mapError { it.uppercase() }).isEqualTo(Outcome.Err("BOOM"))
    }

    @Test
    fun `WHEN the Outcome is Ok THEN flatMap chains to the next result`() {
        val result = Outcome.Ok(2).flatMap { Outcome.Ok(it + 1) }

        assertThat(result).isEqualTo(Outcome.Ok(3))
    }

    @Test
    fun `WHEN the Outcome is Err THEN flatMap short-circuits`() {
        val result: Outcome<Int, String> = Outcome.Err("boom")

        assertThat(result.flatMap { Outcome.Ok(it + 1) }).isEqualTo(Outcome.Err("boom"))
    }

    @Test
    fun `WHEN the Outcome is Ok THEN onSuccess runs its action`() {
        var invoked = false
        Outcome.Ok(1).onSuccess { invoked = true }

        assertThat(invoked).isEqualTo(true)
    }

    @Test
    fun `WHEN the Outcome is Err THEN onFailure runs its action`() {
        var invoked = false
        val result: Outcome<Int, String> = Outcome.Err("boom")
        result.onFailure { invoked = true }

        assertThat(invoked).isEqualTo(true)
    }

    @Test
    fun `WHEN the Outcome is Ok THEN fold resolves the onSuccess branch`() {
        val result = Outcome.Ok(5).fold(onSuccess = { it * 2 }, onFailure = { -1 })

        assertThat(result).isEqualTo(10)
    }

    @Test
    fun `WHEN the Outcome is Err THEN fold resolves the onFailure branch`() {
        val outcome: Outcome<Int, String> = Outcome.Err("boom")
        val result = outcome.fold(onSuccess = { it * 2 }, onFailure = { -1 })

        assertThat(result).isEqualTo(-1)
    }

    @Test
    fun `WHEN the Outcome is Ok THEN getOrNull returns the value`() {
        assertThat(Outcome.Ok(7).getOrNull()).isEqualTo(7)
    }

    @Test
    fun `WHEN the Outcome is Err THEN getOrNull returns null`() {
        val outcome: Outcome<Int, String> = Outcome.Err("boom")

        assertThat(outcome.getOrNull()).isEqualTo(null)
    }

    @Test
    fun `WHEN the Outcome is Ok THEN getOrElse returns the value`() {
        assertThat(Outcome.Ok(7).getOrElse { 0 }).isEqualTo(7)
    }

    @Test
    fun `WHEN the Outcome is Err THEN getOrElse computes a fallback`() {
        val outcome: Outcome<Int, String> = Outcome.Err("boom")

        assertThat(outcome.getOrElse { it.length }).isEqualTo(4)
    }

    @Test
    fun `WHEN the Outcome is Err THEN recover turns it into an Ok`() {
        val outcome: Outcome<Int, String> = Outcome.Err("boom")

        assertThat(outcome.recover { it.length }).isEqualTo(Outcome.Ok(4))
    }

    @Test
    fun `WHEN the Outcome is Ok THEN recover leaves it untouched`() {
        val outcome: Outcome<Int, String> = Outcome.Ok(7)

        assertThat(outcome.recover { 0 }).isEqualTo(Outcome.Ok(7))
    }
}
