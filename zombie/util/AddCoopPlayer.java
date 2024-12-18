package zombie.util;

import zombie.SoundManager;
import zombie.Lua.LuaEventManager;
import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.physics.WorldSimulation;
import zombie.debug.DebugLog;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.LightingJNI;
import zombie.iso.LosUtil;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.connection.ConnectCoopPacket;
import zombie.popman.ZombiePopulationManager;
import zombie.ui.UIManager;

public final class AddCoopPlayer {
   private Stage stage;
   private final IsoPlayer player;

   public AddCoopPlayer(IsoPlayer var1, boolean var2) {
      this.player = var1;
      if (var2) {
         this.stage = AddCoopPlayer.Stage.SendCoopConnect;
      } else {
         this.stage = AddCoopPlayer.Stage.Init;
      }

   }

   public int getPlayerIndex() {
      return this.player == null ? -1 : this.player.PlayerIndex;
   }

   public IsoPlayer getPlayer() {
      return this.player;
   }

   public void update() {
      IsoCell var1;
      ByteBufferWriter var2;
      int var4;
      ConnectCoopPacket var18;
      switch (this.stage) {
         case SendCoopConnect:
            if (GameClient.bClient) {
               GameClient.sendCreatePlayer((byte)this.player.PlayerIndex);
               this.stage = AddCoopPlayer.Stage.ReceiveCoopConnect;
            } else {
               this.stage = AddCoopPlayer.Stage.Init;
            }
            break;
         case Init:
            if (GameClient.bClient) {
               var18 = new ConnectCoopPacket();
               var18.setInit(this.player);
               var2 = GameClient.connection.startPacket();
               PacketTypes.PacketType.ConnectCoop.doPacket(var2);
               var18.write(var2);
               PacketTypes.PacketType.ConnectCoop.send(GameClient.connection);
               this.stage = AddCoopPlayer.Stage.ReceiveClientConnect;
            } else {
               this.stage = AddCoopPlayer.Stage.StartMapLoading;
            }
            break;
         case StartMapLoading:
            var1 = IsoWorld.instance.CurrentCell;
            int var20 = this.player.PlayerIndex;
            IsoChunkMap var22 = var1.ChunkMap[var20];
            IsoChunkMap.bSettingChunk.lock();

            try {
               var22.Unload();
               var22.ignore = false;
               var4 = (int)(this.player.getX() / 8.0F);
               int var5 = (int)(this.player.getY() / 8.0F);

               try {
                  if (LightingJNI.init) {
                     LightingJNI.teleport(var20, var4 - IsoChunkMap.ChunkGridWidth / 2, var5 - IsoChunkMap.ChunkGridWidth / 2);
                  }
               } catch (Exception var16) {
               }

               if (!GameServer.bServer && !GameClient.bClient) {
                  ZombiePopulationManager.instance.playerSpawnedAt(PZMath.fastfloor(this.player.getX()), PZMath.fastfloor(this.player.getY()), PZMath.fastfloor(this.player.getZ()));
               }

               var22.WorldX = var4;
               var22.WorldY = var5;
               if (!GameServer.bServer) {
                  WorldSimulation.instance.activateChunkMap(var20);
               }

               int var6 = var4 - IsoChunkMap.ChunkGridWidth / 2;
               int var7 = var5 - IsoChunkMap.ChunkGridWidth / 2;
               int var8 = var4 + IsoChunkMap.ChunkGridWidth / 2 + 1;
               int var9 = var5 + IsoChunkMap.ChunkGridWidth / 2 + 1;
               int var10 = var6;

               while(true) {
                  if (var10 >= var8) {
                     var22.SwapChunkBuffers();
                     break;
                  }

                  for(int var11 = var7; var11 < var9; ++var11) {
                     if (IsoWorld.instance.getMetaGrid().isValidChunk(var10, var11)) {
                        IsoChunk var12 = var22.LoadChunkForLater(var10, var11, var10 - var6, var11 - var7);
                        if (var12 != null && var12.bLoaded) {
                           var1.setCacheChunk(var12, var20);
                        }
                     }
                  }

                  ++var10;
               }
            } finally {
               IsoChunkMap.bSettingChunk.unlock();
            }

            this.stage = AddCoopPlayer.Stage.CheckMapLoading;
            break;
         case CheckMapLoading:
            var1 = IsoWorld.instance.CurrentCell;
            IsoChunkMap var19 = var1.ChunkMap[this.player.PlayerIndex];
            var19.update();

            for(int var3 = 0; var3 < IsoChunkMap.ChunkGridWidth; ++var3) {
               for(var4 = 0; var4 < IsoChunkMap.ChunkGridWidth; ++var4) {
                  if (IsoWorld.instance.getMetaGrid().isValidChunk(var19.getWorldXMin() + var4, var19.getWorldYMin() + var3) && var19.getChunk(var4, var3) == null) {
                     return;
                  }
               }
            }

            var19.calculateZExtentsForChunkMap();
            IsoGridSquare var21 = var1.getGridSquare(PZMath.fastfloor(this.player.getX()), PZMath.fastfloor(this.player.getY()), PZMath.fastfloor(this.player.getZ()));
            if (var21 != null && var21.getRoom() != null) {
               var21.getRoom().def.setExplored(true);
               var21.getRoom().building.setAllExplored(true);
            }

            this.stage = GameClient.bClient ? AddCoopPlayer.Stage.SendPlayerConnect : AddCoopPlayer.Stage.AddToWorld;
            break;
         case SendPlayerConnect:
            GameClient.connection.username = this.player.username;
            var18 = new ConnectCoopPacket();
            var18.setPlayerConnect(this.player);
            var2 = GameClient.connection.startPacket();
            PacketTypes.PacketType.ConnectCoop.doPacket(var2);
            var18.write(var2);
            PacketTypes.PacketType.ConnectCoop.send(GameClient.connection);
            this.stage = AddCoopPlayer.Stage.ReceivePlayerConnect;
            break;
         case AddToWorld:
            IsoPlayer.players[this.player.PlayerIndex] = this.player;
            LosUtil.cachecleared[this.player.PlayerIndex] = true;
            this.player.updateLightInfo();
            var1 = IsoWorld.instance.CurrentCell;
            this.player.setCurrent(var1.getGridSquare(PZMath.fastfloor(this.player.getX()), PZMath.fastfloor(this.player.getY()), PZMath.fastfloor(this.player.getZ())));
            this.player.updateUsername();
            this.player.setSceneCulled(false);
            if (var1.isSafeToAdd()) {
               var1.getObjectList().add(this.player);
            } else {
               var1.getAddList().add(this.player);
            }

            this.player.getInventory().addItemsToProcessItems();
            LuaEventManager.triggerEvent("OnCreatePlayer", this.player.PlayerIndex, this.player);
            if (this.player.isAsleep()) {
               UIManager.setFadeBeforeUI(this.player.PlayerIndex, true);
               UIManager.FadeOut((double)this.player.PlayerIndex, 2.0);
               UIManager.setFadeTime((double)this.player.PlayerIndex, 0.0);
            }

            this.stage = AddCoopPlayer.Stage.Finished;
            SoundManager.instance.stopMusic(IsoPlayer.DEATH_MUSIC_NAME);
         case ReceiveCoopConnect:
         case ReceiveClientConnect:
         case ReceivePlayerConnect:
         case Finished:
      }

   }

   public boolean isFinished() {
      return this.stage == AddCoopPlayer.Stage.Finished;
   }

   public void playerCreated(int var1) {
      if (this.player.PlayerIndex == var1) {
         DebugLog.Multiplayer.debugln("created player=%d", var1, Short.valueOf((short)4));
         this.stage = AddCoopPlayer.Stage.Init;
      }

   }

   public void accessGranted(int var1) {
      if (this.player.PlayerIndex == var1) {
         DebugLog.log("coop player=" + (var1 + 1) + "/4 access granted");
         this.stage = AddCoopPlayer.Stage.StartMapLoading;
      }

   }

   public void accessDenied(int var1, String var2) {
      if (this.player.PlayerIndex == var1) {
         DebugLog.log("coop player=" + (var1 + 1) + "/4 access denied: " + var2);
         IsoCell var3 = IsoWorld.instance.CurrentCell;
         int var4 = this.player.PlayerIndex;
         IsoChunkMap var5 = var3.ChunkMap[var4];
         var5.Unload();
         var5.ignore = true;
         this.stage = AddCoopPlayer.Stage.Finished;
         LuaEventManager.triggerEvent("OnCoopJoinFailed", var1);
      }

   }

   public void receivePlayerConnect(int var1) {
      if (this.player.PlayerIndex == var1) {
         this.stage = AddCoopPlayer.Stage.AddToWorld;
         this.update();
      }

   }

   public boolean isLoadingThisSquare(int var1, int var2) {
      int var3 = (int)(this.player.getX() / 8.0F);
      int var4 = (int)(this.player.getY() / 8.0F);
      int var5 = var3 - IsoChunkMap.ChunkGridWidth / 2;
      int var6 = var4 - IsoChunkMap.ChunkGridWidth / 2;
      int var7 = var5 + IsoChunkMap.ChunkGridWidth;
      int var8 = var6 + IsoChunkMap.ChunkGridWidth;
      var1 /= 8;
      var2 /= 8;
      return var1 >= var5 && var1 < var7 && var2 >= var6 && var2 < var8;
   }

   public static enum Stage {
      SendCoopConnect,
      ReceiveCoopConnect,
      Init,
      ReceiveClientConnect,
      StartMapLoading,
      CheckMapLoading,
      SendPlayerConnect,
      ReceivePlayerConnect,
      AddToWorld,
      Finished;

      private Stage() {
      }
   }
}
