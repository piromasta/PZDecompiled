package zombie.randomizedWorld.randomizedVehicleStory;

import java.util.ArrayList;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.inventory.ItemSpawner;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.Vector2;
import zombie.iso.zones.Zone;
import zombie.vehicles.BaseVehicle;

public final class RVSConstructionSite extends RandomizedVehicleStoryBase {
   private ArrayList<String> tools = null;

   public RVSConstructionSite() {
      this.name = "Construction Site";
      this.minZoneWidth = 6;
      this.minZoneHeight = 6;
      this.setChance(30);
      this.tools = new ArrayList();
      this.tools.add("Base.PickAxe");
      this.tools.add("Base.Shovel");
      this.tools.add("Base.Shovel2");
      this.tools.add("Base.Hammer");
      this.tools.add("Base.LeadPipe");
      this.tools.add("Base.PipeWrench");
      this.tools.add("Base.Sledgehammer");
      this.tools.add("Base.Sledgehammer2");
      this.needsPavement = true;
   }

   public void randomizeVehicleStory(Zone var1, IsoChunk var2) {
      this.callVehicleStorySpawner(var1, var2, 0.0F);
   }

   public boolean initVehicleStorySpawner(Zone var1, IsoChunk var2, boolean var3) {
      VehicleStorySpawner var4 = VehicleStorySpawner.getInstance();
      var4.clear();
      boolean var5 = Rand.NextBool(2);
      if (var3) {
         var5 = true;
      }

      int var6 = var5 ? 1 : -1;
      Vector2 var7 = IsoDirections.N.ToVector();
      float var8 = 0.5235988F;
      if (var3) {
         var8 = 0.0F;
      }

      var7.rotate(Rand.Next(-var8, var8));
      var4.addElement("vehicle1", (float)(-var6) * 2.0F, 0.0F, var7.getDirection(), 2.0F, 5.0F);
      float var9 = 0.0F;
      var4.addElement("manhole", (float)var6 * 1.5F, 1.5F, var9, 3.0F, 3.0F);
      int var10 = Rand.Next(0, 3);

      for(int var11 = 0; var11 < var10; ++var11) {
         var9 = 0.0F;
         var4.addElement("tool", (float)var6 * Rand.Next(0.0F, 3.0F), -Rand.Next(0.7F, 2.3F), var9, 1.0F, 1.0F);
      }

      var4.setParameter("zone", var1);
      return true;
   }

   public void spawnElement(VehicleStorySpawner var1, VehicleStorySpawner.Element var2) {
      IsoGridSquare var3 = var2.square;
      if (var3 != null) {
         float var4 = PZMath.max(var2.position.x - (float)var3.x, 0.001F);
         float var5 = PZMath.max(var2.position.y - (float)var3.y, 0.001F);
         float var6 = 0.0F;
         float var7 = var2.z;
         Zone var8 = (Zone)var1.getParameter("zone", Zone.class);
         BaseVehicle var9 = (BaseVehicle)var1.getParameter("vehicle1", BaseVehicle.class);
         switch (var2.id) {
            case "manhole":
               var3.AddTileObject(IsoObject.getNew(var3, "street_decoration_01_15", (String)null, false));
               IsoGridSquare var16 = var3.getAdjacentSquare(IsoDirections.E);
               if (var16 != null) {
                  var16.AddTileObject(IsoObject.getNew(var16, "street_decoration_01_26", (String)null, false));
               }

               var16 = var3.getAdjacentSquare(IsoDirections.W);
               if (var16 != null) {
                  var16.AddTileObject(IsoObject.getNew(var16, "street_decoration_01_26", (String)null, false));
               }

               var16 = var3.getAdjacentSquare(IsoDirections.S);
               if (var16 != null) {
                  var16.AddTileObject(IsoObject.getNew(var16, "street_decoration_01_26", (String)null, false));
               }

               var16 = var3.getAdjacentSquare(IsoDirections.N);
               if (var16 != null) {
                  var16.AddTileObject(IsoObject.getNew(var16, "street_decoration_01_26", (String)null, false));
               }
               break;
            case "tool":
               String var15 = (String)this.tools.get(Rand.Next(this.tools.size()));
               ItemSpawner.spawnItem(var15, var3, var4, var5, var6);
               break;
            case "vehicle1":
               ArrayList var12 = new ArrayList();
               var12.add("Base.PickUpTruck");
               var12.add("Base.VanUtility");
               String var13 = (String)var12.get(Rand.Next(var12.size()));
               var9 = this.addVehicle(var8, var2.position.x, var2.position.y, var7, var2.direction, (String)null, var13, (Integer)null, "ConstructionWorker");
               if (var9 != null) {
                  var9.setAlarmed(false);
                  String var14 = "ConstructionWorker";
                  if (var9.getZombieType() != null) {
                     var14 = var9.getRandomZombieType();
                  }

                  this.addZombiesOnVehicle(Rand.Next(2, 5), var14, 0, var9);
                  this.addZombiesOnVehicle(1, "Foreman", 0, var9);
                  var1.setParameter("vehicle1", var9);
               }
         }

      }
   }
}
