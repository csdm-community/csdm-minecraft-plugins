package tv.csdm.minecraft.visuals;

final class ScalePolicy {
    private ScalePolicy() {}
    static double scale(boolean self, boolean fullsize, double others) { return self || fullsize ? 1.0 : others; }
}
