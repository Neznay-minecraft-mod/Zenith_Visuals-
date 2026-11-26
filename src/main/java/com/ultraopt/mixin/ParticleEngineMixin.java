package com.ultraopt.mixin;

import com.ultraopt.Cfg;
import com.ultraopt.CullLogic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {
    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true, require = 0)
    private void ultraopt$createParticle(ParticleOptions options, double x, double y, double z,
                                         double vx, double vy, double vz,
                                         CallbackInfoReturnable<Particle> cir) {
        if (!Cfg.PARTICLE_CULL.b) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        double dx = x - mc.player.getX();
        double dy = y - mc.player.getY();
        double dz = z - mc.player.getZ();
        double d2 = dx * dx + dy * dy + dz * dz;
        double lim = Cfg.PARTICLE_DIST.i;
        if (d2 > lim * lim) {
            CullLogic.partCulled++;
            cir.setReturnValue(null);
            return;
        }
        int thin = Cfg.PARTICLE_THIN.i;
        if (thin > 0 && d2 > 144 && CullLogic.rnd(100) < thin) {
            CullLogic.partCulled++;
            cir.setReturnValue(null);
        }
    }
}
