import tw.zonezero.survivordogtags.game.DogTagShotLedger;
import java.util.UUID;

public class ShotLedgerSmokeTest {
    private static void yes(boolean cond, String reason) {
        if (!cond) throw new AssertionError(reason);
    }
    public static void main(String[] args) {
        UUID shooter = UUID.randomUUID();
        UUID targetA = UUID.randomUUID();
        UUID targetB = UUID.randomUUID();
        long first = DogTagShotLedger.beginShot(shooter, 500);
        yes(DogTagShotLedger.currentShot(shooter, 500) == first, "pellets inherit token");
        yes(DogTagShotLedger.markHit(shooter, targetA, "gun:shotgun", first, 501), "first pellet hits");
        for (int p = 0; p < 15; p++)
            yes(!DogTagShotLedger.markHit(shooter, targetA, "gun:shotgun", first, 501), "pellets duplicate");
        yes(DogTagShotLedger.markHit(shooter, targetB, "gun:shotgun", first, 501), "pierce new target");
        long second = DogTagShotLedger.beginShot(shooter, 500); // same tick, separate trigger
        yes(first != second, "same-tick shots must have distinct ids");
        yes(DogTagShotLedger.markHit(shooter, targetA, "gun:shotgun", second, 500), "second shot counts");
        yes(!DogTagShotLedger.markHit(shooter, targetA, "gun:shotgun", second, 500), "second shot pellets dedupe");
        yes(DogTagShotLedger.currentShot(shooter, 501) == 0, "previous-tick token must not be reused");
        yes(DogTagShotLedger.currentShot(shooter, 504) == 0, "stale shot not reused");
        DogTagShotLedger.forgetShooter(shooter);
        yes(DogTagShotLedger.currentShot(shooter, 505) == 0, "logout clears token");
        long third = DogTagShotLedger.beginShot(shooter, 3); // simulates clock reset
        yes(DogTagShotLedger.markHit(shooter, targetA, "gun:pistol", third, 4), "restart clears hit cache");
        System.out.println("PASS: pellet de-duplication, penetrating shots, and two shots in the same tick");
    }
}
