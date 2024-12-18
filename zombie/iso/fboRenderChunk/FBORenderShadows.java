package zombie.iso.fboRenderChunk;

import java.util.ArrayList;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import zombie.IndieGL;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.VBORenderer;
import zombie.core.skinnedmodel.model.VertexBufferObject;
import zombie.core.sprite.SpriteRenderState;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.iso.IsoCamera;
import zombie.iso.IsoDepthHelper;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.PlayerCamera;
import zombie.iso.Vector2;
import zombie.popman.ObjectPool;

public final class FBORenderShadows {
   private static FBORenderShadows instance = null;
   private final ObjectPool<Shadow> m_shadowPool = new ObjectPool(Shadow::new);
   private final ArrayList<Shadow> m_shadows = new ArrayList();
   private final Vector3f m_tempVector3f_1 = new Vector3f();
   private final Vector3f m_tempVector3f_2 = new Vector3f();
   private Texture DropShadow;
   private final Drawer[][] m_drawers = new Drawer[4][3];

   public static FBORenderShadows getInstance() {
      if (instance == null) {
         instance = new FBORenderShadows();
      }

      return instance;
   }

   private FBORenderShadows() {
      for(int var1 = 0; var1 < 4; ++var1) {
         for(int var2 = 0; var2 < 3; ++var2) {
            this.m_drawers[var1][var2] = new Drawer();
         }
      }

   }

   public void clear() {
      this.m_shadowPool.releaseAll(this.m_shadows);
      this.m_shadows.clear();
      this.DropShadow = Texture.getSharedTexture("media/textures/NewShadow.png");
   }

   public void addShadow(float var1, float var2, float var3, Vector3f var4, float var5, float var6, float var7, float var8, float var9, float var10, float var11, boolean var12) {
      if (this.DropShadow != null && this.DropShadow.isReady()) {
         if (!var12) {
            var5 = Math.max(var5, 0.65F);
            var6 = Math.max(var6, 0.65F);
            var7 = Math.max(var7, 0.65F);
         }

         Vector3f var13 = this.m_tempVector3f_1.set(var4);
         var13.normalize();
         Vector3f var14 = var13.cross(0.0F, 0.0F, 1.0F, this.m_tempVector3f_2);
         var14.x *= var5;
         var14.y *= var5;
         float var15 = var1 + var4.x * var6;
         float var16 = var2 + var4.y * var6;
         float var17 = var1 - var4.x * var7;
         float var18 = var2 - var4.y * var7;
         float var19 = var15 - var14.x;
         float var20 = var15 + var14.x;
         float var21 = var17 - var14.x;
         float var22 = var17 + var14.x;
         float var23 = var18 - var14.y;
         float var24 = var18 + var14.y;
         float var25 = var16 - var14.y;
         float var26 = var16 + var14.y;
         float var27 = var11 * ((var8 + var9 + var10) / 3.0F);
         var27 *= 0.66F;
         this.addShadow(var1, var2, var3, var19, var25, var20, var26, var22, var24, var21, var23, var8, var9, var10, var27, this.DropShadow, false);
         if (DebugOptions.instance.IsoSprite.DropShadowEdges.getValue()) {
            LineDrawer.addLine(var19, var25, var3, var20, var26, var3, 1, 1, 1, (String)null);
            LineDrawer.addLine(var20, var26, var3, var22, var24, var3, 1, 1, 1, (String)null);
            LineDrawer.addLine(var22, var24, var3, var21, var23, var3, 1, 1, 1, (String)null);
            LineDrawer.addLine(var21, var23, var3, var19, var25, var3, 1, 1, 1, (String)null);
         }

      }
   }

   public void addShadow(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10, float var11, float var12, float var13, float var14, float var15, Texture var16, boolean var17) {
      if (!(var15 <= 0.0F)) {
         Shadow var18 = (Shadow)this.m_shadowPool.alloc();
         var18.ox = var1;
         var18.oy = var2;
         var18.oz = var3;
         var18.x0 = var4;
         var18.y0 = var5;
         var18.x1 = var6;
         var18.y1 = var7;
         var18.x2 = var8;
         var18.y2 = var9;
         var18.x3 = var10;
         var18.y3 = var11;
         this.calculateSlopeAngles(var18);
         float var19 = IsoCamera.frameState.CamCharacterX;
         float var20 = IsoCamera.frameState.CamCharacterY;
         float var21 = var1 + var2 < var19 + var20 ? -1.4E-4F : -1.0E-4F;
         var18.depth0 = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(IsoCamera.frameState.CamCharacterX), PZMath.fastfloor(IsoCamera.frameState.CamCharacterY), var4, var5, this.calculateApparentZ(var18, var4, var5, var3)).depthStart + var21;
         var18.depth1 = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(IsoCamera.frameState.CamCharacterX), PZMath.fastfloor(IsoCamera.frameState.CamCharacterY), var6, var7, this.calculateApparentZ(var18, var6, var7, var3)).depthStart + var21;
         var18.depth2 = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(IsoCamera.frameState.CamCharacterX), PZMath.fastfloor(IsoCamera.frameState.CamCharacterY), var8, var9, this.calculateApparentZ(var18, var8, var9, var3)).depthStart + var21;
         var18.depth3 = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(IsoCamera.frameState.CamCharacterX), PZMath.fastfloor(IsoCamera.frameState.CamCharacterY), var10, var11, this.calculateApparentZ(var18, var10, var11, var3)).depthStart + var21;
         var18.r = var12;
         var18.g = var13;
         var18.b = var14;
         var18.a = var15;
         var18.texture = var16;
         var18.bVehicle = var17;
         this.m_shadows.add(var18);
      }
   }

   void calculateSlopeAngles(Shadow var1) {
      var1.slopeAngleX = var1.slopeAngleY = 0.0F;
      IsoGridSquare var2 = IsoWorld.instance.CurrentCell.getGridSquare((double)var1.ox, (double)var1.oy, (double)var1.oz);
      if (var2 != null) {
         IsoDirections var3 = var2.getSlopedSurfaceDirection();
         if (var3 != null) {
            float var4 = var2.getSlopedSurfaceHeightMin();
            float var5 = var2.getSlopedSurfaceHeightMax();
            switch (var3) {
               case N:
                  var1.slopeAngleX = Vector2.getDirection(1.0F, (var5 - var4) * 2.44949F);
                  break;
               case S:
                  var1.slopeAngleX = Vector2.getDirection(1.0F, (var4 - var5) * 2.44949F);
                  break;
               case W:
                  var1.slopeAngleY = Vector2.getDirection(1.0F, (var4 - var5) * 2.44949F);
                  break;
               case E:
                  var1.slopeAngleY = Vector2.getDirection(1.0F, (var5 - var4) * 2.44949F);
            }

         }
      }
   }

   float calculateApparentZ(Shadow var1, float var2, float var3, float var4) {
      if (var1.slopeAngleX == 0.0F && var1.slopeAngleY == 0.0F) {
         return var4;
      } else {
         IsoGridSquare var5 = IsoWorld.instance.CurrentCell.getGridSquare((double)var1.ox, (double)var1.oy, (double)var1.oz);
         float var6 = var5.getSlopedSurfaceHeightMin();
         float var7 = var5.getSlopedSurfaceHeightMax();
         float var8 = var7 - var6;
         float var9 = (var2 - (float)(var5.x - 5)) % 10.0F;
         float var10 = (var3 - (float)(var5.y - 5)) % 10.0F;
         float var10000;
         switch (var5.getSlopedSurfaceDirection()) {
            case N:
               var10000 = PZMath.lerp(var6 - 4.0F * var8, var7 + 5.0F * var8, 1.0F - var10 / 10.0F);
               break;
            case S:
               var10000 = PZMath.lerp(var6 - 5.0F * var8, var7 + 4.0F * var8, var10 / 10.0F);
               break;
            case W:
               var10000 = PZMath.lerp(var6 - 4.0F * var8, var7 + 5.0F * var8, 1.0F - var9 / 10.0F);
               break;
            case E:
               var10000 = PZMath.lerp(var6 - 5.0F * var8, var7 + 4.0F * var8, var9 / 10.0F);
               break;
            default:
               var10000 = -1.0F;
         }

         float var11 = var10000;
         return var11 == -1.0F ? var4 : (float)PZMath.fastfloor(var4) + var11;
      }
   }

   public void renderMain() {
      int var1 = IsoCamera.frameState.playerIndex;
      int var2 = SpriteRenderer.instance.getMainStateIndex();
      Drawer var3 = this.m_drawers[var1][var2];
      var3.m_shadows.clear();
      var3.m_shadows.addAll(this.m_shadows);
      SpriteRenderer.instance.drawGeneric(var3);
      this.m_shadows.clear();
   }

   private void render(ArrayList<Shadow> var1) {
      SpriteRenderState var2 = SpriteRenderer.instance.getRenderingState();
      PlayerCamera var3 = var2.playerCamera[var2.playerIndex];
      float var4 = var3.RightClickX;
      float var5 = var3.RightClickY;
      float var6 = var3.getTOffX();
      float var7 = var3.getTOffY();
      float var8 = var3.DeferedX;
      float var9 = var3.DeferedY;
      float var10 = (Float)Core.getInstance().FloatParamMap.get(0);
      float var11 = (Float)Core.getInstance().FloatParamMap.get(1);
      float var12 = (Float)Core.getInstance().FloatParamMap.get(2);
      float var13 = var10 - var3.XToIso(-var6 - var4, -var7 - var5, 0.0F);
      float var14 = var11 - var3.YToIso(-var6 - var4, -var7 - var5, 0.0F);
      var13 += var8;
      var14 += var9;
      double var15 = (double)((float)var3.OffscreenWidth / 1920.0F);
      double var17 = (double)((float)var3.OffscreenHeight / 1920.0F);
      Matrix4f var19 = Core.getInstance().projectionMatrixStack.alloc();
      var19.setOrtho(-((float)var15) / 2.0F, (float)var15 / 2.0F, -((float)var17) / 2.0F, (float)var17 / 2.0F, -10.0F, 10.0F);
      Core.getInstance().projectionMatrixStack.push(var19);
      VBORenderer var20 = VBORenderer.getInstance();
      GL11.glEnable(2929);
      GL11.glDepthFunc(515);
      GL11.glDepthMask(false);
      IndieGL.glDefaultBlendFuncA();

      for(int var21 = 0; var21 < var1.size(); ++var21) {
         Shadow var22 = (Shadow)var1.get(var21);
         float var23 = 0.0F;
         float var24 = 0.0F;
         float var25 = 1.0F;
         float var26 = 0.0F;
         float var27 = 1.0F;
         float var28 = 1.0F;
         float var29 = 0.0F;
         float var30 = 1.0F;
         Matrix4f var31 = Core.getInstance().modelViewMatrixStack.alloc();
         var31.scaling(Core.scale);
         var31.scale((float)Core.TileScale / 2.0F);
         var31.rotate(0.5235988F, 1.0F, 0.0F, 0.0F);
         var31.rotate(2.3561945F, 0.0F, 1.0F, 0.0F);
         double var32 = (double)(var22.ox - var13);
         double var34 = (double)(var22.oy - var14);
         double var36 = (double)((var22.oz - var12) * 2.44949F);
         var31.translate(-((float)var32), (float)var36, -((float)var34));
         var31.scale(-1.0F, 1.0F, -1.0F);
         var31.translate(0.0F, -0.71999997F, 0.0F);
         if (var22.slopeAngleX != 0.0F) {
            var31.rotate(var22.slopeAngleX, 1.0F, 0.0F, 0.0F);
         }

         if (var22.slopeAngleY != 0.0F) {
            var31.rotate(var22.slopeAngleY, 0.0F, 0.0F, 1.0F);
         }

         var20.cmdPushAndLoadMatrix(5888, var31);
         Core.getInstance().modelViewMatrixStack.push(var31);
         float var38 = VertexBufferObject.getDepthValueAt(0.0F, 0.0F, 0.0F);
         Core.getInstance().modelViewMatrixStack.pop();
         var20.startRun(VBORenderer.getInstance().FORMAT_PositionColorUVDepth);
         var20.setMode(7);
         var20.setTextureID(var22.texture.getTextureId());
         var20.setDepthTest(true);
         var20.addQuadDepth(var22.x0 - var22.ox, 0.0F, var22.y0 - var22.oy, var23, var24, var22.depth0, var22.x1 - var22.ox, 0.0F, var22.y1 - var22.oy, var25, var26, var22.depth1, var22.x2 - var22.ox, 0.0F, var22.y2 - var22.oy, var27, var28, var22.depth2, var22.x3 - var22.ox, 0.0F, var22.y3 - var22.oy, var29, var30, var22.depth3, var22.r, var22.g, var22.b, var22.a);
         var20.endRun();
         var20.cmdPopMatrix(5888);
      }

      var20.flush();
      Core.getInstance().projectionMatrixStack.pop();
      GLStateRenderThread.restore();
   }

   static final class Drawer extends TextureDraw.GenericDrawer {
      private final ArrayList<Shadow> m_shadows = new ArrayList();

      Drawer() {
      }

      public void render() {
         FBORenderShadows.getInstance().render(this.m_shadows);
      }

      public void postRender() {
         FBORenderShadows.instance.m_shadowPool.releaseAll(this.m_shadows);
         this.m_shadows.clear();
      }
   }

   static final class Shadow {
      float ox;
      float oy;
      float oz;
      float x0;
      float y0;
      float x1;
      float y1;
      float x2;
      float y2;
      float x3;
      float y3;
      float slopeAngleX;
      float slopeAngleY;
      float depth0;
      float depth1;
      float depth2;
      float depth3;
      float r;
      float g;
      float b;
      float a;
      Texture texture;
      boolean bVehicle;

      Shadow() {
      }
   }
}
