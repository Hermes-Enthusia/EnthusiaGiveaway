package net.badgersmc.giveaway.application

import net.badgersmc.giveaway.domain.GiveawayState
import net.badgersmc.giveaway.domain.ports.Clock
import net.badgersmc.giveaway.domain.ports.EntryRepository
import net.badgersmc.giveaway.domain.ports.GiveawayRepository
import java.time.Duration
import java.util.UUID

class ListActiveGiveaways(
    private val giveaways: GiveawayRepository,
    private val entries: EntryRepository,
    private val clock: Clock,
) {
    operator fun invoke(viewerUuid: UUID): List<GiveawaySummary> {
        val now = clock.now()
        return giveaways.listByState(GiveawayState.ACTIVE).map { g ->
            val secondsRemaining = Duration.between(now, g.endsAt).seconds.coerceAtLeast(0)
            GiveawaySummary(
                id = g.id,
                title = g.title,
                description = g.description,
                secondsRemaining = secondsRemaining,
                entryCount = entries.playerUuidsFor(g.id).size,
                alreadyEntered = entries.hasEntered(g.id, viewerUuid),
            )
        }
    }
}
