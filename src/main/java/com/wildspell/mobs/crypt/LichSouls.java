package com.wildspell.mobs.crypt;

import com.wildspell.mobs.entity.ColdEffects;
import com.wildspell.mobs.entity.IceLich;
import com.wildspell.mobs.entity.LichWisp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public class LichSouls extends SavedData {
    private static final String NAME = "wildspellmobs_lich_souls";
    private static final int MISSING_LIMIT = 60 * 20;
    static boolean assumeBodyAway;
    public static final double SAFE_RADIUS = 64.0;

    private final Map<UUID, Soul> souls = new HashMap<>();
    private final List<GlobalPos> cleansed = new ArrayList<>();

    public static LichSouls get(ServerLevel anyLevel) {
        return get(anyLevel.getServer());
    }

    public static LichSouls get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(LichSouls::new, LichSouls::load), NAME);
    }

    @Nullable
    public Soul soul(@Nullable UUID id) {
        return id == null ? null : this.souls.get(id);
    }

    public Soul create(ServerLevel level, BlockPos altar) {
        Soul soul = new Soul(UUID.randomUUID(), level.dimension(), level.dimension(), altar.immutable());
        if (level.getBlockState(altar).hasProperty(PhylacteryBlock.FACING)) {
            soul.cryptFacing = level.getBlockState(altar).getValue(PhylacteryBlock.FACING);
        }
        this.souls.put(soul.id, soul);
        this.setDirty();
        return soul;
    }

    public boolean isCleansed(Level level, BlockPos pos) {
        for (GlobalPos zone : this.cleansed) {
            if (zone.dimension() == level.dimension() && zone.pos().distSqr(pos) < SAFE_RADIUS * SAFE_RADIUS) {
                return true;
            }
        }
        return false;
    }

    void forgetWithin(ServerLevel level, AABB area) {
        this.souls.values().removeIf(soul -> {
            if (soul.cryptDimension != level.dimension() || !area.contains(Vec3.atCenterOf(soul.crypt))) {
                return false;
            }
            if (soul.lichId != null && level.getEntity(soul.lichId) instanceof IceLich lich) {
                lich.discard();
            }
            return true;
        });
        this.cleansed.removeIf(zone -> zone.dimension() == level.dimension() && area.contains(Vec3.atCenterOf(zone.pos())));
        this.setDirty();
    }

    public static boolean isCleansedZone(Level level, BlockPos pos) {
        return level instanceof ServerLevel server && get(server).isCleansed(level, pos);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        LichSouls souls = get(server);
        long time = server.overworld().getGameTime();
        List<Soul> all = List.copyOf(souls.souls.values());
        for (Soul soul : all) {
            soul.tick(server, time);
        }
        if (time % 20 == 7) {
            for (ServerLevel level : server.getAllLevels()) {
                PhylacteryBlockEntity.ambushFromAfar(level, all);
            }
        }
    }

    public final class Soul {
        public final UUID id;
        private ResourceKey<Level> dimension;
        private final ResourceKey<Level> cryptDimension;
        private final BlockPos crypt;
        private Direction cryptFacing = Direction.NORTH;
        private BlockPos anchor;
        private boolean inAltar = true;
        @Nullable
        private UUID carrier;
        @Nullable
        private UUID lichId;
        @Nullable
        private UUID reformHunting;
        private int reformTicks = -1;
        private int soulTicks;
        private int missingTicks;
        private int ambushCooldown;
        private boolean burned;
        private boolean fallen;
        private boolean cleansedCrypt;

        Soul(UUID id, ResourceKey<Level> dimension, ResourceKey<Level> cryptDimension, BlockPos crypt) {
            this.id = id;
            this.dimension = dimension;
            this.cryptDimension = cryptDimension;
            this.crypt = crypt;
            this.anchor = crypt;
        }

        public ResourceKey<Level> dimension() {
            return this.dimension;
        }

        public ResourceKey<Level> cryptDimension() {
            return this.cryptDimension;
        }

        public BlockPos crypt() {
            return this.crypt;
        }

        public Direction cryptFacing() {
            return this.cryptFacing;
        }

        public BlockPos anchor() {
            return this.anchor;
        }

        public boolean inAltar() {
            return this.inAltar;
        }

        public boolean burned() {
            return this.burned;
        }

        @Nullable
        public UUID lichId() {
            return this.lichId;
        }

        @Nullable
        public UUID carrier() {
            return this.carrier;
        }

        public boolean dormant() {
            return this.lichId == null && this.soulTicks <= 0 && this.reformTicks < 0 && !this.burned;
        }

        public boolean canStalk(ServerLevel level) {
            if (!this.inAltar || this.burned || this.reformTicks >= 0 || this.soulTicks > 0) {
                return false;
            }
            if (this.lichId == null) {
                return true;
            }
            return assumeBodyAway || this.findLich(level) == null && !level.isLoaded(this.anchor);
        }

        private void changed() {
            LichSouls.this.setDirty();
        }

        void placeOnAltar(ServerLevel level, BlockPos pos) {
            this.dimension = level.dimension();
            this.anchor = pos.immutable();
            this.inAltar = true;
            this.carrier = null;
            this.changed();
        }

        public void moved(Level level, BlockPos pos, @Nullable Entity carrier) {
            UUID carrierId = carrier == null ? null : carrier.getUUID();
            if (!this.inAltar && this.dimension == level.dimension() && this.anchor.equals(pos) && Objects.equals(this.carrier, carrierId)) {
                return;
            }
            this.dimension = level.dimension();
            this.anchor = pos.immutable();
            this.inAltar = false;
            this.carrier = carrierId;
            this.changed();
        }

        public int ambushCooldown() {
            return this.ambushCooldown;
        }

        void coolAmbush() {
            if (this.ambushCooldown > 0) {
                --this.ambushCooldown;
                this.changed();
            }
        }

        @Nullable
        public IceLich findLich(ServerLevel level) {
            return this.lichId != null && level.getEntity(this.lichId) instanceof IceLich lich && lich.isAlive() ? lich : null;
        }

        public boolean claim(IceLich lich) {
            if (lich.getUUID().equals(this.lichId)) {
                return true;
            }
            if (this.lichId == null && this.reformTicks < 0 && !this.burned) {
                this.lichId = lich.getUUID();
                this.soulTicks = 0;
                this.missingTicks = 0;
                this.changed();
                return true;
            }
            return false;
        }

        @Nullable
        public Vec3 reformSpot(ServerLevel level) {
            if (this.inAltar) {
                return Vec3.atBottomCenterOf(this.anchor.above());
            }
            Vec3 spot = ColdEffects.findSpot(24,
                    () -> Vec3.atBottomCenterOf(this.anchor.offset(level.random.nextInt(13) - 6, level.random.nextInt(5) - 1, level.random.nextInt(13) - 6)),
                    at -> BlockPos.containing(at).distSqr(this.anchor) >= 9 && ColdEffects.isOpen(level, BlockPos.containing(at), 3));
            if (spot != null) {
                return spot;
            }
            return ColdEffects.isOpen(level, this.anchor.above(), 3) ? Vec3.atBottomCenterOf(this.anchor.above()) : null;
        }

        @Nullable
        public IceLich raise(ServerLevel level, Vec3 at, @Nullable Player prey) {
            IceLich lich = this.burned ? null : IceLich.summon(level, at, this.id);
            if (lich != null) {
                if (prey != null) {
                    lich.hunt(prey);
                }
                this.lichId = lich.getUUID();
                this.missingTicks = 0;
                this.soulTicks = 0;
                this.changed();
            }
            return lich;
        }

        public void scheduleRise(Player prey, int ticks) {
            this.reformHunting = prey.getUUID();
            this.reformTicks = ticks;
            this.changed();
        }

        public void hurryRise(int ticks) {
            if (this.reformTicks >= 0) {
                this.reformTicks = Math.min(this.reformTicks, ticks);
                this.changed();
            }
        }

        public void onWispArrived(ServerLevel level, @Nullable UUID hunting) {
            this.soulTicks = 0;
            this.changed();
            if (this.lichId != null || this.reformTicks >= 0 || this.burned) {
                return;
            }
            this.reformHunting = hunting;
            this.reformTicks = this.inAltar && level.getBlockEntity(this.anchor) instanceof PhylacteryBlockEntity crypt && crypt.isAwake()
                    ? PhylacteryBlockEntity.REFORM_TICKS_AWAKE : PhylacteryBlockEntity.REFORM_TICKS;
        }

        public void onLichDiscorporated(IceLich lich) {
            if (lich.getUUID().equals(this.lichId)) {
                this.lichId = null;
                this.soulTicks = LichWisp.MAX_AGE;
                this.changed();
            }
        }

        public void onLichRetreated(IceLich lich) {
            if (lich.getUUID().equals(this.lichId)) {
                this.lichId = null;
                this.ambushCooldown = PhylacteryBlockEntity.RETREAT_COOLDOWN;
                this.changed();
            }
        }

        public void burn(ServerLevel level, Vec3 at) {
            if (this.burned) {
                return;
            }
            this.burned = true;
            this.reformTicks = -1;
            this.soulTicks = 0;
            this.changed();
            IceLich old = this.findLich(level);
            if (old != null) {
                old.vanish();
            }
            this.lichId = null;
            ColdEffects.tellNearby(level, new AABB(BlockPos.containing(at)).inflate(64.0),
                    Component.translatable("message.wildspellmobs.phylactery_burned"));
            IceLich last = IceLich.summonLastForm(level, at.add(0.0, 1.0, 0.0), this.id);
            if (last == null) {
                this.fall(level, at);
                return;
            }
            Player prey = level.getNearestPlayer(at.x, at.y, at.z, 48.0, p -> p instanceof Player player && PhylacteryBlockEntity.isPrey(player));
            if (prey != null) {
                last.hunt(prey);
            }
        }

        public void fall(ServerLevel level, Vec3 at) {
            if (this.fallen) {
                return;
            }
            this.fallen = true;
            this.changed();
            Cleansing.purge(level, at);
            ColdEffects.tellNearby(level, new AABB(BlockPos.containing(at)).inflate(64.0),
                    Component.translatable("message.wildspellmobs.lich_fallen"));
        }

        private void tick(MinecraftServer server, long time) {
            ServerLevel cryptLevel = server.getLevel(this.cryptDimension);
            if (this.fallen && !this.cleansedCrypt && cryptLevel != null && time % 40 == 0
                    && cryptLevel.isAreaLoaded(this.crypt, Cleansing.LOADED_RADIUS)) {
                this.cleansedCrypt = true;
                LichSouls.this.cleansed.add(GlobalPos.of(this.cryptDimension, this.crypt));
                LichSouls.this.souls.remove(this.id);
                this.changed();
                Cleansing.cleanse(cryptLevel, this.crypt, this.cryptFacing);
            }
            ServerLevel level = server.getLevel(this.dimension);
            if (level == null || this.cleansedCrypt) {
                return;
            }
            if (this.soulTicks > 0 && --this.soulTicks == 0) {
                this.changed();
            }
            if (this.reformTicks > 0 && --this.reformTicks == 0) {
                this.reform(level);
            }
            if (time % 20 == 0 && this.lichId != null && level.isLoaded(this.anchor)) {
                if (this.findLich(level) != null) {
                    this.missingTicks = 0;
                } else if ((this.missingTicks += 20) >= MISSING_LIMIT) {
                    this.lichId = null;
                    this.missingTicks = 0;
                    this.changed();
                }
            }
        }

        private void reform(ServerLevel level) {
            this.reformTicks = -1;
            this.changed();
            if (this.lichId != null || this.burned) {
                return;
            }
            Vec3 at = level.isLoaded(this.anchor) ? this.reformSpot(level) : null;
            if (at == null) {
                this.reformTicks = 20;
                return;
            }
            Player prey = this.reformHunting != null && level.getEntity(this.reformHunting) instanceof Player player ? player : null;
            if (prey == null && this.carrier != null && level.getEntity(this.carrier) instanceof Player bearer) {
                prey = bearer;
            }
            if (prey != null && (!PhylacteryBlockEntity.isPrey(prey) || this.inAltar && prey.distanceToSqr(Vec3.atCenterOf(this.anchor)) > PhylacteryBlockEntity.leash() * PhylacteryBlockEntity.leash())) {
                prey = null;
            }
            if (prey == null && this.inAltar && level.getBlockEntity(this.anchor) instanceof PhylacteryBlockEntity crypt) {
                prey = level.getEntitiesOfClass(Player.class, crypt.cryptBounds(), PhylacteryBlockEntity::isPrey).stream().findFirst().orElse(null);
            }
            this.reformHunting = null;
            this.raise(level, at, prey);
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Id", this.id);
            tag.putString("Dimension", this.dimension.location().toString());
            tag.putString("CryptDimension", this.cryptDimension.location().toString());
            tag.put("Crypt", NbtUtils.writeBlockPos(this.crypt));
            tag.put("Anchor", NbtUtils.writeBlockPos(this.anchor));
            tag.putInt("CryptFacing", this.cryptFacing.get3DDataValue());
            tag.putBoolean("InAltar", this.inAltar);
            if (this.carrier != null) {
                tag.putUUID("Carrier", this.carrier);
            }
            if (this.lichId != null) {
                tag.putUUID("Lich", this.lichId);
            }
            if (this.reformHunting != null) {
                tag.putUUID("ReformHunting", this.reformHunting);
            }
            tag.putInt("ReformTicks", this.reformTicks);
            tag.putInt("SoulTicks", this.soulTicks);
            tag.putInt("AmbushCooldown", this.ambushCooldown);
            tag.putBoolean("Burned", this.burned);
            tag.putBoolean("Fallen", this.fallen);
            tag.putBoolean("Cleansed", this.cleansedCrypt);
            return tag;
        }
    }

    private static LichSouls load(CompoundTag tag, HolderLookup.Provider registries) {
        LichSouls data = new LichSouls();
        for (Tag entry : tag.getList("Souls", Tag.TAG_COMPOUND)) {
            CompoundTag s = (CompoundTag) entry;
            if (!s.hasUUID("Id")) {
                continue;
            }
            ResourceKey<Level> dimension = dimension(s.getString("Dimension"));
            ResourceKey<Level> cryptDimension = s.contains("CryptDimension", Tag.TAG_STRING) ? dimension(s.getString("CryptDimension"))
                    : s.getBoolean("InAltar") ? dimension : Level.OVERWORLD;
            BlockPos crypt = NbtUtils.readBlockPos(s, "Crypt").orElse(BlockPos.ZERO);
            Soul soul = data.new Soul(s.getUUID("Id"), dimension, cryptDimension, crypt);
            soul.anchor = NbtUtils.readBlockPos(s, "Anchor").orElse(crypt);
            soul.cryptFacing = Direction.from3DDataValue(s.getInt("CryptFacing"));
            if (soul.cryptFacing.getAxis().isVertical()) {
                soul.cryptFacing = Direction.NORTH;
            }
            soul.inAltar = s.getBoolean("InAltar");
            soul.carrier = s.hasUUID("Carrier") ? s.getUUID("Carrier") : null;
            soul.lichId = s.hasUUID("Lich") ? s.getUUID("Lich") : null;
            soul.reformHunting = s.hasUUID("ReformHunting") ? s.getUUID("ReformHunting") : null;
            soul.reformTicks = s.contains("ReformTicks", Tag.TAG_INT) ? s.getInt("ReformTicks") : -1;
            soul.soulTicks = s.getInt("SoulTicks");
            soul.ambushCooldown = s.getInt("AmbushCooldown");
            soul.burned = s.getBoolean("Burned");
            soul.fallen = s.getBoolean("Fallen");
            soul.cleansedCrypt = s.getBoolean("Cleansed");
            if (!soul.cleansedCrypt) {
                data.souls.put(soul.id, soul);
            }
        }
        for (Tag entry : tag.getList("Cleansed", Tag.TAG_COMPOUND)) {
            CompoundTag z = (CompoundTag) entry;
            ResourceKey<Level> dimension = dimension(z.getString("Dimension"));
            NbtUtils.readBlockPos(z, "Pos").ifPresent(pos -> data.cleansed.add(GlobalPos.of(dimension, pos)));
        }
        return data;
    }

    private static ResourceKey<Level> dimension(String id) {
        return ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(id));
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Soul soul : this.souls.values()) {
            list.add(soul.save());
        }
        tag.put("Souls", list);
        ListTag zones = new ListTag();
        for (GlobalPos zone : this.cleansed) {
            CompoundTag z = new CompoundTag();
            z.putString("Dimension", zone.dimension().location().toString());
            z.put("Pos", NbtUtils.writeBlockPos(zone.pos()));
            zones.add(z);
        }
        tag.put("Cleansed", zones);
        return tag;
    }
}
