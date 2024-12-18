package zombie.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import se.krka.kahlua.vm.KahluaTable;
import zombie.SystemDisabler;
import zombie.Lua.LuaManager;
import zombie.characters.Capability;
import zombie.core.Core;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.core.znet.SteamUtils;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.network.anticheats.AntiCheat;
import zombie.network.packets.GlobalObjectsPacket;
import zombie.network.packets.INetworkPacket;
import zombie.network.packets.ObjectChangePacket;
import zombie.network.packets.character.AnimalTracksPacket;
import zombie.network.packets.character.ForageItemFoundPacket;
import zombie.network.packets.world.DebugStoryPacket;

public class PacketTypes {
   public static final byte PacketOrdering_General = 0;
   public static final byte PacketOrdering_Items = 1;
   public static final byte PacketOrdering_ServerCustomization = 2;
   public static final Map<Short, PacketType> packetTypes = new TreeMap();
   public static final KahluaTable packetCountTable;
   private static final HashMap<Long, HashMap<PacketType, INetworkPacket>> packets;
   private static final LastPacket received;
   private static final LastPacket sent;

   public PacketTypes() {
   }

   public static void doPingPacket(ByteBufferWriter var0) {
      var0.putInt(28);
   }

   public static KahluaTable getPacketCounts(int var0) {
      packetCountTable.wipe();
      if (GameClient.bClient) {
         Iterator var1 = packetTypes.values().iterator();

         while(var1.hasNext()) {
            PacketType var2 = (PacketType)var1.next();
            if (var0 == 1) {
               packetCountTable.rawset(String.format("%03d-%s", var2.getId(), var2.name()), String.valueOf(var2.serverPacketCount));
            } else {
               packetCountTable.rawset(String.format("%03d-%s", var2.getId(), var2.name()), String.valueOf(var2.clientPacketCount));
            }
         }
      }

      return packetCountTable;
   }

   private static void LogPacket(UdpConnection var0, PacketType var1, LastPacket var2) {
      if (DebugLog.isLogEnabled(DebugType.Packet, LogSeverity.Debug)) {
         if (var1.equals(var2.type)) {
            ++var2.count;
         } else {
            if (var2.type != null && var2.count > 0L) {
               DebugLog.Packet.debugln("%-10s %-32s (+%d)", var2.name, var2.type.name(), var2.count);
               var2.count = 0L;
            }

            DebugLog.Packet.debugln("%-10s %-32s %s", var2.name, var1.name(), var0 == null ? "" : var0.username);
         }

         var2.type = var1;
      }

   }

   public static INetworkPacket getPacket(PacketType var0, UdpConnection var1) {
      INetworkPacket var2 = null;

      try {
         var2 = (INetworkPacket)((HashMap)packets.computeIfAbsent(var1.getConnectedGUID(), (var0x) -> {
            return new HashMap();
         })).getOrDefault(var0, (INetworkPacket)var0.handler.getDeclaredConstructor().newInstance());
      } catch (Exception var4) {
         DebugLog.General.printException(var4, String.format("Can't create packet of type \"%s\"", var0.name()), LogSeverity.Error);
      }

      return var2;
   }

   static {
      packetCountTable = LuaManager.platform.newTable();
      packets = new HashMap();
      received = new LastPacket("Received");
      sent = new LastPacket("Sent");
      PacketType[] var0 = PacketTypes.PacketType.values();
      int var1 = var0.length;

      for(int var2 = 0; var2 < var1; ++var2) {
         PacketType var3 = var0[var2];
         PacketType var4 = (PacketType)packetTypes.put(var3.getId(), var3);
         if (var4 != null) {
            DebugLog.Multiplayer.error(String.format("PacketType: duplicate \"%s\" \"%s\" id=%d", var4.name(), var3.name(), var3.getId()));
         }
      }

   }

   public static enum PacketType {
      Checksum(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      Validate(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      Login(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      LoginQueueRequest(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      LoginQueueDone(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      CreatePlayer(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      LoadPlayerProfile(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      PlayerConnect(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      ConnectedPlayer(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      ConnectCoop(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      ConnectedCoop(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      GoogleAuthKey(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      GoogleAuth(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      GoogleAuthRequest(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      ServerCustomization(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      PlayerUpdateReliable(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      PlayerUpdateUnreliable(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      ActionPacket(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      PlayerDataRequest(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      HumanVisual(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      SyncClothing(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      SyncVisuals(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      Equip(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      AddXP(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      AddXPMultiplier(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      PlayerDeath(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      ChangeSafety(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      SyncXP(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      BodyDamageUpdate(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      PlayerAttachedItem(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      VariableSync(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      SyncPlayerStats(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      syncPlayerFields(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      ForageItemFound(ForageItemFoundPacket.class),
      ServerMap(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      RequestLargeAreaZip(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      SentChunk(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      RequestZipList(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      NotRequiredInZip(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      RequestData(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      ZombieSimulation(2, 0, Capability.LoginOnServer, GameServer::receiveZombieSimulation, GameClient::receiveZombieSimulation, (CallbackClientProcess)null),
      ZombieSimulationReliable(0, 2, Capability.LoginOnServer, GameServer::receiveZombieSimulation, GameClient::receiveZombieSimulation, (CallbackClientProcess)null),
      ZombieDeath(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      SlowFactor(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      ZombieControl(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      Thump(1, 2, Capability.LoginOnServer, GameServer::receiveThump, GameClient::receiveThump, (CallbackClientProcess)null),
      AnimalCommand(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      AnimalOwnership(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      AnimalPacket(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      AnimalUpdateReliable(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      AnimalUpdateUnreliable(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      AnimalEvent(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      AnimalDeath(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      AnimalTracks(AnimalTracksPacket.class),
      Vehicles(1, 2, Capability.LoginOnServer, GameServer::receiveVehicles, GameClient::receiveVehicles, (CallbackClientProcess)null),
      VehiclesUnreliable(2, 0, Capability.LoginOnServer, GameServer::receiveVehicles, GameClient::receiveVehicles, (CallbackClientProcess)null),
      VehicleAuthorization(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      PassengerMap(1, 2, Capability.LoginOnServer, GameServer::receivePassengerMap, GameClient::receivePassengerMap, (CallbackClientProcess)null),
      EventPacket(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      Helicopter(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      SmashWindow(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      EatFood(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      Drink(1, 2, Capability.LoginOnServer, GameServer::receiveDrink, (CallbackClientProcess)null, (CallbackClientProcess)null),
      BurnCorpse(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      SneezeCough(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      RemoveBlood(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      WakeUpPlayer(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      EatBody(1, 2, Capability.LoginOnServer, GameServer::receiveEatBody, GameClient::receiveEatBody, (CallbackClientProcess)null),
      ReadAnnotedMap(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      ZombieHelmetFalling(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      TimeSync(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      SetMultiplier(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      AccessDenied(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      StartPause(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      StopPause(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      PlayerTimeout(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      SyncClock(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      SandboxOptions(1, 2, Capability.SandboxOptions, GameServer::receiveSandboxOptions, GameClient::receiveSandboxOptions, (CallbackClientProcess)null),
      ServerQuit(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      MessageForAdmin(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      MetaGrid(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      WorldMessage(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      ReceiveCommand(2, 3, Capability.LoginOnServer, GameServer::receiveReceiveCommand, (CallbackClientProcess)null, (CallbackClientProcess)null),
      ReloadOptions(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      Kicked(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      Ping(0, 0, Capability.None, GameServer::receivePing, GameClient::receivePing, GameClient::receivePing),
      PingFromClient(1, 0, Capability.LoginOnServer, GameServer::receivePingFromClient, GameClient::receivePingFromClient, (CallbackClientProcess)null),
      SpawnRegion(1, 2, Capability.LoginOnServer, (CallbackServerProcess)null, GameClient::receiveSpawnRegion, GameClient::receiveSpawnRegion),
      WorldMapPlayerPosition(3, 1, Capability.LoginOnServer, GameServer::receiveWorldMapPlayerPosition, GameClient::receiveWorldMapPlayerPosition, (CallbackClientProcess)null),
      WorldMap(1, 2, Capability.LoginOnServer, GameServer::receiveWorldMap, GameClient::receiveWorldMap, GameClient::receiveWorldMap),
      /** @deprecated */
      @Deprecated
      SyncInventory(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      ItemTransaction(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      GeneralAction(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      NetTimedAction(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      BuildAction(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      FishingAction(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      AddInventoryItemToContainer(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      RemoveInventoryItemFromContainer(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      ReplaceInventoryItemInContainer(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      AddItemToMap(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      AddCorpseToMap(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      RemoveItemFromSquare(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      RemoveCorpseFromMap(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      BecomeCorpse(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      RequestItemsForContainer(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      RemoveContestedItemsFromInventory(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      ItemStats(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      AddBrokenGlass(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      AddExplosiveTrap(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      SledgehammerDestroy(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      SyncPlayerAlarmClock(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      SyncWorldAlarmClock(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      GlobalObjects(GlobalObjectsPacket.class),
      ReceiveModData(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      GlobalModData(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      GlobalModDataRequest(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      SyncItemFields(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      SyncItemModData(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      SyncHandWeaponFields(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      PlayerHitSquare(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      PlayerHitObject(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      PlayerHitVehicle(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      PlayerHitZombie(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      PlayerHitPlayer(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      PlayerHitAnimal(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      ZombieHitPlayer(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      AnimalHitPlayer(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      AnimalHitAnimal(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      AnimalHitThumpable(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      VehicleHitZombie(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      VehicleHitPlayer(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      WeaponHit(1, 2, Capability.LoginOnServer, GameServer::receiveWeaponHit, (CallbackClientProcess)null, (CallbackClientProcess)null),
      PlayerDamage(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      PlayerDamageFromWeapon(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      PlayerEffectsSync(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      PlayerDamageFromCarCrash(1, 2, Capability.LoginOnServer, (CallbackServerProcess)null, GameClient::receivePlayerDamageFromCarCrash, (CallbackClientProcess)null),
      PlaySound(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      WorldSoundPacket(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      AddAmbient(1, 2, Capability.LoginOnServer, (CallbackServerProcess)null, GameClient::receiveAddAmbient, (CallbackClientProcess)null),
      ZombieSound(1, 2, Capability.LoginOnServer, (CallbackServerProcess)null, GameClient::receiveZombieSound, (CallbackClientProcess)null),
      PlayWorldSound(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      StopSound(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      PlaySoundEveryPlayer(1, 2, Capability.LoginOnServer, (CallbackServerProcess)null, GameClient::receivePlaySoundEveryPlayer, (CallbackClientProcess)null),
      PacketCounts(1, 2, Capability.GetStatistic, GameServer::receivePacketCounts, GameClient::receivePacketCounts, (CallbackClientProcess)null),
      ScoreboardUpdate(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      Weather(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      StartRain(1, 2, Capability.LoginOnServer, (CallbackServerProcess)null, GameClient::receiveStartRain, (CallbackClientProcess)null),
      StopRain(1, 2, Capability.LoginOnServer, (CallbackServerProcess)null, GameClient::receiveStopRain, (CallbackClientProcess)null),
      ClimateManagerPacket(1, 2, Capability.ClimateManager, GameServer::receiveClimateManagerPacket, GameClient::receiveClimateManagerPacket, (CallbackClientProcess)null),
      IsoRegionServerPacket(1, 2, Capability.LoginOnServer, (CallbackServerProcess)null, GameClient::receiveIsoRegionServerPacket, (CallbackClientProcess)null),
      IsoRegionClientRequestFullUpdate(1, 2, Capability.LoginOnServer, GameServer::receiveIsoRegionClientRequestFullUpdate, (CallbackClientProcess)null, (CallbackClientProcess)null),
      SyncNonPvpZone(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      SyncThumpable(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      SyncDoorKey(1, 2, Capability.LoginOnServer, GameServer::receiveSyncDoorKey, GameClient::receiveSyncDoorKey, (CallbackClientProcess)null),
      SyncDoorGarage(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      SyncIsoObject(1, 2, Capability.LoginOnServer, GameServer::receiveSyncIsoObject, GameClient::receiveSyncIsoObject, (CallbackClientProcess)null),
      ClientCommand(1, 2, Capability.LoginOnServer, GameServer::receiveClientCommand, GameClient::receiveClientCommand, (CallbackClientProcess)null),
      ObjectModData(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      ObjectChange(ObjectChangePacket.class),
      BloodSplatter(1, 2, Capability.LoginOnServer, (CallbackServerProcess)null, GameClient::receiveBloodSplatter, (CallbackClientProcess)null),
      ZombieDescriptors(1, 2, Capability.LoginOnServer, (CallbackServerProcess)null, GameClient::receiveZombieDescriptors, (CallbackClientProcess)null),
      StartFire(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      StopFire(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      UpdateItemSprite(1, 2, Capability.LoginOnServer, GameServer::receiveUpdateItemSprite, GameClient::receiveUpdateItemSprite, (CallbackClientProcess)null),
      SendCustomColor(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      SyncCompost(1, 2, Capability.LoginOnServer, GameServer::receiveSyncCompost, GameClient::receiveSyncCompost, (CallbackClientProcess)null),
      GetModData(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      SyncPerks(1, 2, Capability.LoginOnServer, GameServer::receiveSyncPerks, GameClient::receiveSyncPerks, (CallbackClientProcess)null),
      SyncWeight(1, 2, Capability.LoginOnServer, GameServer::receiveSyncWeight, GameClient::receiveSyncWeight, (CallbackClientProcess)null),
      SyncInjuries(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      SyncEquippedRadioFreq(1, 2, Capability.LoginOnServer, GameServer::receiveSyncEquippedRadioFreq, GameClient::receiveSyncEquippedRadioFreq, (CallbackClientProcess)null),
      UpdateOverlaySprite(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      AddAlarm(1, 2, Capability.LoginOnServer, (CallbackServerProcess)null, GameClient::receiveAddAlarm, (CallbackClientProcess)null),
      ChangeTextColor(1, 2, Capability.LoginOnServer, GameServer::receiveChangeTextColor, GameClient::receiveChangeTextColor, (CallbackClientProcess)null),
      SyncCustomLightSettings(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      ChunkObjectState(1, 2, Capability.LoginOnServer, GameServer::receiveChunkObjectState, GameClient::receiveChunkObjectState, (CallbackClientProcess)null),
      StartFishSplash(1, 2, Capability.LoginOnServer, GameServer::receiveBigWaterSplash, GameClient::receiveBigWaterSplash, (CallbackClientProcess)null),
      FishingData(1, 2, Capability.LoginOnServer, GameServer::receiveFishingDataRequest, GameClient::receiveFishingData, (CallbackClientProcess)null),
      AddItemInInventory(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      PlayerInventory(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      ExtraInfo(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      Teleport(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      InvMngReqItem(1, 2, Capability.InspectPlayerInventory, GameServer::receiveInvMngReqItem, GameClient::receiveInvMngReqItem, (CallbackClientProcess)null),
      InvMngGetItem(1, 2, Capability.LoginOnServer, GameServer::receiveInvMngGetItem, GameClient::receiveInvMngGetItem, (CallbackClientProcess)null),
      InvMngRemoveItem(1, 2, Capability.InspectPlayerInventory, GameServer::receiveInvMngRemoveItem, GameClient::receiveInvMngRemoveItem, (CallbackClientProcess)null),
      GetDBSchema(1, 2, Capability.SeeDB, GameServer::receiveGetDBSchema, GameClient::receiveGetDBSchema, (CallbackClientProcess)null),
      GetTableResult(1, 2, Capability.SeeDB, GameServer::receiveGetTableResult, GameClient::receiveGetTableResult, (CallbackClientProcess)null),
      ExecuteQuery(1, 2, Capability.ModifyDB, GameServer::receiveExecuteQuery, (CallbackClientProcess)null, (CallbackClientProcess)null),
      ChangePlayerStats(1, 2, Capability.LoginOnServer, GameServer::receiveChangePlayerStats, GameClient::receiveChangePlayerStats, (CallbackClientProcess)null),
      NetworkUsers(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      PVPEvents(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      RequestNetworkUsers(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      NetworkUserAction(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      Roles(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      RequestRoles(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      RolesEdit(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      RequestUserLog(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      AddUserlog(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      RemoveUserlog(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      AddWarningPoint(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      ConstructedZone(1, 2, Capability.LoginOnServer, GameServer::receiveConstructedZone, GameClient::receiveConstructedZone, (CallbackClientProcess)null),
      RegisterZone(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      SyncZone(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      SyncFaction(1, 2, Capability.LoginOnServer, GameServer::receiveSyncFaction, GameClient::receiveSyncFaction, (CallbackClientProcess)null),
      SendFactionInvite(1, 2, Capability.LoginOnServer, GameServer::receiveSendFactionInvite, GameClient::receiveSendFactionInvite, (CallbackClientProcess)null),
      AcceptedFactionInvite(1, 2, Capability.LoginOnServer, GameServer::receiveAcceptedFactionInvite, GameClient::receiveAcceptedFactionInvite, (CallbackClientProcess)null),
      AddTicket(1, 2, Capability.LoginOnServer, GameServer::receiveAddTicket, (CallbackClientProcess)null, (CallbackClientProcess)null),
      ViewTickets(1, 2, Capability.LoginOnServer, GameServer::receiveViewTickets, GameClient::receiveViewTickets, (CallbackClientProcess)null),
      RemoveTicket(1, 2, Capability.AnswerTickets, GameServer::receiveRemoveTicket, (CallbackClientProcess)null, (CallbackClientProcess)null),
      RequestTrading(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      TradingUIAddItem(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      TradingUIRemoveItem(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      TradingUIUpdateState(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      WarStateSync(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      WarSync(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      InitPlayerChat(1, 2, Capability.LoginOnServer, (CallbackServerProcess)null, GameClient::receiveInitPlayerChat, (CallbackClientProcess)null),
      PlayerJoinChat(1, 2, Capability.LoginOnServer, (CallbackServerProcess)null, GameClient::receivePlayerJoinChat, (CallbackClientProcess)null),
      PlayerLeaveChat(1, 2, Capability.LoginOnServer, (CallbackServerProcess)null, GameClient::receivePlayerLeaveChat, (CallbackClientProcess)null),
      ChatMessageFromPlayer(1, 2, Capability.LoginOnServer, GameServer::receiveChatMessageFromPlayer, (CallbackClientProcess)null, (CallbackClientProcess)null),
      ChatMessageToPlayer(1, 2, Capability.LoginOnServer, (CallbackServerProcess)null, GameClient::receiveChatMessageToPlayer, (CallbackClientProcess)null),
      PlayerStartPMChat(1, 2, Capability.LoginOnServer, GameServer::receivePlayerStartPMChat, (CallbackClientProcess)null, (CallbackClientProcess)null),
      AddChatTab(1, 2, Capability.LoginOnServer, (CallbackServerProcess)null, GameClient::receiveAddChatTab, (CallbackClientProcess)null),
      RemoveChatTab(1, 2, Capability.LoginOnServer, (CallbackServerProcess)null, GameClient::receiveRemoveChatTab, (CallbackClientProcess)null),
      PlayerConnectedToChat(1, 2, Capability.LoginOnServer, (CallbackServerProcess)null, GameClient::receivePlayerConnectedToChat, (CallbackClientProcess)null),
      PlayerNotFound(1, 2, Capability.LoginOnServer, (CallbackServerProcess)null, GameClient::receivePlayerNotFound, (CallbackClientProcess)null),
      SafehouseInvite(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      SafehouseAccept(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      SafehouseChangeMember(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      SafehouseChangeOwner(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      SafehouseChangeRespawn(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      SafehouseChangeTitle(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      SafehouseRelease(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      SafehouseClaim(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      SafezoneClaim(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      SafehouseSync(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      Statistic(1, 0, Capability.LoginOnServer, GameServer::receiveStatistic, GameClient::receiveStatistic, (CallbackClientProcess)null),
      StatisticRequest(1, 2, Capability.GetStatistic, GameServer::receiveStatisticRequest, GameClient::receiveStatisticRequest, (CallbackClientProcess)null),
      PopmanDebugCommand(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      ServerDebugInfo(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      ServerLOS(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      WaveSignal(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      PlayerListensChannel(1, 2, Capability.LoginOnServer, GameServer::receivePlayerListensChannel, (CallbackClientProcess)null, (CallbackClientProcess)null),
      RadioServerData(1, 2, Capability.LoginOnServer, GameServer::receiveRadioServerData, GameClient::receiveRadioServerData, (CallbackClientProcess)null),
      RadioDeviceDataState(1, 2, Capability.LoginOnServer, GameServer::receiveRadioDeviceDataState, GameClient::receiveRadioDeviceDataState, (CallbackClientProcess)null),
      RadioPostSilenceEvent(0, 2, Capability.LoginOnServer, (CallbackServerProcess)null, GameClient::receiveRadioPostSilence, (CallbackClientProcess)null),
      SyncRadioData(0, 3, Capability.LoginOnServer, GameServer::receiveSyncRadioData, GameClient::receiveSyncRadioData, (CallbackClientProcess)null),
      BodyPartSync(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null),
      DebugStory(DebugStoryPacket.class),
      SendItemListNet(1, 2, Capability.LoginOnServer, GameServer::receiveSendItemListNet, GameClient::receiveSendItemListNet, (CallbackClientProcess)null),
      GameEntity(3, 0, Capability.None, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null);

      private Capability requiredCapability;
      public int PacketPriority;
      public int PacketReliability;
      public byte OrderingChannel;
      public int handlingType;
      public AntiCheat[] anticheats;
      private final CallbackServerProcess serverHandler;
      private final CallbackClientProcess clientHandler;
      private final CallbackClientProcess clientLoadingHandler;
      public final Class<? extends INetworkPacket> handler;
      public int incomePackets;
      public int outcomePackets;
      public int incomeBytes;
      public int outcomeBytes;
      public int incomeTime;
      public int outcomeTime;
      public long temp;
      public long clientPacketCount;
      public long serverPacketCount;

      private PacketType(int var3, int var4, Capability var5, CallbackServerProcess var6, CallbackClientProcess var7, CallbackClientProcess var8) {
         this(var3, var4, (byte)0, var5, PacketSetting.HandlingType.getType(var6 != null, var7 != null, var8 != null), var6, var7, var8, (Class)null);
      }

      private PacketType(Class var3) {
         this(2, 0, (byte)0, Capability.LoginOnServer, 7, (CallbackServerProcess)null, (CallbackClientProcess)null, (CallbackClientProcess)null, var3);
      }

      private PacketType(int var3, int var4, byte var5, Capability var6, int var7, CallbackServerProcess var8, CallbackClientProcess var9, CallbackClientProcess var10, Class var11) {
         this.requiredCapability = var6;
         this.PacketPriority = var3;
         this.PacketReliability = var4;
         this.OrderingChannel = var5;
         this.handlingType = var7;
         this.serverHandler = var8;
         this.clientHandler = var9;
         this.clientLoadingHandler = var10;
         this.handler = var11;
         if (var11 != null) {
            PacketSetting var12 = getPacketSetting(var11);
            if (var12 == null) {
               DebugLog.Multiplayer.error("The %s class doesn't have PacketSetting attributes", var11.getSimpleName());
            } else {
               this.PacketPriority = var12.priority();
               this.PacketReliability = var12.reliability();
               this.OrderingChannel = var12.ordering();
               this.requiredCapability = var12.requiredCapability();
               this.handlingType = var12.handlingType();
               this.anticheats = var12.anticheats();
            }
         }

         this.resetStatistics();
      }

      private static PacketSetting getPacketSetting(Class<? extends INetworkPacket> var0) {
         PacketSetting[] var1 = (PacketSetting[])var0.getAnnotationsByType(PacketSetting.class);
         return var1.length == 0 ? null : var1[0];
      }

      public void resetStatistics() {
         this.incomePackets = 0;
         this.outcomePackets = 0;
         this.incomeBytes = 0;
         this.outcomeBytes = 0;
         this.incomeTime = 0;
         this.outcomeTime = 0;
         this.clientPacketCount = 0L;
         this.serverPacketCount = 0L;
      }

      public void send(UdpConnection var1) {
         this.outcomeTime = (int)((long)this.outcomeTime + (System.nanoTime() - this.temp));
         var1.endPacket(this.PacketPriority, this.PacketReliability, this.OrderingChannel);
         PacketTypes.LogPacket(var1, this, PacketTypes.sent);
      }

      public void doPacket(ByteBufferWriter var1) {
         this.temp = System.nanoTime();
         var1.putByte((byte)-122);
         var1.putShort(this.getId());
      }

      public short getId() {
         return (short)this.ordinal();
      }

      public void onServerPacket(ByteBuffer var1, UdpConnection var2) throws Exception {
         if (PacketTypes.PacketAuthorization.isAuthorized(var2, this)) {
            PacketTypes.LogPacket(var2, this, PacketTypes.received);
            if ((this.handlingType & 1) != 0) {
               long var3 = System.nanoTime();
               if (this.handler != null) {
                  INetworkPacket var5 = (INetworkPacket)this.handler.getDeclaredConstructor().newInstance();
                  var5.parseServer(var1, var2);
                  if (!var5.isConsistent(var2)) {
                     DebugLog.Multiplayer.warn("The packet %s is not consistent: %s", this.name(), var5.getDescription());
                     var5.sync(this, var2);
                     return;
                  }

                  if (this.anticheats != null) {
                     AntiCheat[] var6 = this.anticheats;
                     int var7 = var6.length;

                     for(int var8 = 0; var8 < var7; ++var8) {
                        AntiCheat var9 = var6[var8];
                        if (!var9.isValid(var2, var5)) {
                           DebugLog.Multiplayer.warn("The packet %s is not valid", this.name());
                           var5.sync(this, var2);
                           return;
                        }
                     }
                  }

                  var5.processServer(this, var2);
               } else {
                  this.serverHandler.call(var1, var2, this.getId());
               }

               this.incomeTime = (int)((long)this.incomeTime + (System.nanoTime() - var3));
            }
         }

      }

      public void onClientPacket(ByteBuffer var1) throws Exception {
         PacketTypes.LogPacket(GameClient.connection, this, PacketTypes.received);
         if ((this.handlingType & 2) != 0) {
            if (this.handler != null) {
               INetworkPacket var2 = (INetworkPacket)this.handler.getDeclaredConstructor().newInstance();
               var2.parseClient(var1, GameClient.connection);
               if (!var2.isConsistent(GameClient.connection)) {
                  DebugLog.Multiplayer.warn("The packet %s is not consistent: %s", this.name(), var2.getDescription());
                  return;
               }

               var2.processClient(GameClient.connection);
            } else {
               this.clientHandler.call(var1, this.getId());
            }
         }

      }

      public boolean onClientLoadingPacket(ByteBuffer var1) throws Exception {
         PacketTypes.LogPacket(GameClient.connection, this, PacketTypes.received);
         if ((this.handlingType & 4) != 0) {
            if (this.handler != null) {
               INetworkPacket var2 = (INetworkPacket)this.handler.getDeclaredConstructor().newInstance();
               var2.parseClientLoading(var1, GameClient.connection);
               if (!var2.isConsistent(GameClient.connection)) {
                  DebugLog.Multiplayer.warn("The packet %s is not consistent: %s", this.name(), var2.getDescription());
                  return false;
               }

               var2.processClientLoading(GameClient.connection);
            } else {
               this.clientLoadingHandler.call(var1, this.getId());
            }

            return true;
         } else {
            return false;
         }
      }
   }

   private static class LastPacket {
      private final String name;
      private PacketType type;
      private long count;

      private LastPacket(String var1) {
         this.name = var1;
      }
   }

   public interface CallbackClientProcess {
      void call(ByteBuffer var1, short var2) throws IOException;
   }

   public interface CallbackServerProcess {
      void call(ByteBuffer var1, UdpConnection var2, short var3) throws Exception;
   }

   public static class PacketAuthorization {
      public PacketAuthorization() {
      }

      private static boolean isAuthorized(UdpConnection var0, PacketType var1) {
         boolean var2 = var1.requiredCapability == Capability.None || var0.role != null && var0.role.haveCapability(var1.requiredCapability);
         if ((!var2 || var1.serverHandler == null && var1.handler == null) && (!Core.bDebug || SystemDisabler.getKickInDebug())) {
            onUnauthorized(var0, var1);
         }

         return var2;
      }

      public static void onUnauthorized(UdpConnection var0, PacketType var1) {
         DebugLog.Multiplayer.warn(String.format("On unauthorized packet %s (%s) was received from user=\"%s\" (%s) ip %s %s", var1.name(), var1.requiredCapability.name(), var0.username, var0.role.getName(), var0.ip, SteamUtils.isSteamModeEnabled() ? var0.steamID : ""));
         AntiCheat.Capability.act(var0, var1.name());
      }

      public interface UnauthorizedPacketPolicy {
         void call(UdpConnection var1, String var2);
      }
   }
}
