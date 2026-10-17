package nl.townsandkingdoms.streamcontrol;
import java.util.*;
import net.minecraft.world.level.ChunkPos;
/** Chunk-indexed immutable read view: physics queries never load chunks or scan the entire world. */
public final class RegionIndex {
    private final Map<Long, List<FlowRegion>> chunks;
    public RegionIndex(Collection<FlowRegion> regions) {
        Map<Long, List<FlowRegion>> map = new HashMap<>();
        for (var r : regions) for (int cz = (r.center().getZ()-r.radius()) >> 4; cz <= (r.center().getZ()+r.radius()) >> 4; cz++)
            for (int cx = (r.center().getX()-r.radius()) >> 4; cx <= (r.center().getX()+r.radius()) >> 4; cx++)
                map.computeIfAbsent(ChunkPos.asLong(cx, cz), k -> new ArrayList<>()).add(r);
        map.replaceAll((key, value) -> List.copyOf(value)); chunks = Map.copyOf(map);
    }
    public FlowRegion at(int x, int y, int z) {
        var candidates = chunks.get(ChunkPos.asLong(x >> 4, z >> 4));
        if (candidates == null) return null;
        FlowRegion best = null;
        for (var r : candidates) if (r.contains(x, y, z) && (best == null || r.priority() > best.priority()
                || r.priority() == best.priority() && r.sequence() > best.sequence())) best = r;
        return best;
    }
}
