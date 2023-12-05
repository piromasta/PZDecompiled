package zombie.commands.serverCommands;

import java.util.ArrayList;
import zombie.commands.CommandArgs;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredRight;
import zombie.core.Translator;
import zombie.core.raknet.UdpConnection;
import zombie.core.znet.ZNet;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;

@CommandName(
   name = "log"
)
@CommandArgs(
   required = {"(.+)", "(.+)"}
)
@CommandHelp(
   helpText = "UI_ServerOptionDesc_SetLogLevel"
)
@RequiredRight(
   requiredRights = 32
)
public class LogCommand extends CommandBase {
   public LogCommand(String var1, String var2, String var3, UdpConnection var4) {
      super(var1, var2, var3, var4);
   }

   public static DebugType getDebugType(String var0) {
      ArrayList var1 = new ArrayList();
      DebugType[] var2 = DebugType.values();
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         DebugType var5 = var2[var4];
         if (var5.name().toLowerCase().startsWith(var0.toLowerCase())) {
            var1.add(var5);
         }
      }

      return var1.size() == 1 ? (DebugType)var1.get(0) : null;
   }

   public static LogSeverity getLogSeverity(String var0) {
      ArrayList var1 = new ArrayList();
      LogSeverity[] var2 = LogSeverity.values();
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         LogSeverity var5 = var2[var4];
         if (var5.name().toLowerCase().startsWith(var0.toLowerCase())) {
            var1.add(var5);
         }
      }

      return var1.size() == 1 ? (LogSeverity)var1.get(0) : null;
   }

   protected String Command() {
      DebugType var1 = getDebugType(this.getCommandArg(0));
      LogSeverity var2 = getLogSeverity(this.getCommandArg(1));
      if (var1 != null && var2 != null) {
         DebugLog.enableLog(var1, var2);
         if (DebugType.Network.equals(var1)) {
            ZNet.SetLogLevel(var2);
         }

         return String.format("Server \"%s\" log level is \"%s\"", var1.name().toLowerCase(), var2.name().toLowerCase());
      } else {
         return Translator.getText("UI_ServerOptionDesc_SetLogLevel", var1 == null ? "\"type\"" : var1.name().toLowerCase(), var2 == null ? "\"severity\"" : var2.name().toLowerCase());
      }
   }
}
