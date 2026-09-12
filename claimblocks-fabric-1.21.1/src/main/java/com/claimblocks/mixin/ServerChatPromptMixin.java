package com.claimblocks.mixin;

import com.claimblocks.ClaimBlocksMod;
import com.claimblocks.chat.ChatPromptRouter;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Captura la respuesta de los menus a nivel de paquete. Asi funciona aunque un plugin
 * de chat consuma el mensaje antes de que llegue al evento normal.
 */
@Mixin({ServerPlayNetworkHandler.class})
public abstract class ServerChatPromptMixin {
   @Inject(
      method = {"onChatMessage(Lnet/minecraft/network/packet/c2s/play/ChatMessageC2SPacket;)V"},
      at = {@At("HEAD")},
      require = 0
   )
   private void claimblocks$captureMenuPrompt(ChatMessageC2SPacket packet, CallbackInfo ci) {
      try {
         ServerPlayNetworkHandler self = (ServerPlayNetworkHandler)(Object)this;
         ServerPlayerEntity player = self.getPlayer();
         if (player == null || !ChatPromptRouter.hasPending(player.getUuid())) {
            return;
         }

         ChatPromptRouter.markPacketCaptureActive();
         ChatPromptRouter.consume(player, packet.chatMessage());
      } catch (Throwable t) {
         ClaimBlocksMod.LOGGER.error("[Tierrasmon Claims] Fallo capturando la respuesta del menu", t);
      }
   }

   @Inject(
      method = {"handleDecoratedMessage(Lnet/minecraft/network/message/SignedMessage;)V"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void claimblocks$hideConsumedPrompt(SignedMessage message, CallbackInfo ci) {
      try {
         ServerPlayNetworkHandler self = (ServerPlayNetworkHandler)(Object)this;
         ServerPlayerEntity player = self.getPlayer();
         if (player == null) {
            return;
         }

         if (ChatPromptRouter.shouldSuppress(player.getUuid(), message.getSignedContent())) {
            ci.cancel();
         }
      } catch (Throwable t) {
         ClaimBlocksMod.LOGGER.error("[Tierrasmon Claims] Fallo ocultando la respuesta del menu", t);
      }
   }
}
