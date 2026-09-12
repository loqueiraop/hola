package com.claimblocks.render;

import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimConfig;
import com.claimblocks.data.ClaimManager;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.UUID;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import org.joml.Vector3f;

/**
 * Dibuja el area de la zona con particulas. Es 100% server-side, asi que
 * funciona con clientes vanilla sin instalar nada.
 */
public final class ParticleBorder {
   private static final int RENDER_DISTANCE = 24;

   private ParticleBorder() {
   }

   public static SimpleParticleType particleFor(String id) {
      if (id != null && !id.isEmpty()) {
         String full = id.contains(":") ? id : legacyToId(id);
         Identifier parsed = Identifier.tryParse(full);
         if (parsed != null) {
            ParticleType<?> type = Registries.PARTICLE_TYPE.get(parsed);
            if (type instanceof SimpleParticleType simple) {
               return simple;
            }
         }

         return ParticleTypes.HAPPY_VILLAGER;
      } else {
         return ParticleTypes.HAPPY_VILLAGER;
      }
   }

   /** Compatibilidad con los ids cortos que guardaban las versiones antiguas. */
   private static String legacyToId(String s) {
      return switch (s) {
         case "flame" -> "minecraft:flame";
         case "soul" -> "minecraft:soul";
         case "heart" -> "minecraft:heart";
         case "end_rod" -> "minecraft:end_rod";
         case "crit" -> "minecraft:crit";
         case "enchant" -> "minecraft:enchant";
         case "dragon" -> "minecraft:dragon_breath";
         case "portal" -> "minecraft:portal";
         case "cloud" -> "minecraft:cloud";
         case "spark" -> "minecraft:electric_spark";
         case "wax" -> "minecraft:wax_on";
         default -> "minecraft:happy_villager";
      };
   }

   public static String[] availableParticles() {
      return new String[]{
         "minecraft:happy_villager",
         "minecraft:heart",
         "minecraft:flame",
         "minecraft:small_flame",
         "minecraft:soul_fire_flame",
         "minecraft:soul",
         "minecraft:end_rod",
         "minecraft:crit",
         "minecraft:enchanted_hit",
         "minecraft:enchant",
         "minecraft:dragon_breath",
         "minecraft:portal",
         "minecraft:reverse_portal",
         "minecraft:cloud",
         "minecraft:electric_spark",
         "minecraft:wax_on",
         "minecraft:glow",
         "minecraft:totem_of_undying",
         "minecraft:firework",
         "minecraft:note",
         "minecraft:snowflake",
         "minecraft:cherry_leaves",
         "minecraft:spore_blossom_air",
         "minecraft:sculk_soul",
         "minecraft:lava",
         "minecraft:splash",
         "minecraft:witch"
      };
   }

   public static String particleLabel(String s) {
      if (s == null) {
         return "Aldeano feliz";
      } else {
         return switch (s) {
            case "minecraft:happy_villager", "happy" -> "Aldeano feliz";
            case "minecraft:heart", "heart" -> "Corazones";
            case "minecraft:flame", "flame" -> "Llamas";
            case "minecraft:small_flame" -> "Llama pequeña";
            case "minecraft:soul_fire_flame" -> "Fuego del alma";
            case "minecraft:soul", "soul" -> "Almas";
            case "minecraft:end_rod", "end_rod" -> "Vara del End";
            case "minecraft:crit", "crit" -> "Críticos";
            case "minecraft:enchanted_hit" -> "Golpe encantado";
            case "minecraft:enchant", "enchant" -> "Encantamiento";
            case "minecraft:dragon_breath", "dragon" -> "Aliento de dragón";
            case "minecraft:portal", "portal" -> "Portal";
            case "minecraft:reverse_portal" -> "Portal inverso";
            case "minecraft:cloud", "cloud" -> "Nube";
            case "minecraft:electric_spark", "spark" -> "Chispa eléctrica";
            case "minecraft:wax_on", "wax" -> "Cera brillante";
            case "minecraft:glow" -> "Brillo (glow)";
            case "minecraft:totem_of_undying" -> "Tótem";
            case "minecraft:firework" -> "Fuegos artificiales";
            case "minecraft:note" -> "Nota musical";
            case "minecraft:snowflake" -> "Copo de nieve";
            case "minecraft:cherry_leaves" -> "Pétalos de cerezo";
            case "minecraft:spore_blossom_air" -> "Esporas";
            case "minecraft:sculk_soul" -> "Alma de sculk";
            case "minecraft:lava" -> "Lava";
            case "minecraft:splash" -> "Salpicadura";
            case "minecraft:witch" -> "Bruja";
            default -> s.contains(":") ? s.substring(s.indexOf(58) + 1) : s;
         };
      }
   }

   /** Rellena la parte de la zona que el jugador tiene cerca con particulas aleatorias. */
   public static void fillClaim(ServerWorld world, ServerPlayerEntity player, Claim claim) {
      SimpleParticleType particle = particleFor(claim.getFlags().borderParticle);
      int density = Math.max(1, Math.min(200, claim.getFlags().particleDensity));
      int radius = claim.getRadius();
      int height = claim.getHeight();
      double minX = (double)(claim.getX() - radius);
      double maxX = (double)(claim.getX() + radius + 1);
      double minZ = (double)(claim.getZ() - radius);
      double maxZ = (double)(claim.getZ() + radius + 1);
      double minY = (double)(claim.getY() - height);
      double maxY = (double)(claim.getY() + height + 1);
      double x0 = Math.max(minX, player.getX() - (double)RENDER_DISTANCE);
      double x1 = Math.min(maxX, player.getX() + (double)RENDER_DISTANCE);
      double z0 = Math.max(minZ, player.getZ() - (double)RENDER_DISTANCE);
      double z1 = Math.min(maxZ, player.getZ() + (double)RENDER_DISTANCE);
      double y0 = Math.max(minY, player.getY() - (double)RENDER_DISTANCE);
      double y1 = Math.min(maxY, player.getY() + (double)RENDER_DISTANCE);
      if (!(x0 > x1) && !(z0 > z1) && !(y0 > y1)) {
         Random random = world.getRandom();

         for (int i = 0; i < density; i++) {
            double px = x0 + random.nextDouble() * (x1 - x0);
            double py = y0 + random.nextDouble() * (y1 - y0);
            double pz = z0 + random.nextDouble() * (z1 - z0);
            world.spawnParticles(player, particle, true, px, py, pz, 1, 0.0, 0.0, 0.0, 0.0);
         }
      }
   }

   public static boolean withinRenderRange(ServerPlayerEntity player, Claim claim) {
      double dx = Math.max(0.0, Math.abs(player.getX() - ((double)claim.getX() + 0.5)) - (double)claim.getRadius());
      double dz = Math.max(0.0, Math.abs(player.getZ() - ((double)claim.getZ() + 0.5)) - (double)claim.getRadius());
      double max = (double)ClaimConfig.get().particleRenderDistance;
      return dx <= max && dz <= max;
   }

   // =====================================================================
   //  Contorno con polvo de colores (particula vanilla minecraft:dust).
   //  Es 100% server-side: el cliente no necesita el mod para verlo.
   // =====================================================================

   private static final double OUTLINE_STEP = 0.5;

   /** Polvo del color del tier de la zona. */
   private static DustParticleEffect dustFor(Claim claim) {
      float r = 1.0F;
      float g = 1.0F;
      float b = 1.0F;
      if (claim.getTier() != null) {
         r = claim.getTier().r;
         g = claim.getTier().g;
         b = claim.getTier().b;
      }

      return new DustParticleEffect(new Vector3f(r, g, b), 1.0F);
   }

   /** Dibuja el contorno en aristas de una zona suelta. */
   public static void drawOutline(ServerWorld world, ServerPlayerEntity player, Claim claim) {
      int radius = claim.getRadius();
      int height = claim.getHeight();
      double minX = (double)(claim.getX() - radius);
      double maxX = (double)(claim.getX() + radius + 1);
      double minZ = (double)(claim.getZ() - radius);
      double maxZ = (double)(claim.getZ() + radius + 1);
      double minY = (double)(claim.getY() - height);
      double maxY = (double)(claim.getY() + height + 1);
      drawWireframe(world, player, dustFor(claim), minX, minY, minZ, maxX, maxY, maxZ);
   }

   private static void drawWireframe(
      ServerWorld world, ServerPlayerEntity player, DustParticleEffect dust, double minX, double minY, double minZ, double maxX, double maxY, double maxZ
   ) {
      // 4 aristas horizontales en X (arriba y abajo)
      lineX(world, player, dust, minX, maxX, minY, minZ);
      lineX(world, player, dust, minX, maxX, minY, maxZ);
      lineX(world, player, dust, minX, maxX, maxY, minZ);
      lineX(world, player, dust, minX, maxX, maxY, maxZ);
      // 4 aristas horizontales en Z
      lineZ(world, player, dust, minZ, maxZ, minY, minX);
      lineZ(world, player, dust, minZ, maxZ, minY, maxX);
      lineZ(world, player, dust, minZ, maxZ, maxY, minX);
      lineZ(world, player, dust, minZ, maxZ, maxY, maxX);
      // 4 pilares verticales en las esquinas
      lineY(world, player, dust, minY, maxY, minX, minZ);
      lineY(world, player, dust, minY, maxY, minX, maxZ);
      lineY(world, player, dust, minY, maxY, maxX, minZ);
      lineY(world, player, dust, minY, maxY, maxX, maxZ);
   }

   /**
    * Recorta el tramo de arista al entorno del jugador antes de recorrerlo, para que
    * una zona de 500x500 no genere miles de particulas.
    */
   private static double startAt(double from, double limit) {
      double aligned = Math.ceil(from / OUTLINE_STEP) * OUTLINE_STEP;
      return Math.max(aligned, Math.ceil(limit / OUTLINE_STEP) * OUTLINE_STEP);
   }

   private static void lineX(ServerWorld world, ServerPlayerEntity player, DustParticleEffect dust, double xa, double xb, double y, double z) {
      double max = (double)ClaimConfig.get().particleRenderDistance;
      if (!(Math.abs(player.getY() - y) > max) && !(Math.abs(player.getZ() - z) > max)) {
         double to = Math.min(xb, player.getX() + max);

         for (double x = startAt(xa, player.getX() - max); x <= to; x += OUTLINE_STEP) {
            world.spawnParticles(player, dust, true, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
         }
      }
   }

   private static void lineZ(ServerWorld world, ServerPlayerEntity player, DustParticleEffect dust, double za, double zb, double y, double x) {
      double max = (double)ClaimConfig.get().particleRenderDistance;
      if (!(Math.abs(player.getY() - y) > max) && !(Math.abs(player.getX() - x) > max)) {
         double to = Math.min(zb, player.getZ() + max);

         for (double z = startAt(za, player.getZ() - max); z <= to; z += OUTLINE_STEP) {
            world.spawnParticles(player, dust, true, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
         }
      }
   }

   private static void lineY(ServerWorld world, ServerPlayerEntity player, DustParticleEffect dust, double ya, double yb, double x, double z) {
      double max = (double)ClaimConfig.get().particleRenderDistance;
      if (!(Math.abs(player.getX() - x) > max) && !(Math.abs(player.getZ() - z) > max)) {
         double to = Math.min(yb, player.getY() + max);

         for (double y = startAt(ya, player.getY() - max); y <= to; y += OUTLINE_STEP) {
            world.spawnParticles(player, dust, true, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
         }
      }
   }

   /**
    * Dibuja el perimetro exterior de un grupo de zonas: se recortan las paredes
    * interiores para que el conjunto se vea como una sola region.
    */
   public static void drawGroupOutline(ServerWorld world, ServerPlayerEntity player, UUID groupId, String dim) {
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
            DustParticleEffect dust = dustFor(mother);
            int count = members.size();
            int[] x0 = new int[count];
            int[] x1 = new int[count];
            int[] z0 = new int[count];
            int[] z1 = new int[count];
            TreeSet<Integer> xsSet = new TreeSet<>();
            TreeSet<Integer> zsSet = new TreeSet<>();

            for (int i = 0; i < count; i++) {
               Claim c = members.get(i);
               int radius = c.getRadius();
               x0[i] = c.getX() - radius;
               x1[i] = c.getX() + radius + 1;
               z0[i] = c.getZ() - radius;
               z1[i] = c.getZ() + radius + 1;
               xsSet.add(x0[i]);
               xsSet.add(x1[i]);
               zsSet.add(z0[i]);
               zsSet.add(z1[i]);
            }

            Integer[] gridX = xsSet.toArray(new Integer[0]);
            Integer[] gridZ = zsSet.toArray(new Integer[0]);
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

               // paredes perpendiculares a X (recorren Z)
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

                        double wx = (double)gridX[i].intValue();
                        double za = (double)gridZ[startJ].intValue();
                        double zb = (double)gridZ[j].intValue();
                        lineZ(world, player, dust, za, zb, minY, wx);
                        lineZ(world, player, dust, za, zb, maxY, wx);
                        lineY(world, player, dust, minY, maxY, wx, za);
                        lineY(world, player, dust, minY, maxY, wx, zb);
                     } else {
                        j++;
                     }
                  }
               }

               // paredes perpendiculares a Z (recorren X)
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

                        double wz = (double)gridZ[j].intValue();
                        double xa = (double)gridX[startI].intValue();
                        double xb = (double)gridX[i].intValue();
                        lineX(world, player, dust, xa, xb, minY, wz);
                        lineX(world, player, dust, xa, xb, maxY, wz);
                        lineY(world, player, dust, minY, maxY, xa, wz);
                        lineY(world, player, dust, minY, maxY, xb, wz);
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
