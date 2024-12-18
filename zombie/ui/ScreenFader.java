package zombie.ui;

import java.util.function.Consumer;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.textures.Texture;

public final class ScreenFader {
   private Stage m_stage;
   private float m_alpha;
   private float m_targetAlpha;
   private long m_fadeStartMS;
   private long m_fadeDurationMS;

   public ScreenFader() {
      this.m_stage = ScreenFader.Stage.StartFadeToBlack;
      this.m_alpha = 0.0F;
      this.m_targetAlpha = 1.0F;
      this.m_fadeStartMS = 0L;
      this.m_fadeDurationMS = 350L;
   }

   public void startFadeToBlack() {
      this.m_alpha = 0.0F;
      this.m_stage = ScreenFader.Stage.StartFadeToBlack;
   }

   public void startFadeFromBlack() {
      this.m_alpha = 1.0F;
      this.m_stage = ScreenFader.Stage.StartFadeFromBlack;
   }

   public void update() {
      switch (this.m_stage) {
         case StartFadeToBlack:
            this.m_targetAlpha = 1.0F;
            this.m_stage = ScreenFader.Stage.UpdateFadeToBlack;
            this.m_fadeStartMS = System.currentTimeMillis();
            break;
         case UpdateFadeToBlack:
            this.m_alpha = PZMath.clamp((float)(System.currentTimeMillis() - this.m_fadeStartMS) / (float)this.m_fadeDurationMS, 0.0F, 1.0F);
            if (this.m_alpha >= this.m_targetAlpha) {
               this.m_stage = ScreenFader.Stage.Hold;
            }
         case Hold:
         default:
            break;
         case StartFadeFromBlack:
            this.m_targetAlpha = 0.0F;
            this.m_stage = ScreenFader.Stage.UpdateFadeFromBlack;
            this.m_fadeStartMS = System.currentTimeMillis();
            break;
         case UpdateFadeFromBlack:
            this.m_alpha = 1.0F - PZMath.clamp((float)(System.currentTimeMillis() - this.m_fadeStartMS) / (float)this.m_fadeDurationMS, 0.0F, 1.0F);
            if (this.m_alpha <= this.m_targetAlpha) {
               this.m_stage = ScreenFader.Stage.Hold;
            }
      }

   }

   public void preRender() {
      Core.getInstance().StartFrame();
      Core.getInstance().EndFrame();
      Core.getInstance().StartFrameUI();
      this.update();
   }

   public void postRender() {
      this.render();
      Core.getInstance().EndFrameUI();
   }

   public void render() {
      int var1 = Core.getInstance().getScreenWidth();
      int var2 = Core.getInstance().getScreenHeight();
      SpriteRenderer.instance.renderi((Texture)null, 0, 0, var1, var2, 0.0F, 0.0F, 0.0F, this.m_alpha, (Consumer)null);
   }

   public boolean isFading() {
      return this.m_stage != ScreenFader.Stage.Hold;
   }

   public float getAlpha() {
      return this.m_alpha;
   }

   private static enum Stage {
      StartFadeToBlack,
      UpdateFadeToBlack,
      Hold,
      StartFadeFromBlack,
      UpdateFadeFromBlack;

      private Stage() {
      }
   }
}
