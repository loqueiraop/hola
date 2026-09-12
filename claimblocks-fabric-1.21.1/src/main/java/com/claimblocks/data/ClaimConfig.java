package com.claimblocks.data;

import com.claimblocks.ClaimBlocksMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

/**
 * Configuracion del servidor, en claimblocks_config.json dentro de la carpeta del mundo.
 * Se recarga en caliente con /tmclaimsadmin reload.
 */
public final class ClaimConfig {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final String FILE = "claimblocks_config.json";
   private static final ClaimConfig INSTANCE = new ClaimConfig();

   public int maxClaimsPerPlayer = 0;
   public int maxMembersPerClaim = 0;
   public boolean protectHoppers = true;
   public boolean protectFluids = true;
   public boolean protectDecoration = true;
   public boolean protectDecorationFromExplosions = true;
   public boolean banTeleportOut = true;
   public float banDamage = 0.0F;
   public int banNoticeSeconds = 2;
   public int trespasserAlertSeconds = 30;
   public int chatPromptSeconds = 90;
   public int maxWelcomeLength = 60;
   public int particleIntervalTicks = 4;
   public int borderIntervalTicks = 10;
   public int particleRenderDistance = 24;
   public int borderWallDistance = 5;
   public boolean borderWallBlink = true;
   public int fireSweepIntervalTicks = 40;
   public int fireSweepRadius = 6;
   public int passiveEffectIntervalTicks = 40;
   public int hostileBurnSeconds = 3;
   public float hostileDamage = 3.0F;
   public int effectDurationTicks = 60;
   public String defaultParticle = "minecraft:happy_villager";
   public int defaultParticleDensity = 10;
   public final Map<ClaimFlags.FlagId, Boolean> defaultFlags = new EnumMap<>(ClaimFlags.FlagId.class);

   private Path file;

   private ClaimConfig() {
      this.resetDefaultFlags();
   }

   public static ClaimConfig get() {
      return INSTANCE;
   }

   private void resetDefaultFlags() {
      ClaimFlags base = new ClaimFlags();
      this.defaultFlags.clear();

      for (ClaimFlags.FlagId id : ClaimFlags.FlagId.values()) {
         this.defaultFlags.put(id, base.get(id));
      }
   }

   public void load(MinecraftServer server) {
      if (server != null) {
         this.file = server.getSavePath(WorldSavePath.ROOT).resolve(FILE);
         this.reload();
      }
   }

   public boolean reload() {
      if (this.file == null) {
         return false;
      } else {
         JsonObject root = new JsonObject();

         try {
            if (Files.exists(this.file)) {
               String text = Files.readString(this.file, StandardCharsets.UTF_8);
               if (!text.isBlank()) {
                  JsonElement el = JsonParser.parseString(text);
                  if (el.isJsonObject()) {
                     root = el.getAsJsonObject();
                  }
               }
            }
         } catch (Exception e) {
            ClaimBlocksMod.LOGGER.error("[Tierrasmon Claims] No se pudo leer " + this.file + "; se usan los valores por defecto", e);
         }

         this.readFrom(root);
         this.write();
         return true;
      }
   }

   private void readFrom(JsonObject root) {
      JsonObject limites = section(root, "limites");
      int legacyMax = root.has("maxClaimsPerPlayer") ? root.get("maxClaimsPerPlayer").getAsInt() : this.maxClaimsPerPlayer;
      this.maxClaimsPerPlayer = Math.max(0, readInt(limites, "maxZonasPorJugador", legacyMax));
      this.maxMembersPerClaim = Math.max(0, readInt(limites, "maxMiembrosPorZona", this.maxMembersPerClaim));

      JsonObject prot = section(root, "protecciones");
      this.protectHoppers = readBool(prot, "tolvasNoSacanItemsDeLaZona", this.protectHoppers);
      this.protectFluids = readBool(prot, "aguaYLavaNoEntranDesdeFuera", this.protectFluids);
      this.protectDecoration = readBool(prot, "cuadrosMarcosYSoportes", this.protectDecoration);
      this.protectDecorationFromExplosions = readBool(prot, "cuadrosResistenExplosiones", this.protectDecorationFromExplosions);

      JsonObject ban = section(root, "baneados");
      this.banTeleportOut = readBool(ban, "expulsarPorTeletransporte", this.banTeleportOut);
      this.banDamage = Math.max(0.0F, readFloat(ban, "danoAlEntrar", this.banDamage));
      this.banNoticeSeconds = Math.max(0, readInt(ban, "segundosEntreAvisos", this.banNoticeSeconds));

      JsonObject avisos = section(root, "avisos");
      this.trespasserAlertSeconds = Math.max(0, readInt(avisos, "segundosEntreAvisosDeIntruso", this.trespasserAlertSeconds));
      this.chatPromptSeconds = Math.max(5, readInt(avisos, "segundosParaResponderEnChat", this.chatPromptSeconds));
      this.maxWelcomeLength = Math.max(10, readInt(avisos, "maxCaracteresDeLosMensajes", this.maxWelcomeLength));

      JsonObject perf = section(root, "rendimiento");
      this.particleIntervalTicks = Math.max(1, readInt(perf, "ticksEntreParticulas", this.particleIntervalTicks));
      this.borderIntervalTicks = Math.max(1, readInt(perf, "ticksEntreActualizacionDeBordes", this.borderIntervalTicks));
      this.particleRenderDistance = Math.max(1, readInt(perf, "distanciaParaVerParticulas", this.particleRenderDistance));
      this.borderWallDistance = Math.max(0, readInt(perf, "distanciaDeLaParedDeAviso", this.borderWallDistance));
      this.borderWallBlink = readBool(perf, "paredDeAvisoParpadea", this.borderWallBlink);
      this.fireSweepIntervalTicks = Math.max(1, readInt(perf, "ticksEntreBarridoDeFuego", this.fireSweepIntervalTicks));
      this.fireSweepRadius = Math.max(0, readInt(perf, "radioDeBarridoDeFuego", this.fireSweepRadius));
      this.passiveEffectIntervalTicks = Math.max(1, readInt(perf, "ticksEntreEfectosPasivos", this.passiveEffectIntervalTicks));

      JsonObject hostiles = section(root, "barreraDeHostiles");
      this.hostileBurnSeconds = Math.max(0, readInt(hostiles, "segundosDeFuego", this.hostileBurnSeconds));
      this.hostileDamage = Math.max(0.0F, readFloat(hostiles, "dano", this.hostileDamage));

      JsonObject efectos = section(root, "efectosPasivos");
      this.effectDurationTicks = Math.max(20, readInt(efectos, "duracionEnTicks", this.effectDurationTicks));

      JsonObject nuevas = section(root, "zonasNuevas");
      this.defaultParticle = readString(nuevas, "particula", this.defaultParticle);
      this.defaultParticleDensity = Math.max(1, readInt(nuevas, "densidadDeParticulas", this.defaultParticleDensity));
      JsonObject flags = section(nuevas, "flags");

      for (ClaimFlags.FlagId id : ClaimFlags.FlagId.values()) {
         boolean fallback = this.defaultFlags.getOrDefault(id, Boolean.FALSE);
         this.defaultFlags.put(id, readBool(flags, id.name(), fallback));
      }
   }

   private void write() {
      JsonObject root = new JsonObject();
      root.add(
         "_ayuda",
         doc(
            "Tierrasmon Claims - configuracion del servidor.",
            "Recarga en caliente con /tmclaimsadmin reload (no hace falta reiniciar).",
            "Si borras una clave se rellena con su valor por defecto al recargar.",
            "Los cambios de 'zonasNuevas' solo afectan a las zonas que se creen a partir de ahora."
         )
      );

      JsonObject limites = new JsonObject();
      limites.add("_doc", doc("maxZonasPorJugador: 0 = sin limite. No se aplica a operadores.", "maxMiembrosPorZona: 0 = sin limite."));
      limites.addProperty("maxZonasPorJugador", this.maxClaimsPerPlayer);
      limites.addProperty("maxMiembrosPorZona", this.maxMembersPerClaim);
      root.add("limites", limites);

      JsonObject prot = new JsonObject();
      prot.add(
         "_doc",
         doc(
            "Protecciones que actuan sin que nadie pise la zona. Apagalas solo si te chocan con otro mod.",
            "tolvasNoSacanItemsDeLaZona: impide que una tolva o vagoneta-tolva vacie cofres desde fuera del borde.",
            "aguaYLavaNoEntranDesdeFuera: bloquea el flujo que cruza hacia dentro (el flujo interno no se toca).",
            "cuadrosMarcosYSoportes: protege cuadros, marcos y soportes de armadura de flechas, mobs y golpes.",
            "cuadrosResistenExplosiones: saca la decoracion de la lista de afectados por TNT y creepers."
         )
      );
      prot.addProperty("tolvasNoSacanItemsDeLaZona", this.protectHoppers);
      prot.addProperty("aguaYLavaNoEntranDesdeFuera", this.protectFluids);
      prot.addProperty("cuadrosMarcosYSoportes", this.protectDecoration);
      prot.addProperty("cuadrosResistenExplosiones", this.protectDecorationFromExplosions);
      root.add("protecciones", prot);

      JsonObject ban = new JsonObject();
      ban.add(
         "_doc",
         doc(
            "Que le pasa a un jugador baneado de una zona cuando entra.",
            "expulsarPorTeletransporte: true lo saca al borde mas cercano; false solo lo empuja.",
            "danoAlEntrar: 0 = sin dano. Ponlo alto solo si quieres que sea letal.",
            "segundosEntreAvisos: cada cuanto se le repite el mensaje."
         )
      );
      ban.addProperty("expulsarPorTeletransporte", this.banTeleportOut);
      ban.addProperty("danoAlEntrar", this.banDamage);
      ban.addProperty("segundosEntreAvisos", this.banNoticeSeconds);
      root.add("baneados", ban);

      JsonObject avisos = new JsonObject();
      avisos.add(
         "_doc",
         doc(
            "segundosEntreAvisosDeIntruso: antiespam del aviso al dueno cuando entra alguien.",
            "segundosParaResponderEnChat: tiempo para escribir un nombre cuando el menu lo pide.",
            "maxCaracteresDeLosMensajes: limite del mensaje de bienvenida y de salida."
         )
      );
      avisos.addProperty("segundosEntreAvisosDeIntruso", this.trespasserAlertSeconds);
      avisos.addProperty("segundosParaResponderEnChat", this.chatPromptSeconds);
      avisos.addProperty("maxCaracteresDeLosMensajes", this.maxWelcomeLength);
      root.add("avisos", avisos);

      JsonObject perf = new JsonObject();
      perf.add(
         "_doc",
         doc(
            "Sube los intervalos para gastar menos CPU y ancho de banda (20 ticks = 1 segundo).",
            "ticksEntreParticulas: cada cuanto se dibujan las particulas del area.",
            "ticksEntreActualizacionDeBordes: cada cuanto se redibuja el contorno de polvo.",
            "distanciaParaVerParticulas: a cuantos bloques del borde se empiezan a ver.",
            "distanciaDeLaParedDeAviso: a que distancia del limite aparece la pared de particulas. 0 la desactiva.",
            "paredDeAvisoParpadea: la pared parpadea, y mas rapido cuanto mas cerca estas del limite.",
            "ticksEntreBarridoDeFuego y radioDeBarridoDeFuego: apagado de fuego dentro de la zona.",
            "ticksEntreEfectosPasivos: cada cuanto se reaplican regeneracion, resistencia y velocidad."
         )
      );
      perf.addProperty("ticksEntreParticulas", this.particleIntervalTicks);
      perf.addProperty("ticksEntreActualizacionDeBordes", this.borderIntervalTicks);
      perf.addProperty("distanciaParaVerParticulas", this.particleRenderDistance);
      perf.addProperty("distanciaDeLaParedDeAviso", this.borderWallDistance);
      perf.addProperty("paredDeAvisoParpadea", this.borderWallBlink);
      perf.addProperty("ticksEntreBarridoDeFuego", this.fireSweepIntervalTicks);
      perf.addProperty("radioDeBarridoDeFuego", this.fireSweepRadius);
      perf.addProperty("ticksEntreEfectosPasivos", this.passiveEffectIntervalTicks);
      root.add("rendimiento", perf);

      JsonObject hostiles = new JsonObject();
      hostiles.add(
         "_doc",
         doc("Flag BURN_HOSTILES: que le pasa a un mob hostil que entra en la zona.", "segundosDeFuego: 0 para no quemarlos. dano: 0 para solo empujarlos.")
      );
      hostiles.addProperty("segundosDeFuego", this.hostileBurnSeconds);
      hostiles.addProperty("dano", this.hostileDamage);
      root.add("barreraDeHostiles", hostiles);

      JsonObject efectos = new JsonObject();
      efectos.add(
         "_doc",
         doc(
            "duracionEnTicks: cuanto dura cada aplicacion de los efectos de las zonas grandes.",
            "Debe ser mayor que ticksEntreEfectosPasivos o el efecto parpadeara."
         )
      );
      efectos.addProperty("duracionEnTicks", this.effectDurationTicks);
      root.add("efectosPasivos", efectos);

      JsonObject nuevas = new JsonObject();
      nuevas.add(
         "_doc",
         doc(
            "Con que valores nace una zona nueva. No cambia las zonas ya creadas.",
            "particula: id de particula para el borde, por ejemplo minecraft:happy_villager.",
            "flags: true = la proteccion viene activada de fabrica. Son los mismos botones del menu."
         )
      );
      nuevas.addProperty("particula", this.defaultParticle);
      nuevas.addProperty("densidadDeParticulas", this.defaultParticleDensity);
      JsonObject flags = new JsonObject();

      for (ClaimFlags.FlagId id : ClaimFlags.FlagId.values()) {
         flags.addProperty(id.name(), this.defaultFlags.getOrDefault(id, Boolean.FALSE));
      }

      nuevas.add("flags", flags);
      root.add("zonasNuevas", nuevas);

      try {
         Files.createDirectories(this.file.getParent());
         Path tmp = this.file.resolveSibling(this.file.getFileName().toString() + ".tmp");
         Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
         Files.move(tmp, this.file, StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException e) {
         ClaimBlocksMod.LOGGER.error("[Tierrasmon Claims] No se pudo escribir " + this.file, e);
      }
   }

   public void applyDefaultsTo(Claim claim) {
      if (claim != null) {
         ClaimFlags f = claim.getOwnFlags();

         for (ClaimFlags.FlagId id : ClaimFlags.FlagId.values()) {
            f.set(id, this.defaultFlags.getOrDefault(id, Boolean.FALSE));
         }

         f.borderParticle = this.defaultParticle;
         f.particleDensity = this.defaultParticleDensity;
      }
   }

   public int trespasserAlertTicks() {
      return this.trespasserAlertSeconds * 20;
   }

   public long chatPromptMillis() {
      return (long)this.chatPromptSeconds * 1000L;
   }

   public long banNoticeTicks() {
      return (long)this.banNoticeSeconds * 20L;
   }

   private static JsonArray doc(String... lines) {
      JsonArray arr = new JsonArray();

      for (String s : lines) {
         arr.add(s);
      }

      return arr;
   }

   private static JsonObject section(JsonObject root, String key) {
      return root.has(key) && root.get(key).isJsonObject() ? root.getAsJsonObject(key) : new JsonObject();
   }

   private static int readInt(JsonObject o, String key, int fallback) {
      try {
         return o.has(key) ? o.get(key).getAsInt() : fallback;
      } catch (Exception e) {
         return fallback;
      }
   }

   private static float readFloat(JsonObject o, String key, float fallback) {
      try {
         return o.has(key) ? o.get(key).getAsFloat() : fallback;
      } catch (Exception e) {
         return fallback;
      }
   }

   private static boolean readBool(JsonObject o, String key, boolean fallback) {
      try {
         return o.has(key) ? o.get(key).getAsBoolean() : fallback;
      } catch (Exception e) {
         return fallback;
      }
   }

   private static String readString(JsonObject o, String key, String fallback) {
      try {
         return o.has(key) && !o.get(key).getAsString().isBlank() ? o.get(key).getAsString() : fallback;
      } catch (Exception e) {
         return fallback;
      }
   }
}
