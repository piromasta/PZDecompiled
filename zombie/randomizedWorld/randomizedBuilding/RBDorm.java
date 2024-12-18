package zombie.randomizedWorld.randomizedBuilding;

import java.util.Objects;
import zombie.core.random.Rand;
import zombie.inventory.ItemPickerJava;
import zombie.inventory.ItemSpawner;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.sprite.IsoSprite;

public final class RBDorm extends RandomizedBuildingBase {
   public void randomizeBuilding(BuildingDef var1) {
      IsoCell var2 = IsoWorld.instance.CurrentCell;

      for(int var3 = var1.x - 1; var3 < var1.x2 + 1; ++var3) {
         for(int var4 = var1.y - 1; var4 < var1.y2 + 1; ++var4) {
            for(int var5 = 0; var5 < 8; ++var5) {
               IsoGridSquare var6 = var2.getGridSquare(var3, var4, var5);
               if (var6 != null && this.roomValid(var6) && var6.getObjects().size() <= 3) {
                  for(int var7 = 0; var7 < var6.getObjects().size(); ++var7) {
                     IsoObject var8 = (IsoObject)var6.getObjects().get(var7);
                     if (var8.isTableSurface() && var6.getObjects().size() <= 3 && Rand.NextBool(3)) {
                        IsoDirections var9 = this.getFacing(var8.getSprite());
                        if (var9 != null) {
                           if (var9 == IsoDirections.E) {
                              ItemSpawner.spawnItem(RBBasic.getDormClutterItem(), var6, 0.42F, Rand.Next(0.34F, 0.74F), var8.getSurfaceOffsetNoTable() / 96.0F);
                           }

                           if (var9 == IsoDirections.W) {
                              ItemSpawner.spawnItem(RBBasic.getDormClutterItem(), var6, 0.64F, Rand.Next(0.34F, 0.74F), var8.getSurfaceOffsetNoTable() / 96.0F);
                           }

                           if (var9 == IsoDirections.N) {
                              ItemSpawner.spawnItem(RBBasic.getDormClutterItem(), var6, Rand.Next(0.44F, 0.64F), 0.67F, var8.getSurfaceOffsetNoTable() / 96.0F);
                           }

                           if (var9 == IsoDirections.S) {
                              ItemSpawner.spawnItem(RBBasic.getDormClutterItem(), var6, Rand.Next(0.44F, 0.64F), 0.42F, var8.getSurfaceOffsetNoTable() / 96.0F);
                           }
                        } else {
                           ItemSpawner.spawnItem(RBBasic.getDormClutterItem(), var6, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), var8.getSurfaceOffsetNoTable() / 96.0F);
                        }

                        if (Rand.NextBool(3)) {
                           if (var9 != null) {
                              if (var9 == IsoDirections.E) {
                                 ItemSpawner.spawnItem(RBBasic.getDormClutterItem(), var6, 0.42F, Rand.Next(0.34F, 0.74F), var8.getSurfaceOffsetNoTable() / 96.0F);
                              }

                              if (var9 == IsoDirections.W) {
                                 ItemSpawner.spawnItem(RBBasic.getDormClutterItem(), var6, 0.64F, Rand.Next(0.34F, 0.74F), var8.getSurfaceOffsetNoTable() / 96.0F);
                              }

                              if (var9 == IsoDirections.N) {
                                 ItemSpawner.spawnItem(RBBasic.getDormClutterItem(), var6, Rand.Next(0.44F, 0.64F), 0.67F, var8.getSurfaceOffsetNoTable() / 96.0F);
                              }

                              if (var9 == IsoDirections.S) {
                                 ItemSpawner.spawnItem(RBBasic.getDormClutterItem(), var6, Rand.Next(0.44F, 0.64F), 0.42F, var8.getSurfaceOffsetNoTable() / 96.0F);
                              }
                           } else {
                              ItemSpawner.spawnItem(RBBasic.getDormClutterItem(), var6, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), var8.getSurfaceOffsetNoTable() / 96.0F);
                           }
                        }
                     }
                  }
               }
            }
         }
      }

   }

   public boolean roomValid(IsoGridSquare var1) {
      return var1.getRoom() != null && "livingroom".equals(var1.getRoom().getName());
   }

   public boolean isValid(BuildingDef var1, boolean var2) {
      IsoCell var3 = IsoWorld.instance.CurrentCell;
      IsoGridSquare var4 = var3.getGridSquare(var1.x, var1.y, 0);
      if (var4 == null) {
         return false;
      } else {
         return var1.getRoom("livingroom") != null && ItemPickerJava.getSquareZombiesType(var4) != null && Objects.equals(ItemPickerJava.getSquareZombiesType(var4), "University");
      }
   }

   public RBDorm() {
      this.name = "Dorm";
      this.setAlwaysDo(true);
   }

   private IsoDirections getFacing(IsoSprite var1) {
      if (var1 != null && var1.getProperties().Is("Facing")) {
         switch (var1.getProperties().Val("Facing")) {
            case "N":
               return IsoDirections.N;
            case "S":
               return IsoDirections.S;
            case "W":
               return IsoDirections.W;
            case "E":
               return IsoDirections.E;
         }
      }

      return null;
   }
}
