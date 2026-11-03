package nl.townsandkingdoms.streamcontrol;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Bounded, staggered sampling of loaded chunks; vertical reach is independent of horizontal range. */
public final class FlowSensorEntity extends BlockEntity implements UtilityAccess {
    public enum Mode { SPEED, DIRECTION, BOATS;
        public static Mode from(int id) { return values()[nl.townsandkingdoms.streamcontrol.Numbers.clamp(id,0,values().length-1)]; }
    }
    private int radius=4, vertical=4, output;
    private Mode mode=Mode.SPEED;
    private boolean emptyOnly;
    private double measuredSpeed;
    private FlowHeading measuredHeading;
    public FlowSensorEntity(BlockPos p,BlockState s) { super(StreamControl.SENSOR_ENTITY.get(),p,s); }
    @Override public BlockEntity blockEntity() { return this; }
    public int output() { return output; }
    public int radius() { return nl.townsandkingdoms.streamcontrol.Numbers.clamp(radius,1,ControlConfig.SENSOR_MAX_RADIUS.get()); }
    public int vertical() { return nl.townsandkingdoms.streamcontrol.Numbers.clamp(vertical,0,ControlConfig.SENSOR_MAX_VERTICAL.get()); }
    public Mode mode() { return mode; }
    @Override public CompoundTag settings() {
        var n=new CompoundTag();n.putString("Kind","sensor");n.putInt("Radius",radius());n.putInt("Vertical",vertical());
        n.putInt("MaxRadius",ControlConfig.SENSOR_MAX_RADIUS.get());n.putInt("MaxVertical",ControlConfig.SENSOR_MAX_VERTICAL.get());
        n.putInt("Mode",mode.ordinal());n.putInt("Output",output);n.putBoolean("EmptyOnly",emptyOnly);
        n.putDouble("MeasuredSpeed",measuredSpeed);n.putInt("MeasuredHeading",measuredHeading==null?-1:measuredHeading.id());return n;
    }
    @Override public void configure(CompoundTag n) {
        radius=nl.townsandkingdoms.streamcontrol.Numbers.clamp(n.getInt("Radius"),1,ControlConfig.SENSOR_MAX_RADIUS.get());
        vertical=nl.townsandkingdoms.streamcontrol.Numbers.clamp(n.getInt("Vertical"),0,ControlConfig.SENSOR_MAX_VERTICAL.get());
        mode=Mode.from(n.getInt("Mode"));emptyOnly=n.getBoolean("EmptyOnly");setChanged();sample();
        if(level instanceof ServerLevel server) server.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);
    }
    @Override protected void saveAdditional(CompoundTag n) { super.saveAdditional(n);n.putInt("Radius",radius);n.putInt("Vertical",vertical);n.putInt("Mode",mode.ordinal());n.putBoolean("EmptyOnly",emptyOnly);n.putInt("Output",output); }
    @Override public void load(CompoundTag n) { super.load(n);radius=n.contains("Radius")?n.getInt("Radius"):4;vertical=n.contains("Vertical")?n.getInt("Vertical"):4;mode=Mode.from(n.getInt("Mode"));emptyOnly=n.getBoolean("EmptyOnly");output=nl.townsandkingdoms.streamcontrol.Numbers.clamp(n.getInt("Output"),0,15); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    public static void tick(Level l,BlockPos p,BlockState s,FlowSensorEntity be) {
        int interval=ControlConfig.SENSOR_INTERVAL.get();
        if(Math.floorMod(l.getGameTime()+p.asLong(),interval)==0) be.sample();
    }
    public static int speedSignal(double speed) { return speed<=1e-6 || !Double.isFinite(speed) ? 0 : nl.townsandkingdoms.streamcontrol.Numbers.clamp((int)Math.ceil(speed/ControlConfig.MAX_SPEED.get()*15),1,15); }
    public void sample() {
        if(!(level instanceof ServerLevel server)) return;
        int result=0;double best=0;FlowHeading heading=null;int r=radius(),v=vertical();
        if(ControlConfig.ENABLED.get()) {
            if(mode==Mode.BOATS) {
                AABB box=new AABB(worldPosition).inflate(r,v,r);
                result=Math.min(15,server.getEntitiesOfClass(Boat.class,box,b->b.isAlive() && (!emptyOnly || !b.isVehicle())
                    && inside(b.getX(),b.getY(),b.getZ(),r,v)).size());
            } else {
                var p=new BlockPos.MutableBlockPos();
                for(int x=-r;x<=r;x++) for(int z=-r;z<=r;z++) {
                    if(x*x+z*z>r*r) continue;
                    for(int y=-v;y<=v;y++) {
                        p.setWithOffset(worldPosition,x,y,z);if(!server.hasChunkAt(p)) continue;
                        var fluid=server.getFluidState(p);if(!fluid.is(FluidTags.WATER)) continue;
                        var flow=fluid.getFlow(server,p);double speed=Math.hypot(flow.x,flow.z);
                        if(Double.isFinite(speed) && speed>best+1e-6) { best=speed;heading=FlowHeading.fromVector(flow.x,flow.z); }
                    }
                }
                result=mode==Mode.SPEED ? speedSignal(best) : heading==null?0:heading.id()+1;
            }
        }
        measuredSpeed=best;measuredHeading=heading;
        if(output!=result) { output=result;setChanged();server.updateNeighbourForOutputSignal(worldPosition,getBlockState().getBlock());server.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2); }
    }
    private boolean inside(double x,double y,double z,int r,int v) {
        double dx=x-worldPosition.getX()-.5,dz=z-worldPosition.getZ()-.5;
        return dx*dx+dz*dz<=r*r && y>=worldPosition.getY()-v && y<worldPosition.getY()+v+1;
    }
}
