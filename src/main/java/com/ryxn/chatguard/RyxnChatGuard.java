package com.ryxn.chatguard;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RyxnChatGuard extends JavaPlugin {

    private final Map<UUID, Integer> offences =
            new ConcurrentHashMap<>();

    private final Map<UUID, Long> mutedUntil =
            new ConcurrentHashMap<>();

    private ChatFilter chatFilter;
    private PunishmentManager punishmentManager;

    private boolean enabled;
    private boolean alerts;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        enabled = getConfig().getBoolean(
                "enabled",
                true
        );

        alerts = getConfig().getBoolean(
                "alerts.enabled",
                true
        );

        punishmentManager =
                new PunishmentManager(this);

        chatFilter =
                new ChatFilter(this);

        getServer()
                .getPluginManager()
                .registerEvents(
                        chatFilter,
                        this
                );

        if (getCommand("rcg") != null) {
            getCommand("rcg").setExecutor(this);
        }

        getLogger().info(
                "================================"
        );

        getLogger().info(
                "RyxnChatGuard enabled"
        );

        getLogger().info(
                "Version: " +
                getDescription().getVersion()
        );

        getLogger().info(
                "Made by Ryxn"
        );

        getLogger().info(
                "Paper 1.21+"
        );

        getLogger().info(
                "================================"
        );
    }

    @Override
    public void onDisable() {

        getLogger().info(
                "RyxnChatGuard disabled."
        );
    }

    public boolean isGuardEnabled() {
        return enabled;
    }

    public void setGuardEnabled(
            boolean enabled
    ) {
        this.enabled = enabled;
    }

    public boolean areAlertsEnabled() {
        return alerts;
    }

    public void setAlertsEnabled(
            boolean alerts
    ) {
        this.alerts = alerts;
    }

    public int getOffences(
            Player player
    ) {

        return offences.getOrDefault(
                player.getUniqueId(),
                0
        );
    }

    public int addOffence(
            Player player
    ) {

        UUID uuid =
                player.getUniqueId();

        return offences.merge(
                uuid,
                1,
                Integer::sum
        );
    }

    public void resetOffences(
            Player player
    ) {

        offences.remove(
                player.getUniqueId()
        );
    }

    public void mute(
            Player player,
            long duration
    ) {

        long expiry =
                System.currentTimeMillis()
                        + duration;

        mutedUntil.put(
                player.getUniqueId(),
                expiry
        );
    }

    public void unmute(
            Player player
    ) {

        mutedUntil.remove(
                player.getUniqueId()
        );
    }

    public boolean isMuted(
            Player player
    ) {

        Long expiry =
                mutedUntil.get(
                        player.getUniqueId()
                );

        if (expiry == null) {
            return false;
        }

        if (expiry <= System.currentTimeMillis()) {

            mutedUntil.remove(
                    player.getUniqueId()
            );

            return false;
        }

        return true;
    }

    public long getMuteRemaining(
            Player player
    ) {

        Long expiry =
                mutedUntil.get(
                        player.getUniqueId()
                );

        if (expiry == null) {
            return 0L;
        }

        return Math.max(
                0L,
                expiry -
                        System.currentTimeMillis()
        );
    }

    public ChatFilter getChatFilter() {
        return chatFilter;
    }

    public PunishmentManager getPunishmentManager() {
        return punishmentManager;
    }

    /*
     * Called by ChatFilter when a player
     * violates a chat rule.
     */
    public void handleChatViolation(
            Player player,
            String category,
            String message
    ) {

        if (player == null
                || category == null) {
            return;
        }

        if (punishmentManager == null) {
            return;
        }

        punishmentManager.handleViolation(
                player,
                category,
                message
        );
    }

    public void alertStaff(
            String message
    ) {

        if (!alerts) {
            return;
        }

        String formatted =
                ChatColor.translateAlternateColorCodes(
                        '&',
                        "&8[&bRyxnChatGuard&8] "
                                + message
                );

        getLogger().warning(
                ChatColor.stripColor(formatted)
        );

        for (Player player :
                getServer().getOnlinePlayers()) {

            if (player.hasPermission(
                    "ryxnchatguard.alerts"
            )) {

                player.sendMessage(
                        formatted
                );
            }
        }
    }

    public void message(
            Player player,
            String message
    ) {

        player.sendMessage(
                ChatColor.translateAlternateColorCodes(
                        '&',
                        "&8[&bRyxnChatGuard&8] "
                                + message
                )
        );
    }
}
