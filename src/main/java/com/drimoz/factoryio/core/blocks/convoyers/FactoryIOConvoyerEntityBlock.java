package com.drimoz.factoryio.core.blocks.convoyers;


import com.drimoz.factoryio.core.blocks.FactoryIOWaterLoggedEntityBlock;

public abstract class FactoryIOConvoyerEntityBlock extends FactoryIOWaterLoggedEntityBlock {
    protected FactoryIOConvoyerEntityBlock(Properties pProperties) {
        super(pProperties, true);
    }
}
