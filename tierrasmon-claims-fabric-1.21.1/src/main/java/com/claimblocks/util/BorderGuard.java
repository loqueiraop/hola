package com.claimblocks.util;

import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimConfig;
import com.claimblocks.data.ClaimFlags;
import com.claimblocks.data.ClaimManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Protege el borde de la zona frente a tolvas y fluidos que vienen de fuera. */
public final class BorderGuard {
   private BorderGuard() {
   }

   private static boolean sameZone(Claim a, Claim b) {
      if (a != null && b != null) {
         return a.getClaimId().equals(b.getClaimId()) || a.getGroupId() != null && a.getGroupId().equals(b.getGroupId());
      } else {
         return false;
      }
   }

   /** true si hay que impedir que se saquen items de {@code from} hacia {@code to}. */
   public static boolean blocksItemExtraction(World world, BlockPos from, BlockPos to) {
      if (world == null || world.isClient() || from == null || to == null) {
         return false;
      } else if (!ClaimConfig.get().protectHoppers) {
         return false;
      } else {
         ClaimManager mgr = ClaimManager.getInstance();
         Claim source = mgr.getClaimAt(world, from);
         if (source == null) {
            return false;
         } else {
            Claim dest = mgr.getClaimAt(world, to);
            if (sameZone(source, dest)) {
               return false;
            } else {
               ClaimFlags f = source.getFlags();
               return f.blockChestAccess || f.publicMode;
            }
         }
      }
   }

   /** true si hay que impedir que un fluido entre desde {@code from} hacia {@code to}. */
   public static boolean blocksFluidEntry(World world, BlockPos from, BlockPos to) {
      if (world == null || world.isClient() || from == null || to == null) {
         return false;
      } else if (!ClaimConfig.get().protectFluids) {
         return false;
      } else {
         ClaimManager mgr = ClaimManager.getInstance();
         Claim dest = mgr.getClaimAt(world, to);
         if (dest == null) {
            return false;
         } else {
            Claim source = mgr.getClaimAt(world, from);
            if (sameZone(dest, source)) {
               return false;
            } else {
               ClaimFlags f = dest.getFlags();
               return f.blockFluids || f.publicMode;
            }
         }
      }
   }
}
