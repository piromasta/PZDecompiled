package zombie.characters;

import zombie.core.opengl.RenderThread;
import zombie.core.rendering.RenderTarget;
import zombie.core.rendering.RenderTexture;

public class Imposter {
   public static RenderTexture BlendTexture;
   public RenderTexture card;
   public boolean cardRendered = false;
   public int sinceLastUpdate = 0;
   public static int ImposterCount = 0;
   public static final int UpdateDelay = 10;
   public static final int Width = 256;
   public static final int Height = 256;

   public Imposter() {
   }

   public static void CreateBlend() {
      if (BlendTexture == null) {
         BlendTexture = new RenderTexture(new RenderTexture.Descriptor("Imposter Blend") {
            {
               this.width = 256;
               this.height = 256;
               this.colourFormat = 32856;
               this.depthFormat = 35056;
               this.depthAsTexture = true;
            }
         });
         BlendTexture.Create();
      }
   }

   public void create() {
      if (this.card == null) {
         this.card = new RenderTexture(new RenderTexture.Descriptor("Imposter Card") {
            {
               this.width = 256;
               this.height = 256;
               this.colourFormat = 32856;
               this.depthFormat = 33189;
               this.depthAsTexture = true;
            }
         });
         this.card.Create();
         this.sinceLastUpdate = ImposterCount % 10;
         ++ImposterCount;
      }

   }

   public void destroy() {
      if (this.card != null) {
         RenderThread.invokeOnRenderContext(this.card, RenderTarget::Destroy);
         this.card = null;
         this.cardRendered = false;
         this.sinceLastUpdate = 0;
      }

   }
}
