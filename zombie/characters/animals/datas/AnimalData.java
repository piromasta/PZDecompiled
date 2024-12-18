package zombie.characters.animals.datas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.Stats;
import zombie.characters.animals.AnimalAllele;
import zombie.characters.animals.AnimalGene;
import zombie.characters.animals.IsoAnimal;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.entity.components.fluids.Fluid;
import zombie.globalObjects.SGlobalObjects;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.Food;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.areas.DesignationZoneAnimal;
import zombie.iso.objects.IsoFeedingTrough;
import zombie.iso.objects.IsoHutch;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.network.GameClient;
import zombie.network.PacketTypes;
import zombie.network.ServerOptions;
import zombie.network.SpawnRegions;
import zombie.network.packets.INetworkPacket;
import zombie.util.PZCalendar;
import zombie.util.StringUtils;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;

public class AnimalData {
   public IsoAnimal parent = null;
   public IsoPlayer attachedPlayer = null;
   private IsoObject attachedTree = null;
   public AnimalBreed breed = null;
   public float milkQty = 0.0F;
   public float woolQty = 0.0F;
   public boolean canHaveMilk = false;
   public float weight = 720.0F;
   private float size = 1.0F;
   private int age = 0;
   private int currentStageNbr = 0;
   public int lastHourCheck = -1;
   public AnimalGrowStage currentStage = null;
   public boolean pregnant = false;
   public int pregnantTime = 0;
   private IsoAnimal femaleToCheck = null;
   public ArrayList<IsoAnimal> animalToInseminate = new ArrayList();
   private int timerInseminate = 0;
   public float maxMilkActual = 0.0F;
   public boolean goingToMomTest = false;
   public boolean goingToMom = false;
   public float goingToMomTimer = 0.0F;
   private ArrayList<SpawnRegions.Point> linkedTrough = new ArrayList();
   public boolean eatingGrass = false;
   public int eggsToday = 0;
   public long eggTime = 0L;
   public boolean fertilized = false;
   public int fertilizedTime = 0;
   private int hutchToEnterTimerFailsafe = 0;
   public HashMap<String, AnimalGene> maleGenome = new HashMap();
   private int hutchPosition = -1;
   private int preferredHutchPosition = -1;
   private int eggLayTime = -1;
   private int hutchPathTimer = -1;
   private int troughPathTimer = -1;
   public IsoFeedingTrough troughToCheck;
   private boolean goingToInseminate = false;
   public long lastMilkTimer = 0L;
   public long lastPregnancyTime = 0L;
   public static final long ONE_WEEK_MILLISECONDS = 604800000L;
   public static final long ONE_DAY_MILLISECONDS = 86400000L;
   public static final long ONE_HOUR_MILLISECONDS = 3600000L;
   public static final int FEATHER_CHANCE_PER_HOUR = 1;
   private long timeToLoseMilk = 604800000L;
   public int lastImpregnateTime = 0;
   public int clutchSize = 0;
   public boolean clutchSizeDone = false;
   public int enterHutchTimerAfterDestroy = 0;

   public AnimalData(IsoAnimal var1, AnimalBreed var2) {
      this.parent = var1;
      if (var2 == null && var1.adef != null) {
         var2 = (AnimalBreed)var1.adef.breeds.get(Rand.Next(0, var1.adef.breeds.size() + 1));
      }

      this.breed = var2;
      if (var1.adef != null && var1.adef.female) {
         var1.getDescriptor().setFemale(true);
      } else if (var1.adef != null && var1.adef.male) {
         var1.getDescriptor().setFemale(false);
      } else {
         var1.getDescriptor().setFemale(Rand.NextBool(2));
      }

   }

   public void checkStages() {
      ArrayList var1 = this.getGrowStage();
      AnimalGrowStage var2 = null;
      if (var1 != null && !var1.isEmpty()) {
         for(int var3 = 0; var3 < var1.size(); ++var3) {
            AnimalGrowStage var4 = (AnimalGrowStage)var1.get(var3);
            if (var4.stage.equals(this.parent.getAnimalType()) && this.getDaysSurvived() >= var4.getAgeToGrow(this.parent)) {
               var2 = var4;
               break;
            }
         }
      }

      if (var2 != null && var2.nextStage != null) {
         this.grow(this.parent.getDescriptor().isFemale() ? var2.nextStage : var2.nextStageMale);
      }

   }

   public void update() {
      boolean var1 = false;
      if (GameTime.getInstance().getHour() != this.lastHourCheck) {
         this.lastHourCheck = GameTime.getInstance().getHour();
         this.parent.setHoursSurvived(this.parent.getHoursSurvived() + 1.0);
         var1 = true;
      }

      boolean var2 = false;
      if (this.getAge() < this.getDaysSurvived()) {
         float var3 = this.getAgeGrowModifier();
         this.setAge(Float.valueOf((float)this.getDaysSurvived() + (var3 - 1.0F)).intValue());
         this.parent.setHoursSurvived((double)(this.getAge() * 24));
         var2 = true;
      }

      if (var1) {
         this.hourGrow(false);
      }

      if (var2) {
         this.growUp(false);
      }

      this.checkStages();
      this.checkPregnancy();
      if (this.eggsToday < this.parent.adef.eggsPerDay && this.eggTime == 0L) {
         this.eggTime = (long)(Long.valueOf(GameTime.instance.getCalender().getTimeInMillis() / 1000L).intValue() + Rand.Next(0, 43200));
      }

   }

   public void callToTrough(IsoFeedingTrough var1) {
      if (this.parent.isExistInTheWorld() && var1 != null && var1.getSquare() != null) {
         this.parent.stopAllMovementNow();
         this.parent.faceThisObject(var1);
         if (this.troughPathTimer <= -1) {
            this.troughPathTimer = Rand.Next(100, 400);
         }

         this.troughToCheck = var1;
      }

   }

   private void checkPregnancy() {
      if (this.pregnant && this.pregnantTime > 0) {
         if (this.parent.stressLevel > 80.0F && Rand.NextBool(50)) {
            DebugLog.DetailedInfo.trace("Animal " + this.parent.getFullName() + " lose baby due to stress");
            this.pregnant = false;
            this.pregnantTime = 0;
         }

         if (this.pregnantTime >= this.getPregnantPeriod()) {
            DebugLog.DetailedInfo.trace("Pregnancy done for " + this.parent.getFullName());
            this.pregnant = false;
            this.pregnantTime = 0;
            int var1 = Rand.Next(this.parent.adef.minBaby, this.parent.adef.maxBaby + 1);
            float var2 = this.parent.getHealth();
            if ((double)var2 < 0.5) {
               DebugLog.Animal.trace("Mother health was too low, reducing nb of babies");
               var1 *= (int)var2;
            }

            DebugLog.Animal.trace("Should pop " + var1 + " babies (" + this.parent.adef.minBaby + "-" + this.parent.adef.maxBaby + ")");

            for(int var3 = 0; var3 < var1; ++var3) {
               this.parent.addBaby();
            }
         }
      }

   }

   public float getAgeGrowModifier() {
      float var10000;
      switch (SandboxOptions.getInstance().AnimalAgeModifier.getValue()) {
         case 1:
            var10000 = 90.0F;
            break;
         case 2:
            var10000 = 10.0F;
            break;
         case 3:
            var10000 = 5.0F;
            break;
         case 4:
         default:
            var10000 = 1.0F;
            break;
         case 5:
            var10000 = 1.0F;
            break;
         case 6:
            var10000 = 1.0F;
      }

      return var10000;
   }

   public void growUp(boolean var1) {
      this.eggsToday = 0;
      if (this.pregnant) {
         ++this.pregnantTime;
         if (this.parent.adef.udder) {
            this.setCanHaveMilk(true);
         }
      }

      float var2 = (this.getMaxSize() - this.getMinSize()) / (float)this.currentStage.getAgeToGrow(this.parent);
      float var3 = (this.getMaxWeight() - this.getMinWeight()) / (float)this.currentStage.getAgeToGrow(this.parent);
      if (this.parent.smallEnclosure) {
         var2 /= 8.0F;
         var3 /= 8.0F;
      }

      this.setSize(Math.min(this.getMaxSize(), this.getSize() + var2 * this.parent.getHealth()));
      this.setWeight(Math.min(this.getMaxWeight(), this.getWeight() + var3 * this.parent.getHealth()));
      this.checkPoop(var1);
      this.animalToInseminate.clear();
   }

   public InventoryItem checkPoop(boolean var1) {
      if (!StringUtils.isNullOrEmpty(this.parent.adef.dung) && this.parent.getHutch() == null) {
         if (Rand.Next(100) > this.parent.adef.dungChancePerDay) {
            return null;
         } else {
            float var2 = 1.0F;
            if (this.parent.isBaby()) {
               var2 = 0.3F;
            }

            var2 *= this.getSize() / this.getMaxSize();
            InventoryItem var3 = InventoryItemFactory.CreateItem(this.parent.adef.dung);
            var3.setWeight(var3.getWeight() * var2);
            var3.setActualWeight(var3.getWeight());
            var3.setCustomWeight(true);
            IsoGridSquare var4 = null;
            if (var1 && !this.parent.connectedDZone.isEmpty()) {
               var4 = this.parent.getRandomSquareInZone();
            }

            if (var4 == null && this.parent.getSquare() != null) {
               var4 = this.parent.getSquare();
            }

            if (var4 != null) {
               var4.AddWorldInventoryItem(var3, 0.0F, 0.0F, 0.0F, true);
            }

            return var3;
         }
      } else {
         return null;
      }
   }

   public InventoryItem dropFeather(boolean var1) {
      if (!StringUtils.isNullOrEmpty(this.parent.getBreed().featherItem) && this.parent.getHutch() == null) {
         if (Rand.Next(100) >= 1) {
            return null;
         } else {
            InventoryItem var2 = InventoryItemFactory.CreateItem(this.parent.getBreed().featherItem);
            IsoGridSquare var3 = null;
            if (var1 && !this.parent.connectedDZone.isEmpty()) {
               var3 = this.parent.getRandomSquareInZone();
            }

            if (var3 == null && this.parent.getSquare() != null) {
               var3 = this.parent.getSquare();
            }

            if (var3 != null) {
               var3.AddWorldInventoryItem(var2, 0.0F, 0.0F, 0.0F, true);
            }

            return var2;
         }
      } else {
         return null;
      }
   }

   public void updateHungerAndThirst(boolean var1) {
      if (!this.parent.isWild()) {
         float var2 = 1.0F;
         if (this.parent.hutch != null || this.parent.isAnimalSitting()) {
            var2 = 0.5F;
         }

         if (!this.parent.isInvincible()) {
            float var3 = this.getHungerReduction();
            float var4 = this.getThirstReduction();
            if (ServerOptions.getInstance().UltraSpeedDoesnotAffectToAnimals.getValue() && GameTime.getInstance().getMultiplier() > 40.0F) {
               var3 /= GameTime.getInstance().getMultiplier() / 100.0F;
               var4 /= GameTime.getInstance().getMultiplier() / 100.0F;
            }

            if (var1) {
               var3 *= this.getHungerReductionMetaMod();
               var4 *= this.getHungerReductionMetaMod();
            }

            Stats var10000 = this.parent.getStats();
            var10000.hunger += var3 * var2;
            this.parent.getStats().hunger = PZMath.clamp(this.parent.getStats().hunger, 0.0F, 1.0F);
            var10000 = this.parent.getStats();
            var10000.thirst += var4 * var2;
            this.parent.getStats().thirst = PZMath.clamp(this.parent.getStats().thirst, 0.0F, 1.0F);
         }

      }
   }

   public boolean reduceHealthDueToMilk() {
      if (!this.canHaveMilk) {
         return false;
      } else {
         return this.getMilkQuantity() / this.getMaxMilk() > 1.15F;
      }
   }

   public void updateHealth() {
      if (!this.parent.isWild()) {
         boolean var1 = true;
         if (this.parent.hutch != null && this.parent.hutch.getHutchDirt() > 40.0F) {
            var1 = false;
         }

         if ((double)this.parent.getStats().hunger > 0.8 || (double)this.parent.getStats().thirst > 0.8) {
            var1 = false;
            this.parent.setHealth(this.parent.getHealth() - this.getHealthLoss(3.0F));
         }

         if (this.parent.isGeriatric()) {
            var1 = false;
            this.parent.setHealth(this.parent.getHealth() - this.getHealthLoss(30.0F));
         }

         if (!this.reduceHealthDueToMilk() && this.parent.getHealth() < 1.0F && var1) {
            this.parent.setHealth(Math.min(1.0F, this.parent.getHealth() + this.getHealthLoss(3.0F)));
         }

      }
   }

   public void hourGrow(boolean var1) {
      this.parent.ignoredTrough.clear();
      if (this.lastImpregnateTime > 0) {
         --this.lastImpregnateTime;
      }

      this.updateHungerAndThirst(var1);
      this.dropFeather(var1);
      this.updateHealth();
      if (var1) {
         this.eatAndDrinkAfterMeta();
      }

      this.updateMilk();
      if (this.getBreed().woolType != null && this.getMaxWool() > 0.0F) {
         this.setWoolQuantity(Math.min(this.getMaxWool(), this.woolQty + this.getWoolInc()));
      }

      this.findFemaleToInseminate((PZCalendar)null);
      if (var1 && this.animalToInseminate != null && !this.animalToInseminate.isEmpty()) {
         for(int var2 = 0; var2 < this.animalToInseminate.size(); ++var2) {
            ((IsoAnimal)this.animalToInseminate.get(var2)).fertilize(this.parent, false);
         }

         this.animalToInseminate.clear();
      }

      this.parent.smallEnclosure = false;
      if (this.parent.getDZone() != null && this.parent.getDZone().getFullZoneSize() < this.parent.adef.minEnclosureSize) {
         this.parent.smallEnclosure = true;
      }

      if (!var1) {
         this.checkEggs(GameTime.getInstance().getCalender(), false);
      }

      this.checkFertilizedTime();
      this.checkOld();
      this.updateWeight();
   }

   private void updateWeight() {
      if ((double)this.parent.getStats().hunger > 0.8) {
         this.setWeight(this.getWeight() - this.getWeight() / 800.0F);
      }

   }

   private void updateMilk() {
      if (this.parent.adef.udder) {
         if (this.canHaveMilk) {
            this.setMilkQuantity(this.milkQty + this.getMilkInc());
            if (!this.isPregnant() && GameTime.getInstance().getCalender().getTimeInMillis() - this.lastMilkTimer > this.timeToLoseMilk) {
               this.canHaveMilk = false;
            }
         } else {
            this.setMilkQuantity(this.milkQty - this.getMilkInc() / 2.0F);
         }
      }

      if (this.reduceHealthDueToMilk()) {
         this.parent.setHealth(this.parent.getHealth() - this.getHealthLoss(10.0F) * (this.milkQty / this.getMaxMilk()));
      }

   }

   private void checkOld() {
      if (!this.parent.isBaby() && this.getMaxAgeGeriatric() > 0.0F && this.getGeriatricPercentage() >= 0.95F) {
         this.parent.setHealth(this.parent.getHealth() - 0.1F);
      }

   }

   public float getHealthLoss(Float var1) {
      if (var1 == null) {
         var1 = 1.0F;
      }

      AnimalAllele var2 = this.parent.getUsedGene("resistance");
      float var3 = this.parent.adef.healthLossMultiplier;
      if (var2 == null) {
         return var3;
      } else {
         float var4 = 1.5F - var2.currentValue;
         float var5 = 1.0F;
         if (ServerOptions.getInstance().UltraSpeedDoesnotAffectToAnimals.getValue() && GameTime.instance.getMultiplier() > 50.0F) {
            var5 = Math.abs(1000.0F - GameTime.instance.getMultiplier()) / 1000.0F;
         }

         return var3 * var4 / var1 * var5;
      }
   }

   public float getMaxMilk() {
      AnimalAllele var1 = this.parent.getUsedGene("maxMilk");
      return var1 == null ? this.parent.adef.maxMilk : this.parent.adef.maxMilk * var1.currentValue;
   }

   public float getMaxMilkActual() {
      return this.maxMilkActual;
   }

   public void setMaxMilkActual(float var1) {
      this.maxMilkActual = var1;
   }

   public float getMaxWool() {
      if (this.parent.adef.maxWool <= 0.0F) {
         return 0.0F;
      } else {
         AnimalAllele var1 = this.parent.getUsedGene("maxWool");
         return var1 == null ? this.parent.adef.maxWool : this.parent.adef.maxWool * var1.currentValue;
      }
   }

   public float getMinMilk() {
      AnimalAllele var1 = this.parent.getUsedGene("maxMilk");
      return var1 == null ? this.parent.adef.minMilk : this.parent.adef.minMilk * var1.currentValue;
   }

   public float getMilkInc() {
      AnimalAllele var1 = this.parent.getUsedGene("milkInc");
      float var2 = this.maxMilkActual / 20.0F;
      if (var1 == null) {
         return var2;
      } else {
         var2 *= var1.currentValue;
         float var3 = 1.0F;
         if (this.parent.stressLevel > 40.0F) {
            var3 = 40.0F / this.parent.stressLevel;
         }

         float var4 = 1.0F;
         if (this.parent.geneticDisorder.contains("poormilk")) {
            var4 = 0.2F;
         }

         return var2 * this.getMilkIncModifier() * var3 * var4;
      }
   }

   public float getWoolInc() {
      AnimalAllele var1 = this.parent.getUsedGene("woolInc");
      float var2 = this.getMaxWool() / 2400.0F;
      if (var1 == null) {
         return var2;
      } else {
         var2 *= var1.currentValue;
         float var3 = 1.0F;
         if (this.parent.stressLevel > 40.0F) {
            var3 = 40.0F / this.parent.stressLevel;
         }

         float var4 = 1.0F;
         if (this.parent.geneticDisorder.contains("poorwool")) {
            var4 = 0.2F;
         }

         return var2 * this.getWoolIncModifier() * var3 * var4;
      }
   }

   private int calcClutchSize() {
      float var1 = 1.0F;
      AnimalAllele var2 = this.parent.getUsedGene("eggClutch");
      if (var2 != null) {
         var1 = var2.currentValue;
      }

      int var3 = Rand.Next(Float.valueOf((float)this.parent.adef.minClutchSize * var1).intValue(), Float.valueOf((float)this.parent.adef.maxClutchSize * var1).intValue());
      var3 = Float.valueOf((float)this.parent.adef.maxClutchSize * var1).intValue();
      return var3;
   }

   public void checkEggs(PZCalendar var1, boolean var2) {
      if (!this.parent.isDead()) {
         if (this.eggsToday < this.parent.adef.eggsPerDay && this.eggTime == 0L && var2) {
            this.eggTime = (long)(Long.valueOf(var1.getTimeInMillis() / 1000L).intValue() - 10);
         }

         if (this.parent.adef.eggsPerDay > 0) {
            if (this.haveLayingEggPeriod() && !this.isInLayingEggPeriod(var1)) {
               this.clutchSizeDone = false;
               return;
            }

            if (this.haveLayingEggPeriod() && !this.clutchSizeDone && this.clutchSize == 0 && this.parent.adef.minClutchSize > 0 && this.isInLayingEggPeriod(var1) && SandboxOptions.getInstance().AnimalMatingSeason.getValue()) {
               this.clutchSize = this.calcClutchSize();
               this.clutchSizeDone = true;
            }

            if (this.haveLayingEggPeriod() && this.clutchSize == 0 && this.parent.adef.minClutchSize > 0 && SandboxOptions.getInstance().AnimalMatingSeason.getValue()) {
               return;
            }

            if (this.eggTime > 0L && var1.getTimeInMillis() / 1000L > this.eggTime) {
               if (this.parent.adef.hutches != null && this.getRegionHutch() != null && !this.getRegionHutch().isDoorClosed() && this.parent.hutch == null && this.getRegionHutch().haveRoomForNewEggs()) {
                  if (var2) {
                     if (!this.getRegionHutch().addMetaEgg(this.parent)) {
                        this.parent.addEgg(true);
                     }

                     ++this.eggsToday;
                     this.eggTime = 0L;
                  } else {
                     this.parent.getBehavior().callToHutch((IsoHutch)null, true);
                  }
               } else if (this.parent.addEgg(false)) {
                  ++this.eggsToday;
                  this.eggTime = 0L;
               }
            }
         }

      }
   }

   public void checkFertilizedTime() {
      if (this.fertilized) {
         ++this.fertilizedTime;
      }

      if (this.fertilizedTime > this.parent.adef.fertilizedTimeMax) {
         this.fertilized = false;
         this.fertilizedTime = 0;
      }

   }

   private float getMilkIncModifier() {
      float var10000;
      switch (SandboxOptions.getInstance().AnimalMilkIncModifier.getValue()) {
         case 1:
            var10000 = 30.0F;
            break;
         case 2:
            var10000 = 5.0F;
            break;
         case 3:
            var10000 = 2.5F;
            break;
         case 4:
         default:
            var10000 = 1.0F;
            break;
         case 5:
            var10000 = 0.7F;
            break;
         case 6:
            var10000 = 0.2F;
      }

      return var10000;
   }

   private float getWoolIncModifier() {
      float var10000;
      switch (SandboxOptions.getInstance().AnimalWoolIncModifier.getValue()) {
         case 1:
            var10000 = 30.0F;
            break;
         case 2:
            var10000 = 5.0F;
            break;
         case 3:
            var10000 = 2.5F;
            break;
         case 4:
         default:
            var10000 = 1.0F;
            break;
         case 5:
            var10000 = 0.7F;
            break;
         case 6:
            var10000 = 0.2F;
      }

      return var10000;
   }

   public int getPregnantPeriod() {
      int var1 = this.parent.adef.pregnantPeriod;
      float var10000;
      switch (SandboxOptions.getInstance().AnimalPregnancyTime.getValue()) {
         case 1:
            var10000 = 0.01F;
            break;
         case 2:
            var10000 = 0.2F;
            break;
         case 3:
            var10000 = 0.7F;
            break;
         case 4:
         default:
            var10000 = 1.0F;
            break;
         case 5:
            var10000 = 2.0F;
            break;
         case 6:
            var10000 = 3.0F;
      }

      float var2 = var10000;
      var1 = Float.valueOf((float)var1 * var2).intValue();
      if (var1 < 8) {
         var1 = 8;
      }

      return var1;
   }

   private float getThirstReduction() {
      AnimalAllele var1 = this.parent.getUsedGene("thirstResistance");
      float var2 = this.parent.adef.thirstMultiplier;
      if (var1 == null) {
         return var2;
      } else {
         float var3 = 1.0F - var1.currentValue + 1.0F;
         if (this.parent.geneticDisorder.contains("highthirst")) {
            var2 *= 10.0F;
         }

         var2 *= var3 * this.getHungerReductionMod();
         return var2;
      }
   }

   private float getHungerReduction() {
      AnimalAllele var1 = this.parent.getUsedGene("hungerResistance");
      float var2 = this.parent.adef.hungerMultiplier;
      if (var1 == null) {
         return var2;
      } else {
         float var3 = 1.0F - var1.currentValue + 1.0F;
         if (this.parent.geneticDisorder.contains("gluttonous")) {
            var2 *= 10.0F;
         }

         var2 *= var3 * this.getHungerReductionMod();
         return var2;
      }
   }

   private float getHungerReductionMetaMod() {
      float var10000;
      switch (SandboxOptions.getInstance().AnimalMetaStatsModifier.getValue()) {
         case 1:
            var10000 = 4.0F;
            break;
         case 2:
            var10000 = 2.0F;
            break;
         case 3:
            var10000 = 1.5F;
            break;
         case 4:
         default:
            var10000 = 1.0F;
            break;
         case 5:
            var10000 = 0.7F;
            break;
         case 6:
            var10000 = 0.2F;
      }

      return var10000;
   }

   private float getHungerReductionMod() {
      float var10000;
      switch (SandboxOptions.getInstance().AnimalStatsModifier.getValue()) {
         case 1:
            var10000 = 4.0F;
            break;
         case 2:
            var10000 = 2.0F;
            break;
         case 3:
            var10000 = 1.5F;
            break;
         case 4:
         default:
            var10000 = 1.0F;
            break;
         case 5:
            var10000 = 0.7F;
            break;
         case 6:
            var10000 = 0.2F;
      }

      return var10000;
   }

   private void eatAndDrinkAfterMetaVehicle() {
      if (!this.parent.isBaby()) {
         boolean var1;
         do {
            if (!((double)this.parent.getStats().hunger >= 0.1)) {
               return;
            }

            var1 = false;
            if (this.parent.getVehicle() != null && this.eatFromVehicle()) {
               var1 = true;
            }
         } while(var1);

      }
   }

   private void eatAndDrinkAfterMeta() {
      if (this.parent.getVehicle() != null) {
         this.eatAndDrinkAfterMetaVehicle();
      } else {
         boolean var1;
         int var2;
         DesignationZoneAnimal var3;
         int var4;
         IsoFeedingTrough var8;
         while((double)this.parent.getStats().hunger >= 0.1) {
            if (this.parent.isBaby() && this.parent.adef.eatFromMother && this.parent.mother != null && this.parent.mother.isExistInTheWorld() && (double)this.parent.mother.getData().getMilkQuantity() > 0.1) {
               Stats var10000 = this.parent.getStats();
               var10000.hunger = (float)((double)var10000.hunger - 0.2);
               this.parent.getStats().hunger = Math.max(0.0F, this.parent.getStats().hunger);
               this.parent.mother.getData().setMilkQuantity(this.parent.mother.getData().getMilkQuantity() - Rand.Next(0.1F, 0.3F));
            } else {
               var1 = false;

               for(var2 = 0; var2 < this.parent.connectedDZone.size(); ++var2) {
                  var3 = (DesignationZoneAnimal)this.parent.connectedDZone.get(var2);
                  if (var3.troughs.isEmpty() && var3.foodOnGround.isEmpty()) {
                     break;
                  }

                  for(var4 = 0; var4 < var3.foodOnGround.size(); ++var4) {
                     IsoWorldInventoryObject var5 = (IsoWorldInventoryObject)var3.foodOnGround.get(var4);
                     if (this.parent.adef.eatTypeTrough != null) {
                        for(int var6 = 0; var6 < this.parent.adef.eatTypeTrough.size(); ++var6) {
                           String var7 = (String)this.parent.adef.eatTypeTrough.get(var6);
                           if (var5.getItem() instanceof Food) {
                              if (var7.equals(((Food)var5.getItem()).getFoodType()) || var7.equals(var5.getItem().getAnimalFeedType())) {
                                 this.parent.eatFromGround = var5;
                                 break;
                              }
                           } else if (var5.getItem() instanceof DrainableComboItem && var7.equals(var5.getItem().getAnimalFeedType())) {
                              this.parent.eatFromGround = var5;
                              break;
                           }
                        }

                        if (this.parent.eatFromGround != null) {
                           this.eat();
                           var1 = true;
                        }
                        break;
                     }
                  }

                  if (!var3.troughs.isEmpty()) {
                     for(var4 = 0; var4 < var3.troughs.size(); ++var4) {
                        var8 = (IsoFeedingTrough)var3.troughs.get(var4);
                        if (this.canEatFromTrough(var8) != null) {
                           this.parent.eatFromTrough = var8;
                           this.eat();
                           var1 = true;
                           break;
                        }
                     }
                  }
               }

               if (!var1) {
                  break;
               }
            }
         }

         while((double)this.parent.getStats().thirst >= 0.1) {
            var1 = false;

            for(var2 = 0; var2 < this.parent.connectedDZone.size(); ++var2) {
               var3 = (DesignationZoneAnimal)this.parent.connectedDZone.get(var2);
               if (var3.troughs.isEmpty()) {
                  break;
               }

               for(var4 = 0; var4 < var3.troughs.size(); ++var4) {
                  var8 = (IsoFeedingTrough)var3.troughs.get(var4);
                  if (var8.getWater() > 0.0F) {
                     this.parent.drinkFromTrough = var8;
                     this.drink();
                     var1 = true;
                     break;
                  }
               }

               if (!var1) {
                  break;
               }
            }

            if (!var1) {
               break;
            }
         }

      }
   }

   private boolean eatFromVehicle() {
      BaseVehicle var1 = this.parent.getVehicle();
      VehiclePart var2 = var1.getPartById("TrailerAnimalFood");
      InventoryItem var3 = null;
      if (var2 != null && var2.getItemContainer() != null) {
         for(int var4 = 0; var4 < var2.getItemContainer().getItems().size(); ++var4) {
            InventoryItem var5 = (InventoryItem)var2.getItemContainer().getItems().get(var4);
            if (this.parent.adef.eatTypeTrough != null) {
               for(int var6 = 0; var6 < this.parent.adef.eatTypeTrough.size(); ++var6) {
                  String var7 = (String)this.parent.adef.eatTypeTrough.get(var6);
                  if (var5 instanceof Food) {
                     if (var7.equals(((Food)var5).getFoodType()) || var7.equals(var5.getAnimalFeedType())) {
                        var3 = var5;
                        break;
                     }
                  } else if (var5 instanceof DrainableComboItem && var7.equals(var5.getAnimalFeedType())) {
                     var3 = var5;
                     break;
                  }
               }
            }

            if (var3 != null) {
               break;
            }
         }

         if (var3 != null) {
            this.eatItem(var3, false);
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   /** @deprecated */
   @Deprecated
   public ArrayList<IsoFeedingTrough> getRandomTroughList() {
      ArrayList var1 = new ArrayList();

      for(int var2 = 0; var2 < this.parent.connectedDZone.size(); ++var2) {
         DesignationZoneAnimal var3 = (DesignationZoneAnimal)this.parent.connectedDZone.get(var2);
         var1.addAll(var3.troughs);
      }

      shuffleList(var1);
      return var1;
   }

   public static void shuffleList(ArrayList<IsoFeedingTrough> var0) {
      int var1 = var0.size();
      Random var2 = new Random();
      var2.nextInt();

      for(int var3 = 0; var3 < var1; ++var3) {
         int var4 = var3 + var2.nextInt(var1 - var3);
         swap(var0, var3, var4);
      }

   }

   private static void swap(List<IsoFeedingTrough> var0, int var1, int var2) {
      IsoFeedingTrough var3 = (IsoFeedingTrough)var0.get(var1);
      var0.set(var1, (IsoFeedingTrough)var0.get(var2));
      var0.set(var2, var3);
   }

   public void resetEatingCheck() {
      this.parent.drinkFromTrough = null;
      this.parent.eatFromTrough = null;
      this.parent.eatFromGround = null;
      this.parent.movingToFood = null;
      this.eatingGrass = false;
      this.parent.clearVariable("idleAction");
   }

   public void eatFood(InventoryItem var1) {
      if (this.parent.isExistInTheWorld() && var1 != null) {
         if (var1 instanceof Food) {
            Food var2 = (Food)var1;
            float var3 = this.parent.getStats().hunger;
            float var4 = var2.getHungerChange();
            if (Math.abs(var4) <= var3) {
               Stats var10000 = this.parent.getStats();
               var10000.hunger += var2.getHungerChange();
            } else {
               float var5 = Math.abs(var4) - var3;
               this.parent.getStats().hunger = 0.0F;
               var2.setHungChange(-var5);
            }

            this.parent.eatFromGround = null;
            if (this.parent.getStats().hunger < 0.0F) {
               this.parent.getStats().hunger = 0.0F;
            }
         }

      }
   }

   private InventoryItem canEatFromTrough(IsoFeedingTrough var1) {
      if (this.parent.adef.eatTypeTrough != null && var1.getContainer() != null) {
         for(int var2 = 0; var2 < var1.getContainer().getItems().size(); ++var2) {
            InventoryItem var3 = (InventoryItem)var1.getContainer().getItems().get(var2);
            if (!(var3 instanceof Food) || !((Food)var3).isRotten()) {
               if (this.parent.adef.eatTypeTrough.contains("All") || this.parent.adef.eatTypeTrough.contains(var3.getFullType()) || this.parent.adef.eatTypeTrough.contains(var3.getAnimalFeedType())) {
                  return var3;
               }

               if (var3 instanceof Food && this.parent.adef.eatTypeTrough.contains(((Food)var3).getFoodType())) {
                  return var3;
               }
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public void drinkFromGround() {
      InventoryItem var1 = this.parent.eatFromGround.getItem();
      var1.getFluidContainer().removeFluid(2.0F / this.parent.adef.thirstBoost);
      Stats var10000 = this.parent.getStats();
      var10000.thirst -= 0.2F * this.parent.adef.thirstBoost;
      if (this.parent.getStats().thirst < 0.0F) {
         this.parent.getStats().thirst = 0.0F;
      }

      if (!((double)this.parent.getStats().thirst < 0.1) && !var1.getFluidContainer().isEmpty()) {
         if (this.parent.getStats().thirst > 0.1F) {
            this.parent.setVariable("idleAction", "eat");
            if (this.parent.adef.eatingTypeNbr > 0) {
               this.parent.setVariable("eatingAnim", "eat" + Rand.Next(1, this.parent.adef.eatingTypeNbr + 1));
            }
         }

      } else {
         this.resetEatingCheck();
         this.parent.setStateEventDelayTimer(0.0F);
      }
   }

   public void drink() {
      if (this.parent.drinkFromTrough != null && !(this.parent.drinkFromTrough.getWater() <= 0.0F)) {
         this.parent.drinkFromTrough.setWater(this.parent.drinkFromTrough.getWater() - 2.0F / this.parent.adef.thirstBoost);
         Stats var10000 = this.parent.getStats();
         var10000.thirst -= 0.2F * this.parent.adef.thirstBoost;
         if (this.parent.getStats().thirst < 0.0F) {
            this.parent.getStats().thirst = 0.0F;
         }

         SGlobalObjects.OnIsoObjectChangedItself("feedingTrough", this.parent.drinkFromTrough);
         if (!((double)this.parent.getStats().thirst < 0.1) && !(this.parent.drinkFromTrough.getWater() <= 0.0F)) {
            if (this.parent.getStats().thirst > 0.1F) {
               this.parent.setVariable("idleAction", "eat");
               if (this.parent.adef.eatingTypeNbr > 0) {
                  this.parent.setVariable("eatingAnim", "eat" + Rand.Next(1, this.parent.adef.eatingTypeNbr + 1));
               }
            }

         } else {
            this.resetEatingCheck();
            this.parent.setStateEventDelayTimer(0.0F);
         }
      }
   }

   public void eatItem(InventoryItem var1, boolean var2) {
      IsoFeedingTrough var3 = null;
      if (var1.getContainer() != null && var1.getContainer().parent instanceof IsoFeedingTrough) {
         var3 = (IsoFeedingTrough)var1.getContainer().parent;
      }

      Stats var10000;
      if (var1.getFluidContainer() != null && var1.getFluidContainer().isPureFluid(Fluid.Get(this.parent.getBreed().getMilkType()))) {
         for(; !var1.getFluidContainer().isEmpty() && (!(this.parent.getStats().hunger <= 0.0F) || !(this.parent.getStats().thirst <= 0.0F)); var1.getFluidContainer().removeFluid(Rand.Next(0.2F / this.parent.adef.hungerBoost, 0.5F / this.parent.adef.hungerBoost))) {
            var10000 = this.parent.getStats();
            var10000.hunger -= 0.2F;
            var10000 = this.parent.getStats();
            var10000.thirst -= 0.2F;
            if (this.parent.getStats().hunger < 0.0F) {
               this.parent.getStats().hunger = 0.0F;
            }

            if (this.parent.getStats().thirst < 0.0F) {
               this.parent.getStats().thirst = 0.0F;
            }
         }
      }

      float var5;
      float var6;
      int var7;
      float var11;
      if (var1 instanceof Food var4) {
         var5 = this.parent.getStats().hunger;
         var6 = var4.getHungerChange();
         if (Math.abs(var6 * this.parent.adef.hungerBoost) <= var5) {
            var10000 = this.parent.getStats();
            float var10002 = var6 * this.parent.adef.hungerBoost;
            var10000.hunger = this.parent.getStats().hunger + var10002;
            var10000 = this.parent.getStats();
            var10000.thirst += var4.getThirstChange();
            if (var2) {
               var1.getWorldItem().getSquare().removeWorldObject(var1.getWorldItem());

               for(var7 = 0; var7 < this.parent.connectedDZone.size(); ++var7) {
                  DesignationZoneAnimal var8 = (DesignationZoneAnimal)this.parent.connectedDZone.get(var7);
                  var8.foodOnGround.remove(var1.getWorldItem());
                  if (IsoPlayer.getInstance() != null) {
                     IsoPlayer.getInstance().setInvPageDirty(true);
                  }
               }
            } else if (var1.getContainer() != null) {
               if (var1.getContainer().getParent() != null && var1.getContainer().getParent() instanceof IsoPlayer && ((IsoPlayer)var1.getContainer().getParent()).getPrimaryHandItem() == var1) {
                  ((IsoPlayer)var1.getContainer().getParent()).setPrimaryHandItem((InventoryItem)null);
               }

               if (var1.getReplaceOnUse() != null) {
                  var1.getContainer().AddItem(var1.getReplaceOnUse());
               }

               var1.getContainer().Remove(var1);
               if (var1.getContainer() != null) {
                  var1.getContainer().setDrawDirty(true);
               }
            }
         } else {
            var11 = Math.abs(var6 * this.parent.adef.hungerBoost) - var5;
            float var12 = Math.abs(var4.getThirstChange()) - this.parent.getStats().thirst / this.parent.adef.thirstBoost;
            this.parent.getStats().hunger = 0.0F;
            var10000 = this.parent.getStats();
            var10000.thirst += var4.getThirstChange() * this.parent.adef.thirstBoost;
            var4.setHungChange(-(var11 / this.parent.adef.hungerBoost));
            if (Math.abs(var4.getThirstChange()) > 0.0F) {
               if (var12 > 0.0F) {
                  var4.setThirstChange(-var12);
               } else {
                  var4.setThirstChange(0.0F);
               }
            }

            if ((double)var4.getHungChange() > -0.01 && var1.getContainer() != null) {
               if (var1.getContainer().getParent() != null && var1.getContainer().getParent() instanceof IsoPlayer && ((IsoPlayer)var1.getContainer().getParent()).getPrimaryHandItem() == var1) {
                  ((IsoPlayer)var1.getContainer().getParent()).setPrimaryHandItem((InventoryItem)null);
               }

               if (var1.getReplaceOnUse() != null) {
                  var1.getContainer().AddItem(var1.getReplaceOnUse());
               }

               var1.getContainer().Remove(var1);
               if (var1.getContainer() != null) {
                  var1.getContainer().setDrawDirty(true);
               }
            }

            if (IsoPlayer.getInstance() != null) {
               IsoPlayer.getInstance().setInvPageDirty(true);
            }
         }
      }

      if (var1 instanceof DrainableComboItem var10) {
         var5 = this.parent.getStats().hunger;
         var6 = (float)var10.getCurrentUses() * 0.1F;
         int var13;
         if (Math.abs(var6) <= var5) {
            var10000 = this.parent.getStats();
            var10000.hunger -= var6;
            var7 = var10.getCurrentUses();

            for(var13 = 0; var13 < var7; ++var13) {
               var10.Use();
               var10000 = this.parent.getStats();
               var10000.hunger -= 0.1F;
            }

            if (var2) {
               for(var13 = 0; var13 < this.parent.connectedDZone.size(); ++var13) {
                  DesignationZoneAnimal var9 = (DesignationZoneAnimal)this.parent.connectedDZone.get(var13);
                  var9.foodOnGround.remove(var1.getWorldItem());
               }
            } else if (var1.getContainer() != null) {
               if (var1.getContainer().getParent() != null && var1.getContainer().getParent() instanceof IsoPlayer && ((IsoPlayer)var1.getContainer().getParent()).getPrimaryHandItem() == var1) {
                  ((IsoPlayer)var1.getContainer().getParent()).setPrimaryHandItem((InventoryItem)null);
               }

               if (var10.getReplaceOnDeplete() != null) {
                  var1.getContainer().AddItem(var10.getReplaceOnDeplete());
               }

               var1.getContainer().Remove(var1);
            }
         } else {
            var11 = (float)Math.round(var5 * 10.0F);

            for(var13 = 0; (float)var13 < var11; ++var13) {
               var10.Use();
            }

            this.parent.getStats().hunger = 0.0F;
         }

         if (IsoPlayer.getInstance() != null) {
            IsoPlayer.getInstance().setInvPageDirty(true);
         }
      }

      this.parent.getStats().thirst = Math.max(this.parent.getStats().thirst, 0.0F);
      if (var3 != null) {
         var3.getContainer().setDrawDirty(true);
         var3.checkOverlayAfterAnimalEat();
      }

      if (this.parent.getStats().hunger < 0.0F) {
         this.parent.getStats().hunger = 0.0F;
      }

   }

   public void eat() {
      if (this.parent.eatFromGround != null) {
         if (!this.parent.eatFromGround.isExistInTheWorld()) {
            this.parent.eatFromGround = null;
         } else {
            this.eatItem(this.parent.eatFromGround.getItem(), true);
            if (this.parent.getStats().hunger < 0.0F) {
               this.parent.getStats().hunger = 0.0F;
            }

            this.resetEatingCheck();
            if (this.parent.getStats().hunger >= this.parent.adef.thirstHungerTrigger) {
               this.parent.getBehavior().isDoingBehavior = false;
               this.parent.getBehavior().checkBehavior();
            } else {
               this.parent.getBehavior().isDoingBehavior = false;
               this.parent.getBehavior().resetBehaviorAction();
               this.parent.setStateEventDelayTimer(0.0F);
            }

         }
      } else if (this.parent.eatFromTrough != null) {
         InventoryItem var1 = this.canEatFromTrough(this.parent.eatFromTrough);
         if (var1 != null) {
            this.eatItem(var1, false);
            if (this.parent.getStats().hunger < 0.1F) {
               this.resetEatingCheck();
               this.parent.setStateEventDelayTimer(0.0F);
            } else {
               if (this.parent.getStats().hunger > this.parent.adef.thirstHungerTrigger) {
                  this.parent.setVariable("idleAction", "eat");
                  if (this.parent.adef.eatingTypeNbr > 0) {
                     this.parent.setVariable("eatingAnim", "eat" + Rand.Next(1, this.parent.adef.eatingTypeNbr + 1));
                  }
               }

               this.parent.setVariable("idleAction", "eat");
               if (this.parent.adef.eatingTypeNbr > 0) {
                  this.parent.setVariable("eatingAnim", "eat" + Rand.Next(1, this.parent.adef.eatingTypeNbr + 1));
               }

            }
         } else {
            this.resetEatingCheck();
            this.parent.setStateEventDelayTimer(0.0F);
         }
      } else {
         Stats var10000;
         if (this.parent.mother != null && this.parent.mother.getCurrentSquare() != null && this.parent.getCurrentSquare().DistToProper(this.parent.mother.getCurrentSquare()) <= 2.0F && this.parent.mother.haveEnoughMilkToFeedFrom()) {
            var10000 = this.parent.getStats();
            var10000.hunger -= 0.2F;
            var10000 = this.parent.getStats();
            var10000.thirst -= 0.2F;
            this.parent.getStats().hunger = Math.max(0.0F, this.parent.getStats().hunger);
            this.parent.getStats().thirst = Math.max(0.0F, this.parent.getStats().thirst);
            this.parent.mother.getData().setMilkQuantity(this.parent.mother.getData().getMilkQuantity() - Rand.Next(0.2F / this.parent.adef.hungerBoost, 0.5F / this.parent.adef.hungerBoost));
            if (!(this.parent.getStats().hunger < 0.1F) && !(this.parent.getStats().thirst < 0.1F) && this.parent.mother.haveEnoughMilkToFeedFrom()) {
               if ((double)this.parent.getStats().hunger > 0.05 && this.parent.mother.haveEnoughMilkToFeedFrom()) {
                  this.parent.setVariable("idleAction", "eat");
                  this.parent.setVariable("eatingAnim", "feed");
               }

            } else {
               this.resetEatingCheck();
               this.parent.setStateEventDelayTimer(0.0F);
               this.parent.mother.getBehavior().blockMovement = false;
            }
         } else if (this.eatingGrass) {
            var10000 = this.parent.getStats();
            var10000.hunger -= 0.15F;
            if (this.parent.getCurrentSquare() != null) {
               if (GameClient.bClient && this.parent.isLocal()) {
                  INetworkPacket.send(PacketTypes.PacketType.AnimalEvent, this.parent, this.parent.getCurrentSquare());
               } else {
                  this.parent.getCurrentSquare().removeGrass();
               }
            }

            if (this.parent.getStats().hunger > 0.0F) {
               this.parent.setStateEventDelayTimer(10.0F);
               this.parent.getBehavior().wanderMulMod = this.parent.adef.wanderMul / 4.0F;
            }

            this.parent.setStateEventDelayTimer(0.0F);
            this.resetEatingCheck();
         }
      }
   }

   public boolean canBePregnant() {
      if (!this.isFemale()) {
         return false;
      } else if (this.getTimeBeforeNextPregnancy() > 0 && this.lastPregnancyTime > 0L && GameTime.getInstance().getCalender().getTimeInMillis() - this.lastPregnancyTime < 86400000L * (long)this.getTimeBeforeNextPregnancy()) {
         return false;
      } else if (this.parent.isGeriatric()) {
         return false;
      } else if (this.parent.isInMatingSeason() && this.parent.adef.eggsPerDay == 0 && !StringUtils.isNullOrEmpty(this.parent.adef.babyType) && this.parent.getCurrentSquare() != null && this.getDaysSurvived() >= this.parent.getMinAgeForBaby() && !this.isPregnant()) {
         return true;
      } else {
         return this.parent.isInMatingSeason() && this.parent.adef.eggsPerDay > 0 && this.fertilizedTime == 0 && !StringUtils.isNullOrEmpty(this.parent.adef.babyType) && this.parent.getCurrentSquare() != null && this.getDaysSurvived() >= this.parent.getMinAgeForBaby() && !this.fertilized;
      }
   }

   public void tryInseminateInMeta(PZCalendar var1) {
      if (!this.parent.isFemale()) {
         this.findFemaleToInseminate(var1);
         if (!this.animalToInseminate.isEmpty()) {
            IsoAnimal var2 = (IsoAnimal)this.animalToInseminate.get(Rand.Next(0, this.animalToInseminate.size()));
            if (var2 != null) {
               var2.fertilize(this.parent, false);
            }

         }
      }
   }

   public void findFemaleToInseminate(PZCalendar var1) {
      if (var1 == null) {
         var1 = GameTime.instance.getCalender();
      }

      if (this.parent.getCurrentSquare() != null) {
         if (this.animalToInseminate.isEmpty() && !this.parent.isFemale() && !StringUtils.isNullOrEmpty(this.parent.adef.mate) && this.getDaysSurvived() >= this.parent.getMinAgeForBaby() && this.getLastImpregnatePeriod(var1) == 0) {
            for(int var2 = 0; var2 < this.parent.connectedDZone.size(); ++var2) {
               DesignationZoneAnimal var3 = (DesignationZoneAnimal)this.parent.connectedDZone.get(var2);

               for(int var4 = 0; var4 < var3.animals.size(); ++var4) {
                  IsoAnimal var5 = (IsoAnimal)var3.animals.get(var4);
                  if (var5 != null && var5 != this.parent && this.parent.adef.mate.equals(var5.getAnimalType()) && var5.getData().canBePregnant()) {
                     this.animalToInseminate.add(var5);
                  }
               }
            }
         }

      }
   }

   public void initSize() {
      this.setSize(this.getMinSize());
      float var1 = (this.getMaxSize() - this.getMinSize()) / (float)this.currentStage.getAgeToGrow(this.parent);
      this.setSize(Math.max(this.getMinSize(), Math.min(this.getMaxSize(), this.getSize() + var1 * (float)(this.getAge() - this.parent.adef.minAge))));
      this.setSize(0.1F);
   }

   public void initWeight() {
      this.setWeight(this.getMinWeight());
      float var1 = (this.getMaxWeight() - this.getMinWeight()) / (float)this.currentStage.getAgeToGrow(this.parent);
      this.setWeight(Math.max(this.getMinWeight(), Math.min(this.getMaxWeight(), this.getWeight() + var1 * (float)(this.getAge() - this.parent.adef.minAge))));
   }

   public void initStage() {
      if (this.currentStage == null) {
         ArrayList var1 = this.getGrowStage();
         if (var1 != null && !var1.isEmpty()) {
            for(int var2 = 0; var2 < var1.size(); ++var2) {
               AnimalGrowStage var3 = (AnimalGrowStage)var1.get(var2);
               if (var3.stage.equals(this.parent.getAnimalType())) {
                  this.currentStage = var3;
                  break;
               }
            }
         }

      }
   }

   public void grow(String var1) {
      if (this.parent.mother != null && this.parent.mother.getBabies() != null) {
         this.parent.mother.getBabies().remove(this.parent);
      }

      IsoAnimal var2 = new IsoAnimal(this.parent.getCell(), (int)this.parent.getX(), (int)this.parent.getY(), (int)this.parent.getZ(), var1, this.breed);
      var2.getData().setAge(this.getAge());
      var2.setHoursSurvived((double)(this.getAge() * 24));
      var2.getData().currentStageNbr = this.currentStageNbr + 1;
      var2.getData().setAttachedPlayer(this.attachedPlayer);
      var2.getData().setAttachedTree(this.attachedTree);
      var2.getStats().setHunger(this.parent.getStats().hunger);
      var2.getStats().setThirst(this.parent.getStats().thirst);
      var2.playerAcceptanceList = this.parent.playerAcceptanceList;
      var2.stressLevel = this.parent.stressLevel;
      var2.fullGenome = this.parent.fullGenome;
      AnimalGene.checkGeneticDisorder(var2);
      var2.getData().initSize();
      var2.setCustomName(this.parent.getCustomName());
      var2.setFemale(this.parent.isFemale());
      var2.setVehicle(this.parent.getVehicle());
      float var3 = (this.size - this.getMinSize()) / (this.getMaxSize() - this.getMinSize());
      if ((double)var3 > 0.7) {
         var3 -= 0.7F;
         var2.getData().setSize(var2.getData().getSize() * (1.0F + var3));
      }

      var2.setIsInvincible(this.parent.isInvincible());
      if (this.parent.getVehicle() == null) {
         this.parent.removeFromWorld();
         this.parent.removeFromSquare();
         var2.addToWorld();
      } else {
         this.parent.getVehicle().replaceGrownAnimalInTrailer(this.parent, var2);
      }

   }

   public int getDaysSurvived() {
      float var1 = 0.0F;
      var1 = Math.max(var1, (float)this.parent.getHoursSurvived());
      int var2 = (int)var1 / 24;
      return var2;
   }

   public boolean canHaveBaby() {
      return this.isFemale() && this.parent.adef.babyType != null && this.getDaysSurvived() >= this.parent.getMinAgeForBaby();
   }

   public void init() {
      this.initStage();
      this.initSize();
      this.initWeight();
      this.lastHourCheck = GameTime.getInstance().getHour();
   }

   public void setAttachedPlayer(IsoPlayer var1) {
      if (var1 != null && (Core.getInstance().animalCheat || var1.getInventory().getFirstType("Rope") != null)) {
         var1.setPrimaryHandItem(var1.getInventory().getFirstType("Rope"));
      }

      this.attachedPlayer = var1;
   }

   public IsoPlayer getAttachedPlayer() {
      return this.attachedPlayer;
   }

   public void setAttachedTree(IsoObject var1) {
      this.attachedTree = var1;
   }

   public IsoObject getAttachedTree() {
      return this.attachedTree;
   }

   public AnimalBreed getBreed() {
      return this.breed;
   }

   public void setBreed(AnimalBreed var1) {
      this.breed = var1;
   }

   public float getMilkQuantity() {
      return this.milkQty;
   }

   public void setMilkQuantity(float var1) {
      if (var1 < this.milkQty) {
         if (this.maxMilkActual < this.getMaxMilk()) {
            this.maxMilkActual += Rand.Next(0.01F, 0.03F);
            this.maxMilkActual = Math.min(this.maxMilkActual, this.getMaxMilk());
         }

         if (this.canHaveMilk) {
            this.updateLastTimeMilked();
         }
      }

      this.milkQty = Math.min(Math.max(var1, 0.0F), this.maxMilkActual);
   }

   public void setSize(float var1) {
      var1 = Math.min(this.getMaxSize(), Math.max(this.getMinSize(), var1));
      this.size = var1;
   }

   public void setSizeForced(float var1) {
      this.size = var1;
   }

   public float getSize() {
      return this.size;
   }

   public void setAge(int var1) {
      this.age = var1;
   }

   public int getAge() {
      return this.age;
   }

   public ArrayList<AnimalGrowStage> getGrowStage() {
      return this.parent.adef.stages;
   }

   public float getWeight() {
      return this.weight;
   }

   public boolean isFemale() {
      return this.parent.getDescriptor().isFemale();
   }

   public String getAgeString(IsoGameCharacter var1) {
      return "Adult";
   }

   public boolean canHaveMilk() {
      return this.canHaveMilk;
   }

   public void setCanHaveMilk(boolean var1) {
      if ((double)this.getGeriatricPercentage() > 0.8) {
         var1 = false;
      }

      if (this.parent.geneticDisorder.contains("nomilk")) {
         var1 = false;
      }

      if (!this.canHaveMilk && var1) {
         this.updateLastTimeMilked();
      }

      if (var1 && this.maxMilkActual == 0.0F) {
         this.maxMilkActual = this.getMinMilk();
      }

      this.canHaveMilk = var1;
   }

   public void setPregnant(boolean var1) {
      this.pregnant = var1;
   }

   public boolean isPregnant() {
      return this.pregnant;
   }

   public int getPregnancyTime() {
      return this.pregnantTime;
   }

   public void setPregnancyTime(int var1) {
      this.pregnantTime = var1;
   }

   public boolean isFertilized() {
      return this.fertilized;
   }

   public void setFertilized(boolean var1) {
      this.fertilized = var1;
   }

   public int getFertilizedTime() {
      return this.fertilizedTime;
   }

   public int setFertilizedTime(int var1) {
      return this.fertilizedTime = var1;
   }

   public float getWoolQuantity() {
      return this.woolQty;
   }

   public void setMaleGenome(HashMap<String, AnimalGene> var1) {
      this.maleGenome = var1;
   }

   public void setWoolQuantity(float var1, boolean var2) {
      if (var2 || this.parent.getAge() >= 200) {
         if (!var2 && this.parent.geneticDisorder.contains("nowool")) {
            var1 = 0.0F;
         }

         float var3 = this.woolQty;
         boolean var4 = var3 >= this.getMaxWool() / 2.0F;
         this.woolQty = Math.min(var1, this.getMaxWool());
         boolean var5 = this.woolQty >= this.getMaxWool() / 2.0F;
         if (var4 != var5) {
            this.parent.clearVariable("idleAction");
            this.parent.getPathFindBehavior2().reset();
            this.parent.resetModel();
         }

      }
   }

   public void setWoolQuantity(float var1) {
      this.setWoolQuantity(var1, false);
   }

   public IsoHutch getRegionHutch() {
      for(int var1 = 0; var1 < this.parent.connectedDZone.size(); ++var1) {
         DesignationZoneAnimal var2 = (DesignationZoneAnimal)this.parent.connectedDZone.get(var1);
         if (!var2.getHutchsConnected().isEmpty()) {
            for(int var3 = 0; var3 < var2.getHutchsConnected().size(); ++var3) {
               IsoHutch var4 = (IsoHutch)var2.getHutchsConnected().get(var3);

               for(int var5 = 0; var5 < this.parent.adef.hutches.size(); ++var5) {
                  String var6 = (String)this.parent.adef.hutches.get(var5);
                  if (var6.equals(var4.type)) {
                     return var4;
                  }
               }
            }
         }
      }

      return null;
   }

   public float getGeriatricPercentage() {
      if ((float)this.getAge() < this.getMaxAgeGeriatric() / 10.0F * 7.0F) {
         return 0.0F;
      } else {
         float var1 = (float)this.getAge() - this.getMaxAgeGeriatric() / 10.0F * 7.0F;
         float var2 = this.getMaxAgeGeriatric() - this.getMaxAgeGeriatric() / 10.0F * 7.0F;
         return var1 > var2 ? 1.0F : var1 / var2;
      }
   }

   public float getMaxAgeGeriatric() {
      int var1 = this.parent.adef.maxAgeGeriatric;
      AnimalAllele var2 = this.parent.getUsedGene("lifeExpectancy");
      float var3 = 1.0F;
      if (var2 != null) {
         var3 = var2.currentValue;
      }

      if (this.parent.geneticDisorder.contains("poorlife")) {
         var1 /= 3;
      }

      if (this.parent.geneticDisorder.contains("dwarf")) {
         var1 /= 2;
      }

      float var4 = 0.25F - var3 / 4.0F + 1.0F;
      return (float)((int)((float)var1 * var4));
   }

   public float getMinSize() {
      float var1 = this.parent.adef.minSize;
      AnimalAllele var2 = this.parent.getUsedGene("maxSize");
      if (var2 == null) {
         return var1;
      } else {
         if (this.parent.geneticDisorder.contains("dwarf")) {
            if (this.parent.isBaby()) {
               var1 /= 1.5F;
            } else {
               var1 /= 2.2F;
            }
         }

         return var1 * var2.currentValue;
      }
   }

   public float getMaxSize() {
      float var1 = this.parent.adef.maxSize;
      AnimalAllele var2 = this.parent.getUsedGene("maxSize");
      if (var2 == null) {
         return var1;
      } else {
         if (this.parent.geneticDisorder.contains("dwarf")) {
            if (this.parent.isBaby()) {
               var1 /= 1.5F;
            } else {
               var1 /= 2.2F;
            }
         }

         return var1 * var2.currentValue;
      }
   }

   public float getMinWeight() {
      float var1 = this.parent.adef.minWeight;
      AnimalAllele var2 = this.parent.getUsedGene("maxWeight");
      if (var2 == null) {
         return var1;
      } else {
         if (this.parent.geneticDisorder.contains("skinny")) {
            var1 /= 3.0F;
         }

         return var1 * var2.currentValue;
      }
   }

   public float getMaxWeight() {
      float var1 = this.parent.adef.maxWeight;
      AnimalAllele var2 = this.parent.getUsedGene("maxWeight");
      if (var2 == null) {
         return var1;
      } else {
         if (this.parent.geneticDisorder.contains("skinny")) {
            var1 /= 3.0F;
         }

         return var1 * var2.currentValue;
      }
   }

   public void setWeight(float var1) {
      var1 = Math.min(this.getMaxWeight(), Math.max(this.getMinWeight(), var1));
      this.weight = var1;
   }

   public int getHutchPosition() {
      return this.hutchPosition;
   }

   public void setHutchPosition(int var1) {
      this.hutchPosition = var1;
   }

   public int getPreferredHutchPosition() {
      return this.preferredHutchPosition;
   }

   public void setPreferredHutchPosition(int var1) {
      this.preferredHutchPosition = var1;
   }

   public int getTimeBeforeNextPregnancy() {
      int var1 = this.parent.adef.timeBeforeNextPregnancy;
      if (var1 > 0) {
         float var10000;
         switch (SandboxOptions.getInstance().AnimalPregnancyTime.getValue()) {
            case 1:
               var10000 = 0.01F;
               break;
            case 2:
               var10000 = 0.2F;
               break;
            case 3:
               var10000 = 0.7F;
               break;
            case 4:
            default:
               var10000 = 1.0F;
               break;
            case 5:
               var10000 = 2.0F;
               break;
            case 6:
               var10000 = 3.0F;
         }

         float var2 = var10000;
         var1 = (int)((float)var1 * var2);
      }

      return var1;
   }

   public String getLastPregnancyPeriod() {
      int var1 = this.getTimeBeforeNextPregnancy();
      if (var1 > 0 && this.lastPregnancyTime > 0L) {
         int var2 = Long.valueOf((this.lastPregnancyTime + (long)var1 * 86400000L - GameTime.getInstance().getCalender().getTimeInMillis()) / 86400000L).intValue();
         return var2 > 0 ? "" + var2 : null;
      } else {
         return null;
      }
   }

   public void updateLastPregnancyTime() {
      this.lastPregnancyTime = GameTime.getInstance().getCalender().getTimeInMillis();
   }

   public int getLastImpregnatePeriod(PZCalendar var1) {
      if (var1 == null) {
         var1 = GameTime.getInstance().getCalender();
      }

      return this.parent.adef.minAgeForBaby != 0 && this.getDaysSurvived() >= this.parent.getMinAgeForBaby() ? this.lastImpregnateTime : -1;
   }

   public Float getLastTimeMilkedInHour() {
      return (float)(GameTime.getInstance().getCalender().getTimeInMillis() - this.lastMilkTimer) / 3600000.0F / 24.0F;
   }

   public void updateLastTimeMilked() {
      this.lastMilkTimer = GameTime.getInstance().getCalender().getTimeInMillis();
   }

   public String getDebugBehaviorString() {
      String var1 = this.parent.getFullName() + " \r\n \r\n";
      if (this.parent.isAnimalSitting()) {
         var1 = var1 + "Animal is sitting. \r\n";
      } else {
         var1 = var1 + "Next wander in " + (int)this.parent.getStateEventDelayTimer() + ". \r\n";
      }

      if (this.parent.getBehavior().blockMovement) {
         var1 = var1 + "Animal is currently blocked from moving. (" + this.parent.getBehavior().blockedFor + ") \r\n";
      }

      if (this.parent.getBehavior().isDoingBehavior) {
         var1 = var1 + "Failcheck Behavior: " + (this.parent.getBehavior().behaviorMaxTime - this.parent.getBehavior().behaviorFailsafe) + "\r\n";
         if (this.parent.getBehavior().behaviorObject != null) {
            if (this.parent.getBehavior().behaviorObject instanceof IsoFeedingTrough) {
               var1 = var1 + "Animal current behavior: " + this.parent.getBehavior().behaviorAction + " at " + ((IsoFeedingTrough)this.parent.getBehavior().behaviorObject).getSquare().getX() + "," + ((IsoFeedingTrough)this.parent.getBehavior().behaviorObject).getSquare().getY() + "\r\n";
            } else if (this.parent.getBehavior().behaviorObject instanceof IsoWorldInventoryObject) {
               var1 = var1 + "Animal current behavior: " + this.parent.getBehavior().behaviorAction + " at " + ((IsoWorldInventoryObject)this.parent.getBehavior().behaviorObject).getSquare().getX() + "," + ((IsoWorldInventoryObject)this.parent.getBehavior().behaviorObject).getSquare().getY() + "\r\n";
            } else if (this.parent.getBehavior().behaviorObject instanceof IsoAnimal) {
               var1 = var1 + "Animal current behavior: " + this.parent.getBehavior().behaviorAction + " at " + ((IsoAnimal)this.parent.getBehavior().behaviorObject).getSquare().getX() + "," + ((IsoAnimal)this.parent.getBehavior().behaviorObject).getSquare().getY() + "\r\n";
            } else if (this.parent.getBehavior().behaviorObject instanceof IsoHutch) {
               if (this.parent.getBehavior().hutchPathTimer > -1) {
                  var1 = var1 + "Animal is waiting to enter hutch in " + this.parent.getBehavior().hutchPathTimer + " at " + (((IsoHutch)this.parent.getBehavior().behaviorObject).getSquare().getX() + ((IsoHutch)this.parent.getBehavior().behaviorObject).getEnterSpotX()) + "," + (((IsoHutch)this.parent.getBehavior().behaviorObject).getSquare().getY() + ((IsoHutch)this.parent.getBehavior().behaviorObject).getEnterSpotY()) + "\r\n";
               } else {
                  var1 = var1 + "Animal is going to enter hutch at " + (((IsoHutch)this.parent.getBehavior().behaviorObject).getSquare().getX() + ((IsoHutch)this.parent.getBehavior().behaviorObject).getEnterSpotX()) + "," + (((IsoHutch)this.parent.getBehavior().behaviorObject).getSquare().getY() + ((IsoHutch)this.parent.getBehavior().behaviorObject).getEnterSpotY()) + "\r\n";
               }
            }
         }
      } else if (this.eatingGrass) {
         var1 = var1 + "Animal trying to eat grass \r\n";
      } else {
         var1 = var1 + "Next behavior action check in: " + Math.round(this.parent.getBehavior().behaviorCheckTimer) + "\r\n";
      }

      if (this.parent.fightingOpponent != null) {
         var1 = var1 + "has a fighter opponent";
         if (this.parent.fightingOpponent instanceof IsoAnimal && ((IsoAnimal)this.parent.fightingOpponent).fightingOpponent == null) {
            var1 = var1 + " but the other don't know it!";
         }

         var1 = var1 + "\r\n";
      }

      if (this.parent.getStats().thirst >= 0.9F || this.parent.getStats().hunger >= 0.9F) {
         if (this.parent.getThumpDelay() == 0.0F) {
            var1 = var1 + "Animal will try to destroy walls due to hunger/thirst. \r\n";
         } else {
            var1 = var1 + "Animal will try to destroy walls due to hunger/thirst in: " + Math.round(this.parent.getThumpDelay()) + " \r\n";
         }
      }

      if (this.goingToMom) {
         var1 = var1 + "Pathing to mom. (" + this.goingToMomTimer + ") \r\n";
         if (this.parent.isAnimalEating()) {
            var1 = var1 + "Feeding from mom. \r\n";
         }
      }

      if (this.femaleToCheck != null) {
         var1 = var1 + "Pathing to fertilize " + this.femaleToCheck.getFullName() + ". \r\n";
      }

      if (this.eatingGrass) {
         var1 = var1 + "Try to eat grass. \r\n";
         if (this.parent.isAnimalEating()) {
            var1 = var1 + "Eating grass on ground. \r\n";
         }
      }

      if (this.parent.eatFromTrough != null && this.parent.isAnimalEating()) {
         var1 = var1 + "Eating food in trough. \r\n";
      }

      if ((this.parent.eatFromGround != null || this.parent.movingToFood != null) && this.parent.isAnimalEating()) {
         var1 = var1 + "Eating food on ground. \r\n";
      }

      if (this.parent.drinkFromTrough != null && this.parent.isAnimalEating()) {
         var1 = var1 + "Drinking from trough. \r\n";
      }

      if (this.parent.alertedChr != null) {
         var1 = var1 + "is alerted (" + this.parent.getBehavior().lastAlerted + ") \r\n";
      }

      if (this.parent.getBehavior().attackAnimalTimer > 0.0F) {
         var1 = var1 + "delay before attacking again: " + Math.round(this.parent.getBehavior().attackAnimalTimer) + " \r\n";
      }

      var1 = var1 + "Current State: " + this.parent.getCurrentState().toString() + " \r\n";
      return var1;
   }

   public boolean isInLayingEggPeriod(PZCalendar var1) {
      if (!SandboxOptions.getInstance().AnimalMatingSeason.getValue()) {
         return true;
      } else {
         return this.parent.adef.layEggPeriodStart > -1 && var1.get(2) + 1 == this.parent.adef.layEggPeriodStart;
      }
   }

   public boolean haveLayingEggPeriod() {
      if (!SandboxOptions.getInstance().AnimalMatingSeason.getValue()) {
         return false;
      } else {
         return this.parent.adef.layEggPeriodStart > -1;
      }
   }

   public int getClutchSize() {
      return this.clutchSize;
   }
}
