package com.paranoidhighlighter.mixin;

import com.paranoidhighlighter.glow.GlowTracker;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {
	@Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
	private void paranoidHighlighter$teamColor(CallbackInfoReturnable<Integer> cir) {
		Entity self = (Entity) (Object) this;
		if (GlowTracker.shouldGlow(self)) {
			cir.setReturnValue(GlowTracker.getColor(self));
		}
	}
}
