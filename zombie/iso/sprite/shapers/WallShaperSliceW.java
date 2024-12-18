package zombie.iso.sprite.shapers;

import zombie.core.textures.TextureDraw;
import zombie.iso.sprite.IsoSprite;

public class WallShaperSliceW extends WallShaper {
   public static final WallShaperSliceW instance = new WallShaperSliceW();

   public WallShaperSliceW() {
   }

   public void accept(TextureDraw var1) {
      super.accept(var1);
      float var2 = IsoSprite.SEAM_SOUTH ? 7.0F : 8.0F;
      float var3 = var2 / (float)var1.tex.getWidthHW();
      var1.x0 = var1.x1 - var2;
      var1.x3 = var1.x2 - var2;
      var1.u0 = var1.u1 - var3;
      var1.u3 = var1.u2 - var3;
      if (var1.tex1 != null) {
         var3 = var2 / (float)var1.tex1.getWidthHW();
         var1.tex1_u0 = var1.tex1_u1 - var3;
         var1.tex1_u3 = var1.tex1_u2 - var3;
      }

      if (var1.tex2 != null) {
         var3 = var2 / (float)var1.tex2.getWidthHW();
         var1.tex2_u0 = var1.tex2_u1 - var3;
         var1.tex2_u3 = var1.tex2_u2 - var3;
      }

      WallPaddingShaper.instance.accept(var1);
   }
}
