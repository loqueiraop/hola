package com.claimblocks.data;

import com.claimblocks.ClaimBlocksMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.GameRules;
import net.minecraft.world.GameRules.BooleanRule;

public final class GlobalFlags {
   private static final String FILE = "global_flags.json";
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static GlobalFlags INSTANCE;
   public boolean globalPVP = true;
   public boolean globalMobGriefing = true;
   public boolean globalFireSpread = true;
   public boolean globalNoMobSpawn = false;

   private GlobalFlags() {
   }

   public static GlobalFlags getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new GlobalFlags();
      }

      return INSTANCE;
   }

   public boolean get(String key) {
      return switch (key) {
         case "globalPVP" -> this.globalPVP;
         case "globalMobGriefing" -> this.globalMobGriefing;
         case "globalFireSpread" -> this.globalFireSpread;
         case "globalNoMobSpawn" -> this.globalNoMobSpawn;
         default -> false;
      };
   }

   public void set(String key, boolean value, MinecraftServer server) {
      switch (key) {
         case "globalPVP":
            this.globalPVP = value;
            break;
         case "globalMobGriefing":
            this.globalMobGriefing = value;
            break;
         case "globalFireSpread":
            this.globalFireSpread = value;
            break;
         case "globalNoMobSpawn":
            this.globalNoMobSpawn = value;
      }

      this.applyToServer(server);
      this.save(server);
   }

   public void applyToServer(MinecraftServer server) {
      if (server != null) {
         server.setPvpEnabled(this.globalPVP);
         ServerWorld overworld = server.getOverworld();
         if (overworld != null) {
            GameRules rules = overworld.getGameRules();
            ((BooleanRule)rules.get(GameRules.DO_MOB_GRIEFING)).set(this.globalMobGriefing, server);
            ((BooleanRule)rules.get(GameRules.DO_FIRE_TICK)).set(this.globalFireSpread, server);
         }
      }
   }

   public void load(MinecraftServer server) {
      Path file = this.file(server);
      if (!Files.exists(file)) {
         ClaimBlocksMod.LOGGER.info("No global_flags.json, defaults applied.");
         this.applyToServer(server);
      } else {
         try {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            JsonObject o = JsonParser.parseString(text).getAsJsonObject();
            if (o.has("globalPVP")) {
               this.globalPVP = o.get("globalPVP").getAsBoolean();
            }

            if (o.has("globalMobGriefing")) {
               this.globalMobGriefing = o.get("globalMobGriefing").getAsBoolean();
            }

            if (o.has("globalNoMobSpawn")) {
               this.globalNoMobSpawn = o.get("globalNoMobSpawn").getAsBoolean();
            }

            if (o.has("globalFireSpread")) {
               this.globalFireSpread = o.get("globalFireSpread").getAsBoolean();
            }

            this.applyToServer(server);
            ClaimBlocksMod.LOGGER
               .info("Global flags cargadas: PVP={} MobGrief={} FireSpread={}", new Object[]{this.globalPVP, this.globalMobGriefing, this.globalFireSpread});
         } catch (Exception var5) {
            ClaimBlocksMod.LOGGER.error("No se pudo cargar global_flags.json", var5);
         }
      }
   }

   public void save(MinecraftServer server) {
      Path file = this.file(server);

      try {
         JsonObject o = new JsonObject();
         o.addProperty("globalPVP", this.globalPVP);
         o.addProperty("globalMobGriefing", this.globalMobGriefing);
         o.addProperty("globalFireSpread", this.globalFireSpread);
         o.addProperty("globalNoMobSpawn", this.globalNoMobSpawn);
         Files.createDirectories(file.getParent());
         Files.writeString(file, GSON.toJson(o), StandardCharsets.UTF_8);
      } catch (IOException var4) {
         ClaimBlocksMod.LOGGER.error("No se pudo guardar global_flags.json", var4);
      }
   }

   private Path file(MinecraftServer s) {
      return s.getSavePath(WorldSavePath.ROOT).resolve("global_flags.json");
   }
}
