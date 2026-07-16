package net.badgersmc.giveaway.application

import net.badgersmc.giveaway.domain.GiveawayId

data class GiveawaySummary(
    val id: GiveawayId,
    val title: String,
    val description: String = "",
    val secondsRemaining: Long,
    val entryCount: Int,
    val alreadyEntered: Boolean,
)
