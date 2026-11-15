package com.pockyl.diegesis.impl.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Vector3fc;

/**
 * Passes on only the quads that face the light: what a flattened shadow is drawn from. A closed model's lit faces cover its shadow
 * once; all its faces cover it twice, its front and back a plate's thickness apart, and the see-through shadow came out darker
 * along the way they overlap (stripes and bands on a carried item's shadow). Face culling cannot sort this out: once flattened,
 * the faces lie in one plane and the GPU draws both sides.
 */
final class LitFaces implements VertexConsumer {
    private static final int FLOATS = 9;
    private final VertexConsumer delegate;
    private final Vector3fc light;
    /** The quad being gathered: x y z r g b a u v per vertex, then the four normals; overlay and light in {@link #packed}. */
    private final float[] vertices = new float[4 * FLOATS + 4 * 3];
    private final int[] packed = new int[4 * 2];
    private int count;
    // A vertex given piece by piece.
    private float x;
    private float y;
    private float z;
    private float red = 1.0F;
    private float green = 1.0F;
    private float blue = 1.0F;
    private float alpha = 1.0F;
    private float u;
    private float v;
    private int overlay;
    private int lightmap;
    private float normalX;
    private float normalY = 1.0F;
    private float normalZ;
    private boolean fixedColor;

    LitFaces(VertexConsumer delegate, Vector3fc light) {
        this.delegate = delegate;
        this.light = light;
    }

    @Override
    public void vertex(float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, int overlay, int light,
            float normalX, float normalY, float normalZ) {
        int at = count * FLOATS;
        vertices[at] = x;
        vertices[at + 1] = y;
        vertices[at + 2] = z;
        vertices[at + 3] = red;
        vertices[at + 4] = green;
        vertices[at + 5] = blue;
        vertices[at + 6] = alpha;
        vertices[at + 7] = u;
        vertices[at + 8] = v;
        int normal = 4 * FLOATS + count * 3;
        vertices[normal] = normalX;
        vertices[normal + 1] = normalY;
        vertices[normal + 2] = normalZ;
        packed[count * 2] = overlay;
        packed[count * 2 + 1] = light;
        if (++count == 4) {
            flush();
        }
    }

    private void flush() {
        count = 0;
        float facing = 0.0F;
        for (int i = 0; i < 4; i++) {
            int normal = 4 * FLOATS + i * 3;
            facing += vertices[normal] * light.x() + vertices[normal + 1] * light.y() + vertices[normal + 2] * light.z();
        }
        if (facing <= 0.0F) {
            return;
        }
        for (int i = 0; i < 4; i++) {
            int at = i * FLOATS;
            int normal = 4 * FLOATS + i * 3;
            delegate.vertex(vertices[at], vertices[at + 1], vertices[at + 2], vertices[at + 3], vertices[at + 4], vertices[at + 5],
                    vertices[at + 6], vertices[at + 7], vertices[at + 8], packed[i * 2], packed[i * 2 + 1], vertices[normal],
                    vertices[normal + 1], vertices[normal + 2]);
        }
    }

    @Override
    public VertexConsumer vertex(double x, double y, double z) {
        this.x = (float) x;
        this.y = (float) y;
        this.z = (float) z;
        return this;
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        if (!fixedColor) {
            this.red = red / 255.0F;
            this.green = green / 255.0F;
            this.blue = blue / 255.0F;
            this.alpha = alpha / 255.0F;
        }
        return this;
    }

    @Override
    public VertexConsumer uv(float u, float v) {
        this.u = u;
        this.v = v;
        return this;
    }

    @Override
    public VertexConsumer overlayCoords(int u, int v) {
        overlay = u & 0xFFFF | v << 16;
        return this;
    }

    @Override
    public VertexConsumer uv2(int u, int v) {
        lightmap = u & 0xFFFF | v << 16;
        return this;
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        normalX = x;
        normalY = y;
        normalZ = z;
        return this;
    }

    @Override
    public void endVertex() {
        vertex(x, y, z, red, green, blue, alpha, u, v, overlay, lightmap, normalX, normalY, normalZ);
    }

    @Override
    public void defaultColor(int red, int green, int blue, int alpha) {
        fixedColor = false;
        color(red, green, blue, alpha);
        fixedColor = true;
    }

    @Override
    public void unsetDefaultColor() {
        fixedColor = false;
    }
}
