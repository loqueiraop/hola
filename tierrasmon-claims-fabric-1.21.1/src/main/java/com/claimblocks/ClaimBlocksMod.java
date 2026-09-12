package com.claimblocks;

import com.claimblocks.chat.ChatPromptRouter;
import com.claimblocks.command.ClaimAdminCommands;
import com.claimblocks.command.ClaimCommands;
import com.claimblocks.command.ClaimMergeCommands;
import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimConfig;
import com.claimblocks.data.ClaimManager;
import com.claimblocks.data.GlobalFlags;
import com.claimblocks.event.BlockProtectionEvents;
import com.claimblocks.event.EntityProtectionEvents;
import com.claimblocks.event.PassiveEffectsManager;
import com.claimblocks.event.PlayerTracker;
import com.claimblocks.gui.AdminClaimSubMenuHandler;
import com.claimblocks.gui.ClaimMenuHandler;
import com.claimblocks.render.ParticleBorder;
import java.util.HashSet;
import java.util.UUID;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStarted;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStopping;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.EndTick;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.Disconnect;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.Join;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClaimBlocksMod implements ModInitializer {
   public static final String MOD_ID = "claimblocks";
   public static final Logger LOGGER = LoggerFactory.getLogger("claimblocks");
   private static int particleCounter = 0;

   public void onInitialize() {
      LOGGER.info("[Tierrasmon Claims] Inicializando Fantastic Claims para Fabric 1.21.1 (100% server-side)...");
      ClaimCommands.register();
      ClaimAdminCommands.register();
      ClaimMergeCommands.register();
      BlockProtectionEvents.register();
      EntityProtectionEvents.register();
      PlayerTracker.register();
      ClaimMenuHandler.registerChatListener();
      ServerLifecycleEvents.SERVER_STARTED.register((ServerStarted)server -> {
         ClaimManager.getInstance().load(server);
         GlobalFlags.getInstance().load(server);
         LOGGER.info("[Tierrasmon Claims] Datos cargados.");
      });
      ServerLifecycleEvents.SERVER_STOPPING.register((ServerStopping)server -> {
         ClaimManager.getInstance().saveNow();
         GlobalFlags.getInstance().save(server);
         LOGGER.info("[Tierrasmon Claims] Datos guardados al apagar.");
      });
      ServerPlayConnectionEvents.JOIN.register((Join)(handler, sender, server) -> ClaimManager.getInstance().flushPendingTo(handler.player));
      ServerPlayConnectionEvents.DISCONNECT.register((Disconnect)(handler, server) -> {
         UUID id = handler.player.getUuid();
         ClaimMenuHandler.clearPrompt(id);
         AdminClaimSubMenuHandler.clearPendingTransfer(id);
         ChatPromptRouter.onPlayerDisconnect(id);
      });
      ServerTickEvents.END_SERVER_TICK.register((EndTick)server -> {
         PlayerTracker.tick(server);
         BlockProtectionEvents.tickFireSweep(server);
         PassiveEffectsManager.tick(server);
         EntityProtectionEvents.tickHostileBarrier(server);
         particleCounter++;
         if (particleCounter % ClaimConfig.get().particleIntervalTicks == 0) {
            renderClaimParticles(server);
         }

         if (particleCounter % ClaimConfig.get().borderIntervalTicks == 0) {
            renderClaimBorders(server);
         }
      });
      LOGGER.info("[Tierrasmon Claims] Inicializacion completada (10 tiers, 34 flags, grupos, panel admin).");
   }

   /** Flag "Ver particulas": rellena el area de la zona. */
   private static void renderClaimParticles(MinecraftServer server) {
      for (ServerWorld world : server.getWorlds()) {
         String dim = world.getRegistryKey().getValue().toString();

         for (ServerPlayerEntity player : world.getPlayers()) {
            HashSet<UUID> drawn = new HashSet<>();
            Claim here = ClaimManager.getInstance().getClaimAt(world, player.getBlockPos());
            if (here != null && here.getFlags().showParticles && here.canModify(player)) {
               ParticleBorder.fillClaim(world, player, here);
               drawn.add(here.getClaimId());
            }

            for (Claim own : ClaimManager.getInstance().getClaimsOf(player.getUuid())) {
               if (!drawn.contains(own.getClaimId())
                  && own.getFlags().showParticles
                  && own.getWorld().equals(dim)
                  && ParticleBorder.withinRenderRange(player, own)) {
                  ParticleBorder.fillClaim(world, player, own);
                  drawn.add(own.getClaimId());
               }
            }
         }
      }
   }

   /** Flag "Ver contorno": dibuja las aristas con polvo del color de la piedra. */
   private static void renderClaimBorders(MinecraftServer server) {
      for (ServerWorld world : server.getWorlds()) {
         String dim = world.getRegistryKey().getValue().toString();

         for (ServerPlayerEntity player : world.getPlayers()) {
            HashSet<UUID> seenClaims = new HashSet<>();
            HashSet<UUID> seenGroups = new HashSet<>();
            Claim here = ClaimManager.getInstance().getClaimAt(world, player.getBlockPos());
            if (here != null && here.getFlags().showBorder && here.canModify(player)) {
               drawBorder(world, player, here, dim, seenClaims, seenGroups);
            }

            for (Claim own : ClaimManager.getInstance().getClaimsOf(player.getUuid())) {
               if (own.getWorld().equals(dim) && own.getFlags().showBorder && ParticleBorder.withinRenderRange(player, own)) {
                  drawBorder(world, player, own, dim, seenClaims, seenGroups);
               }
            }
         }
      }
   }

   private static void drawBorder(
      ServerWorld world, ServerPlayerEntity player, Claim claim, String dim, HashSet<UUID> seenClaims, HashSet<UUID> seenGroups
   ) {
      if (claim.getGroupId() != null) {
         UUID gid = claim.getGroupId();
         if (!seenGroups.contains(gid)) {
            seenGroups.add(gid);
            ParticleBorder.drawGroupOutline(world, player, gid, dim);
         }
      } else if (!seenClaims.contains(claim.getClaimId())) {
         seenClaims.add(claim.getClaimId());
         ParticleBorder.drawOutline(world, player, claim);
      }
   }
}
