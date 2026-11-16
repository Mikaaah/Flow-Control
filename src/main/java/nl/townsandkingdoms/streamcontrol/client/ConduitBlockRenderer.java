package nl.townsandkingdoms.streamcontrol.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import nl.townsandkingdoms.streamcontrol.DormantMechanicalIntegration;
import nl.townsandkingdoms.streamcontrol.StreamControl;

/** 26.3 extraction/submission renderer. Submit callbacks consume immutable snapshots only. */
public final class ConduitBlockRenderer<T extends BlockEntity>
        implements BlockEntityRenderer<T,ConduitBlockRenderer.State> {
    private record Draw(ConduitAnimations.Part part,List<ConduitMeshes.Face> faces) {}
    public static final class State extends BlockEntityRenderState {
        private float yaw;
        private List<Draw> draws=List.of();
        private ConduitAnimations.Rope rope;
    }
    private static final Map<String,Identifier> TEXTURES=new ConcurrentHashMap<>();
    private static final Identifier ROPE_TEXTURE=Identifier.fromNamespaceAndPath("minecraft","textures/block/brown_wool.png");
    private final boolean tether;
    public ConduitBlockRenderer(BlockEntityRendererProvider.Context context) { this(context,false); }
    private ConduitBlockRenderer(BlockEntityRendererProvider.Context context,boolean tether) { this.tether=tether; }
    public static void register() {
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(StreamControl.CONTROLLER.get(),ConduitBlockRenderer::new);
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(StreamControl.SENSOR_ENTITY.get(),ConduitBlockRenderer::new);
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(StreamControl.MOORING_ENTITY.get(),c->new ConduitBlockRenderer<>(c,true));
        if(DormantMechanicalIntegration.ENTITY!=null)
            net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(DormantMechanicalIntegration.ENTITY.get(),ConduitBlockRenderer::new);
        net.fabricmc.fabric.api.resource.v1.ResourceLoader.get(net.minecraft.server.packs.PackType.CLIENT_RESOURCES)
            .registerReloadListener(Identifier.fromNamespaceAndPath(StreamControl.ID,"conduit_meshes"),
                (ResourceManagerReloadListener)manager->clear());
    }
    private static void clear() {ConduitMeshes.clear();ConduitAnimations.clear();TEXTURES.clear();}
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(T be,State state,float partial,Vec3 camera,
                                             ModelFeatureRenderer.CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(be,state,partial,camera,breaking);
        var frame=ConduitAnimations.extract(be,partial);state.yaw=frame.yaw();state.rope=frame.rope();
        List<Draw> draws=new ArrayList<>(frame.parts().size());
        for(var part:frame.parts())draws.add(new Draw(part,ConduitMeshes.mesh(part.model())));
        state.draws=List.copyOf(draws);
    }
    @Override public void submit(State state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera) {
        pose.pushPose();pose.translate(.5,0,.5);pose.rotateDegrees(Axis.YP,state.yaw);pose.translate(-.5,0,-.5);
        for(var draw:state.draws) {
            var part=draw.part();pose.pushPose();pose.translate(part.pivotX(),part.pivotY()+part.offsetY(),part.pivotZ());
            pose.rotateDegrees(Axis.XP,part.angleX());pose.rotateDegrees(Axis.YP,part.angleY());pose.rotateDegrees(Axis.ZP,part.angleZ());
            pose.translate(-part.pivotX(),-part.pivotY(),-part.pivotZ());
            // Faces were grouped by texture while loading. Group submissions avoid a node per face.
            var faces=draw.faces();
            for(int first=0;first<faces.size();) {
                String texture=faces.get(first).texture();int last=first+1;
                while(last<faces.size() && faces.get(last).texture().equals(texture))last++;
                List<ConduitMeshes.Face> group=faces.subList(first,last);
                Identifier png=TEXTURES.computeIfAbsent(texture,Identifier::parse);
                var type=part.emissive()?RenderTypes.entityTranslucentEmissive(png):group.getFirst().transparent()?RenderTypes.entityTranslucent(png):RenderTypes.entityCutout(png);
                int light=part.emissive()?LightCoordsUtil.FULL_BRIGHT:state.lightCoords;
                float brightness=part.brightness();
                collector.submitCustomGeometry(pose,type,(matrix,out)->{
                    for(var face:group) {
                        float[] v=face.vertices();
                        for(int i=0;i<4;i++) {
                            int n=i*5;
                            out.addVertex(matrix.pose(),v[n],v[n+1],v[n+2]).setColor(brightness,brightness,brightness,1)
                                .setUv(v[n+3],v[n+4]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                                .setNormal(matrix,face.nx(),face.ny(),face.nz());
                        }
                    }
                });first=last;
            }
            pose.popPose();
        }
        pose.popPose();
        if(state.rope!=null)submitRope(state.rope,pose,collector,state.lightCoords);
    }
    private static void submitRope(ConduitAnimations.Rope rope,PoseStack pose,SubmitNodeCollector collector,int light) {
        Vec3 a=rope.start(),b=rope.end();double horizontal=Math.hypot(b.x-a.x,b.z-a.z);
        if(horizontal>32 || a.distanceToSqr(b)<1e-8)return;
        double scale=Math.max(.3,horizontal*.6),edge=Math.cosh(horizontal/(2*scale));
        float px=(float)(horizontal>1e-6?-(b.z-a.z)/horizontal*.0125:.0125);
        float pz=(float)(horizontal>1e-6?(b.x-a.x)/horizontal*.0125:0);
        collector.submitCustomGeometry(pose,RenderTypes.entityCutout(ROPE_TEXTURE),(matrix,out)->{
            for(int axis=0;axis<2;axis++)for(int i=0;i<24;i++) {
                Vec3 p=catenary(a,b,i/24.0,horizontal,scale,edge),q=catenary(a,b,(i+1)/24.0,horizontal,scale,edge);
                float sx=axis==0?px:0,sy=axis==1?.0125f:0,sz=axis==0?pz:0;
                ropeVertex(out,matrix,p,sx,sy,sz,0,i/24f,light);ropeVertex(out,matrix,p,-sx,-sy,-sz,1,i/24f,light);
                ropeVertex(out,matrix,q,-sx,-sy,-sz,1,(i+1)/24f,light);ropeVertex(out,matrix,q,sx,sy,sz,0,(i+1)/24f,light);
            }
        });
    }
    private static Vec3 catenary(Vec3 a,Vec3 b,double t,double length,double scale,double edge) {
        return a.lerp(b,t).add(0,scale*(Math.cosh((t-.5)*length/scale)-edge),0);
    }
    private static void ropeVertex(VertexConsumer out,PoseStack.Pose pose,Vec3 p,float x,float y,float z,float u,float v,int light) {
        out.addVertex(pose.pose(),(float)p.x+x,(float)p.y+y,(float)p.z+z).setColor(.55f,.42f,.25f,1)
            .setUv(u,v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose,0,1,0);
    }
    @Override public boolean shouldRenderOffScreen() { return tether; }
    @Override public int getViewDistance() { return 64; }
}
