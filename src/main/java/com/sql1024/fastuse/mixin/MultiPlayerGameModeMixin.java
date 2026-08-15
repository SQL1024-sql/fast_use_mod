package com.sql1024.fastuse.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.sql1024.fastuse.FastUseConfig;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Input;
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
    private int destroyDelay;

    /**
     * The input to send once the diverted click is done, or null when this click is not diverted.
     * Only ever set for the duration of a single {@code useItemOn} call on the client thread.
     */
    @Unique
    private Input fastUse$inputToRestore;

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"))
    private void fastUse$noBreakDelay(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (FastUseConfig.get().removeBreakDelay()) {
            this.destroyDelay = 0;
        }
    }

    /**
     * The server decides between charging the anchor and placing the block from the player's sneak
     * state, so tell it we are sneaking for exactly this one interaction.
     */
    @Inject(method = "useItemOn", at = @At("HEAD"))
    private void fastUse$sneakBeforeAnchorCharge(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult,
                                                 CallbackInfoReturnable<InteractionResult> cir) {
        this.fastUse$inputToRestore = null;
        if (!FastUseConfig.get().placeInsteadOfCharge(player, hand, hitResult)) {
            return;
        }
        Input sent = player.getLastSentInput();
        if (sent.shift()) {
            // The server already sees us sneaking, so vanilla places the block by itself.
            return;
        }
        this.fastUse$inputToRestore = sent;
        player.connection.send(new ServerboundPlayerInputPacket(new Input(
                sent.forward(), sent.backward(), sent.left(), sent.right(), sent.jump(), true, sent.sprint())));
    }

    /** Keep the client-side prediction in step with the sneak state the server is about to see. */
    @WrapOperation(
            method = "performUseItemOn",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isSecondaryUseActive()Z"))
    private boolean fastUse$predictPlacement(LocalPlayer player, Operation<Boolean> original) {
        return original.call(player) || this.fastUse$inputToRestore != null;
    }

    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void fastUse$restoreSneak(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult,
                                      CallbackInfoReturnable<InteractionResult> cir) {
        Input restore = this.fastUse$inputToRestore;
        if (restore != null) {
            this.fastUse$inputToRestore = null;
            player.connection.send(new ServerboundPlayerInputPacket(restore));
        }
    }
}
