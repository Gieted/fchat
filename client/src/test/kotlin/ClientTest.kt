import io.kotest.core.spec.style.WordSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import pl.pawelkielb.fchat.Connection
import pl.pawelkielb.fchat.client.Database
import pl.pawelkielb.fchat.client.client.Client
import pl.pawelkielb.fchat.client.config.ChannelConfig
import pl.pawelkielb.fchat.client.config.ClientConfig
import pl.pawelkielb.fchat.data.Name
import pl.pawelkielb.fchat.packets.ChannelUpdatedPacket
import pl.pawelkielb.fchat.packets.LoginPacket
import pl.pawelkielb.fchat.packets.RequestUpdatesPacket
import java.util.*
import java.util.concurrent.CompletableFuture

class ClientTest : WordSpec({
    "sync()" should {
        "send LoginPacket if it's the first command" {
            val connection = mockk<Connection>(relaxed = true)
            val database = mockk<Database>()
            val config = ClientConfig.defaults()
            val client = Client(database, connection, config)

            every { connection.read() } returns CompletableFuture.completedFuture(null)

            client.sync()
            client.sync()
            verify(exactly = 1) { connection.send(LoginPacket(config.username)) }
        }

        "send RequestUpdatesPacket" {
            val connection = mockk<Connection>(relaxed = true)
            val database = mockk<Database>()
            val config = ClientConfig.defaults()
            val client = Client(database, connection, config)

            every { connection.read() } returns CompletableFuture.completedFuture(null)

            client.sync()
            verify { connection.send(RequestUpdatesPacket()) }
        }

        "save every received channel to database" {
            val connection = mockk<Connection>(relaxed = true)
            val database = mockk<Database>(relaxed = true)
            val config = ClientConfig.defaults()
            val client = Client(database, connection, config)

            val channelId = UUID.randomUUID()

            every { connection.read() } returnsMany listOf(
                ChannelUpdatedPacket.withRandomUUID(channelId, Name.of("Coders")),
                ChannelUpdatedPacket.withRandomUUID(channelId, Name.of("Book readers")),
                null
            ).map { CompletableFuture.completedFuture(it) }

            client.sync()
            verify(exactly = 2) {
                database.saveChannel(any(), any())
            }
            verify() {
                database.saveChannel(Name.of("Coders"), ChannelConfig(channelId))
                database.saveChannel(Name.of("Book readers"), ChannelConfig(channelId))
            }
        }

        "don't fail if there are no updates" {
            val connection = mockk<Connection>(relaxed = true)
            val database = mockk<Database>(relaxed = true)
            val config = ClientConfig.defaults()
            val client = Client(database, connection, config)

            every { connection.read() } returns CompletableFuture.completedFuture(null)

            client.sync()
            verify(exactly = 0) {
                database.saveChannel(any(), any())
            }
        }
    }
})
