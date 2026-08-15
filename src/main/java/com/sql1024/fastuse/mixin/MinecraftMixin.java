package com.sql1024.fastuse.mixin;

import com.sql1024.fastuse.FastUseConfig;
import net.minecraft.client.Minecraft;
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
}
