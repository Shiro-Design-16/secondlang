package com.shiro.secondlang.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.shiro.secondlang.LanguageStacks;
import com.shiro.secondlang.SecondLangConfig;
import java.util.List;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LanguageManager.class)
public class MixinLanguageManager {
	@WrapOperation(method = "onResourceManagerReload(Lnet/minecraft/server/packs/resources/ResourceManager;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/language/ClientLanguage;loadFrom(Lnet/minecraft/server/packs/resources/ResourceManager;Ljava/util/List;Z)Lnet/minecraft/client/resources/language/ClientLanguage;"))
	private static ClientLanguage secondlang$applyFallbackChain(ResourceManager resourceManager, List<String> languages, boolean rightToLeft, Operation<ClientLanguage> original) {
		String selected = languages.isEmpty() ? SecondLangConfig.DEFAULT_LANGUAGE : languages.get(languages.size() - 1);
		List<String> stack = LanguageStacks.build(resourceManager, languages, selected);
		if (!stack.equals(languages)) {
			SecondLangConfig.LOGGER.info("[secondlang] selected={} stack={}", selected, stack);
		}
		return original.call(resourceManager, stack, rightToLeft);
	}
}
