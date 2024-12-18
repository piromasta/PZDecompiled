package zombie.randomizedWorld.randomizedBuilding;

import zombie.core.random.Rand;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.sprite.IsoSprite;

public final class RBBarn extends RandomizedBuildingBase {
   public void randomizeBuilding(BuildingDef var1) {
      IsoCell var2 = IsoWorld.instance.CurrentCell;

      for(int var3 = var1.x - 1; var3 < var1.x2 + 1; ++var3) {
         for(int var4 = var1.y - 1; var4 < var1.y2 + 1; ++var4) {
            for(int var5 = 0; var5 < 8; ++var5) {
               IsoGridSquare var6 = var2.getGridSquare(var3, var4, var5);
               boolean var7 = var6 != null && var6.getRoom() != null && var6.getRoom().getName() != null && ("barn".equals(var6.getRoom().getName()) || "stables".equals(var6.getRoom().getName()));
               if (var6 != null && this.roomValid(var6) && var6.getObjects().size() <= 3) {
                  for(int var8 = 0; var8 < var6.getObjects().size(); ++var8) {
                     IsoObject var9 = (IsoObject)var6.getObjects().get(var8);
                     if (var9.isTableSurface() && var6.getObjects().size() <= 3 && Rand.NextBool(3)) {
                        String var10 = null;
                        if (var7) {
                           var10 = RBBasic.getBarnClutterItem();
                        } else {
                           var10 = RBBasic.getFarmStorageClutterItem();
                        }

                        var9.addItemToObjectSurface(var10);
                     }
                  }
               }
            }
         }
      }

   }

   public boolean roomValid(IsoGridSquare var1) {
      return var1.getRoom() != null && ("barn".equals(var1.getRoom().getName()) || "stables".equals(var1.getRoom().getName()) || "farmstorage".equals(var1.getRoom().getName()));
   }

   public boolean isValid(BuildingDef var1, boolean var2) {
      return var1.getRoom("barn") != null || var1.getRoom("stables") != null || var1.getRoom("farmstorage") != null;
   }

   public RBBarn() {
      this.name = "Barn";
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
