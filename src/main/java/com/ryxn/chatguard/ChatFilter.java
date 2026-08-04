package com.ryxn.chatguard;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ChatFilter implements Listener {

    private final RyxnChatGuard plugin;

    private final Map<UUID, String> lastMessage =
            new ConcurrentHashMap<>();

    private final Map<UUID, Long> lastMessageTime =
            new ConcurrentHashMap<>();

    private final Map<UUID, Integer> spamCount =
            new ConcurrentHashMap<>();

    private final Map<UUID, Long> spamWindow =
            new ConcurrentHashMap<>();

    public ChatFilter(RyxnChatGuard plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {

        Player player = event.getPlayer();

        if (!plugin.isGuardEnabled()) {
            return;
        }

        if (player.hasPermission("ryxnchatguard.bypass")) {
            return;
        }

        /*
         * MUTED PLAYER
         */
        if (plugin.isMuted(player)) {

            event.setCancelled(true);

            long remaining =
                    plugin.getMuteRemaining(player);

            plugin.message(
                    player,
                    "&cYou are muted for another &f"
                            + formatDuration(remaining)
                            + "&c."
            );

            return;
        }

        String original = event.getMessage();

        String normalized = normalize(original);

        if (normalized.isEmpty()) {
            event.setCancelled(true);
            return;
        }

        /*
         * CHAT SPAM
         */
        if (isSpam(player, normalized)) {

            event.setCancelled(true);

            handleViolation(
                    player,
                    "spam",
                    original
            );

            return;
        }

        /*
         * HATE SPEECH / RACISM
         */
        if (matchesList(
                normalized,
                "filters.hate-speech"
        )) {

            event.setCancelled(true);

            handleViolation(
                    player,
                    "hate-speech",
                    original
            );

            return;
        }

        /*
         * SELF-HARM ENCOURAGEMENT
         */
        if (matchesList(
                normalized,
                "filters.self-harm"
        )) {

            event.setCancelled(true);

            handleViolation(
                    player,
                    "self-harm",
                    original
            );

            return;
        }

        /*
         * LIGHT ADVERTISING
         */
        if (matchesList(
                normalized,
                "filters.light-advertising"
        )) {

            event.setCancelled(true);

            handleViolation(
                    player,
                    "light-advertising",
                    original
            );

            return;
        }

        /*
         * HEAVY ADVERTISING
         *
         * Examples:
         * play.example.com
         * example.net
         * server.example.org
         * 123.123.123.123
         */
        if (isHeavyAdvertisement(original)) {

            event.setCancelled(true);

            handleViolation(
                    player,
                    "heavy-advertising",
                    original
            );

            return;
        }

        /*
         * CHEAT / HACK CLIENT MENTIONS
         */
        if (isCheatMention(normalized)) {

            event.setCancelled(true);

            handleViolation(
                    player,
                    "cheating",
                    original
            );

            return;
        }

        /*
         * ACCOUNT SELLING
         */
        if (matchesList(
                normalized,
                "filters.account-selling"
        )) {

            event.setCancelled(true);

            handleViolation(
                    player,
                    "account-selling",
                    original
            );

            return;
        }

        /*
         * DDoS / DOXX / SWAT / DEATH THREATS
         */
        if (matchesList(
                normalized,
                "filters.threats"
        )) {

            event.setCancelled(true);

            handleViolation(
                    player,
                    "threats",
                    original
            );

            return;
        }

        /*
         * DOXXING / PERSONAL INFORMATION
         */
        if (matchesList(
                normalized,
                "filters.doxxing"
        )) {

            event.setCancelled(true);

            handleViolation(
                    player,
                    "doxxing",
                    original
            );

            return;
        }

        /*
         * Save normal message information.
         */
        lastMessage.put(
                player.getUniqueId(),
                normalized
        );

        lastMessageTime.put(
                player.getUniqueId(),
                System.currentTimeMillis()
        );
    }

    /*
     * Normalize text.
     *
     * Examples:
     *
     * H.A.C.K
     * h-a-c-k
     * h a c k
     * H4CK
     *
     * become easier to detect.
     */
    private String normalize(String message) {

        if (message == null) {
            return "";
        }

        String text =
                message.toLowerCase(Locale.ROOT);

        /*
         * Remove spaces and common separators.
         */
        text = text.replaceAll(
                "[\\s._\\-]+",
                ""
        );

        /*
         * Basic leetspeak.
         */
        text = text
                .replace("0", "o")
                .replace("1", "i")
                .replace("3", "e")
                .replace("4", "a")
                .replace("5", "s")
                .replace("7", "t")
                .replace("@", "a")
                .replace("$", "s");

        return text;
    }

    /*
     * CHAT SPAM CHECK
     *
     * Four repeated messages within
     * a short period counts as spam.
     */
    private boolean isSpam(
            Player player,
            String message
    ) {

        UUID uuid =
                player.getUniqueId();

        long now =
                System.currentTimeMillis();

        String previous =
                lastMessage.get(uuid);

        Long previousTime =
                lastMessageTime.get(uuid);

        if (previous != null
                && previous.equals(message)
                && previousTime != null
                && now - previousTime <= 5000) {

            Long window =
                    spamWindow.get(uuid);

            if (window == null
                    || now - window > 10000) {

                spamWindow.put(
                        uuid,
                        now
                );

                spamCount.put(
                        uuid,
                        1
                );

            } else {

                int count =
                        spamCount.merge(
                                uuid,
                                1,
                                Integer::sum
                        );

                /*
                 * Three repeats after the
                 * original message = 4 total.
                 */
                if (count >= 3) {
                    return true;
                }
            }

        } else {

            spamCount.put(
                    uuid,
                    0
            );

            spamWindow.put(
                    uuid,
                    now
            );
        }

        lastMessage.put(
                uuid,
                message
        );

        lastMessageTime.put(
                uuid,
                now
        );

        return false;
    }

    /*
     * CONFIG LIST CHECK
     */
    private boolean matchesList(
            String message,
            String path
    ) {

        List<String> list =
                plugin.getConfig()
                        .getStringList(path);

        if (list == null
                || list.isEmpty()) {
            return false;
        }

        for (String word : list) {

            if (word == null
                    || word.isEmpty()) {
                continue;
            }

            String normalizedWord =
                    normalize(word);

            if (normalizedWord.isEmpty()) {
                continue;
            }

            if (message.contains(normalizedWord)) {
                return true;
            }
        }

        return false;
    }

    /*
     * HEAVY ADVERTISING CHECK
     */
    private boolean isHeavyAdvertisement(
            String message
    ) {

        if (message == null) {
            return false;
        }

        String lower =
                message.toLowerCase(Locale.ROOT);

        /*
         * IPv4 addresses.
         */
        if (lower.matches(
                ".*\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b.*"
        )) {
            return true;
        }

        /*
         * Minecraft/server domains.
         */
        if (lower.matches(
                ".*\\b[a-z0-9-]+\\."
                        + "(com|net|org|gg|me|xyz|"
                        + "tk|ml|cf|ga|io|co|us)\\b.*"
        )) {
            return true;
        }

        /*
         * Common server-hosting domains.
         */
        String[] hostingDomains = {
                ".aternos.",
                ".minehut.",
                ".server.pro",
                ".exaroton.",
                ".playit."
        };

        for (String domain : hostingDomains) {

            if (lower.contains(domain)) {
                return true;
            }
        }

        /*
         * URL-style advertisements.
         */
        return lower.contains("://")
                || lower.contains("www.");
    }

    /*
     * HACK CLIENT / CHEAT CHECK
     */
    private boolean isCheatMention(
            String message
    ) {

        String[] cheats = {

                "wurst",
                "meteor",
                "meteorclient",
                "impact",
                "impactclient",
                "aristois",
                "liquidbounce",
                "liquidbounceclient",
                "sigma",
                "sigmaj",
                "vape",
                "vapelite",
                "vapeclient",
                "future",
                "futureclient",
                "rise",
                "riseclient",
                "tenacity",
                "moonclient",
                "novo",
                "novoline",
                "horion",
                "wolfram",
                "bleachhack",
                "inertia",
                "kami",
                "baritone",

                "xray",
                "xraymod",
                "freecam",
                "reachhack",
                "killaura",
                "autoclicker",
                "aimbot",
                "triggerbot",
                "antiknockback",
                "velocity",
                "speedhack",
                "flyhack",
                "scaffold",
                "bhop",
                "esp",
                "wallhack",
                "noclip",
                "nofall",
                "inventorymove",
                "blink",
                "timer",
                "fastplace",
                "fastbreak",
                "cheststealer",
                "autototem",
                "autopot",
                "criticals"
        };

        for (String cheat : cheats) {

            if (message.contains(cheat)) {
                return true;
            }
        }

        /*
         * Common shorthand.
         */
        if (message.equals("gmc")
                || message.contains("gmcclient")
                || message.contains("gmcmod")) {

            return true;
        }

        /*
         * Minecraft command-style
         * cheat mentions.
         */
        if (message.contains("minecraftxray")
                || message.contains("minecraftxraymod")
                || message.contains("xraymod")) {

            private void handleViolation(
        Player player,
        String type,
        String message
) {
    plugin.getPunishmentManager().handleViolation(
            player,
            type,
            message
    );
}
    private String formatDuration(
            long milliseconds
    ) {

        if (milliseconds <= 0) {
            return "0s";
        }

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

        seconds %= 60;

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

        if (seconds > 0) {
            result.append(seconds)
                    .append("s");
        }

        return result.toString().trim();
    }
  }
