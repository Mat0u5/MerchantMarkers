package com.anthonyhilyard.merchantmarkers.mixin;

import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.List;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.MultiPackResourceManager;

@Mixin(MultiPackResourceManager.class)
public class MultiPackResourceManagerMixin
{
	@WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/util/List;copyOf(Ljava/util/Collection;)Ljava/util/List;"))
	private List<PackResources> mutablePacks(Collection<? extends PackResources> packs, Operation<List<PackResources>> original)
	{
		return Lists.newArrayList(packs);
	}
}