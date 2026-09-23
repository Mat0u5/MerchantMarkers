package com.anthonyhilyard.merchantmarkers.mixin;

import com.anthonyhilyard.merchantmarkers.compat.FTBChunksHandler;
import com.anthonyhilyard.merchantmarkers.render.Markers;
import com.anthonyhilyard.merchantmarkers.config.MerchantMarkersConfig;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import dev.ftb.mods.ftbchunks.client.mapicon.EntityMapIcon;
import dev.ftb.mods.ftbchunks.client.minimap.layers.IconLayerRenderer;
import dev.ftb.mods.ftbchunks.api.client.icon.MapType;
import dev.ftb.mods.ftbchunks.api.client.icon.MapIcon;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;

@Mixin(IconLayerRenderer.class)
public class FTBChunksIconLayerRendererMixin
{
	@Redirect(method = "renderLayer", at = @At(value = "INVOKE", target = "Ldev/ftb/mods/ftbchunks/api/client/icon/MapIcon;draw(Ldev/ftb/mods/ftbchunks/api/client/icon/MapType;Lnet/minecraft/client/gui/GuiGraphics;IIIIZI)V", remap = false), remap = false, require = 0)
	public void redirectIconDraw(MapIcon icon, MapType mapType, GuiGraphics graphics, int x, int y, int w, int h, boolean outsideVisibleArea, int iconAlpha)
	{
		if (icon instanceof EntityMapIcon entityIcon)
		{
			Entity entity = FTBChunksHandler.getEntityFromIcon(entityIcon);

			// If this is a villager, return the dynamic texture instead of the default one.
			if (MerchantMarkersConfig.getInstance().showOnMiniMap.get() && Markers.shouldShowMarker(entity))
			{
				FTBChunksHandler.setCurrentEntity(entity);
			}
		}

		icon.draw(mapType, graphics, x, y, w, h, outsideVisibleArea, iconAlpha);
	}
}
