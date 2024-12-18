package zombie.randomizedWorld.randomizedBuilding;

import java.util.ArrayList;
import zombie.characters.IsoZombie;
import zombie.characters.SurvivorDesc;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;

public final class RBMayorWestPoint extends RandomizedBuildingBase {
   public void randomizeBuilding(BuildingDef var1) {
      IsoCell var2 = IsoWorld.instance.CurrentCell;

      for(int var3 = var1.x - 1; var3 < var1.x2 + 1; ++var3) {
         for(int var4 = var1.y - 1; var4 < var1.y2 + 1; ++var4) {
            for(int var5 = 0; var5 < 8; ++var5) {
               IsoGridSquare var6 = var2.getGridSquare(var3, var4, var5);
               if (var6 != null && this.roomValid(var6)) {
                  RBBasic.doOfficeStuff(var6);
               }
            }
         }
      }

      ArrayList var7 = this.addZombies(var1, 1, "Mayor_West_point", 0, (RoomDef)null);
      if (!var7.isEmpty()) {
         IsoZombie var8 = (IsoZombie)var7.get(0);
         var8.getHumanVisual().setSkinTextureIndex(1);
         SurvivorDesc var9 = var8.getDescriptor();
         if (var9 != null) {
            var9.setForename("Richard");
            var9.setSurname("Rosenwald");
         }
      }
   }

   public boolean roomValid(IsoGridSquare var1) {
      return var1.getRoom() != null && "mayorwestpointoffice".equals(var1.getRoom().getName());
   }

   public boolean isValid(BuildingDef var1, boolean var2) {
      return var1.getRoom("mayorwestpointoffice") != null;
   }

   public RBMayorWestPoint() {
      this.name = "MayorWestPoint";
      this.reallyAlwaysForce = true;
      this.setAlwaysDo(true);
   }
}
