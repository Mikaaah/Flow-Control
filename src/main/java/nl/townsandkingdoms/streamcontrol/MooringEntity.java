package nl.townsandkingdoms.streamcontrol;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Holds one empty boat in place without loading chunks or pulling occupied boats. */
public final class MooringEntity extends BlockEntity implements UtilityAccess {
    private static final Map<ServerLevel,Map<UUID,BlockPos>> CLAIMS=new WeakHashMap<>();
    private int radius=4, vertical=2, cooldown;
    private boolean enabled=true;
    private UUID boatId;
    // Numeric ID is synchronized for O(1) client rendering, UUID remains authoritative on the server.
    private int boatEntityId=-1;
    private double moorX,moorZ;
    public MooringEntity(BlockPos p,BlockState s) { super(StreamControl.MOORING_ENTITY.get(),p,s); }
    @Override public BlockEntity blockEntity() { return this; }
    public UUID boatId() { return boatId; }
    public int boatEntityId() { return boatEntityId; }
    public boolean enabled() { return enabled; }
    public boolean secured() { return boatId != null; }
    public boolean operational() { return ControlConfig.ENABLED.get() && enabled && !getBlockState().getValue(UtilityBlock.POWERED); }
    public double moorX() { return moorX; }
    public double moorZ() { return moorZ; }
    /** Authored tie-ring pivot [8,9,3.5], rotated with the utility's horizontal facing. */
    public Vec3 ropeAnchor() {
        Direction facing=getBlockState().getValue(UtilityBlock.FACING);
        return new Vec3(worldPosition.getX()+.5+facing.getStepX()*.28125,
            worldPosition.getY()+.5625,worldPosition.getZ()+.5+facing.getStepZ()*.28125);
    }
    /** Called after a passenger was actually added; no world/entity scans and no client-only classes. */
    public static void releaseMounted(net.minecraft.world.entity.Entity vehicle) {
        if (!(vehicle.level() instanceof ServerLevel server)) return;
        var claims=CLAIMS.get(server); if(claims==null) return;
        BlockPos pos=claims.get(vehicle.getUUID());
        if(pos!=null && server.hasChunkAt(pos) && server.getBlockEntity(pos) instanceof MooringEntity post
                && vehicle.getUUID().equals(post.boatId)) post.release();
    }
    public int radius() { return nl.townsandkingdoms.streamcontrol.Numbers.clamp(radius,1,ControlConfig.MOORING_MAX_RADIUS.get()); }
    public int vertical() { return nl.townsandkingdoms.streamcontrol.Numbers.clamp(vertical,0,ControlConfig.MOORING_MAX_VERTICAL.get()); }
    @Override public CompoundTag settings() {
        var n=new CompoundTag();n.putString("Kind","mooring");n.putInt("Radius",radius());n.putInt("Vertical",vertical());
        n.putInt("MaxRadius",ControlConfig.MOORING_MAX_RADIUS.get());n.putInt("MaxVertical",ControlConfig.MOORING_MAX_VERTICAL.get());
        n.putBoolean("Enabled",enabled);n.putBoolean("Secured",boatId!=null);n.putBoolean("Powered",getBlockState().getValue(UtilityBlock.POWERED));return n;
    }
    @Override public void configure(CompoundTag n) {
        radius=nl.townsandkingdoms.streamcontrol.Numbers.clamp(n.getInt("Radius"),1,ControlConfig.MOORING_MAX_RADIUS.get());vertical=nl.townsandkingdoms.streamcontrol.Numbers.clamp(n.getInt("Vertical"),0,ControlConfig.MOORING_MAX_VERTICAL.get());
        enabled=n.getBoolean("Enabled");release();changed();
    }
    @Override protected void saveAdditional(CompoundTag n) {
        super.saveAdditional(n);n.putInt("Radius",radius);n.putInt("Vertical",vertical);n.putBoolean("Enabled",enabled);
        if(boatId!=null) { n.putUUID("Boat",boatId);n.putDouble("MoorX",moorX);n.putDouble("MoorZ",moorZ); }
    }
    @Override public void load(CompoundTag n) {
        super.load(n);radius=n.contains("Radius")?n.getInt("Radius"):4;vertical=n.contains("Vertical")?n.getInt("Vertical"):2;enabled=!n.contains("Enabled")||n.getBoolean("Enabled");
        boatId=n.hasUUID("Boat")?n.getUUID("Boat"):null;moorX=n.getDouble("MoorX");moorZ=n.getDouble("MoorZ");
        boatEntityId=n.contains("BoatEntityId")?n.getInt("BoatEntityId"):-1;if(boatId==null || !Double.isFinite(moorX) || !Double.isFinite(moorZ)) { boatId=null;boatEntityId=-1; }
    }
    private void changed() {
        setChanged();if(level instanceof ServerLevel server && !isRemoved()) server.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);
    }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() { return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag() { var n=saveWithoutMetadata();n.putInt("BoatEntityId",boatEntityId);return n; }
    public void release() { release(true); }
    private void release(boolean notify) {
        if(level instanceof ServerLevel server && boatId!=null) {
            var claims=CLAIMS.get(server); if(claims!=null) { claims.remove(boatId,worldPosition);if(claims.isEmpty()) CLAIMS.remove(server); }
        }
        if(boatId!=null || boatEntityId!=-1) { boatId=null;boatEntityId=-1;if(notify) changed(); } cooldown=40;
    }
    @Override public void setRemoved() { release(false);super.setRemoved(); }
    private boolean inRange(Boat boat) {
        double dx=boat.getX()-worldPosition.getX()-.5,dz=boat.getZ()-worldPosition.getZ()-.5;
        return dx*dx+dz*dz<=radius()*radius() && boat.getY()>=worldPosition.getY()-vertical() && boat.getY()<worldPosition.getY()+vertical()+1;
    }
    private boolean available(ServerLevel server,Boat boat) {
        var map=CLAIMS.get(server); if(map==null) return true;
        BlockPos other=map.get(boat.getUUID());
        return other==null || other.equals(worldPosition) || !server.hasChunkAt(other)
            || !(server.getBlockEntity(other) instanceof MooringEntity post) || !boat.getUUID().equals(post.boatId);
    }
    private boolean claim(ServerLevel server,Boat boat) {
        if(!available(server,boat)) return false;
        CLAIMS.computeIfAbsent(server,s->new HashMap<>()).put(boat.getUUID(),worldPosition.immutable());return true;
    }
    public static void tick(Level l,BlockPos p,BlockState s,MooringEntity be) { be.tick(); }
    public void tick() {
        if(!(level instanceof ServerLevel server)) return;
        if(!operational()) { if(boatId!=null) release();return; }
        if(cooldown>0) { cooldown--;return; }
        if(boatId!=null) {
            var entity=server.getEntity(boatId);
            if(!(entity instanceof Boat boat) || !boat.isAlive() || boat.isVehicle() || !inRange(boat)
                || Math.hypot(boat.getX()-moorX,boat.getZ()-moorZ)>1.5 || !claim(server,boat)) { release();return; }
            // Preserve vertical bobbing; horizontal momentum is canceled while moored.
            if(boatEntityId!=boat.getId()) { boatEntityId=boat.getId();changed(); }
            var velocity=boat.getDeltaMovement();
            boolean correcting=Math.abs(boat.getX()-moorX)>1e-7 || Math.abs(boat.getZ()-moorZ)>1e-7
                || Math.abs(velocity.x)>1e-7 || Math.abs(velocity.z)>1e-7;
            if(correcting) { boat.setDeltaMovement(0,velocity.y,0);boat.setPos(moorX,boat.getY(),moorZ);boat.hasImpulse=true; }
            return;
        }
        if(Math.floorMod(server.getGameTime()+worldPosition.asLong(),10)!=0) return;
        var candidates=server.getEntitiesOfClass(Boat.class,new AABB(worldPosition).inflate(radius(),vertical(),radius()),
            boat->boat.isAlive() && !boat.isVehicle() && inRange(boat));
        Boat nearest=null;double nearestDistance=Double.POSITIVE_INFINITY;
        for(Boat boat:candidates) {
            double distance=boat.distanceToSqr(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5);
            if(distance<nearestDistance && available(server,boat)) { nearest=boat;nearestDistance=distance; }
        }
        if(nearest!=null && claim(server,nearest)) { boatId=nearest.getUUID();boatEntityId=nearest.getId();moorX=nearest.getX();moorZ=nearest.getZ();changed(); }
    }
}
