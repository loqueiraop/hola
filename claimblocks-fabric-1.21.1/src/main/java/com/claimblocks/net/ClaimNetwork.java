package com.claimblocks.net;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Canal opcional para los contornos de zona. Si el cliente no tiene el mod
 * simplemente no se le envia nada, asi que los clientes vanilla siguen funcionando.
 */
public final class ClaimNetwork {
   private ClaimNetwork() {
   }

   public static void init() {
      PayloadTypeRegistry.playS2C().register(ClaimBordersPayload.ID, ClaimBordersPayload.CODEC);
   }

   public static boolean canSend(ServerPlayerEntity player) {
      return ServerPlayNetworking.canSend(player, ClaimBordersPayload.ID);
   }

   public static void sendTo(ServerPlayerEntity player, ClaimBordersPayload payload) {
      if (canSend(player)) {
         ServerPlayNetworking.send(player, payload);
      }
   }
}
