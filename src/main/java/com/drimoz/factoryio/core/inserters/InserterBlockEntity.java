package com.drimoz.factoryio.core.inserters;

import com.drimoz.factoryio.core.generic.block.RedstoneCondition;
import com.drimoz.factoryio.FactoryIO;
import net.minecraft.world.MenuProvider;
import com.drimoz.factoryio.core.generic.container.energy.EnergyContainer;
import com.drimoz.factoryio.core.model.Inserter;
import com.drimoz.factoryio.core.model.InserterTuning;
import com.drimoz.factoryio.core.init.ModTags;
import com.drimoz.factoryio.core.upgrade.InserterUpgradeTuning;
import com.drimoz.factoryio.core.upgrade.InserterUpgradeTunings;
import com.drimoz.factoryio.core.upgrade.InserterUpgradeType;
import com.drimoz.factoryio.core.upgrade.InserterUpgrades;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import com.drimoz.factoryio.core.belts.BeltItemHandler;
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
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public class InserterBlockEntity extends BlockEntity implements MenuProvider, GeoBlockEntity {

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

    /** Vue restreinte exposée aux voisins ; le menu passe par {@link #getMenuItems()}. */
    private ExternalItemHandler externalItems;

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
    private RedstoneCondition redstoneCondition = RedstoneCondition.DEFAULT;

    /** Copie de la propriété {@code ENABLED}, cf. {@link #setBlockState}. */
    private boolean enabled;

    /**
     * Interrupteur manuel (FIO-167).
     *
     * <p>Il ne double pas la condition redstone, il la précède : éteint, l'inserter s'arrête
     * quel que soit le signal ; allumé, c'est la condition qui décide. Les deux se résument
     * dans la seule propriété {@code ENABLED} — le tick et le rendu n'en lisent pas d'autre.
     */
    private boolean switchedOn = true;

    /**
     * Plafond d'items par prise, choisi par le joueur (FIO-168). {@link #HAND_SIZE_MAX} veut
     * dire « le maximum » : il suit alors la capacité, modules compris, au lieu de rester
     * figé sur la valeur du moment où le réglage a été fait.
     */
    private int handSizeLimit = HAND_SIZE_MAX;

    /** Voie visée sur un convoyeur (FIO-169). */
    private InserterDropLane dropLane = InserterDropLane.AUTO;

    /**
     * Items livrés depuis le chargement du bloc, pour le débit mesuré de l'écran (FIO-170).
     *
     * <p>Un compteur, pas une moyenne : c'est l'écran qui échantillonne et divise. Il peut
     * déborder sans conséquence, seules les différences sont lues.
     */
    private int itemsDelivered;

    /** Valeur du plafond de main qui signifie « le maximum ». */
    public static final int HAND_SIZE_MAX = 0;

    /** Plafond le plus haut qu'on puisse régler : une pile. */
    public static final int HAND_SIZE_CEILING = 64;

    /**
     * Interpolation du mouvement de tourelle, réglable par machine (FIO-161).
     *
     * <p>Purement visuel : ce drapeau ne doit changer ni le débit, ni les coûts, ni le
     * comportement de transfert. Il vit néanmoins ici, et non dans une configuration client,
     * parce qu'il a été demandé <b>sur la machine</b> — ce qui permet de calmer un inserter
     * précis sans désactiver les autres, là où un réglage global ne le permettrait pas.
     *
     * <p>Désactivé, la tourelle ne se fige pas : elle saute d'une pose à l'autre. Un bras
     * immobile rendrait indiscernables un inserter bloqué, un inserter au repos et un
     * inserter au travail.
     */
    private InserterAnimationMode animationMode = InserterAnimationMode.SMOOTH;

    /** Modules posés sur cet exemplaire. */
    private InserterUpgrades upgrades = InserterUpgrades.NONE;

    /**
     * Réglages effectifs, améliorations comprises, et la base dont ils sont dérivés.
     *
     * <p>Le cache est validé par <b>identité</b> de la base : un datapack remplace le
     * {@link InserterTuning} d'un type d'un seul bloc (FIO-037), jamais champ par champ.
     * Une comparaison de référence suffit donc à détecter un {@code /reload}, pour le prix
     * d'un test par appel — et {@code getTicksPerSwing} est appelé à chaque image côté
     * client, pour interpoler le bras.
     */
    private InserterTuning effectiveTuning;
    private InserterTuning effectiveBase;
    private InserterUpgradeTuning effectiveUpgradeTuning;

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

    /**
     * Positions auxquelles les inventaires mémorisés ont été résolus.
     *
     * <p>Le cache est indexé par position et non par rôle seul. Sans cela, tourner un
     * inserter à la clé conservait les deux inventaires d'avant la rotation : le bloc
     * n'est pas notifié de son propre changement d'état — {@code setBlock} prévient les
     * voisins, pas la position elle-même — et le block entity survit puisque le bloc, lui,
     * ne change pas. L'inserter continuait donc d'aspirer et de déposer du mauvais côté
     * jusqu'au prochain rechargement de chunk. Un datapack qui change {@code grabDistance}
     * à chaud produisait le même décalage.
     */
    private BlockPos cachedSourcePos;
    private BlockPos cachedTargetPos;

    /** Dernier slot ayant abouti, pour repartir de là plutôt que du slot 0. */
    private int lastSourceSlot = 0;
    private int lastTargetSlot = 0;

    /**
     * Taille à partir de laquelle on mémorise le point de départ du balayage.
     *
     * <p>La mémorisation existe pour les <b>grands</b> inventaires : sur un double coffre dont
     * seuls les derniers slots servent, elle rend le coût constant en régime établi au lieu de
     * proportionnel (DT-07).
     *
     * <p>En deçà, elle ne fait rien gagner et elle <b>nuit</b> : commencer ailleurs qu'au
     * premier slot défait l'ordre de priorité que la cible exprime par son indexation. Un
     * convoyeur range ses cases voie lointaine d'abord (FIO-097) ; un balayage qui démarre au
     * milieu saute cette voie et dépose du mauvais côté, ce qui vide la règle de son sens.
     *
     * <p>Le seuil est placé au-dessus du plus grand inventaire à qui l'ordre importe — huit
     * cases pour un convoyeur, trois pour un four — et en dessous d'un coffre simple.
     */
    private static final int MEMOISED_SCAN_THRESHOLD = 16;

    /** Mise en sommeil après des tentatives infructueuses répétées. */
    private int failedAttempts = 0;
    private int sleepTicks = 0;

    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);

    // Life cycle

    public InserterBlockEntity(BlockPos blockPos, BlockState blockState, Inserter inserter) {
        super(inserter.getBlockEntityType().get(), blockPos, blockState);

        this.inserter = inserter;

        this.IS_ENERGY = inserter.useEnergy();
        this.IS_FILTER = inserter.isFilterable();
        this.LAYOUT = InserterSlotLayout.of(inserter);
        this.enabled = readEnabled(blockState);

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

                // Les modules sont des items dans des slots : poser, retirer ou déplacer
                // l'un d'eux passe forcément par ici. C'est le seul point de mise à jour
                // des paliers, ce qui rend impossible qu'ils divergent du contenu réel.
                if (LAYOUT.isUpgrade(slot)) refreshUpgrades();
            }
        };

        // La capability expose une VUE restreinte, pas le stockage lui-même.
        //
        // Les restrictions vivaient sur le stockage, si bien que tout le monde les subissait
        // — y compris le menu, qui passe par la capability. Un module ne pouvait donc ni
        // entrer dans son slot ni en sortir : « seul le slot de carburant est accessible »
        // était vrai du joueur autant que d'un hopper. Le stockage est désormais neutre, et
        // le contrat externe est une enveloppe. C'est la même séparation que celle faite pour
        // l'énergie (cf. BUG-003), et pour la même raison : ce que la machine s'autorise sur
        // son propre inventaire n'a rien à voir avec ce qu'elle offre à ses voisins.
        this.externalItems = new ExternalItemHandler(this.itemStorage, LAYOUT);

        this.lazyItem = LazyOptional.of(() -> this.externalItems);
    }

    /**
     * Ce que les voisins — hoppers, tuyaux, autres mods — ont le droit de faire.
     *
     * <p>Volontairement plus étroit que le stockage : on <b>dépose</b> du carburant, on
     * <b>reprend</b> les résidus, et rien d'autre. Le buffer de transport, les filtres et les
     * modules ne sont pas accessibles de l'extérieur.
     */
    private static final class ExternalItemHandler implements IItemHandlerModifiable {

        private final IItemHandlerModifiable storage;
        private final InserterSlotLayout layout;

        private ExternalItemHandler(IItemHandlerModifiable storage, InserterSlotLayout layout) {
            this.storage = storage;
            this.layout = layout;
        }

        @Override
        public int getSlots() {
            return this.storage.getSlots();
        }

        @NotNull
        @Override
        public ItemStack getStackInSlot(int slot) {
            return this.storage.getStackInSlot(slot);
        }

        @Override
        public int getSlotLimit(int slot) {
            return this.storage.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return acceptsFuel(slot, stack);
        }

        @Override
        public void setStackInSlot(int slot, @NotNull ItemStack stack) {
            // Écrire directement contournerait les deux règles ci-dessous ; un mod qui
            // manipule la capability à la main ne doit pas avoir plus de droits qu'un hopper.
            if (!acceptsFuel(slot, stack)) return;

            this.storage.setStackInSlot(slot, stack);
        }

        @NotNull
        @Override
        public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (!acceptsFuel(slot, stack)) return stack;

            return this.storage.insertItem(slot, stack, simulate);
        }

        /**
         * Seuls les <b>résidus</b> ressortent du slot de carburant.
         *
         * <p>C'est la règle du four vanilla : un hopper récupère le seau vide, pas le
         * charbon. Autoriser l'extraction du carburant lui-même transformait tout hopper
         * placé sous un burner inserter en siphon, qui le laissait à sec en boucle sans que
         * rien ne l'explique au joueur (cf. BUG-044).
         */
        @NotNull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != this.layout.fuel()) return ItemStack.EMPTY;
            if (ForgeHooks.getBurnTime(this.storage.getStackInSlot(slot), null) > 0) return ItemStack.EMPTY;

            return this.storage.extractItem(slot, amount, simulate);
        }

        private boolean acceptsFuel(int slot, @NotNull ItemStack stack) {
            return slot == this.layout.fuel()
                    && ForgeHooks.getBurnTime(stack, null) > 0
                    && stack.is(ModTags.Items.INSERTER_FUEL);
        }
    }

    /**
     * Le stockage réel, sans restriction — pour le menu.
     *
     * <p>Le joueur qui a l'écran ouvert n'est pas un voisin : il pose et retire ses modules,
     * dépose du carburant à la main, et ce sont des gestes que la capability n'a aucune
     * raison d'autoriser à un tuyau.
     */
    public IItemHandlerModifiable getMenuItems() {
        return this.itemStorage;
    }

    // Interface (DataFromData)

    /**
     * Réglages de cet exemplaire : ceux de son type, modifiés par les modules posés.
     *
     * <p>Tout ce qui dépend de la vitesse, de la portée, de la taille de main ou des coûts
     * passe par ici — et par ici seulement. C'est ce qui permet aux améliorations d'agir
     * partout, y compris sur la trajectoire affichée et sur les jauges, sans un seul
     * {@code if} ailleurs dans la classe.
     */
    /**
     * <p>Le cache est revalidé sur <b>deux</b> références, et la seconde vaut d'être
     * expliquée. Un {@code /reload} qui ne change que les facteurs d'amélioration laisse le
     * réglage du type inchangé : surveiller la seule base suffirait à manquer le
     * rechargement, et les modules garderaient leur ancien effet jusqu'au redémarrage. Les
     * deux objets étant remplacés d'un bloc et jamais champ par champ, un {@code !=} suffit
     * dans les deux cas.
     */
    public InserterTuning getEffectiveTuning() {
        InserterTuning base = inserter.getTuning();
        InserterUpgradeTuning upgradeTuning = upgradeTuning();

        if (this.effectiveBase != base || this.effectiveUpgradeTuning != upgradeTuning) {
            this.effectiveBase = base;
            this.effectiveUpgradeTuning = upgradeTuning;
            this.effectiveTuning = this.upgrades.applyTo(base, upgradeTuning);
        }

        return this.effectiveTuning;
    }

    /** Barème des améliorations en vigueur — point de passage unique. */
    private InserterUpgradeTuning upgradeTuning() {
        return InserterUpgradeTunings.current();
    }

    public int getMaximumItemCountPerAction(){
        return getEffectiveTuning().handSize();
    }

    public int getGrabDistance(){
        return getEffectiveTuning().grabDistance();
    }

    /** Durée d'un mouvement de bras, en ticks. Un item en coûte deux. */
    public int getTicksPerSwing(){
        return getEffectiveTuning().ticksPerSwing();
    }

    public int getFuelCapacity(){
        return IS_ENERGY ? getEffectiveTuning().energyCapacity() : getEffectiveTuning().fuelCapacity();
    }

    public int getFuelConsumptionPerAction() {
        return IS_ENERGY ? getEffectiveTuning().energyConsumption() : getEffectiveTuning().fuelConsumption();
    }

    /** Débit théorique en items par seconde : améliorations et taille de main réglée comprises. */
    public double getItemsPerSecond() {
        return 20.0D * getHandSize() / (2.0D * getTicksPerSwing());
    }

    /**
     * Items saisis par prise : la capacité, ou moins si le joueur l'a plafonnée (FIO-168).
     *
     * <p>Le plafond ne relève jamais la capacité : il ne peut que la réduire. Réglé à 3 sur un
     * inserter qui en prenait 4 grâce à un module, il reste à 3 ; module retiré, il tombe à ce
     * que l'inserter sait encore prendre.
     */
    public int getHandSize() {
        int capacity = getMaximumItemCountPerAction();

        return this.handSizeLimit == HAND_SIZE_MAX ? capacity : Math.min(this.handSizeLimit, capacity);
    }

    /** @return le plafond réglé, ou {@link #HAND_SIZE_MAX} */
    public int getHandSizeLimit() {
        return this.handSizeLimit;
    }

    /**
     * Borné, pas refusé : une valeur forgée hors du domaine y est ramenée, comme le seuil
     * redstone. Une valeur au-delà de la capacité du moment est gardée telle quelle — un
     * module de capacité posé plus tard la rendra effective.
     */
    public void setHandSizeLimit(int limit) {
        int bounded = Mth.clamp(limit, HAND_SIZE_MAX, HAND_SIZE_CEILING);
        if (this.handSizeLimit == bounded) return;

        this.handSizeLimit = bounded;
        wakeUp();
        syncToClients();
    }

    public InserterDropLane getDropLane() {
        return this.dropLane;
    }

    public void setDropLane(InserterDropLane lane) {
        if (this.dropLane == lane) return;

        this.dropLane = lane;
        wakeUp();
        syncToClients();
    }

    /** Compteur d'items livrés, lu par l'écran pour mesurer le débit réel. */
    public int getItemsDelivered() {
        return this.itemsDelivered;
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

    /**
     * Lâche le contenu réel au sol. Les filtres sont des items fantômes : ils ne tombent pas.
     *
     * <p>Les modules d'amélioration tombent sans un mot de plus : depuis qu'ils occupent des
     * slots, ils font partie du contenu réel. La ligne qui les rendait séparément a disparu
     * avec l'état qui les dupliquait.
     */
    public void drops() {
        List<ItemStack> dropped = new ArrayList<>();

        for (int slot = 0; slot < itemStorage.getSlots(); slot++) {
            if (!LAYOUT.isDroppable(slot)) continue;

            dropped.add(itemStorage.getStackInSlot(slot));
        }

        SimpleContainer inventory = new SimpleContainer(dropped.size());
        for (int i = 0; i < dropped.size(); i++) {
            inventory.setItem(i, dropped.get(i));
        }

        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    // Interface (Améliorations)

    public InserterUpgrades getUpgrades() {
        return this.upgrades;
    }

    /**
     * Pose un module dans le premier slot d'amélioration libre.
     *
     * <p>Le geste au clic droit reste utile — c'est le plus rapide, et il fonctionne sans
     * ouvrir le menu — mais il n'a plus rien de particulier : il ne fait que remplir un
     * slot. Le remplacement d'un palier inférieur par un meilleur, qui était nécessaire
     * tant qu'un axe n'admettait qu'un module, n'a plus lieu d'être : deux modules cohabitent
     * désormais, et le joueur retire celui qu'il veut par le menu.
     *
     * @return {@code ItemStack.EMPTY} si le module est posé ; {@code null} s'il est refusé —
     *         parce qu'il n'en est pas un, ou parce qu'il ne reste aucun slot libre
     */
    @Nullable
    public ItemStack installUpgrade(InserterUpgradeType type, @Nonnull ItemStack module) {
        if (type.levelOf(module) <= 0) return null;

        int free = firstFreeUpgradeSlot();
        if (free == InserterSlotLayout.NONE) return null;

        ItemStack single = module.copy();
        single.setCount(1);

        // setStackInSlot passe par onContentsChanged, qui relit les paliers : il n'y a rien
        // à mettre à jour ici.
        this.itemStorage.setStackInSlot(free, single);

        return ItemStack.EMPTY;
    }

    /** @return l'index du premier slot d'amélioration vide, ou {@link InserterSlotLayout#NONE} */
    private int firstFreeUpgradeSlot() {
        for (int i = 0; i < LAYOUT.upgradeCount(); i++) {
            int slot = LAYOUT.upgrade(i);

            if (this.itemStorage.getStackInSlot(slot).isEmpty()) return slot;
        }

        return InserterSlotLayout.NONE;
    }

    /**
     * Relit les paliers depuis les slots d'amélioration.
     *
     * <p><b>Serveur uniquement.</b> Le client ne reçoit pas le contenu des slots — ils ne
     * sont dans aucun tag de mise à jour — donc les relire chez lui remettrait tous les
     * paliers à zéro et ferait ralentir l'animation d'un inserter amélioré. Ses paliers
     * viennent de {@code getUpdateTag}.
     */
    private void refreshUpgrades() {
        if (this.level != null && this.level.isClientSide) return;

        InserterUpgrades refreshed = InserterUpgrades.from(
                this.itemStorage, LAYOUT.firstUpgrade(), LAYOUT.upgradeCount());

        if (refreshed.equals(this.upgrades)) return;

        this.upgrades = refreshed;
        invalidateEffectiveTuning();

        wakeUp();
        syncToClients();
        // Le module de redstone avancé change la condition qui s'applique : sans cela, la
        // pose ou le retrait n'aurait d'effet qu'au prochain changement de voisinage.
        reevaluateEnabled();
    }

    /** Force le recalcul des réglages effectifs au prochain accès. */
    private void invalidateEffectiveTuning() {
        this.effectiveBase = null;
        this.effectiveTuning = null;
        this.effectiveUpgradeTuning = null;
    }

    // Interface (Réglages transportables)

    /** Photographie des réglages, pour un configurateur. */
    public InserterSettings captureSettings() {
        List<ItemStack> filters = new ArrayList<>(LAYOUT.filterCount());

        for (int i = 0; i < LAYOUT.filterCount(); i++) {
            filters.add(this.itemStorage.getStackInSlot(LAYOUT.filter(i)).copy());
        }

        return new InserterSettings(this.animationMode, this.isWhitelist, this.tagFilterMask, this.redstoneCondition,
                this.handSizeLimit, this.dropLane, filters);
    }

    /**
     * Applique des réglages venus d'ailleurs.
     *
     * <p>Chaque partie n'est appliquée que si elle a un sens ici : les filtres d'un inserter
     * filtrant vers un inserter qui ne l'est pas n'iraient nulle part, et une condition
     * redstone sur un inserter insensible au redstone serait un réglage sans effet. Le
     * reste passe quand même — copier un réglage entre deux modèles différents est un
     * usage normal, pas une erreur.
     *
     * @return {@code true} si quelque chose a effectivement changé
     */
    public boolean applySettings(InserterSettings settings) {
        boolean changed = false;

        if (this.animationMode != settings.animation()) {
            this.animationMode = settings.animation();
            changed = true;
        }

        if (this.isWhitelist != settings.whitelist()) {
            this.isWhitelist = settings.whitelist();
            changed = true;
        }

        if (LAYOUT.hasFilters()) {
            int mask = settings.tagFilterMask() & ((1 << LAYOUT.filterCount()) - 1);
            if (this.tagFilterMask != mask) {
                this.tagFilterMask = mask;
                changed = true;
            }

            List<ItemStack> filters = settings.filters();
            for (int i = 0; i < LAYOUT.filterCount(); i++) {
                ItemStack filter = i < filters.size() ? filters.get(i) : ItemStack.EMPTY;

                if (ItemStack.isSameItemSameTags(this.itemStorage.getStackInSlot(LAYOUT.filter(i)), filter)) continue;

                this.itemStorage.setStackInSlot(LAYOUT.filter(i), filter.copy());
                changed = true;
            }
        }

        if (isAffectedByRedstone() && !this.redstoneCondition.equals(settings.redstone())) {
            setRedstoneCondition(settings.redstone());
            changed = true;
        }

        int handSize = Mth.clamp(settings.handSizeLimit(), HAND_SIZE_MAX, HAND_SIZE_CEILING);
        if (this.handSizeLimit != handSize) {
            this.handSizeLimit = handSize;
            changed = true;
        }

        if (this.dropLane != settings.dropLane()) {
            this.dropLane = settings.dropLane();
            changed = true;
        }

        if (changed) {
            wakeUp();
            syncToClients();
        }

        return changed;
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

        this.lazyItem = LazyOptional.of(() -> this.externalItems);
        if(IS_ENERGY) {
            this.lazyEnergy = LazyOptional.of(() -> energyStorage);
            syncEnergyLimitsFromDefinition();
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
        tag.putByte("inserterAnimation", (byte) this.animationMode.ordinal());
        writeSettings(tag);

        // L'état du bras est persisté : un inserter bloqué doit se retrouver bloqué au
        // rechargement, pas remis au repos avec un item fantôme en main.
        tag.putByte("inserterState", (byte) this.state.ordinal());
        tag.putBoolean("inserterCarryingFuel", this.carryingFuel);
        tag.putLong("inserterSwingEnd", this.swingEndTick);

        // Le buffer suffit à retrouver un item en cours de livraison, mais pas un trajet
        // de ravitaillement : le carburant a déjà rejoint son slot, le buffer est vide, et
        // l'item affiché disparaissait au rechargement en plein mouvement.
        if (!this.heldStack.isEmpty()) {
            tag.put("inserterHeldStack", this.heldStack.save(new CompoundTag()));
        }

        // Rien pour les améliorations : les modules sont dans « inserterInventory » comme
        // n'importe quel item, et les paliers s'en déduisent. Les écrire ici serait une
        // seconde vérité, donc une occasion de diverger.

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
        this.animationMode = InserterAnimationMode.byOrdinal(tag.getByte("inserterAnimation"));
        readSettings(tag);

        migrateLegacyUpgrades(tag);

        this.upgrades = InserterUpgrades.from(
                this.itemStorage, LAYOUT.firstUpgrade(), LAYOUT.upgradeCount());
        invalidateEffectiveTuning();

        this.state = InserterState.byOrdinal(tag.getByte("inserterState"));
        this.carryingFuel = tag.getBoolean("inserterCarryingFuel");
        this.swingEndTick = tag.getLong("inserterSwingEnd");

        // Le buffer reste la solution de repli pour un monde sauvegardé avant que la main
        // ne soit persistée pour elle-même.
        this.heldStack = tag.contains("inserterHeldStack")
                ? ItemStack.of(tag.getCompound("inserterHeldStack"))
                : this.itemStorage.getStackInSlot(InserterSlotLayout.BUFFER).copy();

        // Un monde sauvegardé avant FIO-060 n'a pas d'état : « inserterState » vaut alors 0,
        // soit WAITING, ce qui est le bon défaut. Mais son buffer peut contenir un item —
        // l'ancien modèle le gardait entre deux actions. WAITING avec la main pleine est
        // une combinaison que la machine à états n'admet pas : on la ramène à BLOCKED,
        // l'état qui décrit exactement « un item à livrer et rien pour l'instant ».
        if (this.state == InserterState.WAITING && !this.heldStack.isEmpty()) {
            this.state = InserterState.BLOCKED;
        }
    }

    /**
     * Reloge dans les slots les modules d'un monde d'avant les slots d'amélioration.
     *
     * <p>L'ancien format rangeait, sous {@code inserterUpgrades}, un sous-compound par axe
     * portant {@code level} et {@code item}. Sans cette reprise, les modules d'un joueur
     * disparaîtraient au premier chargement — et pas seulement leur effet : les <b>items</b>
     * eux-mêmes, qu'il avait fabriqués. Le mod s'interdit de détruire un item ailleurs ; il
     * n'y a pas de raison d'y consentir ici.
     *
     * <p>La reprise est <b>sans perte tolérée</b> : un module qui ne trouve pas de slot
     * libre — sur un inserter dont la définition en offre moins que l'ancien système n'en
     * admettait — tombe au sol plutôt que d'être oublié.
     */
    private void migrateLegacyUpgrades(CompoundTag tag) {
        if (!tag.contains("inserterUpgrades")) return;

        CompoundTag legacy = tag.getCompound("inserterUpgrades");

        for (InserterUpgradeType type : InserterUpgradeType.all()) {
            if (!legacy.contains(type.id())) continue;

            CompoundTag entry = legacy.getCompound(type.id());
            if (!entry.contains("item")) continue;

            ItemStack module = ItemStack.of(entry.getCompound("item"));
            if (module.isEmpty()) continue;

            int free = firstFreeUpgradeSlot();
            if (free != InserterSlotLayout.NONE) {
                this.itemStorage.setStackInSlot(free, module);
                continue;
            }

            // Pas de place : on ne le garde pas en silence.
            FactoryIO.LOGGER.warn(
                    "{} en {} : plus de slot d'amélioration libre, {} est rendu au sol",
                    inserter.getId(), this.worldPosition, module);

            dropAtBlock(module);
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
        tag.putByte("inserterAnimation", (byte) this.animationMode.ordinal());
        writeSettings(tag);
        tag.putByte("inserterState", (byte) this.state.ordinal());
        tag.putBoolean("inserterCarryingFuel", this.carryingFuel);
        tag.putLong("inserterSwingEnd", this.swingEndTick);

        // Rien pour une main vide : le tag part à chaque sendBlockUpdated, autant ne pas y
        // traîner un ItemStack.EMPTY sérialisé.
        if (!this.heldStack.isEmpty()) {
            tag.put("inserterHeldStack", this.heldStack.save(new CompoundTag()));
        }

        // Les paliers seulement : ils changent la durée d'un mouvement et la taille de la
        // main, donc ce que le client affiche. Les modules posés ne servent qu'au serveur.
        if (!this.upgrades.isEmpty()) {
            tag.put("inserterUpgrades", this.upgrades.save());
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
        this.animationMode = InserterAnimationMode.byOrdinal(tag.getByte("inserterAnimation"));
        readSettings(tag);
        this.state = InserterState.byOrdinal(tag.getByte("inserterState"));
        this.carryingFuel = tag.getBoolean("inserterCarryingFuel");
        this.swingEndTick = tag.getLong("inserterSwingEnd");
        this.heldStack = tag.contains("inserterHeldStack")
                ? ItemStack.of(tag.getCompound("inserterHeldStack"))
                : ItemStack.EMPTY;

        this.upgrades = InserterUpgrades.load(tag.getCompound("inserterUpgrades"));
        invalidateEffectiveTuning();
    }

    /** Les réglages de FIO-167 à 169, communs à la sauvegarde et à la synchronisation. */
    private void writeSettings(CompoundTag tag) {
        tag.putBoolean("inserterSwitchedOn", this.switchedOn);
        tag.putByte("inserterHandSize", (byte) this.handSizeLimit);
        tag.putByte("inserterDropLane", (byte) this.dropLane.ordinal());
    }

    /**
     * Un monde antérieur n'a aucune de ces clés, et chacune retombe sur le comportement
     * d'avant : allumé (d'où le {@code contains}, {@code getBoolean} rendrait faux), main au
     * maximum, voie laissée à la bande.
     */
    private void readSettings(CompoundTag tag) {
        this.switchedOn = !tag.contains("inserterSwitchedOn") || tag.getBoolean("inserterSwitchedOn");
        this.handSizeLimit = Mth.clamp(tag.getByte("inserterHandSize"), HAND_SIZE_MAX, HAND_SIZE_CEILING);
        this.dropLane = InserterDropLane.byOrdinal(tag.getByte("inserterDropLane"));
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
        if (this.level == null || this.level.getBlockEntity(this.worldPosition) != this) return false;

        return playerEntity.distanceToSqr(Vec3.atCenterOf(this.worldPosition)) <= MAX_INTERACTION_DISTANCE_SQR;
    }

    /** Portée d'interaction maximale, au carré. Aligné sur le contrôle des paquets C→S. */
    public static final double MAX_INTERACTION_DISTANCE_SQR = 64.0D;

    // Interface (Ticking)

    /**
     * Tick serveur uniquement : fait avancer la machine à états (cf. FIO-060).
     *
     * <p>Aucune émission réseau périodique. L'état visible ne part qu'aux <b>changements
     * d'état</b>, et les jauges du GUI par le {@code ContainerData} du menu — donc
     * uniquement vers les joueurs qui regardent l'écran. La version d'origine envoyait
     * 3 à 4 paquets par tick et par inserter <i>à tous les joueurs du serveur</i>
     * (cf. BUG-004).
     *
     * <p>Le préambule est réduit au strict minimum : le benchmark montre qu'il domine les
     * deux régimes, endormi comme actif, puisque le reste du tick se résume la plupart du
     * temps à comparer l'horloge à une échéance. {@code isEnabled()} lit un champ tenu à
     * jour par {@code setBlockState}, et la conversion du carburant est descendue dans le
     * seul état qui en a besoin.
     */
    public static void tick(Level pLevel, BlockPos pPos, BlockState pState, final InserterBlockEntity pEntity) {

        if (!pEntity.enabled) return;

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

        // Convertir un item en réserve n'a de sens qu'ici : c'est le seul état qui engage
        // une dépense. Les trois autres se contentaient de rappeler une méthode qui
        // repartait aussitôt.
        burnFuel();

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

        // Et saisir dans le même tick, comme la dépose enchaîne sur le retour : attendre le
        // tick suivant perdait un tick par cycle, soit un débit réel inférieur de
        // (2n + 1) / 2n au débit annoncé — jusqu'à ×1,5 sur un inserter rapide amélioré
        // (BUG-051).
        tickWaiting();
    }

    /**
     * Dépose la main dans l'inventaire cible et repart si elle est partie.
     *
     * @return {@code true} si la dépose a abouti
     */
    private boolean tryDrop() {
        // Le joueur a pu vider la main depuis l'écran : il n'y a plus rien à déposer, le bras
        // repart. Sans cela, il resterait tendu indéfiniment à tenter une dépose sans objet.
        if (this.itemStorage.getStackInSlot(BUFFER_SLOT).isEmpty()) {
            beginSwing(InserterState.RETURNING, ItemStack.EMPTY);
            return true;
        }

        ItemStack dropped = expelItems(this, getGrabDistance());
        if (dropped.isEmpty()) return false;

        this.itemsDelivered += dropped.getCount();

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
     * Angle de la tourelle à l'image courante, en degrés (FIO-066).
     *
     * <p>Point d'entrée unique du rendu : le modèle GeckoLib y lit la rotation du bone, et le
     * renderer d'item la position de la pince. Une seule grandeur, donc aucun moyen que les
     * deux se contredisent.
     */
    public float getTurretDegrees(float partialTick) {
        return InserterTurretPose.turretDegrees(this.state, getArmProgress(partialTick), this.animationMode);
    }

    /**
     * Pose du bras à l'image courante : les deux inclinaisons, résolues ensemble.
     *
     * <p>Elles sortent d'un même calcul de cinématique inverse et ne sont donc jamais
     * indépendantes. C'est ce qui rend la dislocation impossible — le défaut qu'avait la
     * première version, où chaque bone recevait un angle calculé de son côté.
     */
    public InserterArmKinematics.Pose getArmPose(float partialTick) {
        return InserterTurretPose.armPose(
                this.state, getArmProgress(partialTick), this.animationMode, getTicksPerSwing());
    }

    public InserterAnimationMode getAnimationMode() {
        return this.animationMode;
    }

    public void setAnimationMode(InserterAnimationMode mode) {
        if (this.animationMode == mode) return;

        this.animationMode = mode;
        syncToClients();
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

        // La capacité entière, pas la main réglée : le plafond du joueur porte sur ce que
        // l'inserter livre, pas sur ce qu'il prélève pour se nourrir.
        return grabInto(pEntity, source, pEntity.LAYOUT.fuel(), pEntity.getMaximumItemCountPerAction(),
                stack -> stack.is(ModTags.Items.INSERTER_FUEL), null);
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

    /**
     * Aligne la réserve sur la définition courante.
     *
     * <p>Un datapack peut changer capacité et débit à chaud (FIO-037), mais le
     * {@code EnergyContainer} d'un inserter déjà posé a été construit avec les anciennes
     * valeurs. Le menu, lui, affichait déjà la nouvelle capacité : la jauge se retrouvait
     * graduée sur une capacité que la machine n'avait pas.
     */
    private void syncEnergyLimitsFromDefinition() {
        if (!IS_ENERGY) return;

        this.energyStorage.overrideEnergyCapacity(inserter.getEnergyCapacity());
        this.energyStorage.overrideMaxTransfer(inserter.getEnergyTransferRate());
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

    /**
     * @return la condition qui s'applique réellement, module d'amélioration compris
     *
     * <p>Sans module de <b>redstone avancé</b>, la condition retombe sur
     * {@link RedstoneCondition#DEFAULT} : un signal coupe l'inserter, et rien de
     * plus. Le réglage fin — modes et seuil — est ce que le module déverrouille.
     *
     * <p>La condition réglée par le joueur est <b>conservée</b> pendant ce temps, et non
     * effacée : retirer puis reposer un module rend son réglage à l'inserter. Un état perdu
     * en silence est le genre de détail qui fait passer une amélioration pour un piège.
     * {@link #getConfiguredRedstoneCondition()} donne cette valeur mémorisée, dont l'écran a
     * besoin pour afficher ce que le joueur a choisi.
     */
    public RedstoneCondition getRedstoneCondition() {
        if (this.upgrades.unlocks(InserterUpgradeType.ADVANCED_REDSTONE, upgradeTuning())) {
            return this.redstoneCondition;
        }

        return RedstoneCondition.DEFAULT;
    }

    /** @return la condition telle que le joueur l'a réglée, module ou non */
    public RedstoneCondition getConfiguredRedstoneCondition() {
        return this.redstoneCondition;
    }

    /**
     * Change la condition et réévalue immédiatement l'état du bloc.
     *
     * <p>Sans cette réévaluation, un inserter resterait dans l'état décidé par l'ancienne
     * condition jusqu'au prochain changement de voisinage — c'est-à-dire, pour un signal
     * stable, indéfiniment.
     */
    public void setRedstoneCondition(RedstoneCondition condition) {
        if (this.redstoneCondition.equals(condition)) return;

        this.redstoneCondition = condition;
        wakeUp();
        syncToClients();
        reevaluateEnabled();
    }

    /** Recalcule la propriété {@code ENABLED} maintenant, sans attendre un changement de voisinage. */
    private void reevaluateEnabled() {
        if (this.level != null && !this.level.isClientSide) {
            getBlockState().getBlock().neighborChanged(
                    getBlockState(), this.level, this.worldPosition, this.level.getBlockState(this.worldPosition).getBlock(), this.worldPosition, false);
        }
    }

    // Interface (Interrupteur, FIO-167)

    public boolean isSwitchedOn() {
        return this.switchedOn;
    }

    public void setSwitchedOn(boolean on) {
        if (this.switchedOn == on) return;

        this.switchedOn = on;
        wakeUp();
        syncToClients();
        reevaluateEnabled();
    }

    /**
     * Lit la condition, en gardant le défaut historique pour un monde antérieur.
     *
     * <p>{@code contains} et non {@code getByte} : sans la clé, {@code getByte} rendrait 0,
     * soit {@code ALWAYS}, et tous les inserters d'un monde existant cesseraient de
     * répondre à la redstone.
     */
    private static RedstoneCondition readCondition(CompoundTag tag) {
        if (!tag.contains("inserterRedstoneMode")) return RedstoneCondition.DEFAULT;

        return new RedstoneCondition(
                RedstoneCondition.Mode.byOrdinal(tag.getByte("inserterRedstoneMode")),
                tag.getByte("inserterRedstoneThreshold"));
    }

    // Interface (Enabled)

    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * Tenu à jour plutôt que relu à chaque tick.
     *
     * <p>{@code setBlockState} est appelé par le chunk chaque fois que l'état du bloc
     * change, y compris au chargement : c'est le point d'accroche exact, et il évite une
     * lecture de propriété dans un préambule qui s'exécute pour chaque inserter du monde,
     * vingt fois par seconde.
     */
    @Override
    public void setBlockState(@NotNull BlockState state) {
        super.setBlockState(state);

        this.enabled = readEnabled(state);
    }

    private static boolean readEnabled(BlockState state) {
        return !state.hasProperty(BlockStateProperties.ENABLED)
                || state.getValue(BlockStateProperties.ENABLED);
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
        if (this.level == null) return null;

        BlockPos pos = this.worldPosition.relative(offset, pDistance);

        LazyOptional<IItemHandler> cached = source ? cachedSource : cachedTarget;
        BlockPos cachedPos = source ? cachedSourcePos : cachedTargetPos;

        // Un cache résolu ailleurs ne vaut rien : l'inserter a tourné, ou sa portée a
        // changé. La position fait donc partie de la clé.
        if (cached != null && cached.isPresent() && pos.equals(cachedPos)) return cached.orElse(null);

        BlockEntity neighbour = this.level.getBlockEntity(pos);
        if (neighbour == null) return null;

        LazyOptional<IItemHandler> capability = neighbour.getCapability(ForgeCapabilities.ITEM_HANDLER, side);
        if (!capability.isPresent()) return null;

        if (source) {
            cachedSource = capability;
            cachedSourcePos = pos;
            capability.addListener(ignored -> cachedSource = null);
        } else {
            cachedTarget = capability;
            cachedTargetPos = pos;
            capability.addListener(ignored -> cachedTarget = null);
        }

        return capability.orElse(null);
    }

    /** Oublie les inventaires voisins et relance l'inserter. */
    public void onNeighbourChanged() {
        this.cachedSource = null;
        this.cachedTarget = null;
        this.cachedSourcePos = null;
        this.cachedTargetPos = null;
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
     * Ce qu'un inventaire accepterait d'une pile, et par quel slot il commencerait.
     *
     * @param movable    nombre d'items réellement acceptés, tous slots confondus
     * @param firstSlot  premier slot ayant accepté quelque chose, à mémoriser pour le
     *                   prochain cycle
     */
    private record InsertPlan(int movable, int firstSlot) {}

    /**
     * Ordre dans lequel les slots de la cible sont sollicités : <b>les piles à compléter
     * d'abord, les slots libres ensuite</b>.
     *
     * <h3>Pourquoi cet ordre, et pas l'ordre positionnel</h3>
     *
     * <p>Un balayage positionnel dépose dans le premier slot qui accepte — et un slot
     * <b>vide</b> accepte toujours. Si le coffre a une case libre avant une pile entamée du
     * même item, l'inserter ouvre une nouvelle pile au lieu de compléter l'ancienne. Sur un
     * coffre où l'on prend et l'on dépose en même temps, des cases se libèrent devant les
     * piles en cours et le même item finit éparpillé sur une dizaine de piles partielles,
     * jusqu'à saturer le coffre en n'y rangeant presque rien.
     *
     * <p>C'est le comportement que tout inventaire correct évite, et la raison d'être de
     * {@code ItemHandlerHelper.insertItemStacked} — qu'on ne peut pas employer tel quel ici,
     * l'éjection ayant besoin de simuler avant d'extraire (cf. BUG-006) et de mémoriser le
     * premier slot preneur (DT-07).
     *
     * <h3>Pourquoi l'ordre est calculé une fois pour toutes</h3>
     *
     * <p>Il est établi <b>avant</b> toute écriture, puis partagé par la simulation et
     * l'insertion. Le recalculer entre les deux le ferait changer sous nos pieds : un slot
     * vide rempli par la passe des libres deviendrait une pile à compléter, donc appartiendrait
     * désormais à la première passe, et l'insertion ne suivrait plus la simulation — la
     * quantité extraite du buffer ne correspondrait plus à ce que la cible accepte, ce qui est
     * exactement la façon dont on détruit des items.
     */
    private static int[] insertionOrder(IItemHandler handler, @Nonnull ItemStack stack, int startSlot) {
        int slots = handler.getSlots();
        int[] order = new int[slots];
        int index = 0;

        for (int pass = 0; pass < 2; pass++) {
            boolean wantMergeable = pass == 0;

            for (int offset = 0; offset < slots; offset++) {
                int slot = Math.floorMod(startSlot + offset, slots);

                if (mergeableWith(handler, stack, slot) == wantMergeable) {
                    order[index++] = slot;
                }
            }
        }

        return order;
    }

    /**
     * Où commencer le balayage d'un inventaire voisin.
     *
     * <p>Le slot mémorisé sur les grands, le premier sur les petits — voir
     * {@link #MEMOISED_SCAN_THRESHOLD} pour la raison, qui n'est pas la performance.
     */
    private static int scanStart(int remembered, int slots) {
        if (slots < MEMOISED_SCAN_THRESHOLD) return 0;

        return Math.floorMod(remembered, Math.max(1, slots));
    }

    /** Ce slot porte-t-il déjà une pile que {@code stack} viendrait compléter ? */
    private static boolean mergeableWith(IItemHandler handler, @Nonnull ItemStack stack, int slot) {
        ItemStack current = handler.getStackInSlot(slot);

        return !current.isEmpty() && ItemHandlerHelper.canItemStacksStack(stack, current);
    }

    /**
     * Simule l'insertion et relève au passage le premier slot preneur.
     *
     * <p>Balaye dans l'ordre fourni, le même que {@link #insertDistributed}, sans quoi la
     * quantité simulée ne correspondrait pas à ce qui sera réellement inséré.
     *
     * <p>Le premier slot preneur sort d'ici plutôt que d'un troisième balayage : l'éjection
     * parcourait l'inventaire cible trois fois — simulation, insertion, puis recherche du
     * slot à mémoriser — pour une information que la première passe connaissait déjà.
     */
    private static InsertPlan planInsert(IItemHandler handler, @Nonnull ItemStack stack, int[] order, int startSlot) {
        int remaining = stack.getCount();
        int firstSlot = -1;

        for (int slot : order) {
            if (remaining <= 0) break;

            int before = remaining;

            remaining = handler.insertItem(slot, ItemHandlerHelper.copyStackWithSize(stack, remaining), true).getCount();

            if (remaining < before && firstSlot < 0) {
                firstSlot = slot;
            }
        }

        return new InsertPlan(stack.getCount() - remaining, firstSlot < 0 ? startSlot : firstSlot);
    }

    /** Insère en répartissant sur plusieurs slots. @return ce qui n'a pas pu être placé */
    @Nonnull
    private static ItemStack insertDistributed(IItemHandler handler, @Nonnull ItemStack stack, int[] order) {
        ItemStack remaining = stack;

        for (int slot : order) {
            if (remaining.isEmpty()) break;

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
     * <p><b>Prise « intelligente » (FIO-182).</b> Avec une {@code destination}, un slot n'est
     * retenu que pour ce qu'une insertion <i>simulée</i> y ferait entrer, et pas un item de
     * plus. Sans elle, devant une machine à plusieurs entrées, l'inserter saisissait un
     * ingrédient déjà plein et restait bloqué avec lui en main, sans jamais livrer les autres ;
     * et prendre plus que la destination n'accepte gardait le reliquat en main — la même
     * impasse, à retardement. Le coût est celui d'une simulation par slot candidat, au moment
     * de la prise seulement.
     *
     * @param destination ce que vise l'inserter devant lui, ou {@code null} : alors tout ce
     *                    que le filtre accepte peut être pris
     * @return la pile prélevée, vide si rien n'a bougé
     */
    @Nonnull
    private static ItemStack grabInto(
            InserterBlockEntity pEntity, IItemHandler source, int targetSlot, int wanted, Predicate<ItemStack> accept,
            @Nullable IItemHandler destination) {
        int slots = source.getSlots();

        int startSlot = scanStart(pEntity.lastSourceSlot, slots);

        for (int offset = 0; offset < slots; offset++) {
            // Balayage circulaire depuis le dernier slot fructueux : sur un grand coffre
            // dont seuls les derniers slots sont remplis, le coût devient constant en
            // régime établi au lieu d'être proportionnel à la taille (cf. DT-07).
            int slot = Math.floorMod(startSlot + offset, slots);

            ItemStack probe = source.extractItem(slot, wanted, true);
            if (probe.isEmpty() || !accept.test(probe)) continue;

            if (destination != null) {
                int fits = planInsert(destination, probe, insertionOrder(destination, probe, 0), 0).movable();
                if (fits <= 0) continue;
                if (fits < probe.getCount()) probe = probe.copyWithCount(fits);
            }

            int movable = pEntity.simulateInsertInternal(targetSlot, probe);
            if (movable <= 0) continue;

            ItemStack taken = source.extractItem(slot, movable, false);
            if (taken.isEmpty()) continue;

            pEntity.lastSourceSlot = slot;
            pEntity.rescueLeftover(
                    pEntity.insertItemInternal(targetSlot, taken, false),
                    // insertItemStacked, et non insertItem : rendre un reliquat à la source
                    // doit compléter la pile d'où il vient plutôt qu'en ouvrir une nouvelle.
                    rest -> ItemHandlerHelper.insertItemStacked(source, rest, false));
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

        // La destination, vue comme la verra le dépôt : même face, même voie de convoyeur.
        IItemHandler destination = pEntity.neighbourHandler(false, facing, pDistance, facing.getOpposite());
        if (destination != null) destination = pEntity.onChosenLane(destination);

        return grabInto(pEntity, source, BUFFER_SLOT, pEntity.getHandSize(),
                stack -> matchesFilters(pEntity, stack, isWhitelist), destination);
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

        target = pEntity.onChosenLane(target);

        int wanted = Math.min(buffer.getCount(), pEntity.getHandSize());

        ItemStack probe = pEntity.extractItemInternal(BUFFER_SLOT, wanted, true);
        if (probe.isEmpty()) return ItemStack.EMPTY;

        int startSlot = scanStart(pEntity.lastTargetSlot, target.getSlots());

        // Établi une seule fois, avant toute écriture, puis partagé par la simulation et
        // l'insertion : elles doivent solliciter les slots dans le même ordre, sinon la
        // quantité extraite du buffer ne correspond plus à ce que la cible accepte.
        int[] order = insertionOrder(target, probe, startSlot);

        // Le premier slot preneur est mémorisé, et non startSlot : réécrire le point de
        // départ avec lui-même rendait la mémorisation inopérante du côté cible, alors que
        // le côté source en bénéficiait bien (cf. DT-07, BUG-036).
        InsertPlan plan = planInsert(target, probe, order, startSlot);
        if (plan.movable() <= 0) return ItemStack.EMPTY;

        ItemStack taken = pEntity.extractItemInternal(BUFFER_SLOT, plan.movable(), false);
        if (taken.isEmpty()) return ItemStack.EMPTY;

        pEntity.lastTargetSlot = plan.firstSlot();
        pEntity.rescueLeftover(
                insertDistributed(target, taken, order),
                rest -> pEntity.insertItemInternal(BUFFER_SLOT, rest, false));
        return taken;
    }

    /**
     * Réduit un convoyeur à la voie choisie, s'il y en a une (FIO-169).
     *
     * <p>Le seul endroit où l'inserter sait qu'un convoyeur existe. Sans choix explicite, il
     * continue de ne rien en savoir : c'est la bande qui range ses cases, lointaine d'abord.
     */
    private IItemHandler onChosenLane(IItemHandler target) {
        if (this.dropLane == InserterDropLane.AUTO || !(target instanceof BeltItemHandler belt)) return target;

        IItemHandler lane = belt.restrictedTo(this.dropLane == InserterDropLane.FAR);

        return lane != null ? lane : target;
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
        // À l'ouverture, et non à chaque tick : c'est le seul moment où l'écart de
        // capacité laissé par un /reload devient visible.
        syncEnergyLimitsFromDefinition();

        return new InserterContainer(pContainerId, inserter, pPlayerInventory, level, getBlockPos());
    }
}
