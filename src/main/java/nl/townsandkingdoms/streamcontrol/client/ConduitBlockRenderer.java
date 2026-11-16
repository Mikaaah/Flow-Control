package nl.townsandkingdoms.streamcontrol.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.*;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/** Modern extraction/submission renderer. No BlockEntity or world reads survive extraction. */
public final class ConduitBlockRenderer<T extends BlockEntity>
        implements BlockEntityRenderer<T,ConduitBlockRenderer.State> {
    private record Group(RenderType layer,RenderType emissive,List<ConduitMeshes.Face> faces) {}
    private record Draw(ConduitAnimations.Part part,List<Group> groups) {}
    public static final class State extends BlockEntityRenderState {
        private float yaw;
        private List<Draw> draws=List.of();
        private ConduitAnimations.Rope rope;
    }
    private static final Map<String,List<Group>> GROUPS=new HashMap<>();
    private static final Identifier ROPE=Identifier.fromNamespaceAndPath("minecraft","textures/block/brown_wool.png");
    public ConduitBlockRenderer(BlockEntityRendererProvider.Context context) {}
    public static void clear() { GROUPS.clear();ConduitMeshes.clear();ConduitAnimations.clear(); }
    public static net.minecraft.server.packs.resources.PreparableReloadListener reloadListener() {
        return new net.minecraft.server.packs.resources.SimplePreparableReloadListener<Void>() {
            @Override protected Void prepare(net.minecraft.server.packs.resources.ResourceManager resources,net.minecraft.util.profiling.ProfilerFiller profiler) { return null; }
            @Override protected void apply(Void ignored,net.minecraft.server.packs.resources.ResourceManager resources,net.minecraft.util.profiling.ProfilerFiller profiler) { clear(); }
        };
    }
    @Override public State createRenderState() {return new State();}
    @Override public void extractRenderState(T be,State state,float partial,Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(be,state,partial,camera,breaking);
        var frame=ConduitAnimations.extract(be,partial);
        state.yaw=frame.yaw();state.rope=frame.rope();
        List<Draw> draws=new ArrayList<>(frame.parts().size());
        for(var part:frame.parts())draws.add(new Draw(part,GROUPS.computeIfAbsent(part.model(),ConduitBlockRenderer::groups)));
        state.draws=List.copyOf(draws);
    }
    private static List<Group> groups(String model) {
        Map<String,List<ConduitMeshes.Face>> byTexture=new LinkedHashMap<>();
        for(var face:ConduitMeshes.mesh(model))byTexture.computeIfAbsent(face.texture(),k->new ArrayList<>()).add(face);
        List<Group> groups=new ArrayList<>();
        byTexture.forEach((png,faces)->{
            var texture=Identifier.parse(png);
            var type=faces.getFirst().transparent()?RenderTypes.entityTranslucent(texture):RenderTypes.entityCutout(texture);
            groups.add(new Group(type,RenderTypes.entityTranslucentEmissive(texture),List.copyOf(faces)));
        });
        return List.copyOf(groups);
    }
    @Override public void submit(State state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera) {
        int light=state.lightCoords;
        pose.pushPose();pose.translate(.5,0,.5);pose.mulPose(Axis.YP.rotationDegrees(state.yaw));pose.translate(-.5,0,-.5);
        for(var draw:state.draws) {
            var part=draw.part();pose.pushPose();pose.translate(part.pivotX(),part.pivotY()+part.offsetY(),part.pivotZ());
            pose.mulPose(Axis.XP.rotationDegrees(part.angleX()));pose.mulPose(Axis.YP.rotationDegrees(part.angleY()));pose.mulPose(Axis.ZP.rotationDegrees(part.angleZ()));
            pose.translate(-part.pivotX(),-part.pivotY(),-part.pivotZ());
            int packed=part.emissive()?LightCoordsUtil.FULL_BRIGHT:light;float color=part.brightness();
            for(var group:draw.groups())collector.submitCustomGeometry(pose,part.emissive()?group.emissive():group.layer(),(transform,out)->emit(group.faces(),transform,out,color,packed));
            pose.popPose();
        }
        pose.popPose();
        var rope=state.rope;
        if(rope!=null && rope.start().distanceToSqr(rope.end())>1e-8 && rope.start().distanceToSqr(rope.end())<1024)
            collector.submitCustomGeometry(pose,RenderTypes.entityCutout(ROPE),(transform,out)->rope(rope,transform,out,light));
    }
    private static void emit(List<ConduitMeshes.Face> faces,PoseStack.Pose pose,VertexConsumer out,float color,int light) {
        for(var face:faces) {
            float[] v=face.vertices();
            for(int i=0;i<4;i++) {
                int n=i*5;
                out.addVertex(pose,v[n],v[n+1],v[n+2]).setColor(color,color,color,1)
                    .setUv(v[n+3],v[n+4]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                    .setNormal(pose,face.nx(),face.ny(),face.nz());
            }
        }
    }
    private static void rope(ConduitAnimations.Rope rope,PoseStack.Pose pose,VertexConsumer out,int light) {
        Vec3 a=rope.start(),b=rope.end();double dx=b.x-a.x,dz=b.z-a.z,length=Math.hypot(dx,dz);
        double scale=Math.max(.3,length*.6),edge=Math.cosh(length/(2*scale));
        float px=(float)(length>1e-6?-dz/length*.0125:.0125),pz=(float)(length>1e-6?dx/length*.0125:0);
        for(int ribbon=0;ribbon<2;ribbon++)for(int i=0;i<24;i++) {
            double t=i/24.0,u=(i+1)/24.0;
            Vec3 p=a.lerp(b,t).add(0,scale*(Math.cosh((t-.5)*length/scale)-edge),0);
            Vec3 q=a.lerp(b,u).add(0,scale*(Math.cosh((u-.5)*length/scale)-edge),0);
            float x=ribbon==0?px:0,y=ribbon==1?.0125f:0,z=ribbon==0?pz:0;
            ropeVertex(out,pose,p,x,y,z,0,(float)t,light);ropeVertex(out,pose,p,-x,-y,-z,1,(float)t,light);
            ropeVertex(out,pose,q,-x,-y,-z,1,(float)u,light);ropeVertex(out,pose,q,x,y,z,0,(float)u,light);
        }
    }
    private static void ropeVertex(VertexConsumer out,PoseStack.Pose pose,Vec3 v,float x,float y,float z,float u,float w,int light) {
        out.addVertex(pose,(float)v.x+x,(float)v.y+y,(float)v.z+z).setColor(.55f,.42f,.25f,1).setUv(u,w)
            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose,0,1,0);
    }
    @Override public boolean shouldRenderOffScreen(){return true;}
    @Override public int getViewDistance(){return 64;}
}
