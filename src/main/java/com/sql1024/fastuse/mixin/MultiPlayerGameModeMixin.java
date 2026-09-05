package com.sql1024.fastuse.mixin;

import com.sql1024.fastuse.FastUseConfig;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
    @Shadow
    private void ensureHasSentCarriedItem() {
        throw new AssertionError();
    }

    @Shadow
    public void handleContainerInput(int containerId, int slotId, int button, ContainerInput input, Player player) {
        throw new AssertionError();
    }

    /** The slot to go back to once this click is done, or -1 when nothing was swapped away from. */
    @Unique
    private int fastUse$slotToRestore = -1;

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void fastUse$autoSwap(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult,
                                  CallbackInfoReturnable<InteractionResult> cir) {
        this.fastUse$slotToRestore = -1;
        FastUseConfig config = FastUseConfig.get();
        ItemStack stack = player.getItemInHand(hand);

        // Get a totem in hand instead of topping up an anchor that is already charged. Nothing is
        // sent for the cancelled interaction, so the anchor keeps the charge it has.
        if (config.chargingChargedAnchor(stack, hitResult.getBlockPos())) {
            if (fastUse$reachForTotem(player, config)) {
                cir.setReturnValue(InteractionResult.FAIL);
            }
            return;
        }

        // The crystal has nowhere to go, so lay the obsidian for it with this click instead. The
        // swap is sent before the interaction packet, so the server places obsidian as well.
        if (hand == InteractionHand.MAIN_HAND && config.needsObsidianFirst(stack, hitResult)) {
            int obsidian = FastUseConfig.hotbarSlot(player, Items.OBSIDIAN);
            if (obsidian >= 0) {
                int crystalSlot = player.getInventory().getSelectedSlot();
                if (fastUse$select(player, obsidian)) {
                    this.fastUse$slotToRestore = crystalSlot;
                }
            }
        }
    }

    /** Back to the crystal once the obsidian is on its way. */
    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void fastUse$restoreSlot(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult,
                                     CallbackInfoReturnable<InteractionResult> cir) {
        int slot = this.fastUse$slotToRestore;
        this.fastUse$slotToRestore = -1;
        if (slot >= 0) {
            fastUse$select(player, slot);
        }
    }

    /**
     * Puts something to set the anchor off with in hand: the hotbar totem, else one dragged up
     * from the backpack, else whatever sits on the fallback key. Returns whether this click
     * should be dropped in favour of that.
     */
    @Unique
    private boolean fastUse$reachForTotem(LocalPlayer player, FastUseConfig config) {
        int totem = FastUseConfig.hotbarSlot(player, Items.TOTEM_OF_UNDYING);
        if (totem >= 0) {
            fastUse$select(player, totem);
            return true;
        }

        // A totem further back only reaches the hand through the inventory menu, which is not
        // the open one when a chest or similar is up.
        int backpack = player.containerMenu == player.inventoryMenu
                ? FastUseConfig.backpackTotemSlot(player)
                : -1;
        if (backpack >= 0) {
            int destination = FastUseConfig.emptyHotbarSlot(player);
            if (destination < 0) {
                destination = config.fallbackSlot();
            }
            // Menu slots line up with inventory slots outside the hotbar; SWAP takes the hotbar
            // slot it swaps with as its button, exactly like pressing that number over the item.
            this.handleContainerInput(player.inventoryMenu.containerId, backpack, destination,
                    ContainerInput.SWAP, player);
            fastUse$select(player, destination);
            return true;
        }

        // No totem at all: the fallback key sets it off instead. Swapping to more glowstone would
        // only charge the anchor again, and cancelling that click forever would wedge us.
        int fallback = config.fallbackSlot();
        if (player.getInventory().getItem(fallback).is(Items.GLOWSTONE)) {
            return false;
        }
        fastUse$select(player, fallback);
        return true;
    }

    /** Selects {@code slot} and tells the server, returning whether anything changed. */
    @Unique
    private boolean fastUse$select(LocalPlayer player, int slot) {
        Inventory inventory = player.getInventory();
        if (inventory.getSelectedSlot() == slot) {
            return false;
        }
        inventory.setSelectedSlot(slot);
        // useItemOn sends this itself, but only after our injection point.
        this.ensureHasSentCarriedItem();
        return true;
    }
}
