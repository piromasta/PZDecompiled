package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.SandboxOptions;
import zombie.WorldSoundManager;
import zombie.Lua.LuaEventManager;
import zombie.audio.BaseSoundEmitter;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.math.PZMath;
import zombie.core.opengl.Shader;
import zombie.core.raknet.UdpConnection;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.model.ItemModelRenderer;
import zombie.core.skinnedmodel.model.WorldItemModelDrawer;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.core.utils.GameTimer;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.LosUtil;
import zombie.iso.areas.NonPvpZone;
import zombie.iso.fboRenderChunk.FBORenderChunk;
import zombie.iso.fboRenderChunk.FBORenderChunkManager;
import zombie.iso.fboRenderChunk.FBORenderItems;
import zombie.iso.fboRenderChunk.FBORenderLevels;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.util.StringUtils;

public class IsoTrap extends IsoObject {
   private int timerBeforeExplosion = 0;
   private int sensorRange = 0;
   private int firePower = 0;
   private int fireRange = 0;
   private int explosionPower = 0;
   private int explosionRange = 0;
   private int smokeRange = 0;
   private int noiseRange = 0;
   private int noiseDuration = 0;
   private float noiseStartTime = 0.0F;
   private float lastWorldSoundTime = 0.0F;
   private float extraDamage = 0.0F;
   private int remoteControlID = -1;
   private String countDownSound = null;
   private String explosionSound = null;
   private HandWeapon weapon;
   private boolean instantExplosion;
   private final GameTimer beep = new GameTimer(1);
   private int explosionDuration = 0;
   private float explosionStartTime = 0.0F;

   public IsoTrap(IsoCell var1) {
      super(var1);
   }

   public IsoTrap(HandWeapon var1, IsoCell var2, IsoGridSquare var3) {
      this.square = var3;
      this.initSprite(var1);
      this.setSensorRange(var1.getSensorRange());
      this.setFireRange(var1.getFireRange());
      this.setFirePower(var1.getFirePower());
      this.setExplosionPower(var1.getExplosionPower());
      this.setExplosionRange(var1.getExplosionRange());
      this.setSmokeRange(var1.getSmokeRange());
      this.setNoiseRange(var1.getNoiseRange());
      this.setNoiseDuration(var1.getNoiseDuration());
      this.setExtraDamage(var1.getExtraDamage());
      this.setRemoteControlID(var1.getRemoteControlID());
      this.setCountDownSound(var1.getCountDownSound());
      this.setExplosionSound(var1.getExplosionSound());
      this.setTimerBeforeExplosion(var1.getExplosionTimer());
      this.setInstantExplosion(var1.isInstantExplosion());
      this.setExplosionDuration(var1.getExplosionDuration());
      if (var1.canBePlaced()) {
         this.weapon = var1;
      }

   }

   private void initSprite(HandWeapon var1) {
      if (var1 != null) {
         String var2;
         if (var1.getPlacedSprite() != null && !var1.getPlacedSprite().isEmpty()) {
            var2 = var1.getPlacedSprite();
         } else if (var1.getTex() != null && var1.getTex().getName() != null) {
            var2 = var1.getTex().getName();
         } else {
            var2 = "media/inventory/world/WItem_Sack.png";
         }

         this.sprite = (IsoSprite)IsoSpriteManager.instance.NamedMap.get(var2);
         if (this.sprite == null) {
            this.sprite = IsoSprite.CreateSprite(IsoSpriteManager.instance);
            this.sprite.LoadSingleTexture(var2);
         }

         Texture var3 = this.sprite.getTextureForCurrentFrame(this.getDir());
         if (var2.startsWith("Item_") && var3 != null) {
            if (var1.getScriptItem() == null) {
               this.sprite.def.scaleAspect((float)var3.getWidthOrig(), (float)var3.getHeightOrig(), (float)(16 * Core.TileScale), (float)(16 * Core.TileScale));
            } else {
               float var10001 = (float)Core.TileScale;
               float var4 = var1.getScriptItem().ScaleWorldIcon * (var10001 / 2.0F);
               this.sprite.def.setScale(var4, var4);
            }
         }

      }
   }

   public void update() {
      if (!GameClient.bClient && this.timerBeforeExplosion > 0 && this.beep.check()) {
         --this.timerBeforeExplosion;
         if (this.timerBeforeExplosion == 0) {
            this.triggerExplosion(this.getSensorRange() > 0);
         } else if (this.getObjectIndex() != -1 && this.weapon.getType().endsWith("Triggered")) {
            String var1 = "TrapTimerLoop";
            if (!StringUtils.isNullOrWhitespace(this.getCountDownSound())) {
               var1 = this.getCountDownSound();
            } else if (this.timerBeforeExplosion == 1) {
               var1 = "TrapTimerExpired";
            }

            if (GameServer.bServer) {
               GameServer.PlayWorldSoundServer(var1, false, this.square, 1.0F, 10.0F, 1.0F, true);
            } else if (!GameClient.bClient) {
               this.getOrCreateEmitter();
               this.emitter.playSound(var1);
            }
         }
      }

      if (!this.updateExplosionDuration()) {
         this.updateVictimsInSensorRange();
         this.updateSounds();
      }
   }

   private boolean updateExplosionDuration() {
      if (!this.isExploding()) {
         return false;
      } else {
         float var1 = (float)GameTime.getInstance().getWorldAgeHours();
         this.explosionStartTime = PZMath.min(this.explosionStartTime, var1);
         float var2 = 60.0F / (float)SandboxOptions.getInstance().getDayLengthMinutes();
         float var3 = 60.0F;
         if (var1 - this.explosionStartTime > (float)this.getExplosionDuration() / var3 * var2) {
            this.explosionStartTime = 0.0F;
            if (this.emitter != null) {
               this.emitter.stopAll();
            }

            if (GameServer.bServer) {
               GameServer.RemoveItemFromMap(this);
            } else if (!GameClient.bClient) {
               this.removeFromWorld();
               this.removeFromSquare();
            }

            return true;
         } else {
            if (this.getSmokeRange() > 0 && this.getObjectIndex() != -1) {
               int var4 = this.getSmokeRange();

               for(int var5 = this.getSquare().getX() - var4; (float)var5 <= this.getX() + (float)var4; ++var5) {
                  for(int var6 = this.getSquare().getY() - var4; var6 <= this.getSquare().getY() + var4; ++var6) {
                     IsoGridSquare var7 = this.getCell().getGridSquare((double)var5, (double)var6, (double)this.getZ());
                     this.refreshSmokeBombSmoke(var7);
                  }
               }
            }

            return false;
         }
      }
   }

   private void refreshSmokeBombSmoke(IsoGridSquare var1) {
      if (var1 != null) {
         IsoFire var2 = var1.getFire();
         if (var2 != null && var2.bSmoke) {
            int var3 = this.getSmokeRange();
            if (!(IsoUtils.DistanceTo((float)var1.getX() + 0.5F, (float)var1.getY() + 0.5F, this.getX() + 0.5F, this.getY() + 0.5F) > (float)var3)) {
               LosUtil.TestResults var4 = LosUtil.lineClear(this.getCell(), PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()), var1.getX(), var1.getY(), var1.getZ(), false);
               if (var4 != LosUtil.TestResults.Blocked && var4 != LosUtil.TestResults.ClearThroughClosedDoor) {
                  if (NonPvpZone.getNonPvpZone(var1.getX(), var1.getY()) == null) {
                     var2.Energy = 40;
                     var2.Life = Rand.Next(var2.MinLife, var2.MaxLife) / 5;
                     var2.LifeStage = 4;
                     var2.LifeStageTimer = var2.LifeStageDuration = var2.Life / 4;
                     var1.smoke();
                  }
               }
            }
         }
      }
   }

   private void updateVictimsInSensorRange() {
      if ((GameServer.bServer || !GameClient.bClient) && this.getSensorRange() > 0) {
         Iterator var1 = this.getCell().getObjectList().iterator();

         while(var1.hasNext()) {
            IsoMovingObject var2 = (IsoMovingObject)var1.next();
            if (var2 instanceof IsoGameCharacter && !((IsoGameCharacter)var2).isInvisible() && IsoUtils.DistanceTo(var2.getX(), var2.getY(), this.getX(), this.getY()) <= (float)this.getSensorRange()) {
               if (GameServer.bServer) {
                  INetworkPacket.sendToAll(PacketTypes.PacketType.AddExplosiveTrap, (UdpConnection)null, null, this.getSquare());
               }

               this.getSquare().explodeTrap();
            }
         }
      }

   }

   private void updateSounds() {
      if (this.noiseStartTime > 0.0F) {
         float var1 = (float)GameTime.getInstance().getWorldAgeHours();
         this.noiseStartTime = PZMath.min(this.noiseStartTime, var1);
         this.lastWorldSoundTime = PZMath.min(this.lastWorldSoundTime, var1);
         float var2 = 60.0F / (float)SandboxOptions.getInstance().getDayLengthMinutes();
         float var3 = 60.0F;
         if (var1 - this.noiseStartTime > (float)this.getNoiseDuration() / var3 * var2) {
            this.noiseStartTime = 0.0F;
            if (this.emitter != null) {
               this.emitter.stopAll();
            }
         } else {
            if (!GameServer.bServer && (this.emitter == null || !this.emitter.isPlaying(this.getExplosionSound()))) {
               BaseSoundEmitter var4 = this.getOrCreateEmitter();
               if (var4 != null) {
                  var4.playSound(this.getExplosionSound());
               }
            }

            if (var1 - this.lastWorldSoundTime > 1.0F / var3 * var2 && this.getObjectIndex() != -1) {
               this.lastWorldSoundTime = var1;
               WorldSoundManager.instance.addSoundRepeating((Object)null, this.getSquare().getX(), this.getSquare().getY(), this.getSquare().getZ(), this.getNoiseRange(), 1, true);
            }
         }
      }

      if (this.emitter != null) {
         this.emitter.tick();
      }

   }

   public IsoGridSquare getRenderSquare() {
      if (this.getSquare() == null) {
         return null;
      } else {
         byte var1 = 8;
         if (PZMath.coordmodulo(this.square.x, var1) == 0 && PZMath.coordmodulo(this.square.y, var1) == var1 - 1) {
            return this.square.getAdjacentSquare(IsoDirections.S);
         } else {
            return PZMath.coordmodulo(this.square.x, var1) == var1 - 1 && PZMath.coordmodulo(this.square.y, var1) == 0 ? this.square.getAdjacentSquare(IsoDirections.E) : this.getSquare();
         }
      }
   }

   public void render(float var1, float var2, float var3, ColorInfo var4, boolean var5, boolean var6, Shader var7) {
      if (Core.getInstance().isOption3DGroundItem() && ItemModelRenderer.itemHasModel(this.getItem())) {
         if (PerformanceSettings.FBORenderChunk && FBORenderChunkManager.instance.isCaching()) {
            int var13 = IsoCamera.frameState.playerIndex;
            IsoGridSquare var14 = this.getRenderSquare();
            FBORenderLevels var15 = var14.chunk.getRenderLevels(var13);
            FBORenderChunk var11 = var15.getFBOForLevel(var14.z, Core.getInstance().getZoom(var13));
            FBORenderItems.getInstance().render(var11.index, this);
            return;
         }

         ItemModelRenderer.RenderStatus var8 = WorldItemModelDrawer.renderMain(this.getItem(), this.getSquare(), this.getRenderSquare(), this.getX() + 0.5F, this.getY() + 0.5F, this.getZ(), 0.0F, -1.0F, true);
         if (var8 == ItemModelRenderer.RenderStatus.Loading || var8 == ItemModelRenderer.RenderStatus.Ready) {
            return;
         }
      }

      if (!this.sprite.hasNoTextures()) {
         Texture var12 = this.sprite.getTextureForCurrentFrame(this.dir);
         if (var12 != null) {
            if (var12.getName().startsWith("Item_")) {
               float var9 = (float)var12.getWidthOrig() * this.sprite.def.getScaleX() / 2.0F;
               float var10 = (float)var12.getHeightOrig() * this.sprite.def.getScaleY() * 3.0F / 4.0F;
               this.setAlphaAndTarget(1.0F);
               this.offsetX = 0.0F;
               this.offsetY = 0.0F;
               this.sx = 0.0F;
               this.sprite.render(this, var1 + 0.5F, var2 + 0.5F, var3, this.dir, this.offsetX + var9, this.offsetY + var10, var4, true);
            } else {
               this.offsetX = (float)(32 * Core.TileScale);
               this.offsetY = (float)(96 * Core.TileScale);
               this.sx = 0.0F;
               super.render(var1, var2, var3, var4, var5, var6, var7);
            }

         }
      }
   }

   public void triggerExplosion(boolean var1) {
      LuaEventManager.triggerEvent("OnThrowableExplode", this, this.square);
      this.timerBeforeExplosion = 0;
      if (var1) {
         if (this.getSensorRange() > 0) {
            this.square.setTrapPositionX(this.square.getX());
            this.square.setTrapPositionY(this.square.getY());
            this.square.setTrapPositionZ(this.square.getZ());
            this.square.drawCircleExplosion(this.getSensorRange(), this, IsoTrap.ExplosionMode.Sensor);
         }
      } else {
         if (!GameServer.bServer && this.square != null && this.getObjectIndex() == -1) {
            this.square.AddTileObject(this);
         }

         if (this.getExplosionSound() != null) {
            this.playExplosionSound();
         }

         if (this.getNoiseRange() > 0) {
            WorldSoundManager.instance.addSound((Object)null, (int)this.getX(), (int)this.getY(), (int)this.getZ(), this.getNoiseRange(), 1);
         } else if (this.getExplosionSound() != null) {
            WorldSoundManager.instance.addSound((Object)null, (int)this.getX(), (int)this.getY(), (int)this.getZ(), 50, 1);
         }

         if (this.getExplosionRange() > 0) {
            this.square.drawCircleExplosion(this.getExplosionRange(), this, IsoTrap.ExplosionMode.Explosion);
         }

         if (this.getFireRange() > 0) {
            this.square.drawCircleExplosion(this.getFireRange(), this, IsoTrap.ExplosionMode.Fire);
         }

         if (this.getSmokeRange() > 0) {
            this.square.drawCircleExplosion(this.getSmokeRange(), this, IsoTrap.ExplosionMode.Smoke);
         }

         if (this.getExplosionDuration() > 0) {
            if (this.explosionStartTime == 0.0F) {
               this.explosionStartTime = (float)GameTime.getInstance().getWorldAgeHours();
            }

            return;
         }

         if (this.weapon == null || !this.weapon.canBeReused()) {
            if (GameServer.bServer) {
               GameServer.RemoveItemFromMap(this);
            } else {
               this.removeFromWorld();
               this.removeFromSquare();
            }
         }
      }

   }

   private BaseSoundEmitter getOrCreateEmitter() {
      if (this.getObjectIndex() == -1) {
         return null;
      } else {
         if (this.emitter == null) {
            this.emitter = IsoWorld.instance.getFreeEmitter(this.getX() + 0.5F, this.getY() + 0.5F, this.getZ());
            IsoWorld.instance.takeOwnershipOfEmitter(this.emitter);
         }

         return this.emitter;
      }
   }

   public void playExplosionSound() {
      if (!StringUtils.isNullOrWhitespace(this.getExplosionSound())) {
         if (this.getObjectIndex() != -1) {
            if (this.getNoiseRange() > 0 && (float)this.getNoiseDuration() > 0.0F) {
               this.noiseStartTime = (float)GameTime.getInstance().getWorldAgeHours();
            }

            if (!GameServer.bServer) {
               this.getOrCreateEmitter();
               if (!this.emitter.isPlaying(this.getExplosionSound())) {
                  this.emitter.playSoundImpl(this.getExplosionSound(), (IsoObject)null);
               }

            } else {
               if (this.getNoiseRange() > 0 && (float)this.getNoiseDuration() > 0.0F) {
                  GameServer.PlayWorldSoundServer(this.getExplosionSound(), this.getSquare(), (float)this.getNoiseRange(), this.getObjectIndex());
               } else {
                  GameServer.PlayWorldSoundServer(this.getExplosionSound(), false, this.getSquare(), 0.0F, 50.0F, 1.0F, false);
               }

            }
         }
      }
   }

   public void load(ByteBuffer var1, int var2, boolean var3) throws IOException {
      super.load(var1, var2, var3);
      this.sensorRange = var1.getInt();
      this.firePower = var1.getInt();
      this.fireRange = var1.getInt();
      this.explosionPower = var1.getInt();
      this.explosionRange = var1.getInt();
      if (var2 >= 219) {
         this.explosionDuration = var1.getInt();
         this.explosionStartTime = var1.getFloat();
      }

      this.smokeRange = var1.getInt();
      this.noiseRange = var1.getInt();
      this.noiseDuration = var1.getInt();
      this.noiseStartTime = var1.getFloat();
      this.extraDamage = var1.getFloat();
      this.remoteControlID = var1.getInt();
      this.timerBeforeExplosion = var1.getInt();
      this.countDownSound = GameWindow.ReadStringUTF(var1);
      this.explosionSound = GameWindow.ReadStringUTF(var1);
      if ("bigExplosion".equals(this.explosionSound)) {
         this.explosionSound = "BigExplosion";
      }

      if ("smallExplosion".equals(this.explosionSound)) {
         this.explosionSound = "SmallExplosion";
      }

      if ("feedback".equals(this.explosionSound)) {
         this.explosionSound = "NoiseTrapExplosion";
      }

      boolean var4 = var1.get() == 1;
      if (var4) {
         InventoryItem var5 = InventoryItem.loadItem(var1, var2);
         if (var5 instanceof HandWeapon) {
            this.weapon = (HandWeapon)var5;
            this.initSprite(this.weapon);
         }
      }

   }

   public void save(ByteBuffer var1, boolean var2) throws IOException {
      super.save(var1, var2);
      var1.putInt(this.sensorRange);
      var1.putInt(this.firePower);
      var1.putInt(this.fireRange);
      var1.putInt(this.explosionPower);
      var1.putInt(this.explosionRange);
      var1.putInt(this.explosionDuration);
      var1.putFloat(this.explosionStartTime);
      var1.putInt(this.smokeRange);
      var1.putInt(this.noiseRange);
      var1.putInt(this.noiseDuration);
      var1.putFloat(this.noiseStartTime);
      var1.putFloat(this.extraDamage);
      var1.putInt(this.remoteControlID);
      var1.putInt(this.timerBeforeExplosion);
      GameWindow.WriteStringUTF(var1, this.countDownSound);
      GameWindow.WriteStringUTF(var1, this.explosionSound);
      if (this.weapon != null) {
         var1.put((byte)1);
         this.weapon.saveWithSize(var1, false);
      } else {
         var1.put((byte)0);
      }

   }

   public void addToWorld() {
      this.getCell().addToProcessIsoObject(this);
   }

   public void removeFromWorld() {
      if (this.emitter != null) {
         if (this.noiseStartTime > 0.0F) {
            this.emitter.stopAll();
         }

         IsoWorld.instance.returnOwnershipOfEmitter(this.emitter);
         this.emitter = null;
      }

      super.removeFromWorld();
   }

   public int getTimerBeforeExplosion() {
      return this.timerBeforeExplosion;
   }

   public void setTimerBeforeExplosion(int var1) {
      this.timerBeforeExplosion = var1;
   }

   public int getSensorRange() {
      return this.sensorRange;
   }

   public void setSensorRange(int var1) {
      this.sensorRange = var1;
   }

   public int getFireRange() {
      return this.fireRange;
   }

   public void setFireRange(int var1) {
      this.fireRange = var1;
   }

   public int getFirePower() {
      return this.firePower;
   }

   public void setFirePower(int var1) {
      this.firePower = var1;
   }

   public int getExplosionPower() {
      return this.explosionPower;
   }

   public void setExplosionPower(int var1) {
      this.explosionPower = var1;
   }

   public int getNoiseDuration() {
      return this.noiseDuration;
   }

   public void setNoiseDuration(int var1) {
      this.noiseDuration = var1;
   }

   public int getNoiseRange() {
      return this.noiseRange;
   }

   public void setNoiseRange(int var1) {
      this.noiseRange = var1;
   }

   public int getExplosionRange() {
      return this.explosionRange;
   }

   public void setExplosionRange(int var1) {
      this.explosionRange = var1;
   }

   public int getSmokeRange() {
      return this.smokeRange;
   }

   public void setSmokeRange(int var1) {
      this.smokeRange = var1;
   }

   public float getExtraDamage() {
      return this.extraDamage;
   }

   public void setExtraDamage(float var1) {
      this.extraDamage = var1;
   }

   public String getObjectName() {
      return "IsoTrap";
   }

   public int getRemoteControlID() {
      return this.remoteControlID;
   }

   public void setRemoteControlID(int var1) {
      this.remoteControlID = var1;
   }

   public String getCountDownSound() {
      return this.countDownSound;
   }

   public void setCountDownSound(String var1) {
      this.countDownSound = var1;
   }

   public String getExplosionSound() {
      return this.explosionSound;
   }

   public void setExplosionSound(String var1) {
      this.explosionSound = var1;
   }

   public int getExplosionDuration() {
      return this.explosionDuration;
   }

   public void setExplosionDuration(int var1) {
      this.explosionDuration = var1;
   }

   public boolean isExploding() {
      return this.getExplosionDuration() > 0 && this.explosionStartTime > 0.0F;
   }

   public InventoryItem getItem() {
      return this.weapon;
   }

   public static void triggerRemote(IsoPlayer var0, int var1, int var2) {
      int var3 = (int)var0.getX();
      int var4 = (int)var0.getY();
      int var5 = (int)var0.getZ();
      int var6 = Math.max(var5 - var2 / 2, 0);
      int var7 = Math.min(var5 + var2 / 2, 8);
      IsoCell var8 = IsoWorld.instance.CurrentCell;

      for(int var9 = var6; var9 < var7; ++var9) {
         for(int var10 = var4 - var2; var10 < var4 + var2; ++var10) {
            for(int var11 = var3 - var2; var11 < var3 + var2; ++var11) {
               IsoGridSquare var12 = var8.getGridSquare(var11, var10, var9);
               if (var12 != null) {
                  for(int var13 = var12.getObjects().size() - 1; var13 >= 0; --var13) {
                     IsoObject var14 = (IsoObject)var12.getObjects().get(var13);
                     if (var14 instanceof IsoTrap && ((IsoTrap)var14).getRemoteControlID() == var1) {
                        ((IsoTrap)var14).triggerExplosion(false);
                     }
                  }
               }
            }
         }
      }

   }

   public boolean isInstantExplosion() {
      return this.instantExplosion;
   }

   public void setInstantExplosion(boolean var1) {
      this.instantExplosion = var1;
   }

   public static enum ExplosionMode {
      Explosion,
      Fire,
      Smoke,
      Sensor;

      private ExplosionMode() {
      }
   }
}
