package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.WorldSoundManager;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characterTextures.BloodClothingType;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.Clothing;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.objects.interfaces.IClothingWasherDryerLogic;
import zombie.network.GameClient;
import zombie.network.GameServer;

public final class ClothingWasherLogic implements IClothingWasherDryerLogic {
   private final IsoObject m_object;
   private boolean bActivated;
   private long soundInstance = -1L;
   private float lastUpdate = -1.0F;
   private boolean cycleFinished = false;
   private float startTime = 0.0F;
   private float cycleLengthMinutes = 90.0F;
   private boolean alreadyExecuted = false;

   public ClothingWasherLogic(IsoObject var1) {
      this.m_object = var1;
   }

   public IsoObject getObject() {
      return this.m_object;
   }

   public void load(ByteBuffer var1, int var2, boolean var3) throws IOException {
      this.bActivated = var1.get() == 1;
      this.lastUpdate = var1.getFloat();
   }

   public void save(ByteBuffer var1, boolean var2) throws IOException {
      var1.put((byte)(this.isActivated() ? 1 : 0));
      var1.putFloat(this.lastUpdate);
   }

   public void update() {
      if (this.getObject().getObjectIndex() != -1) {
         if (!this.getContainer().isPowered()) {
            this.setActivated(false);
         }

         this.updateSound();
         this.cycleFinished();
         if (GameClient.bClient) {
         }

         if (this.getObject().getWaterAmount() <= 0) {
            this.setActivated(false);
         }

         if (!this.isActivated()) {
            this.lastUpdate = -1.0F;
         } else {
            float var1 = (float)GameTime.getInstance().getWorldAgeHours();
            if (this.lastUpdate < 0.0F) {
               this.lastUpdate = var1;
            } else if (this.lastUpdate > var1) {
               this.lastUpdate = var1;
            }

            float var2 = var1 - this.lastUpdate;
            int var3 = (int)(var2 * 60.0F);
            if (var3 >= 1) {
               this.lastUpdate = var1;
               this.getObject().useWater(1 * var3);

               for(int var4 = 0; var4 < this.getContainer().getItems().size(); ++var4) {
                  InventoryItem var5 = (InventoryItem)this.getContainer().getItems().get(var4);
                  if (var5 instanceof Clothing) {
                     Clothing var6 = (Clothing)var5;
                     float var7 = var6.getBloodlevel();
                     if (var7 > 0.0F) {
                        this.removeBlood(var6, (float)(var3 * 2));
                     }

                     float var8 = var6.getDirtyness();
                     if (var8 > 0.0F) {
                        this.removeDirt(var6, (float)(var3 * 2));
                     }

                     var6.setWetness(100.0F);
                  }
               }

            }
         }
      }
   }

   private void removeBlood(Clothing var1, float var2) {
      ItemVisual var3 = var1.getVisual();
      if (var3 != null) {
         for(int var4 = 0; var4 < BloodBodyPartType.MAX.index(); ++var4) {
            BloodBodyPartType var5 = BloodBodyPartType.FromIndex(var4);
            float var6 = var3.getBlood(var5);
            if (var6 > 0.0F) {
               var3.setBlood(var5, var6 - var2 / 100.0F);
            }
         }

         BloodClothingType.calcTotalBloodLevel(var1);
      }
   }

   private void removeDirt(Clothing var1, float var2) {
      ItemVisual var3 = var1.getVisual();
      if (var3 != null) {
         for(int var4 = 0; var4 < BloodBodyPartType.MAX.index(); ++var4) {
            BloodBodyPartType var5 = BloodBodyPartType.FromIndex(var4);
            float var6 = var3.getDirt(var5);
            if (var6 > 0.0F) {
               var3.setDirt(var5, var6 - var2 / 100.0F);
            }
         }

         BloodClothingType.calcTotalDirtLevel(var1);
      }
   }

   public void saveChange(String var1, KahluaTable var2, ByteBuffer var3) {
      if ("washer.state".equals(var1)) {
         var3.put((byte)(this.isActivated() ? 1 : 0));
      }

   }

   public void loadChange(String var1, ByteBuffer var2) {
      if ("washer.state".equals(var1)) {
         this.setActivated(var2.get() == 1);
      }

   }

   public ItemContainer getContainer() {
      return this.getObject().getContainerByType("clothingwasher");
   }

   private void updateSound() {
      if (this.isActivated()) {
         if (!GameServer.bServer) {
            if (this.getObject().emitter != null && this.getObject().emitter.isPlaying("ClothingWasherFinished")) {
               this.getObject().emitter.stopOrTriggerSoundByName("ClothingWasherFinished");
            }

            if (this.soundInstance == -1L) {
               this.getObject().emitter = IsoWorld.instance.getFreeEmitter(this.getObject().getX() + 0.5F, this.getObject().getY() + 0.5F, (float)((int)this.getObject().getZ()));
               IsoWorld.instance.setEmitterOwner(this.getObject().emitter, this.getObject());
               this.soundInstance = this.getObject().emitter.playSoundLoopedImpl("ClothingWasherRunning");
            }
         }

         if (!GameClient.bClient) {
            WorldSoundManager.instance.addSoundRepeating(this, this.getObject().square.x, this.getObject().square.y, this.getObject().square.z, 10, 10, false);
         }
      } else if (this.soundInstance != -1L) {
         this.getObject().emitter.stopOrTriggerSound(this.soundInstance);
         this.soundInstance = -1L;
         if (this.cycleFinished) {
            this.cycleFinished = false;
            this.getObject().emitter.playSoundImpl("ClothingWasherFinished", this.getObject());
         }
      }

   }

   public boolean isItemAllowedInContainer(ItemContainer var1, InventoryItem var2) {
      if (var1 != this.getContainer()) {
         return false;
      } else {
         return !this.isActivated();
      }
   }

   public boolean isRemoveItemAllowedFromContainer(ItemContainer var1, InventoryItem var2) {
      if (var1 != this.getContainer()) {
         return false;
      } else {
         return var1.isEmpty() || !this.isActivated();
      }
   }

   private boolean cycleFinished() {
      if (this.isActivated()) {
         if (!this.alreadyExecuted) {
            this.startTime = (float)GameTime.getInstance().getWorldAgeHours();
            this.alreadyExecuted = true;
         }

         float var1 = (float)GameTime.getInstance().getWorldAgeHours() - this.startTime;
         int var2 = (int)(var1 * 60.0F);
         if ((float)var2 < this.cycleLengthMinutes) {
            return false;
         }

         this.cycleFinished = true;
         this.setActivated(false);
      }

      return true;
   }

   public boolean isActivated() {
      return this.bActivated;
   }

   public void setActivated(boolean var1) {
      boolean var2 = var1 != this.bActivated;
      this.bActivated = var1;
      this.alreadyExecuted = false;
      if (var2) {
         Thread var3 = Thread.currentThread();
         if (var3 == GameWindow.GameThread || var3 == GameServer.MainThread) {
            IsoGenerator.updateGenerator(this.getObject().getSquare());
         }
      }

   }

   public void switchModeOn() {
   }

   public void switchModeOff() {
      this.setActivated(false);
      this.updateSound();
      this.cycleFinished = false;
   }
}
