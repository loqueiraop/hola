package com.claimblocks.mixin;

import com.claimblocks.event.EntityProtectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Un mob que muere por la barrera de hostiles de una zona no suelta loot ni experiencia,
 * para que la barrera no se pueda usar como granja automatica.
 */
@Mixin({LivingEntity.class})
public abstract class BarrierDropsMixin {
   @Inject(
      method = {"dropLoot(Lnet/minecraft/entity/damage/DamageSource;Z)V"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void claimblocks$noBarrierLoot(DamageSource source, boolean causedByPlayer, CallbackInfo ci) {
      LivingEntity self = (LivingEntity)(Object)this;
      if (source == null || !(source.getAttacker() instanceof PlayerEntity)) {
         if (EntityProtectionEvents.killedByBarrier(self)) {
            ci.cancel();
         }
      }
   }

   @Inject(
      method = {"dropXp(Lnet/minecraft/entity/Entity;)V"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void claimblocks$noBarrierXp(Entity attacker, CallbackInfo ci) {
      LivingEntity self = (LivingEntity)(Object)this;
      if (!(attacker instanceof PlayerEntity) && EntityProtectionEvents.killedByBarrier(self)) {
         ci.cancel();
      }
   }
}
