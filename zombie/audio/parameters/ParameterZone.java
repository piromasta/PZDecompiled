package zombie.audio.parameters;

import java.util.ArrayList;
import zombie.audio.FMODGlobalParameter;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;
import zombie.iso.IsoWorld;
import zombie.iso.zones.Zone;

public final class ParameterZone extends FMODGlobalParameter {
   private final String m_zoneName;
   private final ArrayList<Zone> m_zones = new ArrayList();

   public ParameterZone(String var1, String var2) {
      super(var1);
      this.m_zoneName = var2;
   }

   public float calculateCurrentValue() {
      IsoGameCharacter var1 = this.getCharacter();
      if (var1 == null) {
         return 40.0F;
      } else {
         byte var2 = 0;
         this.m_zones.clear();
         IsoWorld.instance.MetaGrid.getZonesIntersecting(PZMath.fastfloor(var1.getX()) - 40, PZMath.fastfloor(var1.getY()) - 40, var2, 80, 80, this.m_zones);
         float var3 = 3.4028235E38F;

         for(int var4 = 0; var4 < this.m_zones.size(); ++var4) {
            Zone var5 = (Zone)this.m_zones.get(var4);
            if (this.m_zoneName.equalsIgnoreCase(var5.getType())) {
               if (var5.contains(PZMath.fastfloor(var1.getX()), PZMath.fastfloor(var1.getY()), var2)) {
                  return 0.0F;
               }

               float var6 = (float)var5.x + (float)var5.w / 2.0F;
               float var7 = (float)var5.y + (float)var5.h / 2.0F;
               float var8 = PZMath.max(PZMath.abs(var1.getX() - var6) - (float)var5.w / 2.0F, 0.0F);
               float var9 = PZMath.max(PZMath.abs(var1.getY() - var7) - (float)var5.h / 2.0F, 0.0F);
               var3 = PZMath.min(var3, var8 * var8 + var9 * var9);
            }
         }

         return (float)((int)PZMath.clamp(PZMath.sqrt(var3), 0.0F, 40.0F));
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
}
