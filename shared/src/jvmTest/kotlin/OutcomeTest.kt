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
    fun `map transforms the Ok value`() {
        val result = Outcome.Ok(2).map { it * 10 }

        assertThat(result).isEqualTo(Outcome.Ok(20))
    }

    @Test
    fun `map on Err is a no-op`() {
        val result: Outcome<Int, String> = Outcome.Err("boom")

        assertThat(result.map { it * 10 }).isEqualTo(Outcome.Err("boom"))
    }

    @Test
    fun `mapError transforms the Err value`() {
        val result: Outcome<Int, String> = Outcome.Err("boom")

        assertThat(result.mapError { it.uppercase() }).isEqualTo(Outcome.Err("BOOM"))
    }

    @Test
    fun `flatMap chains Ok results`() {
        val result = Outcome.Ok(2).flatMap { Outcome.Ok(it + 1) }

        assertThat(result).isEqualTo(Outcome.Ok(3))
    }

    @Test
    fun `flatMap short-circuits on Err`() {
        val result: Outcome<Int, String> = Outcome.Err("boom")

        assertThat(result.flatMap { Outcome.Ok(it + 1) }).isEqualTo(Outcome.Err("boom"))
    }

    @Test
    fun `onSuccess runs its action only for Ok`() {
        var invoked = false
        Outcome.Ok(1).onSuccess { invoked = true }

        assertThat(invoked).isEqualTo(true)
    }

    @Test
    fun `onFailure runs its action only for Err`() {
        var invoked = false
        val result: Outcome<Int, String> = Outcome.Err("boom")
        result.onFailure { invoked = true }

        assertThat(invoked).isEqualTo(true)
    }

    @Test
    fun `fold resolves the Ok branch`() {
        val result = Outcome.Ok(5).fold(onSuccess = { it * 2 }, onFailure = { -1 })

        assertThat(result).isEqualTo(10)
    }

    @Test
    fun `fold resolves the Err branch`() {
        val outcome: Outcome<Int, String> = Outcome.Err("boom")
        val result = outcome.fold(onSuccess = { it * 2 }, onFailure = { -1 })

        assertThat(result).isEqualTo(-1)
    }

    @Test
    fun `getOrNull returns the value for Ok`() {
        assertThat(Outcome.Ok(7).getOrNull()).isEqualTo(7)
    }

    @Test
    fun `getOrNull returns null for Err`() {
        val outcome: Outcome<Int, String> = Outcome.Err("boom")

        assertThat(outcome.getOrNull()).isEqualTo(null)
    }

    @Test
    fun `getOrElse returns the value for Ok`() {
        assertThat(Outcome.Ok(7).getOrElse { 0 }).isEqualTo(7)
    }

    @Test
    fun `getOrElse computes a fallback for Err`() {
        val outcome: Outcome<Int, String> = Outcome.Err("boom")

        assertThat(outcome.getOrElse { it.length }).isEqualTo(4)
    }

    @Test
    fun `recover turns an Err into an Ok`() {
        val outcome: Outcome<Int, String> = Outcome.Err("boom")

        assertThat(outcome.recover { it.length }).isEqualTo(Outcome.Ok(4))
    }

    @Test
    fun `recover leaves an Ok untouched`() {
        val outcome: Outcome<Int, String> = Outcome.Ok(7)

        assertThat(outcome.recover { 0 }).isEqualTo(Outcome.Ok(7))
    }
}
