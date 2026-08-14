package com.sql1024.fastuse.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.sql1024.fastuse.FastUseConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Shadow
    private int rightClickDelay;

    @Shadow
    protected int missTime;

    @WrapOperation(
            method = "startUseItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;isDestroying()Z"))
    private boolean fastUse$useWhileMining(MultiPlayerGameMode gameMode, Operation<Boolean> original) {
        boolean destroying = original.call(gameMode);
        return !FastUseConfig.get().placeWhileMining() && destroying;
    }

    @WrapOperation(
            method = "continueAttack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"))
    private boolean fastUse$mineWhileUsing(LocalPlayer player, Operation<Boolean> original) {
        boolean using = original.call(player);
        return !FastUseConfig.get().mineWhileUsing() && using;
    }

    @Inject(method = "handleKeybinds", at = @At("HEAD"))
    private void fastUse$clearDelays(CallbackInfo ci) {
        FastUseConfig config = FastUseConfig.get();
        if (this.rightClickDelay > config.useDelayTicks()) {
            this.rightClickDelay = config.useDelayTicks();
        }
        if (config.removeMissDelay()) {
            this.missTime = 0;
        }
    }
}
