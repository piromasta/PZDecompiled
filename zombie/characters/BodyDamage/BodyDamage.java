package zombie.characters.BodyDamage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Consumer;
import zombie.FliesSound;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.SandboxOptions;
import zombie.WorldSoundManager;
import zombie.ZomboidGlobals;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.audio.MusicIntensityConfig;
import zombie.audio.parameters.ParameterZombieState;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characters.ClothingWetness;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoLivingCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoSurvivor;
import zombie.characters.IsoZombie;
import zombie.characters.Stats;
import zombie.characters.Moodles.MoodleType;
import zombie.characters.animals.IsoAnimal;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.logger.LoggerManager;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.Food;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.Literature;
import zombie.inventory.types.WeaponType;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.weather.ClimateManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.ServerOptions;
import zombie.network.packets.INetworkPacket;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;
import zombie.vehicles.VehicleWindow;

public final class BodyDamage {
   public final ArrayList<BodyPart> BodyParts = new ArrayList(18);
   public final ArrayList<BodyPartLast> BodyPartsLastState = new ArrayList(18);
   public int DamageModCount = 60;
   public float InfectionGrowthRate = 0.001F;
   public float InfectionLevel = 0.0F;
   public boolean IsInfected;
   public float InfectionTime = -1.0F;
   public float InfectionMortalityDuration = -1.0F;
   public float FakeInfectionLevel = 0.0F;
   public boolean IsFakeInfected;
   public float OverallBodyHealth = 100.0F;
   public float StandardHealthAddition = 0.002F;
   public float ReducedHealthAddition = 0.0013F;
   public float SeverlyReducedHealthAddition = 8.0E-4F;
   public float SleepingHealthAddition = 0.02F;
   public float HealthFromFood = 0.015F;
   public float HealthReductionFromSevereBadMoodles = 0.0165F;
   public int StandardHealthFromFoodTime = 1600;
   public float HealthFromFoodTimer = 0.0F;
   public float BoredomLevel = 0.0F;
   public float BoredomDecreaseFromReading = 0.5F;
   public float InitialThumpPain = 14.0F;
   public float InitialScratchPain = 18.0F;
   public float InitialBitePain = 25.0F;
   public float InitialWoundPain = 80.0F;
   public float ContinualPainIncrease = 0.001F;
   public float PainReductionFromMeds = 30.0F;
   public float StandardPainReductionWhenWell = 0.01F;
   public int OldNumZombiesVisible = 0;
   public int CurrentNumZombiesVisible = 0;
   public float PanicIncreaseValue = 7.0F;
   public float PanicIncreaseValueFrame = 0.035F;
   public float PanicReductionValue = 0.06F;
   public float DrunkIncreaseValue = 400.0F;
   public float DrunkReductionValue = 0.0042F;
   public boolean IsOnFire = false;
   public boolean BurntToDeath = false;
   public float Wetness = 0.0F;
   public float CatchACold = 0.0F;
   public boolean HasACold = false;
   public float ColdStrength = 0.0F;
   public float ColdProgressionRate = 0.0112F;
   public int TimeToSneezeOrCough = -1;
   public int SmokerSneezeTimerMin = 1200;
   public int SmokerSneezeTimerMax = 1600;
   public int MildColdSneezeTimerMin = 600;
   public int MildColdSneezeTimerMax = 800;
   public int ColdSneezeTimerMin = 300;
   public int ColdSneezeTimerMax = 600;
   public int NastyColdSneezeTimerMin = 200;
   public int NastyColdSneezeTimerMax = 300;
   public int SneezeCoughActive = 0;
   public int SneezeCoughTime = 0;
   public int SneezeCoughDelay = 25;
   public float UnhappynessLevel = 0.0F;
   public float ColdDamageStage = 0.0F;
   public IsoGameCharacter ParentChar;
   private float FoodSicknessLevel = 0.0F;
   private int RemotePainLevel;
   private float Temperature = 37.0F;
   private float lastTemperature = 37.0F;
   private float PoisonLevel = 0.0F;
   private boolean reduceFakeInfection = false;
   private float painReduction = 0.0F;
   private float coldReduction = 0.0F;
   private float discomfortLevel = 0.0F;
   private Thermoregulator thermoregulator;
   public static final float InfectionLevelToZombify = 0.001F;
   private boolean WasDraggingCorpse;
   private boolean StartedDraggingCorpse;
   static String behindStr = "BEHIND";
   static String leftStr = "LEFT";
   static String rightStr = "RIGHT";

   public BodyDamage(IsoGameCharacter var1) {
      this.BodyParts.add(new BodyPart(BodyPartType.Hand_L, var1));
      this.BodyParts.add(new BodyPart(BodyPartType.Hand_R, var1));
      this.BodyParts.add(new BodyPart(BodyPartType.ForeArm_L, var1));
      this.BodyParts.add(new BodyPart(BodyPartType.ForeArm_R, var1));
      this.BodyParts.add(new BodyPart(BodyPartType.UpperArm_L, var1));
      this.BodyParts.add(new BodyPart(BodyPartType.UpperArm_R, var1));
      this.BodyParts.add(new BodyPart(BodyPartType.Torso_Upper, var1));
      this.BodyParts.add(new BodyPart(BodyPartType.Torso_Lower, var1));
      this.BodyParts.add(new BodyPart(BodyPartType.Head, var1));
      this.BodyParts.add(new BodyPart(BodyPartType.Neck, var1));
      this.BodyParts.add(new BodyPart(BodyPartType.Groin, var1));
      this.BodyParts.add(new BodyPart(BodyPartType.UpperLeg_L, var1));
      this.BodyParts.add(new BodyPart(BodyPartType.UpperLeg_R, var1));
      this.BodyParts.add(new BodyPart(BodyPartType.LowerLeg_L, var1));
      this.BodyParts.add(new BodyPart(BodyPartType.LowerLeg_R, var1));
      this.BodyParts.add(new BodyPart(BodyPartType.Foot_L, var1));
      this.BodyParts.add(new BodyPart(BodyPartType.Foot_R, var1));
      Iterator var2 = this.BodyParts.iterator();

      while(var2.hasNext()) {
         BodyPart var3 = (BodyPart)var2.next();
         this.BodyPartsLastState.add(new BodyPartLast());
      }

      this.RestoreToFullHealth();
      this.ParentChar = var1;
      if (this.ParentChar instanceof IsoPlayer) {
         this.thermoregulator = new Thermoregulator(this);
      }

      this.setBodyPartsLastState();
   }

   public BodyPart getBodyPart(BodyPartType var1) {
      return (BodyPart)this.BodyParts.get(BodyPartType.ToIndex(var1));
   }

   public BodyPartLast getBodyPartsLastState(BodyPartType var1) {
      return (BodyPartLast)this.BodyPartsLastState.get(BodyPartType.ToIndex(var1));
   }

   public void setBodyPartsLastState() {
      for(int var1 = 0; var1 < this.getBodyParts().size(); ++var1) {
         BodyPart var2 = (BodyPart)this.getBodyParts().get(var1);
         BodyPartLast var3 = (BodyPartLast)this.BodyPartsLastState.get(var1);
         var3.copy(var2);
      }

   }

   public void load(ByteBuffer var1, int var2) throws IOException {
      for(int var3 = 0; var3 < this.getBodyParts().size(); ++var3) {
         BodyPart var4 = (BodyPart)this.getBodyParts().get(var3);
         var4.SetBitten(var1.get() == 1);
         var4.setScratched(var1.get() == 1, false);
         var4.setBandaged(var1.get() == 1, 0.0F);
         var4.setBleeding(var1.get() == 1);
         var4.setDeepWounded(var1.get() == 1);
         var4.SetFakeInfected(var1.get() == 1);
         var4.SetInfected(var1.get() == 1);
         var4.SetHealth(var1.getFloat());
         if (var4.bandaged()) {
            var4.setBandageLife(var1.getFloat());
         }

         var4.setInfectedWound(var1.get() == 1);
         if (var4.isInfectedWound()) {
            var4.setWoundInfectionLevel(var1.getFloat());
         }

         var4.setBiteTime(var1.getFloat());
         var4.setScratchTime(var1.getFloat());
         var4.setBleedingTime(var1.getFloat());
         var4.setAlcoholLevel(var1.getFloat());
         var4.setAdditionalPain(var1.getFloat());
         var4.setDeepWoundTime(var1.getFloat());
         var4.setHaveGlass(var1.get() == 1);
         var4.setGetBandageXp(var1.get() == 1);
         var4.setStitched(var1.get() == 1);
         var4.setStitchTime(var1.getFloat());
         var4.setGetStitchXp(var1.get() == 1);
         var4.setGetSplintXp(var1.get() == 1);
         var4.setFractureTime(var1.getFloat());
         var4.setSplint(var1.get() == 1, 0.0F);
         if (var4.isSplint()) {
            var4.setSplintFactor(var1.getFloat());
         }

         var4.setHaveBullet(var1.get() == 1, 0);
         var4.setBurnTime(var1.getFloat());
         var4.setNeedBurnWash(var1.get() == 1);
         var4.setLastTimeBurnWash(var1.getFloat());
         var4.setSplintItem(GameWindow.ReadString(var1));
         var4.setBandageType(GameWindow.ReadString(var1));
         var4.setCutTime(var1.getFloat());
         var4.setWetness(var1.getFloat());
         var4.setStiffness(var1.getFloat());
      }

      this.setBodyPartsLastState();
      this.loadMainFields(var1, var2);
      if (var1.get() == 1) {
         if (this.thermoregulator != null) {
            this.thermoregulator.load(var1, var2);
         } else {
            Thermoregulator var5 = new Thermoregulator(this);
            var5.load(var1, var2);
            DebugLog.log("Couldnt load Thermoregulator, == null");
         }
      }

   }

   public void save(ByteBuffer var1) throws IOException {
      for(int var2 = 0; var2 < this.getBodyParts().size(); ++var2) {
         BodyPart var3 = (BodyPart)this.getBodyParts().get(var2);
         var1.put((byte)(var3.bitten() ? 1 : 0));
         var1.put((byte)(var3.scratched() ? 1 : 0));
         var1.put((byte)(var3.bandaged() ? 1 : 0));
         var1.put((byte)(var3.bleeding() ? 1 : 0));
         var1.put((byte)(var3.deepWounded() ? 1 : 0));
         var1.put((byte)(var3.IsFakeInfected() ? 1 : 0));
         var1.put((byte)(var3.IsInfected() ? 1 : 0));
         var1.putFloat(var3.getHealth());
         if (var3.bandaged()) {
            var1.putFloat(var3.getBandageLife());
         }

         var1.put((byte)(var3.isInfectedWound() ? 1 : 0));
         if (var3.isInfectedWound()) {
            var1.putFloat(var3.getWoundInfectionLevel());
         }

         var1.putFloat(var3.getBiteTime());
         var1.putFloat(var3.getScratchTime());
         var1.putFloat(var3.getBleedingTime());
         var1.putFloat(var3.getAlcoholLevel());
         var1.putFloat(var3.getAdditionalPain());
         var1.putFloat(var3.getDeepWoundTime());
         var1.put((byte)(var3.haveGlass() ? 1 : 0));
         var1.put((byte)(var3.isGetBandageXp() ? 1 : 0));
         var1.put((byte)(var3.stitched() ? 1 : 0));
         var1.putFloat(var3.getStitchTime());
         var1.put((byte)(var3.isGetStitchXp() ? 1 : 0));
         var1.put((byte)(var3.isGetSplintXp() ? 1 : 0));
         var1.putFloat(var3.getFractureTime());
         var1.put((byte)(var3.isSplint() ? 1 : 0));
         if (var3.isSplint()) {
            var1.putFloat(var3.getSplintFactor());
         }

         var1.put((byte)(var3.haveBullet() ? 1 : 0));
         var1.putFloat(var3.getBurnTime());
         var1.put((byte)(var3.isNeedBurnWash() ? 1 : 0));
         var1.putFloat(var3.getLastTimeBurnWash());
         GameWindow.WriteString(var1, var3.getSplintItem());
         GameWindow.WriteString(var1, var3.getBandageType());
         var1.putFloat(var3.getCutTime());
         var1.putFloat(var3.getWetness());
         var1.putFloat(var3.getStiffness());
      }

      this.saveMainFields(var1);
      var1.put((byte)(this.thermoregulator != null ? 1 : 0));
      if (this.thermoregulator != null) {
         this.thermoregulator.save(var1);
      }

   }

   public void saveMainFields(ByteBuffer var1) {
      var1.putFloat(this.InfectionLevel);
      var1.putFloat(this.getFakeInfectionLevel());
      var1.putFloat(this.getWetness());
      var1.putFloat(this.getCatchACold());
      var1.put((byte)(this.isHasACold() ? 1 : 0));
      var1.putFloat(this.getColdStrength());
      var1.putFloat(this.getUnhappynessLevel());
      var1.putFloat(this.getBoredomLevel());
      var1.putFloat(this.getFoodSicknessLevel());
      var1.putFloat(this.PoisonLevel);
      var1.putFloat(this.Temperature);
      var1.put((byte)(this.isReduceFakeInfection() ? 1 : 0));
      var1.putFloat(this.HealthFromFoodTimer);
      var1.putFloat(this.painReduction);
      var1.putFloat(this.coldReduction);
      var1.putFloat(this.InfectionTime);
      var1.putFloat(this.InfectionMortalityDuration);
      var1.putFloat(this.ColdDamageStage);
      var1.putFloat(this.discomfortLevel);
   }

   public void loadMainFields(ByteBuffer var1, int var2) {
      this.setInfectionLevel(var1.getFloat());
      this.setFakeInfectionLevel(var1.getFloat());
      this.setWetness(var1.getFloat());
      this.setCatchACold(var1.getFloat());
      this.setHasACold(var1.get() == 1);
      this.setColdStrength(var1.getFloat());
      this.setUnhappynessLevel(var1.getFloat());
      this.setBoredomLevel(var1.getFloat());
      this.setFoodSicknessLevel(var1.getFloat());
      this.PoisonLevel = var1.getFloat();
      float var3 = var1.getFloat();
      this.setTemperature(var3);
      this.setReduceFakeInfection(var1.get() == 1);
      this.setHealthFromFoodTimer(var1.getFloat());
      this.painReduction = var1.getFloat();
      this.coldReduction = var1.getFloat();
      this.InfectionTime = var1.getFloat();
      this.InfectionMortalityDuration = var1.getFloat();
      this.ColdDamageStage = var1.getFloat();
      if (var2 >= 211) {
         this.setDiscomfortLevel(var1.getFloat());
      }

      this.calculateOverallHealth();
   }

   public boolean IsFakeInfected() {
      return this.isIsFakeInfected();
   }

   public void OnFire(boolean var1) {
      this.setIsOnFire(var1);
   }

   public boolean IsOnFire() {
      return this.isIsOnFire();
   }

   public boolean WasBurntToDeath() {
      return this.isBurntToDeath();
   }

   public void IncreasePanicFloat(float var1) {
      float var2 = 1.0F;
      if (this.getParentChar().getBetaEffect() > 0.0F) {
         var2 -= this.getParentChar().getBetaDelta();
         if (var2 > 1.0F) {
            var2 = 1.0F;
         }

         if (var2 < 0.0F) {
            var2 = 0.0F;
         }
      }

      if (this.getParentChar().getCharacterTraits().Cowardly.isSet()) {
         var2 *= 2.0F;
      }

      if (this.getParentChar().getCharacterTraits().Brave.isSet()) {
         var2 *= 0.3F;
      }

      if (this.getParentChar().getCharacterTraits().Desensitized.isSet()) {
         var2 *= 0.15F;
      }

      Stats var10000 = this.ParentChar.getStats();
      var10000.Panic += this.getPanicIncreaseValueFrame() * var1 * var2;
      if (this.getParentChar().getStats().Panic > 100.0F) {
         this.ParentChar.getStats().Panic = 100.0F;
      }

   }

   public void IncreasePanic(int var1) {
      if (this.getParentChar().getVehicle() != null) {
         var1 /= 2;
      }

      float var2 = 1.0F;
      if (this.getParentChar().getBetaEffect() > 0.0F) {
         var2 -= this.getParentChar().getBetaDelta();
         if (var2 > 1.0F) {
            var2 = 1.0F;
         }

         if (var2 < 0.0F) {
            var2 = 0.0F;
         }
      }

      if (this.getParentChar().getCharacterTraits().Cowardly.isSet()) {
         var2 *= 2.0F;
      }

      if (this.getParentChar().getCharacterTraits().Brave.isSet()) {
         var2 *= 0.3F;
      }

      if (this.getParentChar().getCharacterTraits().Desensitized.isSet()) {
         var2 *= 0.15F;
      }

      Stats var10000 = this.ParentChar.getStats();
      var10000.Panic += this.getPanicIncreaseValue() * (float)var1 * var2;
      if (this.getParentChar().getStats().Panic > 100.0F) {
         this.ParentChar.getStats().Panic = 100.0F;
      }

   }

   public void ReducePanic() {
      if (!(this.ParentChar.getStats().Panic <= 0.0F)) {
         float var1 = this.getPanicReductionValue() * GameTime.getInstance().getThirtyFPSMultiplier();
         int var2 = PZMath.fastfloor((double)GameTime.instance.getNightsSurvived() / 30.0);
         if (var2 > 5) {
            var2 = 5;
         }

         var1 += this.getPanicReductionValue() * (float)var2;
         if (this.ParentChar.isAsleep()) {
            var1 *= 2.0F;
         }

         Stats var10000 = this.ParentChar.getStats();
         var10000.Panic -= var1;
         if (this.getParentChar().getStats().Panic < 0.0F) {
            this.ParentChar.getStats().Panic = 0.0F;
         }

      }
   }

   public void UpdateDraggingCorpse() {
      IsoGameCharacter var1 = this.getParentChar();
      boolean var2 = var1.isDraggingCorpse();
      if (var2 != this.getWasDraggingCorpse()) {
         this.StartedDraggingCorpse = var2;
         this.setWasDraggingCorpse(var2);
      } else {
         this.StartedDraggingCorpse = false;
      }

   }

   public void UpdatePanicState() {
      IsoGameCharacter var1 = this.getParentChar();
      int var2 = var1.getStats().NumVisibleZombies;
      int var3 = this.getOldNumZombiesVisible();
      this.setOldNumZombiesVisible(var2);
      int var4 = var2 - var3;
      int var5 = 0;
      if (var4 > 0) {
         var5 += var4;
      }

      if (this.StartedDraggingCorpse) {
      }

      if (var5 > 0) {
         this.IncreasePanic(var5);
      } else {
         this.ReducePanic();
      }

   }

   public void JustDrankBooze(Food var1, float var2) {
      float var3 = 1.0F;
      if (this.getParentChar().Traits.HeavyDrinker.isSet()) {
         var3 = 0.3F;
      }

      if (this.getParentChar().Traits.LightDrinker.isSet()) {
         var3 = 4.0F;
      }

      if (var1.getBaseHunger() != 0.0F) {
         var2 = var1.getHungChange() * var2 / var1.getBaseHunger() * 2.0F;
      }

      var3 *= var2;
      if (var1.getName().toLowerCase().contains("beer") || var1.hasTag("LowAlcohol")) {
         var3 *= 0.25F;
      }

      if ((double)this.getParentChar().getStats().hunger > 0.8) {
         var3 = (float)((double)var3 * 1.25);
      } else if ((double)this.getParentChar().getStats().hunger > 0.6) {
         var3 = (float)((double)var3 * 1.1);
      }

      Stats var10000 = this.ParentChar.getStats();
      var10000.Drunkenness += this.getDrunkIncreaseValue() * var3;
      if (this.getParentChar().getStats().Drunkenness > 100.0F) {
         this.ParentChar.getStats().Drunkenness = 100.0F;
      }

      this.getParentChar().SleepingTablet(0.02F * var2);
      this.getParentChar().BetaAntiDepress(0.4F * var2);
      this.getParentChar().BetaBlockers(0.2F * var2);
      this.getParentChar().PainMeds(0.2F * var2);
   }

   public void JustDrankBoozeFluid(float var1) {
      float var2 = 1.0F;
      if (this.getParentChar().Traits.HeavyDrinker.isSet()) {
         var2 = 0.3F;
      }

      if (this.getParentChar().Traits.LightDrinker.isSet()) {
         var2 = 4.0F;
      }

      var2 *= var1;
      if ((double)this.getParentChar().getStats().hunger > 0.8) {
         var2 *= 1.1F;
      } else if ((double)this.getParentChar().getStats().hunger > 0.6) {
         var2 *= 1.25F;
      }

      Stats var10000 = this.ParentChar.getStats();
      var10000.Drunkenness += this.getDrunkIncreaseValue() * var2;
      if (this.getParentChar().getStats().Drunkenness > 100.0F) {
         this.ParentChar.getStats().Drunkenness = 100.0F;
      }

      this.getParentChar().SleepingTablet(0.02F * var1);
      this.getParentChar().BetaAntiDepress(0.4F * var1);
      this.getParentChar().BetaBlockers(0.2F * var1);
      this.getParentChar().PainMeds(0.2F * var1);
   }

   public void JustTookPill(InventoryItem var1) {
      if ("PillsBeta".equals(var1.getType())) {
         if (this.getParentChar() != null && this.getParentChar().getStats().Drunkenness > 10.0F) {
            this.getParentChar().BetaBlockers(0.15F);
         } else {
            this.getParentChar().BetaBlockers(0.3F);
         }
      } else if ("PillsAntiDep".equals(var1.getType())) {
         if (this.getParentChar() != null && this.getParentChar().getStats().Drunkenness > 10.0F) {
            this.getParentChar().BetaAntiDepress(0.15F);
         } else {
            this.getParentChar().BetaAntiDepress(0.3F);
         }
      } else if ("PillsSleepingTablets".equals(var1.getType())) {
         this.getParentChar().SleepingTablet(0.1F);
         if (this.getParentChar() instanceof IsoPlayer) {
            ((IsoPlayer)this.getParentChar()).setSleepingPillsTaken(((IsoPlayer)this.getParentChar()).getSleepingPillsTaken() + 1);
         }
      } else if ("Pills".equals(var1.getType())) {
         if (this.getParentChar() != null && this.getParentChar().getStats().Drunkenness > 10.0F) {
            this.getParentChar().PainMeds(0.15F);
         } else {
            this.getParentChar().PainMeds(0.45F);
         }
      } else if ("PillsVitamins".equals(var1.getType())) {
         Stats var10000;
         if (this.getParentChar() != null && this.getParentChar().getStats().Drunkenness > 10.0F) {
            var10000 = this.getParentChar().getStats();
            var10000.fatigue += var1.getFatigueChange() / 2.0F;
         } else {
            var10000 = this.getParentChar().getStats();
            var10000.fatigue += var1.getFatigueChange();
         }

         var10000 = this.getParentChar().getStats();
         var10000.stress += var1.getStressChange();
      }

      DrainableComboItem var2 = (DrainableComboItem)var1;
      Object var3 = LuaManager.getFunctionObject(var2.getOnEat());
      if (var3 != null) {
         LuaManager.caller.pcallvoid(LuaManager.thread, var3, var1, this.ParentChar);
      }

      var1.UseAndSync();
   }

   public void JustAteFood(Food var1, float var2) {
      this.JustAteFood(var1, var2, false);
   }

   public void JustAteFood(Food var1, float var2, boolean var3) {
      Stats var10000;
      float var4;
      String var5;
      if (var1.getPoisonPower() > 0) {
         var4 = (float)var1.getPoisonPower() * var2;
         if (this.getParentChar().Traits.IronGut.isSet() && var1.getType() != "Bleach") {
            var4 /= 2.0F;
         }

         if (this.getParentChar().Traits.WeakStomach.isSet()) {
            var4 *= 2.0F;
         }

         this.PoisonLevel += var4;
         var10000 = this.ParentChar.getStats();
         var10000.Pain += (float)var1.getPoisonPower() * var2 / 6.0F;
         if (this.ParentChar instanceof IsoPlayer) {
            var5 = String.format("Player %s just ate poisoned food %s with poison power %f", ((IsoPlayer)this.ParentChar).getDisplayName(), var1.getDisplayName(), var4);
            DebugLog.Objects.debugln(var5);
            LoggerManager.getLogger("user").write(var5);
         }
      }

      if (var1.isTainted()) {
         var4 = 20.0F * var2;
         this.PoisonLevel += var4;
         var10000 = this.ParentChar.getStats();
         var10000.Pain += 10.0F * var2 / 6.0F;
         if (this.ParentChar instanceof IsoPlayer) {
            var5 = String.format("Player %s just ate tainted food %s with poison power %f", ((IsoPlayer)this.ParentChar).getDisplayName(), var1.getDisplayName(), var4);
            DebugLog.Objects.debugln(var5);
            LoggerManager.getLogger("user").write(var5);
         }
      }

      if (var1.getReduceInfectionPower() > 0.0F) {
         this.getParentChar().setReduceInfectionPower(var1.getReduceInfectionPower());
      }

      var4 = 1.0F;
      if (var3) {
         if (var1.getBoredomChange() * var2 < 0.0F) {
            var4 = 1.25F;
         } else {
            var4 = 0.75F;
         }

         DebugLog.log("boredomChange %modifier from using an eating utensil: " + var4);
      }

      this.setBoredomLevel(this.getBoredomLevel() + var1.getBoredomChange() * var2 * var4);
      if (this.getBoredomLevel() < 0.0F) {
         this.setBoredomLevel(0.0F);
      }

      var4 = 1.0F;
      if (var3) {
         if (var1.getUnhappyChange() * var2 < 0.0F) {
            var4 = 1.25F;
         } else {
            var4 = 0.75F;
         }

         DebugLog.log("unhappyChange %modifier from using an eating utensil: " + var4);
      }

      this.setUnhappynessLevel(this.getUnhappynessLevel() + var1.getUnhappyChange() * var2 * var4);
      if (this.getUnhappynessLevel() < 0.0F) {
         this.setUnhappynessLevel(0.0F);
      }

      if (var1.isAlcoholic()) {
         this.JustDrankBooze(var1, var2);
      }

      float var9;
      if (this.getParentChar().getStats().hunger <= 0.0F) {
         var9 = Math.abs(var1.getHungerChange()) * var2;
         this.setHealthFromFoodTimer((float)((int)(this.getHealthFromFoodTimer() + var9 * this.getHealthFromFoodTimeByHunger())));
         if (var1.isCooked()) {
            this.setHealthFromFoodTimer((float)((int)(this.getHealthFromFoodTimer() + var9 * this.getHealthFromFoodTimeByHunger())));
         }

         if (this.getHealthFromFoodTimer() > 11000.0F) {
            this.setHealthFromFoodTimer(11000.0F);
         }
      }

      if (!"Tutorial".equals(Core.getInstance().getGameMode())) {
         if (!var1.isCooked() && var1.isbDangerousUncooked()) {
            this.setHealthFromFoodTimer(0.0F);
            int var10 = 75;
            if (var1.hasTag("Egg")) {
               var10 = 5;
            }

            if (this.getParentChar().Traits.IronGut.isSet()) {
               var10 /= 2;
               if (var1.hasTag("Egg")) {
                  var10 = 0;
               }
            }

            if (this.getParentChar().Traits.WeakStomach.isSet()) {
               var10 *= 2;
            }

            if (var10 > 0 && !this.isInfected()) {
               if (var1.isBurnt()) {
                  this.PoisonLevel += 4.0F * var2;
               } else {
                  this.PoisonLevel += 15.0F * var2;
               }
            }
         }

         if (var1.getAge() >= (float)var1.getOffAgeMax()) {
            var9 = var1.getAge() - (float)var1.getOffAgeMax();
            if (var9 == 0.0F) {
               var9 = 1.0F;
            }

            if (var9 > 5.0F) {
               var9 = 5.0F;
            }

            int var6;
            if (var1.getOffAgeMax() > var1.getOffAge()) {
               var6 = (int)(var9 / (float)(var1.getOffAgeMax() - var1.getOffAge()) * 100.0F);
            } else {
               var6 = 100;
            }

            if (this.getParentChar().Traits.IronGut.isSet()) {
               var6 /= 2;
            }

            if (this.getParentChar().Traits.WeakStomach.isSet()) {
               var6 *= 2;
            }

            if (!this.isInfected()) {
               if (Rand.Next(100) < var6) {
                  float var7 = 5.0F * Math.abs(var1.getHungChange() * 10.0F) * var2;
                  this.PoisonLevel += var7;
                  if (this.ParentChar instanceof IsoPlayer) {
                     String var8 = String.format("Player %s just ate spoiled food %s with poison power %f", ((IsoPlayer)this.ParentChar).getDisplayName(), var1.getDisplayName(), var7);
                     DebugLog.Objects.debugln(var8);
                     LoggerManager.getLogger("user").write(var8);
                  }
               } else {
                  this.PoisonLevel += 2.0F * Math.abs(var1.getHungChange() * 10.0F) * var2;
               }
            }
         }

      }
   }

   public void JustAteFood(Food var1) {
      this.JustAteFood(var1, 100.0F);
   }

   private float getHealthFromFoodTimeByHunger() {
      return 13000.0F;
   }

   public void JustReadSomething(Literature var1) {
      this.setBoredomLevel(this.getBoredomLevel() + var1.getBoredomChange());
      if (this.getBoredomLevel() < 0.0F) {
         this.setBoredomLevel(0.0F);
      }

      this.setUnhappynessLevel(this.getUnhappynessLevel() + var1.getUnhappyChange());
      if (this.getUnhappynessLevel() < 0.0F) {
         this.setUnhappynessLevel(0.0F);
      }

   }

   public void JustTookPainMeds() {
      Stats var10000 = this.ParentChar.getStats();
      var10000.Pain -= this.getPainReductionFromMeds();
      if (this.getParentChar().getStats().Pain < 0.0F) {
         this.ParentChar.getStats().Pain = 0.0F;
      }

   }

   public void UpdateWetness() {
      IsoGridSquare var1 = this.getParentChar().getCurrentSquare();
      BaseVehicle var2 = this.getParentChar().getVehicle();
      IsoGameCharacter var3 = this.getParentChar();
      boolean var4 = var1 == null || !var1.isInARoom() && !var1.haveRoof;
      if (var2 != null && var2.hasRoof(var2.getSeat(this.getParentChar()))) {
         var4 = false;
      }

      ClothingWetness var5 = this.getParentChar().getClothingWetness();
      float var6 = 0.0F;
      float var7 = 0.0F;
      float var8 = 0.0F;
      if (var2 != null && ClimateManager.getInstance().isRaining()) {
         VehiclePart var9 = var2.getPartById("Windshield");
         if (var9 != null) {
            VehicleWindow var10 = var9.getWindow();
            if (var10 != null && var10.isDestroyed()) {
               float var11 = ClimateManager.getInstance().getRainIntensity();
               var11 *= var11;
               var11 *= var2.getCurrentSpeedKmHour() / 50.0F;
               if (var11 < 0.1F) {
                  var11 = 0.0F;
               }

               if (var11 > 1.0F) {
                  var11 = 1.0F;
               }

               var8 = var11 * 3.0F;
               var6 = var11;
            }
         }
      }

      if (var4 && (var3.isAsleep() || var3.isResting()) && var3.getBed() != null && var3.getBed().isTent()) {
         var4 = false;
      }

      float var12;
      float var13;
      if (var4 && ClimateManager.getInstance().isRaining()) {
         var12 = ClimateManager.getInstance().getRainIntensity();
         if ((double)var12 < 0.1) {
            var12 = 0.0F;
         }

         var6 = var12;
      } else if (!var4 || !ClimateManager.getInstance().isRaining()) {
         var12 = ClimateManager.getInstance().getAirTemperatureForCharacter(this.getParentChar());
         var13 = 0.1F;
         if (var12 > 5.0F) {
            var13 += (var12 - 5.0F) / 10.0F;
         }

         var13 -= var8;
         if (var13 < 0.0F) {
            var13 = 0.0F;
         }

         var7 = var13;
      }

      if (var5 != null) {
         var5.updateWetness(var6, var7);
      }

      var12 = 0.0F;
      if (this.BodyParts.size() > 0) {
         for(int var14 = 0; var14 < this.BodyParts.size(); ++var14) {
            var12 += ((BodyPart)this.BodyParts.get(var14)).getWetness();
         }

         var12 /= (float)this.BodyParts.size();
      }

      this.Wetness = PZMath.clamp(var12, 0.0F, 100.0F);
      var13 = 0.0F;
      if (this.thermoregulator != null) {
         var13 = this.thermoregulator.getCatchAColdDelta();
      }

      if (!this.isHasACold() && var13 > 0.1F) {
         if (this.getParentChar().Traits.ProneToIllness.isSet()) {
            var13 *= 1.7F;
         }

         if (this.getParentChar().Traits.Resilient.isSet()) {
            var13 *= 0.45F;
         }

         if (this.getParentChar().Traits.Outdoorsman.isSet()) {
            var13 *= 0.25F;
         }

         this.setCatchACold(this.getCatchACold() + (float)ZomboidGlobals.CatchAColdIncreaseRate * var13 * GameTime.instance.getMultiplier());
         if (this.getCatchACold() >= 100.0F) {
            this.setCatchACold(0.0F);
            this.setHasACold(true);
            this.setColdStrength(20.0F);
            this.setTimeToSneezeOrCough(0);
         }
      }

      if (var13 <= 0.1F) {
         this.setCatchACold(this.getCatchACold() - (float)ZomboidGlobals.CatchAColdDecreaseRate);
         if (this.getCatchACold() <= 0.0F) {
            this.setCatchACold(0.0F);
         }
      }

   }

   public void TriggerSneezeCough() {
      if (this.getSneezeCoughActive() <= 0) {
         boolean var1 = this.getParentChar().getMoodles().getMoodleLevel(MoodleType.HasACold) < 1 && this.getParentChar().Traits.Smoker.isSet();
         if (Rand.Next(100) > 50 && !var1) {
            this.setSneezeCoughActive(1);
         } else {
            this.setSneezeCoughActive(2);
         }

         if (this.getParentChar().getMoodles().getMoodleLevel(MoodleType.HasACold) == 2) {
            this.setSneezeCoughActive(1);
         }

         this.setSneezeCoughTime(this.getSneezeCoughDelay());
         if (this.getParentChar().getMoodles().getMoodleLevel(MoodleType.HasACold) == 4) {
            this.setTimeToSneezeOrCough(this.getNastyColdSneezeTimerMin() + Rand.Next(this.getNastyColdSneezeTimerMax() - this.getNastyColdSneezeTimerMin()));
         } else if (this.getParentChar().getMoodles().getMoodleLevel(MoodleType.HasACold) == 3) {
            this.setTimeToSneezeOrCough(this.getColdSneezeTimerMin() + Rand.Next(this.getColdSneezeTimerMax() - this.getColdSneezeTimerMin()));
         } else if (this.getParentChar().getMoodles().getMoodleLevel(MoodleType.HasACold) == 2) {
            this.setTimeToSneezeOrCough(this.getMildColdSneezeTimerMin() + Rand.Next(this.getMildColdSneezeTimerMax() - this.getMildColdSneezeTimerMin()));
         } else if (var1) {
            this.setTimeToSneezeOrCough(this.getSmokerSneezeTimerMin() + Rand.Next(this.getSmokerSneezeTimerMax() - this.getSmokerSneezeTimerMin()));
         }

         boolean var2 = false;
         if (this.getParentChar().getPrimaryHandItem() != null && (this.getParentChar().getPrimaryHandItem().getType().equals("Tissue") || this.getParentChar().getPrimaryHandItem().getType().equals("ToiletPaper") || this.getParentChar().getPrimaryHandItem().hasTag("MuffleSneeze"))) {
            if (this.getParentChar().getPrimaryHandItem().getCurrentUses() > 0) {
               this.getParentChar().getPrimaryHandItem().setCurrentUses(this.getParentChar().getPrimaryHandItem().getCurrentUses() - 1);
               if (this.getParentChar().getPrimaryHandItem().getCurrentUses() <= 0) {
                  this.getParentChar().getPrimaryHandItem().Use();
               }

               var2 = true;
            }
         } else if (this.getParentChar().getSecondaryHandItem() != null && (this.getParentChar().getSecondaryHandItem().getType().equals("Tissue") || this.getParentChar().getSecondaryHandItem().getType().equals("ToiletPaper") || this.getParentChar().getSecondaryHandItem().hasTag("MuffleSneeze")) && this.getParentChar().getSecondaryHandItem().getCurrentUses() > 0) {
            this.getParentChar().getSecondaryHandItem().setCurrentUses(this.getParentChar().getSecondaryHandItem().getCurrentUses() - 1);
            if (this.getParentChar().getSecondaryHandItem().getCurrentUses() <= 0) {
               this.getParentChar().getSecondaryHandItem().Use();
            }

            var2 = true;
         }

         if (var2) {
            this.setSneezeCoughActive(this.getSneezeCoughActive() + 2);
         } else {
            byte var3 = 20;
            byte var4 = 20;
            if (this.getSneezeCoughActive() == 1) {
               var3 = 20;
               var4 = 25;
            }

            if (this.getSneezeCoughActive() == 2) {
               var3 = 35;
               var4 = 40;
            }

            WorldSoundManager.instance.addSound(this.getParentChar(), PZMath.fastfloor(this.getParentChar().getX()), PZMath.fastfloor(this.getParentChar().getY()), PZMath.fastfloor(this.getParentChar().getZ()), var3, var4, true);
         }

      }
   }

   public int IsSneezingCoughing() {
      return this.getSneezeCoughActive();
   }

   public void UpdateCold() {
      if (this.isHasACold()) {
         boolean var1 = true;
         IsoGridSquare var2 = this.getParentChar().getCurrentSquare();
         if (var2 == null || !var2.isInARoom() || this.getParentChar().getMoodles().getMoodleLevel(MoodleType.Wet) > 0 || this.getParentChar().getMoodles().getMoodleLevel(MoodleType.Hypothermia) >= 1 || this.getParentChar().getStats().fatigue > 0.5F || this.getParentChar().getStats().hunger > 0.25F || this.getParentChar().getStats().thirst > 0.25F) {
            var1 = false;
         }

         if (this.getColdReduction() > 0.0F) {
            var1 = true;
            this.setColdReduction(this.getColdReduction() - 0.005F * GameTime.instance.getMultiplier());
            if (this.getColdReduction() < 0.0F) {
               this.setColdReduction(0.0F);
            }
         }

         float var3;
         if (var1) {
            var3 = 1.0F;
            if (this.getParentChar().Traits.ProneToIllness.isSet()) {
               var3 = 0.5F;
            }

            if (this.getParentChar().Traits.Resilient.isSet()) {
               var3 = 1.5F;
            }

            this.setColdStrength(this.getColdStrength() - this.getColdProgressionRate() * var3 * GameTime.instance.getMultiplier());
            if (this.getColdReduction() > 0.0F) {
               this.setColdStrength(this.getColdStrength() - this.getColdProgressionRate() * var3 * GameTime.instance.getMultiplier());
            }

            if (this.getColdStrength() < 0.0F) {
               this.setColdStrength(0.0F);
               this.setHasACold(false);
               this.setCatchACold(0.0F);
            }
         } else {
            var3 = 1.0F;
            if (this.getParentChar().Traits.ProneToIllness.isSet()) {
               var3 = 1.2F;
            }

            if (this.getParentChar().Traits.Resilient.isSet()) {
               var3 = 0.8F;
            }

            this.setColdStrength(this.getColdStrength() + this.getColdProgressionRate() * var3 * GameTime.instance.getMultiplier());
            if (this.getColdStrength() > 100.0F) {
               this.setColdStrength(100.0F);
            }
         }

         if (this.getSneezeCoughTime() > 0) {
            this.setSneezeCoughTime(this.getSneezeCoughTime() - 1);
            if (this.getSneezeCoughTime() == 0) {
               this.setSneezeCoughActive(0);
            }
         }

         if (this.getParentChar().getMoodles().getMoodleLevel(MoodleType.HasACold) > 1 && this.getTimeToSneezeOrCough() >= 0 && !this.ParentChar.IsSpeaking()) {
            this.setTimeToSneezeOrCough(this.getTimeToSneezeOrCough() - 1);
            if (this.getTimeToSneezeOrCough() <= 0) {
               this.TriggerSneezeCough();
            }
         }
      } else if (this.getParentChar().Traits.Smoker.isSet() && this.getTimeToSneezeOrCough() >= 0 && !this.ParentChar.IsSpeaking()) {
         this.setTimeToSneezeOrCough(this.getTimeToSneezeOrCough() - 1);
         if (this.getTimeToSneezeOrCough() <= 0) {
            this.TriggerSneezeCough();
         }
      }

   }

   public float getColdStrength() {
      return this.isHasACold() ? this.ColdStrength : 0.0F;
   }

   public float getWetness() {
      return this.Wetness;
   }

   public void AddDamage(BodyPartType var1, float var2) {
      ((BodyPart)this.getBodyParts().get(BodyPartType.ToIndex(var1))).AddDamage(var2);
   }

   public void AddGeneralHealth(float var1) {
      int var2 = 0;

      for(int var3 = 0; var3 < BodyPartType.ToIndex(BodyPartType.MAX); ++var3) {
         if (((BodyPart)this.getBodyParts().get(var3)).getHealth() < 100.0F) {
            ++var2;
         }
      }

      if (var2 > 0) {
         float var5 = var1 / (float)var2;

         for(int var4 = 0; var4 < BodyPartType.ToIndex(BodyPartType.MAX); ++var4) {
            if (((BodyPart)this.getBodyParts().get(var4)).getHealth() < 100.0F) {
               ((BodyPart)this.getBodyParts().get(var4)).AddHealth(var5);
            }
         }
      }

   }

   public void ReduceGeneralHealth(float var1) {
      if (this.getOverallBodyHealth() <= 10.0F) {
         this.getParentChar().forceAwake();
      }

      if (!(var1 <= 0.0F)) {
         float var2 = var1 / (float)BodyPartType.ToIndex(BodyPartType.MAX);

         for(int var3 = 0; var3 < BodyPartType.ToIndex(BodyPartType.MAX); ++var3) {
            ((BodyPart)this.getBodyParts().get(var3)).ReduceHealth(var2 / BodyPartType.getDamageModifyer(var3));
         }

      }
   }

   public void AddDamage(int var1, float var2) {
      ((BodyPart)this.getBodyParts().get(var1)).AddDamage(var2);
   }

   public void splatBloodFloorBig() {
      this.getParentChar().splatBloodFloorBig();
      this.getParentChar().splatBloodFloorBig();
      this.getParentChar().splatBloodFloorBig();
   }

   private static boolean isSpikedPart(IsoGameCharacter var0, IsoGameCharacter var1, int var2) {
      boolean var3;
      if (!var0.isAimAtFloor()) {
         var3 = var0.isBehind(var1);
      } else {
         var3 = var1.isFallOnFront();
      }

      boolean var4;
      if (var3) {
         var4 = var1.bodyPartIsSpikedBehind(var2);
      } else {
         var4 = var1.bodyPartIsSpiked(var2);
      }

      return var4;
   }

   public static void damageFromSpikedArmor(IsoGameCharacter var0, IsoGameCharacter var1, int var2, HandWeapon var3) {
      boolean var4 = var0 instanceof IsoLivingCharacter && ((IsoLivingCharacter)var0).isDoShove();
      if (var0 != null && (var4 || WeaponType.getWeaponType(var3) == WeaponType.knife)) {
         boolean var5 = isSpikedPart(var0, var1, var2);
         boolean var6 = var5 && var0.isAimAtFloor() && var4;
         boolean var7 = var5 && !var6 && (var0.getPrimaryHandItem() == null || var0.getPrimaryHandItem() instanceof HandWeapon);
         boolean var8 = var5 && !var6 && (var0.getSecondaryHandItem() == null || var0.getSecondaryHandItem() instanceof HandWeapon) && var4;
         if (var6) {
            var1.addBlood(BloodBodyPartType.FromIndex(var2), true, false, false);
            var0.spikePart(BodyPartType.Foot_R);
         }

         if (var7) {
            var1.addBlood(BloodBodyPartType.FromIndex(var2), true, false, false);
            var0.spikePart(BodyPartType.Hand_R);
         }

         if (var8) {
            var1.addBlood(BloodBodyPartType.FromIndex(var2), true, false, false);
            var0.spikePart(BodyPartType.Hand_L);
         }
      }

   }

   public void applyDamageFromWeapon(int var1, float var2, int var3, float var4) {
      BodyPart var5 = this.getBodyPart(BodyPartType.FromIndex(var1));
      switch (var3) {
         case 1:
            var5.generateDeepWound();
            break;
         case 2:
         case 4:
            var5.setCut(true);
            break;
         case 3:
         case 5:
            var5.setScratched(true, true);
            break;
         case 6:
            var5.setHaveBullet(true, 0);
      }

      this.AddDamage(var1, var2);
      this.getParentChar().getStats().setPain(Math.min(this.getParentChar().getStats().getPain() + var4, 100.0F));
      IsoPlayer var6 = (IsoPlayer)Type.tryCastTo(this.getParentChar(), IsoPlayer.class);
      if (var6 != null) {
         if (GameServer.bServer) {
            INetworkPacket.send(var6, PacketTypes.PacketType.PlayerDamageFromWeapon, var6, var1, var2, var3, var4);
         } else if (GameClient.bClient && var6.isLocalPlayer()) {
            var6.updateMovementRates();
            GameClient.sendPlayerInjuries(var6);
            GameClient.sendPlayerDamage(var6);
         }
      }

   }

   public void DamageFromWeapon(HandWeapon var1, int var2) {
      if (GameClient.bClient) {
         IsoPlayer var3 = (IsoPlayer)Type.tryCastTo(this.getParentChar(), IsoPlayer.class);
         if (var3 != null && !var3.isLocalPlayer()) {
            return;
         }
      }

      byte var11 = 0;
      boolean var4 = false;
      boolean var5 = false;
      boolean var6 = false;
      if (!var1.getCategories().contains("Blunt") && !var1.getCategories().contains("SmallBlunt")) {
         if (!var1.isAimedFirearm()) {
            var5 = true;
         } else {
            var6 = true;
         }
      } else {
         var4 = true;
      }

      if (var2 == -1) {
         var2 = Rand.Next(BodyPartType.ToIndex(BodyPartType.Hand_L), BodyPartType.ToIndex(BodyPartType.MAX));
      }

      BodyPart var7 = this.getBodyPart(BodyPartType.FromIndex(var2));
      float var8 = this.getParentChar().getBodyPartClothingDefense(var7.getIndex(), var5, var6);
      if ((float)Rand.Next(100) < var8) {
         IsoPlayer var12 = var1.getUsingPlayer();
         if (var12 != null && WeaponType.getWeaponType(var1) == WeaponType.knife && !var1.hasTag("Handguard")) {
            boolean var13 = isSpikedPart(var12, this.getParentChar(), var2);
            if (var13) {
               this.getParentChar().addBlood(BloodBodyPartType.FromIndex(var2), true, false, false);
               var12.spikePart(BodyPartType.Hand_R);
            }
         }

         this.getParentChar().addHoleFromZombieAttacks(BloodBodyPartType.FromIndex(var2), false);
      } else {
         this.getParentChar().addHole(BloodBodyPartType.FromIndex(var2));
         this.getParentChar().splatBloodFloorBig();
         this.getParentChar().splatBloodFloorBig();
         this.getParentChar().splatBloodFloorBig();
         float var9 = 0.0F;
         if (var5) {
            if (Rand.NextBool(6)) {
               var11 = 1;
               var7.generateDeepWound();
            } else if (Rand.NextBool(3)) {
               var11 = 2;
               var7.setCut(true);
            } else {
               var11 = 3;
               var7.setScratched(true, true);
            }

            var9 = this.getInitialScratchPain() * BodyPartType.getPainModifyer(var2);
         } else if (var4) {
            if (Rand.NextBool(4)) {
               var11 = 4;
               var7.setCut(true);
            } else {
               var11 = 5;
               var7.setScratched(true, true);
            }

            var9 = this.getInitialThumpPain() * BodyPartType.getPainModifyer(var2);
         } else if (var6) {
            var11 = 6;
            var7.setHaveBullet(true, 0);
            var9 = this.getInitialBitePain() * BodyPartType.getPainModifyer(var2);
         }

         float var10 = Rand.Next(var1.getMinDamage(), var1.getMaxDamage()) * 15.0F;
         if (var2 == BodyPartType.ToIndex(BodyPartType.Head)) {
            var10 *= 4.0F;
         }

         if (var2 == BodyPartType.ToIndex(BodyPartType.Neck)) {
            var10 *= 4.0F;
         }

         if (var2 == BodyPartType.ToIndex(BodyPartType.Torso_Upper)) {
            var10 *= 2.0F;
         }

         if (GameClient.bClient) {
            if (var1.isRanged()) {
               var10 = (float)((double)var10 * ServerOptions.getInstance().PVPFirearmDamageModifier.getValue());
            } else {
               var10 = (float)((double)var10 * ServerOptions.getInstance().PVPMeleeDamageModifier.getValue());
            }
         }

         damageFromSpikedArmor(var1.getUsingPlayer(), this.getParentChar(), var2, var1);
         this.applyDamageFromWeapon(var2, var10, var11, var9);
      }
   }

   public boolean AddRandomDamageFromZombie(IsoZombie var1, String var2) {
      if (StringUtils.isNullOrEmpty(var2)) {
         var2 = "Bite";
      }

      this.getParentChar().setVariable("hitpvp", false);
      if (GameServer.bServer) {
         this.getParentChar().sendObjectChange("AddRandomDamageFromZombie", new Object[]{"zombie", var1.OnlineID});
         return true;
      } else {
         byte var3 = 0;
         boolean var4 = false;
         int var5 = 15 + this.getParentChar().getMeleeCombatMod();
         int var6 = 85;
         int var7 = 65;
         String var8 = this.getParentChar().testDotSide(var1);
         boolean var9 = var8.equals(behindStr);
         boolean var10 = var8.equals(leftStr) || var8.equals(rightStr);
         int var11 = this.getParentChar().getSurroundingAttackingZombies();
         var11 = Math.max(var11, 1);
         var5 -= (var11 - 1) * 10;
         var6 -= (var11 - 1) * 30;
         var7 -= (var11 - 1) * 15;
         byte var12 = 3;
         if (SandboxOptions.instance.Lore.Strength.getValue() == 1) {
            var12 = 2;
         }

         if (SandboxOptions.instance.Lore.Strength.getValue() == 3) {
            var12 = 6;
         }

         if (this.ParentChar.Traits.ThickSkinned.isSet()) {
            var5 = (int)((double)var5 * 1.3);
         }

         if (this.ParentChar.Traits.ThinSkinned.isSet()) {
            var5 = (int)((double)var5 / 1.3);
         }

         int var13 = this.getParentChar().getSurroundingAttackingZombies(SandboxOptions.instance.Lore.ZombiesCrawlersDragDown.getValue());
         if (!"EndDeath".equals(this.getParentChar().getHitReaction())) {
            if (!this.getParentChar().isGodMod() && var13 >= var12 && SandboxOptions.instance.Lore.ZombiesDragDown.getValue() && !this.getParentChar().isSitOnGround()) {
               var6 = 0;
               var7 = 0;
               var5 = 0;
               this.getParentChar().setHitReaction("EndDeath");
               this.getParentChar().setDeathDragDown(true);
            } else {
               this.getParentChar().setHitReaction(var2);
            }
         }

         if (var9) {
            var5 -= 15;
            var6 -= 25;
            var7 -= 35;
            if (SandboxOptions.instance.RearVulnerability.getValue() == 1) {
               var5 += 15;
               var6 += 25;
               var7 += 35;
            }

            if (SandboxOptions.instance.RearVulnerability.getValue() == 2) {
               var5 += 7;
               var6 += 17;
               var7 += 23;
            }

            if (var11 > 2) {
               var6 -= 15;
               var7 -= 15;
            }
         }

         if (var10) {
            var5 -= 30;
            var6 -= 7;
            var7 -= 27;
            if (SandboxOptions.instance.RearVulnerability.getValue() == 1) {
               var5 += 30;
               var6 += 7;
               var7 += 27;
            }

            if (SandboxOptions.instance.RearVulnerability.getValue() == 2) {
               var5 += 15;
               var6 += 4;
               var7 += 15;
            }
         }

         float var14;
         boolean var16;
         int var22;
         if (!var1.bCrawling) {
            if (Rand.Next(10) == 0) {
               var22 = Rand.Next(BodyPartType.ToIndex(BodyPartType.Hand_L), BodyPartType.ToIndex(BodyPartType.Groin) + 1);
            } else {
               var22 = Rand.Next(BodyPartType.ToIndex(BodyPartType.Hand_L), BodyPartType.ToIndex(BodyPartType.Neck) + 1);
            }

            var14 = 10.0F * (float)var11;
            if (var9) {
               var14 += 5.0F;
            }

            if (var10) {
               var14 += 2.0F;
            }

            if (var9 && (float)Rand.Next(100) < var14) {
               var22 = BodyPartType.ToIndex(BodyPartType.Neck);
            }

            if (var22 == BodyPartType.ToIndex(BodyPartType.Head) || var22 == BodyPartType.ToIndex(BodyPartType.Neck)) {
               byte var15 = 70;
               if (var9) {
                  var15 = 90;
               }

               if (var10) {
                  var15 = 80;
               }

               if (Rand.Next(100) > var15) {
                  var16 = false;

                  label282:
                  while(true) {
                     do {
                        if (var16) {
                           break label282;
                        }

                        var16 = true;
                        var22 = Rand.Next(BodyPartType.ToIndex(BodyPartType.Torso_Lower) + 1);
                     } while(var22 != BodyPartType.ToIndex(BodyPartType.Head) && var22 != BodyPartType.ToIndex(BodyPartType.Neck) && var22 != BodyPartType.ToIndex(BodyPartType.Groin));

                     var16 = false;
                  }
               }
            }
         } else {
            if (Rand.Next(2) != 0) {
               return false;
            }

            if (Rand.Next(10) == 0) {
               var22 = Rand.Next(BodyPartType.ToIndex(BodyPartType.Groin), BodyPartType.ToIndex(BodyPartType.MAX));
            } else {
               var22 = Rand.Next(BodyPartType.ToIndex(BodyPartType.UpperLeg_L), BodyPartType.ToIndex(BodyPartType.MAX));
            }
         }

         if (var1.inactive) {
            var5 += 20;
            var6 += 20;
            var7 += 20;
         }

         var14 = (float)Rand.Next(1000) / 1000.0F;
         var14 *= (float)(Rand.Next(10) + 10);
         if (GameServer.bServer && this.ParentChar instanceof IsoPlayer || Core.bDebug && this.ParentChar instanceof IsoPlayer) {
            DebugLog.DetailedInfo.trace("zombie did " + var14 + " dmg to " + ((IsoPlayer)this.ParentChar).getDisplayName() + " on body part " + BodyPartType.getDisplayName(BodyPartType.FromIndex(var22)));
         }

         boolean var23 = false;
         var16 = true;
         boolean var17 = var9 || this.getParentChar().isFallOnFront();
         if (Rand.Next(100) > var5) {
            boolean var18 = false;
            if (var17) {
               var18 = this.getParentChar().bodyPartIsSpikedBehind(var22);
            } else {
               var18 = this.getParentChar().bodyPartIsSpiked(var22);
            }

            var1.scratch = true;
            this.getParentChar().helmetFall(var22 == BodyPartType.ToIndex(BodyPartType.Neck) || var22 == BodyPartType.ToIndex(BodyPartType.Head));
            if (Rand.Next(100) > var7) {
               var1.scratch = false;
               var1.laceration = true;
            }

            if (Rand.Next(100) > var6 && !var1.cantBite()) {
               var1.scratch = false;
               var1.laceration = false;
               var16 = false;
            }

            Float var19;
            boolean var20;
            IsoPlayer var21;
            if (var1.scratch) {
               var19 = this.getParentChar().getBodyPartClothingDefense(var22, false, false);
               var1.parameterZombieState.setState(ParameterZombieState.State.AttackScratch);
               if (this.getHealth() > 0.0F) {
                  this.getParentChar().getEmitter().playSoundImpl("ZombieScratch", (IsoObject)null);
               }

               if (this.getHealth() > 0.0F && var18) {
                  if (Rand.NextBool(2)) {
                     this.getParentChar().addBlood(BloodBodyPartType.FromIndex(var22), true, false, false);
                     var1.spikePart(BodyPartType.Hand_L);
                  } else {
                     this.getParentChar().addBlood(BloodBodyPartType.FromIndex(var22), true, false, false);
                     var1.spikePart(BodyPartType.Hand_R);
                  }
               }

               if ((float)Rand.Next(100) < var19) {
                  this.getParentChar().addHoleFromZombieAttacks(BloodBodyPartType.FromIndex(var22), var16);
                  return false;
               }

               var20 = this.getParentChar().addHole(BloodBodyPartType.FromIndex(var22), true);
               if (var20) {
                  this.getParentChar().getEmitter().playSoundImpl("ZombieRipClothing", (IsoObject)null);
               }

               var23 = true;
               this.AddDamage(var22, var14);
               this.SetScratched(var22, true);
               this.getParentChar().addBlood(BloodBodyPartType.FromIndex(var22), true, false, true);
               var3 = 1;
               if (GameServer.bServer && this.ParentChar instanceof IsoPlayer) {
                  DebugLog.DetailedInfo.trace("zombie scratched " + ((IsoPlayer)this.ParentChar).username);
               }

               var21 = (IsoPlayer)Type.tryCastTo(this.getParentChar(), IsoPlayer.class);
               if (var21 != null) {
                  var21.playerVoiceSound("PainFromScratch");
               }
            } else if (var1.laceration) {
               var19 = this.getParentChar().getBodyPartClothingDefense(var22, false, false);
               var1.parameterZombieState.setState(ParameterZombieState.State.AttackLacerate);
               if (this.getHealth() > 0.0F) {
                  this.getParentChar().getEmitter().playSoundImpl("ZombieScratch", (IsoObject)null);
               }

               if (this.getHealth() > 0.0F && var18) {
                  if (Rand.NextBool(2)) {
                     this.getParentChar().addBlood(BloodBodyPartType.FromIndex(var22), true, false, false);
                     var1.spikePart(BodyPartType.Hand_L);
                  } else {
                     this.getParentChar().addBlood(BloodBodyPartType.FromIndex(var22), true, false, false);
                     var1.spikePart(BodyPartType.Hand_R);
                  }
               }

               if ((float)Rand.Next(100) < var19) {
                  this.getParentChar().addHoleFromZombieAttacks(BloodBodyPartType.FromIndex(var22), var16);
                  return false;
               }

               var20 = this.getParentChar().addHole(BloodBodyPartType.FromIndex(var22), true);
               if (var20) {
                  this.getParentChar().getEmitter().playSoundImpl("ZombieRipClothing", (IsoObject)null);
               }

               var23 = true;
               this.AddDamage(var22, var14);
               this.SetCut(var22, true);
               this.getParentChar().addBlood(BloodBodyPartType.FromIndex(var22), true, false, true);
               var3 = 1;
               if (GameServer.bServer && this.ParentChar instanceof IsoPlayer) {
                  DebugLog.DetailedInfo.trace("zombie laceration " + ((IsoPlayer)this.ParentChar).username);
               }

               var21 = (IsoPlayer)Type.tryCastTo(this.getParentChar(), IsoPlayer.class);
               if (var21 != null) {
                  var21.playerVoiceSound("PainFromLacerate");
               }
            } else {
               var19 = this.getParentChar().getBodyPartClothingDefense(var22, true, false);
               var1.parameterZombieState.setState(ParameterZombieState.State.AttackBite);
               if (this.getHealth() > 0.0F) {
                  String var25 = var1.getBiteSoundName();
                  if (var22 == BodyPartType.ToIndex(BodyPartType.Neck)) {
                     var25 = "NeckBite";
                  }

                  this.getParentChar().getEmitter().playSoundImpl(var25, (IsoObject)null);
               }

               if ((float)Rand.Next(100) < var19) {
                  this.getParentChar().addHoleFromZombieAttacks(BloodBodyPartType.FromIndex(var22), var16);
                  if (var18) {
                     this.getParentChar().addBlood(BloodBodyPartType.FromIndex(var22), false, true, false);
                     var1.spikePart(BodyPartType.Head);
                  }

                  return false;
               }

               var20 = this.getParentChar().addHole(BloodBodyPartType.FromIndex(var22), true);
               if (var20) {
                  this.getParentChar().getEmitter().playSoundImpl("ZombieRipClothing", (IsoObject)null);
               }

               var23 = true;
               this.AddDamage(var22, var14);
               this.SetBitten(var22, true);
               var21 = (IsoPlayer)Type.tryCastTo(this.getParentChar(), IsoPlayer.class);
               if (var21 != null) {
                  var21.playerVoiceSound("PainFromBite");
               }

               if (var22 == BodyPartType.ToIndex(BodyPartType.Neck)) {
                  this.getParentChar().addBlood(BloodBodyPartType.FromIndex(var22), false, true, true);
                  this.getParentChar().addBlood(BloodBodyPartType.FromIndex(var22), false, true, true);
                  this.getParentChar().addBlood(BloodBodyPartType.Torso_Upper, false, true, false);
                  this.getParentChar().splatBloodFloorBig();
                  this.getParentChar().splatBloodFloorBig();
                  this.getParentChar().splatBloodFloorBig();
               }

               this.getParentChar().addBlood(BloodBodyPartType.FromIndex(var22), false, true, true);
               if (this.ParentChar instanceof IsoPlayer) {
                  String var10001 = ((IsoPlayer)this.ParentChar).username;
                  DebugLog.DetailedInfo.trace("zombie bite " + var10001 + " in body location " + BloodBodyPartType.FromIndex(var22));
               }

               var3 = 2;
               this.getParentChar().splatBloodFloorBig();
               this.getParentChar().splatBloodFloorBig();
               this.getParentChar().splatBloodFloorBig();
               if (var18) {
                  this.getParentChar().addBlood(BloodBodyPartType.FromIndex(var22), false, true, false);
                  var1.spikePart(BodyPartType.Head);
                  var1.Kill((IsoGameCharacter)null);
               }
            }
         }

         if (!var23) {
            this.getParentChar().addHoleFromZombieAttacks(BloodBodyPartType.FromIndex(var22), var16);
         }

         Stats var10000;
         switch (var3) {
            case 0:
               var10000 = this.ParentChar.getStats();
               var10000.Pain += this.getInitialThumpPain() * BodyPartType.getPainModifyer(var22);
               break;
            case 1:
               var10000 = this.ParentChar.getStats();
               var10000.Pain += this.getInitialScratchPain() * BodyPartType.getPainModifyer(var22);
               break;
            case 2:
               var10000 = this.ParentChar.getStats();
               var10000.Pain += this.getInitialBitePain() * BodyPartType.getPainModifyer(var22);
         }

         if (this.getParentChar().getStats().Pain > 100.0F) {
            this.ParentChar.getStats().Pain = 100.0F;
         }

         if (this.ParentChar instanceof IsoPlayer && GameClient.bClient && ((IsoPlayer)this.ParentChar).isLocalPlayer()) {
            IsoPlayer var24 = (IsoPlayer)this.ParentChar;
            var24.updateMovementRates();
            GameClient.sendPlayerInjuries(var24);
            GameClient.sendPlayerDamage(var24);
         }

         return true;
      }
   }

   public boolean doesBodyPartHaveInjury(BodyPartType var1) {
      return ((BodyPart)this.getBodyParts().get(BodyPartType.ToIndex(var1))).HasInjury();
   }

   public boolean doBodyPartsHaveInjuries(BodyPartType var1, BodyPartType var2) {
      return this.doesBodyPartHaveInjury(var1) || this.doesBodyPartHaveInjury(var2);
   }

   public boolean isBodyPartBleeding(BodyPartType var1) {
      return this.getBodyPart(var1).getBleedingTime() > 0.0F;
   }

   public boolean areBodyPartsBleeding(BodyPartType var1, BodyPartType var2) {
      return this.isBodyPartBleeding(var1) || this.isBodyPartBleeding(var2);
   }

   public void DrawUntexturedQuad(int var1, int var2, int var3, int var4, float var5, float var6, float var7, float var8) {
      SpriteRenderer.instance.renderi((Texture)null, var1, var2, var3, var4, var5, var6, var7, var8, (Consumer)null);
   }

   public float getBodyPartHealth(BodyPartType var1) {
      return ((BodyPart)this.getBodyParts().get(BodyPartType.ToIndex(var1))).getHealth();
   }

   public float getBodyPartHealth(int var1) {
      return ((BodyPart)this.getBodyParts().get(var1)).getHealth();
   }

   public String getBodyPartName(BodyPartType var1) {
      return BodyPartType.ToString(var1);
   }

   public String getBodyPartName(int var1) {
      return BodyPartType.ToString(BodyPartType.FromIndex(var1));
   }

   public float getHealth() {
      return this.getOverallBodyHealth();
   }

   public float getInfectionLevel() {
      return this.InfectionLevel;
   }

   public float getApparentInfectionLevel() {
      float var1 = Math.max(this.getFakeInfectionLevel(), this.InfectionLevel);
      return (double)this.getFoodSicknessLevel() * 0.49 > (double)var1 ? (float)((int)((double)this.getFoodSicknessLevel() * 0.49)) : var1;
   }

   public int getNumPartsBleeding() {
      int var1 = 0;

      for(int var2 = 0; var2 < BodyPartType.ToIndex(BodyPartType.MAX); ++var2) {
         if (((BodyPart)this.getBodyParts().get(var2)).bleeding()) {
            ++var1;
         }
      }

      return var1;
   }

   public int getNumPartsScratched() {
      int var1 = 0;

      for(int var2 = 0; var2 < BodyPartType.ToIndex(BodyPartType.MAX); ++var2) {
         if (((BodyPart)this.getBodyParts().get(var2)).scratched()) {
            ++var1;
         }
      }

      return var1;
   }

   public int getNumPartsBitten() {
      int var1 = 0;

      for(int var2 = 0; var2 < BodyPartType.ToIndex(BodyPartType.MAX); ++var2) {
         if (((BodyPart)this.getBodyParts().get(var2)).bitten()) {
            ++var1;
         }
      }

      return var1;
   }

   public boolean HasInjury() {
      for(int var1 = 0; var1 < BodyPartType.ToIndex(BodyPartType.MAX); ++var1) {
         if (((BodyPart)this.getBodyParts().get(var1)).HasInjury()) {
            return true;
         }
      }

      return false;
   }

   public boolean IsBandaged(BodyPartType var1) {
      return ((BodyPart)this.getBodyParts().get(BodyPartType.ToIndex(var1))).bandaged();
   }

   public boolean IsDeepWounded(BodyPartType var1) {
      return ((BodyPart)this.getBodyParts().get(BodyPartType.ToIndex(var1))).deepWounded();
   }

   public boolean IsBandaged(int var1) {
      return ((BodyPart)this.getBodyParts().get(var1)).bandaged();
   }

   public boolean IsBitten(BodyPartType var1) {
      return ((BodyPart)this.getBodyParts().get(BodyPartType.ToIndex(var1))).bitten();
   }

   public boolean IsBitten(int var1) {
      return ((BodyPart)this.getBodyParts().get(var1)).bitten();
   }

   public boolean IsBleeding(BodyPartType var1) {
      return ((BodyPart)this.getBodyParts().get(BodyPartType.ToIndex(var1))).bleeding();
   }

   public boolean IsBleeding(int var1) {
      return ((BodyPart)this.getBodyParts().get(var1)).bleeding();
   }

   public boolean IsBleedingStemmed(BodyPartType var1) {
      return ((BodyPart)this.getBodyParts().get(BodyPartType.ToIndex(var1))).IsBleedingStemmed();
   }

   public boolean IsBleedingStemmed(int var1) {
      return ((BodyPart)this.getBodyParts().get(var1)).IsBleedingStemmed();
   }

   public boolean IsCauterized(BodyPartType var1) {
      return ((BodyPart)this.getBodyParts().get(BodyPartType.ToIndex(var1))).IsCauterized();
   }

   public boolean IsCauterized(int var1) {
      return ((BodyPart)this.getBodyParts().get(var1)).IsCauterized();
   }

   public boolean IsInfected() {
      return this.IsInfected;
   }

   public boolean IsInfected(BodyPartType var1) {
      return ((BodyPart)this.getBodyParts().get(BodyPartType.ToIndex(var1))).IsInfected();
   }

   public boolean IsInfected(int var1) {
      return ((BodyPart)this.getBodyParts().get(var1)).IsInfected();
   }

   public boolean IsFakeInfected(int var1) {
      return ((BodyPart)this.getBodyParts().get(var1)).IsFakeInfected();
   }

   public void DisableFakeInfection(int var1) {
      ((BodyPart)this.getBodyParts().get(var1)).DisableFakeInfection();
   }

   public boolean IsScratched(BodyPartType var1) {
      return ((BodyPart)this.getBodyParts().get(BodyPartType.ToIndex(var1))).scratched();
   }

   public boolean IsCut(BodyPartType var1) {
      return ((BodyPart)this.getBodyParts().get(BodyPartType.ToIndex(var1))).getCutTime() > 0.0F;
   }

   public boolean IsScratched(int var1) {
      return ((BodyPart)this.getBodyParts().get(var1)).scratched();
   }

   public boolean IsStitched(BodyPartType var1) {
      return ((BodyPart)this.getBodyParts().get(BodyPartType.ToIndex(var1))).stitched();
   }

   public boolean IsStitched(int var1) {
      return ((BodyPart)this.getBodyParts().get(var1)).stitched();
   }

   public boolean IsWounded(BodyPartType var1) {
      return ((BodyPart)this.getBodyParts().get(BodyPartType.ToIndex(var1))).deepWounded();
   }

   public boolean IsWounded(int var1) {
      return ((BodyPart)this.getBodyParts().get(var1)).deepWounded();
   }

   public void RestoreToFullHealth() {
      for(int var1 = 0; var1 < BodyPartType.ToIndex(BodyPartType.MAX); ++var1) {
         ((BodyPart)this.getBodyParts().get(var1)).RestoreToFullHealth();
      }

      if (this.getParentChar() != null && this.getParentChar().getStats() != null) {
         this.getParentChar().getStats().resetStats();
      }

      this.setInfected(false);
      this.setIsFakeInfected(false);
      this.setOverallBodyHealth(100.0F);
      this.setInfectionLevel(0.0F);
      this.setFakeInfectionLevel(0.0F);
      this.setBoredomLevel(0.0F);
      this.setWetness(0.0F);
      this.setCatchACold(0.0F);
      this.setHasACold(false);
      this.setColdStrength(0.0F);
      this.setSneezeCoughActive(0);
      this.setSneezeCoughTime(0);
      this.setTemperature(37.0F);
      this.setUnhappynessLevel(0.0F);
      this.PoisonLevel = 0.0F;
      this.setFoodSicknessLevel(0.0F);
      this.Temperature = 37.0F;
      this.lastTemperature = this.Temperature;
      this.setInfectionTime(-1.0F);
      this.setInfectionMortalityDuration(-1.0F);
      this.setDiscomfortLevel(0.0F);
      if (this.thermoregulator != null) {
         this.thermoregulator.reset();
      }

      MusicIntensityConfig.getInstance().restoreToFullHealth(this.getParentChar());
   }

   public void SetBandaged(int var1, boolean var2, float var3, boolean var4, String var5) {
      ((BodyPart)this.getBodyParts().get(var1)).setBandaged(var2, var3, var4, var5);
   }

   public void SetBitten(BodyPartType var1, boolean var2) {
      ((BodyPart)this.getBodyParts().get(BodyPartType.ToIndex(var1))).SetBitten(var2);
   }

   public void SetBitten(int var1, boolean var2) {
      ((BodyPart)this.getBodyParts().get(var1)).SetBitten(var2);
   }

   public void SetBitten(int var1, boolean var2, boolean var3) {
      ((BodyPart)this.getBodyParts().get(var1)).SetBitten(var2, var3);
   }

   public void SetBleeding(BodyPartType var1, boolean var2) {
      ((BodyPart)this.getBodyParts().get(BodyPartType.ToIndex(var1))).setBleeding(var2);
   }

   public void SetBleeding(int var1, boolean var2) {
      ((BodyPart)this.getBodyParts().get(var1)).setBleeding(var2);
   }

   public void SetBleedingStemmed(BodyPartType var1, boolean var2) {
      ((BodyPart)this.getBodyParts().get(BodyPartType.ToIndex(var1))).SetBleedingStemmed(var2);
   }

   public void SetBleedingStemmed(int var1, boolean var2) {
      ((BodyPart)this.getBodyParts().get(var1)).SetBleedingStemmed(var2);
   }

   public void SetCauterized(BodyPartType var1, boolean var2) {
      ((BodyPart)this.getBodyParts().get(BodyPartType.ToIndex(var1))).SetCauterized(var2);
   }

   public void SetCauterized(int var1, boolean var2) {
      ((BodyPart)this.getBodyParts().get(var1)).SetCauterized(var2);
   }

   public BodyPart setScratchedWindow() {
      int var1 = Rand.Next(BodyPartType.ToIndex(BodyPartType.Hand_L), BodyPartType.ToIndex(BodyPartType.ForeArm_R) + 1);
      this.getBodyPart(BodyPartType.FromIndex(var1)).AddDamage(10.0F);
      this.getBodyPart(BodyPartType.FromIndex(var1)).SetScratchedWindow(true);
      return this.getBodyPart(BodyPartType.FromIndex(var1));
   }

   public void SetScratched(BodyPartType var1, boolean var2) {
      ((BodyPart)this.getBodyParts().get(BodyPartType.ToIndex(var1))).setScratched(var2, false);
   }

   public void SetScratched(int var1, boolean var2) {
      ((BodyPart)this.getBodyParts().get(var1)).setScratched(var2, false);
   }

   public void SetScratchedFromWeapon(int var1, boolean var2) {
      ((BodyPart)this.getBodyParts().get(var1)).SetScratchedWeapon(var2);
   }

   public void SetCut(int var1, boolean var2) {
      ((BodyPart)this.getBodyParts().get(var1)).setCut(var2, false);
   }

   public void SetWounded(BodyPartType var1, boolean var2) {
      ((BodyPart)this.getBodyParts().get(BodyPartType.ToIndex(var1))).setDeepWounded(var2);
   }

   public void SetWounded(int var1, boolean var2) {
      ((BodyPart)this.getBodyParts().get(var1)).setDeepWounded(var2);
   }

   public void ShowDebugInfo() {
      if (this.getDamageModCount() > 0) {
         this.setDamageModCount(this.getDamageModCount() - 1);
      }

   }

   public void UpdateBoredom() {
      if (!(this.getParentChar() instanceof IsoSurvivor)) {
         if (!(this.getParentChar() instanceof IsoPlayer) || !this.getParentChar().Asleep) {
            if (this.getParentChar().getCurrentSquare().isInARoom()) {
               if (!this.getParentChar().isReading()) {
                  this.setBoredomLevel((float)((double)this.getBoredomLevel() + ZomboidGlobals.BoredomIncreaseRate * (double)GameTime.instance.getMultiplier()));
               } else {
                  this.setBoredomLevel((float)((double)this.getBoredomLevel() + ZomboidGlobals.BoredomIncreaseRate / 5.0 * (double)GameTime.instance.getMultiplier()));
               }

               if (this.getParentChar().IsSpeaking() && !this.getParentChar().callOut) {
                  this.setBoredomLevel((float)((double)this.getBoredomLevel() - ZomboidGlobals.BoredomDecreaseRate * (double)GameTime.instance.getMultiplier()));
               }

               if (this.getParentChar().getNumSurvivorsInVicinity() > 0) {
                  this.setBoredomLevel((float)((double)this.getBoredomLevel() - ZomboidGlobals.BoredomDecreaseRate * 0.10000000149011612 * (double)GameTime.instance.getMultiplier()));
               }
            } else if (this.getParentChar().getVehicle() != null) {
               float var1 = this.getParentChar().getVehicle().getCurrentSpeedKmHour();
               if (Math.abs(var1) <= 0.1F) {
                  if (this.getParentChar().isReading()) {
                     this.setBoredomLevel((float)((double)this.getBoredomLevel() + ZomboidGlobals.BoredomIncreaseRate / 5.0 * (double)GameTime.instance.getMultiplier()));
                  } else {
                     this.setBoredomLevel((float)((double)this.getBoredomLevel() + ZomboidGlobals.BoredomIncreaseRate * (double)GameTime.instance.getMultiplier()));
                  }
               } else {
                  this.setBoredomLevel((float)((double)this.getBoredomLevel() - ZomboidGlobals.BoredomDecreaseRate * 0.5 * (double)GameTime.instance.getMultiplier()));
               }
            } else {
               this.setBoredomLevel((float)((double)this.getBoredomLevel() - ZomboidGlobals.BoredomDecreaseRate * 0.10000000149011612 * (double)GameTime.instance.getMultiplier()));
            }

            if (this.getParentChar().getStats().Drunkenness > 20.0F) {
               this.setBoredomLevel((float)((double)this.getBoredomLevel() - ZomboidGlobals.BoredomDecreaseRate * 2.0 * (double)GameTime.instance.getMultiplier()));
            }

            if (this.getParentChar().getStats().Panic > 5.0F) {
               this.setBoredomLevel(0.0F);
            }

            if (this.getBoredomLevel() > 100.0F) {
               this.setBoredomLevel(100.0F);
            }

            if (this.getBoredomLevel() < 0.0F) {
               this.setBoredomLevel(0.0F);
            }

            if (this.getUnhappynessLevel() > 100.0F) {
               this.setUnhappynessLevel(100.0F);
            }

            if (this.getUnhappynessLevel() < 0.0F) {
               this.setUnhappynessLevel(0.0F);
            }

            if (this.getParentChar().getMoodles().getMoodleLevel(MoodleType.Bored) > 1 && !this.getParentChar().isReading()) {
               this.setUnhappynessLevel((float)((double)this.getUnhappynessLevel() + ZomboidGlobals.UnhappinessIncrease * (double)((float)this.getParentChar().getMoodles().getMoodleLevel(MoodleType.Bored)) * (double)GameTime.instance.getMultiplier()));
            }

            if (this.getParentChar().getMoodles().getMoodleLevel(MoodleType.Stress) > 1 && !this.getParentChar().isReading()) {
               this.setUnhappynessLevel((float)((double)this.getUnhappynessLevel() + ZomboidGlobals.UnhappinessIncrease / 2.0 * (double)((float)this.getParentChar().getMoodles().getMoodleLevel(MoodleType.Stress)) * (double)GameTime.instance.getMultiplier()));
            }

            if (this.getParentChar().Traits.Smoker.isSet()) {
               this.getParentChar().setTimeSinceLastSmoke(this.getParentChar().getTimeSinceLastSmoke() + 1.0E-4F * GameTime.instance.getMultiplier());
               if (this.getParentChar().getTimeSinceLastSmoke() > 1.0F) {
                  double var3 = (double)(PZMath.fastfloor(this.getParentChar().getTimeSinceLastSmoke() / 10.0F) + 1);
                  if (var3 > 10.0) {
                     var3 = 10.0;
                  }

                  this.getParentChar().getStats().setStressFromCigarettes((float)((double)this.getParentChar().getStats().getStressFromCigarettes() + ZomboidGlobals.StressFromBiteOrScratch / 8.0 * var3 * (double)GameTime.instance.getMultiplier()));
               }
            }

         }
      }
   }

   public float getUnhappynessLevel() {
      return this.UnhappynessLevel;
   }

   public float getBoredomLevel() {
      return this.BoredomLevel;
   }

   public void UpdateStrength() {
      if (this.getParentChar() == this.getParentChar()) {
         int var1 = 0;
         if (this.getParentChar().getMoodles().getMoodleLevel(MoodleType.Hungry) == 2) {
            ++var1;
         }

         if (this.getParentChar().getMoodles().getMoodleLevel(MoodleType.Hungry) == 3) {
            var1 += 2;
         }

         if (this.getParentChar().getMoodles().getMoodleLevel(MoodleType.Hungry) == 4) {
            var1 += 2;
         }

         if (this.getParentChar().getMoodles().getMoodleLevel(MoodleType.Thirst) == 2) {
            ++var1;
         }

         if (this.getParentChar().getMoodles().getMoodleLevel(MoodleType.Thirst) == 3) {
            var1 += 2;
         }

         if (this.getParentChar().getMoodles().getMoodleLevel(MoodleType.Thirst) == 4) {
            var1 += 2;
         }

         if (this.getParentChar().getMoodles().getMoodleLevel(MoodleType.Sick) == 2) {
            ++var1;
         }

         if (this.getParentChar().getMoodles().getMoodleLevel(MoodleType.Sick) == 3) {
            var1 += 2;
         }

         if (this.getParentChar().getMoodles().getMoodleLevel(MoodleType.Sick) == 4) {
            var1 += 3;
         }

         if (this.getParentChar().getMoodles().getMoodleLevel(MoodleType.Bleeding) == 2) {
            ++var1;
         }

         if (this.getParentChar().getMoodles().getMoodleLevel(MoodleType.Bleeding) == 3) {
            ++var1;
         }

         if (this.getParentChar().getMoodles().getMoodleLevel(MoodleType.Bleeding) == 4) {
            ++var1;
         }

         if (this.getParentChar().getMoodles().getMoodleLevel(MoodleType.Injured) == 2) {
            ++var1;
         }

         if (this.getParentChar().getMoodles().getMoodleLevel(MoodleType.Injured) == 3) {
            var1 += 2;
         }

         if (this.getParentChar().getMoodles().getMoodleLevel(MoodleType.Injured) == 4) {
            var1 += 3;
         }

         this.getParentChar().setMaxWeight((int)((float)this.getParentChar().getMaxWeightBase() * this.getParentChar().getWeightMod()) - var1);
         if (this.getParentChar().getMaxWeight() < 0) {
            this.getParentChar().setMaxWeight(0);
         }

         if (this.getParentChar() instanceof IsoPlayer) {
            this.getParentChar().setMaxWeight((int)((float)this.getParentChar().getMaxWeight() * ((IsoPlayer)this.getParentChar()).getMaxWeightDelta()));
         }

      }
   }

   public float pickMortalityDuration() {
      float var1 = 1.0F;
      if (this.getParentChar().Traits.Resilient.isSet()) {
         var1 = 1.25F;
      }

      if (this.getParentChar().Traits.ProneToIllness.isSet()) {
         var1 = 0.75F;
      }

      switch (SandboxOptions.instance.Lore.Mortality.getValue()) {
         case 1:
            return 0.0F;
         case 2:
            return Rand.Next(0.0F, 30.0F) / 3600.0F * var1;
         case 3:
            return Rand.Next(0.5F, 1.0F) / 60.0F * var1;
         case 4:
            return Rand.Next(3.0F, 12.0F) * var1;
         case 5:
            return Rand.Next(2.0F, 3.0F) * 24.0F * var1;
         case 6:
            return Rand.Next(1.0F, 2.0F) * 7.0F * 24.0F * var1;
         case 7:
            return -1.0F;
         default:
            return -1.0F;
      }
   }

   public void splatBloodFloor() {
      byte var1 = ((IsoPlayer)this.getParentChar()).bleedingLevel;
      if (var1 > 0) {
         float var2 = 1.0F / (float)var1 * 200.0F * GameTime.instance.getInvMultiplier();
         if ((float)Rand.Next((int)var2) < var2 * 0.3F) {
            this.getParentChar().splatBloodFloor();
         }

         if (Rand.Next((int)var2) == 0) {
            this.getParentChar().splatBloodFloor();
         }
      }

   }

   public void Update() {
      if (!(this.getParentChar() instanceof IsoZombie) && !this.getParentChar().isAnimal()) {
         IsoPlayer var1;
         if (GameServer.bServer) {
            var1 = (IsoPlayer)Type.tryCastTo(this.getParentChar(), IsoPlayer.class);
            if (var1 == null || !GameServer.isDelayedDisconnect(var1)) {
               this.splatBloodFloor();
               return;
            }
         } else if (GameClient.bClient) {
            var1 = (IsoPlayer)Type.tryCastTo(this.getParentChar(), IsoPlayer.class);
            if (var1 != null && !var1.isLocalPlayer()) {
               if (this.getParentChar().isAlive()) {
                  this.RestoreToFullHealth();
                  this.splatBloodFloor();
               }

               return;
            }
         }

         if (this.getParentChar().isGodMod()) {
            this.RestoreToFullHealth();
            ((IsoPlayer)this.getParentChar()).bleedingLevel = 0;
         } else if (this.getParentChar().isInvincible()) {
            this.setOverallBodyHealth(100.0F);

            for(int var12 = 0; var12 < BodyPartType.MAX.index(); ++var12) {
               this.getBodyPart(BodyPartType.FromIndex(var12)).SetHealth(100.0F);
            }

         } else {
            float var11 = this.ParentChar.getStats().Pain;
            int var2 = this.getNumPartsBleeding() * 2;
            var2 += this.getNumPartsScratched();
            var2 += this.getNumPartsBitten() * 6;
            if (this.getHealth() >= 60.0F && var2 <= 3) {
               var2 = 0;
            }

            ((IsoPlayer)this.getParentChar()).bleedingLevel = (byte)var2;
            float var3;
            if (var2 > 0) {
               var3 = 1.0F / (float)var2 * 200.0F * GameTime.instance.getInvMultiplier();
               if ((float)Rand.Next((int)var3) < var3 * 0.3F) {
                  this.getParentChar().splatBloodFloor();
               }

               if (Rand.Next((int)var3) == 0) {
                  this.getParentChar().splatBloodFloor();
               }
            }

            if (this.thermoregulator != null) {
               this.thermoregulator.update();
            }

            this.UpdateDraggingCorpse();
            this.UpdateWetness();
            this.UpdateCold();
            this.UpdateBoredom();
            this.UpdateStrength();
            this.UpdatePanicState();
            this.UpdateTemperatureState();
            this.UpdateDiscomfort();
            this.UpdateIllness();
            if (this.getOverallBodyHealth() != 0.0F) {
               if (this.PoisonLevel == 0.0F && this.getFoodSicknessLevel() > 0.0F) {
                  this.setFoodSicknessLevel(this.getFoodSicknessLevel() - (float)(ZomboidGlobals.FoodSicknessDecrease * (double)GameTime.instance.getMultiplier()));
               }

               int var13;
               if (!this.isInfected()) {
                  for(var13 = 0; var13 < BodyPartType.ToIndex(BodyPartType.MAX); ++var13) {
                     if (this.IsInfected(var13)) {
                        this.setInfected(true);
                        if (this.IsFakeInfected(var13)) {
                           this.DisableFakeInfection(var13);
                           this.setInfectionLevel(this.getFakeInfectionLevel());
                           this.setFakeInfectionLevel(0.0F);
                           this.setIsFakeInfected(false);
                           this.setReduceFakeInfection(false);
                        }
                     }
                  }

                  if (this.isInfected() && this.getInfectionTime() < 0.0F && SandboxOptions.instance.Lore.Mortality.getValue() != 7) {
                     this.setInfectionTime(this.getCurrentTimeForInfection());
                     this.setInfectionMortalityDuration(this.pickMortalityDuration());
                  }
               }

               if (!this.isInfected() && !this.isIsFakeInfected()) {
                  for(var13 = 0; var13 < BodyPartType.ToIndex(BodyPartType.MAX); ++var13) {
                     if (this.IsFakeInfected(var13)) {
                        this.setIsFakeInfected(true);
                        break;
                     }
                  }
               }

               if (this.isIsFakeInfected() && !this.isReduceFakeInfection() && this.getParentChar().getReduceInfectionPower() == 0.0F) {
                  this.setFakeInfectionLevel(this.getFakeInfectionLevel() + this.getInfectionGrowthRate() * GameTime.instance.getMultiplier());
                  if (this.getFakeInfectionLevel() > 100.0F) {
                     this.setFakeInfectionLevel(100.0F);
                     this.setReduceFakeInfection(true);
                  }
               }

               Stats var10000 = this.ParentChar.getStats();
               var10000.Drunkenness -= this.getDrunkReductionValue() * GameTime.instance.getMultiplier();
               if (this.getParentChar().getStats().Drunkenness < 0.0F) {
                  this.ParentChar.getStats().Drunkenness = 0.0F;
               }

               var3 = 0.0F;
               if (this.getHealthFromFoodTimer() > 0.0F) {
                  var3 += this.getHealthFromFood() * GameTime.instance.getMultiplier();
                  this.setHealthFromFoodTimer(this.getHealthFromFoodTimer() - 1.0F * GameTime.instance.getMultiplier());
               }

               int var4 = 0;
               if (this.getParentChar() == this.getParentChar() && (this.getParentChar().getMoodles().getMoodleLevel(MoodleType.Hungry) == 2 || this.getParentChar().getMoodles().getMoodleLevel(MoodleType.Sick) == 2 || this.getParentChar().getMoodles().getMoodleLevel(MoodleType.Thirst) == 2)) {
                  var4 = 1;
               }

               if (this.getParentChar() == this.getParentChar() && (this.getParentChar().getMoodles().getMoodleLevel(MoodleType.Hungry) == 3 || this.getParentChar().getMoodles().getMoodleLevel(MoodleType.Sick) == 3 || this.getParentChar().getMoodles().getMoodleLevel(MoodleType.Thirst) == 3)) {
                  var4 = 2;
               }

               if (this.getParentChar() == this.getParentChar() && (this.getParentChar().getMoodles().getMoodleLevel(MoodleType.Hungry) == 4 || this.getParentChar().getMoodles().getMoodleLevel(MoodleType.Thirst) == 4)) {
                  var4 = 3;
               }

               if (this.getParentChar().isAsleep()) {
                  var4 = -1;
               }

               switch (var4) {
                  case 0:
                     var3 += this.getStandardHealthAddition() * GameTime.instance.getMultiplier();
                     break;
                  case 1:
                     var3 += this.getReducedHealthAddition() * GameTime.instance.getMultiplier();
                     break;
                  case 2:
                     var3 += this.getSeverlyReducedHealthAddition() * GameTime.instance.getMultiplier();
                     break;
                  case 3:
                     var3 += 0.0F;
               }

               if (this.getParentChar().isAsleep()) {
                  if (GameClient.bClient) {
                     var3 += 15.0F * GameTime.instance.getGameWorldSecondsSinceLastUpdate() / 3600.0F;
                  } else {
                     var3 += this.getSleepingHealthAddition() * GameTime.instance.getMultiplier();
                  }

                  if (this.getParentChar().getMoodles().getMoodleLevel(MoodleType.Hungry) == 4 || this.getParentChar().getMoodles().getMoodleLevel(MoodleType.Thirst) == 4) {
                     var3 = 0.0F;
                  }
               }

               this.AddGeneralHealth(var3);
               var3 = 0.0F;
               float var14 = 0.0F;
               float var5 = 0.0F;
               float var6 = 0.0F;
               float var7 = 0.0F;
               float var8 = 0.0F;
               float var9 = 0.0F;
               if (this.PoisonLevel > 0.0F) {
                  if (this.PoisonLevel > 10.0F && this.getParentChar().getMoodles().getMoodleLevel(MoodleType.Sick) >= 1) {
                     var14 = 0.0035F * Math.min(this.PoisonLevel / 10.0F, 3.0F) * GameTime.instance.getMultiplier();
                     var3 += var14;
                  }

                  float var10 = 0.0F;
                  if (this.getParentChar().getMoodles().getMoodleLevel(MoodleType.FoodEaten) > 0) {
                     var10 = 1.5E-4F * (float)this.getParentChar().getMoodles().getMoodleLevel(MoodleType.FoodEaten);
                  }

                  this.PoisonLevel = (float)((double)this.PoisonLevel - ((double)var10 + ZomboidGlobals.PoisonLevelDecrease * (double)GameTime.instance.getMultiplier()));
                  if (this.PoisonLevel < 0.0F) {
                     this.PoisonLevel = 0.0F;
                  }

                  this.setFoodSicknessLevel(this.getFoodSicknessLevel() + this.getInfectionGrowthRate() * (float)(2 + Math.round(this.PoisonLevel / 10.0F)) * GameTime.instance.getMultiplier());
                  if (this.getFoodSicknessLevel() > 100.0F) {
                     this.setFoodSicknessLevel(100.0F);
                  }
               }

               if (this.getParentChar().getMoodles().getMoodleLevel(MoodleType.Hungry) == 4) {
                  var5 = this.getHealthReductionFromSevereBadMoodles() / 50.0F * GameTime.instance.getMultiplier();
                  var3 += var5;
               }

               if (this.getParentChar().getMoodles().getMoodleLevel(MoodleType.Sick) == 4) {
                  if (this.FoodSicknessLevel > this.InfectionLevel) {
                     var6 = this.getHealthReductionFromSevereBadMoodles() * GameTime.instance.getMultiplier();
                     var3 += var6;
                  } else if (SandboxOptions.instance.WoundInfectionFactor.getValue() > 0.0 && this.getGeneralWoundInfectionLevel() > this.InfectionLevel) {
                     var6 = this.getHealthReductionFromSevereBadMoodles() * GameTime.instance.getMultiplier();
                     var3 += var6;
                  }
               }

               if (this.getParentChar().getMoodles().getMoodleLevel(MoodleType.Bleeding) == 4) {
                  var7 = this.getHealthReductionFromSevereBadMoodles() * GameTime.instance.getMultiplier();
                  var3 += var7;
               }

               if (this.getParentChar().getMoodles().getMoodleLevel(MoodleType.Thirst) == 4) {
                  var8 = this.getHealthReductionFromSevereBadMoodles() / 10.0F * GameTime.instance.getMultiplier();
                  var3 += var8;
               }

               if (this.getParentChar().getMoodles().getMoodleLevel(MoodleType.HeavyLoad) > 2 && this.getParentChar().getVehicle() == null && !this.getParentChar().isAsleep() && !this.getParentChar().isSitOnGround() && this.getThermoregulator().getMetabolicTarget() != Metabolics.SeatedResting.getMet() && this.getHealth() > 75.0F && Rand.Next(Rand.AdjustForFramerate(10)) == 0) {
                  var9 = this.getHealthReductionFromSevereBadMoodles() / ((float)(5 - this.getParentChar().getMoodles().getMoodleLevel(MoodleType.HeavyLoad)) / 10.0F) * GameTime.instance.getMultiplier();
                  var3 += var9;
                  this.getParentChar().addBackMuscleStrain(var9);
               }

               this.ReduceGeneralHealth(var3);
               IsoGameCharacter var15 = this.getParentChar();
               if (var14 > 0.0F) {
                  LuaEventManager.triggerEvent("OnPlayerGetDamage", var15, "POISON", var14);
               }

               if (var5 > 0.0F) {
                  LuaEventManager.triggerEvent("OnPlayerGetDamage", var15, "HUNGRY", var5);
               }

               if (var6 > 0.0F) {
                  LuaEventManager.triggerEvent("OnPlayerGetDamage", var15, "SICK", var6);
               }

               if (var7 > 0.0F) {
                  LuaEventManager.triggerEvent("OnPlayerGetDamage", var15, "BLEEDING", var7);
               }

               if (var8 > 0.0F) {
                  LuaEventManager.triggerEvent("OnPlayerGetDamage", var15, "THIRST", var8);
               }

               if (var9 > 0.0F) {
                  LuaEventManager.triggerEvent("OnPlayerGetDamage", var15, "HEAVYLOAD", var9);
               }

               if (this.ParentChar.getPainEffect() > 0.0F) {
                  var10000 = this.ParentChar.getStats();
                  var10000.Pain -= 0.023333333F * GameTime.getInstance().getThirtyFPSMultiplier();
                  this.ParentChar.setPainEffect(this.ParentChar.getPainEffect() - GameTime.getInstance().getThirtyFPSMultiplier());
               } else {
                  this.ParentChar.setPainDelta(0.0F);
                  var3 = 0.0F;

                  for(var4 = 0; var4 < BodyPartType.ToIndex(BodyPartType.MAX); ++var4) {
                     var3 += ((BodyPart)this.getBodyParts().get(var4)).getPain() * BodyPartType.getPainModifyer(var4);
                  }

                  var3 -= this.getPainReduction();
                  if (var3 > this.ParentChar.getStats().Pain) {
                     var10000 = this.ParentChar.getStats();
                     var10000.Pain += (var3 - this.ParentChar.getStats().Pain) / 500.0F;
                  } else {
                     this.ParentChar.getStats().Pain = var3;
                  }
               }

               this.setPainReduction(this.getPainReduction() - 0.005F * GameTime.getInstance().getMultiplier());
               if (this.getPainReduction() < 0.0F) {
                  this.setPainReduction(0.0F);
               }

               if (this.getParentChar().getStats().Pain > 100.0F) {
                  this.ParentChar.getStats().Pain = 100.0F;
               }

               if (this.isInfected()) {
                  var13 = SandboxOptions.instance.Lore.Mortality.getValue();
                  if (var13 == 1) {
                     this.ReduceGeneralHealth(110.0F);
                     LuaEventManager.triggerEvent("OnPlayerGetDamage", this.ParentChar, "INFECTION", 110);
                     this.setInfectionLevel(100.0F);
                  } else if (var13 != 7) {
                     var14 = this.getCurrentTimeForInfection();
                     if (this.InfectionMortalityDuration < 0.0F) {
                        this.InfectionMortalityDuration = this.pickMortalityDuration();
                     }

                     if (this.InfectionTime < 0.0F) {
                        this.InfectionTime = var14;
                     }

                     if (this.InfectionTime > var14) {
                        this.InfectionTime = var14;
                     }

                     var5 = (var14 - this.InfectionTime) / this.InfectionMortalityDuration;
                     var5 = Math.min(var5, 1.0F);
                     this.setInfectionLevel(var5 * 100.0F);
                     if (var5 == 1.0F) {
                        this.ReduceGeneralHealth(110.0F);
                        LuaEventManager.triggerEvent("OnPlayerGetDamage", this.ParentChar, "INFECTION", 110);
                     } else {
                        var5 *= var5;
                        var5 *= var5;
                        var6 = (1.0F - var5) * 100.0F;
                        var7 = this.getOverallBodyHealth() - var6;
                        if (var7 > 0.0F && var6 <= 99.0F) {
                           this.ReduceGeneralHealth(var7);
                           LuaEventManager.triggerEvent("OnPlayerGetDamage", this.ParentChar, "INFECTION", var7);
                        }
                     }
                  }
               }

               for(var13 = 0; var13 < BodyPartType.ToIndex(BodyPartType.MAX); ++var13) {
                  ((BodyPart)this.getBodyParts().get(var13)).DamageUpdate();
               }

               this.calculateOverallHealth();
               if (this.getOverallBodyHealth() <= 0.0F) {
                  if (GameClient.bClient && this.getParentChar() instanceof IsoPlayer && !((IsoPlayer)this.getParentChar()).bRemote) {
                     GameClient.sendPlayerDamage((IsoPlayer)this.getParentChar());
                  }

                  if (this.isIsOnFire()) {
                     this.setBurntToDeath(true);

                     for(var13 = 0; var13 < BodyPartType.ToIndex(BodyPartType.MAX); ++var13) {
                        ((BodyPart)this.getBodyParts().get(var13)).SetHealth((float)Rand.Next(90));
                     }
                  } else {
                     this.setBurntToDeath(false);
                  }
               }

               if (this.isReduceFakeInfection() && this.getOverallBodyHealth() > 0.0F) {
                  this.setFakeInfectionLevel(this.getFakeInfectionLevel() - this.getInfectionGrowthRate() * GameTime.instance.getMultiplier() * 2.0F);
               }

               if (this.getParentChar().getReduceInfectionPower() > 0.0F && this.getOverallBodyHealth() > 0.0F) {
                  this.setFakeInfectionLevel(this.getFakeInfectionLevel() - this.getInfectionGrowthRate() * GameTime.instance.getMultiplier());
                  this.getParentChar().setReduceInfectionPower(this.getParentChar().getReduceInfectionPower() - this.getInfectionGrowthRate() * GameTime.instance.getMultiplier());
                  if (this.getParentChar().getReduceInfectionPower() < 0.0F) {
                     this.getParentChar().setReduceInfectionPower(0.0F);
                  }
               }

               if (this.getFakeInfectionLevel() <= 0.0F) {
                  for(var13 = 0; var13 < BodyPartType.ToIndex(BodyPartType.MAX); ++var13) {
                     ((BodyPart)this.getBodyParts().get(var13)).SetFakeInfected(false);
                  }

                  this.setIsFakeInfected(false);
                  this.setFakeInfectionLevel(0.0F);
                  this.setReduceFakeInfection(false);
               }

               if (var11 == this.ParentChar.getStats().Pain) {
                  var10000 = this.ParentChar.getStats();
                  var10000.Pain = (float)((double)var10000.Pain - 0.25 * (double)GameTime.getInstance().getThirtyFPSMultiplier());
               }

               if (this.ParentChar.getStats().Pain < 0.0F) {
                  this.ParentChar.getStats().Pain = 0.0F;
               }

            }
         }
      }
   }

   private void calculateOverallHealth() {
      float var1 = 0.0F;

      for(int var2 = 0; var2 < BodyPartType.ToIndex(BodyPartType.MAX); ++var2) {
         BodyPart var3 = (BodyPart)this.getBodyParts().get(var2);
         var1 += (100.0F - var3.getHealth()) * BodyPartType.getDamageModifyer(var2);
      }

      var1 += this.getDamageFromPills();
      if (var1 > 100.0F) {
         var1 = 100.0F;
      }

      this.setOverallBodyHealth(100.0F - var1);
   }

   public static float getSicknessFromCorpsesRate(int var0) {
      if (SandboxOptions.instance.DecayingCorpseHealthImpact.getValue() == 1) {
         return 0.0F;
      } else if (var0 > 5) {
         float var1 = (float)ZomboidGlobals.FoodSicknessDecrease * 0.07F;
         switch (SandboxOptions.instance.DecayingCorpseHealthImpact.getValue()) {
            case 2:
               var1 = (float)ZomboidGlobals.FoodSicknessDecrease * 0.01F;
            case 3:
            default:
               break;
            case 4:
               var1 = (float)ZomboidGlobals.FoodSicknessDecrease * 0.11F;
               break;
            case 5:
               var1 = (float)ZomboidGlobals.FoodSicknessDecrease;
         }

         int var2 = Math.min(var0 - 5, FliesSound.maxCorpseCount - 5);
         return var1 * (float)var2;
      } else {
         return 0.0F;
      }
   }

   private void UpdateIllness() {
      if (SandboxOptions.instance.DecayingCorpseHealthImpact.getValue() != 1) {
         float var1 = this.GetBaseCorpseSickness();
         if (var1 <= 0.0F) {
            this.getParentChar().setCorpseSicknessRate(0.0F);
         } else {
            if (this.getParentChar().getCorpseSicknessDefense(var1) > 0.0F) {
               float var2 = 1.0F - this.getParentChar().getCorpseSicknessDefense(var1, false) / 100.0F;
               var1 *= var2;
            }

            if (var1 > 0.0F) {
               if (this.getParentChar().HasTrait("Resilient")) {
                  var1 *= 0.75F;
               } else if (this.getParentChar().HasTrait("ProneToIllness")) {
                  var1 *= 1.25F;
               }

               this.setFoodSicknessLevel(this.getFoodSicknessLevel() + var1 * GameTime.getInstance().getMultiplier());
            }

            this.getParentChar().setCorpseSicknessRate(var1);
         }
      }
   }

   public float GetBaseCorpseSickness() {
      int var1 = FliesSound.instance.getCorpseCount(this.getParentChar());
      float var2 = getSicknessFromCorpsesRate(var1);
      return var2;
   }

   private void UpdateTemperatureState() {
      float var1 = 0.06F;
      if (this.getParentChar() instanceof IsoPlayer) {
         if (this.ColdDamageStage > 0.0F) {
            float var2 = 100.0F - this.ColdDamageStage * 100.0F;
            if (var2 <= 0.0F) {
               this.getParentChar().setHealth(0.0F);
               return;
            }

            if (this.OverallBodyHealth > var2) {
               this.ReduceGeneralHealth(this.OverallBodyHealth - var2);
            }
         }

         ((IsoPlayer)this.getParentChar()).setMoveSpeed(var1);
      }

   }

   private float getDamageFromPills() {
      if (this.getParentChar() instanceof IsoPlayer) {
         IsoPlayer var1 = (IsoPlayer)this.getParentChar();
         if (var1.getSleepingPillsTaken() == 10) {
            return 40.0F;
         }

         if (var1.getSleepingPillsTaken() == 11) {
            return 80.0F;
         }

         if (var1.getSleepingPillsTaken() >= 12) {
            return 100.0F;
         }
      }

      return 0.0F;
   }

   public boolean UseBandageOnMostNeededPart() {
      int var1 = 0;
      BodyPart var2 = null;

      for(int var3 = 0; var3 < this.getBodyParts().size(); ++var3) {
         int var4 = 0;
         if (!((BodyPart)this.getBodyParts().get(var3)).bandaged()) {
            if (((BodyPart)this.getBodyParts().get(var3)).bleeding()) {
               var4 += 100;
            }

            if (((BodyPart)this.getBodyParts().get(var3)).scratched()) {
               var4 += 50;
            }

            if (((BodyPart)this.getBodyParts().get(var3)).bitten()) {
               var4 += 50;
            }

            if (var4 > var1) {
               var1 = var4;
               var2 = (BodyPart)this.getBodyParts().get(var3);
            }
         }
      }

      if (var1 > 0 && var2 != null) {
         var2.setBandaged(true, 10.0F);
         return true;
      } else {
         return false;
      }
   }

   public ArrayList<BodyPart> getBodyParts() {
      return this.BodyParts;
   }

   public int getDamageModCount() {
      return this.DamageModCount;
   }

   public void setDamageModCount(int var1) {
      this.DamageModCount = var1;
   }

   public float getInfectionGrowthRate() {
      return this.InfectionGrowthRate;
   }

   public void setInfectionGrowthRate(float var1) {
      this.InfectionGrowthRate = var1;
   }

   public void setInfectionLevel(float var1) {
      this.InfectionLevel = var1;
   }

   public boolean isInfected() {
      return this.IsInfected;
   }

   public void setInfected(boolean var1) {
      this.IsInfected = var1;
   }

   public float getInfectionTime() {
      return this.InfectionTime;
   }

   public void setInfectionTime(float var1) {
      this.InfectionTime = var1;
   }

   public float getInfectionMortalityDuration() {
      return this.InfectionMortalityDuration;
   }

   public void setInfectionMortalityDuration(float var1) {
      this.InfectionMortalityDuration = var1;
   }

   private float getCurrentTimeForInfection() {
      return this.getParentChar() instanceof IsoPlayer ? (float)((IsoPlayer)this.getParentChar()).getHoursSurvived() : (float)GameTime.getInstance().getWorldAgeHours();
   }

   /** @deprecated */
   @Deprecated
   public boolean isInf() {
      return this.IsInfected;
   }

   /** @deprecated */
   @Deprecated
   public void setInf(boolean var1) {
      this.IsInfected = var1;
   }

   public float getFakeInfectionLevel() {
      return this.FakeInfectionLevel;
   }

   public void setFakeInfectionLevel(float var1) {
      this.FakeInfectionLevel = var1;
   }

   public boolean isIsFakeInfected() {
      return this.IsFakeInfected;
   }

   public void setIsFakeInfected(boolean var1) {
      this.IsFakeInfected = var1;
      ((BodyPart)this.getBodyParts().get(0)).SetFakeInfected(var1);
   }

   public float getOverallBodyHealth() {
      return this.OverallBodyHealth;
   }

   public void setOverallBodyHealth(float var1) {
      this.OverallBodyHealth = var1;
   }

   public float getStandardHealthAddition() {
      return this.StandardHealthAddition;
   }

   public void setStandardHealthAddition(float var1) {
      this.StandardHealthAddition = var1;
   }

   public float getReducedHealthAddition() {
      return this.ReducedHealthAddition;
   }

   public void setReducedHealthAddition(float var1) {
      this.ReducedHealthAddition = var1;
   }

   public float getSeverlyReducedHealthAddition() {
      return this.SeverlyReducedHealthAddition;
   }

   public void setSeverlyReducedHealthAddition(float var1) {
      this.SeverlyReducedHealthAddition = var1;
   }

   public float getSleepingHealthAddition() {
      return this.SleepingHealthAddition;
   }

   public void setSleepingHealthAddition(float var1) {
      this.SleepingHealthAddition = var1;
   }

   public float getHealthFromFood() {
      return this.HealthFromFood;
   }

   public void setHealthFromFood(float var1) {
      this.HealthFromFood = var1;
   }

   public float getHealthReductionFromSevereBadMoodles() {
      return this.HealthReductionFromSevereBadMoodles;
   }

   public void setHealthReductionFromSevereBadMoodles(float var1) {
      this.HealthReductionFromSevereBadMoodles = var1;
   }

   public int getStandardHealthFromFoodTime() {
      return this.StandardHealthFromFoodTime;
   }

   public void setStandardHealthFromFoodTime(int var1) {
      this.StandardHealthFromFoodTime = var1;
   }

   public float getHealthFromFoodTimer() {
      return this.HealthFromFoodTimer;
   }

   public void setHealthFromFoodTimer(float var1) {
      this.HealthFromFoodTimer = var1;
   }

   public void setBoredomLevel(float var1) {
      this.BoredomLevel = var1;
   }

   public float getBoredomDecreaseFromReading() {
      return this.BoredomDecreaseFromReading;
   }

   public void setBoredomDecreaseFromReading(float var1) {
      this.BoredomDecreaseFromReading = var1;
   }

   public float getInitialThumpPain() {
      return this.InitialThumpPain;
   }

   public void setInitialThumpPain(float var1) {
      this.InitialThumpPain = var1;
   }

   public float getInitialScratchPain() {
      return this.InitialScratchPain;
   }

   public void setInitialScratchPain(float var1) {
      this.InitialScratchPain = var1;
   }

   public float getInitialBitePain() {
      return this.InitialBitePain;
   }

   public void setInitialBitePain(float var1) {
      this.InitialBitePain = var1;
   }

   public float getInitialWoundPain() {
      return this.InitialWoundPain;
   }

   public void setInitialWoundPain(float var1) {
      this.InitialWoundPain = var1;
   }

   public float getContinualPainIncrease() {
      return this.ContinualPainIncrease;
   }

   public void setContinualPainIncrease(float var1) {
      this.ContinualPainIncrease = var1;
   }

   public float getPainReductionFromMeds() {
      return this.PainReductionFromMeds;
   }

   public void setPainReductionFromMeds(float var1) {
      this.PainReductionFromMeds = var1;
   }

   public float getStandardPainReductionWhenWell() {
      return this.StandardPainReductionWhenWell;
   }

   public void setStandardPainReductionWhenWell(float var1) {
      this.StandardPainReductionWhenWell = var1;
   }

   public int getOldNumZombiesVisible() {
      return this.OldNumZombiesVisible;
   }

   public void setOldNumZombiesVisible(int var1) {
      this.OldNumZombiesVisible = var1;
   }

   public boolean getWasDraggingCorpse() {
      return this.WasDraggingCorpse;
   }

   public void setWasDraggingCorpse(boolean var1) {
      this.WasDraggingCorpse = var1;
   }

   public int getCurrentNumZombiesVisible() {
      return this.CurrentNumZombiesVisible;
   }

   public void setCurrentNumZombiesVisible(int var1) {
      this.CurrentNumZombiesVisible = var1;
   }

   public float getPanicIncreaseValue() {
      return this.PanicIncreaseValue;
   }

   public float getPanicIncreaseValueFrame() {
      return this.PanicIncreaseValueFrame;
   }

   public void setPanicIncreaseValue(float var1) {
      this.PanicIncreaseValue = var1;
   }

   public float getPanicReductionValue() {
      return this.PanicReductionValue;
   }

   public void setPanicReductionValue(float var1) {
      this.PanicReductionValue = var1;
   }

   public float getDrunkIncreaseValue() {
      return this.DrunkIncreaseValue;
   }

   public void setDrunkIncreaseValue(float var1) {
      this.DrunkIncreaseValue = var1;
   }

   public float getDrunkReductionValue() {
      return this.DrunkReductionValue;
   }

   public void setDrunkReductionValue(float var1) {
      this.DrunkReductionValue = var1;
   }

   public boolean isIsOnFire() {
      return this.IsOnFire;
   }

   public void setIsOnFire(boolean var1) {
      this.IsOnFire = var1;
   }

   public boolean isBurntToDeath() {
      return this.BurntToDeath;
   }

   public void setBurntToDeath(boolean var1) {
      this.BurntToDeath = var1;
   }

   public void setWetness(float var1) {
      float var2 = 0.0F;
      if (this.BodyParts.size() > 0) {
         for(int var4 = 0; var4 < this.BodyParts.size(); ++var4) {
            BodyPart var3 = (BodyPart)this.BodyParts.get(var4);
            var3.setWetness(var1);
            var2 += var3.getWetness();
         }

         var2 /= (float)this.BodyParts.size();
      }

      this.Wetness = PZMath.clamp(var2, 0.0F, 100.0F);
   }

   public float getCatchACold() {
      return this.CatchACold;
   }

   public void setCatchACold(float var1) {
      this.CatchACold = var1;
   }

   public boolean isHasACold() {
      return this.HasACold;
   }

   public void setHasACold(boolean var1) {
      this.HasACold = var1;
   }

   public void setColdStrength(float var1) {
      this.ColdStrength = var1;
   }

   public float getColdProgressionRate() {
      return this.ColdProgressionRate;
   }

   public void setColdProgressionRate(float var1) {
      this.ColdProgressionRate = var1;
   }

   public int getTimeToSneezeOrCough() {
      return this.TimeToSneezeOrCough;
   }

   public void setTimeToSneezeOrCough(int var1) {
      this.TimeToSneezeOrCough = var1;
   }

   public int getSmokerSneezeTimerMin() {
      return this.SmokerSneezeTimerMin;
   }

   public int getSmokerSneezeTimerMax() {
      return this.SmokerSneezeTimerMax;
   }

   public int getMildColdSneezeTimerMin() {
      return this.MildColdSneezeTimerMin;
   }

   public void setMildColdSneezeTimerMin(int var1) {
      this.MildColdSneezeTimerMin = var1;
   }

   public int getMildColdSneezeTimerMax() {
      return this.MildColdSneezeTimerMax;
   }

   public void setMildColdSneezeTimerMax(int var1) {
      this.MildColdSneezeTimerMax = var1;
   }

   public int getColdSneezeTimerMin() {
      return this.ColdSneezeTimerMin;
   }

   public void setColdSneezeTimerMin(int var1) {
      this.ColdSneezeTimerMin = var1;
   }

   public int getColdSneezeTimerMax() {
      return this.ColdSneezeTimerMax;
   }

   public void setColdSneezeTimerMax(int var1) {
      this.ColdSneezeTimerMax = var1;
   }

   public int getNastyColdSneezeTimerMin() {
      return this.NastyColdSneezeTimerMin;
   }

   public void setNastyColdSneezeTimerMin(int var1) {
      this.NastyColdSneezeTimerMin = var1;
   }

   public int getNastyColdSneezeTimerMax() {
      return this.NastyColdSneezeTimerMax;
   }

   public void setNastyColdSneezeTimerMax(int var1) {
      this.NastyColdSneezeTimerMax = var1;
   }

   public int getSneezeCoughActive() {
      return this.SneezeCoughActive;
   }

   public void setSneezeCoughActive(int var1) {
      this.SneezeCoughActive = var1;
   }

   public int getSneezeCoughTime() {
      return this.SneezeCoughTime;
   }

   public void setSneezeCoughTime(int var1) {
      this.SneezeCoughTime = var1;
   }

   public int getSneezeCoughDelay() {
      return this.SneezeCoughDelay;
   }

   public void setSneezeCoughDelay(int var1) {
      this.SneezeCoughDelay = var1;
   }

   public void setUnhappynessLevel(float var1) {
      this.UnhappynessLevel = var1;
   }

   public IsoGameCharacter getParentChar() {
      return this.ParentChar;
   }

   public void setParentChar(IsoGameCharacter var1) {
      this.ParentChar = var1;
   }

   public float getTemperature() {
      return this.Temperature;
   }

   public void setTemperature(float var1) {
      this.lastTemperature = this.Temperature;
      this.Temperature = var1;
   }

   public float getTemperatureChangeTick() {
      return this.Temperature - this.lastTemperature;
   }

   public void setPoisonLevel(float var1) {
      this.PoisonLevel = var1;
   }

   public float getPoisonLevel() {
      return this.PoisonLevel;
   }

   public float getFoodSicknessLevel() {
      return this.FoodSicknessLevel;
   }

   public void setFoodSicknessLevel(float var1) {
      this.FoodSicknessLevel = Math.max(var1, 0.0F);
   }

   public boolean isReduceFakeInfection() {
      return this.reduceFakeInfection;
   }

   public void setReduceFakeInfection(boolean var1) {
      this.reduceFakeInfection = var1;
   }

   public void AddRandomDamage() {
      // $FF: Couldn't be decompiled
   }

   public float getPainReduction() {
      return this.painReduction;
   }

   public void setPainReduction(float var1) {
      this.painReduction = var1;
   }

   public float getColdReduction() {
      return this.coldReduction;
   }

   public void setColdReduction(float var1) {
      this.coldReduction = var1;
   }

   public int getRemotePainLevel() {
      return this.RemotePainLevel;
   }

   public void setRemotePainLevel(int var1) {
      this.RemotePainLevel = var1;
   }

   public float getColdDamageStage() {
      return this.ColdDamageStage;
   }

   public void setColdDamageStage(float var1) {
      this.ColdDamageStage = var1;
   }

   public Thermoregulator getThermoregulator() {
      return this.thermoregulator;
   }

   public void decreaseBodyWetness(float var1) {
      float var2 = 0.0F;
      if (this.BodyParts.size() > 0) {
         for(int var4 = 0; var4 < this.BodyParts.size(); ++var4) {
            BodyPart var3 = (BodyPart)this.BodyParts.get(var4);
            var3.setWetness(var3.getWetness() - var1);
            var2 += var3.getWetness();
         }

         var2 /= (float)this.BodyParts.size();
      }

      this.Wetness = PZMath.clamp(var2, 0.0F, 100.0F);
   }

   public void increaseBodyWetness(float var1) {
      float var2 = 0.0F;
      if (this.BodyParts.size() > 0) {
         for(int var4 = 0; var4 < this.BodyParts.size(); ++var4) {
            BodyPart var3 = (BodyPart)this.BodyParts.get(var4);
            var3.setWetness(var3.getWetness() + var1);
            var2 += var3.getWetness();
         }

         var2 /= (float)this.BodyParts.size();
      }

      this.Wetness = PZMath.clamp(var2, 0.0F, 100.0F);
   }

   public void DamageFromAnimal(IsoAnimal var1) {
      float var2 = var1.calcDamage();
      String var3 = this.getParentChar().testDotSide(var1);
      boolean var4 = var3.equals(behindStr);
      this.getParentChar().setHitFromBehind(var4);
      if (!(this.getParentChar() instanceof IsoPlayer) || ((IsoPlayer)this.getParentChar()).isLocalPlayer()) {
         boolean var5 = false;
         byte var6 = 1;
         boolean var7 = true;
         int var17 = Rand.Next(BodyPartType.ToIndex(BodyPartType.Hand_L), BodyPartType.ToIndex(BodyPartType.MAX));
         boolean var8 = false;
         boolean var9 = true;
         boolean var10 = false;
         boolean var11 = true;
         boolean var12 = true;
         BodyPart var13 = this.getBodyPart(BodyPartType.FromIndex(var17));
         float var14 = this.getParentChar().getBodyPartClothingDefense(var13.getIndex(), var9, var10);
         if ((float)Rand.Next(100) < var14) {
            var7 = false;
            this.getParentChar().addHoleFromZombieAttacks(BloodBodyPartType.FromIndex(var17), false);
         }

         if (var7) {
            this.getParentChar().addHole(BloodBodyPartType.FromIndex(var17));
            this.getParentChar().splatBloodFloorBig();
            this.getParentChar().splatBloodFloorBig();
            this.getParentChar().splatBloodFloorBig();
            if (var1.adef.canDoLaceration && Rand.NextBool(6)) {
               var13.generateDeepWound();
            } else if (var1.adef.canDoLaceration && Rand.NextBool(3)) {
               var13.setCut(true);
            } else if (Rand.NextBool(2)) {
               var13.setScratched(true, true);
            }

            if (var17 == BodyPartType.ToIndex(BodyPartType.Head)) {
               var2 *= 4.0F;
            }

            if (var17 == BodyPartType.ToIndex(BodyPartType.Neck)) {
               var2 *= 4.0F;
            }

            if (var17 == BodyPartType.ToIndex(BodyPartType.Torso_Upper)) {
               var2 *= 2.0F;
            }

            this.AddDamage(var17, var2);
            Stats var10000;
            switch (var6) {
               case 0:
                  var10000 = this.ParentChar.getStats();
                  var10000.Pain += this.getInitialThumpPain() * BodyPartType.getPainModifyer(var17);
                  break;
               case 1:
                  var10000 = this.ParentChar.getStats();
                  var10000.Pain += this.getInitialScratchPain() * BodyPartType.getPainModifyer(var17);
                  break;
               case 2:
                  var10000 = this.ParentChar.getStats();
                  var10000.Pain += this.getInitialBitePain() * BodyPartType.getPainModifyer(var17);
            }

            if (this.getParentChar().getStats().Pain > 100.0F) {
               this.ParentChar.getStats().Pain = 100.0F;
            }

            if (this.ParentChar instanceof IsoPlayer && GameClient.bClient && ((IsoPlayer)this.ParentChar).isLocalPlayer()) {
               IsoPlayer var15 = (IsoPlayer)this.ParentChar;
               var15.updateMovementRates();
               GameClient.sendPlayerInjuries(var15);
               GameClient.sendPlayerDamage(var15);
            }

            boolean var18 = false;
            if (!var1.isAimAtFloor()) {
               var18 = var1.isBehind(this.getParentChar());
            } else {
               var18 = this.getParentChar().isFallOnFront();
            }

            boolean var16 = false;
            if (var18) {
               var16 = this.getParentChar().bodyPartIsSpikedBehind(var17);
            } else {
               var16 = this.getParentChar().bodyPartIsSpiked(var17);
            }

            if (var16) {
               this.getParentChar().addBlood(BloodBodyPartType.FromIndex(var17), true, false, false);
               var1.spikePart(BodyPartType.Head);
            }

         }
      }
   }

   public float getGeneralWoundInfectionLevel() {
      if (SandboxOptions.instance.WoundInfectionFactor.getValue() <= 0.0) {
         return 0.0F;
      } else {
         float var1 = 0.0F;
         if (this.BodyParts.size() > 0) {
            for(int var3 = 0; var3 < this.BodyParts.size(); ++var3) {
               BodyPart var2 = (BodyPart)this.BodyParts.get(var3);
               if (var2.isInfectedWound()) {
                  var1 += var2.getWoundInfectionLevel();
               }
            }
         }

         var1 *= 10.0F;
         var1 *= (float)SandboxOptions.instance.WoundInfectionFactor.getValue();
         var1 = Math.min(var1, 100.0F);
         return var1;
      }
   }

   public void UpdateDiscomfort() {
      float var1 = this.getParentChar().isDraggingCorpse() ? 0.3F : 0.0F;
      float var2 = this.getParentChar().getClothingDiscomfortModifier();
      float var3 = 0.0F;
      if (this.getParentChar().isAsleep()) {
         switch (this.getParentChar().getBedType()) {
            case "badBed":
               var3 = 0.3F;
               break;
            case "badBedPillow":
               var3 = 0.2F;
               break;
            case "floor":
               var3 = 0.5F;
               break;
            case "floorPillow":
               var3 = 0.4F;
         }
      }

      float var13 = 1.0F - 0.5F * (this.getParentChar().getStats().getDrunkenness() / 100.0F);
      float var14 = 0.1F * (float)this.getParentChar().getMoodles().getMoodleLevel(MoodleType.Hypothermia);
      float var6 = 0.1F * (float)this.getParentChar().getMoodles().getMoodleLevel(MoodleType.Hyperthermia);
      float var7 = 0.1F * (float)this.getParentChar().getMoodles().getMoodleLevel(MoodleType.Wet);
      float var8 = 0.0F;
      var8 += var3;
      var8 += var2;
      var8 += var1;
      var8 += var14;
      var8 += var6;
      var8 += var7;
      var8 *= var13;
      float var9 = PZMath.clamp(var8, 0.0F, 1.0F) * 100.0F;
      float var10 = 0.005F * GameTime.instance.getMultiplier();
      if (var9 > this.getDiscomfortLevel()) {
         var10 *= 0.025F;
      }

      if (this.getParentChar().isAsleep()) {
         this.setDiscomfortLevel(var9);
      } else if (!PZMath.equal(this.getDiscomfortLevel(), var9, var10)) {
         this.setDiscomfortLevel(PZMath.lerp(this.getDiscomfortLevel(), var9, var10));
      } else if (this.getDiscomfortLevel() != var9) {
         this.setDiscomfortLevel(var9);
      }

      this.setDiscomfortLevel(PZMath.clamp(this.getDiscomfortLevel(), 0.0F, 100.0F));
      if (this.getParentChar().getMoodles().getMoodleLevel(MoodleType.Uncomfortable) >= 1) {
         float var11 = var8 > 1.0F ? 1.0F + var8 % 1.0F * 3.0F : 1.0F;
         if (this.getUnhappynessLevel() < 100.0F && this.getDiscomfortLevel() > 0.0F) {
            float var12 = (float)(ZomboidGlobals.UnhappinessIncrease / 2.0 * (double)(this.getDiscomfortLevel() * var11 / 100.0F) * (double)GameTime.instance.getMultiplier());
            this.setUnhappynessLevel(PZMath.clamp(this.getUnhappynessLevel() + var12, 0.0F, 100.0F));
         }
      }

   }

   public float getDiscomfortLevel() {
      return this.discomfortLevel;
   }

   public void setDiscomfortLevel(float var1) {
      this.discomfortLevel = var1;
   }

   public void addStiffness(BodyPart var1, float var2) {
      var1.addStiffness(var2);
   }

   public void addStiffness(BodyPartType var1, float var2) {
      BodyPart var3 = this.getBodyPart(var1);
      var3.addStiffness(var2);
   }
}
