package nl.townsandkingdoms.streamcontrol;
import com.google.gson.*;
import java.nio.file.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.fabricmc.loader.api.FabricLoader;
public final class ControlConfig {
 public static final class Value<T> { private volatile T value; public Value(T v){value=v;} public T get(){return value;} public void set(T v){value=v;} }
 public static final Value<Boolean> ENABLED=new Value<>(true),WAND_ENABLED=new Value<>(true),OP_ONLY=new Value<>(false),PARTICLES=new Value<>(true);
 public static final Value<Integer> MAX_RADIUS=new Value<>(16),DEFAULT_RADIUS=new Value<>(4),VERTICAL=new Value<>(4),MAX_REGIONS=new Value<>(2048),MAX_PLAYER_REGIONS=new Value<>(256);
 public static final Value<Double> SPEED=new Value<>(1.0),MAX_SPEED=new Value<>(2.0);
 public static int radius(int v){return Math.max(1,Math.min(MAX_RADIUS.get(),v));}
 public static double strength(double v){return Double.isFinite(v)?Math.max(0.05,Math.min(MAX_SPEED.get(),v)):Math.min(1,MAX_SPEED.get());}
 private static boolean flag(JsonObject j,String k,boolean d){return j.has(k)?j.get(k).getAsBoolean():d;}
 private static int integer(JsonObject j,String k,int d,int lo,int hi){return Math.max(lo,Math.min(hi,j.has(k)?j.get(k).getAsInt():d));}
 private static double number(JsonObject j,String k,double d){double v=j.has(k)?j.get(k).getAsDouble():d;return Double.isFinite(v)?Math.max(0.05,Math.min(2,v)):d;}
 public static void load(MinecraftServer server){
  ENABLED.set(true);WAND_ENABLED.set(true);OP_ONLY.set(false);PARTICLES.set(true);MAX_RADIUS.set(16);DEFAULT_RADIUS.set(4);VERTICAL.set(4);MAX_REGIONS.set(2048);MAX_PLAYER_REGIONS.set(256);SPEED.set(1.0);MAX_SPEED.set(2.0);
  Path path=server.getWorldPath(LevelResource.ROOT).resolve("serverconfig/streamcontrol-server.json");
  Path defaults=FabricLoader.getInstance().getGameDir().resolve("defaultconfigs/streamcontrol-server.json");
  try {
   Files.createDirectories(path.getParent());
   if(!Files.exists(path)){
    if(Files.isRegularFile(defaults))Files.copy(defaults,path);
    else Files.writeString(path,"""
{
  "enabled": true,
  "wandEnabled": true,
  "wandRequiresOperator": false,
  "showWandFeedbackParticles": true,
  "defaultRadius": 4,
  "maxRadius": 16,
  "verticalReach": 4,
  "defaultStrength": 1.0,
  "maxStrength": 2.0,
  "maxRegionsPerDimension": 2048,
  "maxWandRegionsPerPlayer": 256
}
""");
   }
   JsonObject j=JsonParser.parseString(Files.readString(path)).getAsJsonObject();
   ENABLED.set(flag(j,"enabled",true));WAND_ENABLED.set(flag(j,"wandEnabled",true));OP_ONLY.set(flag(j,"wandRequiresOperator",false));PARTICLES.set(flag(j,"showWandFeedbackParticles",true));
   MAX_RADIUS.set(integer(j,"maxRadius",16,1,32));DEFAULT_RADIUS.set(integer(j,"defaultRadius",4,1,32));VERTICAL.set(integer(j,"verticalReach",4,0,16));MAX_REGIONS.set(integer(j,"maxRegionsPerDimension",2048,1,8192));MAX_PLAYER_REGIONS.set(integer(j,"maxWandRegionsPerPlayer",256,1,2048));SPEED.set(number(j,"defaultStrength",1));MAX_SPEED.set(number(j,"maxStrength",2));
  }catch(Exception e){org.slf4j.LoggerFactory.getLogger("streamcontrol").error("Cannot load Flow Control settings from {}",path,e);}
 }
}
