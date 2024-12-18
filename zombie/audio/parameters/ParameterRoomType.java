package zombie.audio.parameters;

import zombie.audio.FMODGlobalParameter;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.zones.RoomTone;

public final class ParameterRoomType extends FMODGlobalParameter {
   static ParameterRoomType instance;
   static RoomType roomType = null;

   public ParameterRoomType() {
      super("RoomType");
      instance = this;
   }

   public float calculateCurrentValue() {
      return (float)this.getRoomType().label;
   }

   private RoomType getRoomType() {
      if (roomType != null) {
         return roomType;
      } else {
         IsoGameCharacter var1 = this.getCharacter();
         if (var1 == null) {
            return ParameterRoomType.RoomType.Generic;
         } else {
            BuildingDef var2 = var1.getCurrentBuildingDef();
            if (var2 == null) {
               return ParameterRoomType.RoomType.Generic;
            } else if (var1.getCurrentSquare().getZ() < 0) {
               return var1.getCurrentBuildingDef().getW() > 48 && var1.getCurrentBuildingDef().getH() > 48 ? ParameterRoomType.RoomType.Bunker : ParameterRoomType.RoomType.Basement;
            } else {
               IsoMetaGrid var3 = IsoWorld.instance.getMetaGrid();
               IsoMetaCell var4 = var3.getCellData(PZMath.fastfloor(var1.getX() / (float)IsoCell.CellSizeInSquares), PZMath.fastfloor(var1.getY() / (float)IsoCell.CellSizeInSquares));
               if (var4 != null && !var4.roomTones.isEmpty()) {
                  RoomDef var5 = var1.getCurrentRoomDef();
                  RoomTone var6 = null;

                  for(int var7 = 0; var7 < var4.roomTones.size(); ++var7) {
                     RoomTone var8 = (RoomTone)var4.roomTones.get(var7);
                     RoomDef var9 = var3.getRoomAt(var8.x, var8.y, var8.z);
                     if (var9 != null) {
                        if (var9 == var5) {
                           return ParameterRoomType.RoomType.valueOf(var8.enumValue);
                        }

                        if (var8.entireBuilding && var9.building == var2) {
                           var6 = var8;
                        }
                     }
                  }

                  if (var6 != null) {
                     return ParameterRoomType.RoomType.valueOf(var6.enumValue);
                  } else {
                     return ParameterRoomType.RoomType.Generic;
                  }
               } else {
                  return ParameterRoomType.RoomType.Generic;
               }
            }
         }
      }
   }

   private IsoGameCharacter getCharacter() {
      IsoPlayer var1 = null;

      for(int var2 = 0; var2 < IsoPlayer.numPlayers; ++var2) {
         IsoPlayer var3 = IsoPlayer.players[var2];
         if (var3 != null && (var1 == null || var1.isDead() && var3.isAlive() || var1.Traits.Deaf.isSet() && !var3.Traits.Deaf.isSet())) {
            var1 = var3;
         }
      }

      return var1;
   }

   public static void setRoomType(int var0) {
      try {
         roomType = ParameterRoomType.RoomType.values()[var0];
      } catch (ArrayIndexOutOfBoundsException var2) {
         roomType = null;
      }

   }

   public static void render(IsoPlayer var0) {
      if (instance != null) {
         if (var0 == instance.getCharacter()) {
            if (var0 == IsoCamera.frameState.CamCharacter) {
               var0.drawDebugTextBelow("RoomType : " + instance.getRoomType().name());
            }
         }
      }
   }

   private static enum RoomType {
      Generic(0),
      Barn(1),
      Mall(2),
      Warehouse(3),
      Prison(4),
      Church(5),
      Office(6),
      Factory(7),
      MovieTheater(8),
      Basement(9),
      Bunker(10),
      House(11),
      Commercial(12);

      final int label;

      private RoomType(int var3) {
         this.label = var3;
      }
   }
}
