package zombie.commands.serverCommands;

import java.util.ArrayList;
import java.util.Iterator;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.characters.Role;
import zombie.commands.CommandArgs;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredCapability;
import zombie.core.logger.LoggerManager;
import zombie.core.raknet.UdpConnection;
import zombie.inventory.InventoryItem;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;

@CommandName(
   name = "removeitem"
)
@CommandArgs(
   required = {"([a-zA-Z0-9.-]*[a-zA-Z][a-zA-Z0-9_.-]*)", "(\\d+)"}
)
@CommandHelp(
   helpText = "UI_ServerOptionDesc_RemoveItem"
)
@RequiredCapability(
   requiredCapability = Capability.EditItem
)
public class RemoveItemCommand extends CommandBase {
   public RemoveItemCommand(String var1, Role var2, String var3, UdpConnection var4) {
      super(var1, var2, var3, var4);
   }

   protected String Command() {
      String var1 = this.getCommandArg(0);
      int var2 = Integer.parseInt(this.getCommandArg(1));
      IsoPlayer var3 = GameServer.getPlayerByRealUserName(this.getExecutorUsername());
      if (var3 != null) {
         UdpConnection var4 = GameServer.getConnectionByPlayerOnlineID(var3.OnlineID);
         if (var4 != null && !var3.isDead()) {
            ArrayList var5 = new ArrayList();
            if (var2 == 0) {
               var5.addAll(var3.getInventory().RemoveAll(var1));
            } else {
               for(int var6 = 0; var6 < var2; ++var6) {
                  var5.add(var3.getInventory().RemoveOneOf(var1, true));
               }
            }

            var2 = var5.size();
            Iterator var8 = var5.iterator();

            while(var8.hasNext()) {
               InventoryItem var7 = (InventoryItem)var8.next();
               INetworkPacket.send(var4, PacketTypes.PacketType.RemoveInventoryItemFromContainer, var3.getInventory(), var7);
            }

            String var9 = String.format("%s removed %d items %s from inventory", this.getExecutorUsername(), var2, var1);
            LoggerManager.getLogger("admin").write(var9);
            return var9;
         }
      }

      return "User " + this.getExecutorUsername() + " not found.";
   }
}
