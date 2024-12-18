package zombie.commands.serverCommands;

import java.sql.SQLException;
import zombie.characters.Capability;
import zombie.characters.Role;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredCapability;
import zombie.core.raknet.UdpConnection;
import zombie.iso.areas.SafeHouse;

@CommandName(
   name = "releasesafehouse"
)
@CommandHelp(
   helpText = "UI_ServerOptionDesc_SafeHouse"
)
@RequiredCapability(
   requiredCapability = Capability.CanSetupSafehouses
)
public class ReleaseSafehouseCommand extends CommandBase {
   public ReleaseSafehouseCommand(String var1, Role var2, String var3, UdpConnection var4) {
      super(var1, var2, var3, var4);
   }

   protected String Command() throws SQLException {
      if (this.isCommandComeFromServerConsole()) {
         return getCommandName(this.getClass()) + " can be executed only from the game";
      } else {
         String var1 = this.getExecutorUsername();
         SafeHouse var2 = SafeHouse.hasSafehouse(var1);
         if (var2 != null) {
            if (!var2.isOwner(var1)) {
               return "Only owner can release safehouse";
            } else {
               SafeHouse.removeSafeHouse(var2);
               return "Your safehouse was released";
            }
         } else {
            return "You have no safehouse";
         }
      }
   }
}
