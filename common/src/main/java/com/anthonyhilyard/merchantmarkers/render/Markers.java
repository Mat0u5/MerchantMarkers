package com.anthonyhilyard.merchantmarkers.render;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import com.anthonyhilyard.merchantmarkers.MerchantMarkers;
import com.anthonyhilyard.merchantmarkers.config.MerchantMarkersConfig;
import com.anthonyhilyard.merchantmarkers.config.MerchantMarkersConfig.OverlayType;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.level.block.state.BlockState;

public class Markers
{
	public static record MarkerResource(Identifier texture, OverlayType overlay, int level) {}

	public static record MarkerRenderData(MarkerResource resource, float entityHeight, int y, float alpha) {}
	public static final Identifier MARKER_ARROW = Identifier.fromNamespaceAndPath(MerchantMarkers.MODID, "textures/entity/villager/arrow.png");
	public static final Identifier ICON_OVERLAY = Identifier.fromNamespaceAndPath(MerchantMarkers.MODID, "textures/entity/villager/overlay.png");
	public static final Identifier NUMBER_OVERLAY = Identifier.fromNamespaceAndPath(MerchantMarkers.MODID, "textures/entity/villager/numbers.png");
	public static final Identifier DEFAULT_ICON = Identifier.fromNamespaceAndPath(MerchantMarkers.MODID, "textures/entity/villager/default.png");
	public static final Identifier EMPTY_MARKER = Identifier.fromNamespaceAndPath(MerchantMarkers.MODID, "textures/entity/villager/empty.png");

	private static Supplier<InputStream> emptyMarkerResource = null;

	private static Map<String, MarkerResource> resourceCache = new HashMap<>();

	public static InputStream getEmptyInputStream()
	{
		if (emptyMarkerResource == null)
		{
			emptyMarkerResource = () -> {
				final Minecraft minecraft = Minecraft.getInstance();
				final ResourceManager manager = minecraft.getResourceManager();
				try
				{
					return manager.getResource(Markers.EMPTY_MARKER).get().open();
				}
				catch (Exception e)
				{
					// Don't do anything, maybe the resource pack just isn't ready yet.
					return InputStream.nullInputStream();
				}
			};
		}
		return emptyMarkerResource.get();
	}

	public static String getProfessionName(Entity entity)
	{
		String iconName = "";
		if (entity instanceof Villager)
		{
			// If the profession name contains any colons, replace them with double underscores.
			iconName = ((Villager)entity).getVillagerData().profession().unwrapKey().map(key -> {
				Identifier id = key.identifier();
				return Identifier.DEFAULT_NAMESPACE.equals(id.getNamespace()) ? id.getPath() : id.getNamespace() + "__" + id.getPath();
			}).orElse("");
		}
		else if (entity instanceof WanderingTrader)
		{
			iconName = "wandering_trader";
		}
		else if (entity instanceof Merchant)
		{
			iconName = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
		}
		else
		{
			iconName = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();

			// Check if there is a marker with this profession name.
			Minecraft minecraft = Minecraft.getInstance();
			ResourceManager manager = minecraft.getResourceManager();
			if (!manager.getResource(Identifier.fromNamespaceAndPath(MerchantMarkers.MODID, "textures/entity/villager/markers/" + iconName + ".png")).isPresent())
			{
				// This isn't a valid profession name, so return a blank string.
				iconName = "";
			}
		}
		return iconName;
	}

	public static int getProfessionLevel(Entity entity)
	{
		int level = 0;
		if (MerchantMarkersConfig.getInstance().showLevels() && entity instanceof Villager)
		{
			level = ((Villager)entity).getVillagerData().level();
		}
		return level;
	}

	public static MarkerRenderData extractMarker(EntityRenderDispatcher dispatcher, Entity entity)
	{
		if (!Markers.shouldShowMarker(entity))
		{
			return null;
		}

		Minecraft minecraft = Minecraft.getInstance();
		String profession = getProfessionName(entity);
		int level = getProfessionLevel(entity);

		double squareDistance = dispatcher.distanceToSqr(entity);
		double maxDistance = MerchantMarkersConfig.getInstance().maxDistance.get();

		// If this entity is too far away, don't render the markers.
		if (squareDistance > maxDistance * maxDistance)
		{
			return null;
		}

		double fadePercent = MerchantMarkersConfig.getInstance().fadePercent.get();
		float currentAlpha = 1.0f;

		// We won't do any calculations if fadePercent is 100, since that would make a division by zero.
		if (fadePercent < 100.0)
		{
			// Calculate the distance at which fading begins.
			double startFade = ((1.0 - (fadePercent / 100.0)) * maxDistance);

			// Calculate the current alpha value for this marker.
			currentAlpha = (float)Mth.clamp(1.0 - ((Math.sqrt(squareDistance) - startFade) / (maxDistance - startFade)), 0.0, 1.0);

			// Multiply in the configured opacity value.
			currentAlpha *= MerchantMarkersConfig.getInstance().opacity.get();
		}

		float entityHeight = entity.getBbHeight() + 0.5F;
		int y = "deadmau5".equals(entity.getDisplayName().getString()) ? -28 : -18;
		y -= MerchantMarkersConfig.getInstance().verticalOffset.get();

		return new MarkerRenderData(getMarkerResource(minecraft, profession, level), entityHeight, y, currentAlpha);
	}

	public static void submitMarker(MarkerRenderData marker, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraRenderState)
	{
		poseStack.pushPose();
		poseStack.translate(0.0D, (double)marker.entityHeight(), 0.0D);
		poseStack.mulPose(cameraRenderState.orientation);
		poseStack.scale(0.025F, -0.025F, 0.025F);

		boolean showArrow = MerchantMarkersConfig.getInstance().showArrow.get();
		int y = marker.y();

		if (MerchantMarkersConfig.getInstance().showThroughWalls.get())
		{
			extractMarker(collector, true, marker.resource(), poseStack, -8, showArrow ? y - 9 : y, 0.3f * marker.alpha());

			if (showArrow)
			{
				renderArrow(collector, true, poseStack, 0, y, 0.3f * marker.alpha());
			}
		}

		extractMarker(collector, false, marker.resource(), poseStack, -8, showArrow ? y - 9 : y, marker.alpha());

		if (showArrow)
		{
			renderArrow(collector, false, poseStack, 0, y, marker.alpha());
		}

		poseStack.popPose();
	}

	public static void clearResourceCache()
	{
		resourceCache.clear();
	}

	public static boolean shouldShowMarker(Entity entity)
	{
		// TODO: Cache this?
		String professionName = getProfessionName(entity);
		if (professionName == "" || entity.isInvisible() ||
			(entity instanceof LivingEntity livingEntity && (livingEntity.isBaby() || livingEntity.isDeadOrDying())) ||
			MerchantMarkersConfig.getInstance().professionBlacklist.get().contains(professionName))
		{
			return false;
		}

		return true;
	}

	public static MarkerResource getMarkerResource(Minecraft minecraft, String professionName, int level)
	{
		if (professionName == "")
		{
			return null;
		}

		String resourceKey = String.format("%s-%d", professionName, level);

		// Returned the cached value, if there is one.
		if (resourceCache.containsKey(resourceKey))
		{
			return resourceCache.get(resourceKey);
		}

		MarkerResource result = null;
		OverlayType overlayType = OverlayType.fromValue(MerchantMarkersConfig.getInstance().overlayIndex.get()).orElse(OverlayType.NONE);

		switch (MerchantMarkersConfig.MarkerType.fromText(MerchantMarkersConfig.getInstance().markerType.get()).get())
		{
			case ITEMS:
			{
				Identifier associatedItemKey = MerchantMarkersConfig.getInstance().getAssociatedItem(professionName);
				if (associatedItemKey != null && BuiltInRegistries.ITEM.containsKey(associatedItemKey))
				{
					Item associatedItem = BuiltInRegistries.ITEM.getOptional(associatedItemKey).get();

					ItemStackRenderState itemRenderState = new ItemStackRenderState();
					minecraft.getItemModelResolver().updateForTopItem(itemRenderState, new ItemStack(associatedItem), ItemDisplayContext.GUI, minecraft.level, minecraft.player, 0);

					TextureAtlasSprite sprite = itemRenderState.pickParticleIcon(RandomSource.create());
					if (sprite != null)
					{
						result = new MarkerResource(getSpriteTexture(sprite), overlayType, level);
					}
				}
				break;
			}
			case JOBS:
			{
				// If the entity is a villager, find the (first) job block for their profession.
				Identifier professionId = Identifier.tryParse(professionName.replace("__", ":"));
				Optional<Holder.Reference<VillagerProfession>> profession = professionId == null ? Optional.empty() : BuiltInRegistries.VILLAGER_PROFESSION.get(professionId);
				if (profession.isPresent() && !profession.get().is(VillagerProfession.NONE))
				{
					List<BlockState> jobBlockStates = BuiltInRegistries.POINT_OF_INTEREST_TYPE.registryKeySet().stream()
						.map(key -> BuiltInRegistries.POINT_OF_INTEREST_TYPE.get(key).get())
						.filter(poiType -> profession.get().value().acquirableJobSite().test(poiType))
						.<BlockState>flatMap(poiType -> poiType.unwrap().right().get().matchingStates().stream()).distinct().toList();

					if (!jobBlockStates.isEmpty())
					{
						TextureAtlasSprite sprite = minecraft.getBlockRenderer().getBlockModel(jobBlockStates.iterator().next()).particleIcon();
						result = new MarkerResource(getSpriteTexture(sprite), overlayType, level);
					}
				}
				break;
			}
			case CUSTOM:
			default:
			{
				// Check if the given resource exists, otherwise use the default icon.
				Identifier iconResource = Identifier.fromNamespaceAndPath(MerchantMarkers.MODID, String.format("textures/entity/villager/markers/%s.png", professionName));
				if (minecraft.getResourceManager().getResource(iconResource).isPresent())
				{
					result = new MarkerResource(iconResource, overlayType, level);
				}
				break;
			}
			// Render the generic icon by falling through.
			case GENERIC:
			break;
		}

		if (result == null)
		{
			// If we got this far, we were missing something so just render the default icon.
			result = new MarkerResource(DEFAULT_ICON, overlayType, level);
		}

		// Cache the result.
		resourceCache.put(resourceKey, result);
		return result;
	}

	private static Identifier getSpriteTexture(TextureAtlasSprite sprite)
	{
		Identifier spriteName = sprite.contents().name();
		return Identifier.fromNamespaceAndPath(spriteName.getNamespace(), String.format("textures/%s.png", spriteName.getPath()));
	}

	// Lower order renders first.
	private static final int ICON_ORDER = 0;
	private static final int OVERLAY_ORDER = 1;

	private static void extractMarker(SubmitNodeCollector collector, boolean seeThrough, MarkerResource resource, PoseStack poseStack, int x, int y, float alpha)
	{
		float scale = (float)(double)MerchantMarkersConfig.getInstance().iconScale.get();
		poseStack.pushPose();
		poseStack.scale(scale, scale, 1.0f);
		renderIcon(collector, ICON_ORDER, seeThrough, resource.texture(), poseStack, x, y, alpha);
		renderOverlay(resource, (dx, dy, width, height, sx, sy) -> {
			poseStack.translate(0, 0, 1);
			float imageSize = resource.overlay() == OverlayType.LEVEL ? 32.0f : 16.0f;
			renderIcon(collector, OVERLAY_ORDER, seeThrough, resource.overlay() == OverlayType.LEVEL ? NUMBER_OVERLAY : ICON_OVERLAY, poseStack, x + dx, y + dy, width, height, sx / imageSize, (sx + width) / imageSize, sy / imageSize, (sy + height) / imageSize, alpha);
		});
		poseStack.popPose();
	}

	private static void renderArrow(SubmitNodeCollector collector, boolean seeThrough, PoseStack poseStack, int x, int y, float alpha)
	{
		float scale = (float)(double)MerchantMarkersConfig.getInstance().iconScale.get();
		poseStack.pushPose();
		poseStack.scale(scale, scale, 1.0f);
		renderIcon(collector, ICON_ORDER, seeThrough, MARKER_ARROW, poseStack, x - 8, y + 8, 16, 8, 0, 1, 0, 1, alpha);
		poseStack.popPose();
	}

	@FunctionalInterface
	public interface OverlayRendererMethod { void accept(int dx, int dy, int width, int height, int sx, int sy); }

	public static void renderOverlay(MarkerResource resource, OverlayRendererMethod method)
	{
		if (resource.overlay() == OverlayType.LEVEL)
		{
			renderOverlayLevel(resource, method);
		}
		else if (resource.overlay != OverlayType.NONE)
		{
			renderOverlayIcon(resource, method);
		}
	}

	private static void renderOverlayLevel(MarkerResource resource, OverlayRendererMethod method)
	{
		int processedDigits = resource.level();
		int xOffset = 8;

		// If the overlay is set to "profession level" and this marker has a level to show, add every digit needed.
		// Even though vanilla only supports a max level of 5, this should support any profession level.
		while (processedDigits > 0)
		{
			int currentDigit = processedDigits % 10;
			method.accept(xOffset, 8, 8, 8, (currentDigit % 4) * 8, (currentDigit / 4) * 8);
			processedDigits /= 10;
			xOffset -= 5;
		}
	}

	private static void renderOverlayIcon(MarkerResource resource, OverlayRendererMethod method)
	{
		method.accept(8, 8, 8, 8, (resource.overlay().value() % 2) * 8, (resource.overlay().value() / 2) * 8);
	}

	private static void renderIcon(SubmitNodeCollector collector, int order, boolean seeThrough, Identifier icon, PoseStack poseStack, int x, int y, float alpha)
	{
		renderIcon(collector, order, seeThrough, icon, poseStack, x, y, 16, 16, 0, 1, 0, 1, alpha);
	}

	private static void renderIcon(SubmitNodeCollector collector, int order, boolean seeThrough, Identifier icon, PoseStack poseStack, int x, int y, int w, int h, float u0, float u1, float v0, float v1, float alpha)
	{
		RenderType renderType = seeThrough ? RenderTypes.textSeeThrough(icon) : RenderTypes.text(icon);
		final int light = LightTexture.FULL_BRIGHT;

		collector.order(order).submitCustomGeometry(poseStack, renderType, (pose, vertexConsumer) -> {
			vertexConsumer.addVertex(pose, (float)x,		(float)(y + h),	0).setUv(u0, v1).setColor(1.0f, 1.0f, 1.0f, alpha).setLight(light);
			vertexConsumer.addVertex(pose, (float)(x + w),	(float)(y + h),	0).setUv(u1, v1).setColor(1.0f, 1.0f, 1.0f, alpha).setLight(light);
			vertexConsumer.addVertex(pose, (float)(x + w),	(float)y,		0).setUv(u1, v0).setColor(1.0f, 1.0f, 1.0f, alpha).setLight(light);
			vertexConsumer.addVertex(pose, (float)x,		(float)y,		0).setUv(u0, v0).setColor(1.0f, 1.0f, 1.0f, alpha).setLight(light);
		});
	}
}
