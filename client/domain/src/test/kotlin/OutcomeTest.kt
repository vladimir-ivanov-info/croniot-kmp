import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import org.junit.jupiter.api.Test

class OutcomeTest {

    private val ok: Outcome<Int, String> = Outcome.Ok(1)
    private val err: Outcome<Int, String> = Outcome.Err("boom")

    @Test
    fun `map transforms the value of Ok`() {
        assertThat(ok.map { it + 1 }).isEqualTo(Outcome.Ok(2))
    }

    @Test
    fun `map leaves Err untouched`() {
        assertThat(err.map { it + 1 }).isEqualTo(Outcome.Err("boom"))
    }

    @Test
    fun `mapError transforms the error of Err`() {
        assertThat(err.mapError { "$it!" }).isEqualTo(Outcome.Err("boom!"))
    }

    @Test
    fun `mapError leaves Ok untouched`() {
        assertThat(ok.mapError { "$it!" }).isEqualTo(Outcome.Ok(1))
    }

    @Test
    fun `flatMap chains into another Ok`() {
        assertThat(ok.flatMap { Outcome.Ok(it + 10) }).isEqualTo(Outcome.Ok(11))
    }

    @Test
    fun `flatMap chains into an Err`() {
        assertThat(ok.flatMap { Outcome.Err("nested failure") }).isEqualTo(Outcome.Err("nested failure"))
    }

    @Test
    fun `flatMap short-circuits on Err without invoking the transform`() {
        var invoked = false
        val result = err.flatMap { invoked = true; Outcome.Ok(it) }

        assertThat(result).isEqualTo(Outcome.Err("boom"))
        assertThat(invoked).isEqualTo(false)
    }

    @Test
    fun `onSuccess runs the action for Ok and returns the same outcome`() {
        var captured: Int? = null
        val result = ok.onSuccess { captured = it }

        assertThat(captured).isEqualTo(1)
        assertThat(result).isEqualTo(ok)
    }

    @Test
    fun `onSuccess does not run for Err`() {
        var invoked = false
        err.onSuccess { invoked = true }

        assertThat(invoked).isEqualTo(false)
    }

    @Test
    fun `onFailure runs the action for Err and returns the same outcome`() {
        var captured: String? = null
        val result = err.onFailure { captured = it }

        assertThat(captured).isEqualTo("boom")
        assertThat(result).isEqualTo(err)
    }

    @Test
    fun `onFailure does not run for Ok`() {
        var invoked = false
        ok.onFailure { invoked = true }

        assertThat(invoked).isEqualTo(false)
    }

    @Test
    fun `fold applies onSuccess branch for Ok`() {
        val result = ok.fold(onSuccess = { "value:$it" }, onFailure = { "error:$it" })

        assertThat(result).isEqualTo("value:1")
    }

    @Test
    fun `fold applies onFailure branch for Err`() {
        val result = err.fold(onSuccess = { "value:$it" }, onFailure = { "error:$it" })

        assertThat(result).isEqualTo("error:boom")
    }

    @Test
    fun `getOrNull returns the value for Ok`() {
        assertThat(ok.getOrNull()).isEqualTo(1)
    }

    @Test
    fun `getOrNull returns null for Err`() {
        assertThat(err.getOrNull()).isNull()
    }

    @Test
    fun `getOrElse returns the value for Ok without invoking the fallback`() {
        var invoked = false
        val result = ok.getOrElse { invoked = true; -1 }

        assertThat(result).isEqualTo(1)
        assertThat(invoked).isEqualTo(false)
    }

    @Test
    fun `getOrElse invokes the fallback for Err`() {
        val result = err.getOrElse { -1 }

        assertThat(result).isEqualTo(-1)
    }

    @Test
    fun `recover turns Err into Ok using the transform`() {
        val result = err.recover { "recovered from $it" }

        assertThat(result).isEqualTo(Outcome.Ok("recovered from boom"))
    }

    @Test
    fun `recover leaves Ok untouched`() {
        val result = ok.recover { -1 }

        assertThat(result).isEqualTo(Outcome.Ok(1))
    }
}
