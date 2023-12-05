package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.WorldSoundManager;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.Clothing;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.objects.interfaces.IClothingWasherDryerLogic;
import zombie.network.GameClient;
import zombie.network.GameServer;

public final class ClothingDryerLogic implements IClothingWasherDryerLogic {
   private final IsoObject m_object;
   private boolean bActivated;
   private long soundInstance = -1L;
   private float lastUpdate = -1.0F;
   private boolean cycleFinished = false;
   private float startTime = 0.0F;
   private float cycleLengthMinutes = 90.0F;
   private boolean alreadyExecuted = false;

   public ClothingDryerLogic(IsoObject var1) {
      this.m_object = var1;
   }

   public IsoObject getObject() {
      return this.m_object;
   }

   public void load(ByteBuffer var1, int var2, boolean var3) throws IOException {
      this.bActivated = var1.get() == 1;
   }

   public void save(ByteBuffer var1, boolean var2) throws IOException {
      var1.put((byte)(this.isActivated() ? 1 : 0));
   }

   public void update() {
      if (this.getObject().getObjectIndex() != -1) {
         if (this.getContainer() != null) {
            if (!this.getContainer().isPowered()) {
               this.setActivated(false);
            }

            this.cycleFinished();
            this.updateSound();
            if (GameClient.bClient) {
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

                  for(int var4 = 0; var4 < this.getContainer().getItems().size(); ++var4) {
                     InventoryItem var5 = (InventoryItem)this.getContainer().getItems().get(var4);
                     if (var5 instanceof Clothing) {
                        Clothing var6 = (Clothing)var5;
                        float var7 = var6.getWetness();
                        if (var7 > 0.0F) {
                           var7 -= (float)var3;
                           var6.setWetness(var7);
                           if (GameServer.bServer) {
                           }
                        }
                     }

                     if (var5.isWet() && var5.getItemWhenDry() != null) {
                        var5.setWetCooldown(var5.getWetCooldown() - (float)(var3 * 250));
                        if (var5.getWetCooldown() <= 0.0F) {
                           InventoryItem var8 = InventoryItemFactory.CreateItem(var5.getItemWhenDry());
                           this.getContainer().addItem(var8);
                           this.getContainer().Remove(var5);
                           --var4;
                           var5.setWet(false);
                           IsoWorld.instance.CurrentCell.addToProcessItemsRemove(var5);
                        }
                     }
                  }

               }
            }
         }
      }
   }

   public void saveChange(String var1, KahluaTable var2, ByteBuffer var3) {
      if ("dryer.state".equals(var1)) {
         var3.put((byte)(this.isActivated() ? 1 : 0));
      }

   }

   public void loadChange(String var1, ByteBuffer var2) {
      if ("dryer.state".equals(var1)) {
         this.setActivated(var2.get() == 1);
      }

   }

   public ItemContainer getContainer() {
      return this.getObject().getContainerByType("clothingdryer");
   }

   private void updateSound() {
      if (this.isActivated()) {
         if (!GameServer.bServer) {
            if (this.getObject().emitter != null && this.getObject().emitter.isPlaying("ClothingDryerFinished")) {
               this.getObject().emitter.stopOrTriggerSoundByName("ClothingDryerFinished");
            }

            if (this.soundInstance == -1L) {
               this.getObject().emitter = IsoWorld.instance.getFreeEmitter(this.getObject().getX() + 0.5F, this.getObject().getY() + 0.5F, (float)((int)this.getObject().getZ()));
               IsoWorld.instance.setEmitterOwner(this.getObject().emitter, this.getObject());
               this.soundInstance = this.getObject().emitter.playSoundLoopedImpl("ClothingDryerRunning");
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
            this.getObject().emitter.playSoundImpl("ClothingDryerFinished", this.getObject());
         }
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

   public boolean isItemAllowedInContainer(ItemContainer var1, InventoryItem var2) {
      if (this.isActivated()) {
         return false;
      } else {
         return this.getContainer() == var1;
      }
   }

   public boolean isRemoveItemAllowedFromContainer(ItemContainer var1, InventoryItem var2) {
      if (!this.getContainer().isEmpty() && this.isActivated()) {
         return false;
      } else {
         return this.getContainer() == var1;
      }
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
