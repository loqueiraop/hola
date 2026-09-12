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
   //
   //  Por cada pared de la zona se dibuja:
   //    - una linea a la altura del jugador, para que el borde se vea siempre
   //      aunque la zona sea enorme
   //    - las aristas de arriba y abajo, si caen dentro del alcance
   //    - una pared parpadeante cuando te acercas al limite
   // =====================================================================

   private static final double LINE_STEP = 1.0;
   private static final double WALL_SPAN = 8.0;
   private static final double WALL_STEP = 1.0;

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

   private static double clamp(double value, double lo, double hi) {
      return value < lo ? lo : (value > hi ? hi : value);
   }

   private static double alignUp(double value) {
      return Math.ceil(value / LINE_STEP) * LINE_STEP;
   }

   /** Parpadeo: mas rapido cuanto mas cerca estas del limite. */
   private static boolean blinkOn(ServerWorld world, double distance) {
      ClaimConfig config = ClaimConfig.get();
      if (!config.borderWallBlink) {
         return true;
      } else {
         long period = distance <= config.borderWallDistance / 2.0 ? 3L : 6L;
         return world.getTime() / period % 2L == 0L;
      }
   }

   public static void drawOutline(ServerWorld world, ServerPlayerEntity player, Claim claim) {
      int radius = claim.getRadius();
      int height = claim.getHeight();
      double minX = (double)(claim.getX() - radius);
      double maxX = (double)(claim.getX() + radius + 1);
      double minZ = (double)(claim.getZ() - radius);
      double maxZ = (double)(claim.getZ() + radius + 1);
      double minY = (double)(claim.getY() - height);
      double maxY = (double)(claim.getY() + height + 1);
      DustParticleEffect dust = dustFor(claim);
      wallAlongZ(world, player, dust, minX, minZ, maxZ, minY, maxY);
      wallAlongZ(world, player, dust, maxX, minZ, maxZ, minY, maxY);
      wallAlongX(world, player, dust, minZ, minX, maxX, minY, maxY);
      wallAlongX(world, player, dust, maxZ, minX, maxX, minY, maxY);
      corner(world, player, dust, minX, minZ, minY, maxY);
      corner(world, player, dust, minX, maxZ, minY, maxY);
      corner(world, player, dust, maxX, minZ, minY, maxY);
      corner(world, player, dust, maxX, maxZ, minY, maxY);
   }

   /** Pared en el plano x = wx, que recorre Z entre za y zb. */
   private static void wallAlongZ(
      ServerWorld world, ServerPlayerEntity player, DustParticleEffect dust, double wx, double za, double zb, double minY, double maxY
   ) {
      ClaimConfig config = ClaimConfig.get();
      double reach = (double)config.particleRenderDistance;
      double gap = Math.abs(player.getX() - wx);
      if (!(gap > reach)) {
         double from = Math.max(za, alignUp(player.getZ() - reach));
         double to = Math.min(zb, player.getZ() + reach);
         if (!(from > to)) {
            double levelY = clamp(player.getY(), minY, maxY);
            lineZ(world, player, dust, from, to, levelY, wx);
            if (Math.abs(player.getY() - minY) <= reach) {
               lineZ(world, player, dust, from, to, minY, wx);
            }

            if (Math.abs(player.getY() - maxY) <= reach) {
               lineZ(world, player, dust, from, to, maxY, wx);
            }

            if (gap <= (double)config.borderWallDistance && blinkOn(world, gap)) {
               double wFrom = Math.max(za, alignUp(player.getZ() - WALL_SPAN));
               double wTo = Math.min(zb, player.getZ() + WALL_SPAN);
               double yFrom = Math.max(minY, alignUp(player.getY() - 2.0));
               double yTo = Math.min(maxY, player.getY() + 3.0);

               for (double z = wFrom; z <= wTo; z += WALL_STEP) {
                  for (double y = yFrom; y <= yTo; y += WALL_STEP) {
                     world.spawnParticles(player, dust, true, wx, y, z, 1, 0.0, 0.0, 0.0, 0.0);
                  }
               }
            }
         }
      }
   }

   /** Pared en el plano z = wz, que recorre X entre xa y xb. */
   private static void wallAlongX(
      ServerWorld world, ServerPlayerEntity player, DustParticleEffect dust, double wz, double xa, double xb, double minY, double maxY
   ) {
      ClaimConfig config = ClaimConfig.get();
      double reach = (double)config.particleRenderDistance;
      double gap = Math.abs(player.getZ() - wz);
      if (!(gap > reach)) {
         double from = Math.max(xa, alignUp(player.getX() - reach));
         double to = Math.min(xb, player.getX() + reach);
         if (!(from > to)) {
            double levelY = clamp(player.getY(), minY, maxY);
            lineX(world, player, dust, from, to, levelY, wz);
            if (Math.abs(player.getY() - minY) <= reach) {
               lineX(world, player, dust, from, to, minY, wz);
            }

            if (Math.abs(player.getY() - maxY) <= reach) {
               lineX(world, player, dust, from, to, maxY, wz);
            }

            if (gap <= (double)config.borderWallDistance && blinkOn(world, gap)) {
               double wFrom = Math.max(xa, alignUp(player.getX() - WALL_SPAN));
               double wTo = Math.min(xb, player.getX() + WALL_SPAN);
               double yFrom = Math.max(minY, alignUp(player.getY() - 2.0));
               double yTo = Math.min(maxY, player.getY() + 3.0);

               for (double x = wFrom; x <= wTo; x += WALL_STEP) {
                  for (double y = yFrom; y <= yTo; y += WALL_STEP) {
                     world.spawnParticles(player, dust, true, x, y, wz, 1, 0.0, 0.0, 0.0, 0.0);
                  }
               }
            }
         }
      }
   }

   /** Pilar vertical de esquina, recortado al alcance del jugador. */
   private static void corner(ServerWorld world, ServerPlayerEntity player, DustParticleEffect dust, double x, double z, double minY, double maxY) {
      double reach = (double)ClaimConfig.get().particleRenderDistance;
      if (!(Math.abs(player.getX() - x) > reach) && !(Math.abs(player.getZ() - z) > reach)) {
         double from = Math.max(minY, alignUp(player.getY() - reach));
         double to = Math.min(maxY, player.getY() + reach);

         for (double y = from; y <= to; y += LINE_STEP) {
            world.spawnParticles(player, dust, true, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
         }
      }
   }

   private static void lineX(ServerWorld world, ServerPlayerEntity player, DustParticleEffect dust, double from, double to, double y, double z) {
      for (double x = from; x <= to; x += LINE_STEP) {
         world.spawnParticles(player, dust, true, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
      }
   }

   private static void lineZ(ServerWorld world, ServerPlayerEntity player, DustParticleEffect dust, double from, double to, double y, double x) {
      for (double z = from; z <= to; z += LINE_STEP) {
         world.spawnParticles(player, dust, true, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
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
                        wallAlongZ(world, player, dust, wx, za, zb, minY, maxY);
                        corner(world, player, dust, wx, za, minY, maxY);
                        corner(world, player, dust, wx, zb, minY, maxY);
                     } else {
                        j++;
                     }
                  }
               }

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
                        wallAlongX(world, player, dust, wz, xa, xb, minY, maxY);
                        corner(world, player, dust, xa, wz, minY, maxY);
                        corner(world, player, dust, xb, wz, minY, maxY);
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
