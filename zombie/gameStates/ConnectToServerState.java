package zombie.gameStates;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.SandboxOptions;
import zombie.SystemDisabler;
import zombie.ZomboidFileSystem;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.commands.PlayerType;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.raknet.UdpConnection;
import zombie.core.znet.ISteamWorkshopCallback;
import zombie.core.znet.SteamUGCDetails;
import zombie.core.znet.SteamUtils;
import zombie.core.znet.SteamWorkshop;
import zombie.core.znet.SteamWorkshopItem;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.erosion.ErosionConfig;
import zombie.globalObjects.CGlobalObjects;
import zombie.iso.IsoChunkMap;
import zombie.network.ConnectionManager;
import zombie.network.CoopMaster;
import zombie.network.GameClient;
import zombie.network.ServerOptions;
import zombie.savefile.ClientPlayerDB;
import zombie.world.WorldDictionary;

public final class ConnectToServerState extends GameState {
   public static ConnectToServerState instance;
   private ByteBuffer connectionDetails;
   private State state;
   private ArrayList<WorkshopItem> workshopItems = new ArrayList();
   private ArrayList<WorkshopItem> confirmItems = new ArrayList();
   private ItemQuery query;

   private static void noise(String var0) {
      DebugLog.log("ConnectToServerState: " + var0);
   }

   public ConnectToServerState(ByteBuffer var1) {
      this.connectionDetails = ByteBuffer.allocate(var1.capacity());
      this.connectionDetails.put(var1);
      this.connectionDetails.rewind();
   }

   public void enter() {
      instance = this;
      ConnectionManager.log("connect-state", ConnectToServerState.State.Start.name().toLowerCase(), (UdpConnection)null);
      this.state = ConnectToServerState.State.Start;
   }

   public GameStateMachine.StateAction update() {
      switch (this.state) {
         case Start:
            this.Start();
            break;
         case TestTCP:
            this.TestTCP();
            break;
         case WorkshopInit:
            this.WorkshopInit();
            break;
         case WorkshopQuery:
            this.WorkshopQuery();
            break;
         case WorkshopConfirm:
            this.WorkshopConfirm();
            break;
         case ServerWorkshopItemScreen:
            this.ServerWorkshopItemScreen();
            break;
         case WorkshopUpdate:
            this.WorkshopUpdate();
            break;
         case CheckMods:
            this.CheckMods();
            break;
         case Finish:
            this.Finish();
            break;
         case Exit:
            return GameStateMachine.StateAction.Continue;
      }

      return GameStateMachine.StateAction.Remain;
   }

   private void receiveStartLocation(ByteBuffer var1) {
      LuaEventManager.triggerEvent("OnConnectionStateChanged", "Connected");
      IsoChunkMap.MPWorldXA = var1.getInt();
      IsoChunkMap.MPWorldYA = var1.getInt();
      IsoChunkMap.MPWorldZA = var1.getInt();
      GameClient.username = GameClient.username.trim();
      Core.GameMode = "Multiplayer";
      LuaManager.GlobalObject.createWorld(Core.GameSaveWorld);
      GameClient.instance.bConnected = true;
   }

   private void receiveServerOptions(ByteBuffer var1) throws IOException {
      int var2 = var1.getInt();

      for(int var3 = 0; var3 < var2; ++var3) {
         String var4 = GameWindow.ReadString(var1);
         String var5 = GameWindow.ReadString(var1);
         ServerOptions.instance.putOption(var4, var5);
      }

      Core.getInstance().ResetLua("client", "ConnectedToServer");
      Core.GameMode = "Multiplayer";
      GameClient.connection.ip = GameClient.ip;
   }

   private void receiveSandboxOptions(ByteBuffer var1) throws IOException {
      SandboxOptions.instance.load(var1);
      SandboxOptions.instance.applySettings();
      SandboxOptions.instance.toLua();
   }

   private void receiveGameTime(ByteBuffer var1) throws IOException {
      GameTime.getInstance().load(var1);
      GameTime.getInstance().save();
   }

   private void receiveErosionMain(ByteBuffer var1) {
      GameClient.instance.erosionConfig = new ErosionConfig();
      GameClient.instance.erosionConfig.load(var1);
   }

   private void receiveGlobalObjects(ByteBuffer var1) throws IOException {
      CGlobalObjects.loadInitialState(var1);
   }

   private void receiveResetID(ByteBuffer var1) {
      int var2 = var1.getInt();
      GameClient.instance.setResetID(var2);
   }

   private void receiveBerries(ByteBuffer var1) {
      Core.getInstance().setPoisonousBerry(GameWindow.ReadString(var1));
      GameClient.poisonousBerry = Core.getInstance().getPoisonousBerry();
      Core.getInstance().setPoisonousMushroom(GameWindow.ReadString(var1));
      GameClient.poisonousMushroom = Core.getInstance().getPoisonousMushroom();
   }

   private void receiveWorldDictionary(ByteBuffer var1) throws IOException {
      WorldDictionary.loadDataFromServer(var1);
      ClientPlayerDB.setAllow(true);
      LuaEventManager.triggerEvent("OnConnected");
   }

   private void Start() {
      noise("Start");
      ByteBuffer var1 = this.connectionDetails;
      GameClient.connection.isCoopHost = var1.get() == 1;
      GameClient.connection.maxPlayers = var1.getInt();
      if (var1.get() == 1) {
         long var2 = var1.getLong();
         String var4 = GameWindow.ReadStringUTF(var1);
         Core.GameSaveWorld = "" + var2 + "_" + var4 + "_player";
      }

      GameClient.instance.ID = var1.get();
      ConnectionManager.log("connect-state-" + this.state.name().toLowerCase(), ConnectToServerState.State.TestTCP.name().toLowerCase(), (UdpConnection)null);
      this.state = ConnectToServerState.State.TestTCP;
   }

   private void TestTCP() {
      noise("TestTCP");
      ByteBuffer var1 = this.connectionDetails;
      GameClient.connection.accessLevel = PlayerType.fromString(GameWindow.ReadStringUTF(var1));
      if (!SystemDisabler.getAllowDebugConnections() && Core.bDebug && !SystemDisabler.getOverrideServerConnectDebugCheck() && GameClient.connection.accessLevel != 32 && !CoopMaster.instance.isRunning()) {
         LuaEventManager.triggerEvent("OnConnectFailed", Translator.getText("UI_OnConnectFailed_DebugNotAllowed"));
         GameClient.connection.forceDisconnect("connect-debug-used");
         ConnectionManager.log("connect-state-" + this.state.name().toLowerCase(), ConnectToServerState.State.Exit.name().toLowerCase(), (UdpConnection)null);
         this.state = ConnectToServerState.State.Exit;
      } else {
         GameClient.GameMap = GameWindow.ReadStringUTF(var1);
         if (GameClient.GameMap.contains(";")) {
            String[] var2 = GameClient.GameMap.split(";");
            Core.GameMap = var2[0].trim();
         } else {
            Core.GameMap = GameClient.GameMap.trim();
         }

         if (SteamUtils.isSteamModeEnabled()) {
            ConnectionManager.log("connect-state-" + this.state.name().toLowerCase(), ConnectToServerState.State.WorkshopInit.name().toLowerCase(), (UdpConnection)null);
            this.state = ConnectToServerState.State.WorkshopInit;
         } else {
            ConnectionManager.log("connect-state-" + this.state.name().toLowerCase(), ConnectToServerState.State.CheckMods.name().toLowerCase(), (UdpConnection)null);
            this.state = ConnectToServerState.State.CheckMods;
         }

      }
   }

   private void WorkshopInit() {
      ByteBuffer var1 = this.connectionDetails;
      short var2 = var1.getShort();

      for(int var3 = 0; var3 < var2; ++var3) {
         long var4 = var1.getLong();
         long var6 = var1.getLong();
         WorkshopItem var8 = new WorkshopItem(var4, var6);
         this.workshopItems.add(var8);
      }

      if (this.workshopItems.isEmpty()) {
         ConnectionManager.log("connect-state-" + this.state.name().toLowerCase(), ConnectToServerState.State.WorkshopUpdate.name().toLowerCase(), (UdpConnection)null);
         this.state = ConnectToServerState.State.WorkshopUpdate;
      } else {
         long[] var9 = new long[this.workshopItems.size()];

         for(int var10 = 0; var10 < this.workshopItems.size(); ++var10) {
            WorkshopItem var5 = (WorkshopItem)this.workshopItems.get(var10);
            var9[var10] = var5.ID;
         }

         this.query = new ItemQuery();
         this.query.handle = SteamWorkshop.instance.CreateQueryUGCDetailsRequest(var9, this.query);
         if (this.query.handle != 0L) {
            ConnectionManager.log("connect-state-" + this.state.name().toLowerCase(), ConnectToServerState.State.WorkshopQuery.name().toLowerCase(), (UdpConnection)null);
            this.state = ConnectToServerState.State.WorkshopQuery;
         } else {
            this.query = null;
            LuaEventManager.triggerEvent("OnConnectFailed", Translator.getText("UI_OnConnectFailed_CreateQueryUGCDetailsRequest"));
            GameClient.connection.forceDisconnect("connect-workshop-query");
            ConnectionManager.log("connect-state-" + this.state.name().toLowerCase(), ConnectToServerState.State.Exit.name().toLowerCase(), (UdpConnection)null);
            this.state = ConnectToServerState.State.Exit;
         }

      }
   }

   private void WorkshopConfirm() {
      this.confirmItems.clear();

      for(int var1 = 0; var1 < this.workshopItems.size(); ++var1) {
         WorkshopItem var2 = (WorkshopItem)this.workshopItems.get(var1);
         long var3 = SteamWorkshop.instance.GetItemState(var2.ID);
         String var10000 = SteamWorkshopItem.ItemState.toString(var3);
         noise("WorkshopConfirm GetItemState()=" + var10000 + " ID=" + var2.ID);
         if (SteamWorkshopItem.ItemState.Installed.and(var3) && SteamWorkshopItem.ItemState.NeedsUpdate.not(var3) && var2.details != null && var2.details.getTimeCreated() != 0L && var2.details.getTimeUpdated() != SteamWorkshop.instance.GetItemInstallTimeStamp(var2.ID)) {
            noise("Installed status but timeUpdated doesn't match!!!");
            var3 |= (long)SteamWorkshopItem.ItemState.NeedsUpdate.getValue();
         }

         if (var3 != (long)(SteamWorkshopItem.ItemState.Subscribed.getValue() | SteamWorkshopItem.ItemState.Installed.getValue())) {
            this.confirmItems.add(var2);
         }
      }

      if (this.confirmItems.isEmpty()) {
         this.query = null;
         ConnectionManager.log("connect-state-" + this.state.name().toLowerCase(), ConnectToServerState.State.WorkshopUpdate.name().toLowerCase(), (UdpConnection)null);
         this.state = ConnectToServerState.State.WorkshopUpdate;
      } else if (this.query == null) {
         ConnectionManager.log("connect-state-" + this.state.name().toLowerCase(), ConnectToServerState.State.WorkshopUpdate.name().toLowerCase(), (UdpConnection)null);
         this.state = ConnectToServerState.State.WorkshopUpdate;
      } else {
         assert this.query.isCompleted();

         ArrayList var5 = new ArrayList();

         for(int var6 = 0; var6 < this.workshopItems.size(); ++var6) {
            WorkshopItem var8 = (WorkshopItem)this.workshopItems.get(var6);
            var5.add(SteamUtils.convertSteamIDToString(var8.ID));
         }

         LuaEventManager.triggerEvent("OnServerWorkshopItems", "Required", var5);
         ArrayList var7 = this.query.details;
         this.query = null;
         ConnectionManager.log("connect-state-" + this.state.name().toLowerCase(), ConnectToServerState.State.ServerWorkshopItemScreen.name().toLowerCase(), (UdpConnection)null);
         this.state = ConnectToServerState.State.ServerWorkshopItemScreen;
         LuaEventManager.triggerEvent("OnServerWorkshopItems", "Details", var7);
      }

   }

   private void WorkshopQuery() {
      if (!this.query.isCompleted()) {
         if (this.query.isNotCompleted()) {
            this.query = null;
            ConnectionManager.log("connect-state-" + this.state.name().toLowerCase(), ConnectToServerState.State.ServerWorkshopItemScreen.name().toLowerCase(), (UdpConnection)null);
            this.state = ConnectToServerState.State.ServerWorkshopItemScreen;
            LuaEventManager.triggerEvent("OnServerWorkshopItems", "Error", "ItemQueryNotCompleted");
         }
      } else {
         Iterator var1 = this.query.details.iterator();

         while(true) {
            while(var1.hasNext()) {
               SteamUGCDetails var2 = (SteamUGCDetails)var1.next();
               Iterator var3 = this.workshopItems.iterator();

               while(var3.hasNext()) {
                  WorkshopItem var4 = (WorkshopItem)var3.next();
                  if (var4.ID == var2.getID()) {
                     var4.details = var2;
                     break;
                  }
               }
            }

            ConnectionManager.log("connect-state-" + this.state.name().toLowerCase(), ConnectToServerState.State.WorkshopConfirm.name().toLowerCase(), (UdpConnection)null);
            this.state = ConnectToServerState.State.WorkshopConfirm;
            return;
         }
      }
   }

   private void ServerWorkshopItemScreen() {
   }

   private void WorkshopUpdate() {
      for(int var1 = 0; var1 < this.workshopItems.size(); ++var1) {
         WorkshopItem var2 = (WorkshopItem)this.workshopItems.get(var1);
         var2.update();
         if (var2.state == ConnectToServerState.WorkshopItemState.Fail) {
            ConnectionManager.log("connect-state-" + this.state.name().toLowerCase(), ConnectToServerState.State.ServerWorkshopItemScreen.name().toLowerCase(), (UdpConnection)null);
            this.state = ConnectToServerState.State.ServerWorkshopItemScreen;
            LuaEventManager.triggerEvent("OnServerWorkshopItems", "Error", var2.ID, var2.error);
            return;
         }

         if (var2.state != ConnectToServerState.WorkshopItemState.Ready) {
            return;
         }
      }

      ZomboidFileSystem.instance.resetModFolders();
      LuaEventManager.triggerEvent("OnServerWorkshopItems", "Success");
      ConnectionManager.log("connect-state-" + this.state.name().toLowerCase(), ConnectToServerState.State.CheckMods.name().toLowerCase(), (UdpConnection)null);
      this.state = ConnectToServerState.State.CheckMods;
   }

   private void CheckMods() {
      ByteBuffer var1 = this.connectionDetails;
      ArrayList var2 = new ArrayList();
      HashMap var3 = new HashMap();
      int var4 = var1.getInt();

      for(int var5 = 0; var5 < var4; ++var5) {
         ChooseGameInfo.Mod var6 = new ChooseGameInfo.Mod(GameWindow.ReadStringUTF(var1));
         var6.setUrl(GameWindow.ReadStringUTF(var1));
         var6.setName(GameWindow.ReadStringUTF(var1));
         var2.add(var6.getDir());
         var3.put(var6.getDir(), var6);
      }

      GameClient.instance.ServerMods.clear();
      GameClient.instance.ServerMods.addAll(var2);
      var2.clear();
      String var7 = ZomboidFileSystem.instance.loadModsAux(GameClient.instance.ServerMods, var2);
      if (var7 != null) {
         String var8 = Translator.getText("UI_OnConnectFailed_ModRequired", var7);
         if (var3.get(var7) != null && !"".equals(((ChooseGameInfo.Mod)var3.get(var7)).getUrl())) {
            var8 = var8 + " MODURL=" + ((ChooseGameInfo.Mod)var3.get(var7)).getUrl();
         }

         LuaEventManager.triggerEvent("OnConnectFailed", var8);
         GameClient.connection.forceDisconnect("connect-mod-required");
         ConnectionManager.log("connect-state-" + this.state.name().toLowerCase(), ConnectToServerState.State.Exit.name().toLowerCase(), (UdpConnection)null);
         this.state = ConnectToServerState.State.Exit;
      } else {
         ConnectionManager.log("connect-state-" + this.state.name().toLowerCase(), ConnectToServerState.State.Finish.name().toLowerCase(), (UdpConnection)null);
         this.state = ConnectToServerState.State.Finish;
      }
   }

   private void Finish() {
      ByteBuffer var1 = this.connectionDetails;

      try {
         try {
            this.receiveStartLocation(var1);
         } catch (Exception var11) {
            DebugLog.Multiplayer.printException(var11, "receiveStartLocation error", LogSeverity.Error);
            throw var11;
         }

         try {
            this.receiveServerOptions(var1);
         } catch (IOException var10) {
            DebugLog.Multiplayer.printException(var10, "receiveServerOptions error", LogSeverity.Error);
            throw var10;
         }

         try {
            this.receiveSandboxOptions(var1);
         } catch (IOException var9) {
            DebugLog.Multiplayer.printException(var9, "receiveSandboxOptions error", LogSeverity.Error);
            throw var9;
         }

         try {
            this.receiveGameTime(var1);
         } catch (IOException var8) {
            DebugLog.Multiplayer.printException(var8, "receiveGameTime error", LogSeverity.Error);
            throw var8;
         }

         try {
            this.receiveErosionMain(var1);
         } catch (Exception var7) {
            DebugLog.Multiplayer.printException(var7, "receiveErosionMain error", LogSeverity.Error);
            throw var7;
         }

         try {
            this.receiveGlobalObjects(var1);
         } catch (IOException var6) {
            DebugLog.Multiplayer.printException(var6, "receiveGlobalObjects error", LogSeverity.Error);
            throw var6;
         }

         try {
            this.receiveResetID(var1);
         } catch (Exception var5) {
            DebugLog.Multiplayer.printException(var5, "receiveResetID error", LogSeverity.Error);
            throw var5;
         }

         try {
            this.receiveBerries(var1);
         } catch (Exception var4) {
            DebugLog.Multiplayer.printException(var4, "receiveBerries error", LogSeverity.Error);
            throw var4;
         }

         try {
            this.receiveWorldDictionary(var1);
         } catch (IOException var3) {
            DebugLog.Multiplayer.printException(var3, "receiveWorldDictionary error", LogSeverity.Error);
            throw var3;
         }
      } catch (Exception var12) {
         ExceptionLogger.logException(var12);
         LuaEventManager.triggerEvent("OnConnectFailed", "WorldDictionary error");
         GameClient.connection.forceDisconnect("connection-details-error");
      }

      ConnectionManager.log("connect-state-" + this.state.name().toLowerCase(), ConnectToServerState.State.Exit.name().toLowerCase(), (UdpConnection)null);
      this.state = ConnectToServerState.State.Exit;
   }

   public void FromLua(String var1) {
      if (this.state != ConnectToServerState.State.ServerWorkshopItemScreen) {
         throw new IllegalStateException("state != ServerWorkshopItemScreen");
      } else if ("install".equals(var1)) {
         ConnectionManager.log("connect-state-lua-" + this.state.name().toLowerCase(), ConnectToServerState.State.WorkshopUpdate.name().toLowerCase(), (UdpConnection)null);
         this.state = ConnectToServerState.State.WorkshopUpdate;
      } else if ("disconnect".equals(var1)) {
         LuaEventManager.triggerEvent("OnConnectFailed", "ServerWorkshopItemsCancelled");
         if (GameClient.connection != null) {
            GameClient.connection.forceDisconnect("connect-workshop-canceled");
         }

         ConnectionManager.log("connect-state-lua-" + this.state.name().toLowerCase(), ConnectToServerState.State.Exit.name().toLowerCase(), (UdpConnection)null);
         this.state = ConnectToServerState.State.Exit;
      }
   }

   public void exit() {
      instance = null;
   }

   private static enum State {
      Start,
      TestTCP,
      WorkshopInit,
      WorkshopQuery,
      WorkshopConfirm,
      ServerWorkshopItemScreen,
      WorkshopUpdate,
      CheckMods,
      Finish,
      Exit;

      private State() {
      }
   }

   private static final class WorkshopItem implements ISteamWorkshopCallback {
      long ID;
      long serverTimeStamp;
      WorkshopItemState state;
      boolean subscribed;
      long downloadStartTime;
      long downloadQueryTime;
      String error;
      SteamUGCDetails details;

      WorkshopItem(long var1, long var3) {
         this.state = ConnectToServerState.WorkshopItemState.CheckItemState;
         this.ID = var1;
         this.serverTimeStamp = var3;
      }

      void update() {
         switch (this.state) {
            case CheckItemState:
               this.CheckItemState();
               break;
            case SubscribePending:
               this.SubscribePending();
               break;
            case DownloadPending:
               this.DownloadPending();
            case Ready:
         }

      }

      void setState(WorkshopItemState var1) {
         ConnectToServerState.noise("item state " + this.state + " -> " + var1 + " ID=" + this.ID);
         this.state = var1;
      }

      void CheckItemState() {
         long var1 = SteamWorkshop.instance.GetItemState(this.ID);
         String var10000 = SteamWorkshopItem.ItemState.toString(var1);
         ConnectToServerState.noise("GetItemState()=" + var10000 + " ID=" + this.ID);
         if (!SteamWorkshopItem.ItemState.Subscribed.and(var1)) {
            if (SteamWorkshop.instance.SubscribeItem(this.ID, this)) {
               this.setState(ConnectToServerState.WorkshopItemState.SubscribePending);
            } else {
               this.error = "SubscribeItemFalse";
               this.setState(ConnectToServerState.WorkshopItemState.Fail);
            }
         } else {
            if (SteamWorkshopItem.ItemState.Installed.and(var1) && SteamWorkshopItem.ItemState.NeedsUpdate.not(var1) && this.details != null && this.details.getTimeCreated() != 0L && this.details.getTimeUpdated() != SteamWorkshop.instance.GetItemInstallTimeStamp(this.ID)) {
               ConnectToServerState.noise("Installed status but timeUpdated doesn't match!!!");
               var1 |= (long)SteamWorkshopItem.ItemState.NeedsUpdate.getValue();
            }

            if (SteamWorkshopItem.ItemState.NeedsUpdate.and(var1)) {
               if (SteamWorkshop.instance.DownloadItem(this.ID, true, this)) {
                  this.setState(ConnectToServerState.WorkshopItemState.DownloadPending);
                  this.downloadStartTime = System.currentTimeMillis();
               } else {
                  this.error = "DownloadItemFalse";
                  this.setState(ConnectToServerState.WorkshopItemState.Fail);
               }
            } else if (SteamWorkshopItem.ItemState.Installed.and(var1)) {
               long var3 = SteamWorkshop.instance.GetItemInstallTimeStamp(this.ID);
               if (var3 == 0L) {
                  this.error = "GetItemInstallTimeStamp";
                  this.setState(ConnectToServerState.WorkshopItemState.Fail);
               } else if (var3 != this.serverTimeStamp) {
                  this.error = "VersionMismatch";
                  this.setState(ConnectToServerState.WorkshopItemState.Fail);
               } else {
                  this.setState(ConnectToServerState.WorkshopItemState.Ready);
               }
            } else {
               this.error = "UnknownItemState";
               this.setState(ConnectToServerState.WorkshopItemState.Fail);
            }
         }
      }

      void SubscribePending() {
         if (this.subscribed) {
            long var1 = SteamWorkshop.instance.GetItemState(this.ID);
            if (SteamWorkshopItem.ItemState.Subscribed.and(var1)) {
               this.setState(ConnectToServerState.WorkshopItemState.CheckItemState);
            }
         }

      }

      void DownloadPending() {
         long var1 = System.currentTimeMillis();
         if (this.downloadQueryTime + 100L <= var1) {
            this.downloadQueryTime = var1;
            long var3 = SteamWorkshop.instance.GetItemState(this.ID);
            if (SteamWorkshopItem.ItemState.NeedsUpdate.and(var3)) {
               long[] var5 = new long[2];
               if (SteamWorkshop.instance.GetItemDownloadInfo(this.ID, var5)) {
                  ConnectToServerState.noise("download " + var5[0] + "/" + var5[1] + " ID=" + this.ID);
                  LuaEventManager.triggerEvent("OnServerWorkshopItems", "Progress", SteamUtils.convertSteamIDToString(this.ID), var5[0], Math.max(var5[1], 1L));
               }

            }
         }
      }

      public void onItemCreated(long var1, boolean var3) {
      }

      public void onItemNotCreated(int var1) {
      }

      public void onItemUpdated(boolean var1) {
      }

      public void onItemNotUpdated(int var1) {
      }

      public void onItemSubscribed(long var1) {
         ConnectToServerState.noise("onItemSubscribed itemID=" + var1);
         if (var1 == this.ID) {
            SteamWorkshop.instance.RemoveCallback(this);
            this.subscribed = true;
         }
      }

      public void onItemNotSubscribed(long var1, int var3) {
         ConnectToServerState.noise("onItemNotSubscribed itemID=" + var1 + " result=" + var3);
         if (var1 == this.ID) {
            SteamWorkshop.instance.RemoveCallback(this);
            this.error = "ItemNotSubscribed";
            this.setState(ConnectToServerState.WorkshopItemState.Fail);
         }
      }

      public void onItemDownloaded(long var1) {
         ConnectToServerState.noise("onItemDownloaded itemID=" + var1 + " time=" + (System.currentTimeMillis() - this.downloadStartTime) + " ms");
         if (var1 == this.ID) {
            SteamWorkshop.instance.RemoveCallback(this);
            this.setState(ConnectToServerState.WorkshopItemState.CheckItemState);
         }
      }

      public void onItemNotDownloaded(long var1, int var3) {
         ConnectToServerState.noise("onItemNotDownloaded itemID=" + var1 + " result=" + var3);
         if (var1 == this.ID) {
            SteamWorkshop.instance.RemoveCallback(this);
            this.error = "ItemNotDownloaded";
            this.setState(ConnectToServerState.WorkshopItemState.Fail);
         }
      }

      public void onItemQueryCompleted(long var1, int var3) {
      }

      public void onItemQueryNotCompleted(long var1, int var3) {
      }
   }

   private class ItemQuery implements ISteamWorkshopCallback {
      long handle;
      ArrayList<SteamUGCDetails> details;
      boolean bCompleted;
      boolean bNotCompleted;

      private ItemQuery() {
      }

      public boolean isCompleted() {
         return this.bCompleted;
      }

      public boolean isNotCompleted() {
         return this.bNotCompleted;
      }

      public void onItemCreated(long var1, boolean var3) {
      }

      public void onItemNotCreated(int var1) {
      }

      public void onItemUpdated(boolean var1) {
      }

      public void onItemNotUpdated(int var1) {
      }

      public void onItemSubscribed(long var1) {
      }

      public void onItemNotSubscribed(long var1, int var3) {
      }

      public void onItemDownloaded(long var1) {
      }

      public void onItemNotDownloaded(long var1, int var3) {
      }

      public void onItemQueryCompleted(long var1, int var3) {
         ConnectToServerState.noise("onItemQueryCompleted handle=" + var1 + " numResult=" + var3);
         if (var1 == this.handle) {
            SteamWorkshop.instance.RemoveCallback(this);
            ArrayList var4 = new ArrayList();

            for(int var5 = 0; var5 < var3; ++var5) {
               SteamUGCDetails var6 = SteamWorkshop.instance.GetQueryUGCResult(var1, var5);
               if (var6 != null) {
                  var4.add(var6);
               }
            }

            this.details = var4;
            SteamWorkshop.instance.ReleaseQueryUGCRequest(var1);
            this.bCompleted = true;
         }
      }

      public void onItemQueryNotCompleted(long var1, int var3) {
         ConnectToServerState.noise("onItemQueryNotCompleted handle=" + var1 + " result=" + var3);
         if (var1 == this.handle) {
            SteamWorkshop.instance.RemoveCallback(this);
            SteamWorkshop.instance.ReleaseQueryUGCRequest(var1);
            this.bNotCompleted = true;
         }
      }
   }

   private static enum WorkshopItemState {
      CheckItemState,
      SubscribePending,
      DownloadPending,
      Ready,
      Fail;

      private WorkshopItemState() {
      }
   }
}
