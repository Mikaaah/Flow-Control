package nl.townsandkingdoms.streamcontrol;

import java.util.Locale;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

/** Clockwise compass octants. Direction plus a diagonal bit preserves existing block states. */
public enum FlowHeading {
    NORTH(0,-1), NORTH_EAST(1,-1), EAST(1,0), SOUTH_EAST(1,1),
    SOUTH(0,1), SOUTH_WEST(-1,1), WEST(-1,0), NORTH_WEST(-1,-1);
    private static final FlowHeading[] VALUES = values();
    private final int x, z;
    private final double dx, dz;
    FlowHeading(int x,int z) {
        this.x=x;this.z=z;
        double length=Math.hypot(x,z);dx=x/length;dz=z/length;
    }
    public int id(){return ordinal();}
    public int stepX(){return x;}
    public int stepZ(){return z;}
    public double dx(){return dx;}
    public double dz(){return dz;}
    public boolean diagonal(){return x!=0 && z!=0;}
    public String key(){return name().toLowerCase(Locale.ROOT).replace("_", "");}
    public String abbreviation(){return switch(this){
        case NORTH -> "n";case NORTH_EAST -> "ne";case EAST -> "e";case SOUTH_EAST -> "se";
        case SOUTH -> "s";case SOUTH_WEST -> "sw";case WEST -> "w";case NORTH_WEST -> "nw";
    };}
    public static FlowHeading fromId(int id){return id>=0 && id<VALUES.length?VALUES[id]:null;}
    public static FlowHeading fromDirection(Direction d){return switch(d){
        case NORTH -> NORTH;case EAST -> EAST;case SOUTH -> SOUTH;case WEST -> WEST;
        default -> throw new IllegalArgumentException("Horizontal direction required");
    };}
    /** A diagonal lies clockwise from this cardinal base (NE from N, SE from E, etc.). */
    public Direction cardinal(){return switch(ordinal()/2){
        case 0 -> Direction.NORTH;case 1 -> Direction.EAST;case 2 -> Direction.SOUTH;default -> Direction.WEST;
    };}
    public static FlowHeading fromState(BlockState state){
        int base=fromDirection(state.getValue(ControllerBlock.FACING)).ordinal();
        return VALUES[base+(state.getValue(ControllerBlock.DIAGONAL)?1:0)];
    }
    public BlockState apply(BlockState state){return state.setValue(ControllerBlock.FACING,cardinal()).setValue(ControllerBlock.DIAGONAL,diagonal());}
    public FlowHeading clockwise(){return VALUES[(ordinal()+2)&7];}
    public FlowHeading rotate(Rotation r){return VALUES[(ordinal()+switch(r){
        case NONE -> 0;case CLOCKWISE_90 -> 2;case CLOCKWISE_180 -> 4;case COUNTERCLOCKWISE_90 -> 6;
    })&7];}
    public FlowHeading mirror(Mirror m){
        int tx=m==Mirror.FRONT_BACK?-x:x, tz=m==Mirror.LEFT_RIGHT?-z:z;
        for(var h:VALUES)if(h.x==tx && h.z==tz)return h;
        throw new IllegalStateException("Unreachable compass reflection");
    }
}
