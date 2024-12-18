package zombie.pathfind.nativeCode;

import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.opengl.VBORenderer;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugOptions;
import zombie.iso.IsoCamera;
import zombie.iso.PlayerCamera;

public final class PathfindNativeRenderer {
   public static final PathfindNativeRenderer instance = new PathfindNativeRenderer();
   final Drawer[][] drawers = new Drawer[4][3];
   PlayerCamera camera;

   public PathfindNativeRenderer() {
   }

   public void render() {
      if (Core.bDebug) {
         if (DebugOptions.instance.PathfindPathToMouseEnable.getValue()) {
            int var1 = IsoCamera.frameState.playerIndex;
            int var2 = SpriteRenderer.instance.getMainStateIndex();
            Drawer var3 = this.drawers[var1][var2];
            if (var3 == null) {
               var3 = this.drawers[var1][var2] = new Drawer(var1);
            }

            SpriteRenderer.instance.drawGeneric(var3);
         }
      }
   }

   public void drawLine(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10, float var11) {
      VBORenderer var12 = VBORenderer.getInstance();
      float var13 = this.camera.XToScreenExact(var1, var2, var3, 0);
      float var14 = this.camera.YToScreenExact(var1, var2, var3, 0);
      float var15 = this.camera.XToScreenExact(var4, var5, var6, 0);
      float var16 = this.camera.YToScreenExact(var4, var5, var6, 0);
      if (PerformanceSettings.FBORenderChunk) {
         var13 += this.camera.fixJigglyModelsX * this.camera.zoom;
         var14 += this.camera.fixJigglyModelsY * this.camera.zoom;
         var15 += this.camera.fixJigglyModelsX * this.camera.zoom;
         var16 += this.camera.fixJigglyModelsY * this.camera.zoom;
      }

      if (var7 == 1.0F) {
         var12.addLine(var13, var14, 0.0F, var15, var16, 0.0F, var8, var9, var10, var11);
      } else {
         var12.endRun();
         var12.startRun(var12.FORMAT_PositionColor);
         var12.setMode(7);
         var12.addLineWithThickness(var13, var14, 0.0F, var15, var16, 0.0F, var7, var8, var9, var10, var11);
         var12.endRun();
         var12.startRun(var12.FORMAT_PositionColor);
         var12.setMode(1);
      }

   }

   public void drawRect(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9) {
      float var10 = 1.0F;
      this.drawLine(var1, var2, var3, var1 + var4, var2, var3, var10, var6, var7, var8, var9);
      this.drawLine(var1 + var4, var2, var3, var1 + var4, var2 + var5, var3, var10, var6, var7, var8, var9);
      this.drawLine(var1 + var4, var2 + var5, var3, var1, var2 + var5, var3, var10, var6, var7, var8, var9);
      this.drawLine(var1, var2 + var5, var3, var1, var2, var3, var10, var6, var7, var8, var9);
   }

   native void renderNative();

   native void setDebugOption(String var1, String var2);

   static final class Drawer extends TextureDraw.GenericDrawer {
      int playerIndex;

      Drawer(int var1) {
         this.playerIndex = var1;
      }

      public void render() {
         if (PathfindNativeThread.instance != null) {
            synchronized(PathfindNativeThread.instance.renderLock) {
               VBORenderer var2 = VBORenderer.getInstance();
               var2.startRun(var2.FORMAT_PositionColor);
               var2.setMode(1);
               int var3 = SpriteRenderer.instance.getRenderingPlayerIndex();
               PathfindNativeRenderer.instance.camera = SpriteRenderer.instance.getRenderingPlayerCamera(var3);
               PathfindNativeRenderer.instance.setDebugOption(DebugOptions.instance.PathfindPathToMouseRenderSuccessors.getName(), DebugOptions.instance.PathfindPathToMouseRenderSuccessors.getValueAsString());
               PathfindNativeRenderer.instance.setDebugOption(DebugOptions.instance.PathfindSmoothPlayerPath.getName(), DebugOptions.instance.PathfindSmoothPlayerPath.getValueAsString());
               PathfindNativeRenderer.instance.setDebugOption(DebugOptions.instance.PathfindRenderChunkRegions.getName(), DebugOptions.instance.PathfindRenderChunkRegions.getValueAsString());
               PathfindNativeRenderer.instance.setDebugOption(DebugOptions.instance.PathfindRenderPath.getName(), DebugOptions.instance.PathfindRenderPath.getValueAsString());
               PathfindNativeRenderer.instance.setDebugOption(DebugOptions.instance.PolymapRenderClusters.getName(), DebugOptions.instance.PolymapRenderClusters.getValueAsString());
               PathfindNativeRenderer.instance.setDebugOption(DebugOptions.instance.PolymapRenderConnections.getName(), DebugOptions.instance.PolymapRenderConnections.getValueAsString());
               PathfindNativeRenderer.instance.setDebugOption(DebugOptions.instance.PolymapRenderCrawling.getName(), DebugOptions.instance.PolymapRenderCrawling.getValueAsString());
               PathfindNativeRenderer.instance.setDebugOption(DebugOptions.instance.PolymapRenderNodes.getName(), DebugOptions.instance.PolymapRenderNodes.getValueAsString());
               PathfindNativeRenderer.instance.renderNative();
               var2.endRun();
               var2.flush();
            }
         }
      }
   }
}
