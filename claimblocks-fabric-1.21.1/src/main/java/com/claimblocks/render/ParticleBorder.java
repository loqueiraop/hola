package com.claimblocks.render;

import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimConfig;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

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
}
