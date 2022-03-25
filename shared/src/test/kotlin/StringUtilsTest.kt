import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import pl.pawelkielb.fchat.StringUtils

class StringUtilsTest : WordSpec({
    "increment()" should {
        "append '(1)' when it's first increment" {
            StringUtils.increment("name") shouldBe "name (1)"
        }

        "return '(n+1)' when '(n)' is passed" {
            StringUtils.increment("name (5)") shouldBe "name (6)"
        }

        "increment only last '(n)'" {
            StringUtils.increment("name (5)(1)") shouldBe "name (5)(2)"
        }

        "work for long numbers" {
            StringUtils.increment("name (325)") shouldBe "name (326)"
        }
    }
})
