package com.pockyl.diegesis.impl.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;

import com.pockyl.diegesis.api.client.NodePainter;

/**
 * What a {@link NodePainter#smooth() smooth} painter draws. Before Minecraft 26.x it was kept from the level render and drawn over
 * the finished frame from the focus screen, four times as fine each way into a target of its own and scaled down, so its edges had
 * no steps; that pass worked on OpenGL's state directly, which the game's new renderer (OpenGL or Vulkan) no longer allows. Until it
 * is drawn through the new renderer, smooth painters are drawn with the scene, as they were where the fine targets failed: the same
 * picture, without the smoothing, and a block beside the station can cover them.
 */
public final class SmoothPass {
    private SmoothPass() {
    }

    /** A new level frame. {@code view}: the level's model-view matrix, the camera's turn. */
    static void begin(Matrix4f projection, Matrix4f view) {
    }

    /** False: the painter is drawn with the scene (see the class). */
    static boolean defer(NodePainter painter, PoseStack poseStack, int light) {
        return false;
    }

    /** Draws what was kept over the frame; called first thing by the focus screen. Nothing is kept for now. */
    public static void draw() {
    }

    /** The focus screen closed. */
    public static void release() {
    }
}
