package com.drimoz.factoryio.content.crafter;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

/**
 * Un fluide demandé par une recette (FIO-179) : un fluide précis ou un tag, et une quantité en
 * millibuckets. Exactement l'un des deux est renseigné.
 */
public record FluidInput(@Nullable Fluid fluid, @Nullable TagKey<Fluid> tag, int amount) {

    public FluidInput {
        if ((fluid == null) == (tag == null)) throw new IllegalArgumentException("Un fluide ou un tag, pas les deux");
    }

    public boolean test(FluidStack stack) {
        if (stack.isEmpty()) return false;
        return this.fluid != null ? stack.getFluid().isSame(this.fluid) : stack.getFluid().is(this.tag);
    }

    /**
     * Le fluide à montrer, et à mettre dans un réservoir vide : le fluide précis, ou la première
     * <b>source</b> du tag — l'eau, pas l'eau qui coule.
     */
    public Fluid representative() {
        if (this.fluid != null) return this.fluid;

        var tags = ForgeRegistries.FLUIDS.tags();
        if (tags == null) return Fluids.EMPTY;
        Fluid fallback = Fluids.EMPTY;
        for (Fluid candidate : tags.getTag(this.tag)) {
            if (candidate.isSource(candidate.defaultFluidState())) return candidate;
            if (fallback == Fluids.EMPTY) fallback = candidate;
        }
        return fallback;
    }

    public void toNetwork(FriendlyByteBuf buf) {
        buf.writeBoolean(this.tag != null);
        if (this.tag != null) buf.writeResourceLocation(this.tag.location());
        else buf.writeResourceLocation(ForgeRegistries.FLUIDS.getKey(this.fluid));
        buf.writeVarInt(this.amount);
    }

    public static FluidInput fromNetwork(FriendlyByteBuf buf) {
        boolean isTag = buf.readBoolean();
        ResourceLocation id = buf.readResourceLocation();
        int amount = buf.readVarInt();
        return isTag
                ? new FluidInput(null, TagKey.create(ForgeRegistries.Keys.FLUIDS, id), amount)
                : new FluidInput(ForgeRegistries.FLUIDS.getValue(id), null, amount);
    }
}
