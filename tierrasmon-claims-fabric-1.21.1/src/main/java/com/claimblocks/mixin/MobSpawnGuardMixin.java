package com.claimblocks.mixin;

import com.claimblocks.ClaimBlocksMod;
import com.claimblocks.event.EntityProtectionEvents;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Bloquea el spawn de mobs dentro de una zona segun las flags MOB_SPAWN,
 * ALL_MOB_SPAWN y PASSIVE_MOB_SPAWN. Los spawns provocados por un jugador
 * (huevos, cubos, cria, comandos, dispensadores, conversiones) no se tocan.
 */
@Mixin({MobEntity.class})
public abstract class MobSpawnGuardMixin {
   @Inject(
      method = {"canSpawn(Lnet/minecraft/world/WorldAccess;Lnet/minecraft/entity/SpawnReason;)Z"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void claimblocks$blockClaimedSpawn(WorldAccess worldAccess, SpawnReason reason, CallbackInfoReturnable<Boolean> cir) {
      try {
         MobEntity self = (MobEntity)(Object)this;
         if (worldAccess instanceof World world && !world.isClient()) {
            if (EntityProtectionEvents.shouldBlockSpawn(world, self.getBlockPos(), self, reason)) {
               cir.setReturnValue(false);
            }
         }
      } catch (Throwable t) {
         ClaimBlocksMod.LOGGER.error("[Tierrasmon Claims] Fallo controlando el spawn de un mob", t);
      }
   }
}
