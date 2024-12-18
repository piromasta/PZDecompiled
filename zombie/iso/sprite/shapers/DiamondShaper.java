package zombie.iso.sprite.shapers;

import java.util.function.Consumer;
import zombie.core.PerformanceSettings;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugOptions;
import zombie.iso.IsoCamera;
import zombie.iso.fboRenderChunk.FBORenderLevels;

public class DiamondShaper implements Consumer<TextureDraw> {
   public static final DiamondShaper instance = new DiamondShaper();

   public DiamondShaper() {
   }

   public void accept(TextureDraw var1) {
      if (DebugOptions.instance.Terrain.RenderTiles.IsoGridSquare.MeshCutdown.getValue()) {
         float var2 = 0.5F;
         float var3 = 0.5F;
         float var4 = 0.0F;
         float var5 = 0.0F;
         if (PerformanceSettings.FBORenderChunk) {
            int var6 = FBORenderLevels.getTextureScale(IsoCamera.frameState.zoom);
            var2 = (float)var6;
            var3 = (float)var6;
            var4 = var2;
            var5 = var3;
         }

         float var28 = var1.x0 - var2;
         float var7 = var1.y0 - var3;
         float var8 = var1.x1 + var2;
         float var9 = var1.y1 - var3;
         float var10 = var1.y2 + var3;
         float var11 = var1.y3 + var3;
         float var12 = var8 - var28;
         float var13 = var10 - var9;
         float var14 = var28 + var12 * 0.5F;
         float var15 = var9 + var13 * 0.5F;
         float var16 = 1.0F / (float)var1.tex.getWidthHW();
         float var17 = 1.0F / (float)var1.tex.getHeightHW();
         float var18 = var1.u0 - var16 * var4;
         float var19 = var1.v0 - var17 * var5;
         float var20 = var1.u1 + var16 * var4;
         float var21 = var1.v1 - var17 * var5;
         float var22 = var1.v2 + var17 * var5;
         float var23 = var1.v3 + var17 * var5;
         float var24 = var20 - var18;
         float var25 = var22 - var19;
         float var26 = var18 + var24 * 0.5F;
         float var27 = var21 + var25 * 0.5F;
         var1.x0 = var14;
         var1.y0 = var7;
         var1.u0 = var26;
         var1.v0 = var19;
         var1.x1 = var8;
         var1.y1 = var15;
         var1.u1 = var20;
         var1.v1 = var27;
         var1.x2 = var14;
         var1.y2 = var11;
         var1.u2 = var26;
         var1.v2 = var23;
         var1.x3 = var28;
         var1.y3 = var15;
         var1.u3 = var18;
         var1.v3 = var27;
         if (var1.tex1 != null) {
            var16 = 1.0F / (float)var1.tex1.getWidthHW();
            var17 = 1.0F / (float)var1.tex1.getHeightHW();
            var18 = var1.tex1_u0 - var16 * var4;
            var19 = var1.tex1_v0 - var17 * var5;
            var20 = var1.tex1_u1 + var16 * var4;
            var21 = var1.tex1_v1 - var17 * var5;
            var22 = var1.tex1_v2 + var17 * var5;
            var23 = var1.tex1_v3 + var17 * var5;
            var24 = var20 - var18;
            var25 = var22 - var19;
            var26 = var18 + var24 * 0.5F;
            var27 = var21 + var25 * 0.5F;
            var1.tex1_u0 = var26;
            var1.tex1_v0 = var19;
            var1.tex1_u1 = var20;
            var1.tex1_v1 = var27;
            var1.tex1_u2 = var26;
            var1.tex1_v2 = var23;
            var1.tex1_u3 = var18;
            var1.tex1_v3 = var27;
         }

         if (var1.tex2 != null) {
            var16 = var1.tex2_u0;
            var17 = var1.tex2_v0;
            var18 = var1.tex2_u1;
            var19 = var1.tex2_v1;
            var20 = var1.tex2_v2;
            var21 = var1.tex2_v3;
            var22 = var18 - var16;
            var23 = var20 - var17;
            var24 = var16 + var22 * 0.5F;
            var25 = var19 + var23 * 0.5F;
            var1.tex2_u0 = var24;
            var1.tex2_v0 = var17;
            var1.tex2_u1 = var18;
            var1.tex2_v1 = var25;
            var1.tex2_u2 = var24;
            var1.tex2_v2 = var21;
            var1.tex2_u3 = var16;
            var1.tex2_v3 = var25;
         }

      }
   }
}
