package zombie.network.anticheats;

import zombie.core.raknet.UdpConnection;
import zombie.inventory.InventoryItem;
import zombie.network.packets.INetworkPacket;

public class AntiCheatItem extends AbstractAntiCheat {
   public AntiCheatItem() {
   }

   public String validate(UdpConnection var1, INetworkPacket var2) {
      String var3 = super.validate(var1, var2);
      IAntiCheat var4 = (IAntiCheat)var2;
      return var4.getInventoryItem() == null ? "item not found" : var3;
   }

   public interface IAntiCheat {
      InventoryItem getInventoryItem();
   }
}
