package com.owlmaddie.ui;

import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.EntityRenderState;

public abstract class BubbleRendererContext<S extends EntityRenderState, M extends EntityModel<? super S>> implements FeatureRendererContext<S, M> {
}
