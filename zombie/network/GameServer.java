package zombie.network;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.ConnectException;
import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.AmbientSoundManager;
import zombie.AmbientStreamManager;
import zombie.DebugFileWatcher;
import zombie.GameProfiler;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.MapCollisionData;
import zombie.PersistentOutfits;
import zombie.SandboxOptions;
import zombie.SoundManager;
import zombie.SystemDisabler;
import zombie.VirtualZombieManager;
import zombie.WorldSoundManager;
import zombie.ZomboidFileSystem;
import zombie.ZomboidGlobals;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.asset.AssetManagers;
import zombie.characters.Capability;
import zombie.characters.Faction;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.Role;
import zombie.characters.Roles;
import zombie.characters.Safety;
import zombie.characters.SafetySystemManager;
import zombie.characters.Stats;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.skills.CustomPerks;
import zombie.characters.skills.PerkFactory;
import zombie.commands.CommandBase;
import zombie.core.ActionManager;
import zombie.core.Core;
import zombie.core.ImportantAreaManager;
import zombie.core.Languages;
import zombie.core.PerformanceSettings;
import zombie.core.ProxyPrintStream;
import zombie.core.ThreadGroups;
import zombie.core.TradingManager;
import zombie.core.TransactionManager;
import zombie.core.Translator;
import zombie.core.backup.ZipBackup;
import zombie.core.logger.ExceptionLogger;
import zombie.core.logger.LoggerManager;
import zombie.core.logger.ZLogger;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.physics.PhysicsShape;
import zombie.core.physics.PhysicsShapeAssetManager;
import zombie.core.physics.RagdollSettingsManager;
import zombie.core.profiling.PerformanceProfileFrameProbe;
import zombie.core.profiling.PerformanceProfileProbe;
import zombie.core.raknet.RakNetPeerInterface;
import zombie.core.raknet.RakVoice;
import zombie.core.raknet.UdpConnection;
import zombie.core.raknet.UdpEngine;
import zombie.core.random.Rand;
import zombie.core.random.RandLua;
import zombie.core.random.RandStandard;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.advancedanimation.AnimNodeAssetManager;
import zombie.core.skinnedmodel.model.AiSceneAsset;
import zombie.core.skinnedmodel.model.AiSceneAssetManager;
import zombie.core.skinnedmodel.model.AnimationAsset;
import zombie.core.skinnedmodel.model.AnimationAssetManager;
import zombie.core.skinnedmodel.model.MeshAssetManager;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.model.ModelAssetManager;
import zombie.core.skinnedmodel.model.ModelMesh;
import zombie.core.skinnedmodel.model.jassimp.JAssImpImporter;
import zombie.core.skinnedmodel.population.BeardStyles;
import zombie.core.skinnedmodel.population.ClothingDecals;
import zombie.core.skinnedmodel.population.ClothingItem;
import zombie.core.skinnedmodel.population.ClothingItemAssetManager;
import zombie.core.skinnedmodel.population.HairStyles;
import zombie.core.skinnedmodel.population.OutfitManager;
import zombie.core.skinnedmodel.population.VoiceStyles;
import zombie.core.textures.AnimatedTextureID;
import zombie.core.textures.AnimatedTextureIDAssetManager;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureAssetManager;
import zombie.core.textures.TextureID;
import zombie.core.textures.TextureIDAssetManager;
import zombie.core.utils.UpdateLimit;
import zombie.core.znet.PortMapper;
import zombie.core.znet.SteamGameServer;
import zombie.core.znet.SteamUtils;
import zombie.core.znet.SteamWorkshop;
import zombie.debug.DebugLog;
import zombie.debug.DebugLogStream;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.gameStates.IngameState;
import zombie.globalObjects.SGlobalObjects;
import zombie.inventory.CompressIdenticalItems;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.Food;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.InventoryContainer;
import zombie.inventory.types.Radio;
import zombie.iso.BuildingDef;
import zombie.iso.FishSchoolManager;
import zombie.iso.IsoCamera;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.LosUtil;
import zombie.iso.RoomDef;
import zombie.iso.SpawnPoints;
import zombie.iso.Vector2;
import zombie.iso.Vector3;
import zombie.iso.areas.NonPvpZone;
import zombie.iso.areas.SafeHouse;
import zombie.iso.areas.isoregion.IsoRegions;
import zombie.iso.objects.IsoBarricade;
import zombie.iso.objects.IsoCompost;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoWaveSignal;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.iso.objects.RainManager;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.weather.ClimateManager;
import zombie.iso.zones.Zone;
import zombie.network.anticheats.AntiCheat;
import zombie.network.anticheats.AntiCheatNoClip;
import zombie.network.anticheats.AntiCheatTime;
import zombie.network.chat.ChatServer;
import zombie.network.id.ObjectIDManager;
import zombie.network.packets.AddBrokenGlassPacket;
import zombie.network.packets.INetworkPacket;
import zombie.network.packets.MessageForAdminPacket;
import zombie.network.packets.MetaGridPacket;
import zombie.network.packets.RequestDataPacket;
import zombie.network.packets.SafetyPacket;
import zombie.network.packets.SyncInjuriesPacket;
import zombie.network.packets.SyncVisualsPacket;
import zombie.network.packets.VariableSyncPacket;
import zombie.network.packets.WaveSignalPacket;
import zombie.network.packets.WeatherPacket;
import zombie.network.packets.ZombieHelmetFallingPacket;
import zombie.network.packets.actions.AddCorpseToMapPacket;
import zombie.network.packets.actions.BecomeCorpsePacket;
import zombie.network.packets.actions.HelicopterPacket;
import zombie.network.packets.actions.SmashWindowPacket;
import zombie.network.packets.character.DeadAnimalPacket;
import zombie.network.packets.character.DeadPlayerPacket;
import zombie.network.packets.character.DeadZombiePacket;
import zombie.network.packets.character.RemoveCorpseFromMapPacket;
import zombie.network.packets.hit.HitCharacter;
import zombie.network.packets.service.ReceiveModDataPacket;
import zombie.network.packets.sound.PlayWorldSoundPacket;
import zombie.network.packets.sound.WorldSoundPacket;
import zombie.pathfind.nativeCode.PathfindNative;
import zombie.popman.NetworkZombieManager;
import zombie.popman.NetworkZombiePacker;
import zombie.popman.ZombiePopulationManager;
import zombie.popman.animal.AnimalInstanceManager;
import zombie.radio.ZomboidRadio;
import zombie.radio.devices.DeviceData;
import zombie.sandbox.CustomSandboxOptions;
import zombie.savefile.ServerPlayerDB;
import zombie.scripting.ScriptManager;
import zombie.util.PZSQLUtils;
import zombie.util.PublicServerUtil;
import zombie.util.StringUtils;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.Clipper;
import zombie.vehicles.VehicleManager;
import zombie.vehicles.VehiclePart;
import zombie.vehicles.VehiclesDB2;
import zombie.worldMap.WorldMapRemotePlayer;
import zombie.worldMap.WorldMapRemotePlayers;
import zombie.worldMap.network.WorldMapServer;

public class GameServer {
   public static final int MAX_PLAYERS = 512;
   public static final int TimeLimitForProcessPackets = 70;
   public static final int PacketsUpdateRate = 200;
   public static final int FPS = 10;
   private static final HashMap<String, CCFilter> ccFilters = new HashMap();
   public static int test = 432432;
   public static int DEFAULT_PORT = 16261;
   public static int UDPPort = 16262;
   public static String IPCommandline = null;
   public static int PortCommandline = -1;
   public static int UDPPortCommandline = -1;
   public static Boolean SteamVACCommandline;
   public static boolean GUICommandline;
   public static boolean bServer = false;
   public static boolean bCoop = false;
   public static boolean bDebug = false;
   public static boolean bSoftReset = false;
   public static UdpEngine udpEngine;
   public static final HashMap<Short, Long> IDToAddressMap = new HashMap();
   public static final HashMap<Short, IsoPlayer> IDToPlayerMap = new HashMap();
   public static final ArrayList<IsoPlayer> Players = new ArrayList();
   public static float timeSinceKeepAlive = 0.0F;
   public static int MaxTicksSinceKeepAliveBeforeStall = 60;
   public static final HashSet<UdpConnection> DebugPlayer = new HashSet();
   public static int ResetID = 0;
   public static final ArrayList<String> ServerMods = new ArrayList();
   public static final ArrayList<Long> WorkshopItems = new ArrayList();
   public static String[] WorkshopInstallFolders;
   public static long[] WorkshopTimeStamps;
   public static String ServerName = "servertest";
   public static final DiscordBot discordBot;
   public static String checksum;
   public static String GameMap;
   public static boolean bFastForward;
   public static String ip;
   static int count;
   public static final UdpConnection[] SlotToConnection;
   public static final HashMap<IsoPlayer, Long> PlayerToAddressMap;
   private static boolean bDone;
   private static boolean launched;
   private static final ArrayList<String> consoleCommands;
   private static final ConcurrentLinkedQueue<IZomboidPacket> MainLoopPlayerUpdateQ;
   private static final ConcurrentLinkedQueue<IZomboidPacket> MainLoopNetDataHighPriorityQ;
   private static final ConcurrentLinkedQueue<IZomboidPacket> MainLoopNetDataQ;
   private static final ArrayList<IZomboidPacket> MainLoopNetData2;
   public static final HashMap<Short, Vector2> playerToCoordsMap;
   private static ByteBuffer large_file_bb;
   private static long previousSave;
   private String poisonousBerry = null;
   private String poisonousMushroom = null;
   private String difficulty = "Hardcore";
   private static int droppedPackets;
   private static int countOfDroppedPackets;
   public static int countOfDroppedConnections;
   public static UdpConnection removeZombiesConnection;
   public static UdpConnection removeAnimalsConnection;
   private static UpdateLimit calcCountPlayersInRelevantPositionLimiter;
   private static UpdateLimit sendWorldMapPlayerPositionLimiter;
   public static LoginQueue loginQueue;
   private static int mainCycleExceptionLogCount;
   public static Thread MainThread;
   public static final ArrayList<IsoPlayer> tempPlayers;
   private static final ConcurrentHashMap<String, DelayedConnection> MainLoopDelayedDisconnectQ;
   private static Thread shutdownHook;

   public GameServer() {
   }

   private static String parseIPFromCommandline(String[] var0, int var1, String var2) {
      if (var1 == var0.length - 1) {
         DebugLog.log("expected argument after \"" + var2 + "\"");
         System.exit(0);
      } else if (var0[var1 + 1].trim().isEmpty()) {
         DebugLog.log("empty argument given to \"\" + option + \"\"");
         System.exit(0);
      } else {
         String[] var3 = var0[var1 + 1].trim().split("\\.");
         if (var3.length == 4) {
            for(int var4 = 0; var4 < 4; ++var4) {
               try {
                  int var5 = Integer.parseInt(var3[var4]);
                  if (var5 < 0 || var5 > 255) {
                     DebugLog.log("expected IP address after \"" + var2 + "\", got \"" + var0[var1 + 1] + "\"");
                     System.exit(0);
                  }
               } catch (NumberFormatException var6) {
                  DebugLog.log("expected IP address after \"" + var2 + "\", got \"" + var0[var1 + 1] + "\"");
                  System.exit(0);
               }
            }
         } else {
            DebugLog.log("expected IP address after \"" + var2 + "\", got \"" + var0[var1 + 1] + "\"");
            System.exit(0);
         }
      }

      return var0[var1 + 1];
   }

   private static int parsePortFromCommandline(String[] var0, int var1, String var2) {
      if (var1 == var0.length - 1) {
         DebugLog.log("expected argument after \"" + var2 + "\"");
         System.exit(0);
      } else if (var0[var1 + 1].trim().isEmpty()) {
         DebugLog.log("empty argument given to \"" + var2 + "\"");
         System.exit(0);
      } else {
         try {
            return Integer.parseInt(var0[var1 + 1].trim());
         } catch (NumberFormatException var4) {
            DebugLog.log("expected an integer after \"" + var2 + "\"");
            System.exit(0);
         }
      }

      return -1;
   }

   private static boolean parseBooleanFromCommandline(String[] var0, int var1, String var2) {
      if (var1 == var0.length - 1) {
         DebugLog.log("expected argument after \"" + var2 + "\"");
         System.exit(0);
      } else if (var0[var1 + 1].trim().isEmpty()) {
         DebugLog.log("empty argument given to \"" + var2 + "\"");
         System.exit(0);
      } else {
         String var3 = var0[var1 + 1].trim();
         if ("true".equalsIgnoreCase(var3)) {
            return true;
         }

         if ("false".equalsIgnoreCase(var3)) {
            return false;
         }

         DebugLog.log("expected true or false after \"" + var2 + "\"");
         System.exit(0);
      }

      return false;
   }

   public static void setupCoop() throws FileNotFoundException {
      CoopSlave.init();
   }

   public static void main(String[] var0) {
      Runtime.getRuntime().addShutdownHook(shutdownHook);
      MainThread = Thread.currentThread();
      bServer = true;
      bSoftReset = System.getProperty("softreset") != null;

      int var1;
      for(var1 = 0; var1 < var0.length; ++var1) {
         if (var0[var1] != null) {
            if (var0[var1].startsWith("-cachedir=")) {
               ZomboidFileSystem.instance.setCacheDir(var0[var1].replace("-cachedir=", "").trim());
            } else if (var0[var1].equals("-coop")) {
               bCoop = true;
            }
         }
      }

      String var74;
      if (bCoop) {
         try {
            CoopSlave.initStreams();
         } catch (FileNotFoundException var67) {
            var67.printStackTrace();
         }
      } else {
         try {
            String var10000 = ZomboidFileSystem.instance.getCacheDir();
            var74 = var10000 + File.separator + "server-console.txt";
            FileOutputStream var2 = new FileOutputStream(var74);
            PrintStream var3 = new PrintStream(var2, true);
            System.setOut(new ProxyPrintStream(System.out, var3));
            System.setErr(new ProxyPrintStream(System.err, var3));
         } catch (FileNotFoundException var66) {
            var66.printStackTrace();
         }
      }

      DebugLog.init();
      LoggerManager.init();
      DebugLog.DetailedInfo.trace("cachedir set to \"" + ZomboidFileSystem.instance.getCacheDir() + "\"");
      if (bCoop) {
         try {
            setupCoop();
            CoopSlave.status("UI_ServerStatus_Initialising");
         } catch (FileNotFoundException var65) {
            var65.printStackTrace();
            SteamUtils.shutdown();
            System.exit(37);
            return;
         }
      }

      PZSQLUtils.init();
      Clipper.init();
      RandStandard.INSTANCE.init();
      RandLua.INSTANCE.init();
      DebugLog.General.println("version=%s demo=%s", Core.getInstance().getVersion(), false);
      if (!"25057".isEmpty()) {
         DebugLog.General.println("revision=%s date=%s time=%s (%s)", "25057", "2024-12-17", "17:38:44", "ZB");
      }

      DebugLog.enableServerLogs();
      if (System.getProperty("debug") != null) {
         bDebug = true;
         Core.bDebug = true;
         DebugLog.enableDebugLogs();
      }

      DebugLog.printLogLevels();

      int var4;
      String var5;
      int var7;
      int var8;
      for(var1 = 0; var1 < var0.length; ++var1) {
         if (var0[var1] != null) {
            String[] var75;
            int var77;
            if (!var0[var1].startsWith("-disablelog=")) {
               if (var0[var1].startsWith("-debuglog=")) {
                  var75 = var0[var1].replace("-debuglog=", "").split(",");
                  var77 = var75.length;

                  for(var4 = 0; var4 < var77; ++var4) {
                     var5 = var75[var4];

                     try {
                        DebugLog.setLogEnabled(DebugType.valueOf(var5), true);
                     } catch (IllegalArgumentException var63) {
                     }
                  }
               } else if (var0[var1].equals("-adminusername")) {
                  if (var1 == var0.length - 1) {
                     DebugLog.log("expected argument after \"-adminusername\"");
                     System.exit(0);
                  } else if (!ServerWorldDatabase.isValidUserName(var0[var1 + 1].trim())) {
                     DebugLog.log("invalid username given to \"-adminusername\"");
                     System.exit(0);
                  } else {
                     ServerWorldDatabase.instance.CommandLineAdminUsername = var0[var1 + 1].trim();
                     ++var1;
                  }
               } else if (var0[var1].equals("-adminpassword")) {
                  if (var1 == var0.length - 1) {
                     DebugLog.log("expected argument after \"-adminpassword\"");
                     System.exit(0);
                  } else if (var0[var1 + 1].trim().isEmpty()) {
                     DebugLog.log("empty argument given to \"-adminpassword\"");
                     System.exit(0);
                  } else {
                     ServerWorldDatabase.instance.CommandLineAdminPassword = var0[var1 + 1].trim();
                     ++var1;
                  }
               } else if (!var0[var1].startsWith("-cachedir=")) {
                  if (var0[var1].equals("-ip")) {
                     IPCommandline = parseIPFromCommandline(var0, var1, "-ip");
                     ++var1;
                  } else if (var0[var1].equals("-gui")) {
                     GUICommandline = true;
                  } else if (var0[var1].equals("-nosteam")) {
                     System.setProperty("zomboid.steam", "0");
                  } else if (var0[var1].equals("-stream")) {
                     System.setProperty("zomboid.stream", "1");
                  } else if (var0[var1].equals("-statistic")) {
                     int var76 = parsePortFromCommandline(var0, var1, "-statistic");
                     if (var76 >= 0) {
                        MPStatistic.getInstance().setPeriod(var76);
                        MPStatistic.getInstance().writeEnabled(true);
                     }
                  } else if (var0[var1].equals("-port")) {
                     PortCommandline = parsePortFromCommandline(var0, var1, "-port");
                     ++var1;
                  } else if (var0[var1].equals("-udpport")) {
                     UDPPortCommandline = parsePortFromCommandline(var0, var1, "-udpport");
                     ++var1;
                  } else if (var0[var1].equals("-steamvac")) {
                     SteamVACCommandline = parseBooleanFromCommandline(var0, var1, "-steamvac");
                     ++var1;
                  } else if (var0[var1].equals("-servername")) {
                     if (var1 == var0.length - 1) {
                        DebugLog.log("expected argument after \"-servername\"");
                        System.exit(0);
                     } else if (var0[var1 + 1].trim().isEmpty()) {
                        DebugLog.log("empty argument given to \"-servername\"");
                        System.exit(0);
                     } else {
                        ServerName = var0[var1 + 1].trim();
                        ++var1;
                     }
                  } else if (var0[var1].equals("-coop")) {
                     ServerWorldDatabase.instance.doAdmin = false;
                  } else {
                     DebugLog.log("unknown option \"" + var0[var1] + "\"");
                  }
               }
            } else {
               var75 = var0[var1].replace("-disablelog=", "").split(",");
               var77 = var75.length;

               for(var4 = 0; var4 < var77; ++var4) {
                  var5 = var75[var4];
                  if ("All".equals(var5)) {
                     DebugType[] var6 = DebugType.values();
                     var7 = var6.length;

                     for(var8 = 0; var8 < var7; ++var8) {
                        DebugType var9 = var6[var8];
                        DebugLog.setLogEnabled(var9, false);
                     }
                  } else {
                     try {
                        DebugLog.setLogEnabled(DebugType.valueOf(var5), false);
                     } catch (IllegalArgumentException var64) {
                     }
                  }
               }
            }
         }
      }

      DebugLog.DetailedInfo.trace("server name is \"" + ServerName + "\"");
      var74 = isWorldVersionUnsupported();
      if (var74 != null) {
         DebugLog.log(var74);
         CoopSlave.status(var74);
      } else {
         SteamUtils.init();
         RakNetPeerInterface.init();
         ZombiePopulationManager.init();
         PathfindNative.init();
         Roles.init();

         try {
            ZomboidFileSystem.instance.init();
            Languages.instance.init();
            Translator.loadFiles();
         } catch (Exception var62) {
            DebugLog.General.printException(var62, "Exception Thrown", LogSeverity.Error);
            DebugLog.General.println("Server Terminated.");
         }

         ServerOptions.instance.init();
         initClientCommandFilter();
         if (PortCommandline != -1) {
            ServerOptions.instance.DefaultPort.setValue(PortCommandline);
         }

         if (UDPPortCommandline != -1) {
            ServerOptions.instance.UDPPort.setValue(UDPPortCommandline);
         }

         if (SteamVACCommandline != null) {
            ServerOptions.instance.SteamVAC.setValue(SteamVACCommandline);
         }

         DEFAULT_PORT = ServerOptions.instance.DefaultPort.getValue();
         UDPPort = ServerOptions.instance.UDPPort.getValue();
         if (CoopSlave.instance != null) {
            ServerOptions.instance.ServerPlayerID.setValue("");
         }

         String var78;
         if (SteamUtils.isSteamModeEnabled()) {
            var78 = ServerOptions.instance.PublicName.getValue();
            if (var78 == null || var78.isEmpty()) {
               ServerOptions.instance.PublicName.setValue("My PZ Server");
            }
         }

         var78 = ServerOptions.instance.Map.getValue();
         if (var78 != null && !var78.trim().isEmpty()) {
            GameMap = var78.trim();
            if (GameMap.contains(";")) {
               String[] var79 = GameMap.split(";");
               var78 = var79[0];
            }

            Core.GameMap = var78.trim();
         }

         String var80 = ServerOptions.instance.Mods.getValue();
         int var83;
         String var88;
         if (var80 != null) {
            String[] var81 = var80.split(";");
            String[] var82 = var81;
            var83 = var81.length;

            for(var7 = 0; var7 < var83; ++var7) {
               var88 = var82[var7];
               if (!var88.trim().isEmpty()) {
                  ServerMods.add(var88.trim());
               }
            }
         }

         int var10;
         if (SteamUtils.isSteamModeEnabled()) {
            var4 = ServerOptions.instance.SteamVAC.getValue() ? 3 : 2;
            if (!SteamGameServer.Init(IPCommandline, DEFAULT_PORT, UDPPort, var4, Core.getInstance().getSteamServerVersion())) {
               SteamUtils.shutdown();
               return;
            }

            SteamGameServer.SetProduct("zomboid");
            SteamGameServer.SetGameDescription("Project Zomboid");
            SteamGameServer.SetModDir("zomboid");
            SteamGameServer.SetDedicatedServer(true);
            SteamGameServer.SetMaxPlayerCount(ServerOptions.getInstance().getMaxPlayers());
            SteamGameServer.SetServerName(ServerOptions.instance.PublicName.getValue());
            SteamGameServer.SetMapName(ServerOptions.instance.Map.getValue());
            if (ServerOptions.instance.Public.getValue()) {
               SteamGameServer.SetGameTags(CoopSlave.instance != null ? "hosted" : "");
            } else {
               SteamGameServer.SetGameTags("hidden" + (CoopSlave.instance != null ? ";hosted" : ""));
            }

            SteamGameServer.SetKeyValue("description", ServerOptions.instance.PublicDescription.getValue());
            SteamGameServer.SetKeyValue("version", Core.getInstance().getVersionNumber());
            SteamGameServer.SetKeyValue("open", ServerOptions.instance.Open.getValue() ? "1" : "0");
            SteamGameServer.SetKeyValue("public", ServerOptions.instance.Public.getValue() ? "1" : "0");
            var5 = ServerOptions.instance.Mods.getValue();
            var83 = 0;
            String[] var86 = var5.split(";");
            String[] var90 = var86;
            int var91 = var86.length;

            for(var10 = 0; var10 < var91; ++var10) {
               String var11 = var90[var10];
               if (!StringUtils.isNullOrWhitespace(var11)) {
                  ++var83;
               }
            }

            int var12;
            String var13;
            String[] var93;
            String[] var95;
            int var97;
            if (var5.length() > 128) {
               StringBuilder var92 = new StringBuilder();
               var93 = var5.split(";");
               var95 = var93;
               var97 = var93.length;

               for(var12 = 0; var12 < var97; ++var12) {
                  var13 = var95[var12];
                  if (var92.length() + 1 + var13.length() > 128) {
                     break;
                  }

                  if (var92.length() > 0) {
                     var92.append(';');
                  }

                  var92.append(var13);
               }

               var5 = var92.toString();
            }

            SteamGameServer.SetKeyValue("mods", var5);
            SteamGameServer.SetKeyValue("modCount", String.valueOf(var83));
            SteamGameServer.SetKeyValue("pvp", ServerOptions.instance.PVP.getValue() ? "1" : "0");
            if (bDebug) {
            }

            var88 = ServerOptions.instance.WorkshopItems.getValue();
            if (var88 != null) {
               var93 = var88.split(";");
               var95 = var93;
               var97 = var93.length;

               for(var12 = 0; var12 < var97; ++var12) {
                  var13 = var95[var12];
                  var13 = var13.trim();
                  if (!var13.isEmpty() && SteamUtils.isValidSteamID(var13)) {
                     WorkshopItems.add(SteamUtils.convertStringToSteamID(var13));
                  }
               }
            }

            if (bCoop) {
               CoopSlave.instance.sendMessage("status", (String)null, Translator.getText("UI_ServerStatus_Downloaded_Workshop_Items_Count", WorkshopItems.size()));
            }

            SteamWorkshop.init();
            SteamGameServer.LogOnAnonymous();
            SteamGameServer.EnableHeartBeats(true);
            DebugLog.log("Waiting for response from Steam servers");

            while(true) {
               SteamUtils.runLoop();
               var91 = SteamGameServer.GetSteamServersConnectState();
               if (var91 == SteamGameServer.STEAM_SERVERS_CONNECTED) {
                  if (bCoop) {
                     CoopSlave.status("UI_ServerStatus_Downloading_Workshop_Items");
                  }

                  if (!GameServerWorkshopItems.Install(WorkshopItems)) {
                     return;
                  }
                  break;
               }

               if (var91 == SteamGameServer.STEAM_SERVERS_CONNECTFAILURE) {
                  DebugLog.log("Failed to connect to Steam servers");
                  SteamUtils.shutdown();
                  return;
               }

               try {
                  Thread.sleep(100L);
               } catch (InterruptedException var61) {
               }
            }
         }

         ZipBackup.onStartup();
         ZipBackup.onVersion();
         var4 = 0;

         try {
            ServerWorldDatabase.instance.create();
         } catch (ClassNotFoundException | SQLException var60) {
            var60.printStackTrace();
         }

         if (ServerOptions.instance.UPnP.getValue()) {
            DebugLog.log("Router detection/configuration starting.");
            DebugLog.log("If the server hangs here, set UPnP=false.");
            PortMapper.startup();
            if (PortMapper.discover()) {
               DebugLog.DetailedInfo.trace("UPnP-enabled internet gateway found: " + PortMapper.getGatewayInfo());
               var5 = PortMapper.getExternalAddress();
               DebugLog.DetailedInfo.trace("External IP address: " + var5);
               DebugLog.log("trying to setup port forwarding rules...");
               var83 = 86400;
               boolean var87 = true;
               if (PortMapper.addMapping(DEFAULT_PORT, DEFAULT_PORT, "PZ Server default port", "UDP", var83, var87)) {
                  DebugLog.log(DebugType.Network, "Default port has been mapped successfully");
               } else {
                  DebugLog.log(DebugType.Network, "Failed to map default port");
               }

               if (SteamUtils.isSteamModeEnabled()) {
                  var8 = ServerOptions.instance.UDPPort.getValue();
                  if (PortMapper.addMapping(var8, var8, "PZ Server UDPPort", "UDP", var83, var87)) {
                     DebugLog.log(DebugType.Network, "AdditionUDPPort has been mapped successfully");
                  } else {
                     DebugLog.log(DebugType.Network, "Failed to map AdditionUDPPort");
                  }
               }
            } else {
               DebugLog.log(DebugType.Network, "No UPnP-enabled Internet gateway found, you must configure port forwarding on your gateway manually in order to make your server accessible from the Internet.");
            }
         }

         Core.GameMode = "Multiplayer";
         bDone = false;
         DebugLog.log(DebugType.Network, "Initialising Server Systems...");
         CoopSlave.status("UI_ServerStatus_Initialising");

         try {
            doMinimumInit();
         } catch (Exception var59) {
            DebugLog.General.printException(var59, "Exception Thrown", LogSeverity.Error);
            DebugLog.General.println("Server Terminated.");
         }

         LosUtil.init(100, 100);
         ChatServer.getInstance().init();
         DebugLog.log(DebugType.Network, "Loading world...");
         CoopSlave.status("UI_ServerStatus_LoadingWorld");

         try {
            ClimateManager.setInstance(new ClimateManager());
            RagdollSettingsManager.setInstance(new RagdollSettingsManager());
            IsoWorld.instance.init();
         } catch (Exception var58) {
            DebugLog.General.printException(var58, "Exception Thrown", LogSeverity.Error);
            DebugLog.General.println("Server Terminated.");
            CoopSlave.status("UI_ServerStatus_Terminated");
            return;
         }

         File var84 = ZomboidFileSystem.instance.getFileInCurrentSave("z_outfits.bin");
         if (!var84.exists()) {
            ServerOptions.instance.changeOption("ResetID", Integer.toString(Rand.Next(100000000)));
         }

         try {
            SpawnPoints.instance.initServer2(IsoWorld.instance.MetaGrid);
         } catch (Exception var57) {
            var57.printStackTrace();
         }

         LuaEventManager.triggerEvent("OnGameTimeLoaded");
         SGlobalObjects.initSystems();
         SoundManager.instance = new SoundManager();
         AmbientStreamManager.instance = new AmbientSoundManager();
         AmbientStreamManager.instance.init();
         ServerMap.instance.LastSaved = System.currentTimeMillis();
         VehicleManager.instance = new VehicleManager();
         ServerPlayersVehicles.instance.init();
         DebugOptions.instance.init();
         GameProfiler.init();
         WorldMapServer.instance.readSavefile();

         try {
            startServer();
         } catch (ConnectException var56) {
            var56.printStackTrace();
            SteamUtils.shutdown();
            return;
         }

         if (SteamUtils.isSteamModeEnabled()) {
            DebugLog.DetailedInfo.trace("##########\nServer Steam ID " + SteamGameServer.GetSteamID() + "\n##########");
         }

         UpdateLimit var85 = new UpdateLimit(100L);
         PerformanceSettings.setLockFPS(10);
         IngameState var89 = new IngameState();
         float var94 = 0.0F;
         float[] var96 = new float[20];

         for(var10 = 0; var10 < 20; ++var10) {
            var96[var10] = (float)PerformanceSettings.getLockFPS();
         }

         float var98 = (float)PerformanceSettings.getLockFPS();
         long var99 = System.currentTimeMillis();
         long var100 = System.currentTimeMillis();
         if (!SteamUtils.isSteamModeEnabled()) {
            PublicServerUtil.init();
            PublicServerUtil.insertOrUpdate();
         }

         ServerLOS.init();
         NetworkAIParams.Init();
         int var15 = ServerOptions.instance.RCONPort.getValue();
         String var16 = ServerOptions.instance.RCONPassword.getValue();
         if (var15 != 0 && var16 != null && !var16.isEmpty()) {
            String var17 = System.getProperty("rconlo");
            RCONServer.init(var15, var16, var17 != null);
         }

         LuaManager.GlobalObject.refreshAnimSets(true);

         while(!bDone) {
            try {
               long var101 = System.nanoTime();
               MPStatistics.countServerNetworkingFPS();
               MainLoopNetData2.clear();

               IZomboidPacket var19;
               for(var19 = (IZomboidPacket)MainLoopNetDataHighPriorityQ.poll(); var19 != null; var19 = (IZomboidPacket)MainLoopNetDataHighPriorityQ.poll()) {
                  MainLoopNetData2.add(var19);
               }

               Iterator var102 = MainLoopDelayedDisconnectQ.entrySet().iterator();

               while(var102.hasNext()) {
                  DelayedConnection var20 = (DelayedConnection)((Map.Entry)var102.next()).getValue();
                  if (var20.isCooldown()) {
                     var20.disconnect();
                     var102.remove();
                  }
               }

               MPStatistic.getInstance().setPacketsLength((long)MainLoopNetData2.size());

               int var103;
               IZomboidPacket var104;
               for(var103 = 0; var103 < MainLoopNetData2.size(); ++var103) {
                  var104 = (IZomboidPacket)MainLoopNetData2.get(var103);
                  if (var104.isConnect()) {
                     ((DelayedConnection)var104).connect();
                  } else if (var104.isDisconnect()) {
                     ((DelayedConnection)var104).disconnect();
                  } else {
                     mainLoopDealWithNetData((ZomboidNetData)var104);
                  }
               }

               MainLoopNetData2.clear();

               for(var19 = (IZomboidPacket)MainLoopPlayerUpdateQ.poll(); var19 != null; var19 = (IZomboidPacket)MainLoopPlayerUpdateQ.poll()) {
                  MainLoopNetData2.add(var19);
               }

               MPStatistic.getInstance().setPacketsLength((long)MainLoopNetData2.size());

               for(var103 = 0; var103 < MainLoopNetData2.size(); ++var103) {
                  var104 = (IZomboidPacket)MainLoopNetData2.get(var103);
                  GameServer.s_performance.mainLoopDealWithNetData.invokeAndMeasure((ZomboidNetData)var104, GameServer::mainLoopDealWithNetData);
               }

               MainLoopNetData2.clear();

               for(var19 = (IZomboidPacket)MainLoopNetDataQ.poll(); var19 != null; var19 = (IZomboidPacket)MainLoopNetDataQ.poll()) {
                  MainLoopNetData2.add(var19);
               }

               for(var103 = 0; var103 < MainLoopNetData2.size(); ++var103) {
                  if (var103 % 10 == 0 && (System.nanoTime() - var101) / 1000000L > 70L) {
                     if (droppedPackets == 0) {
                        DebugLog.log("Server is too busy. Server will drop updates of vehicle's physics. Server is closed for new connections.");
                     }

                     droppedPackets += 2;
                     countOfDroppedPackets += MainLoopNetData2.size() - var103;
                     break;
                  }

                  var104 = (IZomboidPacket)MainLoopNetData2.get(var103);
                  GameServer.s_performance.mainLoopDealWithNetData.invokeAndMeasure((ZomboidNetData)var104, GameServer::mainLoopDealWithNetData);
               }

               MainLoopNetData2.clear();
               if (droppedPackets == 1) {
                  DebugLog.log("Server is working normal. Server will not drop updates of vehicle's physics. The server is open for new connections. Server dropped " + countOfDroppedPackets + " packets and " + countOfDroppedConnections + " connections.");
                  countOfDroppedPackets = 0;
                  countOfDroppedConnections = 0;
               }

               droppedPackets = Math.max(0, Math.min(1000, droppedPackets - 1));
               if (!var85.Check()) {
                  long var110 = PZMath.clamp((5000000L - System.nanoTime() + var101) / 1000000L, 0L, 100L);
                  if (var110 > 0L) {
                     try {
                        MPStatistic.getInstance().Main.StartSleep();
                        Thread.sleep(var110);
                        MPStatistic.getInstance().Main.EndSleep();
                     } catch (InterruptedException var55) {
                        var55.printStackTrace();
                     }
                  }
               } else {
                  MPStatistic.getInstance().Main.Start();
                  ++IsoCamera.frameState.frameCount;
                  IsoCamera.frameState.updateUnPausedAccumulator();
                  GameServer.s_performance.frameStep.start();

                  try {
                     timeSinceKeepAlive += GameTime.getInstance().getMultiplier();
                     MPStatistic.getInstance().ServerMapPreupdate.Start();
                     ServerMap.instance.preupdate();
                     MPStatistic.getInstance().ServerMapPreupdate.End();
                     int var105;
                     synchronized(consoleCommands) {
                        var105 = 0;

                        while(true) {
                           if (var105 >= consoleCommands.size()) {
                              consoleCommands.clear();
                              break;
                           }

                           String var21 = (String)consoleCommands.get(var105);

                           try {
                              if (CoopSlave.instance == null || !CoopSlave.instance.handleCommand(var21)) {
                                 System.out.println(handleServerCommand(var21, (UdpConnection)null));
                              }
                           } catch (Exception var69) {
                              var69.printStackTrace();
                           }

                           ++var105;
                        }
                     }

                     if (removeZombiesConnection != null) {
                        NetworkZombieManager.removeZombies(removeZombiesConnection);
                        removeZombiesConnection = null;
                     }

                     if (removeAnimalsConnection != null) {
                        AnimalInstanceManager.getInstance().remove(removeAnimalsConnection);
                        removeAnimalsConnection = null;
                     }

                     GameServer.s_performance.RCONServerUpdate.invokeAndMeasure(RCONServer::update);

                     try {
                        MapCollisionData.instance.updateGameState();
                        MPStatistic.getInstance().IngameStateUpdate.Start();
                        var89.update();
                        MPStatistic.getInstance().IngameStateUpdate.End();
                        VehicleManager.instance.serverUpdate();
                        ObjectIDManager.getInstance().checkForSaveDataFile(false);
                     } catch (Exception var54) {
                        var54.printStackTrace();
                     }

                     var103 = 0;
                     var105 = 0;

                     for(int var106 = 0; var106 < Players.size(); ++var106) {
                        IsoPlayer var22 = (IsoPlayer)Players.get(var106);
                        if (var22.isAlive()) {
                           if (!IsoWorld.instance.CurrentCell.getObjectList().contains(var22)) {
                              IsoWorld.instance.CurrentCell.getObjectList().add(var22);
                           }

                           ++var105;
                           if (var22.isAsleep()) {
                              ++var103;
                           }
                        }

                        ServerMap.instance.characterIn(var22);
                     }

                     ImportantAreaManager.getInstance().process();
                     setFastForward(ServerOptions.instance.SleepAllowed.getValue() && var105 > 0 && var103 == var105);
                     boolean var107 = calcCountPlayersInRelevantPositionLimiter.Check();

                     UdpConnection var23;
                     int var24;
                     int var108;
                     for(var108 = 0; var108 < udpEngine.connections.size(); ++var108) {
                        var23 = (UdpConnection)udpEngine.connections.get(var108);
                        if (var107) {
                           var23.calcCountPlayersInRelevantPosition();
                        }

                        for(var24 = 0; var24 < 4; ++var24) {
                           Vector3 var25 = var23.connectArea[var24];
                           if (var25 != null) {
                              ServerMap.instance.characterIn((int)var25.x, (int)var25.y, (int)var25.z);
                           }

                           ClientServerMap.characterIn(var23, var24);
                        }

                        if (var23.playerDownloadServer != null) {
                           var23.playerDownloadServer.update();
                        }
                     }

                     for(var108 = 0; var108 < IsoWorld.instance.CurrentCell.getObjectList().size(); ++var108) {
                        IsoMovingObject var109 = (IsoMovingObject)IsoWorld.instance.CurrentCell.getObjectList().get(var108);
                        if (!(var109 instanceof IsoAnimal) && var109 instanceof IsoPlayer && !Players.contains(var109)) {
                           DebugLog.log("Disconnected player in CurrentCell.getObjectList() removed");
                           IsoWorld.instance.CurrentCell.getObjectList().remove(var108--);
                        }
                     }

                     ++var4;
                     if (var4 > 150) {
                        for(var108 = 0; var108 < udpEngine.connections.size(); ++var108) {
                           var23 = (UdpConnection)udpEngine.connections.get(var108);

                           try {
                              if (var23.username == null && !var23.awaitingCoopApprove && !LoginQueue.isInTheQueue(var23) && var23.isConnectionAttemptTimeout() && (!var23.googleAuth || var23.isGoogleAuthTimeout())) {
                                 disconnect(var23, "connection-attempt-timeout");
                                 udpEngine.forceDisconnect(var23.getConnectedGUID(), "connection-attempt-timeout");
                              }
                           } catch (Exception var68) {
                              var68.printStackTrace();
                           }
                        }

                        var4 = 0;
                     }

                     MPStatistic.getInstance().ServerMapPostupdate.Start();
                     ServerMap.instance.postupdate();
                     MPStatistic.getInstance().ServerMapPostupdate.End();

                     try {
                        ServerGUI.update();
                     } catch (Exception var53) {
                        var53.printStackTrace();
                     }

                     var100 = var99;
                     var99 = System.currentTimeMillis();
                     long var111 = var99 - var100;
                     var94 = 1000.0F / (float)var111;
                     if (!Float.isNaN(var94)) {
                        var98 = (float)((double)var98 + Math.min((double)(var94 - var98) * 0.05, 1.0));
                     }

                     GameTime.instance.FPSMultiplier = 60.0F / var98;
                     launchCommandHandler();
                     MPStatistic.getInstance().process(var111);
                     if (!SteamUtils.isSteamModeEnabled()) {
                        PublicServerUtil.update();
                        PublicServerUtil.updatePlayerCountIfChanged();
                     }

                     for(var24 = 0; var24 < udpEngine.connections.size(); ++var24) {
                        UdpConnection var112 = (UdpConnection)udpEngine.connections.get(var24);
                        var112.validator.update();
                        if (!var112.chunkObjectState.isEmpty()) {
                           for(int var26 = 0; var26 < var112.chunkObjectState.size(); var26 += 2) {
                              short var27 = var112.chunkObjectState.get(var26);
                              short var28 = var112.chunkObjectState.get(var26 + 1);
                              if (!var112.RelevantTo((float)(var27 * 8 + 4), (float)(var28 * 8 + 4), (float)(var112.ChunkGridWidth * 4 * 8))) {
                                 var112.chunkObjectState.remove(var26, 2);
                                 var26 -= 2;
                              }
                           }
                        }
                     }

                     if (sendWorldMapPlayerPositionLimiter.Check()) {
                        try {
                           sendWorldMapPlayerPosition();
                        } catch (Exception var52) {
                           boolean var113 = true;
                        }
                     }

                     if (CoopSlave.instance != null) {
                        CoopSlave.instance.update();
                        if (CoopSlave.instance.masterLost()) {
                           DebugLog.log("Coop master is not responding, terminating");
                           ServerMap.instance.QueueQuit();
                        }
                     }

                     LoginQueue.update();
                     ZipBackup.onPeriod();
                     SteamUtils.runLoop();
                     TradingManager.getInstance().update();
                     WarManager.update();
                     GameWindow.fileSystem.updateAsyncTransactions();
                  } catch (Exception var71) {
                     if (mainCycleExceptionLogCount-- > 0) {
                        DebugLog.Multiplayer.printException(var71, "Server processing error", LogSeverity.Error);
                     }
                  } finally {
                     GameServer.s_performance.frameStep.end();
                  }
               }
            } catch (Exception var73) {
               if (mainCycleExceptionLogCount-- > 0) {
                  DebugLog.Multiplayer.printException(var73, "Server error", LogSeverity.Error);
               }
            }
         }

         System.exit(0);
      }
   }

   private static void launchCommandHandler() {
      if (!launched) {
         launched = true;
         (new Thread(ThreadGroups.Workers, () -> {
            try {
               BufferedReader var0 = new BufferedReader(new InputStreamReader(System.in));

               while(true) {
                  String var1 = var0.readLine();
                  if (var1 == null) {
                     consoleCommands.add("process-status@eof");
                     break;
                  }

                  if (!var1.isEmpty()) {
                     System.out.println("command entered via server console (System.in): \"" + var1 + "\"");
                     synchronized(consoleCommands) {
                        consoleCommands.add(var1);
                     }
                  }
               }
            } catch (Exception var5) {
               var5.printStackTrace();
            }

         }, "command handler")).start();
      }
   }

   public static String rcon(String var0) {
      try {
         return handleServerCommand(var0, (UdpConnection)null);
      } catch (Throwable var2) {
         var2.printStackTrace();
         return null;
      }
   }

   private static String handleServerCommand(String var0, UdpConnection var1) {
      if (var0 == null) {
         return null;
      } else {
         String var2 = "admin";
         Role var3 = Roles.getDefaultForAdmin();
         if (var1 != null) {
            var2 = var1.username;
            if (!var1.isCoopHost) {
               var3 = var1.role;
            }
         }

         Class var4 = CommandBase.findCommandCls(var0);
         if (var4 != null) {
            Constructor var5 = var4.getConstructors()[0];

            try {
               CommandBase var6 = (CommandBase)var5.newInstance(var2, var3, var0, var1);
               return var6.Execute();
            } catch (InvocationTargetException var7) {
               var7.printStackTrace();
               return "A InvocationTargetException error occured";
            } catch (IllegalAccessException var8) {
               var8.printStackTrace();
               return "A IllegalAccessException error occured";
            } catch (InstantiationException var9) {
               var9.printStackTrace();
               return "A InstantiationException error occured";
            } catch (SQLException var10) {
               var10.printStackTrace();
               return "A SQL error occured";
            }
         } else {
            return "Unknown command " + var0;
         }
      }
   }

   public static void sendTeleport(IsoPlayer var0, float var1, float var2, float var3) {
      if (var0 != null) {
         INetworkPacket.send(var0, PacketTypes.PacketType.Teleport, var0, var1, var2, var3);
         if (var0.getNetworkCharacterAI() != null) {
            var0.getNetworkCharacterAI().resetSpeedLimiter();
         }

         AntiCheatNoClip.teleport(var0);
      }

   }

   public static void sendPlayerExtraInfo(IsoPlayer var0, UdpConnection var1) {
      INetworkPacket.sendToAll(PacketTypes.PacketType.ExtraInfo, (UdpConnection)null, var0);
   }

   public static boolean canModifyPlayerStats(UdpConnection var0, IsoPlayer var1) {
      return var0.role.haveCapability(Capability.CanModifyPlayerStatsInThePlayerStatsUI) || var0.havePlayer(var1);
   }

   static void receiveChangePlayerStats(ByteBuffer var0, UdpConnection var1, short var2) {
      short var3 = var0.getShort();
      IsoPlayer var4 = (IsoPlayer)IDToPlayerMap.get(var3);
      if (var4 != null) {
         String var5 = GameWindow.ReadString(var0);
         var4.setPlayerStats(var0, var5);

         for(int var6 = 0; var6 < udpEngine.connections.size(); ++var6) {
            UdpConnection var7 = (UdpConnection)udpEngine.connections.get(var6);
            if (var7.getConnectedGUID() != var1.getConnectedGUID()) {
               if (var7.getConnectedGUID() == (Long)PlayerToAddressMap.get(var4)) {
                  var7.allChatMuted = var4.isAllChatMuted();
                  var7.role = var4.role;
               }

               ByteBufferWriter var8 = var7.startPacket();
               PacketTypes.PacketType.ChangePlayerStats.doPacket(var8);
               var4.createPlayerStats(var8, var5);
               PacketTypes.PacketType.ChangePlayerStats.send(var7);
            }
         }

      }
   }

   public static void doMinimumInit() throws IOException {
      RandStandard.INSTANCE.init();
      RandLua.INSTANCE.init();
      DebugFileWatcher.instance.init();
      ArrayList var0 = new ArrayList(ServerMods);
      ZomboidFileSystem.instance.loadMods(var0);
      LuaManager.init();
      PerkFactory.init();
      CustomPerks.instance.init();
      CustomPerks.instance.initLua();
      AssetManagers var1 = GameWindow.assetManagers;
      AiSceneAssetManager.instance.create(AiSceneAsset.ASSET_TYPE, var1);
      AnimatedTextureIDAssetManager.instance.create(AnimatedTextureID.ASSET_TYPE, var1);
      AnimationAssetManager.instance.create(AnimationAsset.ASSET_TYPE, var1);
      AnimNodeAssetManager.instance.create(AnimationAsset.ASSET_TYPE, var1);
      ClothingItemAssetManager.instance.create(ClothingItem.ASSET_TYPE, var1);
      MeshAssetManager.instance.create(ModelMesh.ASSET_TYPE, var1);
      ModelAssetManager.instance.create(Model.ASSET_TYPE, var1);
      PhysicsShapeAssetManager.instance.create(PhysicsShape.ASSET_TYPE, var1);
      TextureIDAssetManager.instance.create(TextureID.ASSET_TYPE, var1);
      TextureAssetManager.instance.create(Texture.ASSET_TYPE, var1);
      if (GUICommandline && !bSoftReset) {
         ServerGUI.init();
      }

      CustomSandboxOptions.instance.init();
      CustomSandboxOptions.instance.initInstance(SandboxOptions.instance);
      ScriptManager.instance.Load();
      CustomizationManager.getInstance().load();
      ClothingDecals.init();
      BeardStyles.init();
      HairStyles.init();
      OutfitManager.init();
      VoiceStyles.init();
      JAssImpImporter.Init();
      ModelManager.NoOpenGL = !ServerGUI.isCreated();
      ModelManager.instance.create();
      System.out.println("LOADING ASSETS: START");
      CoopSlave.status("UI_ServerStatus_Loading_Assets");

      while(GameWindow.fileSystem.hasWork()) {
         GameWindow.fileSystem.updateAsyncTransactions();
      }

      System.out.println("LOADING ASSETS: FINISH");
      CoopSlave.status("UI_ServerStatus_Initing_Checksum");

      try {
         LuaManager.initChecksum();
         LuaManager.LoadDirBase("shared");
         LuaManager.LoadDirBase("client", true);
         LuaManager.LoadDirBase("server");
         LuaManager.finishChecksum();
      } catch (Exception var3) {
         var3.printStackTrace();
      }

      ScriptManager.instance.LoadedAfterLua();
      CoopSlave.status("UI_ServerStatus_Loading_Sandbox_Vars");
      String var10002 = ZomboidFileSystem.instance.getCacheDir();
      File var2 = new File(var10002 + File.separator + "Server" + File.separator + ServerName + "_SandboxVars.lua");
      if (var2.exists()) {
         if (!SandboxOptions.instance.loadServerLuaFile(ServerName)) {
            System.out.println("Exiting due to errors loading " + var2.getCanonicalPath());
            System.exit(1);
         }

         SandboxOptions.instance.handleOldServerZombiesFile();
         SandboxOptions.instance.saveServerLuaFile(ServerName);
         SandboxOptions.instance.toLua();
      } else {
         SandboxOptions.instance.handleOldServerZombiesFile();
         SandboxOptions.instance.saveServerLuaFile(ServerName);
         SandboxOptions.instance.toLua();
      }

      LuaEventManager.triggerEvent("OnGameBoot");
      ZomboidGlobals.Load();
      SpawnPoints.instance.initServer1();
      ServerGUI.init2();
   }

   public static void startServer() throws ConnectException {
      String var0 = ServerOptions.instance.Password.getValue();
      if (CoopSlave.instance != null && SteamUtils.isSteamModeEnabled()) {
         var0 = "";
      }

      udpEngine = new UdpEngine(DEFAULT_PORT, UDPPort, ServerOptions.getInstance().getMaxPlayersForEstablishingConnection(), var0, true);
      DebugLog.log(DebugType.Network, "*** SERVER STARTED ****");
      DebugLog.log(DebugType.Network, "*** Steam is " + (SteamUtils.isSteamModeEnabled() ? "enabled" : "not enabled"));
      if (SteamUtils.isSteamModeEnabled()) {
         DebugLog.DetailedInfo.trace("Server is listening on port " + DEFAULT_PORT + " (for Steam connection) and port " + UDPPort + " (for UDPRakNet connection)");
         DebugLog.DetailedInfo.trace("Clients should use " + DEFAULT_PORT + " port for connections");
      } else {
         DebugLog.DetailedInfo.trace("server is listening on port " + DEFAULT_PORT);
      }

      ResetID = ServerOptions.instance.ResetID.getValue();
      String var5;
      if (CoopSlave.instance != null) {
         if (SteamUtils.isSteamModeEnabled()) {
            RakNetPeerInterface var1 = udpEngine.getPeer();
            CoopSlave var10000 = CoopSlave.instance;
            String var10003 = var1.GetServerIP();
            var10000.sendMessage("server-address", (String)null, var10003 + ":" + DEFAULT_PORT);
            long var2 = SteamGameServer.GetSteamID();
            CoopSlave.instance.sendMessage("steam-id", (String)null, SteamUtils.convertSteamIDToString(var2));
         } else {
            var5 = "127.0.0.1";
            CoopSlave.instance.sendMessage("server-address", (String)null, var5 + ":" + DEFAULT_PORT);
         }
      }

      LuaEventManager.triggerEvent("OnServerStarted");
      if (SteamUtils.isSteamModeEnabled()) {
         CoopSlave.status("UI_ServerStatus_Started");
      } else {
         CoopSlave.status("UI_ServerStatus_Started");
      }

      var5 = ServerOptions.instance.DiscordChannel.getValue();
      String var6 = ServerOptions.instance.DiscordToken.getValue();
      boolean var3 = ServerOptions.instance.DiscordEnable.getValue();
      String var4 = ServerOptions.instance.DiscordChannelID.getValue();
      discordBot.connect(var3, var6, var5, var4);
   }

   private static void mainLoopDealWithNetData(ZomboidNetData var0) {
      if (SystemDisabler.getDoMainLoopDealWithNetData()) {
         ByteBuffer var1 = var0.buffer;
         UdpConnection var2 = udpEngine.getActiveConnection(var0.connection);
         if (var0.type == null) {
            ZomboidNetDataPool.instance.discard(var0);
         } else {
            ++var0.type.serverPacketCount;
            MPStatistic.getInstance().addIncomePacket(var0.type, var1.limit());

            try {
               if (var2 == null) {
                  DebugLog.log(DebugType.Network, "Received packet type=" + var0.type.name() + " connection is null.");
                  return;
               }

               if (var2.username == null) {
                  switch (var0.type) {
                     case Login:
                     case Ping:
                     case ScoreboardUpdate:
                     case GoogleAuth:
                     case GoogleAuthKey:
                     case ServerCustomization:
                        break;
                     default:
                        String var10000 = var0.type.name();
                        DebugLog.log("Received packet type=" + var10000 + " before Login, disconnecting " + var2.getInetSocketAddress().getHostString());
                        var2.forceDisconnect("unacceptable-packet");
                        ZomboidNetDataPool.instance.discard(var0);
                        return;
                  }
               }

               var0.type.onServerPacket(var1, var2);
            } catch (Exception var4) {
               if (var2 == null) {
                  DebugLog.log(DebugType.Network, "Error with packet of type: " + var0.type + " connection is null.");
               } else {
                  PacketTypes.PacketType var10001 = var0.type;
                  DebugLog.General.error("Error with packet of type: " + var10001 + " for " + var2.getConnectedGUID());
                  AntiCheat.PacketException.act(var2, var0.type.name());
               }

               var4.printStackTrace();
            }

            ZomboidNetDataPool.instance.discard(var0);
         }
      }
   }

   static void receiveInvMngRemoveItem(ByteBuffer var0, UdpConnection var1, short var2) {
      int var3 = var0.getInt();
      short var4 = var0.getShort();
      IsoPlayer var5 = (IsoPlayer)IDToPlayerMap.get(var4);
      if (var5 != null) {
         InventoryItem var6 = var5.getInventory().getItemWithID(var3);
         if (var6 != null) {
            var5.getInventory().Remove(var6);
            sendRemoveItemFromContainer(var5.getInventory(), var6);
         }
      }

   }

   static void receiveInvMngGetItem(ByteBuffer var0, UdpConnection var1, short var2) throws IOException {
      short var3 = var0.getShort();
      IsoPlayer var4 = (IsoPlayer)IDToPlayerMap.get(var3);
      if (var4 != null) {
         for(int var5 = 0; var5 < udpEngine.connections.size(); ++var5) {
            UdpConnection var6 = (UdpConnection)udpEngine.connections.get(var5);
            if (var6.getConnectedGUID() != var1.getConnectedGUID() && var6.getConnectedGUID() == (Long)PlayerToAddressMap.get(var4)) {
               ByteBufferWriter var7 = var6.startPacket();
               PacketTypes.PacketType.InvMngGetItem.doPacket(var7);
               var0.rewind();
               var7.bb.put(var0);
               PacketTypes.PacketType.InvMngGetItem.send(var6);
               break;
            }
         }

      }
   }

   static void receiveInvMngReqItem(ByteBuffer var0, UdpConnection var1, short var2) {
      int var3 = 0;
      String var4 = null;
      if (var0.get() == 1) {
         var4 = GameWindow.ReadString(var0);
      } else {
         var3 = var0.getInt();
      }

      short var5 = var0.getShort();
      short var6 = var0.getShort();
      IsoPlayer var7 = (IsoPlayer)IDToPlayerMap.get(var6);
      if (var7 != null) {
         for(int var8 = 0; var8 < udpEngine.connections.size(); ++var8) {
            UdpConnection var9 = (UdpConnection)udpEngine.connections.get(var8);
            if (var9.getConnectedGUID() != var1.getConnectedGUID() && var9.getConnectedGUID() == (Long)PlayerToAddressMap.get(var7)) {
               ByteBufferWriter var10 = var9.startPacket();
               PacketTypes.PacketType.InvMngReqItem.doPacket(var10);
               if (var4 != null) {
                  var10.putByte((byte)1);
                  var10.putUTF(var4);
               } else {
                  var10.putByte((byte)0);
                  var10.putInt(var3);
               }

               var10.putShort(var5);
               PacketTypes.PacketType.InvMngReqItem.send(var9);
               break;
            }
         }

      }
   }

   static void receivePlayerStartPMChat(ByteBuffer var0, UdpConnection var1, short var2) {
      ChatServer.getInstance().processPlayerStartWhisperChatPacket(var0);
   }

   static void receiveStatistic(ByteBuffer var0, UdpConnection var1, short var2) {
      try {
         var1.statistic.parse(var0);
      } catch (Exception var4) {
         var4.printStackTrace();
      }

   }

   static void receiveStatisticRequest(ByteBuffer var0, UdpConnection var1, short var2) {
      if (!var1.role.haveCapability(Capability.GetStatistic) && !Core.bDebug) {
         DebugLog.General.error("User " + var1.getConnectedGUID() + " has no rights to access statistics.");
      } else {
         try {
            var1.statistic.enable = var0.get();
            sendStatistic(var1);
         } catch (Exception var4) {
            var4.printStackTrace();
         }

      }
   }

   static void receiveZombieSimulation(ByteBuffer var0, UdpConnection var1, short var2) {
      NetworkZombiePacker.getInstance().receivePacket(var0, var1);
   }

   public static void sendShortStatistic() {
      for(int var0 = 0; var0 < udpEngine.connections.size(); ++var0) {
         UdpConnection var1 = (UdpConnection)udpEngine.connections.get(var0);
         if (var1.statistic.enable == 3) {
            sendShortStatistic(var1);
         }
      }

   }

   public static void sendShortStatistic(UdpConnection var0) {
      try {
         ByteBufferWriter var1 = var0.startPacket();
         PacketTypes.PacketType.StatisticRequest.doPacket(var1);
         MPStatistic.getInstance().write(var1);
         PacketTypes.PacketType.StatisticRequest.send(var0);
      } catch (Exception var2) {
         var2.printStackTrace();
         var0.cancelPacket();
      }

   }

   public static void sendStatistic() {
      for(int var0 = 0; var0 < udpEngine.connections.size(); ++var0) {
         UdpConnection var1 = (UdpConnection)udpEngine.connections.get(var0);
         if (var1.statistic.enable == 1) {
            sendStatistic(var1);
         }
      }

   }

   public static void sendStatistic(UdpConnection var0) {
      ByteBufferWriter var1 = var0.startPacket();
      PacketTypes.PacketType.StatisticRequest.doPacket(var1);

      try {
         MPStatistic.getInstance().getStatisticTable(var1.bb);
         PacketTypes.PacketType.StatisticRequest.send(var0);
      } catch (IOException var3) {
         var3.printStackTrace();
         var0.cancelPacket();
      }

   }

   public static void getStatisticFromClients() {
      try {
         Iterator var0 = udpEngine.connections.iterator();

         while(var0.hasNext()) {
            UdpConnection var1 = (UdpConnection)var0.next();
            ByteBufferWriter var2 = var1.startPacket();
            PacketTypes.PacketType.Statistic.doPacket(var2);
            var2.putLong(System.currentTimeMillis());
            PacketTypes.PacketType.Statistic.send(var1);
         }
      } catch (Exception var3) {
         var3.printStackTrace();
      }

   }

   public static void updateZombieControl(IsoZombie var0, short var1, int var2) {
      if (var0.authOwner != null) {
         INetworkPacket.send(var0.authOwner, PacketTypes.PacketType.ZombieControl, var0, var1, var2);
      }

   }

   static void receivePacketCounts(ByteBuffer var0, UdpConnection var1, short var2) {
      ByteBufferWriter var3 = var1.startPacket();
      PacketTypes.PacketType.PacketCounts.doPacket(var3);
      var3.putInt(PacketTypes.packetTypes.size());
      Iterator var4 = PacketTypes.packetTypes.values().iterator();

      while(var4.hasNext()) {
         PacketTypes.PacketType var5 = (PacketTypes.PacketType)var4.next();
         var3.putShort(var5.getId());
         var3.putLong(var5.serverPacketCount);
      }

      PacketTypes.PacketType.PacketCounts.send(var1);
   }

   static void receiveSandboxOptions(ByteBuffer var0, UdpConnection var1, short var2) {
      try {
         SandboxOptions.instance.load(var0);
         SandboxOptions.instance.applySettings();
         SandboxOptions.instance.toLua();
         SandboxOptions.instance.saveServerLuaFile(ServerName);

         for(int var3 = 0; var3 < udpEngine.connections.size(); ++var3) {
            UdpConnection var4 = (UdpConnection)udpEngine.connections.get(var3);
            ByteBufferWriter var5 = var4.startPacket();
            PacketTypes.PacketType.SandboxOptions.doPacket(var5);
            var0.rewind();
            var5.bb.put(var0);
            PacketTypes.PacketType.SandboxOptions.send(var4);
         }
      } catch (Exception var6) {
         var6.printStackTrace();
      }

   }

   static void receiveChunkObjectState(ByteBuffer var0, UdpConnection var1, short var2) {
      short var3 = var0.getShort();
      short var4 = var0.getShort();
      IsoChunk var5 = ServerMap.instance.getChunk(var3, var4);
      if (var5 == null) {
         var1.chunkObjectState.add(var3);
         var1.chunkObjectState.add(var4);
      } else {
         ByteBufferWriter var6 = var1.startPacket();
         PacketTypes.PacketType.ChunkObjectState.doPacket(var6);
         var6.putShort(var3);
         var6.putShort(var4);

         try {
            if (var5.saveObjectState(var6.bb)) {
               PacketTypes.PacketType.ChunkObjectState.send(var1);
            } else {
               var1.cancelPacket();
            }
         } catch (Throwable var8) {
            var8.printStackTrace();
            var1.cancelPacket();
            return;
         }
      }

   }

   static void receiveSyncFaction(ByteBuffer var0, UdpConnection var1, short var2) {
      String var3 = GameWindow.ReadString(var0);
      String var4 = GameWindow.ReadString(var0);
      int var5 = var0.getInt();
      Faction var6 = Faction.getFaction(var3);
      boolean var7 = false;
      if (var6 == null) {
         var6 = new Faction(var3, var4);
         var7 = true;
         Faction.getFactions().add(var6);
      }

      var6.getPlayers().clear();
      if (var0.get() == 1) {
         var6.setTag(GameWindow.ReadString(var0));
         var6.setTagColor(new ColorInfo(var0.getFloat(), var0.getFloat(), var0.getFloat(), 1.0F));
      }

      for(int var8 = 0; var8 < var5; ++var8) {
         String var9 = GameWindow.ReadString(var0);
         var6.getPlayers().add(var9);
      }

      if (!var6.getOwner().equals(var4)) {
         var6.setOwner(var4);
      }

      boolean var12 = var0.get() == 1;
      if (ChatServer.isInited()) {
         if (var7) {
            ChatServer.getInstance().createFactionChat(var3);
         }

         if (var12) {
            ChatServer.getInstance().removeFactionChat(var3);
         } else {
            ChatServer.getInstance().syncFactionChatMembers(var3, var4, var6.getPlayers());
         }
      }

      if (var12) {
         Faction.getFactions().remove(var6);
         if (bServer || LuaManager.GlobalObject.isAdmin()) {
            DebugLog.log("faction: removed " + var3 + " owner=" + var6.getOwner());
         }
      }

      for(int var13 = 0; var13 < udpEngine.connections.size(); ++var13) {
         UdpConnection var10 = (UdpConnection)udpEngine.connections.get(var13);
         if (var1 == null || var10.getConnectedGUID() != var1.getConnectedGUID()) {
            ByteBufferWriter var11 = var10.startPacket();
            PacketTypes.PacketType.SyncFaction.doPacket(var11);
            var6.writeToBuffer(var11, var12);
            PacketTypes.PacketType.SyncFaction.send(var10);
         }
      }

   }

   public static void sendNonPvpZone(NonPvpZone var0, boolean var1, UdpConnection var2) {
      for(int var3 = 0; var3 < udpEngine.connections.size(); ++var3) {
         UdpConnection var4 = (UdpConnection)udpEngine.connections.get(var3);
         if (var2 == null || var4.getConnectedGUID() != var2.getConnectedGUID()) {
            ByteBufferWriter var5 = var4.startPacket();
            PacketTypes.PacketType.SyncNonPvpZone.doPacket(var5);
            var0.save(var5.bb);
            var5.putBoolean(var1);
            PacketTypes.PacketType.SyncNonPvpZone.send(var4);
         }
      }

   }

   static void receiveChangeTextColor(ByteBuffer var0, UdpConnection var1, short var2) {
      short var3 = var0.getShort();
      IsoPlayer var4 = getPlayerFromConnection(var1, var3);
      if (var4 != null) {
         float var5 = var0.getFloat();
         float var6 = var0.getFloat();
         float var7 = var0.getFloat();
         var4.setSpeakColourInfo(new ColorInfo(var5, var6, var7, 1.0F));

         for(int var8 = 0; var8 < udpEngine.connections.size(); ++var8) {
            UdpConnection var9 = (UdpConnection)udpEngine.connections.get(var8);
            if (var9.getConnectedGUID() != var1.getConnectedGUID()) {
               ByteBufferWriter var10 = var9.startPacket();
               PacketTypes.PacketType.ChangeTextColor.doPacket(var10);
               var10.putShort(var4.getOnlineID());
               var10.putFloat(var5);
               var10.putFloat(var6);
               var10.putFloat(var7);
               PacketTypes.PacketType.ChangeTextColor.send(var9);
            }
         }

      }
   }

   static void receiveSyncCompost(ByteBuffer var0, UdpConnection var1, short var2) {
      int var3 = var0.getInt();
      int var4 = var0.getInt();
      int var5 = var0.getInt();
      IsoGridSquare var6 = IsoWorld.instance.CurrentCell.getGridSquare(var3, var4, var5);
      if (var6 != null) {
         IsoCompost var7 = var6.getCompost();
         if (var7 == null) {
            assert var7 != null;

            var7 = new IsoCompost(var6.getCell(), var6, var7.getSpriteName());
            var6.AddSpecialObject(var7);
         }

         float var8 = var0.getFloat();
         var7.setCompost(var8);
         sendCompost(var7, var1);
      }

   }

   public static void sendCompost(IsoCompost var0, UdpConnection var1) {
      for(int var2 = 0; var2 < udpEngine.connections.size(); ++var2) {
         UdpConnection var3 = (UdpConnection)udpEngine.connections.get(var2);
         if (var3.RelevantTo((float)var0.square.x, (float)var0.square.y) && (var1 != null && var3.getConnectedGUID() != var1.getConnectedGUID() || var1 == null)) {
            ByteBufferWriter var4 = var3.startPacket();
            PacketTypes.PacketType.SyncCompost.doPacket(var4);
            var4.putInt(var0.square.x);
            var4.putInt(var0.square.y);
            var4.putInt(var0.square.z);
            var4.putFloat(var0.getCompost());
            PacketTypes.PacketType.SyncCompost.send(var3);
         }
      }

   }

   public static void sendHelicopter(float var0, float var1, boolean var2) {
      HelicopterPacket var3 = new HelicopterPacket();
      var3.set(var0, var1, var2);

      for(int var4 = 0; var4 < udpEngine.connections.size(); ++var4) {
         UdpConnection var5 = (UdpConnection)udpEngine.connections.get(var4);
         ByteBufferWriter var6 = var5.startPacket();
         PacketTypes.PacketType.Helicopter.doPacket(var6);
         var3.write(var6);
         PacketTypes.PacketType.Helicopter.send(var5);
      }

   }

   public static void sendZone(Zone var0) {
      for(int var1 = 0; var1 < udpEngine.connections.size(); ++var1) {
         UdpConnection var2 = (UdpConnection)udpEngine.connections.get(var1);
         ByteBufferWriter var3 = var2.startPacket();
         PacketTypes.PacketType.RegisterZone.doPacket(var3);
         var3.putUTF(var0.name);
         var3.putUTF(var0.type);
         var3.putInt(var0.x);
         var3.putInt(var0.y);
         var3.putInt(var0.z);
         var3.putInt(var0.w);
         var3.putInt(var0.h);
         var3.putInt(var0.lastActionTimestamp);
         PacketTypes.PacketType.RegisterZone.send(var2);
      }

   }

   static void receiveConstructedZone(ByteBuffer var0, UdpConnection var1, short var2) {
      int var3 = var0.getInt();
      int var4 = var0.getInt();
      int var5 = var0.getInt();
      Zone var6 = IsoWorld.instance.MetaGrid.getZoneAt(var3, var4, var5);
      if (var6 != null) {
         var6.setHaveConstruction(true);
      }

   }

   public static void addXp(IsoPlayer var0, PerkFactory.Perk var1, float var2) {
      addXp(var0, var1, var2, false);
   }

   public static void addXp(IsoPlayer var0, PerkFactory.Perk var1, float var2, boolean var3) {
      if (PlayerToAddressMap.containsKey(var0)) {
         long var4 = (Long)PlayerToAddressMap.get(var0);
         UdpConnection var6 = udpEngine.getActiveConnection(var4);
         if (var6 == null) {
            return;
         }

         INetworkPacket.processPacketOnServer(PacketTypes.PacketType.AddXP, var6, var0, var1, var2, var3);
      }

   }

   public static void addXpMultiplier(IsoPlayer var0, PerkFactory.Perk var1, float var2, int var3, int var4) {
      var0.getXp().addXpMultiplier(var1, var2, var3, var4);
      INetworkPacket.send(var0, PacketTypes.PacketType.AddXPMultiplier, var0, var1, var2, var3, var4);
   }

   private static void answerPing(ByteBuffer var0, UdpConnection var1) {
      String var2 = GameWindow.ReadString(var0);

      for(int var3 = 0; var3 < udpEngine.connections.size(); ++var3) {
         UdpConnection var4 = (UdpConnection)udpEngine.connections.get(var3);
         if (var4.getConnectedGUID() == var1.getConnectedGUID()) {
            ByteBufferWriter var5 = var4.startPacket();
            PacketTypes.PacketType.Ping.doPacket(var5);
            var5.putUTF(var2);
            var5.putInt(udpEngine.connections.size());
            var5.putInt(512);
            PacketTypes.PacketType.Ping.send(var4);
         }
      }

   }

   static void receiveUpdateItemSprite(ByteBuffer var0, UdpConnection var1, short var2) {
      int var3 = var0.getInt();
      String var4 = GameWindow.ReadStringUTF(var0);
      int var5 = var0.getInt();
      int var6 = var0.getInt();
      int var7 = var0.getInt();
      int var8 = var0.getInt();
      IsoGridSquare var9 = IsoWorld.instance.CurrentCell.getGridSquare(var5, var6, var7);
      if (var9 != null && var8 < var9.getObjects().size()) {
         try {
            IsoObject var10 = (IsoObject)var9.getObjects().get(var8);
            if (var10 != null) {
               var10.sprite = IsoSpriteManager.instance.getSprite(var3);
               if (var10.sprite == null && !var4.isEmpty()) {
                  var10.setSprite(var4);
               }

               var10.RemoveAttachedAnims();
               int var11 = var0.get() & 255;

               for(int var12 = 0; var12 < var11; ++var12) {
                  int var13 = var0.getInt();
                  IsoSprite var14 = IsoSpriteManager.instance.getSprite(var13);
                  if (var14 != null) {
                     var10.AttachExistingAnim(var14, 0, 0, false, 0, false, 0.0F);
                  }
               }

               var10.transmitUpdatedSpriteToClients(var1);
            }
         } catch (Exception var15) {
         }
      }

   }

   public static void startFireOnClient(IsoGridSquare var0, int var1, boolean var2, int var3, boolean var4) {
      INetworkPacket.sendToRelativeAndProcess(PacketTypes.PacketType.StartFire, var0.getX(), var0.getY(), var0, var2, var1, var3, var4);
   }

   public static void sendOptionsToClients() {
      for(int var0 = 0; var0 < udpEngine.connections.size(); ++var0) {
         UdpConnection var1 = (UdpConnection)udpEngine.connections.get(var0);
         INetworkPacket.send(var1, PacketTypes.PacketType.ReloadOptions);
      }

   }

   public static void sendBecomeCorpse(IsoDeadBody var0) {
      IsoGridSquare var1 = var0.getSquare();
      if (var1 != null) {
         BecomeCorpsePacket var2 = new BecomeCorpsePacket();
         var2.set(var0);

         for(int var3 = 0; var3 < udpEngine.connections.size(); ++var3) {
            UdpConnection var4 = (UdpConnection)udpEngine.connections.get(var3);
            if (var4.RelevantTo((float)var1.x, (float)var1.y)) {
               ByteBufferWriter var5 = var4.startPacket();
               PacketTypes.PacketType.BecomeCorpse.doPacket(var5);
               var2.write(var5);
               PacketTypes.PacketType.BecomeCorpse.send(var4);
            }
         }
      }

   }

   public static void sendCorpse(IsoDeadBody var0) {
      IsoGridSquare var1 = var0.getSquare();
      if (var1 != null) {
         AddCorpseToMapPacket var2 = new AddCorpseToMapPacket();
         var2.set(var1, var0);

         for(int var3 = 0; var3 < udpEngine.connections.size(); ++var3) {
            UdpConnection var4 = (UdpConnection)udpEngine.connections.get(var3);
            if (var4.RelevantTo((float)var1.x, (float)var1.y)) {
               ByteBufferWriter var5 = var4.startPacket();
               PacketTypes.PacketType.AddCorpseToMap.doPacket(var5);
               var2.write(var5);
               PacketTypes.PacketType.AddCorpseToMap.send(var4);
            }
         }
      }

   }

   static void receiveChatMessageFromPlayer(ByteBuffer var0, UdpConnection var1, short var2) {
      ChatServer.getInstance().processMessageFromPlayerPacket(var0);
   }

   public static void loadModData(IsoGridSquare var0) {
      if (var0.getModData().rawget("id") != null && var0.getModData().rawget("id") != null && (var0.getModData().rawget("remove") == null || ((String)var0.getModData().rawget("remove")).equals("false"))) {
         GameTime.getInstance().getModData().rawset("planting:" + ((Double)var0.getModData().rawget("id")).intValue() + ":x", (double)var0.getX());
         GameTime.getInstance().getModData().rawset("planting:" + ((Double)var0.getModData().rawget("id")).intValue() + ":y", (double)var0.getY());
         GameTime.getInstance().getModData().rawset("planting:" + ((Double)var0.getModData().rawget("id")).intValue() + ":z", (double)var0.getZ());
         GameTime.getInstance().getModData().rawset("planting:" + ((Double)var0.getModData().rawget("id")).intValue() + ":typeOfSeed", var0.getModData().rawget("typeOfSeed"));
         GameTime.getInstance().getModData().rawset("planting:" + ((Double)var0.getModData().rawget("id")).intValue() + ":nbOfGrow", (Double)var0.getModData().rawget("nbOfGrow"));
         GameTime.getInstance().getModData().rawset("planting:" + ((Double)var0.getModData().rawget("id")).intValue() + ":id", var0.getModData().rawget("id"));
         GameTime.getInstance().getModData().rawset("planting:" + ((Double)var0.getModData().rawget("id")).intValue() + ":waterLvl", var0.getModData().rawget("waterLvl"));
         GameTime.getInstance().getModData().rawset("planting:" + ((Double)var0.getModData().rawget("id")).intValue() + ":lastWaterHour", var0.getModData().rawget("lastWaterHour"));
         GameTime.getInstance().getModData().rawset("planting:" + ((Double)var0.getModData().rawget("id")).intValue() + ":waterNeeded", var0.getModData().rawget("waterNeeded"));
         GameTime.getInstance().getModData().rawset("planting:" + ((Double)var0.getModData().rawget("id")).intValue() + ":waterNeededMax", var0.getModData().rawget("waterNeededMax"));
         GameTime.getInstance().getModData().rawset("planting:" + ((Double)var0.getModData().rawget("id")).intValue() + ":mildewLvl", var0.getModData().rawget("mildewLvl"));
         GameTime.getInstance().getModData().rawset("planting:" + ((Double)var0.getModData().rawget("id")).intValue() + ":aphidLvl", var0.getModData().rawget("aphidLvl"));
         GameTime.getInstance().getModData().rawset("planting:" + ((Double)var0.getModData().rawget("id")).intValue() + ":fliesLvl", var0.getModData().rawget("fliesLvl"));
         GameTime.getInstance().getModData().rawset("planting:" + ((Double)var0.getModData().rawget("id")).intValue() + ":fertilizer", var0.getModData().rawget("fertilizer"));
         GameTime.getInstance().getModData().rawset("planting:" + ((Double)var0.getModData().rawget("id")).intValue() + ":nextGrowing", var0.getModData().rawget("nextGrowing"));
         GameTime.getInstance().getModData().rawset("planting:" + ((Double)var0.getModData().rawget("id")).intValue() + ":hasVegetable", var0.getModData().rawget("hasVegetable"));
         GameTime.getInstance().getModData().rawset("planting:" + ((Double)var0.getModData().rawget("id")).intValue() + ":hasSeed", var0.getModData().rawget("hasSeed"));
         GameTime.getInstance().getModData().rawset("planting:" + ((Double)var0.getModData().rawget("id")).intValue() + ":health", var0.getModData().rawget("health"));
         GameTime.getInstance().getModData().rawset("planting:" + ((Double)var0.getModData().rawget("id")).intValue() + ":badCare", var0.getModData().rawget("badCare"));
         GameTime.getInstance().getModData().rawset("planting:" + ((Double)var0.getModData().rawget("id")).intValue() + ":state", var0.getModData().rawget("state"));
         if (var0.getModData().rawget("hoursElapsed") != null) {
            GameTime.getInstance().getModData().rawset("hoursElapsed", var0.getModData().rawget("hoursElapsed"));
         }
      }

      ReceiveModDataPacket var1 = new ReceiveModDataPacket();
      var1.set(var0);

      for(int var2 = 0; var2 < udpEngine.connections.size(); ++var2) {
         UdpConnection var3 = (UdpConnection)udpEngine.connections.get(var2);
         if (var3.RelevantTo((float)var0.getX(), (float)var0.getY())) {
            ByteBufferWriter var4 = var3.startPacket();
            PacketTypes.PacketType.ReceiveModData.doPacket(var4);
            var1.write(var4);
            PacketTypes.PacketType.ReceiveModData.send(var3);
         }
      }

   }

   static void receiveSendModData(ByteBuffer var0, UdpConnection var1, short var2) {
      int var3 = var0.getInt();
      int var4 = var0.getInt();
      int var5 = var0.getInt();
      IsoGridSquare var6 = ServerMap.instance.getGridSquare(var3, var4, var5);
      if (var6 != null) {
         try {
            var6.getModData().load(var0, 219);
            if (var6.getModData().rawget("id") != null && (var6.getModData().rawget("remove") == null || ((String)var6.getModData().rawget("remove")).equals("false"))) {
               GameTime.getInstance().getModData().rawset("planting:" + ((Double)var6.getModData().rawget("id")).intValue() + ":x", (double)var6.getX());
               GameTime.getInstance().getModData().rawset("planting:" + ((Double)var6.getModData().rawget("id")).intValue() + ":y", (double)var6.getY());
               GameTime.getInstance().getModData().rawset("planting:" + ((Double)var6.getModData().rawget("id")).intValue() + ":z", (double)var6.getZ());
               GameTime.getInstance().getModData().rawset("planting:" + ((Double)var6.getModData().rawget("id")).intValue() + ":typeOfSeed", var6.getModData().rawget("typeOfSeed"));
               GameTime.getInstance().getModData().rawset("planting:" + ((Double)var6.getModData().rawget("id")).intValue() + ":nbOfGrow", (Double)var6.getModData().rawget("nbOfGrow"));
               GameTime.getInstance().getModData().rawset("planting:" + ((Double)var6.getModData().rawget("id")).intValue() + ":id", var6.getModData().rawget("id"));
               GameTime.getInstance().getModData().rawset("planting:" + ((Double)var6.getModData().rawget("id")).intValue() + ":waterLvl", var6.getModData().rawget("waterLvl"));
               GameTime.getInstance().getModData().rawset("planting:" + ((Double)var6.getModData().rawget("id")).intValue() + ":lastWaterHour", var6.getModData().rawget("lastWaterHour"));
               GameTime.getInstance().getModData().rawset("planting:" + ((Double)var6.getModData().rawget("id")).intValue() + ":waterNeeded", var6.getModData().rawget("waterNeeded"));
               GameTime.getInstance().getModData().rawset("planting:" + ((Double)var6.getModData().rawget("id")).intValue() + ":waterNeededMax", var6.getModData().rawget("waterNeededMax"));
               GameTime.getInstance().getModData().rawset("planting:" + ((Double)var6.getModData().rawget("id")).intValue() + ":mildewLvl", var6.getModData().rawget("mildewLvl"));
               GameTime.getInstance().getModData().rawset("planting:" + ((Double)var6.getModData().rawget("id")).intValue() + ":aphidLvl", var6.getModData().rawget("aphidLvl"));
               GameTime.getInstance().getModData().rawset("planting:" + ((Double)var6.getModData().rawget("id")).intValue() + ":fliesLvl", var6.getModData().rawget("fliesLvl"));
               GameTime.getInstance().getModData().rawset("planting:" + ((Double)var6.getModData().rawget("id")).intValue() + ":fertilizer", var6.getModData().rawget("fertilizer"));
               GameTime.getInstance().getModData().rawset("planting:" + ((Double)var6.getModData().rawget("id")).intValue() + ":nextGrowing", var6.getModData().rawget("nextGrowing"));
               GameTime.getInstance().getModData().rawset("planting:" + ((Double)var6.getModData().rawget("id")).intValue() + ":hasVegetable", var6.getModData().rawget("hasVegetable"));
               GameTime.getInstance().getModData().rawset("planting:" + ((Double)var6.getModData().rawget("id")).intValue() + ":hasSeed", var6.getModData().rawget("hasSeed"));
               GameTime.getInstance().getModData().rawset("planting:" + ((Double)var6.getModData().rawget("id")).intValue() + ":health", var6.getModData().rawget("health"));
               GameTime.getInstance().getModData().rawset("planting:" + ((Double)var6.getModData().rawget("id")).intValue() + ":badCare", var6.getModData().rawget("badCare"));
               GameTime.getInstance().getModData().rawset("planting:" + ((Double)var6.getModData().rawget("id")).intValue() + ":state", var6.getModData().rawget("state"));
               if (var6.getModData().rawget("hoursElapsed") != null) {
                  GameTime.getInstance().getModData().rawset("hoursElapsed", var6.getModData().rawget("hoursElapsed"));
               }
            }

            LuaEventManager.triggerEvent("onLoadModDataFromServer", var6);

            for(int var7 = 0; var7 < udpEngine.connections.size(); ++var7) {
               UdpConnection var8 = (UdpConnection)udpEngine.connections.get(var7);
               if (var8.RelevantTo((float)var6.getX(), (float)var6.getY()) && (var1 == null || var8.getConnectedGUID() != var1.getConnectedGUID())) {
                  ByteBufferWriter var9 = var8.startPacket();
                  PacketTypes.PacketType.ReceiveModData.doPacket(var9);
                  var9.putInt(var3);
                  var9.putInt(var4);
                  var9.putInt(var5);

                  try {
                     var6.getModData().save(var9.bb);
                  } catch (IOException var11) {
                     var11.printStackTrace();
                  }

                  PacketTypes.PacketType.ReceiveModData.send(var8);
               }
            }
         } catch (IOException var12) {
            var12.printStackTrace();
         }

      }
   }

   static void receiveWeaponHit(ByteBuffer var0, UdpConnection var1, short var2) {
      IsoObject var3 = getIsoObjectRefFromByteBuffer(var0);
      short var4 = var0.getShort();
      String var5 = GameWindow.ReadStringUTF(var0);
      IsoPlayer var6 = getPlayerFromConnection(var1, var4);
      if (var3 != null && var6 != null) {
         InventoryItem var7 = null;
         if (!var5.isEmpty()) {
            var7 = InventoryItemFactory.CreateItem(var5);
            if (!(var7 instanceof HandWeapon)) {
               return;
            }
         }

         if (var7 == null && !(var3 instanceof IsoWindow)) {
            return;
         }

         int var8 = (int)var3.getX();
         int var9 = (int)var3.getY();
         int var10 = (int)var3.getZ();
         if (var3 instanceof IsoDoor) {
            ((IsoDoor)var3).WeaponHit(var6, (HandWeapon)var7);
         } else if (var3 instanceof IsoThumpable) {
            ((IsoThumpable)var3).WeaponHit(var6, (HandWeapon)var7);
         } else if (var3 instanceof IsoWindow) {
            ((IsoWindow)var3).WeaponHit(var6, (HandWeapon)var7);
         } else if (var3 instanceof IsoBarricade) {
            ((IsoBarricade)var3).WeaponHit(var6, (HandWeapon)var7);
         }

         if (var3.getObjectIndex() == -1) {
            ZLogger var10000 = LoggerManager.getLogger("map");
            String var10001 = var1.idStr;
            var10000.write(var10001 + " \"" + var1.username + "\" destroyed " + (var3.getName() != null ? var3.getName() : var3.getObjectName()) + " with " + (var5.isEmpty() ? "BareHands" : var5) + " at " + var8 + "," + var9 + "," + var10);
         }
      }

   }

   private static void putIsoObjectRefToByteBuffer(IsoObject var0, ByteBuffer var1) {
      var1.putInt(var0.square.x);
      var1.putInt(var0.square.y);
      var1.putInt(var0.square.z);
      var1.put((byte)var0.square.getObjects().indexOf(var0));
   }

   private static IsoObject getIsoObjectRefFromByteBuffer(ByteBuffer var0) {
      int var1 = var0.getInt();
      int var2 = var0.getInt();
      int var3 = var0.getInt();
      byte var4 = var0.get();
      IsoGridSquare var5 = ServerMap.instance.getGridSquare(var1, var2, var3);
      return var5 != null && var4 >= 0 && var4 < var5.getObjects().size() ? (IsoObject)var5.getObjects().get(var4) : null;
   }

   static void receiveDrink(ByteBuffer var0, UdpConnection var1, short var2) {
      byte var3 = var0.get();
      float var4 = var0.getFloat();
      IsoPlayer var5 = getPlayerFromConnection(var1, var3);
      if (var5 != null) {
         Stats var10000 = var5.getStats();
         var10000.thirst -= var4;
         if (var5.getStats().thirst < 0.0F) {
            var5.getStats().thirst = 0.0F;
         }
      }

   }

   static void receiveEatFood(ByteBuffer var0, UdpConnection var1, short var2) {
      byte var3 = var0.get();
      float var4 = var0.getFloat();
      InventoryItem var5 = null;

      try {
         var5 = InventoryItem.loadItem(var0, 219);
      } catch (Exception var7) {
         var7.printStackTrace();
      }

      if (var5 instanceof Food) {
         IsoPlayer var6 = getPlayerFromConnection(var1, var3);
         if (var6 != null) {
            var6.Eat(var5, var4);
         }
      }

   }

   static void receivePingFromClient(ByteBuffer var0, UdpConnection var1, short var2) {
      long var3 = var0.getLong();
      if (var3 == -1L) {
         DebugLog.Multiplayer.warn("Player \"%s\" toggled lua debugger", var1.getConnectedGUID());
      } else {
         if (var1.role != Roles.getDefaultForAdmin()) {
            return;
         }

         ByteBufferWriter var5 = var1.startPacket();

         try {
            PacketTypes.PacketType.PingFromClient.doPacket(var5);
            var5.putLong(var3);
            MPStatistics.write(var1, var5.bb);
            PacketTypes.PacketType.PingFromClient.send(var1);
            MPStatistics.requested();
         } catch (Exception var7) {
            var1.cancelPacket();
         }
      }

   }

   static void receiveReceiveCommand(ByteBuffer var0, UdpConnection var1, short var2) {
      String var3 = GameWindow.ReadString(var0);
      String var4 = null;
      var4 = handleClientCommand(var3.substring(1), var1);
      if (var4 == null) {
         var4 = handleServerCommand(var3.substring(1), var1);
      }

      if (var4 == null) {
         var4 = "Unknown command " + var3;
      }

      if (!var3.substring(1).startsWith("roll") && !var3.substring(1).startsWith("card")) {
         ChatServer.getInstance().sendMessageToServerChat(var1, var4);
      } else {
         ChatServer.getInstance().sendMessageToServerChat(var1, var4);
      }

   }

   private static String handleClientCommand(String var0, UdpConnection var1) {
      if (var0 == null) {
         return null;
      } else {
         ArrayList var2 = new ArrayList();
         Matcher var3 = Pattern.compile("([^\"]\\S*|\".*?\")\\s*").matcher(var0);

         while(var3.find()) {
            var2.add(var3.group(1).replace("\"", ""));
         }

         int var4 = var2.size();
         String[] var5 = (String[])var2.toArray(new String[var4]);
         String var6 = var4 > 0 ? var5[0].toLowerCase() : "";
         String var10000;
         if (var6.equals("card")) {
            PlayWorldSoundServer("ChatDrawCard", false, getAnyPlayerFromConnection(var1).getCurrentSquare(), 0.0F, 3.0F, 1.0F, false);
            var10000 = var1.username;
            return var10000 + " drew " + ServerOptions.getRandomCard();
         } else if (var6.equals("roll")) {
            if (var4 != 2) {
               return (String)ServerOptions.clientOptionsList.get("roll");
            } else {
               boolean var13 = false;

               try {
                  int var14 = Integer.parseInt(var5[1]);
                  PlayWorldSoundServer("ChatRollDice", false, getAnyPlayerFromConnection(var1).getCurrentSquare(), 0.0F, 3.0F, 1.0F, false);
                  var10000 = var1.username;
                  return var10000 + " rolls a " + var14 + "-sided dice and obtains " + Rand.Next(var14);
               } catch (Exception var10) {
                  return (String)ServerOptions.clientOptionsList.get("roll");
               }
            }
         } else if (var6.equals("changepwd")) {
            if (var4 == 3) {
               String var12 = var5[1];
               String var8 = var5[2];

               try {
                  return ServerWorldDatabase.instance.changePwd(var1.username, var12.trim(), var8.trim());
               } catch (SQLException var11) {
                  var11.printStackTrace();
                  return "A SQL error occured";
               }
            } else {
               return (String)ServerOptions.clientOptionsList.get("changepwd");
            }
         } else if (var6.equals("dragons")) {
            return "Sorry, you don't have the required materials.";
         } else if (var6.equals("dance")) {
            return "Stop kidding me...";
         } else if (var6.equals("safehouse")) {
            if (var4 == 2 && var1 != null) {
               if (!ServerOptions.instance.PlayerSafehouse.getValue() && !ServerOptions.instance.AdminSafehouse.getValue()) {
                  return "Safehouses are disabled on this server.";
               } else if ("release".equals(var5[1])) {
                  SafeHouse var7 = SafeHouse.hasSafehouse(var1.username);
                  if (var7 == null) {
                     return "You don't have a safehouse.";
                  } else if (!var7.isOwner(var1.username)) {
                     return "Only owner can release safehouse";
                  } else if (!ServerOptions.instance.PlayerSafehouse.getValue() && !var1.role.haveCapability(Capability.CanSetupSafehouses)) {
                     return "Only admin or moderator may release safehouses";
                  } else {
                     SafeHouse.removeSafeHouse(var7);
                     return "Safehouse released";
                  }
               } else {
                  return (String)ServerOptions.clientOptionsList.get("safehouse");
               }
            } else {
               return (String)ServerOptions.clientOptionsList.get("safehouse");
            }
         } else {
            return null;
         }
      }
   }

   private static void PlayWorldSound(String var0, IsoGridSquare var1, float var2, int var3) {
      if (bServer && var1 != null) {
         int var4 = var1.getX();
         int var5 = var1.getY();
         int var6 = var1.getZ();
         PlayWorldSoundPacket var7 = new PlayWorldSoundPacket();
         var7.set(var0, var4, var5, (byte)var6, var3);
         DebugType var10000 = DebugType.Sound;
         String var10001 = var7.getDescription();
         DebugLog.log(var10000, "sending " + var10001 + " radius=" + var2);

         for(int var8 = 0; var8 < udpEngine.connections.size(); ++var8) {
            UdpConnection var9 = (UdpConnection)udpEngine.connections.get(var8);
            IsoPlayer var10 = getAnyPlayerFromConnection(var9);
            if (var10 != null && var9.RelevantTo((float)var4, (float)var5, var2 * 2.0F)) {
               ByteBufferWriter var11 = var9.startPacket();
               PacketTypes.PacketType.PlayWorldSound.doPacket(var11);
               var7.write(var11);
               PacketTypes.PacketType.PlayWorldSound.send(var9);
            }
         }

      }
   }

   public static void PlayWorldSoundServer(String var0, IsoGridSquare var1, float var2, int var3) {
      PlayWorldSound(var0, var1, var2, var3);
   }

   public static void PlayWorldSoundServer(String var0, boolean var1, IsoGridSquare var2, float var3, float var4, float var5, boolean var6) {
      PlayWorldSound(var0, var2, var4, -1);
   }

   public static void PlayWorldSoundServer(IsoGameCharacter var0, String var1, boolean var2, IsoGridSquare var3, float var4, float var5, float var6, boolean var7) {
      if (var0 == null || !var0.isInvisible() || DebugOptions.instance.Character.Debug.PlaySoundWhenInvisible.getValue()) {
         PlayWorldSound(var1, var3, var5, -1);
      }
   }

   public static void PlayWorldSoundWavServer(String var0, boolean var1, IsoGridSquare var2, float var3, float var4, float var5, boolean var6) {
      PlayWorldSound(var0, var2, var4, -1);
   }

   public static void PlaySoundAtEveryPlayer(String var0, int var1, int var2, int var3) {
      PlaySoundAtEveryPlayer(var0, var1, var2, var3, false);
   }

   public static void PlaySoundAtEveryPlayer(String var0) {
      PlaySoundAtEveryPlayer(var0, -1, -1, -1, true);
   }

   public static void PlaySoundAtEveryPlayer(String var0, int var1, int var2, int var3, boolean var4) {
      if (bServer) {
         if (var4) {
            DebugLog.log(DebugType.Sound, "sound: sending " + var0 + " at every player (using player location)");
         } else {
            DebugLog.log(DebugType.Sound, "sound: sending " + var0 + " at every player location x=" + var1 + " y=" + var2);
         }

         for(int var5 = 0; var5 < udpEngine.connections.size(); ++var5) {
            UdpConnection var6 = (UdpConnection)udpEngine.connections.get(var5);
            IsoPlayer var7 = getAnyPlayerFromConnection(var6);
            if (var7 != null && !var7.isDeaf()) {
               if (var4) {
                  var1 = PZMath.fastfloor(var7.getX());
                  var2 = PZMath.fastfloor(var7.getY());
                  var3 = PZMath.fastfloor(var7.getZ());
               }

               ByteBufferWriter var8 = var6.startPacket();
               PacketTypes.PacketType.PlaySoundEveryPlayer.doPacket(var8);
               var8.putUTF(var0);
               var8.putInt(var1);
               var8.putInt(var2);
               var8.putInt(var3);
               PacketTypes.PacketType.PlaySoundEveryPlayer.send(var6);
            }
         }

      }
   }

   public static void sendZombieSound(IsoZombie.ZombieSound var0, IsoZombie var1) {
      float var2 = (float)var0.radius();
      DebugLog.log(DebugType.Sound, "sound: sending zombie sound " + var0);

      for(int var3 = 0; var3 < udpEngine.connections.size(); ++var3) {
         UdpConnection var4 = (UdpConnection)udpEngine.connections.get(var3);
         if (var4.isFullyConnected() && var4.RelevantTo(var1.getX(), var1.getY(), var2)) {
            ByteBufferWriter var5 = var4.startPacket();
            PacketTypes.PacketType.ZombieSound.doPacket(var5);
            var5.putShort(var1.OnlineID);
            var5.putByte((byte)var0.ordinal());
            PacketTypes.PacketType.ZombieSound.send(var4);
         }
      }

   }

   public static boolean helmetFall(IsoGameCharacter var0, boolean var1) {
      InventoryItem var2 = PersistentOutfits.instance.processFallingHat(var0, var1);
      if (var2 == null) {
         return false;
      } else {
         float var3 = var0.getX() + 0.6F;
         float var4 = var0.getY() + 0.6F;
         IsoGridSquare var5 = IsoWorld.instance.CurrentCell.getGridSquare((double)var3, (double)var4, (double)var0.getZ());
         var5.AddWorldInventoryItem(var2, var3 % 1.0F, var4 % 1.0F, var0.getZ(), false);
         ZombieHelmetFallingPacket var6 = new ZombieHelmetFallingPacket();
         var6.set(var0, var2, var3, var4, var0.getZ());

         for(int var7 = 0; var7 < udpEngine.connections.size(); ++var7) {
            UdpConnection var8 = (UdpConnection)udpEngine.connections.get(var7);
            if (var8.isFullyConnected() && var8.RelevantTo(var3, var4)) {
               try {
                  ByteBufferWriter var9 = var8.startPacket();
                  PacketTypes.PacketType.ZombieHelmetFalling.doPacket(var9);
                  var6.write(var9);
                  PacketTypes.PacketType.ZombieHelmetFalling.send(var8);
               } catch (Throwable var10) {
                  var8.cancelPacket();
                  ExceptionLogger.logException(var10);
               }
            }
         }

         return true;
      }
   }

   public static void initClientCommandFilter() {
      String var0 = ServerOptions.getInstance().ClientCommandFilter.getValue();
      ccFilters.clear();
      String[] var1 = var0.split(";");
      String[] var2 = var1;
      int var3 = var1.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         String var5 = var2[var4];
         if (!var5.isEmpty() && var5.contains(".") && (var5.startsWith("+") || var5.startsWith("-"))) {
            String[] var6 = var5.split("\\.");
            if (var6.length == 2) {
               String var7 = var6[0].substring(1);
               String var8 = var6[1];
               CCFilter var9 = new CCFilter();
               var9.command = var8;
               var9.allow = var6[0].startsWith("+");
               var9.next = (CCFilter)ccFilters.get(var7);
               ccFilters.put(var7, var9);
            }
         }
      }

   }

   static void receiveClientCommand(ByteBuffer var0, UdpConnection var1, short var2) {
      byte var3 = var0.get();
      String var4 = GameWindow.ReadString(var0);
      String var5 = GameWindow.ReadString(var0);
      boolean var6 = var0.get() == 1;
      KahluaTable var7 = null;
      if (var6) {
         var7 = LuaManager.platform.newTable();

         try {
            TableNetworkUtils.load(var7, var0);
         } catch (Exception var10) {
            var10.printStackTrace();
            return;
         }
      }

      IsoPlayer var8 = getPlayerFromConnection(var1, var3);
      if (var3 == -1) {
         var8 = getAnyPlayerFromConnection(var1);
      }

      if (var8 == null) {
         DebugLog.log("receiveClientCommand: player is null");
      } else {
         CCFilter var9 = (CCFilter)ccFilters.get(var4);
         if (var9 == null || var9.passes(var5)) {
            ZLogger var10000 = LoggerManager.getLogger("cmd");
            String var10001 = var1.idStr;
            var10000.write(var10001 + " \"" + var8.username + "\" " + var4 + "." + var5 + " @ " + (int)var8.getX() + "," + (int)var8.getY() + "," + (int)var8.getZ());
         }

         if (!"vehicle".equals(var4) || !"remove".equals(var5) || Core.bDebug || var1.role.haveCapability(Capability.GeneralCheats) || var8.networkAI.isDismantleAllowed()) {
            LuaEventManager.triggerEvent("OnClientCommand", var4, var5, var8, var7);
         }
      }
   }

   static void receiveWorldMap(ByteBuffer var0, UdpConnection var1, short var2) throws IOException {
      WorldMapServer.instance.receive(var0, var1);
   }

   public static IsoPlayer getAnyPlayerFromConnection(UdpConnection var0) {
      for(int var1 = 0; var1 < 4; ++var1) {
         if (var0.players[var1] != null) {
            return var0.players[var1];
         }
      }

      return null;
   }

   public static IsoPlayer getPlayerFromConnection(UdpConnection var0, int var1) {
      return var1 >= 0 && var1 < 4 ? var0.players[var1] : null;
   }

   public static IsoPlayer getPlayerByRealUserName(String var0) {
      for(int var1 = 0; var1 < udpEngine.connections.size(); ++var1) {
         UdpConnection var2 = (UdpConnection)udpEngine.connections.get(var1);

         for(int var3 = 0; var3 < 4; ++var3) {
            IsoPlayer var4 = var2.players[var3];
            if (var4 != null && var4.username.equals(var0)) {
               return var4;
            }
         }
      }

      return null;
   }

   public static IsoPlayer getPlayerByUserName(String var0) {
      for(int var1 = 0; var1 < udpEngine.connections.size(); ++var1) {
         UdpConnection var2 = (UdpConnection)udpEngine.connections.get(var1);

         for(int var3 = 0; var3 < 4; ++var3) {
            IsoPlayer var4 = var2.players[var3];
            if (var4 != null && (var4.getDisplayName().equals(var0) || var4.getUsername().equals(var0))) {
               return var4;
            }
         }
      }

      return null;
   }

   public static IsoPlayer getPlayerByUserNameForCommand(String var0) {
      for(int var1 = 0; var1 < udpEngine.connections.size(); ++var1) {
         UdpConnection var2 = (UdpConnection)udpEngine.connections.get(var1);

         for(int var3 = 0; var3 < 4; ++var3) {
            IsoPlayer var4 = var2.players[var3];
            if (var4 != null && (var4.getUsername().toLowerCase().equals(var0.toLowerCase()) || var4.getUsername().toLowerCase().startsWith(var0.toLowerCase()))) {
               return var4;
            }
         }
      }

      return null;
   }

   public static UdpConnection getConnectionByPlayerOnlineID(short var0) {
      return udpEngine.getActiveConnection((Long)IDToAddressMap.get(var0));
   }

   public static UdpConnection getConnectionFromPlayer(IsoPlayer var0) {
      Long var1 = (Long)PlayerToAddressMap.get(var0);
      return var1 == null ? null : udpEngine.getActiveConnection(var1);
   }

   public static void sendAddItemToContainer(ItemContainer var0, InventoryItem var1) {
      if (var0.getCharacter() instanceof IsoPlayer) {
         INetworkPacket.send((IsoPlayer)var0.getCharacter(), PacketTypes.PacketType.AddInventoryItemToContainer, var0, var1);
      } else if (var0.getParent() != null) {
         INetworkPacket.sendToRelative(PacketTypes.PacketType.AddInventoryItemToContainer, (float)((int)var0.getParent().getX()), (float)((int)var0.getParent().getY()), var0, var1);
      } else if (var0.inventoryContainer != null && var0.inventoryContainer.getWorldItem() != null) {
         INetworkPacket.sendToRelative(PacketTypes.PacketType.AddInventoryItemToContainer, (float)((int)var0.inventoryContainer.getWorldItem().getX()), (float)((int)var0.inventoryContainer.getWorldItem().getY()), var0, var1);
      }

   }

   public static void sendAddItemsToContainer(ItemContainer var0, ArrayList<InventoryItem> var1) {
      if (var0.getCharacter() instanceof IsoPlayer) {
         INetworkPacket.send((IsoPlayer)var0.getCharacter(), PacketTypes.PacketType.AddInventoryItemToContainer, var0, var1);
      } else if (var0.getParent() != null) {
         INetworkPacket.sendToRelative(PacketTypes.PacketType.AddInventoryItemToContainer, (float)((int)var0.getParent().getX()), (float)((int)var0.getParent().getY()), var0, var1);
      } else if (var0.inventoryContainer != null && var0.inventoryContainer.getWorldItem() != null) {
         INetworkPacket.sendToRelative(PacketTypes.PacketType.AddInventoryItemToContainer, (float)((int)var0.inventoryContainer.getWorldItem().getX()), (float)((int)var0.inventoryContainer.getWorldItem().getY()), var0, var1);
      }

   }

   public static void sendReplaceItemInContainer(ItemContainer var0, InventoryItem var1, InventoryItem var2) {
      if (var0.getCharacter() instanceof IsoPlayer) {
         INetworkPacket.send((IsoPlayer)var0.getCharacter(), PacketTypes.PacketType.ReplaceInventoryItemInContainer, var0, var1, var2);
      } else if (var0.getParent() != null) {
         INetworkPacket.sendToRelative(PacketTypes.PacketType.ReplaceInventoryItemInContainer, (float)((int)var0.getParent().getX()), (float)((int)var0.getParent().getY()), var0, var1, var2);
      } else if (var0.inventoryContainer != null && var0.inventoryContainer.getWorldItem() != null) {
         INetworkPacket.sendToRelative(PacketTypes.PacketType.ReplaceInventoryItemInContainer, (float)((int)var0.inventoryContainer.getWorldItem().getX()), (float)((int)var0.inventoryContainer.getWorldItem().getY()), var0, var1, var2);
      }

   }

   public static void sendRemoveItemFromContainer(ItemContainer var0, InventoryItem var1) {
      if (var0.getCharacter() instanceof IsoPlayer) {
         INetworkPacket.send((IsoPlayer)var0.getCharacter(), PacketTypes.PacketType.RemoveInventoryItemFromContainer, var0, var1);
      } else if (var0.getParent() != null) {
         INetworkPacket.sendToRelative(PacketTypes.PacketType.RemoveInventoryItemFromContainer, (float)((int)var0.getParent().getX()), (float)((int)var0.getParent().getY()), var0, var1);
      } else if (var0.inventoryContainer != null && var0.inventoryContainer.getWorldItem() != null) {
         INetworkPacket.sendToRelative(PacketTypes.PacketType.RemoveInventoryItemFromContainer, (float)((int)var0.inventoryContainer.getWorldItem().getX()), (float)((int)var0.inventoryContainer.getWorldItem().getY()), var0, var1);
      }

   }

   public static void sendRemoveItemsFromContainer(ItemContainer var0, ArrayList<InventoryItem> var1) {
      if (var0.getCharacter() instanceof IsoPlayer) {
         INetworkPacket.send((IsoPlayer)var0.getCharacter(), PacketTypes.PacketType.RemoveInventoryItemFromContainer, var0, var1);
      } else if (var0.getParent() != null) {
         INetworkPacket.sendToRelative(PacketTypes.PacketType.RemoveInventoryItemFromContainer, (float)((int)var0.getParent().getX()), (float)((int)var0.getParent().getY()), var0, var1);
      } else if (var0.inventoryContainer != null && var0.inventoryContainer.getWorldItem() != null) {
         INetworkPacket.sendToRelative(PacketTypes.PacketType.RemoveInventoryItemFromContainer, (float)((int)var0.inventoryContainer.getWorldItem().getX()), (float)((int)var0.inventoryContainer.getWorldItem().getY()), var0, var1);
      }

   }

   public static void sendSyncPlayerFields(IsoPlayer var0, byte var1) {
      if (var0 != null && var0.OnlineID != -1) {
         INetworkPacket.send(var0, PacketTypes.PacketType.syncPlayerFields, var0, var1);
      }
   }

   public static void sendSyncClothing(IsoPlayer var0, String var1, InventoryItem var2) {
      if (var0 != null && var0.OnlineID != -1) {
         INetworkPacket.sendToRelative(PacketTypes.PacketType.SyncClothing, var0.getX(), var0.getY(), var0);
      }

   }

   public static void syncVisuals(IsoPlayer var0) {
      if (var0 != null && var0.OnlineID != -1) {
         SyncVisualsPacket var1 = new SyncVisualsPacket();
         var1.set(var0);

         for(int var2 = 0; var2 < udpEngine.connections.size(); ++var2) {
            UdpConnection var3 = (UdpConnection)udpEngine.connections.get(var2);
            if (var3.RelevantTo(var0.getX(), var0.getY())) {
               ByteBufferWriter var4 = var3.startPacket();
               PacketTypes.PacketType.SyncVisuals.doPacket(var4);
               var1.write(var4);
               PacketTypes.PacketType.SyncVisuals.send(var3);
            }
         }

      }
   }

   public static void sendItemsInContainer(IsoObject var0, ItemContainer var1) {
      if (udpEngine != null) {
         if (var1 == null) {
            DebugLog.log("sendItemsInContainer: container is null");
         } else {
            if (var0 instanceof IsoWorldInventoryObject) {
               IsoWorldInventoryObject var2 = (IsoWorldInventoryObject)var0;
               if (!(var2.getItem() instanceof InventoryContainer)) {
                  DebugLog.log("sendItemsInContainer: IsoWorldInventoryObject item isn't a container");
                  return;
               }

               InventoryContainer var3 = (InventoryContainer)var2.getItem();
               if (var3.getInventory() != var1) {
                  DebugLog.log("sendItemsInContainer: wrong container for IsoWorldInventoryObject");
                  return;
               }
            } else if (var0 instanceof BaseVehicle) {
               if (var1.vehiclePart == null || var1.vehiclePart.getItemContainer() != var1 || var1.vehiclePart.getVehicle() != var0) {
                  DebugLog.log("sendItemsInContainer: wrong container for BaseVehicle");
                  return;
               }
            } else if (var0 instanceof IsoDeadBody) {
               if (var1 != var0.getContainer()) {
                  DebugLog.log("sendItemsInContainer: wrong container for IsoDeadBody");
                  return;
               }
            } else if (var0.getContainerIndex(var1) == -1) {
               DebugLog.log("sendItemsInContainer: wrong container for IsoObject");
               return;
            }

            if (var0 != null && var1 != null && !var1.getItems().isEmpty()) {
               INetworkPacket.sendToRelative(PacketTypes.PacketType.AddInventoryItemToContainer, (float)var0.square.x, (float)var0.square.y, var1, var1.getItems());
            }
         }
      }
   }

   public static void addConnection(UdpConnection var0) {
      synchronized(MainLoopNetDataHighPriorityQ) {
         MainLoopNetDataHighPriorityQ.add(new DelayedConnection(var0, true));
      }
   }

   public static void addDisconnect(UdpConnection var0) {
      synchronized(MainLoopNetDataHighPriorityQ) {
         MainLoopNetDataHighPriorityQ.add(new DelayedConnection(var0, false));
      }
   }

   public static void addDelayedDisconnect(UdpConnection var0) {
      synchronized(MainLoopDelayedDisconnectQ) {
         MainLoopDelayedDisconnectQ.put(var0.username, new DelayedConnection(var0, false));
      }
   }

   public static void doDelayedDisconnect(IsoPlayer var0) {
      synchronized(MainLoopDelayedDisconnectQ) {
         DelayedConnection var2 = (DelayedConnection)MainLoopDelayedDisconnectQ.remove(var0.username);
         if (var2 != null) {
            var2.disconnect();
         }

      }
   }

   public static boolean isDelayedDisconnect(UdpConnection var0) {
      return var0 != null && var0.username != null ? MainLoopDelayedDisconnectQ.containsKey(var0.username) : false;
   }

   public static boolean isDelayedDisconnect(IsoPlayer var0) {
      return var0 != null && var0.username != null ? MainLoopDelayedDisconnectQ.containsKey(var0.username) : false;
   }

   public static void disconnectPlayer(IsoPlayer var0, UdpConnection var1) {
      if (var0 != null) {
         SafetySystemManager.storeSafety(var0);
         ChatServer.getInstance().disconnectPlayer(var0.getOnlineID());
         if (var0.getVehicle() != null) {
            VehiclesDB2.instance.updateVehicleAndTrailer(var0.getVehicle());
            if (var0.getVehicle().isDriver(var0) && var0.getVehicle().isNetPlayerId(var0.getOnlineID())) {
               var0.getVehicle().setNetPlayerAuthorization(BaseVehicle.Authorization.Server, -1);
               var0.getVehicle().getController().clientForce = 0.0F;
               var0.getVehicle().jniLinearVelocity.set(0.0F, 0.0F, 0.0F);
            }

            int var2 = var0.getVehicle().getSeat(var0);
            if (var2 != -1) {
               var0.getVehicle().clearPassenger(var2);
            }
         }

         NetworkZombieManager.getInstance().clearTargetAuth(var1, var0);
         var0.removeFromWorld();
         var0.removeFromSquare();
         PlayerToAddressMap.remove(var0);
         IDToAddressMap.remove(var0.OnlineID);
         IDToPlayerMap.remove(var0.OnlineID);
         Players.remove(var0);
         SafeHouse.updateSafehousePlayersConnected();
         SafeHouse var5 = SafeHouse.hasSafehouse(var0);
         if (var5 != null && var5.isOwner(var0)) {
            Iterator var3 = IDToPlayerMap.values().iterator();

            while(var3.hasNext()) {
               IsoPlayer var4 = (IsoPlayer)var3.next();
               var5.checkTrespass(var4);
            }
         }

         var1.usernames[var0.PlayerIndex] = null;
         var1.players[var0.PlayerIndex] = null;
         var1.playerIDs[var0.PlayerIndex] = -1;
         var1.ReleventPos[var0.PlayerIndex] = null;
         var1.connectArea[var0.PlayerIndex] = null;
         INetworkPacket.sendToAll(PacketTypes.PacketType.PlayerTimeout, (UdpConnection)null, var0);
         ServerLOS.instance.removePlayer(var0);
         ZombiePopulationManager.instance.updateLoadedAreas();
         DebugLogStream var10000 = DebugLog.DetailedInfo;
         String var10001 = var0.getDisplayName();
         var10000.trace("Disconnected player \"" + var10001 + "\" " + var1.getConnectedGUID());
         ZLogger var6 = LoggerManager.getLogger("user");
         var10001 = var1.idStr;
         var6.write(var10001 + " \"" + var0.getUsername() + "\" disconnected player " + LoggerManager.getPlayerCoords(var0));
         SteamGameServer.RemovePlayer(var0);
      }
   }

   public static void heartBeat() {
      ++count;
   }

   public static short getFreeSlot() {
      for(short var0 = 0; var0 < udpEngine.getMaxConnections(); ++var0) {
         if (SlotToConnection[var0] == null) {
            return var0;
         }
      }

      return -1;
   }

   public static void receiveClientConnect(UdpConnection var0, ServerWorldDatabase.LogonResult var1) {
      ConnectionManager.log("receive-packet", "client-connect", var0);
      short var2 = getFreeSlot();
      short var3 = (short)(var2 * 4);
      if (var0.playerDownloadServer != null) {
         try {
            IDToAddressMap.put(var3, var0.getConnectedGUID());
            var0.playerDownloadServer.destroy();
         } catch (Exception var9) {
            var9.printStackTrace();
         }
      }

      playerToCoordsMap.put(var3, new Vector2());
      SlotToConnection[var2] = var0;
      var0.playerIDs[0] = var3;
      IDToAddressMap.put(var3, var0.getConnectedGUID());
      var0.playerDownloadServer = new PlayerDownloadServer(var0);
      DebugType var10000 = DebugType.Network;
      long var10001 = var0.getConnectedGUID();
      DebugLog.log(var10000, "Connected new client " + var10001 + " ID # " + var3);
      KahluaTable var4 = SpawnPoints.instance.getSpawnRegions();

      for(int var5 = 1; var5 < var4.size() + 1; ++var5) {
         ByteBufferWriter var6 = var0.startPacket();
         PacketTypes.PacketType.SpawnRegion.doPacket(var6);
         var6.putInt(var5);

         try {
            ((KahluaTable)var4.rawget(var5)).save(var6.bb);
            PacketTypes.PacketType.SpawnRegion.send(var0);
         } catch (IOException var8) {
            var8.printStackTrace();
         }
      }

      RequestDataPacket var10 = new RequestDataPacket();
      var10.sendConnectingDetails(var0, var1);
   }

   public static void sendMetaGrid(int var0, int var1, int var2, UdpConnection var3) {
      MetaGridPacket var4 = new MetaGridPacket();
      if (var4.set(var0, var1, var2)) {
         ByteBufferWriter var5 = var3.startPacket();
         PacketTypes.PacketType.MetaGrid.doPacket(var5);
         var4.write(var5);
         PacketTypes.PacketType.MetaGrid.send(var3);
      }

   }

   public static void sendMetaGrid(int var0, int var1, int var2) {
      for(int var3 = 0; var3 < udpEngine.connections.size(); ++var3) {
         UdpConnection var4 = (UdpConnection)udpEngine.connections.get(var3);
         sendMetaGrid(var0, var1, var2, var4);
      }

   }

   private static void preventIndoorZombies(int var0, int var1, int var2) {
      RoomDef var3 = IsoWorld.instance.MetaGrid.getRoomAt(var0, var1, var2);
      if (var3 != null) {
         boolean var4 = isSpawnBuilding(var3.getBuilding());
         var3.getBuilding().setAllExplored(true);
         var3.getBuilding().setAlarmed(false);
         ArrayList var5 = IsoWorld.instance.CurrentCell.getZombieList();

         for(int var6 = 0; var6 < var5.size(); ++var6) {
            IsoZombie var7 = (IsoZombie)var5.get(var6);
            if ((var4 || var7.bIndoorZombie) && var7.getSquare() != null && var7.getSquare().getRoom() != null && var7.getSquare().getRoom().def.building == var3.getBuilding()) {
               VirtualZombieManager.instance.removeZombieFromWorld(var7);
               if (var6 >= var5.size() || var5.get(var6) != var7) {
                  --var6;
               }
            }
         }

      }
   }

   public static void setCustomVariables(IsoPlayer var0, UdpConnection var1) {
      Iterator var2 = VariableSyncPacket.syncedVariables.iterator();

      while(var2.hasNext()) {
         String var3 = (String)var2.next();
         if (var0.getVariable(var3) != null) {
            INetworkPacket.send(var1, PacketTypes.PacketType.VariableSync, var0, var3, var0.getVariableString(var3));
         }
      }

   }

   public static void sendPlayerConnected(IsoPlayer var0, UdpConnection var1) {
      boolean var2 = PlayerToAddressMap.get(var0) != null && var1.getConnectedGUID() == (Long)PlayerToAddressMap.get(var0) && !isDelayedDisconnect(var0);
      if (var0 != null) {
         INetworkPacket.send(var1, PacketTypes.PacketType.ConnectedPlayer, var0, var2);
         setCustomVariables(var0, var1);
         if (!var2) {
            updateHandEquips(var1, var0);
         }
      }

   }

   public static void receivePlayerConnect(ByteBuffer var0, UdpConnection var1, String var2) {
      ConnectionManager.log("receive-packet", "player-connect", var1);
      byte var3 = var0.get();
      DebugLog.DetailedInfo.trace("User: \"%s\" index=%d ip=%s is trying to connect", var2, Integer.valueOf(var3), var1.ip);
      if (var3 >= 0 && var3 < 4 && var1.players[var3] == null) {
         byte var4 = (byte)Math.min(20, var0.get());
         var1.ReleventRange = (byte)(var4 / 2 + 2);
         IsoPlayer var5 = null;
         if (bCoop && SteamUtils.isSteamModeEnabled()) {
            var5 = ServerPlayerDB.getInstance().serverLoadNetworkCharacter(var3, var1.idStr);
         } else {
            var5 = ServerPlayerDB.getInstance().serverLoadNetworkCharacter(var3, var1.username);
         }

         if (var5 == null) {
            AntiCheat.Capability.act(var1, "ConnectPacket");
         } else {
            var1.ReleventPos[var3].x = var5.getX();
            var1.ReleventPos[var3].y = var5.getY();
            var1.ReleventPos[var3].z = var5.getZ();
            var1.connectArea[var3] = null;
            var1.ChunkGridWidth = var4;
            var1.loadedCells[var3] = new ClientServerMap(var3, PZMath.fastfloor(var5.getX()), PZMath.fastfloor(var5.getY()), var4);
            var5.realx = var5.getX();
            var5.realy = var5.getY();
            var5.realz = (byte)((int)var5.getZ());
            var5.PlayerIndex = var3;
            var5.OnlineChunkGridWidth = var4;
            Players.add(var5);
            var5.bRemote = true;
            var1.players[var3] = var5;
            short var6 = var1.playerIDs[var3];
            IDToPlayerMap.put(var6, var5);
            PlayerToAddressMap.put(var5, var1.getConnectedGUID());
            var5.setOnlineID(var6);
            byte var7 = var0.get();
            var5.setExtraInfoFlags(var7);
            if (SteamUtils.isSteamModeEnabled()) {
               var5.setSteamID(var1.steamID);
               SteamGameServer.BUpdateUserData(var1.steamID, var1.username, 0);
            }

            var5.username = var2;
            var5.setRole(var1.role);
            ChatServer.getInstance().initPlayer(var5.OnlineID);
            var1.setFullyConnected();
            sendWeather(var1);
            SafetySystemManager.restoreSafety(var5);
            if (var1.role.haveCapability(Capability.HideFromSteamUserList)) {
               SteamGameServer.AddPlayer(var5);
            }

            for(int var8 = 0; var8 < udpEngine.connections.size(); ++var8) {
               UdpConnection var9 = (UdpConnection)udpEngine.connections.get(var8);
               sendPlayerConnected(var5, var9);
               sendPlayerExtraInfo(var5, var9);
            }

            SyncInjuriesPacket var11 = new SyncInjuriesPacket();
            Iterator var12 = IDToPlayerMap.values().iterator();

            while(var12.hasNext()) {
               IsoPlayer var10 = (IsoPlayer)var12.next();
               if (var10.getOnlineID() != var5.getOnlineID() && var10.isAlive()) {
                  sendPlayerConnected(var10, var1);
                  var11.set(var10);
                  sendPlayerInjuries(var1, var11);
                  setCustomVariables(var10, var1);
               }
            }

            var1.loadedCells[var3].setLoaded();
            var1.loadedCells[var3].sendPacket(var1);
            preventIndoorZombies(PZMath.fastfloor(var5.getX()), PZMath.fastfloor(var5.getY()), PZMath.fastfloor(var5.getZ()));
            ServerLOS.instance.addPlayer(var5);
            WarManager.sendWarToPlayer(var5);
            ZLogger var10000 = LoggerManager.getLogger("user");
            String var10001 = var1.idStr;
            var10000.write(var10001 + " \"" + var5.username + "\" fully connected " + LoggerManager.getPlayerCoords(var5));
         }
      }
   }

   public static void sendInitialWorldState(UdpConnection var0) {
      if (RainManager.isRaining()) {
         sendStartRain(var0);
      }

      VehicleManager.instance.serverSendInitialWorldState(var0);

      try {
         if (!ClimateManager.getInstance().isUpdated()) {
            ClimateManager.getInstance().update();
         }

         ClimateManager.getInstance().sendInitialState(var0);
      } catch (Exception var2) {
         var2.printStackTrace();
      }

   }

   public static void sendObjectModData(IsoObject var0) {
      if (!bSoftReset && !bFastForward) {
         INetworkPacket.sendToRelative(PacketTypes.PacketType.ObjectModData, var0.getX(), var0.getY(), var0);
      }
   }

   public static void sendSlowFactor(IsoGameCharacter var0) {
      if (var0 instanceof IsoPlayer) {
         INetworkPacket.send((IsoPlayer)var0, PacketTypes.PacketType.SlowFactor, var0);
      }
   }

   public static void sendObjectChange(IsoObject var0, String var1, KahluaTable var2) {
      if (!bSoftReset) {
         if (var0 != null && var0.getSquare() != null) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.ObjectChange, (float)((int)var0.getX()), (float)((int)var0.getY()), var0, var1, var2);
         }
      }
   }

   public static void sendObjectChange(IsoObject var0, String var1, Object... var2) {
      if (!bSoftReset) {
         if (var2.length == 0) {
            sendObjectChange(var0, var1, (KahluaTable)null);
         } else if (var2.length % 2 == 0) {
            KahluaTable var3 = LuaManager.platform.newTable();

            for(int var4 = 0; var4 < var2.length; var4 += 2) {
               Object var5 = var2[var4 + 1];
               if (var5 instanceof Float) {
                  var3.rawset(var2[var4], ((Float)var5).doubleValue());
               } else if (var5 instanceof Integer) {
                  var3.rawset(var2[var4], ((Integer)var5).doubleValue());
               } else if (var5 instanceof Short) {
                  var3.rawset(var2[var4], ((Short)var5).doubleValue());
               } else {
                  var3.rawset(var2[var4], var5);
               }
            }

            sendObjectChange(var0, var1, var3);
         }
      }
   }

   public static void updateHandEquips(UdpConnection var0, IsoPlayer var1) {
      var1.updateHandEquips();
   }

   static void receiveSyncIsoObject(ByteBuffer var0, UdpConnection var1, short var2) {
      if (DebugOptions.instance.Network.Server.SyncIsoObject.getValue()) {
         int var3 = var0.getInt();
         int var4 = var0.getInt();
         int var5 = var0.getInt();
         byte var6 = var0.get();
         byte var7 = var0.get();
         byte var8 = var0.get();
         if (var7 == 1) {
            IsoGridSquare var9 = ServerMap.instance.getGridSquare(var3, var4, var5);
            if (var9 != null && var6 >= 0 && var6 < var9.getObjects().size()) {
               ((IsoObject)var9.getObjects().get(var6)).syncIsoObject(true, var8, var1, var0);
            } else if (var9 != null) {
               DebugLog.log("SyncIsoObject: index=" + var6 + " is invalid x,y,z=" + var3 + "," + var4 + "," + var5);
            } else {
               DebugLog.log("SyncIsoObject: sq is null x,y,z=" + var3 + "," + var4 + "," + var5);
            }

         }
      }
   }

   static void receiveSyncDoorKey(ByteBuffer var0, UdpConnection var1, short var2) {
      int var3 = var0.getInt();
      int var4 = var0.getInt();
      int var5 = var0.getInt();
      byte var6 = var0.get();
      int var7 = var0.getInt();
      IsoGridSquare var8 = IsoWorld.instance.CurrentCell.getGridSquare(var3, var4, var5);
      if (var8 != null && var6 >= 0 && var6 < var8.getObjects().size()) {
         IsoObject var9 = (IsoObject)var8.getObjects().get(var6);
         if (var9 instanceof IsoDoor) {
            IsoDoor var10 = (IsoDoor)var9;
            var10.keyId = var7;

            for(int var13 = 0; var13 < udpEngine.connections.size(); ++var13) {
               UdpConnection var12 = (UdpConnection)udpEngine.connections.get(var13);
               if (var12.getConnectedGUID() != var1.getConnectedGUID()) {
                  ByteBufferWriter var11 = var12.startPacket();
                  PacketTypes.PacketType.SyncDoorKey.doPacket(var11);
                  var11.putInt(var3);
                  var11.putInt(var4);
                  var11.putInt(var5);
                  var11.putByte(var6);
                  var11.putInt(var7);
                  PacketTypes.PacketType.SyncDoorKey.send(var12);
               }
            }

         } else {
            DebugLog.log("SyncDoorKey: expected IsoDoor index=" + var6 + " is invalid x,y,z=" + var3 + "," + var4 + "," + var5);
         }
      } else if (var8 != null) {
         DebugLog.log("SyncDoorKey: index=" + var6 + " is invalid x,y,z=" + var3 + "," + var4 + "," + var5);
      } else {
         DebugLog.log("SyncDoorKey: sq is null x,y,z=" + var3 + "," + var4 + "," + var5);
      }
   }

   public static int RemoveItemFromMap(IsoObject var0) {
      int var1 = var0.getObjectIndex();
      INetworkPacket.sendToRelativeAndProcess(PacketTypes.PacketType.RemoveItemFromSquare, (int)var0.getX(), (int)var0.getY(), var0);
      return var1;
   }

   public static void sendBloodSplatter(HandWeapon var0, float var1, float var2, float var3, Vector2 var4, boolean var5, boolean var6) {
      for(int var7 = 0; var7 < udpEngine.connections.size(); ++var7) {
         UdpConnection var8 = (UdpConnection)udpEngine.connections.get(var7);
         ByteBufferWriter var9 = var8.startPacket();
         PacketTypes.PacketType.BloodSplatter.doPacket(var9);
         var9.putUTF(var0 != null ? var0.getType() : "");
         var9.putFloat(var1);
         var9.putFloat(var2);
         var9.putFloat(var3);
         var9.putFloat(var4.getX());
         var9.putFloat(var4.getY());
         var9.putByte((byte)(var5 ? 1 : 0));
         var9.putByte((byte)(var6 ? 1 : 0));
         byte var10 = 0;
         if (var0 != null) {
            var10 = (byte)Math.max(var0.getSplatNumber(), 1);
         }

         var9.putByte(var10);
         PacketTypes.PacketType.BloodSplatter.send(var8);
      }

   }

   public static void disconnect(UdpConnection var0, String var1) {
      if (var0.playerDownloadServer != null) {
         try {
            var0.playerDownloadServer.destroy();
         } catch (Exception var4) {
            var4.printStackTrace();
         }

         var0.playerDownloadServer = null;
      }

      RequestDataManager.getInstance().disconnect(var0);

      int var2;
      for(var2 = 0; var2 < 4; ++var2) {
         IsoPlayer var3 = var0.players[var2];
         if (var3 != null) {
            TransactionManager.cancelAllRelevantToUser(var3);
            ServerPlayerDB.getInstance().serverUpdateNetworkCharacter(var3, var2, var0);
            ChatServer.getInstance().disconnectPlayer(var0.playerIDs[var2]);
            disconnectPlayer(var3, var0);
         }

         var0.usernames[var2] = null;
         var0.players[var2] = null;
         var0.playerIDs[var2] = -1;
         var0.ReleventPos[var2] = null;
         var0.connectArea[var2] = null;
      }

      for(var2 = 0; var2 < udpEngine.getMaxConnections(); ++var2) {
         if (SlotToConnection[var2] == var0) {
            SlotToConnection[var2] = null;
         }
      }

      Iterator var5 = IDToAddressMap.entrySet().iterator();

      while(var5.hasNext()) {
         Map.Entry var6 = (Map.Entry)var5.next();
         if ((Long)var6.getValue() == var0.getConnectedGUID()) {
            var5.remove();
         }
      }

      if (!SteamUtils.isSteamModeEnabled()) {
         PublicServerUtil.updatePlayers();
      }

      if (CoopSlave.instance != null && var0.isCoopHost) {
         DebugLog.log("Host user disconnected, stopping the server");
         ServerMap.instance.QueueQuit();
      }

      if (bServer) {
         ConnectionManager.log("disconnect", var1, var0);
      }

   }

   public static void addIncoming(short var0, ByteBuffer var1, UdpConnection var2) {
      ZomboidNetData var3 = null;
      if (var1.limit() > 2048) {
         var3 = ZomboidNetDataPool.instance.getLong(var1.limit());
      } else {
         var3 = ZomboidNetDataPool.instance.get();
      }

      var3.read(var0, var1, var2);
      if (var3.type == null) {
         try {
            AntiCheat.PacketType.act(var2, String.valueOf(var0));
         } catch (Exception var5) {
            var5.printStackTrace();
         }

      } else {
         var3.time = System.currentTimeMillis();
         if (var3.type != PacketTypes.PacketType.PlayerUpdateUnreliable && var3.type != PacketTypes.PacketType.PlayerUpdateReliable) {
            if (var3.type != PacketTypes.PacketType.VehiclesUnreliable && var3.type != PacketTypes.PacketType.Vehicles) {
               MainLoopNetDataHighPriorityQ.add(var3);
            } else {
               byte var4 = var3.buffer.get(0);
               if (var4 == 9) {
                  MainLoopNetDataQ.add(var3);
               } else {
                  MainLoopNetDataHighPriorityQ.add(var3);
               }
            }
         } else {
            MainLoopPlayerUpdateQ.add(var3);
         }

      }
   }

   public static void smashWindow(IsoWindow var0) {
      SmashWindowPacket var1 = new SmashWindowPacket();
      var1.setSmashWindow(var0);

      for(int var2 = 0; var2 < udpEngine.connections.size(); ++var2) {
         UdpConnection var3 = (UdpConnection)udpEngine.connections.get(var2);
         if (var3.RelevantTo(var0.getX(), var0.getY())) {
            ByteBufferWriter var4 = var3.startPacket();
            PacketTypes.PacketType.SmashWindow.doPacket(var4);
            var1.write(var4);
            PacketTypes.PacketType.SmashWindow.send(var3);
         }
      }

   }

   public static void removeBrokenGlass(IsoWindow var0) {
      SmashWindowPacket var1 = new SmashWindowPacket();
      var1.setRemoveBrokenGlass(var0);

      for(int var2 = 0; var2 < udpEngine.connections.size(); ++var2) {
         UdpConnection var3 = (UdpConnection)udpEngine.connections.get(var2);
         if (var3.RelevantTo(var0.getX(), var0.getY())) {
            ByteBufferWriter var4 = var3.startPacket();
            PacketTypes.PacketType.SmashWindow.doPacket(var4);
            var1.write(var4);
            PacketTypes.PacketType.SmashWindow.send(var3);
         }
      }

   }

   public static void sendHitCharacter(HitCharacter var0, PacketTypes.PacketType var1, UdpConnection var2) {
      for(int var3 = 0; var3 < udpEngine.connections.size(); ++var3) {
         UdpConnection var4 = (UdpConnection)udpEngine.connections.get(var3);
         if (var4.getConnectedGUID() != var2.getConnectedGUID() && var0.isRelevant(var4)) {
            ByteBufferWriter var5 = var4.startPacket();
            var1.doPacket(var5);
            var0.write(var5);
            var1.send(var4);
         }
      }

   }

   public static void sendZombieDeath(IsoZombie var0) {
      try {
         DeadZombiePacket var1 = new DeadZombiePacket();
         var1.set(var0);
         sendZombieDeath(var1);
      } catch (Exception var2) {
         DebugLog.Multiplayer.printException(var2, "SendZombieDeath: failed", LogSeverity.Error);
      }

   }

   public static void sendZombieDeath(DeadZombiePacket var0) {
      try {
         if (Core.bDebug) {
            DebugLog.Multiplayer.debugln("SendZombieDeath: %s", var0.getDescription());
         }

         for(int var1 = 0; var1 < udpEngine.connections.size(); ++var1) {
            UdpConnection var2 = (UdpConnection)udpEngine.connections.get(var1);
            if (var2.RelevantTo(var0.getZombie().getX(), var0.getZombie().getY())) {
               ByteBufferWriter var3 = var2.startPacket();
               PacketTypes.PacketType.ZombieDeath.doPacket(var3);

               try {
                  var0.write(var3);
                  PacketTypes.PacketType.ZombieDeath.send(var2);
               } catch (Exception var5) {
                  var2.cancelPacket();
                  DebugLog.Multiplayer.printException(var5, "SendZombieDeath: failed", LogSeverity.Error);
               }
            }
         }
      } catch (Exception var6) {
         DebugLog.Multiplayer.printException(var6, "SendZombieDeath: failed", LogSeverity.Error);
      }

   }

   public static void sendItemStats(InventoryItem var0) {
      if (var0.getContainer() != null && var0.getContainer().getParent() instanceof IsoPlayer) {
         INetworkPacket.send(getConnectionFromPlayer((IsoPlayer)var0.getContainer().getParent()), PacketTypes.PacketType.ItemStats, var0.getContainer(), var0);
      } else if (var0.getWorldItem() != null) {
         ItemContainer var1 = new ItemContainer("floor", var0.getWorldItem().square, (IsoObject)null);
         INetworkPacket.sendToRelative(PacketTypes.PacketType.ItemStats, (float)var0.getWorldItem().getSquare().x, (float)var0.getWorldItem().getSquare().y, var1, var0);
      } else if (var0.getOutermostContainer() != null) {
         if (var0.getOutermostContainer().getSourceGrid() != null) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.ItemStats, (float)var0.getOutermostContainer().getSourceGrid().x, (float)var0.getOutermostContainer().getSourceGrid().y, var0.getContainer(), var0);
         } else if (var0.getOutermostContainer().getParent() != null) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.ItemStats, var0.getOutermostContainer().getParent().getX(), var0.getOutermostContainer().getParent().getY(), var0.getContainer(), var0);
         } else {
            INetworkPacket.sendToAll(PacketTypes.PacketType.ItemStats, (UdpConnection)null, var0.getOutermostContainer(), var0);
         }

      }
   }

   public static void sendPlayerDeath(DeadPlayerPacket var0, UdpConnection var1) {
      if (Core.bDebug) {
         DebugLog.Multiplayer.debugln("SendPlayerDeath: %s", var0.getDescription());
      }

      for(int var2 = 0; var2 < udpEngine.connections.size(); ++var2) {
         UdpConnection var3 = (UdpConnection)udpEngine.connections.get(var2);
         if (var1 == null || var1.getConnectedGUID() != var3.getConnectedGUID()) {
            ByteBufferWriter var4 = var3.startPacket();
            PacketTypes.PacketType.PlayerDeath.doPacket(var4);
            var0.write(var4);
            PacketTypes.PacketType.PlayerDeath.send(var3);
         }
      }

   }

   public static void sendAnimalDeath(DeadAnimalPacket var0, UdpConnection var1) {
      DebugLog.Multiplayer.debugln("SendAnimalDeath: %s", var0.getDescription());
      Iterator var2 = udpEngine.connections.iterator();

      while(true) {
         UdpConnection var3;
         do {
            if (!var2.hasNext()) {
               return;
            }

            var3 = (UdpConnection)var2.next();
         } while(var1 != null && var1.getConnectedGUID() == var3.getConnectedGUID());

         ByteBufferWriter var4 = var3.startPacket();
         PacketTypes.PacketType.AnimalDeath.doPacket(var4);
         var0.write(var4);
         PacketTypes.PacketType.AnimalDeath.send(var3);
      }
   }

   public static void sendPlayerInjuries(UdpConnection var0, SyncInjuriesPacket var1) {
      ByteBufferWriter var2 = var0.startPacket();
      PacketTypes.PacketType.SyncInjuries.doPacket(var2);
      var1.write(var2);
      PacketTypes.PacketType.SyncInjuries.send(var0);
   }

   public static void sendRemoveCorpseFromMap(IsoDeadBody var0) {
      RemoveCorpseFromMapPacket var1 = new RemoveCorpseFromMapPacket();
      var1.set(var0);
      DebugLog.Death.trace(var1.getDescription());

      for(int var2 = 0; var2 < udpEngine.connections.size(); ++var2) {
         UdpConnection var3 = (UdpConnection)udpEngine.connections.get(var2);
         ByteBufferWriter var4 = var3.startPacket();
         PacketTypes.PacketType.RemoveCorpseFromMap.doPacket(var4);
         var1.write(var4);
         PacketTypes.PacketType.RemoveCorpseFromMap.send(var3);
      }

   }

   public static void receiveEatBody(ByteBuffer var0, UdpConnection var1, short var2) {
      try {
         if (Core.bDebug) {
            DebugLog.log(DebugType.Multiplayer, "ReceiveEatBody");
         }

         short var3 = var0.getShort();
         IsoZombie var4 = (IsoZombie)ServerMap.instance.ZombieMap.get(var3);
         if (var4 == null) {
            DebugLog.Multiplayer.error("ReceiveEatBody: zombie " + var3 + " not found");
            return;
         }

         Iterator var5 = udpEngine.connections.iterator();

         while(var5.hasNext()) {
            UdpConnection var6 = (UdpConnection)var5.next();
            if (var6.RelevantTo(var4.getX(), var4.getY())) {
               if (Core.bDebug) {
                  DebugLog.log(DebugType.Multiplayer, "SendEatBody");
               }

               ByteBufferWriter var7 = var6.startPacket();
               PacketTypes.PacketType.EatBody.doPacket(var7);
               var0.position(0);
               var7.bb.put(var0);
               PacketTypes.PacketType.EatBody.send(var6);
            }
         }
      } catch (Exception var8) {
         DebugLog.Multiplayer.printException(var8, "ReceiveEatBody: failed", LogSeverity.Error);
      }

   }

   public static void receiveSyncRadioData(ByteBuffer var0, UdpConnection var1, short var2) {
      try {
         boolean var3 = var0.get() == 1;
         int var4 = var0.getInt();
         int[] var5 = new int[var4];

         for(int var6 = 0; var6 < var4; ++var6) {
            var5[var6] = var0.getInt();
         }

         RakVoice.SetChannelsRouting(var1.getConnectedGUID(), var3, var5, (short)var4);
         Iterator var10 = udpEngine.connections.iterator();

         while(var10.hasNext()) {
            UdpConnection var7 = (UdpConnection)var10.next();
            if (var7 != var1 && var1.players[0] != null) {
               ByteBufferWriter var8 = var7.startPacket();
               PacketTypes.PacketType.SyncRadioData.doPacket(var8);
               var8.putShort(var1.players[0].OnlineID);
               var0.position(0);
               var8.bb.put(var0);
               PacketTypes.PacketType.SyncRadioData.send(var7);
            }
         }
      } catch (Exception var9) {
         DebugLog.Multiplayer.printException(var9, "SyncRadioData: failed", LogSeverity.Error);
      }

   }

   public static void receiveThump(ByteBuffer var0, UdpConnection var1, short var2) {
      try {
         if (Core.bDebug) {
            DebugLog.log(DebugType.Multiplayer, "ReceiveThump");
         }

         short var3 = var0.getShort();
         IsoZombie var4 = (IsoZombie)ServerMap.instance.ZombieMap.get(var3);
         if (var4 == null) {
            DebugLog.Multiplayer.error("ReceiveThump: zombie " + var3 + " not found");
            return;
         }

         Iterator var5 = udpEngine.connections.iterator();

         while(var5.hasNext()) {
            UdpConnection var6 = (UdpConnection)var5.next();
            if (var6.RelevantTo(var4.getX(), var4.getY())) {
               ByteBufferWriter var7 = var6.startPacket();
               PacketTypes.PacketType.Thump.doPacket(var7);
               var0.position(0);
               var7.bb.put(var0);
               PacketTypes.PacketType.Thump.send(var6);
            }
         }
      } catch (Exception var8) {
         DebugLog.Multiplayer.printException(var8, "ReceiveEatBody: failed", LogSeverity.Error);
      }

   }

   public static void sendWorldSound(WorldSoundManager.WorldSound var0, UdpConnection var1) {
      WorldSoundPacket var2 = new WorldSoundPacket();
      var2.setData(new Object[]{var0});

      for(int var3 = 0; var3 < udpEngine.connections.size(); ++var3) {
         UdpConnection var4 = (UdpConnection)udpEngine.connections.get(var3);
         if (var4.isFullyConnected() && var4.RelevantTo((float)var0.x, (float)var0.y, (float)var0.radius)) {
            ByteBufferWriter var5 = var4.startPacket();
            PacketTypes.PacketType.WorldSoundPacket.doPacket(var5);
            var2.write(var5);
            PacketTypes.PacketType.WorldSoundPacket.send(var4);
         }
      }

   }

   public static void kick(UdpConnection var0, String var1, String var2) {
      ConnectionManager.log("kick", var2, var0);
      INetworkPacket.send(var0, PacketTypes.PacketType.Kicked, var1, var2);
   }

   private static void sendStartRain(UdpConnection var0) {
      ByteBufferWriter var1 = var0.startPacket();
      PacketTypes.PacketType.StartRain.doPacket(var1);
      var1.putInt(RainManager.randRainMin);
      var1.putInt(RainManager.randRainMax);
      var1.putFloat(RainManager.RainDesiredIntensity);
      PacketTypes.PacketType.StartRain.send(var0);
   }

   public static void startRain() {
      if (udpEngine != null) {
         for(int var0 = 0; var0 < udpEngine.connections.size(); ++var0) {
            UdpConnection var1 = (UdpConnection)udpEngine.connections.get(var0);
            sendStartRain(var1);
         }

      }
   }

   private static void sendStopRain(UdpConnection var0) {
      ByteBufferWriter var1 = var0.startPacket();
      PacketTypes.PacketType.StopRain.doPacket(var1);
      PacketTypes.PacketType.StopRain.send(var0);
   }

   public static void stopRain() {
      for(int var0 = 0; var0 < udpEngine.connections.size(); ++var0) {
         UdpConnection var1 = (UdpConnection)udpEngine.connections.get(var0);
         sendStopRain(var1);
      }

   }

   private static void sendWeather(UdpConnection var0) {
      WeatherPacket var1 = new WeatherPacket();
      ByteBufferWriter var2 = var0.startPacket();
      PacketTypes.PacketType.Weather.doPacket(var2);
      var1.write(var2);
      PacketTypes.PacketType.Weather.send(var0);
   }

   public static void sendWeather() {
      for(int var0 = 0; var0 < udpEngine.connections.size(); ++var0) {
         UdpConnection var1 = (UdpConnection)udpEngine.connections.get(var0);
         sendWeather(var1);
      }

   }

   private static boolean isInSameFaction(IsoPlayer var0, IsoPlayer var1) {
      Faction var2 = Faction.getPlayerFaction(var0);
      Faction var3 = Faction.getPlayerFaction(var1);
      return var2 != null && var2 == var3;
   }

   private static boolean isAnyPlayerInSameFaction(UdpConnection var0, IsoPlayer var1) {
      for(int var2 = 0; var2 < 4; ++var2) {
         IsoPlayer var3 = var0.players[var2];
         if (var3 != null && isInSameFaction(var3, var1)) {
            return true;
         }
      }

      return false;
   }

   private static boolean isAnyPlayerInSameSafehouse(UdpConnection var0, IsoPlayer var1) {
      for(int var2 = 0; var2 < 4; ++var2) {
         IsoPlayer var3 = var0.players[var2];
         if (var3 != null && SafeHouse.isInSameSafehouse(var3.getUsername(), var1.getUsername())) {
            return true;
         }
      }

      return false;
   }

   private static boolean shouldSendWorldMapPlayerPosition(UdpConnection var0, IsoPlayer var1) {
      if (var1 != null && !var1.isDead()) {
         UdpConnection var2 = getConnectionFromPlayer(var1);
         if (var2 != null && var2 != var0 && var2.isFullyConnected()) {
            if (var0.role.haveCapability(Capability.SeeWorldMap)) {
               return true;
            } else {
               int var3 = ServerOptions.getInstance().MapRemotePlayerVisibility.getValue();
               if (var3 == 2) {
                  return isAnyPlayerInSameFaction(var0, var1) || isAnyPlayerInSameSafehouse(var0, var1);
               } else {
                  return true;
               }
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static void sendWorldMapPlayerPosition(UdpConnection var0) {
      tempPlayers.clear();

      for(int var1 = 0; var1 < Players.size(); ++var1) {
         IsoPlayer var2 = (IsoPlayer)Players.get(var1);
         if (shouldSendWorldMapPlayerPosition(var0, var2)) {
            tempPlayers.add(var2);
         }
      }

      if (!tempPlayers.isEmpty()) {
         ByteBufferWriter var5 = var0.startPacket();
         PacketTypes.PacketType.WorldMapPlayerPosition.doPacket(var5);
         var5.putBoolean(false);
         var5.putShort((short)tempPlayers.size());

         for(int var6 = 0; var6 < tempPlayers.size(); ++var6) {
            IsoPlayer var3 = (IsoPlayer)tempPlayers.get(var6);
            WorldMapRemotePlayer var4 = WorldMapRemotePlayers.instance.getOrCreatePlayer(var3);
            var4.setPlayer(var3);
            var5.putShort(var4.getOnlineID());
            var5.putShort(var4.getChangeCount());
            var5.putFloat(var4.getX());
            var5.putFloat(var4.getY());
         }

         PacketTypes.PacketType.WorldMapPlayerPosition.send(var0);
      }
   }

   public static void sendWorldMapPlayerPosition() {
      int var0 = ServerOptions.getInstance().MapRemotePlayerVisibility.getValue();

      for(int var1 = 0; var1 < udpEngine.connections.size(); ++var1) {
         UdpConnection var2 = (UdpConnection)udpEngine.connections.get(var1);
         if (var0 != 1 || var2.role.haveCapability(Capability.SeeWorldMap)) {
            sendWorldMapPlayerPosition(var2);
         }
      }

   }

   public static void receiveWorldMapPlayerPosition(ByteBuffer var0, UdpConnection var1, short var2) {
      short var3 = var0.getShort();
      tempPlayers.clear();

      int var5;
      IsoPlayer var6;
      for(int var4 = 0; var4 < var3; ++var4) {
         var5 = var0.getShort();
         var6 = (IsoPlayer)IDToPlayerMap.get(Short.valueOf((short)var5));
         if (var6 != null && shouldSendWorldMapPlayerPosition(var1, var6)) {
            tempPlayers.add(var6);
         }
      }

      if (!tempPlayers.isEmpty()) {
         ByteBufferWriter var8 = var1.startPacket();
         PacketTypes.PacketType.WorldMapPlayerPosition.doPacket(var8);
         var8.putBoolean(true);
         var8.putShort((short)tempPlayers.size());

         for(var5 = 0; var5 < tempPlayers.size(); ++var5) {
            var6 = (IsoPlayer)tempPlayers.get(var5);
            WorldMapRemotePlayer var7 = WorldMapRemotePlayers.instance.getOrCreatePlayer(var6);
            var7.setPlayer(var6);
            var8.putShort(var7.getOnlineID());
            var8.putShort(var7.getChangeCount());
            var8.putUTF(var7.getUsername());
            var8.putUTF(var7.getForename());
            var8.putUTF(var7.getSurname());
            var8.putUTF(var7.getAccessLevel());
            var8.putFloat(var7.getX());
            var8.putFloat(var7.getY());
            var8.putBoolean(var7.isInvisible());
            var8.putBoolean(var7.isDisguised());
         }

         PacketTypes.PacketType.WorldMapPlayerPosition.send(var1);
      }
   }

   private static void syncClock(UdpConnection var0) {
      INetworkPacket.send(var0, PacketTypes.PacketType.SyncClock);
   }

   public static void syncClock() {
      for(int var0 = 0; var0 < udpEngine.connections.size(); ++var0) {
         UdpConnection var1 = (UdpConnection)udpEngine.connections.get(var0);
         syncClock(var1);
      }

   }

   public static void sendServerCommand(String var0, String var1, KahluaTable var2, UdpConnection var3) {
      ByteBufferWriter var4 = var3.startPacket();
      PacketTypes.PacketType.ClientCommand.doPacket(var4);
      var4.putUTF(var0);
      var4.putUTF(var1);
      if (var2 != null && !var2.isEmpty()) {
         var4.putByte((byte)1);

         try {
            KahluaTableIterator var5 = var2.iterator();

            while(var5.advance()) {
               if (!TableNetworkUtils.canSave(var5.getKey(), var5.getValue())) {
                  Object var10000 = var5.getKey();
                  DebugLog.log("ERROR: sendServerCommand: can't save key,value=" + var10000 + "," + var5.getValue());
               }
            }

            TableNetworkUtils.save(var2, var4.bb);
         } catch (IOException var6) {
            var6.printStackTrace();
         }
      } else {
         var4.putByte((byte)0);
      }

      PacketTypes.PacketType.ClientCommand.send(var3);
   }

   public static void sendServerCommand(String var0, String var1, KahluaTable var2) {
      for(int var3 = 0; var3 < udpEngine.connections.size(); ++var3) {
         UdpConnection var4 = (UdpConnection)udpEngine.connections.get(var3);
         sendServerCommand(var0, var1, var2, var4);
      }

   }

   public static void sendServerCommandV(String var0, String var1, Object... var2) {
      if (var2.length == 0) {
         sendServerCommand(var0, var1, (KahluaTable)null);
      } else if (var2.length % 2 != 0) {
         DebugLog.log("ERROR: sendServerCommand called with invalid number of arguments (" + var0 + " " + var1 + ")");
      } else {
         KahluaTable var3 = LuaManager.platform.newTable();

         for(int var4 = 0; var4 < var2.length; var4 += 2) {
            Object var5 = var2[var4 + 1];
            if (var5 instanceof Float) {
               var3.rawset(var2[var4], ((Float)var5).doubleValue());
            } else if (var5 instanceof Integer) {
               var3.rawset(var2[var4], ((Integer)var5).doubleValue());
            } else if (var5 instanceof Short) {
               var3.rawset(var2[var4], ((Short)var5).doubleValue());
            } else {
               var3.rawset(var2[var4], var5);
            }
         }

         sendServerCommand(var0, var1, var3);
      }
   }

   public static void sendServerCommand(IsoPlayer var0, String var1, String var2, KahluaTable var3) {
      if (PlayerToAddressMap.containsKey(var0)) {
         long var4 = (Long)PlayerToAddressMap.get(var0);
         UdpConnection var6 = udpEngine.getActiveConnection(var4);
         if (var6 != null) {
            sendServerCommand(var1, var2, var3, var6);
         }
      }
   }

   public static ArrayList<IsoPlayer> getPlayers(ArrayList<IsoPlayer> var0) {
      var0.clear();

      for(int var1 = 0; var1 < udpEngine.connections.size(); ++var1) {
         UdpConnection var2 = (UdpConnection)udpEngine.connections.get(var1);

         for(int var3 = 0; var3 < 4; ++var3) {
            IsoPlayer var4 = var2.players[var3];
            if (var4 != null && var4.OnlineID != -1) {
               var0.add(var4);
            }
         }
      }

      return var0;
   }

   public static ArrayList<IsoPlayer> getPlayers() {
      ArrayList var0 = new ArrayList();
      return getPlayers(var0);
   }

   public static int getPlayerCount() {
      int var0 = 0;

      for(int var1 = 0; var1 < udpEngine.connections.size(); ++var1) {
         UdpConnection var2 = (UdpConnection)udpEngine.connections.get(var1);
         if (var2.role != null && !var2.role.haveCapability(Capability.HideFromSteamUserList)) {
            for(int var3 = 0; var3 < 4; ++var3) {
               if (var2.playerIDs[var3] != -1) {
                  ++var0;
               }
            }
         }
      }

      return var0;
   }

   public static void sendAmbient(String var0, int var1, int var2, int var3, float var4) {
      DebugLog.log(DebugType.Sound, "ambient: sending " + var0 + " at " + var1 + "," + var2 + " radius=" + var3);

      for(int var5 = 0; var5 < udpEngine.connections.size(); ++var5) {
         UdpConnection var6 = (UdpConnection)udpEngine.connections.get(var5);
         IsoPlayer var7 = getAnyPlayerFromConnection(var6);
         if (var7 != null) {
            ByteBufferWriter var8 = var6.startPacket();
            PacketTypes.PacketType.AddAmbient.doPacket(var8);
            var8.putUTF(var0);
            var8.putInt(var1);
            var8.putInt(var2);
            var8.putInt(var3);
            var8.putFloat(var4);
            PacketTypes.PacketType.AddAmbient.send(var6);
         }
      }

   }

   public static void sendChangeSafety(Safety var0) {
      try {
         SafetyPacket var1 = new SafetyPacket(var0);
         Iterator var2 = udpEngine.connections.iterator();

         while(var2.hasNext()) {
            UdpConnection var3 = (UdpConnection)var2.next();
            ByteBufferWriter var4 = var3.startPacket();
            PacketTypes.PacketType.ChangeSafety.doPacket(var4);
            var1.write(var4);
            PacketTypes.PacketType.ChangeSafety.send(var3);
         }
      } catch (Exception var5) {
         DebugLog.Multiplayer.printException(var5, "SendChangeSafety: failed", LogSeverity.Error);
      }

   }

   static void receivePing(ByteBuffer var0, UdpConnection var1, short var2) {
      var1.ping = true;
      answerPing(var0, var1);
   }

   public static void updateOverlayForClients(IsoObject var0, String var1, float var2, float var3, float var4, float var5, UdpConnection var6) {
      if (udpEngine != null) {
         INetworkPacket.sendToRelative(PacketTypes.PacketType.UpdateOverlaySprite, var6, (float)var0.square.x, (float)var0.square.y, var0, var1, var2, var3, var4, var5);
      }
   }

   public static void sendReanimatedZombieID(IsoPlayer var0, IsoZombie var1) {
      if (PlayerToAddressMap.containsKey(var0)) {
         sendObjectChange(var0, "reanimatedID", (Object[])("ID", (double)var1.OnlineID));
      }

   }

   public static void receiveRadioServerData(ByteBuffer var0, UdpConnection var1, short var2) {
      ByteBufferWriter var3 = var1.startPacket();
      PacketTypes.PacketType.RadioServerData.doPacket(var3);
      ZomboidRadio.getInstance().WriteRadioServerDataPacket(var3);
      PacketTypes.PacketType.RadioServerData.send(var1);
   }

   public static void receiveRadioDeviceDataState(ByteBuffer var0, UdpConnection var1, short var2) {
      byte var3 = var0.get();
      if (var3 == 1) {
         int var4 = var0.getInt();
         int var5 = var0.getInt();
         int var6 = var0.getInt();
         int var7 = var0.getInt();
         IsoGridSquare var8 = IsoWorld.instance.CurrentCell.getGridSquare(var4, var5, var6);
         if (var8 != null && var7 >= 0 && var7 < var8.getObjects().size()) {
            IsoObject var9 = (IsoObject)var8.getObjects().get(var7);
            if (var9 instanceof IsoWaveSignal) {
               DeviceData var10 = ((IsoWaveSignal)var9).getDeviceData();
               if (var10 != null) {
                  try {
                     var10.receiveDeviceDataStatePacket(var0, (UdpConnection)null);
                  } catch (Exception var14) {
                     System.out.print(var14.getMessage());
                  }
               }
            }
         }
      } else {
         short var15;
         if (var3 == 0) {
            var15 = var0.get();
            IsoPlayer var16 = getPlayerFromConnection(var1, var15);
            byte var18 = var0.get();
            if (var16 != null) {
               Radio var20 = null;
               if (var18 == 1 && var16.getPrimaryHandItem() instanceof Radio) {
                  var20 = (Radio)var16.getPrimaryHandItem();
               }

               if (var18 == 2 && var16.getSecondaryHandItem() instanceof Radio) {
                  var20 = (Radio)var16.getSecondaryHandItem();
               }

               if (var20 != null && var20.getDeviceData() != null) {
                  try {
                     var20.getDeviceData().receiveDeviceDataStatePacket(var0, var1);
                  } catch (Exception var13) {
                     System.out.print(var13.getMessage());
                  }
               }
            }
         } else if (var3 == 2) {
            var15 = var0.getShort();
            short var17 = var0.getShort();
            BaseVehicle var19 = VehicleManager.instance.getVehicleByID(var15);
            if (var19 != null) {
               VehiclePart var22 = var19.getPartByIndex(var17);
               if (var22 != null) {
                  DeviceData var21 = var22.getDeviceData();
                  if (var21 != null) {
                     try {
                        var21.receiveDeviceDataStatePacket(var0, (UdpConnection)null);
                     } catch (Exception var12) {
                        System.out.print(var12.getMessage());
                     }
                  }
               }
            }
         }
      }

   }

   public static void sendIsoWaveSignal(long var0, int var2, int var3, int var4, String var5, String var6, String var7, float var8, float var9, float var10, int var11, boolean var12) {
      WaveSignalPacket var13 = new WaveSignalPacket();
      var13.set(var2, var3, var4, var5, var6, var7, var8, var9, var10, var11, var12);

      for(int var14 = 0; var14 < udpEngine.connections.size(); ++var14) {
         UdpConnection var15 = (UdpConnection)udpEngine.connections.get(var14);
         if (var0 != var15.getConnectedGUID()) {
            ByteBufferWriter var16 = var15.startPacket();
            PacketTypes.PacketType.WaveSignal.doPacket(var16);
            var13.write(var16);
            PacketTypes.PacketType.WaveSignal.send(var15);
         }
      }

   }

   public static void receivePlayerListensChannel(ByteBuffer var0, UdpConnection var1, short var2) {
      int var3 = var0.getInt();
      boolean var4 = var0.get() == 1;
      boolean var5 = var0.get() == 1;
      ZomboidRadio.getInstance().PlayerListensChannel(var3, var4, var5);
   }

   public static void sendAlarm(int var0, int var1) {
      DebugLog.log(DebugType.Multiplayer, "SendAlarm at [ " + var0 + " , " + var1 + " ]");

      for(int var2 = 0; var2 < udpEngine.connections.size(); ++var2) {
         UdpConnection var3 = (UdpConnection)udpEngine.connections.get(var2);
         IsoPlayer var4 = getAnyPlayerFromConnection(var3);
         if (var4 != null) {
            ByteBufferWriter var5 = var3.startPacket();
            PacketTypes.PacketType.AddAlarm.doPacket(var5);
            var5.putInt(var0);
            var5.putInt(var1);
            PacketTypes.PacketType.AddAlarm.send(var3);
         }
      }

   }

   public static boolean isSpawnBuilding(BuildingDef var0) {
      return SpawnPoints.instance.isSpawnBuilding(var0);
   }

   private static void setFastForward(boolean var0) {
      if (var0 != bFastForward) {
         bFastForward = var0;
         AntiCheatTime.skip = 2;
         syncClock();
      }
   }

   static void receiveVehicles(ByteBuffer var0, UdpConnection var1, short var2) {
      VehicleManager.instance.serverPacket(var0, var1, var2);
   }

   public static void sendAdminMessage(String var0, int var1, int var2, int var3) {
      MessageForAdminPacket var4 = new MessageForAdminPacket();
      var4.setData(new Object[]{var0, var1, var2, var3});

      for(int var5 = 0; var5 < udpEngine.connections.size(); ++var5) {
         UdpConnection var6 = (UdpConnection)udpEngine.connections.get(var5);
         if (var6.role.haveCapability(Capability.CanSeeMessageForAdmin)) {
            var4.sendToClient(PacketTypes.PacketType.MessageForAdmin, var6);
         }
      }

   }

   public static void sendWakeUpPlayer(IsoPlayer var0, UdpConnection var1) {
      INetworkPacket.processPacketOnServer(PacketTypes.PacketType.WakeUpPlayer, (UdpConnection)null, var0);
   }

   static void receiveGetDBSchema(ByteBuffer var0, UdpConnection var1, short var2) {
      DBSchema var3 = ServerWorldDatabase.instance.getDBSchema();

      for(int var4 = 0; var4 < udpEngine.connections.size(); ++var4) {
         UdpConnection var5 = (UdpConnection)udpEngine.connections.get(var4);
         if (var1 != null && var5.getConnectedGUID() == var1.getConnectedGUID()) {
            ByteBufferWriter var6 = var5.startPacket();
            PacketTypes.PacketType.GetDBSchema.doPacket(var6);
            HashMap var7 = var3.getSchema();
            var6.putInt(var7.size());
            Iterator var8 = var7.keySet().iterator();

            while(var8.hasNext()) {
               String var9 = (String)var8.next();
               HashMap var10 = (HashMap)var7.get(var9);
               var6.putUTF(var9);
               var6.putInt(var10.size());
               Iterator var11 = var10.keySet().iterator();

               while(var11.hasNext()) {
                  String var12 = (String)var11.next();
                  var6.putUTF(var12);
                  var6.putUTF((String)var10.get(var12));
               }
            }

            PacketTypes.PacketType.GetDBSchema.send(var5);
         }
      }

   }

   static void receiveGetTableResult(ByteBuffer var0, UdpConnection var1, short var2) throws SQLException {
      int var3 = var0.getInt();
      String var4 = GameWindow.ReadString(var0);
      ArrayList var5 = ServerWorldDatabase.instance.getTableResult(var4);

      for(int var6 = 0; var6 < udpEngine.connections.size(); ++var6) {
         UdpConnection var7 = (UdpConnection)udpEngine.connections.get(var6);
         if (var1 != null && var7.getConnectedGUID() == var1.getConnectedGUID()) {
            doTableResult(var7, var4, var5, 0, var3);
         }
      }

   }

   private static void doTableResult(UdpConnection var0, String var1, ArrayList<DBResult> var2, int var3, int var4) {
      int var5 = 0;
      boolean var6 = true;
      ByteBufferWriter var7 = var0.startPacket();
      PacketTypes.PacketType.GetTableResult.doPacket(var7);
      var7.putInt(var3);
      var7.putUTF(var1);
      if (var2.size() < var4) {
         var7.putInt(var2.size());
      } else if (var2.size() - var3 < var4) {
         var7.putInt(var2.size() - var3);
      } else {
         var7.putInt(var4);
      }

      for(int var8 = var3; var8 < var2.size(); ++var8) {
         DBResult var9 = null;

         try {
            var9 = (DBResult)var2.get(var8);
            var7.putInt(var9.getColumns().size());
         } catch (Exception var12) {
            var12.printStackTrace();
         }

         Iterator var10 = var9.getColumns().iterator();

         while(var10.hasNext()) {
            String var11 = (String)var10.next();
            var7.putUTF(var11);
            var7.putUTF((String)var9.getValues().get(var11));
         }

         ++var5;
         if (var5 >= var4) {
            var6 = false;
            PacketTypes.PacketType.GetTableResult.send(var0);
            doTableResult(var0, var1, var2, var3 + var5, var4);
            break;
         }
      }

      if (var6) {
         PacketTypes.PacketType.GetTableResult.send(var0);
      }

   }

   static void receiveExecuteQuery(ByteBuffer var0, UdpConnection var1, short var2) throws SQLException {
      if (var1.role.haveCapability(Capability.ModifyDB)) {
         try {
            String var3 = GameWindow.ReadString(var0);
            KahluaTable var4 = LuaManager.platform.newTable();
            var4.load(var0, 219);
            ServerWorldDatabase.instance.executeQuery(var3, var4);
         } catch (Throwable var5) {
            var5.printStackTrace();
         }

      }
   }

   static void receiveSendFactionInvite(ByteBuffer var0, UdpConnection var1, short var2) {
      String var3 = GameWindow.ReadString(var0);
      String var4 = GameWindow.ReadString(var0);
      String var5 = GameWindow.ReadString(var0);
      IsoPlayer var6 = getPlayerByUserName(var5);
      if (var6 != null) {
         Long var7 = (Long)IDToAddressMap.get(var6.getOnlineID());

         for(int var8 = 0; var8 < udpEngine.connections.size(); ++var8) {
            UdpConnection var9 = (UdpConnection)udpEngine.connections.get(var8);
            if (var9.getConnectedGUID() == var7) {
               ByteBufferWriter var10 = var9.startPacket();
               PacketTypes.PacketType.SendFactionInvite.doPacket(var10);
               var10.putUTF(var3);
               var10.putUTF(var4);
               PacketTypes.PacketType.SendFactionInvite.send(var9);
               break;
            }
         }

      }
   }

   static void receiveAcceptedFactionInvite(ByteBuffer var0, UdpConnection var1, short var2) {
      String var3 = GameWindow.ReadString(var0);
      String var4 = GameWindow.ReadString(var0);
      IsoPlayer var5 = getPlayerByUserName(var4);
      Long var6 = (Long)IDToAddressMap.get(var5.getOnlineID());

      for(int var7 = 0; var7 < udpEngine.connections.size(); ++var7) {
         UdpConnection var8 = (UdpConnection)udpEngine.connections.get(var7);
         if (var8.getConnectedGUID() == var6) {
            Faction var9 = Faction.getPlayerFaction(var8.username);
            if (var9 != null && var9.getName().equals(var3)) {
               ByteBufferWriter var10 = var8.startPacket();
               PacketTypes.PacketType.AcceptedFactionInvite.doPacket(var10);
               var10.putUTF(var3);
               var10.putUTF(var4);
               PacketTypes.PacketType.AcceptedFactionInvite.send(var8);
            }
         }
      }

   }

   static void receiveViewTickets(ByteBuffer var0, UdpConnection var1, short var2) throws SQLException {
      String var3 = GameWindow.ReadString(var0);
      if ("".equals(var3)) {
         var3 = null;
      }

      sendTickets(var3, var1);
   }

   private static void sendTickets(String var0, UdpConnection var1) throws SQLException {
      ArrayList var2 = ServerWorldDatabase.instance.getTickets(var0);

      for(int var3 = 0; var3 < udpEngine.connections.size(); ++var3) {
         UdpConnection var4 = (UdpConnection)udpEngine.connections.get(var3);
         if (var4.getConnectedGUID() == var1.getConnectedGUID()) {
            ByteBufferWriter var5 = var4.startPacket();
            PacketTypes.PacketType.ViewTickets.doPacket(var5);
            var5.putInt(var2.size());

            for(int var6 = 0; var6 < var2.size(); ++var6) {
               DBTicket var7 = (DBTicket)var2.get(var6);
               var5.putUTF(var7.getAuthor());
               var5.putUTF(var7.getMessage());
               var5.putInt(var7.getTicketID());
               if (var7.getAnswer() != null) {
                  var5.putByte((byte)1);
                  var5.putUTF(var7.getAnswer().getAuthor());
                  var5.putUTF(var7.getAnswer().getMessage());
                  var5.putInt(var7.getAnswer().getTicketID());
               } else {
                  var5.putByte((byte)0);
               }
            }

            PacketTypes.PacketType.ViewTickets.send(var4);
            break;
         }
      }

   }

   static void receiveAddTicket(ByteBuffer var0, UdpConnection var1, short var2) throws SQLException {
      String var3 = GameWindow.ReadString(var0);
      String var4 = GameWindow.ReadString(var0);
      int var5 = var0.getInt();
      if (var5 == -1) {
         sendAdminMessage("user " + var3 + " added a ticket <LINE> <LINE> " + var4, -1, -1, -1);
      }

      ServerWorldDatabase.instance.addTicket(var3, var4, var5);
      sendTickets(var3, var1);
   }

   static void receiveRemoveTicket(ByteBuffer var0, UdpConnection var1, short var2) throws SQLException {
      int var3 = var0.getInt();
      ServerWorldDatabase.instance.removeTicket(var3);
      sendTickets((String)null, var1);
   }

   public static boolean sendItemListNet(UdpConnection var0, IsoPlayer var1, ArrayList<InventoryItem> var2, IsoPlayer var3, String var4, String var5) {
      for(int var6 = 0; var6 < udpEngine.connections.size(); ++var6) {
         UdpConnection var7 = (UdpConnection)udpEngine.connections.get(var6);
         if (var0 == null || var7 != var0) {
            if (var3 != null) {
               boolean var8 = false;

               for(int var9 = 0; var9 < var7.players.length; ++var9) {
                  IsoPlayer var10 = var7.players[var9];
                  if (var10 != null && var10 == var3) {
                     var8 = true;
                     break;
                  }
               }

               if (!var8) {
                  continue;
               }
            }

            ByteBufferWriter var12 = var7.startPacket();
            PacketTypes.PacketType.SendItemListNet.doPacket(var12);
            var12.putByte((byte)(var3 != null ? 1 : 0));
            if (var3 != null) {
               var12.putShort(var3.getOnlineID());
            }

            var12.putByte((byte)(var1 != null ? 1 : 0));
            if (var1 != null) {
               var12.putShort(var1.getOnlineID());
            }

            GameWindow.WriteString(var12.bb, var4);
            var12.putByte((byte)(var5 != null ? 1 : 0));
            if (var5 != null) {
               GameWindow.WriteString(var12.bb, var5);
            }

            try {
               CompressIdenticalItems.save(var12.bb, var2, (IsoGameCharacter)null);
            } catch (Exception var11) {
               var11.printStackTrace();
               var7.cancelPacket();
               return false;
            }

            PacketTypes.PacketType.SendItemListNet.send(var7);
         }
      }

      return true;
   }

   static void receiveSendItemListNet(ByteBuffer var0, UdpConnection var1, short var2) {
      IsoPlayer var3 = null;
      if (var0.get() == 1) {
         var3 = (IsoPlayer)IDToPlayerMap.get(var0.getShort());
      }

      IsoPlayer var4 = null;
      if (var0.get() == 1) {
         var4 = (IsoPlayer)IDToPlayerMap.get(var0.getShort());
      }

      String var5 = GameWindow.ReadString(var0);
      String var6 = null;
      if (var0.get() == 1) {
         var6 = GameWindow.ReadString(var0);
      }

      ArrayList var7 = new ArrayList();

      try {
         CompressIdenticalItems.load(var0, 219, var7, (ArrayList)null);
      } catch (Exception var9) {
         var9.printStackTrace();
      }

      if (var3 == null) {
         LuaEventManager.triggerEvent("OnReceiveItemListNet", var4, var7, var3, var5, var6);
      } else {
         sendItemListNet(var1, var4, var7, var3, var5, var6);
      }

   }

   public static void sendPlayerDamagedByCarCrash(IsoPlayer var0, float var1) {
      UdpConnection var2 = getConnectionFromPlayer(var0);
      if (var2 != null) {
         ByteBufferWriter var3 = var2.startPacket();
         PacketTypes.PacketType.PlayerDamageFromCarCrash.doPacket(var3);
         var3.putFloat(var1);
         PacketTypes.PacketType.PlayerDamageFromCarCrash.send(var2);
      }
   }

   static void receiveClimateManagerPacket(ByteBuffer var0, UdpConnection var1, short var2) {
      ClimateManager var3 = ClimateManager.getInstance();
      if (var3 != null) {
         try {
            var3.receiveClimatePacket(var0, var1);
         } catch (Exception var5) {
            var5.printStackTrace();
         }
      }

   }

   static void receivePassengerMap(ByteBuffer var0, UdpConnection var1, short var2) {
      PassengerMap.serverReceivePacket(var0, var1);
   }

   static void receiveIsoRegionClientRequestFullUpdate(ByteBuffer var0, UdpConnection var1, short var2) {
      IsoRegions.receiveClientRequestFullDataChunks(var0, var1);
   }

   private static String isWorldVersionUnsupported() {
      String var10002 = ZomboidFileSystem.instance.getSaveDir();
      File var0 = new File(var10002 + File.separator + "Multiplayer" + File.separator + ServerName + File.separator + "map_t.bin");
      if (var0.exists()) {
         DebugLog.log("checking server WorldVersion in map_t.bin");

         try {
            FileInputStream var1 = new FileInputStream(var0);

            String var8;
            label93: {
               label92: {
                  String var14;
                  label112: {
                     try {
                        DataInputStream var2 = new DataInputStream(var1);

                        label87: {
                           label86: {
                              label85: {
                                 try {
                                    byte var3 = var2.readByte();
                                    byte var4 = var2.readByte();
                                    byte var5 = var2.readByte();
                                    byte var6 = var2.readByte();
                                    if (var3 != 71 || var4 != 77 || var5 != 84 || var6 != 77) {
                                       var14 = "The server savefile appears to be from an old version of the game and cannot be loaded.";
                                       break label85;
                                    }

                                    int var7 = var2.readInt();
                                    if (var7 <= 219) {
                                       if (var7 > 143) {
                                          break label87;
                                       }

                                       var8 = "The server savefile appears to be from a pre-animations version of the game and cannot be loaded.\nDue to the extent of changes required to implement animations, saves from earlier versions are not compatible.";
                                       break label86;
                                    }

                                    var8 = "The server savefile appears to be from a newer version of the game and cannot be loaded.";
                                 } catch (Throwable var11) {
                                    try {
                                       var2.close();
                                    } catch (Throwable var10) {
                                       var11.addSuppressed(var10);
                                    }

                                    throw var11;
                                 }

                                 var2.close();
                                 break label93;
                              }

                              var2.close();
                              break label112;
                           }

                           var2.close();
                           break label92;
                        }

                        var2.close();
                     } catch (Throwable var12) {
                        try {
                           var1.close();
                        } catch (Throwable var9) {
                           var12.addSuppressed(var9);
                        }

                        throw var12;
                     }

                     var1.close();
                     return null;
                  }

                  var1.close();
                  return var14;
               }

               var1.close();
               return var8;
            }

            var1.close();
            return var8;
         } catch (Exception var13) {
            var13.printStackTrace();
         }
      } else {
         DebugLog.log("map_t.bin does not exist, cannot determine the server's WorldVersion.  This is ok the first time a server is started.");
      }

      return null;
   }

   public String getPoisonousBerry() {
      return this.poisonousBerry;
   }

   public void setPoisonousBerry(String var1) {
      this.poisonousBerry = var1;
   }

   public String getPoisonousMushroom() {
      return this.poisonousMushroom;
   }

   public void setPoisonousMushroom(String var1) {
      this.poisonousMushroom = var1;
   }

   public String getDifficulty() {
      return this.difficulty;
   }

   public void setDifficulty(String var1) {
      this.difficulty = var1;
   }

   public static void transmitBrokenGlass(IsoGridSquare var0) {
      AddBrokenGlassPacket var1 = new AddBrokenGlassPacket();
      var1.set(var0);

      for(int var2 = 0; var2 < udpEngine.connections.size(); ++var2) {
         UdpConnection var3 = (UdpConnection)udpEngine.connections.get(var2);

         try {
            if (var3.RelevantTo((float)var0.getX(), (float)var0.getY())) {
               ByteBufferWriter var4 = var3.startPacket();
               PacketTypes.PacketType.AddBrokenGlass.doPacket(var4);
               var1.write(var4);
               PacketTypes.PacketType.AddBrokenGlass.send(var3);
            }
         } catch (Throwable var5) {
            var3.cancelPacket();
            ExceptionLogger.logException(var5);
         }
      }

   }

   public static void transmitBigWaterSplash(int var0, int var1, float var2, float var3) {
      for(int var4 = 0; var4 < udpEngine.connections.size(); ++var4) {
         UdpConnection var5 = (UdpConnection)udpEngine.connections.get(var4);

         try {
            if (var5.RelevantTo((float)var0, (float)var1)) {
               ByteBufferWriter var6 = var5.startPacket();
               PacketTypes.PacketType.StartFishSplash.doPacket(var6);
               var6.putInt(var0);
               var6.putInt(var1);
               var6.putFloat(var2);
               var6.putFloat(var3);
               PacketTypes.PacketType.StartFishSplash.send(var5);
            }
         } catch (Throwable var7) {
            var5.cancelPacket();
            ExceptionLogger.logException(var7);
         }
      }

   }

   public static void receiveBigWaterSplash(ByteBuffer var0, UdpConnection var1, short var2) {
      int var3 = var0.getInt();
      int var4 = var0.getInt();
      float var5 = var0.getFloat();
      float var6 = var0.getFloat();

      for(int var7 = 0; var7 < udpEngine.connections.size(); ++var7) {
         UdpConnection var8 = (UdpConnection)udpEngine.connections.get(var7);
         if (var8.getConnectedGUID() != var1.getConnectedGUID() && var8.RelevantTo((float)var3, (float)var4)) {
            try {
               ByteBufferWriter var9 = var8.startPacket();
               PacketTypes.PacketType.StartFishSplash.doPacket(var9);
               var9.putInt(var3);
               var9.putInt(var4);
               var9.putFloat(var5);
               var9.putFloat(var6);
               PacketTypes.PacketType.StartFishSplash.send(var8);
            } catch (Throwable var10) {
               var8.cancelPacket();
               ExceptionLogger.logException(var10);
            }
         }
      }

   }

   public static void transmitFishingData(int var0, int var1, HashMap<Long, Integer> var2, HashMap<Long, FishSchoolManager.ChumData> var3) {
      for(int var4 = 0; var4 < udpEngine.connections.size(); ++var4) {
         UdpConnection var5 = (UdpConnection)udpEngine.connections.get(var4);

         try {
            ByteBufferWriter var6 = var5.startPacket();
            PacketTypes.PacketType.FishingData.doPacket(var6);
            var6.putInt(var0);
            var6.putInt(var1);
            var6.putInt(var2.size());
            Iterator var7 = var2.entrySet().iterator();

            Map.Entry var8;
            while(var7.hasNext()) {
               var8 = (Map.Entry)var7.next();
               var6.putLong((Long)var8.getKey());
            }

            var6.putInt(var3.size());
            var7 = var3.entrySet().iterator();

            while(var7.hasNext()) {
               var8 = (Map.Entry)var7.next();
               var6.putLong((Long)var8.getKey());
               var6.putInt(((FishSchoolManager.ChumData)var8.getValue()).maxForceTime);
            }

            PacketTypes.PacketType.FishingData.send(var5);
         } catch (Throwable var9) {
            var5.cancelPacket();
            ExceptionLogger.logException(var9);
         }
      }

   }

   static void receiveFishingDataRequest(ByteBuffer var0, UdpConnection var1, short var2) {
      try {
         ByteBufferWriter var3 = var1.startPacket();
         PacketTypes.PacketType.FishingData.doPacket(var3);
         FishSchoolManager.getInstance().setFishingData(var3);
         PacketTypes.PacketType.FishingData.send(var1);
      } catch (Throwable var4) {
         var1.cancelPacket();
         ExceptionLogger.logException(var4);
      }

   }

   public static boolean isServerDropPackets() {
      return droppedPackets > 0;
   }

   static void receiveSyncPerks(ByteBuffer var0, UdpConnection var1, short var2) {
      byte var3 = var0.get();
      int var4 = var0.getInt();
      int var5 = var0.getInt();
      int var6 = var0.getInt();
      IsoPlayer var7 = getPlayerFromConnection(var1, var3);
      if (var7 != null) {
         var7.remoteSneakLvl = var4;
         var7.remoteStrLvl = var5;
         var7.remoteFitLvl = var6;

         for(int var8 = 0; var8 < udpEngine.connections.size(); ++var8) {
            UdpConnection var9 = (UdpConnection)udpEngine.connections.get(var8);
            if (var9.getConnectedGUID() != var1.getConnectedGUID()) {
               IsoPlayer var10 = getAnyPlayerFromConnection(var1);
               if (var10 != null) {
                  try {
                     ByteBufferWriter var11 = var9.startPacket();
                     PacketTypes.PacketType.SyncPerks.doPacket(var11);
                     var11.putShort(var7.OnlineID);
                     var11.putInt(var4);
                     var11.putInt(var5);
                     var11.putInt(var6);
                     PacketTypes.PacketType.SyncPerks.send(var9);
                  } catch (Throwable var12) {
                     var1.cancelPacket();
                     ExceptionLogger.logException(var12);
                  }
               }
            }
         }

      }
   }

   static void receiveSyncWeight(ByteBuffer var0, UdpConnection var1, short var2) {
      byte var3 = var0.get();
      double var4 = var0.getDouble();
      IsoPlayer var6 = getPlayerFromConnection(var1, var3);
      if (var6 != null) {
         for(int var7 = 0; var7 < udpEngine.connections.size(); ++var7) {
            UdpConnection var8 = (UdpConnection)udpEngine.connections.get(var7);
            if (var8.getConnectedGUID() != var1.getConnectedGUID()) {
               IsoPlayer var9 = getAnyPlayerFromConnection(var1);
               if (var9 != null) {
                  try {
                     ByteBufferWriter var10 = var8.startPacket();
                     PacketTypes.PacketType.SyncWeight.doPacket(var10);
                     var10.putShort(var6.OnlineID);
                     var10.putDouble(var4);
                     PacketTypes.PacketType.SyncWeight.send(var8);
                  } catch (Throwable var11) {
                     var1.cancelPacket();
                     ExceptionLogger.logException(var11);
                  }
               }
            }
         }

      }
   }

   static void receiveSyncEquippedRadioFreq(ByteBuffer var0, UdpConnection var1, short var2) {
      byte var3 = var0.get();
      int var4 = var0.getInt();
      ArrayList var5 = new ArrayList();

      for(int var6 = 0; var6 < var4; ++var6) {
         var5.add(var0.getInt());
      }

      IsoPlayer var13 = getPlayerFromConnection(var1, var3);
      if (var13 != null) {
         for(int var7 = 0; var7 < udpEngine.connections.size(); ++var7) {
            UdpConnection var8 = (UdpConnection)udpEngine.connections.get(var7);
            if (var8.getConnectedGUID() != var1.getConnectedGUID()) {
               IsoPlayer var9 = getAnyPlayerFromConnection(var1);
               if (var9 != null) {
                  try {
                     ByteBufferWriter var10 = var8.startPacket();
                     PacketTypes.PacketType.SyncEquippedRadioFreq.doPacket(var10);
                     var10.putShort(var13.OnlineID);
                     var10.putInt(var4);

                     for(int var11 = 0; var11 < var5.size(); ++var11) {
                        var10.putInt((Integer)var5.get(var11));
                     }

                     PacketTypes.PacketType.SyncEquippedRadioFreq.send(var8);
                  } catch (Throwable var12) {
                     var1.cancelPacket();
                     ExceptionLogger.logException(var12);
                  }
               }
            }
         }

      }
   }

   public static void sendRadioPostSilence() {
      for(int var0 = 0; var0 < udpEngine.connections.size(); ++var0) {
         UdpConnection var1 = (UdpConnection)udpEngine.connections.get(var0);
         if (var1.statistic.enable == 3) {
            sendShortStatistic(var1);
         }
      }

   }

   public static void sendRadioPostSilence(UdpConnection var0) {
      try {
         ByteBufferWriter var1 = var0.startPacket();
         PacketTypes.PacketType.RadioPostSilenceEvent.doPacket(var1);
         var1.putByte((byte)(ZomboidRadio.POST_RADIO_SILENCE ? 1 : 0));
         PacketTypes.PacketType.RadioPostSilenceEvent.send(var0);
      } catch (Exception var2) {
         var2.printStackTrace();
         var0.cancelPacket();
      }

   }

   static {
      discordBot = new DiscordBot(ServerName, (var0, var1) -> {
         ChatServer.getInstance().sendMessageFromDiscordToGeneralChat(var0, var1);
      });
      checksum = "";
      GameMap = "Muldraugh, KY";
      ip = "127.0.0.1";
      count = 0;
      SlotToConnection = new UdpConnection[512];
      PlayerToAddressMap = new HashMap();
      launched = false;
      consoleCommands = new ArrayList();
      MainLoopPlayerUpdateQ = new ConcurrentLinkedQueue();
      MainLoopNetDataHighPriorityQ = new ConcurrentLinkedQueue();
      MainLoopNetDataQ = new ConcurrentLinkedQueue();
      MainLoopNetData2 = new ArrayList();
      playerToCoordsMap = new HashMap();
      large_file_bb = ByteBuffer.allocate(2097152);
      previousSave = Calendar.getInstance().getTimeInMillis();
      droppedPackets = 0;
      countOfDroppedPackets = 0;
      countOfDroppedConnections = 0;
      removeZombiesConnection = null;
      removeAnimalsConnection = null;
      calcCountPlayersInRelevantPositionLimiter = new UpdateLimit(2000L);
      sendWorldMapPlayerPositionLimiter = new UpdateLimit(1000L);
      loginQueue = new LoginQueue();
      mainCycleExceptionLogCount = 25;
      tempPlayers = new ArrayList();
      MainLoopDelayedDisconnectQ = new ConcurrentHashMap();
      shutdownHook = new Thread() {
         public void run() {
            try {
               System.out.println("Shutdown handling started");
               CoopSlave.status("UI_ServerStatus_Terminated");
               DebugLog.log(DebugType.Network, "Server exited");
               if (GameServer.bSoftReset) {
                  return;
               }

               GameServer.bDone = true;
               ServerMap.instance.QueuedQuit();
               Set var1 = Thread.getAllStackTraces().keySet();
               Iterator var2 = var1.iterator();

               Thread var3;
               while(var2.hasNext()) {
                  var3 = (Thread)var2.next();
                  if (var3 != Thread.currentThread() && !var3.isDaemon() && var3.getClass().getName().startsWith("zombie")) {
                     System.out.println("Interrupting '" + var3.getClass() + "' termination");
                     var3.interrupt();
                  }
               }

               var2 = var1.iterator();

               while(var2.hasNext()) {
                  var3 = (Thread)var2.next();
                  if (var3 != Thread.currentThread() && !var3.isDaemon() && var3.isInterrupted()) {
                     System.out.println("Waiting '" + var3.getName() + "' termination");
                     var3.join();
                  }
               }
            } catch (InterruptedException var4) {
               System.out.println("Shutdown handling interrupted");
            }

            System.out.println("Shutdown handling finished");
         }
      };
   }

   private static class DelayedConnection implements IZomboidPacket {
      public UdpConnection connection;
      public boolean connect;
      public String hostString;
      public long timestamp;

      public DelayedConnection(UdpConnection var1, boolean var2) {
         this.connection = var1;
         this.connect = var2;
         if (var2) {
            try {
               this.hostString = var1.getInetSocketAddress().getHostString();
            } catch (Exception var4) {
               var4.printStackTrace();
            }
         }

         this.timestamp = System.currentTimeMillis() + (long)ServerOptions.getInstance().SafetyDisconnectDelay.getValue() * 2000L;
      }

      public boolean isConnect() {
         return this.connect;
      }

      public boolean isDisconnect() {
         return !this.connect;
      }

      public boolean isCooldown() {
         return System.currentTimeMillis() > this.timestamp;
      }

      public void connect() {
         LoggerManager.getLogger("user").write(String.format("Connection add index=%d guid=%d id=%s", this.connection.index, this.connection.getConnectedGUID(), this.connection.idStr));
         GameServer.udpEngine.connections.add(this.connection);
      }

      public void disconnect() {
         LoginQueue.disconnect(this.connection);
         ActionManager.getInstance().disconnectPlayer(this.connection);
         LoggerManager.getLogger("user").write(String.format("Connection remove index=%d guid=%d id=%s", this.connection.index, this.connection.getConnectedGUID(), this.connection.idStr));
         GameServer.udpEngine.connections.remove(this.connection);
         GameServer.disconnect(this.connection, "receive-disconnect");
      }
   }

   private static class s_performance {
      static final PerformanceProfileFrameProbe frameStep = new PerformanceProfileFrameProbe("GameServer.frameStep");
      static final PerformanceProfileProbe mainLoopDealWithNetData = new PerformanceProfileProbe("GameServer.mainLoopDealWithNetData");
      static final PerformanceProfileProbe RCONServerUpdate = new PerformanceProfileProbe("RCONServer.update");

      private s_performance() {
      }
   }

   private static final class CCFilter {
      String command;
      boolean allow;
      CCFilter next;

      private CCFilter() {
      }

      boolean matches(String var1) {
         return this.command.equals(var1) || "*".equals(this.command);
      }

      boolean passes(String var1) {
         if (this.matches(var1)) {
            return this.allow;
         } else {
            return this.next == null ? true : this.next.passes(var1);
         }
      }
   }
}
