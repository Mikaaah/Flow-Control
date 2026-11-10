package nl.townsandkingdoms.streamcontrol;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Holds one empty boat in place without loading chunks or pulling occupied boats. */
public final class MooringEntity extends BlockEntity implements UtilityAccess {
    private static final Map<ServerLevel,Map<UUID,BlockPos>> CLAIMS=new WeakHashMap<>();
    private int radius=4, vertical=2, cooldown;
    private boolean enabled=true;
    private UUID boatId;
    private double moorX,moorZ;
    public MooringEntity(BlockPos p,BlockState s) { super(StreamControl.MOORING_ENTITY.get(),p,s); }
    @Override public BlockEntity blockEntity() { return this; }
    public UUID boatId() { return boatId; }
    public int radius() { return Math.clamp(radius,1,ControlConfig.MOORING_MAX_RADIUS.get()); }
    public int vertical() { return Math.clamp(vertical,0,ControlConfig.MOORING_MAX_VERTICAL.get()); }
    @Override public CompoundTag settings() {
        var n=new CompoundTag();n.putString("Kind","mooring");n.putInt("Radius",radius());n.putInt("Vertical",vertical());
        n.putInt("MaxRadius",ControlConfig.MOORING_MAX_RADIUS.get());n.putInt("MaxVertical",ControlConfig.MOORING_MAX_VERTICAL.get());
        n.putBoolean("Enabled",enabled);n.putBoolean("Secured",boatId!=null);n.putBoolean("Powered",getBlockState().getValue(UtilityBlock.POWERED));return n;
    }
    @Override public void configure(CompoundTag n) {
        radius=Math.clamp(n.getIntOr("Radius",0),1,ControlConfig.MOORING_MAX_RADIUS.get());vertical=Math.clamp(n.getIntOr("Vertical",0),0,ControlConfig.MOORING_MAX_VERTICAL.get());
        enabled=n.getBooleanOr("Enabled",false);release();changed();
    }
    @Override protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput out) {
        var n=new CompoundTag();n.putInt("Radius",radius);n.putInt("Vertical",vertical);n.putBoolean("Enabled",enabled);
        if(boatId!=null) { n.store("Boat",net.minecraft.core.UUIDUtil.CODEC,boatId);n.putDouble("MoorX",moorX);n.putDouble("MoorZ",moorZ); }out.store("Engineering",CompoundTag.CODEC,n); }
    @Override protected void loadAdditional(net.minecraft.world.level.storage.ValueInput in) {
        super.loadAdditional(in);var n=in.read("Engineering",CompoundTag.CODEC).orElseGet(CompoundTag::new);radius=n.contains("Radius")?n.getIntOr("Radius",0):4;vertical=n.contains("Vertical")?n.getIntOr("Vertical",0):2;enabled=!n.contains("Enabled")||n.getBooleanOr("Enabled",false);
        boatId=n.contains("Boat")?n.read("Boat",net.minecraft.core.UUIDUtil.CODEC).orElse(null):null;moorX=n.getDoubleOr("MoorX",0.0);moorZ=n.getDoubleOr("MoorZ",0.0);
    }
    private void changed() {
        setChanged();if(level instanceof ServerLevel server && !isRemoved()) server.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);
    }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() { return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r) { return saveWithoutMetadata(r); }
    public void release() { release(true); }
    private void release(boolean notify) {
        if(level instanceof ServerLevel server && boatId!=null) CLAIMS.computeIfAbsent(server,s->new HashMap<>()).remove(boatId,worldPosition);
        if(boatId!=null) { boatId=null;if(notify) changed(); } cooldown=40;
    }
    @Override public void setRemoved() { release(false);super.setRemoved(); }
    private boolean inRange(AbstractBoat boat) {
        double dx=boat.getX()-worldPosition.getX()-.5,dz=boat.getZ()-worldPosition.getZ()-.5;
        return dx*dx+dz*dz<=radius()*radius() && boat.getY()>=worldPosition.getY()-vertical() && boat.getY()<worldPosition.getY()+vertical()+1;
    }
    private boolean claim(ServerLevel server,AbstractBoat boat) {
        var map=CLAIMS.computeIfAbsent(server,s->new HashMap<>());BlockPos other=map.get(boat.getUUID());
        if(other!=null && !other.equals(worldPosition) && server.hasChunkAt(other)
            && server.getBlockEntity(other) instanceof MooringEntity post && boat.getUUID().equals(post.boatId)) return false;
        map.put(boat.getUUID(),worldPosition.immutable());return true;
    }
    public static void tick(Level l,BlockPos p,BlockState s,MooringEntity be) { be.tick(); }
    public void tick() {
        if(!(level instanceof ServerLevel server)) return;
        if(!ControlConfig.ENABLED.get() || !enabled || getBlockState().getValue(UtilityBlock.POWERED)) { if(boatId!=null) release();return; }
        if(cooldown>0) { cooldown--;return; }
        if(boatId!=null) {
            var entity=server.getEntity(boatId);
            if(!(entity instanceof AbstractBoat boat) || !boat.isAlive() || boat.isVehicle() || !inRange(boat)
                || Math.hypot(boat.getX()-moorX,boat.getZ()-moorZ)>1.5 || !claim(server,boat)) { release();return; }
            // Preserve vertical bobbing; horizontal momentum is canceled while moored.
            var velocity=boat.getDeltaMovement();boat.setDeltaMovement(0,velocity.y,0);boat.setPos(moorX,boat.getY(),moorZ);boat.syncVelocity=true;
            return;
        }
        if(Math.floorMod(server.getGameTime()+worldPosition.asLong(),10)!=0) return;
        var candidates=server.getEntitiesOfClass(AbstractBoat.class,new AABB(worldPosition).inflate(radius(),vertical(),radius()),
            boat->boat.isAlive() && !boat.isVehicle() && inRange(boat));
        candidates.sort(Comparator.comparingDouble(b->b.distanceToSqr(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5)));
        for(AbstractBoat boat:candidates) if(claim(server,boat)) { boatId=boat.getUUID();moorX=boat.getX();moorZ=boat.getZ();changed();break; }
    }
}
