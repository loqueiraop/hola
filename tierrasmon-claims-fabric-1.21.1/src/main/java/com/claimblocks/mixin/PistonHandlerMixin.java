package com.claimblocks.mixin;

import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import java.util.List;
import net.minecraft.block.piston.PistonHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({PistonHandler.class})
public abstract class PistonHandlerMixin {
   @Shadow
   @Final
   private World world;
   @Shadow
   @Final
   private BlockPos posFrom;
   @Shadow
   @Final
   private Direction motionDirection;
   @Shadow
   @Final
   private List<BlockPos> movedBlocks;

   @Inject(
      method = {"calculatePush"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void claimblocks$blockCrossClaimPiston(CallbackInfoReturnable<Boolean> cir) {
      if (Boolean.TRUE.equals(cir.getReturnValue())) {
         if (this.world != null && !this.world.isClient) {
            if (this.movedBlocks != null && !this.movedBlocks.isEmpty()) {
               ClaimManager mgr = ClaimManager.getInstance();
               Claim pistonClaim = mgr.getClaimAt(this.world, this.posFrom);

               for (BlockPos origin : this.movedBlocks) {
                  BlockPos dest = origin.offset(this.motionDirection);
                  Claim originClaim = mgr.getClaimAt(this.world, origin);
                  Claim destClaim = mgr.getClaimAt(this.world, dest);
                  if ((!sameClaimRef(originClaim, destClaim) || !sameClaimRef(pistonClaim, originClaim))
                     && (protectsBuilding(originClaim) || protectsBuilding(destClaim) || protectsBuilding(pistonClaim))) {
                     cir.setReturnValue(false);
                     return;
                  }
               }
            }
         }
      }
   }

   private static boolean sameClaimRef(Claim a, Claim b) {
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
