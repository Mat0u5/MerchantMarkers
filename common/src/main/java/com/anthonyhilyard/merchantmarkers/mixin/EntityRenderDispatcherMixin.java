package com.anthonyhilyard.merchantmarkers.mixin;

import com.anthonyhilyard.merchantmarkers.MerchantMarkers;
import com.anthonyhilyard.merchantmarkers.config.MerchantMarkersConfig;
import com.anthonyhilyard.merchantmarkers.render.IMarkerHolder;
import com.anthonyhilyard.merchantmarkers.render.Markers;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin
{
	@Inject(method = "extractEntity", at = @At("RETURN"))
	private <E extends Entity> void saveRenderDataMarker(E entity, float partialTick, CallbackInfoReturnable<EntityRenderState> info)
	{
		Markers.MarkerRenderData marker = null;
		if (MerchantMarkers.showMarkers.isDown() || MerchantMarkersConfig.getInstance().alwaysShow.get())
		{
			marker = Markers.extractMarker((EntityRenderDispatcher)(Object)this, entity);
		}
		((IMarkerHolder)info.getReturnValue()).setMerchantMarkersRenderData(marker);
	}

	@Inject(method = "submit", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V"))
	private <S extends EntityRenderState> void submitMarker(S renderState, CameraRenderState cameraRenderState, double x, double y, double z, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CallbackInfo info)
	{
		Markers.MarkerRenderData marker = ((IMarkerHolder)renderState).getMerchantMarkersRenderData();
		if (marker != null)
		{
			Markers.submitMarker(marker, poseStack, submitNodeCollector, cameraRenderState);
		}
	}
}
