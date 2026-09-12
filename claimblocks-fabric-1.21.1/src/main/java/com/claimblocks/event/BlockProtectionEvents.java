package com.claimblocks.event;

import com.claimblocks.ClaimBlocks;
import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimConfig;
import com.claimblocks.data.ClaimManager;
import com.claimblocks.data.ClaimTier;
import com.claimblocks.gui.ClaimMenuHandler;
import java.util.Locale;
import java.util.Set;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.Before;
import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.AbstractSignBlock;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.CropBlock;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.block.NetherWartBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BucketItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.world.World;

public final class BlockProtectionEvents {
   private static int fireSweepCounter = 0;
   private static final Set<String> CONTAINER_KEYWORDS = Set.of(
      "backpack",
      "bag",
      "satchel",
      "pouch",
      "drawer",
      "crate",
      "container",
      "trunk",
      "shulker",
      "barrel",
      "chest",
      "vault",
      "hopper",
      "dispenser",
      "dropper",
      "furnace",
      "smoker",
      "blast",
      "storage",
      "tank",
      "silo",
      "lootr"
   );
   private static final Set<String> CONTAINER_NAMESPACES = Set.of(
      "sophisticatedbackpacks",
      "sophisticatedstorage",
      "sophisticatedcore",
      "travelersbackpack",
      "simplybackpacks",
      "iron_backpacks",
      "ironchests",
      "expandedstorage",
      "functionalstorage",
      "storagedrawers",
      "lootr",
      "metalbarrels",
      "tieredshulkers",
      "tiered_shulkers"
   );

   public static void register() {
      registerBreakEvents();
      registerUseBlockEvent();
      registerItemUseEvent();
   }

   private static boolean isBypassing(PlayerEntity player) {
      return player.hasPermissionLevel(2) && ClaimManager.getInstance().isBypassing(player.getUuid());
   }

   private static boolean denyForVisitor(Claim claim, PlayerEntity player, boolean specificFlag) {
      if (claim.canModify(player)) {
         return false;
      } else if (isBypassing(player)) {
         return false;
      } else {
         return claim.getFlags().publicMode ? true : specificFlag;
      }
   }

   private static void deny(PlayerEntity player, String msg) {
      if (player instanceof ServerPlayerEntity sp) {
         sp.sendMessage(Text.literal(msg).formatted(Formatting.RED), true);
      }
   }

   private static void registerBreakEvents() {
      PlayerBlockBreakEvents.BEFORE.register((Before)(world, player, pos, state, be) -> {
         if (world.isClient) {
            return true;
         } else if (isBypassing(player)) {
            return true;
         } else {
            Claim centerClaim = ClaimManager.getInstance().getClaimByCenter(world, pos);
            if (centerClaim != null) {
               ClaimTier tier = centerClaim.getTier();
               if (tier != null && ClaimBlocks.isClaimConcreteForTier(state.getBlock(), tier)) {
                  if (!centerClaim.isOwner(player) && !player.hasPermissionLevel(2)) {
                     deny(player, "[!] Solo el dueño puede romper esta piedra.");
                     return false;
                  }

                  ClaimManager.getInstance().removeClaim(world, pos);
                  // Quitamos el bloque nosotros y cancelamos la rotura vanilla: si dejamos que
                  // vanilla la haga, suelta el concreto en vez de la piedra de claim.
                  world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
                  ItemStack stack = ClaimBlocks.createTierItem(tier, 1);
                  if (!player.getInventory().insertStack(stack)) {
                     player.dropItem(stack, false);
                  }

                  world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 1.0F, 1.0F);
                  if (player instanceof ServerPlayerEntity sp) {
                     sp.sendMessage(Text.literal("✔ Zona eliminada. Piedra devuelta a tu inventario.").formatted(Formatting.GREEN), false);
                  }

                  return false;
               }
            }

            Claim claim = ClaimManager.getInstance().getClaimAt(world, pos);
            if (claim == null) {
               return true;
            } else if (claim.canModify(player)) {
               return true;
            } else if (!state.isIn(BlockTags.LOGS) || !claim.getFlags().publicMode && !claim.getFlags().blockTreeChopping) {
               if (!isMatureCrop(state) || !claim.getFlags().publicMode && !claim.getFlags().blockCropHarvest) {
                  if (denyForVisitor(claim, player, claim.getFlags().blockBreaking)) {
                     deny(player, "[!] No puedes romper bloques aquí.");
                     return false;
                  } else {
                     return true;
                  }
               } else {
                  deny(player, "[!] No puedes cosechar cultivos aquí.");
                  return false;
               }
            } else {
               deny(player, "[!] No puedes talar árboles en esta zona.");
               return false;
            }
         }
      });
      AttackBlockCallback.EVENT.register((AttackBlockCallback)(player, world, hand, pos, dir) -> {
         if (world.isClient) {
            return ActionResult.PASS;
         } else if (isBypassing(player)) {
            return ActionResult.PASS;
         } else {
            Claim centerClaim = ClaimManager.getInstance().getClaimByCenter(world, pos);
            if (centerClaim != null) {
               ClaimTier tier = centerClaim.getTier();
               BlockState state = world.getBlockState(pos);
               if (tier != null && ClaimBlocks.isClaimConcreteForTier(state.getBlock(), tier)) {
                  if (!centerClaim.isOwner(player) && !player.hasPermissionLevel(2)) {
                     return ActionResult.FAIL;
                  }

                  return ActionResult.PASS;
               }
            }

            Claim claim = ClaimManager.getInstance().getClaimAt(world, pos);
            if (claim != null && !claim.canModify(player)) {
               BlockState state = world.getBlockState(pos);
               if (!state.isIn(BlockTags.LOGS) || !claim.getFlags().publicMode && !claim.getFlags().blockTreeChopping) {
                  if (!isMatureCrop(state) || !claim.getFlags().publicMode && !claim.getFlags().blockCropHarvest) {
                     return denyForVisitor(claim, player, claim.getFlags().blockBreaking) ? ActionResult.FAIL : ActionResult.PASS;
                  } else {
                     return ActionResult.FAIL;
                  }
               } else {
                  return ActionResult.FAIL;
               }
            } else {
               return ActionResult.PASS;
            }
         }
      });
   }

   private static void registerUseBlockEvent() {
      UseBlockCallback.EVENT
         .register(
            (UseBlockCallback)(player, world, hand, hit) -> {
               if (world.isClient) {
                  return ActionResult.PASS;
               } else {
                  BlockPos pos = hit.getBlockPos();
                  ItemStack stack = player.getStackInHand(hand);
                  Claim centerClaim = ClaimManager.getInstance().getClaimByCenter(world, pos);
                  if (centerClaim != null) {
                     ClaimTier tier = centerClaim.getTier();
                     BlockState clickedState = world.getBlockState(pos);
                     if (tier != null && ClaimBlocks.isClaimConcreteForTier(clickedState.getBlock(), tier) && !player.isSneaking()) {
                        if (!centerClaim.isOwner(player) && !player.hasPermissionLevel(2)) {
                           deny(player, "[x] Solo el dueño puede administrar esta zona.");
                           return ActionResult.CONSUME;
                        }

                        if (player instanceof ServerPlayerEntity sp) {
                           ClaimMenuHandler.open(sp, centerClaim, 0);
                        }

                        return ActionResult.CONSUME;
                     }
                  }

                  ClaimTier itemTier = ClaimBlocks.readTier(stack);
                  return itemTier != null && !isBypassing(player)
                     ? tryPlaceClaim(player, world, hand, hit, stack, itemTier)
                     : regularUseBlockChecks(player, world, hand, hit, stack);
               }
            }
         );
   }

   private static ActionResult tryPlaceClaim(PlayerEntity player, World world, Hand hand, BlockHitResult hit, ItemStack stack, ClaimTier tier) {
      BlockPos clicked = hit.getBlockPos();
      BlockState clickedState = world.getBlockState(clicked);
      BlockPos placeAt;
      if (clickedState.isReplaceable()) {
         placeAt = clicked;
      } else {
         placeAt = clicked.offset(hit.getSide());
      }

      BlockState atState = world.getBlockState(placeAt);
      if (!atState.isAir() && !atState.isReplaceable()) {
         return ActionResult.PASS;
      } else {
         Claim ownerOfPlace = ClaimManager.getInstance().getClaimAt(world, placeAt);
         if (ownerOfPlace != null && !ownerOfPlace.canModify(player) && !player.hasPermissionLevel(2)) {
            deny(player, "[x] No puedes construir en esta zona.");
            return ActionResult.CONSUME;
         } else {
            ClaimManager mgr = ClaimManager.getInstance();
            if (mgr.wouldOverlap(world, placeAt, tier.radius, tier.height)) {
               deny(player, "[x] Esta zona se solaparía con otra existente.");
               return ActionResult.CONSUME;
            } else {
               int max = ClaimManager.getMaxClaimsPerPlayer();
               if (max > 0 && !player.hasPermissionLevel(2)) {
                  int owned = mgr.getClaimsOf(player.getUuid()).size();
                  if (owned >= max) {
                     deny(player, "[x] Has alcanzado el límite de zonas (" + max + ").");
                     return ActionResult.CONSUME;
                  }
               }

               Block block = ClaimBlocks.blockForTier(tier);
               world.setBlockState(placeAt, block.getDefaultState());
               world.playSound(null, placeAt, SoundEvents.BLOCK_AMETHYST_BLOCK_PLACE, SoundCategory.BLOCKS, 0.8F, 1.2F);
               Claim created = mgr.createClaim(world, placeAt, player, tier);
               if (!player.getAbilities().creativeMode) {
                  stack.decrement(1);
               }

               player.swingHand(hand);
               if (player instanceof ServerPlayerEntity sp) {
                  sp.sendMessage(
                     Text.literal("✔ Zona creada: ")
                        .formatted(new Formatting[]{Formatting.GREEN, Formatting.BOLD})
                        .append(Text.literal(tier.label()).formatted(new Formatting[]{Formatting.YELLOW, Formatting.BOLD}))
                        .append(Text.literal(" bloques | Altura: +/-" + tier.height).formatted(Formatting.GRAY)),
                     false
                  );
               }

               return ActionResult.CONSUME;
            }
         }
      }
   }

   private static ActionResult regularUseBlockChecks(PlayerEntity player, World world, Hand hand, BlockHitResult hit, ItemStack stack) {
      if (isBypassing(player)) {
         return ActionResult.PASS;
      } else {
         BlockPos pos = hit.getBlockPos();
         BlockPos placeAt = pos.offset(hit.getSide());
         BlockState clickedState = world.getBlockState(pos);
         Block clickedBlock = clickedState.getBlock();

         Claim here = ClaimManager.getInstance().getClaimAt(world, pos);
         if (here != null && !here.canModify(player)) {
            if (here.getFlags().blockAllInteractions) {
               deny(player, "[!] No tienes ningún permiso de interacción en esta zona.");
               return ActionResult.FAIL;
            }

            if (here.getFlags().blockDoorsAccess && isDoorLike(clickedState)) {
               deny(player, "[!] No puedes usar puertas, botones ni placas aquí.");
               return ActionResult.FAIL;
            }
         }

         Claim cc;
         if (!isContainer(world, pos)
            || (cc = ClaimManager.getInstance().getClaimAt(world, pos)) == null
            || cc.canModify(player)
            || !cc.getFlags().publicMode && !cc.getFlags().blockChestAccess) {
            Claim claim;
            if (!(clickedBlock instanceof AnvilBlock)
               || (claim = ClaimManager.getInstance().getClaimAt(world, pos)) == null
               || claim.canModify(player)
               || !claim.getFlags().publicMode && !claim.getFlags().blockAnvilUse) {
               if (!(clickedBlock instanceof AbstractSignBlock)
                  || (claim = ClaimManager.getInstance().getClaimAt(world, pos)) == null
                  || claim.canModify(player)
                  || !claim.getFlags().publicMode && !claim.getFlags().blockSignEditing) {
                  if (!(stack.getItem() instanceof BucketItem)
                     || (claim = ClaimManager.getInstance().getClaimAt(world, placeAt)) == null
                     || claim.canModify(player)
                     || !claim.getFlags().publicMode && !claim.getFlags().blockFluids && !claim.getFlags().blockBuilding) {
                     if (stack.getItem() instanceof BlockItem) {
                        BlockPos finalPos = clickedState.canReplace(new ItemPlacementContext(player, hand, stack, hit)) ? pos : placeAt;
                        Claim claim2 = ClaimManager.getInstance().getClaimAt(world, finalPos);
                        if (claim2 != null && denyForVisitor(claim2, player, claim2.getFlags().blockBuilding)) {
                           deny(player, "[!] No puedes construir aquí.");
                           return ActionResult.FAIL;
                        }
                     }

                     if (isInteractiveBlock(clickedState)
                        && (claim = ClaimManager.getInstance().getClaimAt(world, pos)) != null
                        && denyForVisitor(claim, player, claim.getFlags().blockBuilding)) {
                        deny(player, "[!] No puedes interactuar aquí.");
                        return ActionResult.FAIL;
                     } else {
                        return ActionResult.PASS;
                     }
                  } else {
                     deny(player, "[!] No puedes colocar fluidos aquí.");
                     return ActionResult.FAIL;
                  }
               } else {
                  deny(player, "[!] No puedes editar letreros aquí.");
                  return ActionResult.FAIL;
               }
            } else {
               deny(player, "[!] No puedes usar yunques aquí.");
               return ActionResult.FAIL;
            }
         } else {
            deny(player, "[!] No puedes abrir contenedores aquí.");
            return ActionResult.FAIL;
         }
      }
   }

   private static void registerItemUseEvent() {
      UseItemCallback.EVENT.register((UseItemCallback)(player, world, hand) -> {
         ItemStack stack = player.getStackInHand(hand);
         if (world.isClient) {
            return TypedActionResult.pass(stack);
         } else if (isBypassing(player)) {
            return TypedActionResult.pass(stack);
         } else if (ClaimBlocks.readTierId(stack) != null) {
            return TypedActionResult.pass(stack);
         } else {
            Claim claim = ClaimManager.getInstance().getClaimAt(world, player.getBlockPos());
            if (claim == null || claim.canModify(player)) {
               return TypedActionResult.pass(stack);
            } else if (!claim.getFlags().publicMode && !claim.getFlags().blockItemUse) {
               return TypedActionResult.pass(stack);
            } else {
               deny(player, "[!] No puedes usar items en esta zona.");
               return TypedActionResult.fail(stack);
            }
         }
      });
   }

   public static boolean isContainer(World world, BlockPos pos) {
      BlockState state = world.getBlockState(pos);
      Block b = state.getBlock();
      if (!(b instanceof ChestBlock)
         && !(b instanceof EnderChestBlock)
         && !(b instanceof BarrelBlock)
         && !(b instanceof ShulkerBoxBlock)
         && !(b instanceof AbstractFurnaceBlock)) {
         BlockEntity be = world.getBlockEntity(pos);
         if (be instanceof Inventory) {
            return true;
         } else {
            Identifier id = Registries.BLOCK.getId(b);
            if (id != null) {
               String ns = id.getNamespace().toLowerCase(Locale.ROOT);
               String path = id.getPath().toLowerCase(Locale.ROOT);
               if (CONTAINER_NAMESPACES.contains(ns)) {
                  return true;
               }

               for (String kw : CONTAINER_KEYWORDS) {
                  if (path.contains(kw) || ns.contains(kw)) {
                     return true;
                  }
               }
            }

            return false;
         }
      } else {
         return true;
      }
   }

   private static boolean isMatureCrop(BlockState state) {
      Block b = state.getBlock();
      if (b instanceof CropBlock) {
         if (state.contains(Properties.AGE_7)) {
            return (Integer)state.get(Properties.AGE_7) >= 7;
         }

         if (state.contains(Properties.AGE_3)) {
            return (Integer)state.get(Properties.AGE_3) >= 3;
         }
      }

      if (b instanceof NetherWartBlock) {
         return (Integer)state.get(NetherWartBlock.AGE) >= 3;
      } else {
         return state.contains(Properties.AGE_7) && state.get(Properties.AGE_7) >= 7
            ? true
            : state.contains(Properties.AGE_3) && (Integer)state.get(Properties.AGE_3) >= 3;
      }
   }

   private static boolean isDoorLike(BlockState state) {
      if (state.isIn(BlockTags.DOORS)) {
         return true;
      } else if (state.isIn(BlockTags.TRAPDOORS)) {
         return true;
      } else if (state.isIn(BlockTags.FENCE_GATES)) {
         return true;
      } else if (state.isIn(BlockTags.BUTTONS)) {
         return true;
      } else if (state.isIn(BlockTags.PRESSURE_PLATES)) {
         return true;
      } else {
         return state.getBlock() == Blocks.LEVER;
      }
   }

   private static boolean isInteractiveBlock(BlockState state) {
      if (state.isIn(BlockTags.TRAPDOORS)) {
         return true;
      } else if (state.isIn(BlockTags.SLABS)) {
         return true;
      } else if (state.isIn(BlockTags.IMPERMEABLE)) {
         return true;
      } else if (state.isIn(BlockTags.FENCES)) {
         return true;
      } else if (state.isIn(BlockTags.WOODEN_PRESSURE_PLATES)) {
         return true;
      } else if (state.isIn(BlockTags.BUTTONS)) {
         return true;
      } else {
         Block b = state.getBlock();
         if (b == Blocks.LEVER) {
            return true;
         } else if (b == Blocks.NOTE_BLOCK) {
            return true;
         } else if (b == Blocks.JUKEBOX) {
            return true;
         } else {
            return b == Blocks.LECTERN ? true : b == Blocks.CAKE;
         }
      }
   }

   public static void tickFireSweep(MinecraftServer server) {
      if (++fireSweepCounter % ClaimConfig.get().fireSweepIntervalTicks == 0) {
         for (ServerWorld world : server.getWorlds()) {
            for (Claim claim : ClaimManager.getInstance().getClaimsInWorld(world.getRegistryKey().getValue().toString())) {
               if (claim.getFlags().blockFire || claim.getFlags().publicMode) {
                  for (ServerPlayerEntity p : world.getPlayers()) {
                     if (claim.contains(p.getBlockPos())) {
                        extinguishAround(world, p.getBlockPos(), claim);
                     }
                  }
               }
            }
         }
      }
   }

   private static void extinguishAround(ServerWorld world, BlockPos centre, Claim claim) {
      int r = ClaimConfig.get().fireSweepRadius;
      Mutable m = new Mutable();

      for (int dx = -r; dx <= r; dx++) {
         for (int dy = -r; dy <= r; dy++) {
            for (int dz = -r; dz <= r; dz++) {
               m.set(centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
               if (claim.contains(m)) {
                  BlockState bs = world.getBlockState(m);
                  if (bs.getBlock() == Blocks.FIRE || bs.getBlock() == Blocks.SOUL_FIRE) {
                     world.setBlockState(m.toImmutable(), Blocks.AIR.getDefaultState(), 3);
                  }
               }
            }
         }
      }
   }
}
