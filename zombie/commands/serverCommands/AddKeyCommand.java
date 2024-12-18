package zombie.commands.serverCommands;

import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.characters.Role;
import zombie.commands.AltCommandArgs;
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
   name = "addkey"
)
@AltCommandArgs({@CommandArgs(
   required = {"(.+)", "(\\d+)"},
   optional = "(.+)",
   argName = "add item to player"
), @CommandArgs(
   required = {"(\\d+)"},
   optional = "(.+)",
   argName = "add item to me"
)})
@CommandHelp(
   helpText = "UI_ServerOptionDesc_AddKey"
)
@RequiredCapability(
   requiredCapability = Capability.AddItem
)
public class AddKeyCommand extends CommandBase {
   public static final String toMe = "add item to me";
   public static final String toPlayer = "add item to player";

   public AddKeyCommand(String var1, Role var2, String var3, UdpConnection var4) {
      super(var1, var2, var3, var4);
   }

   protected String Command() {
      String var1 = "";
      if (this.argsName.equals("add item to me") && this.connection == null) {
         return "Pass username";
      } else {
         if (this.getCommandArgsCount() > 1) {
            int var2 = this.getCommandArgsCount();
            if (this.argsName.equals("add item to me") && var2 == 2 || this.argsName.equals("add item to player") && var2 == 3) {
               var1 = this.getCommandArg(this.getCommandArgsCount() - 1);
            }
         }

         IsoPlayer var3;
         String var7;
         if (this.argsName.equals("add item to player")) {
            var3 = GameServer.getPlayerByUserNameForCommand(this.getCommandArg(0));
            if (var3 == null) {
               return "No such user";
            }

            var7 = var3.getDisplayName();
         } else {
            var3 = GameServer.getPlayerByRealUserName(this.getExecutorUsername());
            if (var3 == null) {
               return "No such user";
            }

            var7 = var3.getDisplayName();
         }

         int var8;
         if (this.argsName.equals("add item to me")) {
            var8 = Integer.parseInt(this.getCommandArg(0));
         } else {
            var8 = Integer.parseInt(this.getCommandArg(1));
         }

         IsoPlayer var4 = GameServer.getPlayerByUserNameForCommand(var7);
         if (var4 != null) {
            var7 = var4.getDisplayName();
            UdpConnection var5 = GameServer.getConnectionByPlayerOnlineID(var4.OnlineID);
            if (var5 != null && !var4.isDead()) {
               InventoryItem var6 = var4.getInventory().AddItem("Base.Key1");
               var6.setKeyId(var8);
               if (!var1.isBlank()) {
                  var6.setName(var1);
               }

               INetworkPacket.send(var5, PacketTypes.PacketType.AddInventoryItemToContainer, var4.getInventory(), var6);
               LoggerManager.getLogger("admin").write(this.getExecutorUsername() + " added item " + var6 + " in " + var7 + "'s inventory");
               return "Key " + var6 + " Added in " + var7 + "'s inventory.";
            }
         }

         return "User " + var7 + " not found.";
      }
   }
}
