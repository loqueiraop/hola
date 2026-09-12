package com.claimblocks.util;

import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimConfig;
import com.claimblocks.data.ClaimFlags;
import com.claimblocks.data.ClaimManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/** Protege cuadros, marcos de items y soportes de armadura dentro de una zona. */
public final class DecorationProtection {
   private DecorationProtection() {
   }

   public static boolean isDecoration(Entity entity) {
      return !ClaimConfig.get().protectDecoration ? false : entity instanceof AbstractDecorationEntity || entity instanceof ArmorStandEntity;
   }

   /** Busca la zona de la decoracion: en su posicion, o en el bloque al que esta pegada. */
   public static Claim claimFor(World world, Entity entity) {
      if (world != null && entity != null) {
         ClaimManager mgr = ClaimManager.getInstance();
         BlockPos pos = entity.getBlockPos();
         Claim c = mgr.getClaimAt(world, pos);
         if (c != null) {
            return c;
         } else {
            Direction facing = entity.getHorizontalFacing();
            return facing != null ? mgr.getClaimAt(world, pos.offset(facing.getOpposite())) : null;
         }
      } else {
         return null;
      }
   }

   private static boolean isBypassing(PlayerEntity player) {
      return player.hasPermissionLevel(2) && ClaimManager.getInstance().isBypassing(player.getUuid());
   }

   public static boolean blocksPlayer(Claim claim, PlayerEntity player) {
      if (claim == null || player == null) {
         return false;
      } else if (!claim.canModify(player) && !isBypassing(player)) {
         ClaimFlags f = claim.getFlags();
         return f.blockBuilding || f.blockEntityInteract || f.publicMode;
      } else {
         return false;
      }
   }

   public static boolean blocksDamage(Entity entity, DamageSource source) {
      if (!isDecoration(entity)) {
         return false;
      } else {
         World world = entity.getWorld();
         if (world != null && !world.isClient()) {
            Claim claim = claimFor(world, entity);
            if (claim == null) {
               return false;
            } else {
               PlayerEntity player = responsiblePlayer(source);
               if (player != null) {
                  return blocksPlayer(claim, player);
               } else {
                  ClaimFlags f = claim.getFlags();
                  return source != null && source.isIn(DamageTypeTags.IS_EXPLOSION)
                     ? ClaimConfig.get().protectDecorationFromExplosions && (f.blockExplosions || f.publicMode)
                     : f.blockBuilding || f.publicMode;
               }
            }
         } else {
            return false;
         }
      }
   }

   public static PlayerEntity responsiblePlayer(DamageSource source) {
      if (source == null) {
         return null;
      } else {
         Entity attacker = source.getAttacker();
         if (attacker instanceof PlayerEntity p) {
            return p;
         } else {
            Entity direct = source.getSource();
            return direct instanceof PlayerEntity p2 ? p2 : ownerOf(direct);
         }
      }
   }

   public static PlayerEntity ownerOf(Entity entity) {
      if (entity instanceof ProjectileEntity projectile) {
         Entity owner = projectile.getOwner();
         if (owner instanceof PlayerEntity p) {
            return p;
         }
      }

      return null;
   }
}
