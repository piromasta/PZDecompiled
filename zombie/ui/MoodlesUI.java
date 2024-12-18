package zombie.ui;

import java.util.Stack;
import org.lwjgl.util.Rectangle;
import zombie.characters.IsoGameCharacter;
import zombie.characters.Moodles.MoodleType;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.textures.Texture;
import zombie.input.GameKeyboard;
import zombie.input.Mouse;

public final class MoodlesUI extends UIElement {
   public float clientH = 0.0F;
   public float clientW = 0.0F;
   public boolean Movable = false;
   public int ncclientH = 0;
   public int ncclientW = 0;
   private static MoodlesUI instance = null;
   private static final float OFFSCREEN_Y = 10000.0F;
   public Stack<Rectangle> nestedItems = new Stack();
   float alpha = 1.0F;
   Texture Border = null;
   Texture Background = null;
   Texture Endurance = null;
   Texture Bleeding = null;
   Texture Angry = null;
   Texture Stress = null;
   Texture Thirst = null;
   Texture Panic = null;
   Texture Hungry = null;
   Texture Injured = null;
   Texture Pain = null;
   Texture Sick = null;
   Texture Bored = null;
   Texture Unhappy = null;
   Texture Tired = null;
   Texture HeavyLoad = null;
   Texture Drunk = null;
   Texture Wet = null;
   Texture HasACold = null;
   Texture Dead = null;
   Texture Zombie = null;
   Texture Windchill = null;
   Texture CantSprint = null;
   Texture Uncomfortable = null;
   Texture NoxiousSmell = null;
   Texture FoodEaten = null;
   Texture Hyperthermia = null;
   Texture Hypothermia = null;
   public static Texture plusRed;
   public static Texture plusGreen;
   public static Texture minusRed;
   public static Texture minusGreen;
   float MoodleDistY = 74.0F;
   boolean MouseOver = false;
   int MouseOverSlot = 0;
   int NumUsedSlots = 0;
   private int DebugKeyDelay = 0;
   private int DistFromRighEdge = 10;
   private int[] GoodBadNeutral;
   private int[] MoodleLevel;
   private float[] MoodleOscilationLevel;
   private float[] MoodleSlotsDesiredPos;
   private float[] MoodleSlotsPos;
   private int[] MoodleTypeInSlot;
   private float Oscilator;
   private float OscilatorDecelerator;
   private float OscilatorRate;
   private float OscilatorScalar;
   private float OscilatorStartLevel;
   private float OscilatorStep;
   private IsoGameCharacter UseCharacter;
   private boolean alphaIncrease;

   public MoodlesUI() {
      this.GoodBadNeutral = new int[MoodleType.ToIndex(MoodleType.MAX)];
      this.MoodleLevel = new int[MoodleType.ToIndex(MoodleType.MAX)];
      this.MoodleOscilationLevel = new float[MoodleType.ToIndex(MoodleType.MAX)];
      this.MoodleSlotsDesiredPos = new float[MoodleType.ToIndex(MoodleType.MAX)];
      this.MoodleSlotsPos = new float[MoodleType.ToIndex(MoodleType.MAX)];
      this.MoodleTypeInSlot = new int[MoodleType.ToIndex(MoodleType.MAX)];
      this.Oscilator = 0.0F;
      this.OscilatorDecelerator = 0.96F;
      this.OscilatorRate = 0.8F;
      this.OscilatorScalar = 15.6F;
      this.OscilatorStartLevel = 1.0F;
      this.OscilatorStep = 0.0F;
      this.UseCharacter = null;
      this.alphaIncrease = true;
      this.x = (double)(Core.getInstance().getScreenWidth() - this.DistFromRighEdge);
      this.y = 120.0;
      this.width = 64.0F;
      this.height = (float)Core.getInstance().getScreenHeight();
      int var1 = 0;
      var1 |= 64;
      this.Border = Texture.getSharedTexture("media/ui/Moodles/Border.png", var1);
      this.Background = Texture.getSharedTexture("media/ui/Moodles/Background.png", var1);
      this.Endurance = Texture.getSharedTexture("media/ui/Moodles/Status_DifficultyBreathing.png", var1);
      this.Tired = Texture.getSharedTexture("media/ui/Moodles/Mood_Sleepy.png", var1);
      this.Hungry = Texture.getSharedTexture("media/ui/Moodles/Status_Hunger.png", var1);
      this.Panic = Texture.getSharedTexture("media/ui/Moodles/Mood_Panicked.png", var1);
      this.Sick = Texture.getSharedTexture("media/ui/Moodles/Mood_Nauseous.png", var1);
      this.Bored = Texture.getSharedTexture("media/ui/Moodles/Mood_Bored.png", var1);
      this.Unhappy = Texture.getSharedTexture("media/ui/Moodles/Mood_Sad.png", var1);
      this.Bleeding = Texture.getSharedTexture("media/ui/Moodles/Status_Bleeding.png", var1);
      this.Wet = Texture.getSharedTexture("media/ui/Moodles/Status_Wet.png", var1);
      this.HasACold = Texture.getSharedTexture("media/ui/Moodles/Mood_Ill.png", var1);
      this.Angry = Texture.getSharedTexture("media/ui/Moodles/Mood_Angry.png", var1);
      this.Stress = Texture.getSharedTexture("media/ui/Moodles/Mood_Stressed.png", var1);
      this.Thirst = Texture.getSharedTexture("media/ui/Moodles/Status_Thirst.png", var1);
      this.Injured = Texture.getSharedTexture("media/ui/Moodles/Status_InjuredMinor.png", var1);
      this.Pain = Texture.getSharedTexture("media/ui/Moodles/Mood_Pained.png", var1);
      this.HeavyLoad = Texture.getSharedTexture("media/ui/Moodles/Status_HeavyLoad.png", var1);
      this.Drunk = Texture.getSharedTexture("media/ui/Moodles/Mood_Drunk.png", var1);
      this.Dead = Texture.getSharedTexture("media/ui/Moodles/Mood_Dead.png", var1);
      this.Zombie = Texture.getSharedTexture("media/ui/Moodles/Mood_Zombified.png", var1);
      this.NoxiousSmell = Texture.getSharedTexture("media/ui/Moodles/Mood_NoxiousSmell.png", var1);
      this.FoodEaten = Texture.getSharedTexture("media/ui/Moodles/Status_Hunger.png", var1);
      this.Hyperthermia = Texture.getSharedTexture("media/ui/Moodles/Status_TemperatureHot.png", var1);
      this.Hypothermia = Texture.getSharedTexture("media/ui/Moodles/Status_TemperatureLow.png", var1);
      this.Windchill = Texture.getSharedTexture("media/ui/Moodles/Status_Windchill.png", var1);
      this.CantSprint = Texture.getSharedTexture("media/ui/Moodles/Status_MovementRestricted.png", var1);
      this.Uncomfortable = Texture.getSharedTexture("media/ui/Moodles/Mood_Discomfort.png", var1);
      plusRed = Texture.getSharedTexture("media/ui/Moodle_internal_plus_red.png", var1);
      minusRed = Texture.getSharedTexture("media/ui/Moodle_internal_minus_red.png", var1);
      plusGreen = Texture.getSharedTexture("media/ui/Moodle_internal_plus_green.png", var1);
      minusGreen = Texture.getSharedTexture("media/ui/Moodle_internal_minus_green.png", var1);

      for(int var2 = 0; var2 < MoodleType.ToIndex(MoodleType.MAX); ++var2) {
         this.MoodleSlotsPos[var2] = 10000.0F;
         this.MoodleSlotsDesiredPos[var2] = 10000.0F;
      }

      this.clientW = this.width;
      this.clientH = this.height;
      instance = this;
   }

   public boolean CurrentlyAnimating() {
      boolean var1 = false;

      for(int var2 = 0; var2 < MoodleType.ToIndex(MoodleType.MAX); ++var2) {
         if (this.MoodleSlotsPos[var2] != this.MoodleSlotsDesiredPos[var2]) {
            var1 = true;
         }
      }

      return var1;
   }

   public void Nest(UIElement var1, int var2, int var3, int var4, int var5) {
      this.AddChild(var1);
      this.nestedItems.add(new Rectangle(var5, var2, var3, var4));
   }

   public Boolean onMouseMove(double var1, double var3) {
      if (!this.isVisible()) {
         return Boolean.FALSE;
      } else {
         this.MouseOver = true;
         super.onMouseMove(var1, var3);
         this.MouseOverSlot = (int)(((double)((float)Mouse.getYA()) - this.getY()) / (double)this.MoodleDistY);
         if (this.MouseOverSlot >= this.NumUsedSlots) {
            this.MouseOverSlot = 1000;
         }

         return Boolean.TRUE;
      }
   }

   public void onMouseMoveOutside(double var1, double var3) {
      super.onMouseMoveOutside(var1, var3);
      this.MouseOverSlot = 1000;
      this.MouseOver = false;
   }

   public void render() {
      switch (Core.getInstance().getOptionMoodleSize()) {
         case 1:
            this.width = 32.0F;
            break;
         case 2:
            this.width = 48.0F;
            break;
         case 3:
            this.width = 64.0F;
            break;
         case 4:
            this.width = 80.0F;
            break;
         case 5:
            this.width = 96.0F;
            break;
         case 6:
            this.width = 128.0F;
            break;
         case 7:
            this.width = (float)(TextManager.instance.font.getLineHeight() * 3);
      }

      if (this.UseCharacter != null) {
         if (this.MoodleDistY != 10.0F + this.width) {
            this.UseCharacter.getMoodles().setMoodlesStateChanged(true);
            this.update();
         }

         float var1 = (float)(UIManager.getMillisSinceLastRender() / 33.3);
         this.OscilatorStep += this.OscilatorRate * var1 * 0.5F;
         this.Oscilator = (float)Math.sin((double)this.OscilatorStep);
         int var2 = 0;

         for(int var3 = 0; var3 < MoodleType.ToIndex(MoodleType.MAX); ++var3) {
            if (this.MoodleSlotsPos[var3] != 10000.0F) {
               float var4 = this.Oscilator * this.OscilatorScalar * this.MoodleOscilationLevel[var3];
               Texture var5 = this.Tired;
               Color var6 = new Color(Color.gray);
               switch (this.GoodBadNeutral[var3]) {
                  case 0:
                     var6 = new Color(Color.gray);
                     break;
                  case 1:
                     var6 = Color.abgrToColor(Color.lerpABGR(Color.colorToABGR(new Color(Color.gray)), Color.colorToABGR(Core.getInstance().getGoodHighlitedColor().toColor()), (float)this.MoodleLevel[var3] / 4.0F), var6);
                     break;
                  case 2:
                     var6 = Color.abgrToColor(Color.lerpABGR(Color.colorToABGR(new Color(Color.gray)), Color.colorToABGR(Core.getInstance().getBadHighlitedColor().toColor()), (float)this.MoodleLevel[var3] / 4.0F), var6);
               }

               switch (var3) {
                  case 0:
                     var5 = this.Endurance;
                     break;
                  case 1:
                     var5 = this.Tired;
                     break;
                  case 2:
                     var5 = this.Hungry;
                     break;
                  case 3:
                     var5 = this.Panic;
                     break;
                  case 4:
                     var5 = this.Sick;
                     break;
                  case 5:
                     var5 = this.Bored;
                     break;
                  case 6:
                     var5 = this.Unhappy;
                     break;
                  case 7:
                     var5 = this.Bleeding;
                     break;
                  case 8:
                     var5 = this.Wet;
                     break;
                  case 9:
                     var5 = this.HasACold;
                     break;
                  case 10:
                     var5 = this.Angry;
                     break;
                  case 11:
                     var5 = this.Stress;
                     break;
                  case 12:
                     var5 = this.Thirst;
                     break;
                  case 13:
                     var5 = this.Injured;
                     break;
                  case 14:
                     var5 = this.Pain;
                     break;
                  case 15:
                     var5 = this.HeavyLoad;
                     break;
                  case 16:
                     var5 = this.Drunk;
                     break;
                  case 17:
                     var5 = this.Dead;
                     break;
                  case 18:
                     var5 = this.Zombie;
                     break;
                  case 19:
                     var5 = this.FoodEaten;
                     break;
                  case 20:
                     var5 = this.Hyperthermia;
                     break;
                  case 21:
                     var5 = this.Hypothermia;
                     break;
                  case 22:
                     var5 = this.Windchill;
                     break;
                  case 23:
                     var5 = this.CantSprint;
                     break;
                  case 24:
                     var5 = this.Uncomfortable;
                     break;
                  case 25:
                     var5 = this.NoxiousSmell;
               }

               if (MoodleType.FromIndex(var3).name().equals(Core.getInstance().getBlinkingMoodle())) {
                  if (this.alphaIncrease) {
                     this.alpha += 0.1F * (30.0F / (float)PerformanceSettings.instance.getUIRenderFPS());
                     if (this.alpha > 1.0F) {
                        this.alpha = 1.0F;
                        this.alphaIncrease = false;
                     }
                  } else {
                     this.alpha -= 0.1F * (30.0F / (float)PerformanceSettings.instance.getUIRenderFPS());
                     if (this.alpha < 0.0F) {
                        this.alpha = 0.0F;
                        this.alphaIncrease = true;
                     }
                  }
               }

               if (Core.getInstance().getBlinkingMoodle() == null) {
                  this.alpha = 1.0F;
               }

               short var7 = 9985;
               short var8 = 9729;
               this.Background.getTextureId().setMinFilter(var7);
               this.Background.getTextureId().setMagFilter(var8);
               this.Border.getTextureId().setMinFilter(var7);
               this.Border.getTextureId().setMagFilter(var8);
               this.DrawTextureScaledCol(this.Background, (double)((int)var4), (double)((int)this.MoodleSlotsPos[var3]), (double)this.width, (double)this.width, var6);
               this.DrawTextureScaled(this.Border, (double)((int)var4), (double)((int)this.MoodleSlotsPos[var3]), (double)this.width, (double)this.width, (double)this.alpha);
               float var9 = this.width;
               double var10 = Math.ceil((double)((this.width - var9) / 2.0F));
               var5.getTextureId().setMinFilter(var7);
               var5.getTextureId().setMagFilter(var8);
               this.DrawTextureScaled(var5, (double)((int)((double)var4 + var10)), (double)((int)((double)this.MoodleSlotsPos[var3] + var10)), (double)var9, (double)var9, (double)this.alpha);
               if (this.MouseOver && var2 == this.MouseOverSlot) {
                  String var12 = this.UseCharacter.getMoodles().getMoodleDisplayString(var3);
                  String var13 = this.UseCharacter.getMoodles().getMoodleDescriptionString(var3);
                  int var14 = TextManager.instance.font.getWidth(var12);
                  int var15 = TextManager.instance.font.getWidth(var13);
                  int var16 = Math.max(var14, var15);
                  int var17 = TextManager.instance.font.getLineHeight();
                  int var18 = (int)this.MoodleSlotsPos[var3] + 1;
                  int var19 = (2 + var17) * 2;
                  if (this.width > (float)var19) {
                     var18 = (int)((float)var18 + (this.width - (float)var19) / 2.0F);
                  }

                  this.DrawTextureScaledColor((Texture)null, -10.0 - (double)var16 - 6.0, (double)var18 - 2.0, (double)var16 + 12.0, (double)var19, 0.0, 0.0, 0.0, 0.6);
                  this.DrawTextRight(var12, -10.0, (double)var18, 1.0, 1.0, 1.0, 1.0);
                  this.DrawTextRight(var13, -10.0, (double)(var18 + var17), 0.800000011920929, 0.800000011920929, 0.800000011920929, 1.0);
               }

               ++var2;
            }
         }

         super.render();
      }
   }

   public void wiggle(MoodleType var1) {
      this.MoodleOscilationLevel[MoodleType.ToIndex(var1)] = this.OscilatorStartLevel;
   }

   public void update() {
      this.MoodleDistY = 10.0F + this.width;
      super.update();
      if (this.UseCharacter != null) {
         if (!this.CurrentlyAnimating()) {
            if (this.DebugKeyDelay > 0) {
               --this.DebugKeyDelay;
            } else if (GameKeyboard.isKeyDown(57)) {
               this.DebugKeyDelay = 10;
            }
         }

         float var1 = (float)PerformanceSettings.getLockFPS() / 30.0F;

         float[] var10000;
         int var2;
         for(var2 = 0; var2 < MoodleType.ToIndex(MoodleType.MAX); ++var2) {
            var10000 = this.MoodleOscilationLevel;
            var10000[var2] -= this.MoodleOscilationLevel[var2] * (1.0F - this.OscilatorDecelerator) / var1;
            if ((double)this.MoodleOscilationLevel[var2] < 0.01) {
               this.MoodleOscilationLevel[var2] = 0.0F;
            }
         }

         if (this.UseCharacter.getMoodles().UI_RefreshNeeded()) {
            var2 = 0;

            for(int var3 = 0; var3 < MoodleType.ToIndex(MoodleType.MAX); ++var3) {
               if (MoodleType.FromIndex(var3) == MoodleType.FoodEaten && this.UseCharacter.getMoodles().getMoodleLevel(var3) < 3) {
                  this.MoodleSlotsPos[var3] = 10000.0F;
                  this.MoodleSlotsDesiredPos[var3] = 10000.0F;
                  this.MoodleOscilationLevel[var3] = 0.0F;
               } else if (this.UseCharacter.getMoodles().getMoodleLevel(var3) > 0) {
                  boolean var4 = false;
                  if (this.MoodleLevel[var3] != this.UseCharacter.getMoodles().getMoodleLevel(var3)) {
                     var4 = true;
                     this.MoodleLevel[var3] = this.UseCharacter.getMoodles().getMoodleLevel(var3);
                     this.MoodleOscilationLevel[var3] = this.OscilatorStartLevel;
                  }

                  this.MoodleSlotsDesiredPos[var3] = this.MoodleDistY * (float)var2;
                  if (var4) {
                     if (this.MoodleSlotsPos[var3] == 10000.0F) {
                        this.MoodleSlotsPos[var3] = this.MoodleSlotsDesiredPos[var3] + 500.0F;
                        this.MoodleOscilationLevel[var3] = 0.0F;
                     }

                     this.GoodBadNeutral[var3] = this.UseCharacter.getMoodles().getGoodBadNeutral(var3);
                  } else {
                     this.MoodleOscilationLevel[var3] = 0.0F;
                  }

                  this.MoodleTypeInSlot[var2] = var3;
                  ++var2;
               } else {
                  this.MoodleSlotsPos[var3] = 10000.0F;
                  this.MoodleSlotsDesiredPos[var3] = 10000.0F;
                  this.MoodleOscilationLevel[var3] = 0.0F;
                  this.MoodleLevel[var3] = 0;
               }
            }

            this.NumUsedSlots = var2;
         }

         for(var2 = 0; var2 < MoodleType.ToIndex(MoodleType.MAX); ++var2) {
            if (Math.abs(this.MoodleSlotsPos[var2] - this.MoodleSlotsDesiredPos[var2]) > 0.8F) {
               var10000 = this.MoodleSlotsPos;
               var10000[var2] += (this.MoodleSlotsDesiredPos[var2] - this.MoodleSlotsPos[var2]) * 0.15F;
            } else {
               this.MoodleSlotsPos[var2] = this.MoodleSlotsDesiredPos[var2];
            }
         }

      }
   }

   public void setCharacter(IsoGameCharacter var1) {
      if (var1 != this.UseCharacter) {
         this.UseCharacter = var1;
         if (this.UseCharacter != null && this.UseCharacter.getMoodles() != null) {
            this.UseCharacter.getMoodles().setMoodlesStateChanged(true);
         }

      }
   }

   public static MoodlesUI getInstance() {
      return instance;
   }
}
