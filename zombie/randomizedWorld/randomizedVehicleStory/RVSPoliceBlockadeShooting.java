package zombie.randomizedWorld.randomizedVehicleStory;

import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.Vector2;
import zombie.iso.zones.Zone;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;

public final class RVSPoliceBlockadeShooting extends RandomizedVehicleStoryBase {
   public RVSPoliceBlockadeShooting() {
      this.name = "Police Blockade Shooting";
      this.minZoneWidth = 8;
      this.minZoneHeight = 8;
      this.setChance(10);
      this.setMaximumDays(30);
   }

   public boolean isValid(Zone var1, IsoChunk var2, boolean var3) {
      boolean var4 = super.isValid(var1, var2, var3);
      return !var4 ? false : var1.isRectangle();
   }

   public void randomizeVehicleStory(Zone var1, IsoChunk var2) {
      this.callVehicleStorySpawner(var1, var2, 0.0F);
   }

   public boolean initVehicleStorySpawner(Zone var1, IsoChunk var2, boolean var3) {
      VehicleStorySpawner var4 = VehicleStorySpawner.getInstance();
      var4.clear();
      float var5 = 0.17453292F;
      if (var3) {
         var5 = 0.0F;
      }

      float var6 = 1.5F;
      float var7 = 1.0F;
      if (this.zoneWidth >= 10) {
         var6 = 2.5F;
         var7 = 0.0F;
      }

      boolean var8 = Rand.NextBool(2);
      if (var3) {
         var8 = true;
      }

      IsoDirections var9 = Rand.NextBool(2) ? IsoDirections.W : IsoDirections.E;
      Vector2 var10 = var9.ToVector();
      var10.rotate(Rand.Next(-var5, var5));
      var4.addElement("vehicle1", -var6, var7, var10.getDirection(), 2.0F, 5.0F);
      var10 = var9.Rot180().ToVector();
      var10.rotate(Rand.Next(-var5, var5));
      var4.addElement("vehicle2", var6, -var7, var10.getDirection(), 2.0F, 5.0F);
      var4.addElement("barricade", 0.0F, var8 ? -var7 - 2.5F : var7 + 2.5F, IsoDirections.N.ToVector().getDirection(), (float)this.zoneWidth, 1.0F);
      int var11 = Rand.Next(7, 15);

      for(int var12 = 0; var12 < var11; ++var12) {
         var4.addElement("corpse", Rand.Next((float)(-this.zoneWidth) / 2.0F + 1.0F, (float)this.zoneWidth / 2.0F - 1.0F), var8 ? (float)Rand.Next(-7, -4) - var7 : (float)Rand.Next(5, 8) + var7, IsoDirections.getRandom().ToVector().getDirection(), 1.0F, 2.0F);
      }

      String var13 = "Base.CarLightsPolice";
      if (Rand.NextBool(3)) {
         var13 = "Base.PickUpVanLightsPolice";
      }

      var4.setParameter("zone", var1);
      var4.setParameter("script", var13);
      return true;
   }

   public void spawnElement(VehicleStorySpawner var1, VehicleStorySpawner.Element var2) {
      IsoGridSquare var3 = var2.square;
      if (var3 != null) {
         float var4 = var2.z;
         Zone var5 = (Zone)var1.getParameter("zone", Zone.class);
         String var6 = var1.getParameterString("script");
         BaseVehicle var9;
         switch (var2.id) {
            case "barricade":
               int var12;
               IsoGridSquare var13;
               int var14;
               int var18;
               int var19;
               if (this.horizontalZone) {
                  var14 = PZMath.fastfloor(var2.position.y - var2.width / 2.0F);
                  var18 = PZMath.fastfloor(var2.position.y + var2.width / 2.0F) - 1;
                  var19 = PZMath.fastfloor(var2.position.x);

                  for(var12 = var14; var12 <= var18; ++var12) {
                     var13 = IsoCell.getInstance().getGridSquare(var19, var12, var5.z);
                     if (var13 != null) {
                        if (var12 != var14 && var12 != var18) {
                           var13.AddTileObject(IsoObject.getNew(var13, "construction_01_9", (String)null, false));
                        } else {
                           var13.AddTileObject(IsoObject.getNew(var13, "street_decoration_01_26", (String)null, false));
                        }
                     }
                  }

                  return;
               } else {
                  var14 = PZMath.fastfloor(var2.position.x - var2.width / 2.0F);
                  var18 = PZMath.fastfloor(var2.position.x + var2.width / 2.0F) - 1;
                  var19 = PZMath.fastfloor(var2.position.y);

                  for(var12 = var14; var12 <= var18; ++var12) {
                     var13 = IsoCell.getInstance().getGridSquare(var12, var19, var5.z);
                     if (var13 != null) {
                        if (var12 != var14 && var12 != var18) {
                           var13.AddTileObject(IsoObject.getNew(var13, "construction_01_8", (String)null, false));
                        } else {
                           var13.AddTileObject(IsoObject.getNew(var13, "street_decoration_01_26", (String)null, false));
                        }
                     }
                  }

                  return;
               }
            case "corpse":
               var9 = (BaseVehicle)var1.getParameter("vehicle1", BaseVehicle.class);
               if (var9 != null) {
                  createRandomDeadBody(var2.position.x, var2.position.y, (float)var5.z, var2.direction, false, 10, 10, (String)null);
                  IsoDirections var16 = this.horizontalZone ? (var2.position.x < var9.getX() ? IsoDirections.W : IsoDirections.E) : (var2.position.y < var9.getY() ? IsoDirections.N : IsoDirections.S);
                  float var17 = var16.ToVector().getDirection();
                  this.addTrailOfBlood(var2.position.x, var2.position.y, var2.z, var17, 5);
               }
               break;
            case "vehicle1":
            case "vehicle2":
               var9 = this.addVehicle(var5, var2.position.x, var2.position.y, var4, var2.direction, (String)null, var6, (Integer)null, (String)null);
               if (var9 != null) {
                  var9.setAlarmed(false);
                  var1.setParameter(var2.id, var9);
                  if (Rand.NextBool(3)) {
                     var9.setHeadlightsOn(true);
                     var9.setLightbarLightsMode(2);
                     VehiclePart var10 = var9.getBattery();
                     if (var10 != null) {
                        var10.setLastUpdated(0.0F);
                     }
                  }

                  String var15 = "PoliceRiot";
                  if (var9.getZombieType() != null && var9.hasZombieType("Police_SWAT")) {
                     var15 = var9.getRandomZombieType();
                  }

                  Integer var11 = 0;
                  this.addZombiesOnVehicle(Rand.Next(2, 4), var15, var11, var9);
               }
         }

      }
   }
}
