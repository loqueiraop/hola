package com.claimblocks.util;

import com.claimblocks.ClaimBlocksMod;
import com.mojang.authlib.GameProfile;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.UserCache;
import net.minecraft.util.Uuids;

/**
 * Resuelve jugadores por nombre incluso si estan desconectados.
 * En servidores offline-mode calcula el UUID offline correcto, para que
 * los miembros y baneos no se pierdan al reconectar.
 */
public final class PlayerLookup {
   private static final ExecutorService LOOKUP = Executors.newSingleThreadExecutor(r -> {
      Thread t = new Thread(r, "ClaimBlocks-ProfileLookup");
      t.setDaemon(true);
      return t;
   });

   private PlayerLookup() {
   }

   public static boolean isOfflineMode(MinecraftServer server) {
      return server != null && !server.isOnlineMode();
   }

   /** Resolucion inmediata: jugador conectado, o UUID offline si el servidor no autentica. */
   public static PlayerLookup.Resolved resolve(MinecraftServer server, String name) {
      if (server != null && name != null && !name.isBlank()) {
         String clean = name.trim();
         ServerPlayerEntity online = server.getPlayerManager().getPlayer(clean);
         if (online != null) {
            return new PlayerLookup.Resolved(online.getUuid(), online.getName().getString(), online);
         } else if (isOfflineMode(server)) {
            UUID offline = Uuids.getOfflinePlayerUuid(clean);
            return new PlayerLookup.Resolved(offline, clean, server.getPlayerManager().getPlayer(offline));
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   /**
    * Resolucion asincrona: si no esta conectado consulta la cache de perfiles en otro hilo
    * y devuelve el resultado en el hilo del servidor.
    */
   public static void resolveAsync(MinecraftServer server, String name, Consumer<PlayerLookup.Resolved> callback) {
      if (server != null && name != null && !name.isBlank()) {
         String clean = name.trim();
         PlayerLookup.Resolved immediate = resolve(server, clean);
         if (immediate != null) {
            callback.accept(immediate);
         } else {
            UserCache cache = server.getUserCache();
            if (cache == null) {
               callback.accept(null);
            } else {
               LOOKUP.execute(() -> {
                  PlayerLookup.Resolved found = null;

                  try {
                     Optional<GameProfile> profile = cache.findByName(clean);
                     if (profile.isPresent() && profile.get().getId() != null) {
                        GameProfile p = profile.get();
                        found = new PlayerLookup.Resolved(p.getId(), p.getName() == null ? clean : p.getName(), null);
                     }
                  } catch (Throwable t) {
                     ClaimBlocksMod.LOGGER.warn("[Tierrasmon Claims] No se pudo resolver el perfil de '" + clean + "'", t);
                  }

                  PlayerLookup.Resolved result = found;
                  server.execute(() -> {
                     PlayerLookup.Resolved withOnline = result;
                     if (result != null) {
                        withOnline = new PlayerLookup.Resolved(result.id(), result.name(), server.getPlayerManager().getPlayer(result.id()));
                     }

                     callback.accept(withOnline);
                  });
               });
            }
         }
      } else {
         callback.accept(null);
      }
   }

   public static String nameOf(MinecraftServer server, UUID id) {
      if (id == null) {
         return "?";
      } else {
         if (server != null) {
            ServerPlayerEntity online = server.getPlayerManager().getPlayer(id);
            if (online != null) {
               return online.getName().getString();
            }

            UserCache cache = server.getUserCache();
            if (cache != null) {
               Optional<GameProfile> profile = cache.getByUuid(id);
               if (profile.isPresent() && profile.get().getName() != null) {
                  return profile.get().getName();
               }
            }
         }

         return id.toString().substring(0, 8);
      }
   }

   public static record Resolved(UUID id, String name, ServerPlayerEntity online) {
      public boolean isOnline() {
         return this.online != null;
      }
   }
}
