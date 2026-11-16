package nl.townsandkingdoms.streamcontrol.client;

import java.util.*;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import nl.townsandkingdoms.streamcontrol.*;

/** Data-only visual snapshots; newer renderer APIs can copy these into their render state.
 * No world scan, packets, common-to-Create reference, or frame-rate-dependent rotation. */
public final class ConduitAnimations {
    public record Part(String model,float pivotX,float pivotY,float pivotZ,
                       float angleX,float angleY,float angleZ,float offsetY,float brightness,boolean emissive) {}
    public record Rope(Vec3 start,Vec3 end) {}
    public record Frame(float yaw,List<Part> parts,Rope rope) {}
    private static final Map<BlockEntity,Motion> MOTIONS=new WeakHashMap<>();
    private static final class Motion { double time=-1,spin,core;float blend,vane,needle; }
    private ConduitAnimations() {}
    public static void clear() { MOTIONS.clear(); }
    private static Part part(String model,float x,float y,float z,float ax,float ay,float az,float offset,float brightness,boolean emissive) {
        return new Part("streamcontrol:block/parts/"+model,x,y,z,ax,ay,az,offset,brightness,emissive);
    }
    private static void add(List<Part> parts,String model,float x,float y,float z,float ax,float ay,float az,float offset) {
        parts.add(part(model,x,y,z,ax,ay,az,offset,1,false));
    }
    private static void glow(List<Part> parts,String model,float x,float y,float z,float ax,float ay,float az,float offset,float brightness) {
        if(brightness>0.005f)parts.add(part(model+"_emissive",x,y,z,ax,ay,az,offset,brightness,true));
    }
    public static Frame extract(BlockEntity be,float partial) {
        if(be.getLevel()==null)return new Frame(0,List.of(),null);
        String id=BuiltInRegistries.BLOCK.getKey(be.getBlockState().getBlock()).getPath();
        Motion motion=MOTIONS.computeIfAbsent(be,b->new Motion());
        double now=be.getLevel().getGameTime()+partial;
        double dt=motion.time<0?0:Math.clamp(now-motion.time,0,5);motion.time=now;
        float ease=(float)(1-Math.exp(-dt*.22));
        var block=be.getBlockState();Direction facing=block.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
            ?block.getValue(BlockStateProperties.HORIZONTAL_FACING):Direction.NORTH;
        // The supplied frame stays on its cardinal base. Eight-way water vectors are independent.
        float yaw=switch(facing){case EAST->-90;case SOUTH->-180;case WEST->-270;default->0;};
        boolean active=false;double strength=0;float rpm=0;
        if(be instanceof FlowController c) {active=c.visualActive();strength=c.visualStrength();rpm=c.rpm();}
        motion.blend=Mth.lerp(ease,motion.blend,active?1:0);
        boolean mechanical=id.equals("mechanical_current_generator");
        double rate=mechanical?(Float.isFinite(rpm)?rpm*.3:0):12*Math.clamp(strength,0,2)*motion.blend;
        motion.spin=(motion.spin+rate*dt)%360;motion.core=(motion.core+1.2*motion.blend*dt)%360;
        float spin=(float)motion.spin,core=(float)motion.core;
        float bob=(float)Math.sin(now*.06)*.035f*motion.blend;
        List<Part> parts=new ArrayList<>();Rope rope=null;
        switch(id) {
            case "current_generator" -> {
                add(parts,id+"/impeller",.5f,.5f,.25f,0,0,spin,0);
                add(parts,id+"/conduit_core",.5f,.5f,.625f,0,core,0,bob);
                glow(parts,id+"/conduit_core",.5f,.5f,.625f,0,core,0,bob,motion.blend);
                add(parts,id+"/glass_windows",.5f,0,.5f,0,0,0,0);
            }
            case "stillwater_anchor" -> {
                add(parts,id+"/conduit_outer_cage",.5f,.5f,.5f,0,core,0,bob);
                add(parts,id+"/conduit_eye",.5f,.5f,.5f,0,-core*.5f,0,bob);
                glow(parts,id+"/conduit_eye",.5f,.5f,.5f,0,-core*.5f,0,bob,motion.blend);
            }
            case "flow_sensor" -> {
                if(be instanceof FlowSensorEntity sensor) {
                    var heading=sensor.measuredHeading();
                    float target=heading==null?motion.vane:-heading.id()*45-yaw;
                    motion.vane=Mth.rotLerp(ease,motion.vane,target);
                    float needle=-60+120*Mth.clamp((float)(sensor.measuredSpeed()/Math.max(.05,ControlConfig.MAX_SPEED.get())),0,1);
                    motion.needle=Mth.rotLerp(ease,motion.needle,needle);
                }
                add(parts,id+"/current_vane",.5f,.75f,.5f,0,motion.vane,0,0);
                add(parts,id+"/dial_needle",.5f,.3125f,.078125f,0,0,motion.needle,0);
                glow(parts,id+"/dial_needle",.5f,.3125f,.078125f,0,0,motion.needle,0,.75f);
                add(parts,id+"/dial_glass",.5f,0,.5f,0,0,0,0);
            }
            case "mechanical_current_generator" -> {
                add(parts,id+"/shaft",.5f,.125f,.5f,0,spin,0,0);
                add(parts,id+"/propeller",.5f,.5625f,.1875f,0,0,spin,0);
            }
            case "mooring_post" -> {
                add(parts,id+"/rope_anchor",.5f,.5625f,.21875f,0,0,0,0);
                add(parts,id+"/status_indicator",.5f,1.0625f,.375f,0,0,0,0);
                if(be instanceof MooringEntity post) {
                    glow(parts,id+"/status_indicator",.5f,1.0625f,.375f,0,0,0,0,post.secured()?1:0);
                    var boat=be.getLevel().getEntity(post.boatEntityId());
                    if(post.secured()&&boat!=null)rope=new Rope(post.ropeAnchor().subtract(Vec3.atLowerCornerOf(be.getBlockPos())),
                            boat.getPosition(partial).add(0,.35,0).subtract(Vec3.atLowerCornerOf(be.getBlockPos())));
                }
            }
            default -> {}
        }
        return new Frame(yaw,List.copyOf(parts),rope);
    }
}
