package com.claimblocks.mixin;

import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({EnderPearlEntity.class})
public abstract class EnderPearlMixin {
   @Inject(
      method = {"onCollision"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void claimblocks$blockTeleport(HitResult hit, CallbackInfo ci) {
      EnderPearlEntity self = (EnderPearlEntity)(Object)this;
      World world = self.getWorld();
      if (world != null && !world.isClient) {
         if (self.getOwner() instanceof PlayerEntity player) {
            if (!player.hasPermissionLevel(2) || !ClaimManager.getInstance().isBypassing(player.getUuid())) {
               Claim c = ClaimManager.getInstance().getClaimAt(world, self.getBlockPos());
               if (c != null) {
                  if (!c.canModify(player)) {
                     if (c.getFlags().publicMode || c.getFlags().blockEnderPearl) {
                        self.discard();
                        ci.cancel();
                     }
                  }
               }
            }
         }
      }
   }
}
