package net.badgersmc.giveaway.infrastructure.menus

import com.github.stefvanschie.inventoryframework.gui.GuiItem
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui
import com.github.stefvanschie.inventoryframework.pane.StaticPane
import net.badgersmc.giveaway.application.CancelGiveaway
import net.badgersmc.giveaway.application.CancelResult
import net.badgersmc.giveaway.application.StartFromTemplateResult
import net.badgersmc.giveaway.application.StartGiveawayFromTemplate
import net.badgersmc.giveaway.domain.Giveaway
import net.badgersmc.giveaway.domain.GiveawayState
import net.badgersmc.giveaway.domain.Template
import net.badgersmc.giveaway.domain.ports.GiveawayRepository
import net.badgersmc.giveaway.infrastructure.components.Styled
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.time.Duration
import java.time.Instant

class AdminGiveawayMenu(
    private val giveaways: GiveawayRepository,
    private val cancelGiveaway: CancelGiveaway,
    private val scheduleWizard: ScheduleWizard,
    private val templates: List<Template>,
    private val startFromTemplate: StartGiveawayFromTemplate,
) {
    fun open(player: Player) {
        val gui = ChestGui(6, "Giveaway Admin")
        gui.blockItemTheft()
        val pane = StaticPane(0, 0, 9, 6)
        val now = Instant.now()
        val rows = listOf(
            giveaways.listByState(GiveawayState.SCHEDULED),
            giveaways.listByState(GiveawayState.ACTIVE),
        ).flatten()

        if (rows.isEmpty()) {
            val empty = ItemStack(Material.BARRIER)
            empty.editMeta {
                it.displayName(Styled.body("No scheduled or active giveaways"))
            }
            pane.addItem(GuiItem(empty) { it.isCancelled = true }, 4, 2)
        } else {
            rows.take(45).forEachIndexed { index, g ->
                pane.addItem(buildRow(g, now, player), index % 9, index / 9)
            }
        }

        // Schedule-new button
        val schedule = ItemStack(Material.NETHER_STAR)
        schedule.editMeta {
            it.displayName(Styled.success("🗓 Schedule New")
                .decorate(TextDecoration.BOLD))
        }
        pane.addItem(GuiItem(schedule) { event ->
            event.isCancelled = true
            player.closeInventory()
            scheduleWizard.open(player)
        }, 7, 5)

        // Templates button
        if (templates.isNotEmpty()) {
            val tpl = ItemStack(Material.BOOK)
            tpl.editMeta {
                it.displayName(Styled.accent("📋 Start from Template")
                    .decorate(TextDecoration.BOLD))
            }
            pane.addItem(GuiItem(tpl) { event ->
                event.isCancelled = true
                player.closeInventory()
                openTemplates(player)
            }, 8, 5)
        }

        gui.addPane(pane)
        gui.show(player)
    }

    private fun buildRow(g: Giveaway, now: Instant, player: Player): GuiItem {
        val mat = if (g.state == GiveawayState.ACTIVE) Material.YELLOW_DYE else Material.GRAY_DYE
        val item = ItemStack(mat)
        val remaining = Duration.between(now, g.endsAt).seconds.coerceAtLeast(0L)
        item.editMeta { meta ->
            meta.displayName(Styled.giveawayTitle(g.title))
            meta.lore(listOf(
                Styled.body("State: ${g.state.name.lowercase().replaceFirstChar { it.uppercase() }}"),
                Styled.timeLeft(remaining),
                Styled.body("Winners: ${g.maxWinners}"),
                Component.text(g.command, NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Styled.error("Shift-click to cancel"),
            ))
        }
        return GuiItem(item) { event ->
            event.isCancelled = true
            if (!event.isShiftClick) return@GuiItem
            when (cancelGiveaway.invoke(g.id)) {
                CancelResult.Cancelled -> {
                    player.sendActionBar(Styled.error("Cancelled: ${g.title}"))
                    open(player)
                }
                CancelResult.NotFound -> player.sendActionBar(Styled.body("Already gone"))
                CancelResult.AlreadyFinal -> player.sendActionBar(Styled.body("Already finalized"))
            }
        }
    }

    private fun openTemplates(player: Player) {
        val gui = ChestGui(3, "Start from Template")
        gui.blockItemTheft()
        val pane = StaticPane(0, 0, 9, 3)

        templates.take(9).forEachIndexed { index, t ->
            val item = ItemStack(Material.BOOK)
            item.editMeta { meta ->
                meta.displayName(Styled.giveawayTitle(t.title))
                meta.lore(listOf(
                    Component.text(t.description, NamedTextColor.WHITE)
                        .decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    Styled.body("Winners: ${t.maxWinners}  ·  Duration: ${Styled.timeLeft(t.durationSeconds)}"),
                    Styled.body("Command: ${t.command}"),
                    Component.empty(),
                    Styled.success("Click to start"),
                ))
            }
            pane.addItem(GuiItem(item) { event ->
                event.isCancelled = true
                val result = startFromTemplate.invoke(t.name, player.uniqueId)
                when (result) {
                    is StartFromTemplateResult.Created -> {
                        player.closeInventory()
                        player.sendMessage(Styled.success("Started: ${t.title}"))
                        open(player)
                    }
                    is StartFromTemplateResult.Invalid ->
                        player.sendActionBar(Styled.error("Invalid: ${result.reason}"))
                    StartFromTemplateResult.NotFound ->
                        player.sendActionBar(Styled.error("Template not found"))
                }
            }, index, 1)
        }

        val back = ItemStack(Material.ARROW)
        back.editMeta {
            it.displayName(Styled.body("← Back"))
        }
        pane.addItem(GuiItem(back) { event ->
            event.isCancelled = true
            open(player)
        }, 0, 2)

        gui.addPane(pane)
        gui.show(player)
    }
}
