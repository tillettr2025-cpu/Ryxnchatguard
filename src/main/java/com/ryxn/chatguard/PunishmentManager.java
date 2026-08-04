package com.ryxn.chatguard;

import org.bukkit.ChatColor;
import java.time.Duration;
import org.bukkit.entity.Player;

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
                plugin.getConfig()
                        .getString(
                                path + ".action",
                                "MUTE"
                        );

        long duration =
                plugin.getConfig()
                        .getLong(
                                path + ".duration",
                                0L
                        );

        String reason =
                plugin.getConfig()
                        .getString(
                                path + ".reason",
                                "ChatGuard violation"
                        );

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
                    false
            );

            return;
        }

        if (action.equalsIgnoreCase("IP_BAN")) {

            ban(
                    player,
                    reason,
                    true
            );
        }
    }

    private void ban(
            Player player,
            String reason,
            boolean ipBan
    ) {

        String durationText =
                plugin.getConfig()
                        .getString(
                                "punishments.ban-duration",
                                "permanent"
                        );

        if (ipBan) {

            else {
    player.ban(
            reason,
            (java.time.Duration) null,
            "RyxnChatGuard"
    );
}

        } else {
    player.ban(
            reason,
            (java.time.Duration) null,
            "RyxnChatGuard"
    );
}

        player.kickPlayer(
                ChatColor.RED
                        + "You have been banned.\n\n"
                        + ChatColor.WHITE
                        + "Reason: "
                        + reason
                        + "\n"
                        + ChatColor.GRAY
                        + "Duration: "
                        + durationText
        );

        plugin.alertStaff(
                "&c"
                        + player.getName()
                        + " &7was banned by "
                        + "&bRyxnChatGuard"
                        + " &7for &e"
                        + reason
        );
    }

    private String format(
            long milliseconds
    ) {

        long seconds =
                milliseconds / 1000;

        long days =
                seconds / 86400;

        seconds %= 86400;

        long hours =
                seconds / 3600;

        seconds %= 3600;

        long minutes =
                seconds / 60;

        if (days > 0) {
            return days + "d";
        }

        if (hours > 0) {
            return hours + "h";
        }

        if (minutes > 0) {
            return minutes + "m";
        }

        return seconds + "s";
    }
  }
