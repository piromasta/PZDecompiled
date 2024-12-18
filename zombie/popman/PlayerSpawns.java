package zombie.popman;

import java.util.ArrayList;
import zombie.SandboxOptions;
import zombie.iso.BuildingDef;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;

final class PlayerSpawns {
   private final ArrayList<PlayerSpawn> playerSpawns = new ArrayList();

   PlayerSpawns() {
   }

   public void addSpawn(int var1, int var2, int var3) {
      PlayerSpawn var4 = new PlayerSpawn(var1, var2, var3);
      if (var4.building != null) {
         this.playerSpawns.add(var4);
      }

   }

   public void update() {
      long var1 = System.currentTimeMillis();

      for(int var3 = 0; var3 < this.playerSpawns.size(); ++var3) {
         PlayerSpawn var4 = (PlayerSpawn)this.playerSpawns.get(var3);
         if (var4.counter == -1L) {
            var4.counter = var1;
         }

         if (var4.counter + 10000L <= var1) {
            this.playerSpawns.remove(var3--);
         }
      }

   }

   public boolean allowZombie(IsoGridSquare var1) {
      for(int var2 = 0; var2 < this.playerSpawns.size(); ++var2) {
         PlayerSpawn var3 = (PlayerSpawn)this.playerSpawns.get(var2);
         if (!var3.allowZombie(var1)) {
            return false;
         }
      }

      return true;
   }

   private static class PlayerSpawn {
      public int x;
      public int y;
      public long counter;
      public BuildingDef building;
      public RoomDef room;

      public PlayerSpawn(int var1, int var2, int var3) {
         this.x = var1;
         this.y = var2;
         this.counter = -1L;
         RoomDef var4 = IsoWorld.instance.getMetaGrid().getRoomAt(var1, var2, var3);
         if (var4 != null) {
            this.building = var4.getBuilding();
            this.room = var4;
         }

      }

      public boolean allowZombie(IsoGridSquare var1) {
         switch (SandboxOptions.instance.Lore.PlayerSpawnZombieRemoval.getValue()) {
            case 1:
               if (this.building == null) {
                  return true;
               }

               if (var1.getBuilding() != null && this.building == var1.getBuilding().getDef()) {
                  return false;
               }

               if (var1.getX() >= this.building.getX() - 15 && var1.getX() < this.building.getX2() + 15 && var1.getY() >= this.building.getY() - 15 && var1.getY() < this.building.getY2() + 15) {
                  return false;
               }
               break;
            case 2:
               if (this.building == null) {
                  return true;
               }

               if (var1.getBuilding() != null && this.building == var1.getBuilding().getDef()) {
                  return false;
               }
               break;
            case 3:
               if (this.room == null) {
                  return true;
               }

               if (var1.getRoom() != null && this.room == var1.getRoom().getRoomDef()) {
                  return false;
               }
               break;
            case 4:
               return true;
         }

         return true;
      }
   }
}
