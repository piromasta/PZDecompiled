package zombie.commands.serverCommands;

import java.sql.SQLException;
import zombie.characters.Capability;
import zombie.characters.Role;
import zombie.commands.CommandArgs;
import zombie.commands.CommandBase;
import zombie.commands.CommandName;
import zombie.commands.RequiredCapability;
import zombie.core.raknet.UdpConnection;

@CommandName(
   name = "removeadmin"
)
@CommandArgs(
   required = {"(.+)"}
)
@RequiredCapability(
   requiredCapability = Capability.ChangeAccessLevel
)
public class RemoveAdminCommand extends CommandBase {
   public RemoveAdminCommand(String var1, Role var2, String var3, UdpConnection var4) {
      super(var1, var2, var3, var4);
   }

   protected String Command() throws SQLException {
      return SetAccessLevelCommand.update(this.getExecutorUsername(), this.connection, this.getCommandArg(0), "");
   }
}
