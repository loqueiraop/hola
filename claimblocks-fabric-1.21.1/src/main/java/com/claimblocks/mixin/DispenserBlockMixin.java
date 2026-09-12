package com.claimblocks.mixin;

import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.DispenserBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({DispenserBlock.class})
public abstract class DispenserBlockMixin {
   @Inject(
      method = {"dispense"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void claimblocks$blockCrossClaimDispense(ServerWorld world, BlockState state, BlockPos pos, CallbackInfo ci) {
      if (world != null && !world.isClient) {
         Direction facing;
         try {
            facing = (Direction)state.get(Properties.FACING);
         } catch (Exception var10) {
            return;
         }

         BlockPos target = pos.offset(facing);
         ClaimManager mgr = ClaimManager.getInstance();
         Claim selfClaim = mgr.getClaimAt(world, pos);
         Claim targetClaim = mgr.getClaimAt(world, target);
         if (!sameClaim(selfClaim, targetClaim)) {
            if (protectsBuilding(targetClaim) || protectsBuilding(selfClaim)) {
               ci.cancel();
            }
         }
      }
   }

   private static boolean sameClaim(Claim a, Claim b) {
      if (a == null && b == null) {
         return true;
      } else {
         return a != null && b != null ? a.getClaimId().equals(b.getClaimId()) : false;
      }
   }

   private static boolean protectsBuilding(Claim c) {
      return c == null ? false : c.getFlags().publicMode || c.getFlags().blockBuilding;
   }
}
