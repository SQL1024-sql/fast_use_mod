package com.sql1024.fastuse;

import com.sql1024.fastuse.mixin.MultiPlayerGameModeAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Everything that moves the hotbar around on the player's behalf. */
public final class HotbarAssist {
    /** How long to keep watching an anchor before giving up on ever seeing it go off. */
    private static final int ANCHOR_TIMEOUT_TICKS = 60;

    /** The anchor a click was traded for a totem, watched until it goes off. */
    private static BlockPos watchedAnchor;
    private static int watchedTicks;
    /** Where the main hand was before the offhand totem popped, or -1 when it has not. */
    private static int slotBeforeOffhandPop = -1;
    private static boolean offhandHadTotem;

    private HotbarAssist() {
    }

    public static void tick(Minecraft client) {
        LocalPlayer player = client.player;
        FastUseConfig config = FastUseConfig.get();
        if (player == null || !config.enabled) {
            reset();
            return;
        }
        // A popped totem is the emergency; the anchor can wait a tick.
        if (guardOffhandTotem(client, player, config)) {
            return;
        }
        followUpAnchor(client, player, config);
    }

    private static void reset() {
        watchedAnchor = null;
        slotBeforeOffhandPop = -1;
        offhandHadTotem = false;
    }

    /**
     * With the offhand totem gone, put one in the main hand until the offhand has another, then
     * go back to the slot that was in use. Refilling the offhand is left to the player.
     *
     * @return whether the main hand is currently standing in for the offhand
     */
    private static boolean guardOffhandTotem(Minecraft client, LocalPlayer player, FastUseConfig config) {
        boolean hasTotem = player.getOffhandItem().is(Items.TOTEM_OF_UNDYING);
        boolean popped = offhandHadTotem && !hasTotem;
        offhandHadTotem = hasTotem;
        if (!config.offhandTotemGuard) {
            slotBeforeOffhandPop = -1;
            return false;
        }
        if (popped && slotBeforeOffhandPop < 0) {
            int previous = player.getInventory().getSelectedSlot();
            ensureHotbarTotem(client, player, config);
            int totem = hotbarSlot(player, Items.TOTEM_OF_UNDYING);
            if (totem >= 0 && select(client, player, totem)) {
                slotBeforeOffhandPop = previous;
            }
        } else if (hasTotem && slotBeforeOffhandPop >= 0) {
            select(client, player, slotBeforeOffhandPop);
            slotBeforeOffhandPop = -1;
        }
        return slotBeforeOffhandPop >= 0;
    }

    /** Once the anchor has gone off, top the hotbar back up with a totem and take the next anchor. */
    private static void followUpAnchor(Minecraft client, LocalPlayer player, FastUseConfig config) {
        if (watchedAnchor == null) {
            return;
        }
        if (!config.returnToAnchorSlot || client.level == null) {
            watchedAnchor = null;
            return;
        }
        if (FastUseConfig.chargedAnchorAt(client.level, watchedAnchor)) {
            if (++watchedTicks < ANCHOR_TIMEOUT_TICKS) {
                return;
            }
            // Still sitting there charged, so nothing came of it; leave the hotbar alone.
            watchedAnchor = null;
            return;
        }
        watchedAnchor = null;
        // The totem may have been spent on the blast, so replace it before reaching for an anchor.
        ensureHotbarTotem(client, player, config);
        int anchor = hotbarSlot(player, Items.RESPAWN_ANCHOR);
        if (anchor >= 0) {
            select(client, player, anchor);
        }
    }

    /** Starts watching {@code pos} for the blast that a totem was just readied for. */
    public static void watchAnchor(BlockPos pos) {
        watchedAnchor = pos.immutable();
        watchedTicks = 0;
    }

    /**
     * Puts something to set an anchor off with in hand: a hotbar totem, one brought up from the
     * backpack, else whatever sits on the fallback key.
     *
     * @return whether the click that asked for this should be dropped in favour of it
     */
    public static boolean reachForTotem(Minecraft client, LocalPlayer player, FastUseConfig config) {
        ensureHotbarTotem(client, player, config);
        int totem = hotbarSlot(player, Items.TOTEM_OF_UNDYING);
        if (totem >= 0) {
            select(client, player, totem);
            return true;
        }
        // No totem anywhere: the fallback key sets it off instead. Swapping to more glowstone
        // would only charge the anchor again, and cancelling that click forever would wedge us.
        int fallback = config.fallbackSlot();
        if (player.getInventory().getItem(fallback).is(Items.GLOWSTONE)) {
            return false;
        }
        select(client, player, fallback);
        return true;
    }

    /** Brings a totem up from the backpack when the hotbar has none. */
    private static void ensureHotbarTotem(Minecraft client, LocalPlayer player, FastUseConfig config) {
        if (hotbarSlot(player, Items.TOTEM_OF_UNDYING) >= 0
                // Backpack slots only line up with menu slots while the inventory is the open menu.
                || player.containerMenu != player.inventoryMenu) {
            return;
        }
        int backpack = backpackSlot(player, Items.TOTEM_OF_UNDYING);
        if (backpack < 0) {
            return;
        }
        int destination = emptyHotbarSlot(player);
        if (destination < 0) {
            destination = config.fallbackSlot();
        }
        swapIntoHotbar(client, player, backpack, destination);
    }

    /** Selects {@code slot} and tells the server, returning whether anything changed. */
    public static boolean select(Minecraft client, LocalPlayer player, int slot) {
        Inventory inventory = player.getInventory();
        if (client.gameMode == null || inventory.getSelectedSlot() == slot) {
            return false;
        }
        inventory.setSelectedSlot(slot);
        ((MultiPlayerGameModeAccessor) client.gameMode).fastUse$ensureHasSentCarriedItem();
        return true;
    }

    /** The same swap as pressing a hotbar number with the cursor over a backpack slot. */
    private static void swapIntoHotbar(Minecraft client, LocalPlayer player, int backpackSlot, int hotbarSlot) {
        if (client.gameMode != null) {
            client.gameMode.handleContainerInput(player.inventoryMenu.containerId, backpackSlot, hotbarSlot,
                    ContainerInput.SWAP, player);
        }
    }

    /** The first hotbar slot holding {@code item}, or -1 when there is none. */
    public static int hotbarSlot(LocalPlayer player, Item item) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < Inventory.SELECTION_SIZE; slot++) {
            if (inventory.getItem(slot).is(item)) {
                return slot;
            }
        }
        return -1;
    }

    /** The first slot outside the hotbar holding {@code item}, or -1 when there is none. */
    private static int backpackSlot(LocalPlayer player, Item item) {
        Inventory inventory = player.getInventory();
        for (int slot = Inventory.SELECTION_SIZE; slot < Inventory.INVENTORY_SIZE; slot++) {
            if (inventory.getItem(slot).is(item)) {
                return slot;
            }
        }
        return -1;
    }

    /** The first empty hotbar slot, or -1 when the hotbar is full. */
    private static int emptyHotbarSlot(LocalPlayer player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < Inventory.SELECTION_SIZE; slot++) {
            if (inventory.getItem(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }
}
