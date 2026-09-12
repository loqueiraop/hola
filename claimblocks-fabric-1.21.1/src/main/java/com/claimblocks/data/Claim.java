package com.claimblocks.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

public class Claim {
   private final UUID claimId;
   private UUID ownerUUID;
   private String ownerName;
   private String tierId;
   private final int radius;
   private final int height;
   private final String world;
   private final int x;
   private final int y;
   private final int z;
   private long createdAt;
   private UUID groupId;
   private final List<UUID> members = new ArrayList<>();
   private final List<String> memberNames = new ArrayList<>();
   private final Set<UUID> bannedPlayers = new HashSet<>();
   private final ClaimFlags flags = new ClaimFlags();

   public Claim(UUID claimId, UUID ownerUUID, String ownerName, String tierId, int radius, int height, String world, int x, int y, int z) {
      this.claimId = claimId;
      this.ownerUUID = ownerUUID;
      this.ownerName = ownerName;
      this.tierId = tierId;
      this.radius = radius;
      this.height = height;
      this.world = world;
      this.x = x;
      this.y = y;
      this.z = z;
      this.createdAt = System.currentTimeMillis();
   }

   public static Claim create(UUID owner, String ownerName, ClaimTier tier, String world, BlockPos pos) {
      return new Claim(UUID.randomUUID(), owner, ownerName, tier.id, tier.radius, tier.height, world, pos.getX(), pos.getY(), pos.getZ());
   }

   public UUID getClaimId() {
      return this.claimId;
   }

   public UUID getOwnerUUID() {
      return this.ownerUUID;
   }

   public String getOwnerName() {
      return this.ownerName;
   }

   public String getTierId() {
      return this.tierId;
   }

   public int getRadius() {
      return this.radius;
   }

   /** Altura efectiva: si la zona esta fusionada manda la altura de la zona madre. */
   public int getHeight() {
      return this.effectiveHeight();
   }

   public int getOwnHeight() {
      return this.height;
   }

   public UUID getGroupId() {
      return this.groupId;
   }

   public void setGroupId(UUID id) {
      this.groupId = id;
   }

   public boolean isGrouped() {
      return this.groupId != null;
   }

   public Claim getMother() {
      return this.groupId == null ? null : ClaimManager.getInstance().getMotherClaim(this.groupId);
   }

   public boolean isGroupMother() {
      if (this.groupId == null) {
         return false;
      } else {
         ClaimGroup g = ClaimManager.getInstance().getGroup(this.groupId);
         return g != null && this.claimId.equals(g.getMotherClaimId());
      }
   }

   public int effectiveHeight() {
      Claim mother;
      return this.groupId != null && (mother = ClaimManager.getInstance().getMotherClaim(this.groupId)) != null && mother != this
         ? mother.height
         : this.height;
   }

   public String getWorld() {
      return this.world;
   }

   public int getX() {
      return this.x;
   }

   public int getY() {
      return this.y;
   }

   public int getZ() {
      return this.z;
   }

   public BlockPos getCenter() {
      return new BlockPos(this.x, this.y, this.z);
   }

   public List<UUID> getMembers() {
      return this.members;
   }

   public List<String> getMemberNames() {
      return this.memberNames;
   }

   /** Los baneos viven en la zona madre cuando la zona esta fusionada. */
   private Claim banHolder() {
      Claim mother;
      return this.groupId != null && (mother = ClaimManager.getInstance().getMotherClaim(this.groupId)) != null ? mother : this;
   }

   public Set<UUID> getBannedPlayers() {
      return this.banHolder().bannedPlayers;
   }

   public Set<UUID> getOwnBannedPlayers() {
      return this.bannedPlayers;
   }

   /** Las flags efectivas: las de la zona madre si esta fusionada. */
   public ClaimFlags getFlags() {
      Claim mother;
      return this.groupId != null && (mother = ClaimManager.getInstance().getMotherClaim(this.groupId)) != null && mother != this
         ? mother.flags
         : this.flags;
   }

   public ClaimFlags getOwnFlags() {
      return this.flags;
   }

   public long getCreatedAt() {
      return this.createdAt;
   }

   public void setCreatedAt(long t) {
      this.createdAt = t;
   }

   public ClaimTier getTier() {
      ClaimTier t;
      return this.tierId != null && (t = ClaimTier.byId(this.tierId)) != null ? t : ClaimTier.closestMatch(this.radius, this.height);
   }

   public String sizeLabel() {
      if (this.tierId != null && this.tierId.startsWith("claimstone_")) {
         return this.tierId.substring("claimstone_".length());
      } else {
         ClaimTier t = this.getTier();
         return t == null ? this.radius + "x" + this.radius : t.label();
      }
   }

   public boolean contains(BlockPos pos) {
      if (Math.abs(pos.getX() - this.x) <= this.radius && Math.abs(pos.getZ() - this.z) <= this.radius) {
         int cy = this.y;
         int h = this.height;
         Claim mother;
         if (this.groupId != null && (mother = ClaimManager.getInstance().getMotherClaim(this.groupId)) != null) {
            cy = mother.y;
            h = mother.height;
         }

         return pos.getY() - cy <= h && cy - pos.getY() <= h;
      } else {
         return false;
      }
   }

   public boolean overlapsWith(BlockPos otherCenter, int otherRadius, int otherHeight) {
      return Math.abs(otherCenter.getX() - this.x) < this.radius + otherRadius
         && Math.abs(otherCenter.getZ() - this.z) < this.radius + otherRadius
         && Math.abs(otherCenter.getY() - this.y) < this.height + otherHeight;
   }

   public Box getBoundingBox() {
      return new Box(
         (double)(this.x - this.radius),
         (double)(this.y - this.height),
         (double)(this.z - this.radius),
         (double)(this.x + this.radius + 1),
         (double)(this.y + this.height + 1),
         (double)(this.z + this.radius + 1)
      );
   }

   public boolean isOwner(UUID id) {
      return this.ownerUUID != null && this.ownerUUID.equals(id);
   }

   public boolean isOwner(PlayerEntity p) {
      return this.isOwner(p.getUuid());
   }

   public boolean isMember(UUID id) {
      return this.members.contains(id);
   }

   public boolean isMember(PlayerEntity p) {
      return this.isMember(p.getUuid());
   }

   public boolean isBanned(UUID id) {
      return this.banHolder().bannedPlayers.contains(id);
   }

   public boolean canModify(PlayerEntity p) {
      if (this.isOwner(p) || this.isMember(p) || p.hasPermissionLevel(2)) {
         return true;
      } else {
         ClaimGroup g;
         return this.groupId != null && (g = ClaimManager.getInstance().getGroup(this.groupId)) != null && g.isRegistered(p.getUuid());
      }
   }

   public void addMember(UUID id, String name) {
      if (!this.members.contains(id)) {
         this.members.add(id);
         this.memberNames.add(name == null ? "" : name);
      }
   }

   public void removeMember(UUID id) {
      int idx = this.members.indexOf(id);
      if (idx >= 0) {
         this.members.remove(idx);
         if (idx < this.memberNames.size()) {
            this.memberNames.remove(idx);
         }
      }
   }

   public void banPlayer(UUID id) {
      this.banHolder().bannedPlayers.add(id);
      this.removeMember(id);
      if (this.groupId != null) {
         for (Claim c : ClaimManager.getInstance().getGroupClaims(this.groupId)) {
            c.removeMember(id);
         }
      }
   }

   public void unbanPlayer(UUID id) {
      this.banHolder().bannedPlayers.remove(id);
      this.bannedPlayers.remove(id);
      if (this.groupId != null) {
         for (Claim c : ClaimManager.getInstance().getGroupClaims(this.groupId)) {
            c.bannedPlayers.remove(id);
         }
      }
   }

   public void setOwner(UUID id, String name) {
      this.ownerUUID = id;
      this.ownerName = name;
   }

   public void setTierId(String id) {
      this.tierId = id;
   }

   public JsonObject toJson() {
      JsonObject o = new JsonObject();
      o.addProperty("claimId", this.claimId.toString());
      o.addProperty("ownerUUID", this.ownerUUID == null ? "" : this.ownerUUID.toString());
      o.addProperty("ownerName", this.ownerName == null ? "" : this.ownerName);
      if (this.tierId != null) {
         o.addProperty("tierId", this.tierId);
      }

      o.addProperty("radius", this.radius);
      o.addProperty("height", this.height);
      o.addProperty("world", this.world);
      o.addProperty("x", this.x);
      o.addProperty("y", this.y);
      o.addProperty("z", this.z);
      o.addProperty("createdAt", this.createdAt);
      if (this.groupId != null) {
         o.addProperty("groupId", this.groupId.toString());
      }

      JsonArray mem = new JsonArray();

      for (UUID id : this.members) {
         mem.add(id.toString());
      }

      o.add("members", mem);
      JsonArray memN = new JsonArray();

      for (String n : this.memberNames) {
         memN.add(n == null ? "" : n);
      }

      o.add("memberNames", memN);
      JsonArray banned = new JsonArray();

      for (UUID id : this.bannedPlayers) {
         banned.add(id.toString());
      }

      o.add("bannedPlayers", banned);

      JsonObject f = new JsonObject();
      f.addProperty("blockBuilding", this.flags.blockBuilding);
      f.addProperty("blockBreaking", this.flags.blockBreaking);
      f.addProperty("blockExplosions", this.flags.blockExplosions);
      f.addProperty("blockFire", this.flags.blockFire);
      f.addProperty("blockMobSpawn", this.flags.blockMobSpawn);
      f.addProperty("blockPVP", this.flags.blockPVP);
      f.addProperty("blockMobDamage", this.flags.blockMobDamage);
      f.addProperty("trespasserAlerts", this.flags.trespasserAlerts);
      f.addProperty("blockItemUse", this.flags.blockItemUse);
      f.addProperty("blockEntityInteract", this.flags.blockEntityInteract);
      f.addProperty("blockTrampling", this.flags.blockTrampling);
      f.addProperty("blockFluids", this.flags.blockFluids);
      f.addProperty("pvpAll", this.flags.pvpAll);
      f.addProperty("blockTreeChopping", this.flags.blockTreeChopping);
      f.addProperty("publicMode", this.flags.publicMode);
      f.addProperty("showWelcome", this.flags.showWelcome);
      f.addProperty("welcomeMessage", this.flags.welcomeMessage == null ? "" : this.flags.welcomeMessage);
      f.addProperty("showLeave", this.flags.showLeave);
      f.addProperty("leaveMessage", this.flags.leaveMessage == null ? "" : this.flags.leaveMessage);
      f.addProperty("showBorder", this.flags.showBorder);
      f.addProperty("showParticles", this.flags.showParticles);
      f.addProperty("borderParticle", this.flags.borderParticle == null ? "minecraft:happy_villager" : this.flags.borderParticle);
      f.addProperty("particleDensity", this.flags.particleDensity);
      f.addProperty("burnHostiles", this.flags.burnHostiles);
      f.addProperty("effectRegeneration", this.flags.effectRegeneration);
      f.addProperty("effectResistance", this.flags.effectResistance);
      f.addProperty("effectSpeed", this.flags.effectSpeed);
      f.addProperty("blockAnimalKilling", this.flags.blockAnimalKilling);
      f.addProperty("blockChestAccess", this.flags.blockChestAccess);
      f.addProperty("blockCropHarvest", this.flags.blockCropHarvest);
      f.addProperty("blockAnvilUse", this.flags.blockAnvilUse);
      f.addProperty("blockEnderPearl", this.flags.blockEnderPearl);
      f.addProperty("blockSignEditing", this.flags.blockSignEditing);
      f.addProperty("allowFlight", this.flags.allowFlight);
      f.addProperty("blockDoorsAccess", this.flags.blockDoorsAccess);
      f.addProperty("blockAllInteractions", this.flags.blockAllInteractions);
      f.addProperty("blockAllMobSpawn", this.flags.blockAllMobSpawn);
      f.addProperty("blockPassiveMobSpawn", this.flags.blockPassiveMobSpawn);
      o.add("flags", f);
      return o;
   }

   public static Claim fromJson(JsonObject o) {
      UUID id = o.has("claimId") ? UUID.fromString(o.get("claimId").getAsString()) : UUID.randomUUID();
      UUID owner = o.has("ownerUUID") && !o.get("ownerUUID").getAsString().isEmpty() ? UUID.fromString(o.get("ownerUUID").getAsString()) : null;
      String ownerName = o.has("ownerName") ? o.get("ownerName").getAsString() : "";
      String world = o.has("world") ? o.get("world").getAsString() : "minecraft:overworld";
      int x = o.get("x").getAsInt();
      int y = o.get("y").getAsInt();
      int z = o.get("z").getAsInt();
      String tierId;
      int height;
      int radius;
      if (o.has("radius") && o.has("height")) {
         radius = o.get("radius").getAsInt();
         height = o.get("height").getAsInt();
         tierId = o.has("tierId") ? o.get("tierId").getAsString() : null;
      } else if (o.has("tier")) {
         int legacy = o.get("tier").getAsInt();
         ClaimTier t = ClaimTier.byLegacyTier(legacy);
         if (t == null) {
            t = ClaimTier.VALUES[0];
         }

         radius = t.radius;
         height = t.height;
         tierId = t.id;
      } else {
         radius = 10;
         height = 15;
         tierId = "claimstone_10x10";
      }

      Claim c = new Claim(id, owner, ownerName, tierId, radius, height, world, x, y, z);
      c.createdAt = o.has("createdAt") ? o.get("createdAt").getAsLong() : 0L;
      if (o.has("groupId") && !o.get("groupId").getAsString().isEmpty()) {
         c.groupId = UUID.fromString(o.get("groupId").getAsString());
      }

      if (o.has("members")) {
         JsonArray arr = o.getAsJsonArray("members");
         JsonArray names = o.has("memberNames") ? o.getAsJsonArray("memberNames") : new JsonArray();

         for (int i = 0; i < arr.size(); i++) {
            UUID mid = UUID.fromString(arr.get(i).getAsString());
            String mname = i < names.size() ? names.get(i).getAsString() : "";
            c.addMember(mid, mname);
         }
      }

      if (o.has("bannedPlayers")) {
         JsonArray arr = o.getAsJsonArray("bannedPlayers");

         for (int i = 0; i < arr.size(); i++) {
            c.bannedPlayers.add(UUID.fromString(arr.get(i).getAsString()));
         }
      }

      if (o.has("flags")) {
         JsonObject f = o.getAsJsonObject("flags");
         applyBool(f, "blockBuilding", v -> c.flags.blockBuilding = v);
         applyBool(f, "blockBreaking", v -> c.flags.blockBreaking = v);
         applyBool(f, "blockExplosions", v -> c.flags.blockExplosions = v);
         applyBool(f, "blockFire", v -> c.flags.blockFire = v);
         applyBool(f, "blockMobSpawn", v -> c.flags.blockMobSpawn = v);
         applyBool(f, "blockPVP", v -> c.flags.blockPVP = v);
         applyBool(f, "blockMobDamage", v -> c.flags.blockMobDamage = v);
         applyBool(f, "trespasserAlerts", v -> c.flags.trespasserAlerts = v);
         applyBool(f, "blockItemUse", v -> c.flags.blockItemUse = v);
         applyBool(f, "blockEntityInteract", v -> c.flags.blockEntityInteract = v);
         applyBool(f, "blockTrampling", v -> c.flags.blockTrampling = v);
         applyBool(f, "blockFluids", v -> c.flags.blockFluids = v);
         applyBool(f, "pvpAll", v -> c.flags.pvpAll = v);
         applyBool(f, "blockTreeChopping", v -> c.flags.blockTreeChopping = v);
         applyBool(f, "publicMode", v -> c.flags.publicMode = v);
         applyBool(f, "showWelcome", v -> c.flags.showWelcome = v);
         if (f.has("welcomeMessage")) {
            c.flags.welcomeMessage = f.get("welcomeMessage").getAsString();
         }

         applyBool(f, "showLeave", v -> c.flags.showLeave = v);
         if (f.has("leaveMessage")) {
            c.flags.leaveMessage = f.get("leaveMessage").getAsString();
         }

         applyBool(f, "showBorder", v -> c.flags.showBorder = v);
         applyBool(f, "showParticles", v -> c.flags.showParticles = v);
         if (f.has("borderParticle")) {
            c.flags.borderParticle = f.get("borderParticle").getAsString();
         }

         if (f.has("particleDensity")) {
            c.flags.particleDensity = f.get("particleDensity").getAsInt();
         }

         applyBool(f, "burnHostiles", v -> c.flags.burnHostiles = v);
         applyBool(f, "effectRegeneration", v -> c.flags.effectRegeneration = v);
         applyBool(f, "effectResistance", v -> c.flags.effectResistance = v);
         applyBool(f, "effectSpeed", v -> c.flags.effectSpeed = v);
         applyBool(f, "blockAnimalKilling", v -> c.flags.blockAnimalKilling = v);
         applyBool(f, "blockChestAccess", v -> c.flags.blockChestAccess = v);
         applyBool(f, "blockCropHarvest", v -> c.flags.blockCropHarvest = v);
         applyBool(f, "blockAnvilUse", v -> c.flags.blockAnvilUse = v);
         applyBool(f, "blockEnderPearl", v -> c.flags.blockEnderPearl = v);
         applyBool(f, "blockSignEditing", v -> c.flags.blockSignEditing = v);
         applyBool(f, "allowFlight", v -> c.flags.allowFlight = v);
         applyBool(f, "blockDoorsAccess", v -> c.flags.blockDoorsAccess = v);
         applyBool(f, "blockAllInteractions", v -> c.flags.blockAllInteractions = v);
         applyBool(f, "blockAllMobSpawn", v -> c.flags.blockAllMobSpawn = v);
         applyBool(f, "blockPassiveMobSpawn", v -> c.flags.blockPassiveMobSpawn = v);
      }

      return c;
   }

   private static void applyBool(JsonObject o, String key, Claim.BoolSetter setter) {
      if (o.has(key)) {
         setter.set(o.get(key).getAsBoolean());
      }
   }

   private interface BoolSetter {
      void set(boolean value);
   }
}
