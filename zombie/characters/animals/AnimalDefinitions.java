package zombie.characters.animals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.GameTime;
import zombie.Lua.LuaManager;
import zombie.characters.animals.datas.AnimalBreed;
import zombie.characters.animals.datas.AnimalGrowStage;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.model.Model;
import zombie.util.StringUtils;

public class AnimalDefinitions {
   public String animalTypeStr;
   public Model bodyModel;
   public Model bodyModelSkel;
   public Model bodyModelFleece;
   public String bodyModelStr;
   public Model bodyModelHeadless;
   public String bodyModelFleeceStr;
   public String bodyModelSkelStr;
   public String bodyModelHeadlessStr;
   public String textureSkeleton;
   public String textureSkinned;
   public String animset;
   public String mate;
   public float shadoww;
   public float shadowfm;
   public float shadowbm;
   public float turnDelta = 0.8F;
   public float animalSize;
   public float minSize;
   public float maxSize;
   public int minAge;
   public int minEnclosureSize;
   public String babyType;
   public int minAgeForBaby;
   public int maxAgeGeriatric;
   public boolean udder;
   public boolean female;
   public boolean male;
   public ArrayList<AnimalGrowStage> stages;
   public ArrayList<AnimalBreed> breeds;
   public ArrayList<AnimalAllele> genome;
   public boolean alwaysFleeHumans = true;
   public boolean fleeZombies = true;
   public boolean canBeAttached = false;
   public boolean canBeTransported = false;
   public float hungerMultiplier = 0.0F;
   public float thirstMultiplier = 0.0F;
   public float healthLossMultiplier = 0.05F;
   public float wanderMul = 400.0F;
   public int idleTypeNbr = 0;
   public int eatingTypeNbr = 0;
   public int sittingTypeNbr = 0;
   public boolean eatFromMother = false;
   public boolean periodicRun = false;
   public int pregnantPeriod = 0;
   public boolean eatGrass = false;
   public boolean sitRandomly = false;
   public ArrayList<String> eatTypeTrough = null;
   public boolean canBeMilked = false;
   public int minBaby = 1;
   public int maxBaby = 1;
   public int idleEmoteChance = 1000;
   public int eggsPerDay = 0;
   public String eggType = null;
   public int fertilizedTimeMax = 0;
   public int timeToHatch = 0;
   public boolean canBePicked = true;
   public ArrayList<String> hutches;
   public int enterHutchTime = 0;
   public int exitHutchTime = 0;
   public ArrayList<String> genes;
   public float minMilk = 0.0F;
   public float maxMilk = 0.0F;
   public float maxWool = 0.0F;
   public float minWeight = 10.0F;
   public float maxWeight = 100.0F;
   public String carcassItem;
   public int attackDist = 1;
   public int attackTimer = 1000;
   public boolean dontAttackOtherMale = false;
   public boolean canBeFeedByHand = false;
   public float baseDmg = 0.5F;
   public String milkAnimPreset;
   public ArrayList<String> feedByHandType;
   public float trailerBaseSize;
   public boolean canBePet = false;
   public boolean attackBack = false;
   public float collisionSize = 0.0F;
   public float baseEncumbrance = 1.0F;
   public int matingPeriodStart = 0;
   public int matingPeriodEnd = 0;
   public int timeBeforeNextPregnancy = 0;
   public float thirstHungerTrigger = 0.1F;
   public boolean collidable = true;
   public boolean canThump = true;
   public boolean wild = false;
   public int spottingDist = 10;
   public String group;
   public boolean canBeAlerted = false;
   public String dung;
   public boolean attackIfStressed = false;
   public int happyAnim = 0;
   public String ropeBone = "Bip01_Head";
   public int minClutchSize = -1;
   public int maxClutchSize = -1;
   public int layEggPeriodStart = -1;
   public boolean stressAboveGround = false;
   public boolean canClimbStairs = false;
   public boolean stressUnderRain = false;
   public boolean canClimbFences = false;
   public boolean needMom = true;
   public boolean canBeDomesticated = true;
   public int dungChancePerDay = 50;
   public float hungerBoost = 1.0F;
   public float thirstBoost = 1.0F;
   public float distToEat = 2.0F;
   public boolean knockdownAttack = false;
   public int minBodyPart = 0;
   public boolean canDoLaceration = false;
   public float maxBlood;
   public float minBlood;
   public boolean litterEatTogether = false;
   public boolean addTrackingXp = true;
   public float corpseSize = 1.0F;
   public float corpseLength = 1.0F;
   public float idleSoundRadius = 0.0F;
   public float idleSoundVolume = 0.0F;
   public static HashMap<String, AnimalDefinitions> animalDefs;

   public AnimalDefinitions() {
   }

   public static HashMap<String, AnimalDefinitions> getAnimalDefs() {
      if (animalDefs == null) {
         loadAnimalDefinitions();
      }

      return animalDefs;
   }

   public static ArrayList<AnimalDefinitions> getAnimalDefsArray() {
      if (animalDefs == null) {
         loadAnimalDefinitions();
      }

      ArrayList var0 = new ArrayList();
      Iterator var1 = animalDefs.values().iterator();

      while(var1.hasNext()) {
         AnimalDefinitions var2 = (AnimalDefinitions)var1.next();
         var0.add(var2);
      }

      return var0;
   }

   public static void loadAnimalDefinitions() {
      if (AnimalGenomeDefinitions.fullGenomeDef == null) {
         AnimalGenomeDefinitions.loadGenomeDefinition();
      }

      animalDefs = new HashMap();
      KahluaTableImpl var0 = (KahluaTableImpl)LuaManager.env.rawget("AnimalDefinitions");
      if (var0 != null) {
         KahluaTableImpl var1 = (KahluaTableImpl)var0.rawget("animals");
         KahluaTableIterator var2 = var1.iterator();

         while(var2.advance()) {
            AnimalDefinitions var3 = new AnimalDefinitions();
            var3.animalTypeStr = var2.getKey().toString();
            var3.stages = new ArrayList();
            var3.breeds = new ArrayList();
            var3.genome = new ArrayList();
            KahluaTableIterator var4 = ((KahluaTableImpl)var2.getValue()).iterator();

            while(var4.advance()) {
               String var5 = var4.getKey().toString();
               Object var6 = var4.getValue();
               String var7 = var6.toString().trim();
               if ("bodyModel".equalsIgnoreCase(var5)) {
                  var3.bodyModelStr = var6.toString();
               }

               if ("bodyModelFleece".equalsIgnoreCase(var5)) {
                  var3.bodyModelFleeceStr = var6.toString();
               }

               if ("bodyModelHeadless".equalsIgnoreCase(var5)) {
                  var3.bodyModelHeadlessStr = var6.toString();
               }

               if ("textureSkeleton".equalsIgnoreCase(var5)) {
                  var3.textureSkeleton = var6.toString();
               }

               if ("textureSkinned".equalsIgnoreCase(var5)) {
                  var3.textureSkinned = var6.toString();
               }

               if ("bodyModelSkel".equalsIgnoreCase(var5)) {
                  var3.bodyModelSkelStr = var6.toString();
               }

               if ("carcassItem".equalsIgnoreCase(var5)) {
                  var3.carcassItem = var7;
               }

               if ("corpseSize".equalsIgnoreCase(var5)) {
                  var3.corpseSize = Float.valueOf(var7);
               }

               if ("corpseLength".equalsIgnoreCase(var5)) {
                  var3.corpseLength = Float.valueOf(var7);
               }

               if ("idleSoundRadius".equalsIgnoreCase(var5)) {
                  var3.idleSoundRadius = Float.valueOf(var7);
               }

               if ("idleSoundVolume".equalsIgnoreCase(var5)) {
                  var3.idleSoundVolume = Float.valueOf(var7);
               }

               if ("maxBlood".equalsIgnoreCase(var5)) {
                  var3.maxBlood = Float.valueOf(var7);
               }

               if ("minBlood".equalsIgnoreCase(var5)) {
                  var3.minBlood = Float.valueOf(var7);
               }

               if ("milkAnimPreset".equalsIgnoreCase(var5)) {
                  var3.milkAnimPreset = var7;
               }

               if ("animset".equalsIgnoreCase(var5)) {
                  var3.animset = var7;
               }

               if ("ropeBone".equalsIgnoreCase(var5)) {
                  var3.ropeBone = var7;
               }

               if ("mate".equalsIgnoreCase(var5)) {
                  var3.mate = var7;
               }

               if ("dung".equalsIgnoreCase(var5)) {
                  var3.dung = var7;
               }

               if ("shadoww".equalsIgnoreCase(var5)) {
                  var3.shadoww = Float.parseFloat(var7);
               }

               if ("shadowfm".equalsIgnoreCase(var5)) {
                  var3.shadowfm = Float.parseFloat(var7);
               }

               if ("shadowbm".equalsIgnoreCase(var5)) {
                  var3.shadowbm = Float.parseFloat(var7);
               }

               if ("turnDelta".equalsIgnoreCase(var5)) {
                  var3.turnDelta = Float.parseFloat(var7);
               }

               if ("animalSize".equalsIgnoreCase(var5)) {
                  var3.animalSize = Float.parseFloat(var7);
               }

               if ("minSize".equalsIgnoreCase(var5)) {
                  var3.minSize = Float.parseFloat(var7);
               }

               if ("maxSize".equalsIgnoreCase(var5)) {
                  var3.maxSize = Float.parseFloat(var7);
               }

               if ("hungerMultiplier".equalsIgnoreCase(var5)) {
                  var3.hungerMultiplier = Float.parseFloat(var7);
               }

               if ("thirstMultiplier".equalsIgnoreCase(var5)) {
                  var3.thirstMultiplier = Float.parseFloat(var7);
               }

               if ("healthLossMultiplier".equalsIgnoreCase(var5)) {
                  var3.healthLossMultiplier = Float.parseFloat(var7);
               }

               if ("wanderMul".equalsIgnoreCase(var5)) {
                  var3.wanderMul = Float.parseFloat(var7);
               }

               if ("minEnclosureSize".equalsIgnoreCase(var5)) {
                  var3.minEnclosureSize = Float.valueOf(var7).intValue();
               }

               if ("happyAnim".equalsIgnoreCase(var5)) {
                  var3.happyAnim = Float.valueOf(var7).intValue();
               }

               if ("minAge".equalsIgnoreCase(var5)) {
                  var3.minAge = Float.valueOf(var7).intValue();
               }

               if ("minAgeForBaby".equalsIgnoreCase(var5)) {
                  var3.minAgeForBaby = Float.valueOf(var7).intValue();
               }

               if ("attackDist".equalsIgnoreCase(var5)) {
                  var3.attackDist = Float.valueOf(var7).intValue();
               }

               if ("attackTimer".equalsIgnoreCase(var5)) {
                  var3.attackTimer = Float.valueOf(var7).intValue();
               }

               if ("timeBeforeNextPregnancy".equalsIgnoreCase(var5)) {
                  var3.timeBeforeNextPregnancy = Float.valueOf(var7).intValue();
               }

               if ("spottingDist".equalsIgnoreCase(var5)) {
                  var3.spottingDist = Float.valueOf(var7).intValue();
               }

               if ("dungChancePerDay".equalsIgnoreCase(var5)) {
                  var3.dungChancePerDay = Float.valueOf(var7).intValue();
               }

               if ("minBodyPart".equalsIgnoreCase(var5)) {
                  var3.minBodyPart = Float.valueOf(var7).intValue();
               }

               if ("minClutchSize".equalsIgnoreCase(var5)) {
                  var3.minClutchSize = Float.valueOf(var7).intValue();
               }

               if ("maxClutchSize".equalsIgnoreCase(var5)) {
                  var3.maxClutchSize = Float.valueOf(var7).intValue();
               }

               if ("layEggPeriodStart".equalsIgnoreCase(var5)) {
                  var3.layEggPeriodStart = Float.valueOf(var7).intValue();
               }

               if ("matingPeriodStart".equalsIgnoreCase(var5)) {
                  var3.matingPeriodStart = Float.valueOf(var7).intValue();
               }

               if ("matingPeriodEnd".equalsIgnoreCase(var5)) {
                  var3.matingPeriodEnd = Float.valueOf(var7).intValue();
               }

               if ("babyType".equalsIgnoreCase(var5)) {
                  var3.babyType = var7;
               }

               if ("group".equalsIgnoreCase(var5)) {
                  var3.group = var7;
               }

               if ("eggType".equalsIgnoreCase(var5)) {
                  var3.eggType = var7;
               }

               if ("maxAgeGeriatric".equalsIgnoreCase(var5)) {
                  var3.maxAgeGeriatric = Float.valueOf(var7).intValue();
               }

               if ("idleEmoteChance".equalsIgnoreCase(var5)) {
                  var3.idleEmoteChance = Float.valueOf(var7).intValue();
               }

               if ("eggsPerDay".equalsIgnoreCase(var5)) {
                  var3.eggsPerDay = Float.valueOf(var7).intValue();
               }

               if ("fertilizedTimeMax".equalsIgnoreCase(var5)) {
                  var3.fertilizedTimeMax = Float.valueOf(var7).intValue();
               }

               if ("timeToHatch".equalsIgnoreCase(var5)) {
                  var3.timeToHatch = Float.valueOf(var7).intValue();
               }

               if ("enterHutchTime".equalsIgnoreCase(var5)) {
                  var3.enterHutchTime = Float.valueOf(var7).intValue();
               }

               if ("exitHutchTime".equalsIgnoreCase(var5)) {
                  var3.exitHutchTime = Float.valueOf(var7).intValue();
               }

               if ("idleTypeNbr".equalsIgnoreCase(var5)) {
                  var3.idleTypeNbr = Float.valueOf(var7).intValue();
               }

               if ("eatingTypeNbr".equalsIgnoreCase(var5)) {
                  var3.eatingTypeNbr = Float.valueOf(var7).intValue();
               }

               if ("sittingTypeNbr".equalsIgnoreCase(var5)) {
                  var3.sittingTypeNbr = Float.valueOf(var7).intValue();
               }

               if ("pregnantPeriod".equalsIgnoreCase(var5)) {
                  var3.pregnantPeriod = Float.valueOf(var7).intValue();
               }

               if ("minWeight".equalsIgnoreCase(var5)) {
                  var3.minWeight = Float.valueOf(var7);
               }

               if ("maxWeight".equalsIgnoreCase(var5)) {
                  var3.maxWeight = Float.valueOf(var7);
               }

               if ("litterEatTogether".equalsIgnoreCase(var5)) {
                  var3.litterEatTogether = Boolean.parseBoolean(var7);
               }

               if ("udder".equalsIgnoreCase(var5)) {
                  var3.udder = Boolean.parseBoolean(var7);
               }

               if ("female".equalsIgnoreCase(var5)) {
                  var3.female = Boolean.parseBoolean(var7);
               }

               if ("male".equalsIgnoreCase(var5)) {
                  var3.male = Boolean.parseBoolean(var7);
               }

               if ("addTrackingXp".equalsIgnoreCase(var5)) {
                  var3.addTrackingXp = Boolean.parseBoolean(var7);
               }

               if ("fleeZombies".equalsIgnoreCase(var5)) {
                  var3.fleeZombies = Boolean.parseBoolean(var7);
               }

               if ("stressAboveGround".equalsIgnoreCase(var5)) {
                  var3.stressAboveGround = Boolean.parseBoolean(var7);
               }

               if ("stressUnderRain".equalsIgnoreCase(var5)) {
                  var3.stressUnderRain = Boolean.parseBoolean(var7);
               }

               if ("canClimbFences".equalsIgnoreCase(var5)) {
                  var3.canClimbFences = Boolean.parseBoolean(var7);
               }

               if ("needMom".equalsIgnoreCase(var5)) {
                  var3.needMom = Boolean.parseBoolean(var7);
               }

               if ("canBeDomesticated".equalsIgnoreCase(var5)) {
                  var3.canBeDomesticated = Boolean.parseBoolean(var7);
               }

               if ("knockdownAttack".equalsIgnoreCase(var5)) {
                  var3.knockdownAttack = Boolean.parseBoolean(var7);
               }

               if ("canDoLaceration".equalsIgnoreCase(var5)) {
                  var3.canDoLaceration = Boolean.parseBoolean(var7);
               }

               if ("canClimbStairs".equalsIgnoreCase(var5)) {
                  var3.canClimbStairs = Boolean.parseBoolean(var7);
               }

               if ("canBeAlerted".equalsIgnoreCase(var5)) {
                  var3.canBeAlerted = Boolean.parseBoolean(var7);
               }

               if ("attackIfStressed".equalsIgnoreCase(var5)) {
                  var3.attackIfStressed = Boolean.parseBoolean(var7);
               }

               if ("alwaysFleeHumans".equalsIgnoreCase(var5)) {
                  var3.alwaysFleeHumans = Boolean.parseBoolean(var7);
               }

               if ("canBeAttached".equalsIgnoreCase(var5)) {
                  var3.canBeAttached = Boolean.parseBoolean(var7);
               }

               if ("canBeTransported".equalsIgnoreCase(var5)) {
                  var3.canBeTransported = Boolean.parseBoolean(var7);
               }

               if ("eatFromMother".equalsIgnoreCase(var5)) {
                  var3.eatFromMother = Boolean.parseBoolean(var7);
               }

               if ("periodicRun".equalsIgnoreCase(var5)) {
                  var3.periodicRun = Boolean.parseBoolean(var7);
               }

               if ("eatGrass".equalsIgnoreCase(var5)) {
                  var3.eatGrass = Boolean.parseBoolean(var7);
               }

               if ("sitRandomly".equalsIgnoreCase(var5)) {
                  var3.sitRandomly = Boolean.parseBoolean(var7);
               }

               if ("canBeMilked".equalsIgnoreCase(var5)) {
                  var3.canBeMilked = Boolean.parseBoolean(var7);
               }

               if ("canBePicked".equalsIgnoreCase(var5)) {
                  var3.canBePicked = Boolean.parseBoolean(var7);
               }

               if ("collidable".equalsIgnoreCase(var5)) {
                  var3.collidable = Boolean.parseBoolean(var7);
               }

               if ("canThump".equalsIgnoreCase(var5)) {
                  var3.canThump = Boolean.parseBoolean(var7);
               }

               if ("wild".equalsIgnoreCase(var5)) {
                  var3.wild = Boolean.parseBoolean(var7);
               }

               if ("dontAttackOtherMale".equalsIgnoreCase(var5)) {
                  var3.dontAttackOtherMale = Boolean.parseBoolean(var7);
               }

               if ("canBePet".equalsIgnoreCase(var5)) {
                  var3.canBePet = Boolean.parseBoolean(var7);
               }

               if ("attackBack".equalsIgnoreCase(var5)) {
                  var3.attackBack = Boolean.parseBoolean(var7);
               }

               if ("canBeFeedByHand".equalsIgnoreCase(var5)) {
                  var3.canBeFeedByHand = Boolean.parseBoolean(var7);
               }

               if ("eatTypeTrough".equalsIgnoreCase(var5)) {
                  var3.eatTypeTrough = new ArrayList(Arrays.asList(var7.split(",")));
               }

               if ("hutches".equalsIgnoreCase(var5)) {
                  var3.hutches = new ArrayList(Arrays.asList(var7.split(",")));
               }

               if ("feedByHandType".equalsIgnoreCase(var5)) {
                  var3.feedByHandType = new ArrayList(Arrays.asList(var7.split(",")));
               }

               if ("collisionSize".equalsIgnoreCase(var5)) {
                  var3.collisionSize = Float.parseFloat(var7);
               }

               if ("thirstHungerTrigger".equalsIgnoreCase(var5)) {
                  var3.thirstHungerTrigger = Float.parseFloat(var7);
               }

               if ("hungerBoost".equalsIgnoreCase(var5)) {
                  var3.hungerBoost = Float.parseFloat(var7);
               }

               if ("thirstBoost".equalsIgnoreCase(var5)) {
                  var3.thirstBoost = Float.parseFloat(var7);
               }

               if ("distToEat".equalsIgnoreCase(var5)) {
                  var3.distToEat = Float.parseFloat(var7);
               }

               if ("babyNbr".equalsIgnoreCase(var5)) {
                  String[] var8 = var7.split(",");
                  var3.minBaby = Integer.parseInt(var8[0]);
                  var3.maxBaby = Integer.parseInt(var8[1]);
               }

               if ("maxMilk".equalsIgnoreCase(var5)) {
                  var3.maxMilk = Float.valueOf(var7);
               }

               if ("baseDmg".equalsIgnoreCase(var5)) {
                  var3.baseDmg = Float.valueOf(var7);
               }

               if ("minMilk".equalsIgnoreCase(var5)) {
                  var3.minMilk = Float.valueOf(var7);
               }

               if ("trailerBaseSize".equalsIgnoreCase(var5)) {
                  var3.trailerBaseSize = Float.valueOf(var7);
               }

               if ("maxWool".equalsIgnoreCase(var5)) {
                  var3.maxWool = Float.valueOf(var7);
               }

               if ("baseEncumbrance".equalsIgnoreCase(var5)) {
                  var3.baseEncumbrance = Float.valueOf(var7);
               }

               if ("stages".equalsIgnoreCase(var5)) {
                  loadStages(var3, (KahluaTableImpl)var6);
               }

               if ("breeds".equalsIgnoreCase(var5)) {
                  loadBreeds(var3, (KahluaTableImpl)var6);
               }

               if ("genes".equalsIgnoreCase(var5)) {
                  loadGenes(var3, (KahluaTableImpl)var6);
               }
            }

            animalDefs.put(var3.animalTypeStr, var3);
         }

         Iterator var9 = animalDefs.values().iterator();

         while(var9.hasNext()) {
            AnimalDefinitions var10 = (AnimalDefinitions)var9.next();
            var10.bodyModel = ModelManager.instance.getLoadedModel(var10.bodyModelStr);
            if (!StringUtils.isNullOrEmpty(var10.bodyModelFleeceStr)) {
               var10.bodyModelFleece = ModelManager.instance.getLoadedModel(var10.bodyModelFleeceStr);
            }

            if (!StringUtils.isNullOrEmpty(var10.bodyModelSkelStr)) {
               var10.bodyModelSkel = ModelManager.instance.getLoadedModel(var10.bodyModelSkelStr);
            }

            if (!StringUtils.isNullOrEmpty(var10.bodyModelHeadlessStr)) {
               var10.bodyModelHeadless = ModelManager.instance.getLoadedModel(var10.bodyModelHeadlessStr);
            }
         }

      }
   }

   private static void loadGenes(AnimalDefinitions var0, KahluaTableImpl var1) {
      KahluaTableIterator var2 = var1.iterator();
      var0.genes = new ArrayList();

      while(var2.advance()) {
         String var3 = var2.getValue().toString().trim();
         if (!var0.genes.contains(var3)) {
            var0.genes.add(var3);
         }
      }

   }

   private static void loadBreeds(AnimalDefinitions var0, KahluaTableImpl var1) {
      KahluaTableIterator var2 = var1.iterator();

      while(var2.advance()) {
         Object var3 = var2.getValue();
         AnimalBreed var4 = new AnimalBreed();
         var4.name = var2.getKey().toString();
         KahluaTableIterator var5 = ((KahluaTableImpl)var3).iterator();

         while(var5.advance()) {
            String var6 = var5.getKey().toString();
            Object var7 = var5.getValue();
            String var8 = var7.toString();
            if ("texture".equalsIgnoreCase(var6)) {
               var4.texture = new ArrayList(Arrays.asList(var8.split(",")));
            }

            if ("textureMale".equalsIgnoreCase(var6)) {
               var4.textureMale = var8;
            }

            if ("textureBaby".equalsIgnoreCase(var6)) {
               var4.textureBaby = var8;
            }

            if ("milkType".equalsIgnoreCase(var6)) {
               var4.milkType = var8;
            }

            if ("woolType".equalsIgnoreCase(var6)) {
               var4.woolType = var8;
            }

            if ("forcedGenes".equalsIgnoreCase(var6)) {
               var4.loadForcedGenes((KahluaTableImpl)var7);
            }

            if ("invIconMale".equalsIgnoreCase(var6)) {
               var4.invIconMale = var8;
            }

            if ("invIconFemale".equalsIgnoreCase(var6)) {
               var4.invIconFemale = var8;
            }

            if ("invIconBaby".equalsIgnoreCase(var6)) {
               var4.invIconBaby = var8;
            }

            if ("invIconMaleDead".equalsIgnoreCase(var6)) {
               var4.invIconMaleDead = var8;
            }

            if ("invIconFemaleDead".equalsIgnoreCase(var6)) {
               var4.invIconFemaleDead = var8;
            }

            if ("invIconBabyDead".equalsIgnoreCase(var6)) {
               var4.invIconBabyDead = var8;
            }

            if ("leather".equalsIgnoreCase(var6)) {
               var4.leather = var8;
            }

            if ("headItem".equalsIgnoreCase(var6)) {
               var4.headItem = var8;
            }

            if ("featherItem".equalsIgnoreCase(var6)) {
               var4.featherItem = var8;
            }

            if ("maxFeather".equalsIgnoreCase(var6)) {
               var4.maxFeather = Float.valueOf(var8).intValue();
            }

            if ("sounds".equalsIgnoreCase(var6)) {
               var4.loadSounds((KahluaTableImpl)var7);
            }
         }

         var0.breeds.add(var4);
      }

   }

   private static void loadStages(AnimalDefinitions var0, KahluaTableImpl var1) {
      KahluaTableIterator var2 = var1.iterator();

      while(var2.advance()) {
         Object var3 = var2.getValue();
         AnimalGrowStage var4 = new AnimalGrowStage();
         var4.stage = var2.getKey().toString();
         KahluaTableIterator var5 = ((KahluaTableImpl)var3).iterator();

         while(var5.advance()) {
            String var6 = var5.getKey().toString();
            Object var7 = var5.getValue();
            String var8 = var7.toString();
            if ("ageToGrow".equalsIgnoreCase(var6)) {
               var4.ageToGrow = Float.valueOf(var8).intValue();
            }

            if ("nextStage".equalsIgnoreCase(var6)) {
               var4.nextStage = var8;
            }

            if ("nextStageMale".equalsIgnoreCase(var6)) {
               var4.nextStageMale = var8;
            }
         }

         var0.stages.add(var4);
      }

   }

   public AnimalBreed getBreedByName(String var1) {
      for(int var2 = 0; var2 < this.breeds.size(); ++var2) {
         if (((AnimalBreed)this.breeds.get(var2)).name.equals(var1)) {
            return (AnimalBreed)this.breeds.get(var2);
         }
      }

      return null;
   }

   public AnimalBreed getRandomBreed() {
      return (AnimalBreed)this.breeds.get(Rand.Next(0, this.breeds.size()));
   }

   public static AnimalDefinitions getDef(IsoAnimal var0) {
      return (AnimalDefinitions)animalDefs.get(var0.getAnimalType());
   }

   public static AnimalDefinitions getDef(String var0) {
      return (AnimalDefinitions)animalDefs.get(var0);
   }

   public ArrayList<AnimalBreed> getBreeds() {
      return this.breeds;
   }

   public String getAnimalType() {
      return this.animalTypeStr;
   }

   public String getBodyModelStr() {
      return this.bodyModelStr;
   }

   public boolean isInsideHutchTime(Integer var1) {
      if (var1 == null || var1 < 0) {
         var1 = GameTime.getInstance().getHour();
      }

      int var2 = this.enterHutchTime;
      int var3 = this.exitHutchTime;
      if (var2 < var3) {
         return var1 >= var2 && var1 < var3;
      } else {
         return var1 < var3 || var1 >= var2;
      }
   }

   public boolean isOutsideHutchTime() {
      return !this.isInsideHutchTime((Integer)null);
   }

   public String getGroup() {
      return this.group;
   }

   public static void Reset() {
      animalDefs = null;
   }

   public boolean canBeSkeleton() {
      return this.textureSkeleton != null && this.bodyModelSkel != null;
   }

   public int getMinBaby() {
      return this.minBaby;
   }

   public int getMaxBaby() {
      return this.maxBaby;
   }

   public String getBabyType() {
      return this.babyType;
   }
}
