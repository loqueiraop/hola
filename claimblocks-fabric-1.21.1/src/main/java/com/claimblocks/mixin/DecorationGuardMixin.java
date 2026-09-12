package com.claimblocks.mixin;

import com.claimblocks.ClaimBlocksMod;
import com.claimblocks.util.DecorationProtection;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Protege cuadros, marcos de items y soportes de armadura de flechas, mobs y explosiones. */
@Mixin({AbstractDecorationEntity.class, ItemFrameEntity.class, ArmorStandEntity.class})
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
         if (DecorationProtection.blocksDamage(self, source)) {
            cir.setReturnValue(false);
         }
      } catch (Throwable t) {
         ClaimBlocksMod.LOGGER.error("[Tierrasmon Claims] Fallo protegiendo una decoracion", t);
      }
   }
}
