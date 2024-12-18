package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Objects;
import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.WorldSoundManager;
import zombie.Lua.LuaEventManager;
import zombie.ai.states.ThumpState;
import zombie.audio.parameters.ParameterMeleeHitSurface;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.math.PZMath;
import zombie.core.raknet.UdpConnection;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.Food;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.objects.interfaces.Thumpable;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.weather.ClimateManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.util.Type;

public class IsoCompost extends IsoObject implements Thumpable {
   private float compost;
   private float LastUpdated;
   public int Health;
   public int MaxHealth;
   public float partialThumpDmg;
   private int thumpDmg;

   public IsoCompost(IsoCell var1) {
      super(var1);
      this.compost = 0.0F;
      this.LastUpdated = -1.0F;
      this.Health = 100;
      this.MaxHealth = 100;
      this.partialThumpDmg = 0.0F;
      this.thumpDmg = 8;
   }

   public IsoCompost(IsoCell var1, IsoGridSquare var2, String var3) {
      this(var1, var2, IsoSpriteManager.instance.getSprite(var3));
   }

   public IsoCompost(IsoCell var1, IsoGridSquare var2, IsoSprite var3) {
      this.compost = 0.0F;
      this.LastUpdated = -1.0F;
      this.Health = 100;
      this.MaxHealth = 100;
      this.partialThumpDmg = 0.0F;
      this.thumpDmg = 8;
      this.sprite = var3;
      this.square = var2;
      this.container = new ItemContainer();
      this.container.setType("composter");
      this.container.setParent(this);
      this.container.bExplored = true;
      int var4 = PZMath.tryParseInt(this.sprite.getProperties().Val("ContainerCapacity"), 30);
      this.container.setCapacity(var4);
   }

   public void update() {
      if (!GameClient.bClient && this.container != null) {
         float var1 = (float)GameTime.getInstance().getWorldAgeHours();
         if (this.LastUpdated < 0.0F) {
            this.LastUpdated = var1;
         } else if (this.LastUpdated > var1) {
            this.LastUpdated = var1;
         }

         float var2 = var1 - this.LastUpdated;
         if (!(var2 <= 0.0F)) {
            this.LastUpdated = var1;
            int var3 = SandboxOptions.instance.getCompostHours();
            int var4 = 0;

            int var5;
            InventoryItem var6;
            for(var5 = 0; var5 < this.container.getItems().size(); ++var5) {
               var6 = (InventoryItem)this.container.getItems().get(var5);
               if (var6 instanceof Food) {
                  Food var7 = (Food)var6;
                  if (Objects.equals(var7.getFullType(), "Base.Worm") && var7.isFresh()) {
                     var4 += var4;
                  }
               }
            }

            for(var5 = 0; var5 < this.container.getItems().size(); ++var5) {
               var6 = (InventoryItem)this.container.getItems().get(var5);
               boolean var11 = var6.hasTag("Compostable");
               if (var6 instanceof Food && !var6.hasTag("CantCompost ")) {
                  Food var8 = (Food)var6;
                  if (GameServer.bServer && (!Objects.equals(var8.getFullType(), "Base.Worm") || !var8.isFresh())) {
                     var8.updateAge();
                  }

                  if (var8.isRotten() || var11) {
                     if (this.getCompost() < 100.0F) {
                        var8.setRottenTime(0.0F);
                        var8.setCompostTime(var8.getCompostTime() + var2);
                     }

                     if (var8.getCompostTime() >= (float)var3) {
                        float var9 = Math.abs(var8.getHungChange()) * 2.0F;
                        if (var9 == 0.0F) {
                           var9 = Math.abs(var8.getWeight()) * 10.0F;
                        }

                        this.setCompost(this.getCompost() + var9);
                        if (this.getCompost() > 100.0F) {
                           this.setCompost(100.0F);
                        }

                        if (GameServer.bServer) {
                           GameServer.sendCompost(this, (UdpConnection)null);
                           GameServer.sendRemoveItemFromContainer(this.container, var6);
                        }

                        if (var4 >= 2 && !"Winter".equals(ClimateManager.getInstance().getSeasonName()) && Rand.Next(10) == 0) {
                           InventoryItem var10 = InventoryItemFactory.CreateItem("Base.Worm");
                           this.container.AddItem(var10);
                           if (GameServer.bServer && var10 != null) {
                              GameServer.sendAddItemToContainer(this.container, var10);
                           }
                        }

                        var6.Use();
                        IsoWorld.instance.CurrentCell.addToProcessItemsRemove(var6);
                     }
                  }
               }
            }

            this.updateSprite();
         }
      }
   }

   public void updateSprite() {
      if (this.getCompost() >= 10.0F && this.sprite.getName().equals("camping_01_19")) {
         this.sprite = IsoSpriteManager.instance.getSprite("camping_01_20");
         this.transmitUpdatedSpriteToClients();
      } else if (this.getCompost() < 10.0F && this.sprite.getName().equals("camping_01_20")) {
         this.sprite = IsoSpriteManager.instance.getSprite("camping_01_19");
         this.transmitUpdatedSpriteToClients();
      } else if (this.getCompost() >= 10.0F && this.sprite.getName().equals("carpentry_02_116")) {
         this.sprite = IsoSpriteManager.instance.getSprite("carpentry_02_117");
         this.transmitUpdatedSpriteToClients();
      } else if (this.getCompost() < 10.0F && this.sprite.getName().equals("carpentry_02_117")) {
         this.sprite = IsoSpriteManager.instance.getSprite("carpentry_02_116");
         this.transmitUpdatedSpriteToClients();
      }

   }

   public void syncCompost() {
      if (GameClient.bClient) {
         GameClient.sendCompost(this);
      } else if (GameServer.bServer) {
         GameServer.sendCompost(this, (UdpConnection)null);
      }

   }

   public void sync() {
      this.syncCompost();
   }

   public void load(ByteBuffer var1, int var2, boolean var3) throws IOException {
      super.load(var1, var2, var3);
      if (this.container != null) {
         this.container.setType("composter");
      }

      this.compost = var1.getFloat();
      this.LastUpdated = var1.getFloat();
      if (var2 >= 213) {
         this.Health = var1.getInt();
         this.MaxHealth = var1.getInt();
      }

   }

   public void save(ByteBuffer var1, boolean var2) throws IOException {
      super.save(var1, var2);
      var1.putFloat(this.compost);
      var1.putFloat(this.LastUpdated);
      var1.putInt(this.Health);
      var1.putInt(this.MaxHealth);
   }

   public String getObjectName() {
      return "IsoCompost";
   }

   public float getCompost() {
      return this.compost;
   }

   public void setCompost(float var1) {
      this.compost = PZMath.clamp(var1, 0.0F, 100.0F);
   }

   public void remove() {
      if (this.getSquare() != null) {
         this.getSquare().transmitRemoveItemFromSquare(this);
      }
   }

   public void addToWorld() {
      this.getCell().addToProcessIsoObject(this);
   }

   public Thumpable getThumpableFor(IsoGameCharacter var1) {
      return this.isDestroyed() ? null : this;
   }

   public void setHealth(int var1) {
      this.Health = var1;
   }

   public int getHealth() {
      return this.Health;
   }

   public void setMaxHealth(int var1) {
      this.MaxHealth = var1;
   }

   public int getMaxHealth() {
      return this.MaxHealth;
   }

   private void dropContainedItems() {
      ArrayList var1 = new ArrayList();

      for(int var2 = 0; var2 < this.getContainerCount(); ++var2) {
         ItemContainer var3 = this.getContainerByIndex(var2);
         var1.clear();
         var1.addAll(var3.getItems());
         var3.removeItemsFromProcessItems();
         var3.removeAllItems();

         for(int var4 = 0; var4 < var1.size(); ++var4) {
            this.getSquare().AddWorldInventoryItem((InventoryItem)var1.get(var4), 0.0F, 0.0F, 0.0F);
         }
      }

   }

   public void Thump(IsoMovingObject var1) {
      if (SandboxOptions.instance.Lore.ThumpOnConstruction.getValue()) {
         if (var1 instanceof IsoGameCharacter) {
            Thumpable var2 = this.getThumpableFor((IsoGameCharacter)var1);
            if (var2 == null) {
               return;
            }
         }

         if (var1 instanceof IsoZombie) {
            int var5 = var1.getCurrentSquare().getMovingObjects().size();
            if (var1.getCurrentSquare().getW() != null) {
               var5 += var1.getCurrentSquare().getW().getMovingObjects().size();
            }

            if (var1.getCurrentSquare().getE() != null) {
               var5 += var1.getCurrentSquare().getE().getMovingObjects().size();
            }

            if (var1.getCurrentSquare().getS() != null) {
               var5 += var1.getCurrentSquare().getS().getMovingObjects().size();
            }

            if (var1.getCurrentSquare().getN() != null) {
               var5 += var1.getCurrentSquare().getN().getMovingObjects().size();
            }

            int var3 = this.thumpDmg;
            int var4;
            if (var5 >= var3) {
               var4 = 1 * ThumpState.getFastForwardDamageMultiplier();
               this.Health -= var4;
            } else {
               this.partialThumpDmg += (float)var5 / (float)var3 * (float)ThumpState.getFastForwardDamageMultiplier();
               if ((int)this.partialThumpDmg > 0) {
                  var4 = (int)this.partialThumpDmg;
                  this.Health -= var4;
                  this.partialThumpDmg -= (float)var4;
               }
            }

            WorldSoundManager.instance.addSound(var1, this.square.getX(), this.square.getY(), this.square.getZ(), 20, 20, true, 4.0F, 15.0F);
         }

         if (this.isDestroyed()) {
            String var6 = "BreakObject";
            ((IsoGameCharacter)var1).getEmitter().playSound(var6, this);
            if (GameServer.bServer) {
               GameServer.PlayWorldSoundServer((IsoGameCharacter)var1, var6, false, var1.getCurrentSquare(), 0.2F, 20.0F, 1.1F, true);
            }

            WorldSoundManager.instance.addSound((Object)null, this.square.getX(), this.square.getY(), this.square.getZ(), 10, 20, true, 4.0F, 15.0F);
            var1.setThumpTarget((Thumpable)null);
            if (this.getObjectIndex() != -1) {
               this.addItemsFromProperties();
               this.dropContainedItems();
               this.square.transmitRemoveItemFromSquare(this);
            }
         }

      }
   }

   public void WeaponHit(IsoGameCharacter var1, HandWeapon var2) {
      if (!this.isDestroyed()) {
         IsoPlayer var3 = (IsoPlayer)Type.tryCastTo(var1, IsoPlayer.class);
         if (GameClient.bClient) {
            if (var3 != null) {
               GameClient.instance.sendWeaponHit(var3, var2, this);
            }

         } else {
            LuaEventManager.triggerEvent("OnWeaponHitThumpable", var1, var2, this);
            if (var3 != null) {
               var3.setMeleeHitSurface(ParameterMeleeHitSurface.Material.Wood);
            }

            var1.getEmitter().playSound(var2.getDoorHitSound(), this);
            if (GameServer.bServer) {
               GameServer.PlayWorldSoundServer(var1, var2.getDoorHitSound(), false, this.getSquare(), 1.0F, 20.0F, 2.0F, false);
            }

            if (var2 != null) {
               this.Damage((float)var2.getDoorDamage());
            } else {
               this.Damage(50.0F);
            }

            WorldSoundManager.instance.addSound(var1, this.square.getX(), this.square.getY(), this.square.getZ(), 20, 20, false, 0.0F, 15.0F);
            if (this.isDestroyed()) {
               if (var1 != null) {
                  String var4 = "BreakObject";
                  var1.getEmitter().playSound(var4);
                  if (GameServer.bServer) {
                     GameServer.PlayWorldSoundServer(var4, false, var1.getCurrentSquare(), 0.2F, 20.0F, 1.1F, true);
                  }
               }

               this.addItemsFromProperties();
               this.dropContainedItems();
               this.square.transmitRemoveItemFromSquare(this);
               if (!GameServer.bServer) {
                  this.square.RemoveTileObject(this);
               }
            }

         }
      }
   }

   public void Damage(float var1) {
      this.DirtySlice();
      this.Health = (int)((float)this.Health - var1);
   }

   public boolean isDestroyed() {
      return this.Health <= 0;
   }

   public float getThumpCondition() {
      return this.getMaxHealth() <= 0 ? 0.0F : (float)PZMath.clamp(this.getHealth(), 0, this.getMaxHealth()) / (float)this.getMaxHealth();
   }
}
