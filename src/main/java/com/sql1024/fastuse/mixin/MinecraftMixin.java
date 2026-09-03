package com.sql1024.fastuse.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.sql1024.fastuse.FastUseConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Shadow
    private int rightClickDelay;

    @Inject(method = "handleKeybinds", at = @At("HEAD"))
    private void fastUse$clearUseDelay(CallbackInfo ci) {
        int delay = FastUseConfig.get().useDelayTicks();
        if (this.rightClickDelay > delay) {
            this.rightClickDelay = delay;
        }
    }

    /** startUseItem bails out while a block is being broken; pretend it is not. */
    @WrapOperation(
            method = "startUseItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;isDestroying()Z"))
    private boolean fastUse$useWhileMining(MultiPlayerGameMode gameMode, Operation<Boolean> original) {
        boolean destroying = original.call(gameMode);
        return !FastUseConfig.get().placeWhileMining() && destroying;
    }
}
