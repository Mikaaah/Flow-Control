package nl.townsandkingdoms.streamcontrol;

import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/** Opt-in checks invoked only by the dedicated-server integration test. */
public final class VanillaWaterRegressionChecks {
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    public static void run(ServerLevel level, Map<String,Object> results) throws Exception {
        Properties streams = new Properties();
        try (var input = Files.newInputStream(net.fabricmc.loader.api.FabricLoader.getInstance()
                .getConfigDir().resolve("streamsreflowing.properties"))) {
            streams.load(input);
        }
        results.put("forceBakedFluid", streams.getProperty("forceBakedFluid"));

        Map<String,BlockState> fixtures = new LinkedHashMap<>();
        fixtures.put("source", Blocks.WATER.defaultBlockState());
        fixtures.put("flowing", Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 4));
        fixtures.put("waterloggedSlab", Blocks.OAK_SLAB.defaultBlockState()
                .setValue(BlockStateProperties.WATERLOGGED, true));
        fixtures.put("seagrass", Blocks.SEAGRASS.defaultBlockState());
        fixtures.put("kelp", Blocks.KELP.defaultBlockState());
        var data = ControlData.get(level);
        boolean enabled = ControlConfig.ENABLED.get();
        int index = 0;
        try {
            for (var entry : fixtures.entrySet()) {
                String name = entry.getKey();
                BlockPos p = new BlockPos(96 + index++ * 8, 120, 96);
                for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
                    level.setBlock(p.offset(x,-1,z), Blocks.STONE.defaultBlockState(), 2);
                    level.setBlock(p.offset(x,0,z), Math.abs(x)==2 || Math.abs(z)==2
                            ? Blocks.STONE.defaultBlockState() : Blocks.WATER.defaultBlockState(), 2);
                }
                level.setBlock(p, entry.getValue(), 2);
                BlockState before = level.getBlockState(p);
                var fluid = level.getFluidState(p);
                check(fluid.is(FluidTags.WATER), name + ": missing water");
                if (!name.equals("source") && !name.equals("flowing")) {
                    check(before.equals(entry.getValue()), name + ": waterlogged block replaced");
                }
                check(data.at(p.getX(),p.getY(),p.getZ()) == null, name + ": dirty test position");
                ControlConfig.ENABLED.set(false);
                Vec3 baseline = fluid.getFlow(level,p);
                ControlConfig.ENABLED.set(true);
                Vec3 observed = fluid.getFlow(level,p);
                check(observed.distanceToSqr(baseline) < 1e-12, name + ": uncontrolled flow changed");
                Vec3 foreignFlow = new Vec3(0.125,-0.25,0.875);
                check(FlowOverrides.apply(foreignFlow,level,p,fluid) == foreignFlow,
                        name + ": upstream result was not passed through");

                long key = 91000 + index;
                try {
                    check(data.put(level,new FlowRegion(key,p,1,0,1,0,0.75,false,false,
                            new UUID(0,0),data.next())), name + ": region refused");
                    Vec3 controlled = fluid.getFlow(level,p);
                    check(controlled.distanceToSqr(new Vec3(0.75,0,0)) < 1e-12,
                            name + ": controller did not set flow");
                    check(level.getBlockState(p).equals(before) && level.getFluidState(p).equals(fluid),
                            name + ": controller altered block or fluid state");
                } finally { data.remove(level,key); }

                check(fluid.getFlow(level,p).distanceToSqr(baseline) < 1e-12,
                        name + ": removing controller did not restore flow");
                check(level.getBlockState(p).equals(before) && level.getFluidState(p).equals(fluid),
                        name + ": flow lookup altered block or fluid state");
                results.put("vanillaWaterRegression_" + name,true);
            }
            results.put("uncontrolledWaterAndWaterloggedStatePreservation",true);
        } finally { ControlConfig.ENABLED.set(enabled); }
    }
}
