package com.drimoz.factoryio.content.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * Le volume d'un multibloc, maître au <b>centre-bas</b>.
 *
 * <p>{@code width} se mesure en travers de la face, {@code depth} dans son axe : un volume
 * non carré tourne avec la machine. Largeur et profondeur sont impaires, sans quoi le
 * centre ne tomberait pas sur un bloc.
 */
public record MultiblockShape(int width, int height, int depth) {

    public MultiblockShape {
        if (width < 1 || height < 1 || depth < 1 || width % 2 == 0 || depth % 2 == 0) {
            throw new IllegalArgumentException(
                    "Volume de multibloc invalide : " + width + "×" + height + "×" + depth);
        }
    }

    /** Toutes les positions du volume, maître compris. */
    public List<BlockPos> positions(BlockPos master, Direction facing) {
        int halfX = halfX(facing);
        int halfZ = halfZ(facing);

        List<BlockPos> positions = new ArrayList<>((2 * halfX + 1) * this.height * (2 * halfZ + 1));
        for (int dy = 0; dy < this.height; dy++) {
            for (int dx = -halfX; dx <= halfX; dx++) {
                for (int dz = -halfZ; dz <= halfZ; dz++) {
                    positions.add(master.offset(dx, dy, dz));
                }
            }
        }
        return positions;
    }

    /** La boîte englobante, en coordonnées du monde. */
    public AABB bounds(BlockPos master, Direction facing) {
        int halfX = halfX(facing);
        int halfZ = halfZ(facing);

        return new AABB(
                master.getX() - halfX, master.getY(), master.getZ() - halfZ,
                master.getX() + halfX + 1, master.getY() + this.height, master.getZ() + halfZ + 1);
    }

    // Une face orientée nord ou sud s'étend en X ; est ou ouest, en Z.

    private int halfX(Direction facing) {
        return (facing.getAxis() == Direction.Axis.Z ? this.width : this.depth) / 2;
    }

    private int halfZ(Direction facing) {
        return (facing.getAxis() == Direction.Axis.Z ? this.depth : this.width) / 2;
    }
}
