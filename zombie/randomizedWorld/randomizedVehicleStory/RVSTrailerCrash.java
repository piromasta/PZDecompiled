package zombie.randomizedWorld.randomizedVehicleStory;

import java.util.ArrayList;
import zombie.core.random.Rand;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.Vector2;
import zombie.iso.zones.Zone;
import zombie.vehicles.BaseVehicle;

public final class RVSTrailerCrash extends RandomizedVehicleStoryBase {
   public RVSTrailerCrash() {
      this.name = "Trailer Crash";
      this.minZoneWidth = 5;
      this.minZoneHeight = 12;
      this.setChance(45);
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
      var4.addElement("trailer", var7, var8 + 2.5F + 1.0F + (float)var9 / 2.0F, var6.getDirection(), 2.0F, (float)var9);
      boolean var10 = Rand.NextBool(2);
      var6 = var10 ? IsoDirections.E.ToVector() : IsoDirections.W.ToVector();
      var6.rotate(Rand.Next(-var5, var5));
      float var11 = 0.0F;
      float var12 = var8 - 2.5F - 1.0F;
      var4.addElement("vehicle2", var11, var12, var6.getDirection(), 2.0F, 5.0F);
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
         switch (var2.id) {
            case "vehicle1":
               var9 = this.addVehicle(var5, var2.position.x, var2.position.y, var4, var2.direction, (String)null, "Base.PickUpVan", (Integer)null, (String)null);
               if (var9 != null) {
                  var9.setAlarmed(false);
                  var9 = var9.setSmashed("Front");
                  ArrayList var13 = new ArrayList();
                  var13.add("Base.Trailer");
                  var13.add("Base.TrailerCover");
                  var13.add("Base.Trailer_Livestock");
                  String var11 = (String)var13.get(Rand.Next(var13.size()));
                  if (Rand.NextBool(6)) {
                     var11 = "Base.TrailerAdvert";
                  }

                  BaseVehicle var12 = this.addTrailer(var9, var5, var3.getChunk(), (String)null, (String)null, var11);
                  if (var12 != null && Rand.NextBool(3)) {
                     var12.setAngles(var12.getAngleX(), Rand.Next(90.0F, 110.0F), var12.getAngleZ());
                  }

                  if (Rand.Next(10) < 4) {
                     this.addZombiesOnVehicle(Rand.Next(2, 5), (String)null, (Integer)null, var9);
                  }

                  var1.setParameter("vehicle1", var9);
               }
               break;
            case "vehicle2":
               var9 = this.addVehicle(var5, var2.position.x, var2.position.y, var4, var2.direction, "bad", (String)null, (Integer)null, (String)null);
               if (var9 != null) {
                  var9.setAlarmed(false);
                  String var10 = var6 ? "Right" : "Left";
                  var9 = var9.setSmashed(var10);
                  var9.setBloodIntensity(var10, 1.0F);
                  var1.setParameter("vehicle2", var9);
               }
         }

      }
   }
}
