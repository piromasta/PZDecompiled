package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoUtils;
import zombie.iso.areas.IsoRoom;
import zombie.iso.areas.isoregion.regions.IWorldRegion;

public final class ParameterFirearmInside extends FMODLocalParameter {
   private static final float VALUE_DIFFERENT_BUILDING = -1.0F;
   private static final float VALUE_OUTSIDE = 0.0F;
   private static final float VALUE_SAME_BUILDING = 1.0F;
   private final IsoPlayer m_character;

   public ParameterFirearmInside(IsoPlayer var1) {
      super("FirearmInside");
      this.m_character = var1;
   }

   public float calculateCurrentValue() {
      Object var1 = this.getRoom(this.m_character);
      if (var1 == null) {
         return 0.0F;
      } else {
         IsoPlayer var2 = this.getClosestListener(this.m_character.getX(), this.m_character.getY(), this.m_character.getZ());
         if (var2 == null) {
            return -1.0F;
         } else if (var2 == this.m_character) {
            return 1.0F;
         } else {
            Object var3 = this.getRoom(var2);
            return var1 == var3 ? 1.0F : -1.0F;
         }
      }
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

   private IsoPlayer getClosestListener(float var1, float var2, float var3) {
      if (IsoPlayer.numPlayers == 1) {
         return IsoPlayer.players[0];
      } else {
         float var4 = 3.4028235E38F;
         IsoPlayer var5 = null;

         for(int var6 = 0; var6 < IsoPlayer.numPlayers; ++var6) {
            IsoPlayer var7 = IsoPlayer.players[var6];
            if (var7 != null) {
               if (var7 == this.m_character) {
                  return this.m_character;
               }

               if (var7.getCurrentSquare() != null) {
                  float var8 = var7.getX();
                  float var9 = var7.getY();
                  float var10 = var7.getZ();
                  float var11 = IsoUtils.DistanceToSquared(var8, var9, var10 * 3.0F, var1, var2, var3 * 3.0F);
                  var11 *= PZMath.pow(var7.getHearDistanceModifier(), 2.0F);
                  if (var11 < var4) {
                     var4 = var11;
                     var5 = var7;
                  }
               }
            }
         }

         return var5;
      }
   }
}
