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
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.AmbientSoundManager;
import zombie.AmbientStreamManager;
import zombie.DebugFileWatcher;
import zombie.GameProfiler;
import zombie.GameSounds;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.MapCollisionData;
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
import zombie.audio.GameSound;
import zombie.audio.GameSoundClip;
import zombie.characters.Faction;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.Safety;
import zombie.characters.SafetySystemManager;
import zombie.characters.Stats;
import zombie.characters.SurvivorDesc;
import zombie.characters.SurvivorFactory;
import zombie.characters.BodyDamage.BodyPart;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.characters.skills.CustomPerks;
import zombie.characters.skills.PerkFactory;
import zombie.commands.CommandBase;
import zombie.commands.PlayerType;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.Languages;
import zombie.core.PerformanceSettings;
import zombie.core.ProxyPrintStream;
import zombie.core.Rand;
import zombie.core.ThreadGroups;
import zombie.core.Translator;
import zombie.core.backup.ZipBackup;
import zombie.core.logger.ExceptionLogger;
import zombie.core.logger.LoggerManager;
import zombie.core.logger.ZLogger;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.profiling.PerformanceProfileFrameProbe;
import zombie.core.profiling.PerformanceProfileProbe;
import zombie.core.raknet.RakNetPeerInterface;
import zombie.core.raknet.RakVoice;
import zombie.core.raknet.UdpConnection;
import zombie.core.raknet.UdpEngine;
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
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.core.stash.StashSystem;
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
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.erosion.ErosionMain;
import zombie.gameStates.IngameState;
import zombie.globalObjects.SGlobalObjectNetwork;
import zombie.globalObjects.SGlobalObjects;
import zombie.inventory.CompressIdenticalItems;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.ItemPickerJava;
import zombie.inventory.RecipeManager;
import zombie.inventory.types.AlarmClock;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.Food;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.InventoryContainer;
import zombie.inventory.types.Radio;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.LosUtil;
import zombie.iso.ObjectsSyncRequests;
import zombie.iso.RoomDef;
import zombie.iso.SpawnPoints;
import zombie.iso.Vector2;
import zombie.iso.Vector3;
import zombie.iso.areas.NonPvpZone;
import zombie.iso.areas.SafeHouse;
import zombie.iso.areas.isoregion.IsoRegions;
import zombie.iso.objects.BSFurnace;
import zombie.iso.objects.IsoBarricade;
import zombie.iso.objects.IsoCompost;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoFire;
import zombie.iso.objects.IsoFireManager;
import zombie.iso.objects.IsoGenerator;
import zombie.iso.objects.IsoLightSwitch;
import zombie.iso.objects.IsoMannequin;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoTrap;
import zombie.iso.objects.IsoWaveSignal;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.iso.objects.RainManager;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.weather.ClimateManager;
import zombie.network.chat.ChatServer;
import zombie.network.packets.ActionPacket;
import zombie.network.packets.AddXp;
import zombie.network.packets.CleanBurn;
import zombie.network.packets.DeadPlayerPacket;
import zombie.network.packets.DeadZombiePacket;
import zombie.network.packets.Disinfect;
import zombie.network.packets.EventPacket;
import zombie.network.packets.PlaySoundPacket;
import zombie.network.packets.PlayWorldSoundPacket;
import zombie.network.packets.PlayerDataRequestPacket;
import zombie.network.packets.PlayerPacket;
import zombie.network.packets.RemoveBullet;
import zombie.network.packets.RemoveCorpseFromMap;
import zombie.network.packets.RemoveGlass;
import zombie.network.packets.RequestDataPacket;
import zombie.network.packets.SafetyPacket;
import zombie.network.packets.StartFire;
import zombie.network.packets.Stitch;
import zombie.network.packets.StopSoundPacket;
import zombie.network.packets.SyncClothingPacket;
import zombie.network.packets.SyncInjuriesPacket;
import zombie.network.packets.SyncNonPvpZonePacket;
import zombie.network.packets.SyncSafehousePacket;
import zombie.network.packets.ValidatePacket;
import zombie.network.packets.WaveSignal;
import zombie.network.packets.hit.HitCharacterPacket;
import zombie.popman.MPDebugInfo;
import zombie.popman.NetworkZombieManager;
import zombie.popman.NetworkZombiePacker;
import zombie.popman.ZombiePopulationManager;
import zombie.radio.ZomboidRadio;
import zombie.radio.devices.DeviceData;
import zombie.sandbox.CustomSandboxOptions;
import zombie.savefile.ServerPlayerDB;
import zombie.scripting.ScriptManager;
import zombie.util.PZSQLUtils;
import zombie.util.PublicServerUtil;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.Clipper;
import zombie.vehicles.PolygonalMap2;
import zombie.vehicles.VehicleManager;
import zombie.vehicles.VehiclePart;
import zombie.vehicles.VehiclesDB2;
import zombie.world.moddata.GlobalModData;
import zombie.worldMap.WorldMapRemotePlayer;
import zombie.worldMap.WorldMapRemotePlayers;

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
   public static final HashMap<String, Integer> transactionIDMap;
   public static final ObjectsSyncRequests worldObjectsServerSyncReq;
   public static String ip;
   static int count;
   private static final UdpConnection[] SlotToConnection;
   private static final HashMap<IsoPlayer, Long> PlayerToAddressMap;
   private static final ArrayList<Integer> alreadyRemoved;
   private static boolean bDone;
   private static boolean launched;
   private static final ArrayList<String> consoleCommands;
   private static final HashMap<Long, IZomboidPacket> MainLoopPlayerUpdate;
   private static final ConcurrentLinkedQueue<IZomboidPacket> MainLoopPlayerUpdateQ;
   private static final ConcurrentLinkedQueue<IZomboidPacket> MainLoopNetDataHighPriorityQ;
   private static final ConcurrentLinkedQueue<IZomboidPacket> MainLoopNetDataQ;
   private static final ArrayList<IZomboidPacket> MainLoopNetData2;
   private static final HashMap<Short, Vector2> playerToCoordsMap;
   private static final HashMap<Short, Integer> playerMovedToFastMap;
   private static ByteBuffer large_file_bb;
   private static long previousSave;
   private String poisonousBerry = null;
   private String poisonousMushroom = null;
   private String difficulty = "Hardcore";
   private static int droppedPackets;
   private static int countOfDroppedPackets;
   private static int countOfDroppedConnections;
   public static UdpConnection removeZombiesConnection;
   private static UpdateLimit calcCountPlayersInRelevantPositionLimiter;
   private static UpdateLimit sendWorldMapPlayerPositionLimiter;
   public static LoginQueue loginQueue;
   private static int mainCycleExceptionLogCount;
   public static Thread MainThread;
   public static final ArrayList<IsoPlayer> tempPlayers;

   public GameServer() {
   }

   public static void PauseAllClients() {
      String var0 = "[SERVERMSG] Server saving...Please wait";

      for(int var1 = 0; var1 < udpEngine.connections.size(); ++var1) {
         UdpConnection var2 = (UdpConnection)udpEngine.connections.get(var1);
         ByteBufferWriter var3 = var2.startPacket();
         PacketTypes.PacketType.StartPause.doPacket(var3);
         var3.putUTF(var0);
         PacketTypes.PacketType.StartPause.send(var2);
      }

   }

   public static void UnPauseAllClients() {
      String var0 = "[SERVERMSG] Server saved game...enjoy :)";

      for(int var1 = 0; var1 < udpEngine.connections.size(); ++var1) {
         UdpConnection var2 = (UdpConnection)udpEngine.connections.get(var1);
         ByteBufferWriter var3 = var2.startPacket();
         PacketTypes.PacketType.StopPause.doPacket(var3);
         var3.putUTF(var0);
         PacketTypes.PacketType.StopPause.send(var2);
      }

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
         } catch (FileNotFoundException var68) {
            var68.printStackTrace();
         }
      } else {
         try {
            String var10000 = ZomboidFileSystem.instance.getCacheDir();
            var74 = var10000 + File.separator + "server-console.txt";
            FileOutputStream var2 = new FileOutputStream(var74);
            PrintStream var3 = new PrintStream(var2, true);
            System.setOut(new ProxyPrintStream(System.out, var3));
            System.setErr(new ProxyPrintStream(System.err, var3));
         } catch (FileNotFoundException var67) {
            var67.printStackTrace();
         }
      }

      DebugLog.init();
      LoggerManager.init();
      DebugLog.log("cachedir set to \"" + ZomboidFileSystem.instance.getCacheDir() + "\"");
      if (bCoop) {
         try {
            setupCoop();
            CoopSlave.status("UI_ServerStatus_Initialising");
         } catch (FileNotFoundException var66) {
            var66.printStackTrace();
            SteamUtils.shutdown();
            System.exit(37);
            return;
         }
      }

      PZSQLUtils.init();
      Clipper.init();
      Rand.init();
      if (System.getProperty("debug") != null) {
         bDebug = true;
         Core.bDebug = true;
      }

      DebugLog.General.println("version=%s demo=%s", Core.getInstance().getVersion(), false);
      DebugLog.General.println("revision=%s date=%s time=%s", "", "", "");

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
                     } catch (IllegalArgumentException var64) {
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
                     } catch (IllegalArgumentException var65) {
                     }
                  }
               }
            }
         }
      }

      DebugLog.log("server name is \"" + ServerName + "\"");
      var74 = isWorldVersionUnsupported();
      if (var74 != null) {
         DebugLog.log(var74);
         CoopSlave.status(var74);
      } else {
         SteamUtils.init();
         RakNetPeerInterface.init();
         ZombiePopulationManager.init();

         try {
            ZomboidFileSystem.instance.init();
            Languages.instance.init();
            Translator.loadFiles();
         } catch (Exception var63) {
            DebugLog.General.printException(var63, "Exception Thrown", LogSeverity.Error);
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
            SteamGameServer.SetKeyValue("version", Core.getInstance().getVersion());
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

            SteamWorkshop.init();
            SteamGameServer.LogOnAnonymous();
            SteamGameServer.EnableHeartBeats(true);
            DebugLog.log("Waiting for response from Steam servers");

            while(true) {
               SteamUtils.runLoop();
               var91 = SteamGameServer.GetSteamServersConnectState();
               if (var91 == SteamGameServer.STEAM_SERVERS_CONNECTED) {
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
               } catch (InterruptedException var62) {
               }
            }
         }

         ZipBackup.onStartup();
         ZipBackup.onVersion();
         var4 = 0;

         try {
            ServerWorldDatabase.instance.create();
         } catch (ClassNotFoundException | SQLException var61) {
            var61.printStackTrace();
         }

         if (ServerOptions.instance.UPnP.getValue()) {
            DebugLog.log("Router detection/configuration starting.");
            DebugLog.log("If the server hangs here, set UPnP=false.");
            PortMapper.startup();
            if (PortMapper.discover()) {
               DebugLog.log("UPnP-enabled internet gateway found: " + PortMapper.getGatewayInfo());
               var5 = PortMapper.getExternalAddress();
               DebugLog.log("External IP address: " + var5);
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
         } catch (Exception var60) {
            DebugLog.General.printException(var60, "Exception Thrown", LogSeverity.Error);
            DebugLog.General.println("Server Terminated.");
         }

         LosUtil.init(100, 100);
         ChatServer.getInstance().init();
         DebugLog.log(DebugType.Network, "Loading world...");
         CoopSlave.status("UI_ServerStatus_LoadingWorld");

         try {
            ClimateManager.setInstance(new ClimateManager());
            IsoWorld.instance.init();
         } catch (Exception var59) {
            DebugLog.General.printException(var59, "Exception Thrown", LogSeverity.Error);
            DebugLog.General.println("Server Terminated.");
            CoopSlave.status("UI_ServerStatus_Terminated");
            return;
         }

         File var84 = ZomboidFileSystem.instance.getFileInCurrentSave("z_outfits.bin");
         if (!var84.exists()) {
            ServerOptions.instance.changeOption("ResetID", (new Integer(Rand.Next(100000000))).toString());
         }

         try {
            SpawnPoints.instance.initServer2();
         } catch (Exception var58) {
            var58.printStackTrace();
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

         try {
            startServer();
         } catch (ConnectException var57) {
            var57.printStackTrace();
            SteamUtils.shutdown();
            return;
         }

         if (SteamUtils.isSteamModeEnabled()) {
            DebugLog.log("##########\nServer Steam ID " + SteamGameServer.GetSteamID() + "\n##########");
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

               MPStatistic.getInstance().setPacketsLength((long)MainLoopNetData2.size());

               IZomboidPacket var20;
               int var102;
               for(var102 = 0; var102 < MainLoopNetData2.size(); ++var102) {
                  var20 = (IZomboidPacket)MainLoopNetData2.get(var102);
                  UdpConnection var21;
                  if (var20.isConnect()) {
                     var21 = ((DelayedConnection)var20).connection;
                     LoggerManager.getLogger("user").write("added connection index=" + var21.index + " " + ((DelayedConnection)var20).hostString);
                     udpEngine.connections.add(var21);
                  } else if (var20.isDisconnect()) {
                     var21 = ((DelayedConnection)var20).connection;
                     LoginQueue.disconnect(var21);
                     LoggerManager.getLogger("user").write(var21.idStr + " \"" + var21.username + "\" removed connection index=" + var21.index);
                     udpEngine.connections.remove(var21);
                     disconnect(var21, "receive-disconnect");
                  } else {
                     mainLoopDealWithNetData((ZomboidNetData)var20);
                  }
               }

               MainLoopPlayerUpdate.clear();

               for(var19 = (IZomboidPacket)MainLoopPlayerUpdateQ.poll(); var19 != null; var19 = (IZomboidPacket)MainLoopPlayerUpdateQ.poll()) {
                  ZomboidNetData var103 = (ZomboidNetData)var19;
                  long var105 = var103.connection * 4L + (long)var103.buffer.getShort(0);
                  ZomboidNetData var23 = (ZomboidNetData)MainLoopPlayerUpdate.put(var105, var103);
                  if (var23 != null) {
                     ZomboidNetDataPool.instance.discard(var23);
                  }
               }

               MainLoopNetData2.clear();
               MainLoopNetData2.addAll(MainLoopPlayerUpdate.values());
               MainLoopPlayerUpdate.clear();
               MPStatistic.getInstance().setPacketsLength((long)MainLoopNetData2.size());

               for(var102 = 0; var102 < MainLoopNetData2.size(); ++var102) {
                  var20 = (IZomboidPacket)MainLoopNetData2.get(var102);
                  GameServer.s_performance.mainLoopDealWithNetData.invokeAndMeasure((ZomboidNetData)var20, GameServer::mainLoopDealWithNetData);
               }

               MainLoopNetData2.clear();

               for(var19 = (IZomboidPacket)MainLoopNetDataQ.poll(); var19 != null; var19 = (IZomboidPacket)MainLoopNetDataQ.poll()) {
                  MainLoopNetData2.add(var19);
               }

               for(var102 = 0; var102 < MainLoopNetData2.size(); ++var102) {
                  if (var102 % 10 == 0 && (System.nanoTime() - var101) / 1000000L > 70L) {
                     if (droppedPackets == 0) {
                        DebugLog.log("Server is too busy. Server will drop updates of vehicle's physics. Server is closed for new connections.");
                     }

                     droppedPackets += 2;
                     countOfDroppedPackets += MainLoopNetData2.size() - var102;
                     break;
                  }

                  var20 = (IZomboidPacket)MainLoopNetData2.get(var102);
                  GameServer.s_performance.mainLoopDealWithNetData.invokeAndMeasure((ZomboidNetData)var20, GameServer::mainLoopDealWithNetData);
               }

               MainLoopNetData2.clear();
               if (droppedPackets == 1) {
                  DebugLog.log("Server is working normal. Server will not drop updates of vehicle's physics. The server is open for new connections. Server dropped " + countOfDroppedPackets + " packets and " + countOfDroppedConnections + " connections.");
                  countOfDroppedPackets = 0;
                  countOfDroppedConnections = 0;
               }

               droppedPackets = Math.max(0, Math.min(1000, droppedPackets - 1));
               if (!var85.Check()) {
                  long var111 = PZMath.clamp((5000000L - System.nanoTime() + var101) / 1000000L, 0L, 100L);
                  if (var111 > 0L) {
                     try {
                        MPStatistic.getInstance().Main.StartSleep();
                        Thread.sleep(var111);
                        MPStatistic.getInstance().Main.EndSleep();
                     } catch (InterruptedException var56) {
                        var56.printStackTrace();
                     }
                  }
               } else {
                  MPStatistic.getInstance().Main.Start();
                  ++IsoCamera.frameState.frameCount;
                  GameServer.s_performance.frameStep.start();

                  try {
                     timeSinceKeepAlive += GameTime.getInstance().getMultiplier();
                     MPStatistic.getInstance().ServerMapPreupdate.Start();
                     ServerMap.instance.preupdate();
                     MPStatistic.getInstance().ServerMapPreupdate.End();
                     int var104;
                     synchronized(consoleCommands) {
                        for(var104 = 0; var104 < consoleCommands.size(); ++var104) {
                           String var106 = (String)consoleCommands.get(var104);

                           try {
                              if (CoopSlave.instance == null || !CoopSlave.instance.handleCommand(var106)) {
                                 System.out.println(handleServerCommand(var106, (UdpConnection)null));
                              }
                           } catch (Exception var69) {
                              var69.printStackTrace();
                           }
                        }

                        consoleCommands.clear();
                     }

                     if (removeZombiesConnection != null) {
                        NetworkZombieManager.removeZombies(removeZombiesConnection);
                        removeZombiesConnection = null;
                     }

                     GameServer.s_performance.RCONServerUpdate.invokeAndMeasure(RCONServer::update);

                     try {
                        MapCollisionData.instance.updateGameState();
                        MPStatistic.getInstance().IngameStateUpdate.Start();
                        var89.update();
                        MPStatistic.getInstance().IngameStateUpdate.End();
                        VehicleManager.instance.serverUpdate();
                     } catch (Exception var55) {
                        var55.printStackTrace();
                     }

                     var102 = 0;
                     var104 = 0;

                     for(int var107 = 0; var107 < Players.size(); ++var107) {
                        IsoPlayer var22 = (IsoPlayer)Players.get(var107);
                        if (var22.isAlive()) {
                           if (!IsoWorld.instance.CurrentCell.getObjectList().contains(var22)) {
                              IsoWorld.instance.CurrentCell.getObjectList().add(var22);
                           }

                           ++var104;
                           if (var22.isAsleep()) {
                              ++var102;
                           }
                        }

                        ServerMap.instance.characterIn(var22);
                     }

                     setFastForward(ServerOptions.instance.SleepAllowed.getValue() && var104 > 0 && var102 == var104);
                     boolean var109 = calcCountPlayersInRelevantPositionLimiter.Check();

                     int var24;
                     int var108;
                     UdpConnection var110;
                     for(var108 = 0; var108 < udpEngine.connections.size(); ++var108) {
                        var110 = (UdpConnection)udpEngine.connections.get(var108);
                        if (var109) {
                           var110.calcCountPlayersInRelevantPosition();
                        }

                        for(var24 = 0; var24 < 4; ++var24) {
                           Vector3 var25 = var110.connectArea[var24];
                           if (var25 != null) {
                              ServerMap.instance.characterIn((int)var25.x, (int)var25.y, (int)var25.z);
                           }

                           ClientServerMap.characterIn(var110, var24);
                        }

                        if (var110.playerDownloadServer != null) {
                           var110.playerDownloadServer.update();
                        }
                     }

                     for(var108 = 0; var108 < IsoWorld.instance.CurrentCell.getObjectList().size(); ++var108) {
                        IsoMovingObject var112 = (IsoMovingObject)IsoWorld.instance.CurrentCell.getObjectList().get(var108);
                        if (var112 instanceof IsoPlayer && !Players.contains(var112)) {
                           DebugLog.log("Disconnected player in CurrentCell.getObjectList() removed");
                           IsoWorld.instance.CurrentCell.getObjectList().remove(var108--);
                        }
                     }

                     ++var4;
                     if (var4 > 150) {
                        for(var108 = 0; var108 < udpEngine.connections.size(); ++var108) {
                           var110 = (UdpConnection)udpEngine.connections.get(var108);

                           try {
                              if (var110.username == null && !var110.awaitingCoopApprove && !LoginQueue.isInTheQueue(var110) && var110.isConnectionAttemptTimeout()) {
                                 disconnect(var110, "connection-attempt-timeout");
                                 udpEngine.forceDisconnect(var110.getConnectedGUID(), "connection-attempt-timeout");
                              }
                           } catch (Exception var54) {
                              var54.printStackTrace();
                           }
                        }

                        var4 = 0;
                     }

                     worldObjectsServerSyncReq.serverSendRequests(udpEngine);
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
                     long var113 = var99 - var100;
                     var94 = 1000.0F / (float)var113;
                     if (!Float.isNaN(var94)) {
                        var98 = (float)((double)var98 + Math.min((double)(var94 - var98) * 0.05, 1.0));
                     }

                     GameTime.instance.FPSMultiplier = 60.0F / var98;
                     launchCommandHandler();
                     MPStatistic.getInstance().process(var113);
                     if (!SteamUtils.isSteamModeEnabled()) {
                        PublicServerUtil.update();
                        PublicServerUtil.updatePlayerCountIfChanged();
                     }

                     for(var24 = 0; var24 < udpEngine.connections.size(); ++var24) {
                        UdpConnection var114 = (UdpConnection)udpEngine.connections.get(var24);
                        if (var114.checksumState == UdpConnection.ChecksumState.Different && var114.checksumTime + 8000L < System.currentTimeMillis()) {
                           DebugLog.log("timed out connection because checksum was different");
                           var114.checksumState = UdpConnection.ChecksumState.Init;
                           var114.forceDisconnect("checksum-timeout");
                        } else {
                           var114.validator.update();
                           if (!var114.chunkObjectState.isEmpty()) {
                              for(int var26 = 0; var26 < var114.chunkObjectState.size(); var26 += 2) {
                                 short var27 = var114.chunkObjectState.get(var26);
                                 short var28 = var114.chunkObjectState.get(var26 + 1);
                                 if (!var114.RelevantTo((float)(var27 * 10 + 5), (float)(var28 * 10 + 5), (float)(var114.ChunkGridWidth * 4 * 10))) {
                                    var114.chunkObjectState.remove(var26, 2);
                                    var26 -= 2;
                                 }
                              }
                           }
                        }
                     }

                     if (sendWorldMapPlayerPositionLimiter.Check()) {
                        try {
                           sendWorldMapPlayerPosition();
                        } catch (Exception var52) {
                           boolean var115 = true;
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

         CoopSlave.status("UI_ServerStatus_Terminated");
         DebugLog.log(DebugType.Network, "Server exited");
         ServerGUI.shutdown();
         ServerPlayerDB.getInstance().close();
         VehiclesDB2.instance.Reset();
         SteamUtils.shutdown();
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
         String var3 = "admin";
         if (var1 != null) {
            var2 = var1.username;
            var3 = PlayerType.toString(var1.accessLevel);
         }

         if (var1 != null && var1.isCoopHost) {
            var3 = "admin";
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
      UdpConnection var4 = getConnectionFromPlayer(var0);
      if (var4 == null) {
         DebugLog.log("No connection found for user " + var0.getUsername());
      } else {
         ByteBufferWriter var5 = var4.startPacket();
         PacketTypes.PacketType.Teleport.doPacket(var5);
         var5.putByte((byte)0);
         var5.putFloat(var1);
         var5.putFloat(var2);
         var5.putFloat(var3);
         PacketTypes.PacketType.Teleport.send(var4);
         if (var4.players[0] != null && var4.players[0].getNetworkCharacterAI() != null) {
            var4.players[0].getNetworkCharacterAI().resetSpeedLimiter();
         }

      }
   }

   static void receiveTeleport(ByteBuffer var0, UdpConnection var1, short var2) {
      String var3 = GameWindow.ReadString(var0);
      float var4 = var0.getFloat();
      float var5 = var0.getFloat();
      float var6 = var0.getFloat();
      IsoPlayer var7 = getPlayerByRealUserName(var3);
      if (var7 != null) {
         UdpConnection var8 = getConnectionFromPlayer(var7);
         if (var8 != null) {
            ByteBufferWriter var9 = var8.startPacket();
            PacketTypes.PacketType.Teleport.doPacket(var9);
            var9.putByte((byte)var7.PlayerIndex);
            var9.putFloat(var4);
            var9.putFloat(var5);
            var9.putFloat(var6);
            PacketTypes.PacketType.Teleport.send(var8);
            if (var7.getNetworkCharacterAI() != null) {
               var7.getNetworkCharacterAI().resetSpeedLimiter();
            }

            if (var7.isAsleep()) {
               var7.setAsleep(false);
               var7.setAsleepTime(0.0F);
               sendWakeUpPlayer(var7, (UdpConnection)null);
            }

         }
      }
   }

   public static void sendPlayerExtraInfo(IsoPlayer var0, UdpConnection var1) {
      for(int var2 = 0; var2 < udpEngine.connections.size(); ++var2) {
         UdpConnection var3 = (UdpConnection)udpEngine.connections.get(var2);
         ByteBufferWriter var4 = var3.startPacket();
         PacketTypes.PacketType.ExtraInfo.doPacket(var4);
         var4.putShort(var0.OnlineID);
         var4.putUTF(var0.accessLevel);
         var4.putByte((byte)(var0.isGodMod() ? 1 : 0));
         var4.putByte((byte)(var0.isGhostMode() ? 1 : 0));
         var4.putByte((byte)(var0.isInvisible() ? 1 : 0));
         var4.putByte((byte)(var0.isNoClip() ? 1 : 0));
         var4.putByte((byte)(var0.isShowAdminTag() ? 1 : 0));
         PacketTypes.PacketType.ExtraInfo.send(var3);
      }

   }

   static void receiveExtraInfo(ByteBuffer var0, UdpConnection var1, short var2) {
      short var3 = var0.getShort();
      boolean var4 = var0.get() == 1;
      boolean var5 = var0.get() == 1;
      boolean var6 = var0.get() == 1;
      boolean var7 = var0.get() == 1;
      boolean var8 = var0.get() == 1;
      boolean var9 = var0.get() == 1;
      IsoPlayer var10 = getPlayerFromConnection(var1, var3);
      if (var10 != null) {
         var10.setGodMod(var4);
         var10.setGhostMode(var5);
         var10.setInvisible(var6);
         var10.setNoClip(var7);
         var10.setShowAdminTag(var8);
         var10.setCanHearAll(var9);
         sendPlayerExtraInfo(var10, var1);
      }

   }

   static void receiveAddXp(ByteBuffer var0, UdpConnection var1, short var2) {
      AddXp var3 = new AddXp();
      var3.parse(var0, var1);
      if (var3.isConsistent() && var3.validate(var1)) {
         if (!canModifyPlayerStats(var1, var3.target.getCharacter())) {
            PacketTypes.PacketType.AddXP.onUnauthorized(var1);
         } else {
            var3.process();
            if (canModifyPlayerStats(var1, (IsoPlayer)null)) {
               var3.target.getCharacter().getXp().recalcSumm();
            }

            for(int var4 = 0; var4 < udpEngine.connections.size(); ++var4) {
               UdpConnection var5 = (UdpConnection)udpEngine.connections.get(var4);
               if (var5.getConnectedGUID() != var1.getConnectedGUID() && var5.getConnectedGUID() == (Long)PlayerToAddressMap.get(var3.target.getCharacter())) {
                  ByteBufferWriter var6 = var5.startPacket();
                  PacketTypes.PacketType.AddXP.doPacket(var6);
                  var3.write(var6);
                  PacketTypes.PacketType.AddXP.send(var5);
               }
            }

         }
      }
   }

   private static boolean canSeePlayerStats(UdpConnection var0) {
      return var0.accessLevel != 1;
   }

   private static boolean canModifyPlayerStats(UdpConnection var0, IsoPlayer var1) {
      return (var0.accessLevel & 56) != 0 || var0.havePlayer(var1);
   }

   static void receiveSyncXP(ByteBuffer var0, UdpConnection var1, short var2) {
      IsoPlayer var3 = (IsoPlayer)IDToPlayerMap.get(var0.getShort());
      if (var3 != null) {
         if (!canModifyPlayerStats(var1, var3)) {
            PacketTypes.PacketType.SyncXP.onUnauthorized(var1);
         } else {
            if (var3 != null && !var3.isDead()) {
               try {
                  var3.getXp().load(var0, 195);
               } catch (IOException var9) {
                  var9.printStackTrace();
               }

               for(int var4 = 0; var4 < udpEngine.connections.size(); ++var4) {
                  UdpConnection var5 = (UdpConnection)udpEngine.connections.get(var4);
                  if (var5.getConnectedGUID() != var1.getConnectedGUID()) {
                     ByteBufferWriter var6 = var5.startPacket();
                     PacketTypes.PacketType.SyncXP.doPacket(var6);
                     var6.putShort(var3.getOnlineID());

                     try {
                        var3.getXp().save(var6.bb);
                     } catch (IOException var8) {
                        var8.printStackTrace();
                     }

                     PacketTypes.PacketType.SyncXP.send(var5);
                  }
               }
            }

         }
      }
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
                  var7.accessLevel = PlayerType.fromString(var4.accessLevel);
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
      Rand.init();
      DebugFileWatcher.instance.init();
      ArrayList var0 = new ArrayList(ServerMods);
      ZomboidFileSystem.instance.loadMods(var0);
      LuaManager.init();
      PerkFactory.init();
      CustomPerks.instance.init();
      CustomPerks.instance.initLua();
      AssetManagers var1 = GameWindow.assetManagers;
      AiSceneAssetManager.instance.create(AiSceneAsset.ASSET_TYPE, var1);
      AnimationAssetManager.instance.create(AnimationAsset.ASSET_TYPE, var1);
      AnimNodeAssetManager.instance.create(AnimationAsset.ASSET_TYPE, var1);
      ClothingItemAssetManager.instance.create(ClothingItem.ASSET_TYPE, var1);
      MeshAssetManager.instance.create(ModelMesh.ASSET_TYPE, var1);
      ModelAssetManager.instance.create(Model.ASSET_TYPE, var1);
      TextureIDAssetManager.instance.create(TextureID.ASSET_TYPE, var1);
      TextureAssetManager.instance.create(Texture.ASSET_TYPE, var1);
      if (GUICommandline && !bSoftReset) {
         ServerGUI.init();
      }

      CustomSandboxOptions.instance.init();
      CustomSandboxOptions.instance.initInstance(SandboxOptions.instance);
      ScriptManager.instance.Load();
      ClothingDecals.init();
      BeardStyles.init();
      HairStyles.init();
      OutfitManager.init();
      if (!bSoftReset) {
         JAssImpImporter.Init();
         ModelManager.NoOpenGL = !ServerGUI.isCreated();
         ModelManager.instance.create();
         System.out.println("LOADING ASSETS: START");

         while(GameWindow.fileSystem.hasWork()) {
            GameWindow.fileSystem.updateAsyncTransactions();
         }

         System.out.println("LOADING ASSETS: FINISH");
      }

      try {
         LuaManager.initChecksum();
         LuaManager.LoadDirBase("shared");
         LuaManager.LoadDirBase("client", true);
         LuaManager.LoadDirBase("server");
         LuaManager.finishChecksum();
      } catch (Exception var3) {
         var3.printStackTrace();
      }

      RecipeManager.LoadedAfterLua();
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

      udpEngine = new UdpEngine(DEFAULT_PORT, UDPPort, ServerOptions.getInstance().getMaxPlayers(), var0, true);
      DebugLog.log(DebugType.Network, "*** SERVER STARTED ****");
      DebugLog.log(DebugType.Network, "*** Steam is " + (SteamUtils.isSteamModeEnabled() ? "enabled" : "not enabled"));
      if (SteamUtils.isSteamModeEnabled()) {
         DebugLog.log(DebugType.Network, "Server is listening on port " + DEFAULT_PORT + " (for Steam connection) and port " + UDPPort + " (for UDPRakNet connection)");
         DebugLog.log(DebugType.Network, "Clients should use " + DEFAULT_PORT + " port for connections");
      } else {
         DebugLog.log(DebugType.Network, "server is listening on port " + DEFAULT_PORT);
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
                  DebugLog.General.error("Error with packet of type: " + var0.type + " for " + var2.username);
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
         for(int var6 = 0; var6 < udpEngine.connections.size(); ++var6) {
            UdpConnection var7 = (UdpConnection)udpEngine.connections.get(var6);
            if (var7.getConnectedGUID() != var1.getConnectedGUID() && var7.getConnectedGUID() == (Long)PlayerToAddressMap.get(var5)) {
               ByteBufferWriter var8 = var7.startPacket();
               PacketTypes.PacketType.InvMngRemoveItem.doPacket(var8);
               var8.putInt(var3);
               PacketTypes.PacketType.InvMngRemoveItem.send(var7);
               break;
            }
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

   static void receiveRequestZipList(ByteBuffer var0, UdpConnection var1, short var2) throws Exception {
      if (!var1.wasInLoadingQueue) {
         kick(var1, "UI_Policy_Kick", "The server received an invalid request");
      }

      if (var1.playerDownloadServer != null) {
         var1.playerDownloadServer.receiveRequestArray(var0);
      }

   }

   static void receiveRequestLargeAreaZip(ByteBuffer var0, UdpConnection var1, short var2) {
      if (!var1.wasInLoadingQueue) {
         kick(var1, "UI_Policy_Kick", "The server received an invalid request");
      }

      if (var1.playerDownloadServer != null) {
         int var3 = var0.getInt();
         int var4 = var0.getInt();
         int var5 = var0.getInt();
         var1.connectArea[0] = new Vector3((float)var3, (float)var4, (float)var5);
         var1.ChunkGridWidth = var5;
         ZombiePopulationManager.instance.updateLoadedAreas();
      }

   }

   static void receiveNotRequiredInZip(ByteBuffer var0, UdpConnection var1, short var2) {
      if (var1.playerDownloadServer != null) {
         var1.playerDownloadServer.receiveCancelRequest(var0);
      }

   }

   static void receiveLogin(ByteBuffer var0, UdpConnection var1, short var2) {
      ConnectionManager.log("receive-packet", "login", var1);
      String var3 = GameWindow.ReadString(var0).trim();
      String var4 = GameWindow.ReadString(var0).trim();
      String var5 = GameWindow.ReadString(var0).trim();
      ByteBufferWriter var6;
      if (!var5.equals(Core.getInstance().getVersion())) {
         var6 = var1.startPacket();
         PacketTypes.PacketType.AccessDenied.doPacket(var6);
         LoggerManager.getLogger("user").write("access denied: user \"" + var3 + "\" client version (" + var5 + ") does not match server version (" + Core.getInstance().getVersion() + ")");
         var6.putUTF("ClientVersionMismatch##" + var5 + "##" + Core.getInstance().getVersion());
         PacketTypes.PacketType.AccessDenied.send(var1);
         ConnectionManager.log("access-denied", "version-mismatch", var1);
         var1.forceDisconnect("access-denied-client-version");
      }

      var1.wasInLoadingQueue = false;
      var1.ip = var1.getInetSocketAddress().getHostString();
      var1.validator.reset();
      var1.idStr = var1.ip;
      if (SteamUtils.isSteamModeEnabled()) {
         var1.steamID = udpEngine.getClientSteamID(var1.getConnectedGUID());
         if (var1.steamID == -1L) {
            var6 = var1.startPacket();
            PacketTypes.PacketType.AccessDenied.doPacket(var6);
            LoggerManager.getLogger("user").write("access denied: The client \"" + var3 + "\" did not complete the connection and authorization procedure in zombienet");
            var6.putUTF("ClientIsNofFullyConnectedInZombienet");
            PacketTypes.PacketType.AccessDenied.send(var1);
            ConnectionManager.log("access-denied", "znet-error", var1);
            var1.forceDisconnect("access-denied-zombienet-connect");
         }

         var1.ownerID = udpEngine.getClientOwnerSteamID(var1.getConnectedGUID());
         var1.idStr = SteamUtils.convertSteamIDToString(var1.steamID);
         if (var1.steamID != var1.ownerID) {
            String var10001 = var1.idStr;
            var1.idStr = var10001 + "(owner=" + SteamUtils.convertSteamIDToString(var1.ownerID) + ")";
         }
      }

      var1.password = var4;
      LoggerManager.getLogger("user").write(var1.idStr + " \"" + var3 + "\" attempting to join");
      ServerWorldDatabase.LogonResult var13;
      ByteBufferWriter var17;
      if (CoopSlave.instance != null && SteamUtils.isSteamModeEnabled()) {
         for(int var14 = 0; var14 < udpEngine.connections.size(); ++var14) {
            UdpConnection var18 = (UdpConnection)udpEngine.connections.get(var14);
            if (var18 != var1 && var18.steamID == var1.steamID) {
               LoggerManager.getLogger("user").write("access denied: user \"" + var3 + "\" already connected");
               var17 = var1.startPacket();
               PacketTypes.PacketType.AccessDenied.doPacket(var17);
               var17.putUTF("AlreadyConnected");
               PacketTypes.PacketType.AccessDenied.send(var1);
               ConnectionManager.log("access-denied", "already-connected-steamid", var1);
               var1.forceDisconnect("access-denied-already-connected-cs");
               return;
            }
         }

         var1.username = var3;
         var1.usernames[0] = var3;
         var1.isCoopHost = udpEngine.connections.size() == 1;
         DebugLog.Multiplayer.debugln(var1.idStr + " isCoopHost=" + var1.isCoopHost);
         var1.accessLevel = 1;
         if (!ServerOptions.instance.DoLuaChecksum.getValue()) {
            var1.checksumState = UdpConnection.ChecksumState.Done;
         }

         if (getPlayerCount() >= ServerOptions.getInstance().getMaxPlayers()) {
            var6 = var1.startPacket();
            PacketTypes.PacketType.AccessDenied.doPacket(var6);
            var6.putUTF("ServerFull");
            PacketTypes.PacketType.AccessDenied.send(var1);
            ConnectionManager.log("access-denied", "server-full", var1);
            var1.forceDisconnect("access-denied-server-full-cs");
         } else {
            if (isServerDropPackets() && ServerOptions.instance.DenyLoginOnOverloadedServer.getValue()) {
               var6 = var1.startPacket();
               PacketTypes.PacketType.AccessDenied.doPacket(var6);
               LoggerManager.getLogger("user").write("access denied: user \"" + var3 + "\" Server is too busy");
               var6.putUTF("Server is too busy.");
               PacketTypes.PacketType.AccessDenied.send(var1);
               ConnectionManager.log("access-denied", "server-busy", var1);
               var1.forceDisconnect("access-denied-server-busy-cs");
               ++countOfDroppedConnections;
            }

            LoggerManager.getLogger("user").write(var1.idStr + " \"" + var3 + "\" allowed to join");
            ServerWorldDatabase var10002 = ServerWorldDatabase.instance;
            Objects.requireNonNull(var10002);
            var13 = var10002.new LogonResult();
            var13.accessLevel = PlayerType.toString(var1.accessLevel);
            receiveClientConnect(var1, var13);
         }
      } else {
         var13 = ServerWorldDatabase.instance.authClient(var3, var4, var1.ip, var1.steamID);
         ByteBufferWriter var16;
         if (var13.bAuthorized) {
            int var7;
            for(var7 = 0; var7 < udpEngine.connections.size(); ++var7) {
               UdpConnection var8 = (UdpConnection)udpEngine.connections.get(var7);

               for(int var9 = 0; var9 < 4; ++var9) {
                  if (var3.equals(var8.usernames[var9])) {
                     LoggerManager.getLogger("user").write("access denied: user \"" + var3 + "\" already connected");
                     ByteBufferWriter var10 = var1.startPacket();
                     PacketTypes.PacketType.AccessDenied.doPacket(var10);
                     var10.putUTF("AlreadyConnected");
                     PacketTypes.PacketType.AccessDenied.send(var1);
                     ConnectionManager.log("access-denied", "already-connected-username", var1);
                     var1.forceDisconnect("access-denied-already-connected-username");
                     return;
                  }
               }
            }

            var1.username = var3;
            var1.usernames[0] = var3;
            transactionIDMap.put(var3, var13.transactionID);
            if (CoopSlave.instance != null) {
               var1.isCoopHost = udpEngine.connections.size() == 1;
               DebugLog.log(var1.idStr + " isCoopHost=" + var1.isCoopHost);
            }

            var1.accessLevel = PlayerType.fromString(var13.accessLevel);
            var1.preferredInQueue = var13.priority;
            if (!ServerOptions.instance.DoLuaChecksum.getValue() || var13.accessLevel.equals("admin")) {
               var1.checksumState = UdpConnection.ChecksumState.Done;
            }

            if (!var13.accessLevel.equals("") && getPlayerCount() >= ServerOptions.getInstance().getMaxPlayers()) {
               var16 = var1.startPacket();
               PacketTypes.PacketType.AccessDenied.doPacket(var16);
               var16.putUTF("ServerFull");
               PacketTypes.PacketType.AccessDenied.send(var1);
               ConnectionManager.log("access-denied", "server-full-no-admin", var1);
               var1.forceDisconnect("access-denied-server-full");
               return;
            }

            if (!ServerWorldDatabase.instance.containsUser(var3) && ServerWorldDatabase.instance.containsCaseinsensitiveUser(var3)) {
               var16 = var1.startPacket();
               PacketTypes.PacketType.AccessDenied.doPacket(var16);
               var16.putUTF("InvalidUsername");
               PacketTypes.PacketType.AccessDenied.send(var1);
               ConnectionManager.log("access-denied", "invalid-username", var1);
               var1.forceDisconnect("access-denied-invalid-username");
               return;
            }

            var7 = var1.getAveragePing();
            DebugLog.Multiplayer.debugln("User %s ping %d ms", var1.username, var7);
            if (MPStatistics.doKickWhileLoading(var1, (long)var7)) {
               var17 = var1.startPacket();
               PacketTypes.PacketType.AccessDenied.doPacket(var17);
               LoggerManager.getLogger("user").write("access denied: user \"" + var3 + "\" ping is too high");
               var17.putUTF("Ping");
               PacketTypes.PacketType.AccessDenied.send(var1);
               ConnectionManager.log("access-denied", "ping-limit", var1);
               var1.forceDisconnect("access-denied-ping-limit");
               return;
            }

            if (var13.newUser) {
               try {
                  ServerWorldDatabase.instance.addUser(var3, var4);
                  LoggerManager.getLogger("user").write(var1.idStr + " \"" + var3 + "\" was added");
               } catch (SQLException var11) {
                  DebugLog.Multiplayer.printException(var11, "ServerWorldDatabase.addUser error", LogSeverity.Error);
               }
            }

            LoggerManager.getLogger("user").write(var1.idStr + " \"" + var3 + "\" allowed to join");

            try {
               if (ServerOptions.instance.AutoCreateUserInWhiteList.getValue() && !ServerWorldDatabase.instance.containsUser(var3)) {
                  ServerWorldDatabase.instance.addUser(var3, var4);
               } else {
                  ServerWorldDatabase.instance.setPassword(var3, var4);
               }
            } catch (Exception var12) {
               var12.printStackTrace();
            }

            ServerWorldDatabase.instance.updateLastConnectionDate(var3, var4);
            if (SteamUtils.isSteamModeEnabled()) {
               String var15 = SteamUtils.convertSteamIDToString(var1.steamID);
               ServerWorldDatabase.instance.setUserSteamID(var3, var15);
            }

            receiveClientConnect(var1, var13);
         } else {
            var16 = var1.startPacket();
            PacketTypes.PacketType.AccessDenied.doPacket(var16);
            if (var13.banned) {
               LoggerManager.getLogger("user").write("access denied: user \"" + var3 + "\" is banned");
               if (var13.bannedReason != null && !var13.bannedReason.isEmpty()) {
                  var16.putUTF("BannedReason##" + var13.bannedReason);
               } else {
                  var16.putUTF("Banned");
               }
            } else if (!var13.bAuthorized) {
               LoggerManager.getLogger("user").write("access denied: user \"" + var3 + "\" reason \"" + var13.dcReason + "\"");
               var16.putUTF(var13.dcReason != null ? var13.dcReason : "AccessDenied");
            }

            PacketTypes.PacketType.AccessDenied.send(var1);
            ConnectionManager.log("access-denied", "unauthorized", var1);
            var1.forceDisconnect("access-denied-unauthorized");
         }

      }
   }

   static void receiveSendInventory(ByteBuffer var0, UdpConnection var1, short var2) {
      short var3 = var0.getShort();
      Long var4 = (Long)IDToAddressMap.get(var3);
      if (var4 != null) {
         for(int var5 = 0; var5 < udpEngine.connections.size(); ++var5) {
            UdpConnection var6 = (UdpConnection)udpEngine.connections.get(var5);
            if (var6.getConnectedGUID() == var4) {
               ByteBufferWriter var7 = var6.startPacket();
               PacketTypes.PacketType.SendInventory.doPacket(var7);
               var7.bb.put(var0);
               PacketTypes.PacketType.SendInventory.send(var6);
               break;
            }
         }
      }

   }

   static void receivePlayerStartPMChat(ByteBuffer var0, UdpConnection var1, short var2) {
      ChatServer.getInstance().processPlayerStartWhisperChatPacket(var0);
   }

   static void receiveRequestInventory(ByteBuffer var0, UdpConnection var1, short var2) {
      short var3 = var0.getShort();
      short var4 = var0.getShort();
      Long var5 = (Long)IDToAddressMap.get(var4);
      if (var5 != null) {
         for(int var6 = 0; var6 < udpEngine.connections.size(); ++var6) {
            UdpConnection var7 = (UdpConnection)udpEngine.connections.get(var6);
            if (var7.getConnectedGUID() == var5) {
               ByteBufferWriter var8 = var7.startPacket();
               PacketTypes.PacketType.RequestInventory.doPacket(var8);
               var8.putShort(var3);
               PacketTypes.PacketType.RequestInventory.send(var7);
               break;
            }
         }
      }

   }

   static void receiveStatistic(ByteBuffer var0, UdpConnection var1, short var2) {
      try {
         var1.statistic.parse(var0);
      } catch (Exception var4) {
         var4.printStackTrace();
      }

   }

   static void receiveStatisticRequest(ByteBuffer var0, UdpConnection var1, short var2) {
      if (var1.accessLevel != 32 && !Core.bDebug) {
         DebugLog.General.error("User " + var1.username + " has no rights to access statistics.");
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
      try {
         if (var0.authOwner == null) {
            return;
         }

         ByteBufferWriter var3 = var0.authOwner.startPacket();
         PacketTypes.PacketType.ZombieControl.doPacket(var3);
         var3.putShort(var0.OnlineID);
         var3.putShort(var1);
         var3.putInt(var2);
         PacketTypes.PacketType.ZombieControl.send(var0.authOwner);
      } catch (Exception var4) {
         var4.printStackTrace();
      }

   }

   static void receivePlayerUpdate(ByteBuffer var0, UdpConnection var1, short var2) {
      if (var1.checksumState != UdpConnection.ChecksumState.Done) {
         kick(var1, "UI_Policy_Kick", (String)null);
         var1.forceDisconnect("kick-checksum");
      } else {
         PlayerPacket var3 = PlayerPacket.l_receive.playerPacket;
         var3.parse(var0, var1);
         IsoPlayer var4 = getPlayerFromConnection(var1, var3.id);

         try {
            if (var4 == null) {
               DebugLog.General.error("receivePlayerUpdate: Server received position for unknown player (id:" + var3.id + "). Server will ignore this data.");
            } else {
               if (var1.accessLevel == 1 && var4.networkAI.doCheckAccessLevel() && (var3.booleanVariables & (!SystemDisabler.getAllowDebugConnections() && !SystemDisabler.getOverrideServerConnectDebugCheck() ? '\uf000' : '쀀')) != 0 && ServerOptions.instance.AntiCheatProtectionType12.getValue() && PacketValidator.checkUser(var1)) {
                  PacketValidator.doKickUser(var1, var3.getClass().getSimpleName(), "Type12", (String)null);
               }

               if (!var4.networkAI.checkPosition(var1, var4, (float)PZMath.fastfloor(var3.realx), (float)PZMath.fastfloor(var3.realy))) {
                  return;
               }

               if (!var4.networkAI.isSetVehicleHit()) {
                  var4.networkAI.parse(var3);
               }

               var4.bleedingLevel = var3.bleedingLevel;
               if (var4.networkAI.distance.getLength() > (float)IsoChunkMap.ChunkWidthInTiles) {
                  MPStatistic.getInstance().teleport();
               }

               var1.ReleventPos[var4.PlayerIndex].x = var3.realx;
               var1.ReleventPos[var4.PlayerIndex].y = var3.realy;
               var1.ReleventPos[var4.PlayerIndex].z = (float)var3.realz;
               var3.id = var4.getOnlineID();
            }
         } catch (Exception var8) {
            var8.printStackTrace();
         }

         if (ServerOptions.instance.KickFastPlayers.getValue()) {
            Vector2 var5 = (Vector2)playerToCoordsMap.get(Integer.valueOf(var3.id));
            if (var5 == null) {
               var5 = new Vector2();
               var5.x = var3.x;
               var5.y = var3.y;
               playerToCoordsMap.put(var3.id, var5);
            } else {
               if (!var4.accessLevel.equals("") && !var4.isGhostMode() && (Math.abs(var3.x - var5.x) > 4.0F || Math.abs(var3.y - var5.y) > 4.0F)) {
                  if (playerMovedToFastMap.get(var3.id) == null) {
                     playerMovedToFastMap.put(var3.id, 1);
                  } else {
                     playerMovedToFastMap.put(var3.id, (Integer)playerMovedToFastMap.get(Integer.valueOf(var3.id)) + 1);
                  }

                  ZLogger var10000 = LoggerManager.getLogger("admin");
                  String var10001 = var4.getDisplayName();
                  var10000.write(var10001 + " go too fast (" + playerMovedToFastMap.get(Integer.valueOf(var3.id)) + " times)");
                  if ((Integer)playerMovedToFastMap.get(var3.id) == 10) {
                     LoggerManager.getLogger("admin").write(var4.getDisplayName() + " kicked for going too fast");
                     kick(var1, "UI_Policy_Kick", (String)null);
                     var1.forceDisconnect("kick-fast-player");
                     return;
                  }
               }

               var5.x = var3.x;
               var5.y = var3.y;
            }
         }

         if (var4 != null) {
            for(int var9 = 0; var9 < udpEngine.connections.size(); ++var9) {
               UdpConnection var6 = (UdpConnection)udpEngine.connections.get(var9);
               if (var1.getConnectedGUID() != var6.getConnectedGUID() && var6.isFullyConnected() && (var4.checkCanSeeClient(var6) && var6.RelevantTo(var3.x, var3.y) || var2 == PacketTypes.PacketType.PlayerUpdateReliable.getId() && (var6.accessLevel > var1.accessLevel || var1.accessLevel == 32))) {
                  ByteBufferWriter var7 = var6.startPacket();
                  ((PacketTypes.PacketType)PacketTypes.packetTypes.get(var2)).doPacket(var7);
                  var0.position(0);
                  var0.position(2);
                  var7.bb.putShort(var4.getOnlineID());
                  var7.bb.put(var0);
                  ((PacketTypes.PacketType)PacketTypes.packetTypes.get(var2)).send(var6);
               }
            }
         }

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

   static void receiveReadAnnotedMap(ByteBuffer var0, UdpConnection var1, short var2) {
      String var3 = GameWindow.ReadString(var0);
      StashSystem.prepareBuildingStash(var3);
   }

   static void receiveTradingUIRemoveItem(ByteBuffer var0, UdpConnection var1, short var2) {
      short var3 = var0.getShort();
      short var4 = var0.getShort();
      int var5 = var0.getInt();
      Long var6 = (Long)IDToAddressMap.get(var4);
      if (var6 != null) {
         for(int var7 = 0; var7 < udpEngine.connections.size(); ++var7) {
            UdpConnection var8 = (UdpConnection)udpEngine.connections.get(var7);
            if (var8.getConnectedGUID() == var6) {
               ByteBufferWriter var9 = var8.startPacket();
               PacketTypes.PacketType.TradingUIRemoveItem.doPacket(var9);
               var9.putShort(var3);
               var9.putInt(var5);
               PacketTypes.PacketType.TradingUIRemoveItem.send(var8);
               break;
            }
         }
      }

   }

   static void receiveTradingUIUpdateState(ByteBuffer var0, UdpConnection var1, short var2) {
      short var3 = var0.getShort();
      short var4 = var0.getShort();
      int var5 = var0.getInt();
      Long var6 = (Long)IDToAddressMap.get(var4);
      if (var6 != null) {
         for(int var7 = 0; var7 < udpEngine.connections.size(); ++var7) {
            UdpConnection var8 = (UdpConnection)udpEngine.connections.get(var7);
            if (var8.getConnectedGUID() == var6) {
               ByteBufferWriter var9 = var8.startPacket();
               PacketTypes.PacketType.TradingUIUpdateState.doPacket(var9);
               var9.putShort(var3);
               var9.putInt(var5);
               PacketTypes.PacketType.TradingUIUpdateState.send(var8);
               break;
            }
         }
      }

   }

   static void receiveTradingUIAddItem(ByteBuffer var0, UdpConnection var1, short var2) {
      short var3 = var0.getShort();
      short var4 = var0.getShort();
      InventoryItem var5 = null;

      try {
         var5 = InventoryItem.loadItem(var0, 195);
      } catch (Exception var12) {
         var12.printStackTrace();
      }

      if (var5 != null) {
         Long var6 = (Long)IDToAddressMap.get(var4);
         if (var6 != null) {
            for(int var7 = 0; var7 < udpEngine.connections.size(); ++var7) {
               UdpConnection var8 = (UdpConnection)udpEngine.connections.get(var7);
               if (var8.getConnectedGUID() == var6) {
                  ByteBufferWriter var9 = var8.startPacket();
                  PacketTypes.PacketType.TradingUIAddItem.doPacket(var9);
                  var9.putShort(var3);

                  try {
                     var5.saveWithSize(var9.bb, false);
                  } catch (IOException var11) {
                     var11.printStackTrace();
                  }

                  PacketTypes.PacketType.TradingUIAddItem.send(var8);
                  break;
               }
            }
         }

      }
   }

   static void receiveRequestTrading(ByteBuffer var0, UdpConnection var1, short var2) {
      short var3 = var0.getShort();
      short var4 = var0.getShort();
      byte var5 = var0.get();
      Long var6 = (Long)IDToAddressMap.get(var3);
      if (var5 == 0) {
         var6 = (Long)IDToAddressMap.get(var4);
      }

      if (var6 != null) {
         for(int var7 = 0; var7 < udpEngine.connections.size(); ++var7) {
            UdpConnection var8 = (UdpConnection)udpEngine.connections.get(var7);
            if (var8.getConnectedGUID() == var6) {
               ByteBufferWriter var9 = var8.startPacket();
               PacketTypes.PacketType.RequestTrading.doPacket(var9);
               if (var5 == 0) {
                  var9.putShort(var3);
               } else {
                  var9.putShort(var4);
               }

               var9.putByte(var5);
               PacketTypes.PacketType.RequestTrading.send(var8);
               break;
            }
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
         DebugLog.log("faction: removed " + var3 + " owner=" + var6.getOwner());
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

   static void receiveSyncNonPvpZone(ByteBuffer var0, UdpConnection var1, short var2) {
      try {
         SyncNonPvpZonePacket var3 = new SyncNonPvpZonePacket();
         var3.parse(var0, var1);
         if (var3.isConsistent()) {
            sendNonPvpZone(var3.zone, var3.doRemove, var1);
            var3.process();
            DebugLog.Multiplayer.debugln("ReceiveSyncNonPvpZone: %s", var3.getDescription());
         }
      } catch (Exception var4) {
         DebugLog.Multiplayer.printException(var4, "ReceiveSyncNonPvpZone: failed", LogSeverity.Error);
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

   /** @deprecated */
   @Deprecated
   static void receiveTransactionID(ByteBuffer var0, UdpConnection var1) {
      short var2 = var0.getShort();
      int var3 = var0.getInt();
      IsoPlayer var4 = (IsoPlayer)IDToPlayerMap.get(var2);
      if (var4 != null) {
         transactionIDMap.put(var4.username, var3);
         var4.setTransactionID(var3);
         ServerWorldDatabase.instance.saveTransactionID(var4.username, var3);
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
            var7 = new IsoCompost(var6.getCell(), var6);
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

   static void receiveCataplasm(ByteBuffer var0, UdpConnection var1, short var2) {
      short var3 = var0.getShort();
      IsoPlayer var4 = (IsoPlayer)IDToPlayerMap.get(var3);
      if (var4 != null) {
         int var5 = var0.getInt();
         float var6 = var0.getFloat();
         float var7 = var0.getFloat();
         float var8 = var0.getFloat();
         if (var6 > 0.0F) {
            var4.getBodyDamage().getBodyPart(BodyPartType.FromIndex(var5)).setPlantainFactor(var6);
         }

         if (var7 > 0.0F) {
            var4.getBodyDamage().getBodyPart(BodyPartType.FromIndex(var5)).setComfreyFactor(var7);
         }

         if (var8 > 0.0F) {
            var4.getBodyDamage().getBodyPart(BodyPartType.FromIndex(var5)).setGarlicFactor(var8);
         }

         for(int var9 = 0; var9 < udpEngine.connections.size(); ++var9) {
            UdpConnection var10 = (UdpConnection)udpEngine.connections.get(var9);
            if (var10.getConnectedGUID() != var1.getConnectedGUID()) {
               ByteBufferWriter var11 = var10.startPacket();
               PacketTypes.PacketType.Cataplasm.doPacket(var11);
               var11.putShort(var3);
               var11.putInt(var5);
               var11.putFloat(var6);
               var11.putFloat(var7);
               var11.putFloat(var8);
               PacketTypes.PacketType.Cataplasm.send(var10);
            }
         }
      }

   }

   static void receiveSledgehammerDestroy(ByteBuffer var0, UdpConnection var1, short var2) {
      if (ServerOptions.instance.AllowDestructionBySledgehammer.getValue()) {
         receiveRemoveItemFromSquare(var0, var1, var2);
      }

   }

   public static void AddExplosiveTrap(HandWeapon var0, IsoGridSquare var1, boolean var2) {
      IsoTrap var3 = new IsoTrap(var0, var1.getCell(), var1);
      int var4 = 0;
      if (var0.getExplosionRange() > 0) {
         var4 = var0.getExplosionRange();
      }

      if (var0.getFireRange() > 0) {
         var4 = var0.getFireRange();
      }

      if (var0.getSmokeRange() > 0) {
         var4 = var0.getSmokeRange();
      }

      var1.AddTileObject(var3);

      for(int var5 = 0; var5 < udpEngine.connections.size(); ++var5) {
         UdpConnection var6 = (UdpConnection)udpEngine.connections.get(var5);
         ByteBufferWriter var7 = var6.startPacket();
         PacketTypes.PacketType.AddExplosiveTrap.doPacket(var7);
         var7.putInt(var1.x);
         var7.putInt(var1.y);
         var7.putInt(var1.z);

         try {
            var0.saveWithSize(var7.bb, false);
         } catch (IOException var9) {
            var9.printStackTrace();
         }

         var7.putInt(var4);
         var7.putBoolean(var2);
         var7.putBoolean(false);
         PacketTypes.PacketType.AddExplosiveTrap.send(var6);
      }

   }

   static void receiveAddExplosiveTrap(ByteBuffer var0, UdpConnection var1, short var2) {
      int var3 = var0.getInt();
      int var4 = var0.getInt();
      int var5 = var0.getInt();
      IsoGridSquare var6 = IsoWorld.instance.CurrentCell.getGridSquare(var3, var4, var5);
      if (var6 != null) {
         InventoryItem var7 = null;

         try {
            var7 = InventoryItem.loadItem(var0, 195);
         } catch (Exception var14) {
            var14.printStackTrace();
         }

         if (var7 == null) {
            return;
         }

         HandWeapon var8 = (HandWeapon)var7;
         String var10000 = var1.username;
         DebugLog.log("trap: user \"" + var10000 + "\" added " + var7.getFullType() + " at " + var3 + "," + var4 + "," + var5);
         ZLogger var16 = LoggerManager.getLogger("map");
         String var10001 = var1.idStr;
         var16.write(var10001 + " \"" + var1.username + "\" added " + var7.getFullType() + " at " + var3 + "," + var4 + "," + var5);
         if (var8.isInstantExplosion()) {
            IsoTrap var9 = new IsoTrap(var8, var6.getCell(), var6);
            var6.AddTileObject(var9);
            var9.triggerExplosion(false);
         }

         for(int var15 = 0; var15 < udpEngine.connections.size(); ++var15) {
            UdpConnection var10 = (UdpConnection)udpEngine.connections.get(var15);
            if (var10.getConnectedGUID() != var1.getConnectedGUID()) {
               ByteBufferWriter var11 = var10.startPacket();
               PacketTypes.PacketType.AddExplosiveTrap.doPacket(var11);
               var11.putInt(var3);
               var11.putInt(var4);
               var11.putInt(var5);

               try {
                  var8.saveWithSize(var11.bb, false);
               } catch (IOException var13) {
                  var13.printStackTrace();
               }

               PacketTypes.PacketType.AddExplosiveTrap.send(var10);
            }
         }
      }

   }

   public static void sendHelicopter(float var0, float var1, boolean var2) {
      for(int var3 = 0; var3 < udpEngine.connections.size(); ++var3) {
         UdpConnection var4 = (UdpConnection)udpEngine.connections.get(var3);
         ByteBufferWriter var5 = var4.startPacket();
         PacketTypes.PacketType.Helicopter.doPacket(var5);
         var5.putFloat(var0);
         var5.putFloat(var1);
         var5.putBoolean(var2);
         PacketTypes.PacketType.Helicopter.send(var4);
      }

   }

   static void receiveRegisterZone(ByteBuffer var0, UdpConnection var1, short var2) {
      String var3 = GameWindow.ReadString(var0);
      String var4 = GameWindow.ReadString(var0);
      int var5 = var0.getInt();
      int var6 = var0.getInt();
      int var7 = var0.getInt();
      int var8 = var0.getInt();
      int var9 = var0.getInt();
      int var10 = var0.getInt();
      boolean var11 = var0.get() == 1;
      ArrayList var12 = IsoWorld.instance.getMetaGrid().getZonesAt(var5, var6, var7);
      boolean var13 = false;
      Iterator var14 = var12.iterator();

      while(var14.hasNext()) {
         IsoMetaGrid.Zone var15 = (IsoMetaGrid.Zone)var14.next();
         if (var4.equals(var15.getType())) {
            var13 = true;
            var15.setName(var3);
            var15.setLastActionTimestamp(var10);
         }
      }

      if (!var13) {
         IsoWorld.instance.getMetaGrid().registerZone(var3, var4, var5, var6, var7, var8, var9);
      }

      if (var11) {
         for(int var17 = 0; var17 < udpEngine.connections.size(); ++var17) {
            UdpConnection var18 = (UdpConnection)udpEngine.connections.get(var17);
            if (var18.getConnectedGUID() != var1.getConnectedGUID()) {
               ByteBufferWriter var16 = var18.startPacket();
               PacketTypes.PacketType.RegisterZone.doPacket(var16);
               var16.putUTF(var3);
               var16.putUTF(var4);
               var16.putInt(var5);
               var16.putInt(var6);
               var16.putInt(var7);
               var16.putInt(var8);
               var16.putInt(var9);
               var16.putInt(var10);
               PacketTypes.PacketType.RegisterZone.send(var18);
            }
         }
      }

   }

   public static void sendZone(IsoMetaGrid.Zone var0, UdpConnection var1) {
      for(int var2 = 0; var2 < udpEngine.connections.size(); ++var2) {
         UdpConnection var3 = (UdpConnection)udpEngine.connections.get(var2);
         if (var1 == null || var3.getConnectedGUID() != var1.getConnectedGUID()) {
            ByteBufferWriter var4 = var3.startPacket();
            PacketTypes.PacketType.RegisterZone.doPacket(var4);
            var4.putUTF(var0.name);
            var4.putUTF(var0.type);
            var4.putInt(var0.x);
            var4.putInt(var0.y);
            var4.putInt(var0.z);
            var4.putInt(var0.w);
            var4.putInt(var0.h);
            var4.putInt(var0.lastActionTimestamp);
            PacketTypes.PacketType.RegisterZone.send(var3);
         }
      }

   }

   static void receiveConstructedZone(ByteBuffer var0, UdpConnection var1, short var2) {
      int var3 = var0.getInt();
      int var4 = var0.getInt();
      int var5 = var0.getInt();
      IsoMetaGrid.Zone var6 = IsoWorld.instance.MetaGrid.getZoneAt(var3, var4, var5);
      if (var6 != null) {
         var6.setHaveConstruction(true);
      }

   }

   public static void addXp(IsoPlayer var0, PerkFactory.Perk var1, int var2) {
      if (PlayerToAddressMap.containsKey(var0)) {
         long var3 = (Long)PlayerToAddressMap.get(var0);
         UdpConnection var5 = udpEngine.getActiveConnection(var3);
         if (var5 == null) {
            return;
         }

         AddXp var6 = new AddXp();
         var6.set(var0, var1, var2);
         ByteBufferWriter var7 = var5.startPacket();
         PacketTypes.PacketType.AddXP.doPacket(var7);
         var6.write(var7);
         PacketTypes.PacketType.AddXP.send(var5);
      }

   }

   static void receiveWriteLog(ByteBuffer var0, UdpConnection var1, short var2) {
   }

   static void receiveChecksum(ByteBuffer var0, UdpConnection var1, short var2) {
      NetChecksum.comparer.serverPacket(var0, var1);
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

   static void receiveWorldMessage(ByteBuffer var0, UdpConnection var1, short var2) {
      if (!var1.allChatMuted) {
         String var3 = GameWindow.ReadString(var0);
         String var4 = GameWindow.ReadString(var0);
         if (var4.length() > 256) {
            var4 = var4.substring(0, 256);
         }

         for(int var5 = 0; var5 < udpEngine.connections.size(); ++var5) {
            UdpConnection var6 = (UdpConnection)udpEngine.connections.get(var5);
            ByteBufferWriter var7 = var6.startPacket();
            PacketTypes.PacketType.WorldMessage.doPacket(var7);
            var7.putUTF(var3);
            var7.putUTF(var4);
            PacketTypes.PacketType.WorldMessage.send(var6);
         }

         discordBot.sendMessage(var3, var4);
         LoggerManager.getLogger("chat").write(var1.index + " \"" + var1.username + "\" A \"" + var4 + "\"");
      }
   }

   static void receiveGetModData(ByteBuffer var0, UdpConnection var1, short var2) {
      LuaEventManager.triggerEvent("SendCustomModData");
   }

   static void receiveStopFire(ByteBuffer var0, UdpConnection var1, short var2) {
      byte var3 = var0.get();
      short var11;
      if (var3 == 1) {
         var11 = var0.getShort();
         IsoPlayer var13 = (IsoPlayer)IDToPlayerMap.get(var11);
         if (var13 != null) {
            var13.sendObjectChange("StopBurning");
         }

      } else if (var3 == 2) {
         var11 = var0.getShort();
         IsoZombie var12 = (IsoZombie)ServerMap.instance.ZombieMap.get(var11);
         if (var12 != null) {
            var12.StopBurning();
         }

      } else {
         int var4 = var0.getInt();
         int var5 = var0.getInt();
         int var6 = var0.getInt();
         IsoGridSquare var7 = ServerMap.instance.getGridSquare(var4, var5, var6);
         if (var7 != null) {
            var7.stopFire();

            for(int var8 = 0; var8 < udpEngine.connections.size(); ++var8) {
               UdpConnection var9 = (UdpConnection)udpEngine.connections.get(var8);
               if (var9.RelevantTo((float)var4, (float)var5) && var9.getConnectedGUID() != var1.getConnectedGUID()) {
                  ByteBufferWriter var10 = var9.startPacket();
                  PacketTypes.PacketType.StopFire.doPacket(var10);
                  var10.putInt(var4);
                  var10.putInt(var5);
                  var10.putInt(var6);
                  PacketTypes.PacketType.StopFire.send(var9);
               }
            }

         }
      }
   }

   /** @deprecated */
   @Deprecated
   static void receiveStartFire(ByteBuffer var0, UdpConnection var1, short var2) {
      StartFire var3 = new StartFire();
      var3.parse(var0, var1);
      if (var3.isConsistent() && var3.validate(var1)) {
         var3.process();

         for(int var4 = 0; var4 < udpEngine.connections.size(); ++var4) {
            UdpConnection var5 = (UdpConnection)udpEngine.connections.get(var4);
            if (var5.getConnectedGUID() != var1.getConnectedGUID()) {
               ByteBufferWriter var6 = var5.startPacket();
               PacketTypes.PacketType.StartFire.doPacket(var6);
               var3.write(var6);
               PacketTypes.PacketType.StartFire.send(var5);
            }
         }

      }
   }

   public static void startFireOnClient(IsoGridSquare var0, int var1, boolean var2, int var3, boolean var4) {
      StartFire var5 = new StartFire();
      var5.set(var0, var2, var1, var3, var4);
      var5.process();

      for(int var6 = 0; var6 < udpEngine.connections.size(); ++var6) {
         UdpConnection var7 = (UdpConnection)udpEngine.connections.get(var6);
         if (var7.RelevantTo((float)var0.getX(), (float)var0.getY())) {
            ByteBufferWriter var8 = var7.startPacket();
            PacketTypes.PacketType.StartFire.doPacket(var8);
            var5.write(var8);
            PacketTypes.PacketType.StartFire.send(var7);
         }
      }

   }

   public static void sendOptionsToClients() {
      for(int var0 = 0; var0 < udpEngine.connections.size(); ++var0) {
         UdpConnection var1 = (UdpConnection)udpEngine.connections.get(var0);
         ByteBufferWriter var2 = var1.startPacket();
         PacketTypes.PacketType.ReloadOptions.doPacket(var2);
         var2.putInt(ServerOptions.instance.getPublicOptions().size());
         String var3 = null;
         Iterator var4 = ServerOptions.instance.getPublicOptions().iterator();

         while(var4.hasNext()) {
            var3 = (String)var4.next();
            var2.putUTF(var3);
            var2.putUTF(ServerOptions.instance.getOption(var3));
         }

         PacketTypes.PacketType.ReloadOptions.send(var1);
      }

   }

   public static void sendBecomeCorpse(IsoDeadBody var0) {
      IsoGridSquare var1 = var0.getSquare();
      if (var1 != null) {
         for(int var2 = 0; var2 < udpEngine.connections.size(); ++var2) {
            UdpConnection var3 = (UdpConnection)udpEngine.connections.get(var2);
            if (var3.RelevantTo((float)var1.x, (float)var1.y)) {
               ByteBufferWriter var4 = var3.startPacket();
               PacketTypes.PacketType.BecomeCorpse.doPacket(var4);

               try {
                  var4.putShort(var0.getObjectID());
                  var4.putShort(var0.getOnlineID());
                  var4.putFloat(var0.getReanimateTime());
                  if (var0.isPlayer()) {
                     var4.putByte((byte)2);
                  } else if (var0.isZombie()) {
                     var4.putByte((byte)1);
                  } else {
                     var4.putByte((byte)0);
                  }

                  var4.putInt(var1.x);
                  var4.putInt(var1.y);
                  var4.putInt(var1.z);
                  PacketTypes.PacketType.BecomeCorpse.send(var3);
               } catch (Exception var6) {
                  var3.cancelPacket();
                  DebugLog.Multiplayer.printException(var6, "SendBecomeCorpse failed", LogSeverity.Error);
               }
            }
         }
      }

   }

   public static void sendCorpse(IsoDeadBody var0) {
      IsoGridSquare var1 = var0.getSquare();
      if (var1 != null) {
         for(int var2 = 0; var2 < udpEngine.connections.size(); ++var2) {
            UdpConnection var3 = (UdpConnection)udpEngine.connections.get(var2);
            if (var3.RelevantTo((float)var1.x, (float)var1.y)) {
               ByteBufferWriter var4 = var3.startPacket();
               PacketTypes.PacketType.AddCorpseToMap.doPacket(var4);
               var4.putShort(var0.getObjectID());
               var4.putShort(var0.getOnlineID());
               var4.putInt(var1.x);
               var4.putInt(var1.y);
               var4.putInt(var1.z);
               var0.writeToRemoteBuffer(var4);
               PacketTypes.PacketType.AddCorpseToMap.send(var3);
            }
         }
      }

   }

   static void receiveAddCorpseToMap(ByteBuffer var0, UdpConnection var1, short var2) {
      short var3 = var0.getShort();
      short var4 = var0.getShort();
      int var5 = var0.getInt();
      int var6 = var0.getInt();
      int var7 = var0.getInt();
      IsoObject var8 = WorldItemTypes.createFromBuffer(var0);
      if (var8 != null && var8 instanceof IsoDeadBody) {
         var8.loadFromRemoteBuffer(var0, false);
         ((IsoDeadBody)var8).setObjectID(var3);
         IsoGridSquare var9 = ServerMap.instance.getGridSquare(var5, var6, var7);
         if (var9 != null) {
            var9.addCorpse((IsoDeadBody)var8, true);

            for(int var10 = 0; var10 < udpEngine.connections.size(); ++var10) {
               UdpConnection var11 = (UdpConnection)udpEngine.connections.get(var10);
               if (var11.getConnectedGUID() != var1.getConnectedGUID() && var11.RelevantTo((float)var5, (float)var6)) {
                  ByteBufferWriter var12 = var11.startPacket();
                  PacketTypes.PacketType.AddCorpseToMap.doPacket(var12);
                  var0.rewind();
                  var12.bb.put(var0);
                  PacketTypes.PacketType.AddCorpseToMap.send(var11);
               }
            }
         }

         LoggerManager.getLogger("item").write(var1.idStr + " \"" + var1.username + "\" corpse +1 " + var5 + "," + var6 + "," + var7);
      }
   }

   static void receiveSmashWindow(ByteBuffer var0, UdpConnection var1, short var2) {
      IsoObject var3 = IsoWorld.instance.getItemFromXYZIndexBuffer(var0);
      if (var3 != null && var3 instanceof IsoWindow) {
         byte var4 = var0.get();
         if (var4 == 1) {
            ((IsoWindow)var3).smashWindow(true);
            smashWindow((IsoWindow)var3, 1);
         } else if (var4 == 2) {
            ((IsoWindow)var3).setGlassRemoved(true);
            smashWindow((IsoWindow)var3, 2);
         }
      }

   }

   public static void sendPlayerConnect(IsoPlayer var0, UdpConnection var1) {
      ByteBufferWriter var2 = var1.startPacket();
      PacketTypes.PacketType.PlayerConnect.doPacket(var2);
      if (var1.getConnectedGUID() != (Long)PlayerToAddressMap.get(var0)) {
         var2.putShort(var0.OnlineID);
      } else {
         var2.putShort((short)-1);
         var2.putByte((byte)var0.PlayerIndex);
         var2.putShort(var0.OnlineID);

         try {
            GameTime.getInstance().saveToPacket(var2.bb);
         } catch (IOException var6) {
            var6.printStackTrace();
         }
      }

      var2.putFloat(var0.x);
      var2.putFloat(var0.y);
      var2.putFloat(var0.z);
      var2.putUTF(var0.username);
      if (var1.getConnectedGUID() != (Long)PlayerToAddressMap.get(var0)) {
         try {
            var0.getDescriptor().save(var2.bb);
            var0.getHumanVisual().save(var2.bb);
            ItemVisuals var3 = new ItemVisuals();
            var0.getItemVisuals(var3);
            var3.save(var2.bb);
         } catch (IOException var5) {
            var5.printStackTrace();
         }
      }

      if (SteamUtils.isSteamModeEnabled()) {
         var2.putLong(var0.getSteamID());
      }

      var2.putByte((byte)(var0.isGodMod() ? 1 : 0));
      var2.putByte((byte)(var0.isGhostMode() ? 1 : 0));
      var0.getSafety().save(var2.bb);
      var2.putByte(PlayerType.fromString(var0.accessLevel));
      var2.putByte((byte)(var0.isInvisible() ? 1 : 0));
      if (var1.getConnectedGUID() != (Long)PlayerToAddressMap.get(var0)) {
         try {
            var0.getXp().save(var2.bb);
         } catch (IOException var4) {
            var4.printStackTrace();
         }
      }

      var2.putUTF(var0.getTagPrefix());
      var2.putFloat(var0.getTagColor().r);
      var2.putFloat(var0.getTagColor().g);
      var2.putFloat(var0.getTagColor().b);
      var2.putDouble(var0.getHoursSurvived());
      var2.putInt(var0.getZombieKills());
      var2.putUTF(var0.getDisplayName());
      var2.putFloat(var0.getSpeakColour().r);
      var2.putFloat(var0.getSpeakColour().g);
      var2.putFloat(var0.getSpeakColour().b);
      var2.putBoolean(var0.showTag);
      var2.putBoolean(var0.factionPvp);
      var2.putInt(var0.getAttachedItems().size());

      for(int var7 = 0; var7 < var0.getAttachedItems().size(); ++var7) {
         var2.putUTF(var0.getAttachedItems().get(var7).getLocation());
         var2.putUTF(var0.getAttachedItems().get(var7).getItem().getFullType());
      }

      var2.putInt(var0.remoteSneakLvl);
      var2.putInt(var0.remoteStrLvl);
      var2.putInt(var0.remoteFitLvl);
      PacketTypes.PacketType.PlayerConnect.send(var1);
      if (var1.getConnectedGUID() != (Long)PlayerToAddressMap.get(var0)) {
         updateHandEquips(var1, var0);
      }

   }

   /** @deprecated */
   @Deprecated
   static void receiveRequestPlayerData(ByteBuffer var0, UdpConnection var1, short var2) {
      IsoPlayer var3 = (IsoPlayer)IDToPlayerMap.get(var0.getShort());
      if (var3 != null) {
         sendPlayerConnect(var3, var1);
      }

   }

   static void receiveChatMessageFromPlayer(ByteBuffer var0, UdpConnection var1, short var2) {
      ChatServer.getInstance().processMessageFromPlayerPacket(var0);
   }

   public static void loadModData(IsoGridSquare var0) {
      if (var0.getModData().rawget("id") != null && var0.getModData().rawget("id") != null && (var0.getModData().rawget("remove") == null || ((String)var0.getModData().rawget("remove")).equals("false"))) {
         GameTime.getInstance().getModData().rawset("planting:" + ((Double)var0.getModData().rawget("id")).intValue() + ":x", new Double((double)var0.getX()));
         GameTime.getInstance().getModData().rawset("planting:" + ((Double)var0.getModData().rawget("id")).intValue() + ":y", new Double((double)var0.getY()));
         GameTime.getInstance().getModData().rawset("planting:" + ((Double)var0.getModData().rawget("id")).intValue() + ":z", new Double((double)var0.getZ()));
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

      for(int var1 = 0; var1 < udpEngine.connections.size(); ++var1) {
         UdpConnection var2 = (UdpConnection)udpEngine.connections.get(var1);
         if (var2.RelevantTo((float)var0.getX(), (float)var0.getY())) {
            ByteBufferWriter var3 = var2.startPacket();
            PacketTypes.PacketType.ReceiveModData.doPacket(var3);
            var3.putInt(var0.getX());
            var3.putInt(var0.getY());
            var3.putInt(var0.getZ());

            try {
               var0.getModData().save(var3.bb);
            } catch (IOException var5) {
               var5.printStackTrace();
            }

            PacketTypes.PacketType.ReceiveModData.send(var2);
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
            var6.getModData().load(var0, 195);
            if (var6.getModData().rawget("id") != null && (var6.getModData().rawget("remove") == null || ((String)var6.getModData().rawget("remove")).equals("false"))) {
               GameTime.getInstance().getModData().rawset("planting:" + ((Double)var6.getModData().rawget("id")).intValue() + ":x", new Double((double)var6.getX()));
               GameTime.getInstance().getModData().rawset("planting:" + ((Double)var6.getModData().rawget("id")).intValue() + ":y", new Double((double)var6.getY()));
               GameTime.getInstance().getModData().rawset("planting:" + ((Double)var6.getModData().rawget("id")).intValue() + ":z", new Double((double)var6.getZ()));
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

   private static void process(ZomboidNetData var0) {
      ByteBuffer var1 = var0.buffer;
      UdpConnection var2 = udpEngine.getActiveConnection(var0.connection);

      try {
         switch (var0.type) {
            default:
               doZomboidDataInMainLoop(var0);
         }
      } catch (Exception var4) {
         DebugLog.log(DebugType.Network, "Error with packet of type: " + var0.type);
         var4.printStackTrace();
      }
   }

   static void receiveEatFood(ByteBuffer var0, UdpConnection var1, short var2) {
      byte var3 = var0.get();
      float var4 = var0.getFloat();
      InventoryItem var5 = null;

      try {
         var5 = InventoryItem.loadItem(var0, 195);
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
      ByteBufferWriter var3 = var1.startPacket();
      long var4 = var0.getLong();
      if (var4 == -1L) {
         DebugLog.Multiplayer.warn("Player \"%s\" toggled lua debugger", var1.username);
      } else {
         if (var1.accessLevel != 32) {
            return;
         }

         PacketTypes.PacketType.PingFromClient.doPacket(var3);

         try {
            var3.putLong(var4);
            MPStatistics.write(var1, var3.bb);
            PacketTypes.PacketType.PingFromClient.send(var1);
            MPStatistics.requested();
         } catch (Exception var7) {
            var1.cancelPacket();
         }
      }

   }

   static void receiveBandage(ByteBuffer var0, UdpConnection var1, short var2) {
      short var3 = var0.getShort();
      IsoPlayer var4 = (IsoPlayer)IDToPlayerMap.get(var3);
      if (var4 != null) {
         int var5 = var0.getInt();
         boolean var6 = var0.get() == 1;
         float var7 = var0.getFloat();
         boolean var8 = var0.get() == 1;
         String var9 = GameWindow.ReadStringUTF(var0);
         var4.getBodyDamage().SetBandaged(var5, var6, var7, var8, var9);

         for(int var10 = 0; var10 < udpEngine.connections.size(); ++var10) {
            UdpConnection var11 = (UdpConnection)udpEngine.connections.get(var10);
            if (var11.getConnectedGUID() != var1.getConnectedGUID()) {
               ByteBufferWriter var12 = var11.startPacket();
               PacketTypes.PacketType.Bandage.doPacket(var12);
               var12.putShort(var3);
               var12.putInt(var5);
               var12.putBoolean(var6);
               var12.putFloat(var7);
               var12.putBoolean(var8);
               GameWindow.WriteStringUTF(var12.bb, var9);
               PacketTypes.PacketType.Bandage.send(var11);
            }
         }
      }

   }

   static void receiveStitch(ByteBuffer var0, UdpConnection var1, short var2) {
      Stitch var3 = new Stitch();
      var3.parse(var0, var1);
      if (var3.isConsistent() && var3.validate(var1)) {
         for(int var4 = 0; var4 < udpEngine.connections.size(); ++var4) {
            UdpConnection var5 = (UdpConnection)udpEngine.connections.get(var4);
            if (var5.getConnectedGUID() != var1.getConnectedGUID()) {
               ByteBufferWriter var6 = var5.startPacket();
               PacketTypes.PacketType.Stitch.doPacket(var6);
               var3.write(var6);
               PacketTypes.PacketType.Stitch.send(var5);
            }
         }

      }
   }

   /** @deprecated */
   @Deprecated
   static void receiveWoundInfection(ByteBuffer var0, UdpConnection var1, short var2) {
      short var3 = var0.getShort();
      IsoPlayer var4 = (IsoPlayer)IDToPlayerMap.get(var3);
      if (var4 != null) {
         int var5 = var0.getInt();
         boolean var6 = var0.get() == 1;
         var4.getBodyDamage().getBodyPart(BodyPartType.FromIndex(var5)).setInfectedWound(var6);

         for(int var7 = 0; var7 < udpEngine.connections.size(); ++var7) {
            UdpConnection var8 = (UdpConnection)udpEngine.connections.get(var7);
            if (var8.getConnectedGUID() != var1.getConnectedGUID()) {
               ByteBufferWriter var9 = var8.startPacket();
               PacketTypes.PacketType.WoundInfection.doPacket(var9);
               var9.putShort(var3);
               var9.putInt(var5);
               var9.putBoolean(var6);
               PacketTypes.PacketType.WoundInfection.send(var8);
            }
         }
      }

   }

   static void receiveDisinfect(ByteBuffer var0, UdpConnection var1, short var2) {
      Disinfect var3 = new Disinfect();
      var3.parse(var0, var1);
      if (var3.isConsistent() && var3.validate(var1)) {
         for(int var4 = 0; var4 < udpEngine.connections.size(); ++var4) {
            UdpConnection var5 = (UdpConnection)udpEngine.connections.get(var4);
            if (var5.getConnectedGUID() != var1.getConnectedGUID()) {
               ByteBufferWriter var6 = var5.startPacket();
               PacketTypes.PacketType.Disinfect.doPacket(var6);
               var3.write(var6);
               PacketTypes.PacketType.Disinfect.send(var5);
            }
         }

      }
   }

   static void receiveSplint(ByteBuffer var0, UdpConnection var1, short var2) {
      short var3 = var0.getShort();
      IsoPlayer var4 = (IsoPlayer)IDToPlayerMap.get(var3);
      if (var4 != null) {
         int var5 = var0.getInt();
         boolean var6 = var0.get() == 1;
         String var7 = var6 ? GameWindow.ReadStringUTF(var0) : null;
         float var8 = var6 ? var0.getFloat() : 0.0F;
         BodyPart var9 = var4.getBodyDamage().getBodyPart(BodyPartType.FromIndex(var5));
         var9.setSplint(var6, var8);
         var9.setSplintItem(var7);

         for(int var10 = 0; var10 < udpEngine.connections.size(); ++var10) {
            UdpConnection var11 = (UdpConnection)udpEngine.connections.get(var10);
            if (var11.getConnectedGUID() != var1.getConnectedGUID()) {
               ByteBufferWriter var12 = var11.startPacket();
               PacketTypes.PacketType.Splint.doPacket(var12);
               var12.putShort(var3);
               var12.putInt(var5);
               var12.putBoolean(var6);
               if (var6) {
                  var12.putUTF(var7);
                  var12.putFloat(var8);
               }

               PacketTypes.PacketType.Splint.send(var11);
            }
         }
      }

   }

   static void receiveAdditionalPain(ByteBuffer var0, UdpConnection var1, short var2) {
      short var3 = var0.getShort();
      IsoPlayer var4 = (IsoPlayer)IDToPlayerMap.get(var3);
      if (var4 != null) {
         int var5 = var0.getInt();
         float var6 = var0.getFloat();
         BodyPart var7 = var4.getBodyDamage().getBodyPart(BodyPartType.FromIndex(var5));
         var7.setAdditionalPain(var7.getAdditionalPain() + var6);

         for(int var8 = 0; var8 < udpEngine.connections.size(); ++var8) {
            UdpConnection var9 = (UdpConnection)udpEngine.connections.get(var8);
            if (var9.getConnectedGUID() != var1.getConnectedGUID()) {
               ByteBufferWriter var10 = var9.startPacket();
               PacketTypes.PacketType.AdditionalPain.doPacket(var10);
               var10.putShort(var3);
               var10.putInt(var5);
               var10.putFloat(var6);
               PacketTypes.PacketType.AdditionalPain.send(var9);
            }
         }
      }

   }

   static void receiveRemoveGlass(ByteBuffer var0, UdpConnection var1, short var2) {
      RemoveGlass var3 = new RemoveGlass();
      var3.parse(var0, var1);
      if (var3.isConsistent() && var3.validate(var1)) {
         var3.process();

         for(int var4 = 0; var4 < udpEngine.connections.size(); ++var4) {
            UdpConnection var5 = (UdpConnection)udpEngine.connections.get(var4);
            if (var5.getConnectedGUID() != var1.getConnectedGUID()) {
               ByteBufferWriter var6 = var5.startPacket();
               PacketTypes.PacketType.RemoveGlass.doPacket(var6);
               var3.write(var6);
               PacketTypes.PacketType.RemoveGlass.send(var5);
            }
         }

      }
   }

   static void receiveRemoveBullet(ByteBuffer var0, UdpConnection var1, short var2) {
      RemoveBullet var3 = new RemoveBullet();
      var3.parse(var0, var1);
      if (var3.isConsistent() && var3.validate(var1)) {
         var3.process();

         for(int var4 = 0; var4 < udpEngine.connections.size(); ++var4) {
            UdpConnection var5 = (UdpConnection)udpEngine.connections.get(var4);
            if (var5.getConnectedGUID() != var1.getConnectedGUID()) {
               ByteBufferWriter var6 = var5.startPacket();
               PacketTypes.PacketType.RemoveBullet.doPacket(var6);
               var3.write(var6);
               PacketTypes.PacketType.RemoveBullet.send(var5);
            }
         }

      }
   }

   static void receiveCleanBurn(ByteBuffer var0, UdpConnection var1, short var2) {
      CleanBurn var3 = new CleanBurn();
      var3.parse(var0, var1);
      if (var3.isConsistent() && var3.validate(var1)) {
         var3.process();

         for(int var4 = 0; var4 < udpEngine.connections.size(); ++var4) {
            UdpConnection var5 = (UdpConnection)udpEngine.connections.get(var4);
            if (var5.getConnectedGUID() != var1.getConnectedGUID()) {
               ByteBufferWriter var6 = var5.startPacket();
               PacketTypes.PacketType.CleanBurn.doPacket(var6);
               var3.write(var6);
               PacketTypes.PacketType.CleanBurn.send(var5);
            }
         }

      }
   }

   static void receiveBodyDamageUpdate(ByteBuffer var0, UdpConnection var1, short var2) {
      BodyDamageSync.instance.serverPacket(var0);
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
                     return "You don't own a safehouse.";
                  } else if (!ServerOptions.instance.PlayerSafehouse.getValue() && !"admin".equals(var1.accessLevel) && !"moderator".equals(var1.accessLevel)) {
                     return "Only admin or moderator may release safehouses";
                  } else {
                     var7.removeSafeHouse((IsoPlayer)null);
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

   public static void doZomboidDataInMainLoop(ZomboidNetData var0) {
      synchronized(MainLoopNetDataHighPriorityQ) {
         MainLoopNetDataHighPriorityQ.add(var0);
      }
   }

   static void receiveEquip(ByteBuffer var0, UdpConnection var1, short var2) {
      byte var3 = var0.get();
      byte var4 = var0.get();
      byte var5 = var0.get();
      InventoryItem var6 = null;
      IsoPlayer var7 = getPlayerFromConnection(var1, var3);
      if (var5 == 1) {
         try {
            var6 = InventoryItem.loadItem(var0, 195);
         } catch (Exception var15) {
            var15.printStackTrace();
         }

         if (var6 == null) {
            LoggerManager.getLogger("user").write(var1.idStr + " equipped unknown item type");
            return;
         }
      }

      if (var7 != null) {
         if (var6 != null) {
            var6.setContainer(var7.getInventory());
         }

         if (var4 == 0) {
            var7.setPrimaryHandItem(var6);
         } else {
            if (var5 == 2) {
               var6 = var7.getPrimaryHandItem();
            }

            var7.setSecondaryHandItem(var6);
         }

         try {
            if (var5 == 1 && var6 != null && var0.get() == 1) {
               var6.getVisual().load(var0, 195);
            }
         } catch (IOException var14) {
            var14.printStackTrace();
         }
      }

      if (var7 != null) {
         for(int var8 = 0; var8 < udpEngine.connections.size(); ++var8) {
            UdpConnection var9 = (UdpConnection)udpEngine.connections.get(var8);
            if (var9.getConnectedGUID() != var1.getConnectedGUID()) {
               IsoPlayer var10 = getAnyPlayerFromConnection(var9);
               if (var10 != null) {
                  ByteBufferWriter var11 = var9.startPacket();
                  PacketTypes.PacketType.Equip.doPacket(var11);
                  var11.putShort(var7.OnlineID);
                  var11.putByte(var4);
                  var11.putByte(var5);
                  if (var5 == 1) {
                     try {
                        var6.saveWithSize(var11.bb, false);
                        if (var6.getVisual() != null) {
                           var11.bb.put((byte)1);
                           var6.getVisual().save(var11.bb);
                        } else {
                           var11.bb.put((byte)0);
                        }
                     } catch (IOException var13) {
                        var13.printStackTrace();
                     }
                  }

                  PacketTypes.PacketType.Equip.send(var9);
               }
            }
         }

      }
   }

   static void receivePlayerConnect(ByteBuffer var0, UdpConnection var1, short var2) {
      receivePlayerConnect(var0, var1, var1.username);
      sendInitialWorldState(var1);
   }

   static void receiveScoreboardUpdate(ByteBuffer var0, UdpConnection var1, short var2) {
      ByteBufferWriter var3 = var1.startPacket();
      PacketTypes.PacketType.ScoreboardUpdate.doPacket(var3);
      ArrayList var4 = new ArrayList();
      ArrayList var5 = new ArrayList();
      ArrayList var6 = new ArrayList();

      int var7;
      for(var7 = 0; var7 < udpEngine.connections.size(); ++var7) {
         UdpConnection var8 = (UdpConnection)udpEngine.connections.get(var7);
         if (var8.isFullyConnected()) {
            for(int var9 = 0; var9 < 4; ++var9) {
               if (var8.usernames[var9] != null) {
                  var4.add(var8.usernames[var9]);
                  IsoPlayer var10 = getPlayerByRealUserName(var8.usernames[var9]);
                  if (var10 != null) {
                     var5.add(var10.getDisplayName());
                  } else {
                     String var11 = ServerWorldDatabase.instance.getDisplayName(var8.usernames[var9]);
                     var5.add(var11 == null ? var8.usernames[var9] : var11);
                  }

                  if (SteamUtils.isSteamModeEnabled()) {
                     var6.add(var8.steamID);
                  }
               }
            }
         }
      }

      var3.putInt(var4.size());

      for(var7 = 0; var7 < var4.size(); ++var7) {
         var3.putUTF((String)var4.get(var7));
         var3.putUTF((String)var5.get(var7));
         if (SteamUtils.isSteamModeEnabled()) {
            var3.putLong((Long)var6.get(var7));
         }
      }

      PacketTypes.PacketType.ScoreboardUpdate.send(var1);
   }

   static void receiveStopSound(ByteBuffer var0, UdpConnection var1, short var2) {
      StopSoundPacket var3 = new StopSoundPacket();
      var3.parse(var0, var1);

      for(int var4 = 0; var4 < udpEngine.connections.size(); ++var4) {
         UdpConnection var5 = (UdpConnection)udpEngine.connections.get(var4);
         if (var5.getConnectedGUID() != var1.getConnectedGUID() && var5.isFullyConnected()) {
            ByteBufferWriter var6 = var5.startPacket();
            PacketTypes.PacketType.StopSound.doPacket(var6);
            var3.write(var6);
            PacketTypes.PacketType.StopSound.send(var5);
         }
      }

   }

   static void receivePlaySound(ByteBuffer var0, UdpConnection var1, short var2) {
      PlaySoundPacket var3 = new PlaySoundPacket();
      var3.parse(var0, var1);
      IsoMovingObject var4 = var3.getMovingObject();
      if (var3.isConsistent()) {
         int var5 = 70;
         GameSound var6 = GameSounds.getSound(var3.getName());
         int var7;
         if (var6 != null) {
            for(var7 = 0; var7 < var6.clips.size(); ++var7) {
               GameSoundClip var8 = (GameSoundClip)var6.clips.get(var7);
               if (var8.hasMaxDistance()) {
                  var5 = Math.max(var5, (int)var8.distanceMax);
               }
            }
         }

         for(var7 = 0; var7 < udpEngine.connections.size(); ++var7) {
            UdpConnection var11 = (UdpConnection)udpEngine.connections.get(var7);
            if (var11.getConnectedGUID() != var1.getConnectedGUID() && var11.isFullyConnected()) {
               IsoPlayer var9 = getAnyPlayerFromConnection(var11);
               if (var9 != null && (var4 == null || var11.RelevantTo(var4.getX(), var4.getY(), (float)var5))) {
                  ByteBufferWriter var10 = var11.startPacket();
                  PacketTypes.PacketType.PlaySound.doPacket(var10);
                  var3.write(var10);
                  PacketTypes.PacketType.PlaySound.send(var11);
               }
            }
         }

      }
   }

   static void receivePlayWorldSound(ByteBuffer var0, UdpConnection var1, short var2) {
      PlayWorldSoundPacket var3 = new PlayWorldSoundPacket();
      var3.parse(var0, var1);
      if (var3.isConsistent()) {
         int var4 = 70;
         GameSound var5 = GameSounds.getSound(var3.getName());
         int var6;
         if (var5 != null) {
            for(var6 = 0; var6 < var5.clips.size(); ++var6) {
               GameSoundClip var7 = (GameSoundClip)var5.clips.get(var6);
               if (var7.hasMaxDistance()) {
                  var4 = Math.max(var4, (int)var7.distanceMax);
               }
            }
         }

         for(var6 = 0; var6 < udpEngine.connections.size(); ++var6) {
            UdpConnection var10 = (UdpConnection)udpEngine.connections.get(var6);
            if (var10.getConnectedGUID() != var1.getConnectedGUID() && var10.isFullyConnected()) {
               IsoPlayer var8 = getAnyPlayerFromConnection(var10);
               if (var8 != null && var10.RelevantTo((float)var3.getX(), (float)var3.getY(), (float)var4)) {
                  ByteBufferWriter var9 = var10.startPacket();
                  PacketTypes.PacketType.PlayWorldSound.doPacket(var9);
                  var3.write(var9);
                  PacketTypes.PacketType.PlayWorldSound.send(var10);
               }
            }
         }

      }
   }

   private static void PlayWorldSound(String var0, IsoGridSquare var1, float var2) {
      if (bServer && var1 != null) {
         int var3 = var1.getX();
         int var4 = var1.getY();
         int var5 = var1.getZ();
         PlayWorldSoundPacket var6 = new PlayWorldSoundPacket();
         var6.set(var0, var3, var4, (byte)var5);
         DebugType var10000 = DebugType.Sound;
         String var10001 = var6.getDescription();
         DebugLog.log(var10000, "sending " + var10001 + " radius=" + var2);

         for(int var7 = 0; var7 < udpEngine.connections.size(); ++var7) {
            UdpConnection var8 = (UdpConnection)udpEngine.connections.get(var7);
            IsoPlayer var9 = getAnyPlayerFromConnection(var8);
            if (var9 != null && var8.RelevantTo((float)var3, (float)var4, var2 * 2.0F)) {
               ByteBufferWriter var10 = var8.startPacket();
               PacketTypes.PacketType.PlayWorldSound.doPacket(var10);
               var6.write(var10);
               PacketTypes.PacketType.PlayWorldSound.send(var8);
            }
         }

      }
   }

   public static void PlayWorldSoundServer(String var0, boolean var1, IsoGridSquare var2, float var3, float var4, float var5, boolean var6) {
      PlayWorldSound(var0, var2, var4);
   }

   public static void PlayWorldSoundServer(IsoGameCharacter var0, String var1, boolean var2, IsoGridSquare var3, float var4, float var5, float var6, boolean var7) {
      if (var0 == null || !var0.isInvisible() || DebugOptions.instance.Character.Debug.PlaySoundWhenInvisible.getValue()) {
         PlayWorldSound(var1, var3, var5);
      }
   }

   public static void PlayWorldSoundWavServer(String var0, boolean var1, IsoGridSquare var2, float var3, float var4, float var5, boolean var6) {
      PlayWorldSound(var0, var2, var4);
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
                  var1 = (int)var7.getX();
                  var2 = (int)var7.getY();
                  var3 = (int)var7.getZ();
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

   static void receiveZombieHelmetFalling(ByteBuffer var0, UdpConnection var1, short var2) {
      byte var3 = var0.get();
      short var4 = var0.getShort();
      String var5 = GameWindow.ReadString(var0);
      IsoZombie var6 = (IsoZombie)ServerMap.instance.ZombieMap.get(var4);
      IsoPlayer var7 = getPlayerFromConnection(var1, var3);
      if (var7 != null && var6 != null) {
         var6.serverRemoveItemFromZombie(var5);

         for(int var8 = 0; var8 < udpEngine.connections.size(); ++var8) {
            UdpConnection var9 = (UdpConnection)udpEngine.connections.get(var8);
            if (var9.getConnectedGUID() != var1.getConnectedGUID()) {
               IsoPlayer var10 = getAnyPlayerFromConnection(var1);
               if (var10 != null) {
                  try {
                     ByteBufferWriter var11 = var9.startPacket();
                     PacketTypes.PacketType.ZombieHelmetFalling.doPacket(var11);
                     var11.putShort(var4);
                     var11.putUTF(var5);
                     PacketTypes.PacketType.ZombieHelmetFalling.send(var9);
                  } catch (Throwable var12) {
                     var1.cancelPacket();
                     ExceptionLogger.logException(var12);
                  }
               }
            }
         }

      }
   }

   static void receivePlayerAttachedItem(ByteBuffer var0, UdpConnection var1, short var2) {
      byte var3 = var0.get();
      String var4 = GameWindow.ReadString(var0);
      boolean var5 = var0.get() == 1;
      InventoryItem var6 = null;
      if (var5) {
         String var7 = GameWindow.ReadString(var0);
         var6 = InventoryItemFactory.CreateItem(var7);
         if (var6 == null) {
            return;
         }
      }

      IsoPlayer var13 = getPlayerFromConnection(var1, var3);
      if (var13 != null) {
         var13.setAttachedItem(var4, var6);

         for(int var8 = 0; var8 < udpEngine.connections.size(); ++var8) {
            UdpConnection var9 = (UdpConnection)udpEngine.connections.get(var8);
            if (var9.getConnectedGUID() != var1.getConnectedGUID()) {
               IsoPlayer var10 = getAnyPlayerFromConnection(var1);
               if (var10 != null) {
                  try {
                     ByteBufferWriter var11 = var9.startPacket();
                     PacketTypes.PacketType.PlayerAttachedItem.doPacket(var11);
                     var11.putShort(var13.OnlineID);
                     GameWindow.WriteString(var11.bb, var4);
                     var11.putByte((byte)(var5 ? 1 : 0));
                     if (var5) {
                        GameWindow.WriteString(var11.bb, var6.getFullType());
                     }

                     PacketTypes.PacketType.PlayerAttachedItem.send(var9);
                  } catch (Throwable var12) {
                     var1.cancelPacket();
                     ExceptionLogger.logException(var12);
                  }
               }
            }
         }

      }
   }

   static void receiveSyncClothing(ByteBuffer var0, UdpConnection var1, short var2) {
      SyncClothingPacket var3 = new SyncClothingPacket();
      var3.parse(var0, var1);
      if (var3.isConsistent()) {
         for(int var4 = 0; var4 < udpEngine.connections.size(); ++var4) {
            UdpConnection var5 = (UdpConnection)udpEngine.connections.get(var4);
            if (var5.getConnectedGUID() != var1.getConnectedGUID()) {
               IsoPlayer var6 = getAnyPlayerFromConnection(var1);
               if (var6 != null) {
                  ByteBufferWriter var7 = var5.startPacket();
                  PacketTypes.PacketType.SyncClothing.doPacket(var7);
                  var3.write(var7);
                  PacketTypes.PacketType.SyncClothing.send(var5);
               }
            }
         }

      }
   }

   static void receiveHumanVisual(ByteBuffer var0, UdpConnection var1, short var2) {
      short var3 = var0.getShort();
      IsoPlayer var4 = (IsoPlayer)IDToPlayerMap.get(var3);
      if (var4 != null) {
         if (!var1.havePlayer(var4)) {
            DebugLog.Network.warn("User " + var1.username + " sent HumanVisual packet for non owned player #" + var4.OnlineID);
         } else {
            try {
               var4.getHumanVisual().load(var0, 195);
            } catch (Throwable var11) {
               ExceptionLogger.logException(var11);
               return;
            }

            for(int var5 = 0; var5 < udpEngine.connections.size(); ++var5) {
               UdpConnection var6 = (UdpConnection)udpEngine.connections.get(var5);
               if (var6.getConnectedGUID() != var1.getConnectedGUID()) {
                  IsoPlayer var7 = getAnyPlayerFromConnection(var6);
                  if (var7 != null) {
                     ByteBufferWriter var8 = var6.startPacket();
                     PacketTypes.PacketType.HumanVisual.doPacket(var8);

                     try {
                        var8.putShort(var4.OnlineID);
                        var4.getHumanVisual().save(var8.bb);
                        PacketTypes.PacketType.HumanVisual.send(var6);
                     } catch (Throwable var10) {
                        var6.cancelPacket();
                        ExceptionLogger.logException(var10);
                     }
                  }
               }
            }

         }
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

         if (!"vehicle".equals(var4) || !"remove".equals(var5) || Core.bDebug || PlayerType.isPrivileged(var1.accessLevel) || var8.networkAI.isDismantleAllowed()) {
            LuaEventManager.triggerEvent("OnClientCommand", var4, var5, var8, var7);
         }
      }
   }

   static void receiveGlobalObjects(ByteBuffer var0, UdpConnection var1, short var2) {
      byte var3 = var0.get();
      IsoPlayer var4 = getPlayerFromConnection(var1, var3);
      if (var3 == -1) {
         var4 = getAnyPlayerFromConnection(var1);
      }

      if (var4 == null) {
         DebugLog.log("receiveGlobalObjects: player is null");
      } else {
         SGlobalObjectNetwork.receive(var0, var4);
      }
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
            if (var4 != null && (var4.getDisplayName().toLowerCase().equals(var0.toLowerCase()) || var4.getDisplayName().toLowerCase().startsWith(var0.toLowerCase()))) {
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

   static void receiveRemoveBlood(ByteBuffer var0, UdpConnection var1, short var2) {
      int var3 = var0.getInt();
      int var4 = var0.getInt();
      int var5 = var0.getInt();
      boolean var6 = var0.get() == 1;
      IsoGridSquare var7 = IsoWorld.instance.CurrentCell.getGridSquare(var3, var4, var5);
      if (var7 != null) {
         var7.removeBlood(false, var6);

         for(int var8 = 0; var8 < udpEngine.connections.size(); ++var8) {
            UdpConnection var9 = (UdpConnection)udpEngine.connections.get(var8);
            if (var9 != var1 && var9.RelevantTo((float)var3, (float)var4)) {
               ByteBufferWriter var10 = var9.startPacket();
               PacketTypes.PacketType.RemoveBlood.doPacket(var10);
               var10.putInt(var3);
               var10.putInt(var4);
               var10.putInt(var5);
               var10.putBoolean(var6);
               PacketTypes.PacketType.RemoveBlood.send(var9);
            }
         }

      }
   }

   public static void sendAddItemToContainer(ItemContainer var0, InventoryItem var1) {
      Object var2 = var0.getParent();
      if (var0.getContainingItem() != null && var0.getContainingItem().getWorldItem() != null) {
         var2 = var0.getContainingItem().getWorldItem();
      }

      if (var2 == null) {
         DebugLog.General.error("container has no parent object");
      } else {
         IsoGridSquare var3 = ((IsoObject)var2).getSquare();
         if (var3 == null) {
            DebugLog.General.error("container parent object has no square");
         } else {
            for(int var4 = 0; var4 < udpEngine.connections.size(); ++var4) {
               UdpConnection var5 = (UdpConnection)udpEngine.connections.get(var4);
               if (var5.RelevantTo((float)var3.x, (float)var3.y)) {
                  ByteBufferWriter var6 = var5.startPacket();
                  PacketTypes.PacketType.AddInventoryItemToContainer.doPacket(var6);
                  if (var2 instanceof IsoDeadBody) {
                     var6.putShort((short)0);
                     var6.putInt(((IsoObject)var2).square.getX());
                     var6.putInt(((IsoObject)var2).square.getY());
                     var6.putInt(((IsoObject)var2).square.getZ());
                     var6.putByte((byte)((IsoObject)var2).getStaticMovingObjectIndex());
                  } else if (var2 instanceof IsoWorldInventoryObject) {
                     var6.putShort((short)1);
                     var6.putInt(((IsoObject)var2).square.getX());
                     var6.putInt(((IsoObject)var2).square.getY());
                     var6.putInt(((IsoObject)var2).square.getZ());
                     var6.putInt(((IsoWorldInventoryObject)var2).getItem().id);
                  } else if (var2 instanceof BaseVehicle) {
                     var6.putShort((short)3);
                     var6.putInt(((IsoObject)var2).square.getX());
                     var6.putInt(((IsoObject)var2).square.getY());
                     var6.putInt(((IsoObject)var2).square.getZ());
                     var6.putShort(((BaseVehicle)var2).VehicleID);
                     var6.putByte((byte)var0.vehiclePart.getIndex());
                  } else {
                     var6.putShort((short)2);
                     var6.putInt(((IsoObject)var2).square.getX());
                     var6.putInt(((IsoObject)var2).square.getY());
                     var6.putInt(((IsoObject)var2).square.getZ());
                     var6.putByte((byte)((IsoObject)var2).square.getObjects().indexOf(var2));
                     var6.putByte((byte)((IsoObject)var2).getContainerIndex(var0));
                  }

                  try {
                     CompressIdenticalItems.save(var6.bb, var1);
                  } catch (Exception var8) {
                     var8.printStackTrace();
                  }

                  PacketTypes.PacketType.AddInventoryItemToContainer.send(var5);
               }
            }

         }
      }
   }

   public static void sendRemoveItemFromContainer(ItemContainer var0, InventoryItem var1) {
      Object var2 = var0.getParent();
      if (var0.getContainingItem() != null && var0.getContainingItem().getWorldItem() != null) {
         var2 = var0.getContainingItem().getWorldItem();
      }

      if (var2 == null) {
         DebugLog.log("sendRemoveItemFromContainer: o is null");
      } else {
         IsoGridSquare var3 = ((IsoObject)var2).getSquare();
         if (var3 == null) {
            DebugLog.log("sendRemoveItemFromContainer: square is null");
         } else {
            for(int var4 = 0; var4 < udpEngine.connections.size(); ++var4) {
               UdpConnection var5 = (UdpConnection)udpEngine.connections.get(var4);
               if (var5.RelevantTo((float)var3.x, (float)var3.y)) {
                  ByteBufferWriter var6 = var5.startPacket();
                  PacketTypes.PacketType.RemoveInventoryItemFromContainer.doPacket(var6);
                  if (var2 instanceof IsoDeadBody) {
                     var6.putShort((short)0);
                     var6.putInt(((IsoObject)var2).square.getX());
                     var6.putInt(((IsoObject)var2).square.getY());
                     var6.putInt(((IsoObject)var2).square.getZ());
                     var6.putByte((byte)((IsoObject)var2).getStaticMovingObjectIndex());
                     var6.putInt(1);
                     var6.putInt(var1.id);
                  } else if (var2 instanceof IsoWorldInventoryObject) {
                     var6.putShort((short)1);
                     var6.putInt(((IsoObject)var2).square.getX());
                     var6.putInt(((IsoObject)var2).square.getY());
                     var6.putInt(((IsoObject)var2).square.getZ());
                     var6.putInt(((IsoWorldInventoryObject)var2).getItem().id);
                     var6.putInt(1);
                     var6.putInt(var1.id);
                  } else if (var2 instanceof BaseVehicle) {
                     var6.putShort((short)3);
                     var6.putInt(((IsoObject)var2).square.getX());
                     var6.putInt(((IsoObject)var2).square.getY());
                     var6.putInt(((IsoObject)var2).square.getZ());
                     var6.putShort(((BaseVehicle)var2).VehicleID);
                     var6.putByte((byte)var0.vehiclePart.getIndex());
                     var6.putInt(1);
                     var6.putInt(var1.id);
                  } else {
                     var6.putShort((short)2);
                     var6.putInt(((IsoObject)var2).square.getX());
                     var6.putInt(((IsoObject)var2).square.getY());
                     var6.putInt(((IsoObject)var2).square.getZ());
                     var6.putByte((byte)((IsoObject)var2).square.getObjects().indexOf(var2));
                     var6.putByte((byte)((IsoObject)var2).getContainerIndex(var0));
                     var6.putInt(1);
                     var6.putInt(var1.id);
                  }

                  PacketTypes.PacketType.RemoveInventoryItemFromContainer.send(var5);
               }
            }

         }
      }
   }

   static void receiveRemoveInventoryItemFromContainer(ByteBuffer var0, UdpConnection var1, short var2) {
      alreadyRemoved.clear();
      ByteBufferReader var3 = new ByteBufferReader(var0);
      short var4 = var3.getShort();
      int var5 = var3.getInt();
      int var6 = var3.getInt();
      int var7 = var3.getInt();
      IsoGridSquare var8 = IsoWorld.instance.CurrentCell.getGridSquare(var5, var6, var7);
      if (var8 == null) {
         var8 = ServerMap.instance.getGridSquare(var5, var6, var7);
      }

      HashSet var9 = new HashSet();
      boolean var10 = false;
      int var11 = 0;
      int var12;
      int var14;
      int var15;
      InventoryItem var16;
      int var23;
      if (var4 == 0) {
         var12 = var3.getByte();
         var11 = var0.getInt();
         if (var8 != null && var12 >= 0 && var12 < var8.getStaticMovingObjects().size()) {
            IsoObject var13 = (IsoObject)var8.getStaticMovingObjects().get(var12);
            if (var13 != null && var13.getContainer() != null) {
               for(var14 = 0; var14 < var11; ++var14) {
                  var15 = var3.getInt();
                  var16 = var13.getContainer().getItemWithID(var15);
                  if (var16 == null) {
                     alreadyRemoved.add(var15);
                  } else {
                     var13.getContainer().Remove(var16);
                     var10 = true;
                     var9.add(var16.getFullType());
                  }
               }

               var13.getContainer().setExplored(true);
               var13.getContainer().setHasBeenLooted(true);
            }
         }
      } else if (var4 == 1) {
         if (var8 != null) {
            var12 = var3.getInt();
            var11 = var0.getInt();
            ItemContainer var21 = null;

            for(var14 = 0; var14 < var8.getWorldObjects().size(); ++var14) {
               IsoWorldInventoryObject var25 = (IsoWorldInventoryObject)var8.getWorldObjects().get(var14);
               if (var25 != null && var25.getItem() instanceof InventoryContainer && var25.getItem().id == var12) {
                  var21 = ((InventoryContainer)var25.getItem()).getInventory();
                  break;
               }
            }

            if (var21 != null) {
               for(var14 = 0; var14 < var11; ++var14) {
                  var15 = var3.getInt();
                  var16 = var21.getItemWithID(var15);
                  if (var16 == null) {
                     alreadyRemoved.add(var15);
                  } else {
                     var21.Remove(var16);
                     var9.add(var16.getFullType());
                  }
               }

               var21.setExplored(true);
               var21.setHasBeenLooted(true);
            }
         }
      } else {
         int var17;
         short var20;
         if (var4 == 2) {
            var20 = var3.getByte();
            var23 = var3.getByte();
            var11 = var0.getInt();
            if (var8 != null && var20 >= 0 && var20 < var8.getObjects().size()) {
               IsoObject var26 = (IsoObject)var8.getObjects().get(var20);
               ItemContainer var27 = var26 != null ? var26.getContainerByIndex(var23) : null;
               if (var27 != null) {
                  for(int var31 = 0; var31 < var11; ++var31) {
                     var17 = var3.getInt();
                     InventoryItem var18 = var27.getItemWithID(var17);
                     if (var18 == null) {
                        alreadyRemoved.add(var17);
                     } else {
                        var27.Remove(var18);
                        var27.setExplored(true);
                        var27.setHasBeenLooted(true);
                        var10 = true;
                        var9.add(var18.getFullType());
                     }
                  }

                  LuaManager.updateOverlaySprite(var26);
               }
            }
         } else if (var4 == 3) {
            var20 = var3.getShort();
            var23 = var3.getByte();
            var11 = var0.getInt();
            BaseVehicle var28 = VehicleManager.instance.getVehicleByID(var20);
            if (var28 != null) {
               VehiclePart var29 = var28 == null ? null : var28.getPartByIndex(var23);
               ItemContainer var32 = var29 == null ? null : var29.getItemContainer();
               if (var32 != null) {
                  for(var17 = 0; var17 < var11; ++var17) {
                     int var33 = var3.getInt();
                     InventoryItem var19 = var32.getItemWithID(var33);
                     if (var19 == null) {
                        alreadyRemoved.add(var33);
                     } else {
                        var32.Remove(var19);
                        var32.setExplored(true);
                        var32.setHasBeenLooted(true);
                        var10 = true;
                        var9.add(var19.getFullType());
                     }
                  }
               }
            }
         }
      }

      for(var12 = 0; var12 < udpEngine.connections.size(); ++var12) {
         UdpConnection var24 = (UdpConnection)udpEngine.connections.get(var12);
         if (var24.getConnectedGUID() != var1.getConnectedGUID() && var8 != null && var24.RelevantTo((float)var8.x, (float)var8.y)) {
            var0.rewind();
            ByteBufferWriter var30 = var24.startPacket();
            PacketTypes.PacketType.RemoveInventoryItemFromContainer.doPacket(var30);
            var30.bb.put(var0);
            PacketTypes.PacketType.RemoveInventoryItemFromContainer.send(var24);
         }
      }

      if (!alreadyRemoved.isEmpty()) {
         ByteBufferWriter var22 = var1.startPacket();
         PacketTypes.PacketType.RemoveContestedItemsFromInventory.doPacket(var22);
         var22.putInt(alreadyRemoved.size());

         for(var23 = 0; var23 < alreadyRemoved.size(); ++var23) {
            var22.putInt((Integer)alreadyRemoved.get(var23));
         }

         PacketTypes.PacketType.RemoveContestedItemsFromInventory.send(var1);
      }

      alreadyRemoved.clear();
      LoggerManager.getLogger("item").write(var1.idStr + " \"" + var1.username + "\" container -" + var11 + " " + var5 + "," + var6 + "," + var7 + " " + var9.toString());
   }

   private static void readItemStats(ByteBuffer var0, InventoryItem var1) {
      int var2 = var0.getInt();
      float var3 = var0.getFloat();
      boolean var4 = var0.get() == 1;
      var1.setUses(var2);
      if (var1 instanceof DrainableComboItem) {
         ((DrainableComboItem)var1).setDelta(var3);
         ((DrainableComboItem)var1).updateWeight();
      }

      if (var4 && var1 instanceof Food var5) {
         var5.setHungChange(var0.getFloat());
         var5.setCalories(var0.getFloat());
         var5.setCarbohydrates(var0.getFloat());
         var5.setLipids(var0.getFloat());
         var5.setProteins(var0.getFloat());
         var5.setThirstChange(var0.getFloat());
         var5.setFluReduction(var0.getInt());
         var5.setPainReduction(var0.getFloat());
         var5.setEndChange(var0.getFloat());
         var5.setReduceFoodSickness(var0.getInt());
         var5.setStressChange(var0.getFloat());
         var5.setFatigueChange(var0.getFloat());
      }

   }

   static void receiveItemStats(ByteBuffer var0, UdpConnection var1, short var2) {
      short var3 = var0.getShort();
      int var4 = var0.getInt();
      int var5 = var0.getInt();
      int var6 = var0.getInt();
      IsoGridSquare var7 = IsoWorld.instance.CurrentCell.getGridSquare(var4, var5, var6);
      int var8;
      int var9;
      int var10;
      byte var15;
      ItemContainer var21;
      InventoryItem var23;
      switch (var3) {
         case 0:
            var15 = var0.get();
            var9 = var0.getInt();
            if (var7 != null && var15 >= 0 && var15 < var7.getStaticMovingObjects().size()) {
               IsoMovingObject var18 = (IsoMovingObject)var7.getStaticMovingObjects().get(var15);
               var21 = var18.getContainer();
               if (var21 != null) {
                  var23 = var21.getItemWithID(var9);
                  if (var23 != null) {
                     readItemStats(var0, var23);
                  }
               }
            }
            break;
         case 1:
            var8 = var0.getInt();
            if (var7 != null) {
               for(var9 = 0; var9 < var7.getWorldObjects().size(); ++var9) {
                  IsoWorldInventoryObject var17 = (IsoWorldInventoryObject)var7.getWorldObjects().get(var9);
                  if (var17.getItem() != null && var17.getItem().id == var8) {
                     readItemStats(var0, var17.getItem());
                     break;
                  }

                  if (var17.getItem() instanceof InventoryContainer) {
                     var21 = ((InventoryContainer)var17.getItem()).getInventory();
                     var23 = var21.getItemWithID(var8);
                     if (var23 != null) {
                        readItemStats(var0, var23);
                        break;
                     }
                  }
               }
            }
            break;
         case 2:
            var15 = var0.get();
            var9 = var0.get();
            var10 = var0.getInt();
            if (var7 != null && var15 >= 0 && var15 < var7.getObjects().size()) {
               IsoObject var19 = (IsoObject)var7.getObjects().get(var15);
               ItemContainer var22 = var19.getContainerByIndex(var9);
               if (var22 != null) {
                  InventoryItem var24 = var22.getItemWithID(var10);
                  if (var24 != null) {
                     readItemStats(var0, var24);
                  }
               }
            }
            break;
         case 3:
            var8 = var0.getShort();
            var9 = var0.get();
            var10 = var0.getInt();
            BaseVehicle var11 = VehicleManager.instance.getVehicleByID((short)var8);
            if (var11 != null) {
               VehiclePart var12 = var11.getPartByIndex(var9);
               if (var12 != null) {
                  ItemContainer var13 = var12.getItemContainer();
                  if (var13 != null) {
                     InventoryItem var14 = var13.getItemWithID(var10);
                     if (var14 != null) {
                        readItemStats(var0, var14);
                     }
                  }
               }
            }
      }

      for(var8 = 0; var8 < udpEngine.connections.size(); ++var8) {
         UdpConnection var16 = (UdpConnection)udpEngine.connections.get(var8);
         if (var16 != var1 && var16.RelevantTo((float)var4, (float)var5)) {
            ByteBufferWriter var20 = var16.startPacket();
            PacketTypes.PacketType.ItemStats.doPacket(var20);
            var0.rewind();
            var20.bb.put(var0);
            PacketTypes.PacketType.ItemStats.send(var16);
         }
      }

   }

   static void receiveRequestItemsForContainer(ByteBuffer var0, UdpConnection var1, short var2) {
      ByteBufferReader var3 = new ByteBufferReader(var0);
      short var4 = var0.getShort();
      String var5 = GameWindow.ReadString(var0);
      String var6 = GameWindow.ReadString(var0);
      int var7 = var3.getInt();
      int var8 = var3.getInt();
      int var9 = var3.getInt();
      short var10 = var3.getShort();
      byte var11 = -1;
      byte var12 = -1;
      int var13 = 0;
      short var14 = 0;
      IsoGridSquare var15 = IsoWorld.instance.CurrentCell.getGridSquare(var7, var8, var9);
      IsoObject var16 = null;
      ItemContainer var17 = null;
      int var25;
      if (var10 == 2) {
         var11 = var3.getByte();
         var12 = var3.getByte();
         if (var15 != null && var11 >= 0 && var11 < var15.getObjects().size()) {
            var16 = (IsoObject)var15.getObjects().get(var11);
            if (var16 != null) {
               var17 = var16.getContainerByIndex(var12);
               if (var17 == null || var17.isExplored()) {
                  return;
               }
            }
         }
      } else if (var10 == 3) {
         var14 = var3.getShort();
         var12 = var3.getByte();
         BaseVehicle var24 = VehicleManager.instance.getVehicleByID(var14);
         if (var24 != null) {
            VehiclePart var18 = ((BaseVehicle)var24).getPartByIndex(var12);
            var17 = var18 == null ? null : var18.getItemContainer();
            if (var17 == null || var17.isExplored()) {
               return;
            }
         }
      } else if (var10 == 1) {
         var13 = var3.getInt();

         for(var25 = 0; var25 < var15.getWorldObjects().size(); ++var25) {
            IsoWorldInventoryObject var19 = (IsoWorldInventoryObject)var15.getWorldObjects().get(var25);
            if (var19 != null && var19.getItem() instanceof InventoryContainer && var19.getItem().id == var13) {
               var17 = ((InventoryContainer)var19.getItem()).getInventory();
               break;
            }
         }
      } else if (var10 == 0) {
         var11 = var3.getByte();
         if (var15 != null && var11 >= 0 && var11 < var15.getStaticMovingObjects().size()) {
            var16 = (IsoObject)var15.getStaticMovingObjects().get(var11);
            if (var16 != null && var16.getContainer() != null) {
               if (var16.getContainer().isExplored()) {
                  return;
               }

               var17 = var16.getContainer();
            }
         }
      }

      if (var17 != null && !var17.isExplored()) {
         var17.setExplored(true);
         var25 = var17.Items.size();
         ItemPickerJava.fillContainer(var17, (IsoPlayer)IDToPlayerMap.get(var4));
         if (var25 != var17.Items.size()) {
            for(int var26 = 0; var26 < udpEngine.connections.size(); ++var26) {
               UdpConnection var20 = (UdpConnection)udpEngine.connections.get(var26);
               if (var20.RelevantTo((float)var15.x, (float)var15.y)) {
                  ByteBufferWriter var21 = var20.startPacket();
                  PacketTypes.PacketType.AddInventoryItemToContainer.doPacket(var21);
                  var21.putShort(var10);
                  var21.putInt(var7);
                  var21.putInt(var8);
                  var21.putInt(var9);
                  if (var10 == 0) {
                     var21.putByte(var11);
                  } else if (var10 == 1) {
                     var21.putInt(var13);
                  } else if (var10 == 3) {
                     var21.putShort(var14);
                     var21.putByte(var12);
                  } else {
                     var21.putByte(var11);
                     var21.putByte(var12);
                  }

                  try {
                     CompressIdenticalItems.save(var21.bb, var17.getItems(), (IsoGameCharacter)null);
                  } catch (Exception var23) {
                     var23.printStackTrace();
                  }

                  PacketTypes.PacketType.AddInventoryItemToContainer.send(var20);
               }
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
               for(int var7 = 0; var7 < udpEngine.connections.size(); ++var7) {
                  UdpConnection var8 = (UdpConnection)udpEngine.connections.get(var7);
                  if (var8.RelevantTo((float)var0.square.x, (float)var0.square.y)) {
                     ByteBufferWriter var4 = var8.startPacket();
                     PacketTypes.PacketType.AddInventoryItemToContainer.doPacket(var4);
                     if (var0 instanceof IsoDeadBody) {
                        var4.putShort((short)0);
                     } else if (var0 instanceof IsoWorldInventoryObject) {
                        var4.putShort((short)1);
                     } else if (var0 instanceof BaseVehicle) {
                        var4.putShort((short)3);
                     } else {
                        var4.putShort((short)2);
                     }

                     var4.putInt(var0.getSquare().getX());
                     var4.putInt(var0.getSquare().getY());
                     var4.putInt(var0.getSquare().getZ());
                     if (var0 instanceof IsoDeadBody) {
                        var4.putByte((byte)var0.getStaticMovingObjectIndex());
                     } else if (var0 instanceof IsoWorldInventoryObject) {
                        var4.putInt(((IsoWorldInventoryObject)var0).getItem().id);
                     } else if (var0 instanceof BaseVehicle) {
                        var4.putShort(((BaseVehicle)var0).VehicleID);
                        var4.putByte((byte)var1.vehiclePart.getIndex());
                     } else {
                        var4.putByte((byte)var0.getObjectIndex());
                        var4.putByte((byte)var0.getContainerIndex(var1));
                     }

                     try {
                        CompressIdenticalItems.save(var4.bb, var1.getItems(), (IsoGameCharacter)null);
                     } catch (Exception var6) {
                        var6.printStackTrace();
                     }

                     PacketTypes.PacketType.AddInventoryItemToContainer.send(var8);
                  }
               }

            }
         }
      }
   }

   private static void logDupeItem(UdpConnection var0, String var1) {
      IsoPlayer var2 = null;

      for(int var3 = 0; var3 < Players.size(); ++var3) {
         if (var0.username.equals(((IsoPlayer)Players.get(var3)).username)) {
            var2 = (IsoPlayer)Players.get(var3);
            break;
         }
      }

      String var4 = "";
      if (var2 != null) {
         var4 = LoggerManager.getPlayerCoords(var2);
         ZLogger var10000 = LoggerManager.getLogger("user");
         String var10001 = var2.getDisplayName();
         var10000.write("Error: Dupe item ID for " + var10001 + " " + var4);
      }

      ServerWorldDatabase.instance.addUserlog(var0.username, Userlog.UserlogType.DupeItem, var1, GameServer.class.getSimpleName(), 1);
   }

   static void receiveAddInventoryItemToContainer(ByteBuffer var0, UdpConnection var1, short var2) {
      ByteBufferReader var3 = new ByteBufferReader(var0);
      short var4 = var3.getShort();
      int var5 = var3.getInt();
      int var6 = var3.getInt();
      int var7 = var3.getInt();
      IsoGridSquare var8 = IsoWorld.instance.CurrentCell.getGridSquare(var5, var6, var7);
      HashSet var9 = new HashSet();
      byte var10 = 0;
      if (var8 == null) {
         DebugLog.log("ERROR sendItemsToContainer square is null");
      } else {
         ItemContainer var11 = null;
         IsoObject var12 = null;
         int var13;
         int var21;
         if (var4 == 0) {
            var13 = var3.getByte();
            if (var13 < 0 || var13 >= var8.getStaticMovingObjects().size()) {
               DebugLog.log("ERROR sendItemsToContainer invalid corpse index");
               return;
            }

            IsoObject var14 = (IsoObject)var8.getStaticMovingObjects().get(var13);
            if (var14 != null && var14.getContainer() != null) {
               var11 = var14.getContainer();
            }
         } else if (var4 == 1) {
            var13 = var3.getInt();

            for(var21 = 0; var21 < var8.getWorldObjects().size(); ++var21) {
               IsoWorldInventoryObject var15 = (IsoWorldInventoryObject)var8.getWorldObjects().get(var21);
               if (var15 != null && var15.getItem() instanceof InventoryContainer && var15.getItem().id == var13) {
                  var11 = ((InventoryContainer)var15.getItem()).getInventory();
                  break;
               }
            }

            if (var11 == null) {
               DebugLog.log("ERROR sendItemsToContainer can't find world item with id=" + var13);
               return;
            }
         } else {
            short var20;
            byte var22;
            if (var4 == 2) {
               var20 = var3.getByte();
               var22 = var3.getByte();
               if (var20 < 0 || var20 >= var8.getObjects().size()) {
                  DebugLog.log("ERROR sendItemsToContainer invalid object index");

                  for(int var23 = 0; var23 < var8.getObjects().size(); ++var23) {
                     if (((IsoObject)var8.getObjects().get(var23)).getContainer() != null) {
                        var20 = (byte)var23;
                        var22 = 0;
                        break;
                     }
                  }

                  if (var20 == -1) {
                     return;
                  }
               }

               var12 = (IsoObject)var8.getObjects().get(var20);
               var11 = var12 != null ? var12.getContainerByIndex(var22) : null;
            } else if (var4 == 3) {
               var20 = var3.getShort();
               var22 = var3.getByte();
               BaseVehicle var26 = VehicleManager.instance.getVehicleByID(var20);
               if (var26 == null) {
                  DebugLog.log("ERROR sendItemsToContainer invalid vehicle id");
                  return;
               }

               VehiclePart var16 = var26.getPartByIndex(var22);
               var11 = var16 == null ? null : var16.getItemContainer();
            }
         }

         if (var11 != null) {
            try {
               ArrayList var24 = CompressIdenticalItems.load(var3.bb, 195, (ArrayList)null, (ArrayList)null);

               for(var21 = 0; var21 < var24.size(); ++var21) {
                  InventoryItem var27 = (InventoryItem)var24.get(var21);
                  if (var27 != null) {
                     if (var11.containsID(var27.id)) {
                        System.out.println("Error: Dupe item ID for " + var1.username);
                        logDupeItem(var1, var27.getDisplayName());
                     } else {
                        var11.addItem(var27);
                        var11.setExplored(true);
                        var9.add(var27.getFullType());
                        if (var12 instanceof IsoMannequin) {
                           ((IsoMannequin)var12).wearItem(var27, (IsoGameCharacter)null);
                        }
                     }
                  }
               }
            } catch (Exception var17) {
               var17.printStackTrace();
            }

            if (var12 != null) {
               LuaManager.updateOverlaySprite(var12);
               if ("campfire".equals(var11.getType())) {
                  var12.sendObjectChange("container.customTemperature");
               }
            }
         }
      }

      for(int var18 = 0; var18 < udpEngine.connections.size(); ++var18) {
         UdpConnection var19 = (UdpConnection)udpEngine.connections.get(var18);
         if (var19.getConnectedGUID() != var1.getConnectedGUID() && var19.RelevantTo((float)var8.x, (float)var8.y)) {
            var0.rewind();
            ByteBufferWriter var25 = var19.startPacket();
            PacketTypes.PacketType.AddInventoryItemToContainer.doPacket(var25);
            var25.bb.put(var0);
            PacketTypes.PacketType.AddInventoryItemToContainer.send(var19);
         }
      }

      LoggerManager.getLogger("item").write(var1.idStr + " \"" + var1.username + "\" container +" + var10 + " " + var5 + "," + var6 + "," + var7 + " " + var9.toString());
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

         if (!var0.isDead()) {
            ServerWorldDatabase.instance.saveTransactionID(var0.username, var0.getTransactionID());
         }

         NetworkZombieManager.getInstance().clearTargetAuth(var1, var0);
         var0.removeFromWorld();
         var0.removeFromSquare();
         PlayerToAddressMap.remove(var0);
         IDToAddressMap.remove(var0.OnlineID);
         IDToPlayerMap.remove(var0.OnlineID);
         Players.remove(var0);
         SafeHouse.updateSafehousePlayersConnected();
         SafeHouse var6 = SafeHouse.hasSafehouse(var0);
         if (var6 != null && var6.isOwner(var0)) {
            Iterator var3 = IDToPlayerMap.values().iterator();

            while(var3.hasNext()) {
               IsoPlayer var4 = (IsoPlayer)var3.next();
               var6.checkTrespass(var4);
            }
         }

         var1.usernames[var0.PlayerIndex] = null;
         var1.players[var0.PlayerIndex] = null;
         var1.playerIDs[var0.PlayerIndex] = -1;
         var1.ReleventPos[var0.PlayerIndex] = null;
         var1.connectArea[var0.PlayerIndex] = null;

         for(int var7 = 0; var7 < udpEngine.connections.size(); ++var7) {
            UdpConnection var8 = (UdpConnection)udpEngine.connections.get(var7);
            ByteBufferWriter var5 = var8.startPacket();
            PacketTypes.PacketType.PlayerTimeout.doPacket(var5);
            var5.putShort(var0.OnlineID);
            PacketTypes.PacketType.PlayerTimeout.send(var8);
         }

         ServerLOS.instance.removePlayer(var0);
         ZombiePopulationManager.instance.updateLoadedAreas();
         DebugType var10000 = DebugType.Network;
         String var10001 = var0.getDisplayName();
         DebugLog.log(var10000, "Disconnected player \"" + var10001 + "\" " + var1.idStr);
         ZLogger var9 = LoggerManager.getLogger("user");
         var10001 = var1.idStr;
         var9.write(var10001 + " \"" + var0.getUsername() + "\" disconnected player " + LoggerManager.getPlayerCoords(var0));
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
      playerMovedToFastMap.put(var3, 0);
      SlotToConnection[var2] = var0;
      var0.playerIDs[0] = var3;
      IDToAddressMap.put(var3, var0.getConnectedGUID());
      var0.playerDownloadServer = new PlayerDownloadServer(var0);
      DebugLog.log(DebugType.Network, "Connected new client " + var0.username + " ID # " + var3);
      var0.playerDownloadServer.startConnectionTest();
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

   static void receiveReplaceOnCooked(ByteBuffer var0, UdpConnection var1, short var2) {
      ByteBufferReader var3 = new ByteBufferReader(var0);
      int var4 = var3.getInt();
      int var5 = var3.getInt();
      int var6 = var3.getInt();
      byte var7 = var3.getByte();
      byte var8 = var3.getByte();
      int var9 = var3.getInt();
      IsoGridSquare var10 = ServerMap.instance.getGridSquare(var4, var5, var6);
      if (var10 != null) {
         if (var7 >= 0 && var7 < var10.getObjects().size()) {
            IsoObject var11 = (IsoObject)var10.getObjects().get(var7);
            if (var11 != null) {
               ItemContainer var12 = var11.getContainerByIndex(var8);
               if (var12 != null) {
                  InventoryItem var13 = var12.getItemWithID(var9);
                  if (var13 != null) {
                     Food var14 = (Food)Type.tryCastTo(var13, Food.class);
                     if (var14 != null) {
                        if (var14.getReplaceOnCooked() != null && !var14.isRotten()) {
                           for(int var15 = 0; var15 < var14.getReplaceOnCooked().size(); ++var15) {
                              InventoryItem var16 = var12.AddItem((String)var14.getReplaceOnCooked().get(var15));
                              if (var16 != null) {
                                 var16.copyConditionModData(var14);
                                 if (var16 instanceof Food && var14 instanceof Food) {
                                 }

                                 if (var16 instanceof Food && ((Food)var16).isBadInMicrowave() && var12.isMicrowave()) {
                                    var16.setUnhappyChange(5.0F);
                                    var16.setBoredomChange(5.0F);
                                    ((Food)var16).setCookedInMicrowave(true);
                                 }

                                 sendAddItemToContainer(var12, var16);
                              }
                           }

                           sendRemoveItemFromContainer(var12, var14);
                           var12.Remove((InventoryItem)var14);
                           IsoWorld.instance.CurrentCell.addToProcessItemsRemove((InventoryItem)var14);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   static void receivePlayerDataRequest(ByteBuffer var0, UdpConnection var1, short var2) {
      PlayerDataRequestPacket var3 = new PlayerDataRequestPacket();
      var3.parse(var0, var1);
      if (var3.isConsistent()) {
         var3.process(var1);
      }

   }

   static void receiveRequestData(ByteBuffer var0, UdpConnection var1, short var2) {
      RequestDataPacket var3 = new RequestDataPacket();
      var3.parse(var0, var1);
      var3.processServer(PacketTypes.PacketType.RequestData, var1);
   }

   public static void sendMetaGrid(int var0, int var1, int var2, UdpConnection var3) {
      IsoMetaGrid var4 = IsoWorld.instance.MetaGrid;
      if (var0 >= var4.getMinX() && var0 <= var4.getMaxX() && var1 >= var4.getMinY() && var1 <= var4.getMaxY()) {
         IsoMetaCell var5 = var4.getCellData(var0, var1);
         if (var5.info != null && var2 >= 0 && var2 < var5.info.RoomList.size()) {
            ByteBufferWriter var6 = var3.startPacket();
            PacketTypes.PacketType.MetaGrid.doPacket(var6);
            var6.putShort((short)var0);
            var6.putShort((short)var1);
            var6.putShort((short)var2);
            var6.putBoolean(var5.info.getRoom(var2).def.bLightsActive);
            PacketTypes.PacketType.MetaGrid.send(var3);
         }
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

   private static void receivePlayerConnect(ByteBuffer var0, UdpConnection var1, String var2) {
      ConnectionManager.log("receive-packet", "player-connect", var1);
      DebugLog.General.println("User:'" + var2 + "' ip:" + var1.ip + " is trying to connect");
      byte var3 = var0.get();
      if (var3 >= 0 && var3 < 4 && var1.players[var3] == null) {
         byte var4 = (byte)Math.min(20, var0.get());
         var1.ReleventRange = (byte)(var4 / 2 + 2);
         float var5 = var0.getFloat();
         float var6 = var0.getFloat();
         float var7 = var0.getFloat();
         var1.ReleventPos[var3].x = var5;
         var1.ReleventPos[var3].y = var6;
         var1.ReleventPos[var3].z = var7;
         var1.connectArea[var3] = null;
         var1.ChunkGridWidth = var4;
         var1.loadedCells[var3] = new ClientServerMap(var3, (int)var5, (int)var6, var4);
         SurvivorDesc var8 = SurvivorFactory.CreateSurvivor();

         try {
            var8.load(var0, 195, (IsoGameCharacter)null);
         } catch (IOException var23) {
            var23.printStackTrace();
         }

         IsoPlayer var9 = new IsoPlayer((IsoCell)null, var8, (int)var5, (int)var6, (int)var7);
         var9.realx = var5;
         var9.realy = var6;
         var9.realz = (byte)((int)var7);
         var9.PlayerIndex = var3;
         var9.OnlineChunkGridWidth = var4;
         Players.add(var9);
         var9.bRemote = true;

         try {
            var9.getHumanVisual().load(var0, 195);
            var9.getItemVisuals().load(var0, 195);
         } catch (IOException var22) {
            var22.printStackTrace();
         }

         short var10 = var1.playerIDs[var3];
         IDToPlayerMap.put(var10, var9);
         var1.players[var3] = var9;
         PlayerToAddressMap.put(var9, var1.getConnectedGUID());
         var9.setOnlineID(var10);

         try {
            var9.getXp().load(var0, 195);
         } catch (IOException var21) {
            var21.printStackTrace();
         }

         var9.setAllChatMuted(var0.get() == 1);
         var1.allChatMuted = var9.isAllChatMuted();
         var9.setTagPrefix(GameWindow.ReadString(var0));
         var9.setTagColor(new ColorInfo(var0.getFloat(), var0.getFloat(), var0.getFloat(), 1.0F));
         var9.setTransactionID(var0.getInt());
         var9.setHoursSurvived(var0.getDouble());
         var9.setZombieKills(var0.getInt());
         var9.setDisplayName(GameWindow.ReadString(var0));
         var9.setSpeakColour(new Color(var0.getFloat(), var0.getFloat(), var0.getFloat(), 1.0F));
         var9.showTag = var0.get() == 1;
         var9.factionPvp = var0.get() == 1;
         if (SteamUtils.isSteamModeEnabled()) {
            var9.setSteamID(var1.steamID);
            String var11 = GameWindow.ReadStringUTF(var0);
            SteamGameServer.BUpdateUserData(var1.steamID, var1.username, 0);
         }

         byte var25 = var0.get();
         InventoryItem var12 = null;
         if (var25 == 1) {
            try {
               var12 = InventoryItem.loadItem(var0, 195);
            } catch (IOException var20) {
               var20.printStackTrace();
               return;
            }

            if (var12 == null) {
               LoggerManager.getLogger("user").write(var1.idStr + " equipped unknown item");
               return;
            }

            var9.setPrimaryHandItem(var12);
         }

         var12 = null;
         byte var13 = var0.get();
         if (var13 == 2) {
            var9.setSecondaryHandItem(var9.getPrimaryHandItem());
         }

         if (var13 == 1) {
            try {
               var12 = InventoryItem.loadItem(var0, 195);
            } catch (IOException var19) {
               var19.printStackTrace();
               return;
            }

            if (var12 == null) {
               LoggerManager.getLogger("user").write(var1.idStr + " equipped unknown item");
               return;
            }

            var9.setSecondaryHandItem(var12);
         }

         int var14 = var0.getInt();

         int var15;
         for(var15 = 0; var15 < var14; ++var15) {
            String var16 = GameWindow.ReadString(var0);
            InventoryItem var17 = InventoryItemFactory.CreateItem(GameWindow.ReadString(var0));
            if (var17 != null) {
               var9.setAttachedItem(var16, var17);
            }
         }

         var15 = var0.getInt();
         var9.remoteSneakLvl = var15;
         var9.username = var2;
         var9.accessLevel = PlayerType.toString(var1.accessLevel);
         if (!var9.accessLevel.equals("") && CoopSlave.instance == null) {
            var9.setGhostMode(true);
            var9.setInvisible(true);
            var9.setGodMod(true);
         }

         ChatServer.getInstance().initPlayer(var9.OnlineID);
         var1.setFullyConnected();
         sendWeather(var1);
         SafetySystemManager.restoreSafety(var9);

         for(int var26 = 0; var26 < udpEngine.connections.size(); ++var26) {
            UdpConnection var28 = (UdpConnection)udpEngine.connections.get(var26);
            sendPlayerConnect(var9, var28);
         }

         SyncInjuriesPacket var27 = new SyncInjuriesPacket();
         Iterator var29 = IDToPlayerMap.values().iterator();

         while(var29.hasNext()) {
            IsoPlayer var18 = (IsoPlayer)var29.next();
            if (var18.getOnlineID() != var9.getOnlineID() && var18.isAlive()) {
               sendPlayerConnect(var18, var1);
               var27.set(var18);
               sendPlayerInjuries(var1, var27);
            }
         }

         var1.loadedCells[var3].setLoaded();
         var1.loadedCells[var3].sendPacket(var1);
         preventIndoorZombies((int)var5, (int)var6, (int)var7);
         ServerLOS.instance.addPlayer(var9);
         ZLogger var10000 = LoggerManager.getLogger("user");
         String var10001 = var1.idStr;
         var10000.write(var10001 + " \"" + var9.username + "\" fully connected " + LoggerManager.getPlayerCoords(var9));

         try {
            var29 = NonPvpZone.getAllZones().iterator();

            while(var29.hasNext()) {
               NonPvpZone var30 = (NonPvpZone)var29.next();
               sendNonPvpZone(var30, false, var1);
            }
         } catch (Exception var24) {
            DebugLog.Multiplayer.printException(var24, "Send non PVP zones", LogSeverity.Error);
         }

      }
   }

   static void receivePlayerSave(ByteBuffer var0, UdpConnection var1, short var2) {
      if ((Calendar.getInstance().getTimeInMillis() - previousSave) / 60000L >= 0L) {
         byte var3 = var0.get();
         if (var3 >= 0 && var3 < 4) {
            short var4 = var0.getShort();
            float var5 = var0.getFloat();
            float var6 = var0.getFloat();
            float var7 = var0.getFloat();
            ServerMap.instance.saveZoneInsidePlayerInfluence(var4);
         }
      }
   }

   static void receiveSendPlayerProfile(ByteBuffer var0, UdpConnection var1, short var2) {
      ServerPlayerDB.getInstance().serverUpdateNetworkCharacter(var0, var1);
   }

   static void receiveLoadPlayerProfile(ByteBuffer var0, UdpConnection var1, short var2) {
      ServerPlayerDB.getInstance().serverLoadNetworkCharacter(var0, var1);
   }

   private static void coopAccessGranted(int var0, UdpConnection var1) {
      ByteBufferWriter var2 = var1.startPacket();
      PacketTypes.PacketType.AddCoopPlayer.doPacket(var2);
      var2.putBoolean(true);
      var2.putByte((byte)var0);
      PacketTypes.PacketType.AddCoopPlayer.send(var1);
   }

   private static void coopAccessDenied(String var0, int var1, UdpConnection var2) {
      ByteBufferWriter var3 = var2.startPacket();
      PacketTypes.PacketType.AddCoopPlayer.doPacket(var3);
      var3.putBoolean(false);
      var3.putByte((byte)var1);
      var3.putUTF(var0);
      PacketTypes.PacketType.AddCoopPlayer.send(var2);
   }

   static void receiveAddCoopPlayer(ByteBuffer var0, UdpConnection var1, short var2) {
      byte var3 = var0.get();
      byte var4 = var0.get();
      if (!ServerOptions.instance.AllowCoop.getValue() && var4 != 0) {
         coopAccessDenied("Coop players not allowed", var4, var1);
      } else if (var4 >= 0 && var4 < 4) {
         if (var1.players[var4] != null && !var1.players[var4].isDead()) {
            coopAccessDenied("Coop player " + (var4 + 1) + "/4 already exists", var4, var1);
         } else {
            String var5;
            if (var3 != 1) {
               if (var3 == 2) {
                  var5 = var1.usernames[var4];
                  if (var5 == null) {
                     coopAccessDenied("Coop player login wasn't received", var4, var1);
                  } else {
                     DebugLog.log("coop player=" + (var4 + 1) + "/4 username=\"" + var5 + "\" player info received");
                     receivePlayerConnect(var0, var1, var5);
                  }
               }
            } else {
               var5 = GameWindow.ReadStringUTF(var0);
               if (var5.isEmpty()) {
                  coopAccessDenied("No username given", var4, var1);
               } else {
                  for(int var6 = 0; var6 < udpEngine.connections.size(); ++var6) {
                     UdpConnection var7 = (UdpConnection)udpEngine.connections.get(var6);

                     for(int var8 = 0; var8 < 4; ++var8) {
                        if ((var7 != var1 || var4 != var8) && var5.equals(var7.usernames[var8])) {
                           coopAccessDenied("User \"" + var5 + "\" already connected", var4, var1);
                           return;
                        }
                     }
                  }

                  DebugLog.log("coop player=" + (var4 + 1) + "/4 username=\"" + var5 + "\" is joining");
                  short var10;
                  float var13;
                  if (var1.players[var4] != null) {
                     DebugLog.log("coop player=" + (var4 + 1) + "/4 username=\"" + var5 + "\" is replacing dead player");
                     var10 = var1.players[var4].OnlineID;
                     disconnectPlayer(var1.players[var4], var1);
                     float var12 = var0.getFloat();
                     var13 = var0.getFloat();
                     var1.usernames[var4] = var5;
                     var1.ReleventPos[var4] = new Vector3(var12, var13, 0.0F);
                     var1.connectArea[var4] = new Vector3(var12 / 10.0F, var13 / 10.0F, (float)var1.ChunkGridWidth);
                     var1.playerIDs[var4] = var10;
                     IDToAddressMap.put(var10, var1.getConnectedGUID());
                     coopAccessGranted(var4, var1);
                     ZombiePopulationManager.instance.updateLoadedAreas();
                     if (ChatServer.isInited()) {
                        ChatServer.getInstance().initPlayer(var10);
                     }

                  } else if (getPlayerCount() >= ServerOptions.getInstance().getMaxPlayers()) {
                     coopAccessDenied("Server is full", var4, var1);
                  } else {
                     var10 = -1;

                     short var11;
                     for(var11 = 0; var11 < udpEngine.getMaxConnections(); ++var11) {
                        if (SlotToConnection[var11] == var1) {
                           var10 = var11;
                           break;
                        }
                     }

                     var11 = (short)(var10 * 4 + var4);
                     DebugLog.log("coop player=" + (var4 + 1) + "/4 username=\"" + var5 + "\" assigned id=" + var11);
                     var13 = var0.getFloat();
                     float var9 = var0.getFloat();
                     var1.usernames[var4] = var5;
                     var1.ReleventPos[var4] = new Vector3(var13, var9, 0.0F);
                     var1.playerIDs[var4] = var11;
                     var1.connectArea[var4] = new Vector3(var13 / 10.0F, var9 / 10.0F, (float)var1.ChunkGridWidth);
                     IDToAddressMap.put(var11, var1.getConnectedGUID());
                     coopAccessGranted(var4, var1);
                     ZombiePopulationManager.instance.updateLoadedAreas();
                  }
               }
            }
         }
      } else {
         coopAccessDenied("Invalid coop player index", var4, var1);
      }
   }

   private static void sendInitialWorldState(UdpConnection var0) {
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

   static void receiveObjectModData(ByteBuffer var0, UdpConnection var1, short var2) {
      int var3 = var0.getInt();
      int var4 = var0.getInt();
      int var5 = var0.getInt();
      int var6 = var0.getInt();
      boolean var7 = var0.get() == 1;
      IsoGridSquare var8 = IsoWorld.instance.CurrentCell.getGridSquare(var3, var4, var5);
      if (var8 != null && var6 >= 0 && var6 < var8.getObjects().size()) {
         IsoObject var9 = (IsoObject)var8.getObjects().get(var6);
         int var10;
         if (var7) {
            var10 = var9.getWaterAmount();

            try {
               var9.getModData().load(var0, 195);
            } catch (IOException var12) {
               var12.printStackTrace();
            }

            if (var10 != var9.getWaterAmount()) {
               LuaEventManager.triggerEvent("OnWaterAmountChange", var9, var10);
            }
         } else if (var9.hasModData()) {
            var9.getModData().wipe();
         }

         for(var10 = 0; var10 < udpEngine.connections.size(); ++var10) {
            UdpConnection var11 = (UdpConnection)udpEngine.connections.get(var10);
            if (var11.getConnectedGUID() != var1.getConnectedGUID() && var11.RelevantTo((float)var3, (float)var4)) {
               sendObjectModData(var9, var11);
            }
         }
      } else if (var8 != null) {
         DebugLog.log("receiveObjectModData: index=" + var6 + " is invalid x,y,z=" + var3 + "," + var4 + "," + var5);
      } else if (bDebug) {
         DebugLog.log("receiveObjectModData: sq is null x,y,z=" + var3 + "," + var4 + "," + var5);
      }

   }

   private static void sendObjectModData(IsoObject var0, UdpConnection var1) {
      if (var0.getSquare() != null) {
         ByteBufferWriter var2 = var1.startPacket();
         PacketTypes.PacketType.ObjectModData.doPacket(var2);
         var2.putInt(var0.getSquare().getX());
         var2.putInt(var0.getSquare().getY());
         var2.putInt(var0.getSquare().getZ());
         var2.putInt(var0.getSquare().getObjects().indexOf(var0));
         if (var0.getModData().isEmpty()) {
            var2.putByte((byte)0);
         } else {
            var2.putByte((byte)1);

            try {
               var0.getModData().save(var2.bb);
            } catch (IOException var4) {
               var4.printStackTrace();
            }
         }

         PacketTypes.PacketType.ObjectModData.send(var1);
      }
   }

   public static void sendObjectModData(IsoObject var0) {
      if (!bSoftReset && !bFastForward) {
         for(int var1 = 0; var1 < udpEngine.connections.size(); ++var1) {
            UdpConnection var2 = (UdpConnection)udpEngine.connections.get(var1);
            if (var2.RelevantTo(var0.getX(), var0.getY())) {
               sendObjectModData(var0, var2);
            }
         }

      }
   }

   public static void sendSlowFactor(IsoGameCharacter var0) {
      if (var0 instanceof IsoPlayer) {
         if (PlayerToAddressMap.containsKey(var0)) {
            long var1 = (Long)PlayerToAddressMap.get((IsoPlayer)var0);
            UdpConnection var3 = udpEngine.getActiveConnection(var1);
            if (var3 != null) {
               ByteBufferWriter var4 = var3.startPacket();
               PacketTypes.PacketType.SlowFactor.doPacket(var4);
               var4.putByte((byte)((IsoPlayer)var0).PlayerIndex);
               var4.putFloat(var0.getSlowTimer());
               var4.putFloat(var0.getSlowFactor());
               PacketTypes.PacketType.SlowFactor.send(var3);
            }
         }
      }
   }

   private static void sendObjectChange(IsoObject var0, String var1, KahluaTable var2, UdpConnection var3) {
      if (var0.getSquare() != null) {
         ByteBufferWriter var4 = var3.startPacket();
         PacketTypes.PacketType.ObjectChange.doPacket(var4);
         if (var0 instanceof IsoPlayer) {
            var4.putByte((byte)1);
            var4.putShort(((IsoPlayer)var0).OnlineID);
         } else if (var0 instanceof BaseVehicle) {
            var4.putByte((byte)2);
            var4.putShort(((BaseVehicle)var0).getId());
         } else if (var0 instanceof IsoWorldInventoryObject) {
            var4.putByte((byte)3);
            var4.putInt(var0.getSquare().getX());
            var4.putInt(var0.getSquare().getY());
            var4.putInt(var0.getSquare().getZ());
            var4.putInt(((IsoWorldInventoryObject)var0).getItem().getID());
         } else if (var0 instanceof IsoDeadBody) {
            var4.putByte((byte)4);
            var4.putInt(var0.getSquare().getX());
            var4.putInt(var0.getSquare().getY());
            var4.putInt(var0.getSquare().getZ());
            var4.putInt(var0.getStaticMovingObjectIndex());
         } else {
            var4.putByte((byte)0);
            var4.putInt(var0.getSquare().getX());
            var4.putInt(var0.getSquare().getY());
            var4.putInt(var0.getSquare().getZ());
            var4.putInt(var0.getSquare().getObjects().indexOf(var0));
         }

         var4.putUTF(var1);
         var0.saveChange(var1, var2, var4.bb);
         PacketTypes.PacketType.ObjectChange.send(var3);
      }
   }

   public static void sendObjectChange(IsoObject var0, String var1, KahluaTable var2) {
      if (!bSoftReset) {
         if (var0 != null) {
            for(int var3 = 0; var3 < udpEngine.connections.size(); ++var3) {
               UdpConnection var4 = (UdpConnection)udpEngine.connections.get(var3);
               if (var4.RelevantTo(var0.getX(), var0.getY())) {
                  sendObjectChange(var0, var1, var2, var4);
               }
            }

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

   private static void updateHandEquips(UdpConnection var0, IsoPlayer var1) {
      ByteBufferWriter var2 = var0.startPacket();
      PacketTypes.PacketType.Equip.doPacket(var2);
      var2.putShort(var1.OnlineID);
      var2.putByte((byte)0);
      var2.putByte((byte)(var1.getPrimaryHandItem() != null ? 1 : 0));
      if (var1.getPrimaryHandItem() != null) {
         try {
            var1.getPrimaryHandItem().saveWithSize(var2.bb, false);
            if (var1.getPrimaryHandItem().getVisual() != null) {
               var2.bb.put((byte)1);
               var1.getPrimaryHandItem().getVisual().save(var2.bb);
            } else {
               var2.bb.put((byte)0);
            }
         } catch (IOException var5) {
            var5.printStackTrace();
         }
      }

      PacketTypes.PacketType.Equip.send(var0);
      var2 = var0.startPacket();
      PacketTypes.PacketType.Equip.doPacket(var2);
      var2.putShort(var1.OnlineID);
      var2.putByte((byte)1);
      if (var1.getSecondaryHandItem() == var1.getPrimaryHandItem() && var1.getSecondaryHandItem() != null) {
         var2.putByte((byte)2);
      } else {
         var2.putByte((byte)(var1.getSecondaryHandItem() != null ? 1 : 0));
      }

      if (var1.getSecondaryHandItem() != null) {
         try {
            var1.getSecondaryHandItem().saveWithSize(var2.bb, false);
            if (var1.getSecondaryHandItem().getVisual() != null) {
               var2.bb.put((byte)1);
               var1.getSecondaryHandItem().getVisual().save(var2.bb);
            } else {
               var2.bb.put((byte)0);
            }
         } catch (IOException var4) {
            var4.printStackTrace();
         }
      }

      PacketTypes.PacketType.Equip.send(var0);
   }

   public static void receiveSyncCustomLightSettings(ByteBuffer var0, UdpConnection var1, short var2) {
      int var3 = var0.getInt();
      int var4 = var0.getInt();
      int var5 = var0.getInt();
      byte var6 = var0.get();
      IsoGridSquare var7 = ServerMap.instance.getGridSquare(var3, var4, var5);
      if (var7 != null && var6 >= 0 && var6 < var7.getObjects().size()) {
         if (var7.getObjects().get(var6) instanceof IsoLightSwitch) {
            ((IsoLightSwitch)var7.getObjects().get(var6)).receiveSyncCustomizedSettings(var0, var1);
         } else {
            DebugLog.log("Sync Lightswitch custom settings: found object not a instance of IsoLightSwitch, x,y,z=" + var3 + "," + var4 + "," + var5);
         }
      } else if (var7 != null) {
         DebugLog.log("Sync Lightswitch custom settings: index=" + var6 + " is invalid x,y,z=" + var3 + "," + var4 + "," + var5);
      } else {
         DebugLog.log("Sync Lightswitch custom settings: sq is null x,y,z=" + var3 + "," + var4 + "," + var5);
      }

   }

   private static void sendAlarmClock_Player(short var0, int var1, boolean var2, int var3, int var4, boolean var5, UdpConnection var6) {
      ByteBufferWriter var7 = var6.startPacket();
      PacketTypes.PacketType.SyncAlarmClock.doPacket(var7);
      var7.putShort(AlarmClock.PacketPlayer);
      var7.putShort(var0);
      var7.putInt(var1);
      var7.putByte((byte)(var2 ? 1 : 0));
      if (!var2) {
         var7.putInt(var3);
         var7.putInt(var4);
         var7.putByte((byte)(var5 ? 1 : 0));
      }

      PacketTypes.PacketType.SyncAlarmClock.send(var6);
   }

   private static void sendAlarmClock_World(int var0, int var1, int var2, int var3, boolean var4, int var5, int var6, boolean var7, UdpConnection var8) {
      ByteBufferWriter var9 = var8.startPacket();
      PacketTypes.PacketType.SyncAlarmClock.doPacket(var9);
      var9.putShort(AlarmClock.PacketWorld);
      var9.putInt(var0);
      var9.putInt(var1);
      var9.putInt(var2);
      var9.putInt(var3);
      var9.putByte((byte)(var4 ? 1 : 0));
      if (!var4) {
         var9.putInt(var5);
         var9.putInt(var6);
         var9.putByte((byte)(var7 ? 1 : 0));
      }

      PacketTypes.PacketType.SyncAlarmClock.send(var8);
   }

   static void receiveSyncAlarmClock(ByteBuffer var0, UdpConnection var1, short var2) {
      short var3 = var0.getShort();
      int var5;
      int var7;
      if (var3 == AlarmClock.PacketPlayer) {
         short var16 = var0.getShort();
         var5 = var0.getInt();
         boolean var17 = var0.get() == 1;
         var7 = 0;
         int var18 = 0;
         boolean var19 = false;
         if (!var17) {
            var7 = var0.getInt();
            var18 = var0.getInt();
            var19 = var0.get() == 1;
         }

         IsoPlayer var20 = getPlayerFromConnection(var1, var16);
         if (var20 != null) {
            for(int var21 = 0; var21 < udpEngine.connections.size(); ++var21) {
               UdpConnection var22 = (UdpConnection)udpEngine.connections.get(var21);
               if (var22 != var1) {
                  sendAlarmClock_Player(var20.getOnlineID(), var5, var17, var7, var18, var19, var22);
               }
            }
         }

      } else if (var3 == AlarmClock.PacketWorld) {
         int var4 = var0.getInt();
         var5 = var0.getInt();
         int var6 = var0.getInt();
         var7 = var0.getInt();
         boolean var8 = var0.get() == 1;
         int var9 = 0;
         int var10 = 0;
         boolean var11 = false;
         if (!var8) {
            var9 = var0.getInt();
            var10 = var0.getInt();
            var11 = var0.get() == 1;
         }

         IsoGridSquare var12 = ServerMap.instance.getGridSquare(var4, var5, var6);
         if (var12 == null) {
            DebugLog.log("SyncAlarmClock: sq is null x,y,z=" + var4 + "," + var5 + "," + var6);
         } else {
            AlarmClock var13 = null;

            int var14;
            for(var14 = 0; var14 < var12.getWorldObjects().size(); ++var14) {
               IsoWorldInventoryObject var15 = (IsoWorldInventoryObject)var12.getWorldObjects().get(var14);
               if (var15 != null && var15.getItem() instanceof AlarmClock && var15.getItem().id == var7) {
                  var13 = (AlarmClock)var15.getItem();
                  break;
               }
            }

            if (var13 == null) {
               DebugLog.log("SyncAlarmClock: AlarmClock is null x,y,z=" + var4 + "," + var5 + "," + var6);
            } else {
               if (var8) {
                  var13.stopRinging();
               } else {
                  var13.setHour(var9);
                  var13.setMinute(var10);
                  var13.setAlarmSet(var11);
               }

               for(var14 = 0; var14 < udpEngine.connections.size(); ++var14) {
                  UdpConnection var23 = (UdpConnection)udpEngine.connections.get(var14);
                  if (var23 != var1) {
                     sendAlarmClock_World(var4, var5, var6, var7, var8, var9, var10, var11, var23);
                  }
               }
            }

         }
      }
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

   static void receiveSyncIsoObjectReq(ByteBuffer var0, UdpConnection var1, short var2) {
      short var3 = var0.getShort();
      if (var3 <= 50 && var3 > 0) {
         ByteBufferWriter var4 = var1.startPacket();
         PacketTypes.PacketType.SyncIsoObjectReq.doPacket(var4);
         var4.putShort(var3);

         for(int var5 = 0; var5 < var3; ++var5) {
            int var6 = var0.getInt();
            int var7 = var0.getInt();
            int var8 = var0.getInt();
            byte var9 = var0.get();
            IsoGridSquare var10 = ServerMap.instance.getGridSquare(var6, var7, var8);
            if (var10 != null && var9 >= 0 && var9 < var10.getObjects().size()) {
               ((IsoObject)var10.getObjects().get(var9)).syncIsoObjectSend(var4);
            } else if (var10 != null) {
               var4.putInt(var10.getX());
               var4.putInt(var10.getY());
               var4.putInt(var10.getZ());
               var4.putByte(var9);
               var4.putByte((byte)0);
               var4.putByte((byte)0);
            } else {
               var4.putInt(var6);
               var4.putInt(var7);
               var4.putInt(var8);
               var4.putByte(var9);
               var4.putByte((byte)2);
               var4.putByte((byte)0);
            }
         }

         PacketTypes.PacketType.SyncIsoObjectReq.send(var1);
      }
   }

   static void receiveSyncObjects(ByteBuffer var0, UdpConnection var1, short var2) {
      short var3 = var0.getShort();
      if (var3 == 1) {
         SyncObjectChunkHashes(var0, var1);
      } else if (var3 == 3) {
         SyncObjectsGridSquareRequest(var0, var1);
      } else if (var3 == 5) {
         SyncObjectsRequest(var0, var1);
      }

   }

   public static void SyncObjectChunkHashes(ByteBuffer var0, UdpConnection var1) {
      short var2 = var0.getShort();
      if (var2 <= 10 && var2 > 0) {
         ByteBufferWriter var3 = var1.startPacket();
         PacketTypes.PacketType.SyncObjects.doPacket(var3);
         var3.putShort((short)2);
         int var4 = var3.bb.position();
         var3.putShort((short)0);
         int var5 = 0;

         int var6;
         for(var6 = 0; var6 < var2; ++var6) {
            int var7 = var0.getInt();
            int var8 = var0.getInt();
            long var9 = var0.getLong();
            IsoChunk var11 = ServerMap.instance.getChunk(var7, var8);
            if (var11 != null) {
               ++var5;
               var3.putShort((short)var11.wx);
               var3.putShort((short)var11.wy);
               var3.putLong(var11.getHashCodeObjects());
               int var12 = var3.bb.position();
               var3.putShort((short)0);
               int var13 = 0;

               int var14;
               for(var14 = var7 * 10; var14 < var7 * 10 + 10; ++var14) {
                  for(int var15 = var8 * 10; var15 < var8 * 10 + 10; ++var15) {
                     for(int var16 = 0; var16 <= 7; ++var16) {
                        IsoGridSquare var17 = ServerMap.instance.getGridSquare(var14, var15, var16);
                        if (var17 == null) {
                           break;
                        }

                        var3.putByte((byte)(var17.getX() - var11.wx * 10));
                        var3.putByte((byte)(var17.getY() - var11.wy * 10));
                        var3.putByte((byte)var17.getZ());
                        var3.putInt((int)var17.getHashCodeObjects());
                        ++var13;
                     }
                  }
               }

               var14 = var3.bb.position();
               var3.bb.position(var12);
               var3.putShort((short)var13);
               var3.bb.position(var14);
            }
         }

         var6 = var3.bb.position();
         var3.bb.position(var4);
         var3.putShort((short)var5);
         var3.bb.position(var6);
         PacketTypes.PacketType.SyncObjects.send(var1);
      }
   }

   public static void SyncObjectChunkHashes(IsoChunk var0, UdpConnection var1) {
      ByteBufferWriter var2 = var1.startPacket();
      PacketTypes.PacketType.SyncObjects.doPacket(var2);
      var2.putShort((short)2);
      var2.putShort((short)1);
      var2.putShort((short)var0.wx);
      var2.putShort((short)var0.wy);
      var2.putLong(var0.getHashCodeObjects());
      int var3 = var2.bb.position();
      var2.putShort((short)0);
      int var4 = 0;

      int var5;
      for(var5 = var0.wx * 10; var5 < var0.wx * 10 + 10; ++var5) {
         for(int var6 = var0.wy * 10; var6 < var0.wy * 10 + 10; ++var6) {
            for(int var7 = 0; var7 <= 7; ++var7) {
               IsoGridSquare var8 = ServerMap.instance.getGridSquare(var5, var6, var7);
               if (var8 == null) {
                  break;
               }

               var2.putByte((byte)(var8.getX() - var0.wx * 10));
               var2.putByte((byte)(var8.getY() - var0.wy * 10));
               var2.putByte((byte)var8.getZ());
               var2.putInt((int)var8.getHashCodeObjects());
               ++var4;
            }
         }
      }

      var5 = var2.bb.position();
      var2.bb.position(var3);
      var2.putShort((short)var4);
      var2.bb.position(var5);
      PacketTypes.PacketType.SyncObjects.send(var1);
   }

   public static void SyncObjectsGridSquareRequest(ByteBuffer var0, UdpConnection var1) {
      short var2 = var0.getShort();
      if (var2 <= 100 && var2 > 0) {
         ByteBufferWriter var3 = var1.startPacket();
         PacketTypes.PacketType.SyncObjects.doPacket(var3);
         var3.putShort((short)4);
         int var4 = var3.bb.position();
         var3.putShort((short)0);
         int var5 = 0;

         int var6;
         for(var6 = 0; var6 < var2; ++var6) {
            int var7 = var0.getInt();
            int var8 = var0.getInt();
            byte var9 = var0.get();
            IsoGridSquare var10 = ServerMap.instance.getGridSquare(var7, var8, var9);
            if (var10 != null) {
               ++var5;
               var3.putInt(var7);
               var3.putInt(var8);
               var3.putByte((byte)var9);
               var3.putByte((byte)var10.getObjects().size());
               var3.putInt(0);
               int var11 = var3.bb.position();

               int var12;
               for(var12 = 0; var12 < var10.getObjects().size(); ++var12) {
                  var3.putLong(((IsoObject)var10.getObjects().get(var12)).customHashCode());
               }

               var12 = var3.bb.position();
               var3.bb.position(var11 - 4);
               var3.putInt(var12);
               var3.bb.position(var12);
            }
         }

         var6 = var3.bb.position();
         var3.bb.position(var4);
         var3.putShort((short)var5);
         var3.bb.position(var6);
         PacketTypes.PacketType.SyncObjects.send(var1);
      }
   }

   public static void SyncObjectsRequest(ByteBuffer var0, UdpConnection var1) {
      short var2 = var0.getShort();
      if (var2 <= 100 && var2 > 0) {
         for(int var3 = 0; var3 < var2; ++var3) {
            int var4 = var0.getInt();
            int var5 = var0.getInt();
            byte var6 = var0.get();
            long var7 = var0.getLong();
            IsoGridSquare var9 = ServerMap.instance.getGridSquare(var4, var5, var6);
            if (var9 != null) {
               for(int var10 = 0; var10 < var9.getObjects().size(); ++var10) {
                  if (((IsoObject)var9.getObjects().get(var10)).customHashCode() == var7) {
                     ByteBufferWriter var11 = var1.startPacket();
                     PacketTypes.PacketType.SyncObjects.doPacket(var11);
                     var11.putShort((short)6);
                     var11.putInt(var4);
                     var11.putInt(var5);
                     var11.putByte((byte)var6);
                     var11.putLong(var7);
                     var11.putByte((byte)var9.getObjects().size());

                     for(int var12 = 0; var12 < var9.getObjects().size(); ++var12) {
                        var11.putLong(((IsoObject)var9.getObjects().get(var12)).customHashCode());
                     }

                     try {
                        ((IsoObject)var9.getObjects().get(var10)).writeToRemoteBuffer(var11);
                     } catch (Throwable var13) {
                        DebugLog.log("ERROR: GameServer.SyncObjectsRequest " + var13.getMessage());
                        var1.cancelPacket();
                        break;
                     }

                     PacketTypes.PacketType.SyncObjects.send(var1);
                     break;
                  }
               }
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

   static void receiveSyncThumpable(ByteBuffer var0, UdpConnection var1, short var2) {
      int var3 = var0.getInt();
      int var4 = var0.getInt();
      int var5 = var0.getInt();
      byte var6 = var0.get();
      int var7 = var0.getInt();
      byte var8 = var0.get();
      int var9 = var0.getInt();
      IsoGridSquare var10 = IsoWorld.instance.CurrentCell.getGridSquare(var3, var4, var5);
      if (var10 != null && var6 >= 0 && var6 < var10.getObjects().size()) {
         IsoObject var11 = (IsoObject)var10.getObjects().get(var6);
         if (var11 instanceof IsoThumpable) {
            IsoThumpable var12 = (IsoThumpable)var11;
            var12.lockedByCode = var7;
            var12.lockedByPadlock = var8 == 1;
            var12.keyId = var9;

            for(int var14 = 0; var14 < udpEngine.connections.size(); ++var14) {
               UdpConnection var15 = (UdpConnection)udpEngine.connections.get(var14);
               if (var15.getConnectedGUID() != var1.getConnectedGUID()) {
                  ByteBufferWriter var13 = var15.startPacket();
                  PacketTypes.PacketType.SyncThumpable.doPacket(var13);
                  var13.putInt(var3);
                  var13.putInt(var4);
                  var13.putInt(var5);
                  var13.putByte(var6);
                  var13.putInt(var7);
                  var13.putByte(var8);
                  var13.putInt(var9);
                  PacketTypes.PacketType.SyncThumpable.send(var15);
               }
            }

         } else {
            DebugLog.log("SyncThumpable: expected IsoThumpable index=" + var6 + " is invalid x,y,z=" + var3 + "," + var4 + "," + var5);
         }
      } else if (var10 != null) {
         DebugLog.log("SyncThumpable: index=" + var6 + " is invalid x,y,z=" + var3 + "," + var4 + "," + var5);
      } else {
         DebugLog.log("SyncThumpable: sq is null x,y,z=" + var3 + "," + var4 + "," + var5);
      }
   }

   static void receiveRemoveItemFromSquare(ByteBuffer var0, UdpConnection var1, short var2) {
      int var3 = var0.getInt();
      int var4 = var0.getInt();
      int var5 = var0.getInt();
      int var6 = var0.getInt();
      IsoGridSquare var7 = IsoWorld.instance.CurrentCell.getGridSquare(var3, var4, var5);
      if (var7 != null && var6 >= 0 && var6 < var7.getObjects().size()) {
         IsoObject var8 = (IsoObject)var7.getObjects().get(var6);
         if (!(var8 instanceof IsoWorldInventoryObject)) {
            IsoRegions.setPreviousFlags(var7);
         }

         DebugLog.log(DebugType.Objects, "object: removing " + var8 + " index=" + var6 + " " + var3 + "," + var4 + "," + var5);
         if (var8 instanceof IsoWorldInventoryObject) {
            LoggerManager.getLogger("item").write(var1.idStr + " \"" + var1.username + "\" floor -1 " + var3 + "," + var4 + "," + var5 + " [" + ((IsoWorldInventoryObject)var8).getItem().getFullType() + "]");
         } else {
            String var9 = var8.getName() != null ? var8.getName() : var8.getObjectName();
            if (var8.getSprite() != null && var8.getSprite().getName() != null) {
               var9 = var9 + " (" + var8.getSprite().getName() + ")";
            }

            LoggerManager.getLogger("map").write(var1.idStr + " \"" + var1.username + "\" removed " + var9 + " at " + var3 + "," + var4 + "," + var5);
         }

         int var12;
         if (var8.isTableSurface()) {
            for(var12 = var6 + 1; var12 < var7.getObjects().size(); ++var12) {
               IsoObject var10 = (IsoObject)var7.getObjects().get(var12);
               if (var10.isTableTopObject() || var10.isTableSurface()) {
                  var10.setRenderYOffset(var10.getRenderYOffset() - var8.getSurfaceOffset());
               }
            }
         }

         if (!(var8 instanceof IsoWorldInventoryObject)) {
            LuaEventManager.triggerEvent("OnObjectAboutToBeRemoved", var8);
         }

         if (!var7.getObjects().contains(var8)) {
            throw new IllegalArgumentException("OnObjectAboutToBeRemoved not allowed to remove the object");
         }

         var8.removeFromWorld();
         var8.removeFromSquare();
         var7.RecalcAllWithNeighbours(true);
         if (!(var8 instanceof IsoWorldInventoryObject)) {
            IsoWorld.instance.CurrentCell.checkHaveRoof(var3, var4);
            MapCollisionData.instance.squareChanged(var7);
            PolygonalMap2.instance.squareChanged(var7);
            ServerMap.instance.physicsCheck(var3, var4);
            IsoRegions.squareChanged(var7, true);
            IsoGenerator.updateGenerator(var7);
         }

         for(var12 = 0; var12 < udpEngine.connections.size(); ++var12) {
            UdpConnection var13 = (UdpConnection)udpEngine.connections.get(var12);
            if (var13.getConnectedGUID() != var1.getConnectedGUID()) {
               ByteBufferWriter var11 = var13.startPacket();
               PacketTypes.PacketType.RemoveItemFromSquare.doPacket(var11);
               var11.putInt(var3);
               var11.putInt(var4);
               var11.putInt(var5);
               var11.putInt(var6);
               PacketTypes.PacketType.RemoveItemFromSquare.send(var13);
            }
         }
      }

   }

   public static int RemoveItemFromMap(IsoObject var0) {
      int var1 = var0.getSquare().getX();
      int var2 = var0.getSquare().getY();
      int var3 = var0.getSquare().getZ();
      int var4 = var0.getSquare().getObjects().indexOf(var0);
      IsoGridSquare var5 = IsoWorld.instance.CurrentCell.getGridSquare(var1, var2, var3);
      if (var5 != null && !(var0 instanceof IsoWorldInventoryObject)) {
         IsoRegions.setPreviousFlags(var5);
      }

      LuaEventManager.triggerEvent("OnObjectAboutToBeRemoved", var0);
      if (!var0.getSquare().getObjects().contains(var0)) {
         throw new IllegalArgumentException("OnObjectAboutToBeRemoved not allowed to remove the object");
      } else {
         var0.removeFromWorld();
         var0.removeFromSquare();
         if (var5 != null) {
            var5.RecalcAllWithNeighbours(true);
         }

         if (!(var0 instanceof IsoWorldInventoryObject)) {
            IsoWorld.instance.CurrentCell.checkHaveRoof(var1, var2);
            MapCollisionData.instance.squareChanged(var5);
            PolygonalMap2.instance.squareChanged(var5);
            ServerMap.instance.physicsCheck(var1, var2);
            IsoRegions.squareChanged(var5, true);
            IsoGenerator.updateGenerator(var5);
         }

         for(int var6 = 0; var6 < udpEngine.connections.size(); ++var6) {
            UdpConnection var7 = (UdpConnection)udpEngine.connections.get(var6);
            if (var7.RelevantTo((float)var1, (float)var2)) {
               ByteBufferWriter var8 = var7.startPacket();
               PacketTypes.PacketType.RemoveItemFromSquare.doPacket(var8);
               var8.putInt(var1);
               var8.putInt(var2);
               var8.putInt(var3);
               var8.putInt(var4);
               PacketTypes.PacketType.RemoveItemFromSquare.send(var7);
            }
         }

         return var4;
      }
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

   static void receiveAddItemToMap(ByteBuffer var0, UdpConnection var1, short var2) {
      IsoObject var3 = WorldItemTypes.createFromBuffer(var0);
      if (var3 instanceof IsoFire && ServerOptions.instance.NoFire.getValue()) {
         DebugLog.log("user \"" + var1.username + "\" tried to start a fire");
      } else {
         var3.loadFromRemoteBuffer(var0);
         if (var3.square != null) {
            DebugLog.log(DebugType.Objects, "object: added " + var3 + " index=" + var3.getObjectIndex() + " " + var3.getX() + "," + var3.getY() + "," + var3.getZ());
            ZLogger var10000;
            String var10001;
            if (var3 instanceof IsoWorldInventoryObject) {
               var10000 = LoggerManager.getLogger("item");
               var10001 = var1.idStr;
               var10000.write(var10001 + " \"" + var1.username + "\" floor +1 " + (int)var3.getX() + "," + (int)var3.getY() + "," + (int)var3.getZ() + " [" + ((IsoWorldInventoryObject)var3).getItem().getFullType() + "]");
            } else {
               String var4 = var3.getName() != null ? var3.getName() : var3.getObjectName();
               if (var3.getSprite() != null && var3.getSprite().getName() != null) {
                  var4 = var4 + " (" + var3.getSprite().getName() + ")";
               }

               var10000 = LoggerManager.getLogger("map");
               var10001 = var1.idStr;
               var10000.write(var10001 + " \"" + var1.username + "\" added " + var4 + " at " + var3.getX() + "," + var3.getY() + "," + var3.getZ());
            }

            var3.addToWorld();
            var3.square.RecalcProperties();
            if (!(var3 instanceof IsoWorldInventoryObject)) {
               var3.square.restackSheetRope();
               IsoWorld.instance.CurrentCell.checkHaveRoof(var3.square.getX(), var3.square.getY());
               MapCollisionData.instance.squareChanged(var3.square);
               PolygonalMap2.instance.squareChanged(var3.square);
               ServerMap.instance.physicsCheck(var3.square.x, var3.square.y);
               IsoRegions.squareChanged(var3.square);
               IsoGenerator.updateGenerator(var3.square);
            }

            for(int var7 = 0; var7 < udpEngine.connections.size(); ++var7) {
               UdpConnection var5 = (UdpConnection)udpEngine.connections.get(var7);
               if (var5.getConnectedGUID() != var1.getConnectedGUID() && var5.RelevantTo((float)var3.square.x, (float)var3.square.y)) {
                  ByteBufferWriter var6 = var5.startPacket();
                  PacketTypes.PacketType.AddItemToMap.doPacket(var6);
                  var3.writeToRemoteBuffer(var6);
                  PacketTypes.PacketType.AddItemToMap.send(var5);
               }
            }

            if (!(var3 instanceof IsoWorldInventoryObject)) {
               LuaEventManager.triggerEvent("OnObjectAdded", var3);
            } else {
               ((IsoWorldInventoryObject)var3).dropTime = GameTime.getInstance().getWorldAgeHours();
            }
         } else if (bDebug) {
            DebugLog.log("AddItemToMap: sq is null");
         }

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
            if (ServerOptions.instance.AntiCheatProtectionType13.getValue() && PacketValidator.checkUser(var2)) {
               PacketValidator.doKickUser(var2, String.valueOf(var0), "Type13", (String)null);
            }
         } catch (Exception var5) {
            var5.printStackTrace();
         }

      } else {
         var3.time = System.currentTimeMillis();
         if (var3.type != PacketTypes.PacketType.PlayerUpdate && var3.type != PacketTypes.PacketType.PlayerUpdateReliable) {
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

   public static void smashWindow(IsoWindow var0, int var1) {
      for(int var2 = 0; var2 < udpEngine.connections.size(); ++var2) {
         UdpConnection var3 = (UdpConnection)udpEngine.connections.get(var2);
         if (var3.RelevantTo(var0.getX(), var0.getY())) {
            ByteBufferWriter var4 = var3.startPacket();
            PacketTypes.PacketType.SmashWindow.doPacket(var4);
            var4.putInt(var0.square.getX());
            var4.putInt(var0.square.getY());
            var4.putInt(var0.square.getZ());
            var4.putByte((byte)var0.square.getObjects().indexOf(var0));
            var4.putByte((byte)var1);
            PacketTypes.PacketType.SmashWindow.send(var3);
         }
      }

   }

   static void receiveHitCharacter(ByteBuffer var0, UdpConnection var1, short var2) {
      try {
         HitCharacterPacket var3 = HitCharacterPacket.process(var0);
         if (var3 != null) {
            var3.parse(var0, var1);
            if (var3.isConsistent() && var3.validate(var1)) {
               DebugLog.Damage.trace(var3.getDescription());
               sendHitCharacter(var3, var1);
               var3.tryProcess();
            }
         }
      } catch (Exception var4) {
         DebugLog.Multiplayer.printException(var4, "ReceiveHitCharacter: failed", LogSeverity.Error);
      }

   }

   private static void sendHitCharacter(HitCharacterPacket var0, UdpConnection var1) {
      DebugLog.Damage.trace(var0.getDescription());

      for(int var2 = 0; var2 < udpEngine.connections.size(); ++var2) {
         UdpConnection var3 = (UdpConnection)udpEngine.connections.get(var2);
         if (var3.getConnectedGUID() != var1.getConnectedGUID() && var0.isRelevant(var3)) {
            ByteBufferWriter var4 = var3.startPacket();
            PacketTypes.PacketType.HitCharacter.doPacket(var4);
            var0.write(var4);
            PacketTypes.PacketType.HitCharacter.send(var3);
         }
      }

   }

   static void receiveZombieDeath(ByteBuffer var0, UdpConnection var1, short var2) {
      try {
         DeadZombiePacket var3 = new DeadZombiePacket();
         var3.parse(var0, var1);
         if (Core.bDebug) {
            DebugLog.Multiplayer.debugln("ReceiveZombieDeath: %s", var3.getDescription());
         }

         if (var3.isConsistent()) {
            if (var3.getZombie().isReanimatedPlayer()) {
               sendZombieDeath(var3.getZombie());
            } else {
               sendZombieDeath(var3);
            }

            var3.process();
         }
      } catch (Exception var4) {
         DebugLog.Multiplayer.printException(var4, "ReceiveZombieDeath: failed", LogSeverity.Error);
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

   private static void sendZombieDeath(DeadZombiePacket var0) {
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

   static void receivePlayerDeath(ByteBuffer var0, UdpConnection var1, short var2) {
      try {
         DeadPlayerPacket var3 = new DeadPlayerPacket();
         var3.parse(var0, var1);
         if (Core.bDebug) {
            DebugLog.Multiplayer.debugln("ReceivePlayerDeath: %s", var3.getDescription());
         }

         String var4 = var3.getPlayer().username;
         ChatServer.getInstance().disconnectPlayer(var3.getPlayer().getOnlineID());
         ServerWorldDatabase.instance.saveTransactionID(var4, 0);
         var3.getPlayer().setTransactionID(0);
         transactionIDMap.put(var4, 0);
         SafetySystemManager.clearSafety(var3.getPlayer());
         if (var3.getPlayer().accessLevel.equals("") && !ServerOptions.instance.Open.getValue() && ServerOptions.instance.DropOffWhiteListAfterDeath.getValue()) {
            try {
               ServerWorldDatabase.instance.removeUser(var4);
            } catch (SQLException var6) {
               DebugLog.Multiplayer.printException(var6, "ReceivePlayerDeath: db failed", LogSeverity.Warning);
            }
         }

         if (var3.isConsistent()) {
            var3.id = var3.getPlayer().getOnlineID();
            sendPlayerDeath(var3, var1);
            var3.process();
         }

         var3.getPlayer().setStateMachineLocked(true);
      } catch (Exception var7) {
         DebugLog.Multiplayer.printException(var7, "ReceivePlayerDeath: failed", LogSeverity.Error);
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

   static void receivePlayerDamage(ByteBuffer var0, UdpConnection var1, short var2) {
      try {
         short var3 = var0.getShort();
         float var4 = var0.getFloat();
         IsoPlayer var5 = getPlayerFromConnection(var1, var3);
         if (var5 != null) {
            var5.getBodyDamage().load(var0, IsoWorld.getWorldVersion());
            var5.getStats().setPain(var4);
            if (Core.bDebug) {
               DebugLog.Multiplayer.debugln("ReceivePlayerDamage: \"%s\" %f", var5.getUsername(), var5.getBodyDamage().getOverallBodyHealth());
            }

            sendPlayerDamage(var5, var1);
         }
      } catch (Exception var6) {
         DebugLog.Multiplayer.printException(var6, "ReceivePlayerDamage: failed", LogSeverity.Error);
      }

   }

   public static void sendPlayerDamage(IsoPlayer var0, UdpConnection var1) {
      if (Core.bDebug) {
         DebugLog.Multiplayer.debugln("SendPlayerDamage: \"%s\" %f", var0.getUsername(), var0.getBodyDamage().getOverallBodyHealth());
      }

      for(int var2 = 0; var2 < udpEngine.connections.size(); ++var2) {
         UdpConnection var3 = (UdpConnection)udpEngine.connections.get(var2);
         if (var1.getConnectedGUID() != var3.getConnectedGUID()) {
            ByteBufferWriter var4 = var3.startPacket();
            PacketTypes.PacketType.PlayerDamage.doPacket(var4);

            try {
               var4.putShort(var0.getOnlineID());
               var4.putFloat(var0.getStats().getPain());
               var0.getBodyDamage().save(var4.bb);
               PacketTypes.PacketType.PlayerDamage.send(var3);
            } catch (Exception var6) {
               var3.cancelPacket();
               DebugLog.Multiplayer.printException(var6, "SendPlayerDamage: failed", LogSeverity.Error);
            }
         }
      }

   }

   static void receiveSyncInjuries(ByteBuffer var0, UdpConnection var1, short var2) {
      try {
         SyncInjuriesPacket var3 = new SyncInjuriesPacket();
         var3.parse(var0, var1);
         DebugLog.Damage.trace(var3.getDescription());
         if (var3.process()) {
            var3.id = var3.player.getOnlineID();
            sendPlayerInjuries(var1, var3);
         }
      } catch (Exception var4) {
         DebugLog.Multiplayer.printException(var4, "ReceivePlayerInjuries: failed", LogSeverity.Error);
      }

   }

   private static void sendPlayerInjuries(UdpConnection var0, SyncInjuriesPacket var1) {
      ByteBufferWriter var2 = var0.startPacket();
      PacketTypes.PacketType.SyncInjuries.doPacket(var2);
      var1.write(var2);
      PacketTypes.PacketType.SyncInjuries.send(var0);
   }

   static void receiveKeepAlive(ByteBuffer var0, UdpConnection var1, short var2) {
      MPDebugInfo.instance.serverPacket(var0, var1);
   }

   static void receiveRemoveCorpseFromMap(ByteBuffer var0, UdpConnection var1, short var2) {
      RemoveCorpseFromMap var3 = new RemoveCorpseFromMap();
      var3.parse(var0, var1);
      if (var3.isConsistent()) {
         var3.process();

         for(int var4 = 0; var4 < udpEngine.connections.size(); ++var4) {
            UdpConnection var5 = (UdpConnection)udpEngine.connections.get(var4);
            if (var5.getConnectedGUID() != var1.getConnectedGUID() && var3.isRelevant(var5)) {
               ByteBufferWriter var6 = var5.startPacket();
               PacketTypes.PacketType.RemoveCorpseFromMap.doPacket(var6);
               var3.write(var6);
               PacketTypes.PacketType.RemoveCorpseFromMap.send(var5);
            }
         }

      }
   }

   public static void sendRemoveCorpseFromMap(IsoDeadBody var0) {
      RemoveCorpseFromMap var1 = new RemoveCorpseFromMap();
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

   static void receiveEventPacket(ByteBuffer var0, UdpConnection var1, short var2) {
      try {
         EventPacket var3 = new EventPacket();
         var3.parse(var0, var1);
         Iterator var4 = udpEngine.connections.iterator();

         while(var4.hasNext()) {
            UdpConnection var5 = (UdpConnection)var4.next();
            if (var5.getConnectedGUID() != var1.getConnectedGUID() && var3.isRelevant(var5)) {
               ByteBufferWriter var6 = var5.startPacket();
               PacketTypes.PacketType.EventPacket.doPacket(var6);
               var3.write(var6);
               PacketTypes.PacketType.EventPacket.send(var5);
            }
         }
      } catch (Exception var7) {
         DebugLog.Multiplayer.printException(var7, "ReceiveEvent: failed", LogSeverity.Error);
      }

   }

   static void receiveActionPacket(ByteBuffer var0, UdpConnection var1, short var2) {
      try {
         ActionPacket var3 = new ActionPacket();
         var3.parse(var0, var1);
         Iterator var4 = udpEngine.connections.iterator();

         while(var4.hasNext()) {
            UdpConnection var5 = (UdpConnection)var4.next();
            if (var5.getConnectedGUID() != var1.getConnectedGUID() && var3.isRelevant(var5)) {
               ByteBufferWriter var6 = var5.startPacket();
               PacketTypes.PacketType.ActionPacket.doPacket(var6);
               var3.write(var6);
               PacketTypes.PacketType.ActionPacket.send(var5);
            }
         }
      } catch (Exception var7) {
         DebugLog.Multiplayer.printException(var7, "ReceiveAction: failed", LogSeverity.Error);
      }

   }

   static void receiveKillZombie(ByteBuffer var0, UdpConnection var1, short var2) {
      try {
         short var3 = var0.getShort();
         boolean var4 = var0.get() != 0;
         DebugLog.Death.trace("id=%d, isFallOnFront=%b", var3, var4);
         IsoZombie var5 = (IsoZombie)ServerMap.instance.ZombieMap.get(var3);
         if (var5 != null) {
            var5.setFallOnFront(var4);
            var5.becomeCorpse();
         } else {
            DebugLog.Multiplayer.error("ReceiveKillZombie: zombie not found");
         }
      } catch (Exception var6) {
         DebugLog.Multiplayer.printException(var6, "ReceiveKillZombie: failed", LogSeverity.Error);
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
            if (var6.RelevantTo(var4.x, var4.y)) {
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
            if (var6.RelevantTo(var4.x, var4.y)) {
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

   public static void sendWorldSound(UdpConnection var0, WorldSoundManager.WorldSound var1) {
      ByteBufferWriter var2 = var0.startPacket();
      PacketTypes.PacketType.WorldSound.doPacket(var2);

      try {
         var2.putInt(var1.x);
         var2.putInt(var1.y);
         var2.putInt(var1.z);
         var2.putInt(var1.radius);
         var2.putInt(var1.volume);
         var2.putByte((byte)(var1.stresshumans ? 1 : 0));
         var2.putFloat(var1.zombieIgnoreDist);
         var2.putFloat(var1.stressMod);
         var2.putByte((byte)(var1.sourceIsZombie ? 1 : 0));
         PacketTypes.PacketType.WorldSound.send(var0);
      } catch (Exception var4) {
         DebugLog.Sound.printException(var4, "SendWorldSound: failed", LogSeverity.Error);
         var0.cancelPacket();
      }

   }

   public static void sendWorldSound(WorldSoundManager.WorldSound var0, UdpConnection var1) {
      for(int var2 = 0; var2 < udpEngine.connections.size(); ++var2) {
         UdpConnection var3 = (UdpConnection)udpEngine.connections.get(var2);
         if ((var1 == null || var1.getConnectedGUID() != var3.getConnectedGUID()) && var3.isFullyConnected()) {
            IsoPlayer var4 = getAnyPlayerFromConnection(var3);
            if (var4 != null && var3.RelevantTo((float)var0.x, (float)var0.y, (float)var0.radius)) {
               sendWorldSound(var3, var0);
            }
         }
      }

   }

   static void receiveWorldSound(ByteBuffer var0, UdpConnection var1, short var2) {
      int var3 = var0.getInt();
      int var4 = var0.getInt();
      int var5 = var0.getInt();
      int var6 = var0.getInt();
      int var7 = var0.getInt();
      boolean var8 = var0.get() == 1;
      float var9 = var0.getFloat();
      float var10 = var0.getFloat();
      boolean var11 = var0.get() == 1;
      DebugLog.Sound.noise("x=%d y=%d z=%d, radius=%d", var3, var4, var5, var6);
      WorldSoundManager.WorldSound var12 = WorldSoundManager.instance.addSound((Object)null, var3, var4, var5, var6, var7, var8, var9, var10, var11, false, true);
      if (var12 != null) {
         sendWorldSound(var12, var1);
      }

   }

   public static void kick(UdpConnection var0, String var1, String var2) {
      DebugLog.General.warn("The player " + var0.username + " was kicked. The reason was " + var1 + ", " + var2);
      ConnectionManager.log("kick", var2, var0);
      ByteBufferWriter var3 = var0.startPacket();

      try {
         PacketTypes.PacketType.Kicked.doPacket(var3);
         var3.putUTF(var1);
         var3.putUTF(var2);
         PacketTypes.PacketType.Kicked.send(var0);
      } catch (Exception var5) {
         DebugLog.Multiplayer.printException(var5, "Kick: failed", LogSeverity.Error);
         var0.cancelPacket();
      }

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
      GameTime var1 = GameTime.getInstance();
      ByteBufferWriter var2 = var0.startPacket();
      PacketTypes.PacketType.Weather.doPacket(var2);
      var2.putByte((byte)var1.getDawn());
      var2.putByte((byte)var1.getDusk());
      var2.putByte((byte)(var1.isThunderDay() ? 1 : 0));
      var2.putFloat(var1.Moon);
      var2.putFloat(var1.getAmbientMin());
      var2.putFloat(var1.getAmbientMax());
      var2.putFloat(var1.getViewDistMin());
      var2.putFloat(var1.getViewDistMax());
      var2.putFloat(IsoWorld.instance.getGlobalTemperature());
      var2.putUTF(IsoWorld.instance.getWeather());
      ErosionMain.getInstance().sendState(var2.bb);
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

   private static boolean isInSameSafehouse(IsoPlayer var0, IsoPlayer var1) {
      ArrayList var2 = SafeHouse.getSafehouseList();

      for(int var3 = 0; var3 < var2.size(); ++var3) {
         SafeHouse var4 = (SafeHouse)var2.get(var3);
         if (var4.playerAllowed(var0.getUsername()) && var4.playerAllowed(var1.getUsername())) {
            return true;
         }
      }

      return false;
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
         if (var3 != null && isInSameSafehouse(var3, var1)) {
            return true;
         }
      }

      return false;
   }

   private static boolean shouldSendWorldMapPlayerPosition(UdpConnection var0, IsoPlayer var1) {
      if (var1 != null && !var1.isDead()) {
         UdpConnection var2 = getConnectionFromPlayer(var1);
         if (var2 != null && var2 != var0 && var2.isFullyConnected()) {
            if (var0.accessLevel > 1) {
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
         if (var0 != 1 || var2.accessLevel != 1) {
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
         }

         PacketTypes.PacketType.WorldMapPlayerPosition.send(var1);
      }
   }

   private static void syncClock(UdpConnection var0) {
      GameTime var1 = GameTime.getInstance();
      ByteBufferWriter var2 = var0.startPacket();
      PacketTypes.PacketType.SyncClock.doPacket(var2);
      var2.putBoolean(bFastForward);
      var2.putFloat(var1.getTimeOfDay());
      var2.putInt(var1.getNightsSurvived());
      PacketTypes.PacketType.SyncClock.send(var0);
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

         for(int var3 = 0; var3 < 4; ++var3) {
            if (var2.playerIDs[var3] != -1) {
               ++var0;
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

   static void receiveChangeSafety(ByteBuffer var0, UdpConnection var1, short var2) {
      try {
         SafetyPacket var3 = new SafetyPacket();
         var3.parse(var0, var1);
         var3.log(var1, "ReceiveChangeSafety");
         var3.process();
      } catch (Exception var4) {
         DebugLog.Multiplayer.printException(var4, "ReceiveZombieDeath: failed", LogSeverity.Error);
      }

   }

   public static void sendChangeSafety(Safety var0) {
      try {
         SafetyPacket var1 = new SafetyPacket(var0);
         var1.log((UdpConnection)null, "SendChangeSafety");
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
         for(int var7 = 0; var7 < udpEngine.connections.size(); ++var7) {
            UdpConnection var8 = (UdpConnection)udpEngine.connections.get(var7);
            if (var8 != null && var0.square != null && var8.RelevantTo((float)var0.square.x, (float)var0.square.y) && (var6 == null || var8.getConnectedGUID() != var6.getConnectedGUID())) {
               ByteBufferWriter var9 = var8.startPacket();
               PacketTypes.PacketType.UpdateOverlaySprite.doPacket(var9);
               GameWindow.WriteStringUTF(var9.bb, var1);
               var9.putInt(var0.getSquare().getX());
               var9.putInt(var0.getSquare().getY());
               var9.putInt(var0.getSquare().getZ());
               var9.putFloat(var2);
               var9.putFloat(var3);
               var9.putFloat(var4);
               var9.putFloat(var5);
               var9.putInt(var0.getSquare().getObjects().indexOf(var0));
               PacketTypes.PacketType.UpdateOverlaySprite.send(var8);
            }
         }

      }
   }

   static void receiveUpdateOverlaySprite(ByteBuffer var0, UdpConnection var1, short var2) {
      String var3 = GameWindow.ReadStringUTF(var0);
      int var4 = var0.getInt();
      int var5 = var0.getInt();
      int var6 = var0.getInt();
      float var7 = var0.getFloat();
      float var8 = var0.getFloat();
      float var9 = var0.getFloat();
      float var10 = var0.getFloat();
      int var11 = var0.getInt();
      IsoGridSquare var12 = IsoWorld.instance.CurrentCell.getGridSquare(var4, var5, var6);
      if (var12 != null && var11 < var12.getObjects().size()) {
         try {
            IsoObject var13 = (IsoObject)var12.getObjects().get(var11);
            if (var13 != null && var13.setOverlaySprite(var3, var7, var8, var9, var10, false)) {
               updateOverlayForClients(var13, var3, var7, var8, var9, var10, var1);
            }
         } catch (Exception var14) {
         }
      }

   }

   public static void sendReanimatedZombieID(IsoPlayer var0, IsoZombie var1) {
      if (PlayerToAddressMap.containsKey(var0)) {
         sendObjectChange(var0, "reanimatedID", (Object[])("ID", (double)var1.OnlineID));
      }

   }

   static void receiveSyncSafehouse(ByteBuffer var0, UdpConnection var1, short var2) {
      SyncSafehousePacket var3 = new SyncSafehousePacket();
      var3.parse(var0, var1);
      if (var3.validate(var1)) {
         var3.process();
         sendSafehouse(var3, var1);
         if (ChatServer.isInited()) {
            if (var3.shouldCreateChat) {
               ChatServer.getInstance().createSafehouseChat(var3.safehouse.getId());
            }

            if (var3.remove) {
               ChatServer.getInstance().removeSafehouseChat(var3.safehouse.getId());
            } else {
               ChatServer.getInstance().syncSafehouseChatMembers(var3.safehouse.getId(), var3.ownerUsername, var3.safehouse.getPlayers());
            }
         }

      }
   }

   public static void receiveKickOutOfSafehouse(ByteBuffer var0, UdpConnection var1, short var2) {
      try {
         IsoPlayer var3 = (IsoPlayer)IDToPlayerMap.get(var0.getShort());
         if (var3 == null) {
            return;
         }

         IsoPlayer var4 = var1.players[0];
         if (var4 == null) {
            return;
         }

         SafeHouse var5 = SafeHouse.hasSafehouse(var4);
         if (var5 == null) {
            return;
         }

         if (!var5.isOwner(var4)) {
            return;
         }

         if (!var5.playerAllowed(var3)) {
            return;
         }

         UdpConnection var6 = getConnectionFromPlayer(var3);
         if (var6 == null) {
            return;
         }

         ByteBufferWriter var7 = var6.startPacket();
         PacketTypes.PacketType.KickOutOfSafehouse.doPacket(var7);
         var7.putByte((byte)var3.PlayerIndex);
         var7.putFloat((float)(var5.getX() - 1));
         var7.putFloat((float)(var5.getY() - 1));
         var7.putFloat(0.0F);
         PacketTypes.PacketType.KickOutOfSafehouse.send(var6);
         if (var3.getNetworkCharacterAI() != null) {
            var3.getNetworkCharacterAI().resetSpeedLimiter();
         }

         if (var3.isAsleep()) {
            var3.setAsleep(false);
            var3.setAsleepTime(0.0F);
            sendWakeUpPlayer(var3, (UdpConnection)null);
         }
      } catch (Exception var8) {
         DebugLog.Multiplayer.printException(var8, "ReceiveKickOutOfSafehouse: failed", LogSeverity.Error);
      }

   }

   public static void sendSafehouse(SyncSafehousePacket var0, UdpConnection var1) {
      for(int var2 = 0; var2 < udpEngine.connections.size(); ++var2) {
         UdpConnection var3 = (UdpConnection)udpEngine.connections.get(var2);
         if (var1 == null || var3.getConnectedGUID() != var1.getConnectedGUID()) {
            ByteBufferWriter var4 = var3.startPacket();
            PacketTypes.PacketType.SyncSafehouse.doPacket(var4);
            var0.write(var4);
            PacketTypes.PacketType.SyncSafehouse.send(var3);
         }
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
      WaveSignal var13 = new WaveSignal();
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

   public static void receiveWaveSignal(ByteBuffer var0, UdpConnection var1, short var2) {
      WaveSignal var3 = new WaveSignal();
      var3.parse(var0, var1);
      var3.process(var1);
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
         syncClock();
      }
   }

   static void receiveSendCustomColor(ByteBuffer var0, UdpConnection var1, short var2) {
      int var3 = var0.getInt();
      int var4 = var0.getInt();
      int var5 = var0.getInt();
      int var6 = var0.getInt();
      float var7 = var0.getFloat();
      float var8 = var0.getFloat();
      float var9 = var0.getFloat();
      float var10 = var0.getFloat();
      IsoGridSquare var11 = IsoWorld.instance.CurrentCell.getGridSquare(var3, var4, var5);
      if (var11 != null && var6 < var11.getObjects().size()) {
         IsoObject var12 = (IsoObject)var11.getObjects().get(var6);
         if (var12 != null) {
            var12.setCustomColor(var7, var8, var9, var10);
         }
      }

      for(int var15 = 0; var15 < udpEngine.connections.size(); ++var15) {
         UdpConnection var13 = (UdpConnection)udpEngine.connections.get(var15);
         if (var13.RelevantTo((float)var3, (float)var4) && (var1 != null && var13.getConnectedGUID() != var1.getConnectedGUID() || var1 == null)) {
            ByteBufferWriter var14 = var13.startPacket();
            PacketTypes.PacketType.SendCustomColor.doPacket(var14);
            var14.putInt(var3);
            var14.putInt(var4);
            var14.putInt(var5);
            var14.putInt(var6);
            var14.putFloat(var7);
            var14.putFloat(var8);
            var14.putFloat(var9);
            var14.putFloat(var10);
            PacketTypes.PacketType.SendCustomColor.send(var13);
         }
      }

   }

   static void receiveSyncFurnace(ByteBuffer var0, UdpConnection var1, short var2) {
      int var3 = var0.getInt();
      int var4 = var0.getInt();
      int var5 = var0.getInt();
      IsoGridSquare var6 = IsoWorld.instance.CurrentCell.getGridSquare(var3, var4, var5);
      if (var6 == null) {
         DebugLog.log("receiveFurnaceChange: square is null x,y,z=" + var3 + "," + var4 + "," + var5);
      } else {
         BSFurnace var7 = null;

         for(int var8 = 0; var8 < var6.getObjects().size(); ++var8) {
            if (var6.getObjects().get(var8) instanceof BSFurnace) {
               var7 = (BSFurnace)var6.getObjects().get(var8);
               break;
            }
         }

         if (var7 == null) {
            DebugLog.log("receiveFurnaceChange: furnace is null x,y,z=" + var3 + "," + var4 + "," + var5);
         } else {
            var7.fireStarted = var0.get() == 1;
            var7.fuelAmount = var0.getFloat();
            var7.fuelDecrease = var0.getFloat();
            var7.heat = var0.getFloat();
            var7.sSprite = GameWindow.ReadString(var0);
            var7.sLitSprite = GameWindow.ReadString(var0);
            sendFuranceChange(var7, var1);
         }
      }
   }

   static void receiveVehicles(ByteBuffer var0, UdpConnection var1, short var2) {
      VehicleManager.instance.serverPacket(var0, var1, var2);
   }

   static void receiveTimeSync(ByteBuffer var0, UdpConnection var1, short var2) {
      GameTime.receiveTimeSync(var0, var1);
   }

   public static void sendFuranceChange(BSFurnace var0, UdpConnection var1) {
      for(int var2 = 0; var2 < udpEngine.connections.size(); ++var2) {
         UdpConnection var3 = (UdpConnection)udpEngine.connections.get(var2);
         if (var3.RelevantTo((float)var0.square.x, (float)var0.square.y) && (var1 != null && var3.getConnectedGUID() != var1.getConnectedGUID() || var1 == null)) {
            ByteBufferWriter var4 = var3.startPacket();
            PacketTypes.PacketType.SyncFurnace.doPacket(var4);
            var4.putInt(var0.square.x);
            var4.putInt(var0.square.y);
            var4.putInt(var0.square.z);
            var4.putByte((byte)(var0.isFireStarted() ? 1 : 0));
            var4.putFloat(var0.getFuelAmount());
            var4.putFloat(var0.getFuelDecrease());
            var4.putFloat(var0.getHeat());
            GameWindow.WriteString(var4.bb, var0.sSprite);
            GameWindow.WriteString(var4.bb, var0.sLitSprite);
            PacketTypes.PacketType.SyncFurnace.send(var3);
         }
      }

   }

   static void receiveUserlog(ByteBuffer var0, UdpConnection var1, short var2) {
      String var3 = GameWindow.ReadString(var0);
      ArrayList var4 = ServerWorldDatabase.instance.getUserlog(var3);

      for(int var5 = 0; var5 < udpEngine.connections.size(); ++var5) {
         UdpConnection var6 = (UdpConnection)udpEngine.connections.get(var5);
         if (var6.getConnectedGUID() == var1.getConnectedGUID()) {
            ByteBufferWriter var7 = var6.startPacket();
            PacketTypes.PacketType.Userlog.doPacket(var7);
            var7.putInt(var4.size());
            var7.putUTF(var3);

            for(int var8 = 0; var8 < var4.size(); ++var8) {
               Userlog var9 = (Userlog)var4.get(var8);
               var7.putInt(Userlog.UserlogType.FromString(var9.getType()).index());
               var7.putUTF(var9.getText());
               var7.putUTF(var9.getIssuedBy());
               var7.putInt(var9.getAmount());
               var7.putUTF(var9.getLastUpdate());
            }

            PacketTypes.PacketType.Userlog.send(var6);
         }
      }

   }

   static void receiveAddUserlog(ByteBuffer var0, UdpConnection var1, short var2) throws SQLException {
      String var3 = GameWindow.ReadString(var0);
      String var4 = GameWindow.ReadString(var0);
      String var5 = GameWindow.ReadString(var0);
      ServerWorldDatabase.instance.addUserlog(var3, Userlog.UserlogType.FromString(var4), var5, var1.username, 1);
      LoggerManager.getLogger("admin").write(var1.username + " added log on user " + var3 + ", log: " + var5);
   }

   static void receiveRemoveUserlog(ByteBuffer var0, UdpConnection var1, short var2) throws SQLException {
      String var3 = GameWindow.ReadString(var0);
      String var4 = GameWindow.ReadString(var0);
      String var5 = GameWindow.ReadString(var0);
      ServerWorldDatabase.instance.removeUserLog(var3, var4, var5);
      LoggerManager.getLogger("admin").write(var1.username + " removed log on user " + var3 + ", type:" + var4 + ", log: " + var5);
   }

   static void receiveAddWarningPoint(ByteBuffer var0, UdpConnection var1, short var2) throws SQLException {
      String var3 = GameWindow.ReadString(var0);
      String var4 = GameWindow.ReadString(var0);
      int var5 = var0.getInt();
      ServerWorldDatabase.instance.addWarningPoint(var3, var4, var5, var1.username);
      LoggerManager.getLogger("admin").write(var1.username + " added " + var5 + " warning point(s) on " + var3 + ", reason:" + var4);

      for(int var6 = 0; var6 < udpEngine.connections.size(); ++var6) {
         UdpConnection var7 = (UdpConnection)udpEngine.connections.get(var6);
         if (var7.username.equals(var3)) {
            ByteBufferWriter var8 = var7.startPacket();
            PacketTypes.PacketType.WorldMessage.doPacket(var8);
            var8.putUTF(var1.username);
            var8.putUTF(" gave you " + var5 + " warning point(s), reason: " + var4 + " ");
            PacketTypes.PacketType.WorldMessage.send(var7);
         }
      }

   }

   public static void sendAdminMessage(String var0, int var1, int var2, int var3) {
      for(int var4 = 0; var4 < udpEngine.connections.size(); ++var4) {
         UdpConnection var5 = (UdpConnection)udpEngine.connections.get(var4);
         if (canSeePlayerStats(var5)) {
            ByteBufferWriter var6 = var5.startPacket();
            PacketTypes.PacketType.MessageForAdmin.doPacket(var6);
            var6.putUTF(var0);
            var6.putInt(var1);
            var6.putInt(var2);
            var6.putInt(var3);
            PacketTypes.PacketType.MessageForAdmin.send(var5);
         }
      }

   }

   static void receiveWakeUpPlayer(ByteBuffer var0, UdpConnection var1, short var2) {
      IsoPlayer var3 = getPlayerFromConnection(var1, var0.getShort());
      if (var3 != null) {
         var3.setAsleep(false);
         var3.setAsleepTime(0.0F);
         sendWakeUpPlayer(var3, var1);
      }

   }

   public static void sendWakeUpPlayer(IsoPlayer var0, UdpConnection var1) {
      for(int var2 = 0; var2 < udpEngine.connections.size(); ++var2) {
         UdpConnection var3 = (UdpConnection)udpEngine.connections.get(var2);
         if (var1 == null || var3.getConnectedGUID() != var1.getConnectedGUID()) {
            ByteBufferWriter var4 = var3.startPacket();
            PacketTypes.PacketType.WakeUpPlayer.doPacket(var4);
            var4.putShort(var0.getOnlineID());
            PacketTypes.PacketType.WakeUpPlayer.send(var3);
         }
      }

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
      if (var1.accessLevel == 32) {
         try {
            String var3 = GameWindow.ReadString(var0);
            KahluaTable var4 = LuaManager.platform.newTable();
            var4.load(var0, 195);
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
         CompressIdenticalItems.load(var0, 195, var7, (ArrayList)null);
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
                                    if (var7 <= 195) {
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
      for(int var1 = 0; var1 < udpEngine.connections.size(); ++var1) {
         UdpConnection var2 = (UdpConnection)udpEngine.connections.get(var1);

         try {
            if (var2.RelevantTo((float)var0.getX(), (float)var0.getY())) {
               ByteBufferWriter var3 = var2.startPacket();
               PacketTypes.PacketType.AddBrokenGlass.doPacket(var3);
               var3.putInt((short)var0.getX());
               var3.putInt((short)var0.getY());
               var3.putInt((short)var0.getZ());
               PacketTypes.PacketType.AddBrokenGlass.send(var2);
            }
         } catch (Throwable var4) {
            var2.cancelPacket();
            ExceptionLogger.logException(var4);
         }
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

   static void receiveGlobalModData(ByteBuffer var0, UdpConnection var1, short var2) {
      GlobalModData.instance.receive(var0);
   }

   static void receiveGlobalModDataRequest(ByteBuffer var0, UdpConnection var1, short var2) {
      GlobalModData.instance.receiveRequest(var0, var1);
   }

   static void receiveSendSafehouseInvite(ByteBuffer var0, UdpConnection var1, short var2) {
      String var3 = GameWindow.ReadString(var0);
      String var4 = GameWindow.ReadString(var0);
      String var5 = GameWindow.ReadString(var0);
      IsoPlayer var6 = getPlayerByUserName(var5);
      Long var7 = (Long)IDToAddressMap.get(var6.getOnlineID());
      int var8 = var0.getInt();
      int var9 = var0.getInt();
      int var10 = var0.getInt();
      int var11 = var0.getInt();

      for(int var12 = 0; var12 < udpEngine.connections.size(); ++var12) {
         UdpConnection var13 = (UdpConnection)udpEngine.connections.get(var12);
         if (var13.getConnectedGUID() == var7) {
            ByteBufferWriter var14 = var13.startPacket();
            PacketTypes.PacketType.SendSafehouseInvite.doPacket(var14);
            var14.putUTF(var3);
            var14.putUTF(var4);
            var14.putInt(var8);
            var14.putInt(var9);
            var14.putInt(var10);
            var14.putInt(var11);
            PacketTypes.PacketType.SendSafehouseInvite.send(var13);
            break;
         }
      }

   }

   static void receiveAcceptedSafehouseInvite(ByteBuffer var0, UdpConnection var1, short var2) {
      String var3 = GameWindow.ReadString(var0);
      String var4 = GameWindow.ReadString(var0);
      String var5 = GameWindow.ReadString(var0);
      int var6 = var0.getInt();
      int var7 = var0.getInt();
      int var8 = var0.getInt();
      int var9 = var0.getInt();
      SafeHouse var10 = SafeHouse.getSafeHouse(var6, var7, var8, var9);
      if (var10 != null) {
         var10.addPlayer(var5);
      } else {
         DebugLog.log("WARN: player '" + var5 + "' accepted the invitation, but the safehouse not found for x=" + var6 + " y=" + var7 + " w=" + var8 + " h=" + var9);
      }

      for(int var11 = 0; var11 < udpEngine.connections.size(); ++var11) {
         UdpConnection var12 = (UdpConnection)udpEngine.connections.get(var11);
         ByteBufferWriter var13 = var12.startPacket();
         PacketTypes.PacketType.AcceptedSafehouseInvite.doPacket(var13);
         var13.putUTF(var3);
         var13.putUTF(var4);
         var13.putUTF(var5);
         var13.putInt(var6);
         var13.putInt(var7);
         var13.putInt(var8);
         var13.putInt(var9);
         PacketTypes.PacketType.AcceptedSafehouseInvite.send(var12);
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

   static void receiveSneezeCough(ByteBuffer var0, UdpConnection var1, short var2) {
      short var3 = var0.getShort();
      byte var4 = var0.get();
      IsoPlayer var5 = (IsoPlayer)IDToPlayerMap.get(var3);
      if (var5 != null) {
         float var6 = var5.x;
         float var7 = var5.y;
         int var8 = 0;

         for(int var9 = udpEngine.connections.size(); var8 < var9; ++var8) {
            UdpConnection var10 = (UdpConnection)udpEngine.connections.get(var8);
            if (var1.getConnectedGUID() != var10.getConnectedGUID() && var10.RelevantTo(var6, var7)) {
               ByteBufferWriter var11 = var10.startPacket();
               PacketTypes.PacketType.SneezeCough.doPacket(var11);
               var11.putShort(var3);
               var11.putByte(var4);
               PacketTypes.PacketType.SneezeCough.send(var10);
            }
         }
      }

   }

   static void receiveBurnCorpse(ByteBuffer var0, UdpConnection var1, short var2) {
      short var3 = var0.getShort();
      short var4 = var0.getShort();
      IsoPlayer var5 = (IsoPlayer)IDToPlayerMap.get(var3);
      if (var5 == null) {
         DebugLog.Network.warn("Player not found by id " + var3);
      } else {
         IsoDeadBody var6 = IsoDeadBody.getDeadBody(var4);
         if (var6 == null) {
            DebugLog.Network.warn("Corpse not found by id " + var4);
         } else {
            float var7 = IsoUtils.DistanceTo(var5.x, var5.y, var6.x, var6.y);
            if (var7 <= 1.8F) {
               IsoFireManager.StartFire(var6.getCell(), var6.getSquare(), true, 100);
            } else {
               DebugLog.Network.warn("Distance between player and corpse too big: " + var7);
            }

         }
      }
   }

   public static void sendValidatePacket(UdpConnection var0, boolean var1, boolean var2, boolean var3) {
      ByteBufferWriter var4 = var0.startPacket();

      try {
         ValidatePacket var5 = new ValidatePacket();
         var5.setSalt(var0.validator.getSalt(), var1, var2, var3);
         PacketTypes.PacketType.Validate.doPacket(var4);
         var5.write(var4);
         PacketTypes.PacketType.Validate.send(var0);
         var5.log(GameClient.connection, "send-packet");
      } catch (Exception var6) {
         var0.cancelPacket();
         DebugLog.Multiplayer.printException(var6, "SendValidatePacket: failed", LogSeverity.Error);
      }

   }

   static void receiveValidatePacket(ByteBuffer var0, UdpConnection var1, short var2) {
      ValidatePacket var3 = new ValidatePacket();
      var3.parse(var0, var1);
      var3.log(GameClient.connection, "receive-packet");
      if (var3.isConsistent()) {
         var3.process(var1);
      }

   }

   static {
      discordBot = new DiscordBot(ServerName, (var0, var1) -> {
         ChatServer.getInstance().sendMessageFromDiscordToGeneralChat(var0, var1);
      });
      checksum = "";
      GameMap = "Muldraugh, KY";
      transactionIDMap = new HashMap();
      worldObjectsServerSyncReq = new ObjectsSyncRequests(false);
      ip = "127.0.0.1";
      count = 0;
      SlotToConnection = new UdpConnection[512];
      PlayerToAddressMap = new HashMap();
      alreadyRemoved = new ArrayList();
      launched = false;
      consoleCommands = new ArrayList();
      MainLoopPlayerUpdate = new HashMap();
      MainLoopPlayerUpdateQ = new ConcurrentLinkedQueue();
      MainLoopNetDataHighPriorityQ = new ConcurrentLinkedQueue();
      MainLoopNetDataQ = new ConcurrentLinkedQueue();
      MainLoopNetData2 = new ArrayList();
      playerToCoordsMap = new HashMap();
      playerMovedToFastMap = new HashMap();
      large_file_bb = ByteBuffer.allocate(2097152);
      previousSave = Calendar.getInstance().getTimeInMillis();
      droppedPackets = 0;
      countOfDroppedPackets = 0;
      countOfDroppedConnections = 0;
      removeZombiesConnection = null;
      calcCountPlayersInRelevantPositionLimiter = new UpdateLimit(2000L);
      sendWorldMapPlayerPositionLimiter = new UpdateLimit(1000L);
      loginQueue = new LoginQueue();
      mainCycleExceptionLogCount = 25;
      tempPlayers = new ArrayList();
   }

   private static class DelayedConnection implements IZomboidPacket {
      public UdpConnection connection;
      public boolean connect;
      public String hostString;

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

      }

      public boolean isConnect() {
         return this.connect;
      }

      public boolean isDisconnect() {
         return !this.connect;
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
