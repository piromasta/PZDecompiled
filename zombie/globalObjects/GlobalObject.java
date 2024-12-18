package zombie.globalObjects;

import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaManager;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;

public abstract class GlobalObject {
   protected GlobalObjectSystem system;
   protected int x;
   protected int y;
   protected int z;
   protected final KahluaTable modData;

   GlobalObject(GlobalObjectSystem var1, int var2, int var3, int var4) {
      this.system = var1;
      this.x = var2;
      this.y = var3;
      this.z = var4;
      this.modData = LuaManager.platform.newTable();
   }

   public GlobalObjectSystem getSystem() {
      return this.system;
   }

   public void setLocation(int var1, int var2, int var3) {
   }

   public int getX() {
      return this.x;
   }

   public int getY() {
      return this.y;
   }

   public int getZ() {
      return this.z;
   }

   public IsoGridSquare getSquare() {
      return IsoWorld.instance.CurrentCell.getGridSquare(this.x, this.y, this.z);
   }

   public IsoObject getIsoObject() {
      IsoGridSquare var1 = this.getSquare();
      if (var1 == null) {
         return null;
      } else {
         for(int var2 = 0; var2 < var1.getObjects().size(); ++var2) {
            IsoObject var3 = (IsoObject)var1.getObjects().get(var2);
            if (this.isValidIsoObject(var3)) {
               return var3;
            }
         }

         return null;
      }
   }

   public boolean isValidIsoObject(IsoObject var1) {
      if (var1 == null) {
         return false;
      } else {
         KahluaTable var2 = var1.getModData();
         if (!"farming".equals(this.system.getName())) {
            return false;
         } else {
            return this.getModData().rawget("state") != null && this.getModData().rawget("nbOfGrow") != null & this.getModData().rawget("health") != null;
         }
      }
   }

   public KahluaTable getModData() {
      return this.modData;
   }

   public void Reset() {
      this.system = null;
      this.modData.wipe();
   }

   public void destroyThisObject() {
      Object var1;
      if ("farming".equals(this.system.getName())) {
         if (this.getSquare() != null) {
            this.getSquare().playSound("RemovePlant");
         }

         var1 = LuaManager.getFunctionObject("SFarmingSystem.destroyPlant");
         if (var1 != null) {
            LuaManager.caller.pcallvoid(LuaManager.thread, var1, this.getSquare());
         }
      } else if ("campfire".equals(this.system.getName())) {
         var1 = LuaManager.getFunctionObject("SCampfireSystem.putOut");
         if (var1 != null) {
            LuaManager.caller.pcallvoid(LuaManager.thread, var1, this.getSquare());
         }
      }

   }
}
