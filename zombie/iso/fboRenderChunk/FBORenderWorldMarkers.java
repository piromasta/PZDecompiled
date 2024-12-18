package zombie.iso.fboRenderChunk;

import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.VBORenderer;
import zombie.core.sprite.SpriteRenderState;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.debug.LineDrawer;
import zombie.iso.IsoCamera;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoDepthHelper;
import zombie.iso.IsoWorld;
import zombie.iso.PlayerCamera;
import zombie.iso.WorldMarkers;
import zombie.popman.ObjectPool;
import zombie.ui.UIManager;
import zombie.util.StringUtils;

public class FBORenderWorldMarkers {
   private static FBORenderWorldMarkers instance = null;
   private final ArrayList<Marker> m_highlights = new ArrayList();
   private final ObjectPool<Marker> m_highlightPool = new ObjectPool(Marker::new);
   private final ObjectPool<Drawer> m_drawerPool = new ObjectPool(Drawer::new);
   private boolean m_bOutline = false;
   private float m_outlineR = 1.0F;
   private float m_outlineG = 1.0F;
   private float m_outlineB = 1.0F;
   private boolean m_bUseGroundDepth = true;

   public FBORenderWorldMarkers() {
   }

   public static FBORenderWorldMarkers getInstance() {
      if (instance == null) {
         instance = new FBORenderWorldMarkers();
      }

      return instance;
   }

   public void render(List<WorldMarkers.GridSquareMarker> var1) {
      int var2;
      for(var2 = 0; var2 < this.m_highlights.size(); ++var2) {
         Marker var3 = (Marker)this.m_highlights.get(var2);
         this.m_highlights.remove(var2--);
         this.m_highlightPool.release((Object)var3);
      }

      var2 = IsoCamera.frameState.playerIndex;
      Drawer var12 = (Drawer)this.m_drawerPool.alloc();
      var12.playerIndex = var2;
      this.m_highlightPool.releaseAll(var12.m_highlights);
      var12.m_highlights.clear();

      int var4;
      for(var4 = 0; var4 < var1.size(); ++var4) {
         WorldMarkers.GridSquareMarker var5 = (WorldMarkers.GridSquareMarker)var1.get(var4);
         if (var5.isActive()) {
            float var6 = var5.getOriginalX() + 0.5F;
            float var7 = var5.getOriginalY() + 0.5F;
            float var8 = var5.getSize() * 0.69F;
            Marker var9 = ((Marker)this.m_highlightPool.alloc()).set(var6 - var8, var7 - var8, var6 + var8, var7 + var8, var5.getOriginalZ(), var5.getR(), var5.getG(), var5.getB(), var5.getAlpha());
            String var10 = var5.getTextureName();
            String var11 = var5.getOverlayTextureName();
            if (!StringUtils.equals(var10, "circle_center") && !StringUtils.equals(var10, "circle_highlight_2")) {
               if (StringUtils.equals(var10, "circle_highlight")) {
                  var9.texture1 = Texture.getSharedTexture("media/textures/worldMap/circle_center.png");
                  var9.texture2 = Texture.getSharedTexture("media/textures/worldMap/circle_only_highlight.png");
               }
            } else {
               var9.texture1 = Texture.getSharedTexture("media/textures/worldMap/circle_center.png");
            }

            if (StringUtils.equals(var11, "circle_only_highlight") || StringUtils.equals(var11, "circle_only_highlight_2")) {
               var9.texture2 = Texture.getSharedTexture("media/textures/worldMap/circle_only_highlight.png");
            }

            this.m_highlights.add(var9);
         }
      }

      for(var4 = 0; var4 < this.m_highlights.size(); ++var4) {
         Marker var13 = (Marker)this.m_highlights.get(var4);
         if (var13.isOnScreen(var2)) {
            Marker var14 = ((Marker)this.m_highlightPool.alloc()).set(var13);
            this.renderOutline(var14);
            var12.m_highlights.add(var14);
         }
      }

      if (var12.m_highlights.isEmpty()) {
         this.m_drawerPool.release((Object)var12);
      } else {
         SpriteRenderer.instance.drawGeneric(var12);
      }

   }

   private void renderOutline(Marker var1) {
      if (this.m_bOutline) {
         float var2 = this.m_outlineR;
         float var3 = this.m_outlineG;
         float var4 = this.m_outlineB;
         var2 = var1.r;
         var3 = var1.g;
         var4 = var1.b;
         LineDrawer.addRect(var1.x1, var1.y1, var1.z, var1.x2 - var1.x1, var1.y2 - var1.y1, var2, var3, var4);
      }
   }

   private static final class Marker {
      float x1;
      float y1;
      float x2;
      float y2;
      float z;
      float r;
      float g;
      float b;
      float a;
      long renderTimeMS = 0L;
      Texture texture1;
      Texture texture2;

      private Marker() {
      }

      Marker set(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9) {
         this.x1 = var1;
         this.y1 = var2;
         this.x2 = var3;
         this.y2 = var4;
         this.z = var5;
         this.r = var6;
         this.g = var7;
         this.b = var8;
         this.a = var9;
         this.renderTimeMS = UIManager.uiRenderTimeMS;
         this.texture1 = null;
         this.texture2 = null;
         return this;
      }

      Marker set(Marker var1) {
         this.set(var1.x1, var1.y1, var1.x2, var1.y2, var1.z, var1.r, var1.g, var1.b, var1.a);
         this.texture1 = var1.texture1;
         this.texture2 = var1.texture2;
         return this;
      }

      boolean isOnScreen(int var1) {
         IsoChunkMap var2 = IsoWorld.instance.CurrentCell.getChunkMap(var1);
         if (var2.ignore) {
            return false;
         } else {
            int var3 = var2.getWorldXMinTiles();
            int var4 = var2.getWorldYMinTiles();
            int var5 = var2.getWorldXMaxTiles();
            int var6 = var2.getWorldYMaxTiles();
            return this.x1 < (float)var5 && this.x2 > (float)var3 && this.y1 < (float)var6 && this.y2 > (float)var4;
         }
      }

      void render(float var1, float var2, float var3, float var4, float var5) {
         VBORenderer var6 = VBORenderer.getInstance();
         float var7 = 0.0F;
         float var8 = 0.0F;
         float var9 = 1.0F;
         float var10 = 0.0F;
         float var11 = 1.0F;
         float var12 = 1.0F;
         float var13 = 0.0F;
         float var14 = 1.0F;
         float var15 = (this.x1 + this.x2) / 2.0F;
         float var16 = (this.y1 + this.y2) / 2.0F;
         float var17 = -1.0E-4F;
         var17 -= 5.0E-5F;
         float var18 = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(var1), PZMath.fastfloor(var2), this.x1, this.y1, this.z).depthStart + var17;
         float var19 = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(var1), PZMath.fastfloor(var2), this.x2, this.y1, this.z).depthStart + var17;
         float var20 = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(var1), PZMath.fastfloor(var2), this.x2, this.y2, this.z).depthStart + var17;
         float var21 = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(var1), PZMath.fastfloor(var2), this.x1, this.y2, this.z).depthStart + var17;
         Matrix4f var22 = Core.getInstance().modelViewMatrixStack.alloc();
         var22.scaling(Core.scale);
         var22.scale((float)Core.TileScale / 2.0F);
         var22.rotate(0.5235988F, 1.0F, 0.0F, 0.0F);
         var22.rotate(2.3561945F, 0.0F, 1.0F, 0.0F);
         double var23 = (double)(var15 - var4);
         double var25 = (double)(var16 - var5);
         double var27 = (double)((this.z - var3) * 2.44949F);
         var22.translate(-((float)var23), (float)var27, -((float)var25));
         var22.scale(-1.0F, 1.0F, -1.0F);
         var22.translate(0.0F, -0.71999997F, 0.0F);
         var6.cmdPushAndLoadMatrix(5888, var22);
         var6.startRun(var6.FORMAT_PositionColorUVDepth);
         var6.setMode(7);
         var6.setDepthTest(FBORenderWorldMarkers.getInstance().m_bUseGroundDepth);
         if (this.texture1 != null) {
            var6.setTextureID(this.texture1.getTextureId());
         }

         var6.addQuadDepth(this.x1 - var15, this.z, this.y1 - var16, var7, var8, var18, this.x2 - var15, this.z, this.y1 - var16, var9, var10, var19, this.x2 - var15, this.z, this.y2 - var16, var11, var12, var20, this.x1 - var15, this.z, this.y2 - var16, var13, var14, var21, this.r, this.g, this.b, this.a);
         var6.endRun();
         var6.startRun(var6.FORMAT_PositionColorUVDepth);
         var6.setMode(7);
         var6.setDepthTest(FBORenderWorldMarkers.getInstance().m_bUseGroundDepth);
         if (this.texture2 != null) {
            var6.setTextureID(this.texture2.getTextureId());
         }

         var6.addQuadDepth(this.x1 - var15, this.z, this.y1 - var16, var7, var8, var18, this.x2 - var15, this.z, this.y1 - var16, var9, var10, var19, this.x2 - var15, this.z, this.y2 - var16, var11, var12, var20, this.x1 - var15, this.z, this.y2 - var16, var13, var14, var21, this.r, this.g, this.b, this.a);
         var6.endRun();
         var6.cmdPopMatrix(5888);
      }
   }

   private static final class Drawer extends TextureDraw.GenericDrawer {
      final ArrayList<Marker> m_highlights = new ArrayList();
      int playerIndex;

      private Drawer() {
      }

      public void render() {
         SpriteRenderState var1 = SpriteRenderer.instance.getRenderingState();
         PlayerCamera var2 = var1.playerCamera[var1.playerIndex];
         float var3 = var2.RightClickX;
         float var4 = var2.RightClickY;
         float var5 = var2.getTOffX();
         float var6 = var2.getTOffY();
         float var7 = var2.DeferedX;
         float var8 = var2.DeferedY;
         float var9 = (Float)Core.getInstance().FloatParamMap.get(0);
         float var10 = (Float)Core.getInstance().FloatParamMap.get(1);
         float var11 = (Float)Core.getInstance().FloatParamMap.get(2);
         float var12 = var9 - var2.XToIso(-var5 - var3, -var6 - var4, 0.0F);
         float var13 = var10 - var2.YToIso(-var5 - var3, -var6 - var4, 0.0F);
         var12 += var7;
         var13 += var8;
         double var14 = (double)((float)var2.OffscreenWidth / 1920.0F);
         double var16 = (double)((float)var2.OffscreenHeight / 1920.0F);
         Matrix4f var18 = Core.getInstance().projectionMatrixStack.alloc();
         var18.setOrtho(-((float)var14) / 2.0F, (float)var14 / 2.0F, -((float)var16) / 2.0F, (float)var16 / 2.0F, -10.0F, 10.0F);
         Core.getInstance().projectionMatrixStack.push(var18);
         GL11.glEnable(2929);
         GL11.glDepthFunc(515);
         GL11.glDepthMask(false);
         GL11.glBlendFunc(770, 771);

         for(int var19 = 0; var19 < this.m_highlights.size(); ++var19) {
            Marker var20 = (Marker)this.m_highlights.get(var19);
            var20.render(var9, var10, var11, var12, var13);
         }

         VBORenderer var21 = VBORenderer.getInstance();
         var21.flush();
         Core.getInstance().projectionMatrixStack.pop();
         GLStateRenderThread.restore();
      }

      public void postRender() {
         FBORenderWorldMarkers.getInstance().m_drawerPool.release((Object)this);
      }
   }
}
