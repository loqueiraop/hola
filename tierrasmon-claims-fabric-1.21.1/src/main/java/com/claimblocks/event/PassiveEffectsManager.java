package com.claimblocks.event;

import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimConfig;
import com.claimblocks.data.ClaimManager;
import com.claimblocks.data.ClaimTier;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;

public final class PassiveEffectsManager {
   private static int counter = 0;
   private static final Set<UUID> grantedFlight = ConcurrentHashMap.newKeySet();

   private PassiveEffectsManager() {
   }

   public static void tick(MinecraftServer server) {
      if (++counter % 20 == 0) {
         boolean runEffects = counter % Math.max(20, ClaimConfig.get().passiveEffectIntervalTicks) < 20;

         for (ServerWorld world : server.getWorlds()) {
            for (ServerPlayerEntity player : world.getPlayers()) {
               Claim claim = ClaimManager.getInstance().getClaimAt(world, player.getBlockPos());
               handleFlight(player, claim);
               if (runEffects) {
                  applyEffects(player, claim);
               }
            }
         }
      }
   }

   private static int paidLevel(ClaimTier t) {
      if (t == null) {
         return 0;
      } else {
         String var1 = t.id;

         return switch (var1) {
            case "claimstone_250x250" -> 1;
            case "claimstone_300x300" -> 2;
            case "claimstone_500x500" -> 3;
            default -> 0;
         };
      }
   }

   private static void handleFlight(ServerPlayerEntity player, Claim claim) {
      boolean creativeOrSpectator = player.interactionManager.getGameMode() == GameMode.CREATIVE
         || player.interactionManager.getGameMode() == GameMode.SPECTATOR;
      UUID id = player.getUuid();
      boolean inClaimWithFlight = false;
      if (claim != null) {
         int level = paidLevel(claim.getTier());
         if (level >= 3 && claim.isOwner(player) && claim.getFlags().allowFlight) {
            inClaimWithFlight = true;
         }
      }

      if (inClaimWithFlight) {
         if (!player.getAbilities().allowFlying && !creativeOrSpectator) {
            player.getAbilities().allowFlying = true;
            grantedFlight.add(id);
            player.sendAbilitiesUpdate();
            player.sendMessage(Text.literal("✔ Vuelo activado (Owner 500x500).").formatted(Formatting.GREEN), true);
         } else if (creativeOrSpectator) {
            grantedFlight.remove(id);
         }
      } else if (grantedFlight.contains(id)) {
         if (!creativeOrSpectator) {
            player.getAbilities().allowFlying = false;
            player.getAbilities().flying = false;
            player.sendAbilitiesUpdate();
            player.sendMessage(Text.literal("[i] Saliste de la zona de vuelo.").formatted(Formatting.AQUA), true);
         }

         grantedFlight.remove(id);
      }
   }

   private static void applyEffects(ServerPlayerEntity player, Claim claim) {
      if (claim != null) {
         int level = paidLevel(claim.getTier());
         if (level != 0) {
            if (claim.canModify(player)) {
               boolean canRegen = level >= 1;
               boolean canResist = level >= 2;
               boolean canSpeed = level >= 2;
               if (canRegen && claim.getFlags().effectRegeneration) {
                  player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, ClaimConfig.get().effectDurationTicks, 0, true, false, true));
               }

               if (canResist && claim.getFlags().effectResistance) {
                  player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, ClaimConfig.get().effectDurationTicks, 0, true, false, true));
               }

               if (canSpeed && claim.getFlags().effectSpeed) {
                  player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, ClaimConfig.get().effectDurationTicks, 0, true, false, true));
               }
            }
         }
      }
   }

   public static void onPlayerDisconnect(UUID id) {
      grantedFlight.remove(id);
   }
}
