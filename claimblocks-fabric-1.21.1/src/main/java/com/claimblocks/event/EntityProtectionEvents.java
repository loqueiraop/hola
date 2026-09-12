package com.claimblocks.event;

import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimConfig;
import com.claimblocks.data.ClaimFlags;
import com.claimblocks.data.ClaimManager;
import com.claimblocks.data.GlobalFlags;
import com.claimblocks.mixin.EntityInvulnerabilityAccessor;
import com.claimblocks.render.ParticleBorder;
import com.claimblocks.util.DecorationProtection;
import java.util.List;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AllowDamage;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.Load;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.AmbientEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.entity.passive.AbstractDonkeyEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.SnowGolemEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.ChestBoatEntity;
import net.minecraft.entity.vehicle.StorageMinecartEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

public final class EntityProtectionEvents {
   private static final String BARRIER_TAG = "claimblocks_barrier";
   private static int barrierCounter = 0;

   public static void register() {
      registerMobSpawnFallback();
      registerDamageGuards();
      registerInteractionGuard();
   }

   private static boolean isBypassing(PlayerEntity player) {
      return player.hasPermissionLevel(2) && ClaimManager.getInstance().isBypassing(player.getUuid());
   }

   // ---------------------------------------------------------------- spawns

   private static boolean isPlayerDrivenSpawn(SpawnReason reason) {
      if (reason == null) {
         return false;
      } else {
         return switch (reason) {
            case SPAWN_EGG, BUCKET, BREEDING, COMMAND, DISPENSER, CONVERSION -> true;
            default -> false;
         };
      }
   }

   private static boolean isPassiveAnimal(MobEntity mob) {
      return mob instanceof AnimalEntity || mob instanceof WaterCreatureEntity || mob instanceof AmbientEntity;
   }

   /** Mobs que "pertenecen" a un jugador: con nombre, domesticados, crias o golems construidos. */
   private static boolean isPlayerOwnedMob(MobEntity mob) {
      if (mob.hasCustomName() || mob.isPersistent()) {
         return true;
      } else if (mob instanceof TameableEntity tameable && tameable.isTamed()) {
         return true;
      } else if (mob instanceof IronGolemEntity || mob instanceof SnowGolemEntity) {
         return true;
      } else {
         return mob instanceof PassiveEntity passive && passive.isBaby();
      }
   }

   /** Llamado desde MobSpawnGuardMixin. */
   public static boolean shouldBlockSpawn(World world, BlockPos pos, MobEntity mob, SpawnReason reason) {
      if (isPlayerDrivenSpawn(reason) || isPlayerOwnedMob(mob)) {
         return false;
      } else if (GlobalFlags.getInstance().globalNoMobSpawn) {
         return true;
      } else {
         Claim claim = ClaimManager.getInstance().getClaimAt(world, pos);
         if (claim == null) {
            return false;
         } else {
            ClaimFlags f = claim.getFlags();
            if (f.blockAllMobSpawn) {
               return true;
            } else if (f.blockPassiveMobSpawn && isPassiveAnimal(mob)) {
               return true;
            } else {
               return mob instanceof Monster && (f.blockMobSpawn || f.publicMode);
            }
         }
      }
   }

   /** Red de seguridad: si algo se cuela por otra via, se descarta al aparecer. */
   private static void registerMobSpawnFallback() {
      ServerEntityEvents.ENTITY_LOAD.register((Load)(entity, world) -> {
         if (entity instanceof MobEntity mob && entity.age == 0) {
            if (shouldBlockSpawn(world, entity.getBlockPos(), mob, SpawnReason.NATURAL)) {
               entity.discard();
            }
         }
      });
   }

   // ------------------------------------------------------- barrera hostiles

   /** Empuja, quema y dana a los mobs hostiles que entran en una zona con BURN_HOSTILES. */
   public static void tickHostileBarrier(MinecraftServer server) {
      if (++barrierCounter % 5 == 0) {
         for (ServerWorld world : server.getWorlds()) {
            String dim = world.getRegistryKey().getValue().toString();

            for (Claim claim : ClaimManager.getInstance().getClaimsInWorld(dim)) {
               if (claim.getFlags().burnHostiles && hasPlayerNear(world, claim)) {
                  Box box = claim.getBoundingBox();
                  List<HostileEntity> hostiles = world.getEntitiesByClass(HostileEntity.class, box, h -> h.isAlive() && claim.contains(h.getBlockPos()));

                  for (HostileEntity hostile : hostiles) {
                     repelHostile(claim, hostile);
                  }
               }
            }
         }
      }
   }

   private static boolean hasPlayerNear(ServerWorld world, Claim claim) {
      for (ServerPlayerEntity player : world.getPlayers()) {
         if (ParticleBorder.withinRenderRange(player, claim)) {
            return true;
         }
      }

      return false;
   }

   private static void repelHostile(Claim claim, LivingEntity mob) {
      double mx = mob.getX();
      double mz = mob.getZ();
      int radius = claim.getRadius();
      double cx = (double)claim.getX() + 0.5;
      double cz = (double)claim.getZ() + 0.5;
      double distWest = mx - (cx - (double)radius);
      double distEast = cx + (double)radius - mx;
      double distNorth = mz - (cz - (double)radius);
      double distSouth = cz + (double)radius - mz;
      double pushX = 0.0;
      double pushZ = 0.0;
      double nearest = Math.min(Math.min(distWest, distEast), Math.min(distNorth, distSouth));
      if (nearest == distWest) {
         pushX = -1.0;
      } else if (nearest == distEast) {
         pushX = 1.0;
      } else {
         pushZ = nearest == distNorth ? -1.0 : 1.0;
      }

      mob.setVelocity(pushX * 1.1, 0.42, pushZ * 1.1);
      mob.velocityDirty = true;
      mob.velocityModified = true;
      ClaimConfig config = ClaimConfig.get();
      if (config.hostileBurnSeconds > 0) {
         mob.setOnFireFor((float)config.hostileBurnSeconds);
      }

      if (config.hostileDamage > 0.0F) {
         ((EntityInvulnerabilityAccessor)mob).setTimeUntilRegen(0);
         mob.damage(mob.getDamageSources().generic(), config.hostileDamage);
      }

      mob.addCommandTag(BARRIER_TAG);
   }

   /** Llamado desde BarrierDropsMixin. */
   public static boolean killedByBarrier(LivingEntity mob) {
      if (mob == null) {
         return false;
      } else if (mob.getCommandTags().contains(BARRIER_TAG)) {
         return true;
      } else {
         World world = mob.getWorld();
         if (world == null || world.isClient() || !(mob instanceof Monster)) {
            return false;
         } else {
            Claim claim = ClaimManager.getInstance().getClaimAt(world, mob.getBlockPos());
            return claim != null && claim.getFlags().burnHostiles;
         }
      }
   }

   // ---------------------------------------------------------------- damage

   private static void registerDamageGuards() {
      ServerLivingEntityEvents.ALLOW_DAMAGE
         .register(
            (AllowDamage)(entity, source, amount) -> {
               if (entity.getWorld().isClient()) {
                  return true;
               } else if (DecorationProtection.blocksDamage(entity, source)) {
                  return false;
               } else {
                  Entity attacker = source.getAttacker();
                  Claim c = ClaimManager.getInstance().getClaimAt(entity.getWorld(), entity.getBlockPos());
                  if (c == null && entity instanceof PlayerEntity && attacker instanceof PlayerEntity && !GlobalFlags.getInstance().globalPVP) {
                     if (attacker instanceof ServerPlayerEntity sp) {
                        sp.sendMessage(Text.literal("[!] El PVP está desactivado en este servidor.").formatted(Formatting.RED), true);
                     }

                     return false;
                  } else if (c == null) {
                     return true;
                  } else {
                     if (entity instanceof PlayerEntity victim && attacker instanceof PlayerEntity aggressor) {
                        if (isBypassing(aggressor)) {
                           return true;
                        }

                        if (c.getFlags().pvpAll) {
                           return true;
                        }

                        if (c.getFlags().blockPVP && (!c.canModify(aggressor) || !c.canModify(victim) || c.getFlags().publicMode)) {
                           if (aggressor instanceof ServerPlayerEntity sp) {
                              sp.sendMessage(Text.literal("[!] El PVP está desactivado en esta zona.").formatted(Formatting.RED), true);
                           }

                           return false;
                        }
                     }

                     if (entity instanceof PlayerEntity
                        && attacker instanceof LivingEntity
                        && !(attacker instanceof PlayerEntity)
                        && (c.getFlags().blockMobDamage || c.getFlags().publicMode)) {
                        return false;
                     } else {
                        PlayerEntity p;
                        if (!(entity instanceof AnimalEntity)
                           || !(attacker instanceof PlayerEntity)
                           || c.canModify(p = (PlayerEntity)attacker)
                           || isBypassing(p)
                           || !c.getFlags().publicMode && !c.getFlags().blockAnimalKilling) {
                           return !c.getFlags().blockExplosions || !source.isOf(DamageTypes.EXPLOSION) && !source.isOf(DamageTypes.PLAYER_EXPLOSION);
                        } else {
                           if (p instanceof ServerPlayerEntity sp) {
                              sp.sendMessage(Text.literal("[!] No puedes matar animales en esta zona.").formatted(Formatting.RED), true);
                           }

                           return false;
                        }
                     }
                  }
               }
            }
         );
      AttackEntityCallback.EVENT
         .register(
            (AttackEntityCallback)(player, world, hand, target, hit) -> {
               if (world.isClient()) {
                  return ActionResult.PASS;
               } else if (isBypassing(player)) {
                  return ActionResult.PASS;
               } else {
                  if (DecorationProtection.isDecoration(target)) {
                     Claim deco = DecorationProtection.claimFor(world, target);
                     if (DecorationProtection.blocksPlayer(deco, player)) {
                        if (player instanceof ServerPlayerEntity sp) {
                           sp.sendMessage(Text.literal("[!] Esta decoración está protegida.").formatted(Formatting.RED), true);
                        }

                        return ActionResult.FAIL;
                     }
                  }

                  Claim c = ClaimManager.getInstance().getClaimAt(world, target.getBlockPos());
                  if (c == null) {
                     return ActionResult.PASS;
                  } else if (c.canModify(player)) {
                     return ActionResult.PASS;
                  } else if (c.getFlags().blockAllInteractions) {
                     if (player instanceof ServerPlayerEntity sp) {
                        sp.sendMessage(Text.literal("[!] No puedes interactuar con nada en esta zona.").formatted(Formatting.RED), true);
                     }

                     return ActionResult.FAIL;
                  } else if ((target instanceof AnimalEntity || target instanceof MerchantEntity)
                     && (c.getFlags().publicMode || c.getFlags().blockAnimalKilling || c.getFlags().blockEntityInteract || c.getFlags().blockBuilding)) {
                     if (player instanceof ServerPlayerEntity sp) {
                        sp.sendMessage(Text.literal("[!] No puedes dañar entidades aquí.").formatted(Formatting.RED), true);
                     }

                     return ActionResult.FAIL;
                  } else {
                     return ActionResult.PASS;
                  }
               }
            }
         );
   }

   // ----------------------------------------------------------- interaction

   private static void registerInteractionGuard() {
      UseEntityCallback.EVENT
         .register(
            (UseEntityCallback)(player, world, hand, entity, hit) -> {
               if (world.isClient()) {
                  return ActionResult.PASS;
               } else if (isBypassing(player)) {
                  return ActionResult.PASS;
               } else {
                  if (DecorationProtection.isDecoration(entity)) {
                     Claim deco = DecorationProtection.claimFor(world, entity);
                     if (DecorationProtection.blocksPlayer(deco, player)) {
                        if (player instanceof ServerPlayerEntity sp) {
                           sp.sendMessage(Text.literal("[!] Esta decoración está protegida.").formatted(Formatting.RED), true);
                        }

                        return ActionResult.FAIL;
                     }
                  }

                  Claim c = ClaimManager.getInstance().getClaimAt(world, entity.getBlockPos());
                  if (c == null) {
                     return ActionResult.PASS;
                  } else if (c.canModify(player)) {
                     return ActionResult.PASS;
                  } else if (c.getFlags().blockAllInteractions) {
                     if (player instanceof ServerPlayerEntity sp) {
                        sp.sendMessage(Text.literal("[!] No puedes interactuar con nada en esta zona.").formatted(Formatting.RED), true);
                     }

                     return ActionResult.FAIL;
                  } else {
                     boolean isContainerEntity = entity instanceof StorageMinecartEntity
                        || entity instanceof ChestBoatEntity
                        || entity instanceof AbstractDonkeyEntity;
                     if (!isContainerEntity || !c.getFlags().publicMode && !c.getFlags().blockChestAccess) {
                        if (!c.getFlags().publicMode && !c.getFlags().blockEntityInteract) {
                           return ActionResult.PASS;
                        } else {
                           if (player instanceof ServerPlayerEntity sp) {
                              sp.sendMessage(Text.literal("[!] No puedes interactuar con entidades aquí.").formatted(Formatting.RED), true);
                           }

                           return ActionResult.FAIL;
                        }
                     } else {
                        if (player instanceof ServerPlayerEntity sp) {
                           sp.sendMessage(Text.literal("[!] No puedes abrir este contenedor aquí.").formatted(Formatting.RED), true);
                        }

                        return ActionResult.FAIL;
                     }
                  }
               }
            }
         );
   }
}
