package com.sql1024.fastuse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FastUseConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static FastUseConfig instance;

    public static final List<String> DEFAULT_ITEMS = List.of("minecraft:end_crystal", "minecraft:glowstone");

    private transient List<Item> resolvedItems;

    public boolean enabled = true;
    /** When true (the default) the use delay is only removed while one of {@link #items} is held. */
    public boolean restrictToItems = true;
    /** Item ids that switch fast use on. */
    public List<String> items = DEFAULT_ITEMS;
    /** Ticks to leave between item uses while fast use is active; 0 removes the delay entirely. */
    public int useDelayTicks = 0;

    public static FastUseConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    /** The items from {@link #items} that exist in the registry, resolved once and kept. */
    public List<Item> activationItems() {
        if (this.resolvedItems == null) {
            List<Item> resolved = new ArrayList<>();
            for (String id : this.items == null ? DEFAULT_ITEMS : this.items) {
                Item item = lookup(id);
                if (item == null) {
                    FastUseMod.LOGGER.warn("Unknown item id in fast_use_mod.json: {}", id);
                } else {
                    resolved.add(item);
                }
            }
            this.resolvedItems = resolved;
        }
        return this.resolvedItems;
    }

    private static Item lookup(String id) {
        Identifier key = id == null ? null : Identifier.tryParse(id);
        if (key == null) {
            return null;
        }
        // ITEM is a defaulted registry, so an unknown id resolves to air rather than nothing.
        Item item = BuiltInRegistries.ITEM.getOptional(key).orElse(null);
        return item != null && key.equals(BuiltInRegistries.ITEM.getKey(item)) ? item : null;
    }

    /** True while the client player holds one of the activation items in either hand. */
    public boolean holdingActivationItem() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return false;
        }
        LocalPlayer player = client.player;
        if (player == null) {
            return false;
        }
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        for (Item item : activationItems()) {
            if (mainHand.is(item) || offHand.is(item)) {
                return true;
            }
        }
        return false;
    }

    /** The master switch: on, and holding one of the activation items if that is required. */
    public boolean active() {
        return this.enabled && (!this.restrictToItems || holdingActivationItem());
    }

    /** The use delay to enforce: the configured one while active, otherwise vanilla's own. */
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
