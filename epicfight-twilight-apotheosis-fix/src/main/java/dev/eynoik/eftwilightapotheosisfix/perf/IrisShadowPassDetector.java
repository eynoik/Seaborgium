package dev.eynoik.eftwilightapotheosisfix.perf;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Optional Iris integration without a hard Iris dependency.
 *
 * Epic Fight's renderer is CPU-heavy because SkinnedMesh performs pose/skinning work on the
 * Render Thread. With shader packs, Iris renders living entities again into the shadow map, so
 * the same Epic Fight entity can pay the skinned-render cost twice per frame.
 *
 * Iris exposes ShadowRenderingState#areShadowsCurrentlyBeingRendered(). Resolve it once through a
 * MethodHandle and keep the hot-path check allocation-free. If Iris is absent or changes the API,
 * this helper permanently falls back to false and Epic Fight behaves exactly as before.
 */
public final class IrisShadowPassDetector {
    private static final String IRIS_SHADOW_STATE =
            "net.irisshaders.iris.shadows.ShadowRenderingState";

    private static final AtomicBoolean warningPrinted = new AtomicBoolean();
    private static volatile MethodHandle shadowPassHandle;
    private static volatile int state; // 0 = unresolved, 1 = available, -1 = unavailable

    private IrisShadowPassDetector() {
    }

    public static boolean isShadowPass() {
        int currentState = state;
        if (currentState == 0) {
            resolve();
            currentState = state;
        }

        if (currentState != 1) {
            return false;
        }

        MethodHandle handle = shadowPassHandle;
        if (handle == null) {
            return false;
        }

        try {
            return (boolean) handle.invokeExact();
        } catch (Throwable throwable) {
            shadowPassHandle = null;
            state = -1;
            if (warningPrinted.compareAndSet(false, true)) {
                System.err.println(
                        "[EFTwilightApotheosisFix] Iris shadow-state call failed; "
                                + "disabling shadow fast-path. " + throwable
                );
            }
            return false;
        }
    }

    private static void resolve() {
        synchronized (IrisShadowPassDetector.class) {
            if (state != 0) {
                return;
            }

            try {
                Class<?> shadowState = Class.forName(
                        IRIS_SHADOW_STATE,
                        false,
                        IrisShadowPassDetector.class.getClassLoader()
                );
                Method method = shadowState.getDeclaredMethod(
                        "areShadowsCurrentlyBeingRendered"
                );
                method.setAccessible(true);

                shadowPassHandle = MethodHandles.lookup().unreflect(method);
                state = 1;
                System.out.println(
                        "[EFTwilightApotheosisFix] Iris shadow pass detected; "
                                + "Epic Fight will use vanilla living-entity rendering in shadow maps."
                );
            } catch (ClassNotFoundException ignored) {
                state = -1;
            } catch (Throwable throwable) {
                shadowPassHandle = null;
                state = -1;
                if (warningPrinted.compareAndSet(false, true)) {
                    System.err.println(
                            "[EFTwilightApotheosisFix] Could not resolve Iris shadow state; "
                                    + "shadow fast-path disabled. " + throwable
                    );
                }
            }
        }
    }
}
