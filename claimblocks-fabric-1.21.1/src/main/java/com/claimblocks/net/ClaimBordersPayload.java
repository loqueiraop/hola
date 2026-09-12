package com.claimblocks.net;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Contornos de zona que el servidor envia al cliente para dibujarlos.
 * Cada caja es {minX, minY, minZ, maxX, maxY, maxZ, r, g, b}.
 */
public record ClaimBordersPayload(List<double[]> boxes) implements CustomPayload {
   public static final CustomPayload.Id<ClaimBordersPayload> ID = new CustomPayload.Id<>(Identifier.of("claimblocks", "borders"));
   public static final PacketCodec<PacketByteBuf, ClaimBordersPayload> CODEC = CustomPayload.codecOf(
      ClaimBordersPayload::write, ClaimBordersPayload::new
   );

   public ClaimBordersPayload(PacketByteBuf buf) {
      this(read(buf));
   }

   private static List<double[]> read(PacketByteBuf buf) {
      int count = buf.readVarInt();
      List<double[]> out = new ArrayList<>(Math.max(0, Math.min(count, 4096)));

      for (int i = 0; i < count; i++) {
         out.add(
            new double[]{
               buf.readDouble(),
               buf.readDouble(),
               buf.readDouble(),
               buf.readDouble(),
               buf.readDouble(),
               buf.readDouble(),
               (double)buf.readFloat(),
               (double)buf.readFloat(),
               (double)buf.readFloat()
            }
         );
      }

      return out;
   }

   private void write(PacketByteBuf buf) {
      buf.writeVarInt(this.boxes.size());

      for (double[] box : this.boxes) {
         buf.writeDouble(box[0]);
         buf.writeDouble(box[1]);
         buf.writeDouble(box[2]);
         buf.writeDouble(box[3]);
         buf.writeDouble(box[4]);
         buf.writeDouble(box[5]);
         buf.writeFloat((float)box[6]);
         buf.writeFloat((float)box[7]);
         buf.writeFloat((float)box[8]);
      }
   }

   public CustomPayload.Id<? extends CustomPayload> getId() {
      return ID;
   }
}
