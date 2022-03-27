import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import pl.pawelkielb.fchat.TaskQueue
import kotlin.time.Duration.Companion.seconds

class TaskQueueTest : WordSpec({
    "run()" should {
        "run tasks one by one" {
            val queue = TaskQueue()
            var i = 0

            launch {
                queue.run {
                    runBlocking {
                        delay(1.seconds)
                    }
                    i shouldBe 0
                }
            }
            launch {
                queue.run {
                    i++
                }
            }
        }
    }
})
