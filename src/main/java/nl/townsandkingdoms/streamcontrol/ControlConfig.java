package nl.townsandkingdoms.streamcontrol;
import com.google.gson.*;
import java.nio.file.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.fabricmc.loader.api.FabricLoader;
public final class ControlConfig {
 public static final class Value<T> {private volatile T value;public Value(T v){value=v;}public T get(){return value;}public void set(T v){value=v;}}
 public static final Value<Boolean> ENABLED=new Value<>(true);
 public static final Value<Boolean> WAND_ENABLED=new Value<>(true);
 public static final Value<Boolean> OP_ONLY=new Value<>(false);
 public static final Value<Boolean> PARTICLES=new Value<>(true);
 public static final Value<Integer> MAX_RADIUS=new Value<>(16);
 public static final Value<Integer> DEFAULT_RADIUS=new Value<>(4);
 public static final Value<Integer> VERTICAL=new Value<>(4);
 public static final Value<Double> SPEED=new Value<>(1.0);
 public static final Value<Double> MAX_SPEED=new Value<>(2.0);
 public static final Value<Integer> MAX_REGIONS=new Value<>(2048);
 public static final Value<Integer> MAX_PLAYER_REGIONS=new Value<>(256);
 public static final Value<Integer> MAX_VERTICAL=new Value<>(16);
 public static final Value<Integer> LINK_RANGE=new Value<>(64);
 public static final Value<Integer> SENSOR_MAX_RADIUS=new Value<>(16);
 public static final Value<Integer> SENSOR_MAX_VERTICAL=new Value<>(16);
 public static final Value<Integer> SENSOR_INTERVAL=new Value<>(20);
 public static final Value<Integer> MOORING_MAX_RADIUS=new Value<>(8);
 public static final Value<Integer> MOORING_MAX_VERTICAL=new Value<>(8);
 public static final Value<Double> RPM_FULL_STRENGTH=new Value<>(128.0);
 public static final Value<Double> KINETIC_STRESS=new Value<>(4.0);
 public static int radius(int n){return Math.max(1,Math.min(MAX_RADIUS.get(),n));}
 public static double strength(double n){return Double.isFinite(n)?Math.max(.05,Math.min(MAX_SPEED.get(),n)):Math.min(1,MAX_SPEED.get());}
 public static void load(MinecraftServer server){
  Path path=server.getWorldPath(LevelResource.ROOT).resolve("serverconfig/streamcontrol-server.json");
  try {
   Files.createDirectories(path.getParent());
   Path defaults=FabricLoader.getInstance().getGameDir().resolve("defaultconfigs/streamcontrol-server.json");
   if(!Files.exists(path) && Files.isRegularFile(defaults))Files.copy(defaults,path);
   JsonObject n=Files.exists(path)?JsonParser.parseString(Files.readString(path)).getAsJsonObject():new JsonObject();
   if(!n.has("enabled")) n.addProperty("enabled",true);
   ENABLED.set(n.get("enabled").getAsBoolean());
   if(!n.has("wandEnabled")) n.addProperty("wandEnabled",true);
   WAND_ENABLED.set(n.get("wandEnabled").getAsBoolean());
   if(!n.has("wandRequiresOperator")) n.addProperty("wandRequiresOperator",false);
   OP_ONLY.set(n.get("wandRequiresOperator").getAsBoolean());
   if(!n.has("showWandFeedbackParticles")) n.addProperty("showWandFeedbackParticles",true);
   PARTICLES.set(n.get("showWandFeedbackParticles").getAsBoolean());
   if(!n.has("maxRadius")) n.addProperty("maxRadius",16);
   MAX_RADIUS.set(Math.max(1,Math.min(32,n.get("maxRadius").getAsInt())));
   if(!n.has("defaultRadius")) n.addProperty("defaultRadius",4);
   DEFAULT_RADIUS.set(Math.max(1,Math.min(32,n.get("defaultRadius").getAsInt())));
   if(!n.has("verticalReach")) n.addProperty("verticalReach",4);
   VERTICAL.set(Math.max(0,Math.min(16,n.get("verticalReach").getAsInt())));
   if(!n.has("defaultStrength")) n.addProperty("defaultStrength",1.0);
   SPEED.set(Double.isFinite(n.get("defaultStrength").getAsDouble())?Math.max(0.05,Math.min(2.0,n.get("defaultStrength").getAsDouble())):1.0);
   if(!n.has("maxStrength")) n.addProperty("maxStrength",2.0);
   MAX_SPEED.set(Double.isFinite(n.get("maxStrength").getAsDouble())?Math.max(0.05,Math.min(2.0,n.get("maxStrength").getAsDouble())):2.0);
   if(!n.has("maxRegionsPerDimension")) n.addProperty("maxRegionsPerDimension",2048);
   MAX_REGIONS.set(Math.max(1,Math.min(8192,n.get("maxRegionsPerDimension").getAsInt())));
   if(!n.has("maxWandRegionsPerPlayer")) n.addProperty("maxWandRegionsPerPlayer",256);
   MAX_PLAYER_REGIONS.set(Math.max(1,Math.min(2048,n.get("maxWandRegionsPerPlayer").getAsInt())));
   if(!n.has("maxControllerVerticalReach")) n.addProperty("maxControllerVerticalReach",16);
   MAX_VERTICAL.set(Math.max(0,Math.min(16,n.get("maxControllerVerticalReach").getAsInt())));
   if(!n.has("maxControllerLinkDistance")) n.addProperty("maxControllerLinkDistance",64);
   LINK_RANGE.set(Math.max(1,Math.min(256,n.get("maxControllerLinkDistance").getAsInt())));
   if(!n.has("maxSensorRadius")) n.addProperty("maxSensorRadius",16);
   SENSOR_MAX_RADIUS.set(Math.max(1,Math.min(16,n.get("maxSensorRadius").getAsInt())));
   if(!n.has("maxSensorVerticalReach")) n.addProperty("maxSensorVerticalReach",16);
   SENSOR_MAX_VERTICAL.set(Math.max(0,Math.min(16,n.get("maxSensorVerticalReach").getAsInt())));
   if(!n.has("sensorSampleIntervalTicks")) n.addProperty("sensorSampleIntervalTicks",20);
   SENSOR_INTERVAL.set(Math.max(5,Math.min(200,n.get("sensorSampleIntervalTicks").getAsInt())));
   if(!n.has("maxMooringRadius")) n.addProperty("maxMooringRadius",8);
   MOORING_MAX_RADIUS.set(Math.max(1,Math.min(16,n.get("maxMooringRadius").getAsInt())));
   if(!n.has("maxMooringVerticalReach")) n.addProperty("maxMooringVerticalReach",8);
   MOORING_MAX_VERTICAL.set(Math.max(0,Math.min(16,n.get("maxMooringVerticalReach").getAsInt())));
   if(!n.has("kineticRpmForFullStrength")) n.addProperty("kineticRpmForFullStrength",128.0);
   RPM_FULL_STRENGTH.set(Double.isFinite(n.get("kineticRpmForFullStrength").getAsDouble())?Math.max(1.0,Math.min(1024.0,n.get("kineticRpmForFullStrength").getAsDouble())):128.0);
   if(!n.has("kineticStressImpact")) n.addProperty("kineticStressImpact",4.0);
   KINETIC_STRESS.set(Double.isFinite(n.get("kineticStressImpact").getAsDouble())?Math.max(0.0,Math.min(256.0,n.get("kineticStressImpact").getAsDouble())):4.0);
   Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(n));
  }catch(Exception e){throw new IllegalStateException("Cannot load Flow Control settings from "+path,e);}
 }
}
