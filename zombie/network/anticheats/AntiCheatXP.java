package zombie.network.anticheats;

import zombie.SandboxOptions;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.raknet.UdpConnection;
import zombie.network.packets.INetworkPacket;

public class AntiCheatXP extends AbstractAntiCheat {
   private static final float MAX_XP_GROWTH_RATE = 0.0F;

   public AntiCheatXP() {
   }

   public String validate(UdpConnection var1, INetworkPacket var2) {
      String var3 = super.validate(var1, var2);
      IAntiCheat var4 = (IAntiCheat)var2;
      if (var1.role.haveCapability(Capability.AddXP)) {
         return var3;
      } else {
         double var5 = 0.0 * SandboxOptions.instance.multipliersConfig.XPMultiplierGlobal.getValue();
         return (double)var4.getAmount() > var5 ? String.format("xp=%f > max=%f", var4.getAmount(), var5) : var3;
      }
   }

   public interface IAntiCheat {
      IsoPlayer getPlayer();

      float getAmount();
   }
}
