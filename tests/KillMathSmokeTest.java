import tw.zonezero.survivordogtags.game.DogTagKillMath;

public class KillMathSmokeTest {
    private static void eq(int got, int want) {
        if (got != want) throw new AssertionError("got=" + got + " want=" + want);
    }
    public static void main(String[] args) {
        eq(DogTagKillMath.baseline(-10, -20), 0);
        eq(DogTagKillMath.baseline(35, 100), 100);
        eq(DogTagKillMath.baseline(120, 100), 120);
        eq(DogTagKillMath.combine(180, 110, 150, 170), 180);
        eq(DogTagKillMath.combine(170, 110, 185, 170), 185);
        eq(DogTagKillMath.combine(0, 0, 0, 50), 50);
        eq(DogTagKillMath.baseline(Integer.MAX_VALUE, 1), Integer.MAX_VALUE);
        System.out.println("PASS: historical vanilla + legacy zombie kills imported without additive double counting");
    }
}
