import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.WordSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import pl.pawelkielb.fchat.Connection
import pl.pawelkielb.fchat.client.Client
import pl.pawelkielb.fchat.client.Database
import pl.pawelkielb.fchat.client.config.ChannelConfig
import pl.pawelkielb.fchat.client.config.ClientConfig
import pl.pawelkielb.fchat.data.Name
import pl.pawelkielb.fchat.packets.ChannelUpdatedPacket
import pl.pawelkielb.fchat.packets.LoginPacket
import pl.pawelkielb.fchat.packets.RequestUpdatesPacket
import pl.pawelkielb.fchat.packets.UpdateChannelPacket
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
                ChannelUpdatedPacket(channelId, Name.of("Coders")),
                ChannelUpdatedPacket(channelId, Name.of("Book readers")),
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

    "createPrivateChannel()" should {
        "send UpdateChannelPacket" {
            val connection = mockk<Connection>(relaxed = true)
            val database = mockk<Database>()
            val config = ClientConfig.defaults()
            val client = Client(database, connection, config)

            every { connection.read() } returns CompletableFuture.completedFuture(null)

            val paul = Name.of("Paul")

            client.createPrivateChannel(paul)

            verify { connection.send(match { it is UpdateChannelPacket && it.name == paul && it.members == listOf(paul) }) }
        }

        "throw NullPointerException when null is passed as parameter" {
            val connection = mockk<Connection>()
            val database = mockk<Database>()
            val config = ClientConfig.defaults()
            val client = Client(database, connection, config)

            shouldThrow<NullPointerException> {
                client.createPrivateChannel(null)
            }
        }
    }
})
