package zombie.iso.fboRenderChunk;

import java.util.ArrayList;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import zombie.characters.IsoPlayer;
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
import zombie.iso.areas.DesignationZone;
import zombie.iso.areas.DesignationZoneAnimal;
import zombie.iso.areas.NonPvpZone;
import zombie.iso.areas.SafeHouse;
import zombie.network.GameClient;
import zombie.popman.ObjectPool;
import zombie.ui.UIManager;

public class FBORenderAreaHighlights {
   private static FBORenderAreaHighlights instance = null;
   private final ArrayList<AreaHighlight> m_highlights = new ArrayList();
   private final ObjectPool<AreaHighlight> m_highlightPool = new ObjectPool(AreaHighlight::new);
   private final ObjectPool<Drawer> m_drawerPool = new ObjectPool(Drawer::new);
   private boolean m_bOutline = true;
   private float m_outlineR = 1.0F;
   private float m_outlineG = 1.0F;
   private float m_outlineB = 1.0F;
   private boolean m_bUseGroundDepth = true;

   public FBORenderAreaHighlights() {
   }

   public static FBORenderAreaHighlights getInstance() {
      if (instance == null) {
         instance = new FBORenderAreaHighlights();
      }

      return instance;
   }

   public void addHighlight(int var1, int var2, int var3, int var4, int var5, float var6, float var7, float var8, float var9) {
      AreaHighlight var10 = ((AreaHighlight)this.m_highlightPool.alloc()).set(var1, var2, var3, var4, var5, var6, var7, var8, var9);
      this.m_highlights.add(var10);
   }

   public void render() {
      int var1;
      for(var1 = 0; var1 < this.m_highlights.size(); ++var1) {
         AreaHighlight var2 = (AreaHighlight)this.m_highlights.get(var1);
         if (UIManager.uiRenderTimeMS != var2.renderTimeMS) {
            this.m_highlights.remove(var1--);
            this.m_highlightPool.release((Object)var2);
         }
      }

      var1 = IsoCamera.frameState.playerIndex;
      Drawer var3 = (Drawer)this.m_drawerPool.alloc();
      var3.playerIndex = var1;
      this.m_highlightPool.releaseAll(var3.m_highlights);
      var3.m_highlights.clear();
      this.renderUserDefinedAreas(var3);
      this.renderNonPVPZones(var3);
      this.renderSafehouses(var3);
      this.renderAnimalDesigationZones(var3);
      if (var3.m_highlights.isEmpty()) {
         this.m_drawerPool.release((Object)var3);
      } else {
         SpriteRenderer.instance.drawGeneric(var3);
      }

   }

   private void renderUserDefinedAreas(Drawer var1) {
      int var2 = var1.playerIndex;

      for(int var3 = 0; var3 < this.m_highlights.size(); ++var3) {
         AreaHighlight var4 = (AreaHighlight)this.m_highlights.get(var3);
         if (var4.isOnScreen(var2)) {
            AreaHighlight var5 = ((AreaHighlight)this.m_highlightPool.alloc()).set(var4);
            this.renderOutline(var5);
            var5.clampToChunkMap(var2);
            var1.m_highlights.add(var5);
         }
      }

   }

   private void renderNonPVPZones(Drawer var1) {
      int var2 = var1.playerIndex;
      IsoPlayer var3 = IsoPlayer.players[var2];
      if (GameClient.bClient && var3 != null && var3.isSeeNonPvpZone()) {
         ArrayList var4 = NonPvpZone.getAllZones();

         for(int var5 = 0; var5 < var4.size(); ++var5) {
            NonPvpZone var6 = (NonPvpZone)var4.get(var5);
            float var7 = 0.0F;
            float var8 = 0.0F;
            float var9 = 1.0F;
            float var10 = 0.25F;
            AreaHighlight var11 = ((AreaHighlight)this.m_highlightPool.alloc()).set(var6.getX(), var6.getY(), var6.getX2(), var6.getY2(), 0, var7, var8, var9, var10);
            this.tryRenderArea(var1, var11);
         }
      }

   }

   private void renderSafehouses(Drawer var1) {
      int var2 = var1.playerIndex;
      if (GameClient.bClient && Core.bDebug) {
         ArrayList var3 = SafeHouse.getSafehouseList();

         for(int var4 = 0; var4 < var3.size(); ++var4) {
            SafeHouse var5 = (SafeHouse)var3.get(var4);
            float var6 = 1.0F;
            float var7 = 0.0F;
            float var8 = 0.0F;
            float var9 = 0.25F;
            AreaHighlight var10 = ((AreaHighlight)this.m_highlightPool.alloc()).set(var5.getX(), var5.getY(), var5.getX2(), var5.getY2(), 0, var6, var7, var8, var9);
            this.tryRenderArea(var1, var10);
         }
      }

   }

   private void renderAnimalDesigationZones(Drawer var1) {
      int var2 = var1.playerIndex;
      IsoPlayer var3 = IsoPlayer.players[var2];
      if (var3 != null && var3.isSeeDesignationZone()) {
         ArrayList var4 = var3.getSelectedZonesForHighlight();
         ArrayList var5 = DesignationZone.allZones;

         for(int var6 = 0; var6 < var5.size(); ++var6) {
            DesignationZone var7 = (DesignationZone)var5.get(var6);
            float var8 = DesignationZoneAnimal.ZONECOLORR;
            float var9 = DesignationZoneAnimal.ZONECOLORG;
            float var10 = DesignationZoneAnimal.ZONECOLORB;
            float var11 = 0.8F;
            if (var4.contains(var7.getId())) {
               var8 = DesignationZoneAnimal.ZONESELECTEDCOLORR;
               var9 = DesignationZoneAnimal.ZONESELECTEDCOLORG;
               var10 = DesignationZoneAnimal.ZONESELECTEDCOLORB;
               var11 = 0.8F;
            }

            AreaHighlight var12 = ((AreaHighlight)this.m_highlightPool.alloc()).set(var7.x, var7.y, var7.x + var7.w, var7.y + var7.h, var7.z, var8, var9, var10, var11);
            this.tryRenderArea(var1, var12);
         }
      }

   }

   private void tryRenderArea(Drawer var1, AreaHighlight var2) {
      int var3 = var1.playerIndex;
      if (var2.isOnScreen(var3)) {
         this.renderOutline(var2);
         var2.clampToChunkMap(var3);
         var1.m_highlights.add(var2);
      } else {
         this.m_highlightPool.release((Object)var2);
      }

   }

   private void renderOutline(AreaHighlight var1) {
      if (this.m_bOutline) {
         float var2 = this.m_outlineR;
         float var3 = this.m_outlineG;
         float var4 = this.m_outlineB;
         var2 = var1.r;
         var3 = var1.g;
         var4 = var1.b;
         LineDrawer.addRect((float)var1.x1, (float)var1.y1, (float)var1.z, (float)(var1.x2 - var1.x1), (float)(var1.y2 - var1.y1), var2, var3, var4);
      }
   }

   private static final class AreaHighlight {
      int x1;
      int y1;
      int x2;
      int y2;
      int z;
      float r;
      float g;
      float b;
      float a;
      long renderTimeMS = 0L;

      private AreaHighlight() {
      }

      AreaHighlight set(int var1, int var2, int var3, int var4, int var5, float var6, float var7, float var8, float var9) {
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
         return this;
      }

      AreaHighlight set(AreaHighlight var1) {
         return this.set(var1.x1, var1.y1, var1.x2, var1.y2, var1.z, var1.r, var1.g, var1.b, var1.a);
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
            return this.x1 < var5 && this.x2 > var3 && this.y1 < var6 && this.y2 > var4;
         }
      }

      void clampToChunkMap(int var1) {
         IsoChunkMap var2 = IsoWorld.instance.CurrentCell.getChunkMap(var1);
         if (!var2.ignore) {
            int var3 = var2.getWorldXMinTiles();
            int var4 = var2.getWorldYMinTiles();
            int var5 = var2.getWorldXMaxTiles();
            int var6 = var2.getWorldYMaxTiles();
            this.x1 = PZMath.max(this.x1, var3);
            this.y1 = PZMath.max(this.y1, var4);
            this.x2 = PZMath.min(this.x2, var5);
            this.y2 = PZMath.min(this.y2, var6);
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
         float var15 = (float)(this.x1 + this.x2) / 2.0F;
         float var16 = (float)(this.y1 + this.y2) / 2.0F;
         float var17 = var15 + var16 < var1 + var2 ? -1.4E-4F : -1.0E-4F;
         float var18 = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(var1), PZMath.fastfloor(var2), (float)this.x1, (float)this.y1, (float)this.z).depthStart + var17;
         float var19 = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(var1), PZMath.fastfloor(var2), (float)this.x2, (float)this.y1, (float)this.z).depthStart + var17;
         float var20 = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(var1), PZMath.fastfloor(var2), (float)this.x2, (float)this.y2, (float)this.z).depthStart + var17;
         float var21 = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(var1), PZMath.fastfloor(var2), (float)this.x1, (float)this.y2, (float)this.z).depthStart + var17;
         Matrix4f var22 = Core.getInstance().modelViewMatrixStack.alloc();
         var22.scaling(Core.scale);
         var22.scale((float)Core.TileScale / 2.0F);
         var22.rotate(0.5235988F, 1.0F, 0.0F, 0.0F);
         var22.rotate(2.3561945F, 0.0F, 1.0F, 0.0F);
         double var23 = (double)(var15 - var4);
         double var25 = (double)(var16 - var5);
         double var27 = (double)(((float)this.z - var3) * 2.44949F);
         var22.translate(-((float)var23), (float)var27, -((float)var25));
         var22.scale(-1.0F, 1.0F, -1.0F);
         var22.translate(0.0F, -0.71999997F, 0.0F);
         var6.cmdPushAndLoadMatrix(5888, var22);
         var6.startRun(var6.FORMAT_PositionColorUVDepth);
         var6.setMode(7);
         var6.setDepthTest(FBORenderAreaHighlights.getInstance().m_bUseGroundDepth);
         var6.setTextureID(Texture.getWhite().getTextureId());
         var6.addQuadDepth((float)this.x1 - var15, (float)this.z, (float)this.y1 - var16, var7, var8, var18, (float)this.x2 - var15, (float)this.z, (float)this.y1 - var16, var9, var10, var19, (float)this.x2 - var15, (float)this.z, (float)this.y2 - var16, var11, var12, var20, (float)this.x1 - var15, (float)this.z, (float)this.y2 - var16, var13, var14, var21, this.r, this.g, this.b, this.a);
         var6.endRun();
         var6.cmdPopMatrix(5888);
      }
   }

   private static final class Drawer extends TextureDraw.GenericDrawer {
      final ArrayList<AreaHighlight> m_highlights = new ArrayList();
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
            AreaHighlight var20 = (AreaHighlight)this.m_highlights.get(var19);
            var20.render(var9, var10, var11, var12, var13);
         }

         VBORenderer var21 = VBORenderer.getInstance();
         var21.flush();
         Core.getInstance().projectionMatrixStack.pop();
         GLStateRenderThread.restore();
      }

      public void postRender() {
         FBORenderAreaHighlights.getInstance().m_drawerPool.release((Object)this);
      }
   }
}
