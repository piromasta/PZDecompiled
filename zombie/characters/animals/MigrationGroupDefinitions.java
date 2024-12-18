package zombie.characters.animals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.GameTime;
import zombie.Lua.LuaManager;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.iso.IsoWorld;

public class MigrationGroupDefinitions {
   public String type;
   public String male;
   public String female;
   public String baby;
   public int minAnimal = 0;
   public int maxAnimal = 0;
   public int maxMale = 0;
   public int babyChance = 0;
   public int minTimeBeforeEat = 10;
   public int maxTimeBeforeEat = 60;
   public int timeToEat = 30;
   public int minTimeBeforeSleep = 180;
   public int maxTimeBeforeSleep = 600;
   public ArrayList<String> sleepPeriodStart;
   public ArrayList<String> sleepPeriodEnd;
   public ArrayList<String> eatPeriodStart;
   public ArrayList<String> eatPeriodEnd;
   public int timeToSleep = 30;
   public float speed = 1.0F;
   public int trackChance = 200;
   public int poopChance = 200;
   public int brokenTwigsChance = 200;
   public int herbGrazeChance = 200;
   public int furChance = 200;
   public int flatHerbChance = 200;
   ArrayList<String> possibleBreed;
   ArrayList<MigrationGroupDefinitionsGrouped> group;
   public static HashMap<String, MigrationGroupDefinitions> migrationDef;

   public MigrationGroupDefinitions() {
   }

   public static HashMap<String, MigrationGroupDefinitions> getMigrationDefs() {
      if (migrationDef == null) {
         loadMigrationsDefinitions();
      }

      return migrationDef;
   }

   public static void loadMigrationsDefinitions() {
      migrationDef = new HashMap();
      KahluaTableImpl var0 = (KahluaTableImpl)LuaManager.env.rawget("MigrationGroupDefinitions");
      if (var0 != null) {
         KahluaTableIterator var1 = var0.iterator();

         while(true) {
            while(var1.advance()) {
               MigrationGroupDefinitions var2 = new MigrationGroupDefinitions();
               var2.type = var1.getKey().toString();
               KahluaTableImpl var3 = (KahluaTableImpl)var1.getValue();
               if (var3.rawget("groups") != null) {
                  var2.group = loadGroup((KahluaTableImpl)var3.rawget("groups"));
                  migrationDef.put(var2.type, var2);
               } else {
                  KahluaTableIterator var4 = var3.iterator();

                  while(var4.advance()) {
                     String var5 = var4.getKey().toString();
                     Object var6 = var4.getValue();
                     String var7 = var6.toString().trim();
                     if ("male".equalsIgnoreCase(var5)) {
                        var2.male = var7;
                     }

                     if ("female".equalsIgnoreCase(var5)) {
                        var2.female = var7;
                     }

                     if ("baby".equalsIgnoreCase(var5)) {
                        var2.baby = var7;
                     }

                     if ("maxAnimal".equalsIgnoreCase(var5)) {
                        var2.maxAnimal = Float.valueOf(var7).intValue();
                     }

                     if ("minAnimal".equalsIgnoreCase(var5)) {
                        var2.minAnimal = Float.valueOf(var7).intValue();
                     }

                     if ("maxMale".equalsIgnoreCase(var5)) {
                        var2.maxMale = Float.valueOf(var7).intValue();
                     }

                     if ("babyChance".equalsIgnoreCase(var5)) {
                        var2.babyChance = Float.valueOf(var7).intValue();
                     }

                     if ("minTimeBeforeEat".equalsIgnoreCase(var5)) {
                        var2.minTimeBeforeEat = Float.valueOf(var7).intValue();
                     }

                     if ("maxTimeBeforeEat".equalsIgnoreCase(var5)) {
                        var2.maxTimeBeforeEat = Float.valueOf(var7).intValue();
                     }

                     if ("timeToEat".equalsIgnoreCase(var5)) {
                        var2.timeToEat = Float.valueOf(var7).intValue();
                     }

                     if ("trackChance".equalsIgnoreCase(var5)) {
                        var2.trackChance = Float.valueOf(var7).intValue();
                     }

                     if ("poopChance".equalsIgnoreCase(var5)) {
                        var2.poopChance = Float.valueOf(var7).intValue();
                     }

                     if ("brokenTwigsChance".equalsIgnoreCase(var5)) {
                        var2.brokenTwigsChance = Float.valueOf(var7).intValue();
                     }

                     if ("herbGrazeChance".equalsIgnoreCase(var5)) {
                        var2.herbGrazeChance = Float.valueOf(var7).intValue();
                     }

                     if ("flatHerbChance".equalsIgnoreCase(var5)) {
                        var2.flatHerbChance = Float.valueOf(var7).intValue();
                     }

                     if ("furChance".equalsIgnoreCase(var5)) {
                        var2.furChance = Float.valueOf(var7).intValue();
                     }

                     if ("minTimeBeforeSleep".equalsIgnoreCase(var5)) {
                        var2.minTimeBeforeSleep = Float.valueOf(var7).intValue();
                     }

                     if ("maxTimeBeforeSleep".equalsIgnoreCase(var5)) {
                        var2.maxTimeBeforeSleep = Float.valueOf(var7).intValue();
                     }

                     if ("timeToSleep".equalsIgnoreCase(var5)) {
                        var2.timeToSleep = Float.valueOf(var7).intValue();
                     }

                     if ("sleepPeriodStart".equalsIgnoreCase(var5)) {
                        var2.sleepPeriodStart = new ArrayList(Arrays.asList(var7.split(",")));
                     }

                     if ("sleepPeriodEnd".equalsIgnoreCase(var5)) {
                        var2.sleepPeriodEnd = new ArrayList(Arrays.asList(var7.split(",")));
                     }

                     if ("eatPeriodEnd".equalsIgnoreCase(var5)) {
                        var2.eatPeriodEnd = new ArrayList(Arrays.asList(var7.split(",")));
                     }

                     if ("eatPeriodStart".equalsIgnoreCase(var5)) {
                        var2.eatPeriodStart = new ArrayList(Arrays.asList(var7.split(",")));
                     }

                     if ("speed".equalsIgnoreCase(var5)) {
                        var2.speed = Float.valueOf(var7);
                     }

                     if ("possibleBreed".equalsIgnoreCase(var5)) {
                        var2.possibleBreed = new ArrayList(Arrays.asList(var7.split(",")));
                     }
                  }

                  migrationDef.put(var2.type, var2);
               }
            }

            return;
         }
      }
   }

   private static ArrayList<MigrationGroupDefinitionsGrouped> loadGroup(KahluaTableImpl var0) {
      KahluaTableIterator var1 = var0.iterator();
      ArrayList var2 = new ArrayList();

      while(var1.advance()) {
         String var3 = var1.getKey().toString();
         KahluaTableImpl var4 = (KahluaTableImpl)var1.getValue();
         MigrationGroupDefinitionsGrouped var5 = new MigrationGroupDefinitionsGrouped();
         var5.animal = var4.rawgetStr("animal");
         var5.chance = var4.rawgetInt("chance");
         var2.add(var5);
      }

      return var2;
   }

   public static ArrayList<IsoAnimal> generatePossibleAnimals(VirtualAnimal var0, String var1) {
      MigrationGroupDefinitions var2 = (MigrationGroupDefinitions)getMigrationDefs().get(var1);
      if (var2 == null) {
         DebugLog.Animal.debugln("Couldn't find a migration group definition for type: " + var1 + " check MigrationGroupDefinitions.lua");
         return null;
      } else if (var2.group != null && !var2.group.isEmpty()) {
         return generatePossibleAnimalsFromGroup(var0, var2);
      } else {
         int var3 = Rand.Next(1000000) + 1000000;
         var0.migrationGroup = var1;
         ArrayList var4 = new ArrayList();

         int var5;
         for(var5 = 0; var5 < var2.maxMale; ++var5) {
            IsoAnimal var6 = new IsoAnimal(IsoWorld.instance.getCell(), 0, 0, 0, var2.male, var2.getRandBreed());
            var6.virtualID = (double)var3;
            var6.migrationGroup = var1;
            var4.add(var6);
         }

         var5 = Rand.Next(var2.minAnimal, var2.maxAnimal + 1);

         for(int var8 = 0; var8 < var5; ++var8) {
            IsoAnimal var7 = new IsoAnimal(IsoWorld.instance.getCell(), 0, 0, 0, var2.female, var2.getRandBreed());
            var7.randomizeAge();
            var7.virtualID = (double)var3;
            var7.migrationGroup = var1;
            var4.add(var7);
            if (var7.getData().canHaveBaby() && Rand.Next(100) < var2.babyChance) {
               var4.add(var7.addBaby());
            }
         }

         var0.m_animals.addAll(var4);
         return var4;
      }
   }

   private static ArrayList<IsoAnimal> generatePossibleAnimalsFromGroup(VirtualAnimal var0, MigrationGroupDefinitions var1) {
      int var2 = 0;
      int var3 = 0;
      MigrationGroupDefinitions var4 = null;

      int var5;
      for(var5 = 0; var5 < var1.group.size(); ++var5) {
         var2 += ((MigrationGroupDefinitionsGrouped)var1.group.get(var5)).chance;
      }

      var5 = Rand.Next(var2);

      for(int var6 = 0; var6 < var1.group.size(); ++var6) {
         int var7 = ((MigrationGroupDefinitionsGrouped)var1.group.get(var6)).chance;
         var4 = (MigrationGroupDefinitions)getMigrationDefs().get(((MigrationGroupDefinitionsGrouped)var1.group.get(var6)).animal);
         if (var7 + var3 >= var5) {
            break;
         }

         var3 += var7;
         var4 = null;
      }

      if (var4 == null) {
         DebugLog.Animal.debugln("Couldn't find a migration group definition for type: " + var1.type + " check MigrationGroupDefinitions.lua");
         return null;
      } else {
         return generatePossibleAnimals(var0, var4.type);
      }
   }

   public static double getNextEatTime(String var0) {
      MigrationGroupDefinitions var1 = (MigrationGroupDefinitions)getMigrationDefs().get(var0);
      if (var1 == null) {
         DebugLog.Animal.debugln("Couldn't find a migration group definition for type: " + var0 + " check MigrationGroupDefinitions.lua");
         return 0.0;
      } else {
         return GameTime.getInstance().getWorldAgeHours() + (double)Rand.Next(var1.minTimeBeforeEat, var1.maxTimeBeforeEat + 1) / 60.0;
      }
   }

   public static double getNextSleepTime(String var0) {
      MigrationGroupDefinitions var1 = (MigrationGroupDefinitions)getMigrationDefs().get(var0);
      if (var1 == null) {
         DebugLog.Animal.debugln("Couldn't find a migration group definition for type: " + var0 + " check MigrationGroupDefinitions.lua");
         return 0.0;
      } else {
         return GameTime.getInstance().getWorldAgeHours() + (double)Rand.Next(var1.minTimeBeforeSleep, var1.maxTimeBeforeSleep + 1) / 60.0;
      }
   }

   public String getRandBreed() {
      return (String)this.possibleBreed.get(Rand.Next(0, this.possibleBreed.size()));
   }

   public static void initValueFromDef(VirtualAnimal var0) {
      MigrationGroupDefinitions var1 = (MigrationGroupDefinitions)getMigrationDefs().get(var0.migrationGroup);
      if (var1 != null) {
         var0.speed = var1.speed;
         var0.timeToEat = var1.timeToEat;
         var0.timeToSleep = var1.timeToSleep;
         var0.trackChance = var1.trackChance;
         var0.poopChance = var1.poopChance;
         var0.brokenTwigsChance = var1.brokenTwigsChance;
         var0.herbGrazeChance = var1.herbGrazeChance;
         var0.furChance = var1.furChance;
         var0.flatHerbChance = var1.flatHerbChance;
         int var2;
         if (var1.sleepPeriodStart != null) {
            for(var2 = 0; var2 < var1.sleepPeriodStart.size(); ++var2) {
               var0.sleepPeriodStart.add(Integer.parseInt((String)var1.sleepPeriodStart.get(var2)));
            }
         }

         if (var1.sleepPeriodEnd != null) {
            for(var2 = 0; var2 < var1.sleepPeriodEnd.size(); ++var2) {
               var0.sleepPeriodEnd.add(Integer.parseInt((String)var1.sleepPeriodEnd.get(var2)));
            }
         }

         if (var1.eatPeriodStart != null) {
            for(var2 = 0; var2 < var1.eatPeriodStart.size(); ++var2) {
               var0.eatPeriodStart.add(Integer.parseInt((String)var1.eatPeriodStart.get(var2)));
            }
         }

         if (var1.eatPeriodEnd != null) {
            for(var2 = 0; var2 < var1.eatPeriodEnd.size(); ++var2) {
               var0.eatPeriodEnd.add(Integer.parseInt((String)var1.eatPeriodEnd.get(var2)));
            }
         }

      }
   }

   public static void Reset() {
      migrationDef = null;
   }

   public static class MigrationGroupDefinitionsGrouped {
      public String animal;
      public int chance;

      public MigrationGroupDefinitionsGrouped() {
      }
   }
}
