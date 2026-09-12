package com.claimblocks.chat;

import com.claimblocks.ClaimBlocksMod;
import com.claimblocks.gui.AdminClaimSubMenuHandler;
import com.claimblocks.gui.ClaimMenuHandler;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Enruta las respuestas que el jugador escribe en el chat cuando un menu le pide un dato.
 * La captura se hace a nivel de paquete, asi que funciona aunque un plugin de chat
 * se coma el mensaje antes (servidores hibridos tipo Mohist / Arclight / Magma).
 */
public final class ChatPromptRouter {
   private static final long SUPPRESS_WINDOW_MS = 2000L;
   private static final Map<UUID, ChatPromptRouter.Suppression> suppressions = new ConcurrentHashMap<>();
   private static volatile boolean packetCaptureActive;

   private ChatPromptRouter() {
   }

   public static boolean isPacketCaptureActive() {
      return packetCaptureActive;
   }

   public static boolean hasPending(UUID id) {
      return id != null && (ClaimMenuHandler.hasPrompt(id) || AdminClaimSubMenuHandler.hasPendingTransfer(id));
   }

   /** Devuelve true si el mensaje era la respuesta a un menu (y por tanto no debe salir en el chat). */
   public static boolean consume(ServerPlayerEntity player, String raw) {
      if (player != null && raw != null) {
         MinecraftServer server = player.getServer();
         if (server == null) {
            return false;
         } else {
            UUID id = player.getUuid();
            UUID adminTransferClaim = AdminClaimSubMenuHandler.popPendingTransfer(id);
            if (adminTransferClaim != null) {
               String clean = stripControlChars(raw);
               markSuppressed(id, raw);
               server.execute(() -> ClaimMenuHandler.dispatchAdminTransfer(player, adminTransferClaim, clean));
               return true;
            } else {
               ClaimMenuHandler.PendingChat prompt = ClaimMenuHandler.popPrompt(id);
               if (prompt != null) {
                  String clean = stripControlChars(raw);
                  markSuppressed(id, raw);
                  server.execute(() -> ClaimMenuHandler.dispatchPrompt(player, prompt, clean));
                  return true;
               } else {
                  return false;
               }
            }
         }
      } else {
         return false;
      }
   }

   private static void markSuppressed(UUID id, String raw) {
      suppressions.put(id, new ChatPromptRouter.Suppression(raw, System.currentTimeMillis() + SUPPRESS_WINDOW_MS));
   }

   public static boolean shouldSuppress(UUID id, String raw) {
      if (id != null && raw != null) {
         ChatPromptRouter.Suppression s = suppressions.get(id);
         if (s == null) {
            return false;
         } else if (System.currentTimeMillis() > s.expiresAt()) {
            suppressions.remove(id, s);
            return false;
         } else if (!s.rawText().equals(raw)) {
            return false;
         } else {
            suppressions.remove(id, s);
            return true;
         }
      } else {
         return false;
      }
   }

   public static void onPlayerDisconnect(UUID id) {
      if (id != null) {
         suppressions.remove(id);
      }
   }

   public static void markPacketCaptureActive() {
      if (!packetCaptureActive) {
         packetCaptureActive = true;
         ClaimBlocksMod.LOGGER.info("[ClaimBlocks] Captura de respuestas a nivel de paquete ACTIVA (compatible con plugins de chat).");
      }
   }

   public static String stripControlChars(String s) {
      if (s == null) {
         return "";
      } else {
         StringBuilder sb = new StringBuilder(s.length());

         for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\n' || c == '\r' || c == '\t') {
               sb.append(' ');
            } else if (c >= ' ') {
               sb.append(c);
            }
         }

         return sb.toString().trim();
      }
   }

   public static String sanitize(String s) {
      if (s == null) {
         return "";
      } else {
         StringBuilder sb = new StringBuilder(s.length());

         for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c == 167 || c == '&') && i + 1 < s.length() && isColorCode(s.charAt(i + 1))) {
               i++;
            } else if (c == '\n' || c == '\r' || c == '\t') {
               sb.append(' ');
            } else if (c >= ' ') {
               sb.append(c);
            }
         }

         return sb.toString().trim().replaceAll("\\s{2,}", " ");
      }
   }

   private static boolean isColorCode(char c) {
      return "0123456789abcdefklmnorABCDEFKLMNOR".indexOf(c) >= 0;
   }

   public static boolean isCancel(String s) {
      if (s != null && !s.isBlank()) {
         String t = s.trim();
         return t.equalsIgnoreCase("cancelar") || t.equalsIgnoreCase("cancel") || t.startsWith("/");
      } else {
         return true;
      }
   }

   /** Saca un nombre de jugador plausible de una frase (por si el chat trae prefijos del plugin). */
   public static String extractPlayerName(String s) {
      String clean = sanitize(s);
      if (!clean.isEmpty() && !isValidName(clean)) {
         String[] parts = clean.split(" ");

         for (int i = parts.length - 1; i >= 0; i--) {
            if (isValidName(parts[i])) {
               return parts[i];
            }
         }

         return clean;
      } else {
         return clean;
      }
   }

   private static boolean isValidName(String s) {
      if (s != null && s.length() >= 3 && s.length() <= 16) {
         for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean ok = c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_';
            if (!ok) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private static record Suppression(String rawText, long expiresAt) {
   }
}
