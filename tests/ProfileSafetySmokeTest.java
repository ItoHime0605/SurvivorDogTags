import tw.zonezero.survivordogtags.game.DogTagProfileSafety;

public final class ProfileSafetySmokeTest {
    static void yes(boolean ok) { if (!ok) throw new AssertionError(); }
    public static void main(String[] args) {
        yes(DogTagProfileSafety.safeName(null).equals("Unknown"));
        yes(DogTagProfileSafety.safeName(" ").equals("Unknown"));
        yes(DogTagProfileSafety.safeName("Survivor_001").equals("Survivor_001"));
        yes(DogTagProfileSafety.safeName("a".repeat(200)).length() == 48);
        yes(DogTagProfileSafety.safeName("a".repeat(47) + "😀" + "z").length() == 47);
        yes(DogTagProfileSafety.safeLevel(-500) == 0);
        yes(DogTagProfileSafety.safeLevel(500) == 5);
        yes(DogTagProfileSafety.safeKills(-50) == 0);
        System.out.println("PASS: ID card packet-safe name, level and kill counters");
    }
}
