package zombie.randomizedWorld.randomizedBuilding;

import java.util.ArrayList;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.characters.SurvivorDesc;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.objects.IsoWindow;

public final class RBJackieJaye extends RandomizedBuildingBase {
   public void randomizeBuilding(BuildingDef var1) {
      IsoCell var2 = IsoWorld.instance.CurrentCell;

      for(int var3 = var1.x - 1; var3 < var1.x2 + 1; ++var3) {
         for(int var4 = var1.y - 1; var4 < var1.y2 + 1; ++var4) {
            for(int var5 = 0; var5 < 8; ++var5) {
               IsoGridSquare var6 = var2.getGridSquare(var3, var4, var5);
               if (var6 != null) {
                  if (this.roomValid(var6)) {
                     RBBasic.doOfficeStuff(var6);
                  }

                  for(int var7 = 0; var7 < var6.getObjects().size(); ++var7) {
                     IsoObject var8 = (IsoObject)var6.getObjects().get(var7);
                     if (var8 instanceof IsoWindow && var6.getRoom() != null && ((IsoWindow)var8).getOppositeSquare() != null && ((IsoWindow)var8).getOppositeSquare().isOutside()) {
                        ((IsoWindow)var8).addSheet((IsoGameCharacter)null);
                        ((IsoWindow)var8).HasCurtains().ToggleDoor((IsoGameCharacter)null);
                     } else if (var8 instanceof IsoWindow && var6.isOutside() && ((IsoWindow)var8).getOppositeSquare() != null && ((IsoWindow)var8).getOppositeSquare().getRoom() != null) {
                        ((IsoWindow)var8).addSheet((IsoGameCharacter)null);
                        ((IsoWindow)var8).HasCurtains().ToggleDoor((IsoGameCharacter)null);
                     }
                  }
               }
            }
         }
      }

      RoomDef var10 = var1.getRoom("jackiejayestudio");
      if (var10 != null) {
         ArrayList var11 = this.addZombies(var1, 1, "Jackie_Jaye", 100, var10);
         IsoZombie var12 = (IsoZombie)var11.get(0);
         var12.getHumanVisual().setSkinTextureIndex(1);
         SurvivorDesc var13 = var12.getDescriptor();
         if (var13 != null) {
            var13.setForename("Jackie");
            var13.setSurname("Jaye");
            IsoGridSquare var14 = var10.getFreeSquare();
            if (var14 != null) {
               this.addItemOnGround(var14, "Microphone");
               this.addItemOnGround(var14, "Notepad");
               this.addItemOnGround(var14, "Pen");
               InventoryItem var15 = InventoryItemFactory.CreateItem("Base.PressID");
               if (var15 != null) {
                  var15.nameAfterDescriptor(var13);
                  var12.addItemToSpawnAtDeath(var15);
                  IsoGridSquare var9 = var14.getCell().getGridSquare(12482, 3910, 0);
                  if (var9 != null) {
                     this.addSleepingBagWestEast(12482, 3910, 0);
                  }
               }
            }
         }
      }
   }

   public boolean roomValid(IsoGridSquare var1) {
      return var1.getRoom() != null && "jackiejayestudio".equals(var1.getRoom().getName());
   }

   public boolean isValid(BuildingDef var1, boolean var2) {
      return var1.getRoom("jackiejayestudio") != null;
   }

   public RBJackieJaye() {
      this.name = "JackieJaye";
      this.reallyAlwaysForce = true;
      this.setAlwaysDo(true);
   }
}
