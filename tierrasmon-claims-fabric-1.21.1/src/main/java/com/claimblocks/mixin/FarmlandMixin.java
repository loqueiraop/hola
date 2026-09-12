package com.claimblocks.mixin;

import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({FarmlandBlock.class})
public abstract class FarmlandMixin {
   @Inject(
      method = {"onLandedUpon"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void claimblocks$cancelTrampling(World world, BlockState state, BlockPos pos, Entity entity, float fallDistance, CallbackInfo ci) {
      if (world != null && !world.isClient) {
         if (entity instanceof PlayerEntity player) {
            Claim c = ClaimManager.getInstance().getClaimAt(world, pos);
            if (c != null) {
               if (!c.canModify(player)) {
                  if (c.getFlags().publicMode || c.getFlags().blockTrampling) {
                     ci.cancel();
                  }
               }
            }
         }
      }
   }
}
