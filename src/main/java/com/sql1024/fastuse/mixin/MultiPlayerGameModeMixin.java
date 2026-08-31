package com.sql1024.fastuse.mixin;

import com.sql1024.fastuse.FastUseConfig;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
    @Shadow
    private void ensureHasSentCarriedItem() {
        throw new AssertionError();
    }

    /**
     * Swap to the totem instead of topping up an anchor that is already charged. Nothing is sent
     * for the cancelled interaction, so the anchor keeps the charge it has.
     */
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void fastUse$totemInsteadOfRecharge(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult,
                                                CallbackInfoReturnable<InteractionResult> cir) {
        FastUseConfig config = FastUseConfig.get();
        if (!config.chargingChargedAnchor(player.getItemInHand(hand), hitResult.getBlockPos())) {
            return;
        }
        int slot = FastUseConfig.hotbarTotemSlot(player);
        if (slot < 0) {
            // No totem to swap to, so leave the click alone and let it charge at vanilla speed.
            return;
        }
        Inventory inventory = player.getInventory();
        if (inventory.getSelectedSlot() != slot) {
            inventory.setSelectedSlot(slot);
            // useItemOn would have done this itself; we cancel before it runs, so tell the server here.
            this.ensureHasSentCarriedItem();
        }
        cir.setReturnValue(InteractionResult.FAIL);
    }
}
