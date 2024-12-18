package zombie.randomizedWorld.randomizedRanch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.characters.animals.AnimalDefinitions;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.animals.datas.AnimalBreed;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.areas.DesignationZoneAnimal;
import zombie.iso.zones.Zone;
import zombie.randomizedWorld.RandomizedWorldBase;
import zombie.util.StringUtils;

public class RandomizedRanchBase extends RandomizedWorldBase {
   public boolean alwaysDo = false;
   public static final int baseChance = 15;
   public static int totalChance = 0;
   public static final String ranchStory = "Ranch";
   public int chance = 0;

   public RandomizedRanchBase() {
   }

   public static boolean checkRanchStory(Zone var0, boolean var1) {
      if ("Ranch".equals(var0.type) && var0.isFullyStreamed() && var0.hourLastSeen == 0) {
         DesignationZoneAnimal var2 = new DesignationZoneAnimal("Ranch", var0.x, var0.y, var0.z, var0.x + var0.getWidth(), var0.y + var0.getHeight());
         ArrayList var3 = DesignationZoneAnimal.getAllDZones((ArrayList)null, var2, (DesignationZoneAnimal)null);

         for(int var4 = 0; var4 < var3.size(); ++var4) {
            DesignationZoneAnimal var5 = (DesignationZoneAnimal)var3.get(var4);
            if (var5.hourLastSeen > 0) {
               var2.setName(var5.getName());
               var2.hourLastSeen = var5.hourLastSeen;
            }
         }

         if (var2.hourLastSeen == 0) {
            doRandomRanch(var0, var1, var2);
         }

         ++var2.hourLastSeen;
         ++var0.hourLastSeen;
         return true;
      } else {
         return false;
      }
   }

   private static boolean doRandomRanch(Zone var0, boolean var1, DesignationZoneAnimal var2) {
      ++var0.hourLastSeen;
      byte var3 = 6;
      switch (SandboxOptions.instance.AnimalRanchChance.getValue()) {
         case 1:
            return false;
         case 2:
            var3 = 7;
         case 3:
         default:
            break;
         case 4:
            var3 = 20;
            break;
         case 5:
            var3 = 55;
            break;
         case 6:
            var3 = 85;
            break;
         case 7:
            var3 = 120;
      }

      if (var1) {
         var3 = 100;
      }

      if (Rand.Next(100) < var3) {
         randomizeRanch(var0, var2);
      }

      return false;
   }

   public static RanchZoneDefinitions getRandomDef(String var0) {
      RanchZoneDefinitions var1 = null;
      HashMap var3 = RanchZoneDefinitions.getDefs();
      int var4 = Rand.Next(RanchZoneDefinitions.totalChance);
      int var5 = 0;
      ArrayList var2;
      if (!StringUtils.isNullOrEmpty(var0)) {
         int var9 = 0;
         var2 = (ArrayList)var3.get(var0);
         if (var2 == null) {
            DebugLog.Animal.debugln(var0 + " wasn't found in the RanchZoneDefinitions");
            return null;
         } else {
            int var10;
            for(var10 = 0; var10 < var2.size(); ++var10) {
               var9 += ((RanchZoneDefinitions)var2.get(var10)).chance;
            }

            var4 = Rand.Next(var9);

            for(var10 = 0; var10 < var2.size(); ++var10) {
               var1 = (RanchZoneDefinitions)var2.get(var10);
               if (var1.chance + var5 >= var4) {
                  break;
               }

               var5 += var1.chance;
               var1 = null;
            }

            return var1;
         }
      } else {
         Iterator var6 = var3.keySet().iterator();

         while(var6.hasNext() && var1 == null) {
            String var7 = (String)var6.next();
            var2 = (ArrayList)var3.get(var7);

            for(int var8 = 0; var8 < var2.size(); ++var8) {
               var1 = (RanchZoneDefinitions)var2.get(var8);
               if (var1.chance + var5 >= var4) {
                  return var1;
               }

               var5 += var1.chance;
               var1 = null;
            }
         }

         return null;
      }
   }

   public static void randomizeRanch(Zone var0, DesignationZoneAnimal var1) {
      String var2 = var0.name;
      RanchZoneDefinitions var3 = getRandomDef(var2);
      if (var3 == null) {
         DebugLog.Animal.debugln("No def was found for this ranch " + var2 + " was found in the RanchZoneDefinitions");
      } else {
         if (!var3.possibleDef.isEmpty()) {
            var3 = getDefInPossibleDefList(var3.possibleDef);
         }

         AnimalDefinitions var4 = AnimalDefinitions.getDef(var3.femaleType);
         AnimalDefinitions var5 = AnimalDefinitions.getDef(var3.maleType);
         if (var4 == null) {
            DebugLog.Animal.debugln("No female def was found for " + var3.femaleType);
         } else if (var5 == null) {
            DebugLog.Animal.debugln("No male def was found for " + var3.maleType);
         } else {
            AnimalBreed var6 = null;
            boolean var7 = true;
            if (!StringUtils.isNullOrEmpty(var3.forcedBreed)) {
               var6 = var4.getBreedByName(var3.forcedBreed);
               var7 = false;
               if (var6 == null) {
                  DebugLog.Animal.debugln("No breed def was found for " + var3.forcedBreed + " taking random one");
                  var7 = true;
               }
            }

            int var8 = Rand.Next(var3.minFemaleNb, var3.maxFemaleNb + 1);
            int var9 = Rand.Next(var3.minMaleNb, var3.maxMaleNb + 1);
            if (Rand.Next(100) > var3.maleChance) {
               var9 = 0;
            }

            int var10;
            String var10001;
            for(var10 = 0; var10 < var8; ++var10) {
               if (var7) {
                  var6 = getRandomBreed(var4);
               }

               IsoGridSquare var11 = var0.getRandomFreeSquareInZone();
               if (var11 == null) {
                  DebugLog.Animal.debugln("No free square was found in the zone.");
                  return;
               }

               IsoAnimal var12 = new IsoAnimal(IsoWorld.instance.getCell(), var11.x, var11.y, var0.z, var4.animalTypeStr, var6);
               var12.setWild(false);
               var12.addToWorld();
               var12.randomizeAge();
               if (Core.getInstance().animalCheat) {
                  var10001 = Translator.getText("IGUI_AnimalType_" + var12.getAnimalType());
                  var12.setCustomName(var10001 + " " + var12.getAnimalID());
               }

               IsoAnimal var13 = null;
               if (var12.getData().canHaveBaby() && Rand.Next(100) <= var3.chanceForBaby) {
                  var13 = var12.addBaby();
                  var13.setWild(false);
                  if (Core.getInstance().animalCheat) {
                     var13.setCustomName(Translator.getText("IGUI_AnimalType_Baby", var13.mother.getFullName()));
                  }

                  if (var12.canBeMilked()) {
                     var12.getData().setMilkQuantity(Rand.Next(5.0F, var12.getData().getMaxMilk()));
                  }

                  var13.randomizeAge();
               }

               if (GameTime.getInstance().getWorldAgeDaysSinceBegin() > 60.0) {
                  int var14 = Math.max(0, 190 - (int)GameTime.getInstance().getWorldAgeDaysSinceBegin());
                  if (Rand.NextBool(var14)) {
                     var12.setHealth(0.0F);
                     if (var13 != null) {
                        var13.setHealth(0.0F);
                     }
                  }
               }
            }

            for(var10 = 0; var10 < var9; ++var10) {
               if (var7) {
                  var6 = getRandomBreed(var5);
               }

               IsoAnimal var15 = new IsoAnimal(IsoWorld.instance.getCell(), Rand.Next(var0.x, var0.x + var0.getWidth()), Rand.Next(var0.y, var0.y + var0.getHeight()), var0.z, var5.animalTypeStr, var6);
               if (Core.getInstance().animalCheat) {
                  var10001 = Translator.getText("IGUI_AnimalType_" + var15.getAnimalType());
                  var15.setCustomName(var10001 + " " + var15.getAnimalID());
               }

               var15.addToWorld();
               var15.randomizeAge();
               if (GameTime.getInstance().getWorldAgeDaysSinceBegin() > 60.0) {
                  int var16 = Math.max(0, 250 - (int)GameTime.getInstance().getWorldAgeDaysSinceBegin());
                  if (Rand.NextBool(var16)) {
                     var15.setHealth(0.0F);
                  }
               }
            }

            var1.setName(Translator.getText("UI_Ranch", Translator.getText("IGUI_AnimalType_Global_" + var3.globalName), Rand.Next(10000)));
         }
      }
   }

   private static RanchZoneDefinitions getDefInPossibleDefList(ArrayList<RanchZoneDefinitions> var0) {
      int var2 = 0;

      int var3;
      for(var3 = 0; var3 < var0.size(); ++var3) {
         var2 += ((RanchZoneDefinitions)var0.get(var3)).chance;
      }

      var3 = Rand.Next(var2);
      int var4 = 0;

      for(int var5 = 0; var5 < var0.size(); ++var5) {
         RanchZoneDefinitions var1 = (RanchZoneDefinitions)var0.get(var5);
         if (var1.chance + var4 >= var3) {
            return var1;
         }

         var4 += var1.chance;
         var1 = null;
      }

      return null;
   }

   private static AnimalBreed getRandomBreed(AnimalDefinitions var0) {
      return (AnimalBreed)var0.getBreeds().get(Rand.Next(0, var0.getBreeds().size()));
   }

   public boolean isValid() {
      return true;
   }
}
