package zombie.commands.serverCommands;

import java.util.Iterator;
import zombie.Lua.LuaManager;
import zombie.characters.Capability;
import zombie.characters.Role;
import zombie.commands.CommandArgs;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredCapability;
import zombie.core.raknet.UdpConnection;

@CommandName(
   name = "reloadlua"
)
@CommandArgs(
   required = {"(\\S+)"}
)
@CommandHelp(
   helpText = "UI_ServerOptionDesc_ReloadLua"
)
@RequiredCapability(
   requiredCapability = Capability.ReloadLuaFiles
)
public class ReloadLuaCommand extends CommandBase {
   public ReloadLuaCommand(String var1, Role var2, String var3, UdpConnection var4) {
      super(var1, var2, var3, var4);
   }

   protected String Command() {
      String var1 = this.getCommandArg(0);
      Iterator var2 = LuaManager.loaded.iterator();

      String var3;
      do {
         if (!var2.hasNext()) {
            return "Unknown Lua file";
         }

         var3 = (String)var2.next();
      } while(!var3.endsWith(var1));

      LuaManager.loaded.remove(var3);
      LuaManager.RunLua(var3, true);
      return "Lua file reloaded";
   }
}
