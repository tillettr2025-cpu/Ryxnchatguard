package com.ryxn.chatguard;

import org.bukkit.entity.Player;

import java.time.Duration;

public final class PunishmentManager {

    private final RyxnChatGuard plugin;

    public PunishmentManager(RyxnChatGuard plugin) {
        this.plugin = plugin;
    }

    public void punish(
            Player player,
            String category,
            int offence
    ) {

        String path =
                "punishments."
                        + category
                        + "."
                        + offence;

        String action =
                plugin.getConfig().getString(
                        path + ".action",
                        "MUTE"
                );

        long duration =
                plugin.getConfig().getLong(
                        path + ".duration",
                        0L
                );

        String reason =
                plugin.getConfig().getString(
                        path + ".reason",
                        "ChatGuard violation"
                );

        if (action == null) {
            action = "MUTE";
        }

        if (action.equalsIgnoreCase("WARN")) {

            plugin.message(
                    player,
                    "&eWarning: &f" + reason
            );

            return;
        }

        if (action.equalsIgnoreCase("MUTE")) {

            if (duration <= 0) {
                return;
            }

            plugin.mute(
                    player,
                    duration
            );

            plugin.message(
                    player,
                    "&cYou have been muted for &f"
                            + format(duration)
                            + "&c."
            );

            plugin.message(
                    player,
                    "&7Reason: &f" + reason
            );

            return;
        }

        if (action.equalsIgnoreCase("BAN")) {

            ban(
                    player,
                    reason,
                    false,
                    duration
            );

            return;
        }

        if (action.equalsIgnoreCase("IP_BAN")) {

            ban(
                    player,
                    reason,
                    true,
                    duration
            );

            return;
        }

        plugin.getLogger().warning(
                "Unknown punishment action '"
                        + action
                        + "' for "
                        + category
                        + " offence "
                        + offence
        );
    }

    private void ban(
            Player player,
            String reason,
            boolean ipBan,
            long duration
    ) {

        /*
         * Paper 1.21+ has multiple ban()
         * overloads. We explicitly use Duration
         * so Java cannot confuse it with Instant.
         */

        if (duration <= 0) {

            player.ban(
                    reason,
                    Duration.ZERO,
                    "RyxnChatGuard"
            );

        } else {

            player.ban(
                    reason,
                    Duration.ofMillis(duration),
                    "RyxnChatGuard"
            );
        }

        /*
         * IP-ban handling.
         *
         * The normal Player.ban() call bans the
         * player account. If IP banning is enabled
         * in the server implementation, use the
         * server's IP ban system as well.
         */
        if (ipBan) {

            String address =
                    player.getAddress() != null
                            && player.getAddress().getAddress() != null
                            ? player.getAddress()
                                    .getAddress()
                                    .getHostAddress()
                            : null;

            if (address != null) {

                plugin.getServer()
                        .getBanList(
                                org.bukkit.BanList.Type.IP
                        )
                        .addBan(
                                address,
                                reason,
                                duration <= 0
                                        ? null
                                        : new java.util.Date(
                                                System.currentTimeMillis()
                                                        + duration
                                        ),
                                "RyxnChatGuard"
                        );
            }
        }

        player.kickPlayer(
                "§cYou have been banned.\n\n"
                        + "§7Reason: §f"
                        + reason
        );
    }

    private String format(long milliseconds) {

        long seconds =
                Math.max(
                        0L,
                        milliseconds / 1000L
                );

        long days =
                seconds / 86400L;

        seconds %= 86400L;

        long hours =
                seconds / 3600L;

        seconds %= 3600L;

        long minutes =
                seconds / 60L;

        seconds %= 60L;

        StringBuilder result =
                new StringBuilder();

        if (days > 0) {
            result.append(days)
                    .append("d ");
        }

        if (hours > 0) {
            result.append(hours)
                    .append("h ");
        }

        if (minutes > 0) {
            result.append(minutes)
                    .append("m ");
        }

        if (seconds > 0 || result.length() == 0) {
            result.append(seconds)
                    .append("s");
        }

        return result.toString().trim();
    }

    /*
     * Used by ChatFilter.
     */
    public void handleViolation(
            Player player,
            String category,
            String message
    ) {

        if (player == null
                || category == null) {
            return;
        }

        int offence =
                plugin.addOffence(player);

        punish(
                player,
                category,
                offence
        );

        plugin.alertStaff(
                "&c"
                        + player.getName()
                        + " &7triggered &e"
                        + category
                        + " &7(offence &c"
                        + offence
                        + "&7)"
        );
    }
        }
