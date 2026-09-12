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
import com.claimblocks.net.ClaimBordersPayload;
import com.claimblocks.net.ClaimNetwork;
import com.claimblocks.render.ParticleBorder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
   private static final Map<UUID, Integer> lastBorderHash = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> lastBorderSend = new ConcurrentHashMap<>();
   private static final long BORDER_KEEPALIVE_MS = 2000L;

   public void onInitialize() {
      LOGGER.info("[ClaimBlocks] Inicializando Fantastic Claims para Fabric 1.21.1...");
      ClaimNetwork.init();
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
         LOGGER.info("[ClaimBlocks] Datos cargados.");
      });
      ServerLifecycleEvents.SERVER_STOPPING.register((ServerStopping)server -> {
         ClaimManager.getInstance().saveNow();
         GlobalFlags.getInstance().save(server);
         LOGGER.info("[ClaimBlocks] Datos guardados al apagar.");
      });
      ServerPlayConnectionEvents.JOIN.register((Join)(handler, sender, server) -> ClaimManager.getInstance().flushPendingTo(handler.player));
      ServerPlayConnectionEvents.DISCONNECT.register((Disconnect)(handler, server) -> {
         UUID id = handler.player.getUuid();
         ClaimMenuHandler.clearPrompt(id);
         AdminClaimSubMenuHandler.clearPendingTransfer(id);
         ChatPromptRouter.onPlayerDisconnect(id);
         lastBorderHash.remove(id);
         lastBorderSend.remove(id);
      });
      ServerTickEvents.END_SERVER_TICK.register((EndTick)server -> {
         PlayerTracker.tick(server);
         BlockProtectionEvents.tickFireSweep(server);
         PassiveEffectsManager.tick(server);
         EntityProtectionEvents.tickHostileBarrier(server);
         if (++particleCounter % ClaimConfig.get().particleIntervalTicks == 0) {
            renderClaimParticles(server);
         }

         if (particleCounter % ClaimConfig.get().borderIntervalTicks == 0) {
            sendBorderPackets(server);
         }
      });
      LOGGER.info("[ClaimBlocks] Inicializacion completada (10 tiers, 34 flags, grupos, panel admin).");
   }

   // ------------------------------------------------------------- particulas

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

   // ---------------------------------------------------------------- bordes

   private static int borderHash(List<double[]> boxes) {
      int h = 1;

      for (double[] box : boxes) {
         h = 31 * h + Arrays.hashCode(box);
      }

      return h;
   }

   private static void sendBorderPackets(MinecraftServer server) {
      for (ServerWorld world : server.getWorlds()) {
         String dim = world.getRegistryKey().getValue().toString();

         for (ServerPlayerEntity player : world.getPlayers()) {
            if (ClaimNetwork.canSend(player)) {
               ArrayList<double[]> boxes = new ArrayList<>();
               HashSet<UUID> seenClaims = new HashSet<>();
               HashSet<UUID> seenGroups = new HashSet<>();
               Claim here = ClaimManager.getInstance().getClaimAt(world, player.getBlockPos());
               if (here != null && here.getFlags().showBorder && here.canModify(player)) {
                  addBorder(boxes, here, dim, seenClaims, seenGroups);
               }

               for (Claim own : ClaimManager.getInstance().getClaimsOf(player.getUuid())) {
                  if (own.getWorld().equals(dim) && own.getFlags().showBorder && ParticleBorder.withinRenderRange(player, own)) {
                     addBorder(boxes, own, dim, seenClaims, seenGroups);
                  }
               }

               int hash = borderHash(boxes);
               Integer previous = lastBorderHash.get(player.getUuid());
               long now = System.currentTimeMillis();
               Long lastSend = lastBorderSend.get(player.getUuid());
               boolean changed = previous == null || previous != hash;
               boolean keepAlive = lastSend == null || now - lastSend >= BORDER_KEEPALIVE_MS;
               if (changed || keepAlive) {
                  lastBorderHash.put(player.getUuid(), hash);
                  lastBorderSend.put(player.getUuid(), now);
                  ClaimNetwork.sendTo(player, new ClaimBordersPayload(boxes));
               }
            }
         }
      }
   }

   private static void addBorder(ArrayList<double[]> out, Claim claim, String dim, HashSet<UUID> seenClaims, HashSet<UUID> seenGroups) {
      if (claim.getGroupId() != null) {
         UUID gid = claim.getGroupId();
         if (!seenGroups.contains(gid)) {
            seenGroups.add(gid);
            addGroupOutline(out, gid, dim);
         }
      } else if (!seenClaims.contains(claim.getClaimId())) {
         seenClaims.add(claim.getClaimId());
         out.add(boxOf(claim));
      }
   }

   private static double[] boxOf(Claim claim) {
      int radius = claim.getRadius();
      int height = claim.getHeight();
      float r = 1.0F;
      float g = 1.0F;
      float b = 1.0F;
      if (claim.getTier() != null) {
         r = claim.getTier().r;
         g = claim.getTier().g;
         b = claim.getTier().b;
      }

      return new double[]{
         (double)(claim.getX() - radius),
         (double)(claim.getY() - height),
         (double)(claim.getZ() - radius),
         (double)(claim.getX() + radius + 1),
         (double)(claim.getY() + height + 1),
         (double)(claim.getZ() + radius + 1),
         (double)r,
         (double)g,
         (double)b
      };
   }

   /**
    * Dibuja el perimetro exterior de un grupo de zonas: se recortan las paredes
    * interiores para que el conjunto se vea como una sola region.
    */
   private static void addGroupOutline(ArrayList<double[]> out, UUID groupId, String dim) {
      ClaimManager mgr = ClaimManager.getInstance();
      Claim mother = mgr.getMotherClaim(groupId);
      if (mother != null) {
         ArrayList<Claim> members = new ArrayList<>();

         for (Claim c : mgr.getGroupClaims(groupId)) {
            if (c.getWorld().equals(dim)) {
               members.add(c);
            }
         }

         if (!members.isEmpty()) {
            double minY = (double)(mother.getY() - mother.getOwnHeight());
            double maxY = (double)(mother.getY() + mother.getOwnHeight() + 1);
            float r = 1.0F;
            float g = 1.0F;
            float b = 1.0F;
            if (mother.getTier() != null) {
               r = mother.getTier().r;
               g = mother.getTier().g;
               b = mother.getTier().b;
            }

            int count = members.size();
            int[] x0 = new int[count];
            int[] x1 = new int[count];
            int[] z0 = new int[count];
            int[] z1 = new int[count];
            TreeSet<Integer> xs = new TreeSet<>();
            TreeSet<Integer> zs = new TreeSet<>();

            for (int i = 0; i < count; i++) {
               Claim c = members.get(i);
               int radius = c.getRadius();
               x0[i] = c.getX() - radius;
               x1[i] = c.getX() + radius + 1;
               z0[i] = c.getZ() - radius;
               z1[i] = c.getZ() + radius + 1;
               xs.add(x0[i]);
               xs.add(x1[i]);
               zs.add(z0[i]);
               zs.add(z1[i]);
            }

            Integer[] gridX = xs.toArray(new Integer[0]);
            Integer[] gridZ = zs.toArray(new Integer[0]);
            int nx = gridX.length;
            int nz = gridZ.length;
            if (nx >= 2 && nz >= 2) {
               boolean[][] filled = new boolean[nx - 1][nz - 1];

               for (int i = 0; i < nx - 1; i++) {
                  double cx = (double)(gridX[i] + gridX[i + 1]) / 2.0;

                  for (int j = 0; j < nz - 1; j++) {
                     double cz = (double)(gridZ[j] + gridZ[j + 1]) / 2.0;
                     boolean inside = false;

                     for (int k = 0; k < count; k++) {
                        if (cx >= (double)x0[k] && cx < (double)x1[k] && cz >= (double)z0[k] && cz < (double)z1[k]) {
                           inside = true;
                           break;
                        }
                     }

                     filled[i][j] = inside;
                  }
               }

               // paredes paralelas a Z
               for (int i = 0; i < nx; i++) {
                  int j = 0;

                  while (j < nz - 1) {
                     boolean left = i > 0 && filled[i - 1][j];
                     boolean right = i < nx - 1 && filled[i][j];
                     if (left != right) {
                        int startJ = j;

                        while (j < nz - 1 && (i > 0 && filled[i - 1][j]) != (i < nx - 1 && filled[i][j])) {
                           j++;
                        }

                        out.add(
                           new double[]{
                              (double)gridX[i].intValue() - 0.03,
                              minY,
                              (double)gridZ[startJ].intValue(),
                              (double)gridX[i].intValue() + 0.03,
                              maxY,
                              (double)gridZ[j].intValue(),
                              (double)r,
                              (double)g,
                              (double)b
                           }
                        );
                     } else {
                        j++;
                     }
                  }
               }

               // paredes paralelas a X
               for (int j = 0; j < nz; j++) {
                  int i = 0;

                  while (i < nx - 1) {
                     boolean up = j > 0 && filled[i][j - 1];
                     boolean down = j < nz - 1 && filled[i][j];
                     if (up != down) {
                        int startI = i;

                        while (i < nx - 1 && (j > 0 && filled[i][j - 1]) != (j < nz - 1 && filled[i][j])) {
                           i++;
                        }

                        out.add(
                           new double[]{
                              (double)gridX[startI].intValue(),
                              minY,
                              (double)gridZ[j].intValue() - 0.03,
                              (double)gridX[i].intValue(),
                              maxY,
                              (double)gridZ[j].intValue() + 0.03,
                              (double)r,
                              (double)g,
                              (double)b
                           }
                        );
                     } else {
                        i++;
                     }
                  }
               }
            }
         }
      }
   }
}
