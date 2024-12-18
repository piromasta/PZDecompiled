package zombie.debug;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import zombie.GameTime;
import zombie.ZomboidFileSystem;
import zombie.config.ConfigFile;
import zombie.config.ConfigOption;
import zombie.config.StringConfigOption;
import zombie.core.Core;
import zombie.core.logger.LoggerManager;
import zombie.core.logger.ZLogger;
import zombie.iso.IsoWorld;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.scripting.ScriptManager;
import zombie.scripting.ScriptType;
import zombie.ui.UIDebugConsole;
import zombie.util.StringUtils;

public final class DebugLog {
   private static final DebugLogStream[] s_streams = new DebugLogStream[DebugType.values().length];
   private static final LogSeverity s_minimumLogSeverity;
   private static final LogSeverity s_defaultLogSeverity;
   private static boolean s_initialized;
   public static boolean printServerTime;
   private static final OutputStreamWrapper s_stdout;
   private static final OutputStreamWrapper s_stderr;
   private static final PrintStream s_originalOut;
   private static final PrintStream s_originalErr;
   private static final PrintStream GeneralErr;
   private static ZLogger s_logFileLogger;
   public static final DebugLogStream ActionSystem;
   public static final DebugLogStream Animation;
   public static final DebugLogStream AnimationDetailed;
   public static final DebugLogStream Asset;
   public static final DebugLogStream Clothing;
   public static final DebugLogStream Combat;
   public static final DebugLogStream Damage;
   public static final DebugLogStream Death;
   public static final DebugLogStream Discord;
   public static final DebugLogStream Entity;
   public static final DebugLogStream FileIO;
   public static final DebugLogStream Fireplace;
   public static final DebugLogStream General;
   public static final DebugLogStream DetailedInfo;
   public static final DebugLogStream Input;
   public static final DebugLogStream IsoRegion;
   public static final DebugLogStream Lua;
   public static final DebugLogStream MapLoading;
   public static final DebugLogStream Mod;
   public static final DebugLogStream Multiplayer;
   public static final DebugLogStream Network;
   public static final DebugLogStream NetworkFileDebug;
   public static final DebugLogStream Packet;
   public static final DebugLogStream Objects;
   public static final DebugLogStream Radio;
   public static final DebugLogStream Recipe;
   public static final DebugLogStream Script;
   public static final DebugLogStream Shader;
   public static final DebugLogStream Sound;
   public static final DebugLogStream Sprite;
   public static final DebugLogStream Statistic;
   public static final DebugLogStream UnitTests;
   public static final DebugLogStream Vehicle;
   public static final DebugLogStream Voice;
   public static final DebugLogStream Zombie;
   public static final DebugLogStream Animal;
   public static final DebugLogStream ItemPicker;
   public static final DebugLogStream CraftLogic;
   public static final DebugLogStream Action;
   public static final DebugLogStream Physics;
   public static final DebugLogStream Zone;
   public static final DebugLogStream WorldGen;
   public static final DebugLogStream Lightning;
   public static final DebugLogStream Grapple;
   public static final DebugLogStream Foraging;
   public static final DebugLogStream Saving;
   public static final DebugLogStream Energy;
   public static final DebugLogStream Fluid;
   public static final DebugLogStream Translation;
   public static final DebugLogStream Moveable;
   public static final DebugLogStream Basement;
   public static final DebugLogStream Xml;
   private static boolean s_logTraceFileLocationEnabled;
   public static final int VERSION1 = 1;
   public static final int VERSION2 = 2;
   public static final int VERSION = 4;

   public DebugLog() {
   }

   public static DebugType getAnimalLog() {
      return DebugType.Animal;
   }

   public static void enableServerLogs() {
      enableLog(DebugType.Action, LogSeverity.Warning);
      enableLog(DebugType.Death, LogSeverity.Warning);
      enableLog(DebugType.Discord, LogSeverity.General);
      enableLog(DebugType.General, LogSeverity.General);
      enableLog(DebugType.Lua, LogSeverity.Warning);
      enableLog(DebugType.Multiplayer, LogSeverity.Warning);
      enableLog(DebugType.Network, LogSeverity.Warning);
      enableLog(DebugType.Objects, LogSeverity.Warning);
      enableLog(DebugType.Vehicle, LogSeverity.Warning);
   }

   public static void enableDebugLogs() {
      if (!Core.bDebug && System.getProperty("debug") == null) {
         enableLog(DebugType.General, LogSeverity.General);
      } else {
         enableLog(DebugType.General, LogSeverity.Trace);
      }

   }

   private static DebugLogStream createDebugLogStream(DebugType var0, LogSeverity var1) {
      DebugLogStream var2 = new DebugLogStream(s_originalOut, s_originalOut, s_originalErr, new GenericDebugLogFormatter(var0));
      if (s_streams[var0.ordinal()] != null) {
         throw new IllegalArgumentException(String.format("DebugType.%s is already registered. Existing logger: %s", var0.name(), s_streams[var0.ordinal()].toString()));
      } else {
         s_streams[var0.ordinal()] = var2;
         var2.setLogSeverity(var1);
         return var2;
      }
   }

   private static DebugLogStream createDebugLogStream(DebugType var0) {
      return createDebugLogStream(var0, LogSeverity.Off);
   }

   public static DebugLogStream getOrCreateDebugLogStream(DebugType var0) {
      return s_streams[var0.ordinal()] != null ? s_streams[var0.ordinal()] : createDebugLogStream(var0);
   }

   public static void printLogLevels() {
      if (!GameServer.bServer) {
         DetailedInfo.trace("You can setup the log levels in the " + getConfigFileName() + " file");
      }

      StringBuilder var0 = new StringBuilder();
      DebugType[] var1 = DebugType.values();
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         DebugType var4 = var1[var3];
         if (var4 != null) {
            if (isEnabled(var4)) {
               General.println("LogType %12s (severity:%8s)", var4.name(), s_streams[var4.ordinal()] == null ? "???" : s_streams[var4.ordinal()].getLogSeverity().name());
            } else {
               if (!var0.isEmpty()) {
                  var0.append(", ");
               }

               var0.append(var4.name());
            }
         }
      }

      if (!var0.isEmpty()) {
         General.println("The following LogTypes are disabled: " + var0);
      }

   }

   public static void enableLog(DebugType var0, LogSeverity var1) {
      setLogSeverity(var0, var1);
   }

   public static LogSeverity getLogLevel(DebugType var0) {
      return getLogSeverity(var0);
   }

   public static LogSeverity getLogSeverity(DebugType var0) {
      return getOrCreateDebugLogStream(var0).getLogSeverity();
   }

   public static void setLogSeverity(DebugType var0, LogSeverity var1) {
      getOrCreateDebugLogStream(var0).setLogSeverity(var1);
   }

   public static boolean isEnabled(DebugType var0) {
      return getOrCreateDebugLogStream(var0).isEnabled();
   }

   public static boolean isLogEnabled(DebugType var0, LogSeverity var1) {
      return getOrCreateDebugLogStream(var0).isLogEnabled(var1);
   }

   public static String formatString(DebugType var0, LogSeverity var1, Object var2, boolean var3, String var4) {
      return isLogEnabled(var0, var1) ? formatStringVarArgs(var0, var1, var2, var3, "%s", var4) : null;
   }

   public static String formatString(DebugType var0, LogSeverity var1, Object var2, boolean var3, String var4, Object... var5) {
      return isLogEnabled(var0, var1) ? formatStringVarArgs(var0, var1, var2, var3, var4, var5) : null;
   }

   public static String formatStringVarArgs(DebugType var0, LogSeverity var1, Object var2, boolean var3, String var4, Object... var5) {
      if (!isLogEnabled(var0, var1)) {
         return null;
      } else {
         String var6 = generateCurrentTimeMillisStr();
         int var7 = IsoWorld.instance.getFrameNo();
         String var8 = StringUtils.leftJustify(var0.toString(), 12);
         String var9 = String.format(var4, var5);
         String var10 = "" + var2 + var9;
         if (!DebugLog.RepeatWatcher.check(var0, var1, var10, var3)) {
            return null;
         } else {
            String var11 = var1.LogPrefix + var8 + " f:" + var7 + ", t:" + var6 + "> " + var10;
            echoToLogFile(var11);
            return var11;
         }
      }
   }

   private static String generateCurrentTimeMillisStr() {
      String var0 = String.valueOf(System.currentTimeMillis());
      if (GameServer.bServer || GameClient.bClient || printServerTime) {
         var0 = var0 + ", st:" + NumberFormat.getNumberInstance().format(TimeUnit.NANOSECONDS.toMillis(GameTime.getServerTime()));
      }

      return var0;
   }

   private static void echoToLogFile(String var0) {
      if (s_logFileLogger == null) {
         if (s_initialized) {
            return;
         }

         s_logFileLogger = new ZLogger(GameServer.bServer ? "DebugLog-server" : "DebugLog", false);
      }

      try {
         s_logFileLogger.writeUnsafe(var0, (String)null, false);
      } catch (Exception var2) {
         s_originalErr.println("Exception thrown writing to log file.");
         s_originalErr.println(var2);
         var2.printStackTrace(s_originalErr);
      }

   }

   public static void log(DebugType var0, String var1) {
      var0.println(var1);
   }

   public static void setLogEnabled(DebugType var0, boolean var1) {
      DebugLogStream var2 = getOrCreateDebugLogStream(var0);
      if (var2.isEnabled() != var1) {
         var2.setLogSeverity(var1 ? s_defaultLogSeverity : LogSeverity.Off);
      }

   }

   public static void log(String var0) {
      log(DebugType.General, var0);
   }

   public static ArrayList<DebugType> getDebugTypes() {
      ArrayList var0 = new ArrayList(Arrays.asList(DebugType.values()));
      var0.sort((var0x, var1) -> {
         return String.CASE_INSENSITIVE_ORDER.compare(var0x.name(), var1.name());
      });
      return var0;
   }

   public static void save() {
      LogSeverity[] var0 = LogSeverity.values();
      String[] var1 = new String[var0.length];

      for(int var2 = 0; var2 < var0.length; ++var2) {
         var1[var2] = var0[var2].name();
      }

      ArrayList var8 = new ArrayList();
      DebugType[] var3 = DebugType.values();
      int var4 = var3.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         DebugType var6 = var3[var5];
         StringConfigOption var7 = new StringConfigOption(var6.name(), LogSeverity.Off.name(), var1);
         var7.setValue(getLogSeverity(var6).name());
         var8.add(var7);
      }

      String var10000 = ZomboidFileSystem.instance.getCacheDir();
      String var9 = var10000 + File.separator + "debuglog.ini";
      ConfigFile var10 = new ConfigFile();
      var10.write(var9, 4, var8);
   }

   private static String getConfigFileName() {
      String var10000 = ZomboidFileSystem.instance.getCacheDir();
      return var10000 + File.separator + "debuglog.ini";
   }

   private static void setDefaultLogSeverity() {
      enableLog(DebugType.Network, LogSeverity.General);
      enableLog(DebugType.General, LogSeverity.General);
      enableLog(DebugType.DetailedInfo, LogSeverity.Debug);
      enableLog(DebugType.Lua, LogSeverity.General);
      enableLog(DebugType.Mod, LogSeverity.Debug);
      enableLog(DebugType.Combat, LogSeverity.Debug);
      enableLog(DebugType.Objects, LogSeverity.Debug);
      enableLog(DebugType.MapLoading, LogSeverity.Debug);
      enableLog(DebugType.Asset, LogSeverity.Debug);
      enableLog(DebugType.Script, LogSeverity.Debug);
      enableLog(DebugType.Multiplayer, LogSeverity.Debug);
      enableLog(DebugType.Death, LogSeverity.Debug);
      enableLog(DebugType.Animal, LogSeverity.Debug);
      enableLog(DebugType.ItemPicker, LogSeverity.Debug);
      enableLog(DebugType.Action, LogSeverity.Debug);
      enableLog(DebugType.ExitDebug, LogSeverity.Debug);
   }

   public static void load() {
      String var0 = getConfigFileName();
      ConfigFile var1 = new ConfigFile();
      File var2 = new File(var0);
      if (!var2.exists()) {
         setDefaultLogSeverity();
         save();
      }

      if (var1.read(var0)) {
         if (var1.getVersion() != 4) {
            setDefaultLogSeverity();
            save();
         } else {
            for(int var3 = 0; var3 < var1.getOptions().size(); ++var3) {
               ConfigOption var4 = (ConfigOption)var1.getOptions().get(var3);

               try {
                  DebugType var5 = DebugType.valueOf(var4.getName());
                  if (var1.getVersion() == 1) {
                     setLogEnabled(var5, StringUtils.tryParseBoolean(var4.getValueAsString()));
                  } else {
                     LogSeverity var6 = LogSeverity.valueOf(var4.getValueAsString());
                     setLogSeverity(var5, var6);
                  }
               } catch (Exception var7) {
               }
            }
         }
      }

   }

   public static LogSeverity getMinimumLogSeverity() {
      return s_minimumLogSeverity;
   }

   public static boolean isLogTraceFileLocationEnabled() {
      return s_logTraceFileLocationEnabled;
   }

   public static void setStdOut(OutputStream var0) {
      s_stdout.setStream(var0);
   }

   public static void setStdErr(OutputStream var0) {
      s_stderr.setStream(var0);
   }

   public static void init() {
      if (!s_initialized) {
         s_initialized = true;
         setStdOut(System.out);
         setStdErr(System.err);
         System.setOut(General);
         System.setErr(GeneralErr);
         if (!GameServer.bServer) {
            load();
         }

         s_logFileLogger = LoggerManager.getLogger(GameServer.bServer ? "DebugLog-server" : "DebugLog");
      }
   }

   public static void loadDebugConfig(String var0) {
      if (!GameServer.bServer) {
         try {
            File var1;
            if (var0 == null) {
               String var10000 = ZomboidFileSystem.instance.getCacheDir();
               var0 = var10000 + File.separator + "debuglog.cfg";
               var1 = new File(var0);
               if (!var1.exists() || !var1.isFile()) {
                  return;
               }
            }

            log("Attempting to read debug config...");
            var1 = new File(var0);
            if (!var1.exists() || !var1.isFile()) {
               log("Attempting relative path...");
               File var2 = new File("");
               Path var3 = Path.of(var2.toURI()).getParent();
               var1 = new File("" + var3 + File.separator + var0);
            }

            DetailedInfo.trace("file = " + var1.getAbsolutePath());
            if (!var1.exists() || !var1.isFile()) {
               log("Could not find debug config.");
               return;
            }

            String var19 = null;
            HashMap var20 = new HashMap();
            HashMap var4 = new HashMap();
            ArrayList var5 = null;
            boolean var6 = false;
            BufferedReader var7 = new BufferedReader(new FileReader(var1));

            String var11;
            String var14;
            try {
               String var8 = null;
               String var9 = null;

               label123:
               while(true) {
                  while(true) {
                     do {
                        do {
                           do {
                              String var10;
                              if ((var10 = var7.readLine()) == null) {
                                 break label123;
                              }

                              var8 = var9;
                              var9 = var10.trim();
                           } while(var9.startsWith("//"));
                        } while(var9.startsWith("#"));
                     } while(StringUtils.isNullOrWhitespace(var9));

                     if (var9.startsWith("=")) {
                        var19 = var9.substring(1).trim();
                     } else if (var9.startsWith("$")) {
                        try {
                           var11 = var9.substring(1).trim();
                           int var12 = var11.indexOf(61);
                           String var13 = var11.substring(0, var12).trim();
                           var14 = var11.substring(var12 + 1).trim();
                           var4.put(var13, var14);
                        } catch (Exception var16) {
                           var16.printStackTrace();
                        }
                     } else if (!var6 && var9.startsWith("{") && var8 != null) {
                        var6 = true;
                        var5 = new ArrayList();
                        var20.put(var8, var5);
                     } else if (var6) {
                        if (var9.startsWith("}")) {
                           var6 = false;
                        } else {
                           var5.add(var9);
                        }
                     }
                  }
               }
            } catch (Throwable var17) {
               try {
                  var7.close();
               } catch (Throwable var15) {
                  var17.addSuppressed(var15);
               }

               throw var17;
            }

            var7.close();
            if (var19 != null) {
               if (var19.startsWith("$")) {
                  log("Selected debug alias = '" + var19 + "'");
                  var19 = (String)var4.get(var19.substring(1).trim());
               } else {
                  log("Selected debug profile = '" + var19 + "'");
               }

               String[] var21 = var19.split("\\+");
               String[] var22 = var21;
               int var23 = var21.length;

               for(int var24 = 0; var24 < var23; ++var24) {
                  var11 = var22[var24];
                  String var25 = var11.trim();
                  if (var20.containsKey(var25)) {
                     log("Debug.cfg loading profile '" + var25 + "'");
                     Iterator var26 = ((ArrayList)var20.get(var25)).iterator();

                     while(var26.hasNext()) {
                        var14 = (String)var26.next();
                        if (var14.startsWith("+")) {
                           readConfigCommand(var14.substring(1), true);
                        } else if (var14.startsWith("-")) {
                           readConfigCommand(var14.substring(1), false);
                        } else {
                           log("unknown command: '" + var14 + "'");
                        }
                     }
                  } else {
                     log("Debug.cfg profile note found: '" + var25 + "'");
                  }
               }
            }
         } catch (Exception var18) {
            var18.printStackTrace();
         }

      }
   }

   private static void readConfigCommand(String var0, boolean var1) {
      try {
         String var2 = var0;
         String var3 = null;
         if (StringUtils.containsWhitespace(var0)) {
            String[] var4 = var0.split("\\s+");
            var2 = var4[0].trim();
            var3 = var4[1].trim();
         }

         LogSeverity var10 = LogSeverity.Debug;
         if (!StringUtils.isNullOrWhitespace(var3)) {
            var10 = LogSeverity.valueOf(var3);
         }

         if (var2.equalsIgnoreCase("LogTraceFileLocation")) {
            s_logTraceFileLocationEnabled = var1;
            return;
         }

         if (var2.equalsIgnoreCase("all")) {
            DebugType[] var11 = DebugType.values();
            int var12 = var11.length;

            for(int var13 = 0; var13 < var12; ++var13) {
               DebugType var8 = var11[var13];
               if (var8 != DebugType.General || var1) {
                  setLogSeverity(var8, var10);
                  setLogEnabled(var8, var1);
               }
            }

            return;
         }

         DebugType var5;
         if (var2.contains(".")) {
            String[] var6 = var2.split("\\.");
            var5 = DebugType.valueOf(var6[0]);
            ScriptType var7 = ScriptType.valueOf(var6[1]);
            ScriptManager.EnableDebug(var7, var1);
         } else {
            var5 = DebugType.valueOf(var2);
         }

         setLogSeverity(var5, var10);
         setLogEnabled(var5, var1);
      } catch (Exception var9) {
         General.printException(var9, "Exception thrown in readConfigCommand", LogSeverity.Error);
      }

   }

   public static void nativeLog(String var0, String var1, String var2) {
      DebugType var3 = (DebugType)StringUtils.tryParseEnum(DebugType.class, var0, DebugType.General);
      LogSeverity var4 = (LogSeverity)StringUtils.tryParseEnum(LogSeverity.class, var1, LogSeverity.General);
      var3.routedWrite(1, var4, var2);
   }

   static {
      s_minimumLogSeverity = LogSeverity.Warning;
      s_defaultLogSeverity = LogSeverity.General;
      s_initialized = false;
      printServerTime = false;
      s_stdout = new OutputStreamWrapper(System.out);
      s_stderr = new OutputStreamWrapper(System.err);
      s_originalOut = new PrintStream(s_stdout, true);
      s_originalErr = new PrintStream(s_stderr, true);
      GeneralErr = new DebugLogStream(s_originalErr, s_originalErr, s_originalErr, new GeneralErrorDebugLogFormatter(), LogSeverity.All);
      ActionSystem = createDebugLogStream(DebugType.ActionSystem);
      Animation = createDebugLogStream(DebugType.Animation);
      AnimationDetailed = createDebugLogStream(DebugType.AnimationDetailed);
      Asset = createDebugLogStream(DebugType.Asset);
      Clothing = createDebugLogStream(DebugType.Clothing);
      Combat = createDebugLogStream(DebugType.Combat);
      Damage = createDebugLogStream(DebugType.Damage);
      Death = createDebugLogStream(DebugType.Death);
      Discord = createDebugLogStream(DebugType.Discord);
      Entity = createDebugLogStream(DebugType.Entity);
      FileIO = createDebugLogStream(DebugType.FileIO);
      Fireplace = createDebugLogStream(DebugType.Fireplace);
      General = createDebugLogStream(DebugType.General, s_defaultLogSeverity);
      DetailedInfo = createDebugLogStream(DebugType.DetailedInfo);
      Input = createDebugLogStream(DebugType.Input);
      IsoRegion = createDebugLogStream(DebugType.IsoRegion);
      Lua = createDebugLogStream(DebugType.Lua);
      MapLoading = createDebugLogStream(DebugType.MapLoading);
      Mod = createDebugLogStream(DebugType.Mod);
      Multiplayer = createDebugLogStream(DebugType.Multiplayer);
      Network = createDebugLogStream(DebugType.Network);
      NetworkFileDebug = createDebugLogStream(DebugType.NetworkFileDebug);
      Packet = createDebugLogStream(DebugType.Packet);
      Objects = createDebugLogStream(DebugType.Objects);
      Radio = createDebugLogStream(DebugType.Radio);
      Recipe = createDebugLogStream(DebugType.Recipe);
      Script = createDebugLogStream(DebugType.Script);
      Shader = createDebugLogStream(DebugType.Shader);
      Sound = createDebugLogStream(DebugType.Sound);
      Sprite = createDebugLogStream(DebugType.Sprite);
      Statistic = createDebugLogStream(DebugType.Statistic);
      UnitTests = createDebugLogStream(DebugType.UnitTests);
      Vehicle = createDebugLogStream(DebugType.Vehicle);
      Voice = createDebugLogStream(DebugType.Voice);
      Zombie = createDebugLogStream(DebugType.Zombie);
      Animal = createDebugLogStream(DebugType.Animal);
      ItemPicker = createDebugLogStream(DebugType.ItemPicker);
      CraftLogic = createDebugLogStream(DebugType.CraftLogic);
      Action = createDebugLogStream(DebugType.Action);
      Physics = createDebugLogStream(DebugType.Physics);
      Zone = createDebugLogStream(DebugType.Zone);
      WorldGen = createDebugLogStream(DebugType.WorldGen);
      Lightning = createDebugLogStream(DebugType.Lightning);
      Grapple = createDebugLogStream(DebugType.Grapple);
      Foraging = createDebugLogStream(DebugType.Foraging);
      Saving = createDebugLogStream(DebugType.Saving);
      Energy = createDebugLogStream(DebugType.Energy);
      Fluid = createDebugLogStream(DebugType.Fluid);
      Translation = createDebugLogStream(DebugType.Translation);
      Moveable = createDebugLogStream(DebugType.Moveable);
      Basement = createDebugLogStream(DebugType.Basement);
      Xml = createDebugLogStream(DebugType.Xml);
      s_logTraceFileLocationEnabled = false;
   }

   private static final class RepeatWatcher {
      private static final Object Lock = "RepeatWatcher_Lock";
      private static String m_lastLine = null;
      private static DebugType m_lastDebugType = null;
      private static LogSeverity m_lastLogSeverity = null;

      private RepeatWatcher() {
      }

      public static boolean check(DebugType var0, LogSeverity var1, String var2, boolean var3) {
         synchronized(Lock) {
            if (var3) {
               m_lastLine = null;
               m_lastDebugType = null;
               m_lastLogSeverity = null;
               return true;
            } else if (m_lastLine == null) {
               m_lastLine = var2;
               m_lastDebugType = var0;
               m_lastLogSeverity = var1;
               return true;
            } else if (m_lastDebugType == var0 && m_lastLogSeverity == var1 && m_lastLine.equals(var2)) {
               return false;
            } else {
               m_lastLine = var2;
               m_lastDebugType = var0;
               m_lastLogSeverity = var1;
               return true;
            }
         }
      }
   }

   private static final class OutputStreamWrapper extends FilterOutputStream {
      public OutputStreamWrapper(OutputStream var1) {
         super(var1);
      }

      public void write(byte[] var1, int var2, int var3) throws IOException {
         this.out.write(var1, var2, var3);
         if (Core.bDebug && UIDebugConsole.instance != null && DebugOptions.instance.UIDebugConsoleDebugLog.getValue()) {
            UIDebugConsole.instance.addOutput(var1, var2, var3);
         }

      }

      public void setStream(OutputStream var1) {
         this.out = var1;
      }
   }
}
