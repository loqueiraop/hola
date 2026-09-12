package com.claimblocks.mixin;

import com.claimblocks.ClaimBlocksMod;
import com.claimblocks.util.BorderGuard;
import java.util.function.BooleanSupplier;
import net.minecraft.block.BlockState;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.entity.Hopper;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Impide que una tolva saque items de una zona protegida desde fuera del borde. */
@Mixin({HopperBlockEntity.class})
public abstract class HopperGuardMixin {
   @Inject(
      method = {"extract(Lnet/minecraft/world/World;Lnet/minecraft/block/entity/Hopper;)Z"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private static void claimblocks$blockHopperSuck(World world, Hopper hopper, CallbackInfoReturnable<Boolean> cir) {
      try {
         if (world == null || hopper == null) {
            return;
         }

         BlockPos hopperPos = BlockPos.ofFloored(hopper.getHopperX(), hopper.getHopperY(), hopper.getHopperZ());
         BlockPos above = hopperPos.offset(Direction.UP);
         if (BorderGuard.blocksItemExtraction(world, above, hopperPos)) {
            cir.setReturnValue(false);
         }
      } catch (Throwable t) {
         ClaimBlocksMod.LOGGER.error("[Tierrasmon Claims] Fallo controlando una tolva (absorcion)", t);
      }
   }

   @Inject(
      method = {
         "insertAndExtract(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/block/entity/HopperBlockEntity;Ljava/util/function/BooleanSupplier;)Z"
      },
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private static void claimblocks$blockHopperPush(
      World world, BlockPos pos, BlockState state, HopperBlockEntity blockEntity, BooleanSupplier booleanSupplier, CallbackInfoReturnable<Boolean> cir
   ) {
      try {
         if (world == null || pos == null || state == null) {
            return;
         }

         Direction facing = (Direction)state.get(HopperBlock.FACING);
         if (facing == null) {
            return;
         }

         if (BorderGuard.blocksItemExtraction(world, pos, pos.offset(facing))) {
            cir.setReturnValue(false);
         }
      } catch (Throwable t) {
         ClaimBlocksMod.LOGGER.error("[Tierrasmon Claims] Fallo controlando una tolva (empuje)", t);
      }
   }
}
