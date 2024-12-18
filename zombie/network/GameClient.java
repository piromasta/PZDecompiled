package zombie.network;

import fmod.javafmod;
import fmod.fmod.FMODManager;
import fmod.fmod.FMOD_STUDIO_EVENT_DESCRIPTION;
import gnu.trove.list.array.TShortArrayList;
import gnu.trove.map.hash.TShortObjectHashMap;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.apache.commons.codec.binary.Base32;
import org.joml.Vector3f;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.AmbientStreamManager;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.SandboxOptions;
import zombie.SharedDescriptors;
import zombie.SystemDisabler;
import zombie.VirtualZombieManager;
import zombie.WorldSoundManager;
import zombie.ZomboidFileSystem;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.characters.Capability;
import zombie.characters.Faction;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.Safety;
import zombie.characters.CharacterTimedActions.BaseAction;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.skills.PerkFactory;
import zombie.chat.ChatManager;
import zombie.commands.serverCommands.ListCommand;
import zombie.commands.serverCommands.LogCommand;
import zombie.core.Core;
import zombie.core.ThreadGroups;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.core.raknet.UdpEngine;
import zombie.core.raknet.VoiceManager;
import zombie.core.raknet.VoiceManagerData;
import zombie.core.random.Rand;
import zombie.core.textures.ColorInfo;
import zombie.core.utils.UpdateLimit;
import zombie.core.znet.SteamFriends;
import zombie.core.znet.SteamUtils;
import zombie.core.znet.ZNet;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.erosion.ErosionConfig;
import zombie.inventory.CompressIdenticalItems;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.Food;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.InventoryContainer;
import zombie.inventory.types.Radio;
import zombie.iso.FishSchoolManager;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;
import zombie.iso.WorldStreamer;
import zombie.iso.areas.NonPvpZone;
import zombie.iso.areas.SafeHouse;
import zombie.iso.areas.isoregion.IsoRegions;
import zombie.iso.objects.IsoCompost;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoWaveSignal;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.iso.objects.IsoZombieGiblets;
import zombie.iso.objects.RainManager;
import zombie.iso.objects.interfaces.Thumpable;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.weather.ClimateManager;
import zombie.iso.zones.Zone;
import zombie.network.packets.INetworkPacket;
import zombie.network.packets.KickedPacket;
import zombie.network.packets.RequestDataPacket;
import zombie.network.packets.SafetyPacket;
import zombie.network.packets.SledgehammerDestroyPacket;
import zombie.network.packets.SyncInjuriesPacket;
import zombie.network.packets.WaveSignalPacket;
import zombie.network.packets.WeatherPacket;
import zombie.network.packets.actions.EatFoodPacket;
import zombie.network.packets.actions.EventPacket;
import zombie.network.packets.actions.SmashWindowPacket;
import zombie.network.packets.actions.SneezeCoughPacket;
import zombie.network.packets.character.DeadAnimalPacket;
import zombie.network.packets.character.DeadPlayerPacket;
import zombie.network.packets.character.DeadZombiePacket;
import zombie.network.packets.character.PlayerPacket;
import zombie.network.packets.character.RemoveCorpseFromMapPacket;
import zombie.network.packets.connection.LoadPlayerProfilePacket;
import zombie.network.packets.hit.AnimalHitAnimalPacket;
import zombie.network.packets.hit.AnimalHitPlayerPacket;
import zombie.network.packets.hit.AnimalHitThumpablePacket;
import zombie.network.packets.hit.PlayerHitAnimalPacket;
import zombie.network.packets.hit.PlayerHitObjectPacket;
import zombie.network.packets.hit.PlayerHitPlayerPacket;
import zombie.network.packets.hit.PlayerHitSquarePacket;
import zombie.network.packets.hit.PlayerHitVehiclePacket;
import zombie.network.packets.hit.PlayerHitZombiePacket;
import zombie.network.packets.hit.VehicleHitPlayerPacket;
import zombie.network.packets.hit.VehicleHitZombiePacket;
import zombie.network.packets.hit.ZombieHitPlayerPacket;
import zombie.network.packets.sound.PlayWorldSoundPacket;
import zombie.network.packets.sound.StopSoundPacket;
import zombie.popman.NetworkZombieSimulator;
import zombie.popman.ZombieCountOptimiser;
import zombie.radio.ZomboidRadio;
import zombie.radio.devices.DeviceData;
import zombie.util.AddCoopPlayer;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehicleInterpolationData;
import zombie.vehicles.VehicleManager;
import zombie.vehicles.VehiclePart;
import zombie.worldMap.WorldMapRemotePlayer;
import zombie.worldMap.WorldMapRemotePlayers;
import zombie.worldMap.network.WorldMapClient;

public class GameClient {
   public static final GameClient instance = new GameClient();
   public static final int DEFAULT_PORT = 16361;
   public static boolean bClient = false;
   public static UdpConnection connection;
   public static int count = 0;
   public static String ip = "localhost";
   public static String ServerName = "";
   public static String localIP = "";
   public static String password = "testpass";
   public static String googleKey = "";
   public static boolean allChatMuted = false;
   public static String username = "lemmy101";
   public static String serverPassword = "";
   public static boolean useSteamRelay = false;
   public static int authType = 1;
   public UdpEngine udpEngine;
   public byte ID = -1;
   public float timeSinceKeepAlive = 0.0F;
   UpdateLimit itemSendFrequency = new UpdateLimit(3000L);
   public static int port;
   public boolean bPlayerConnectSent = false;
   private boolean bClientStarted = false;
   private int ResetID = 0;
   private boolean bConnectionLost = false;
   public static String checksum;
   public static boolean checksumValid;
   public static List<Long> pingsList;
   public static String GameMap;
   public static boolean bFastForward;
   public static final ClientServerMap[] loadedCells;
   public int DEBUG_PING = 5;
   public static boolean bCoopInvite;
   public ArrayList<IsoPlayer> connectedPlayers = new ArrayList();
   private static boolean isPaused;
   private final ArrayList<IsoPlayer> players = new ArrayList();
   public boolean idMapDirty = true;
   private static final int sendZombieWithoutNeighbor = 4000;
   private static final int sendZombieWithNeighbor = 200;
   public final UpdateLimit sendZombieTimer = new UpdateLimit(4000L);
   public final UpdateLimit sendZombieRequestsTimer = new UpdateLimit(200L);
   private final UpdateLimit UpdateChannelsRoamingLimit = new UpdateLimit(3010L);
   private long disconnectTime = System.currentTimeMillis();
   private static final long disconnectTimeLimit = 10000L;
   public static long steamID;
   public static final Map<Short, Vector2> positions;
   private int safehouseUpdateTimer = 0;
   private Vector3f vehicle1PositionVector = new Vector3f();
   private Vector3f vehicle2PositionVector = new Vector3f();
   private Vector3f vehicle1VelocityVector = new Vector3f();
   private Vector3f vehicle2VelocityVector = new Vector3f();
   private boolean delayPacket = false;
   private final ArrayList<Integer> delayedDisconnect = new ArrayList();
   static TShortArrayList tempShortList;
   private volatile RequestState request;
   public KahluaTable ServerSpawnRegions = null;
   static final ConcurrentLinkedQueue<ZomboidNetData> MainLoopNetDataQ;
   static final ArrayList<ZomboidNetData> MainLoopNetData;
   static final ArrayList<ZomboidNetData> LoadingMainLoopNetData;
   static final ArrayList<ZomboidNetData> DelayedCoopNetData;
   public boolean bConnected = false;
   public int TimeSinceLastUpdate = 0;
   ByteBuffer staticTest = ByteBuffer.allocate(20000);
   ByteBufferWriter wr;
   long StartHeartMilli;
   long EndHeartMilli;
   public int ping;
   public static float ServerPredictedAhead;
   public static final HashMap<Short, IsoPlayer> IDToPlayerMap;
   public static final TShortObjectHashMap<IsoZombie> IDToZombieMap;
   public static boolean bIngame;
   public static boolean askPing;
   public static boolean askCustomizationData;
   public static boolean sendQR;
   public final ArrayList<String> ServerMods;
   public ErosionConfig erosionConfig;
   public static Calendar startAuth;
   public static String poisonousBerry;
   public static String poisonousMushroom;
   final ArrayList<ZomboidNetData> incomingNetData;
   private final HashMap<ItemContainer, ArrayList<InventoryItem>> itemsToSend;
   private final HashMap<ItemContainer, ArrayList<InventoryItem>> itemsToSendRemove;
   KahluaTable dbSchema;

   public GameClient() {
      this.wr = new ByteBufferWriter(this.staticTest);
      this.StartHeartMilli = 0L;
      this.EndHeartMilli = 0L;
      this.ping = 0;
      this.ServerMods = new ArrayList();
      this.incomingNetData = new ArrayList();
      this.itemsToSend = new HashMap();
      this.itemsToSendRemove = new HashMap();
   }

   public IsoPlayer getPlayerByOnlineID(short var1) {
      return (IsoPlayer)IDToPlayerMap.get(var1);
   }

   public void init() {
      LoadingMainLoopNetData.clear();
      MainLoopNetDataQ.clear();
      MainLoopNetData.clear();
      DelayedCoopNetData.clear();
      bIngame = false;
      IDToPlayerMap.clear();
      IDToZombieMap.clear();
      pingsList.clear();
      this.itemsToSend.clear();
      this.itemsToSendRemove.clear();
      IDToZombieMap.setAutoCompactionFactor(0.0F);
      this.bPlayerConnectSent = false;
      this.bConnectionLost = false;
      this.delayedDisconnect.clear();
      GameWindow.bServerDisconnected = false;
      this.ServerSpawnRegions = null;
      this.startClient();
   }

   public void startClient() {
      if (this.bClientStarted) {
         this.udpEngine.Connect(ip, port, serverPassword, useSteamRelay);
      } else {
         try {
            this.udpEngine = new UdpEngine(Rand.Next(10000) + 12345, 0, 1, (String)null, false);
            if (CoopMaster.instance != null && CoopMaster.instance.isRunning()) {
               this.udpEngine.Connect("127.0.0.1", CoopMaster.instance.getServerPort(), serverPassword, false);
            } else {
               this.udpEngine.Connect(ip, port, serverPassword, useSteamRelay);
            }

            this.bClientStarted = true;
         } catch (Exception var2) {
            DebugLog.Network.printException(var2, "Exception thrown during GameClient.startClient.", LogSeverity.Error);
         }

      }
   }

   static void receiveStatistic(ByteBuffer var0, short var1) {
      try {
         long var2 = var0.getLong();
         ByteBufferWriter var4 = connection.startPacket();
         PacketTypes.PacketType.Statistic.doPacket(var4);
         var4.putLong(var2);
         MPStatisticClient.getInstance().send(var4);
         PacketTypes.PacketType.Statistic.send(connection);
      } catch (Exception var5) {
         var5.printStackTrace();
      }

   }

   static void receiveStatisticRequest(ByteBuffer var0, short var1) {
      try {
         MPStatistic.getInstance().setStatisticTable(var0);
      } catch (IOException var3) {
         var3.printStackTrace();
      }

      LuaEventManager.triggerEvent("OnServerStatisticReceived");
   }

   public String generateSecretKey(String var1) {
      SecureRandom var2 = new SecureRandom();
      byte[] var3 = new byte[20];
      var2.nextBytes(var3);
      Base32 var4 = new Base32();
      String var5 = var4.encodeToString(var3);
      return var5;
   }

   public String getGoogleAuthenticatorBarCode(String var1, String var2, String var3) {
      try {
         String var10000 = URLEncoder.encode(var3 + ":" + var2, "UTF-8").replace("+", "%20");
         return "otpauth://totp/" + var10000 + "?secret=" + URLEncoder.encode(var1, "UTF-8").replace("+", "%20") + "&issuer=" + URLEncoder.encode(var3, "UTF-8").replace("+", "%20");
      } catch (UnsupportedEncodingException var5) {
         throw new IllegalStateException(var5);
      }
   }

   public String getQR(String var1, String var2) {
      String var4 = "Zomboid";
      String var5 = this.getGoogleAuthenticatorBarCode(var2, var1, var4);
      DebugLog.General.println(var5);
      return var5;
   }

   static void receiveZombieSimulation(ByteBuffer var0, short var1) {
      NetworkZombieSimulator.getInstance().clear();
      boolean var2 = var0.get() == 1;
      if (var2) {
         instance.sendZombieTimer.setUpdatePeriod(200L);
      } else {
         instance.sendZombieTimer.setUpdatePeriod(4000L);
      }

      short var3 = var0.getShort();

      short var4;
      short var5;
      for(var4 = 0; var4 < var3; ++var4) {
         var5 = var0.getShort();
         IsoZombie var6 = (IsoZombie)IDToZombieMap.get(var5);
         if (var6 != null) {
            VirtualZombieManager.instance.removeZombieFromWorld(var6);
         }
      }

      var4 = var0.getShort();

      for(var5 = 0; var5 < var4; ++var5) {
         short var7 = var0.getShort();
         NetworkZombieSimulator.getInstance().add(var7);
      }

      NetworkZombieSimulator.getInstance().added();
      NetworkZombieSimulator.getInstance().receivePacket(var0, connection);
   }

   public void Shutdown() {
      if (this.bClientStarted) {
         this.udpEngine.Shutdown();
         this.bClientStarted = false;
      }

   }

   public void update() {
      ZombieCountOptimiser.startCount();
      if (this.safehouseUpdateTimer == 0 && ServerOptions.instance.DisableSafehouseWhenPlayerConnected.getValue()) {
         this.safehouseUpdateTimer = 3000;
         SafeHouse.updateSafehousePlayersConnected();
      }

      if (this.safehouseUpdateTimer > 0) {
         --this.safehouseUpdateTimer;
      }

      for(ZomboidNetData var1 = (ZomboidNetData)MainLoopNetDataQ.poll(); var1 != null; var1 = (ZomboidNetData)MainLoopNetDataQ.poll()) {
         MainLoopNetData.add(var1);
      }

      synchronized(this.delayedDisconnect) {
         while(!this.delayedDisconnect.isEmpty()) {
            int var2 = (Integer)this.delayedDisconnect.remove(0);
            switch (var2) {
               case 17:
                  if (!SteamUtils.isSteamModeEnabled()) {
                     LuaEventManager.triggerEvent("OnConnectFailed", (Object)null);
                  }
                  break;
               case 18:
                  LuaEventManager.triggerEvent("OnConnectFailed", Translator.getText("UI_OnConnectFailed_AlreadyConnected"));
               case 19:
               case 20:
               case 22:
               case 25:
               case 26:
               case 27:
               case 28:
               case 29:
               case 30:
               case 31:
               default:
                  break;
               case 21:
                  LuaEventManager.triggerEvent("OnDisconnect");
                  break;
               case 23:
                  LuaEventManager.triggerEvent("OnConnectFailed", Translator.getText("UI_OnConnectFailed_Banned"));
                  break;
               case 24:
                  LuaEventManager.triggerEvent("OnConnectFailed", Translator.getText("UI_OnConnectFailed_InvalidServerPassword"));
                  break;
               case 32:
                  LuaEventManager.triggerEvent("OnConnectFailed", Translator.getText("UI_OnConnectFailed_ConnectionLost"));
            }
         }
      }

      int var7;
      ZomboidNetData var8;
      if (!this.bConnectionLost) {
         if (!this.bPlayerConnectSent) {
            for(var7 = 0; var7 < MainLoopNetData.size(); ++var7) {
               var8 = (ZomboidNetData)MainLoopNetData.get(var7);
               if (!this.gameLoadingDealWithNetData(var8)) {
                  LoadingMainLoopNetData.add(var8);
               }
            }

            MainLoopNetData.clear();
            WorldStreamer.instance.updateMain();
         } else {
            if (!LoadingMainLoopNetData.isEmpty()) {
               DebugLog.log(DebugType.Network, "Processing delayed packets...");
               MainLoopNetData.addAll(0, LoadingMainLoopNetData);
               LoadingMainLoopNetData.clear();
            }

            if (!DelayedCoopNetData.isEmpty() && IsoWorld.instance.AddCoopPlayers.isEmpty()) {
               DebugLog.log(DebugType.Network, "Processing delayed coop packets...");
               MainLoopNetData.addAll(0, DelayedCoopNetData);
               DelayedCoopNetData.clear();
            }

            long var9 = System.currentTimeMillis();

            int var10;
            for(var10 = 0; var10 < MainLoopNetData.size(); ++var10) {
               ZomboidNetData var4 = (ZomboidNetData)MainLoopNetData.get(var10);
               if (var4.time + (long)this.DEBUG_PING <= var9) {
                  this.mainLoopDealWithNetData(var4);
                  MainLoopNetData.remove(var10--);
               }
            }

            for(var10 = 0; var10 < IsoWorld.instance.CurrentCell.getObjectList().size(); ++var10) {
               IsoMovingObject var11 = (IsoMovingObject)IsoWorld.instance.CurrentCell.getObjectList().get(var10);
               if (!(var11 instanceof IsoAnimal) && var11 instanceof IsoPlayer && !((IsoPlayer)var11).isLocalPlayer() && !this.getPlayers().contains(var11)) {
                  if (Core.bDebug) {
                     DebugLog.log("Disconnected/Distant player " + ((IsoPlayer)var11).username + " in CurrentCell.getObjectList() removed");
                  }

                  IsoWorld.instance.CurrentCell.getObjectList().remove(var10--);
               }
            }

            try {
               this.sendAddedRemovedItems(false);
            } catch (Exception var5) {
               var5.printStackTrace();
               ExceptionLogger.logException(var5);
            }

            if (this.UpdateChannelsRoamingLimit.Check()) {
               VoiceManager.getInstance().UpdateChannelsRoaming(connection);
            }

            this.updateVehiclesAnticlipping();
            WorldStreamer.instance.updateMain();
            MPStatisticClient.getInstance().update();
            this.timeSinceKeepAlive += GameTime.getInstance().getMultiplier();
            connection.checkReady();
            ChatManager.UpdateClient();
         }
      } else {
         if (!this.bPlayerConnectSent) {
            for(var7 = 0; var7 < MainLoopNetData.size(); ++var7) {
               var8 = (ZomboidNetData)MainLoopNetData.get(var7);
               this.gameLoadingDealWithNetData(var8);
            }

            MainLoopNetData.clear();
         } else {
            for(var7 = 0; var7 < MainLoopNetData.size(); ++var7) {
               var8 = (ZomboidNetData)MainLoopNetData.get(var7);
               if (var8.type == PacketTypes.PacketType.Kicked) {
                  KickedPacket var3 = new KickedPacket();
                  var3.parse(var8.buffer, (UdpConnection)null);
                  GameWindow.kickReason = var3.description + " " + var3.reason;
                  DebugLog.Multiplayer.warn("ReceiveKickedDisconnect: " + var3.reason);
               }
            }

            MainLoopNetData.clear();
         }

         GameWindow.bServerDisconnected = true;
         connection = null;
         ConnectionManager.getInstance().process();
      }
   }

   private void updateVehiclesAnticlipping() {
      if (DebugOptions.instance.Multiplayer.Debug.AnticlippingAlgorithm.getValue()) {
         ArrayList var1 = IsoWorld.instance.CurrentCell.getVehicles();

         for(int var2 = 0; var2 < var1.size(); ++var2) {
            BaseVehicle var3 = (BaseVehicle)var1.get(var2);
            if (var3.getCurrentSpeedKmHour() > 0.8F && (var3.netPlayerAuthorization == BaseVehicle.Authorization.Remote || var3.netPlayerAuthorization == BaseVehicle.Authorization.RemoteCollide)) {
               VehicleInterpolationData var4 = var3.interpolation.getLastAddedInterpolationPoint();
               if (var4 != null) {
                  var4.getPosition(this.vehicle1PositionVector);
                  var4.getVelocity(this.vehicle1VelocityVector);

                  for(int var5 = 0; var5 < var1.size(); ++var5) {
                     BaseVehicle var6 = (BaseVehicle)var1.get(var5);
                     if (var6 != var3 && var6.getCurrentSpeedKmHour() > 0.8F && !var6.interpolation.isDelayLengthIncreased()) {
                        VehicleInterpolationData var7 = var6.interpolation.getLastAddedInterpolationPoint();
                        if (var7 != null) {
                           var7.getPosition(this.vehicle2PositionVector);
                           var7.getVelocity(this.vehicle2VelocityVector);
                           float var8 = this.vehicle1PositionVector.distance(this.vehicle2PositionVector);
                           float var9 = this.vehicle1VelocityVector.distance(this.vehicle2VelocityVector);
                           if (var8 < var9 * 3.5F) {
                              int var10 = Integer.parseInt(MPStatistics.getLuaStatus().rawget("lastPing").toString());
                              float var11 = 2.0F;
                              if (var10 > 290) {
                                 var11 = 3.0F;
                              }

                              var3.interpolation.setDelayLength(var11);
                              if (var3.getVehicleTowing() != null) {
                                 var3.getVehicleTowing().interpolation.setDelayLength(var11);
                              }

                              if (var3.getVehicleTowedBy() != null) {
                                 var3.getVehicleTowedBy().interpolation.setDelayLength(var11);
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

      }
   }

   public void smashWindow(IsoWindow var1) {
      SmashWindowPacket var2 = new SmashWindowPacket();
      var2.setSmashWindow(var1);
      ByteBufferWriter var3 = connection.startPacket();
      PacketTypes.PacketType.SmashWindow.doPacket(var3);
      var2.write(var3);
      PacketTypes.PacketType.SmashWindow.send(connection);
   }

   public void removeBrokenGlass(IsoWindow var1) {
      SmashWindowPacket var2 = new SmashWindowPacket();
      var2.setRemoveBrokenGlass(var1);
      ByteBufferWriter var3 = connection.startPacket();
      PacketTypes.PacketType.SmashWindow.doPacket(var3);
      var2.write(var3);
      PacketTypes.PacketType.SmashWindow.send(connection);
   }

   static void receivePingFromClient(ByteBuffer var0, short var1) {
      MPStatistics.parse(var0);
   }

   public void delayPacket(int var1, int var2, int var3) {
      if (IsoWorld.instance != null) {
         for(int var4 = 0; var4 < IsoWorld.instance.AddCoopPlayers.size(); ++var4) {
            AddCoopPlayer var5 = (AddCoopPlayer)IsoWorld.instance.AddCoopPlayers.get(var4);
            if (var5.isLoadingThisSquare(var1, var2)) {
               this.delayPacket = true;
               return;
            }
         }

      }
   }

   private void mainLoopDealWithNetData(ZomboidNetData var1) {
      ByteBuffer var2 = var1.buffer;
      int var3 = var2.position();
      this.delayPacket = false;
      if (var1.type == null) {
         ZomboidNetDataPool.instance.discard(var1);
      } else {
         ++var1.type.clientPacketCount;

         try {
            this.mainLoopHandlePacketInternal(var1, var2);
            if (this.delayPacket) {
               var2.position(var3);
               DelayedCoopNetData.add(var1);
               return;
            }
         } catch (Exception var5) {
            DebugLog.Network.printException(var5, "Error with packet of type: " + var1.type, LogSeverity.Error);
         }

         ZomboidNetDataPool.instance.discard(var1);
      }
   }

   private void mainLoopHandlePacketInternal(ZomboidNetData var1, ByteBuffer var2) throws Exception {
      if (DebugOptions.instance.Network.Client.MainLoop.getValue()) {
         var1.type.onClientPacket(var2);
      }
   }

   static void receiveAddBrokenGlass(ByteBuffer var0, short var1) {
      int var2 = var0.getInt();
      int var3 = var0.getInt();
      int var4 = var0.getInt();
      IsoGridSquare var5 = IsoWorld.instance.CurrentCell.getGridSquare(var2, var3, var4);
      if (var5 != null) {
         var5.addBrokenGlass();
      }

   }

   public static void sendBigWaterSplash(int var0, int var1, float var2, float var3) {
      ByteBufferWriter var4 = connection.startPacket();
      PacketTypes.PacketType.StartFishSplash.doPacket(var4);
      var4.putInt(var0);
      var4.putInt(var1);
      var4.putFloat(var2);
      var4.putFloat(var3);
      PacketTypes.PacketType.StartFishSplash.send(connection);
   }

   static void receiveBigWaterSplash(ByteBuffer var0, short var1) {
      int var2 = var0.getInt();
      int var3 = var0.getInt();
      float var4 = var0.getFloat();
      float var5 = var0.getFloat();
      IsoGridSquare var6 = IsoWorld.instance.CurrentCell.getGridSquare(var2, var3, 0);
      if (var6 != null) {
         var6.startWaterSplash(true, var4, var5);
      }

   }

   public static void sendFishingDataRequest() {
      ByteBufferWriter var0 = connection.startPacket();
      PacketTypes.PacketType.FishingData.doPacket(var0);
      PacketTypes.PacketType.FishingData.send(connection);
   }

   static void receiveFishingData(ByteBuffer var0, short var1) {
      FishSchoolManager.getInstance().receiveFishingData(var0);
   }

   static void receivePlayerDamageFromCarCrash(ByteBuffer var0, short var1) {
      float var2 = var0.getFloat();
      if (IsoPlayer.getInstance().getVehicle() == null) {
         DebugLog.Multiplayer.error("Receive damage from car crash, can't find vehicle");
      } else {
         IsoPlayer.getInstance().getVehicle().addRandomDamageFromCrash(IsoPlayer.getInstance(), var2);
         LuaEventManager.triggerEvent("OnPlayerGetDamage", IsoPlayer.getInstance(), "CARCRASHDAMAGE", var2);
      }
   }

   static void receivePacketCounts(ByteBuffer var0, short var1) {
      int var2 = var0.getInt();

      for(int var3 = 0; var3 < var2; ++var3) {
         short var4 = var0.getShort();
         long var5 = var0.getLong();
         PacketTypes.PacketType var7 = (PacketTypes.PacketType)PacketTypes.packetTypes.get(var4);
         if (var7 != null) {
            var7.serverPacketCount = var5;
         }
      }

   }

   public void requestPacketCounts() {
      ByteBufferWriter var1 = connection.startPacket();
      PacketTypes.PacketType.PacketCounts.doPacket(var1);
      PacketTypes.PacketType.PacketCounts.send(connection);
   }

   public static boolean IsClientPaused() {
      return isPaused;
   }

   public static void setIsClientPaused(boolean var0) {
      isPaused = var0;
   }

   static void receiveChatMessageToPlayer(ByteBuffer var0, short var1) {
      ChatManager.getInstance().processChatMessagePacket(var0);
   }

   static void receivePlayerConnectedToChat(ByteBuffer var0, short var1) {
      ChatManager.getInstance().setFullyConnected();
   }

   static void receivePlayerJoinChat(ByteBuffer var0, short var1) {
      ChatManager.getInstance().processJoinChatPacket(var0);
   }

   static void receiveInvMngRemoveItem(ByteBuffer var0, short var1) {
      int var2 = var0.getInt();
      InventoryItem var3 = IsoPlayer.getInstance().getInventory().getItemWithIDRecursiv(var2);
      if (var3 == null) {
         DebugLog.log("ERROR: invMngRemoveItem can not find " + var2 + " item.");
      } else {
         IsoPlayer.getInstance().removeWornItem(var3);
         if (var3.getCategory().equals("Clothing")) {
            LuaEventManager.triggerEvent("OnClothingUpdated", IsoPlayer.getInstance());
         }

         if (var3 == IsoPlayer.getInstance().getPrimaryHandItem()) {
            IsoPlayer.getInstance().setPrimaryHandItem((InventoryItem)null);
            LuaEventManager.triggerEvent("OnClothingUpdated", IsoPlayer.getInstance());
         } else if (var3 == IsoPlayer.getInstance().getSecondaryHandItem()) {
            IsoPlayer.getInstance().setSecondaryHandItem((InventoryItem)null);
            LuaEventManager.triggerEvent("OnClothingUpdated", IsoPlayer.getInstance());
         }

         boolean var4 = IsoPlayer.getInstance().getInventory().removeItemWithIDRecurse(var2);
         if (!var4) {
            DebugLog.log("ERROR: GameClient.invMngRemoveItem can not remove item " + var2);
         }

      }
   }

   static void receiveInvMngGetItem(ByteBuffer var0, short var1) throws IOException {
      short var2 = var0.getShort();
      InventoryItem var3 = null;

      try {
         var3 = InventoryItem.loadItem(var0, 219);
      } catch (Exception var5) {
         var5.printStackTrace();
      }

      if (var3 != null) {
         IsoPlayer.getInstance().getInventory().addItem(var3);
      }

   }

   static void receiveInvMngReqItem(ByteBuffer var0, short var1) throws IOException {
      int var2 = 0;
      String var3 = null;
      if (var0.get() == 1) {
         var3 = GameWindow.ReadString(var0);
      } else {
         var2 = var0.getInt();
      }

      short var4 = var0.getShort();
      InventoryItem var5 = null;
      if (var3 == null) {
         var5 = IsoPlayer.getInstance().getInventory().getItemWithIDRecursiv(var2);
         if (var5 == null) {
            DebugLog.log("ERROR: invMngRemoveItem can not find " + var2 + " item.");
            return;
         }
      } else {
         var5 = InventoryItemFactory.CreateItem(var3);
      }

      if (var5 != null) {
         if (var3 == null) {
            IsoPlayer.getInstance().removeWornItem(var5);
            if (var5.getCategory().equals("Clothing")) {
               LuaEventManager.triggerEvent("OnClothingUpdated", IsoPlayer.getInstance());
            }

            if (var5 == IsoPlayer.getInstance().getPrimaryHandItem()) {
               IsoPlayer.getInstance().setPrimaryHandItem((InventoryItem)null);
               LuaEventManager.triggerEvent("OnClothingUpdated", IsoPlayer.getInstance());
            } else if (var5 == IsoPlayer.getInstance().getSecondaryHandItem()) {
               IsoPlayer.getInstance().setSecondaryHandItem((InventoryItem)null);
               LuaEventManager.triggerEvent("OnClothingUpdated", IsoPlayer.getInstance());
            }

            IsoPlayer.getInstance().getInventory().removeItemWithIDRecurse(var5.getID());
         } else {
            IsoPlayer.getInstance().getInventory().RemoveOneOf(var3.split("\\.")[1]);
         }

         ByteBufferWriter var6 = connection.startPacket();
         PacketTypes.PacketType.InvMngGetItem.doPacket(var6);
         var6.putShort(var4);
         var5.saveWithSize(var6.bb, false);
         PacketTypes.PacketType.InvMngGetItem.send(connection);
      }

   }

   public static void invMngRequestItem(int var0, String var1, short var2, String var3) {
      ByteBufferWriter var4 = connection.startPacket();
      PacketTypes.PacketType.InvMngReqItem.doPacket(var4);
      if (var1 != null) {
         var4.putByte((byte)1);
         var4.putUTF(var1);
      } else {
         var4.putByte((byte)0);
         var4.putInt(var0);
      }

      var4.putShort(IsoPlayer.getInstance().getOnlineID());
      var4.putShort(var2);
      PacketTypes.PacketType.InvMngReqItem.send(connection);
   }

   public static void invMngRequestRemoveItem(int var0, short var1, String var2) {
      ByteBufferWriter var3 = connection.startPacket();
      PacketTypes.PacketType.InvMngRemoveItem.doPacket(var3);
      var3.putInt(var0);
      var3.putShort(var1);
      PacketTypes.PacketType.InvMngRemoveItem.send(connection);
   }

   static void receiveSyncFaction(ByteBuffer var0, short var1) {
      String var2 = GameWindow.ReadString(var0);
      String var3 = GameWindow.ReadString(var0);
      int var4 = var0.getInt();
      Faction var5 = Faction.getFaction(var2);
      if (var5 == null) {
         var5 = new Faction(var2, var3);
         Faction.getFactions().add(var5);
      }

      var5.getPlayers().clear();
      if (var0.get() == 1) {
         var5.setTag(GameWindow.ReadString(var0));
         var5.setTagColor(new ColorInfo(var0.getFloat(), var0.getFloat(), var0.getFloat(), 1.0F));
      }

      for(int var6 = 0; var6 < var4; ++var6) {
         var5.getPlayers().add(GameWindow.ReadString(var0));
      }

      var5.setOwner(var3);
      boolean var7 = var0.get() == 1;
      if (var7) {
         Faction.getFactions().remove(var5);
         if (GameServer.bServer || LuaManager.GlobalObject.isAdmin()) {
            DebugLog.log("faction: removed " + var2 + " owner=" + var5.getOwner());
         }
      }

      LuaEventManager.triggerEvent("SyncFaction", var2);
   }

   static void receiveChangeTextColor(ByteBuffer var0, short var1) {
      short var2 = var0.getShort();
      IsoPlayer var3 = (IsoPlayer)IDToPlayerMap.get(var2);
      if (var3 != null) {
         float var4 = var0.getFloat();
         float var5 = var0.getFloat();
         float var6 = var0.getFloat();
         var3.setSpeakColourInfo(new ColorInfo(var4, var5, var6, 1.0F));
      }
   }

   static void receivePlaySoundEveryPlayer(ByteBuffer var0, short var1) {
      String var2 = GameWindow.ReadString(var0);
      int var3 = var0.getInt();
      int var4 = var0.getInt();
      int var5 = var0.getInt();
      DebugLog.log(DebugType.Sound, "sound: received " + var2 + " at " + var3 + "," + var4 + "," + var5);
      if (!Core.SoundDisabled) {
         FMOD_STUDIO_EVENT_DESCRIPTION var6 = FMODManager.instance.getEventDescription(var2);
         if (var6 == null) {
            return;
         }

         long var7 = javafmod.FMOD_Studio_System_CreateEventInstance(var6.address);
         if (var7 <= 0L) {
            return;
         }

         javafmod.FMOD_Studio_EventInstance_SetVolume(var7, (float)Core.getInstance().getOptionAmbientVolume() / 20.0F);
         javafmod.FMOD_Studio_EventInstance3D(var7, (float)var3, (float)var4, (float)var5);
         javafmod.FMOD_Studio_StartEvent(var7);
         javafmod.FMOD_Studio_ReleaseEventInstance(var7);
      }

   }

   static void receiveAddAlarm(ByteBuffer var0, short var1) {
      int var2 = var0.getInt();
      int var3 = var0.getInt();
      DebugLog.log(DebugType.Multiplayer, "ReceiveAlarm at [ " + var2 + " , " + var3 + " ]");
      IsoGridSquare var4 = IsoWorld.instance.CurrentCell.getGridSquare(var2, var3, 0);
      if (var4 != null && var4.getBuilding() != null && var4.getBuilding().getDef() != null) {
         var4.getBuilding().getDef().bAlarmed = true;
         AmbientStreamManager.instance.doAlarm(var4.room.def);
      }
   }

   static void receiveSyncDoorKey(ByteBuffer var0, short var1) {
      int var2 = var0.getInt();
      int var3 = var0.getInt();
      int var4 = var0.getInt();
      byte var5 = var0.get();
      IsoGridSquare var6 = IsoWorld.instance.CurrentCell.getGridSquare(var2, var3, var4);
      if (var6 == null) {
         instance.delayPacket(var2, var3, var4);
      } else {
         if (var5 >= 0 && var5 < var6.getObjects().size()) {
            IsoObject var7 = (IsoObject)var6.getObjects().get(var5);
            if (var7 instanceof IsoDoor) {
               IsoDoor var8 = (IsoDoor)var7;
               var8.keyId = var0.getInt();
            } else {
               DebugLog.log("SyncDoorKey: expected IsoDoor index=" + var5 + " is invalid x,y,z=" + var2 + "," + var3 + "," + var4);
            }
         } else {
            DebugLog.log("SyncDoorKey: index=" + var5 + " is invalid x,y,z=" + var2 + "," + var3 + "," + var4);
         }

      }
   }

   static void receiveConstructedZone(ByteBuffer var0, short var1) {
      int var2 = var0.getInt();
      int var3 = var0.getInt();
      int var4 = var0.getInt();
      Zone var5 = IsoWorld.instance.MetaGrid.getZoneAt(var2, var3, var4);
      if (var5 != null) {
         var5.setHaveConstruction(true);
      }

   }

   static void receiveZombieDescriptors(ByteBuffer var0, short var1) {
      try {
         SharedDescriptors.Descriptor var2 = new SharedDescriptors.Descriptor();
         var2.load(var0, 219);
         SharedDescriptors.registerPlayerZombieDescriptor(var2);
      } catch (Exception var3) {
         var3.printStackTrace();
      }

   }

   public void sendAddXp(IsoPlayer var1, PerkFactory.Perk var2, float var3, boolean var4) {
      INetworkPacket.send(PacketTypes.PacketType.AddXP, var1, var2, var3, var4);
   }

   public void sendGetAnimalTracks(IsoGameCharacter var1) {
      INetworkPacket.send(PacketTypes.PacketType.AnimalTracks, var1);
   }

   public void sendSyncXp(IsoPlayer var1) {
      INetworkPacket.send(PacketTypes.PacketType.SyncXP, var1);
   }

   static void receivePing(ByteBuffer var0, short var1) {
      String var2 = GameWindow.ReadString(var0);
      int var10000 = var0.getInt() - 1;
      String var3 = "" + var10000 + "/" + var0.getInt();
      LuaEventManager.triggerEvent("ServerPinged", var2, var3);
      connection.forceDisconnect("receive-ping");
      askPing = false;
   }

   public static void sendChangeSafety(Safety var0) {
      ByteBufferWriter var1 = connection.startPacket();
      PacketTypes.PacketType.ChangeSafety.doPacket(var1);

      try {
         SafetyPacket var2 = new SafetyPacket(var0);
         var2.write(var1);
         PacketTypes.PacketType.ChangeSafety.send(connection);
      } catch (Exception var3) {
         connection.cancelPacket();
         DebugLog.Multiplayer.printException(var3, "SendChangeSafety: failed", LogSeverity.Error);
      }

   }

   public void addDisconnectPacket(int var1) {
      synchronized(this.delayedDisconnect) {
         this.delayedDisconnect.add(var1);
      }

      ConnectionManager.log("disconnect", String.valueOf(var1), connection);
   }

   public void connectionLost() {
      this.bConnectionLost = true;
      positions.clear();
      WorldMapRemotePlayers.instance.Reset();
   }

   public static void SendCommandToServer(String var0) {
      if (ServerOptions.clientOptionsList == null) {
         ServerOptions.initClientCommandsHelp();
      }

      if (var0.startsWith("/roll")) {
         try {
            int var1 = Integer.parseInt(var0.split(" ")[1]);
            if (var1 > 100) {
               ChatManager.getInstance().showServerChatMessage((String)ServerOptions.clientOptionsList.get("roll"));
               return;
            }
         } catch (Exception var5) {
            ChatManager.getInstance().showServerChatMessage((String)ServerOptions.clientOptionsList.get("roll"));
            return;
         }

         if (!IsoPlayer.getInstance().getInventory().contains("Dice") && !connection.role.haveCapability(Capability.GeneralCheats)) {
            ChatManager.getInstance().showServerChatMessage((String)ServerOptions.clientOptionsList.get("roll"));
            return;
         }
      }

      if (var0.startsWith("/card") && !IsoPlayer.getInstance().getInventory().contains("CardDeck") && !connection.role.haveCapability(Capability.GeneralCheats)) {
         ChatManager.getInstance().showServerChatMessage((String)ServerOptions.clientOptionsList.get("card"));
      } else {
         if (var0.startsWith("/list")) {
            String[] var6 = var0.split(" ");
            if (var6.length == 2) {
               ChatManager.getInstance().showServerChatMessage(ListCommand.List(var6[1]));
            }
         }

         if (!var0.startsWith("/log ")) {
            ByteBufferWriter var8 = connection.startPacket();
            PacketTypes.PacketType.ReceiveCommand.doPacket(var8);
            var8.putUTF(var0);
            PacketTypes.PacketType.ReceiveCommand.send(connection);
         } else {
            String var7 = ChatManager.getInstance().getFocusTab().getTitleID();
            if ("UI_chat_admin_tab_title_id".equals(var7)) {
               ByteBufferWriter var2 = connection.startPacket();
               PacketTypes.PacketType.ReceiveCommand.doPacket(var2);
               var2.putUTF(var0);
               PacketTypes.PacketType.ReceiveCommand.send(connection);
            } else if ("UI_chat_main_tab_title_id".equals(var7)) {
               String[] var9 = var0.split(" ");
               if (var9.length == 3) {
                  DebugType var3 = LogCommand.getDebugType(var9[1]);
                  LogSeverity var4 = LogCommand.getLogSeverity(var9[2]);
                  if (var3 != null && var4 != null) {
                     DebugLog.enableLog(var3, var4);
                     ChatManager.getInstance().showServerChatMessage(String.format("Client \"%s\" log level is \"%s\"", var3.name().toLowerCase(), var4.name().toLowerCase()));
                     if (DebugType.Network.equals(var3)) {
                        ZNet.SetLogLevel(DebugLog.getLogLevel(DebugType.Network));
                     }
                  } else {
                     ChatManager.getInstance().showServerChatMessage(Translator.getText("UI_ServerOptionDesc_SetLogLevel", var3 == null ? "\"type\"" : var3.name().toLowerCase(), var4 == null ? "\"severity\"" : var4.name().toLowerCase()));
                  }
               }
            }

         }
      }
   }

   public static void sendServerPing(long var0) {
      if (connection != null) {
         ByteBufferWriter var2 = connection.startPacket();
         PacketTypes.PacketType.PingFromClient.doPacket(var2);
         var2.putLong(var0);
         PacketTypes.PacketType.PingFromClient.send(connection);
         if (var0 == -1L) {
            DebugLog.Multiplayer.debugln("Player \"%s\" toggled lua debugger", connection.username);
         }
      }

   }

   private boolean gameLoadingDealWithNetData(ZomboidNetData var1) {
      ByteBuffer var2 = var1.buffer;

      try {
         return var1.type.onClientLoadingPacket(var2);
      } catch (Exception var4) {
         DebugLog.log(DebugType.Network, "Error with packet of type: " + var1.type);
         var4.printStackTrace();
         ZomboidNetDataPool.instance.discard(var1);
         return true;
      }
   }

   static void receiveStartRain(ByteBuffer var0, short var1) {
      RainManager.setRandRainMin(var0.getInt());
      RainManager.setRandRainMax(var0.getInt());
      RainManager.startRaining();
      RainManager.RainDesiredIntensity = var0.getFloat();
   }

   static void receiveStopRain(ByteBuffer var0, short var1) {
      RainManager.stopRaining();
   }

   static void receiveWeather(ByteBuffer var0, short var1) {
      WeatherPacket var2 = new WeatherPacket();
      var2.parse(var0, connection);
   }

   static void receiveWorldMapPlayerPosition(ByteBuffer var0, short var1) {
      tempShortList.clear();
      boolean var2 = var0.get() == 1;
      short var3 = var0.getShort();

      int var5;
      for(int var4 = 0; var4 < var3; ++var4) {
         var5 = var0.getShort();
         WorldMapRemotePlayer var6 = WorldMapRemotePlayers.instance.getOrCreatePlayerByID((short)var5);
         short var7;
         if (var2) {
            var7 = var0.getShort();
            String var8 = GameWindow.ReadStringUTF(var0);
            String var9 = GameWindow.ReadStringUTF(var0);
            String var10 = GameWindow.ReadStringUTF(var0);
            String var11 = GameWindow.ReadStringUTF(var0);
            float var12 = var0.getFloat();
            float var13 = var0.getFloat();
            boolean var14 = var0.get() == 1;
            boolean var15 = var0.get() == 1;
            var6.setFullData(var7, var8, var9, var10, var11, var12, var13, var14, var15);
            if (positions.containsKey(Short.valueOf((short)var5))) {
               ((Vector2)positions.get(Short.valueOf((short)var5))).set(var12, var13);
            } else {
               positions.put(Short.valueOf((short)var5), new Vector2(var12, var13));
            }
         } else {
            var7 = var0.getShort();
            float var17 = var0.getFloat();
            float var18 = var0.getFloat();
            if (var6.getChangeCount() != var7) {
               tempShortList.add((short)var5);
            } else {
               var6.setPosition(var17, var18);
               if (positions.containsKey(Short.valueOf((short)var5))) {
                  ((Vector2)positions.get(Short.valueOf((short)var5))).set(var17, var18);
               } else {
                  positions.put(Short.valueOf((short)var5), new Vector2(var17, var18));
               }
            }
         }
      }

      if (!tempShortList.isEmpty()) {
         ByteBufferWriter var16 = connection.startPacket();
         PacketTypes.PacketType.WorldMapPlayerPosition.doPacket(var16);
         var16.putShort((short)tempShortList.size());

         for(var5 = 0; var5 < tempShortList.size(); ++var5) {
            var16.putShort(tempShortList.get(var5));
         }

         PacketTypes.PacketType.WorldMapPlayerPosition.send(connection);
      }
   }

   static void receiveClientCommand(ByteBuffer var0, short var1) {
      String var2 = GameWindow.ReadString(var0);
      String var3 = GameWindow.ReadString(var0);
      boolean var4 = var0.get() == 1;
      KahluaTable var5 = null;
      if (var4) {
         var5 = LuaManager.platform.newTable();

         try {
            TableNetworkUtils.load(var5, var0);
         } catch (Exception var7) {
            var7.printStackTrace();
            return;
         }
      }

      LuaEventManager.triggerEvent("OnServerCommand", var2, var3, var5);
   }

   static void receiveWorldMap(ByteBuffer var0, short var1) throws IOException {
      WorldMapClient.instance.receive(var0);
   }

   public void setRequest(RequestState var1) {
      this.request = var1;
   }

   public void GameLoadingRequestData() {
      RequestDataPacket var2 = new RequestDataPacket();
      this.request = GameClient.RequestState.Start;

      while(this.request != GameClient.RequestState.Complete) {
         if (this.request == GameClient.RequestState.Start) {
            var2.setRequest();
            ByteBufferWriter var1 = connection.startPacket();
            PacketTypes.PacketType.RequestData.doPacket(var1);
            var2.write(var1);
            PacketTypes.PacketType.RequestData.send(connection);
            this.request = GameClient.RequestState.Loading;
         }

         try {
            Thread.sleep(30L);
         } catch (InterruptedException var4) {
            DebugLog.Multiplayer.printException(var4, "GameLoadingRequestData sleep error", LogSeverity.Error);
         }
      }

   }

   static void receiveSendCustomColor(ByteBuffer var0, short var1) {
      int var2 = var0.getInt();
      int var3 = var0.getInt();
      int var4 = var0.getInt();
      int var5 = var0.getInt();
      IsoGridSquare var6 = IsoWorld.instance.CurrentCell.getGridSquare(var2, var3, var4);
      if (var6 == null) {
         instance.delayPacket(var2, var3, var4);
      } else {
         if (var6 != null && var5 < var6.getObjects().size()) {
            IsoObject var7 = (IsoObject)var6.getObjects().get(var5);
            if (var7 != null) {
               var7.setCustomColor(new ColorInfo(var0.getFloat(), var0.getFloat(), var0.getFloat(), var0.getFloat()));
            }
         }

      }
   }

   static void receiveUpdateItemSprite(ByteBuffer var0, short var1) {
      int var2 = var0.getInt();
      String var3 = GameWindow.ReadStringUTF(var0);
      int var4 = var0.getInt();
      int var5 = var0.getInt();
      int var6 = var0.getInt();
      int var7 = var0.getInt();
      IsoGridSquare var8 = IsoWorld.instance.CurrentCell.getGridSquare(var4, var5, var6);
      if (var8 == null) {
         instance.delayPacket(var4, var5, var6);
      } else {
         if (var8 != null && var7 < var8.getObjects().size()) {
            try {
               IsoObject var9 = (IsoObject)var8.getObjects().get(var7);
               if (var9 != null) {
                  boolean var10 = var9.sprite != null && var9.sprite.getProperties().Is("HitByCar") && var9.sprite.getProperties().Val("DamagedSprite") != null && !var9.sprite.getProperties().Val("DamagedSprite").isEmpty();
                  var9.sprite = IsoSpriteManager.instance.getSprite(var2);
                  if (var9.sprite == null && !var3.isEmpty()) {
                     var9.setSprite(var3);
                  }

                  var9.RemoveAttachedAnims();
                  int var11 = var0.get() & 255;

                  for(int var12 = 0; var12 < var11; ++var12) {
                     int var13 = var0.getInt();
                     IsoSprite var14 = IsoSpriteManager.instance.getSprite(var13);
                     if (var14 != null) {
                        var9.AttachExistingAnim(var14, 0, 0, false, 0, false, 0.0F);
                     }
                  }

                  if (var9 instanceof IsoThumpable && var10 && (var9.sprite == null || !var9.sprite.getProperties().Is("HitByCar"))) {
                     ((IsoThumpable)var9).setBlockAllTheSquare(false);
                  }

                  var8.RecalcAllWithNeighbours(true);
               }
            } catch (Exception var15) {
            }
         }

      }
   }

   private KahluaTable copyTable(KahluaTable var1) {
      KahluaTable var2 = LuaManager.platform.newTable();
      KahluaTableIterator var3 = var1.iterator();

      while(var3.advance()) {
         Object var4 = var3.getKey();
         Object var5 = var3.getValue();
         if (var5 instanceof KahluaTable) {
            var2.rawset(var4, this.copyTable((KahluaTable)var5));
         } else {
            var2.rawset(var4, var5);
         }
      }

      return var2;
   }

   public KahluaTable getServerSpawnRegions() {
      return this.copyTable(this.ServerSpawnRegions);
   }

   public static void sendZombieHit(IsoZombie var0, IsoPlayer var1) {
      boolean var2 = var1.isLocalPlayer();
      boolean var3 = !var0.isRemoteZombie();
      if (var3 && var2) {
         ByteBufferWriter var4 = connection.startPacket();

         try {
            PacketTypes.PacketType.ZombieHitPlayer.doPacket(var4);
            ZombieHitPlayerPacket var5 = new ZombieHitPlayerPacket();
            var5.set(var0, var1);
            var5.write(var4);
            PacketTypes.PacketType.ZombieHitPlayer.send(connection);
         } catch (Exception var6) {
            connection.cancelPacket();
            DebugLog.Multiplayer.printException(var6, "SendHitCharacter: failed", LogSeverity.Error);
         }
      }

   }

   public static void sendAnimalHitPlayer(IsoGameCharacter var0, IsoMovingObject var1, float var2, boolean var3) {
      AnimalHitPlayerPacket var4 = new AnimalHitPlayerPacket();
      var4.set((IsoAnimal)var0, (IsoPlayer)var1, var3, var2);
      ByteBufferWriter var5 = connection.startPacket();
      PacketTypes.PacketType.AnimalHitPlayer.doPacket(var5);
      var4.write(var5);
      PacketTypes.PacketType.AnimalHitPlayer.send(connection);
   }

   public static void sendAnimalHitAnimal(IsoGameCharacter var0, IsoMovingObject var1, float var2, boolean var3) {
      AnimalHitAnimalPacket var4 = new AnimalHitAnimalPacket();
      var4.set((IsoAnimal)var0, (IsoAnimal)var1, var3, var2);
      ByteBufferWriter var5 = connection.startPacket();
      PacketTypes.PacketType.AnimalHitAnimal.doPacket(var5);
      var4.write(var5);
      PacketTypes.PacketType.AnimalHitAnimal.send(connection);
   }

   public static void sendAnimalHitThumpable(IsoGameCharacter var0) {
      AnimalHitThumpablePacket var1 = new AnimalHitThumpablePacket();
      var1.set((IsoAnimal)var0, ((IsoAnimal)var0).thumpTarget);
      ByteBufferWriter var2 = connection.startPacket();
      PacketTypes.PacketType.AnimalHitThumpable.doPacket(var2);
      var1.write(var2);
      PacketTypes.PacketType.AnimalHitThumpable.send(connection);
   }

   public static void sendForageItemFound(IsoPlayer var0, String var1, float var2) {
      INetworkPacket.send(PacketTypes.PacketType.ForageItemFound, var0, var1, var2);
   }

   public static boolean sendPlayerHit(IsoGameCharacter var0, IsoObject var1, HandWeapon var2, float var3, boolean var4, float var5, boolean var6, boolean var7, boolean var8) {
      boolean var9 = false;
      ByteBufferWriter var11;
      if (var1 == null) {
         PlayerHitSquarePacket var10 = new PlayerHitSquarePacket();
         var10.set(new Object[]{(IsoPlayer)var0, var2, var6});
         var11 = connection.startPacket();
         PacketTypes.PacketType.PlayerHitSquare.doPacket(var11);
         var10.write(var11);
         PacketTypes.PacketType.PlayerHitSquare.send(connection);
         var9 = true;
      } else if (var1 instanceof IsoAnimal) {
         PlayerHitAnimalPacket var12 = new PlayerHitAnimalPacket();
         var12.set(new Object[]{(IsoPlayer)var0, (IsoAnimal)var1, var2, var3, var4, var5, var6, var8});
         var11 = connection.startPacket();
         PacketTypes.PacketType.PlayerHitAnimal.doPacket(var11);
         var12.write(var11);
         PacketTypes.PacketType.PlayerHitAnimal.send(connection);
         var9 = true;
      } else if (var1 instanceof IsoPlayer) {
         PlayerHitPlayerPacket var13 = new PlayerHitPlayerPacket();
         var13.set(new Object[]{(IsoPlayer)var0, (IsoPlayer)var1, var2, var3, var4, var5, var6, var8});
         var11 = connection.startPacket();
         PacketTypes.PacketType.PlayerHitPlayer.doPacket(var11);
         var13.write(var11);
         PacketTypes.PacketType.PlayerHitPlayer.send(connection);
         var9 = true;
      } else if (var1 instanceof IsoZombie) {
         PlayerHitZombiePacket var14 = new PlayerHitZombiePacket();
         var14.set(new Object[]{(IsoPlayer)var0, (IsoZombie)var1, var2, var3, var4, var5, var6, var7, var8});
         var11 = connection.startPacket();
         PacketTypes.PacketType.PlayerHitZombie.doPacket(var11);
         var14.write(var11);
         PacketTypes.PacketType.PlayerHitZombie.send(connection);
         var9 = true;
      } else if (var1 instanceof BaseVehicle) {
         PlayerHitVehiclePacket var15 = new PlayerHitVehiclePacket();
         var15.set((IsoPlayer)var0, (BaseVehicle)var1, var2, var6, var3);
         var11 = connection.startPacket();
         PacketTypes.PacketType.PlayerHitVehicle.doPacket(var11);
         var15.write(var11);
         PacketTypes.PacketType.PlayerHitVehicle.send(connection);
         var9 = true;
      } else if (var1 instanceof IsoObject) {
         PlayerHitObjectPacket var16 = new PlayerHitObjectPacket();
         var16.set((IsoPlayer)var0, var1, var2, var6);
         var11 = connection.startPacket();
         PacketTypes.PacketType.PlayerHitObject.doPacket(var11);
         var16.write(var11);
         PacketTypes.PacketType.PlayerHitObject.send(connection);
         var9 = true;
      } else {
         DebugLog.Multiplayer.warn(String.format("SendHitCharacter: unknown target type (wielder=%s, target=%s)", var0.getClass().getName(), var1.getClass().getName()));
      }

      return var9;
   }

   public static void sendVehicleHit(IsoPlayer var0, IsoGameCharacter var1, BaseVehicle var2, float var3, boolean var4, int var5, float var6, boolean var7) {
      ByteBufferWriter var8 = connection.startPacket();

      try {
         if (var1 instanceof IsoPlayer) {
            PacketTypes.PacketType.VehicleHitPlayer.doPacket(var8);
            VehicleHitPlayerPacket var9 = new VehicleHitPlayerPacket();
            var9.set(var0, (IsoPlayer)var1, var2, var3, var4, var5, var6, var7);
            var9.write(var8);
            PacketTypes.PacketType.VehicleHitPlayer.send(connection);
         } else if (var1 instanceof IsoZombie) {
            PacketTypes.PacketType.VehicleHitZombie.doPacket(var8);
            VehicleHitZombiePacket var11 = new VehicleHitZombiePacket();
            var11.set(var0, (IsoZombie)var1, var2, var3, var4, var5, var6, var7);
            var11.write(var8);
            PacketTypes.PacketType.VehicleHitZombie.send(connection);
         } else {
            DebugLog.Multiplayer.warn(String.format("SendHitVehicle: unknown target type (wielder=%s, target=%s)", var0.getClass().getName(), var1.getClass().getName()));
         }
      } catch (Exception var10) {
         connection.cancelPacket();
         DebugLog.Multiplayer.printException(var10, "SendHitVehicle: failed", LogSeverity.Error);
      }

   }

   public static void sendZombieDeath(IsoZombie var0) {
      ByteBufferWriter var1 = connection.startPacket();
      PacketTypes.PacketType.ZombieDeath.doPacket(var1);

      try {
         DeadZombiePacket var2 = new DeadZombiePacket();
         var2.set(var0);
         var2.write(var1);
         PacketTypes.PacketType.ZombieDeath.send(connection);
         if (Core.bDebug) {
            DebugLog.Multiplayer.debugln("SendZombieDeath: %s", var2.getDescription());
         }
      } catch (Exception var3) {
         connection.cancelPacket();
         DebugLog.Multiplayer.printException(var3, "SendZombieDeath: failed", LogSeverity.Error);
      }

   }

   public static void sendAnimalDeath(IsoAnimal var0) {
      ByteBufferWriter var1 = connection.startPacket();
      PacketTypes.PacketType.AnimalDeath.doPacket(var1);

      try {
         DeadAnimalPacket var2 = new DeadAnimalPacket();
         var2.set(var0);
         var2.write(var1);
         PacketTypes.PacketType.AnimalDeath.send(connection);
      } catch (Exception var3) {
         connection.cancelPacket();
         DebugLog.Multiplayer.printException(var3, "SendAnimalDeath: failed", LogSeverity.Error);
      }

   }

   public static void sendPlayerDeath(IsoPlayer var0) {
      ByteBufferWriter var1 = connection.startPacket();
      PacketTypes.PacketType.PlayerDeath.doPacket(var1);

      try {
         DeadPlayerPacket var2 = new DeadPlayerPacket();
         var2.set(var0);
         var2.write(var1);
         PacketTypes.PacketType.PlayerDeath.send(connection);
      } catch (Exception var3) {
         connection.cancelPacket();
         DebugLog.Multiplayer.printException(var3, "SendPlayerDeath: failed", LogSeverity.Error);
      }

   }

   public static void sendPlayerDamage(IsoPlayer var0) {
      INetworkPacket.send(PacketTypes.PacketType.PlayerDamage, var0);
   }

   public static void sendPlayerInjuries(IsoPlayer var0) {
      SyncInjuriesPacket var1 = new SyncInjuriesPacket();
      var1.set(var0);
      ByteBufferWriter var2 = connection.startPacket();
      PacketTypes.PacketType.SyncInjuries.doPacket(var2);

      try {
         var1.write(var2);
         PacketTypes.PacketType.SyncInjuries.send(connection);
         DebugLog.Damage.trace(var1.getDescription());
      } catch (Exception var4) {
         connection.cancelPacket();
         DebugLog.Multiplayer.printException(var4, "SendPlayerInjuries: failed", LogSeverity.Error);
      }

   }

   public static void sendRemoveCorpseFromMap(IsoDeadBody var0) {
      RemoveCorpseFromMapPacket var1 = new RemoveCorpseFromMapPacket();
      var1.set(var0);
      DebugLog.Death.trace(var1.getDescription());
      ByteBufferWriter var2 = connection.startPacket();
      PacketTypes.PacketType.RemoveCorpseFromMap.doPacket(var2);
      var1.write(var2);
      PacketTypes.PacketType.RemoveCorpseFromMap.send(connection);
   }

   public static void sendEvent(IsoPlayer var0, String var1) {
      ByteBufferWriter var2 = connection.startPacket();
      PacketTypes.PacketType.EventPacket.doPacket(var2);

      try {
         EventPacket var3 = new EventPacket();
         if (var3.set(var0, var1)) {
            var3.write(var2);
            PacketTypes.PacketType.EventPacket.send(connection);
         } else {
            connection.cancelPacket();
         }
      } catch (Exception var4) {
         connection.cancelPacket();
         DebugLog.Multiplayer.printException(var4, "SendEvent: failed", LogSeverity.Error);
      }

   }

   public static void sendAction(BaseAction var0, boolean var1) {
      INetworkPacket.send(PacketTypes.PacketType.ActionPacket, var1, var0);
   }

   public static void sendEatBody(IsoZombie var0, IsoMovingObject var1) {
      ByteBufferWriter var2 = connection.startPacket();
      PacketTypes.PacketType.EatBody.doPacket(var2);

      try {
         var2.putShort(var0.getOnlineID());
         if (var1 instanceof IsoDeadBody var3) {
            var2.putByte((byte)1);
            var2.putBoolean(var0.getVariableBoolean("onknees"));
            var2.putFloat(var0.getEatSpeed());
            var2.putFloat(var0.getStateEventDelayTimer());
            var2.putInt(var3.getStaticMovingObjectIndex());
            var2.putFloat((float)var3.getSquare().getX());
            var2.putFloat((float)var3.getSquare().getY());
            var2.putFloat((float)var3.getSquare().getZ());
         } else if (var1 instanceof IsoPlayer) {
            var2.putByte((byte)2);
            var2.putBoolean(var0.getVariableBoolean("onknees"));
            var2.putFloat(var0.getEatSpeed());
            var2.putFloat(var0.getStateEventDelayTimer());
            var2.putShort(((IsoPlayer)var1).getOnlineID());
         } else {
            var2.putByte((byte)0);
         }

         if (Core.bDebug) {
            DebugLog.log(DebugType.Multiplayer, "SendEatBody");
         }

         PacketTypes.PacketType.EatBody.send(connection);
      } catch (Exception var4) {
         DebugLog.Multiplayer.printException(var4, "SendEatBody: failed", LogSeverity.Error);
         connection.cancelPacket();
      }

   }

   public static void receiveEatBody(ByteBuffer var0, short var1) {
      try {
         short var2 = var0.getShort();
         byte var3 = var0.get();
         if (Core.bDebug) {
            DebugLog.log(DebugType.Multiplayer, String.format("ReceiveEatBody: zombie=%d type=%d", var2, var3));
         }

         IsoZombie var4 = (IsoZombie)IDToZombieMap.get(var2);
         if (var4 == null) {
            DebugLog.Multiplayer.error("ReceiveEatBody: zombie " + var2 + " not found");
            return;
         }

         boolean var5;
         float var6;
         float var7;
         if (var3 == 1) {
            var5 = var0.get() != 0;
            var6 = var0.getFloat();
            var7 = var0.getFloat();
            int var8 = var0.getInt();
            float var9 = var0.getFloat();
            float var10 = var0.getFloat();
            float var11 = var0.getFloat();
            IsoGridSquare var12 = IsoWorld.instance.CurrentCell.getGridSquare((double)var9, (double)var10, (double)var11);
            if (var12 == null) {
               DebugLog.Multiplayer.error("ReceiveEatBody: incorrect square");
               return;
            }

            if (var8 >= 0 && var8 < var12.getStaticMovingObjects().size()) {
               IsoDeadBody var13 = (IsoDeadBody)var12.getStaticMovingObjects().get(var8);
               if (var13 != null) {
                  var4.setTarget((IsoMovingObject)null);
                  var4.setEatBodyTarget(var13, true, var6);
                  var4.setVariable("onknees", var5);
                  var4.setStateEventDelayTimer(var7);
               } else {
                  DebugLog.Multiplayer.error("ReceiveEatBody: no corpse with index " + var8 + " on square");
               }
            } else {
               DebugLog.Multiplayer.error("ReceiveEatBody: no corpse on square");
            }
         } else if (var3 == 2) {
            var5 = var0.get() != 0;
            var6 = var0.getFloat();
            var7 = var0.getFloat();
            short var15 = var0.getShort();
            IsoPlayer var16 = (IsoPlayer)IDToPlayerMap.get(var15);
            if (var16 == null) {
               DebugLog.Multiplayer.error("ReceiveEatBody: player " + var15 + " not found");
               return;
            }

            var4.setTarget((IsoMovingObject)null);
            var4.setEatBodyTarget(var16, true, var6);
            var4.setVariable("onknees", var5);
            var4.setStateEventDelayTimer(var7);
         } else {
            var4.setEatBodyTarget((IsoMovingObject)null, false);
         }
      } catch (Exception var14) {
         DebugLog.Multiplayer.printException(var14, "ReceiveEatBody: failed", LogSeverity.Error);
      }

   }

   public static void sendThump(IsoGameCharacter var0, Thumpable var1) {
      ByteBufferWriter var2 = connection.startPacket();
      PacketTypes.PacketType.Thump.doPacket(var2);

      try {
         short var3 = var0.getOnlineID();
         String var4 = var0.getVariableString("ThumpType");
         var2.putShort(var3);
         var2.putByte((byte)NetworkVariables.ThumpType.fromString(var4).ordinal());
         if (var1 instanceof IsoObject var5) {
            var2.putInt(var5.getObjectIndex());
            var2.putFloat((float)var5.getSquare().getX());
            var2.putFloat((float)var5.getSquare().getY());
            var2.putFloat((float)var5.getSquare().getZ());
         } else {
            var2.putInt(-1);
         }

         if (Core.bDebug) {
            DebugLog.log(DebugType.Multiplayer, String.format("SendThump: zombie=%d type=%s target=%s", var3, var4, var1 == null ? "null" : var1.getClass().getSimpleName()));
         }

         PacketTypes.PacketType.Thump.send(connection);
      } catch (Exception var6) {
         DebugLog.Multiplayer.printException(var6, "SendThump: failed", LogSeverity.Error);
         connection.cancelPacket();
      }

   }

   public static void receiveSyncRadioData(ByteBuffer var0, short var1) {
      short var2 = var0.getShort();
      VoiceManagerData var3 = VoiceManagerData.get(var2);
      synchronized(var3.radioData) {
         var3.isCanHearAll = var0.get() == 1;
         short var5 = (short)var0.getInt();
         var3.radioData.clear();

         for(int var6 = 0; var6 < var5 / 4; ++var6) {
            int var7 = var0.getInt();
            int var8 = var0.getInt();
            int var9 = var0.getInt();
            int var10 = var0.getInt();
            var3.radioData.add(new VoiceManagerData.RadioData(var7, (float)var8, (float)var9, (float)var10));
         }

      }
   }

   public static void receiveThump(ByteBuffer var0, short var1) {
      try {
         short var2 = var0.getShort();
         String var3 = NetworkVariables.ThumpType.fromByte(var0.get()).toString();
         if (Core.bDebug) {
            DebugLog.log(DebugType.Multiplayer, String.format("ReceiveThump: zombie=%d type=%s", var2, var3));
         }

         IsoZombie var4 = (IsoZombie)IDToZombieMap.get(var2);
         if (var4 == null) {
            DebugLog.Multiplayer.error("ReceiveThump: zombie " + var2 + " not found");
            return;
         }

         var4.setVariable("ThumpType", var3);
         int var5 = var0.getInt();
         if (var5 == -1) {
            var4.setThumpTarget((Thumpable)null);
            return;
         }

         float var6 = var0.getFloat();
         float var7 = var0.getFloat();
         float var8 = var0.getFloat();
         IsoGridSquare var9 = IsoWorld.instance.CurrentCell.getGridSquare((double)var6, (double)var7, (double)var8);
         if (var9 == null) {
            DebugLog.Multiplayer.error("ReceiveThump: incorrect square");
            return;
         }

         IsoObject var10 = (IsoObject)var9.getObjects().get(var5);
         if (var10 instanceof Thumpable) {
            var4.setThumpTarget(var10);
         } else {
            DebugLog.Multiplayer.error("ReceiveThump: no thumpable with index " + var5 + " on square");
         }
      } catch (Exception var11) {
         DebugLog.Multiplayer.printException(var11, "ReceiveThump: failed", LogSeverity.Error);
      }

   }

   public void sendWorldSound(WorldSoundManager.WorldSound var1) {
      INetworkPacket.send(PacketTypes.PacketType.WorldSoundPacket, var1);
   }

   public void sendLoginQueueRequest() {
      ByteBufferWriter var1 = connection.startPacket();
      PacketTypes.PacketType.LoginQueueRequest.doPacket(var1);
      PacketTypes.PacketType.LoginQueueRequest.send(connection);
      ConnectionManager.log("send-packet", "login-queue-request", connection);
   }

   public void sendLoginQueueDone(long var1) {
      INetworkPacket.send(PacketTypes.PacketType.LoginQueueDone, var1);
      ConnectionManager.log("send-packet", "login-queue-done", connection);
   }

   public static boolean canSeePlayerStats() {
      return connection.role.haveCapability(Capability.CanSeePlayersStats);
   }

   public void sendPersonalColor(IsoPlayer var1) {
      ByteBufferWriter var2 = connection.startPacket();
      PacketTypes.PacketType.ChangeTextColor.doPacket(var2);
      var2.putShort((short)var1.getPlayerNum());
      var2.putFloat(Core.getInstance().getMpTextColor().r);
      var2.putFloat(Core.getInstance().getMpTextColor().g);
      var2.putFloat(Core.getInstance().getMpTextColor().b);
      PacketTypes.PacketType.ChangeTextColor.send(connection);
   }

   public void sendChangedPlayerStats(IsoPlayer var1) {
      ByteBufferWriter var2 = connection.startPacket();
      PacketTypes.PacketType.ChangePlayerStats.doPacket(var2);
      var1.createPlayerStats(var2, username);
      PacketTypes.PacketType.ChangePlayerStats.send(connection);
   }

   static void receiveChangePlayerStats(ByteBuffer var0, short var1) {
      short var2 = var0.getShort();
      IsoPlayer var3 = (IsoPlayer)IDToPlayerMap.get(var2);
      if (var3 != null) {
         String var4 = GameWindow.ReadString(var0);
         var3.setPlayerStats(var0, var4);
         allChatMuted = var3.isAllChatMuted();
      }
   }

   public void sendPlayerConnect(IsoPlayer var1) {
      var1.setOnlineID((short)-1);
      connection.username = var1.username;
      INetworkPacket.send(PacketTypes.PacketType.PlayerConnect, var1);
      allChatMuted = var1.isAllChatMuted();
      sendPerks(var1);
      var1.updateEquippedRadioFreq();
      this.bPlayerConnectSent = true;
      ConnectionManager.log("send-packet", "player-connect", connection);
   }

   public static void sendCreatePlayer(byte var0) {
      INetworkPacket.send(PacketTypes.PacketType.CreatePlayer, var0);
   }

   public void sendPlayer2(IsoPlayer var1) {
      if (bClient && var1.isLocalPlayer()) {
         if (var1.networkAI.isReliable()) {
            this.sendPlayer(var1);
         }

         PlayerPacket var2 = var1.networkAI.playerPacket;
         var2.set(var1);
         if (var2.type >= 2) {
            var2.sendToServer(PacketTypes.PacketType.PlayerUpdateReliable);
         } else if (var2.type >= 1) {
            var2.sendToServer(PacketTypes.PacketType.PlayerUpdateReliable);
         }

      }
   }

   public void sendPlayer(IsoPlayer var1) {
      var1.networkAI.needToUpdate();
   }

   public void heartBeat() {
      ++count;
   }

   public static IsoZombie getZombie(short var0) {
      return (IsoZombie)IDToZombieMap.get(var0);
   }

   public static void sendPlayerExtraInfo(IsoPlayer var0) {
      INetworkPacket.send(PacketTypes.PacketType.ExtraInfo, var0);
   }

   public void setResetID(int var1) {
      this.ResetID = 0;
      this.loadResetID();
      if (this.ResetID != var1) {
         ArrayList var2 = new ArrayList();
         var2.add("map_symbols.bin");
         var2.add("map_visited.bin");
         var2.add("recorded_media.bin");

         String var10002;
         int var3;
         File var4;
         File var5;
         for(var3 = 0; var3 < var2.size(); ++var3) {
            try {
               var4 = ZomboidFileSystem.instance.getFileInCurrentSave((String)var2.get(var3));
               if (var4.exists()) {
                  var10002 = ZomboidFileSystem.instance.getCacheDir();
                  var5 = new File(var10002 + File.separator + (String)var2.get(var3));
                  if (var5.exists()) {
                     var5.delete();
                  }

                  var4.renameTo(var5);
               }
            } catch (Exception var7) {
               ExceptionLogger.logException(var7);
            }
         }

         DebugLog.log("server was reset, deleting " + Core.GameMode + File.separator + Core.GameSaveWorld);
         LuaManager.GlobalObject.deleteSave(Core.GameMode + File.separator + Core.GameSaveWorld);
         LuaManager.GlobalObject.createWorld(Core.GameSaveWorld);

         for(var3 = 0; var3 < var2.size(); ++var3) {
            try {
               var4 = ZomboidFileSystem.instance.getFileInCurrentSave((String)var2.get(var3));
               var10002 = ZomboidFileSystem.instance.getCacheDir();
               var5 = new File(var10002 + File.separator + (String)var2.get(var3));
               if (var5.exists()) {
                  var5.renameTo(var4);
               }
            } catch (Exception var6) {
               ExceptionLogger.logException(var6);
            }
         }
      }

      this.ResetID = var1;
      this.saveResetID();
   }

   public void loadResetID() {
      File var1 = ZomboidFileSystem.instance.getFileInCurrentSave("serverid.dat");
      if (var1.exists()) {
         FileInputStream var2 = null;

         try {
            var2 = new FileInputStream(var1);
         } catch (FileNotFoundException var7) {
            var7.printStackTrace();
         }

         DataInputStream var3 = new DataInputStream(var2);

         try {
            this.ResetID = var3.readInt();
         } catch (IOException var6) {
            var6.printStackTrace();
         }

         try {
            var2.close();
         } catch (IOException var5) {
            var5.printStackTrace();
         }
      }

   }

   private void saveResetID() {
      File var1 = ZomboidFileSystem.instance.getFileInCurrentSave("serverid.dat");
      FileOutputStream var2 = null;

      try {
         var2 = new FileOutputStream(var1);
      } catch (FileNotFoundException var7) {
         var7.printStackTrace();
      }

      DataOutputStream var3 = new DataOutputStream(var2);

      try {
         var3.writeInt(this.ResetID);
      } catch (IOException var6) {
         var6.printStackTrace();
      }

      try {
         var2.close();
      } catch (IOException var5) {
         var5.printStackTrace();
      }

   }

   static void receiveScoreboardUpdate(ByteBuffer var0, short var1) {
      int var2 = var0.getInt();
      instance.connectedPlayers = new ArrayList();
      ArrayList var3 = new ArrayList();
      ArrayList var4 = new ArrayList();
      ArrayList var5 = new ArrayList();

      for(int var6 = 0; var6 < var2; ++var6) {
         String var7 = GameWindow.ReadString(var0);
         String var8 = GameWindow.ReadString(var0);
         var3.add(var7);
         var4.add(var8);
         instance.connectedPlayers.add(instance.getPlayerFromUsername(var7));
         if (SteamUtils.isSteamModeEnabled()) {
            String var9 = SteamUtils.convertSteamIDToString(var0.getLong());
            var5.add(var9);
         }
      }

      LuaEventManager.triggerEvent("OnScoreboardUpdate", var3, var4, var5);
   }

   public ArrayList<IsoPlayer> getPlayers() {
      if (!this.idMapDirty) {
         return this.players;
      } else {
         this.players.clear();
         this.players.addAll(IDToPlayerMap.values());
         this.idMapDirty = false;
         return this.players;
      }
   }

   public void sendWeaponHit(IsoPlayer var1, HandWeapon var2, IsoObject var3) {
      if (var1 != null && var3 != null && var1.isLocalPlayer()) {
         ByteBufferWriter var4 = connection.startPacket();
         PacketTypes.PacketType.WeaponHit.doPacket(var4);
         var4.putInt(var3.square.x);
         var4.putInt(var3.square.y);
         var4.putInt(var3.square.z);
         var4.putByte((byte)var3.getObjectIndex());
         var4.putShort((short)var1.getPlayerNum());
         var4.putUTF(var2 != null ? var2.getFullType() : "");
         PacketTypes.PacketType.WeaponHit.send(connection);
      }
   }

   static void receiveSyncIsoObject(ByteBuffer var0, short var1) {
      if (DebugOptions.instance.Network.Client.SyncIsoObject.getValue()) {
         int var2 = var0.getInt();
         int var3 = var0.getInt();
         int var4 = var0.getInt();
         byte var5 = var0.get();
         byte var6 = var0.get();
         byte var7 = var0.get();
         if (var6 != 2) {
         }

         if (var6 == 1) {
            IsoGridSquare var8 = IsoWorld.instance.CurrentCell.getGridSquare(var2, var3, var4);
            if (var8 == null) {
               return;
            }

            if (var5 >= 0 && var5 < var8.getObjects().size()) {
               ((IsoObject)var8.getObjects().get(var5)).syncIsoObject(true, var7, (UdpConnection)null, var0);
            } else {
               DebugLog.Network.warn("SyncIsoObject: index=" + var5 + " is invalid x,y,z=" + var2 + "," + var3 + "," + var4);
            }
         }

      }
   }

   static void skipPacket(ByteBuffer var0, short var1) {
   }

   public static void receivePlayerTimeout(short var0) {
      WorldMapRemotePlayers.instance.removePlayerByID(var0);
      positions.remove(var0);
      IsoPlayer var1 = (IsoPlayer)IDToPlayerMap.get(var0);
      if (var1 != null) {
         DebugLog.DetailedInfo.trace("Received timeout for player " + var1.username + " id " + var1.OnlineID);
         NetworkZombieSimulator.getInstance().clearTargetAuth(var1);
         if (var1.getVehicle() != null) {
            int var2 = var1.getVehicle().getSeat(var1);
            if (var2 != -1) {
               var1.getVehicle().clearPassenger(var2);
            }

            VehicleManager.instance.sendRequestGetPosition(var1.getVehicle().VehicleID, PacketTypes.PacketType.Vehicles);
         }

         var1.removeFromWorld();
         var1.removeFromSquare();
         IDToPlayerMap.remove(var1.OnlineID);
         instance.idMapDirty = true;
         LuaEventManager.triggerEvent("OnMiniScoreboardUpdate");
      }
   }

   public void disconnect(boolean var1) {
      if (var1) {
         this.resetDisconnectTimer();
      }

      this.bConnected = false;
      if (IsoPlayer.getInstance() != null) {
         IsoPlayer.getInstance().setOnlineID((short)-1);
      }

   }

   public void resetDisconnectTimer() {
      this.disconnectTime = System.currentTimeMillis();
   }

   public String getReconnectCountdownTimer() {
      return String.valueOf((int)Math.ceil((double)((10000L - System.currentTimeMillis() + this.disconnectTime) / 1000L)));
   }

   public boolean canConnect() {
      return System.currentTimeMillis() - this.disconnectTime > 10000L;
   }

   public void addIncoming(short var1, ByteBuffer var2) {
      if (connection != null) {
         if (var1 == PacketTypes.PacketType.SentChunk.getId()) {
            WorldStreamer.instance.receiveChunkPart(var2);
         } else if (var1 == PacketTypes.PacketType.NotRequiredInZip.getId()) {
            WorldStreamer.instance.receiveNotRequired(var2);
         } else if (var1 == PacketTypes.PacketType.LoadPlayerProfile.getId()) {
            LoadPlayerProfilePacket var4 = new LoadPlayerProfilePacket();
            var4.parse(var2, connection);
            var4.processClient(connection);
         } else {
            ZomboidNetData var3 = null;
            if (var2.remaining() > 2048) {
               var3 = ZomboidNetDataPool.instance.getLong(var2.remaining());
            } else {
               var3 = ZomboidNetDataPool.instance.get();
            }

            var3.read(var1, var2, connection);
            var3.time = System.currentTimeMillis();
            MainLoopNetDataQ.add(var3);
         }
      }
   }

   public void doDisconnect(String var1) {
      if (connection != null) {
         connection.forceDisconnect(var1);
         this.bConnected = false;
         connection = null;
         bClient = false;
      } else {
         instance.Shutdown();
      }

   }

   public void removeZombieFromCache(IsoZombie var1) {
      if (IDToZombieMap.containsKey(var1.OnlineID)) {
         IDToZombieMap.remove(var1.OnlineID);
      }

   }

   public void sendWorldMessage(String var1) {
      ChatManager.getInstance().showInfoMessage(var1);
   }

   private void convertGameSaveWorldDirectory(String var1, String var2) {
      File var3 = new File(var1);
      if (var3.isDirectory()) {
         File var4 = new File(var2);
         boolean var5 = var3.renameTo(var4);
         if (var5) {
            DebugLog.log("CONVERT: The GameSaveWorld directory was renamed from " + var1 + " to " + var2);
         } else {
            DebugLog.log("ERROR: The GameSaveWorld directory cannot rename from " + var1 + " to " + var2);
         }

      }
   }

   public void doConnect(String var1, String var2, String var3, String var4, String var5, String var6, String var7, boolean var8, int var9) {
      this.doConnect(var1, var2, var3, var4, var5, var6, var7, var8, var9, "");
   }

   public void doConnect(String var1, String var2, String var3, String var4, String var5, String var6, String var7, boolean var8, int var9, String var10) {
      username = var1.trim();
      password = var2.trim();
      ip = var3.trim();
      localIP = var4.trim();
      port = Integer.parseInt(var5.trim());
      serverPassword = var6.trim();
      ServerName = var7.trim();
      useSteamRelay = var8;
      authType = var9;
      googleKey = var10;
      instance.init();
      String var10000 = ip;
      Core.GameSaveWorld = var10000 + "_" + port + "_" + ServerWorldDatabase.encrypt(var1);
      this.convertGameSaveWorldDirectory(ZomboidFileSystem.instance.getGameModeCacheDir() + File.separator + ip + "_" + port + "_" + var1, ZomboidFileSystem.instance.getCurrentSaveDir());
      if (CoopMaster.instance != null && CoopMaster.instance.isRunning()) {
         Core.GameSaveWorld = CoopMaster.instance.getPlayerSaveFolder(CoopMaster.instance.getServerName());
      }

   }

   public void doConnectCoop(String var1) {
      username = SteamFriends.GetPersonaName();
      password = "";
      ip = var1;
      localIP = "";
      port = 0;
      serverPassword = "";
      this.init();
      if (CoopMaster.instance != null && CoopMaster.instance.isRunning()) {
         Core.GameSaveWorld = CoopMaster.instance.getPlayerSaveFolder(CoopMaster.instance.getServerName());
      }

   }

   public void scoreboardUpdate() {
      ByteBufferWriter var1 = connection.startPacket();
      PacketTypes.PacketType.ScoreboardUpdate.doPacket(var1);
      PacketTypes.PacketType.ScoreboardUpdate.send(connection);
   }

   static void receiveAddAmbient(ByteBuffer var0, short var1) {
      String var2 = GameWindow.ReadString(var0);
      int var3 = var0.getInt();
      int var4 = var0.getInt();
      int var5 = var0.getInt();
      float var6 = var0.getFloat();
      DebugLog.log(DebugType.Sound, "ambient: received " + var2 + " at " + var3 + "," + var4 + " radius=" + var5);
      AmbientStreamManager.instance.addAmbient(var2, var3, var4, var5, var6);
   }

   public void sendClientCommand(IsoPlayer var1, String var2, String var3, KahluaTable var4) {
      ByteBufferWriter var5 = connection.startPacket();
      PacketTypes.PacketType.ClientCommand.doPacket(var5);
      var5.putByte((byte)(var1 != null ? var1.PlayerIndex : -1));
      var5.putUTF(var2);
      var5.putUTF(var3);
      if (var4 != null && !var4.isEmpty()) {
         var5.putByte((byte)1);

         try {
            KahluaTableIterator var6 = var4.iterator();

            while(var6.advance()) {
               if (!TableNetworkUtils.canSave(var6.getKey(), var6.getValue())) {
                  Object var10000 = var6.getKey();
                  DebugLog.log("ERROR: sendClientCommand: can't save key,value=" + var10000 + "," + var6.getValue());
               }
            }

            TableNetworkUtils.save(var4, var5.bb);
         } catch (IOException var7) {
            var7.printStackTrace();
         }
      } else {
         var5.putByte((byte)0);
      }

      PacketTypes.PacketType.ClientCommand.send(connection);
   }

   public void sendClientCommandV(IsoPlayer var1, String var2, String var3, Object... var4) {
      if (var4.length == 0) {
         this.sendClientCommand(var1, var2, var3, (KahluaTable)null);
      } else if (var4.length % 2 != 0) {
         DebugLog.log("ERROR: sendClientCommand called with wrong number of arguments (" + var2 + " " + var3 + ")");
      } else {
         KahluaTable var5 = LuaManager.platform.newTable();

         for(int var6 = 0; var6 < var4.length; var6 += 2) {
            Object var7 = var4[var6 + 1];
            if (var7 instanceof Float) {
               var5.rawset(var4[var6], ((Float)var7).doubleValue());
            } else if (var7 instanceof Integer) {
               var5.rawset(var4[var6], ((Integer)var7).doubleValue());
            } else if (var7 instanceof Short) {
               var5.rawset(var4[var6], ((Short)var7).doubleValue());
            } else {
               var5.rawset(var4[var6], var7);
            }
         }

         this.sendClientCommand(var1, var2, var3, var5);
      }
   }

   public void sendAttachedItem(IsoPlayer var1, String var2, InventoryItem var3) {
      INetworkPacket.send(PacketTypes.PacketType.PlayerAttachedItem, var1, var2, var3);
   }

   public void sendVisual(IsoPlayer var1) {
      if (var1 != null && var1.OnlineID != -1) {
         INetworkPacket.send(PacketTypes.PacketType.HumanVisual, var1);
      }
   }

   static void receiveBloodSplatter(ByteBuffer var0, short var1) {
      String var2 = GameWindow.ReadString(var0);
      float var3 = var0.getFloat();
      float var4 = var0.getFloat();
      float var5 = var0.getFloat();
      float var6 = var0.getFloat();
      float var7 = var0.getFloat();
      boolean var8 = var0.get() == 1;
      boolean var9 = var0.get() == 1;
      byte var10 = var0.get();
      IsoCell var11 = IsoWorld.instance.CurrentCell;
      IsoGridSquare var12 = var11.getGridSquare((double)var3, (double)var4, (double)var5);
      if (var12 == null) {
         instance.delayPacket(PZMath.fastfloor(var3), PZMath.fastfloor(var4), PZMath.fastfloor(var5));
      } else {
         int var13;
         int var14;
         if (var9 && SandboxOptions.instance.BloodLevel.getValue() > 1) {
            for(var13 = -1; var13 <= 1; ++var13) {
               for(var14 = -1; var14 <= 1; ++var14) {
                  if (var13 != 0 || var14 != 0) {
                     new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, var11, var3, var4, var5, (float)var13 * Rand.Next(0.25F, 0.5F), (float)var14 * Rand.Next(0.25F, 0.5F));
                  }
               }
            }

            new IsoZombieGiblets(IsoZombieGiblets.GibletType.Eye, var11, var3, var4, var5, var6 * 0.8F, var7 * 0.8F);
         } else {
            if (SandboxOptions.instance.BloodLevel.getValue() > 1) {
               for(var13 = 0; var13 < var10; ++var13) {
                  var12.splatBlood(3, 0.3F);
               }

               var12.getChunk().addBloodSplat(var3, var4, (float)PZMath.fastfloor(var5), Rand.Next(20));
               new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, var11, var3, var4, var5, var6 * 1.5F, var7 * 1.5F);
            }

            byte var17 = 3;
            var14 = 0;
            byte var15 = 1;
            switch (SandboxOptions.instance.BloodLevel.getValue()) {
               case 1:
                  var15 = 0;
                  break;
               case 2:
                  var15 = 1;
                  var17 = 5;
                  var14 = 2;
               case 3:
               default:
                  break;
               case 4:
                  var15 = 3;
                  var17 = 2;
                  break;
               case 5:
                  var15 = 10;
                  var17 = 0;
            }

            for(int var16 = 0; var16 < var15; ++var16) {
               if (Rand.Next(var8 ? 8 : var17) == 0) {
                  new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, var11, var3, var4, var5, var6 * 1.5F, var7 * 1.5F);
               }

               if (Rand.Next(var8 ? 8 : var17) == 0) {
                  new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, var11, var3, var4, var5, var6 * 1.5F, var7 * 1.5F);
               }

               if (Rand.Next(var8 ? 8 : var17) == 0) {
                  new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, var11, var3, var4, var5, var6 * 1.8F, var7 * 1.8F);
               }

               if (Rand.Next(var8 ? 8 : var17) == 0) {
                  new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, var11, var3, var4, var5, var6 * 1.9F, var7 * 1.9F);
               }

               if (Rand.Next(var8 ? 4 : var14) == 0) {
                  new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, var11, var3, var4, var5, var6 * 3.5F, var7 * 3.5F);
               }

               if (Rand.Next(var8 ? 4 : var14) == 0) {
                  new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, var11, var3, var4, var5, var6 * 3.8F, var7 * 3.8F);
               }

               if (Rand.Next(var8 ? 4 : var14) == 0) {
                  new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, var11, var3, var4, var5, var6 * 3.9F, var7 * 3.9F);
               }

               if (Rand.Next(var8 ? 4 : var14) == 0) {
                  new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, var11, var3, var4, var5, var6 * 1.5F, var7 * 1.5F);
               }

               if (Rand.Next(var8 ? 4 : var14) == 0) {
                  new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, var11, var3, var4, var5, var6 * 3.8F, var7 * 3.8F);
               }

               if (Rand.Next(var8 ? 4 : var14) == 0) {
                  new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, var11, var3, var4, var5, var6 * 3.9F, var7 * 3.9F);
               }

               if (Rand.Next(var8 ? 9 : 6) == 0) {
                  new IsoZombieGiblets(IsoZombieGiblets.GibletType.Eye, var11, var3, var4, var5, var6 * 0.8F, var7 * 0.8F);
               }
            }

         }
      }
   }

   static void receiveZombieSound(ByteBuffer var0, short var1) {
      short var2 = var0.getShort();
      byte var3 = var0.get();
      IsoZombie.ZombieSound var4 = IsoZombie.ZombieSound.fromIndex(var3);
      DebugLog.log(DebugType.Sound, "sound: received " + var3 + " for zombie " + var2);
      IsoZombie var5 = (IsoZombie)IDToZombieMap.get(var2);
      if (var5 != null && var5.getCurrentSquare() != null) {
         float var6 = (float)var4.radius();
         String var7;
         switch (var4) {
            case Burned:
               var7 = var5.getDescriptor().getVoicePrefix() + "Death";
               var5.getEmitter().playVocals(var7);
               break;
            case DeadCloseKilled:
               var5.getEmitter().playSoundImpl("HeadStab", (IsoObject)null);
               var7 = var5.getDescriptor().getVoicePrefix() + "Death";
               var5.getEmitter().playVocals(var7);
               var5.getEmitter().tick();
               break;
            case DeadNotCloseKilled:
               if (var5.isKilledBySlicingWeapon()) {
                  var5.getEmitter().playSoundImpl("HeadSlice", (IsoObject)null);
               } else {
                  var5.getEmitter().playSoundImpl("HeadSmash", (IsoObject)null);
               }

               var7 = var5.getDescriptor().getVoicePrefix() + "Death";
               var5.getEmitter().playVocals(var7);
               var5.getEmitter().tick();
               break;
            case Hurt:
               var5.playHurtSound();
               break;
            case Idle:
               var7 = var5.getDescriptor().getVoicePrefix() + "Idle";
               var5.getEmitter().playVocals(var7);
               break;
            case Lunge:
               var7 = var5.getDescriptor().getVoicePrefix() + "Attack";
               var5.getEmitter().playVocals(var7);
               break;
            default:
               DebugLog.log("unhandled zombie sound " + var4);
         }
      }

   }

   public void eatFood(IsoPlayer var1, Food var2, float var3) {
      EatFoodPacket var4 = new EatFoodPacket();
      var4.set(var1, var2, var3);
      ByteBufferWriter var5 = connection.startPacket();
      PacketTypes.PacketType.EatFood.doPacket(var5);
      var4.write(var5);
      PacketTypes.PacketType.EatFood.send(connection);
   }

   public void drink(IsoPlayer var1, float var2) {
      ByteBufferWriter var3 = connection.startPacket();
      PacketTypes.PacketType.Drink.doPacket(var3);
      var3.putByte((byte)var1.PlayerIndex);
      var3.putFloat(var2);
      PacketTypes.PacketType.Drink.send(connection);
   }

   public void addToItemSendBuffer(IsoObject var1, ItemContainer var2, InventoryItem var3) {
      if (var1 instanceof IsoWorldInventoryObject) {
         InventoryItem var4 = ((IsoWorldInventoryObject)var1).getItem();
         if (var3 == null || var4 == null || !(var4 instanceof InventoryContainer) || var2 != ((InventoryContainer)var4).getInventory()) {
            DebugLog.log("ERROR: addToItemSendBuffer parent=" + var1 + " item=" + var3);
            if (Core.bDebug) {
               throw new IllegalStateException();
            } else {
               return;
            }
         }
      } else if (var1 instanceof BaseVehicle) {
         if (var2.vehiclePart == null || var2.vehiclePart.getItemContainer() != var2 || var2.vehiclePart.getVehicle() != var1) {
            DebugLog.log("ERROR: addToItemSendBuffer parent=" + var1 + " item=" + var3);
            if (Core.bDebug) {
               throw new IllegalStateException();
            }

            return;
         }
      } else if (var1 == null || var3 == null || var1.getContainerIndex(var2) == -1) {
         DebugLog.log("ERROR: addToItemSendBuffer parent=" + var1 + " item=" + var3);
         if (Core.bDebug) {
            throw new IllegalStateException();
         }

         return;
      }

      ArrayList var5;
      if (this.itemsToSendRemove.containsKey(var2)) {
         var5 = (ArrayList)this.itemsToSendRemove.get(var2);
         if (var5.remove(var3)) {
            if (var5.isEmpty()) {
               this.itemsToSendRemove.remove(var2);
            }

            return;
         }
      }

      if (this.itemsToSend.containsKey(var2)) {
         ((ArrayList)this.itemsToSend.get(var2)).add(var3);
      } else {
         var5 = new ArrayList();
         this.itemsToSend.put(var2, var5);
         var5.add(var3);
      }

   }

   public void addToItemRemoveSendBuffer(IsoObject var1, ItemContainer var2, InventoryItem var3) {
      if (var1 instanceof IsoWorldInventoryObject) {
         InventoryItem var4 = ((IsoWorldInventoryObject)var1).getItem();
         if (var3 == null || var4 == null || !(var4 instanceof InventoryContainer) || var2 != ((InventoryContainer)var4).getInventory()) {
            DebugLog.log("ERROR: addToItemRemoveSendBuffer parent=" + var1 + " item=" + var3);
            if (Core.bDebug) {
               throw new IllegalStateException();
            }

            return;
         }
      } else if (var1 instanceof BaseVehicle) {
         if (var2.vehiclePart == null || var2.vehiclePart.getItemContainer() != var2 || var2.vehiclePart.getVehicle() != var1) {
            DebugLog.log("ERROR: addToItemRemoveSendBuffer parent=" + var1 + " item=" + var3);
            if (Core.bDebug) {
               throw new IllegalStateException();
            }

            return;
         }
      } else if (var1 instanceof IsoDeadBody) {
         if (var3 == null || var2 != var1.getContainer()) {
            DebugLog.log("ERROR: addToItemRemoveSendBuffer parent=" + var1 + " item=" + var3);
            if (Core.bDebug) {
               throw new IllegalStateException();
            } else {
               return;
            }
         }
      } else if (var1 == null || var3 == null || var1.getContainerIndex(var2) == -1) {
         DebugLog.log("ERROR: addToItemRemoveSendBuffer parent=" + var1 + " item=" + var3);
         if (Core.bDebug) {
            throw new IllegalStateException();
         }

         return;
      }

      if (!SystemDisabler.doWorldSyncEnable) {
         ArrayList var5;
         if (this.itemsToSend.containsKey(var2)) {
            var5 = (ArrayList)this.itemsToSend.get(var2);
            if (var5.remove(var3)) {
               if (var5.isEmpty()) {
                  this.itemsToSend.remove(var2);
               }

               return;
            }
         }

         if (this.itemsToSendRemove.containsKey(var2)) {
            ((ArrayList)this.itemsToSendRemove.get(var2)).add(var3);
         } else {
            var5 = new ArrayList();
            var5.add(var3);
            this.itemsToSendRemove.put(var2, var5);
         }

      } else {
         INetworkPacket.send(PacketTypes.PacketType.RemoveInventoryItemFromContainer, var2, var3);
      }
   }

   public void sendAddedRemovedItems(boolean var1) {
      boolean var2 = var1 || this.itemSendFrequency.Check();
      Object var10000;
      Iterator var3;
      Map.Entry var4;
      ItemContainer var5;
      ArrayList var6;
      Object var7;
      int var9;
      if (!SystemDisabler.doWorldSyncEnable && !this.itemsToSendRemove.isEmpty() && var2) {
         var3 = this.itemsToSendRemove.entrySet().iterator();

         label123:
         while(true) {
            do {
               do {
                  if (!var3.hasNext()) {
                     this.itemsToSendRemove.clear();
                     break label123;
                  }

                  var4 = (Map.Entry)var3.next();
                  var5 = (ItemContainer)var4.getKey();
                  var6 = (ArrayList)var4.getValue();
                  var7 = var5.getParent();
                  if (var5.getContainingItem() != null && var5.getContainingItem().getWorldItem() != null) {
                     var7 = var5.getContainingItem().getWorldItem();
                  }
               } while(var7 == null);
            } while(((IsoObject)var7).square == null);

            try {
               INetworkPacket.send(PacketTypes.PacketType.RemoveInventoryItemFromContainer, var5, var6);
            } catch (Exception var11) {
               DebugLog.log("sendAddedRemovedItems: itemsToSendRemove container:" + var5 + "." + var7 + " items:" + var6);
               if (var6 != null) {
                  for(var9 = 0; var9 < var6.size(); ++var9) {
                     if (var6.get(var9) == null) {
                        DebugLog.log("item:null");
                     } else {
                        var10000 = var6.get(var9);
                        DebugLog.log("item:" + ((InventoryItem)var10000).getName());
                     }
                  }

                  DebugLog.log("itemSize:" + var6.size());
               }

               var11.printStackTrace();
               connection.cancelPacket();
            }
         }
      }

      if (!this.itemsToSend.isEmpty() && var2) {
         var3 = this.itemsToSend.entrySet().iterator();

         while(true) {
            do {
               do {
                  if (!var3.hasNext()) {
                     this.itemsToSend.clear();
                     return;
                  }

                  var4 = (Map.Entry)var3.next();
                  var5 = (ItemContainer)var4.getKey();
                  var6 = (ArrayList)var4.getValue();
                  var7 = var5.getParent();
                  if (var5.getContainingItem() != null && var5.getContainingItem().getWorldItem() != null) {
                     var7 = var5.getContainingItem().getWorldItem();
                  }
               } while(var7 == null);
            } while(((IsoObject)var7).square == null);

            try {
               INetworkPacket.send(PacketTypes.PacketType.AddInventoryItemToContainer, var5, var6);
            } catch (Exception var10) {
               DebugLog.log("sendAddedRemovedItems: itemsToSend container:" + var5 + "." + var7 + " items:" + var6);
               if (var6 != null) {
                  for(var9 = 0; var9 < var6.size(); ++var9) {
                     if (var6.get(var9) == null) {
                        DebugLog.log("item:null");
                     } else {
                        var10000 = var6.get(var9);
                        DebugLog.log("item:" + ((InventoryItem)var10000).getName());
                     }
                  }

                  DebugLog.log("itemSize:" + var6.size());
               }

               var10.printStackTrace();
               connection.cancelPacket();
            }
         }
      }
   }

   public void checkAddedRemovedItems(IsoObject var1) {
      if (var1 != null) {
         if (!this.itemsToSend.isEmpty() || !this.itemsToSendRemove.isEmpty()) {
            if (var1 instanceof IsoDeadBody) {
               if (this.itemsToSend.containsKey(var1.getContainer()) || this.itemsToSendRemove.containsKey(var1.getContainer())) {
                  this.sendAddedRemovedItems(true);
               }

            } else {
               ItemContainer var3;
               if (var1 instanceof IsoWorldInventoryObject) {
                  InventoryItem var4 = ((IsoWorldInventoryObject)var1).getItem();
                  if (var4 instanceof InventoryContainer) {
                     var3 = ((InventoryContainer)var4).getInventory();
                     if (this.itemsToSend.containsKey(var3) || this.itemsToSendRemove.containsKey(var3)) {
                        this.sendAddedRemovedItems(true);
                     }
                  }

               } else if (!(var1 instanceof BaseVehicle)) {
                  for(int var2 = 0; var2 < var1.getContainerCount(); ++var2) {
                     var3 = var1.getContainerByIndex(var2);
                     if (this.itemsToSend.containsKey(var3) || this.itemsToSendRemove.containsKey(var3)) {
                        this.sendAddedRemovedItems(true);
                        return;
                     }
                  }

               }
            }
         }
      }
   }

   public void sendItemStats(InventoryItem var1) {
      if (var1 != null) {
         if (var1.getWorldItem() != null && var1.getWorldItem().getWorldObjectIndex() != -1) {
            IsoWorldInventoryObject var6 = var1.getWorldItem();
            INetworkPacket.send(PacketTypes.PacketType.ItemStats, var1.getContainer(), var1);
         } else if (var1.getContainer() == null) {
            DebugLog.log("ERROR: sendItemStats(): item is neither in a container nor on the ground");
            if (Core.bDebug) {
               throw new IllegalStateException();
            }
         } else {
            ItemContainer var2 = var1.getContainer();
            Object var3 = var2.getParent();
            if (var2.getContainingItem() != null && var2.getContainingItem().getWorldItem() != null) {
               var3 = var2.getContainingItem().getWorldItem();
            }

            if (var3 instanceof IsoWorldInventoryObject) {
               InventoryItem var5 = ((IsoWorldInventoryObject)var3).getItem();
               if (!(var5 instanceof InventoryContainer) || var2 != ((InventoryContainer)var5).getInventory()) {
                  DebugLog.log("ERROR: sendItemStats() parent=" + var3 + " item=" + var1);
                  if (Core.bDebug) {
                     throw new IllegalStateException();
                  }

                  return;
               }
            } else if (var3 instanceof BaseVehicle) {
               if (var2.vehiclePart == null || var2.vehiclePart.getItemContainer() != var2 || var2.vehiclePart.getVehicle() != var3) {
                  DebugLog.log("ERROR: sendItemStats() parent=" + var3 + " item=" + var1);
                  if (Core.bDebug) {
                     throw new IllegalStateException();
                  }

                  return;
               }
            } else if (var3 instanceof IsoDeadBody) {
               if (var2 != ((IsoObject)var3).getContainer()) {
                  DebugLog.log("ERROR: sendItemStats() parent=" + var3 + " item=" + var1);
                  if (Core.bDebug) {
                     throw new IllegalStateException();
                  }

                  return;
               }
            } else if (var3 == null || ((IsoObject)var3).getContainerIndex(var2) == -1) {
               DebugLog.log("ERROR: sendItemStats() parent=" + var3 + " item=" + var1);
               if (Core.bDebug) {
                  throw new IllegalStateException();
               } else {
                  return;
               }
            }

            INetworkPacket.send(PacketTypes.PacketType.ItemStats, var2, var1);
         }
      }
   }

   public void PlayWorldSound(String var1, int var2, int var3, byte var4) {
      PlayWorldSoundPacket var5 = new PlayWorldSoundPacket();
      var5.set(var1, var2, var3, var4, -1);
      ByteBufferWriter var6 = connection.startPacket();
      PacketTypes.PacketType.PlayWorldSound.doPacket(var6);
      var5.write(var6);
      PacketTypes.PacketType.PlayWorldSound.send(connection);
   }

   public void PlaySound(String var1, boolean var2, IsoMovingObject var3) {
      INetworkPacket.send(PacketTypes.PacketType.PlaySound, var1, var2, var3);
   }

   public void StopSound(IsoMovingObject var1, String var2, boolean var3) {
      ByteBufferWriter var4 = connection.startPacket();
      PacketTypes.PacketType.StopSound.doPacket(var4);
      StopSoundPacket var5 = new StopSoundPacket();
      var5.set(var1, var2, var3);
      var5.write(var4);
      PacketTypes.PacketType.StopSound.send(connection);
   }

   public void startLocalServer() throws Exception {
      bClient = true;
      ip = "127.0.0.1";
      Thread var1 = new Thread(ThreadGroups.Workers, () -> {
         String var0 = System.getProperty("file.separator");
         String var1 = System.getProperty("java.class.path");
         String var10000 = System.getProperty("java.home");
         String var2 = var10000 + var0 + "bin" + var0 + "java";
         ProcessBuilder var3 = new ProcessBuilder(new String[]{var2, "-Xms2048m", "-Xmx2048m", "-Djava.library.path=../natives/", "-cp", "lwjgl.jar;lwjgl_util.jar;sqlitejdbc-v056.jar;../bin/", "zombie.network.GameServer"});
         var3.redirectErrorStream(true);
         Process var4 = null;

         try {
            var4 = var3.start();
         } catch (IOException var10) {
            var10.printStackTrace();
         }

         InputStreamReader var5 = new InputStreamReader(var4.getInputStream());
         boolean var6 = false;

         try {
            while(!var5.ready()) {
               int var7;
               try {
                  while((var7 = var5.read()) != -1) {
                     System.out.print((char)var7);
                  }
               } catch (IOException var11) {
                  var11.printStackTrace();
               }

               try {
                  var5.close();
               } catch (IOException var9) {
                  var9.printStackTrace();
               }
            }
         } catch (IOException var12) {
            var12.printStackTrace();
         }

      });
      var1.setUncaughtExceptionHandler(GameWindow::uncaughtException);
      var1.start();
   }

   public static void sendPing() {
      if (bClient) {
         ByteBufferWriter var0 = connection.startPingPacket();
         PacketTypes.doPingPacket(var0);
         var0.putLong(System.currentTimeMillis());
         var0.putLong(0L);
         connection.endPingPacket();
      }

   }

   static void receiveVehicles(ByteBuffer var0, short var1) {
      VehicleManager.instance.clientPacket(var0);
   }

   public IsoPlayer getPlayerFromUsername(String var1) {
      if (var1 != null) {
         ArrayList var2 = this.getPlayers();

         for(int var3 = 0; var3 < var2.size(); ++var3) {
            IsoPlayer var4 = (IsoPlayer)var2.get(var3);
            if (var4.getUsername().equals(var1)) {
               return var4;
            }
         }
      }

      return null;
   }

   public static void destroy(IsoObject var0) {
      if (ServerOptions.instance.AllowDestructionBySledgehammer.getValue()) {
         SledgehammerDestroyPacket var1 = new SledgehammerDestroyPacket();
         var1.set(var0);
         ByteBufferWriter var2 = connection.startPacket();
         PacketTypes.PacketType.SledgehammerDestroy.doPacket(var2);
         var1.write(var2);
         PacketTypes.PacketType.SledgehammerDestroy.send(connection);
         var0.getSquare().RemoveTileObject(var0);
      }

   }

   public static void sendStopFire(IsoGridSquare var0) {
      INetworkPacket.send(PacketTypes.PacketType.StopFire, var0);
   }

   public static void receiveRadioDeviceDataState(ByteBuffer var0, short var1) {
      byte var2 = var0.get();
      if (var2 == 1) {
         int var3 = var0.getInt();
         int var4 = var0.getInt();
         int var5 = var0.getInt();
         int var6 = var0.getInt();
         IsoGridSquare var7 = IsoWorld.instance.CurrentCell.getGridSquare(var3, var4, var5);
         if (var7 != null && var6 >= 0 && var6 < var7.getObjects().size()) {
            IsoObject var8 = (IsoObject)var7.getObjects().get(var6);
            if (var8 instanceof IsoWaveSignal) {
               DeviceData var9 = ((IsoWaveSignal)var8).getDeviceData();
               if (var9 != null) {
                  try {
                     var9.receiveDeviceDataStatePacket(var0, (UdpConnection)null);
                  } catch (Exception var13) {
                     System.out.print(var13.getMessage());
                  }
               }
            }
         }
      } else {
         short var14;
         if (var2 == 0) {
            var14 = var0.getShort();
            IsoPlayer var15 = (IsoPlayer)IDToPlayerMap.get(var14);
            byte var17 = var0.get();
            if (var15 != null) {
               Radio var19 = null;
               if (var17 == 1 && var15.getPrimaryHandItem() instanceof Radio) {
                  var19 = (Radio)var15.getPrimaryHandItem();
               }

               if (var17 == 2 && var15.getSecondaryHandItem() instanceof Radio) {
                  var19 = (Radio)var15.getSecondaryHandItem();
               }

               if (var19 != null && var19.getDeviceData() != null) {
                  try {
                     var19.getDeviceData().receiveDeviceDataStatePacket(var0, connection);
                  } catch (Exception var12) {
                     System.out.print(var12.getMessage());
                  }
               }
            }
         } else if (var2 == 2) {
            var14 = var0.getShort();
            short var16 = var0.getShort();
            BaseVehicle var18 = VehicleManager.instance.getVehicleByID(var14);
            if (var18 != null) {
               VehiclePart var21 = var18.getPartByIndex(var16);
               if (var21 != null) {
                  DeviceData var20 = var21.getDeviceData();
                  if (var20 != null) {
                     try {
                        var20.receiveDeviceDataStatePacket(var0, (UdpConnection)null);
                     } catch (Exception var11) {
                        System.out.print(var11.getMessage());
                     }
                  }
               }
            }
         }
      }

   }

   public static void sendRadioServerDataRequest() {
      ByteBufferWriter var0 = connection.startPacket();
      PacketTypes.PacketType.RadioServerData.doPacket(var0);
      PacketTypes.PacketType.RadioServerData.send(connection);
   }

   public static void receiveRadioServerData(ByteBuffer var0, short var1) {
      ZomboidRadio var2 = ZomboidRadio.getInstance();
      int var3 = var0.getInt();

      for(int var4 = 0; var4 < var3; ++var4) {
         String var5 = GameWindow.ReadString(var0);
         int var6 = var0.getInt();

         for(int var7 = 0; var7 < var6; ++var7) {
            int var8 = var0.getInt();
            String var9 = GameWindow.ReadString(var0);
            var2.addChannelName(var9, var8, var5);
         }
      }

      var2.setHasRecievedServerData(true);
      ZomboidRadio.POST_RADIO_SILENCE = var0.get() == 1;
   }

   public static void receiveRadioPostSilence(ByteBuffer var0, short var1) {
      ZomboidRadio.POST_RADIO_SILENCE = var0.get() == 1;
   }

   public static void sendIsoWaveSignal(int var0, int var1, int var2, String var3, String var4, String var5, float var6, float var7, float var8, int var9, boolean var10) {
      ByteBufferWriter var11 = connection.startPacket();
      PacketTypes.PacketType.WaveSignal.doPacket(var11);

      try {
         WaveSignalPacket var12 = new WaveSignalPacket();
         var12.set(var0, var1, var2, var3, var4, var5, var6, var7, var8, var9, var10);
         var12.write(var11);
         PacketTypes.PacketType.WaveSignal.send(connection);
      } catch (Exception var13) {
         connection.cancelPacket();
         DebugLog.Multiplayer.printException(var13, "SendIsoWaveSignal: failed", LogSeverity.Error);
      }

   }

   public static void sendPlayerListensChannel(int var0, boolean var1, boolean var2) {
      ByteBufferWriter var3 = connection.startPacket();
      PacketTypes.PacketType.PlayerListensChannel.doPacket(var3);
      var3.putInt(var0);
      var3.putByte((byte)(var1 ? 1 : 0));
      var3.putByte((byte)(var2 ? 1 : 0));
      PacketTypes.PacketType.PlayerListensChannel.send(connection);
   }

   public static void sendCompost(IsoCompost var0) {
      ByteBufferWriter var1 = connection.startPacket();
      PacketTypes.PacketType.SyncCompost.doPacket(var1);
      var1.putInt(var0.getSquare().getX());
      var1.putInt(var0.getSquare().getY());
      var1.putInt(var0.getSquare().getZ());
      var1.putFloat(var0.getCompost());
      PacketTypes.PacketType.SyncCompost.send(connection);
   }

   static void receiveSyncCompost(ByteBuffer var0, short var1) {
      int var2 = var0.getInt();
      int var3 = var0.getInt();
      int var4 = var0.getInt();
      IsoGridSquare var5 = IsoWorld.instance.CurrentCell.getGridSquare(var2, var3, var4);
      if (var5 != null) {
         IsoCompost var6 = var5.getCompost();
         if (var6 == null) {
            var6 = new IsoCompost(var5.getCell(), var5, var6.getSpriteName());
            var5.AddSpecialObject(var6);
         }

         var6.setCompost(var0.getFloat());
         var6.updateSprite();
      }

   }

   public void requestUserlog(String var1) {
      if (connection.role.haveCapability(Capability.ReadUserLog)) {
         INetworkPacket.send(PacketTypes.PacketType.RequestUserLog, var1);
      }
   }

   public void addUserlog(String var1, String var2, String var3) {
      if (connection.role.haveCapability(Capability.AddUserlog)) {
         INetworkPacket.send(PacketTypes.PacketType.AddUserlog, var1, var2, var3);
      }

   }

   public void removeUserlog(String var1, String var2, String var3) {
      if (connection.role.haveCapability(Capability.WorkWithUserlog)) {
         INetworkPacket.send(PacketTypes.PacketType.RemoveUserlog, var1, var2, var3);
      }

   }

   public void addWarningPoint(String var1, String var2, int var3) {
      if (connection.role.haveCapability(Capability.AddUserlog)) {
         INetworkPacket.send(PacketTypes.PacketType.AddWarningPoint, var1, var2, var3);
      }

   }

   public void getDBSchema() {
      ByteBufferWriter var1 = connection.startPacket();
      PacketTypes.PacketType.GetDBSchema.doPacket(var1);
      PacketTypes.PacketType.GetDBSchema.send(connection);
   }

   static void receiveGetDBSchema(ByteBuffer var0, short var1) {
      if (connection.role.haveCapability(Capability.SeeDB)) {
         instance.dbSchema = LuaManager.platform.newTable();
         int var2 = var0.getInt();

         for(int var3 = 0; var3 < var2; ++var3) {
            KahluaTable var4 = LuaManager.platform.newTable();
            String var5 = GameWindow.ReadString(var0);
            int var6 = var0.getInt();

            for(int var7 = 0; var7 < var6; ++var7) {
               KahluaTable var8 = LuaManager.platform.newTable();
               String var9 = GameWindow.ReadString(var0);
               String var10 = GameWindow.ReadString(var0);
               var8.rawset("name", var9);
               var8.rawset("type", var10);
               var4.rawset(var7, var8);
            }

            instance.dbSchema.rawset(var5, var4);
         }

         LuaEventManager.triggerEvent("OnGetDBSchema", instance.dbSchema);
      }
   }

   public void getTableResult(String var1, int var2) {
      ByteBufferWriter var3 = connection.startPacket();
      PacketTypes.PacketType.GetTableResult.doPacket(var3);
      var3.putInt(var2);
      var3.putUTF(var1);
      PacketTypes.PacketType.GetTableResult.send(connection);
   }

   static void receiveGetTableResult(ByteBuffer var0, short var1) {
      ArrayList var2 = new ArrayList();
      int var3 = var0.getInt();
      String var4 = GameWindow.ReadString(var0);
      int var5 = var0.getInt();
      ArrayList var6 = new ArrayList();

      for(int var7 = 0; var7 < var5; ++var7) {
         DBResult var8 = new DBResult();
         var8.setTableName(var4);
         int var9 = var0.getInt();

         for(int var10 = 0; var10 < var9; ++var10) {
            String var11 = GameWindow.ReadString(var0);
            String var12 = GameWindow.ReadString(var0);
            var8.getValues().put(var11, var12);
            if (var7 == 0) {
               var6.add(var11);
            }
         }

         var8.setColumns(var6);
         var2.add(var8);
      }

      LuaEventManager.triggerEvent("OnGetTableResult", var2, var3, var4);
   }

   public void executeQuery(String var1, KahluaTable var2) {
      if (connection.role.haveCapability(Capability.ModifyDB)) {
         ByteBufferWriter var3 = connection.startPacket();
         PacketTypes.PacketType.ExecuteQuery.doPacket(var3);

         try {
            var3.putUTF(var1);
            var2.save(var3.bb);
         } catch (Throwable var8) {
            ExceptionLogger.logException(var8);
         } finally {
            PacketTypes.PacketType.ExecuteQuery.send(connection);
         }

      }
   }

   public ArrayList<IsoPlayer> getConnectedPlayers() {
      return this.connectedPlayers;
   }

   public static void sendNonPvpZone(NonPvpZone var0, boolean var1) {
      ByteBufferWriter var2 = connection.startPacket();
      PacketTypes.PacketType.SyncNonPvpZone.doPacket(var2);
      var0.save(var2.bb);
      var2.putBoolean(var1);
      PacketTypes.PacketType.SyncNonPvpZone.send(connection);
   }

   public static void sendFaction(Faction var0, boolean var1) {
      ByteBufferWriter var2 = connection.startPacket();
      PacketTypes.PacketType.SyncFaction.doPacket(var2);
      var0.writeToBuffer(var2, var1);
      PacketTypes.PacketType.SyncFaction.send(connection);
   }

   public static void sendFactionInvite(Faction var0, IsoPlayer var1, String var2) {
      ByteBufferWriter var3 = connection.startPacket();
      PacketTypes.PacketType.SendFactionInvite.doPacket(var3);
      var3.putUTF(var0.getName());
      var3.putUTF(var1.getUsername());
      var3.putUTF(var2);
      PacketTypes.PacketType.SendFactionInvite.send(connection);
   }

   static void receiveSendFactionInvite(ByteBuffer var0, short var1) {
      String var2 = GameWindow.ReadString(var0);
      String var3 = GameWindow.ReadString(var0);
      LuaEventManager.triggerEvent("ReceiveFactionInvite", var2, var3);
   }

   public static void acceptFactionInvite(Faction var0, String var1) {
      ByteBufferWriter var2 = connection.startPacket();
      PacketTypes.PacketType.AcceptedFactionInvite.doPacket(var2);
      var2.putUTF(var0.getName());
      var2.putUTF(var1);
      PacketTypes.PacketType.AcceptedFactionInvite.send(connection);
   }

   static void receiveAcceptedFactionInvite(ByteBuffer var0, short var1) {
      String var2 = GameWindow.ReadString(var0);
      String var3 = GameWindow.ReadString(var0);
      Faction var4 = Faction.getFaction(var2);
      if (var4 != null) {
         var4.addPlayer(var3);
      }

      LuaEventManager.triggerEvent("AcceptedFactionInvite", var2, var3);
   }

   public static void addTicket(String var0, String var1, int var2) {
      ByteBufferWriter var3 = connection.startPacket();
      PacketTypes.PacketType.AddTicket.doPacket(var3);
      var3.putUTF(var0);
      var3.putUTF(var1);
      var3.putInt(var2);
      PacketTypes.PacketType.AddTicket.send(connection);
   }

   public static void getTickets(String var0) {
      ByteBufferWriter var1 = connection.startPacket();
      PacketTypes.PacketType.ViewTickets.doPacket(var1);
      var1.putUTF(var0);
      PacketTypes.PacketType.ViewTickets.send(connection);
   }

   static void receiveViewTickets(ByteBuffer var0, short var1) {
      ArrayList var2 = new ArrayList();
      int var3 = var0.getInt();

      for(int var4 = 0; var4 < var3; ++var4) {
         DBTicket var5 = new DBTicket(GameWindow.ReadString(var0), GameWindow.ReadString(var0), var0.getInt());
         var2.add(var5);
         if (var0.get() == 1) {
            DBTicket var6 = new DBTicket(GameWindow.ReadString(var0), GameWindow.ReadString(var0), var0.getInt());
            var6.setIsAnswer(true);
            var5.setAnswer(var6);
         }
      }

      LuaEventManager.triggerEvent("ViewTickets", var2);
   }

   public static void removeTicket(int var0) {
      ByteBufferWriter var1 = connection.startPacket();
      PacketTypes.PacketType.RemoveTicket.doPacket(var1);
      var1.putInt(var0);
      PacketTypes.PacketType.RemoveTicket.send(connection);
   }

   public static boolean sendItemListNet(IsoPlayer var0, ArrayList<InventoryItem> var1, IsoPlayer var2, String var3, String var4) {
      ByteBufferWriter var5 = connection.startPacket();
      PacketTypes.PacketType.SendItemListNet.doPacket(var5);
      var5.putByte((byte)(var2 != null ? 1 : 0));
      if (var2 != null) {
         var5.putShort(var2.getOnlineID());
      }

      var5.putByte((byte)(var0 != null ? 1 : 0));
      if (var0 != null) {
         var5.putShort(var0.getOnlineID());
      }

      GameWindow.WriteString(var5.bb, var3);
      var5.putByte((byte)(var4 != null ? 1 : 0));
      if (var4 != null) {
         GameWindow.WriteString(var5.bb, var4);
      }

      try {
         CompressIdenticalItems.save(var5.bb, var1, (IsoGameCharacter)null);
      } catch (Exception var7) {
         var7.printStackTrace();
         connection.cancelPacket();
         return false;
      }

      PacketTypes.PacketType.SendItemListNet.send(connection);
      return true;
   }

   static void receiveSendItemListNet(ByteBuffer var0, short var1) {
      IsoPlayer var2 = null;
      if (var0.get() != 1) {
         var2 = (IsoPlayer)IDToPlayerMap.get(var0.getShort());
      }

      IsoPlayer var3 = null;
      if (var0.get() == 1) {
         var3 = (IsoPlayer)IDToPlayerMap.get(var0.getShort());
      }

      String var4 = GameWindow.ReadString(var0);
      String var5 = null;
      if (var0.get() == 1) {
         var5 = GameWindow.ReadString(var0);
      }

      short var6 = var0.getShort();
      ArrayList var7 = new ArrayList(var6);

      try {
         for(int var8 = 0; var8 < var6; ++var8) {
            InventoryItem var9 = InventoryItem.loadItem(var0, 219);
            if (var9 != null) {
               var7.add(var9);
            }
         }
      } catch (IOException var10) {
         var10.printStackTrace();
      }

      LuaEventManager.triggerEvent("OnReceiveItemListNet", var3, var7, var2, var4, var5);
   }

   public void requestTrading(IsoPlayer var1, IsoPlayer var2) {
   }

   public void acceptTrading(IsoPlayer var1, IsoPlayer var2, boolean var3) {
   }

   public void tradingUISendAddItem(IsoPlayer var1, IsoPlayer var2, InventoryItem var3) {
   }

   public void tradingUISendRemoveItem(IsoPlayer var1, IsoPlayer var2, InventoryItem var3) {
   }

   public void tradingUISendUpdateState(IsoPlayer var1, IsoPlayer var2, int var3) {
   }

   public static void setServerStatisticEnable(boolean var0) {
      ByteBufferWriter var1 = connection.startPacket();
      PacketTypes.PacketType.StatisticRequest.doPacket(var1);
      var1.putBoolean(var0);
      PacketTypes.PacketType.StatisticRequest.send(connection);
      MPStatistic.clientStatisticEnable = var0;
   }

   public static boolean getServerStatisticEnable() {
      return MPStatistic.clientStatisticEnable;
   }

   static void receiveSpawnRegion(ByteBuffer var0, short var1) {
      if (instance.ServerSpawnRegions == null) {
         instance.ServerSpawnRegions = LuaManager.platform.newTable();
      }

      int var2 = var0.getInt();
      KahluaTable var3 = LuaManager.platform.newTable();

      try {
         var3.load(var0, 219);
      } catch (Exception var5) {
         var5.printStackTrace();
      }

      instance.ServerSpawnRegions.rawset(var2, var3);
   }

   static void receiveClimateManagerPacket(ByteBuffer var0, short var1) {
      ClimateManager var2 = ClimateManager.getInstance();
      if (var2 != null) {
         try {
            var2.receiveClimatePacket(var0, (UdpConnection)null);
         } catch (Exception var4) {
            var4.printStackTrace();
         }
      }

   }

   static void receivePassengerMap(ByteBuffer var0, short var1) {
      PassengerMap.clientReceivePacket(var0);
   }

   static void receiveIsoRegionServerPacket(ByteBuffer var0, short var1) {
      IsoRegions.receiveServerUpdatePacket(var0);
   }

   public static void sendIsoRegionDataRequest() {
      ByteBufferWriter var0 = connection.startPacket();
      PacketTypes.PacketType.IsoRegionClientRequestFullUpdate.doPacket(var0);
      PacketTypes.PacketType.IsoRegionClientRequestFullUpdate.send(connection);
   }

   public void sendSandboxOptionsToServer(SandboxOptions var1) {
      ByteBufferWriter var2 = connection.startPacket();
      PacketTypes.PacketType.SandboxOptions.doPacket(var2);

      try {
         var1.save(var2.bb);
      } catch (IOException var7) {
         ExceptionLogger.logException(var7);
      } finally {
         PacketTypes.PacketType.SandboxOptions.send(connection);
      }

   }

   static void receiveSandboxOptions(ByteBuffer var0, short var1) {
      try {
         SandboxOptions.instance.load(var0);
         SandboxOptions.instance.applySettings();
         SandboxOptions.instance.toLua();
      } catch (Exception var3) {
         ExceptionLogger.logException(var3);
      }

   }

   static void receiveChunkObjectState(ByteBuffer var0, short var1) {
      short var2 = var0.getShort();
      short var3 = var0.getShort();
      IsoChunk var4 = IsoWorld.instance.CurrentCell.getChunk(var2, var3);
      if (var4 != null) {
         try {
            var4.loadObjectState(var0);
         } catch (Throwable var6) {
            ExceptionLogger.logException(var6);
         }

      }
   }

   static void receivePlayerLeaveChat(ByteBuffer var0, short var1) {
      ChatManager.getInstance().processLeaveChatPacket(var0);
   }

   static void receiveInitPlayerChat(ByteBuffer var0, short var1) {
      ChatManager.getInstance().processInitPlayerChatPacket(var0);
   }

   static void receiveAddChatTab(ByteBuffer var0, short var1) {
      ChatManager.getInstance().processAddTabPacket(var0);
   }

   static void receiveRemoveChatTab(ByteBuffer var0, short var1) {
      ChatManager.getInstance().processRemoveTabPacket(var0);
   }

   static void receivePlayerNotFound(ByteBuffer var0, short var1) {
      String var2 = GameWindow.ReadStringUTF(var0);
      ChatManager.getInstance().processPlayerNotFound(var2);
   }

   public static void sendPerks(IsoPlayer var0) {
      ByteBufferWriter var1 = connection.startPacket();
      PacketTypes.PacketType.SyncPerks.doPacket(var1);
      var1.putByte((byte)var0.PlayerIndex);
      var1.putInt(var0.getPerkLevel(PerkFactory.Perks.Sneak));
      var1.putInt(var0.getPerkLevel(PerkFactory.Perks.Strength));
      var1.putInt(var0.getPerkLevel(PerkFactory.Perks.Fitness));
      PacketTypes.PacketType.SyncPerks.send(connection);
   }

   static void receiveSyncPerks(ByteBuffer var0, short var1) {
      short var2 = var0.getShort();
      int var3 = var0.getInt();
      int var4 = var0.getInt();
      int var5 = var0.getInt();
      IsoPlayer var6 = (IsoPlayer)IDToPlayerMap.get(var2);
      if (var6 != null && !var6.isLocalPlayer()) {
         var6.remoteSneakLvl = var3;
         var6.remoteStrLvl = var4;
         var6.remoteFitLvl = var5;
      }
   }

   public static void sendWeight(IsoPlayer var0) {
      ByteBufferWriter var1 = connection.startPacket();
      PacketTypes.PacketType.SyncWeight.doPacket(var1);
      var1.putByte((byte)var0.PlayerIndex);
      var1.putDouble(var0.getNutrition().getWeight());
      PacketTypes.PacketType.SyncWeight.send(connection);
   }

   static void receiveSyncWeight(ByteBuffer var0, short var1) {
      short var2 = var0.getShort();
      double var3 = var0.getDouble();
      IsoPlayer var5 = (IsoPlayer)IDToPlayerMap.get(var2);
      if (var5 != null && !var5.isLocalPlayer()) {
         var5.getNutrition().setWeight(var3);
      }

   }

   public static void sendEquippedRadioFreq(IsoPlayer var0) {
      ByteBufferWriter var1 = connection.startPacket();
      PacketTypes.PacketType.SyncEquippedRadioFreq.doPacket(var1);
      var1.putByte((byte)var0.PlayerIndex);
      var1.putInt(var0.invRadioFreq.size());

      for(int var2 = 0; var2 < var0.invRadioFreq.size(); ++var2) {
         var1.putInt((Integer)var0.invRadioFreq.get(var2));
      }

      PacketTypes.PacketType.SyncEquippedRadioFreq.send(connection);
   }

   static void receiveSyncEquippedRadioFreq(ByteBuffer var0, short var1) {
      short var2 = var0.getShort();
      int var3 = var0.getInt();
      IsoPlayer var4 = (IsoPlayer)IDToPlayerMap.get(var2);
      if (var4 != null) {
         var4.invRadioFreq.clear();

         int var5;
         for(var5 = 0; var5 < var3; ++var5) {
            var4.invRadioFreq.add(var0.getInt());
         }

         for(var5 = 0; var5 < var4.invRadioFreq.size(); ++var5) {
            System.out.println(var4.invRadioFreq.get(var5));
         }
      }

   }

   public static void sendSneezingCoughing(IsoPlayer var0, int var1, byte var2) {
      SneezeCoughPacket var3 = new SneezeCoughPacket();
      var3.set(var0, var1, var2);
      ByteBufferWriter var4 = connection.startPacket();
      PacketTypes.PacketType.SneezeCough.doPacket(var4);
      var3.write(var4);
      PacketTypes.PacketType.SneezeCough.send(connection);
   }

   public static void rememberPlayerPosition(IsoPlayer var0, float var1, float var2) {
      if (var0 != null && !var0.isLocalPlayer()) {
         if (positions.containsKey(var0.getOnlineID())) {
            ((Vector2)positions.get(var0.getOnlineID())).set(var1, var2);
         } else {
            positions.put(var0.getOnlineID(), new Vector2(var1, var2));
         }

         WorldMapRemotePlayer var3 = WorldMapRemotePlayers.instance.getPlayerByID(var0.getOnlineID());
         if (var3 != null) {
            var3.setPosition(var1, var2);
         }

      }
   }

   static {
      port = GameServer.DEFAULT_PORT;
      checksum = "";
      checksumValid = false;
      pingsList = new ArrayList();
      loadedCells = new ClientServerMap[4];
      isPaused = false;
      steamID = 0L;
      positions = new HashMap(ServerOptions.getInstance().getMaxPlayers());
      tempShortList = new TShortArrayList();
      MainLoopNetDataQ = new ConcurrentLinkedQueue();
      MainLoopNetData = new ArrayList();
      LoadingMainLoopNetData = new ArrayList();
      DelayedCoopNetData = new ArrayList();
      ServerPredictedAhead = 0.0F;
      IDToPlayerMap = new HashMap();
      IDToZombieMap = new TShortObjectHashMap();
      askPing = false;
      askCustomizationData = false;
      sendQR = false;
      startAuth = null;
      poisonousBerry = null;
      poisonousMushroom = null;
   }

   public static enum RequestState {
      Start,
      Loading,
      Complete;

      private RequestState() {
      }
   }
}
