package me.senseiwells.mojank

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.BeforeTest

class MojankTests {
    // Valid profiles:
    private lateinit var sensei: SimpleMojankProfile
    private lateinit var santa: SimpleMojankProfile

    // Invalid profiles:
    private lateinit var spaceship: SimpleMojankProfile
    private lateinit var happy: SimpleMojankProfile

    @BeforeTest
    fun beforeTests() {
        sensei = SimpleMojankProfile(
            UUID.fromString("d4fca8c4-e083-4300-9a73-bf438847861c"),
            "senseiwells"
        )
        santa = SimpleMojankProfile(
            UUID.fromString("c114a6c1-f7dc-431c-9289-53900bb98930"),
            "SuperSanta"
        )

        spaceship = SimpleMojankProfile(
            UUID.fromString("00000000-0000-0000-0000-000000000000"),
            "!<=>!"
        )
        happy = SimpleMojankProfile(
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            "!^_^!"
        )
    }

    @Nested
    @DisplayName("Test usernameToSimpleProfile")
    inner class UsernameToSimpleProfileTests {
        @Test
        fun `with valid username`() = runTest {
            val result = Mojank.usernameToSimpleProfile(sensei.name)
            assert(result.isSuccess && result.get() == sensei)
        }

        @Test
        fun `with invalid username`() = runTest {
            val result = Mojank.usernameToSimpleProfile(spaceship.name)
            assert(result.isFailure && result.isConclusive)
        }
    }

    @Nested
    @DisplayName("Test usernamesToSimpleProfiles")
    inner class UsernamesToSimpleProfilesTest {
        @Test
        fun `with valid usernames`() = runTest {
            val result = Mojank.usernamesToSimpleProfiles(listOf(sensei.name, santa.name))
            assert(result.isSuccess && result.get().containsAll(listOf(sensei, santa)))
        }

        @Test
        fun `with valid and invalid usernames`() = runTest {
            val result = Mojank.usernamesToSimpleProfiles(listOf(sensei.name, spaceship.name))
            assert(result.isPartial && result.get().size == 1 && result.get().first() == sensei && result.isConclusive)
        }

        @Test
        fun `with invalid usernames`() = runTest {
            val result = Mojank.usernamesToSimpleProfiles(listOf(spaceship.name, happy.name))
            assert(result.isFailure && result.isConclusive)
        }

        @Test
        fun `with no usernames`() = runTest {
            val result = Mojank.usernamesToSimpleProfiles(listOf())
            assert(result.isSuccess && result.get().isEmpty())
        }
    }

    @Nested
    @DisplayName("Test uuidToProfile")
    inner class UUIDToProfileTests {
        @Test
        fun `with valid username`() = runTest {
            val result = Mojank.uuidToProfile(sensei.id)
            assert(result.isSuccess && result.get().toSimple() == sensei)
        }

        @Test
        fun `with invalid username`() = runTest {
            val result = Mojank.uuidToProfile(spaceship.id)
            assert(!result.isSuccessOrPartial && result.isConclusive)
        }
    }

    @Nested
    @DisplayName("Test attempts")
    inner class AttemptTests {
        @Test
        fun `with username request`() = runTest {
            val result = Mojank.attempt { usernameToUUID(sensei.name) }
            assert(result.isSuccess && result.get() == sensei.id)
        }
    }
}