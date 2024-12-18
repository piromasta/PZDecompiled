package zombie.network.anticheats;

import zombie.characters.IsoPlayer;
import zombie.core.raknet.UdpConnection;
import zombie.network.packets.INetworkPacket;

public class AntiCheatRole extends AbstractAntiCheat {
   public AntiCheatRole() {
   }

   public String validate(UdpConnection var1, INetworkPacket var2) {
      String var3 = super.validate(var1, var2);
      IAntiCheat var4 = (IAntiCheat)var2;
      return var4.getPlayer().networkAI.doCheckAccessLevel() && var4.getRoleID() != var4.getPlayer().getRole().getName().hashCode() ? "invalid role" : var3;
   }

   public interface IAntiCheat {
      int getRoleID();

      IsoPlayer getPlayer();
   }
}
