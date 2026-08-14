package com.sql1024.fastuse.mixin;

import com.sql1024.fastuse.FastUseConfig;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
    @Shadow
    private int destroyDelay;

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"))
    private void fastUse$noBreakDelay(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (FastUseConfig.get().removeBreakDelay()) {
            this.destroyDelay = 0;
        }
    }
}
