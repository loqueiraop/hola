package com.claimblocks.client;

import com.claimblocks.ClaimBlocks;
import com.claimblocks.data.ClaimTier;
import com.claimblocks.net.ClaimBordersPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Parte cliente opcional: dibuja el contorno de las zonas que envia el servidor
 * y una vista previa del area al tener una piedra de proteccion en la mano.
 */
public class ClaimBlocksClient implements ClientModInitializer {
   public void onInitializeClient() {
      ClientPlayNetworking.registerGlobalReceiver(
         ClaimBordersPayload.ID, (payload, context) -> context.client().execute(() -> ClientBorderStore.receive(payload.boxes()))
      );
      ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientBorderStore.clear());
      WorldRenderEvents.AFTER_ENTITIES.register(context -> {
         MinecraftClient client = MinecraftClient.getInstance();
         ClientPlayerEntity player = client.player;
         if (player != null && client.world != null) {
            VertexConsumerProvider consumers = context.consumers();
            if (consumers != null) {
               Vec3d camera = context.camera().getPos();
               MatrixStack matrices = context.matrixStack();
               if (matrices != null) {
                  VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());
                  matrices.push();
                  matrices.translate(-camera.x, -camera.y, -camera.z);

                  for (double[] box : ClientBorderStore.current()) {
                     WorldRenderer.drawBox(
                        matrices, lines, box[0], box[1], box[2], box[3], box[4], box[5], (float)box[6], (float)box[7], (float)box[8], 0.9F
                     );
                  }

                  ClaimTier tier = ClaimBlocks.readTier(player.getMainHandStack());
                  if (tier == null) {
                     tier = ClaimBlocks.readTier(player.getOffHandStack());
                  }

                  if (tier != null) {
                     BlockPos pos = player.getBlockPos();
                     WorldRenderer.drawBox(
                        matrices,
                        lines,
                        (double)(pos.getX() - tier.radius),
                        (double)(pos.getY() - tier.height),
                        (double)(pos.getZ() - tier.radius),
                        (double)(pos.getX() + tier.radius + 1),
                        (double)(pos.getY() + tier.height + 1),
                        (double)(pos.getZ() + tier.radius + 1),
                        tier.r,
                        tier.g,
                        tier.b,
                        1.0F
                     );
                  }

                  matrices.pop();
               }
            }
         }
      });
   }
}
