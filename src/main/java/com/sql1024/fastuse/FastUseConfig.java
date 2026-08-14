package com.sql1024.fastuse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Items;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FastUseConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static FastUseConfig instance;

    public boolean enabled = true;
    /** When true (the default) every feature below only works while an end crystal is held. */
    public boolean requireEndCrystal = true;
    public boolean placeWhileMining = true;
    public boolean mineWhileUsing = true;
    public int useDelayTicks = 0;
    public boolean removeBreakDelay = true;
    public boolean removeMissDelay = true;

    public static FastUseConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    /** True while the client player holds an end crystal in either hand. */
    public static boolean holdingEndCrystal() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return false;
        }
        LocalPlayer player = client.player;
        if (player == null) {
            return false;
        }
        return player.getMainHandItem().is(Items.END_CRYSTAL) || player.getOffhandItem().is(Items.END_CRYSTAL);
    }

    /** The master switch: on, and holding an end crystal if that is required. */
    public boolean active() {
        return this.enabled && (!this.requireEndCrystal || holdingEndCrystal());
    }

    public boolean placeWhileMining() {
        return active() && this.placeWhileMining;
    }

    public boolean mineWhileUsing() {
        return active() && this.mineWhileUsing;
    }

    public boolean removeBreakDelay() {
        return active() && this.removeBreakDelay;
    }

    public boolean removeMissDelay() {
        return active() && this.removeMissDelay;
    }

    public int useDelayTicks() {
        return active() ? Math.max(0, this.useDelayTicks) : Integer.MAX_VALUE;
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("fast_use_mod.json");
    }

    private static FastUseConfig load() {
        Path path = path();
        if (Files.exists(path)) {
            try (BufferedReader reader = Files.newBufferedReader(path)) {
                FastUseConfig loaded = GSON.fromJson(reader, FastUseConfig.class);
                if (loaded != null) {
                    return loaded;
                }
            } catch (Exception e) {
                FastUseMod.LOGGER.warn("Could not read {}, falling back to defaults", path, e);
            }
        }
        FastUseConfig config = new FastUseConfig();
        config.save();
        return config;
    }

    public void save() {
        Path path = path();
        try {
            Files.createDirectories(path.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            FastUseMod.LOGGER.warn("Could not write {}", path, e);
        }
    }
}
