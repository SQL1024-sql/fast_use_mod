package com.sql1024.fastuse.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Lets code outside an interaction send the carried-item packet the game keeps to itself. */
@Mixin(MultiPlayerGameMode.class)
public interface MultiPlayerGameModeAccessor {
    @Invoker("ensureHasSentCarriedItem")
    void fastUse$ensureHasSentCarriedItem();
}
