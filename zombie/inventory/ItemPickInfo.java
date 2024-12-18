package zombie.inventory;

import java.util.ArrayList;
import java.util.Objects;
import zombie.GameTime;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.areas.IsoRoom;
import zombie.iso.zones.Zone;
import zombie.scripting.itemConfig.SelectorBucket;
import zombie.scripting.itemConfig.enums.SelectorType;
import zombie.scripting.itemConfig.enums.SituatedType;
import zombie.scripting.objects.VehicleScript;
import zombie.util.StringUtils;

public class ItemPickInfo {
   private ItemPickerJava.ItemPickerRoom roomDist;
   private ItemContainer itemContainer;
   private int worldAgeDays;
   private boolean exterior;
   private boolean isJunk;
   private int roomID = -1;
   private int containerID = -1;
   private int vehicleID = -1;
   private final int[] zones = new int[32];
   private int zoneCount = 0;
   private final int[] tiles = new int[16];
   private int tileCount = 0;
   private float resultValue = 0.0F;
   private static final ItemPickInfo instance = new ItemPickInfo();

   private ItemPickInfo() {
   }

   private void reset() {
      this.worldAgeDays = -1;
      this.exterior = true;
      this.isJunk = false;
      this.roomID = -1;
      this.containerID = -1;
      this.vehicleID = -1;
      this.zoneCount = 0;
      this.tileCount = 0;
      this.resultValue = 0.0F;
      this.roomDist = null;
      this.itemContainer = null;
   }

   protected boolean isShop() {
      return this.roomDist != null && this.roomDist.isShop;
   }

   protected boolean isJunk() {
      return this.isJunk;
   }

   protected void setJunk(boolean var1) {
      this.isJunk = var1;
   }

   protected void updateRoomDist(ItemPickerJava.ItemPickerRoom var1) {
      if (var1 != null) {
         this.roomDist = var1;
      }

   }

   public boolean isMatch(SelectorBucket var1) {
      if (var1.getSelectorType() != SelectorType.Default && var1.getSelectorType() != SelectorType.None) {
         int var2;
         switch (var1.getSelectorType()) {
            case Container:
               return var1.containsSelectorID(this.containerID);
            case Room:
               return var1.containsSelectorID(this.roomID);
            case Zone:
               if (var1.hasSelectorIDs()) {
                  for(var2 = 0; var2 < this.zoneCount; ++var2) {
                     if (var1.containsSelectorID(this.zones[var2])) {
                        return true;
                     }
                  }
               }
               break;
            case Situated:
               if (var1.getSelectorSituated() == SituatedType.Exterior) {
                  return this.exterior;
               }

               if (var1.getSelectorSituated() == SituatedType.Interior) {
                  return !this.exterior;
               }

               if (var1.getSelectorSituated() == SituatedType.Shop) {
                  return this.isShop();
               }

               if (var1.getSelectorSituated() == SituatedType.Junk) {
                  return this.isJunk();
               }
               break;
            case WorldAge:
               return this.worldAgeDays >= var1.getSelectorWorldAge();
            case Tile:
               if (var1.hasSelectorIDs()) {
                  for(var2 = 0; var2 < this.tileCount; ++var2) {
                     if (var1.containsSelectorID(this.tiles[var2])) {
                        return true;
                     }
                  }
               }
               break;
            case Vehicle:
               return var1.containsSelectorID(this.vehicleID);
         }

         return false;
      } else {
         return true;
      }
   }

   public void setResultValue(float var1) {
      this.resultValue = var1;
   }

   public float getResultValue() {
      return this.resultValue;
   }

   private static ItemPickerJava.ItemPickerRoom getRoomDist(IsoGridSquare var0, ItemContainer var1) {
      IsoRoom var2 = var0.getRoom();
      if (var2 == null) {
         return null;
      } else {
         ItemPickerJava.ItemPickerRoom var3 = null;
         if (ItemPickerJava.rooms.containsKey("all")) {
            var3 = (ItemPickerJava.ItemPickerRoom)ItemPickerJava.rooms.get("all");
         }

         if (ItemPickerJava.rooms.containsKey(var2.getName())) {
            var3 = (ItemPickerJava.ItemPickerRoom)ItemPickerJava.rooms.get(var2.getName());
         }

         return var3;
      }
   }

   public static ItemPickInfo GetPickInfo(ItemContainer var0, Caller var1) {
      instance.reset();
      if (var0 == null) {
         if (Core.bDebug) {
            DebugLog.log("ItemPickInfo -> unable to set pick info: container == null, Caller: " + var1);
         }

         return null;
      } else {
         instance.itemContainer = var0;
         instance.worldAgeDays = (int)(GameTime.instance.getWorldAgeHours() / 24.0);
         String var2 = var0.getType();
         if (var2 != null) {
            instance.containerID = ItemConfigurator.GetIdForString(var2);
            if (instance.containerID == -1) {
               DebugLog.log("ItemPickInfo -> cannot get ID for container: " + var2);
            }
         }

         ItemContainer var3 = var0.getOutermostContainer();
         IsoGridSquare var4 = null;
         if (var3.getVehiclePart() != null && var3.getVehiclePart().getVehicle() != null) {
            var4 = var3.getVehiclePart().getVehicle().getSquare();
            VehicleScript var5 = var3.getVehiclePart().getVehicle().getScript();
            if (var5 != null && var5.getName() != null) {
               instance.vehicleID = ItemConfigurator.GetIdForString(var5.getName());
            }
         }

         if (var4 == null) {
            var4 = var3.getSquare();
         }

         if (var4 == null && Objects.equals(var3.type, "floor")) {
         }

         if (var4 != null) {
            instance.roomDist = getRoomDist(var4, var0);
            instance.exterior = !var4.isInARoom();
            String var10 = var4.getRoom() != null ? var4.getRoom().getName() : null;
            if (var10 != null) {
               instance.roomID = ItemConfigurator.GetIdForString(var10);
               if (instance.roomID == -1) {
                  DebugLog.log("ItemPickInfo -> cannot get ID for room: " + var10);
               }
            }

            int var8;
            for(int var6 = 0; var6 < var4.getObjects().size(); ++var6) {
               IsoObject var7 = (IsoObject)var4.getObjects().get(var6);
               if (var7.getSprite() != null && var7.getSprite().getID() >= 0 && var7.getSprite().getName() != null) {
                  var8 = var7.getSprite().getID();
                  if (var8 != -1) {
                     instance.tiles[instance.tileCount++] = var8;
                     if (instance.tileCount >= instance.tiles.length) {
                        break;
                     }
                  } else {
                     DebugLog.log("ItemPickInfo -> cannot get ID for tile: " + var7.getSprite().getName());
                  }
               }
            }

            ArrayList var11 = IsoWorld.instance.MetaGrid.getZonesAt(var4.x, var4.y, 0);

            for(int var9 = 0; var9 < var11.size(); ++var9) {
               Zone var12 = (Zone)var11.get(var9);
               if (var12.type != null && !StringUtils.isNullOrWhitespace(var12.type)) {
                  var8 = ItemConfigurator.GetIdForString(var12.type);
                  if (var8 >= 0) {
                     instance.zones[instance.zoneCount++] = var8;
                     if (instance.zoneCount >= instance.zones.length) {
                        break;
                     }
                  } else {
                     DebugLog.log("ItemPickInfo -> cannot get ID for zone: " + var12.type);
                  }
               }

               if (var12.name != null && !StringUtils.isNullOrWhitespace(var12.name)) {
                  var8 = ItemConfigurator.GetIdForString(var12.name);
                  if (var8 >= 0) {
                     instance.zones[instance.zoneCount++] = var8;
                     if (instance.zoneCount >= instance.zones.length) {
                        break;
                     }
                  } else {
                     DebugLog.log("ItemPickInfo -> cannot get ID for zone: " + var12.name);
                  }
               }
            }
         } else if (Core.bDebug && var1 != ItemPickInfo.Caller.FillContainerType) {
            DebugLog.log("ItemPickInfo -> unable to set source grid pick info, Caller: " + var1 + ", for Container: " + var2);
            if (var3 != var0) {
               DebugLog.log("ItemPickInfo -> Outermost Container: " + var3.type);
            }

            if (var0.getVehiclePart() != null && var0.getVehiclePart().getVehicle() != null) {
               DebugLog.log("ItemPickInfo -> Vehicle: " + var0.getVehiclePart().getVehicle().getName());
            }
         }

         return instance;
      }
   }

   public static enum Caller {
      FillContainer,
      FillContainerType,
      RollProceduralItem,
      RollItem,
      DoRollItem,
      RollContainerItem,
      Unknown;

      private Caller() {
      }
   }
}
