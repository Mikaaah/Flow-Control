package nl.townsandkingdoms.streamcontrol.client;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/** Reloadable immutable meshes compiled from the supplied standard block JSON partials.
 * Native PNG resources are used directly, preserving non-square conduit atlases and cutout alpha.
 * Parsing happens once per partial per resource reload, never once per block or per frame. */
public final class ConduitMeshes {
    public record Face(String texture,float[] vertices,float nx,float ny,float nz,boolean transparent) {}
    private static final Map<String,List<Face>> CACHE=new HashMap<>();
    private ConduitMeshes() {}
    public static void clear() { CACHE.clear(); }
    public static List<Face> mesh(String id) { return CACHE.computeIfAbsent(id,ConduitMeshes::load); }
    private static List<Face> load(String id) {
        var split=id.split(":",2);
        var resource=ResourceLocation.fromNamespaceAndPath(split[0],"models/"+split[1]+".json");
        try(var in=Minecraft.getInstance().getResourceManager().getResource(resource).orElseThrow().open();
                var reader=new InputStreamReader(in,StandardCharsets.UTF_8)) {
            JsonObject root=JsonParser.parseReader(reader).getAsJsonObject();
            Map<String,String> textures=new HashMap<>();
            root.getAsJsonObject("textures").entrySet().forEach(e->textures.put(e.getKey(),e.getValue().getAsString()));
            List<Face> result=new ArrayList<>();
            for(var element:root.getAsJsonArray("elements")) {
                var cube=element.getAsJsonObject();float[] from=xyz(cube.getAsJsonArray("from")),to=xyz(cube.getAsJsonArray("to"));
                for(var entry:cube.getAsJsonObject("faces").entrySet()) {
                    var face=entry.getValue().getAsJsonObject();String texture=face.get("texture").getAsString();
                    for(int i=0;texture.startsWith("#")&&i<16;i++) texture=textures.getOrDefault(texture.substring(1),"minecraft:missingno");
                    var textureId=texture.split(":",2);String png=textureId[0]+":textures/"+textureId[1]+".png";
                    float[][] points=corners(entry.getKey(),from,to);float[] uv=new float[4];
                    for(int i=0;i<4;i++)uv[i]=face.getAsJsonArray("uv").get(i).getAsFloat()/16f;
                    float[][] uvs={{uv[0],uv[1]},{uv[0],uv[3]},{uv[2],uv[3]},{uv[2],uv[1]}};
                    int rotation=face.has("rotation")?Math.floorMod(face.get("rotation").getAsInt()/90,4):0;
                    float[] vertices=new float[20];
                    for(int i=0;i<4;i++) {
                        if(cube.has("rotation"))rotate(points[i],cube.getAsJsonObject("rotation"));
                        int n=i*5;vertices[n]=points[i][0]/16;vertices[n+1]=points[i][1]/16;vertices[n+2]=points[i][2]/16;
                        vertices[n+3]=uvs[(i+rotation)%4][0];vertices[n+4]=uvs[(i+rotation)%4][1];
                    }
                    float ax=points[1][0]-points[0][0],ay=points[1][1]-points[0][1],az=points[1][2]-points[0][2];
                    float bx=points[2][0]-points[0][0],by=points[2][1]-points[0][1],bz=points[2][2]-points[0][2];
                    float nx=ay*bz-az*by,ny=az*bx-ax*bz,nz=ax*by-ay*bx;
                    float length=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);if(length<1e-6)continue;
                    result.add(new Face(png,vertices,nx/length,ny/length,nz/length,texture.contains("tinted_glass")));
                }
            }
            // Keep each texture contiguous to reduce buffer switching; geometry order is stable.
            result.sort(Comparator.comparing(Face::texture));return List.copyOf(result);
        } catch(Exception e) { throw new IllegalStateException("Cannot load Flow Control renderer partial "+resource,e); }
    }
    private static float[] xyz(JsonArray a) {return new float[]{a.get(0).getAsFloat(),a.get(1).getAsFloat(),a.get(2).getAsFloat()};}
    private static void rotate(float[] p,JsonObject r) {
        float[] origin=xyz(r.getAsJsonArray("origin"));float angle=r.get("angle").getAsFloat();
        int axis=switch(r.get("axis").getAsString()){case "x"->0;case "y"->1;default->2;};
        double radians=Math.toRadians(angle),c=Math.cos(radians),s=Math.sin(radians);
        float x=p[0]-origin[0],y=p[1]-origin[1],z=p[2]-origin[2];
        if(r.has("rescale")&&r.get("rescale").getAsBoolean()) {
            float scale=(float)(1/Math.cos(Math.toRadians(Math.abs(angle))));
            if(axis!=0)x*=scale;if(axis!=1)y*=scale;if(axis!=2)z*=scale;
        }
        switch(axis){case 0->{p[0]=x;p[1]=(float)(y*c-z*s);p[2]=(float)(y*s+z*c);}
            case 1->{p[0]=(float)(x*c+z*s);p[1]=y;p[2]=(float)(-x*s+z*c);}
            default->{p[0]=(float)(x*c-y*s);p[1]=(float)(x*s+y*c);p[2]=z;}}
        for(int i=0;i<3;i++)p[i]+=origin[i];
    }
    private static float[][] corners(String face,float[] a,float[] b) {
        float x=a[0],y=a[1],z=a[2],X=b[0],Y=b[1],Z=b[2];
        return switch(face){
            case "north"->new float[][]{{X,Y,z},{X,y,z},{x,y,z},{x,Y,z}};
            case "south"->new float[][]{{x,Y,Z},{x,y,Z},{X,y,Z},{X,Y,Z}};
            case "west"->new float[][]{{x,Y,z},{x,y,z},{x,y,Z},{x,Y,Z}};
            case "east"->new float[][]{{X,Y,Z},{X,y,Z},{X,y,z},{X,Y,z}};
            case "up"->new float[][]{{x,Y,z},{x,Y,Z},{X,Y,Z},{X,Y,z}};
            default->new float[][]{{x,y,Z},{x,y,z},{X,y,z},{X,y,Z}};
        };
    }
}
