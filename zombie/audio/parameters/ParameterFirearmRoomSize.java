package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.characters.IsoPlayer;
import zombie.iso.IsoGridSquare;
import zombie.iso.areas.IsoRoom;
import zombie.iso.areas.isoregion.regions.IWorldRegion;

public final class ParameterFirearmRoomSize extends FMODLocalParameter {
   private final IsoPlayer m_character;

   public ParameterFirearmRoomSize(IsoPlayer var1) {
      super("FirearmRoomSize");
      this.m_character = var1;
   }

   public float calculateCurrentValue() {
      Object var1 = this.getRoom(this.m_character);
      return var1 == null ? 0.0F : this.getRoomSize(var1);
   }

   private Object getRoom(IsoPlayer var1) {
      IsoGridSquare var2 = var1.getCurrentSquare();
      if (var2 == null) {
         return null;
      } else {
         IsoRoom var3 = var2.getRoom();
         if (var3 != null) {
            return var3;
         } else {
            IWorldRegion var4 = var2.getIsoWorldRegion();
            return var4 != null && var4.isPlayerRoom() ? var4 : null;
         }
      }
   }

   private float getRoomSize(Object var1) {
      return var1 instanceof IsoRoom ? (float)((IsoRoom)var1).getRoomDef().getArea() : (float)((IWorldRegion)var1).getSquareSize();
   }
}
