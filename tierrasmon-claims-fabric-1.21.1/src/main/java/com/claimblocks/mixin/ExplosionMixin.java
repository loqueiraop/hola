package com.claimblocks.mixin;

import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Explosion.class})
public abstract class ExplosionMixin {
   @Shadow
   @Final
   private World world;
   @Shadow
   @Final
   private ObjectArrayList<BlockPos> affectedBlocks;

   @Inject(
      method = {"collectBlocksAndDamageEntities"},
      at = {@At("RETURN")}
   )
   private void claimblocks$filterAffectedBlocks(CallbackInfo ci) {
      if (this.world != null && !this.world.isClient) {
         if (this.affectedBlocks != null && !this.affectedBlocks.isEmpty()) {
            this.affectedBlocks.removeIf(pos -> {
               Claim c = ClaimManager.getInstance().getClaimAt(this.world, pos);
               return c != null && c.getFlags().blockExplosions;
            });
         }
      }
   }
}
