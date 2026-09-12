package com.claimblocks.mixin;

import com.claimblocks.ClaimBlocksMod;
import com.claimblocks.util.ExplosionGuard;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.explosion.EntityExplosionBehavior;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Igual que ExplosionBehaviorGuardMixin pero para explosiones con entidad asociada (creepers, TNT). */
@Mixin({EntityExplosionBehavior.class})
public abstract class EntityExplosionBehaviorGuardMixin {
   @Inject(
      method = {
         "canDestroyBlock(Lnet/minecraft/world/explosion/Explosion;Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;F)Z"
      },
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void claimblocks$keepClaimedBlocks(
      Explosion explosion, BlockView view, BlockPos pos, BlockState state, float power, CallbackInfoReturnable<Boolean> cir
   ) {
      try {
         if (ExplosionGuard.protects(view, pos)) {
            cir.setReturnValue(false);
         }
      } catch (Throwable t) {
         ClaimBlocksMod.LOGGER.error("[ClaimBlocks] Fallo protegiendo bloques de una explosion", t);
      }
   }
}
