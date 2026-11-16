package nl.townsandkingdoms.streamcontrol.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import java.util.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import nl.townsandkingdoms.streamcontrol.StreamControl;

/** Supplied JSON partials are rendered once; fixed bodies remain ordinary chunk geometry. */
public final class ConduitBlockRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
    private static final Map<String,ResourceLocation> TEXTURES=new HashMap<>();
    public ConduitBlockRenderer(BlockEntityRendererProvider.Context context) {}
    public static void clearCaches() { ConduitMeshes.clear();ConduitAnimations.clear();TEXTURES.clear(); }
    @Override public void render(T be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        renderParts(be,partial,pose,buffers,light,overlay);
    }
    public static void renderParts(BlockEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        renderFrame(ConduitAnimations.extract(be,partial),pose,buffers,light,overlay);
    }
    public static void renderFrame(ConduitAnimations.Frame frame,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        pose.pushPose();pose.translate(.5,0,.5);pose.mulPose(Axis.YP.rotationDegrees(frame.yaw()));pose.translate(-.5,0,-.5);
        for(var part:frame.parts()) {
            pose.pushPose();pose.translate(part.pivotX(),part.pivotY()+part.offsetY(),part.pivotZ());
            pose.mulPose(Axis.XP.rotationDegrees(part.angleX()));pose.mulPose(Axis.YP.rotationDegrees(part.angleY()));pose.mulPose(Axis.ZP.rotationDegrees(part.angleZ()));
            pose.translate(-part.pivotX(),-part.pivotY(),-part.pivotZ());
            for(var face:ConduitMeshes.mesh(part.model())) {
                var texture=TEXTURES.computeIfAbsent(face.texture(),ResourceLocation::parse);
                var type=face.transparent()?RenderType.entityTranslucent(texture):RenderType.entityCutoutNoCull(texture);
                var out=buffers.getBuffer(type);float[] v=face.vertices();
                for(int i=0;i<4;i++) {
                    int n=i*5;float brightness=part.brightness();
                    out.addVertex(pose.last().pose(),v[n],v[n+1],v[n+2]).setColor(brightness,brightness,brightness,1)
                        .setUv(v[n+3],v[n+4]).setOverlay(overlay).setLight(part.emissive()?LightTexture.FULL_BRIGHT:light)
                        .setNormal(pose.last(),face.nx(),face.ny(),face.nz());
                }
            }
            pose.popPose();
        }
        pose.popPose();
        if(frame.rope()!=null)rope(frame.rope(),pose,buffers,light);
    }
    private static void rope(ConduitAnimations.Rope rope,PoseStack pose,MultiBufferSource buffers,int light) {
        Vec3 a=rope.start(),b=rope.end();double dx=b.x-a.x,dz=b.z-a.z;
        double horizontal=Math.hypot(dx,dz);if(horizontal>32 || a.distanceToSqr(b)<1e-8)return;
        double scale=Math.max(.3,horizontal*.6),edge=Math.cosh(horizontal/(2*scale));
        var out=buffers.getBuffer(RenderType.leash());
        // Two crossed ribbons give the rope depth from every viewing angle; 24 fixed segments.
        for(int axis=0;axis<2;axis++)for(int i=0;i<24;i++) {
            double t0=i/24.0,t1=(i+1)/24.0;
            Vec3 p=catenary(a,b,t0,horizontal,scale,edge),q=catenary(a,b,t1,horizontal,scale,edge);
            float width=.0125f;float sx=axis==0?width:0,sy=axis==1?width:0,sz=axis==0?width:0;
            ropeVertex(out,pose,p,sx,sy,sz,light);ropeVertex(out,pose,p,-sx,-sy,-sz,light);
            ropeVertex(out,pose,q,-sx,-sy,-sz,light);ropeVertex(out,pose,q,sx,sy,sz,light);
        }
    }
    private static Vec3 catenary(Vec3 a,Vec3 b,double t,double length,double scale,double edge) {
        return a.lerp(b,t).add(0,scale*(Math.cosh((t-.5)*length/scale)-edge),0);
    }
    private static void ropeVertex(VertexConsumer out,PoseStack pose,Vec3 v,float x,float y,float z,int light) {
        out.addVertex(pose.last().pose(),(float)v.x+x,(float)v.y+y,(float)v.z+z).setColor(.30f,.22f,.12f,1).setLight(light);
    }
    @Override public boolean shouldRenderOffScreen(T be){return be instanceof nl.townsandkingdoms.streamcontrol.MooringEntity post && post.secured();}
    @Override public int getViewDistance(){return 64;}
}
