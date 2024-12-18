package zombie.inventory.types;

import java.util.List;
import zombie.GameTime;
import zombie.Lua.LuaManager;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.entity.energy.Energy;
import zombie.interfaces.IUpdater;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.inventory.ItemUser;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoBarbecue;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Item;
import zombie.util.StringUtils;

public final class DrainableComboItem extends InventoryItem implements Drainable, IUpdater {
   protected boolean bUseWhileEquiped = true;
   protected boolean bUseWhileUnequiped = false;
   protected int ticksPerEquipUse = 30;
   protected float useDelta = 0.03125F;
   protected float delta = 1.0F;
   protected float ticks = 0.0F;
   protected String ReplaceOnDeplete = null;
   protected String ReplaceOnDepleteFullType = null;
   public List<String> ReplaceOnCooked = null;
   private String OnCooked = null;
   private boolean canConsolidate = true;
   private float WeightEmpty = 0.0F;
   private static final float MIN_HEAT = 0.2F;
   private static final float MAX_HEAT = 3.0F;
   private String onEat = null;
   protected float Heat = 1.0F;
   protected int LastCookMinute = 0;

   public DrainableComboItem(String var1, String var2, String var3, String var4) {
      super(var1, var2, var3, var4);
   }

   public DrainableComboItem(String var1, String var2, String var3, Item var4) {
      super(var1, var2, var3, var4);
   }

   public boolean IsDrainable() {
      return true;
   }

   public int getSaveType() {
      return Item.Type.Drainable.ordinal();
   }

   public boolean CanStack(InventoryItem var1) {
      return false;
   }

   public int getMaxUses() {
      return (int)Math.floor((double)(1.0F / this.useDelta));
   }

   public void setCurrentUses(int var1) {
      this.uses = var1;
      this.updateWeight();
   }

   /** @deprecated */
   @Deprecated
   public void setUsedDelta(float var1) {
      this.setCurrentUsesFloat(var1);
   }

   public void setCurrentUsesFloat(float var1) {
      this.uses = (int)(var1 / this.useDelta);
   }

   public float getCurrentUsesFloat() {
      return (float)this.uses * this.useDelta;
   }

   public void render() {
   }

   public void renderlast() {
   }

   public boolean shouldUpdateInWorld() {
      return !GameServer.bServer && this.Heat != 1.0F;
   }

   public void update() {
      ItemContainer var1 = this.getOutermostContainer();
      float var2;
      int var3;
      if (var1 != null) {
         var2 = var1.getTemprature();
         if (this.Heat > var2) {
            this.Heat -= 0.001F * GameTime.instance.getMultiplier();
            if (this.Heat < Math.max(0.2F, var2)) {
               this.Heat = Math.max(0.2F, var2);
            }
         }

         if (this.Heat < var2) {
            this.Heat += var2 / 1000.0F * GameTime.instance.getMultiplier();
            if (this.Heat > Math.min(3.0F, var2)) {
               this.Heat = Math.min(3.0F, var2);
            }
         }

         if (this.IsCookable && this.Heat > 1.6F) {
            var3 = GameTime.getInstance().getMinutes();
            if (var3 != this.LastCookMinute) {
               this.LastCookMinute = var3;
               float var4 = this.Heat / 1.5F;
               if (var1.getTemprature() <= 1.6F) {
                  var4 *= 0.05F;
               }

               float var5 = this.CookingTime;
               if (var5 < 1.0F) {
                  var5 = 10.0F;
               }

               var5 += var4;
               if (!this.isCooked() && var5 > this.MinutesToCook) {
                  this.setCooked(true);
                  if (this.getReplaceOnCooked() != null) {
                     for(int var6 = 0; var6 < this.getReplaceOnCooked().size(); ++var6) {
                        InventoryItem var7 = this.container.AddItem((String)this.getReplaceOnCooked().get(var6));
                        if (var7 != null) {
                           if (var7 instanceof DrainableComboItem) {
                              var7.setCurrentUses(this.getCurrentUses());
                           }

                           var7.copyConditionModData(this);
                        }
                     }

                     this.container.Remove((InventoryItem)this);
                     IsoWorld.instance.CurrentCell.addToProcessItemsRemove((InventoryItem)this);
                     return;
                  }

                  if (this.getOnCooked() != null) {
                     LuaManager.caller.protectedCall(LuaManager.thread, LuaManager.env.rawget(this.getOnCooked()), new Object[]{this});
                     return;
                  }
               }

               if (this.CookingTime > this.MinutesToBurn) {
                  this.Burnt = true;
                  this.setCooked(false);
               }
            }
         }
      }

      if (this.container == null && this.Heat != 1.0F) {
         var2 = 1.0F;
         if (this.Heat > var2) {
            this.Heat -= 0.001F * GameTime.instance.getMultiplier();
            if (this.Heat < var2) {
               this.Heat = var2;
            }
         }

         if (this.Heat < var2) {
            this.Heat += var2 / 1000.0F * GameTime.instance.getMultiplier();
            if (this.Heat > var2) {
               this.Heat = var2;
            }
         }
      }

      if (this.bUseWhileEquiped && this.uses > 0) {
         IsoPlayer var8 = null;
         if (this.container != null && this.container.parent instanceof IsoPlayer) {
            for(var3 = 0; var3 < IsoPlayer.numPlayers; ++var3) {
               if (this.container.parent == IsoPlayer.players[var3]) {
                  var8 = IsoPlayer.players[var3];
               }
            }
         }

         if (var8 != null && (this.canBeActivated() && this.isActivated() || !this.canBeActivated()) && (var8.isHandItem(this) || var8.isAttachedItem(this))) {
            this.ticks += GameTime.instance.getMultiplier();

            while(this.ticks >= (float)this.ticksPerEquipUse) {
               this.ticks -= (float)this.ticksPerEquipUse;
               if (this.uses > 0) {
                  this.Use();
               }
            }
         }
      }

      if (this.bUseWhileUnequiped && this.uses > 0 && (this.canBeActivated() && this.isActivated() || !this.canBeActivated())) {
         this.ticks += GameTime.instance.getMultiplier();

         while(this.ticks >= (float)this.ticksPerEquipUse) {
            this.ticks -= (float)this.ticksPerEquipUse;
            if (this.uses > 0) {
               this.Use();
            }
         }
      }

      if (this.getCurrentUses() <= 0 && this.getReplaceOnDeplete() == null && !this.isKeepOnDeplete() && this.container != null) {
         if (this.container.parent instanceof IsoGameCharacter) {
            IsoGameCharacter var9 = (IsoGameCharacter)this.container.parent;
            var9.removeFromHands(this);
         }

         this.container.Items.remove(this);
         this.container.setDirty(true);
         this.container.setDrawDirty(true);
         if (GameServer.bServer) {
            GameServer.sendRemoveItemFromContainer(this.container, this);
         }

         this.container = null;
      }

   }

   public void Use() {
      this.Use(false, false, false);
   }

   public void Use(boolean var1, boolean var2, boolean var3) {
      if (this.getWorldItem() != null) {
         ItemUser.UseItem(this);
         if (GameServer.bServer && var3) {
            this.syncItemFields();
         }

      } else {
         --this.uses;
         if (this.uses <= 0) {
            this.delta = 0.0F;
            if (this.getReplaceOnDeplete() != null) {
               String var4 = this.getReplaceOnDepleteFullType();
               if (this.container != null) {
                  InventoryItem var5 = this.container.AddItem(var4);
                  if (this.container.parent instanceof IsoGameCharacter) {
                     IsoGameCharacter var6 = (IsoGameCharacter)this.container.parent;
                     if (var6.getPrimaryHandItem() == this) {
                        var6.setPrimaryHandItem(var5);
                     }

                     if (var6.getSecondaryHandItem() == this) {
                        var6.setSecondaryHandItem(var5);
                     }
                  }

                  var5.setCondition(this.getCondition());
                  var5.setFavorite(this.isFavorite());
                  ItemContainer var8 = this.container;
                  this.container.Remove((InventoryItem)this);
                  if (GameServer.bServer) {
                     GameServer.sendReplaceItemInContainer(var8, this, var5);
                  }
               }
            } else {
               if (this.isKeepOnDeplete()) {
                  if (var3) {
                     this.syncItemFields();
                  }

                  return;
               }

               if (this.container != null && this.isDisappearOnUse()) {
                  if (this.container.parent instanceof IsoGameCharacter) {
                     IsoGameCharacter var7 = (IsoGameCharacter)this.container.parent;
                     var7.removeFromHands(this);
                  }

                  this.container.Items.remove(this);
                  this.container.setDirty(true);
                  this.container.setDrawDirty(true);
                  if (GameServer.bServer && var3) {
                     GameServer.sendRemoveItemFromContainer(this.container, this);
                  }

                  this.container = null;
               }
            }
         }

         this.updateWeight();
         if (var3) {
            this.syncItemFields();
         }

      }
   }

   public void syncItemFields() {
      ItemContainer var1 = this.getOutermostContainer();
      if (var1 != null && var1.getParent() instanceof IsoPlayer) {
         if (GameClient.bClient) {
            INetworkPacket.send(PacketTypes.PacketType.ItemStats, this.getContainer(), this);
         } else if (GameServer.bServer) {
            INetworkPacket.send((IsoPlayer)var1.getParent(), PacketTypes.PacketType.ItemStats, this.getContainer(), this);
         }
      }

   }

   public void updateWeight() {
      if (this.getReplaceOnDeplete() != null) {
         if (this.getCurrentUsesFloat() >= 1.0F) {
            this.setCustomWeight(true);
            this.setActualWeight(this.getScriptItem().getActualWeight());
            this.setWeight(this.getActualWeight());
            return;
         }

         Item var1 = ScriptManager.instance.getItem(this.ReplaceOnDepleteFullType);
         if (var1 != null) {
            this.setCustomWeight(true);
            this.setActualWeight((this.getScriptItem().getActualWeight() - var1.getActualWeight()) * this.getCurrentUsesFloat() + var1.getActualWeight());
            this.setWeight(this.getActualWeight());
         }
      }

      if (this.getWeightEmpty() != 0.0F) {
         this.setCustomWeight(true);
         this.setActualWeight((this.getScriptItem().getActualWeight() - this.WeightEmpty) * this.getCurrentUsesFloat() + this.WeightEmpty);
      }

   }

   public float getWeightEmpty() {
      return this.WeightEmpty;
   }

   public void setWeightEmpty(float var1) {
      this.WeightEmpty = var1;
   }

   public boolean isUseWhileEquiped() {
      return this.bUseWhileEquiped;
   }

   public void setUseWhileEquiped(boolean var1) {
      this.bUseWhileEquiped = var1;
   }

   public boolean isUseWhileUnequiped() {
      return this.bUseWhileUnequiped;
   }

   public void setUseWhileUnequiped(boolean var1) {
      this.bUseWhileUnequiped = var1;
   }

   public int getTicksPerEquipUse() {
      return this.ticksPerEquipUse;
   }

   public void setTicksPerEquipUse(int var1) {
      this.ticksPerEquipUse = var1;
   }

   public float getUseDelta() {
      return this.useDelta;
   }

   public void setUseDelta(float var1) {
      this.useDelta = var1;
   }

   public float getTicks() {
      return this.ticks;
   }

   public void setTicks(float var1) {
      this.ticks = var1;
   }

   public void setReplaceOnDeplete(String var1) {
      this.ReplaceOnDeplete = var1;
      this.ReplaceOnDepleteFullType = this.getReplaceOnDepleteFullType();
   }

   public String getReplaceOnDeplete() {
      return this.ReplaceOnDeplete;
   }

   public String getReplaceOnDepleteFullType() {
      return StringUtils.moduleDotType(this.getModule(), this.ReplaceOnDeplete);
   }

   public void setHeat(float var1) {
      this.Heat = PZMath.clamp(var1, 0.0F, 3.0F);
   }

   public float getHeat() {
      return this.Heat;
   }

   public float getInvHeat() {
      return (1.0F - this.Heat) / 3.0F;
   }

   public boolean finishupdate() {
      if (this.container != null) {
         if (this.Heat != this.container.getTemprature() || this.container.isTemperatureChanging()) {
            return false;
         }

         if (this.container.type.equals("campfire") || this.container.parent instanceof IsoBarbecue) {
            return false;
         }
      }

      return true;
   }

   public boolean canConsolidate() {
      return this.canConsolidate;
   }

   public void setCanConsolidate(boolean var1) {
      this.canConsolidate = var1;
   }

   public List<String> getReplaceOnCooked() {
      return this.ReplaceOnCooked;
   }

   public void setReplaceOnCooked(List<String> var1) {
      this.ReplaceOnCooked = var1;
   }

   public String getOnCooked() {
      return this.OnCooked;
   }

   public void setOnCooked(String var1) {
      this.OnCooked = var1;
   }

   public String getOnEat() {
      return this.onEat;
   }

   public void setOnEat(String var1) {
      this.onEat = var1;
   }

   public boolean isEnergy() {
      return this.getEnergy() != null;
   }

   public Energy getEnergy() {
      return null;
   }

   public boolean isFullUses() {
      return this.uses >= this.getMaxUses();
   }

   public boolean isEmptyUses() {
      return this.getCurrentUsesFloat() <= 0.0F;
   }

   public void randomizeUses() {
      if (this.getMaxUses() != 1) {
         int var1 = Rand.Next(this.getMaxUses()) + 1;
         if (this.hasTag("LessFull")) {
            var1 = Math.min(var1, Rand.Next(this.getMaxUses()) + 1);
         }

         if (var1 <= this.getMaxUses()) {
            if (var1 > 0) {
               this.setCurrentUses(var1);
            }
         }
      }
   }
}
