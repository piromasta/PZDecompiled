package zombie.commands.serverCommands;

import java.util.ArrayList;
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
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Item;

@CommandName(
   name = "additem"
)
@AltCommandArgs({@CommandArgs(
   required = {"(.+)", "([a-zA-Z0-9.-]*[a-zA-Z][a-zA-Z0-9_.-]*)"},
   optional = "(\\d+)",
   argName = "add item to player"
), @CommandArgs(
   required = {"([a-zA-Z0-9.-]*[a-zA-Z][a-zA-Z0-9_.-]*)"},
   optional = "(\\d+)",
   argName = "add item to me"
)})
@CommandHelp(
   helpText = "UI_ServerOptionDesc_AddItem"
)
@RequiredCapability(
   requiredCapability = Capability.AddItem
)
public class AddItemCommand extends CommandBase {
   public static final String toMe = "add item to me";
   public static final String toPlayer = "add item to player";

   public AddItemCommand(String var1, Role var2, String var3, UdpConnection var4) {
      super(var1, var2, var3, var4);
   }

   protected String Command() {
      int var1 = 1;
      if (this.argsName.equals("add item to me") && this.connection == null) {
         return "Pass username";
      } else {
         if (this.getCommandArgsCount() > 1) {
            int var2 = this.getCommandArgsCount();
            if (this.argsName.equals("add item to me") && var2 == 2 || this.argsName.equals("add item to player") && var2 == 3) {
               var1 = Integer.parseInt(this.getCommandArg(this.getCommandArgsCount() - 1));
            }
         }

         if (var1 > 100) {
            System.out.println("Cannot spawn over 100 items at a time");
            var1 = 100;
         }

         IsoPlayer var3;
         String var8;
         if (this.argsName.equals("add item to player")) {
            var3 = GameServer.getPlayerByUserNameForCommand(this.getCommandArg(0));
            if (var3 == null) {
               return "No such user";
            }

            var8 = var3.getDisplayName();
         } else {
            var3 = GameServer.getPlayerByRealUserName(this.getExecutorUsername());
            if (var3 == null) {
               return "No such user";
            }

            var8 = var3.getDisplayName();
         }

         String var9;
         if (this.argsName.equals("add item to me")) {
            var9 = this.getCommandArg(0);
         } else {
            var9 = this.getCommandArg(1);
         }

         Item var4 = ScriptManager.instance.FindItem(var9);
         if (var4 == null) {
            return "Item " + var9 + " doesn't exist.";
         } else {
            IsoPlayer var5 = GameServer.getPlayerByUserNameForCommand(var8);
            if (var5 != null) {
               var8 = var5.getDisplayName();
               UdpConnection var6 = GameServer.getConnectionByPlayerOnlineID(var5.OnlineID);
               if (var6 != null && !var5.isDead()) {
                  ArrayList var7 = var5.getInventory().AddItems(var9, var1);
                  INetworkPacket.send(var6, PacketTypes.PacketType.AddItemInInventory, var5, var7);
                  LoggerManager.getLogger("admin").write(this.getExecutorUsername() + " added item " + var9 + " in " + var8 + "'s inventory");
                  return "Item " + var9 + " Added in " + var8 + "'s inventory.";
               }
            }

            return "User " + var8 + " not found.";
         }
      }
   }
}
