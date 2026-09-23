package com.anthonyhilyard.merchantmarkers.mixin;

import com.anthonyhilyard.merchantmarkers.compat.FTBChunksHandler;
import com.anthonyhilyard.merchantmarkers.render.Markers;
import com.anthonyhilyard.merchantmarkers.config.MerchantMarkersConfig;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftblibrary.icon.Icon;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;


@Mixin(FTBChunksClient.class)
public class FTBChunksClientMixin
{
	@WrapOperation(method = "onMapIconEvent", at = @At(value = "INVOKE", target = "Ldev/ftb/mods/ftblibrary/icon/EntityIconLoader;getIcon(Lnet/minecraft/world/entity/Entity;)Ldev/ftb/mods/ftblibrary/icon/Icon;"), require = 0)
	public Icon<?> redirectGetIcon(Entity entity, Operation<Icon<?>> original)
	{
		// If this is a villager, return the dynamic texture instead of the default one.
		if (MerchantMarkersConfig.getInstance().showOnMiniMap.get() && Markers.shouldShowMarker(entity))
		{
			return Icon.getIcon(FTBChunksHandler.villagerTexture);
		}

		return original.call(entity);
	}
}
