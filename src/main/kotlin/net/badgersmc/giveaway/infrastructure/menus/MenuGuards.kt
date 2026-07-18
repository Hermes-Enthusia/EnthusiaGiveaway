package net.badgersmc.giveaway.infrastructure.menus

import com.github.stefvanschie.inventoryframework.gui.type.ChestGui

/**
 * Block all raw item movement in a menu (anti-dupe / anti-item-deletion).
 *
 * IFramework 0.11.6 cancels only **top-inventory** clicks by default. That leaves the
 * double-click *collect-to-cursor* sweep (which vacuums matching items out of the menu's
 * displayed slots into the player's cursor) and item *drags* uncancelled — so a player can
 * steal virtual GuiItem items OR drag items from their own inventory into the menu,
 * where they are silently deleted.
 *
 * Cancelling every click + drag closes all of those vectors. Intended menu actions are
 * driven by GuiItem onClick consumers (plain code), which IFramework still invokes even
 * when the event is cancelled — so buttons and clickable entries keep working; only raw
 * item movement is blocked. None of the Giveaway GUIs accept item drop-in, so a blanket
 * cancel is always correct here.
 */
fun ChestGui.blockItemTheft() {
    setOnGlobalClick { it.isCancelled = true }
    setOnGlobalDrag { it.isCancelled = true }
}
