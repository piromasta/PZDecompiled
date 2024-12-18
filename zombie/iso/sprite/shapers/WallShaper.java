package zombie.iso.sprite.shapers;

import java.util.function.Consumer;
import zombie.core.Color;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugOptions;

public class WallShaper implements Consumer<TextureDraw> {
   public final int[] col = new int[4];
   protected int colTint = 0;

   public WallShaper() {
   }

   public void setTintColor(int var1) {
      this.colTint = var1;
   }

   public void accept(TextureDraw var1) {
      if (DebugOptions.instance.Terrain.RenderTiles.IsoGridSquare.Walls.Lighting.getValue()) {
         var1.col0 = Color.blendBGR(var1.col0, this.col[0]);
         var1.col1 = Color.blendBGR(var1.col1, this.col[1]);
         var1.col2 = Color.blendBGR(var1.col2, this.col[2]);
         var1.col3 = Color.blendBGR(var1.col3, this.col[3]);
      }

      if (DebugOptions.instance.FBORenderChunk.NoLighting.getValue()) {
         float var2 = Color.getAlphaChannelFromABGR(var1.col0);
         var1.col0 = Color.colorToABGR(1.0F, 1.0F, 1.0F, var2);
         var1.col1 = var1.col0;
         var1.col2 = var1.col0;
         var1.col3 = var1.col0;
      }

      if (this.colTint != 0) {
         var1.col0 = Color.tintABGR(var1.col0, this.colTint);
         var1.col1 = Color.tintABGR(var1.col1, this.colTint);
         var1.col2 = Color.tintABGR(var1.col2, this.colTint);
         var1.col3 = Color.tintABGR(var1.col3, this.colTint);
      }

   }
}
