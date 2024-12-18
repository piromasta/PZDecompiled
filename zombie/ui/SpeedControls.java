package zombie.ui;

import zombie.GameTime;
import zombie.SoundManager;
import zombie.core.Core;
import zombie.core.textures.Texture;
import zombie.network.GameClient;
import zombie.network.GameServer;

public final class SpeedControls extends UIElement {
   public static SpeedControls instance = null;
   public static final int PauseSpeed = 0;
   public static final int PlaySpeed = 1;
   public static final int FastForwardSpeed = 2;
   public static final int FasterForwardSpeed = 3;
   public static final int WaitSpeed = 4;
   private static final int ResumeSpeed = 5;
   private final float PlayMultiplier = 1.0F;
   private final float FastForwardMultiplier = 5.0F;
   private final float FasterForwardMultipler = 20.0F;
   private final float WaitMultiplier = 40.0F;
   private int CurrentSpeed = 1;
   private int SpeedBeforePause = 1;
   private float MultiBeforePause = 1.0F;
   private boolean doFrameStep = false;
   private boolean frameSkipped = false;
   private boolean MouseOver = false;
   private static HUDButton Play;
   private static HUDButton Pause;
   private static HUDButton FastForward;
   private static HUDButton FasterForward;
   private static HUDButton Wait;
   private static HUDButton StepForward;

   public SpeedControls() {
      this.x = 0.0;
      this.y = 0.0;
      byte var1 = 2;
      StepForward = new SCButton("StepForward", 1.0F, 0.0F, "media/ui/speedControls/StepForward_Off.png", "media/ui/speedControls/StepForward_Off.png", this);
      Pause = new SCButton("Pause", (float)(StepForward.x + (double)StepForward.width + (double)var1), 0.0F, "media/ui/speedControls/Pause_Off.png", "media/ui/speedControls/Pause_On.png", this);
      Play = new SCButton("Play", (float)(Pause.x + (double)Pause.width + (double)var1), 0.0F, "media/ui/speedControls/Play_Off.png", "media/ui/speedControls/Play_On.png", this);
      FastForward = new SCButton("Fast Forward x 1", (float)(Play.x + (double)Play.width + (double)var1), 0.0F, "media/ui/speedControls/FFwd1_Off.png", "media/ui/speedControls/FFwd1_On.png", this);
      FasterForward = new SCButton("Fast Forward x 2", (float)(FastForward.x + (double)FastForward.width + (double)var1), 0.0F, "media/ui/speedControls/FFwd2_Off.png", "media/ui/speedControls/FFwd2_On.png", this);
      Wait = new SCButton("Wait", (float)(FasterForward.x + (double)FasterForward.width + (double)var1), 0.0F, "media/ui/speedControls/Wait_Off.png", "media/ui/speedControls/Wait_On.png", this);
      this.width = (float)((int)Wait.x) + Wait.width;
      this.height = Wait.height;
      if (Core.bDebug) {
         this.AddChild(StepForward);
      }

      this.AddChild(Pause);
      this.AddChild(Play);
      this.AddChild(FastForward);
      this.AddChild(FasterForward);
      this.AddChild(Wait);
   }

   public void ButtonClicked(String var1) {
      GameTime.instance.setMultiplier(1.0F);
      if (var1.equals(StepForward.name)) {
         if (this.CurrentSpeed > 0) {
            this.SetCurrentGameSpeed(0);
         } else {
            this.SetCurrentGameSpeed(1);
            this.doFrameStep = true;
         }
      }

      if (var1.equals(Pause.name)) {
         if (this.CurrentSpeed > 0) {
            this.SetCurrentGameSpeed(0);
         } else {
            this.SetCurrentGameSpeed(5);
         }
      }

      if (var1.equals(Play.name)) {
         this.SetCurrentGameSpeed(1);
         GameTime.instance.setMultiplier(1.0F);
      }

      if (var1.equals(FastForward.name)) {
         this.SetCurrentGameSpeed(2);
         GameTime.instance.setMultiplier(5.0F);
      }

      if (var1.equals(FasterForward.name)) {
         this.SetCurrentGameSpeed(3);
         GameTime.instance.setMultiplier(20.0F);
      }

      if (var1.equals(Wait.name)) {
         this.SetCurrentGameSpeed(4);
         GameTime.instance.setMultiplier(40.0F);
      }

   }

   public int getCurrentGameSpeed() {
      return !GameClient.bClient && !GameServer.bServer ? this.CurrentSpeed : 1;
   }

   public void SetCorrectIconStates() {
      if (this.CurrentSpeed == 0) {
         super.ButtonClicked(Pause.name);
      }

      if (this.CurrentSpeed == 1) {
         super.ButtonClicked(Play.name);
      }

      if (GameTime.instance.getTrueMultiplier() == 5.0F) {
         super.ButtonClicked(FastForward.name);
      }

      if (GameTime.instance.getTrueMultiplier() == 20.0F) {
         super.ButtonClicked(FasterForward.name);
      }

      if (GameTime.instance.getTrueMultiplier() == 40.0F) {
         super.ButtonClicked(Wait.name);
      }

   }

   public void Pause() {
      this.SetCurrentGameSpeed(0);
   }

   public void SetCurrentGameSpeed(int var1) {
      if (this.CurrentSpeed > 0 && var1 == 0) {
         SoundManager.instance.pauseSoundAndMusic();
         SoundManager.instance.setMusicState("PauseMenu");
      } else if (this.CurrentSpeed == 0 && var1 > 0) {
         SoundManager.instance.setMusicState("InGame");
         SoundManager.instance.resumeSoundAndMusic();
      }

      GameTime.instance.setMultiplier(1.0F);
      if (var1 == 0) {
         this.SpeedBeforePause = this.CurrentSpeed;
         this.MultiBeforePause = GameTime.instance.getMultiplier();
      }

      if (var1 == 5) {
         var1 = this.SpeedBeforePause;
         GameTime.instance.setMultiplier(this.MultiBeforePause);
      }

      this.CurrentSpeed = var1;
      this.SetCorrectIconStates();
   }

   public Boolean onMouseMove(double var1, double var3) {
      if (!this.isVisible()) {
         return false;
      } else {
         this.MouseOver = true;
         super.onMouseMove(var1, var3);
         this.SetCorrectIconStates();
         return Boolean.TRUE;
      }
   }

   public void onMouseMoveOutside(double var1, double var3) {
      super.onMouseMoveOutside(var1, var3);
      this.MouseOver = false;
      this.SetCorrectIconStates();
   }

   public void render() {
      super.render();
      if ("Tutorial".equals(Core.GameMode)) {
         StepForward.setVisible(false);
         Pause.setVisible(false);
         Play.setVisible(false);
         FastForward.setVisible(false);
         FasterForward.setVisible(false);
         Wait.setVisible(false);
      }

      this.SetCorrectIconStates();
   }

   public void update() {
      super.update();
      this.SetCorrectIconStates();
      if (this.frameSkipped) {
         this.SetCurrentGameSpeed(0);
         this.frameSkipped = false;
      }

      if (this.doFrameStep) {
         this.frameSkipped = true;
         this.doFrameStep = false;
      }

   }

   public void stepForward() {
      if (GameClient.bClient) {
         GameClient.SendCommandToServer("/setTimeSpeed 0");
      } else if (this.CurrentSpeed > 0) {
         this.SetCurrentGameSpeed(0);
      } else {
         this.SetCurrentGameSpeed(1);
         this.doFrameStep = true;
      }

   }

   public boolean isPaused() {
      return this.CurrentSpeed == 0;
   }

   public static final class SCButton extends HUDButton {
      private static final int BORDER = 3;

      public SCButton(String var1, float var2, float var3, String var4, String var5, UIElement var6) {
         super(var1, (double)var2, (double)var3, var4, var5, var6);
         this.width += 6.0F;
         this.height += 6.0F;
      }

      public void render() {
         int var1 = 3;
         if (this.clicked) {
            ++var1;
         }

         this.DrawTextureScaledCol((Texture)null, 0.0, this.clicked ? 1.0 : 0.0, (double)this.width, (double)this.height, 0.0, 0.0, 0.0, 0.75);
         if (!this.mouseOver && !this.name.equals(this.display.getClickedValue())) {
            this.DrawTextureScaled(this.texture, 3.0, (double)var1, (double)this.texture.getWidth(), (double)this.texture.getHeight(), (double)this.notclickedAlpha);
         } else {
            this.DrawTextureScaled(this.highlight, 3.0, (double)var1, (double)this.highlight.getWidth(), (double)this.highlight.getHeight(), (double)this.clickedalpha);
         }

         if (this.overicon != null) {
            this.DrawTextureScaled(this.overicon, 3.0, (double)var1, (double)this.overicon.getWidth(), (double)this.overicon.getHeight(), 1.0);
         }

      }
   }
}
