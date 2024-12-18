package zombie.commands.serverCommands;

import zombie.characters.Capability;
import zombie.characters.Role;
import zombie.commands.CommandArgs;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredCapability;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameServer;

@CommandName(
   name = "remove"
)
@CommandArgs(
   required = {"(.+)"}
)
@CommandHelp(
   helpText = "UI_ServerOptionDesc_Remove"
)
@RequiredCapability(
   requiredCapability = Capability.AnimalCheats
)
public class RemoveCommand extends CommandBase {
   public RemoveCommand(String var1, Role var2, String var3, UdpConnection var4) {
      super(var1, var2, var3, var4);
   }

   public static String Remove(UdpConnection var0, String var1) {
      String var2;
      if ("animals".equalsIgnoreCase(var1)) {
         GameServer.removeAnimalsConnection = var0;
         var2 = "Animals removed";
      } else {
         var2 = "Subsystem error: " + var1;
      }

      return var2;
   }

   protected String Command() {
      return Remove(this.connection, this.getCommandArg(0));
   }
}
