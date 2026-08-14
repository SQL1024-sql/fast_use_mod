package com.sql1024.fastuse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Settings for Fast Use, stored in {@code config/fast_use_mod.json}.
 *
 * <p>Every getter folds in the {@link #enabled} master switch so the mixins only
 * ever have to ask one question.
 */
public class FastUseConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static FastUseConfig instance;

	/** Master switch, flipped by the in-game toggle keybind. */
	public boolean enabled = true;

	/** Allow right-click (place/use) to go through while a block is being broken. */
	public boolean placeWhileMining = true;

	/** Allow left-click block breaking to continue while an item is being used. */
	public boolean mineWhileUsing = true;

	/** Ticks forced between two item uses. Vanilla is 4; 0 means every tick. */
	public int useDelayTicks = 0;

	/** Drop the 5 tick cooldown the client applies after instantly breaking a block. */
	public boolean removeBreakDelay = true;

	/** Drop the 10 tick penalty applied after swinging at nothing. */
	public boolean removeMissDelay = true;

	public static FastUseConfig get() {
		if (instance == null) {
			instance = load();
		}

		return instance;
	}

	public boolean placeWhileMining() {
		return enabled && placeWhileMining;
	}

	public boolean mineWhileUsing() {
		return enabled && mineWhileUsing;
	}

	public boolean removeBreakDelay() {
		return enabled && removeBreakDelay;
	}

	public boolean removeMissDelay() {
		return enabled && removeMissDelay;
	}

	/** Vanilla's 4 tick gap once the mod is off, the configured gap while it is on. */
	public int useDelayTicks() {
		return enabled ? Math.max(0, useDelayTicks) : Integer.MAX_VALUE;
	}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("fast_use_mod.json");
	}

	private static FastUseConfig load() {
		Path path = path();

		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path)) {
				FastUseConfig loaded = GSON.fromJson(reader, FastUseConfig.class);

				if (loaded != null) {
					return loaded;
				}
			} catch (Exception e) {
				FastUseMod.LOGGER.warn("Could not read {}, falling back to defaults", path, e);
			}
		}

		FastUseConfig fresh = new FastUseConfig();
		fresh.save();
		return fresh;
	}

	public void save() {
		Path path = path();

		try {
			Files.createDirectories(path.getParent());

			try (Writer writer = Files.newBufferedWriter(path)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException e) {
			FastUseMod.LOGGER.warn("Could not write {}", path, e);
		}
	}
}
