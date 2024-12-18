package zombie.randomizedWorld.randomizedVehicleStory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import zombie.characters.animals.AnimalDefinitions;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.animals.datas.AnimalBreed;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;
import zombie.iso.zones.Zone;

public final class RVSRoadKillSmall extends RandomizedVehicleStoryBase {
   public RVSRoadKillSmall() {
      this.name = "Roadkill - Small Animal Run Over By Vehicle";
      this.minZoneWidth = 4;
      this.minZoneHeight = 11;
      this.setChance(10);
      this.needsRuralVegetation = true;
      this.notTown = true;
   }

   public static ArrayList<String> getBreeds() {
      ArrayList var0 = new ArrayList();
      var0.add("appalachian");
      var0.add("cottontail");
      var0.add("swamp");
      var0.add("grey");
      var0.add("meleagris");
      return var0;
   }

   public void randomizeVehicleStory(Zone var1, IsoChunk var2) {
      float var3 = 0.5235988F;
      this.callVehicleStorySpawner(var1, var2, Rand.Next(-var3, var3));
   }

   public boolean initVehicleStorySpawner(Zone var1, IsoChunk var2, boolean var3) {
      VehicleStorySpawner var4 = VehicleStorySpawner.getInstance();
      var4.clear();
      Vector2 var5 = IsoDirections.N.ToVector();
      float var6 = 2.5F;
      var4.addElement("corpse", 0.0F, var6 - (float)(var3 ? 7 : Rand.Next(4, 7)), var5.getDirection() + 3.1415927F, 1.0F, 2.0F);
      var4.setParameter("zone", var1);
      ArrayList var7 = getBreeds();
      String var8 = (String)var7.get(Rand.Next(var7.size()));
      var4.setParameter("breed", var8);
      return true;
   }

   public void spawnElement(VehicleStorySpawner var1, VehicleStorySpawner.Element var2) {
      IsoGridSquare var3 = var2.square;
      if (var3 != null) {
         switch (var2.id) {
            case "corpse":
               LinkedHashMap var6 = new LinkedHashMap();
               LinkedHashMap var7 = new LinkedHashMap();
               var6.put("appalachian", "rabdoe");
               var6.put("cottontail", "rabdoe");
               var6.put("swamp", "rabdoe");
               var6.put("grey", "raccoonsow");
               var6.put("meleagris", "turkeyhen");
               var7.put("appalachian", "rabbuck");
               var7.put("cottontail", "rabbuck");
               var7.put("swamp", "rabbuck");
               var7.put("grey", "raccoonboar");
               var7.put("meleagris", "gobblers");
               String var8 = var1.getParameterString("breed");
               String var9 = (String)var6.get(var8);
               if (Rand.NextBool(2)) {
                  var9 = (String)var7.get(var8);
               }

               AnimalDefinitions var10 = AnimalDefinitions.getDef(var9);
               if (var10 == null) {
                  DebugLog.General.warn("can't spawn animal type \"", var9, "\"");
               } else {
                  AnimalBreed var11 = var10.getBreedByName(var8);
                  if (var11 == null) {
                     DebugLog.General.warn("can't spawn animal type/breed \"", var9, "\" / \"", var8, "\"");
                  } else {
                     IsoAnimal var12 = new IsoAnimal(IsoWorld.instance.getCell(), (int)var2.position.x, (int)var2.position.y, 0, var9, var8);
                     var12.randomizeAge();
                     var12.setHealth(0.0F);
                     this.addTrailOfBlood(var2.position.x, var2.position.y, var2.z, var2.direction, 1);
                  }
               }
            default:
         }
      }
   }
}
