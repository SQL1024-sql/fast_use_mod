package com.sql1024.fastuse;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FastUseMod implements ClientModInitializer {
    public static final String MOD_ID = "fast_use_mod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

    private KeyMapping toggleKey;

    @Override
    public void onInitializeClient() {
        this.toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.fast_use_mod.toggle",
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(this::onEndTick);

        FastUseConfig config = FastUseConfig.get();
        LOGGER.info("Fast Use ready (enabled={}, requireEndCrystal={})", config.enabled, config.requireEndCrystal);
    }

    private void onEndTick(Minecraft client) {
        while (this.toggleKey.consumeClick()) {
            FastUseConfig config = FastUseConfig.get();
            config.enabled = !config.enabled;
            config.save();
            if (client.gui != null) {
                client.gui.setOverlayMessage(Component.translatable(message(config)), false);
            }
        }
    }

    private static String message(FastUseConfig config) {
        if (!config.enabled) {
            return "message.fast_use_mod.disabled";
        }
        return config.requireEndCrystal ? "message.fast_use_mod.enabled_crystal" : "message.fast_use_mod.enabled";
    }
}
