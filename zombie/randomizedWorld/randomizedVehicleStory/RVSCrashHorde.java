package zombie.randomizedWorld.randomizedVehicleStory;

import java.util.ArrayList;
import zombie.characters.IsoZombie;
import zombie.core.random.Rand;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.Vector2;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.zones.Zone;
import zombie.vehicles.BaseVehicle;

public final class RVSCrashHorde extends RandomizedVehicleStoryBase {
   public RVSCrashHorde() {
      this.name = "Crash Horde";
      this.minZoneWidth = 8;
      this.minZoneHeight = 8;
      this.setChance(10);
      this.setMinimumDays(60);
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
      var4.addElement("vehicle1", 0.0F, 0.0F, var6.getDirection(), 2.0F, 5.0F);
      var4.setParameter("zone", var1);
      var4.setParameter("burnt", Rand.NextBool(5));
      return true;
   }

   public void spawnElement(VehicleStorySpawner var1, VehicleStorySpawner.Element var2) {
      IsoGridSquare var3 = var2.square;
      if (var3 != null) {
         float var4 = var2.z;
         Zone var5 = (Zone)var1.getParameter("zone", Zone.class);
         boolean var6 = var1.getParameterBoolean("burnt");
         switch (var2.id) {
            case "vehicle1":
               BaseVehicle var9 = this.addVehicleFlipped(var5, var2.position.x, var2.position.y, var4 + 0.25F, var2.direction, var6 ? "normalburnt" : "bad", (String)null, (Integer)null, (String)null);
               if (var9 != null) {
                  var9.setAlarmed(false);
                  int var10 = Rand.Next(4);
                  String var11 = null;
                  switch (var10) {
                     case 0:
                        var11 = "Front";
                        break;
                     case 1:
                        var11 = "Rear";
                        break;
                     case 2:
                        var11 = "Left";
                        break;
                     case 3:
                        var11 = "Right";
                  }

                  var9 = var9.setSmashed(var11);
                  var9.setBloodIntensity("Front", Rand.Next(0.7F, 1.0F));
                  var9.setBloodIntensity("Rear", Rand.Next(0.7F, 1.0F));
                  var9.setBloodIntensity("Left", Rand.Next(0.7F, 1.0F));
                  var9.setBloodIntensity("Right", Rand.Next(0.7F, 1.0F));
                  String var12 = null;
                  if (var9.getZombieType() != null) {
                     var12 = var9.getRandomZombieType();
                  }

                  ArrayList var13 = this.addZombiesOnVehicle(Rand.Next(2, 4), var12, (Integer)null, var9);
                  if (var13 != null) {
                     for(int var14 = 0; var14 < var13.size(); ++var14) {
                        IsoZombie var15 = (IsoZombie)var13.get(var14);
                        var15.upKillCount = false;
                        this.addBloodSplat(var15.getSquare(), Rand.Next(10, 20));
                        if (var6) {
                           var15.setSkeleton(true);
                           var15.getHumanVisual().setSkinTextureIndex(0);
                        } else {
                           var15.DoCorpseInventory();
                           if (Rand.NextBool(10)) {
                              var15.setFakeDead(true);
                              var15.bCrawling = true;
                              var15.setCanWalk(false);
                              var15.setCrawlerType(1);
                           }
                        }

                        new IsoDeadBody(var15, false);
                     }

                     this.addZombiesOnVehicle(Rand.Next(12, 20), (String)null, (Integer)null, var9);
                  }
               }
            default:
         }
      }
   }
}
