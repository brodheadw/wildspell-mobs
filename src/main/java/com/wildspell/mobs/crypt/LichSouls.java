package com.wildspell.mobs.crypt;

import com.wildspell.mobs.entity.ColdEffects;
import com.wildspell.mobs.entity.IceLich;
import com.wildspell.mobs.entity.LichWisp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Every lich's soul, wherever its phylactery has gone. A phylactery starts on its crypt's altar (a
 * {@link PhylacteryBlockEntity}), but once its wards are broken it can be carried off as an item, into
 * a chest or another dimension; the soul's state lives here, in the world's saved data, so it
 * follows. One {@link Soul} per lich:
 * <ul>
 *   <li>its <b>anchor</b>, where the phylactery is (on the altar, or wherever it was last seen: a
 *       player carrying it, an item on the ground, the spot it was put in a chest);</li>
 *   <li>its form: a body ({@link Soul#lichId}), a soul in flight home ({@link Soul#soulTicks}), or a
 *       re-forming countdown ({@link Soul#reformTicks}). It has at most one at a time;</li>
 *   <li>whether its phylactery has burned (the only way to destroy one), and once its last form has
 *       fallen, whether its crypt has been cleansed ({@link Cleansing}).</li>
 * </ul>
 * Cleansed crypts are remembered as safe zones where the cold's undead no longer rise.
 */
public class LichSouls extends SavedData {
    private static final String NAME = "wildspellmobs_lich_souls";
    /** Ticks a lich can go unfound (while its phylactery's area is loaded) before its soul stops waiting for it. */
    private static final int MISSING_LIMIT = 60 * 20;
    /** Radius around a cleansed crypt where Frozen Zombies don't freeze and Rime Skulls don't spawn. */
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

    /** A new soul, for a phylactery on its altar at {@code altar}. */
    public Soul create(ServerLevel level, BlockPos altar) {
        Soul soul = new Soul(UUID.randomUUID(), level.dimension(), altar.immutable());
        if (level.getBlockState(altar).hasProperty(PhylacteryBlock.FACING)) {
            soul.cryptFacing = level.getBlockState(altar).getValue(PhylacteryBlock.FACING);
        }
        this.souls.put(soul.id, soul);
        this.setDirty();
        return soul;
    }

    /** True within {@link #SAFE_RADIUS} of a cleansed crypt. */
    public boolean isCleansed(Level level, BlockPos pos) {
        for (GlobalPos zone : this.cleansed) {
            if (zone.dimension() == level.dimension() && zone.pos().distSqr(pos) < SAFE_RADIUS * SAFE_RADIUS) {
                return true;
            }
        }
        return false;
    }

    /** Gametests: forget a safe zone, so it can't reach into the next test's arena. */
    public void forgetCleansed(Level level, BlockPos crypt) {
        this.cleansed.removeIf(zone -> zone.dimension() == level.dimension() && zone.pos().equals(crypt));
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
            ServerLevel level = server.getLevel(soul.dimension);
            if (level != null) {
                soul.tick(level, time);
            }
        }
        // From here, not the phylacteries, so no crypt need be loaded for a lich to sense a player far off.
        if (time % 20 == 7) {
            for (ServerLevel level : server.getAllLevels()) {
                PhylacteryBlockEntity.ambushFromAfar(level, all);
            }
        }
    }

    /** One lich's soul. */
    public final class Soul {
        public final UUID id;
        private ResourceKey<Level> dimension;
        /** The altar the phylactery was made on: where the crypt is, and where its cleansing happens. */
        private final BlockPos crypt;
        /** Which way the altar faces, into the crypt. */
        private net.minecraft.core.Direction cryptFacing = net.minecraft.core.Direction.NORTH;
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

        Soul(UUID id, ResourceKey<Level> dimension, BlockPos crypt) {
            this.id = id;
            this.dimension = dimension;
            this.crypt = crypt;
            this.anchor = crypt;
        }

        public ResourceKey<Level> dimension() {
            return this.dimension;
        }

        public BlockPos crypt() {
            return this.crypt;
        }

        public net.minecraft.core.Direction cryptFacing() {
            return this.cryptFacing;
        }

        /** Where the phylactery is, or was last seen. */
        public BlockPos anchor() {
            return this.anchor;
        }

        public boolean inAltar() {
            return this.inAltar;
        }

        /** Its phylactery has burned: the lich is mortal. */
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

        /** No body, no soul in flight and no re-forming under way: the lich is waiting to rise. */
        public boolean dormant() {
            return this.lichId == null && this.soulTicks <= 0 && this.reformTicks < 0 && !this.burned;
        }

        private void changed() {
            LichSouls.this.setDirty();
        }

        /** The phylactery was set on an altar at {@code pos}. */
        void placeOnAltar(ServerLevel level, BlockPos pos) {
            this.dimension = level.dimension();
            this.anchor = pos.immutable();
            this.inAltar = true;
            this.carrier = null;
            this.changed();
        }

        /** The phylactery was taken off its altar, or moved while carried or lying about. */
        public void moved(Level level, BlockPos pos, @Nullable Entity carrier) {
            UUID carrierId = carrier == null ? null : carrier.getUUID();
            if (!this.inAltar && this.dimension == level.dimension() && this.anchor.equals(pos) && java.util.Objects.equals(this.carrier, carrierId)) {
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

        /** The phylactery's lich, if it's loaded and alive. */
        @Nullable
        public IceLich findLich(ServerLevel level) {
            return this.lichId != null && level.getEntity(this.lichId) instanceof IceLich lich && lich.isAlive() ? lich : null;
        }

        /**
         * A bound lich checking in. It's this soul's lich if it's the one on record, or if the soul has
         * lost track of its body and this one turns up; any other body is a stale copy.
         */
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

        /** Where the lich re-forms: over its altar, or beside wherever its phylactery has been taken. */
        @Nullable
        public Vec3 reformSpot(ServerLevel level) {
            if (this.inAltar) {
                return Vec3.atBottomCenterOf(this.anchor.above());
            }
            for (int attempt = 0; attempt < 24; ++attempt) {
                BlockPos pos = this.anchor.offset(level.random.nextInt(13) - 6, level.random.nextInt(5) - 1, level.random.nextInt(13) - 6);
                if (pos.distSqr(this.anchor) >= 9 && ColdEffects.isOpen(level, pos, 3)) {
                    return Vec3.atBottomCenterOf(pos);
                }
            }
            return ColdEffects.isOpen(level, this.anchor.above(), 3) ? Vec3.atBottomCenterOf(this.anchor.above()) : null;
        }

        /** Raises this soul's lich at {@code at}, hunting {@code prey} if given. Nothing rises on Peaceful. */
        @Nullable
        public IceLich raise(ServerLevel level, Vec3 at, @Nullable Player prey) {
            IceLich lich = level.getDifficulty() == Difficulty.PEACEFUL || this.burned ? null : IceLich.summon(level, at, this.id);
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

        /** Hurries a re-forming already under way. */
        public void hurryRise(int ticks) {
            if (this.reformTicks >= 0) {
                this.reformTicks = Math.min(this.reformTicks, ticks);
                this.changed();
            }
        }

        /** The lich's soul has flown home; it re-forms after a while. */
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

        /** The lich was struck down; its soul is on the way home. */
        public void onLichDiscorporated(IceLich lich) {
            if (lich.getUUID().equals(this.lichId)) {
                this.lichId = null;
                this.soulTicks = LichWisp.MAX_AGE;
                this.changed();
            }
        }

        /** The lich gave up its hunt and sank back into its phylactery. */
        public void onLichRetreated(IceLich lich) {
            if (lich.getUUID().equals(this.lichId)) {
                this.lichId = null;
                this.ambushCooldown = PhylacteryBlockEntity.RETREAT_COOLDOWN;
                this.changed();
            }
        }

        /**
         * The phylactery has burned: the lich is mortal. Its body, if it has one here, is torn back to
         * the flames; either way its last form rises there, mortal and enraged.
         */
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
                old.vanishInto(at);
            }
            this.lichId = null;
            ColdEffects.tellNearby(level, new AABB(BlockPos.containing(at)).inflate(64.0),
                    net.minecraft.network.chat.Component.translatable("message.wildspellmobs.phylactery_burned"));
            if (level.getDifficulty() != Difficulty.PEACEFUL) {
                Player prey = level.getNearestPlayer(at.x, at.y, at.z, 48.0, p -> p instanceof Player player && PhylacteryBlockEntity.isPrey(player));
                IceLich last = IceLich.summonLastForm(level, at.add(0.0, 1.0, 0.0), this.id);
                if (last != null && prey != null) {
                    last.hunt(prey);
                }
            }
        }

        /** Its last form has fallen: its hold on the caves breaks, and its crypt is cleansed as soon as it's loaded. */
        public void fall(ServerLevel level, Vec3 at) {
            if (this.fallen) {
                return;
            }
            this.fallen = true;
            this.changed();
            Cleansing.purge(level, at);
            ColdEffects.tellNearby(level, new AABB(BlockPos.containing(at)).inflate(64.0),
                    net.minecraft.network.chat.Component.translatable("message.wildspellmobs.lich_fallen"));
        }

        private void tick(ServerLevel level, long time) {
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
            if (this.fallen && !this.cleansedCrypt && time % 40 == 0 && level.isAreaLoaded(this.crypt, Cleansing.LOADED_RADIUS)) {
                this.cleansedCrypt = true;
                LichSouls.this.cleansed.add(GlobalPos.of(level.dimension(), this.crypt));
                this.changed();
                Cleansing.cleanse(level, this.crypt, this.cryptFacing);
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
                // Its phylactery's surroundings aren't loaded (or there's no room): try again shortly.
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
            ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(s.getString("Dimension")));
            BlockPos crypt = NbtUtils.readBlockPos(s, "Crypt").orElse(BlockPos.ZERO);
            Soul soul = data.new Soul(s.getUUID("Id"), dimension, crypt);
            soul.anchor = NbtUtils.readBlockPos(s, "Anchor").orElse(crypt);
            soul.cryptFacing = net.minecraft.core.Direction.from3DDataValue(s.getInt("CryptFacing"));
            if (soul.cryptFacing.getAxis().isVertical()) {
                soul.cryptFacing = net.minecraft.core.Direction.NORTH;
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
            data.souls.put(soul.id, soul);
        }
        for (Tag entry : tag.getList("Cleansed", Tag.TAG_COMPOUND)) {
            CompoundTag z = (CompoundTag) entry;
            ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(z.getString("Dimension")));
            NbtUtils.readBlockPos(z, "Pos").ifPresent(pos -> data.cleansed.add(GlobalPos.of(dimension, pos)));
        }
        return data;
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
