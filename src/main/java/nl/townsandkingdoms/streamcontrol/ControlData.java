package nl.townsandkingdoms.streamcontrol;
import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
public final class ControlData extends SavedData {
    private final Map<Long, FlowRegion> regions = new LinkedHashMap<>();
    private volatile RegionIndex index = new RegionIndex(List.of());
    private long sequence;
    public static final net.minecraft.world.level.saveddata.SavedDataType<ControlData> TYPE = new net.minecraft.world.level.saveddata.SavedDataType<>("streamcontrol_regions", ControlData::new, CompoundTag.CODEC.xmap(ControlData::load, ControlData::save), null);
    public static ControlData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }
    public static ControlData load(CompoundTag tag) {
        var data = new ControlData();
        var list = tag.getListOrEmpty("Regions");
        for (int i = 0; i < Math.min(list.size(), 8192); i++) {
            var r = FlowRegion.load(list.getCompoundOrEmpty(i)); data.regions.put(r.key(), r);
            data.sequence = Math.max(data.sequence, r.sequence());
        }
        data.reindex(); return data;
    }
    public CompoundTag save() {
        var tag = new CompoundTag();
        var list = new ListTag(); for (var r : regions.values()) list.add(r.save()); tag.put("Regions", list); return tag;
    }
    public Collection<FlowRegion> all() { return List.copyOf(regions.values()); }
    public FlowRegion at(int x, int y, int z) { return index.at(x, y, z); }
    public FlowRegion byKey(long key) { return regions.get(key); }
    public long next() { return ++sequence; }
    public boolean put(ServerLevel level, FlowRegion r) {
        var prior = regions.get(r.key());
        if (prior != null && (!prior.center().equals(r.center()) || prior.controller() != r.controller())) return false;
        if (!regions.containsKey(r.key()) && regions.size() >= ControlConfig.MAX_REGIONS.get()) return false;
        regions.put(r.key(), r); reindex(); setDirty(); ControlNetwork.update(level, r); return true;
    }
    public void remove(ServerLevel level, long key) {
        if (regions.remove(key) == null) return;
        reindex(); setDirty(); ControlNetwork.remove(level, key);
    }
    public int owned(UUID player) { return (int)regions.values().stream().filter(r -> !r.controller() && r.owner().equals(player)).count(); }
    private void reindex() { index = new RegionIndex(regions.values()); }
}
