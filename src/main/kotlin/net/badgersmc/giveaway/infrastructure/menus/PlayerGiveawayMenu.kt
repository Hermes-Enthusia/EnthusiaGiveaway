package net.badgersmc.giveaway.infrastructure.menus

import com.github.stefvanschie.inventoryframework.gui.GuiItem
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui
import com.github.stefvanschie.inventoryframework.pane.StaticPane
import net.badgersmc.giveaway.application.EnterGiveaway
import net.badgersmc.giveaway.application.EnterResult
import net.badgersmc.giveaway.application.GiveawaySummary
import net.badgersmc.giveaway.application.ListActiveGiveaways
import net.badgersmc.giveaway.infrastructure.components.Styled
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
        gui.setOnTopClick { it.isCancelled = true }

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
            meta.displayName(Styled.giveawayTitle(s.title))
            val lore = mutableListOf<Component>()
            if (s.description.isNotBlank()) {
                lore.add(Component.text(s.description, NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false))
                lore.add(Component.empty())
            }
            lore.add(Styled.timeLeft(s.secondsRemaining))
            lore.add(Styled.body("Entries: ${s.entryCount}"))
            lore.add(Component.empty())
            lore.add(if (s.alreadyEntered) Styled.success("✓ Entered") else Styled.accent("Click to enter"))
            meta.lore(lore)
        }

        return GuiItem(item) { event ->
            event.isCancelled = true
            if (s.alreadyEntered) {
                player.sendActionBar(Styled.body("You are already entered."))
                return@GuiItem
            }
            when (enterGiveaway.invoke(s.id, player.uniqueId)) {
                EnterResult.Success -> {
                    player.sendActionBar(Styled.success("Entered: ${s.title}"))
                    player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f)
                    render(player)
                }
                EnterResult.AlreadyEntered -> player.sendActionBar(Styled.body("You are already entered."))
                EnterResult.GiveawayNotFound -> player.sendActionBar(Styled.error("This giveaway no longer exists."))
                EnterResult.NotActive -> player.sendActionBar(Styled.error("This giveaway is not active."))
            }
        }
    }

    private fun infoItem(text: String, color: NamedTextColor): GuiItem {
        val item = ItemStack(Material.BARRIER)
        item.editMeta { meta ->
            meta.displayName(Component.text(text).color(color).decoration(TextDecoration.ITALIC, false))
        }
        return GuiItem(item) { it.isCancelled = true }
    }
}
