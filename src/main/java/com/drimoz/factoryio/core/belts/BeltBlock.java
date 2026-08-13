package com.drimoz.factoryio.core.belts;

import com.drimoz.factoryio.core.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Un convoyeur.
 *
 * <h2>Tier et sens sont des traits du bloc, pas des propriétés d'état</h2>
 *
 * <p>Trois tiers et trois sens de circulation auraient multiplié par neuf les 32 variantes de
 * blockstate, pour une information qui ne change jamais sur un bloc donné. Ils sont donc portés
 * par la classe, comme le fait déjà {@code InserterBlock} pour les traits de son type.
 *
 * <p>Conséquence pratique : le jour où les modèles d'ascenseur existeront, il suffira
 * d'enregistrer cette même classe avec {@link BeltFlow#LIFT_UP}. Aucun blockstate à toucher.
 *
 * <h2>Les connexions se calculent au placement, jamais au tick</h2>
 *
 * <p>{@code getStateForPlacement} et {@code updateShape} lisent les voisins ; le tick ne lit
 * plus rien. C'est ce qui garde le coût du transport proportionnel au nombre de blocs et non
 * au nombre de recherches de block entity.
 */
public class BeltBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    /** Forme visible : voir {@link BeltShape}, dont les huit valeurs sont celles des modèles. */
    public static final IntegerProperty CONNECTED =
            IntegerProperty.create("connected", 0, BeltShape.MAX_CONNECTED);

    /** Une bande est une demi-dalle : huit unités, comme les modèles du dépôt. */
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 8, 16);

    private final BeltTier tier;
    private final BeltFlow flow;

    public BeltBlock(BeltTier tier, BeltFlow flow, Properties properties) {
        super(properties);

        this.tier = tier;
        this.flow = flow;

        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(CONNECTED, 0)
                .setValue(WATERLOGGED, false));
    }

    // Interface (Traits)

    public BeltTier tier() {
        return this.tier;
    }

    public BeltFlow flow() {
        return this.flow;
    }

    // Interface (État)

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CONNECTED, WATERLOGGED);
    }

    @Override
    public RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @NotNull
    @Override
    public VoxelShape getShape(
            @NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {

        return SHAPE;
    }

    /**
     * Un convoyeur poussé par un piston se désynchroniserait de son block entity.
     *
     * <p>Le bloc se déplacerait, son contenu non — ou l'inverse selon l'ordre. Le refuser est
     * la seule réponse honnête, et c'est ce que fait vanilla pour les mêmes raisons.
     */
    @NotNull
    @Override
    public PushReaction getPistonPushReaction(@NotNull BlockState state) {
        return PushReaction.BLOCK;
    }

    @NotNull
    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        // La bande sort dans la direction où regarde le joueur : on pose une ligne en marchant
        // le long, ce qui est le geste attendu.
        Direction facing = context.getHorizontalDirection();

        BlockState state = defaultBlockState()
                .setValue(FACING, facing)
                .setValue(WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER);

        return state.setValue(CONNECTED, connectedFor(level, pos, facing));
    }

    @NotNull
    @Override
    public BlockState updateShape(
            BlockState state, @NotNull Direction direction, @NotNull BlockState neighbour,
            @NotNull LevelAccessor level, @NotNull BlockPos pos, @NotNull BlockPos neighbourPos) {

        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }

        return state.setValue(CONNECTED, connectedFor(level, pos, state.getValue(FACING)));
    }

    @Override
    public void neighborChanged(
            @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
            @NotNull Block block, @NotNull BlockPos fromPos, boolean moving) {

        super.neighborChanged(state, level, pos, block, fromPos, moving);

        if (level.getBlockEntity(pos) instanceof BeltBlockEntity belt) belt.onNeighbourChanged();
    }

    @NotNull
    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @NotNull
    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    // Interface (Connexions)

    /**
     * Valeur de {@code connected} d'après les voisins.
     *
     * <p>Toute la décision est dans {@link BeltShape}, qui se teste sans le monde. Ici, on ne
     * fait que lire.
     */
    private int connectedFor(LevelReader level, BlockPos pos, Direction facing) {
        boolean fromBack = fedFrom(level, pos, pos.relative(facing.getOpposite()));
        boolean fromLeft = fedFrom(level, pos, pos.relative(BeltShape.leftOf(facing)));
        boolean fromRight = fedFrom(level, pos, pos.relative(BeltShape.rightOf(facing)));

        boolean hasOutput = level.getBlockState(this.flow.exit(pos, facing)).getBlock() instanceof BeltBlock;

        return BeltShape.connectedOf(fromBack, fromLeft, fromRight, hasOutput, this.flow.allowsCurve());
    }

    /**
     * Ce voisin déverse-t-il ici ?
     *
     * <p>Occuper la place ne suffit pas : un convoyeur perpendiculaire est bien à côté, mais il
     * déverse ailleurs. Seule sa <b>sortie</b> tranche — voir {@link BeltFlow#feeds}.
     */
    private static boolean fedFrom(LevelReader level, BlockPos pos, BlockPos candidate) {
        BlockState state = level.getBlockState(candidate);

        if (!(state.getBlock() instanceof BeltBlock belt)) return false;
        if (!state.hasProperty(FACING)) return false;

        return BeltFlow.feeds(candidate, belt.flow(), state.getValue(FACING), pos);
    }

    // Interface (Block entity)

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new BeltBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            @NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {

        // Serveur uniquement : le client n'a rien à faire avancer tant que la synchronisation
        // n'est pas écrite (cf. 08 §6).
        if (level.isClientSide) return null;

        return createTickerHelper(type, ModBlocks.BELT_ENTITY.get(), BeltBlockEntity::tick);
    }

    // Interface (Cassage)

    /**
     * Le contenu tombe au sol.
     *
     * <p>Un convoyeur plein cassé sans rien lâcher détruirait des items — ce que le reste du
     * mod s'interdit partout ailleurs.
     */
    @Override
    public void onRemove(
            BlockState state, @NotNull Level level, @NotNull BlockPos pos,
            BlockState newState, boolean moving) {

        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof BeltBlockEntity belt) {
            List<net.minecraft.world.item.ItemStack> contents = belt.contents();

            SimpleContainer container = new SimpleContainer(contents.size());
            for (int index = 0; index < contents.size(); index++) {
                container.setItem(index, contents.get(index));
            }

            Containers.dropContents(level, pos, container);
        }

        super.onRemove(state, level, pos, newState, moving);
    }
}
