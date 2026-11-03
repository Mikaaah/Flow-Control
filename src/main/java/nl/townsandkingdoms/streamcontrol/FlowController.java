package nl.townsandkingdoms.streamcontrol;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Shared controller contract; the base mod never loads Create classes. */
public interface FlowController {
    BlockEntity blockEntity();
    ControllerSettings controls();
    default boolean isStill() { return blockEntity().getBlockState().getBlock() instanceof ControllerBlock b && b.still; }
    /** NaN means a normal controller. A kinetic controller returns signed RPM. */
    default float rpm() { return Float.NaN; }
    default int radius() { return controls().a().radius(); }
    default double strength() { return controls().a().strength(); }
    default boolean connectedOnly() { return controls().a().connectedOnly(); }
    default FlowHeading heading() { return FlowHeading.fromState(blockEntity().getBlockState()); }
    default void configure(FlowHeading heading, int radius, double strength, boolean connected) {
        controls().setA(new FlowProfile(radius,controls().a().vertical(),strength,heading,connected,controls().a().action()));
    }
    default void configure(Direction d, int r, double speed, boolean connected) { configure(FlowHeading.fromDirection(d),r,speed,connected); }
    default void configure(Direction d, int r, double speed) { configure(d,r,speed,connectedOnly()); }
    default void setRadius(int r) { configure(heading(),r,strength(),connectedOnly()); }
    default void refresh() { controls().refresh(); }
    default void waterChanged() { controls().invalidate(); refresh(); }
}
