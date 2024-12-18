package zombie.network.anticheats;

import zombie.characters.Faction;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.SafetySystemManager;
import zombie.core.math.PZMath;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoMovingObject;
import zombie.iso.areas.NonPvpZone;
import zombie.network.ServerOptions;
import zombie.network.packets.INetworkPacket;
import zombie.util.Type;

public class AntiCheatSafety extends AbstractAntiCheat {
   public AntiCheatSafety() {
   }

   public String validate(UdpConnection var1, INetworkPacket var2) {
      String var3 = super.validate(var1, var2);
      IAntiCheat var4 = (IAntiCheat)var2;
      boolean var5 = SafetySystemManager.checkUpdateDelay(var4.getWielder(), var4.getTarget());
      return var5 ? var3 : checkPVP(var4.getWielder(), var4.getTarget());
   }

   private static String checkPVP(IsoGameCharacter var0, IsoMovingObject var1) {
      IsoPlayer var2 = (IsoPlayer)Type.tryCastTo(var0, IsoPlayer.class);
      IsoPlayer var3 = (IsoPlayer)Type.tryCastTo(var1, IsoPlayer.class);
      if (var2 == null) {
         return "wielder not found";
      } else if (var3 == null) {
         return "target not found";
      } else if (var3.isGodMod()) {
         return "target is in god-mode";
      } else if (!ServerOptions.instance.PVP.getValue()) {
         return "PVP is disabled";
      } else if (ServerOptions.instance.SafetySystem.getValue() && var0.getSafety().isEnabled() && var3.getSafety().isEnabled()) {
         return "safety is enabled";
      } else if (NonPvpZone.getNonPvpZone(PZMath.fastfloor(var2.getX()), PZMath.fastfloor(var2.getY())) != null) {
         long var4 = SafetySystemManager.getSafetyTimestamp(var2.getUsername());
         return System.currentTimeMillis() - var4 < SafetySystemManager.getSafetyDelay() ? "" : "wiedler is in non-pvp zone";
      } else if (NonPvpZone.getNonPvpZone(PZMath.fastfloor(var3.getX()), PZMath.fastfloor(var3.getY())) != null) {
         return "target is in non-pvp zone";
      } else {
         return !var2.isFactionPvp() && !var3.isFactionPvp() && Faction.isInSameFaction(var2, var3) ? "faction pvp is disabled" : null;
      }
   }

   public interface IAntiCheat {
      IsoGameCharacter getTarget();

      IsoPlayer getWielder();
   }
}
