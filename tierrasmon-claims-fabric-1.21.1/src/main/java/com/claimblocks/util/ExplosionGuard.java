package com.claimblocks.util;

import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/** Decide si un bloque concreto debe sobrevivir a una explosion por estar en una zona protegida. */
public final class ExplosionGuard {
   private ExplosionGuard() {
   }

   public static boolean protects(BlockView view, BlockPos pos) {
      if (pos != null && view instanceof World world) {
         if (world.isClient()) {
            return false;
         } else {
            Claim c = ClaimManager.getInstance().getClaimAt(world, pos);
            return c != null && (c.getFlags().blockExplosions || c.getFlags().publicMode);
         }
      } else {
         return false;
      }
   }
}
