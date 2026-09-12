package com.claimblocks.mixin;

import com.claimblocks.ClaimBlocksMod;
import com.claimblocks.util.BorderGuard;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Impide que agua y lava entren en la zona cruzando el borde desde fuera. */
@Mixin({FlowableFluid.class})
public abstract class FluidGuardMixin {
   @Inject(
      method = {
         "canFlow(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/Direction;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/fluid/FluidState;Lnet/minecraft/fluid/Fluid;)Z"
      },
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void claimblocks$blockFluidEntering(
      BlockView view,
      BlockPos fromPos,
      BlockState fromState,
      Direction direction,
      BlockPos toPos,
      BlockState toState,
      FluidState fluidState,
      Fluid fluid,
      CallbackInfoReturnable<Boolean> cir
   ) {
      try {
         if (!(view instanceof World world)) {
            return;
         }

         if (BorderGuard.blocksFluidEntry(world, fromPos, toPos)) {
            cir.setReturnValue(false);
         }
      } catch (Throwable t) {
         ClaimBlocksMod.LOGGER.error("[ClaimBlocks] Fallo controlando un fluido", t);
      }
   }
}
