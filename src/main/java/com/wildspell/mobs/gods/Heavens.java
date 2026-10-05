package com.wildspell.mobs.gods;

import com.wildspell.mobs.SpawnBalance;
import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.entity.Apollo;
import com.wildspell.mobs.entity.Diana;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public class Heavens extends SavedData {
    private static final String NAME = "wildspellmobs_heavens";
    public static final String SPARED = WildspellMobs.MODID + ":spared";
    public static final String APOLLO = "apollo";
    public static final String DIANA = "diana";
    public static final long NIGHT_TIME = 18000L;
    private static final int RETURN_SECONDS = 5;

    private boolean sunSlain;
    private final Set<UUID> sunSlayers = new HashSet<>();
    private int apolloSpared;
    private final Map<UUID, Set<String>> pending = new HashMap<>();
    @Nullable
    private Hunt hunt;

    private final Map<UUID, Integer> sunGaze = new HashMap<>();
    private final Map<UUID, Integer> moonGaze = new HashMap<>();
    @Nullable
    private UUID apollo;

    public static Heavens get(ServerLevel anyLevel) {
        return get(anyLevel.getServer());
    }

    public static Heavens get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(Heavens::new, Heavens::load), NAME);
    }

    public boolean sunSlain() {
        return this.sunSlain;
    }

    public boolean isSunSlayer(UUID player) {
        return this.sunSlayers.contains(player);
    }

    public boolean apolloIsWary() {
        return this.apolloSpared > 0;
    }

    @Nullable
    public Hunt hunt() {
        return this.hunt;
    }

    public static void markSpared(Player player, String god) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        ListTag spared = persisted.getList(SPARED, Tag.TAG_STRING);
        if (!spared.contains(StringTag.valueOf(god))) {
            spared.add(StringTag.valueOf(god));
        }
        persisted.put(SPARED, spared);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }

    public static boolean hasSpared(Player player, String god) {
        return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getList(SPARED, Tag.TAG_STRING).contains(StringTag.valueOf(god));
    }

    public void spare(MinecraftServer server, UUID player, String god) {
        ServerPlayer online = server.getPlayerList().getPlayer(player);
        if (online != null) {
            markSpared(online, god);
        } else {
            this.pending.computeIfAbsent(player, id -> new LinkedHashSet<>()).add(god);
            this.setDirty();
        }
    }

    public void apolloSpared() {
        this.apolloSpared++;
        this.setDirty();
    }

    public void slayTheSun(MinecraftServer server, Set<UUID> slayers) {
        this.sunSlain = true;
        this.sunSlayers.addAll(slayers);
        this.setDirty();
        this.holdTheNight(server);
    }

    public void holdTheNight(MinecraftServer server) {
        if (!this.sunSlain) {
            return;
        }
        ServerLevel overworld = server.overworld();
        GameRules.BooleanValue daylight = overworld.getGameRules().getRule(GameRules.RULE_DAYLIGHT);
        if (daylight.get()) {
            daylight.set(false, server);
        }
        long time = overworld.getDayTime();
        if (Math.floorMod(time, 24000L) != NIGHT_TIME) {
            overworld.setDayTime(time - Math.floorMod(time, 24000L) + NIGHT_TIME);
        }
    }

    Set<String> pending(UUID player) {
        return this.pending.getOrDefault(player, Set.of());
    }

    void forgetPending(UUID player) {
        this.pending.remove(player);
    }

    void addSlayer(UUID player) {
        this.sunSlayers.add(player);
    }

    void restoreTheSun() {
        this.sunSlain = false;
        this.sunSlayers.clear();
        this.apolloSpared = 0;
        this.setDirty();
    }

    public boolean apolloCanCome(MinecraftServer server) {
        return !this.sunSlain && this.apollo(server) == null;
    }

    @Nullable
    public Apollo apollo(MinecraftServer server) {
        if (this.apollo == null) {
            return null;
        }
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getEntity(this.apollo) instanceof Apollo found && found.isAlive()) {
                return found;
            }
        }
        this.apollo = null;
        return null;
    }

    public void apolloArrived(Apollo arrived) {
        this.apollo = arrived.getUUID();
    }

    public void apolloGone(Apollo gone) {
        if (gone.getUUID().equals(this.apollo)) {
            this.apollo = null;
        }
    }

    public Hunt beginHunt(ServerLevel origin, boolean grieving) {
        this.hunt = new Hunt(origin.dimension(), grieving);
        this.setDirty();
        return this.hunt;
    }

    public void endHunt(MinecraftServer server, boolean released, @Nullable String message) {
        Hunt ending = this.hunt;
        if (ending == null) {
            return;
        }
        this.hunt = null;
        this.setDirty();
        for (UUID quarry : ending.quarry) {
            if (released) {
                this.spare(server, quarry, DIANA);
            }
            ServerPlayer player = server.getPlayerList().getPlayer(quarry);
            if (message != null && player != null) {
                player.displayClientMessage(Component.translatable(message).withStyle(ChatFormatting.AQUA), false);
            }
        }
        Diana diana = ending.diana(server);
        if (diana != null) {
            diana.depart(released);
        }
    }

    void forgetHunt() {
        this.hunt = null;
        this.setDirty();
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        Heavens heavens = get(server);
        long time = server.overworld().getGameTime();
        if (time % 40 == 3) {
            heavens.holdTheNight(server);
        }
        if (time % 20 != 11) {
            return;
        }
        heavens.tickHunt(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            heavens.watch(player);
        }
    }

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        Heavens heavens = get(level);
        Set<String> owed = heavens.pending.remove(event.getEntity().getUUID());
        if (owed != null) {
            owed.forEach(god -> markSpared(event.getEntity(), god));
            heavens.setDirty();
        }
    }

    private void watch(ServerPlayer player) {
        int seconds = SpawnBalance.GOD_GAZE_SECONDS.get();
        if (this.gazeAt(player, Gaze.SUN, this.sunGaze, this.apolloCanCome(player.server)) >= seconds) {
            this.sunGaze.remove(player.getUUID());
            Apollo.descend(player.serverLevel(), player, this.apolloIsWary());
        }
        Hunt current = this.hunt;
        boolean hunted = current != null && current.quarry.contains(player.getUUID());
        if (this.gazeAt(player, Gaze.MOON, this.moonGaze, !hunted) >= seconds) {
            this.moonGaze.remove(player.getUUID());
            this.callDiana(player);
        }
    }

    private int gazeAt(ServerPlayer player, Gaze body, Map<UUID, Integer> held, boolean open) {
        if (!open || !body.holds(player)) {
            held.remove(player.getUUID());
            return 0;
        }
        int seconds = held.merge(player.getUUID(), 1, Integer::sum);
        float rise = (float) seconds / SpawnBalance.GOD_GAZE_SECONDS.get();
        player.playNotifySound(body == Gaze.SUN ? SoundEvents.BEACON_AMBIENT : SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.AMBIENT, 0.6F + rise, 0.6F + rise * 0.9F);
        return seconds;
    }

    private void callDiana(ServerPlayer player) {
        if (this.hunt == null) {
            this.beginHunt(player.serverLevel(), this.sunSlain);
        }
        this.hunt.quarry.add(player.getUUID());
        this.setDirty();
        Diana diana = this.hunt.diana(player.server);
        if (diana == null) {
            Diana.arrive(player.serverLevel(), player, this.hunt);
        } else {
            diana.greet(player);
        }
    }

    private void tickHunt(MinecraftServer server) {
        Hunt current = this.hunt;
        if (current == null) {
            return;
        }
        ServerLevel origin = server.getLevel(current.origin);
        if (origin == null) {
            this.endHunt(server, false, null);
            return;
        }
        for (UUID id : List.copyOf(current.quarry)) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null && !player.isAlive()) {
                current.quarry.remove(id);
                this.setDirty();
                player.displayClientMessage(Component.translatable("message.wildspellmobs.diana_caught"), false);
            }
        }
        if (Gaze.SUN.isUp(origin)) {
            this.dawn(server, current);
        }
        if (this.hunt == null) {
            return;
        }
        if (current.quarry.isEmpty()) {
            this.endHunt(server, false, null);
            return;
        }
        if (current.diana(server) == null && ++current.absentSeconds >= RETURN_SECONDS) {
            current.absentSeconds = 0;
            ServerPlayer quarry = current.nextQuarry(server);
            if (quarry != null) {
                Diana.arrive(quarry.serverLevel(), quarry, current);
            }
        }
    }

    void dawn(MinecraftServer server, Hunt current) {
        List<UUID> released = new ArrayList<>();
        for (UUID id : current.quarry) {
            if (!(current.grieving && this.sunSlayers.contains(id))) {
                released.add(id);
            }
        }
        if (released.size() == current.quarry.size()) {
            this.endHunt(server, true, "message.wildspellmobs.diana_dawn");
            return;
        }
        for (UUID id : released) {
            current.quarry.remove(id);
            this.spare(server, id, DIANA);
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.wildspellmobs.diana_dawn_grieving"), false);
            }
        }
        this.setDirty();
    }

    public static Heavens load(CompoundTag tag, HolderLookup.Provider registries) {
        Heavens heavens = new Heavens();
        heavens.sunSlain = tag.getBoolean("SunSlain");
        for (Tag id : tag.getList("SunSlayers", Tag.TAG_INT_ARRAY)) {
            heavens.sunSlayers.add(NbtUtils.loadUUID(id));
        }
        heavens.apolloSpared = tag.getInt("ApolloSpared");
        CompoundTag pending = tag.getCompound("Pending");
        for (String key : pending.getAllKeys()) {
            Set<String> gods = new LinkedHashSet<>();
            for (Tag god : pending.getList(key, Tag.TAG_STRING)) {
                gods.add(god.getAsString());
            }
            heavens.pending.put(UUID.fromString(key), gods);
        }
        if (tag.contains("Hunt", Tag.TAG_COMPOUND)) {
            heavens.hunt = Hunt.load(tag.getCompound("Hunt"));
        }
        return heavens;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("SunSlain", this.sunSlain);
        ListTag slayers = new ListTag();
        this.sunSlayers.forEach(id -> slayers.add(NbtUtils.createUUID(id)));
        tag.put("SunSlayers", slayers);
        tag.putInt("ApolloSpared", this.apolloSpared);
        CompoundTag pending = new CompoundTag();
        this.pending.forEach((id, gods) -> {
            ListTag list = new ListTag();
            gods.forEach(god -> list.add(StringTag.valueOf(god)));
            pending.put(id.toString(), list);
        });
        tag.put("Pending", pending);
        if (this.hunt != null) {
            tag.put("Hunt", this.hunt.save());
        }
        return tag;
    }

    public static final class Hunt {
        public final ResourceKey<Level> origin;
        public final boolean grieving;
        private final Set<UUID> quarry = new LinkedHashSet<>();
        private float health = -1.0F;
        @Nullable
        private UUID diana;
        private int absentSeconds;

        Hunt(ResourceKey<Level> origin, boolean grieving) {
            this.origin = origin;
            this.grieving = grieving;
        }

        public Set<UUID> quarry() {
            return this.quarry;
        }

        public void add(Player player) {
            this.quarry.add(player.getUUID());
        }

        public float health() {
            return this.health;
        }

        public void setHealth(float health) {
            this.health = health;
        }

        public boolean isDiana(Diana entity) {
            return entity.getUUID().equals(this.diana);
        }

        public void claim(Diana entity) {
            this.diana = entity.getUUID();
            this.absentSeconds = 0;
        }

        public void release(Diana entity) {
            if (this.isDiana(entity)) {
                this.diana = null;
            }
        }

        @Nullable
        public Diana diana(MinecraftServer server) {
            if (this.diana == null) {
                return null;
            }
            for (ServerLevel level : server.getAllLevels()) {
                if (level.getEntity(this.diana) instanceof Diana found && found.isAlive()) {
                    return found;
                }
            }
            this.diana = null;
            return null;
        }

        @Nullable
        ServerPlayer nextQuarry(MinecraftServer server) {
            for (UUID id : this.quarry) {
                ServerPlayer player = server.getPlayerList().getPlayer(id);
                if (player != null && player.isAlive() && !player.isSpectator()) {
                    return player;
                }
            }
            return null;
        }

        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Origin", this.origin.location().toString());
            tag.putBoolean("Grieving", this.grieving);
            ListTag quarry = new ListTag();
            this.quarry.forEach(id -> quarry.add(NbtUtils.createUUID(id)));
            tag.put("Quarry", quarry);
            tag.putFloat("Health", this.health);
            return tag;
        }

        static Hunt load(CompoundTag tag) {
            ResourceKey<Level> origin = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(tag.getString("Origin")));
            Hunt hunt = new Hunt(origin, tag.getBoolean("Grieving"));
            for (Tag id : tag.getList("Quarry", Tag.TAG_INT_ARRAY)) {
                hunt.quarry.add(NbtUtils.loadUUID(id));
            }
            hunt.health = tag.getFloat("Health");
            return hunt;
        }
    }
}
