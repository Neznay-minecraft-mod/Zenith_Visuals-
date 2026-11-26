package com.ultraopt.mixin;

import com.ultraopt.CullLogic;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true, require = 0)
    private void ultraopt$shouldRender(Entity entity, Frustum frustum, double camX, double camY, double camZ,
                                       CallbackInfoReturnable<Boolean> cir) {
        if (!CullLogic.visible(entity, camX, camY, camZ)) {
            cir.setReturnValue(false);
        }
    }
}
