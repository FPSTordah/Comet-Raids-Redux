package com.cometmod.commands;

import com.hypixel.hytale.server.core.permissions.HytalePermissions;

public final class CometPermissions {

    private CometPermissions() {
    }

    public static final String BASE = HytalePermissions.fromCommand("comet");
    public static final String ADMIN = HytalePermissions.fromCommand("comet.admin");

    public static final String SPAWN = HytalePermissions.fromCommand("comet.spawn");
    public static final String TEST = HytalePermissions.fromCommand("comet.test");
    public static final String ZONE = HytalePermissions.fromCommand("comet.zone");
    public static final String DESTROY_ALL = HytalePermissions.fromCommand("comet.destroyall");
    public static final String RELOAD = HytalePermissions.fromCommand("comet.reload");
    public static final String SET_SPAWN = HytalePermissions.fromCommand("comet.setspawn");
    public static final String SCHEDULE_SPAWN = HytalePermissions.fromCommand("comet.schedulespawn");
    public static final String REMOVE_SPAWN = HytalePermissions.fromCommand("comet.removespawn");
    public static final String LIST_SPAWNS = HytalePermissions.fromCommand("comet.listspawns");
}
