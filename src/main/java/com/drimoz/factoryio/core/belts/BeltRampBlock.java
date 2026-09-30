package com.drimoz.factoryio.core.belts;

import com.drimoz.factoryio.core.model.Belt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * La rampe d'un convoyeur : monte ou descend d'un cran, à la vitesse de son tier.
 *
 * <p>Elle fait partie de la famille de son convoyeur — même définition, même vitesse, même
 * block entity — et n'en diffère que par son sens de circulation : tout le transport, la
 * persistance et la synchronisation sont ceux de {@link BeltBlock}.
 *
 * <h2>Monter ou descendre se décide à la pose</h2>
 *
 * <p>Un seul item par tier. La rampe descend si un convoyeur, derrière et un cran plus haut,
 * y tomberait — c'est le geste de qui prolonge une ligne vers le bas. Sinon elle monte.
 */
public class BeltRampBlock extends BeltBlock {

    public static final EnumProperty<BeltFlow> FLOW = EnumProperty.create("flow", BeltFlow.class, BeltFlow::isRamp);

    /** Une demi-dalle, plus la moitié haute du bloc côté haut : on la gravit à pied. */
    private static final VoxelShape SLAB = Block.box(0, 0, 0, 16, 8, 16);

    /** Formes par sens de circulation, indexées par {@link Direction#get2DDataValue}. */
    private static final VoxelShape[] UP_SHAPES = new VoxelShape[4];
    private static final VoxelShape[] DOWN_SHAPES = new VoxelShape[4];

    static {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            UP_SHAPES[facing.get2DDataValue()] = Shapes.or(SLAB, half(facing));
            DOWN_SHAPES[facing.get2DDataValue()] = Shapes.or(SLAB, half(facing.getOpposite()));
        }
    }

    private final Supplier<Block> base;

    /**
     * @param base le convoyeur de la famille, dont la rampe prend le nom
     */
    public BeltRampBlock(Belt belt, Supplier<Block> base, Properties properties) {
        super(belt, properties);

        this.base = base;

        registerDefaultState(defaultBlockState().setValue(FLOW, BeltFlow.RAMP_UP));
    }

    // Interface (État)

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);

        builder.add(FLOW);
    }

    @Override
    public BeltFlow flowOf(BlockState state) {
        return state.getValue(FLOW);
    }

    @NotNull
    @Override
    public VoxelShape getShape(
            @NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {

        VoxelShape[] shapes = state.getValue(FLOW) == BeltFlow.RAMP_UP ? UP_SHAPES : DOWN_SHAPES;

        return shapes[state.getValue(FACING).get2DDataValue()];
    }

    @Override
    protected BlockState orient(LevelReader level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        BlockPos above = pos.relative(facing.getOpposite()).above();

        // La question posée au convoyeur du haut, telle que le transport la posera : si cette
        // position portait une descente, y tomberait-il ?
        BeltFlow.Placed feeder = placedAt(level, above);
        BeltFlow.World world = worldWith(level, pos, new BeltFlow.Placed(BeltFlow.RAMP_DOWN, facing));

        boolean falls = feeder != null
                && pos.equals(BeltFlow.target(above, feeder.flow(), feeder.facing(), world));

        return state.setValue(FLOW, falls ? BeltFlow.RAMP_DOWN : BeltFlow.RAMP_UP);
    }

    /**
     * « Rampe » accolé au nom du convoyeur, pour tous les tiers — ceux d'un modpack compris,
     * qui n'ont pas de clé à eux.
     */
    @NotNull
    @Override
    public MutableComponent getName() {
        return Component.translatable("block.factor_io.belt_ramp", this.base.get().getName());
    }

    // Inner work

    /** La moitié haute du bloc, du côté {@code side}. */
    private static VoxelShape half(Direction side) {
        return switch (side) {
            case NORTH -> Block.box(0, 8, 0, 16, 16, 8);
            case SOUTH -> Block.box(0, 8, 8, 16, 16, 16);
            case WEST -> Block.box(0, 8, 0, 8, 16, 16);
            default -> Block.box(8, 8, 0, 16, 16, 16);
        };
    }
}
