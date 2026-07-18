package net.badgersmc.giveaway.infrastructure.menus

import com.github.stefvanschie.inventoryframework.gui.GuiItem
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui
import com.github.stefvanschie.inventoryframework.pane.StaticPane
import net.badgersmc.giveaway.application.EnterGiveaway
import net.badgersmc.giveaway.application.EnterResult
import net.badgersmc.giveaway.application.GiveawaySummary
import net.badgersmc.giveaway.application.ListActiveGiveaways
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class PlayerGiveawayMenu(
    private val listActive: ListActiveGiveaways,
    private val enterGiveaway: EnterGiveaway,
) {
    fun open(player: Player) {
        render(player)
    }

    private fun render(player: Player) {
        val summaries = listActive.invoke(player.uniqueId)
        val gui = ChestGui(6, "Active Giveaways")
        gui.blockItemTheft()

        val pane = StaticPane(0, 0, 9, 6)

        if (summaries.isEmpty()) {
            pane.addItem(infoItem("No active giveaways", NamedTextColor.GRAY), 4, 2)
        } else {
            summaries.take(54).forEachIndexed { index, summary ->
                pane.addItem(buildRow(summary, player), index % 9, index / 9)
            }
        }

        gui.addPane(pane)
        gui.show(player)
    }

    private fun buildRow(s: GiveawaySummary, player: Player): GuiItem {
        val material = if (s.alreadyEntered) Material.LIME_DYE else Material.PAPER
        val item = ItemStack(material)
        item.editMeta { meta ->
            meta.displayName(
                Component.text(s.title)
                    .color(NamedTextColor.GOLD)
                    .decorate(TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false)
            )
            meta.lore(listOf(
                Component.text("Time remaining: ${formatDuration(s.secondsRemaining)}")
                    .color(NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false),
                Component.text("Entries: ${s.entryCount}")
                    .color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                if (s.alreadyEntered)
                    Component.text("✓ Entered").color(NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, false)
                else
                    Component.text("Click to enter").color(NamedTextColor.AQUA)
                        .decoration(TextDecoration.ITALIC, false),
            ))
        }

        return GuiItem(item) { event ->
            event.isCancelled = true
            if (s.alreadyEntered) {
                actionbar(player, "You are already entered.", NamedTextColor.GRAY)
                return@GuiItem
            }
            when (enterGiveaway.invoke(s.id, player.uniqueId)) {
                EnterResult.Success -> {
                    actionbar(player, "Entered: ${s.title}", NamedTextColor.GREEN)
                    player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f)
                    render(player) // refresh
                }
                EnterResult.AlreadyEntered -> actionbar(player, "You are already entered.", NamedTextColor.GRAY)
                EnterResult.GiveawayNotFound -> actionbar(player, "This giveaway no longer exists.", NamedTextColor.RED)
                EnterResult.NotActive -> actionbar(player, "This giveaway is not active.", NamedTextColor.RED)
            }
        }
    }

    private fun infoItem(text: String, color: NamedTextColor): GuiItem {
        val item = ItemStack(Material.BARRIER)
        item.editMeta { meta ->
            meta.displayName(
                Component.text(text).color(color).decoration(TextDecoration.ITALIC, false)
            )
        }
        return GuiItem(item) { it.isCancelled = true }
    }

    private fun actionbar(player: Player, text: String, color: NamedTextColor) {
        player.sendActionBar(Component.text(text).color(color))
    }

    private fun formatDuration(seconds: Long): String {
        if (seconds <= 0) return "expired"
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return buildString {
            if (h > 0) append("${h}h ")
            if (h > 0 || m > 0) append("${m}m ")
            append("${s}s")
        }
    }
}
