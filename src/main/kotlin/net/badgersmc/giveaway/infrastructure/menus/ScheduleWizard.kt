package net.badgersmc.giveaway.infrastructure.menus

import com.github.stefvanschie.inventoryframework.gui.GuiItem
import com.github.stefvanschie.inventoryframework.gui.type.AnvilGui
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui
import com.github.stefvanschie.inventoryframework.pane.StaticPane
import net.badgersmc.giveaway.application.ScheduleGiveaway
import net.badgersmc.giveaway.application.ScheduleResult
import net.badgersmc.giveaway.application.util.DurationParser
import net.badgersmc.giveaway.infrastructure.components.Styled
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * Sequential AnvilGui wizard for scheduling a giveaway.
 * Steps: title → duration → command → winner count → confirm.
 */
class ScheduleWizard(private val scheduleGiveaway: ScheduleGiveaway) {

    fun open(player: Player) = promptTitle(player)

    private fun promptTitle(player: Player) {
        anvilStep("Enter title", "Giveaway", player) { value ->
            if (value.isBlank()) {
                player.sendMessage(Styled.error("Title must not be blank."))
                promptTitle(player)
            } else promptDuration(player, value)
        }
    }

    private fun promptDuration(player: Player, title: String) {
        anvilStep("Duration (e.g. 1h30m)", "1h", player) { value ->
            val seconds = DurationParser.parse(value)
            if (seconds == null) {
                player.sendMessage(Styled.error("Invalid duration. Try 1h30m, 45m, 2d."))
                promptDuration(player, title)
            } else promptCommand(player, title, seconds)
        }
    }

    private fun promptCommand(player: Player, title: String, durationSeconds: Long) {
        anvilStep("Console command", "give <player> diamond 1", player) { value ->
            promptWinners(player, title, durationSeconds, value)
        }
    }

    private fun promptWinners(player: Player, title: String, durationSeconds: Long, command: String) {
        anvilStep("Winner count", "1", player) { value ->
            val winners = value.toIntOrNull()
            if (winners == null || winners < 1) {
                player.sendMessage(Styled.error("Winner count must be a positive integer."))
                promptWinners(player, title, durationSeconds, command)
            } else confirm(player, title, durationSeconds, command, winners)
        }
    }

    private fun confirm(player: Player, title: String, durationSeconds: Long, command: String, winners: Int) {
        val gui = ChestGui(3, "Confirm Giveaway")
        gui.blockItemTheft()
        val pane = StaticPane(0, 0, 9, 3)

        val summary = ItemStack(Material.PAPER)
        summary.editMeta { meta ->
            meta.displayName(Styled.giveawayTitle(title))
            meta.lore(listOf(
                Styled.body("Duration: ${durationSeconds}s"),
                Styled.body("Winners: $winners"),
                Styled.body("Command: $command"),
            ))
        }
        pane.addItem(GuiItem(summary) { it.isCancelled = true }, 4, 0)

        val confirmItem = ItemStack(Material.LIME_CONCRETE)
        confirmItem.editMeta {
            it.displayName(Styled.success("✓ Confirm").decorate(TextDecoration.BOLD))
        }
        pane.addItem(GuiItem(confirmItem) { event ->
            event.isCancelled = true
            val result = scheduleGiveaway.invoke(title, durationSeconds, command, winners, player.uniqueId)
            when (result) {
                is ScheduleResult.Created -> player.sendMessage(
                    Styled.success("Scheduled ").append(Styled.giveawayTitle(title))
                )
                is ScheduleResult.Invalid -> player.sendMessage(
                    Styled.error("Could not schedule: ${result.field} ${result.reason}")
                )
            }
            player.closeInventory()
        }, 2, 2)

        val cancelItem = ItemStack(Material.RED_CONCRETE)
        cancelItem.editMeta {
            it.displayName(Styled.error("✗ Cancel").decoration(TextDecoration.ITALIC, false))
        }
        pane.addItem(GuiItem(cancelItem) { event ->
            event.isCancelled = true
            player.closeInventory()
        }, 6, 2)

        gui.addPane(pane)
        gui.show(player)
    }

    private fun anvilStep(prompt: String, initial: String, player: Player, onAccept: (String) -> Unit) {
        val gui = AnvilGui(prompt)
        gui.setOnTopClick { it.isCancelled = true }
        var current = initial

        val firstPane = StaticPane(0, 0, 1, 1)
        val input = ItemStack(Material.PAPER)
        input.editMeta {
            it.displayName(Component.text(initial).decoration(TextDecoration.ITALIC, false))
        }
        firstPane.addItem(GuiItem(input) { it.isCancelled = true }, 0, 0)
        gui.firstItemComponent.addPane(firstPane)

        gui.setOnNameInputChanged { newName -> current = newName }

        val thirdPane = StaticPane(0, 0, 1, 1)
        val confirmItem = ItemStack(Material.LIME_DYE)
        confirmItem.editMeta {
            it.displayName(Styled.success("Confirm").decoration(TextDecoration.ITALIC, false))
        }
        thirdPane.addItem(GuiItem(confirmItem) { event ->
            event.isCancelled = true
            val captured = current
            event.whoClicked.closeInventory()
            onAccept(captured)
        }, 0, 0)
        gui.resultComponent.addPane(thirdPane)

        gui.show(player)
    }
}
