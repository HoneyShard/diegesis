package com.pockyl.diegesis.impl.client.render;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Whether a shader pack draws its shadow map right now (Iris, Oculus). Entities are drawn once more for it, from the sun: the clear
 * view's cone lies in the camera's space, so they cast their whole shadow and are left as they are.
 */
final class ShaderShadows {
    private static final MethodHandle RENDERING_SHADOW_PASS = find();

    private ShaderShadows() {
    }

    static boolean drawing() {
        if (RENDERING_SHADOW_PASS == null) {
            return false;
        }
        try {
            return (boolean) RENDERING_SHADOW_PASS.invoke();
        } catch (Throwable e) {
            return false;
        }
    }

    private static MethodHandle find() {
        try {
            Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi", false, ShaderShadows.class.getClassLoader());
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            MethodHandle instance = lookup.findStatic(api, "getInstance", MethodType.methodType(api));
            MethodHandle shadowPass = lookup.findVirtual(api, "isRenderingShadowPass", MethodType.methodType(boolean.class));
            return MethodHandles.foldArguments(shadowPass, instance);
        } catch (ReflectiveOperationException | LinkageError e) {
            return null;
        }
    }
}
