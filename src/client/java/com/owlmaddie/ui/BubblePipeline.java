package com.owlmaddie.ui;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.LogicOp;
import com.mojang.blaze3d.platform.PolygonMode;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Optional;

public class BubblePipeline {
    public static RenderPipeline BUBBLE_PIPELINE =
        RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
                .withLocation(Identifier.of("creaturechat", "pipelines/bubble"))
                .withBlend(BlendFunction.TRANSLUCENT)
                .withDepthTestFunction(DepthTestFunction.EQUAL_DEPTH_TEST)
                .withCull(false)
                .withVertexFormat(VertexFormats.POSITION_COLOR_TEXTURE_LIGHT, VertexFormat.DrawMode.QUADS)
                .build();

    public static RenderLayer BUBBLE_LAYER = RenderLayer.of(
            "bubble",
            4194304,
            BUBBLE_PIPELINE,
            RenderLayer.MultiPhaseParameters.builder().build(true)
    );


    public static void register() {
        RenderPipelines.register(BUBBLE_PIPELINE);
    }
}
