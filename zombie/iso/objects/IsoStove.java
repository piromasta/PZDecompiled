package zombie.iso.objects;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import zombie.GameTime;
import zombie.SoundManager;
import zombie.audio.BaseSoundEmitter;
import zombie.core.ImportantAreaManager;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.fboRenderChunk.FBORenderChunk;
import zombie.iso.objects.interfaces.Activatable;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteGrid;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.util.Type;

public class IsoStove extends IsoObject implements Activatable {
   private static final ArrayList<IsoObject> s_tempObjects = new ArrayList();
   boolean activated = false;
   long soundInstance = -1L;
   private float maxTemperature = 0.0F;
   private double stopTime;
   private double startTime;
   private float currentTemperature = 0.0F;
   private int secondsTimer = -1;
   private boolean firstTurnOn = true;
   private boolean broken = false;
   private boolean hasMetal = false;

   public IsoStove(IsoCell var1, IsoGridSquare var2, IsoSprite var3) {
      super(var1, var2, var3);
   }

   public String getObjectName() {
      return "Stove";
   }

   public IsoStove(IsoCell var1) {
      super(var1);
   }

   public boolean Activated() {
      return this.activated;
   }

   public void update() {
      if (this.Activated() && (this.container == null || !this.container.isPowered())) {
         this.setActivated(false);
         if (this.container != null) {
            this.container.addItemsToProcessItems();
         }
      }

      if (this.Activated() && this.isMicrowave() && this.stopTime > 0.0 && this.stopTime < GameTime.instance.getWorldAgeHours()) {
         this.setActivated(false);
      }

      boolean var1 = GameServer.bServer || !GameClient.bClient && !GameServer.bServer;
      if (var1 && this.Activated() && this.hasMetal && Rand.Next(Rand.AdjustForFramerate(200)) == 100) {
         this.secondsTimer = -1;
         if (this.emitter != null && this.soundInstance != -1L) {
            this.emitter.stopSound(this.soundInstance);
            this.soundInstance = -1L;
         }

         this.setActivated(false);
         this.setBroken(true);
         IsoFireManager.StartFire(this.container.SourceGrid.getCell(), this.container.SourceGrid, true, 10000);
      }

      if (this.Activated()) {
         if (this.hasMetal || this.stopTime != 0.0 || this.currentTemperature < this.getMaxTemperature() || this.container.getItems().size() > 0) {
            ImportantAreaManager.getInstance().updateOrAdd((int)this.getX(), (int)this.getY());
         }

         if (this.stopTime > 0.0 && this.stopTime < GameTime.instance.getWorldAgeHours()) {
            boolean var2 = this.getContainer() != null && this.getContainer().isStove();
            if (!this.isMicrowave() && var2 && this.isSpriteGridOriginObject()) {
               BaseSoundEmitter var3 = IsoWorld.instance.getFreeEmitter(this.getX() + 0.5F, this.getY() + 0.5F, (float)((int)this.getZ()));
               var3.playSoundImpl("StoveTimerExpired", (IsoObject)this);
            }

            this.stopTime = 0.0;
            this.startTime = 0.0;
            this.secondsTimer = -1;
         }

         if (this.getMaxTemperature() > 0.0F && this.currentTemperature < this.getMaxTemperature()) {
            float var4 = (this.getMaxTemperature() - this.currentTemperature) / 700.0F;
            if (var4 < 0.05F) {
               var4 = 0.05F;
            }

            this.currentTemperature += var4 * GameTime.instance.getMultiplier();
            if (this.currentTemperature > this.getMaxTemperature()) {
               this.currentTemperature = this.getMaxTemperature();
            }
         } else if (this.currentTemperature > this.getMaxTemperature()) {
            this.currentTemperature -= (this.currentTemperature - this.getMaxTemperature()) / 1000.0F * GameTime.instance.getMultiplier();
            if (this.currentTemperature < 0.0F) {
               this.currentTemperature = 0.0F;
            }
         }
      } else if (this.currentTemperature > 0.0F) {
         this.currentTemperature -= 0.1F * GameTime.instance.getMultiplier();
         this.currentTemperature = Math.max(this.currentTemperature, 0.0F);
      }

      if (this.container != null && this.isMicrowave()) {
         if (this.Activated()) {
            this.currentTemperature = this.getMaxTemperature();
         } else {
            this.currentTemperature = 0.0F;
         }
      }

      if (this.isSpriteGridOriginObject() && this.emitter != null) {
         if (this.Activated() && this.secondsTimer > 0) {
            if (!this.emitter.isPlaying("StoveTimer")) {
               this.emitter.playSoundImpl("StoveTimer", (IsoObject)this);
            }
         } else if (this.emitter.isPlaying("StoveTimer")) {
            this.emitter.stopSoundByName("StoveTimer");
         }
      }

      this.checkLightSourceActive();
   }

   public void load(ByteBuffer var1, int var2, boolean var3) throws IOException {
      super.load(var1, var2, var3);
      this.activated = var1.get() == 1;
      this.secondsTimer = var1.getInt();
      this.maxTemperature = var1.getFloat();
      this.firstTurnOn = var1.get() == 1;
      this.broken = var1.get() == 1;
   }

   public void save(ByteBuffer var1, boolean var2) throws IOException {
      super.save(var1, var2);
      var1.put((byte)(this.activated ? 1 : 0));
      var1.putInt(this.secondsTimer);
      var1.putFloat(this.maxTemperature);
      var1.put((byte)(this.firstTurnOn ? 1 : 0));
      var1.put((byte)(this.broken ? 1 : 0));
   }

   public void addToWorld() {
      if (this.container != null) {
         IsoCell var1 = this.getCell();
         var1.addToProcessIsoObject(this);
         this.container.addItemsToProcessItems();
         this.setActivated(this.activated);
         this.addLightSourceToWorld();
      }
   }

   public void Toggle() {
      this.setActivated(!this.activated);
      this.container.addItemsToProcessItems();
      IsoGenerator.updateGenerator(this.square);
   }

   public void PlayToggleSound() {
      SoundManager.instance.PlayWorldSound(this.isMicrowave() ? "ToggleMicrowave" : "ToggleStove", this.getSquare(), 1.0F, 1.0F, 1.0F, false);
   }

   public void sync() {
      this.syncIsoObject(false, (byte)(this.activated ? 1 : 0), (UdpConnection)null, (ByteBuffer)null);
   }

   private void doSound() {
      if (GameServer.bServer) {
         this.hasMetal();
      } else if (this.isSpriteGridOriginObject()) {
         boolean var1 = this.getContainer() != null && this.getContainer().isStove();
         if (this.isMicrowave()) {
            if (this.activated) {
               if (this.emitter != null) {
                  if (this.soundInstance != -1L) {
                     this.emitter.stopSound(this.soundInstance);
                  }

                  this.emitter.stopSoundByName("StoveTimer");
               }

               this.emitter = IsoWorld.instance.getFreeEmitter(this.getX() + 0.5F, this.getY() + 0.5F, (float)PZMath.fastfloor(this.getZ()));
               IsoWorld.instance.setEmitterOwner(this.emitter, this);
               if (this.hasMetal()) {
                  this.soundInstance = this.emitter.playSoundLoopedImpl("MicrowaveCookingMetal");
               } else {
                  this.soundInstance = this.emitter.playSoundLoopedImpl("MicrowaveRunning");
               }
            } else if (this.soundInstance != -1L) {
               if (this.emitter != null) {
                  this.emitter.stopSound(this.soundInstance);
                  this.emitter.stopSoundByName("StoveTimer");
                  this.emitter = null;
               }

               this.soundInstance = -1L;
               if (this.container != null && this.container.isPowered()) {
                  BaseSoundEmitter var2 = IsoWorld.instance.getFreeEmitter(this.getX() + 0.5F, this.getY() + 0.5F, (float)PZMath.fastfloor(this.getZ()));
                  var2.playSoundImpl("MicrowaveTimerExpired", (IsoObject)this);
               }
            }
         } else if (this.getContainer() != null && var1) {
            if (this.Activated()) {
               if (this.emitter == null) {
                  this.emitter = IsoWorld.instance.getFreeEmitter(this.getX() + 0.5F, this.getY() + 0.5F, (float)((int)this.getZ()));
                  IsoWorld.instance.setEmitterOwner(this.emitter, this);
                  this.soundInstance = this.emitter.playSoundLoopedImpl("StoveRunning");
               } else if (!this.emitter.isPlaying("StoveRunning")) {
                  this.soundInstance = this.emitter.playSoundLoopedImpl("StoveRunning");
               }
            } else if (this.soundInstance != -1L) {
               if (this.emitter != null) {
                  this.emitter.stopSound(this.soundInstance);
                  this.emitter.stopSoundByName("StoveTimer");
                  this.emitter = null;
               }

               this.soundInstance = -1L;
            }
         }

      }
   }

   private boolean hasMetal() {
      int var1 = this.getContainer().getItems().size();

      for(int var2 = 0; var2 < var1; ++var2) {
         InventoryItem var3 = (InventoryItem)this.getContainer().getItems().get(var2);
         if (var3.getMetalValue() > 0.0F || var3.hasTag("HasMetal")) {
            this.hasMetal = true;
            return true;
         }
      }

      this.hasMetal = false;
      return false;
   }

   public String getActivatableType() {
      return "stove";
   }

   public void syncIsoObjectSend(ByteBufferWriter var1) {
      var1.putInt(this.square.getX());
      var1.putInt(this.square.getY());
      var1.putInt(this.square.getZ());
      byte var2 = (byte)this.square.getObjects().indexOf(this);
      var1.putByte(var2);
      var1.putByte((byte)1);
      var1.putByte((byte)(this.activated ? 1 : 0));
      var1.putInt(this.secondsTimer);
      var1.putFloat(this.maxTemperature);
   }

   public void syncIsoObject(boolean var1, byte var2, UdpConnection var3, ByteBuffer var4) {
      if (this.square == null) {
         System.out.println("ERROR: " + this.getClass().getSimpleName() + " square is null");
      } else if (this.getObjectIndex() == -1) {
         PrintStream var10000 = System.out;
         String var10001 = this.getClass().getSimpleName();
         var10000.println("ERROR: " + var10001 + " not found on square " + this.square.getX() + "," + this.square.getY() + "," + this.square.getZ());
      } else {
         if (GameClient.bClient && !var1) {
            ByteBufferWriter var8 = GameClient.connection.startPacket();
            PacketTypes.PacketType.SyncIsoObject.doPacket(var8);
            this.syncIsoObjectSend(var8);
            PacketTypes.PacketType.SyncIsoObject.send(GameClient.connection);
         } else if (var1) {
            boolean var5 = var2 == 1;
            this.secondsTimer = var4.getInt();
            this.maxTemperature = var4.getFloat();
            this.setActivated(var5);
            this.container.addItemsToProcessItems();
         }

         if (GameServer.bServer) {
            Iterator var9 = GameServer.udpEngine.connections.iterator();

            while(true) {
               UdpConnection var6;
               do {
                  if (!var9.hasNext()) {
                     return;
                  }

                  var6 = (UdpConnection)var9.next();
               } while(var3 != null && var6.getConnectedGUID() == var3.getConnectedGUID());

               ByteBufferWriter var7 = var6.startPacket();
               PacketTypes.PacketType.SyncIsoObject.doPacket(var7);
               this.syncIsoObjectSend(var7);
               PacketTypes.PacketType.SyncIsoObject.send(var6);
            }
         }
      }
   }

   public void setActivated(boolean var1) {
      if (!this.isBroken()) {
         this.activated = var1;
         if (this.firstTurnOn && this.getMaxTemperature() == 0.0F) {
            boolean var2 = this.getContainer() != null && this.getContainer().isStove();
            if (this.isMicrowave() && this.secondsTimer < 0) {
               this.maxTemperature = 100.0F;
            }

            if (var2 && this.secondsTimer < 0) {
               this.maxTemperature = 200.0F;
            }
         }

         if (this.firstTurnOn) {
            this.firstTurnOn = false;
         }

         if (this.activated) {
            if (this.isMicrowave() && this.secondsTimer < 0) {
               this.secondsTimer = 3600;
            }

            if (this.secondsTimer > 0) {
               this.startTime = GameTime.instance.getWorldAgeHours();
               this.stopTime = this.startTime + (double)this.secondsTimer / 3600.0;
            }
         } else {
            this.stopTime = 0.0;
            this.startTime = 0.0;
            this.hasMetal = false;
         }

         this.doSound();
         if (this.getOnOverlay() != null) {
            this.invalidateRenderChunkLevel(FBORenderChunk.DIRTY_OBJECT_MODIFY);
         }

         if (GameServer.bServer) {
            this.sync();
            this.syncSpriteGridObjects(true, true);
         }

      }
   }

   private void doOverlay() {
      if (this.Activated() && this.getOverlaySprite() == null) {
         String[] var1 = this.getSprite().getName().split("_");
         String var2 = var1[0] + "_" + var1[1] + "_ON_" + var1[2] + "_" + var1[3];
         this.setOverlaySprite(var2);
      } else if (!this.Activated()) {
         this.setOverlaySprite((String)null);
      }

   }

   public void setTimer(int var1) {
      this.secondsTimer = var1;
      if (this.activated && this.secondsTimer > 0) {
         this.startTime = GameTime.instance.getWorldAgeHours();
         this.stopTime = this.startTime + (double)this.secondsTimer / 3600.0;
      }

   }

   public int getTimer() {
      return this.secondsTimer;
   }

   public float getMaxTemperature() {
      return this.maxTemperature;
   }

   public void setMaxTemperature(float var1) {
      this.maxTemperature = var1;
   }

   public boolean isMicrowave() {
      return this.getContainer() != null && this.getContainer().isMicrowave();
   }

   public int isRunningFor() {
      return this.startTime == 0.0 ? 0 : (int)((GameTime.instance.getWorldAgeHours() - this.startTime) * 3600.0);
   }

   public float getCurrentTemperature() {
      return this.currentTemperature + 100.0F;
   }

   public boolean isTemperatureChanging() {
      return this.currentTemperature != (this.activated ? this.maxTemperature : 0.0F);
   }

   public boolean isBroken() {
      return this.broken;
   }

   public void setBroken(boolean var1) {
      this.broken = var1;
   }

   private boolean isSpriteGridOriginObject() {
      IsoSprite var1 = this.getSprite();
      if (var1 == null) {
         return false;
      } else {
         IsoSpriteGrid var2 = var1.getSpriteGrid();
         if (var2 == null) {
            return true;
         } else {
            int var3 = var2.getSpriteGridPosX(var1);
            int var4 = var2.getSpriteGridPosY(var1);
            return var3 == 0 && var4 == 0;
         }
      }
   }

   public void syncSpriteGridObjects(boolean var1, boolean var2) {
      this.getSpriteGridObjects(s_tempObjects);

      for(int var3 = s_tempObjects.size() - 1; var3 >= 0; --var3) {
         IsoStove var4 = (IsoStove)Type.tryCastTo((IsoObject)s_tempObjects.get(var3), IsoStove.class);
         if (var4 != null && var4 != this) {
            var4.activated = this.activated;
            var4.maxTemperature = this.maxTemperature;
            var4.firstTurnOn = this.firstTurnOn;
            var4.secondsTimer = this.secondsTimer;
            var4.startTime = this.startTime;
            var4.stopTime = this.stopTime;
            var4.hasMetal = this.hasMetal;
            var4.doSound();
            if (var1) {
               if (var4.container != null) {
                  var4.container.addItemsToProcessItems();
               }

               IsoGenerator.updateGenerator(var4.square);
            }

            if (var2) {
               var4.sync();
            }
         }
      }

   }

   public boolean shouldShowOnOverlay() {
      if (!this.Activated()) {
         return false;
      } else {
         int var1 = IsoCamera.frameState.playerIndex;
         return this.getSquare() != null && this.getSquare().isSeen(var1);
      }
   }

   protected boolean shouldLightSourceBeActive() {
      return this.Activated();
   }
}
