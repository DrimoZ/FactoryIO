package com.drimoz.factoryio.core.belts;

import com.drimoz.factoryio.core.model.Belt;
import com.drimoz.factoryio.core.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
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
 * <p>Plusieurs tiers auraient multiplié les 32 variantes de blockstate, pour une information
 * qui ne change jamais sur un bloc donné. Le tier est donc porté par la classe, comme le fait
 * déjà {@code InserterBlock} pour les traits de son type. Seule la <b>rampe</b>
 * ({@link BeltRampBlock}) ajoute un état — monter ou descendre — parce qu'elle le décide à la
 * pose.
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

    private final Belt belt;

    public BeltBlock(Belt belt, Properties properties) {
        super(properties);

        this.belt = belt;

        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(CONNECTED, 0)
                .setValue(WATERLOGGED, false));
    }

    // Interface (Traits)

    /** La définition de ce convoyeur : vitesse, apparence, tier suivant. */
    public Belt belt() {
        return this.belt;
    }

    /** Sens de circulation sous cet état. À plat, toujours, sauf sur une rampe. */
    public BeltFlow flowOf(BlockState state) {
        return BeltFlow.HORIZONTAL;
    }

    /**
     * Le convoyeur à cette position, tel que {@link BeltFlow#target} le demande.
     *
     * <p>Un chunk non chargé répond « rien » : {@code getBlockState} le chargerait, et une ligne
     * qui y pointe le ferait charger à chaque résolution, de proche en proche (08 §9).
     */
    @Nullable
    public static BeltFlow.Placed placedAt(LevelReader level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return null;

        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BeltBlock block) || !state.hasProperty(FACING)) return null;

        return new BeltFlow.Placed(block.flowOf(state), state.getValue(FACING));
    }

    /** Position dans laquelle le convoyeur posé en {@code pos} déverse, ou {@code null}. */
    @Nullable
    public static BlockPos targetOf(LevelReader level, BlockPos pos) {
        BeltFlow.Placed self = placedAt(level, pos);
        if (self == null) return null;

        return BeltFlow.target(pos, self.flow(), self.facing(), at -> placedAt(level, at));
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

        BlockState state = orient(level, pos, defaultBlockState()
                .setValue(FACING, facing)
                .setValue(WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER));

        return state.setValue(CONNECTED, connectedFor(level, pos, state));
    }

    /** Ce qu'une rampe décide à la pose — monter ou descendre. Rien, sur une bande à plat. */
    protected BlockState orient(LevelReader level, BlockPos pos, BlockState state) {
        return state;
    }

    @NotNull
    @Override
    public BlockState updateShape(
            BlockState state, @NotNull Direction direction, @NotNull BlockState neighbour,
            @NotNull LevelAccessor level, @NotNull BlockPos pos, @NotNull BlockPos neighbourPos) {

        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }

        return state.setValue(CONNECTED, connectedFor(level, pos, state));
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
    protected int connectedFor(LevelReader level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        BeltFlow flow = flowOf(state);

        // Le monde, tel qu'il sera une fois cet état posé : à la pose, la position est encore
        // vide, et un voisin qui y déverse n'y trouverait rien.
        BeltFlow.World world = worldWith(level, pos, new BeltFlow.Placed(flow, facing));

        boolean fromBack = fedFrom(world, pos, facing.getOpposite());
        boolean fromLeft = fedFrom(world, pos, BeltShape.leftOf(facing));
        boolean fromRight = fedFrom(world, pos, BeltShape.rightOf(facing));

        // Deux bandes face à face ne se passent rien, et ne doivent donc pas afficher de
        // raccord : c'est BeltFlow.accepts qui les refuse, pour la forme comme pour le transport.
        boolean hasOutput = BeltFlow.target(pos, flow, facing, world) != null;

        return BeltShape.connectedOf(fromBack, fromLeft, fromRight, hasOutput, flow.allowsCurve());
    }

    /** Le monde, où {@code pos} porterait {@code self}. */
    protected static BeltFlow.World worldWith(LevelReader level, BlockPos pos, BeltFlow.Placed self) {
        return at -> at.equals(pos) ? self : placedAt(level, at);
    }

    /**
     * Un voisin de ce côté déverse-t-il ici ?
     *
     * <p>Occuper la place ne suffit pas : un convoyeur perpendiculaire est bien à côté, mais il
     * déverse ailleurs. Seule sa <b>sortie</b> tranche — voir {@link BeltFlow#target}.
     */
    private static boolean fedFrom(BeltFlow.World world, BlockPos pos, Direction side) {
        for (BlockPos candidate : BeltFlow.sources(pos, side)) {
            BeltFlow.Placed placed = world.at(candidate);
            if (placed == null) continue;

            if (pos.equals(BeltFlow.target(candidate, placed.flow(), placed.facing(), world))) return true;
        }

        return false;
    }

    /**
     * Les connexions d'une rampe passent par des voisins <b>en diagonale</b>, que Minecraft ne
     * prévient jamais d'un changement.
     *
     * <p>{@code updateShape} et {@code neighborChanged} ne concernent que les six faces. Or une
     * rampe montante déverse devant et un cran plus haut, et ce qui tombe sur une rampe
     * descendante vient de derrière et d'un cran plus haut. Sans ce relais, poser le convoyeur
     * du sommet laisserait la rampe convaincue de buter — la ligne coupée sans rien de visible,
     * le défaut exact que 08 §11 redoutait.
     *
     * <p>Appelé à chaque changement d'état, pose et retrait compris ; les huit diagonales
     * verticales, et elles seules. La propagation s'arrête d'elle-même : un voisin dont la forme
     * ne change pas n'est pas réécrit.
     */
    private static void refreshDiagonals(Level level, BlockPos pos) {
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos beside = pos.relative(side);

            refresh(level, beside.above());
            refresh(level, beside.below());
        }
    }

    private static void refresh(Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) return;

        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BeltBlock belt)) return;

        if (level.getBlockEntity(pos) instanceof BeltBlockEntity entity) entity.onNeighbourChanged();

        BlockState updated = state.setValue(CONNECTED, belt.connectedFor(level, pos, state));
        if (updated != state) level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
    }

    @Override
    public void onPlace(
            @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
            @NotNull BlockState old, boolean moving) {

        super.onPlace(state, level, pos, old, moving);

        if (!level.isClientSide) refreshDiagonals(level, pos);
    }

    // Interface (Pose à la main)

    /**
     * Poser un item sur la bande, ou en reprendre un.
     *
     * <p>Factorio le permet, et c'est ici la seule façon d'alimenter un convoyeur tant que
     * l'inserter ne les connaît pas ([FIO-097](../../../../../../../docs/06-BACKLOG.md)). La
     * voie et la case se déduisent de l'endroit exact où le joueur clique : viser la voie
     * gauche doit poser sur la voie gauche.
     */
    @NotNull
    @Override
    public InteractionResult use(
            @NotNull BlockState state, Level level, @NotNull BlockPos pos, Player player,
            @NotNull InteractionHand hand, BlockHitResult hit) {

        if (!(level.getBlockEntity(pos) instanceof BeltBlockEntity belt)) return InteractionResult.PASS;

        Direction facing = state.getValue(FACING);

        Vec3 local = hit.getLocation().subtract(Vec3.atLowerCornerOf(pos)).subtract(0.5D, 0D, 0.5D);

        int lane = local.dot(BeltPath.vector(BeltShape.leftOf(facing))) > 0D
                ? BeltTransport.LEFT
                : BeltTransport.RIGHT;

        int slot = slotAt(local.dot(BeltPath.vector(facing)), BeltLane.DEFAULT_CAPACITY);

        ItemStack held = player.getItemInHand(hand);

        if (held.isEmpty()) {
            if (level.isClientSide) return InteractionResult.SUCCESS;

            ItemStack taken = belt.takeNear(lane, slot);
            if (taken.isEmpty()) return InteractionResult.PASS;

            player.getInventory().placeItemBackInInventory(taken);

            return InteractionResult.CONSUME;
        }

        if (level.isClientSide) return InteractionResult.SUCCESS;

        if (!belt.acceptAt(lane, slot, held)) return InteractionResult.CONSUME;

        if (!player.getAbilities().instabuild) held.shrink(BeltBlockEntity.ITEMS_PER_SLOT);

        return InteractionResult.CONSUME;
    }

    /**
     * Case visée, d'après la distance au centre du bloc le long de la circulation.
     *
     * <p>Le repère est celui du rendu : l'avance va de 0 au bord d'entrée à 1 au bord de
     * sortie, et les cases s'y répartissent à parts égales.
     */
    private static int slotAt(double along, int slots) {
        int slot = (int) ((along + 0.5D) * slots);

        return Math.max(0, Math.min(slot, slots - 1));
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

        // Des deux côtés, et c'est délibéré : le mouvement d'un convoyeur est déterministe,
        // donc le client le rejoue au lieu de le recevoir. L'alternative — un paquet par item
        // et par mouvement — est le premier piège recensé (08 §1), et celui dans lequel le
        // code des inserters est tombé (BUG-004).
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
            List<ItemStack> contents = belt.contents();

            SimpleContainer container = new SimpleContainer(contents.size());
            for (int index = 0; index < contents.size(); index++) {
                container.setItem(index, contents.get(index));
            }

            Containers.dropContents(level, pos, container);
        }

        super.onRemove(state, level, pos, newState, moving);

        if (!state.is(newState.getBlock()) && !level.isClientSide) refreshDiagonals(level, pos);
    }
}
