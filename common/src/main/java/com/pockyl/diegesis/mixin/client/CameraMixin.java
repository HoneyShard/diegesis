package com.pockyl.diegesis.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.pockyl.diegesis.impl.client.focus.FocusController;

/**
 * Moves the camera to the station while the player works at it, and gives it the station's field of view. Neither loader has a
 * camera position event (NeoForge's ViewportEvent only turns the camera). Since Minecraft 26.x the camera is placed once a frame by
 * {@code alignWithEntity}, before the frame's culling is worked out from it. Runs after vanilla and other mods placed the camera, and only while
 * focused, so third-person and camera mods keep working the rest of the time. Its priority makes it the last hook at the
 * end of the setup: camera mods that place the camera there too (Perspective API, under Leawind's Third Person) ran after it
 * and turned the station's view away. Their pose is where the flight starts and ends.
 */
@Mixin(value = Camera.class, priority = 2000)
abstract class CameraMixin {
    /** Blocks between the camera and the eyes before the player's body is drawn. */
    private static final double BODY_CLEARANCE = 0.75;

    @Shadow
    private boolean detached;

    @Shadow
    protected abstract void setPosition(Vec3 pos);

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Shadow
    public abstract Vec3 position();

    @Shadow
    public abstract float xRot();

    @Shadow
    public abstract float yRot();

    @Inject(method = "alignWithEntity", at = @At("TAIL"))
    private void diegesis$focusCamera(float partialTicks, CallbackInfo ci) {
        FocusController focus = FocusController.get();
        if (!focus.isActive()) {
            return;
        }
        focus.frame();
        boolean detachedView = detached;
        Vec3 eye = position();
        FocusController.CameraPose pose = focus.cameraPose(eye, yRot(), xRot());
        if (pose == null) {
            return;
        }
        setRotation(pose.yaw(), pose.pitch());
        setPosition(pose.position());
        // Detached draws the player's own body - but only once the camera is out of the head, never from inside it, and
        // never when the scene hides it.
        detached = !focus.options().hidePlayer() && (detachedView || pose.position().distanceToSqr(eye) > BODY_CLEARANCE * BODY_CLEARANCE);
    }

    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
    private void diegesis$focusFov(float partialTicks, CallbackInfoReturnable<Float> cir) {
        FocusController focus = FocusController.get();
        if (focus.isActive()) {
            cir.setReturnValue((float) focus.fov(cir.getReturnValueF()));
        }
    }
}
