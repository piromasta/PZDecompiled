package zombie.network.anticheats;

import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.raknet.UdpConnection;
import zombie.network.ServerOptions;
import zombie.network.packets.INetworkPacket;

public class AntiCheatSafeHouseSurvivor extends AbstractAntiCheat {
   public AntiCheatSafeHouseSurvivor() {
   }

   public String validate(UdpConnection var1, INetworkPacket var2) {
      String var3 = super.validate(var1, var2);
      IAntiCheat var4 = (IAntiCheat)var2;
      if (!var1.role.haveCapability(Capability.CanSetupSafehouses)) {
         int var5 = ServerOptions.instance.SafehouseDaySurvivedToClaim.getValue();
         if (var5 > 0 && var4.getSurvivor().getHoursSurvived() < (double)(var5 * 24)) {
            return String.format("player \"%s\" not survived enough", var4.getSurvivor().getUsername());
         }
      }

      return var3;
   }

   public interface IAntiCheat {
      IsoPlayer getSurvivor();
   }
}
