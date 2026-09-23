package com.anthonyhilyard.merchantmarkers.mixin;

import com.anthonyhilyard.merchantmarkers.compat.FTBChunksHandler;
import com.anthonyhilyard.merchantmarkers.render.Markers;
import com.anthonyhilyard.merchantmarkers.config.MerchantMarkersConfig;

import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftblibrary.icon.EntityIconLoader;
import dev.ftb.mods.ftblibrary.icon.Icon;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;


@Mixin(FTBChunksClient.class)
public class FTBChunksClientMixin
{
	@Redirect(method = "onMapIconEvent", at = @At(value = "INVOKE", target = "Ldev/ftb/mods/ftblibrary/icon/EntityIconLoader;getIcon(Lnet/minecraft/world/entity/Entity;)Ldev/ftb/mods/ftblibrary/icon/Icon;"), require = 0)
	public Icon<?> redirectGetIcon(Entity entity)
	{
		// If this is a villager, return the dynamic texture instead of the default one.
		if (MerchantMarkersConfig.getInstance().showOnMiniMap.get() && Markers.shouldShowMarker(entity))
		{
			return Icon.getIcon(FTBChunksHandler.villagerTexture);
		}

		return EntityIconLoader.getIcon(entity);
	}
}
