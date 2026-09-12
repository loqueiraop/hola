package com.claimblocks.data;

import com.claimblocks.ClaimBlocks;
import com.claimblocks.ClaimBlocksMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ClaimManager {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final String DATA_FILE = "claimblocks_data.json";
   private static ClaimManager INSTANCE;
   private final Map<String, List<Claim>> claimsByWorld = new ConcurrentHashMap<>();
   private MinecraftServer server;
   private final Set<UUID> bypassPlayers = ConcurrentHashMap.newKeySet();
   private final Map<UUID, List<Text>> pendingMessages = new ConcurrentHashMap<>();
   private final Map<UUID, ClaimGroup> groups = new ConcurrentHashMap<>();
   private final Map<UUID, Claim> claimIndex = new ConcurrentHashMap<>();
   private final AtomicReference<String> pendingWrite = new AtomicReference<>();
   private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> {
      Thread t = new Thread(r, "ClaimBlocks-IO");
      t.setDaemon(true);
      return t;
   });

   private ClaimManager() {
   }

   public static synchronized ClaimManager getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new ClaimManager();
      }

      return INSTANCE;
   }

   public static int getMaxClaimsPerPlayer() {
      return ClaimConfig.get().maxClaimsPerPlayer;
   }

   public static void setMaxClaimsPerPlayer(int n) {
      ClaimConfig.get().maxClaimsPerPlayer = Math.max(0, n);
   }

   public MinecraftServer getServer() {
      return this.server;
   }

   public Claim createClaim(World world, BlockPos pos, PlayerEntity owner, ClaimTier tier) {
      String dim = world.getRegistryKey().getValue().toString();
      Claim c = Claim.create(owner.getUuid(), owner.getName().getString(), tier, dim, pos);
      ClaimConfig.get().applyDefaultsTo(c);
      if (tier != null) {
         switch (tier.id) {
            case "claimstone_500x500":
               c.getOwnFlags().effectRegeneration = true;
               c.getOwnFlags().effectResistance = true;
               c.getOwnFlags().effectSpeed = true;
               c.getOwnFlags().allowFlight = true;
               break;
            case "claimstone_300x300":
               c.getOwnFlags().effectRegeneration = true;
               c.getOwnFlags().effectResistance = true;
               c.getOwnFlags().effectSpeed = true;
               break;
            case "claimstone_250x250":
               c.getOwnFlags().effectRegeneration = true;
         }
      }

      this.claimsByWorld.computeIfAbsent(dim, k -> Collections.synchronizedList(new ArrayList<>())).add(c);
      this.claimIndex.put(c.getClaimId(), c);
      this.save();
      return c;
   }

   public boolean removeClaim(World world, BlockPos pos) {
      String dim = world.getRegistryKey().getValue().toString();
      List<Claim> list = this.claimsByWorld.get(dim);
      if (list == null) {
         return false;
      } else {
         Claim found = null;
         synchronized (list) {
            for (Claim c : list) {
               if (c.getX() == pos.getX() && c.getY() == pos.getY() && c.getZ() == pos.getZ()) {
                  found = c;
                  break;
               }
            }

            if (found != null) {
               list.remove(found);
            }
         }

         if (found != null) {
            this.claimIndex.remove(found.getClaimId());
            this.onClaimRemoved(found);
            this.save();
            return true;
         } else {
            return false;
         }
      }
   }

   /** Si se rompe la zona madre de un grupo, el grupo se deshace y las hijas se devuelven. */
   private void onClaimRemoved(Claim claim) {
      ClaimGroup g;
      if (claim.getGroupId() != null && (g = this.groups.get(claim.getGroupId())) != null && claim.getClaimId().equals(g.getMotherClaimId())) {
         this.dissolveGroupBreaking(g.getGroupId());
      }
   }

   public int clearClaimsOf(UUID playerId) {
      int total = 0;

      for (Entry<String, List<Claim>> e : this.claimsByWorld.entrySet()) {
         List<Claim> list = e.getValue();
         List<Claim> toRemove = new ArrayList<>();
         synchronized (list) {
            for (Claim c : list) {
               if (c.isOwner(playerId)) {
                  toRemove.add(c);
               }
            }
         }

         for (Claim cx : toRemove) {
            if (this.server != null) {
               ServerWorld w = this.worldFor(e.getKey());
               if (w != null) {
                  BlockPos p = cx.getCenter();
                  if (ClaimBlocks.isClaimConcreteForTier(w.getBlockState(p).getBlock(), cx.getTier())) {
                     w.setBlockState(p, Blocks.AIR.getDefaultState());
                  }
               }
            }

            synchronized (list) {
               list.remove(cx);
            }

            this.claimIndex.remove(cx.getClaimId());
            this.onClaimRemoved(cx);
            total++;
         }
      }

      if (total > 0) {
         this.save();
      }

      return total;
   }

   public boolean transferOwnership(Claim claim, UUID newOwnerId, String newOwnerName) {
      if (claim != null && newOwnerId != null) {
         claim.setOwner(newOwnerId, newOwnerName);
         this.save();
         return true;
      } else {
         return false;
      }
   }

   private ServerWorld worldFor(String dimensionKey) {
      if (this.server == null) {
         return null;
      } else {
         for (ServerWorld w : this.server.getWorlds()) {
            if (w.getRegistryKey().getValue().toString().equals(dimensionKey)) {
               return w;
            }
         }

         return null;
      }
   }

   public Claim getClaimAt(World world, BlockPos pos) {
      String dim = world.getRegistryKey().getValue().toString();
      List<Claim> list = this.claimsByWorld.get(dim);
      if (list == null) {
         return null;
      } else {
         synchronized (list) {
            for (Claim c : list) {
               if (c.contains(pos)) {
                  return c;
               }
            }

            return null;
         }
      }
   }

   public Claim getClaimByCenter(World world, BlockPos pos) {
      String dim = world.getRegistryKey().getValue().toString();
      List<Claim> list = this.claimsByWorld.get(dim);
      if (list == null) {
         return null;
      } else {
         synchronized (list) {
            for (Claim c : list) {
               if (c.getX() == pos.getX() && c.getY() == pos.getY() && c.getZ() == pos.getZ()) {
                  return c;
               }
            }

            return null;
         }
      }
   }

   public boolean wouldOverlap(World world, BlockPos pos, int radius, int height) {
      String dim = world.getRegistryKey().getValue().toString();
      List<Claim> list = this.claimsByWorld.get(dim);
      if (list == null) {
         return false;
      } else {
         synchronized (list) {
            for (Claim c : list) {
               if (c.overlapsWith(pos, radius, height)) {
                  return true;
               }
            }

            return false;
         }
      }
   }

   public List<Claim> overlappingClaims(World world, BlockPos pos, int radius, int height) {
      ArrayList<Claim> out = new ArrayList<>();
      String dim = world.getRegistryKey().getValue().toString();
      List<Claim> list = this.claimsByWorld.get(dim);
      if (list == null) {
         return out;
      } else {
         synchronized (list) {
            for (Claim c : list) {
               if (c.overlapsWith(pos, radius, height)) {
                  out.add(c);
               }
            }

            return out;
         }
      }
   }

   public ClaimGroup getGroup(UUID groupId) {
      return groupId == null ? null : this.groups.get(groupId);
   }

   public ClaimGroup getGroupOf(Claim claim) {
      return claim == null ? null : this.getGroup(claim.getGroupId());
   }

   public Claim findClaimById(UUID claimId) {
      return claimId == null ? null : this.claimIndex.get(claimId);
   }

   public Claim getMotherClaim(UUID groupId) {
      ClaimGroup g = this.getGroup(groupId);
      return g != null && g.getMotherClaimId() != null ? this.claimIndex.get(g.getMotherClaimId()) : null;
   }

   public ClaimGroup createGroup(Claim mother, String name) {
      UUID gid = UUID.randomUUID();
      ClaimGroup g = new ClaimGroup(gid, name, mother.getClaimId(), mother.getOwnerUUID());
      this.groups.put(gid, g);
      mother.setGroupId(gid);
      this.save();
      return g;
   }

   public void registerPlayer(UUID groupId, UUID playerId) {
      ClaimGroup g = this.getGroup(groupId);
      if (g != null) {
         g.register(playerId);
         this.save();
      }
   }

   public boolean isRegistered(UUID groupId, UUID playerId) {
      ClaimGroup g = this.getGroup(groupId);
      return g != null && g.isRegistered(playerId);
   }

   public ClaimGroup getGroupByRegistered(UUID playerId) {
      for (ClaimGroup g : this.groups.values()) {
         if (g.isRegistered(playerId)) {
            return g;
         }
      }

      return null;
   }

   public void joinClaimToGroup(Claim claim, UUID groupId) {
      if (claim != null && this.groups.containsKey(groupId)) {
         claim.setGroupId(groupId);
         this.save();
      }
   }

   public List<Claim> getGroupClaims(UUID groupId) {
      ArrayList<Claim> out = new ArrayList<>();
      if (groupId == null) {
         return out;
      } else {
         for (Claim c : this.getAllClaims()) {
            if (groupId.equals(c.getGroupId())) {
               out.add(c);
            }
         }

         return out;
      }
   }

   /** Deshace el grupo dejando todas las zonas en su sitio. */
   public void dissolveGroup(UUID groupId) {
      if (this.groups.remove(groupId) != null) {
         for (Claim c : this.getAllClaims()) {
            if (groupId.equals(c.getGroupId())) {
               c.setGroupId(null);
            }
         }

         this.save();
      }
   }

   /** Deshace el grupo rompiendo las zonas hijas y devolviendo sus piedras. */
   public void dissolveGroupBreaking(UUID groupId) {
      ClaimGroup g = this.groups.get(groupId);
      if (g != null) {
         Claim mother = this.getMotherClaim(groupId);
         UUID motherId = mother != null ? mother.getClaimId() : g.getMotherClaimId();

         for (Claim c : this.getGroupClaims(groupId)) {
            if (motherId == null || !c.getClaimId().equals(motherId)) {
               this.breakAndReturn(c);
            }
         }

         this.groups.remove(groupId);

         for (Claim c : this.getAllClaims()) {
            if (groupId.equals(c.getGroupId())) {
               c.setGroupId(null);
            }
         }

         this.save();
      }
   }

   public void leaveGroupBreaking(UUID groupId, UUID playerId) {
      ClaimGroup g = this.getGroup(groupId);
      if (g != null) {
         if (playerId != null && playerId.equals(g.getMotherOwnerId())) {
            this.dissolveGroupBreaking(groupId);
         } else {
            g.unregister(playerId);

            for (Claim c : this.getGroupClaims(groupId)) {
               if (c.isOwner(playerId)) {
                  this.breakAndReturn(c);
               }
            }

            this.save();
         }
      }
   }

   /** Quita el bloque de la zona y devuelve la piedra a su dueno (o la suelta en el suelo). */
   private void breakAndReturn(Claim claim) {
      ServerWorld world = this.worldFor(claim.getWorld());
      BlockPos pos = claim.getCenter();
      ClaimTier tier = claim.getTier();
      if (world != null && tier != null && ClaimBlocks.isClaimConcreteForTier(world.getBlockState(pos).getBlock(), tier)) {
         world.setBlockState(pos, Blocks.AIR.getDefaultState());
      }

      if (world != null && tier != null) {
         ItemStack stack = ClaimBlocks.createTierItem(tier, 1);
         ServerPlayerEntity owner = this.server != null && claim.getOwnerUUID() != null
            ? this.server.getPlayerManager().getPlayer(claim.getOwnerUUID())
            : null;
         if (owner != null) {
            if (!owner.getInventory().insertStack(stack)) {
               owner.dropItem(stack, false);
            }
         } else {
            world.spawnEntity(new ItemEntity(world, (double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5, stack));
         }
      }

      List<Claim> list = this.claimsByWorld.get(claim.getWorld());
      if (list != null) {
         synchronized (list) {
            list.remove(claim);
         }
      }

      this.claimIndex.remove(claim.getClaimId());
   }

   public void removePlayerFromGroup(UUID groupId, UUID playerId) {
      ClaimGroup g = this.getGroup(groupId);
      if (g != null) {
         if (playerId != null && playerId.equals(g.getMotherOwnerId())) {
            this.dissolveGroup(groupId);
         } else {
            g.unregister(playerId);

            for (Claim c : this.getGroupClaims(groupId)) {
               if (c.isOwner(playerId)) {
                  c.setGroupId(null);
               }
            }

            this.save();
         }
      }
   }

   public List<Claim> getAllClaims() {
      ArrayList<Claim> all = new ArrayList<>();

      for (List<Claim> l : this.claimsByWorld.values()) {
         synchronized (l) {
            all.addAll(l);
         }
      }

      return all;
   }

   public List<Claim> getClaimsOf(UUID playerId) {
      ArrayList<Claim> r = new ArrayList<>();

      for (List<Claim> l : this.claimsByWorld.values()) {
         synchronized (l) {
            for (Claim c : l) {
               if (c.isOwner(playerId)) {
                  r.add(c);
               }
            }
         }
      }

      return r;
   }

   public List<Claim> getClaimsInWorld(String dim) {
      List<Claim> l = this.claimsByWorld.getOrDefault(dim, Collections.emptyList());
      synchronized (l) {
         return new ArrayList<>(l);
      }
   }

   private String snapshotJson() {
      JsonObject root = new JsonObject();
      JsonArray claims = new JsonArray();

      for (Claim c : this.getAllClaims()) {
         claims.add(c.toJson());
      }

      root.add("claims", claims);
      JsonArray groupArr = new JsonArray();

      for (ClaimGroup g : this.groups.values()) {
         groupArr.add(g.toJson());
      }

      root.add("groups", groupArr);
      return GSON.toJson(root);
   }

   /** Guardado asincrono: colapsa varias peticiones seguidas en una sola escritura. */
   public void save() {
      if (this.server != null) {
         Path file = this.dataFile(this.server);
         String json = this.snapshotJson();
         boolean needsTask = this.pendingWrite.getAndSet(json) == null;
         if (needsTask) {
            IO.execute(() -> {
               String pending = this.pendingWrite.getAndSet(null);
               if (pending != null) {
                  writeAtomic(file, pending);
               }
            });
         }
      }
   }

   public void saveNow() {
      if (this.server != null) {
         Path file = this.dataFile(this.server);
         String json = this.snapshotJson();
         this.pendingWrite.set(null);
         writeAtomic(file, json);
      }
   }

   private static void writeAtomic(Path file, String json) {
      try {
         Files.createDirectories(file.getParent());
         Path tmp = file.resolveSibling(file.getFileName().toString() + ".tmp");
         Files.writeString(tmp, json, StandardCharsets.UTF_8);
         if (Files.exists(file)) {
            try {
               Files.copy(file, file.resolveSibling(file.getFileName().toString() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
            }
         }

         try {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
         } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
         }
      } catch (IOException e) {
         ClaimBlocksMod.LOGGER.error("Could not save claims to " + file, e);
      }
   }

   public void load(MinecraftServer server) {
      this.server = server;
      this.claimsByWorld.clear();
      this.claimIndex.clear();
      this.groups.clear();
      ClaimConfig.get().load(server);
      Path file = this.dataFile(server);
      if (!looksValid(file)) {
         Path bak = file.resolveSibling(file.getFileName().toString() + ".bak");
         if (looksValid(bak)) {
            ClaimBlocksMod.LOGGER.warn("[Tierrasmon Claims] {} no se puede leer; restaurando desde {}", file, bak);
            file = bak;
         }
      }

      if (!Files.exists(file)) {
         ClaimBlocksMod.LOGGER.info("No existing claims file at {}, starting fresh.", file);
      } else {
         try {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            if (text.isBlank()) {
               return;
            }

            JsonElement el = JsonParser.parseString(text);
            if (!el.isJsonObject()) {
               return;
            }

            JsonArray arr = el.getAsJsonObject().getAsJsonArray("claims");
            if (arr == null) {
               return;
            }

            int count = 0;
            int migrated = 0;

            for (JsonElement e : arr) {
               JsonObject obj = e.getAsJsonObject();
               boolean wasLegacy = !obj.has("radius") && obj.has("tier");
               Claim c = Claim.fromJson(obj);
               this.claimsByWorld.computeIfAbsent(c.getWorld(), k -> Collections.synchronizedList(new ArrayList<>())).add(c);
               this.claimIndex.put(c.getClaimId(), c);
               count++;
               if (wasLegacy) {
                  migrated++;
               }
            }

            JsonArray groupArr = el.getAsJsonObject().getAsJsonArray("groups");
            if (groupArr != null) {
               for (JsonElement e : groupArr) {
                  ClaimGroup g = ClaimGroup.fromJson(e.getAsJsonObject());
                  this.groups.put(g.getGroupId(), g);
               }
            }

            // Limpia grupos huerfanos (sin zona madre valida)
            ArrayList<UUID> orphans = new ArrayList<>();

            for (ClaimGroup g : this.groups.values()) {
               if (g.getMotherClaimId() == null || this.claimIndex.get(g.getMotherClaimId()) == null) {
                  orphans.add(g.getGroupId());
               }
            }

            for (UUID gid : orphans) {
               this.groups.remove(gid);

               for (Claim c : this.getAllClaims()) {
                  if (gid.equals(c.getGroupId())) {
                     c.setGroupId(null);
                  }
               }
            }

            ClaimBlocksMod.LOGGER.info("Loaded {} claims from {} (migrated {} legacy)", new Object[]{count, file, migrated});
            if (migrated > 0) {
               this.save();
            }
         } catch (Exception e) {
            ClaimBlocksMod.LOGGER.error("Could not load claims from " + file, e);
         }
      }
   }

   private static boolean looksValid(Path file) {
      try {
         if (!Files.exists(file)) {
            return false;
         } else {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            if (text.isBlank()) {
               return false;
            } else {
               JsonElement el = JsonParser.parseString(text);
               return el.isJsonObject() && el.getAsJsonObject().has("claims");
            }
         }
      } catch (Exception e) {
         return false;
      }
   }

   private Path dataFile(MinecraftServer s) {
      return s.getSavePath(WorldSavePath.ROOT).resolve(DATA_FILE);
   }

   public boolean isBypassing(UUID id) {
      return this.bypassPlayers.contains(id);
   }

   public boolean toggleBypass(UUID id) {
      if (this.bypassPlayers.contains(id)) {
         this.bypassPlayers.remove(id);
         return false;
      } else {
         this.bypassPlayers.add(id);
         return true;
      }
   }

   public Set<UUID> getBypassPlayers() {
      return this.bypassPlayers;
   }

   public void queueMessage(UUID owner, Text msg) {
      this.pendingMessages.computeIfAbsent(owner, k -> Collections.synchronizedList(new ArrayList<>())).add(msg);
   }

   public void flushPendingTo(ServerPlayerEntity player) {
      List<Text> msgs = this.pendingMessages.remove(player.getUuid());
      if (msgs != null) {
         synchronized (msgs) {
            for (Text t : msgs) {
               player.sendMessage(t, false);
            }
         }
      }
   }

   public void onPlayerDisconnect(UUID id) {
      this.bypassPlayers.remove(id);
   }
}
