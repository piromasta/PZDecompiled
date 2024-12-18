package zombie.debug.debugWindows;

import imgui.ImColor;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.extension.implot.ImPlot;
import java.util.Arrays;
import zombie.CombatManager;
import zombie.GameTime;
import zombie.characters.IsoPlayer;
import zombie.characters.skills.PerkFactory;
import zombie.core.math.PZMath;
import zombie.debug.BaseDebugWindow;
import zombie.debug.LineDrawer;
import zombie.inventory.types.HandWeapon;

public class AimPlotter extends BaseDebugWindow {
   private static final float PointBlankDistance = 3.0F;
   final ImVec2[] vec2 = new ImVec2[]{new ImVec2(), new ImVec2()};
   int MAX_RECORDS = 1000;
   Float[] AimPenalty;
   Float[] MovePenalty;
   Float[] RecoilDelay;
   Float[] AttackAnim;
   Float[] Racking;
   Float[] time;
   boolean showAimDelay;
   boolean showMovePenalty;
   boolean showRecoilDelay;
   boolean showAttack;
   boolean showRack;
   float RANGE_SCALE_MAX;
   float RANGE_SCALE_MIN;
   boolean proneTarget;
   boolean showCurrent;
   boolean showMoodles;
   boolean showWeather;
   float lightLevel;
   float scale;

   public String getTitle() {
      return "Aim Debugger";
   }

   public AimPlotter() {
      this.AimPenalty = new Float[this.MAX_RECORDS];
      this.MovePenalty = new Float[this.MAX_RECORDS];
      this.RecoilDelay = new Float[this.MAX_RECORDS];
      this.AttackAnim = new Float[this.MAX_RECORDS];
      this.Racking = new Float[this.MAX_RECORDS];
      this.time = new Float[this.MAX_RECORDS];
      this.showAimDelay = true;
      this.showMovePenalty = true;
      this.showRecoilDelay = true;
      this.showAttack = true;
      this.showRack = true;
      this.RANGE_SCALE_MAX = 100.0F;
      this.RANGE_SCALE_MIN = -100.0F;
      this.proneTarget = false;
      this.showCurrent = false;
      this.showMoodles = true;
      this.showWeather = true;
      this.lightLevel = 1.0F;
      this.scale = 10.0F;

      for(int var1 = this.MAX_RECORDS - 1; var1 >= 0; --var1) {
         this.time[var1] = (float)var1;
      }

      Arrays.fill(this.AimPenalty, 0.0F);
      Arrays.fill(this.MovePenalty, 0.0F);
      Arrays.fill(this.RecoilDelay, 0.0F);
      Arrays.fill(this.AttackAnim, 0.0F);
      Arrays.fill(this.Racking, 0.0F);
   }

   public void update() {
      this.shiftLeft(this.AimPenalty);
      this.shiftLeft(this.MovePenalty);
      this.shiftLeft(this.RecoilDelay);
      this.shiftLeft(this.AttackAnim);
      this.shiftLeft(this.Racking);
      IsoPlayer var1 = IsoPlayer.getInstance();
      if (var1 != null) {
         this.AimPenalty[0] = PZMath.max(0.0F, var1.getAimingDelay());
         this.MovePenalty[0] = PZMath.max(0.0F, var1.getBeenMovingFor() * 0.5F - ((float)var1.getPerkLevel(PerkFactory.Perks.Aiming) * 1.5F + (float)var1.getPerkLevel(PerkFactory.Perks.Nimble) * 1.0F));
         this.RecoilDelay[0] = var1.getRecoilDelay();
         this.AttackAnim[0] = var1.isPerformingAttackAnimation() ? 50.0F : 0.0F;
         this.Racking[0] = var1.getVariableBoolean("isracking") ? 50.0F : 0.0F;
      } else {
         this.AimPenalty[0] = 0.0F;
         this.MovePenalty[0] = 0.0F;
         this.RecoilDelay[0] = 0.0F;
         this.AttackAnim[0] = 0.0F;
         this.Racking[0] = 0.0F;
      }

   }

   protected void doWindowContents() {
      if (!GameTime.isGamePaused()) {
         this.update();
      }

      if (ImGui.beginTabBar("tabSelector")) {
         if (ImGui.beginTabItem("State")) {
            this.showAimDelay = Wrappers.checkbox("Aiming Delay", this.showAimDelay);
            ImGui.sameLine();
            this.showRecoilDelay = Wrappers.checkbox("Recoil Delay", this.showRecoilDelay);
            ImGui.sameLine();
            this.showMovePenalty = Wrappers.checkbox("Movement Penalty", this.showMovePenalty);
            ImGui.sameLine();
            this.showAttack = Wrappers.checkbox("Attack Anim", this.showAttack);
            ImGui.sameLine();
            this.showRack = Wrappers.checkbox("Racking", this.showRack);
            ImPlot.setNextPlotLimits(0.0, (double)this.MAX_RECORDS, 0.0, 100.0, 1);
            if (ImPlot.beginPlot("Aiming Variables", "ticks (" + this.MAX_RECORDS + ")", "Value")) {
               if (this.showAimDelay) {
                  ImPlot.plotLine("Aiming Delay", this.time, this.AimPenalty);
               }

               if (this.showRecoilDelay) {
                  ImPlot.plotLine("Recoil Delay", this.time, this.RecoilDelay);
               }

               if (this.showMovePenalty) {
                  ImPlot.plotLine("Movement Penalty", this.time, this.MovePenalty);
               }

               if (this.showAttack) {
                  ImPlot.plotLine("Attack Anim", this.time, this.AttackAnim);
               }

               if (this.showRack) {
                  ImPlot.plotLine("Racking", this.time, this.Racking);
               }

               ImPlot.endPlot();
            }

            ImGui.endTabItem();
         }

         if (ImGui.beginTabItem("Range")) {
            if (IsoPlayer.getInstance() != null) {
               this.doRangePlot(IsoPlayer.getInstance(), IsoPlayer.getInstance().getUseHandWeapon());
            }

            ImGui.endTabItem();
         }

         ImGui.endTabBar();
      }

   }

   void doRangePlot(IsoPlayer var1, HandWeapon var2) {
      this.showCurrent = Wrappers.checkbox("Current Values", this.showCurrent);
      ImGui.sameLine();
      ImGui.beginDisabled(!this.showCurrent);
      this.showMoodles = Wrappers.checkbox("Moodles", this.showMoodles);
      ImGui.sameLine();
      this.showWeather = Wrappers.checkbox("Weather", this.showWeather);
      ImGui.endDisabled();
      if (this.showCurrent) {
         this.RANGE_SCALE_MAX = 200.0F;
         this.RANGE_SCALE_MIN = 0.0F;
      } else {
         this.RANGE_SCALE_MAX = 100.0F;
         this.RANGE_SCALE_MIN = -100.0F;
      }

      if (var1 != null && var2 != null) {
         int var3 = ((int)Math.floor((double)var2.getMaxRange()) + 3) * 10;
         Float[] var4 = new Float[var3];
         Float[] var5 = new Float[var3];
         Float[] var6 = new Float[var3];
         ImPlot.setNextPlotLimits(0.0, (double)var2.getMaxRange(), (double)this.RANGE_SCALE_MIN, (double)this.RANGE_SCALE_MAX, 1);
         if (ImPlot.beginPlot("Range vs HitChance", "Distance", "Chance")) {
            float var7 = var2.getMaxSightRange(var1);
            float var8 = var2.getMinSightRange(var1);
            if (this.showCurrent) {
               this.vec2[0] = ImPlot.plotToPixels(0.0, 100.0, 0);
               this.vec2[1] = ImPlot.plotToPixels((double)var2.getMaxRange(), 100.0, 0);
               ImPlot.getPlotDrawList().addLine(this.vec2[0].x, this.vec2[0].y, this.vec2[1].x, this.vec2[1].y, ImColor.rgb(0, 200, 0));
            } else {
               this.vec2[0] = ImPlot.plotToPixels(0.0, 0.0, 0);
               this.vec2[1] = ImPlot.plotToPixels((double)var2.getMaxRange(), 0.0, 0);
               ImPlot.getPlotDrawList().addLine(this.vec2[0].x, this.vec2[0].y, this.vec2[1].x, this.vec2[1].y, ImColor.rgb(200, 0, 0));
            }

            this.vec2[0] = ImPlot.plotToPixels(3.0, (double)this.RANGE_SCALE_MIN, 0);
            this.vec2[1] = ImPlot.plotToPixels(3.0, (double)this.RANGE_SCALE_MAX, 0);
            ImPlot.getPlotDrawList().addLine(this.vec2[0].x, this.vec2[0].y, this.vec2[1].x, this.vec2[1].y, ImColor.rgb(0, 150, 150));
            LineDrawer.DrawIsoCircle(var1.getX(), var1.getY(), var1.getZ(), 3.0F, 32, 0.0F, 0.75F, 0.75F, 0.3F);
            this.vec2[0] = ImPlot.plotToPixels((double)(var8 + (var7 - var8) * 0.5F), (double)this.RANGE_SCALE_MIN, 0);
            this.vec2[1] = ImPlot.plotToPixels((double)(var8 + (var7 - var8) * 0.5F), (double)this.RANGE_SCALE_MAX, 0);
            ImPlot.getPlotDrawList().addLine(this.vec2[0].x, this.vec2[0].y, this.vec2[1].x, this.vec2[1].y, ImColor.rgb(0, 200, 0));
            LineDrawer.DrawIsoCircle(var1.getX(), var1.getY(), var1.getZ(), var8 + (var7 - var8) * 0.5F, 32, 0.0F, 1.0F, 0.0F, 0.3F);
            this.vec2[0] = ImPlot.plotToPixels((double)var8, (double)this.RANGE_SCALE_MIN, 0);
            this.vec2[1] = ImPlot.plotToPixels((double)var8, (double)this.RANGE_SCALE_MAX, 0);
            ImPlot.getPlotDrawList().addLine(this.vec2[0].x, this.vec2[0].y, this.vec2[1].x, this.vec2[1].y, ImColor.rgb(2000, 200, 0));
            LineDrawer.DrawIsoCircle(var1.getX(), var1.getY(), var1.getZ(), var8, 32, 1.0F, 1.0F, 0.0F, 0.3F);
            this.vec2[0] = ImPlot.plotToPixels((double)var7, (double)this.RANGE_SCALE_MIN, 0);
            this.vec2[1] = ImPlot.plotToPixels((double)var7, (double)this.RANGE_SCALE_MAX, 0);
            ImPlot.getPlotDrawList().addLine(this.vec2[0].x, this.vec2[0].y, this.vec2[1].x, this.vec2[1].y, ImColor.rgb(200, 200, 0));
            LineDrawer.DrawIsoCircle(var1.getX(), var1.getY(), var1.getZ(), var7, 32, 1.0F, 1.0F, 0.0F, 0.3F);
            LineDrawer.DrawIsoCircle(var1.getX(), var1.getY(), var1.getZ(), var2.getMaxRange(), 32, 1.0F, 0.0F, 0.0F, 0.3F);
            float var9 = 0.0F;
            float var10 = 0.0F;
            float var11 = 0.0F;
            if (this.showCurrent) {
               var9 = (float)var2.getHitChance();
               var10 = var2.getCriticalChance();
               if (var9 > 95.0F) {
                  var9 = 95.0F;
               }

               var9 += var2.getAimingPerkHitChanceModifier() * (float)var1.getPerkLevel(PerkFactory.Perks.Aiming);
               var10 += (float)(var2.getAimingPerkCritModifier() * var1.getPerkLevel(PerkFactory.Perks.Aiming));
               var9 -= 100.0F - 100.0F / var1.getWornItemsVisionModifier();
            }

            for(int var12 = 0; var12 < var3; ++var12) {
               float var13 = (float)var12 / this.scale;
               float var14 = var9;
               float var15 = var10;
               if (var13 <= var2.getMaxRange()) {
                  var14 = var9 + PZMath.max(CombatManager.getDistanceModifierSightless(var13, this.proneTarget) - (this.showCurrent ? CombatManager.getAimDelayPenaltySightless(PZMath.max(0.0F, var1.getAimingDelay()), var13) : 0.0F), CombatManager.getDistanceModifier(var13, var8, var7, this.proneTarget) - (this.showCurrent ? CombatManager.getAimDelayPenalty(PZMath.max(0.0F, var1.getAimingDelay()), var13, var8, var7) : 0.0F));
                  var15 = var10 + PZMath.max(CombatManager.getDistanceModifierSightless(var13, this.proneTarget) - (this.showCurrent ? CombatManager.getAimDelayPenaltySightless(PZMath.max(0.0F, var1.getAimingDelay()), var13) : 0.0F), CombatManager.getDistanceModifier(var13, var8, var7, this.proneTarget) - (this.showCurrent ? CombatManager.getAimDelayPenalty(PZMath.max(0.0F, var1.getAimingDelay()), var13, var8, var7) : 0.0F));
               }

               if (this.showCurrent) {
                  var14 -= CombatManager.getMovePenalty(var1, var13);
                  var15 -= CombatManager.getMovePenalty(var1, var13);
                  if (this.showWeather) {
                     var14 -= CombatManager.getWeatherPenalty(var1, var2, var1.getSquare(), var13);
                     var15 -= CombatManager.getWeatherPenalty(var1, var2, var1.getSquare(), var13);
                  }

                  if (this.showMoodles) {
                     var14 -= CombatManager.getMoodlesPenalty(var1, var13);
                     var15 -= CombatManager.getMoodlesPenalty(var1, var13);
                  }
               }

               var6[var12] = var15;
               var5[var12] = var13;
               var4[var12] = var14;
               if (this.showCurrent && var12 > 0 && (var14 <= 0.0F && var4[var12 - 1] > 0.0F || var14 >= 0.0F && var4[var12 - 1] < 0.0F)) {
                  this.vec2[0] = ImPlot.plotToPixels((double)var13, (double)this.RANGE_SCALE_MIN, 0);
                  this.vec2[1] = ImPlot.plotToPixels((double)var13, (double)this.RANGE_SCALE_MAX, 0);
                  ImPlot.getPlotDrawList().addLine(this.vec2[0].x, this.vec2[0].y, this.vec2[1].x, this.vec2[1].y, ImColor.rgb(200, 100, 0));
                  LineDrawer.DrawIsoCircle(var1.getX(), var1.getY(), var1.getZ(), var13, 32, 1.0F, 0.5F, 0.0F, 0.3F);
               }
            }

            ImPlot.plotLine("HitChance", var5, var4);
            ImPlot.plotLine("Critical", var5, var6);
            ImPlot.endPlot();
         }

      }
   }

   <T> void shiftLeft(T[] var1) {
      for(int var2 = var1.length - 1; var2 > 0; --var2) {
         var1[var2] = var1[var2 - 1];
      }

   }

   <T> void shiftRight(T[] var1) {
      for(int var2 = 0; var2 < var1.length - 1; ++var2) {
         var1[var2] = var1[var2 + 1];
      }

   }
}
