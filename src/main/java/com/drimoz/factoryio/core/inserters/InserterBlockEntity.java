package com.drimoz.factoryio.core.inserters;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.generic.block_entity.MenuBlockEntity;
import com.drimoz.factoryio.core.generic.container.energy.EnergyContainer;
import com.drimoz.factoryio.core.model.Inserter;
import com.drimoz.factoryio.core.init.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.network.chat.Component;


import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public class InserterBlockEntity extends MenuBlockEntity implements GeoBlockEntity {

    // Public constants

    public static final int BUFFER_SLOT = InserterSlotLayout.BUFFER;

    public final boolean IS_ENERGY;
    public final boolean IS_FILTER;

    /** Source unique de vérité pour les index de slots (cf. DT-03). */
    public final InserterSlotLayout LAYOUT;

    /** Nombre d'items de carburant que l'inserter garde en réserve dans son slot. */
    public static final int FUEL_BUFFER_TARGET = 5;

    // Private constants

    private final Inserter inserter;

    // Protected properties

    protected EnergyContainer energyStorage;
    protected LazyOptional<IEnergyStorage> lazyEnergy;

    protected ItemStackHandler itemStorage;
    protected LazyOptional<IItemHandler> lazyItem;

    // private properties

    /** État du bras. Remplace le compteur de cooldown (cf. FIO-060). */
    private InserterState state = InserterState.WAITING;

    /**
     * Le bras rapporte du carburant pour l'inserter lui-même, et non un item pour la
     * cible : le trajet s'arrête à la main.
     */
    private boolean carryingFuel = false;

    private boolean isWhitelist = true;
    private int current_fuel_value = 0;

    /**
     * Slots de filtre passés en « correspondance par tag », un bit par slot.
     *
     * <p>Un masque plutôt que cinq booléens : cela tient dans un entier, se persiste et se
     * synchronise d'une pièce, et le nombre de filtres reste une donnée de
     * {@link InserterSlotLayout} plutôt qu'une constante répétée ici.
     */
    private int tagFilterMask = 0;

    /**
     * Condition d'activation liée au signal redstone (cf. FIO-070).
     *
     * <p>Par défaut « actif tant que le signal est sous 1 », c'est-à-dire le comportement
     * historique : un monde existant se recharge inchangé.
     */
    private InserterRedstoneCondition redstoneCondition = InserterRedstoneCondition.DEFAULT;

    /**
     * Tick de jeu auquel le mouvement de bras en cours se termine.
     *
     * <p>Une échéance absolue, et non un compteur : elle est envoyée une fois, au
     * changement d'état, et le client en déduit seul la progression à partir de sa propre
     * horloge. Un compteur devrait être synchronisé à chaque tick, ce qui ramènerait
     * exactement le trafic périodique supprimé par BUG-004.
     */
    private long swingEndTick = 0L;

    /**
     * Copie de l'item en main, pour le rendu.
     *
     * <p>La vérité reste le slot buffer, mais le client n'en reçoit pas le contenu : les
     * slots du menu ne sont synchronisés qu'aux joueurs ayant l'écran ouvert. Cette copie
     * voyage dans {@code getUpdateTag} et sert au rendu de tous les autres.
     */
    private ItemStack heldStack = ItemStack.EMPTY;

    // Optimisations du tick (DT-07) — jamais persistées, purement locales.

    /** Inventaires voisins mémorisés ; {@code null} = à résoudre. */
    private LazyOptional<IItemHandler> cachedSource;
    private LazyOptional<IItemHandler> cachedTarget;

    /** Dernier slot ayant abouti, pour repartir de là plutôt que du slot 0. */
    private int lastSourceSlot = 0;
    private int lastTargetSlot = 0;

    /** Mise en sommeil après des tentatives infructueuses répétées. */
    private int failedAttempts = 0;
    private int sleepTicks = 0;

    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);

    // Life cycle

    public InserterBlockEntity(BlockPos blockPos, BlockState blockState, Inserter inserter) {
        this(inserter.getBlockEntityType().get(), blockPos, blockState, inserter);
    }

    public InserterBlockEntity(
            BlockEntityType<?> blockEntityType,
            BlockPos blockPos,
            BlockState blockState,
            Inserter inserter
    ) {
        super(blockEntityType, blockPos, blockState);

        this.inserter = inserter;

        this.IS_ENERGY = inserter.useEnergy();
        this.IS_FILTER = inserter.isFilterable();
        this.LAYOUT = InserterSlotLayout.of(inserter);

        if (IS_ENERGY) {
            this.energyStorage = new EnergyContainer(inserter.getEnergyCapacity(), inserter.getEnergyTransferRate()) {
                @Override
                protected void onEnergyChanged() {
                    InserterBlockEntity.this.setChanged();

                    // Un inserter à plat s'endort comme n'importe quel inserter qui
                    // n'aboutit pas. Sans ce réveil, le courant qui revient mettait
                    // jusqu'à MAX_SLEEP_TICKS à être pris en compte (cf. BUG-037).
                    InserterBlockEntity.this.wakeUp();
                }

                // Contrat externe : un bloc voisin ne doit jamais pouvoir vider
                // l'inserter. La consommation propre passe par consumeInternal().

                @Override
                public int extractEnergy(int maxExtract, boolean simulate) {
                    return 0;
                }

                @Override
                public boolean canExtract() {
                    return false;
                }
            };

            this.lazyEnergy = LazyOptional.of(() -> this.energyStorage);
        }

        this.itemStorage = new ItemStackHandler(LAYOUT.size()) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();

                // Du carburant déposé à la main doit relancer un inserter endormi.
                wakeUp();
            }

            @NotNull
            @Override
            public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                // Depuis l'extérieur, seul le slot de carburant est accessible.
                if (slot != LAYOUT.fuel()) return stack;
                if (ForgeHooks.getBurnTime(stack, null) <= 0) return stack;
                if (!stack.is(ModTags.Items.INSERTER_FUEL)) return stack;

                return super.insertItem(slot, stack, simulate);
            }

            @NotNull
            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (slot != LAYOUT.fuel()) return ItemStack.EMPTY;

                return super.extractItem(slot, amount, simulate);
            }

        };
        this.lazyItem = LazyOptional.of(() -> this.itemStorage);
    }

    // Interface (DataFromData)

    public int getMaximumItemCountPerAction(){
        return inserter.getPreferredItemCountPerAction();
    }

    public int getGrabDistance(){
        return inserter.getGrabDistance();
    }

    /** Durée d'un mouvement de bras, en ticks. Un item en coûte deux. */
    public int getTicksPerSwing(){
        return inserter.getTicksPerSwing();
    }

    public int getFuelCapacity(){
        return IS_ENERGY ? inserter.getEnergyCapacity() : inserter.getFuelCapacity();
    }

    public int getFuelConsumptionPerAction() {
        return IS_ENERGY ? inserter.getEnergyConsumption() : inserter.getFuelConsumption();
    }

    /** Nombre d'items de carburant que l'inserter cherche à conserver en réserve. */
    public int getPreferredFuelItemBufferCount() {
        return FUEL_BUFFER_TARGET;
    }

    // Interface (Name)

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    // Interface (ItemStorage)

    /** Lâche le contenu réel au sol. Les filtres sont des items fantômes : ils ne tombent pas. */
    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemStorage.getSlots());

        for (int slot = 0; slot < itemStorage.getSlots(); slot++) {
            if (!LAYOUT.isDroppable(slot)) continue;

            inventory.setItem(slot, itemStorage.getStackInSlot(slot));
        }

        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    public boolean isEmpty() {
        for(int i = 0; i < this.itemStorage.getSlots(); i++) {
            if (!this.itemStorage.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }

        return true;
    }

    public void clearContent() {
        for(int i = 0; i < this.itemStorage.getSlots(); i++) {
            this.itemStorage.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    // Interface (Capabilities)

    @Nonnull @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyItem.cast();
        // Toutes les faces, et surtout side == null : cette requête interne est celle
        // qu'utilisent The One Probe, les compteurs et beaucoup de mods d'énergie.
        // La restriction à DOWN les rendait tous aveugles (cf. BUG-021).
        if (cap == ForgeCapabilities.ENERGY && IS_ENERGY) return lazyEnergy.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();

        this.lazyItem = LazyOptional.of(() -> itemStorage);
        if(IS_ENERGY) {
            this.lazyEnergy = LazyOptional.of(() -> energyStorage);
        }
    }

    @Override
    public void invalidateCaps()  {
        super.invalidateCaps();

        this.lazyItem.invalidate();
        if (IS_ENERGY) {
            this.lazyEnergy.invalidate();
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        tag.put("inserterInventory", this.itemStorage.serializeNBT());

        if (IS_ENERGY) {
            tag.putInt("inserterEnergyLevel", this.energyStorage.getEnergyStored());
        }
        else {
            tag.putInt("inserterFuelLevel",this.getCurrentFuelValue());
        }

        // Sans ces lignes, chaque rechargement de monde remettait tous les filtres en
        // whitelist et repartait d'un compteur nul (cf. BUG-008).
        tag.putBoolean("inserterWhitelist", this.isWhitelist);
        tag.putInt("inserterTagFilters", this.tagFilterMask);
        tag.putByte("inserterRedstoneMode", (byte) this.redstoneCondition.mode().ordinal());
        tag.putByte("inserterRedstoneThreshold", (byte) this.redstoneCondition.threshold());

        // L'état du bras est persisté : un inserter bloqué doit se retrouver bloqué au
        // rechargement, pas remis au repos avec un item fantôme en main.
        tag.putByte("inserterState", (byte) this.state.ordinal());
        tag.putBoolean("inserterCarryingFuel", this.carryingFuel);
        tag.putLong("inserterSwingEnd", this.swingEndTick);

        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        this.itemStorage.deserializeNBT(tag.getCompound("inserterInventory"));
        if(IS_ENERGY) {
            energyStorage.overrideCurrentEnergy(tag.getInt("inserterEnergyLevel"));
        }
        else {
            this.overrideCurrentFuelValue(tag.getInt("inserterFuelLevel"));
        }

        // contains() : un monde sauvegardé avant ce correctif n'a pas ces clés, et
        // getBoolean() renverrait false — soit l'inverse du défaut attendu.
        this.isWhitelist = !tag.contains("inserterWhitelist") || tag.getBoolean("inserterWhitelist");
        this.tagFilterMask = tag.getInt("inserterTagFilters");
        this.redstoneCondition = readCondition(tag);

        this.state = InserterState.byOrdinal(tag.getByte("inserterState"));
        this.carryingFuel = tag.getBoolean("inserterCarryingFuel");
        this.swingEndTick = tag.getLong("inserterSwingEnd");
        this.heldStack = this.itemStorage.getStackInSlot(InserterSlotLayout.BUFFER).copy();

        // Un monde sauvegardé avant FIO-060 n'a pas d'état : « inserterState » vaut alors 0,
        // soit WAITING, ce qui est le bon défaut. Mais son buffer peut contenir un item —
        // l'ancien modèle le gardait entre deux actions. WAITING avec la main pleine est
        // une combinaison que la machine à états n'admet pas : on la ramène à BLOCKED,
        // l'état qui décrit exactement « un item à livrer et rien pour l'instant ».
        if (this.state == InserterState.WAITING && !this.heldStack.isEmpty()) {
            this.state = InserterState.BLOCKED;
        }
    }

    // Interface (Synchronisation client)

    /**
     * État envoyé au client à la découverte du bloc et à chaque {@code sendBlockUpdated}.
     *
     * <p>Volontairement minimal : seul ce que le client doit connaître <b>en dehors</b>
     * du GUI. Les jauges d'énergie et de carburant passent par le {@code ContainerData}
     * du menu, donc uniquement vers les joueurs qui l'ont ouvert.
     */
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("inserterWhitelist", this.isWhitelist);
        tag.putInt("inserterTagFilters", this.tagFilterMask);
        tag.putByte("inserterRedstoneMode", (byte) this.redstoneCondition.mode().ordinal());
        tag.putByte("inserterRedstoneThreshold", (byte) this.redstoneCondition.threshold());
        tag.putByte("inserterState", (byte) this.state.ordinal());
        tag.putBoolean("inserterCarryingFuel", this.carryingFuel);
        tag.putLong("inserterSwingEnd", this.swingEndTick);

        // Rien pour une main vide : le tag part à chaque sendBlockUpdated, autant ne pas y
        // traîner un ItemStack.EMPTY sérialisé.
        if (!this.heldStack.isEmpty()) {
            tag.put("inserterHeldStack", this.heldStack.save(new CompoundTag()));
        }

        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        if (tag.contains("inserterWhitelist")) {
            this.isWhitelist = tag.getBoolean("inserterWhitelist");
        }
        this.tagFilterMask = tag.getInt("inserterTagFilters");
        this.redstoneCondition = readCondition(tag);
        this.state = InserterState.byOrdinal(tag.getByte("inserterState"));
        this.carryingFuel = tag.getBoolean("inserterCarryingFuel");
        this.swingEndTick = tag.getLong("inserterSwingEnd");
        this.heldStack = tag.contains("inserterHeldStack")
                ? ItemStack.of(tag.getCompound("inserterHeldStack"))
                : ItemStack.EMPTY;
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        CompoundTag tag = packet.getTag();
        if (tag != null) handleUpdateTag(tag);
    }

    /** Marque l'état comme modifié et le pousse aux clients qui suivent le chunk. */
    private void syncToClients() {
        setChanged();

        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public boolean stillValid(Player playerEntity) {
        if (this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        } else {
            return !(playerEntity.distanceToSqr((double)this.worldPosition.getX() + 0.5, (double)this.worldPosition.getY() + 0.5, (double)this.worldPosition.getZ() + 0.5) > 64.0);
        }
    }

    // Interface (Ticking)

    /**
     * Tick serveur uniquement : fait avancer la machine à états (cf. FIO-060).
     *
     * <p>Aucune émission réseau périodique. L'état visible ne part qu'aux <b>changements
     * d'état</b>, et les jauges du GUI par le {@code ContainerData} du menu — donc
     * uniquement vers les joueurs qui regardent l'écran. La version d'origine envoyait
     * 3 à 4 paquets par tick et par inserter <i>à tous les joueurs du serveur</i>
     * (cf. BUG-004).
     */
    public static void tick(Level pLevel, BlockPos pPos, BlockState pState, final InserterBlockEntity pEntity) {

        if (!pEntity.isEnabled()) return;

        pEntity.burnFuel();

        switch (pEntity.state) {
            case WAITING -> pEntity.tickWaiting();
            case SWINGING -> pEntity.tickSwinging();
            case BLOCKED -> pEntity.tickBlocked();
            case RETURNING -> pEntity.tickReturning();
        }
    }

    /** Main vide, bras côté source : chercher quelque chose à saisir. */
    private void tickWaiting() {
        // Inserter endormi : rien à faire tant qu'il n'est pas réveillé (cf. DT-07).
        if (this.sleepTicks > 0) {
            this.sleepTicks--;
            return;
        }

        // Le ravitaillement est gratuit et prioritaire : le conditionner à la réserve
        // courante enfermerait tout burner à sec dans un blocage définitif (cf. BUG-012).
        if (needsFuel()) {
            ItemStack fuel = refuel(this, getGrabDistance());

            if (!fuel.isEmpty()) {
                // Le carburant est déjà dans son slot ; le mouvement n'est plus qu'un
                // trajet à afficher, qui s'arrête à la main.
                this.carryingFuel = true;
                beginSwing(InserterState.SWINGING, fuel);
                return;
            }
        }

        if (!hasPowerForAction()) {
            registerFailedAttempt();
            return;
        }

        ItemStack picked = suckItems(this, getGrabDistance(), isWhitelist());

        if (picked.isEmpty()) {
            registerFailedAttempt();
            return;
        }

        this.carryingFuel = false;
        beginSwing(InserterState.SWINGING, picked);
    }

    /** Main pleine, bras en route : à l'arrivée, tenter la dépose. */
    private void tickSwinging() {
        if (isSwingRunning()) return;

        // Le carburant a déjà rejoint son slot au moment de la saisie : il n'y a rien à
        // déposer, le bras repart.
        if (this.carryingFuel) {
            beginSwing(InserterState.RETURNING, ItemStack.EMPTY);
            return;
        }

        if (!tryDrop()) {
            // Cible pleine ou absente : le bras reste tendu, item en main. C'est le point
            // clé du design (§2) — rendre l'item au buffer pour le reprendre au cycle
            // suivant serait à la fois plus faux visuellement et plus compliqué.
            enterState(InserterState.BLOCKED);
            registerFailedAttempt();
        }
    }

    /** Bras tendu au-dessus d'une cible qui refuse : réessayer. */
    private void tickBlocked() {
        if (this.sleepTicks > 0) {
            this.sleepTicks--;
            return;
        }

        if (tryDrop()) return;

        registerFailedAttempt();
    }

    /** Bras vide en route vers la source. */
    private void tickReturning() {
        if (isSwingRunning()) return;

        // Pas de syncToClients ici : le client sait déjà que le retour s'achève à
        // swingEndTick, et il n'y a rien à afficher ni en RETURNING ni en WAITING. Le
        // paquet économisé est la moitié du trafic d'un cycle nominal.
        this.state = InserterState.WAITING;
        setChanged();
    }

    /**
     * Dépose la main dans l'inventaire cible et repart si elle est partie.
     *
     * @return {@code true} si la dépose a abouti
     */
    private boolean tryDrop() {
        ItemStack dropped = expelItems(this, getGrabDistance());
        if (dropped.isEmpty()) return false;

        beginSwing(InserterState.RETURNING, ItemStack.EMPTY);
        return true;
    }

    /**
     * Espace les tentatives d'un inserter qui n'aboutit pas.
     *
     * <p>Un inserter face à un mur, ou dont le coffre est vide, réévaluait la situation
     * vingt fois par seconde indéfiniment. Le recul est plafonné à une seconde pour que
     * la reprise reste imperceptible — et parce qu'un voisin situé à deux blocs ne
     * déclenche aucun {@code neighborChanged} qui pourrait réveiller l'inserter.
     */
    private void registerFailedAttempt() {
        this.failedAttempts++;

        if (this.failedAttempts < FAILURES_BEFORE_SLEEP) return;

        this.sleepTicks = Math.min(this.failedAttempts, MAX_SLEEP_TICKS);
    }

    /** Relance immédiatement un inserter endormi. */
    private void wakeUp() {
        this.failedAttempts = 0;
        this.sleepTicks = 0;
    }

    private static final int FAILURES_BEFORE_SLEEP = 5;
    private static final int MAX_SLEEP_TICKS = 20;

    // Interface (Animation)

    /**
     * Démarre un mouvement de bras et le fait connaître aux clients qui suivent le chunk.
     *
     * <p>Le coût est facturé ici, au démarrage : un mouvement engagé va toujours à son
     * terme. Un inserter qui tombe à sec en cours de route finit sa course plutôt que de
     * se figer avec un item en l'air.
     *
     * @param carried item en main pendant le mouvement, vide pour un retour
     */
    private void beginSwing(InserterState movingState, ItemStack carried) {
        if (this.level == null) return;

        this.heldStack = carried.copy();
        this.swingEndTick = this.level.getGameTime() + getTicksPerSwing();

        // Le trajet du carburant est offert : c'est déjà le cas depuis BUG-012, et faire
        // payer un burner à sec pour aller chercher de quoi redémarrer le condamnerait.
        if (!this.carryingFuel) {
            useFuelOrEnergy();
        }

        // Toute action qui aboutit annule le recul accumulé (cf. DT-07).
        wakeUp();

        enterState(movingState);
    }

    /** Change d'état et le pousse aux clients qui suivent le chunk. */
    private void enterState(InserterState next) {
        this.state = next;

        syncToClients();
    }

    /** @return {@code true} si le mouvement en cours n'est pas encore arrivé à son terme */
    private boolean isSwingRunning() {
        return this.level != null && this.level.getGameTime() < this.swingEndTick;
    }

    /**
     * @return avancement du bras sur sa course, de 0 (au départ) à 1 (arrivé)
     *
     * <p>Calculé à partir de l'échéance synchronisée et de l'horloge locale : le client
     * interpole seul, sans un paquet pendant le mouvement.
     *
     * <p>Un bras arrivé mais qui n'est pas reparti — {@code BLOCKED} — vaut 1 : il reste
     * tendu au-dessus de la cible.
     */
    public float getArmProgress(float partialTick) {
        if (this.level == null || this.state == InserterState.BLOCKED) return 1f;

        int duration = getTicksPerSwing();
        double remaining = this.swingEndTick - (this.level.getGameTime() + partialTick);

        if (remaining <= 0) return 1f;
        if (remaining >= duration) return 0f;

        return (float) (1.0 - remaining / duration);
    }

    /** État du bras. */
    public InserterState getState() {
        return this.state;
    }

    /**
     * @return copie de l'item en main, vide si le bras est vide
     *
     * <p>À croiser avec {@link #getState()} : seuls {@code SWINGING} et {@code BLOCKED}
     * portent réellement un item.
     */
    public ItemStack getHeldStack() {
        return this.heldStack;
    }

    /** @return {@code true} si la main porte du carburant destiné à l'inserter lui-même */
    public boolean isCarryingFuel() {
        return this.carryingFuel;
    }

    /** @return {@code true} si la réserve permet de payer une action */
    private boolean hasPowerForAction() {
        return IS_ENERGY
                ? getCurrentEnergy() >= getFuelConsumptionPerAction()
                : getCurrentFuelValue() >= getFuelConsumptionPerAction();
    }

    /** @return {@code true} si le slot de carburant doit être réapprovisionné */
    private boolean needsFuel() {
        if (IS_ENERGY) return false;

        return this.itemStorage.getStackInSlot(LAYOUT.fuel()).getCount() < this.getPreferredFuelItemBufferCount();
    }

    /**
     * Aspire du carburant depuis l'inventaire situé à l'arrière.
     *
     * @return la pile prélevée, vide si rien n'a bougé
     */
    @Nonnull
    private static ItemStack refuel(InserterBlockEntity pEntity, int pDistance) {
        Direction facing = getFacing(pEntity);

        IItemHandler source = pEntity.neighbourHandler(true, facing.getOpposite(), pDistance, facing);
        if (source == null) return ItemStack.EMPTY;

        return grabInto(pEntity, source, pEntity.LAYOUT.fuel(), stack -> stack.is(ModTags.Items.INSERTER_FUEL));
    }

    /**
     * Convertit un item du slot de carburant en réserve de combustion.
     *
     * <p>Deux règles, toutes deux calquées sur le four vanilla (cf. BUG-041) :
     *
     * <p>1. <b>Consommer tardivement</b>, seulement quand la réserve ne suffit plus à
     * payer un mouvement. Brûler dès qu'il reste de la place gaspillait la différence à
     * chaque conversion.
     *
     * <p>2. <b>Écrêter, ne pas refuser.</b> L'ancienne garde exigeait que le
     * {@code burnTime} entier tienne dans la place restante, si bien qu'un carburant plus
     * riche que la capacité n'était <i>jamais</i> converti — mais l'inserter le ramenait
     * quand même, et bouchait son propre slot sans un mot.
     */
    private void burnFuel() {
        if (IS_ENERGY) return;

        // La réserve couvre encore le prochain mouvement : rien à consommer.
        if (this.current_fuel_value >= getFuelConsumptionPerAction()) return;

        ItemStack stack = this.itemStorage.getStackInSlot(LAYOUT.fuel());
        if (stack.isEmpty()) return;

        int burnTime = ForgeHooks.getBurnTime(stack, null);
        if (burnTime <= 0) return;

        int wasted = Math.max(0, burnTime - (this.getFuelCapacity() - this.current_fuel_value));
        if (wasted > 0) {
            FactoryIO.LOGGER.debug(
                    "{} en {} : {} ticks de combustion perdus, la réserve ne fait que {}",
                    stack.getItem(), this.worldPosition, wasted, this.getFuelCapacity());
        }

        this.addToCurrentFuelValue(burnTime);

        // Conserve les items conteneurs (seau de lave -> seau vide) sans casser le NBT.
        ItemStack remainder = stack.hasCraftingRemainingItem() ? stack.getCraftingRemainingItem() : ItemStack.EMPTY;
        stack.shrink(1);

        if (stack.isEmpty() && !remainder.isEmpty()) {
            this.itemStorage.setStackInSlot(LAYOUT.fuel(), remainder);
        } else {
            this.itemStorage.setStackInSlot(LAYOUT.fuel(), stack);
            this.dropAtBlock(remainder);
        }
    }

    // Interface (Energy)

    public int getCurrentEnergy() {
        if (!IS_ENERGY) return -1;

        return this.energyStorage.getCurrentEnergy();
    }

    public int getEnergyCapacity() {
        if (!IS_ENERGY) return -1;

        return this.energyStorage.getEnergyCapacity();
    }

    public void overrideCurrentEnergy(int energy) {
        if (!IS_ENERGY) return;

        this.energyStorage.overrideCurrentEnergy(energy);
    }

    public void overrideEnergyCapacity(int energy) {
        if (!IS_ENERGY) return;

        this.energyStorage.overrideEnergyCapacity(energy);
    }

    /**
     * Consomme l'énergie de fonctionnement.
     *
     * <p>Passe par {@code consumeInternal} et non par {@code extractEnergy}, que la
     * sous-classe anonyme neutralise volontairement pour interdire aux blocs voisins
     * de vider l'inserter (cf. BUG-003).
     */
    public void consumeEnergy(int energy) {
        if (!IS_ENERGY) return;

        this.energyStorage.consumeInternal(energy);
    }

    // Interface (Fuel)

    public int getCurrentFuelValue() {
        if (IS_ENERGY) return -1;

        return this.current_fuel_value;
    }

    public void overrideCurrentFuelValue(int fuel) {
        if(IS_ENERGY) return;

        // Les deux gardes précédentes étaient écrasées par l'affectation finale, si bien
        // que la réserve pouvait devenir négative ou dépasser la capacité (cf. BUG-013).
        this.current_fuel_value = Mth.clamp(fuel, 0, this.getFuelCapacity());
    }

    public void addToCurrentFuelValue(int fuel) {
        if(IS_ENERGY) return;

        this.current_fuel_value += fuel;
        this.overrideCurrentFuelValue(this.current_fuel_value);
    }

    public void removeFromToCurrentFuelValue(int fuel) {
        if(IS_ENERGY) return;

        this.current_fuel_value -= fuel;
        this.overrideCurrentFuelValue(this.current_fuel_value);
    }

    // Interface (Whitelist)

    public boolean isWhitelist() {
        return isWhitelist;
    }

    public void setWhitelist(boolean whitelist) {
        if (this.isWhitelist == whitelist) return;

        this.isWhitelist = whitelist;
        syncToClients();
    }

    // Interface (Condition redstone)

    /** @return {@code true} si la définition déclare cet inserter sensible au redstone */
    public boolean isAffectedByRedstone() {
        return inserter.isAffectedByRedstone();
    }

    public InserterRedstoneCondition getRedstoneCondition() {
        return this.redstoneCondition;
    }

    /**
     * Change la condition et réévalue immédiatement l'état du bloc.
     *
     * <p>Sans cette réévaluation, un inserter resterait dans l'état décidé par l'ancienne
     * condition jusqu'au prochain changement de voisinage — c'est-à-dire, pour un signal
     * stable, indéfiniment.
     */
    public void setRedstoneCondition(InserterRedstoneCondition condition) {
        if (this.redstoneCondition.equals(condition)) return;

        this.redstoneCondition = condition;
        wakeUp();
        syncToClients();

        if (this.level != null && !this.level.isClientSide) {
            getBlockState().getBlock().neighborChanged(
                    getBlockState(), this.level, this.worldPosition, this.level.getBlockState(this.worldPosition).getBlock(), this.worldPosition, false);
        }
    }

    /**
     * Lit la condition, en gardant le défaut historique pour un monde antérieur.
     *
     * <p>{@code contains} et non {@code getByte} : sans la clé, {@code getByte} rendrait 0,
     * soit {@code ALWAYS}, et tous les inserters d'un monde existant cesseraient de
     * répondre à la redstone.
     */
    private static InserterRedstoneCondition readCondition(CompoundTag tag) {
        if (!tag.contains("inserterRedstoneMode")) return InserterRedstoneCondition.DEFAULT;

        return new InserterRedstoneCondition(
                InserterRedstoneCondition.Mode.byOrdinal(tag.getByte("inserterRedstoneMode")),
                tag.getByte("inserterRedstoneThreshold"));
    }

    // Interface (Enabled)

    public boolean isEnabled() {
        return this.getBlockState().getValue(BlockStateProperties.ENABLED);
    }

    public void setEnabled(boolean enabled) {
        if (this.level == null) return;

        // BlockState est immuable : setValue renvoie un nouvel état, il faut le poser
        // dans le monde (cf. BUG-018).
        this.level.setBlock(
                this.worldPosition,
                this.getBlockState().setValue(BlockStateProperties.ENABLED, enabled),
                Block.UPDATE_ALL);
    }

    // Inner work

    private static Direction getFacing(BlockEntity entity) {
        return entity.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
    }

    /**
     * @return {@code true} si la pile est acceptée par les filtres
     *
     * <p>Un inserter non filtrant, ou dont tous les slots de filtre sont vides, accepte
     * tout. Sinon la présence dans la liste décide, selon le mode whitelist/blacklist.
     *
     * <p>La comparaison porte sur le <b>type</b> d'item et non sur son NBT : un filtre
     * posé avec une pioche neuve doit aussi laisser passer une pioche usée
     * (cf. DT-02).
     *
     * <p>Un slot passé en mode tag élargit la correspondance à tout item partageant un tag
     * avec lui : un filtre posé avec une plaque de fer laisse alors passer toutes les
     * plaques (cf. FIO-069).
     */
    private static boolean matchesFilters(InserterBlockEntity pEntity, ItemStack stack, boolean isWhitelist) {
        InserterSlotLayout layout = pEntity.LAYOUT;
        if (!layout.hasFilters()) return true;

        boolean anyFilterSet = false;
        boolean listed = false;

        for (int i = 0; i < layout.filterCount(); i++) {
            ItemStack filter = pEntity.itemStorage.getStackInSlot(layout.filter(i));
            if (filter.isEmpty()) continue;

            anyFilterSet = true;
            if (matchesFilter(filter, stack, pEntity.isTagFilter(i))) {
                listed = true;
                break;
            }
        }

        if (!anyFilterSet) return true;

        return isWhitelist == listed;
    }

    /** @return {@code true} si {@code stack} correspond au filtre {@code filter} */
    public static boolean matchesFilter(ItemStack filter, ItemStack stack, boolean byTag) {
        if (ItemStack.isSameItem(filter, stack)) return true;
        if (!byTag) return false;

        // Un tag partagé suffit. C'est volontairement large : tant que le joueur ne peut
        // pas désigner *quel* tag — ce que la refonte du GUI apportera (FIO-071) — poser
        // une plaque de fer pour dire « les plaques » est le geste le plus naturel.
        return filter.getTags().anyMatch(stack::is);
    }

    // Interface (Filtres par tag)

    /** @param filterIndex rang du slot de filtre, 0-based */
    public boolean isTagFilter(int filterIndex) {
        return (this.tagFilterMask & (1 << filterIndex)) != 0;
    }

    /**
     * Pose un item fantôme dans un slot de filtre.
     *
     * <p>Le menu passe encore par ses {@code Slot}, qui écrivent dans le handler ; il
     * viendra ici avec la refonte du GUI (FIO-071), qui doit de toute façon sortir la
     * logique fantôme du menu.
     *
     * @param filterIndex rang du slot de filtre, 0-based
     */
    public void setFilter(int filterIndex, @Nonnull ItemStack filter) {
        if (!LAYOUT.hasFilters() || filterIndex < 0 || filterIndex >= LAYOUT.filterCount()) return;

        ItemStack ghost = filter.copy();
        ghost.setCount(1);

        this.itemStorage.setStackInSlot(LAYOUT.filter(filterIndex), ghost);
    }

    /** Bascule un slot de filtre entre correspondance exacte et correspondance par tag. */
    public void toggleTagFilter(int filterIndex) {
        if (!LAYOUT.hasFilters() || filterIndex < 0 || filterIndex >= LAYOUT.filterCount()) return;

        this.tagFilterMask ^= 1 << filterIndex;

        // Un filtre qui change relance un inserter que ce même filtre avait endormi.
        wakeUp();
        syncToClients();
    }

    private ItemStack insertItemInternal(int slot, @Nonnull ItemStack itemStack, boolean simulate) {
        if (itemStack.isEmpty()) return itemStack;

        ItemStack currentItemStack = this.itemStorage.getStackInSlot(slot);
        int countLimitForItemStack = itemStack.getMaxStackSize();

        if (!currentItemStack.isEmpty()) {
            if (!ItemHandlerHelper.canItemStacksStack(itemStack, currentItemStack)) {
                return itemStack;
            }

            countLimitForItemStack -= currentItemStack.getCount();
        }

        // Cannot Insert More
        if (countLimitForItemStack <= 0) return itemStack;

        boolean reachedLimit = itemStack.getCount() > countLimitForItemStack;

        if (!simulate) {
            if (currentItemStack.isEmpty()) {
                this.itemStorage.setStackInSlot(slot, reachedLimit ? ItemHandlerHelper.copyStackWithSize(itemStack, countLimitForItemStack) : itemStack);
            } else {
                currentItemStack.grow(reachedLimit ? countLimitForItemStack : itemStack.getCount());
            }

            this.setChanged();
        }

        return reachedLimit ? ItemHandlerHelper.copyStackWithSize(itemStack, itemStack.getCount() - countLimitForItemStack) : ItemStack.EMPTY;
    }

    @Nonnull
    private ItemStack extractItemInternal(int slot, int amount, boolean simulate) {
        // No Removal
        if (amount <= 0) return ItemStack.EMPTY;

        // Empty Slot
        ItemStack currentItemStack = this.itemStorage.getStackInSlot(slot);
        if (currentItemStack.isEmpty()) return ItemStack.EMPTY;

        int itemCountToExtract = Math.min(amount, currentItemStack.getMaxStackSize());

        if (currentItemStack.getCount() <= itemCountToExtract) {
            if (!simulate) {
                this.itemStorage.setStackInSlot(slot, ItemStack.EMPTY);
                this.setChanged();

                return currentItemStack;
            }

            return currentItemStack.copy();
        }

        if (!simulate) {
            this.itemStorage.setStackInSlot(slot, ItemHandlerHelper.copyStackWithSize(currentItemStack, currentItemStack.getCount() - itemCountToExtract));
            this.setChanged();
        }

        return ItemHandlerHelper.copyStackWithSize(currentItemStack, itemCountToExtract);
    }

    /**
     * @param offset  direction dans laquelle chercher le voisin, depuis l'inserter
     * @param side    face du voisin en contact avec l'inserter, du point de vue du voisin
     */
    /**
     * Résout l'inventaire voisin, avec cache.
     *
     * <p>Sans cache, chaque action coûtait un {@code getBlockEntity} suivi d'un
     * {@code getCapability} — sur le chemin le plus chaud du mod (cf. DT-07).
     *
     * <p>Seuls les résultats <b>positifs</b> sont mémorisés, et l'invalidation est
     * confiée au {@code LazyOptional} lui-même. Mettre en cache une absence serait
     * dangereux : un coffre posé à deux blocs (long handed inserter) ne déclenche aucun
     * {@code neighborChanged} sur l'inserter, le cache négatif ne serait donc jamais
     * invalidé. C'est la mise en sommeil qui borne le coût des recherches infructueuses.
     */
    @Nullable
    private IItemHandler neighbourHandler(boolean source, Direction offset, int pDistance, Direction side) {
        LazyOptional<IItemHandler> cached = source ? cachedSource : cachedTarget;
        if (cached != null && cached.isPresent()) return cached.orElse(null);

        if (this.level == null) return null;

        BlockEntity neighbour = this.level.getBlockEntity(this.worldPosition.relative(offset, pDistance));
        if (neighbour == null) return null;

        LazyOptional<IItemHandler> capability = neighbour.getCapability(ForgeCapabilities.ITEM_HANDLER, side);
        if (!capability.isPresent()) return null;

        if (source) {
            cachedSource = capability;
            capability.addListener(ignored -> cachedSource = null);
        } else {
            cachedTarget = capability;
            capability.addListener(ignored -> cachedTarget = null);
        }

        return capability.orElse(null);
    }

    /** Oublie les inventaires voisins et relance l'inserter. */
    public void onNeighbourChanged() {
        this.cachedSource = null;
        this.cachedTarget = null;
        wakeUp();
    }

    /** Nombre d'items de {@code stack} que le slot interne {@code slot} peut réellement accueillir. */
    private int simulateInsertInternal(int slot, @Nonnull ItemStack stack) {
        if (stack.isEmpty()) return 0;

        ItemStack current = this.itemStorage.getStackInSlot(slot);
        if (current.isEmpty()) {
            return Math.min(stack.getCount(), stack.getMaxStackSize());
        }
        if (!ItemHandlerHelper.canItemStacksStack(stack, current)) return 0;

        return Math.max(0, Math.min(stack.getCount(), current.getMaxStackSize() - current.getCount()));
    }

    /**
     * Nombre d'items de {@code stack} que {@code handler} accepte, tous slots confondus.
     *
     * <p>Balaye dans le même ordre que {@link #insertDistributed}, sans quoi la quantité
     * simulée ne correspondrait pas à ce qui sera réellement inséré.
     */
    private static int simulateInsert(IItemHandler handler, @Nonnull ItemStack stack, int startSlot) {
        int remaining = stack.getCount();
        int slots = handler.getSlots();

        for (int offset = 0; offset < slots && remaining > 0; offset++) {
            int slot = Math.floorMod(startSlot + offset, slots);
            remaining = handler.insertItem(slot, ItemHandlerHelper.copyStackWithSize(stack, remaining), true).getCount();
        }

        return stack.getCount() - remaining;
    }

    /** Insère en répartissant sur plusieurs slots. @return ce qui n'a pas pu être placé */
    @Nonnull
    private static ItemStack insertDistributed(IItemHandler handler, @Nonnull ItemStack stack, int startSlot) {
        ItemStack remaining = stack;
        int slots = handler.getSlots();

        for (int offset = 0; offset < slots && !remaining.isEmpty(); offset++) {
            int slot = Math.floorMod(startSlot + offset, slots);
            remaining = handler.insertItem(slot, remaining, false);
        }

        return remaining;
    }

    /**
     * Filet de sécurité : un reliquat inattendu ne doit jamais être détruit (cf. BUG-006).
     * On tente de le rendre à la source, puis on le laisse tomber au sol en dernier recours.
     */
    private void rescueLeftover(@Nonnull ItemStack leftover, UnaryOperator<ItemStack> fallback) {
        if (leftover.isEmpty()) return;

        FactoryIO.LOGGER.warn("Reliquat inattendu lors d'un transfert en {} : {}", this.worldPosition, leftover);

        this.dropAtBlock(fallback.apply(leftover));
    }

    /** Laisse tomber une pile au sol, en dernier recours. Sans effet si elle est vide. */
    private void dropAtBlock(@Nonnull ItemStack stack) {
        if (stack.isEmpty() || this.level == null) return;

        Containers.dropItemStack(
                this.level,
                this.worldPosition.getX() + 0.5,
                this.worldPosition.getY() + 0.5,
                this.worldPosition.getZ() + 0.5,
                stack);
    }

    /**
     * Prend au plus une « main » d'items acceptés par {@code accept} et les place dans
     * {@code targetSlot}.
     *
     * <p>Ordre impératif : simuler l'extraction, calculer ce que la destination accepte
     * réellement, puis extraire exactement cette quantité. L'inverse détruit des items.
     *
     * @return la pile prélevée, vide si rien n'a bougé
     */
    @Nonnull
    private static ItemStack grabInto(InserterBlockEntity pEntity, IItemHandler source, int targetSlot, Predicate<ItemStack> accept) {
        int wanted = pEntity.getMaximumItemCountPerAction();
        int slots = source.getSlots();

        for (int offset = 0; offset < slots; offset++) {
            // Balayage circulaire depuis le dernier slot fructueux : sur un grand coffre
            // dont seuls les derniers slots sont remplis, le coût devient constant en
            // régime établi au lieu d'être proportionnel à la taille (cf. DT-07).
            int slot = Math.floorMod(pEntity.lastSourceSlot + offset, slots);

            ItemStack probe = source.extractItem(slot, wanted, true);
            if (probe.isEmpty() || !accept.test(probe)) continue;

            int movable = pEntity.simulateInsertInternal(targetSlot, probe);
            if (movable <= 0) continue;

            ItemStack taken = source.extractItem(slot, movable, false);
            if (taken.isEmpty()) continue;

            pEntity.lastSourceSlot = slot;
            pEntity.rescueLeftover(
                    pEntity.insertItemInternal(targetSlot, taken, false),
                    rest -> ItemHandlerHelper.insertItem(source, rest, false));
            return taken;
        }

        return ItemStack.EMPTY;
    }

    /**
     * Saisit une main d'items dans l'inventaire arrière.
     *
     * <p>Le ravitaillement en carburant ne passe plus par ici : c'est {@code tickWaiting}
     * qui le tente d'abord, et cette méthode n'est appelée qu'avec la main vide — un
     * invariant de l'état {@code WAITING}.
     *
     * @return la pile prélevée, vide si rien n'a bougé
     */
    @Nonnull
    private static ItemStack suckItems(InserterBlockEntity pEntity, int pDistance, boolean isWhitelist) {
        Direction facing = getFacing(pEntity);

        // La face du coffre en contact avec l'inserter, vue depuis le coffre, est `facing`.
        IItemHandler source = pEntity.neighbourHandler(true, facing.getOpposite(), pDistance, facing);
        if (source == null) return ItemStack.EMPTY;

        return grabInto(pEntity, source, BUFFER_SLOT, stack -> matchesFilters(pEntity, stack, isWhitelist));
    }

    /** @return la pile déposée dans l'inventaire avant, vide si rien n'a bougé */
    @Nonnull
    private static ItemStack expelItems(InserterBlockEntity pEntity, int pDistance) {
        ItemStack buffer = pEntity.itemStorage.getStackInSlot(BUFFER_SLOT);
        if (buffer.isEmpty()) return ItemStack.EMPTY;

        Direction facing = getFacing(pEntity);

        // La face de la cible en contact avec l'inserter, vue depuis la cible, est l'opposé
        // de `facing` (cf. BUG-023).
        IItemHandler target = pEntity.neighbourHandler(false, facing, pDistance, facing.getOpposite());
        if (target == null) return ItemStack.EMPTY;

        int wanted = Math.min(buffer.getCount(), pEntity.getMaximumItemCountPerAction());

        ItemStack probe = pEntity.extractItemInternal(BUFFER_SLOT, wanted, true);
        if (probe.isEmpty()) return ItemStack.EMPTY;

        int startSlot = Math.floorMod(pEntity.lastTargetSlot, Math.max(1, target.getSlots()));

        int movable = simulateInsert(target, probe, startSlot);
        if (movable <= 0) return ItemStack.EMPTY;

        ItemStack taken = pEntity.extractItemInternal(BUFFER_SLOT, movable, false);
        if (taken.isEmpty()) return ItemStack.EMPTY;

        pEntity.lastTargetSlot = firstAcceptingSlot(target, taken, startSlot);
        pEntity.rescueLeftover(
                insertDistributed(target, taken, startSlot),
                rest -> pEntity.insertItemInternal(BUFFER_SLOT, rest, false));
        return taken;
    }

    /**
     * Premier slot de {@code handler}, à partir de {@code startSlot}, qui accepte quelque
     * chose de {@code stack}.
     *
     * <p>C'est lui qu'il faut mémoriser, et non {@code startSlot} : réécrire le point de
     * départ avec lui-même rendait la mémorisation du dernier slot fructueux inopérante
     * du côté cible, alors que le côté source en bénéficiait bien (cf. DT-07, BUG-036).
     */
    private static int firstAcceptingSlot(IItemHandler handler, @Nonnull ItemStack stack, int startSlot) {
        int slots = handler.getSlots();

        for (int offset = 0; offset < slots; offset++) {
            int slot = Math.floorMod(startSlot + offset, slots);

            if (handler.insertItem(slot, stack, true).getCount() < stack.getCount()) return slot;
        }

        return startSlot;
    }

    private void useFuelOrEnergy() {
        if (this.IS_ENERGY) {
            this.consumeEnergy(this.getFuelConsumptionPerAction());
        }
        else {
            this.removeFromToCurrentFuelValue(this.getFuelConsumptionPerAction());
        }
    }

    // Interface GeckLib

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::idlePredicate));
    }

    private PlayState idlePredicate(AnimationState<InserterBlockEntity> state) {
        state.getController().setAnimation(IDLE);

        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animatableCache;
    }

    // Interface (Menu)

    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new InserterContainer(pContainerId, inserter, pPlayerInventory, level, getBlockPos());
    }
}
