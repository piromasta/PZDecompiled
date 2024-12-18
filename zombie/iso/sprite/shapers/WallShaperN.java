package zombie.iso.sprite.shapers;

import zombie.core.textures.TextureDraw;

public class WallShaperN extends WallShaper {
   public static final WallShaperN instance = new WallShaperN();

   public WallShaperN() {
   }

   public void accept(TextureDraw var1) {
      super.accept(var1);
      var1.x0 = var1.x0 * 0.5F + var1.x1 * 0.5F;
      var1.x3 = var1.x2 * 0.5F + var1.x3 * 0.5F;
      var1.u0 = var1.u0 * 0.5F + var1.u1 * 0.5F;
      var1.u3 = var1.u2 * 0.5F + var1.u3 * 0.5F;
      if (var1.tex1 != null) {
         var1.tex1_u0 = var1.tex1_u0 * 0.5F + var1.tex1_u1 * 0.5F;
         var1.tex1_u3 = var1.tex1_u2 * 0.5F + var1.tex1_u3 * 0.5F;
      }

      if (var1.tex2 != null) {
         var1.tex2_u0 = var1.tex2_u0 * 0.5F + var1.tex2_u1 * 0.5F;
         var1.tex2_u3 = var1.tex2_u2 * 0.5F + var1.tex2_u3 * 0.5F;
      }

      WallPaddingShaper.instance.accept(var1);
   }
}
