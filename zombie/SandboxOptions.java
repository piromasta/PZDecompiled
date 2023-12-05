package zombie;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.Lua.LuaManager;
import zombie.config.BooleanConfigOption;
import zombie.config.ConfigFile;
import zombie.config.ConfigOption;
import zombie.config.DoubleConfigOption;
import zombie.config.EnumConfigOption;
import zombie.config.IntegerConfigOption;
import zombie.config.StringConfigOption;
import zombie.core.Core;
import zombie.core.Rand;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.debug.DebugLog;
import zombie.iso.SliceY;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerSettingsManager;
import zombie.sandbox.CustomBooleanSandboxOption;
import zombie.sandbox.CustomDoubleSandboxOption;
import zombie.sandbox.CustomEnumSandboxOption;
import zombie.sandbox.CustomIntegerSandboxOption;
import zombie.sandbox.CustomSandboxOption;
import zombie.sandbox.CustomSandboxOptions;
import zombie.sandbox.CustomStringSandboxOption;
import zombie.util.Type;

public final class SandboxOptions {
   public static final SandboxOptions instance = new SandboxOptions();
   public int Speed = 3;
   public final EnumSandboxOption Zombies = (EnumSandboxOption)this.newEnumOption("Zombies", 6, 4).setTranslation("ZombieCount");
   public final EnumSandboxOption Distribution = (EnumSandboxOption)this.newEnumOption("Distribution", 2, 1).setTranslation("ZombieDistribution");
   public final EnumSandboxOption DayLength = this.newEnumOption("DayLength", 26, 2);
   public final EnumSandboxOption StartYear = this.newEnumOption("StartYear", 100, 1);
   public final EnumSandboxOption StartMonth = this.newEnumOption("StartMonth", 12, 7);
   public final EnumSandboxOption StartDay = this.newEnumOption("StartDay", 31, 23);
   public final EnumSandboxOption StartTime = this.newEnumOption("StartTime", 9, 2);
   public final EnumSandboxOption WaterShut = this.newEnumOption("WaterShut", 8, 2).setValueTranslation("Shutoff");
   public final EnumSandboxOption ElecShut = this.newEnumOption("ElecShut", 8, 2).setValueTranslation("Shutoff");
   public final IntegerSandboxOption WaterShutModifier = (IntegerSandboxOption)this.newIntegerOption("WaterShutModifier", -1, 2147483647, 14).setTranslation("WaterShut");
   public final IntegerSandboxOption ElecShutModifier = (IntegerSandboxOption)this.newIntegerOption("ElecShutModifier", -1, 2147483647, 14).setTranslation("ElecShut");
   public final EnumSandboxOption FoodLoot = (EnumSandboxOption)this.newEnumOption("FoodLoot", 7, 4).setValueTranslation("Rarity").setTranslation("LootFood");
   public final EnumSandboxOption LiteratureLoot = (EnumSandboxOption)this.newEnumOption("LiteratureLoot", 7, 4).setValueTranslation("Rarity").setTranslation("LootLiterature");
   public final EnumSandboxOption MedicalLoot = (EnumSandboxOption)this.newEnumOption("MedicalLoot", 7, 4).setValueTranslation("Rarity").setTranslation("LootMedical");
   public final EnumSandboxOption SurvivalGearsLoot = (EnumSandboxOption)this.newEnumOption("SurvivalGearsLoot", 7, 4).setValueTranslation("Rarity").setTranslation("LootSurvivalGears");
   public final EnumSandboxOption CannedFoodLoot = (EnumSandboxOption)this.newEnumOption("CannedFoodLoot", 7, 4).setValueTranslation("Rarity").setTranslation("LootCannedFood");
   public final EnumSandboxOption WeaponLoot = (EnumSandboxOption)this.newEnumOption("WeaponLoot", 7, 4).setValueTranslation("Rarity").setTranslation("LootWeapon");
   public final EnumSandboxOption RangedWeaponLoot = (EnumSandboxOption)this.newEnumOption("RangedWeaponLoot", 7, 4).setValueTranslation("Rarity").setTranslation("LootRangedWeapon");
   public final EnumSandboxOption AmmoLoot = (EnumSandboxOption)this.newEnumOption("AmmoLoot", 7, 4).setValueTranslation("Rarity").setTranslation("LootAmmo");
   public final EnumSandboxOption MechanicsLoot = (EnumSandboxOption)this.newEnumOption("MechanicsLoot", 7, 4).setValueTranslation("Rarity").setTranslation("LootMechanics");
   public final EnumSandboxOption OtherLoot = (EnumSandboxOption)this.newEnumOption("OtherLoot", 7, 4).setValueTranslation("Rarity").setTranslation("LootOther");
   public final EnumSandboxOption Temperature = (EnumSandboxOption)this.newEnumOption("Temperature", 5, 3).setTranslation("WorldTemperature");
   public final EnumSandboxOption Rain = (EnumSandboxOption)this.newEnumOption("Rain", 5, 3).setTranslation("RainAmount");
   public final EnumSandboxOption ErosionSpeed = this.newEnumOption("ErosionSpeed", 5, 3);
   public final IntegerSandboxOption ErosionDays = this.newIntegerOption("ErosionDays", -1, 36500, 0);
   public final DoubleSandboxOption XpMultiplier = this.newDoubleOption("XpMultiplier", 0.001, 1000.0, 1.0);
   public final BooleanSandboxOption XpMultiplierAffectsPassive = this.newBooleanOption("XpMultiplierAffectsPassive", false);
   public final EnumSandboxOption Farming = (EnumSandboxOption)this.newEnumOption("Farming", 5, 3).setTranslation("FarmingSpeed");
   public final EnumSandboxOption CompostTime = this.newEnumOption("CompostTime", 8, 2);
   public final EnumSandboxOption StatsDecrease = (EnumSandboxOption)this.newEnumOption("StatsDecrease", 5, 3).setTranslation("StatDecrease");
   public final EnumSandboxOption NatureAbundance = (EnumSandboxOption)this.newEnumOption("NatureAbundance", 5, 3).setTranslation("NatureAmount");
   public final EnumSandboxOption Alarm = (EnumSandboxOption)this.newEnumOption("Alarm", 6, 4).setTranslation("HouseAlarmFrequency");
   public final EnumSandboxOption LockedHouses = (EnumSandboxOption)this.newEnumOption("LockedHouses", 6, 4).setTranslation("LockedHouseFrequency");
   public final BooleanSandboxOption StarterKit = this.newBooleanOption("StarterKit", false);
   public final BooleanSandboxOption Nutrition = this.newBooleanOption("Nutrition", false);
   public final EnumSandboxOption FoodRotSpeed = (EnumSandboxOption)this.newEnumOption("FoodRotSpeed", 5, 3).setTranslation("FoodSpoil");
   public final EnumSandboxOption FridgeFactor = (EnumSandboxOption)this.newEnumOption("FridgeFactor", 5, 3).setTranslation("FridgeEffect");
   public final EnumSandboxOption LootRespawn = this.newEnumOption("LootRespawn", 5, 1).setValueTranslation("Respawn");
   public final IntegerSandboxOption SeenHoursPreventLootRespawn = this.newIntegerOption("SeenHoursPreventLootRespawn", 0, 2147483647, 0);
   public final StringSandboxOption WorldItemRemovalList = this.newStringOption("WorldItemRemovalList", "Base.Hat,Base.Glasses", -1);
   public final DoubleSandboxOption HoursForWorldItemRemoval = this.newDoubleOption("HoursForWorldItemRemoval", 0.0, 2.147483647E9, 24.0);
   public final BooleanSandboxOption ItemRemovalListBlacklistToggle = this.newBooleanOption("ItemRemovalListBlacklistToggle", false);
   public final EnumSandboxOption TimeSinceApo = this.newEnumOption("TimeSinceApo", 13, 1);
   public final EnumSandboxOption PlantResilience = this.newEnumOption("PlantResilience", 5, 3);
   public final EnumSandboxOption PlantAbundance = this.newEnumOption("PlantAbundance", 5, 3).setValueTranslation("NatureAmount");
   public final EnumSandboxOption EndRegen = (EnumSandboxOption)this.newEnumOption("EndRegen", 5, 3).setTranslation("EnduranceRegen");
   public final EnumSandboxOption Helicopter = this.newEnumOption("Helicopter", 4, 2).setValueTranslation("HelicopterFreq");
   public final EnumSandboxOption MetaEvent = this.newEnumOption("MetaEvent", 3, 2).setValueTranslation("MetaEventFreq");
   public final EnumSandboxOption SleepingEvent = this.newEnumOption("SleepingEvent", 3, 1).setValueTranslation("MetaEventFreq");
   public final DoubleSandboxOption GeneratorFuelConsumption = this.newDoubleOption("GeneratorFuelConsumption", 0.0, 100.0, 1.0);
   public final EnumSandboxOption GeneratorSpawning = this.newEnumOption("GeneratorSpawning", 5, 3);
   public final EnumSandboxOption SurvivorHouseChance = this.newEnumOption("SurvivorHouseChance", 6, 3);
   public final EnumSandboxOption AnnotatedMapChance = this.newEnumOption("AnnotatedMapChance", 6, 4);
   public final IntegerSandboxOption CharacterFreePoints = this.newIntegerOption("CharacterFreePoints", -100, 100, 0);
   public final EnumSandboxOption ConstructionBonusPoints = this.newEnumOption("ConstructionBonusPoints", 5, 3);
   public final EnumSandboxOption NightDarkness = this.newEnumOption("NightDarkness", 4, 3);
   public final EnumSandboxOption NightLength = this.newEnumOption("NightLength", 5, 3);
   public final BooleanSandboxOption BoneFracture = this.newBooleanOption("BoneFracture", true);
   public final EnumSandboxOption InjurySeverity = this.newEnumOption("InjurySeverity", 3, 2);
   public final DoubleSandboxOption HoursForCorpseRemoval = this.newDoubleOption("HoursForCorpseRemoval", -1.0, 2.147483647E9, -1.0);
   public final EnumSandboxOption DecayingCorpseHealthImpact = this.newEnumOption("DecayingCorpseHealthImpact", 4, 3);
   public final EnumSandboxOption BloodLevel = this.newEnumOption("BloodLevel", 5, 3);
   public final EnumSandboxOption ClothingDegradation = this.newEnumOption("ClothingDegradation", 4, 3);
   public final BooleanSandboxOption FireSpread = this.newBooleanOption("FireSpread", true);
   public final IntegerSandboxOption DaysForRottenFoodRemoval = this.newIntegerOption("DaysForRottenFoodRemoval", -1, 2147483647, -1);
   public final BooleanSandboxOption AllowExteriorGenerator = this.newBooleanOption("AllowExteriorGenerator", true);
   public final EnumSandboxOption MaxFogIntensity = this.newEnumOption("MaxFogIntensity", 3, 1);
   public final EnumSandboxOption MaxRainFxIntensity = this.newEnumOption("MaxRainFxIntensity", 3, 1);
   public final BooleanSandboxOption EnableSnowOnGround = this.newBooleanOption("EnableSnowOnGround", true);
   public final BooleanSandboxOption AttackBlockMovements = this.newBooleanOption("AttackBlockMovements", true);
   public final EnumSandboxOption VehicleStoryChance = this.newEnumOption("VehicleStoryChance", 6, 3).setValueTranslation("SurvivorHouseChance");
   public final EnumSandboxOption ZoneStoryChance = this.newEnumOption("ZoneStoryChance", 6, 3).setValueTranslation("SurvivorHouseChance");
   public final BooleanSandboxOption AllClothesUnlocked = this.newBooleanOption("AllClothesUnlocked", false);
   public final BooleanSandboxOption EnableTaintedWaterText = this.newBooleanOption("EnableTaintedWaterText", true);
   public final BooleanSandboxOption EnableVehicles = this.newBooleanOption("EnableVehicles", true);
   public final EnumSandboxOption CarSpawnRate = this.newEnumOption("CarSpawnRate", 5, 4);
   public final DoubleSandboxOption ZombieAttractionMultiplier = this.newDoubleOption("ZombieAttractionMultiplier", 0.0, 100.0, 1.0);
   public final BooleanSandboxOption VehicleEasyUse = this.newBooleanOption("VehicleEasyUse", false);
   public final EnumSandboxOption InitialGas = this.newEnumOption("InitialGas", 6, 3);
   public final EnumSandboxOption FuelStationGas = this.newEnumOption("FuelStationGas", 9, 5);
   public final EnumSandboxOption LockedCar = this.newEnumOption("LockedCar", 6, 4);
   public final DoubleSandboxOption CarGasConsumption = this.newDoubleOption("CarGasConsumption", 0.0, 100.0, 1.0);
   public final EnumSandboxOption CarGeneralCondition = this.newEnumOption("CarGeneralCondition", 5, 3);
   public final EnumSandboxOption CarDamageOnImpact = this.newEnumOption("CarDamageOnImpact", 5, 3);
   public final EnumSandboxOption DamageToPlayerFromHitByACar = this.newEnumOption("DamageToPlayerFromHitByACar", 5, 1);
   public final BooleanSandboxOption TrafficJam = this.newBooleanOption("TrafficJam", true);
   public final EnumSandboxOption CarAlarm = (EnumSandboxOption)this.newEnumOption("CarAlarm", 6, 4).setTranslation("CarAlarmFrequency");
   public final BooleanSandboxOption PlayerDamageFromCrash = this.newBooleanOption("PlayerDamageFromCrash", true);
   public final DoubleSandboxOption SirenShutoffHours = this.newDoubleOption("SirenShutoffHours", 0.0, 168.0, 0.0);
   public final EnumSandboxOption ChanceHasGas = this.newEnumOption("ChanceHasGas", 3, 2);
   public final EnumSandboxOption RecentlySurvivorVehicles = this.newEnumOption("RecentlySurvivorVehicles", 4, 3);
   public final BooleanSandboxOption MultiHitZombies = this.newBooleanOption("MultiHitZombies", false);
   public final EnumSandboxOption RearVulnerability = this.newEnumOption("RearVulnerability", 3, 3);
   public final EnumSandboxOption EnablePoisoning = this.newEnumOption("EnablePoisoning", 3, 1);
   public final EnumSandboxOption MaggotSpawn = this.newEnumOption("MaggotSpawn", 3, 1);
   public final DoubleSandboxOption LightBulbLifespan = this.newDoubleOption("LightBulbLifespan", 0.0, 1000.0, 1.0);
   protected final ArrayList<SandboxOption> options = new ArrayList();
   protected final HashMap<String, SandboxOption> optionByName = new HashMap();
   public final Map Map = new Map();
   public final ZombieLore Lore = new ZombieLore();
   public final ZombieConfig zombieConfig = new ZombieConfig();
   public final int FIRST_YEAR = 1993;
   private final int SANDBOX_VERSION = 5;
   private final ArrayList<SandboxOption> m_customOptions = new ArrayList();

   public SandboxOptions() {
      CustomSandboxOptions.instance.initInstance(this);
      this.loadGameFile("Apocalypse");
      this.setDefaultsToCurrentValues();
   }

   public static SandboxOptions getInstance() {
      return instance;
   }

   public void toLua() {
      KahluaTable var1 = (KahluaTable)LuaManager.env.rawget("SandboxVars");

      for(int var2 = 0; var2 < this.options.size(); ++var2) {
         ((SandboxOption)this.options.get(var2)).toTable(var1);
      }

   }

   public void updateFromLua() {
      if (Core.GameMode.equals("LastStand")) {
         GameTime.instance.multiplierBias = 1.2F;
      }

      KahluaTable var1 = (KahluaTable)LuaManager.env.rawget("SandboxVars");

      for(int var2 = 0; var2 < this.options.size(); ++var2) {
         ((SandboxOption)this.options.get(var2)).fromTable(var1);
      }

      switch (this.Speed) {
         case 1:
            GameTime.instance.multiplierBias = 0.8F;
            break;
         case 2:
            GameTime.instance.multiplierBias = 0.9F;
            break;
         case 3:
            GameTime.instance.multiplierBias = 1.0F;
            break;
         case 4:
            GameTime.instance.multiplierBias = 1.1F;
            break;
         case 5:
            GameTime.instance.multiplierBias = 1.2F;
      }

      if (this.Zombies.getValue() == 1) {
         VirtualZombieManager.instance.MaxRealZombies = 400;
      }

      if (this.Zombies.getValue() == 2) {
         VirtualZombieManager.instance.MaxRealZombies = 350;
      }

      if (this.Zombies.getValue() == 3) {
         VirtualZombieManager.instance.MaxRealZombies = 300;
      }

      if (this.Zombies.getValue() == 4) {
         VirtualZombieManager.instance.MaxRealZombies = 200;
      }

      if (this.Zombies.getValue() == 5) {
         VirtualZombieManager.instance.MaxRealZombies = 100;
      }

      if (this.Zombies.getValue() == 6) {
         VirtualZombieManager.instance.MaxRealZombies = 0;
      }

      VirtualZombieManager.instance.MaxRealZombies = 1;
      this.applySettings();
   }

   public void initSandboxVars() {
      KahluaTable var1 = (KahluaTable)LuaManager.env.rawget("SandboxVars");

      for(int var2 = 0; var2 < this.options.size(); ++var2) {
         SandboxOption var3 = (SandboxOption)this.options.get(var2);
         var3.fromTable(var1);
         var3.toTable(var1);
      }

   }

   public int randomWaterShut(int var1) {
      switch (var1) {
         case 2:
            return Rand.Next(0, 30);
         case 3:
            return Rand.Next(0, 60);
         case 4:
            return Rand.Next(0, 180);
         case 5:
            return Rand.Next(0, 360);
         case 6:
            return Rand.Next(0, 1800);
         case 7:
            return Rand.Next(60, 180);
         case 8:
            return Rand.Next(180, 360);
         default:
            return -1;
      }
   }

   public int randomElectricityShut(int var1) {
      switch (var1) {
         case 2:
            return Rand.Next(14, 30);
         case 3:
            return Rand.Next(14, 60);
         case 4:
            return Rand.Next(14, 180);
         case 5:
            return Rand.Next(14, 360);
         case 6:
            return Rand.Next(14, 1800);
         case 7:
            return Rand.Next(60, 180);
         case 8:
            return Rand.Next(180, 360);
         default:
            return -1;
      }
   }

   public int getTemperatureModifier() {
      return this.Temperature.getValue();
   }

   public int getRainModifier() {
      return this.Rain.getValue();
   }

   public int getErosionSpeed() {
      return this.ErosionSpeed.getValue();
   }

   public int getFoodLootModifier() {
      return this.FoodLoot.getValue();
   }

   public int getWeaponLootModifier() {
      return this.WeaponLoot.getValue();
   }

   public int getOtherLootModifier() {
      return this.OtherLoot.getValue();
   }

   public int getWaterShutModifier() {
      return this.WaterShutModifier.getValue();
   }

   public int getElecShutModifier() {
      return this.ElecShutModifier.getValue();
   }

   public int getTimeSinceApo() {
      return this.TimeSinceApo.getValue();
   }

   public double getEnduranceRegenMultiplier() {
      switch (this.EndRegen.getValue()) {
         case 1:
            return 1.8;
         case 2:
            return 1.3;
         case 3:
         default:
            return 1.0;
         case 4:
            return 0.7;
         case 5:
            return 0.4;
      }
   }

   public double getStatsDecreaseMultiplier() {
      switch (this.StatsDecrease.getValue()) {
         case 1:
            return 2.0;
         case 2:
            return 1.6;
         case 3:
         default:
            return 1.0;
         case 4:
            return 0.8;
         case 5:
            return 0.65;
      }
   }

   public int getDayLengthMinutes() {
      switch (this.DayLength.getValue()) {
         case 1:
            return 15;
         case 2:
            return 30;
         default:
            return (this.DayLength.getValue() - 2) * 60;
      }
   }

   public int getDayLengthMinutesDefault() {
      switch (this.DayLength.getDefaultValue()) {
         case 1:
            return 15;
         case 2:
            return 30;
         default:
            return (this.DayLength.getDefaultValue() - 2) * 60;
      }
   }

   public int getCompostHours() {
      switch (this.CompostTime.getValue()) {
         case 1:
            return 168;
         case 2:
            return 336;
         case 3:
            return 504;
         case 4:
            return 672;
         case 5:
            return 1008;
         case 6:
            return 1344;
         case 7:
            return 1680;
         case 8:
            return 2016;
         default:
            return 336;
      }
   }

   public void applySettings() {
      GameTime.instance.setStartYear(this.getFirstYear() + this.StartYear.getValue() - 1);
      GameTime.instance.setStartMonth(this.StartMonth.getValue() - 1);
      GameTime.instance.setStartDay(this.StartDay.getValue() - 1);
      GameTime.instance.setMinutesPerDay((float)this.getDayLengthMinutes());
      if (this.StartTime.getValue() == 1) {
         GameTime.instance.setStartTimeOfDay(7.0F);
      } else if (this.StartTime.getValue() == 2) {
         GameTime.instance.setStartTimeOfDay(9.0F);
      } else if (this.StartTime.getValue() == 3) {
         GameTime.instance.setStartTimeOfDay(12.0F);
      } else if (this.StartTime.getValue() == 4) {
         GameTime.instance.setStartTimeOfDay(14.0F);
      } else if (this.StartTime.getValue() == 5) {
         GameTime.instance.setStartTimeOfDay(17.0F);
      } else if (this.StartTime.getValue() == 6) {
         GameTime.instance.setStartTimeOfDay(21.0F);
      } else if (this.StartTime.getValue() == 7) {
         GameTime.instance.setStartTimeOfDay(0.0F);
      } else if (this.StartTime.getValue() == 8) {
         GameTime.instance.setStartTimeOfDay(2.0F);
      } else if (this.StartTime.getValue() == 9) {
         GameTime.instance.setStartTimeOfDay(5.0F);
      }

   }

   public void save(ByteBuffer var1) throws IOException {
      var1.put((byte)83);
      var1.put((byte)65);
      var1.put((byte)78);
      var1.put((byte)68);
      var1.putInt(195);
      var1.putInt(5);
      var1.putInt(this.options.size());

      for(int var2 = 0; var2 < this.options.size(); ++var2) {
         SandboxOption var3 = (SandboxOption)this.options.get(var2);
         GameWindow.WriteStringUTF(var1, var3.asConfigOption().getName());
         GameWindow.WriteStringUTF(var1, var3.asConfigOption().getValueAsString());
      }

   }

   public void load(ByteBuffer var1) throws IOException {
      var1.mark();
      byte var3 = var1.get();
      byte var4 = var1.get();
      byte var5 = var1.get();
      byte var6 = var1.get();
      int var2;
      if (var3 == 83 && var4 == 65 && var5 == 78 && var6 == 68) {
         var2 = var1.getInt();
      } else {
         var2 = 41;
         var1.reset();
      }

      if (var2 >= 88) {
         int var7 = 2;
         if (var2 >= 131) {
            var7 = var1.getInt();
         }

         int var8 = var1.getInt();

         for(int var9 = 0; var9 < var8; ++var9) {
            String var10 = GameWindow.ReadStringUTF(var1);
            String var11 = GameWindow.ReadStringUTF(var1);
            var10 = this.upgradeOptionName(var10, var7);
            var11 = this.upgradeOptionValue(var10, var11, var7);
            SandboxOption var12 = (SandboxOption)this.optionByName.get(var10);
            if (var12 == null) {
               DebugLog.log("ERROR unknown SandboxOption \"" + var10 + "\"");
            } else {
               var12.asConfigOption().parse(var11);
            }
         }

         if (var2 < 157) {
            instance.CannedFoodLoot.setValue(instance.FoodLoot.getValue());
            instance.AmmoLoot.setValue(instance.WeaponLoot.getValue());
            instance.RangedWeaponLoot.setValue(instance.WeaponLoot.getValue());
            instance.MedicalLoot.setValue(instance.OtherLoot.getValue());
            instance.LiteratureLoot.setValue(instance.OtherLoot.getValue());
            instance.SurvivalGearsLoot.setValue(instance.OtherLoot.getValue());
            instance.MechanicsLoot.setValue(instance.OtherLoot.getValue());
         }

      }
   }

   public int getFirstYear() {
      return 1993;
   }

   private static String[] parseName(String var0) {
      String[] var1 = new String[]{null, var0};
      if (var0.contains(".")) {
         String[] var2 = var0.split("\\.");
         if (var2.length == 2) {
            var1[0] = var2[0];
            var1[1] = var2[1];
         }
      }

      return var1;
   }

   private BooleanSandboxOption newBooleanOption(String var1, boolean var2) {
      return new BooleanSandboxOption(this, var1, var2);
   }

   private DoubleSandboxOption newDoubleOption(String var1, double var2, double var4, double var6) {
      return new DoubleSandboxOption(this, var1, var2, var4, var6);
   }

   private EnumSandboxOption newEnumOption(String var1, int var2, int var3) {
      return new EnumSandboxOption(this, var1, var2, var3);
   }

   private IntegerSandboxOption newIntegerOption(String var1, int var2, int var3, int var4) {
      return new IntegerSandboxOption(this, var1, var2, var3, var4);
   }

   private StringSandboxOption newStringOption(String var1, String var2, int var3) {
      return new StringSandboxOption(this, var1, var2, var3);
   }

   protected SandboxOptions addOption(SandboxOption var1) {
      this.options.add(var1);
      this.optionByName.put(var1.asConfigOption().getName(), var1);
      return this;
   }

   public int getNumOptions() {
      return this.options.size();
   }

   public SandboxOption getOptionByIndex(int var1) {
      return (SandboxOption)this.options.get(var1);
   }

   public SandboxOption getOptionByName(String var1) {
      return (SandboxOption)this.optionByName.get(var1);
   }

   public void set(String var1, Object var2) {
      if (var1 != null && var2 != null) {
         SandboxOption var3 = (SandboxOption)this.optionByName.get(var1);
         if (var3 == null) {
            throw new IllegalArgumentException("unknown SandboxOption \"" + var1 + "\"");
         } else {
            var3.asConfigOption().setValueFromObject(var2);
         }
      } else {
         throw new IllegalArgumentException();
      }
   }

   public void copyValuesFrom(SandboxOptions var1) {
      if (var1 == null) {
         throw new NullPointerException();
      } else {
         for(int var2 = 0; var2 < this.options.size(); ++var2) {
            ((SandboxOption)this.options.get(var2)).asConfigOption().setValueFromObject(((SandboxOption)var1.options.get(var2)).asConfigOption().getValueAsObject());
         }

      }
   }

   public void resetToDefault() {
      for(int var1 = 0; var1 < this.options.size(); ++var1) {
         ((SandboxOption)this.options.get(var1)).asConfigOption().resetToDefault();
      }

   }

   public void setDefaultsToCurrentValues() {
      for(int var1 = 0; var1 < this.options.size(); ++var1) {
         ((SandboxOption)this.options.get(var1)).asConfigOption().setDefaultToCurrentValue();
      }

   }

   public SandboxOptions newCopy() {
      SandboxOptions var1 = new SandboxOptions();
      var1.copyValuesFrom(this);
      return var1;
   }

   public static boolean isValidPresetName(String var0) {
      if (var0 != null && !var0.isEmpty()) {
         return !var0.contains("/") && !var0.contains("\\") && !var0.contains(":") && !var0.contains(";") && !var0.contains("\"") && !var0.contains(".");
      } else {
         return false;
      }
   }

   private boolean readTextFile(String var1, boolean var2) {
      ConfigFile var3 = new ConfigFile();
      if (!var3.read(var1)) {
         return false;
      } else {
         int var4 = var3.getVersion();
         HashSet var5 = null;
         int var6;
         if (var2 && var4 == 1) {
            var5 = new HashSet();

            for(var6 = 0; var6 < this.options.size(); ++var6) {
               if ("ZombieLore".equals(((SandboxOption)this.options.get(var6)).getTableName())) {
                  var5.add(((SandboxOption)this.options.get(var6)).getShortName());
               }
            }
         }

         for(var6 = 0; var6 < var3.getOptions().size(); ++var6) {
            ConfigOption var7 = (ConfigOption)var3.getOptions().get(var6);
            String var8 = var7.getName();
            String var9 = var7.getValueAsString();
            if (var5 != null && var5.contains(var8)) {
               var8 = "ZombieLore." + var8;
            }

            if (var2 && var4 == 1) {
               if ("WaterShutModifier".equals(var8)) {
                  var8 = "WaterShut";
               } else if ("ElecShutModifier".equals(var8)) {
                  var8 = "ElecShut";
               }
            }

            var8 = this.upgradeOptionName(var8, var4);
            var9 = this.upgradeOptionValue(var8, var9, var4);
            SandboxOption var10 = (SandboxOption)this.optionByName.get(var8);
            if (var10 != null) {
               var10.asConfigOption().parse(var9);
            }
         }

         return true;
      }
   }

   private boolean writeTextFile(String var1, int var2) {
      ConfigFile var3 = new ConfigFile();
      ArrayList var4 = new ArrayList();
      Iterator var5 = this.options.iterator();

      while(var5.hasNext()) {
         SandboxOption var6 = (SandboxOption)var5.next();
         var4.add(var6.asConfigOption());
      }

      return var3.write(var1, var2, var4);
   }

   public boolean loadServerTextFile(String var1) {
      return this.readTextFile(ServerSettingsManager.instance.getNameInSettingsFolder(var1 + "_sandbox.ini"), false);
   }

   public boolean loadServerLuaFile(String var1) {
      boolean var2 = this.readLuaFile(ServerSettingsManager.instance.getNameInSettingsFolder(var1 + "_SandboxVars.lua"));
      if (this.Lore.Speed.getValue() == 1) {
         this.Lore.Speed.setValue(2);
      }

      return var2;
   }

   public boolean saveServerLuaFile(String var1) {
      return this.writeLuaFile(ServerSettingsManager.instance.getNameInSettingsFolder(var1 + "_SandboxVars.lua"), false);
   }

   public boolean loadPresetFile(String var1) {
      return this.readTextFile(LuaManager.getSandboxCacheDir() + File.separator + var1 + ".cfg", true);
   }

   public boolean savePresetFile(String var1) {
      return !isValidPresetName(var1) ? false : this.writeTextFile(LuaManager.getSandboxCacheDir() + File.separator + var1 + ".cfg", 5);
   }

   public boolean loadGameFile(String var1) {
      File var2 = ZomboidFileSystem.instance.getMediaFile("lua/shared/Sandbox/" + var1 + ".lua");
      if (!var2.exists()) {
         throw new RuntimeException("media/lua/shared/Sandbox/" + var1 + ".lua not found");
      } else {
         try {
            LuaManager.loaded.remove(var2.getAbsolutePath().replace("\\", "/"));
            Object var3 = LuaManager.RunLua(var2.getAbsolutePath());
            if (!(var3 instanceof KahluaTable)) {
               throw new RuntimeException(var2.getName() + " must return a SandboxVars table");
            } else {
               for(int var4 = 0; var4 < this.options.size(); ++var4) {
                  ((SandboxOption)this.options.get(var4)).fromTable((KahluaTable)var3);
               }

               return true;
            }
         } catch (Exception var5) {
            ExceptionLogger.logException(var5);
            return false;
         }
      }
   }

   public boolean saveGameFile(String var1) {
      return !Core.bDebug ? false : this.writeLuaFile("media/lua/shared/Sandbox/" + var1 + ".lua", true);
   }

   private void saveCurrentGameBinFile() {
      File var1 = ZomboidFileSystem.instance.getFileInCurrentSave("map_sand.bin");

      try {
         FileOutputStream var2 = new FileOutputStream(var1);

         try {
            BufferedOutputStream var3 = new BufferedOutputStream(var2);

            try {
               synchronized(SliceY.SliceBufferLock) {
                  SliceY.SliceBuffer.clear();
                  this.save(SliceY.SliceBuffer);
                  var3.write(SliceY.SliceBuffer.array(), 0, SliceY.SliceBuffer.position());
               }
            } catch (Throwable var9) {
               try {
                  var3.close();
               } catch (Throwable var7) {
                  var9.addSuppressed(var7);
               }

               throw var9;
            }

            var3.close();
         } catch (Throwable var10) {
            try {
               var2.close();
            } catch (Throwable var6) {
               var10.addSuppressed(var6);
            }

            throw var10;
         }

         var2.close();
      } catch (Exception var11) {
         ExceptionLogger.logException(var11);
      }

   }

   public void handleOldZombiesFile1() {
      if (!GameServer.bServer) {
         String var1 = ZomboidFileSystem.instance.getFileNameInCurrentSave("zombies.ini");
         ConfigFile var2 = new ConfigFile();
         if (var2.read(var1)) {
            for(int var3 = 0; var3 < var2.getOptions().size(); ++var3) {
               ConfigOption var4 = (ConfigOption)var2.getOptions().get(var3);
               SandboxOption var5 = (SandboxOption)this.optionByName.get("ZombieConfig." + var4.getName());
               if (var5 != null) {
                  var5.asConfigOption().parse(var4.getValueAsString());
               }
            }
         }

      }
   }

   public void handleOldZombiesFile2() {
      if (!GameServer.bServer) {
         String var1 = ZomboidFileSystem.instance.getFileNameInCurrentSave("zombies.ini");
         File var2 = new File(var1);
         if (var2.exists()) {
            try {
               DebugLog.log("deleting " + var2.getAbsolutePath());
               var2.delete();
               this.saveCurrentGameBinFile();
            } catch (Exception var4) {
               ExceptionLogger.logException(var4);
            }

         }
      }
   }

   public void handleOldServerZombiesFile() {
      if (GameServer.bServer) {
         if (this.loadServerZombiesFile(GameServer.ServerName)) {
            String var1 = ServerSettingsManager.instance.getNameInSettingsFolder(GameServer.ServerName + "_zombies.ini");

            try {
               File var2 = new File(var1);
               DebugLog.log("deleting " + var2.getAbsolutePath());
               var2.delete();
               this.saveServerLuaFile(GameServer.ServerName);
            } catch (Exception var3) {
               ExceptionLogger.logException(var3);
            }
         }

      }
   }

   public boolean loadServerZombiesFile(String var1) {
      String var2 = ServerSettingsManager.instance.getNameInSettingsFolder(var1 + "_zombies.ini");
      ConfigFile var3 = new ConfigFile();
      if (var3.read(var2)) {
         for(int var4 = 0; var4 < var3.getOptions().size(); ++var4) {
            ConfigOption var5 = (ConfigOption)var3.getOptions().get(var4);
            SandboxOption var6 = (SandboxOption)this.optionByName.get("ZombieConfig." + var5.getName());
            if (var6 != null) {
               var6.asConfigOption().parse(var5.getValueAsString());
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private boolean readLuaFile(String var1) {
      File var2 = (new File(var1)).getAbsoluteFile();
      if (!var2.exists()) {
         return false;
      } else {
         Object var3 = LuaManager.env.rawget("SandboxVars");
         KahluaTable var4 = null;
         if (var3 instanceof KahluaTable) {
            var4 = (KahluaTable)var3;
         }

         LuaManager.env.rawset("SandboxVars", (Object)null);

         boolean var17;
         try {
            LuaManager.loaded.remove(var2.getAbsolutePath().replace("\\", "/"));
            Object var5 = LuaManager.RunLua(var2.getAbsolutePath());
            Object var16 = LuaManager.env.rawget("SandboxVars");
            if (var16 == null) {
               var17 = false;
               return var17;
            }

            if (var16 instanceof KahluaTable) {
               KahluaTable var7 = (KahluaTable)var16;
               int var8 = 0;
               Object var9 = var7.rawget("VERSION");
               if (var9 != null) {
                  if (var9 instanceof Double) {
                     var8 = ((Double)var9).intValue();
                  } else {
                     DebugLog.log("ERROR: VERSION=\"" + var9 + "\" in " + var1);
                  }

                  var7.rawset("VERSION", (Object)null);
               }

               var7 = this.upgradeLuaTable("", var7, var8);

               for(int var10 = 0; var10 < this.options.size(); ++var10) {
                  ((SandboxOption)this.options.get(var10)).fromTable(var7);
               }
            }

            var17 = true;
         } catch (Exception var14) {
            ExceptionLogger.logException(var14);
            boolean var6 = false;
            return var6;
         } finally {
            if (var4 != null) {
               LuaManager.env.rawset("SandboxVars", var4);
            }

         }

         return var17;
      }
   }

   private boolean writeLuaFile(String var1, boolean var2) {
      File var3 = (new File(var1)).getAbsoluteFile();
      DebugLog.log("writing " + var1);

      try {
         FileWriter var4 = new FileWriter(var3);

         try {
            HashMap var5 = new HashMap();
            ArrayList var6 = new ArrayList();
            var5.put("", new ArrayList());
            Iterator var7 = this.options.iterator();

            while(var7.hasNext()) {
               SandboxOption var8 = (SandboxOption)var7.next();
               if (var8.getTableName() == null) {
                  ((ArrayList)var5.get("")).add(var8);
               } else {
                  if (var5.get(var8.getTableName()) == null) {
                     var5.put(var8.getTableName(), new ArrayList());
                     var6.add(var8.getTableName());
                  }

                  ((ArrayList)var5.get(var8.getTableName())).add(var8);
               }
            }

            String var20 = System.lineSeparator();
            if (var2) {
               var4.write("return {" + var20);
            } else {
               var4.write("SandboxVars = {" + var20);
            }

            var4.write("    VERSION = 5," + var20);
            Iterator var21 = ((ArrayList)var5.get("")).iterator();

            String var10001;
            while(var21.hasNext()) {
               SandboxOption var9 = (SandboxOption)var21.next();
               if (!var2) {
                  String var10 = var9.asConfigOption().getTooltip();
                  if (var10 != null) {
                     var10 = var10.replace("\\n", " ").replace("\\\"", "\"");
                     var10 = var10.replaceAll("\n", var20 + "    -- ");
                     var4.write("    -- " + var10 + var20);
                  }

                  EnumSandboxOption var11 = (EnumSandboxOption)Type.tryCastTo(var9, EnumSandboxOption.class);
                  if (var11 != null) {
                     for(int var12 = 1; var12 < var11.getNumValues(); ++var12) {
                        try {
                           String var13 = var11.getValueTranslationByIndexOrNull(var12);
                           if (var13 != null) {
                              var4.write("    -- " + var12 + " = " + var13.replace("\\\"", "\"") + var20);
                           }
                        } catch (Exception var17) {
                           ExceptionLogger.logException(var17);
                        }
                     }
                  }
               }

               var10001 = var9.asConfigOption().getName();
               var4.write("    " + var10001 + " = " + var9.asConfigOption().getValueAsLuaString() + "," + var20);
            }

            var21 = var6.iterator();

            while(var21.hasNext()) {
               String var22 = (String)var21.next();
               var4.write("    " + var22 + " = {" + var20);
               Iterator var23 = ((ArrayList)var5.get(var22)).iterator();

               while(var23.hasNext()) {
                  SandboxOption var24 = (SandboxOption)var23.next();
                  if (!var2) {
                     String var25 = var24.asConfigOption().getTooltip();
                     if (var25 != null) {
                        var25 = var25.replace("\\n", " ").replace("\\\"", "\"");
                        var25 = var25.replaceAll("\n", var20 + "        -- ");
                        var4.write("        -- " + var25 + var20);
                     }

                     if (var24 instanceof EnumSandboxOption) {
                        for(int var26 = 1; var26 < ((EnumSandboxOption)var24).getNumValues(); ++var26) {
                           try {
                              String var14 = ((EnumSandboxOption)var24).getValueTranslationByIndexOrNull(var26);
                              if (var14 != null) {
                                 var4.write("        -- " + var26 + " = " + var14 + var20);
                              }
                           } catch (Exception var16) {
                              ExceptionLogger.logException(var16);
                           }
                        }
                     }
                  }

                  var10001 = var24.getShortName();
                  var4.write("        " + var10001 + " = " + var24.asConfigOption().getValueAsLuaString() + "," + var20);
               }

               var4.write("    }," + var20);
            }

            var4.write("}" + var20);
         } catch (Throwable var18) {
            try {
               var4.close();
            } catch (Throwable var15) {
               var18.addSuppressed(var15);
            }

            throw var18;
         }

         var4.close();
         return true;
      } catch (Exception var19) {
         ExceptionLogger.logException(var19);
         return false;
      }
   }

   public void load() {
      File var1 = ZomboidFileSystem.instance.getFileInCurrentSave("map_sand.bin");

      try {
         FileInputStream var2 = new FileInputStream(var1);

         try {
            BufferedInputStream var3 = new BufferedInputStream(var2);

            try {
               synchronized(SliceY.SliceBufferLock) {
                  SliceY.SliceBuffer.clear();
                  int var5 = var3.read(SliceY.SliceBuffer.array());
                  SliceY.SliceBuffer.limit(var5);
                  this.load(SliceY.SliceBuffer);
                  this.handleOldZombiesFile1();
                  this.applySettings();
                  this.toLua();
               }
            } catch (Throwable var10) {
               try {
                  var3.close();
               } catch (Throwable var8) {
                  var10.addSuppressed(var8);
               }

               throw var10;
            }

            var3.close();
         } catch (Throwable var11) {
            try {
               var2.close();
            } catch (Throwable var7) {
               var11.addSuppressed(var7);
            }

            throw var11;
         }

         var2.close();
         return;
      } catch (FileNotFoundException var12) {
      } catch (Exception var13) {
         ExceptionLogger.logException(var13);
      }

      this.resetToDefault();
      this.updateFromLua();
   }

   public void loadCurrentGameBinFile() {
      File var1 = ZomboidFileSystem.instance.getFileInCurrentSave("map_sand.bin");

      try {
         FileInputStream var2 = new FileInputStream(var1);

         try {
            BufferedInputStream var3 = new BufferedInputStream(var2);

            try {
               synchronized(SliceY.SliceBufferLock) {
                  SliceY.SliceBuffer.clear();
                  int var5 = var3.read(SliceY.SliceBuffer.array());
                  SliceY.SliceBuffer.limit(var5);
                  this.load(SliceY.SliceBuffer);
               }

               this.toLua();
            } catch (Throwable var10) {
               try {
                  var3.close();
               } catch (Throwable var8) {
                  var10.addSuppressed(var8);
               }

               throw var10;
            }

            var3.close();
         } catch (Throwable var11) {
            try {
               var2.close();
            } catch (Throwable var7) {
               var11.addSuppressed(var7);
            }

            throw var11;
         }

         var2.close();
      } catch (Exception var12) {
         ExceptionLogger.logException(var12);
      }

   }

   private String upgradeOptionName(String var1, int var2) {
      return var1;
   }

   private String upgradeOptionValue(String var1, String var2, int var3) {
      if (var3 < 3 && "DayLength".equals(var1)) {
         this.DayLength.parse(var2);
         if (this.DayLength.getValue() == 8) {
            this.DayLength.setValue(14);
         } else if (this.DayLength.getValue() == 9) {
            this.DayLength.setValue(26);
         }

         var2 = this.DayLength.getValueAsString();
      }

      int var4;
      if (var3 < 4 && "CarSpawnRate".equals(var1)) {
         try {
            var4 = (int)Double.parseDouble(var2);
            if (var4 > 1) {
               var2 = Integer.toString(var4 + 1);
            }
         } catch (NumberFormatException var8) {
            var8.printStackTrace();
         }
      }

      if (var3 < 5) {
         if ("FoodLoot".equals(var1) || "CannedFoodLoot".equals(var1) || "LiteratureLoot".equals(var1) || "SurvivalGearsLoot".equals(var1) || "MedicalLoot".equals(var1) || "WeaponLoot".equals(var1) || "RangedWeaponLoot".equals(var1) || "AmmoLoot".equals(var1) || "MechanicsLoot".equals(var1) || "OtherLoot".equals(var1)) {
            try {
               var4 = (int)Double.parseDouble(var2);
               if (var4 > 0) {
                  var2 = Integer.toString(var4 + 2);
               }
            } catch (NumberFormatException var7) {
               var7.printStackTrace();
            }
         }

         if ("FuelStationGas".equals(var1)) {
            try {
               var4 = (int)Double.parseDouble(var2);
               if (var4 > 1) {
                  var2 = Integer.toString(var4 + 1);
               }
            } catch (NumberFormatException var6) {
               var6.printStackTrace();
            }
         }

         if ("RecentlySurvivorVehicles".equals(var1)) {
            try {
               var4 = (int)Double.parseDouble(var2);
               if (var4 > 0) {
                  var2 = Integer.toString(var4 + 1);
               }
            } catch (NumberFormatException var5) {
               var5.printStackTrace();
            }
         }
      }

      return var2;
   }

   private KahluaTable upgradeLuaTable(String var1, KahluaTable var2, int var3) {
      KahluaTable var4 = LuaManager.platform.newTable();
      KahluaTableIterator var5 = var2.iterator();

      while(var5.advance()) {
         if (!(var5.getKey() instanceof String)) {
            throw new IllegalStateException("expected a String key");
         }

         if (var5.getValue() instanceof KahluaTable) {
            KahluaTable var6 = this.upgradeLuaTable(var1 + var5.getKey() + ".", (KahluaTable)var5.getValue(), var3);
            var4.rawset(var5.getKey(), var6);
         } else {
            String var8 = this.upgradeOptionName(var1 + var5.getKey(), var3);
            String var7 = this.upgradeOptionValue(var8, var5.getValue().toString(), var3);
            var4.rawset(var8.replace(var1, ""), var7);
         }
      }

      return var4;
   }

   public void sendToServer() {
      if (GameClient.bClient) {
         GameClient.instance.sendSandboxOptionsToServer(this);
      }

   }

   public void newCustomOption(CustomSandboxOption var1) {
      CustomBooleanSandboxOption var2 = (CustomBooleanSandboxOption)Type.tryCastTo(var1, CustomBooleanSandboxOption.class);
      if (var2 != null) {
         this.addCustomOption(new BooleanSandboxOption(this, var2.m_id, var2.defaultValue), var1);
      } else {
         CustomDoubleSandboxOption var3 = (CustomDoubleSandboxOption)Type.tryCastTo(var1, CustomDoubleSandboxOption.class);
         if (var3 != null) {
            this.addCustomOption(new DoubleSandboxOption(this, var3.m_id, var3.min, var3.max, var3.defaultValue), var1);
         } else {
            CustomEnumSandboxOption var4 = (CustomEnumSandboxOption)Type.tryCastTo(var1, CustomEnumSandboxOption.class);
            if (var4 != null) {
               EnumSandboxOption var7 = new EnumSandboxOption(this, var4.m_id, var4.numValues, var4.defaultValue);
               if (var4.m_valueTranslation != null) {
                  var7.setValueTranslation(var4.m_valueTranslation);
               }

               this.addCustomOption(var7, var1);
            } else {
               CustomIntegerSandboxOption var5 = (CustomIntegerSandboxOption)Type.tryCastTo(var1, CustomIntegerSandboxOption.class);
               if (var5 != null) {
                  this.addCustomOption(new IntegerSandboxOption(this, var5.m_id, var5.min, var5.max, var5.defaultValue), var1);
               } else {
                  CustomStringSandboxOption var6 = (CustomStringSandboxOption)Type.tryCastTo(var1, CustomStringSandboxOption.class);
                  if (var6 != null) {
                     this.addCustomOption(new StringSandboxOption(this, var6.m_id, var6.defaultValue, -1), var1);
                  } else {
                     throw new IllegalArgumentException("unhandled CustomSandboxOption " + var1);
                  }
               }
            }
         }
      }
   }

   private void addCustomOption(SandboxOption var1, CustomSandboxOption var2) {
      var1.setCustom();
      if (var2.m_page != null) {
         var1.setPageName(var2.m_page);
      }

      if (var2.m_translation != null) {
         var1.setTranslation(var2.m_translation);
      }

      this.m_customOptions.add(var1);
   }

   private void removeCustomOptions() {
      this.options.removeAll(this.m_customOptions);
      Iterator var1 = this.m_customOptions.iterator();

      while(var1.hasNext()) {
         SandboxOption var2 = (SandboxOption)var1.next();
         this.optionByName.remove(var2.asConfigOption().getName());
      }

      this.m_customOptions.clear();
   }

   public static void Reset() {
      instance.removeCustomOptions();
   }

   public boolean getAllClothesUnlocked() {
      return this.AllClothesUnlocked.getValue();
   }

   public final class Map {
      public final BooleanSandboxOption AllowMiniMap = SandboxOptions.this.newBooleanOption("Map.AllowMiniMap", false);
      public final BooleanSandboxOption AllowWorldMap = SandboxOptions.this.newBooleanOption("Map.AllowWorldMap", true);
      public final BooleanSandboxOption MapAllKnown = SandboxOptions.this.newBooleanOption("Map.MapAllKnown", false);

      Map() {
      }
   }

   public final class ZombieLore {
      public final EnumSandboxOption Speed = (EnumSandboxOption)SandboxOptions.this.newEnumOption("ZombieLore.Speed", 4, 2).setTranslation("ZSpeed");
      public final EnumSandboxOption Strength = (EnumSandboxOption)SandboxOptions.this.newEnumOption("ZombieLore.Strength", 4, 2).setTranslation("ZStrength");
      public final EnumSandboxOption Toughness = (EnumSandboxOption)SandboxOptions.this.newEnumOption("ZombieLore.Toughness", 4, 2).setTranslation("ZToughness");
      public final EnumSandboxOption Transmission = (EnumSandboxOption)SandboxOptions.this.newEnumOption("ZombieLore.Transmission", 4, 1).setTranslation("ZTransmission");
      public final EnumSandboxOption Mortality = (EnumSandboxOption)SandboxOptions.this.newEnumOption("ZombieLore.Mortality", 7, 5).setTranslation("ZInfectionMortality");
      public final EnumSandboxOption Reanimate = (EnumSandboxOption)SandboxOptions.this.newEnumOption("ZombieLore.Reanimate", 6, 3).setTranslation("ZReanimateTime");
      public final EnumSandboxOption Cognition = (EnumSandboxOption)SandboxOptions.this.newEnumOption("ZombieLore.Cognition", 4, 3).setTranslation("ZCognition");
      public final EnumSandboxOption CrawlUnderVehicle = (EnumSandboxOption)SandboxOptions.this.newEnumOption("ZombieLore.CrawlUnderVehicle", 7, 5).setTranslation("ZCrawlUnderVehicle");
      public final EnumSandboxOption Memory = (EnumSandboxOption)SandboxOptions.this.newEnumOption("ZombieLore.Memory", 5, 2).setTranslation("ZMemory");
      public final EnumSandboxOption Sight = (EnumSandboxOption)SandboxOptions.this.newEnumOption("ZombieLore.Sight", 4, 2).setTranslation("ZSight");
      public final EnumSandboxOption Hearing = (EnumSandboxOption)SandboxOptions.this.newEnumOption("ZombieLore.Hearing", 4, 2).setTranslation("ZHearing");
      public final BooleanSandboxOption ThumpNoChasing = SandboxOptions.this.newBooleanOption("ZombieLore.ThumpNoChasing", false);
      public final BooleanSandboxOption ThumpOnConstruction = SandboxOptions.this.newBooleanOption("ZombieLore.ThumpOnConstruction", true);
      public final EnumSandboxOption ActiveOnly = (EnumSandboxOption)SandboxOptions.this.newEnumOption("ZombieLore.ActiveOnly", 3, 1).setTranslation("ActiveOnly");
      public final BooleanSandboxOption TriggerHouseAlarm = SandboxOptions.this.newBooleanOption("ZombieLore.TriggerHouseAlarm", false);
      public final BooleanSandboxOption ZombiesDragDown = SandboxOptions.this.newBooleanOption("ZombieLore.ZombiesDragDown", true);
      public final BooleanSandboxOption ZombiesFenceLunge = SandboxOptions.this.newBooleanOption("ZombieLore.ZombiesFenceLunge", true);
      public final EnumSandboxOption DisableFakeDead = SandboxOptions.this.newEnumOption("ZombieLore.DisableFakeDead", 3, 1);

      private ZombieLore() {
      }
   }

   public final class ZombieConfig {
      public final DoubleSandboxOption PopulationMultiplier = SandboxOptions.this.newDoubleOption("ZombieConfig.PopulationMultiplier", 0.0, 4.0, 1.0);
      public final DoubleSandboxOption PopulationStartMultiplier = SandboxOptions.this.newDoubleOption("ZombieConfig.PopulationStartMultiplier", 0.0, 4.0, 1.0);
      public final DoubleSandboxOption PopulationPeakMultiplier = SandboxOptions.this.newDoubleOption("ZombieConfig.PopulationPeakMultiplier", 0.0, 4.0, 1.5);
      public final IntegerSandboxOption PopulationPeakDay = SandboxOptions.this.newIntegerOption("ZombieConfig.PopulationPeakDay", 1, 365, 28);
      public final DoubleSandboxOption RespawnHours = SandboxOptions.this.newDoubleOption("ZombieConfig.RespawnHours", 0.0, 8760.0, 72.0);
      public final DoubleSandboxOption RespawnUnseenHours = SandboxOptions.this.newDoubleOption("ZombieConfig.RespawnUnseenHours", 0.0, 8760.0, 16.0);
      public final DoubleSandboxOption RespawnMultiplier = SandboxOptions.this.newDoubleOption("ZombieConfig.RespawnMultiplier", 0.0, 1.0, 0.1);
      public final DoubleSandboxOption RedistributeHours = SandboxOptions.this.newDoubleOption("ZombieConfig.RedistributeHours", 0.0, 8760.0, 12.0);
      public final IntegerSandboxOption FollowSoundDistance = SandboxOptions.this.newIntegerOption("ZombieConfig.FollowSoundDistance", 10, 1000, 100);
      public final IntegerSandboxOption RallyGroupSize = SandboxOptions.this.newIntegerOption("ZombieConfig.RallyGroupSize", 0, 1000, 20);
      public final IntegerSandboxOption RallyTravelDistance = SandboxOptions.this.newIntegerOption("ZombieConfig.RallyTravelDistance", 5, 50, 20);
      public final IntegerSandboxOption RallyGroupSeparation = SandboxOptions.this.newIntegerOption("ZombieConfig.RallyGroupSeparation", 5, 25, 15);
      public final IntegerSandboxOption RallyGroupRadius = SandboxOptions.this.newIntegerOption("ZombieConfig.RallyGroupRadius", 1, 10, 3);

      private ZombieConfig() {
      }
   }

   public static class EnumSandboxOption extends EnumConfigOption implements SandboxOption {
      protected String translation;
      protected String tableName;
      protected String shortName;
      protected boolean bCustom;
      protected String pageName;
      protected String valueTranslation;

      public EnumSandboxOption(SandboxOptions var1, String var2, int var3, int var4) {
         super(var2, var3, var4);
         String[] var5 = SandboxOptions.parseName(var2);
         this.tableName = var5[0];
         this.shortName = var5[1];
         var1.addOption(this);
      }

      public ConfigOption asConfigOption() {
         return this;
      }

      public String getShortName() {
         return this.shortName;
      }

      public String getTableName() {
         return this.tableName;
      }

      public SandboxOption setTranslation(String var1) {
         this.translation = var1;
         return this;
      }

      public String getTranslatedName() {
         String var10000 = this.translation == null ? this.getShortName() : this.translation;
         return Translator.getText("Sandbox_" + var10000);
      }

      public String getTooltip() {
         String var10000 = this.translation == null ? this.getShortName() : this.translation;
         String var1 = Translator.getTextOrNull("Sandbox_" + var10000 + "_tooltip");
         String var2 = this.getValueTranslationByIndexOrNull(this.defaultValue);
         String var3 = var2 == null ? null : Translator.getText("Sandbox_Default", var2);
         if (var1 == null) {
            return var3;
         } else {
            return var3 == null ? var1 : var1 + "\\n" + var3;
         }
      }

      public void fromTable(KahluaTable var1) {
         Object var2;
         if (this.tableName != null) {
            var2 = var1.rawget(this.tableName);
            if (!(var2 instanceof KahluaTable)) {
               return;
            }

            var1 = (KahluaTable)var2;
         }

         var2 = var1.rawget(this.getShortName());
         if (var2 != null) {
            this.setValueFromObject(var2);
         }

      }

      public void toTable(KahluaTable var1) {
         if (this.tableName != null) {
            Object var2 = var1.rawget(this.tableName);
            if (var2 instanceof KahluaTable) {
               var1 = (KahluaTable)var2;
            } else {
               KahluaTable var3 = LuaManager.platform.newTable();
               var1.rawset(this.tableName, var3);
               var1 = var3;
            }
         }

         var1.rawset(this.getShortName(), this.getValueAsObject());
      }

      public void setCustom() {
         this.bCustom = true;
      }

      public boolean isCustom() {
         return this.bCustom;
      }

      public SandboxOption setPageName(String var1) {
         this.pageName = var1;
         return this;
      }

      public String getPageName() {
         return this.pageName;
      }

      public EnumSandboxOption setValueTranslation(String var1) {
         this.valueTranslation = var1;
         return this;
      }

      public String getValueTranslation() {
         return this.valueTranslation != null ? this.valueTranslation : (this.translation == null ? this.getShortName() : this.translation);
      }

      public String getValueTranslationByIndex(int var1) {
         if (var1 >= 1 && var1 <= this.getNumValues()) {
            String var10000 = this.getValueTranslation();
            return Translator.getText("Sandbox_" + var10000 + "_option" + var1);
         } else {
            throw new ArrayIndexOutOfBoundsException();
         }
      }

      public String getValueTranslationByIndexOrNull(int var1) {
         if (var1 >= 1 && var1 <= this.getNumValues()) {
            String var10000 = this.getValueTranslation();
            return Translator.getTextOrNull("Sandbox_" + var10000 + "_option" + var1);
         } else {
            throw new ArrayIndexOutOfBoundsException();
         }
      }
   }

   public interface SandboxOption {
      ConfigOption asConfigOption();

      String getShortName();

      String getTableName();

      SandboxOption setTranslation(String var1);

      String getTranslatedName();

      String getTooltip();

      void fromTable(KahluaTable var1);

      void toTable(KahluaTable var1);

      void setCustom();

      boolean isCustom();

      SandboxOption setPageName(String var1);

      String getPageName();
   }

   public static class IntegerSandboxOption extends IntegerConfigOption implements SandboxOption {
      protected String translation;
      protected String tableName;
      protected String shortName;
      protected boolean bCustom;
      protected String pageName;

      public IntegerSandboxOption(SandboxOptions var1, String var2, int var3, int var4, int var5) {
         super(var2, var3, var4, var5);
         String[] var6 = SandboxOptions.parseName(var2);
         this.tableName = var6[0];
         this.shortName = var6[1];
         var1.addOption(this);
      }

      public ConfigOption asConfigOption() {
         return this;
      }

      public String getShortName() {
         return this.shortName;
      }

      public String getTableName() {
         return this.tableName;
      }

      public SandboxOption setTranslation(String var1) {
         this.translation = var1;
         return this;
      }

      public String getTranslatedName() {
         String var10000 = this.translation == null ? this.getShortName() : this.translation;
         return Translator.getText("Sandbox_" + var10000);
      }

      public String getTooltip() {
         String var10000;
         String var1;
         if ("ZombieConfig".equals(this.tableName)) {
            var10000 = this.translation == null ? this.getShortName() : this.translation;
            var1 = Translator.getTextOrNull("Sandbox_" + var10000 + "_help");
         } else {
            var10000 = this.translation == null ? this.getShortName() : this.translation;
            var1 = Translator.getTextOrNull("Sandbox_" + var10000 + "_tooltip");
         }

         String var2 = Translator.getText("Sandbox_MinMaxDefault", this.min, this.max, this.defaultValue);
         if (var1 == null) {
            return var2;
         } else {
            return var2 == null ? var1 : var1 + "\\n" + var2;
         }
      }

      public void fromTable(KahluaTable var1) {
         Object var2;
         if (this.tableName != null) {
            var2 = var1.rawget(this.tableName);
            if (!(var2 instanceof KahluaTable)) {
               return;
            }

            var1 = (KahluaTable)var2;
         }

         var2 = var1.rawget(this.getShortName());
         if (var2 != null) {
            this.setValueFromObject(var2);
         }

      }

      public void toTable(KahluaTable var1) {
         if (this.tableName != null) {
            Object var2 = var1.rawget(this.tableName);
            if (var2 instanceof KahluaTable) {
               var1 = (KahluaTable)var2;
            } else {
               KahluaTable var3 = LuaManager.platform.newTable();
               var1.rawset(this.tableName, var3);
               var1 = var3;
            }
         }

         var1.rawset(this.getShortName(), this.getValueAsObject());
      }

      public void setCustom() {
         this.bCustom = true;
      }

      public boolean isCustom() {
         return this.bCustom;
      }

      public SandboxOption setPageName(String var1) {
         this.pageName = var1;
         return this;
      }

      public String getPageName() {
         return this.pageName;
      }
   }

   public static class DoubleSandboxOption extends DoubleConfigOption implements SandboxOption {
      protected String translation;
      protected String tableName;
      protected String shortName;
      protected boolean bCustom;
      protected String pageName;

      public DoubleSandboxOption(SandboxOptions var1, String var2, double var3, double var5, double var7) {
         super(var2, var3, var5, var7);
         String[] var9 = SandboxOptions.parseName(var2);
         this.tableName = var9[0];
         this.shortName = var9[1];
         var1.addOption(this);
      }

      public ConfigOption asConfigOption() {
         return this;
      }

      public String getShortName() {
         return this.shortName;
      }

      public String getTableName() {
         return this.tableName;
      }

      public SandboxOption setTranslation(String var1) {
         this.translation = var1;
         return this;
      }

      public String getTranslatedName() {
         String var10000 = this.translation == null ? this.getShortName() : this.translation;
         return Translator.getText("Sandbox_" + var10000);
      }

      public String getTooltip() {
         String var10000;
         String var1;
         if ("ZombieConfig".equals(this.tableName)) {
            var10000 = this.translation == null ? this.getShortName() : this.translation;
            var1 = Translator.getTextOrNull("Sandbox_" + var10000 + "_help");
         } else {
            var10000 = this.translation == null ? this.getShortName() : this.translation;
            var1 = Translator.getTextOrNull("Sandbox_" + var10000 + "_tooltip");
         }

         String var2 = Translator.getText("Sandbox_MinMaxDefault", String.format("%.02f", this.min), String.format("%.02f", this.max), String.format("%.02f", this.defaultValue));
         if (var1 == null) {
            return var2;
         } else {
            return var2 == null ? var1 : var1 + "\\n" + var2;
         }
      }

      public void fromTable(KahluaTable var1) {
         Object var2;
         if (this.tableName != null) {
            var2 = var1.rawget(this.tableName);
            if (!(var2 instanceof KahluaTable)) {
               return;
            }

            var1 = (KahluaTable)var2;
         }

         var2 = var1.rawget(this.getShortName());
         if (var2 != null) {
            this.setValueFromObject(var2);
         }

      }

      public void toTable(KahluaTable var1) {
         if (this.tableName != null) {
            Object var2 = var1.rawget(this.tableName);
            if (var2 instanceof KahluaTable) {
               var1 = (KahluaTable)var2;
            } else {
               KahluaTable var3 = LuaManager.platform.newTable();
               var1.rawset(this.tableName, var3);
               var1 = var3;
            }
         }

         var1.rawset(this.getShortName(), this.getValueAsObject());
      }

      public void setCustom() {
         this.bCustom = true;
      }

      public boolean isCustom() {
         return this.bCustom;
      }

      public SandboxOption setPageName(String var1) {
         this.pageName = var1;
         return this;
      }

      public String getPageName() {
         return this.pageName;
      }
   }

   public static class BooleanSandboxOption extends BooleanConfigOption implements SandboxOption {
      protected String translation;
      protected String tableName;
      protected String shortName;
      protected boolean bCustom;
      protected String pageName;

      public BooleanSandboxOption(SandboxOptions var1, String var2, boolean var3) {
         super(var2, var3);
         String[] var4 = SandboxOptions.parseName(var2);
         this.tableName = var4[0];
         this.shortName = var4[1];
         var1.addOption(this);
      }

      public ConfigOption asConfigOption() {
         return this;
      }

      public String getShortName() {
         return this.shortName;
      }

      public String getTableName() {
         return this.tableName;
      }

      public SandboxOption setTranslation(String var1) {
         this.translation = var1;
         return this;
      }

      public String getTranslatedName() {
         String var10000 = this.translation == null ? this.getShortName() : this.translation;
         return Translator.getText("Sandbox_" + var10000);
      }

      public String getTooltip() {
         String var10000 = this.translation == null ? this.getShortName() : this.translation;
         return Translator.getTextOrNull("Sandbox_" + var10000 + "_tooltip");
      }

      public void fromTable(KahluaTable var1) {
         Object var2;
         if (this.tableName != null) {
            var2 = var1.rawget(this.tableName);
            if (!(var2 instanceof KahluaTable)) {
               return;
            }

            var1 = (KahluaTable)var2;
         }

         var2 = var1.rawget(this.getShortName());
         if (var2 != null) {
            this.setValueFromObject(var2);
         }

      }

      public void toTable(KahluaTable var1) {
         if (this.tableName != null) {
            Object var2 = var1.rawget(this.tableName);
            if (var2 instanceof KahluaTable) {
               var1 = (KahluaTable)var2;
            } else {
               KahluaTable var3 = LuaManager.platform.newTable();
               var1.rawset(this.tableName, var3);
               var1 = var3;
            }
         }

         var1.rawset(this.getShortName(), this.getValueAsObject());
      }

      public void setCustom() {
         this.bCustom = true;
      }

      public boolean isCustom() {
         return this.bCustom;
      }

      public SandboxOption setPageName(String var1) {
         this.pageName = var1;
         return this;
      }

      public String getPageName() {
         return this.pageName;
      }
   }

   public static class StringSandboxOption extends StringConfigOption implements SandboxOption {
      protected String translation;
      protected String tableName;
      protected String shortName;
      protected boolean bCustom;
      protected String pageName;

      public StringSandboxOption(SandboxOptions var1, String var2, String var3, int var4) {
         super(var2, var3, var4);
         String[] var5 = SandboxOptions.parseName(var2);
         this.tableName = var5[0];
         this.shortName = var5[1];
         var1.addOption(this);
      }

      public ConfigOption asConfigOption() {
         return this;
      }

      public String getShortName() {
         return this.shortName;
      }

      public String getTableName() {
         return this.tableName;
      }

      public SandboxOption setTranslation(String var1) {
         this.translation = var1;
         return this;
      }

      public String getTranslatedName() {
         String var10000 = this.translation == null ? this.getShortName() : this.translation;
         return Translator.getText("Sandbox_" + var10000);
      }

      public String getTooltip() {
         String var10000 = this.translation == null ? this.getShortName() : this.translation;
         return Translator.getTextOrNull("Sandbox_" + var10000 + "_tooltip");
      }

      public void fromTable(KahluaTable var1) {
         Object var2;
         if (this.tableName != null) {
            var2 = var1.rawget(this.tableName);
            if (!(var2 instanceof KahluaTable)) {
               return;
            }

            var1 = (KahluaTable)var2;
         }

         var2 = var1.rawget(this.getShortName());
         if (var2 != null) {
            this.setValueFromObject(var2);
         }

      }

      public void toTable(KahluaTable var1) {
         if (this.tableName != null) {
            Object var2 = var1.rawget(this.tableName);
            if (var2 instanceof KahluaTable) {
               var1 = (KahluaTable)var2;
            } else {
               KahluaTable var3 = LuaManager.platform.newTable();
               var1.rawset(this.tableName, var3);
               var1 = var3;
            }
         }

         var1.rawset(this.getShortName(), this.getValueAsObject());
      }

      public void setCustom() {
         this.bCustom = true;
      }

      public boolean isCustom() {
         return this.bCustom;
      }

      public SandboxOption setPageName(String var1) {
         this.pageName = var1;
         return this;
      }

      public String getPageName() {
         return this.pageName;
      }
   }
}
