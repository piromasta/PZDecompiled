package zombie.viewCone;

import zombie.core.Core;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureFBO;

public class ViewConeTextureFBO {
   public TextureFBO viewConeFBO;
   public static ViewConeTextureFBO instance = new ViewConeTextureFBO();
   private Texture tex;
   boolean inited = false;
   int ww = 0;
   int hh = 0;

   public ViewConeTextureFBO() {
   }

   public void init() {
      this.resize(Core.getInstance().getScreenWidth(), Core.getInstance().getScreenHeight());
      this.inited = true;
   }

   public void resize(int var1, int var2) {
      if (this.viewConeFBO != null) {
         this.viewConeFBO.releaseTexture();
         this.viewConeFBO.destroy();
      }

      if (this.tex != null) {
         this.tex.destroy();
      }

      this.tex = new Texture(var1 / 4, var2 / 4, 16);
      this.viewConeFBO = new TextureFBO(this.tex, false);
      this.ww = var1;
      this.hh = var2;
   }

   public void stopDrawing() {
      if (!this.inited) {
         this.init();
      }

      this.viewConeFBO.endDrawing();
   }

   public void startDrawing() {
      if (!this.inited) {
         this.init();
      } else if (Core.getInstance().getScreenWidth() != this.ww || Core.getInstance().getScreenHeight() != this.hh) {
         this.resize(Core.getInstance().getScreenWidth(), Core.getInstance().getScreenHeight());
      }

      this.viewConeFBO.startDrawing(true, true);
   }

   public Texture getTexture() {
      return this.tex;
   }
}
