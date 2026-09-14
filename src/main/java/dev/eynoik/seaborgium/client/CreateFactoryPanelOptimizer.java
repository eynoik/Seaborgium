package dev.eynoik.seaborgium.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Optional Create compatibility kept reflection-only so Seaborgium does not
 * acquire a hard runtime dependency on Create.
 *
 * Create's FactoryPanelBlockEntity uses a fixed render box inflated by eight
 * blocks in every direction. That keeps connection paths visible, but it also
 * makes a panel survive frustum culling in many views where none of its actual
 * geometry can be seen. The replacement box is built from the panel block and
 * the endpoints of the connection paths which the renderer really draws.
 */
public final class CreateFactoryPanelOptimizer {
    private static final String FACTORY_PANEL_BLOCK_ENTITY =
            "com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity";
    private static final String FACTORY_PANEL_BEHAVIOUR =
            "com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour";
    private static final String FACTORY_PANEL_POSITION =
            "com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition";

    private static final double GEOMETRY_PADDING = 1.25;

    private static volatile boolean initialized;
    private static volatile boolean compatible;
    private static Field panelsField;
    private static Field targetedByField;
    private static Field targetedByLinksField;
    private static Method panelPositionPosMethod;

    private CreateFactoryPanelOptimizer() {
    }

    public static AABB optimizedRenderBounds(BlockEntity blockEntity) {
        if (!FACTORY_PANEL_BLOCK_ENTITY.equals(blockEntity.getClass().getName())) {
            return null;
        }

        ensureInitialized(blockEntity.getClass().getClassLoader());
        if (!compatible) {
            return null;
        }

        try {
            BlockPos origin = blockEntity.getBlockPos();
            MutableBounds bounds = new MutableBounds(origin);

            Object panelsObject = panelsField.get(blockEntity);
            if (!(panelsObject instanceof Map<?, ?> panels)) {
                return null;
            }

            for (Object behaviour : panels.values()) {
                if (behaviour == null) {
                    continue;
                }

                Object targetedByObject = targetedByField.get(behaviour);
                if (targetedByObject instanceof Map<?, ?> targetedBy) {
                    for (Object panelPosition : targetedBy.keySet()) {
                        includePanelPosition(bounds, panelPosition);
                    }
                }

                Object targetedByLinksObject = targetedByLinksField.get(behaviour);
                if (targetedByLinksObject instanceof Map<?, ?> targetedByLinks) {
                    for (Object position : targetedByLinks.keySet()) {
                        if (position instanceof BlockPos blockPos) {
                            bounds.include(blockPos);
                        }
                    }
                }
            }

            return bounds.toAabb(GEOMETRY_PADDING);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Returning null keeps Create's original conservative render bounds.
            return null;
        }
    }

    private static void includePanelPosition(MutableBounds bounds, Object panelPosition)
            throws ReflectiveOperationException {
        if (panelPosition == null) {
            return;
        }
        Object position = panelPositionPosMethod.invoke(panelPosition);
        if (position instanceof BlockPos blockPos) {
            bounds.include(blockPos);
        }
    }

    private static void ensureInitialized(ClassLoader classLoader) {
        if (initialized) {
            return;
        }

        synchronized (CreateFactoryPanelOptimizer.class) {
            if (initialized) {
                return;
            }

            try {
                Class<?> blockEntityClass = Class.forName(FACTORY_PANEL_BLOCK_ENTITY, false, classLoader);
                Class<?> behaviourClass = Class.forName(FACTORY_PANEL_BEHAVIOUR, false, classLoader);
                Class<?> positionClass = Class.forName(FACTORY_PANEL_POSITION, false, classLoader);

                panelsField = blockEntityClass.getField("panels");
                targetedByField = behaviourClass.getField("targetedBy");
                targetedByLinksField = behaviourClass.getField("targetedByLinks");
                panelPositionPosMethod = positionClass.getMethod("pos");
                compatible = true;
            } catch (ReflectiveOperationException | LinkageError ignored) {
                compatible = false;
            } finally {
                initialized = true;
            }
        }
    }

    private static final class MutableBounds {
        private double minX;
        private double minY;
        private double minZ;
        private double maxX;
        private double maxY;
        private double maxZ;

        private MutableBounds(BlockPos position) {
            minX = position.getX();
            minY = position.getY();
            minZ = position.getZ();
            maxX = position.getX() + 1.0;
            maxY = position.getY() + 1.0;
            maxZ = position.getZ() + 1.0;
        }

        private void include(BlockPos position) {
            minX = Math.min(minX, position.getX());
            minY = Math.min(minY, position.getY());
            minZ = Math.min(minZ, position.getZ());
            maxX = Math.max(maxX, position.getX() + 1.0);
            maxY = Math.max(maxY, position.getY() + 1.0);
            maxZ = Math.max(maxZ, position.getZ() + 1.0);
        }

        private AABB toAabb(double padding) {
            return new AABB(
                    minX - padding,
                    minY - padding,
                    minZ - padding,
                    maxX + padding,
                    maxY + padding,
                    maxZ + padding
            );
        }
    }
}
