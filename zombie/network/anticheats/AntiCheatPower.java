package zombie.network.anticheats;

import zombie.SystemDisabler;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.raknet.UdpConnection;
import zombie.network.packets.INetworkPacket;

public class AntiCheatPower extends AbstractAntiCheat {
   public AntiCheatPower() {
   }

   public String validate(UdpConnection var1, INetworkPacket var2) {
      String var3 = super.validate(var1, var2);
      IAntiCheat var4 = (IAntiCheat)var2;
      if (var1.role.haveCapability(Capability.ToggleGodModHimself)) {
         return var3;
      } else {
         int var5 = 4096;
         if (SystemDisabler.getKickInDebug()) {
            var5 |= 8192;
         }

         return var4.getPlayer().networkAI.doCheckAccessLevel() && (var4.getBooleanVariables() & var5) != 0 ? "invalid mode" : var3;
      }
   }

   public interface IAntiCheat {
      int getBooleanVariables();

      IsoPlayer getPlayer();
   }
}
