package com.sql1024.fastuse.mixin;

import com.sql1024.fastuse.FastUseConfig;
import com.sql1024.fastuse.HotbarAssist;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
    /** The slot to go back to once this click is done, or -1 when nothing was swapped away from. */
    @Unique
    private int fastUse$slotToRestore = -1;

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void fastUse$autoSwap(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult,
                                  CallbackInfoReturnable<InteractionResult> cir) {
        this.fastUse$slotToRestore = -1;
        Minecraft client = Minecraft.getInstance();
        FastUseConfig config = FastUseConfig.get();
        ItemStack stack = player.getItemInHand(hand);

        // Get a totem in hand instead of topping up an anchor that is already charged. Nothing is
        // sent for the cancelled interaction, so the anchor keeps the charge it has.
        if (config.chargingChargedAnchor(stack, hitResult.getBlockPos())) {
            if (HotbarAssist.reachForTotem(client, player, config)) {
                HotbarAssist.watchAnchor(hitResult.getBlockPos());
                cir.setReturnValue(InteractionResult.FAIL);
            }
            return;
        }

        // The crystal has nowhere to go, so lay the obsidian for it with this click instead. The
        // swap is sent before the interaction packet, so the server places obsidian as well.
        if (hand == InteractionHand.MAIN_HAND && config.needsObsidianFirst(stack, hitResult)) {
            int obsidian = HotbarAssist.hotbarSlot(player, Items.OBSIDIAN);
            if (obsidian >= 0) {
                int crystalSlot = player.getInventory().getSelectedSlot();
                if (HotbarAssist.select(client, player, obsidian)) {
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
            HotbarAssist.select(Minecraft.getInstance(), player, slot);
        }
    }
}
