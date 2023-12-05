package zombie;

import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaManager;
import zombie.debug.DebugLog;

public final class ZomboidGlobals {
   public static double RunningEnduranceReduce = 0.0;
   public static double SprintingEnduranceReduce = 0.0;
   public static double ImobileEnduranceReduce = 0.0;
   public static double SittingEnduranceMultiplier = 5.0;
   public static double ThirstIncrease = 0.0;
   public static double ThirstSleepingIncrease = 0.0;
   public static double ThirstLevelToAutoDrink = 0.0;
   public static double ThirstLevelReductionOnAutoDrink = 0.0;
   public static double HungerIncrease = 0.0;
   public static double HungerIncreaseWhenWellFed = 0.0;
   public static double HungerIncreaseWhileAsleep = 0.0;
   public static double HungerIncreaseWhenExercise = 0.0;
   public static double FatigueIncrease = 0.0;
   public static double StressReduction = 0.0;
   public static double BoredomIncreaseRate = 0.0;
   public static double BoredomDecreaseRate = 0.0;
   public static double UnhappinessIncrease = 0.0;
   public static double StressFromSoundsMultiplier = 0.0;
   public static double StressFromBiteOrScratch = 0.0;
   public static double StressFromHemophobic = 0.0;
   public static double AngerDecrease = 0.0;
   public static double BroodingAngerDecreaseMultiplier = 0.0;
   public static double SleepFatigueReduction = 0.0;
   public static double WetnessIncrease = 0.0;
   public static double WetnessDecrease = 0.0;
   public static double CatchAColdIncreaseRate = 0.0;
   public static double CatchAColdDecreaseRate = 0.0;
   public static double PoisonLevelDecrease = 0.0;
   public static double PoisonHealthReduction = 0.0;
   public static double FoodSicknessDecrease = 0.0;

   public ZomboidGlobals() {
   }

   public static void Load() {
      KahluaTable var0 = (KahluaTable)LuaManager.env.rawget("ZomboidGlobals");
      SprintingEnduranceReduce = (Double)var0.rawget("SprintingEnduranceReduce");
      RunningEnduranceReduce = (Double)var0.rawget("RunningEnduranceReduce");
      ImobileEnduranceReduce = (Double)var0.rawget("ImobileEnduranceIncrease");
      ThirstIncrease = (Double)var0.rawget("ThirstIncrease");
      ThirstSleepingIncrease = (Double)var0.rawget("ThirstSleepingIncrease");
      ThirstLevelToAutoDrink = (Double)var0.rawget("ThirstLevelToAutoDrink");
      ThirstLevelReductionOnAutoDrink = (Double)var0.rawget("ThirstLevelReductionOnAutoDrink");
      HungerIncrease = (Double)var0.rawget("HungerIncrease");
      HungerIncreaseWhenWellFed = (Double)var0.rawget("HungerIncreaseWhenWellFed");
      HungerIncreaseWhileAsleep = (Double)var0.rawget("HungerIncreaseWhileAsleep");
      HungerIncreaseWhenExercise = (Double)var0.rawget("HungerIncreaseWhenExercise");
      FatigueIncrease = (Double)var0.rawget("FatigueIncrease");
      StressReduction = (Double)var0.rawget("StressDecrease");
      BoredomIncreaseRate = (Double)var0.rawget("BoredomIncrease");
      BoredomDecreaseRate = (Double)var0.rawget("BoredomDecrease");
      UnhappinessIncrease = (Double)var0.rawget("UnhappinessIncrease");
      StressFromSoundsMultiplier = (Double)var0.rawget("StressFromSoundsMultiplier");
      StressFromBiteOrScratch = (Double)var0.rawget("StressFromBiteOrScratch");
      StressFromHemophobic = (Double)var0.rawget("StressFromHemophobic");
      AngerDecrease = (Double)var0.rawget("AngerDecrease");
      BroodingAngerDecreaseMultiplier = (Double)var0.rawget("BroodingAngerDecreaseMultiplier");
      SleepFatigueReduction = (Double)var0.rawget("SleepFatigueReduction");
      WetnessIncrease = (Double)var0.rawget("WetnessIncrease");
      WetnessDecrease = (Double)var0.rawget("WetnessDecrease");
      CatchAColdIncreaseRate = (Double)var0.rawget("CatchAColdIncreaseRate");
      CatchAColdDecreaseRate = (Double)var0.rawget("CatchAColdDecreaseRate");
      PoisonLevelDecrease = (Double)var0.rawget("PoisonLevelDecrease");
      PoisonHealthReduction = (Double)var0.rawget("PoisonHealthReduction");
      FoodSicknessDecrease = (Double)var0.rawget("FoodSicknessDecrease");
   }

   public static void toLua() {
      KahluaTable var0 = (KahluaTable)LuaManager.env.rawget("ZomboidGlobals");
      if (var0 == null) {
         DebugLog.log("ERROR: ZomboidGlobals table undefined in Lua");
      } else {
         double var1 = 1.0;
         if (SandboxOptions.instance.getFoodLootModifier() == 1) {
            var1 = 0.0;
         } else if (SandboxOptions.instance.getFoodLootModifier() == 2) {
            var1 = 0.05;
         } else if (SandboxOptions.instance.getFoodLootModifier() == 3) {
            var1 = 0.2;
         } else if (SandboxOptions.instance.getFoodLootModifier() == 4) {
            var1 = 0.6;
         } else if (SandboxOptions.instance.getFoodLootModifier() == 5) {
            var1 = 1.0;
         } else if (SandboxOptions.instance.getFoodLootModifier() == 6) {
            var1 = 2.0;
         } else if (SandboxOptions.instance.getFoodLootModifier() == 7) {
            var1 = 4.0;
         }

         var0.rawset("FoodLootModifier", var1);
         double var3 = 1.0;
         if (SandboxOptions.instance.getWeaponLootModifier() == 1) {
            var3 = 0.0;
         } else if (SandboxOptions.instance.getWeaponLootModifier() == 2) {
            var3 = 0.05;
         } else if (SandboxOptions.instance.getWeaponLootModifier() == 3) {
            var3 = 0.2;
         } else if (SandboxOptions.instance.getWeaponLootModifier() == 4) {
            var3 = 0.6;
         } else if (SandboxOptions.instance.getWeaponLootModifier() == 5) {
            var3 = 1.0;
         } else if (SandboxOptions.instance.getWeaponLootModifier() == 6) {
            var3 = 2.0;
         } else if (SandboxOptions.instance.getWeaponLootModifier() == 7) {
            var3 = 4.0;
         }

         var0.rawset("WeaponLootModifier", var3);
         double var5 = 1.0;
         if (SandboxOptions.instance.getOtherLootModifier() == 1) {
            var5 = 0.0;
         } else if (SandboxOptions.instance.getOtherLootModifier() == 2) {
            var5 = 0.05;
         } else if (SandboxOptions.instance.getOtherLootModifier() == 3) {
            var5 = 0.2;
         } else if (SandboxOptions.instance.getOtherLootModifier() == 4) {
            var5 = 0.6;
         } else if (SandboxOptions.instance.getOtherLootModifier() == 5) {
            var5 = 1.0;
         } else if (SandboxOptions.instance.getOtherLootModifier() == 6) {
            var5 = 2.0;
         } else if (SandboxOptions.instance.getOtherLootModifier() == 7) {
            var5 = 4.0;
         }

         var0.rawset("OtherLootModifier", var5);
      }
   }
}
