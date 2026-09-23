package com.anthonyhilyard.merchantmarkers.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.anthonyhilyard.merchantmarkers.render.IMarkerHolder;
import com.anthonyhilyard.merchantmarkers.render.Markers.MarkerRenderData;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

@Mixin(EntityRenderState.class)
public class EntityRenderStateMixin implements IMarkerHolder
{
	@Unique
	private MarkerRenderData merchantMarkerRenderData = null;

	@Override
	@Unique
	public MarkerRenderData getMerchantMarkersRenderData()
	{
		return merchantMarkerRenderData;
	}

	@Override
	@Unique
	public void setMerchantMarkersRenderData(MarkerRenderData marker)
	{
		merchantMarkerRenderData = marker;
	}
}
