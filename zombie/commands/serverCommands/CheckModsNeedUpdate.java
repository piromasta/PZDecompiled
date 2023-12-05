package zombie.commands.serverCommands;

import zombie.Lua.LuaManager;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredRight;
import zombie.core.raknet.UdpConnection;

@CommandName(
   name = "checkModsNeedUpdate"
)
@CommandHelp(
   helpText = "UI_ServerOptionDesc_CheckModsNeedUpdate"
)
@RequiredRight(
   requiredRights = 62
)
public class CheckModsNeedUpdate extends CommandBase {
   public CheckModsNeedUpdate(String var1, String var2, String var3, UdpConnection var4) {
      super(var1, var2, var3, var4);
   }

   protected String Command() {
      LuaManager.GlobalObject.checkModsNeedUpdate(this.connection);
      return "Checking started. The answer will be written in the log file and in the chat";
   }
}
