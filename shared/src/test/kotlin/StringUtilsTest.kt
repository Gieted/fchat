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

    "incrementFileName()" should {
        "append '(1)' when it's first increment" {
            StringUtils.incrementFileName("name.ext") shouldBe "name (1).ext"
        }

        "return '(n+1)' when '(n)' is passed" {
            StringUtils.incrementFileName("name (5).ext") shouldBe "name (6).ext"
        }

        "increment only last '(n)'" {
            StringUtils.incrementFileName("name (5)(1).ext") shouldBe "name (5)(2).ext"
        }

        "work for long numbers" {
            StringUtils.incrementFileName("name (325).ext") shouldBe "name (326).ext"
        }

        "work when no extension is provided" {
            StringUtils.incrementFileName("name") shouldBe "name (1)"
        }

        "work with multiple dots" {
            StringUtils.incrementFileName("name.ext.ext2") shouldBe "name.ext (1).ext2"
        }
    }
})
