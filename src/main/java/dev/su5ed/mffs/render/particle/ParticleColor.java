package dev.su5ed.mffs.render.particle;

// Packets transmitting ParticleColor use ordinal (int) serialization.

// Each color entry is (red, green, blue) in the range [0..1].
// Values can be written as X / 255.0F for familiar 0-255 range, or as a plain float (0.0 – 1.0).
// Pure red = (1, 0, 0) | pure blue = (0, 0, 1) | pure cyan = (0, 1, 1).
// Mixing: increase green toward 1 to shift blue → cyan; decrease red/green to deepen/saturate.
public enum ParticleColor {
    BLUE_BEAM(0, 165 / 255.0F, 1),     // red=0, green≈0.71 (cyan tint), blue=1
    BLUE_FIELD(52 / 255.0F, 254 / 255.0F, 1),
    RED(1, 0, 0),
    WHITE(1, 1, 1);

    private final float red;
    private final float green;
    private final float blue;

    ParticleColor(float red, float green, float blue) {
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    public float getRed() {
        return this.red;
    }

    public float getGreen() {
        return this.green;
    }

    public float getBlue() {
        return this.blue;
    }
}
