package com.cometmod.spawn;

import com.cometmod.*;
import com.cometmod.commands.*;
import com.cometmod.services.*;
import com.cometmod.spawn.*;
import com.cometmod.systems.*;
import com.cometmod.wave.*;


import static com.cometmod.config.parser.ConfigJson.extractArrayObjects;
import static com.cometmod.config.parser.ConfigJson.extractIntValue;
import static com.cometmod.config.parser.ConfigJson.extractJsonArray;
import static com.cometmod.config.parser.ConfigJson.extractStringValue;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.cometmod.config.validation.ConfigValidationReport;
import com.cometmod.config.validation.ConfigValidator;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Minimal fixed spawn manager retained so the project compiles.
 * <p>
 * This implementation only loads and exposes configured spawn points from
 * fixed_spawns.json. Scheduler behavior is intentionally left disabled in this
 * source snapshot.
 */
public class FixedSpawnManager {

    private static final Logger LOGGER = Logger.getLogger("FixedSpawnManager");
    private static final String FILE_NAME = "fixed_spawns.json";

    private final List<SpawnPoint> spawnPoints = new ArrayList<>();
    private volatile World world;
    private volatile Store<EntityStore> store;

    public void load() {
        synchronized (spawnPoints) {
            spawnPoints.clear();
        }

        File file = resolveConfigFile();
        if (!file.exists()) {
            LOGGER.info("Fixed spawn file not found at " + file.getAbsolutePath() + " (continuing with 0 points)");
            return;
        }

        try {
            String json = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            logValidationReport(ConfigValidator.validateFixedSpawns(json));
            String spawnsArray = extractJsonArray(json, "spawns");
            if (spawnsArray == null) {
                LOGGER.warning("No spawns[] array found in fixed_spawns.json. Continuing with 0 points.");
                return;
            }

            List<String> spawnObjects = extractArrayObjects(spawnsArray);
            for (String obj : spawnObjects) {
                Integer xVal = extractIntValue(obj, "x");
                Integer yVal = extractIntValue(obj, "y");
                Integer zVal = extractIntValue(obj, "z");
                if (xVal == null || yVal == null || zVal == null) {
                    LOGGER.warning("Skipping fixed spawn entry missing x/y/z: " + obj);
                    continue;
                }

                int x = xVal;
                int y = yVal;
                int z = zVal;
                String name = extractStringValue(obj, "name");
                if (name == null || name.trim().isEmpty()) {
                    name = "Spawn " + (sizeUnsafe() + 1);
                }
                synchronized (spawnPoints) {
                    spawnPoints.add(new SpawnPoint(name, x, y, z));
                }
            }
            LOGGER.info("Loaded " + getSpawnPoints().size() + " fixed spawn point(s) from " + file.getAbsolutePath());
        } catch (Exception e) {
            LOGGER.warning("Failed to load fixed spawn file: " + e.getMessage());
        }
    }

    private void logValidationReport(ConfigValidationReport report) {
        if (report == null || report.isClean()) {
            return;
        }
        for (String info : report.getInfos()) {
            LOGGER.info("[ConfigValidation][fixed_spawns.json] " + info);
        }
        for (String warning : report.getWarnings()) {
            LOGGER.warning("[ConfigValidation][fixed_spawns.json] " + warning);
        }
        for (String error : report.getErrors()) {
            LOGGER.warning("[ConfigValidation][fixed_spawns.json] ERROR: " + error);
        }
    }

    public void reload() {
        load();
    }

    public void start(World world, Store<EntityStore> store) {
        this.world = world;
        this.store = store;
        LOGGER.info("FixedSpawnManager started with " + getSpawnPoints().size() +
                " configured points (scheduling is disabled in this source snapshot).");
    }

    public void stop() {
        this.world = null;
        this.store = null;
    }

    public List<SpawnPoint> getSpawnPoints() {
        synchronized (spawnPoints) {
            return Collections.unmodifiableList(new ArrayList<>(spawnPoints));
        }
    }

    public World getWorld() {
        return world;
    }

    public Store<EntityStore> getStore() {
        return store;
    }

    private int sizeUnsafe() {
        synchronized (spawnPoints) {
            return spawnPoints.size();
        }
    }

    private File resolveConfigFile() {
        try {
            CometModPlugin plugin = CometModPlugin.getInstance();
            if (plugin != null) {
                Path pluginFile = plugin.getFile();
                if (pluginFile != null && pluginFile.getParent() != null) {
                    File inPluginDir = pluginFile.getParent().resolve(FILE_NAME).toFile();
                    if (inPluginDir.exists()) {
                        return inPluginDir;
                    }
                }
            }
        } catch (Exception ignored) {
            // Fall back below.
        }
        return new File(FILE_NAME);
    }

    public static final class SpawnPoint {
        private final String name;
        private final int x;
        private final int y;
        private final int z;

        public SpawnPoint(String name, int x, int y, int z) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public String getName() {
            return name;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getZ() {
            return z;
        }
    }
}
