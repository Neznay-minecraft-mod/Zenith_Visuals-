package com.ultraopt.mixin;

import com.ultraopt.CullLogic;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true, require = 0)
    private void ultraopt$tickNonPassenger(Entity entity, CallbackInfo ci) {
        if (CullLogic.skipTick(entity)) {
            ci.cancel();
        }
    }
}
