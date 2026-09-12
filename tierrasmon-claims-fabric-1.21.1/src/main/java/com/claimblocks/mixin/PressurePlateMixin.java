package com.claimblocks.mixin;

import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import net.minecraft.block.AbstractPressurePlateBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Un intruso no activa las placas de presion de la zona. */
@Mixin({AbstractPressurePlateBlock.class})
public abstract class PressurePlateMixin {
   @Inject(
      method = {"updatePlateState"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void claimblocks$blockVisitorPlate(Entity entity, World world, BlockPos pos, BlockState state, int output, CallbackInfo ci) {
      if (!world.isClient() && entity instanceof ServerPlayerEntity player) {
         Claim claim = ClaimManager.getInstance().getClaimAt(world, pos);
         if (claim != null && !claim.getFlags().publicMode) {
            boolean bypassing = player.hasPermissionLevel(2) && ClaimManager.getInstance().isBypassing(player.getUuid());
            if (!claim.canModify(player) && !bypassing) {
               ci.cancel();
            }
         }
      }
   }
}
