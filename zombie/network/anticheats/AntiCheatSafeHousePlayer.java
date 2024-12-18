package zombie.network.anticheats;

import zombie.characters.Capability;
import zombie.core.raknet.UdpConnection;
import zombie.iso.areas.SafeHouse;
import zombie.network.packets.INetworkPacket;

public class AntiCheatSafeHousePlayer extends AbstractAntiCheat {
   public AntiCheatSafeHousePlayer() {
   }

   public String validate(UdpConnection var1, INetworkPacket var2) {
      String var3 = super.validate(var1, var2);
      IAntiCheat var4 = (IAntiCheat)var2;
      return !var1.role.haveCapability(Capability.CanSetupSafehouses) && !var1.havePlayer(var4.getSafehouse().getOwner()) && !var1.havePlayer(var4.getPlayer()) ? "player not found" : var3;
   }

   public interface IAntiCheat {
      String getPlayer();

      SafeHouse getSafehouse();
   }
}
