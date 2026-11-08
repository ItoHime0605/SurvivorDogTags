package tw.zonezero.survivordogtags.game;

/**
 * Packet-safe text and numeric bounds for an ID card. This is deliberately
 * Minecraft-independent and is unit tested without a running game.
 */
public final class DogTagProfileSafety {
    private DogTagProfileSafety() {}

    /** The profile packet writes player names with a strict 48-char limit. */
    public static String safeName(String name) {
        if (name == null || name.isBlank()) return "Unknown";
        String cleaned = name.strip();
        if (cleaned.length() <= 48) return cleaned;
        int last = 48;
        if (Character.isHighSurrogate(cleaned.charAt(last - 1))) last--;
        return cleaned.substring(0, last);
    }

    public static int safeLevel(int level) {
        return Math.max(0, Math.min(5, level));
    }

    public static int safeKills(int kills) {
        return Math.max(0, kills);
    }
}
