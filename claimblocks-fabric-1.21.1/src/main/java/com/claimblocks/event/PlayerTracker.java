package com.claimblocks.event;

import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimConfig;
import com.claimblocks.data.ClaimGroup;
import com.claimblocks.data.ClaimManager;
import com.claimblocks.mixin.EntityInvulnerabilityAccessor;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.Disconnect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

/** Avisos de entrada y salida de zona, alertas al dueno y barrera para jugadores baneados. */
public final class PlayerTracker {
   private static final Map<UUID, UUID> lastClaim = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> lastAlert = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> lastBanHit = new ConcurrentHashMap<>();
   private static int bypassReminderCounter = 0;

   public static void register() {
      ServerPlayConnectionEvents.DISCONNECT.register((Disconnect)(handler, server) -> onDisconnect(handler.player.getUuid()));
   }

   public static void onDisconnect(UUID id) {
      lastClaim.remove(id);
      lastAlert.remove(id);
      lastBanHit.remove(id);
      ClaimManager.getInstance().onPlayerDisconnect(id);
      PassiveEffectsManager.onPlayerDisconnect(id);
   }

   public static void tick(MinecraftServer server) {
      boolean showBypassReminder = ++bypassReminderCounter % 60 == 0;

      for (ServerWorld world : server.getWorlds()) {
         for (ServerPlayerEntity player : world.getPlayers()) {
            handle(world, player);
            if (showBypassReminder && player.hasPermissionLevel(2) && ClaimManager.getInstance().isBypassing(player.getUuid())) {
               player.sendMessage(Text.literal("[!] BYPASS ACTIVO").formatted(new Formatting[]{Formatting.RED, Formatting.BOLD}), true);
            }
         }
      }
   }

   private static void handle(ServerWorld world, ServerPlayerEntity player) {
      Claim claim = ClaimManager.getInstance().getClaimAt(world, player.getBlockPos());
      UUID previousZone = lastClaim.get(player.getUuid());
      UUID currentZone = claim == null ? null : zoneId(claim);
      if (Objects.equals(previousZone, currentZone)) {
         if (claim != null && claim.isBanned(player.getUuid()) && !player.hasPermissionLevel(2)) {
            repelBanned(world, player, claim);
         }
      } else {
         Claim previous = resolveZone(previousZone);
         if (previous != null) {
            MutableText leaveMsg = previous.getFlags().showLeave
                  && previous.getFlags().leaveMessage != null
                  && !previous.getFlags().leaveMessage.isBlank()
               ? Text.literal("[Claim] ")
                  .formatted(Formatting.GRAY)
                  .append(Text.literal(truncate(previous.getFlags().leaveMessage, 50)).formatted(Formatting.GOLD))
               : Text.literal("[Claim] ")
                  .formatted(Formatting.GRAY)
                  .append(Text.literal("Saliendo de la zona ").formatted(Formatting.RED))
                  .append(Text.literal(truncate(zoneLabel(previous), 24)).formatted(new Formatting[]{Formatting.WHITE, Formatting.BOLD}));
            player.sendMessage(leaveMsg, true);
            player.playSoundToPlayer(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 2.0F, 0.9F);
         }

         if (claim != null) {
            if (claim.isBanned(player.getUuid()) && !player.hasPermissionLevel(2)) {
               repelBanned(world, player, claim);
               lastClaim.remove(player.getUuid());
               return;
            }

            MutableText enterMsg = claim.getFlags().showWelcome
                  && claim.getFlags().welcomeMessage != null
                  && !claim.getFlags().welcomeMessage.isBlank()
               ? Text.literal("[Claim] ")
                  .formatted(Formatting.GRAY)
                  .append(Text.literal(truncate(claim.getFlags().welcomeMessage, 50)).formatted(Formatting.GREEN))
               : Text.literal("[Claim] ")
                  .formatted(Formatting.GRAY)
                  .append(Text.literal("Entrando a la zona ").formatted(Formatting.GREEN))
                  .append(Text.literal(truncate(zoneLabel(claim), 24)).formatted(new Formatting[]{Formatting.WHITE, Formatting.BOLD}));
            player.sendMessage(enterMsg, true);
            player.playSoundToPlayer(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 2.0F, 1.4F);
            if (claim.getFlags().trespasserAlerts && !claim.canModify(player)) {
               long now = world.getTime();
               Long last = lastAlert.get(player.getUuid());
               if (last == null || now - last > (long)ClaimConfig.get().trespasserAlertTicks()) {
                  lastAlert.put(player.getUuid(), now);
                  UUID ownerId = zoneOwner(claim);
                  ServerPlayerEntity owner = ownerId == null ? null : world.getServer().getPlayerManager().getPlayer(ownerId);
                  if (owner != null) {
                     MutableText alert = Text.literal("[!] ")
                        .formatted(new Formatting[]{Formatting.RED, Formatting.BOLD})
                        .append(Text.literal(truncate(player.getName().getString(), 16)).formatted(new Formatting[]{Formatting.WHITE, Formatting.BOLD}))
                        .append(Text.literal(" entró a tu zona en X=" + claim.getX() + " Z=" + claim.getZ()).formatted(Formatting.YELLOW));
                     owner.sendMessage(alert, false);
                  }
               }
            }
         }

         if (currentZone == null) {
            lastClaim.remove(player.getUuid());
         } else {
            lastClaim.put(player.getUuid(), currentZone);
         }
      }
   }

   /** Una zona fusionada se trata como una sola zona a efectos de mensajes de entrada/salida. */
   private static UUID zoneId(Claim claim) {
      return claim.getGroupId() != null ? claim.getGroupId() : claim.getClaimId();
   }

   private static Claim resolveZone(UUID id) {
      if (id == null) {
         return null;
      } else {
         ClaimGroup group = ClaimManager.getInstance().getGroup(id);
         return group != null ? ClaimManager.getInstance().getMotherClaim(id) : ClaimManager.getInstance().findClaimById(id);
      }
   }

   private static String zoneLabel(Claim claim) {
      if (claim == null) {
         return "";
      } else {
         ClaimGroup group;
         return claim.getGroupId() != null && (group = ClaimManager.getInstance().getGroup(claim.getGroupId())) != null
            ? group.getName()
            : claim.getOwnerName() + " (" + claim.sizeLabel() + ")";
      }
   }

   private static UUID zoneOwner(Claim claim) {
      ClaimGroup group;
      return claim.getGroupId() != null
            && (group = ClaimManager.getInstance().getGroup(claim.getGroupId())) != null
            && group.getMotherOwnerId() != null
         ? group.getMotherOwnerId()
         : claim.getOwnerUUID();
   }

   /** Saca al jugador baneado por el borde mas cercano (o lo empuja, segun config). */
   private static void repelBanned(ServerWorld world, ServerPlayerEntity player, Claim claim) {
      double cx = (double)claim.getX() + 0.5;
      double cz = (double)claim.getZ() + 0.5;
      double reach = (double)claim.getRadius() + 1.5;
      double px = player.getX();
      double pz = player.getZ();
      double distWest = px - (cx - reach);
      double distEast = cx + reach - px;
      double distNorth = pz - (cz - reach);
      double distSouth = cz + reach - pz;
      double nearest = Math.min(Math.min(distWest, distEast), Math.min(distNorth, distSouth));
      double targetX = px;
      double targetZ = pz;
      if (nearest == distWest) {
         targetX = cx - reach;
      } else if (nearest == distEast) {
         targetX = cx + reach;
      } else if (nearest == distNorth) {
         targetZ = cz - reach;
      } else {
         targetZ = cz + reach;
      }

      ClaimConfig config = ClaimConfig.get();
      if (config.banTeleportOut) {
         double safeY = safeY(world, targetX, player.getY(), targetZ);
         player.teleport(world, targetX, safeY, targetZ, player.getYaw(), player.getPitch());
         player.setVelocity(0.0, 0.0, 0.0);
      } else {
         double dx = player.getX() - cx;
         double dz = player.getZ() - cz;
         double len = Math.max(1.0E-4, Math.sqrt(dx * dx + dz * dz));
         player.setVelocity(dx / len * 1.2, 0.42, dz / len * 1.2);
      }

      player.velocityModified = true;
      long now = world.getTime();
      Long last = lastBanHit.get(player.getUuid());
      if (last == null || now - last >= ClaimConfig.get().banNoticeTicks()) {
         lastBanHit.put(player.getUuid(), now);
         if (config.banDamage > 0.0F) {
            ((EntityInvulnerabilityAccessor)player).setTimeUntilRegen(0);
            player.damage(player.getDamageSources().magic(), config.banDamage);
         }

         player.sendMessage(Text.literal("[!] Estás baneado de esta zona.").formatted(new Formatting[]{Formatting.RED, Formatting.BOLD}), false);
         player.playSoundToPlayer(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1.0F, 0.6F);
      }
   }

   /** Busca un hueco de dos bloques de aire cerca de la Y actual para no dejarlo dentro de un muro. */
   private static double safeY(ServerWorld world, double x, double y, double z) {
      int bx = (int)Math.floor(x);
      int bz = (int)Math.floor(z);
      int by = (int)Math.floor(y);

      for (int offset = 0; offset <= 6; offset++) {
         for (int sign = 1; sign >= -1; sign -= 2) {
            int candidate = by + offset * sign;
            if (candidate >= world.getBottomY()
               && candidate <= world.getTopY() - 2
               && world.getBlockState(new BlockPos(bx, candidate, bz)).isAir()
               && world.getBlockState(new BlockPos(bx, candidate + 1, bz)).isAir()) {
               return (double)candidate;
            }
         }
      }

      return y;
   }

   private static String truncate(String s, int max) {
      if (s == null) {
         return "";
      } else {
         return s.length() <= max ? s : s.substring(0, Math.max(0, max - 3)) + "...";
      }
   }
}
