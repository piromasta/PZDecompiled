package zombie.randomizedWorld.randomizedVehicleStory;

import java.util.ArrayList;
import zombie.characters.animals.IsoAnimal;
import zombie.core.random.Rand;
import zombie.inventory.types.Food;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;
import zombie.iso.zones.Zone;
import zombie.util.StringUtils;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;

public final class RVSAnimalTrailerOnRoad extends RandomizedVehicleStoryBase {
   public RVSAnimalTrailerOnRoad() {
      this.name = "Livestock Trailer On Road";
      this.minZoneWidth = 5;
      this.minZoneHeight = 12;
      this.setChance(45);
   }

   private static ArrayList<AnimalSpawn> getAnimalType() {
      ArrayList var0 = new ArrayList();
      var0.add(new AnimalSpawn("bull", "cow", 1, 2));
      var0.add(new AnimalSpawn("ram", "ewe", 2, 4));
      var0.add(new AnimalSpawn("boar", "sow", 2, 4));
      var0.add(new AnimalSpawn("cockerel", "hen", 3, 10));
      var0.add(new AnimalSpawn("gobblers", "turkeyhen", 3, 6));
      var0.add(new AnimalSpawn("rabdoe", "rabbuck", 3, 10));
      return var0;
   }

   private static ArrayList<FoodSpawn> getFoodType() {
      ArrayList var0 = new ArrayList();
      var0.add(new FoodSpawn("AnimalFeedBag", 2, 5));
      var0.add(new FoodSpawn("HayTuft", 15, 50));
      var0.add(new FoodSpawn("GrassTuft", 15, 50));
      return var0;
   }

   public void randomizeVehicleStory(Zone var1, IsoChunk var2) {
      this.callVehicleStorySpawner(var1, var2, 0.0F);
   }

   public boolean initVehicleStorySpawner(Zone var1, IsoChunk var2, boolean var3) {
      VehicleStorySpawner var4 = VehicleStorySpawner.getInstance();
      var4.clear();
      float var5 = 0.5235988F;
      if (var3) {
         var5 = 0.0F;
      }

      Vector2 var6 = IsoDirections.N.ToVector();
      var6.rotate(Rand.Next(-var5, var5));
      float var7 = 0.0F;
      float var8 = -1.5F;
      var4.addElement("vehicle1", var7, var8, var6.getDirection(), 2.0F, 5.0F);
      byte var9 = 4;
      boolean var10 = Rand.NextBool(2);
      var6 = var10 ? IsoDirections.E.ToVector() : IsoDirections.W.ToVector();
      var6.rotate(Rand.Next(-var5, var5));
      float var11 = 0.0F;
      float var12 = var8 - 2.5F - 1.0F;
      var4.addElement("vehicle2", var11, var12, var6.getDirection(), 2.0F, 5.0F);
      var4.addElement("animals", var7, var8 + 5.0F + 1.0F + (float)var9, var6.getDirection(), 2.0F, (float)var9);
      var4.setParameter("zone", var1);
      var4.setParameter("east", var10);
      return true;
   }

   public void spawnElement(VehicleStorySpawner var1, VehicleStorySpawner.Element var2) {
      IsoGridSquare var3 = var2.square;
      if (var3 != null) {
         float var4 = var2.z;
         Zone var5 = (Zone)var1.getParameter("zone", Zone.class);
         boolean var6 = var1.getParameterBoolean("east");
         BaseVehicle var9;
         String var10;
         switch (var2.id) {
            case "vehicle1":
               var9 = this.addVehicle(var5, var2.position.x, var2.position.y, var4, var2.direction, (String)null, "Base.PickUpVan", (Integer)null, "Rancher");
               if (var9 != null) {
                  var9.setAlarmed(false);
                  var9 = var9.setSmashed("Front");
                  var10 = null;
                  BaseVehicle var11;
                  if (Rand.NextBool(2)) {
                     var11 = this.addTrailer(var9, var5, var3.getChunk(), (String)null, (String)null, "Base.Trailer_Livestock");
                  } else {
                     var11 = this.addTrailer(var9, var5, var3.getChunk(), (String)null, (String)null, "Base.Trailer_Horsebox");
                  }

                  var1.setParameter("vehicle1", var9);
                  var1.setParameter("trailer", var11);
                  if (Rand.Next(100) < 80) {
                     this.addZombiesOnVehicle(1, "Farmer", (Integer)null, var9);
                  }
               }
               break;
            case "vehicle2":
               var9 = this.addVehicle(var5, var2.position.x, var2.position.y, var4, var2.direction, "bad", (String)null, (Integer)null, (String)null);
               if (var9 != null) {
                  var9.setAlarmed(false);
                  var10 = var6 ? "Right" : "Left";
                  var9 = var9.setSmashed(var10);
                  var9.setBloodIntensity(var10, 1.0F);
                  var1.setParameter("vehicle2", var9);
               }
               break;
            case "animals":
               this.spawnAnimals(var1, var3);
         }

      }
   }

   private void spawnAnimals(VehicleStorySpawner var1, IsoGridSquare var2) {
      ArrayList var3 = getAnimalType();
      AnimalSpawn var4 = (AnimalSpawn)var3.get(Rand.Next(0, var3.size()));
      int var5 = Rand.Next(var4.minNbOfAnimals, var4.maxNbOfAnimals);
      String var6 = "";
      ArrayList var7 = new ArrayList();
      IsoAnimal var8 = null;
      ArrayList var9 = new ArrayList();
      IsoGridSquare var10 = this.getRandomSquare(var2.getX(), var2.getY(), var2.getZ());
      if (var10 != null) {
         IsoAnimal var11 = new IsoAnimal(IsoWorld.instance.getCell(), var10.getX(), var10.getY(), var10.getZ(), var4.male, "");
         this.randomizeAnimal(var11, (IsoAnimal)null);
         var8 = var11;
         var7.add(var11);
         var6 = var11.getBreed().name;
         --var5;
      }

      for(int var15 = 0; var15 < var5; ++var15) {
         var10 = this.getRandomSquare(var2.getX(), var2.getY(), var2.getZ());
         if (var10 != null) {
            IsoAnimal var12 = new IsoAnimal(IsoWorld.instance.getCell(), var10.getX(), var10.getY(), var10.getZ(), var4.female, var6);
            this.randomizeAnimal(var12, var8);
            var7.add(var12);
            var6 = var12.getBreed().name;
            var9.add(var12);
            if (var12.getData().getDaysSurvived() >= var12.getMinAgeForBaby() && Rand.NextBool(6)) {
               IsoAnimal var13 = var12.addBaby();
               this.randomizeAnimal(var13, (IsoAnimal)null);
               var7.add(var13);
               --var5;
            }
         }
      }

      BaseVehicle var16 = (BaseVehicle)var1.getParameter("trailer", BaseVehicle.class);
      if (var16 != null) {
         boolean var17 = false;
         if (Rand.NextBool(3)) {
            var17 = true;
            VehiclePart var18 = var16.getPartById("TrunkDoorOpened");
            if (var18 != null && var18.getDoor() != null) {
               var18.getDoor().setOpen(true);
            }
         }

         for(int var19 = 0; var19 < var7.size(); ++var19) {
            IsoAnimal var14 = (IsoAnimal)var7.get(var19);
            if (var17 && (Rand.Next(100) <= 30 || !var16.canAddAnimalInTrailer(var14))) {
               if (Rand.NextBool(10)) {
                  var14.setHealth(0.0F);
               }
            } else {
               var16.addAnimalInTrailer(var14);
            }
         }

         this.spawnFood(var16);
         this.spawnEggs(var16, var8, var9);
      }
   }

   private void spawnEggs(BaseVehicle var1, IsoAnimal var2, ArrayList<IsoAnimal> var3) {
      if (var3 != null && !var3.isEmpty()) {
         IsoAnimal var4 = (IsoAnimal)var3.get(0);
         if (var4 != null && var4.adef != null && !StringUtils.isNullOrEmpty(var4.adef.eggType)) {
            VehiclePart var5 = var1.getPartById("TrailerAnimalEggs");
            if (var5 != null && var5.getItemContainer() != null) {
               int var6 = Rand.Next(3, 10);

               for(int var7 = 0; var7 < var6; ++var7) {
                  IsoAnimal var8 = (IsoAnimal)var3.get(Rand.Next(0, var3.size()));
                  Food var9 = var8.createEgg();
                  var9.setFertilizedTime(Rand.Next(2, 20));
                  var5.getItemContainer().addItem(var9);
               }

            }
         }
      }
   }

   private void spawnFood(BaseVehicle var1) {
      VehiclePart var2 = var1.getPartById("TrailerAnimalFood");
      if (var2 != null && var2.getItemContainer() != null) {
         ArrayList var3 = getFoodType();
         FoodSpawn var4 = (FoodSpawn)var3.get(Rand.Next(0, var3.size()));
         var2.getItemContainer().AddItems(var4.type, Rand.Next(var4.min, var4.max));
      }
   }

   private IsoGridSquare getRandomSquare(int var1, int var2, int var3) {
      IsoGridSquare var4 = null;

      for(int var5 = 0; var5 < 20; ++var5) {
         var4 = IsoWorld.instance.getCell().getGridSquare(Rand.Next(var1 - 5, var1 + 5), Rand.Next(var2 - 5, var2 + 5), var3);
         if (var4 != null && var4.isFree(false)) {
            return var4;
         }
      }

      return var4;
   }

   private void randomizeAnimal(IsoAnimal var1, IsoAnimal var2) {
      var1.addToWorld();
      var1.randomizeAge();
      var1.setWild(false);
      var1.setHealth(Rand.Next(0.5F, 1.0F));
      var1.setDebugStress(Rand.Next(50.0F, 100.0F));
      if (Rand.NextBool(5) && var1.isFemale() && var2 != null && var1.getData().getDaysSurvived() >= var1.getMinAgeForBaby()) {
         var1.fertilize(var2, true);
         var1.getData().setPregnancyTime(Rand.Next(20, 60));
         var1.getData().setFertilizedTime(Rand.Next(20, 60));
      }

   }

   private static final class AnimalSpawn {
      String male = null;
      String female = null;
      int maxNbOfAnimals = 0;
      int minNbOfAnimals = 0;

      public AnimalSpawn(String var1, String var2, int var3, int var4) {
         this.male = var1;
         this.female = var2;
         this.minNbOfAnimals = var3;
         this.maxNbOfAnimals = var4;
      }
   }

   private static final class FoodSpawn {
      String type = null;
      int max = 0;
      int min = 0;

      public FoodSpawn(String var1, int var2, int var3) {
         this.type = var1;
         this.min = var2;
         this.max = var3;
      }
   }
}
