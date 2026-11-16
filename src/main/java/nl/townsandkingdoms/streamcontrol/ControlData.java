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
    private boolean runtimeValidated;
    public static ControlData get(ServerLevel level) {
        var data=level.getDataStorage().computeIfAbsent(new Factory<>(ControlData::new, ControlData::load), "streamcontrol_regions"); data.validateRuntime(level); return data;
    }
    public static ControlData load(CompoundTag tag, HolderLookup.Provider provider) {
        var data = new ControlData();
        var list = tag.getList("Regions", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(list.size(), 8192); i++) {
            var r = FlowRegion.load(list.getCompound(i)); data.regions.put(r.key(), r);
            data.sequence = Math.max(data.sequence, r.sequence());
        }
        data.reindex(); return data;
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        var list = new ListTag(); for (var r : regions.values()) list.add(r.save()); tag.put("Regions", list); return tag;
    }
    private void validateRuntime(ServerLevel level) {
        if(runtimeValidated) return;
        runtimeValidated=true;
        // Never load controller chunks merely because an old SavedData record exists.
        removeMatching(level,r->r.controller() && (!level.hasChunkAt(r.center())
            || !(level.getBlockEntity(r.center()) instanceof FlowController)));
    }
    /** Bulk command cleanup performs one spatial-index rebuild, not one per region. */
    public int removeMatching(ServerLevel level,java.util.function.Predicate<FlowRegion> predicate) {
        var removed=new ArrayList<Long>();
        var iterator=regions.entrySet().iterator();
        while(iterator.hasNext()) {
            var entry=iterator.next();
            if(predicate.test(entry.getValue())) { removed.add(entry.getKey());iterator.remove(); }
        }
        if(removed.isEmpty()) return 0;
        reindex();setDirty();
        for(long key:removed) ControlNetwork.remove(level,key);
        return removed.size();
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
