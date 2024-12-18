package zombie.gameStates;

import java.util.function.Consumer;
import zombie.GameTime;
import zombie.IndieGL;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.textures.Texture;
import zombie.input.GameKeyboard;
import zombie.input.Mouse;
import zombie.ui.TextManager;
import zombie.ui.UIManager;

public final class TISLogoState extends GameState {
   public float alpha = 0.0F;
   public float alphaStep = 0.02F;
   public float logoDisplayTime = 20.0F;
   public int screenNumber = 1;
   public int stage = 0;
   public float targetAlpha = 0.0F;
   private boolean bNoRender = false;
   private final LogoElement logoTIS = new LogoElement("media/ui/Logos/TheIndieStoneLogo_Lineart_White.png");
   private final LogoElement logoFMOD = new LogoElement("media/ui/Logos/FMOD.png");
   private final LogoElement logoGA = new LogoElement("media/ui/Logos/GeneralArcade.png");
   private final LogoElement logoFI = new LogoElement("media/ui/Logos/FormosaInteractive.png");
   private final LogoElement logoVB = new LogoElement("media/ui/Logos/VertexBreak.png");
   private static final int SCREEN_TIS = 1;
   private static final int SCREEN_OTHER = 2;
   private static final int STAGE_FADING_IN_LOGO = 0;
   private static final int STAGE_HOLDING_LOGO = 1;
   private static final int STAGE_FADING_OUT_LOGO = 2;
   private static final int STAGE_EXIT = 3;

   public TISLogoState() {
   }

   public void enter() {
      UIManager.bSuspend = true;
      this.alpha = 0.0F;
      this.targetAlpha = 1.0F;
   }

   public void exit() {
      UIManager.bSuspend = false;
   }

   public void render() {
      if (this.bNoRender) {
         Core.getInstance().StartFrame();
         SpriteRenderer.instance.renderi((Texture)null, 0, 0, Core.getInstance().getOffscreenWidth(0), Core.getInstance().getOffscreenHeight(0), 0.0F, 0.0F, 0.0F, 1.0F, (Consumer)null);
         Core.getInstance().EndFrame();
      } else {
         Core.getInstance().StartFrame();
         Core.getInstance().EndFrame();
         boolean var1 = UIManager.useUIFBO;
         UIManager.useUIFBO = false;
         Core.getInstance().StartFrameUI();
         SpriteRenderer.instance.renderi((Texture)null, 0, 0, Core.getInstance().getOffscreenWidth(0), Core.getInstance().getOffscreenHeight(0), 0.0F, 0.0F, 0.0F, 1.0F, (Consumer)null);
         if (this.screenNumber == 1) {
            this.logoTIS.centerOnScreen();
            this.logoTIS.render(this.alpha, (String)null);
         }

         if (this.screenNumber == 2) {
            this.renderAttribution();
         }

         Core.getInstance().EndFrameUI();
         UIManager.useUIFBO = var1;
      }
   }

   private void renderAttribution() {
      int var1 = Core.getInstance().getScreenWidth();
      int var2 = Core.getInstance().getScreenHeight();
      int var3 = var1 / 4;
      int var4 = var3;
      if (var2 / 3 < var3) {
         var3 = var2 / 3;
         var4 = var3;
      }

      int var5 = var1 / 2 - var3 / 4 - var3 / 2;
      int var6 = var1 / 2 + var3 / 4 + var3 / 2;
      int var7 = var2 / 2 - var4 / 8 - var4 / 2;
      int var8 = var2 / 2 + var4 / 8 + var4 / 2;
      Texture var9 = this.logoGA.m_texture;
      int var10;
      int var11;
      if (var9 != null && var9.isReady()) {
         var10 = var5 - var3 / 2;
         var11 = var7 - var4 / 2;
         this.logoGA.setBounds(var10, var11, var3, var4);
         this.logoGA.render(this.alpha, (String)null);
      }

      var9 = this.logoVB.m_texture;
      if (var9 != null && var9.isReady()) {
         var10 = var5 - var3 / 2;
         var11 = var8 - var4 / 2;
         this.logoVB.setBounds(var10, var11, var3, var4);
         this.logoVB.render(this.alpha, (String)null);
      }

      var9 = this.logoFI.m_texture;
      if (var9 != null && var9.isReady()) {
         var10 = var6 - var3 / 2;
         var11 = var7 - var4 / 2;
         this.logoFI.setBounds(var10, var11, var3, var4);
         this.logoFI.render(this.alpha, (String)null);
      }

      var9 = this.logoFMOD.m_texture;
      if (var9 != null && var9.isReady()) {
         var10 = var6 - var3 / 2;
         var11 = var8 - var4 / 2;
         this.logoFMOD.setBounds(var10, var11, var3, var4);
         String var12 = "Made with FMOD Studio by Firelight Technologies Pty Ltd.";
         this.logoFMOD.render(this.alpha, var12);
      }

   }

   public GameStateMachine.StateAction update() {
      if (Mouse.isLeftDown() || GameKeyboard.isKeyDown(28) || GameKeyboard.isKeyDown(57) || GameKeyboard.isKeyDown(1)) {
         this.stage = 3;
      }

      if (this.stage == 0) {
         this.targetAlpha = 1.0F;
         if (this.alpha == 1.0F) {
            this.stage = 1;
            this.logoDisplayTime = 20.0F;
         }
      }

      if (this.stage == 1) {
         this.logoDisplayTime -= GameTime.getInstance().getThirtyFPSMultiplier();
         if (this.logoDisplayTime <= 0.0F) {
            this.stage = 2;
         }
      }

      if (this.stage == 2) {
         this.targetAlpha = 0.0F;
         if (this.alpha == 0.0F) {
            if (this.screenNumber == 1) {
               this.screenNumber = 2;
               this.stage = 0;
            } else {
               this.stage = 3;
            }
         }
      }

      if (this.stage == 3) {
         this.targetAlpha = 0.0F;
         if (this.alpha == 0.0F) {
            this.bNoRender = true;
            return GameStateMachine.StateAction.Continue;
         }
      }

      if (this.alpha < this.targetAlpha) {
         this.alpha += this.alphaStep * GameTime.getInstance().getMultiplier();
         if (this.alpha > this.targetAlpha) {
            this.alpha = this.targetAlpha;
         }
      } else if (this.alpha > this.targetAlpha) {
         this.alpha -= this.alphaStep * GameTime.getInstance().getMultiplier();
         if (this.stage == 3) {
            this.alpha -= this.alphaStep * GameTime.getInstance().getMultiplier();
         }

         if (this.alpha < this.targetAlpha) {
            this.alpha = this.targetAlpha;
         }
      }

      return GameStateMachine.StateAction.Remain;
   }

   private static final class LogoElement {
      Texture m_texture;
      int m_x;
      int m_y;
      int m_width;
      int m_height;

      LogoElement(String var1) {
         this.m_texture = Texture.getSharedTexture(var1);
         if (this.m_texture != null) {
            this.m_width = this.m_texture.getWidth();
            this.m_height = this.m_texture.getHeight();
         }

      }

      void centerOnScreen() {
         this.m_x = (Core.getInstance().getScreenWidth() - this.m_width) / 2;
         this.m_y = (Core.getInstance().getScreenHeight() - this.m_height) / 2;
      }

      void setBounds(int var1, int var2, int var3, int var4) {
         this.m_x = var1;
         this.m_y = var2;
         this.m_width = var3;
         this.m_height = var4;
      }

      void render(float var1, String var2) {
         if (this.m_texture != null && this.m_texture.isReady()) {
            float var3 = (float)this.m_width;
            float var4 = (float)this.m_height;
            float var5 = Math.min(var3 / (float)this.m_texture.getWidth(), var4 / (float)this.m_texture.getHeight());
            var3 = (float)this.m_texture.getWidth() * var5;
            var4 = (float)this.m_texture.getHeight() * var5;
            IndieGL.glEnable(3042);
            IndieGL.glBlendFunc(770, 771);
            SpriteRenderer.instance.render(this.m_texture, (float)this.m_x + ((float)this.m_width - var3) / 2.0F, (float)this.m_y + ((float)this.m_height - var4) / 2.0F, var3, var4, 1.0F, 1.0F, 1.0F, var1, (Consumer)null);
            if (var2 != null) {
               float var6 = (float)this.m_y + ((float)this.m_height - var4) / 2.0F + var4 + 16.0F;
               TextManager.instance.DrawStringCentre((double)((float)this.m_x + (float)this.m_width / 2.0F), (double)var6, var2, 1.0, 1.0, 1.0, (double)var1);
            }

         }
      }
   }
}
