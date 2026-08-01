import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import org.junit.jupiter.api.Test

class OutcomeTest {

    private val ok: Outcome<Int, String> = Outcome.Ok(1)
    private val err: Outcome<Int, String> = Outcome.Err("boom")

    @Test
    fun `WHEN Outcome is Ok THEN map transforms the value`() {
        assertThat(ok.map { it + 1 }).isEqualTo(Outcome.Ok(2))
    }

    @Test
    fun `WHEN Outcome is Err THEN map leaves it untouched`() {
        assertThat(err.map { it + 1 }).isEqualTo(Outcome.Err("boom"))
    }

    @Test
    fun `WHEN Outcome is Err THEN mapError transforms the error`() {
        assertThat(err.mapError { "$it!" }).isEqualTo(Outcome.Err("boom!"))
    }

    @Test
    fun `WHEN Outcome is Ok THEN mapError leaves it untouched`() {
        assertThat(ok.mapError { "$it!" }).isEqualTo(Outcome.Ok(1))
    }

    @Test
    fun `WHEN Outcome is Ok and transform returns Ok THEN flatMap chains into that Ok`() {
        assertThat(ok.flatMap { Outcome.Ok(it + 10) }).isEqualTo(Outcome.Ok(11))
    }

    @Test
    fun `WHEN Outcome is Ok and transform returns Err THEN flatMap chains into that Err`() {
        assertThat(ok.flatMap { Outcome.Err("nested failure") }).isEqualTo(Outcome.Err("nested failure"))
    }

    @Test
    fun `WHEN Outcome is Err THEN flatMap short-circuits without invoking the transform`() {
        var invoked = false
        val result = err.flatMap { invoked = true; Outcome.Ok(it) }

        assertThat(result).isEqualTo(Outcome.Err("boom"))
        assertThat(invoked).isEqualTo(false)
    }

    @Test
    fun `WHEN Outcome is Ok THEN onSuccess runs the action and returns the same outcome`() {
        var captured: Int? = null
        val result = ok.onSuccess { captured = it }

        assertThat(captured).isEqualTo(1)
        assertThat(result).isEqualTo(ok)
    }

    @Test
    fun `WHEN Outcome is Err THEN onSuccess does not run`() {
        var invoked = false
        err.onSuccess { invoked = true }

        assertThat(invoked).isEqualTo(false)
    }

    @Test
    fun `WHEN Outcome is Err THEN onFailure runs the action and returns the same outcome`() {
        var captured: String? = null
        val result = err.onFailure { captured = it }

        assertThat(captured).isEqualTo("boom")
        assertThat(result).isEqualTo(err)
    }

    @Test
    fun `WHEN Outcome is Ok THEN onFailure does not run`() {
        var invoked = false
        ok.onFailure { invoked = true }

        assertThat(invoked).isEqualTo(false)
    }

    @Test
    fun `WHEN Outcome is Ok THEN fold applies the onSuccess branch`() {
        val result = ok.fold(onSuccess = { "value:$it" }, onFailure = { "error:$it" })

        assertThat(result).isEqualTo("value:1")
    }

    @Test
    fun `WHEN Outcome is Err THEN fold applies the onFailure branch`() {
        val result = err.fold(onSuccess = { "value:$it" }, onFailure = { "error:$it" })

        assertThat(result).isEqualTo("error:boom")
    }

    @Test
    fun `WHEN Outcome is Ok THEN getOrNull returns the value`() {
        assertThat(ok.getOrNull()).isEqualTo(1)
    }

    @Test
    fun `WHEN Outcome is Err THEN getOrNull returns null`() {
        assertThat(err.getOrNull()).isNull()
    }

    @Test
    fun `WHEN Outcome is Ok THEN getOrElse returns the value without invoking the fallback`() {
        var invoked = false
        val result = ok.getOrElse { invoked = true; -1 }

        assertThat(result).isEqualTo(1)
        assertThat(invoked).isEqualTo(false)
    }

    @Test
    fun `WHEN Outcome is Err THEN getOrElse invokes the fallback`() {
        val result = err.getOrElse { -1 }

        assertThat(result).isEqualTo(-1)
    }

    @Test
    fun `WHEN Outcome is Err THEN recover turns it into Ok using the transform`() {
        val result = err.recover { "recovered from $it" }

        assertThat(result).isEqualTo(Outcome.Ok("recovered from boom"))
    }

    @Test
    fun `WHEN Outcome is Ok THEN recover leaves it untouched`() {
        val result = ok.recover { -1 }

        assertThat(result).isEqualTo(Outcome.Ok(1))
    }
}
