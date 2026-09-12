package com.claimblocks.mixin;

import com.claimblocks.ClaimBlocksMod;
import com.claimblocks.util.DecorationProtection;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Protege cuadros, marcos de items y soportes de armadura de flechas, mobs y explosiones.
 *
 * Se engancha a Entity.damage y no a las clases de decoracion porque ni
 * AbstractDecorationEntity ni PaintingEntity declaran damage: lo heredan de Entity,
 * asi que inyectar en ellas dejaria los cuadros sin proteger. El filtro es un
 * instanceof, asi que no pesa para el resto de entidades.
 */
@Mixin({Entity.class})
public abstract class DecorationGuardMixin {
   @Inject(
      method = {"damage(Lnet/minecraft/entity/damage/DamageSource;F)Z"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void claimblocks$protectDecoration(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
      try {
         Entity self = (Entity)(Object)this;
         if (DecorationProtection.isDecoration(self) && DecorationProtection.blocksDamage(self, source)) {
            cir.setReturnValue(false);
         }
      } catch (Throwable t) {
         ClaimBlocksMod.LOGGER.error("[Tierrasmon Claims] Fallo protegiendo una decoracion", t);
      }
   }
}
