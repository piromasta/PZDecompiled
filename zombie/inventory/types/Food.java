package zombie.inventory.types;

import fmod.fmod.FMODManager;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.SandboxOptions;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.audio.BaseSoundEmitter;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.SurvivorDesc;
import zombie.characters.animals.AnimalDefinitions;
import zombie.characters.animals.AnimalGene;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.skills.PerkFactory;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.random.Rand;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.ItemSoundManager;
import zombie.inventory.ItemType;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoCompost;
import zombie.iso.objects.IsoFireManager;
import zombie.iso.objects.IsoFireplace;
import zombie.iso.objects.IsoHutch;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ModelScript;
import zombie.ui.ObjectTooltip;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.util.io.BitHeader;
import zombie.util.io.BitHeaderRead;
import zombie.util.io.BitHeaderWrite;
import zombie.vehicles.BaseVehicle;

public final class Food extends InventoryItem {
   protected boolean bBadCold = false;
   protected boolean bGoodHot = false;
   private static final float MIN_HEAT = 0.2F;
   private static final float MAX_HEAT = 3.0F;
   protected float Heat = 1.0F;
   protected float endChange = 0.0F;
   protected float hungChange = 0.0F;
   protected String useOnConsume = null;
   protected boolean rotten = false;
   protected boolean bDangerousUncooked = false;
   protected int LastCookMinute = 0;
   public float thirstChange = 0.0F;
   public boolean Poison = false;
   private List<String> ReplaceOnCooked = null;
   private float baseHunger = 0.0F;
   public ArrayList<String> spices = null;
   private boolean isSpice = false;
   private boolean isTainted = false;
   private int poisonDetectionLevel = -1;
   private Integer PoisonLevelForRecipe = 0;
   private int UseForPoison = 0;
   private int PoisonPower = 0;
   private String FoodType = null;
   private String CustomEatSound = null;
   private boolean RemoveNegativeEffectOnCooked = false;
   private String Chef = null;
   private String OnCooked = null;
   private String WorldTextureCooked;
   private String WorldTextureRotten;
   private String WorldTextureOverdone;
   private int fluReduction = 0;
   private int ReduceFoodSickness = 0;
   private float painReduction = 0.0F;
   private String HerbalistType;
   private float carbohydrates = 0.0F;
   private float lipids = 0.0F;
   private float proteins = 0.0F;
   private float calories = 0.0F;
   private boolean packaged = false;
   private float freezingTime = 0.0F;
   private boolean frozen = false;
   private boolean canBeFrozen = true;
   protected float LastFrozenUpdate = -1.0F;
   public static final float FreezerAgeMultiplier = 0.0F;
   private String replaceOnRotten = null;
   private boolean forceFoodTypeAsName = false;
   private float rottenTime = 0.0F;
   private float compostTime = 0.0F;
   private String onEat = null;
   private boolean badInMicrowave = false;
   private boolean cookedInMicrowave = false;
   private long m_cookingSound = 0L;
   private int m_cookingParameter = -1;
   private int milkQty = 0;
   private String milkType = null;
   private boolean fertilized = false;
   private int fertilizedTime = 0;
   private int timeToHatch = 0;
   private String animalHatch = null;
   private String animalHatchBreed = null;
   private long lastEggTimeCheck = 0L;
   public int motherID = 0;
   public HashMap<String, AnimalGene> eggGenome = null;
   private long lastUpdateTime;
   private static final int COOKING_STATE_COOKING = 0;
   private static final int COOKING_STATE_BURNING = 1;

   public String getCategory() {
      return this.mainCategory != null ? this.mainCategory : "Food";
   }

   public Food(String var1, String var2, String var3, String var4) {
      super(var1, var2, var3, var4);
      Texture.WarnFailFindTexture = false;
      this.texturerotten = Texture.trygetTexture(var4 + "Rotten");
      String var5 = "Rotten.png";
      if (this.texturerotten == null) {
         this.texturerotten = Texture.trygetTexture(var4 + "Spoiled");
         if (this.texturerotten != null) {
            var5 = "Spoiled.png";
         }
      }

      if (this.texturerotten == null) {
         this.texturerotten = Texture.trygetTexture(var4 + "_Rotten");
         if (this.texturerotten != null) {
            var5 = "_Rotten.png";
         }
      }

      this.textureCooked = Texture.trygetTexture(var4 + "Cooked");
      String var6 = "Cooked.png";
      if (this.textureCooked == null) {
         this.textureCooked = Texture.trygetTexture(var4 + "_Cooked");
         if (this.textureCooked != null) {
            var6 = "_Cooked.png";
         }
      }

      this.textureBurnt = Texture.trygetTexture(var4 + "Overdone");
      String var7 = "Overdone.png";
      if (this.textureBurnt == null) {
         this.textureBurnt = Texture.trygetTexture(var4 + "Burnt");
         if (this.textureBurnt != null) {
            var7 = "Burnt.png";
         }
      }

      if (this.textureBurnt == null) {
         this.textureBurnt = Texture.trygetTexture(var4 + "_Burnt");
         if (this.textureBurnt != null) {
            var7 = "_Burnt.png";
         }
      }

      Texture.WarnFailFindTexture = true;
      if (this.texturerotten == null) {
         this.texturerotten = this.texture;
      }

      if (this.textureCooked == null) {
         this.textureCooked = this.texture;
      }

      if (this.textureBurnt == null) {
         this.textureBurnt = this.texture;
      }

      this.WorldTextureCooked = this.WorldTexture.replace(".png", var6);
      this.WorldTextureOverdone = this.WorldTexture.replace(".png", var7);
      this.WorldTextureRotten = this.WorldTexture.replace(".png", var5);
      this.cat = ItemType.Food;
      this.lastUpdateTime = System.currentTimeMillis();
   }

   public Food(String var1, String var2, String var3, Item var4) {
      super(var1, var2, var3, var4);
      String var5 = var4.ItemName;
      Texture.WarnFailFindTexture = false;
      this.texture = var4.NormalTexture;
      if (var4.SpecialTextures.size() == 0) {
         boolean var6 = false;
      }

      if (var4.SpecialTextures.size() > 0) {
         this.texturerotten = (Texture)var4.SpecialTextures.get(0);
      }

      if (var4.SpecialTextures.size() > 1) {
         this.textureCooked = (Texture)var4.SpecialTextures.get(1);
      }

      if (var4.SpecialTextures.size() > 2) {
         this.textureBurnt = (Texture)var4.SpecialTextures.get(2);
      }

      Texture.WarnFailFindTexture = true;
      if (this.texturerotten == null) {
         this.texturerotten = this.texture;
      }

      if (this.textureCooked == null) {
         this.textureCooked = this.texture;
      }

      if (this.textureBurnt == null) {
         this.textureBurnt = this.texture;
      }

      if (var4.SpecialWorldTextureNames.size() > 0) {
         this.WorldTextureRotten = (String)var4.SpecialWorldTextureNames.get(0);
      }

      if (var4.SpecialWorldTextureNames.size() > 1) {
         this.WorldTextureCooked = (String)var4.SpecialWorldTextureNames.get(1);
      }

      if (var4.SpecialWorldTextureNames.size() > 2) {
         this.WorldTextureOverdone = (String)var4.SpecialWorldTextureNames.get(2);
      }

      this.cat = ItemType.Food;
      this.lastUpdateTime = System.currentTimeMillis();
   }

   public boolean IsFood() {
      return true;
   }

   public int getSaveType() {
      return Item.Type.Food.ordinal();
   }

   public boolean checkEggHatch(IsoHutch var1) {
      if (!this.isFertilized()) {
         return false;
      } else {
         int var2 = 0;
         int var3 = 0;
         int var4 = 0;
         if (!StringUtils.isNullOrEmpty(this.animalHatch)) {
            if (this.lastEggTimeCheck != (long)GameTime.getInstance().getHour()) {
               ++this.fertilizedTime;
            }

            if (this.fertilizedTime >= this.timeToHatch) {
               this.fertilizedTime = 0;
               boolean var5 = false;
               boolean var6 = false;
               if (var1 == null) {
                  if (this.getWorldItem() != null) {
                     var2 = (int)this.getWorldItem().getX();
                     var3 = (int)this.getWorldItem().getY();
                     var4 = (int)this.getWorldItem().getZ();
                  }

                  if (this.getContainer() != null) {
                     var5 = true;
                  }

                  if (this.getContainer() != null && this.getContainer().getParent() instanceof BaseVehicle && ((BaseVehicle)this.getContainer().getParent()).getAnimalTrailerSize() > 0.0F) {
                     var2 = (int)this.getContainer().getParent().getX();
                     var3 = (int)this.getContainer().getParent().getY();
                     var4 = (int)this.getContainer().getParent().getZ();
                     var6 = true;
                  }

                  if (this.getContainer() != null && this.getContainer().getParent() != null && this.getContainer().getParent() instanceof IsoPlayer) {
                     var2 = (int)this.getContainer().getParent().getX();
                     var3 = (int)this.getContainer().getParent().getY();
                     var4 = (int)this.getContainer().getParent().getZ();
                     var5 = true;
                  }

                  if (var2 == 0 && var3 == 0 && !var5) {
                     return false;
                  }
               }

               AnimalDefinitions var7 = AnimalDefinitions.getDef(this.animalHatch);
               IsoAnimal var8 = new IsoAnimal(IsoWorld.instance.getCell(), var2, var3, var4, this.animalHatch, var7.getBreedByName(this.animalHatchBreed));
               var8.fullGenome = this.eggGenome;
               var8.attachBackToMother = this.motherID;
               AnimalGene.checkGeneticDisorder(var8);
               if (var5) {
                  AnimalInventoryItem var9 = (AnimalInventoryItem)InventoryItemFactory.CreateItem("Base.Animal");
                  var9.setAnimal(var8);
                  var8.removeFromWorld();
                  var8.removeFromSquare();
                  this.getContainer().AddItem((InventoryItem)var9);
                  this.getContainer().Remove((InventoryItem)this);
               } else if (var1 != null) {
                  var1.addAnimalInside(var8);
               } else if (var6) {
                  ((BaseVehicle)this.getContainer().getParent()).addAnimalInTrailer(var8);
                  this.getContainer().Remove((InventoryItem)this);
               } else {
                  var8.addToWorld();
                  this.getWorldItem().removeFromWorld();
                  this.getWorldItem().removeFromSquare();
               }

               return true;
            }

            this.lastEggTimeCheck = (long)GameTime.getInstance().getHour();
         }

         return false;
      }
   }

   public void update() {
      if (this.hasTag("AlreadyCooked")) {
         this.setCooked(true);
      }

      this.updateTemperature();
      this.checkEggHatch((IsoHutch)null);
      ItemContainer var1 = this.getOutermostContainer();
      if (var1 != null) {
         int var2;
         float var3;
         if (this.IsCookable && !this.isFrozen()) {
            if (this.Heat > 1.6F) {
               this.setFertilized(false);
               var2 = GameTime.getInstance().getMinutes();
               if (var2 != this.LastCookMinute) {
                  if (GameServer.bServer) {
                     GameServer.sendItemStats(this);
                  }

                  this.LastCookMinute = var2;
                  var3 = this.Heat / 1.5F;
                  if (var1.getTemprature() <= 1.6F) {
                     var3 *= 0.05F;
                  }

                  this.CookingTime += var3;
                  if (this.shouldPlayCookingSound()) {
                     ItemSoundManager.addItem(this);
                  }

                  if (this.isTainted && this.CookingTime > Math.min(this.MinutesToCook, 10.0F)) {
                     this.isTainted = false;
                  }

                  if (!this.isCooked() && !this.Burnt && (this.CookingTime > this.MinutesToCook || this.CookingTime > this.MinutesToBurn)) {
                     int var4;
                     if (this.getReplaceOnCooked() != null && !this.isRotten()) {
                        for(var4 = 0; var4 < this.getReplaceOnCooked().size(); ++var4) {
                           InventoryItem var7 = this.container.AddItem((String)this.getReplaceOnCooked().get(var4));
                           if (var7 != null) {
                              var7.copyConditionModData(this);
                              if (var7 instanceof Food && this instanceof Food) {
                              }

                              if (var7 instanceof Food && ((Food)var7).isBadInMicrowave() && this.container.isMicrowave()) {
                                 var7.setUnhappyChange(5.0F);
                                 var7.setBoredomChange(5.0F);
                                 ((Food)var7).cookedInMicrowave = true;
                              }
                           }
                        }

                        this.container.Remove((InventoryItem)this);
                        IsoWorld.instance.CurrentCell.addToProcessItemsRemove((InventoryItem)this);
                        return;
                     }

                     this.setCooked(true);
                     if (this.getScriptItem().RemoveUnhappinessWhenCooked) {
                        this.setUnhappyChange(0.0F);
                     }

                     if (this.type.equals("RicePot") || this.type.equals("PastaPot") || this.type.equals("RicePan") || this.type.equals("PastaPan") || this.type.equals("WaterPotRice") || this.type.equals("WaterPotPasta") || this.type.equals("WaterSaucepanRice") || this.type.equals("WaterSaucepanPasta") || this.type.equals("RiceBowl") || this.type.equals("PastaBowl")) {
                        this.setAge(0.0F);
                        this.setOffAge(1);
                        this.setOffAgeMax(2);
                     }

                     if (this.isRemoveNegativeEffectOnCooked()) {
                        if (this.thirstChange > 0.0F) {
                           this.setThirstChange(0.0F);
                        }

                        if (this.unhappyChange > 0.0F) {
                           this.setUnhappyChange(0.0F);
                        }

                        if (this.boredomChange > 0.0F) {
                           this.setBoredomChange(0.0F);
                        }
                     }

                     if (this.getOnCooked() != null) {
                        LuaManager.caller.protectedCall(LuaManager.thread, LuaManager.env.rawget(this.getOnCooked()), new Object[]{this});
                     }

                     if (this.isBadInMicrowave() && this.container.isMicrowave()) {
                        this.setUnhappyChange(5.0F);
                        this.setBoredomChange(5.0F);
                        this.cookedInMicrowave = true;
                     }

                     if (this.Chef != null && !this.Chef.isEmpty()) {
                        for(var4 = 0; var4 < IsoPlayer.numPlayers; ++var4) {
                           IsoPlayer var5 = IsoPlayer.players[var4];
                           if (var5 != null && !var5.isDead() && this.Chef.equals(var5.getFullName())) {
                              if (GameServer.bServer) {
                                 GameServer.addXp(var5, PerkFactory.Perks.Cooking, 10.0F);
                              } else if (!GameClient.bClient) {
                                 var5.getXp().AddXP(PerkFactory.Perks.Cooking, 10.0F);
                              }
                              break;
                           }
                        }
                     }
                  }

                  if (this.CookingTime > this.MinutesToBurn) {
                     this.Burnt = true;
                     this.setCooked(false);
                  }

                  if (GameServer.bServer) {
                     GameServer.sendItemStats(this);
                  }

                  if (IsoWorld.instance.isHydroPowerOn() && this.Burnt && this.CookingTime >= 50.0F && this.CookingTime >= this.MinutesToCook * 2.0F + this.MinutesToBurn / 2.0F && Rand.Next(Rand.AdjustForFramerate(200)) == 0) {
                     boolean var6 = this.container != null && this.container.getParent() != null && this.container.getParent().getName() != null && this.container.getParent().getName().equals("Campfire");
                     if (!var6 && this.container != null && this.container.getParent() != null && this.container.getParent() instanceof IsoFireplace) {
                        var6 = true;
                     }

                     if (this.container != null && this.container.SourceGrid != null && !var6) {
                        IsoFireManager.StartFire(this.container.SourceGrid.getCell(), this.container.SourceGrid, true, 500000);
                        this.IsCookable = false;
                     }
                  }
               }
            }
         } else {
            if (GameServer.bServer) {
               this.updateAge(false);
            }

            if (this.isTainted && this.Heat > 1.6F && !this.isFrozen()) {
               var2 = GameTime.getInstance().getMinutes();
               if (var2 != this.LastCookMinute) {
                  this.LastCookMinute = var2;
                  var3 = 1.0F;
                  if (var1.getTemprature() <= 1.6F) {
                     var3 = (float)((double)var3 * 0.2);
                  }

                  this.CookingTime += var3;
                  if (this.CookingTime > 10.0F) {
                     this.isTainted = false;
                  }

                  if (GameServer.bServer) {
                     GameServer.sendItemStats(this);
                  }
               }
            }
         }
      }

      this.updateRotting(var1);
      this.lastUpdateTime = System.currentTimeMillis();
   }

   public void updateSound(BaseSoundEmitter var1) {
      if (this.shouldPlayCookingSound()) {
         if (var1.isPlaying(this.m_cookingSound)) {
            this.setCookingParameter(var1);
            return;
         }

         ItemContainer var2 = this.getOutermostContainer();
         IsoGridSquare var3 = var2.getParent().getSquare();
         var1.setPos((float)var3.getX() + 0.5F, (float)var3.getY() + 0.5F, (float)var3.getZ());
         this.m_cookingSound = var1.playSoundImpl(this.getCookingSound(), (IsoObject)null);
         this.setCookingParameter(var1);
      } else {
         var1.stopOrTriggerSound(this.m_cookingSound);
         this.m_cookingSound = 0L;
         this.m_cookingParameter = -1;
         ItemSoundManager.removeItem(this);
      }

   }

   private boolean shouldPlayCookingSound() {
      if (GameServer.bServer) {
         return false;
      } else if (StringUtils.isNullOrWhitespace(this.getCookingSound())) {
         return false;
      } else {
         ItemContainer var1 = this.getOutermostContainer();
         if (var1 != null && var1.getParent() != null && var1.getParent().getObjectIndex() != -1 && !(var1.getTemprature() <= 1.6F)) {
            return this.isCookable() && !this.isFrozen() && this.getHeat() > 1.6F;
         } else {
            return false;
         }
      }
   }

   private void setCookingParameter(BaseSoundEmitter var1) {
      boolean var2 = this.CookingTime > this.MinutesToCook;
      int var3 = var2 ? 1 : 0;
      if (var3 != this.m_cookingParameter) {
         this.m_cookingParameter = var3;
         var1.setParameterValue(this.m_cookingSound, FMODManager.instance.getParameterDescription("CookingState"), (float)this.m_cookingParameter);
      }

   }

   private void updateTemperature() {
      float var1 = GameServer.bServer ? (float)(System.currentTimeMillis() - this.lastUpdateTime) / 50.0F : 1.0F;
      ItemContainer var2 = this.getOutermostContainer();
      float var3 = var2 == null ? 1.0F : var2.getTemprature();
      if (this.Heat > var3) {
         this.Heat -= 0.001F * var1 * GameTime.instance.getMultiplier();
         if (this.Heat < Math.max(0.2F, var3)) {
            this.Heat = Math.max(0.2F, var3);
         }
      }

      if (this.Heat < var3) {
         this.Heat += var3 / 1000.0F * var1 * GameTime.instance.getMultiplier();
         if (this.Heat > Math.min(3.0F, var3)) {
            this.Heat = Math.min(3.0F, var3);
         }
      }

   }

   private void updateRotting(ItemContainer var1) {
      if ((double)this.OffAgeMax != 1.0E9) {
         if (!GameClient.bClient) {
            if (this.replaceOnRotten != null && !this.replaceOnRotten.isEmpty()) {
               this.updateAge();
               if (this.isRotten()) {
                  InventoryItem var2 = InventoryItemFactory.CreateItem(this.getModule() + "." + this.replaceOnRotten, this);
                  if (var2 == null) {
                     String var10001 = this.replaceOnRotten;
                     DebugLog.General.warn("ReplaceOnRotten = " + var10001 + " doesn't exist for " + this.getFullType());
                     this.destroyThisItem();
                     return;
                  }

                  var2.setAge(this.getAge());
                  IsoWorldInventoryObject var3 = this.getWorldItem();
                  if (var3 != null && var3.getSquare() != null) {
                     IsoGridSquare var4 = var3.getSquare();
                     if (!GameServer.bServer) {
                        var3.item = var2;
                        var2.setWorldItem(var3);
                        var3.updateSprite();
                        IsoWorld.instance.CurrentCell.addToProcessItemsRemove((InventoryItem)this);
                        LuaEventManager.triggerEvent("OnContainerUpdate");
                        return;
                     }

                     var4.AddWorldInventoryItem(var2, var3.xoff, var3.yoff, var3.zoff, true);
                  } else if (this.container != null) {
                     this.container.AddItem(var2);
                     if (GameServer.bServer) {
                        GameServer.sendAddItemToContainer(this.container, var2);
                     }
                  }

                  this.destroyThisItem();
                  return;
               }
            }

            if (SandboxOptions.instance.DaysForRottenFoodRemoval.getValue() >= 0) {
               if (var1 != null && var1.parent instanceof IsoCompost) {
                  return;
               }

               this.updateAge();
               if (this.getAge() > (float)(this.getOffAgeMax() + SandboxOptions.instance.DaysForRottenFoodRemoval.getValue())) {
                  this.destroyThisItem();
                  return;
               }
            }

         }
      }
   }

   private float getFridgeFactor() {
      float var10000;
      switch (SandboxOptions.instance.FridgeFactor.getValue()) {
         case 1:
            var10000 = 0.4F;
            break;
         case 2:
            var10000 = 0.3F;
            break;
         case 3:
         default:
            var10000 = 0.2F;
            break;
         case 4:
            var10000 = 0.1F;
            break;
         case 5:
            var10000 = 0.03F;
            break;
         case 6:
            var10000 = 0.0F;
      }

      return var10000;
   }

   private float getFoodRotSpeed() {
      float var10000;
      switch (SandboxOptions.instance.FoodRotSpeed.getValue()) {
         case 1:
            var10000 = 1.7F;
            break;
         case 2:
            var10000 = 1.4F;
            break;
         case 3:
         default:
            var10000 = 1.0F;
            break;
         case 4:
            var10000 = 0.7F;
            break;
         case 5:
            var10000 = 0.4F;
      }

      return var10000;
   }

   public void updateAge() {
      this.updateAge(true);
   }

   public void updateAge(boolean var1) {
      float var2 = (float)GameTime.getInstance().getWorldAgeHours();
      ItemContainer var3 = this.getOutermostContainer();
      this.updateFreezing(var3, var2);
      boolean var4 = false;
      if (var3 != null && var3.getSourceGrid() != null && var3.getSourceGrid().haveElectricity()) {
         var4 = true;
      }

      float var5 = 0.2F;
      if (SandboxOptions.instance.FridgeFactor.getValue() == 1) {
         var5 = 0.4F;
      } else if (SandboxOptions.instance.FridgeFactor.getValue() == 2) {
         var5 = 0.3F;
      } else if (SandboxOptions.instance.FridgeFactor.getValue() == 4) {
         var5 = 0.1F;
      } else if (SandboxOptions.instance.FridgeFactor.getValue() == 5) {
         var5 = 0.03F;
      } else if (SandboxOptions.instance.FridgeFactor.getValue() == 6) {
         var5 = 0.0F;
      }

      if (this.LastAged < 0.0F) {
         this.LastAged = var2;
      } else if (this.LastAged > var2) {
         this.LastAged = var2;
      }

      if (var2 > this.LastAged) {
         double var6 = (double)(var2 - this.LastAged);
         if (var3 != null && this.Heat != var3.getTemprature()) {
            if (var6 < 0.3333333432674408) {
               if (!IsoWorld.instance.getCell().getProcessItems().contains(this)) {
                  this.Heat = GameTime.instance.Lerp(this.Heat, var3.getTemprature(), (float)var6 / 0.33333334F);
                  IsoWorld.instance.getCell().addToProcessItems((InventoryItem)this);
               }
            } else {
               this.Heat = var3.getTemprature();
            }
         }

         if (this.isFrozen()) {
            var6 *= 0.0;
         } else if (var3 != null && (var3.getType().equals("fridge") || var3.getType().equals("freezer"))) {
            if (var3.getSourceGrid() != null && var3.getSourceGrid().haveElectricity()) {
               var6 *= (double)this.getFridgeFactor();
            } else if (SandboxOptions.instance.getElecShutModifier() > -1 && this.LastAged < (float)(SandboxOptions.instance.getElecShutModifier() * 24)) {
               float var8 = Math.min((float)(SandboxOptions.instance.getElecShutModifier() * 24), var2);
               var6 = (double)((var8 - this.LastAged) * this.getFridgeFactor());
               if (var2 > (float)(SandboxOptions.instance.getElecShutModifier() * 24)) {
                  var6 += (double)(var2 - (float)(SandboxOptions.instance.getElecShutModifier() * 24));
               }
            }
         }

         boolean var12 = !this.Burnt && this.OffAge < 1000000000 && this.Age < (float)this.OffAge;
         boolean var9 = !this.Burnt && this.OffAgeMax < 1000000000 && this.Age >= (float)this.OffAgeMax;
         this.Age = (float)((double)this.Age + var6 * (double)this.getFoodRotSpeed() / 24.0);
         this.LastAged = var2;
         boolean var10 = !this.Burnt && this.OffAge < 1000000000 && this.Age < (float)this.OffAge;
         boolean var11 = !this.Burnt && this.OffAgeMax < 1000000000 && this.Age >= (float)this.OffAgeMax;
         if (!GameServer.bServer && (var12 != var10 || var9 != var11)) {
            LuaEventManager.triggerEvent("OnContainerUpdate", this);
         }

         if (var1 && GameServer.bServer) {
            GameServer.sendItemStats(this);
         }
      }

   }

   public void setAutoAge() {
      ItemContainer var1 = this.getOutermostContainer();
      float var2 = (float)GameTime.getInstance().getWorldAgeHours() / 24.0F;
      var2 += (float)((SandboxOptions.instance.TimeSinceApo.getValue() - 1) * 30);
      float var3 = var2;
      boolean var4 = false;
      if (var1 != null && var1.getParent() != null && var1.getParent().getSprite() != null) {
         var4 = var1.getParent().getSprite().getProperties().Is("IsFridge");
      }

      if (var1 != null && (var4 || var1.getType().equals("fridge") || var1.getType().equals("freezer"))) {
         int var5 = SandboxOptions.instance.ElecShutModifier.getValue();
         if (var5 > -1) {
            float var6 = Math.min((float)var5, var2);
            int var7 = SandboxOptions.instance.FridgeFactor.getValue();
            float var8 = 0.2F;
            if (var7 == 1) {
               var8 = 0.4F;
            } else if (var7 == 2) {
               var8 = 0.3F;
            } else if (var7 == 4) {
               var8 = 0.1F;
            } else if (var7 == 5) {
               var8 = 0.03F;
            } else if (var7 == 6) {
               var8 = 0.0F;
            }

            if (!var1.getType().equals("fridge") && this.canBeFrozen() && !var4) {
               float var9 = var6;
               float var10 = 100.0F;
               if (var2 > var6) {
                  float var11 = (var2 - var6) * 24.0F;
                  float var12 = 1440.0F / GameTime.getInstance().getMinutesPerDay() * 60.0F * 5.0F;
                  float var13 = 0.0095999995F;
                  var10 -= var13 * var12 * var11;
                  if (var10 > 0.0F) {
                     var9 = var6 + var11 / 24.0F;
                  } else {
                     float var14 = 100.0F / (var13 * var12);
                     var9 = var6 + var14 / 24.0F;
                     var10 = 0.0F;
                  }
               }

               var3 = var2 - var9;
               var3 += var9 * 0.0F;
               this.setFreezingTime(var10);
            } else {
               var3 = var2 - var6;
               var3 += var6 * this.getFridgeFactor();
            }
         }
      }

      this.Age = var3 * this.getFoodRotSpeed();
      this.LastAged = (float)GameTime.getInstance().getWorldAgeHours();
      this.LastFrozenUpdate = this.LastAged;
      if (var1 != null) {
         this.setHeat(var1.getTemprature());
      }

   }

   private void updateFreezing(ItemContainer var1, float var2) {
      if (this.LastFrozenUpdate < 0.0F) {
         this.LastFrozenUpdate = var2;
      } else if (this.LastFrozenUpdate > var2) {
         this.LastFrozenUpdate = var2;
      }

      if (var2 > this.LastFrozenUpdate) {
         float var3 = var2 - this.LastFrozenUpdate;
         float var4 = 4.0F;
         float var5 = 1.5F;
         if (this.isFreezing()) {
            this.setFertilized(false);
            this.setFreezingTime(this.getFreezingTime() + var3 / var4 * 100.0F);
         }

         if (this.isThawing()) {
            float var6 = var5;
            if (var1 != null && "fridge".equals(var1.getType()) && var1.isPowered()) {
               var6 = var5 * 2.0F;
            }

            if (var1 != null && var1.getTemprature() > 1.0F) {
               var6 /= 6.0F;
            }

            this.setFreezingTime(this.getFreezingTime() - var3 / var6 * 100.0F);
         }

         this.LastFrozenUpdate = var2;
      }

   }

   public float getActualWeight() {
      float var1;
      float var3;
      if (this.haveExtraItems()) {
         var1 = this.getHungChange();
         float var7 = this.getBaseHunger();
         var3 = var7 == 0.0F ? 0.0F : var1 / var7;
         float var4 = 0.0F;
         if (this.getReplaceOnUse() != null) {
            String var5 = this.getReplaceOnUseFullType();
            Item var6 = ScriptManager.instance.getItem(var5);
            if (var6 != null) {
               var4 = var6.getActualWeight();
            }
         }

         float var9 = super.getActualWeight() + this.getExtraItemsWeight();
         float var10 = (var9 - var4) * var3 + var4;
         return var10;
      } else {
         if (this.getReplaceOnUse() != null && !this.isCustomWeight()) {
            String var8 = this.getReplaceOnUseFullType();
            Item var2 = ScriptManager.instance.getItem(var8);
            if (var2 != null) {
               var3 = 1.0F;
               if (this.getScriptItem().getHungerChange() < 0.0F) {
                  var3 = this.getHungChange() * 100.0F / this.getScriptItem().getHungerChange();
               } else if (this.getScriptItem().getThirstChange() < 0.0F) {
                  var3 = this.getThirstChange() * 100.0F / this.getScriptItem().getThirstChange();
               }

               return (this.getScriptItem().getActualWeight() - var2.getActualWeight()) * var3 + var2.getActualWeight();
            }
         } else if (!this.isCustomWeight()) {
            var1 = 1.0F;
            if (this.getScriptItem().getHungerChange() < 0.0F) {
               var1 = this.getHungChange() * 100.0F / this.getScriptItem().getHungerChange();
            } else if (this.getScriptItem().getThirstChange() < 0.0F) {
               var1 = this.getThirstChange() * 100.0F / this.getScriptItem().getThirstChange();
            }

            return this.getScriptItem().getActualWeight() * var1;
         }

         return super.getActualWeight();
      }
   }

   public float getWeight() {
      return this.getReplaceOnUse() != null ? this.getActualWeight() : super.getWeight();
   }

   public boolean CanStack(InventoryItem var1) {
      return false;
   }

   public void save(ByteBuffer var1, boolean var2) throws IOException {
      super.save(var1, var2);
      var1.putFloat(this.Age);
      var1.putFloat(this.LastAged);
      BitHeaderWrite var3 = BitHeader.allocWrite(BitHeader.HeaderSize.Byte, var1);
      if (this.calories != 0.0F || this.proteins != 0.0F || this.lipids != 0.0F || this.carbohydrates != 0.0F) {
         var3.addFlags(1);
         var1.putFloat(this.calories);
         var1.putFloat(this.proteins);
         var1.putFloat(this.lipids);
         var1.putFloat(this.carbohydrates);
      }

      if (this.hungChange != 0.0F) {
         var3.addFlags(2);
         var1.putFloat(this.hungChange);
      }

      if (this.baseHunger != 0.0F) {
         var3.addFlags(4);
         var1.putFloat(this.baseHunger);
      }

      if (this.unhappyChange != 0.0F) {
         var3.addFlags(8);
         var1.putFloat(this.unhappyChange);
      }

      if (this.boredomChange != 0.0F) {
         var3.addFlags(16);
         var1.putFloat(this.boredomChange);
      }

      if (this.thirstChange != 0.0F) {
         var3.addFlags(32);
         var1.putFloat(this.thirstChange);
      }

      BitHeaderWrite var4 = BitHeader.allocWrite(BitHeader.HeaderSize.Integer, var1);
      if (this.Heat != 1.0F) {
         var4.addFlags(1);
         var1.putFloat(this.Heat);
      }

      if (this.LastCookMinute != 0) {
         var4.addFlags(2);
         var1.putInt(this.LastCookMinute);
      }

      if (this.CookingTime != 0.0F) {
         var4.addFlags(4);
         var1.putFloat(this.CookingTime);
      }

      if (this.Cooked) {
         var4.addFlags(8);
      }

      if (this.Burnt) {
         var4.addFlags(16);
      }

      if (this.IsCookable) {
         var4.addFlags(32);
      }

      if (this.bDangerousUncooked) {
         var4.addFlags(64);
      }

      if (this.poisonDetectionLevel != -1) {
         var4.addFlags(128);
         var1.put((byte)this.poisonDetectionLevel);
      }

      if (this.spices != null) {
         var4.addFlags(256);
         var1.put((byte)this.spices.size());
         Iterator var5 = this.spices.iterator();

         while(var5.hasNext()) {
            String var6 = (String)var5.next();
            GameWindow.WriteString(var1, var6);
         }
      }

      if (this.PoisonPower != 0) {
         var4.addFlags(512);
         var1.put((byte)this.PoisonPower);
      }

      if (this.Chef != null) {
         var4.addFlags(1024);
         GameWindow.WriteString(var1, this.Chef);
      }

      if ((double)this.OffAge != 1.0E9) {
         var4.addFlags(2048);
         var1.putInt(this.OffAge);
      }

      if ((double)this.OffAgeMax != 1.0E9) {
         var4.addFlags(4096);
         var1.putInt(this.OffAgeMax);
      }

      if (this.painReduction != 0.0F) {
         var4.addFlags(8192);
         var1.putFloat(this.painReduction);
      }

      if (this.fluReduction != 0) {
         var4.addFlags(16384);
         var1.putInt(this.fluReduction);
      }

      if (this.ReduceFoodSickness != 0) {
         var4.addFlags(32768);
         var1.putInt(this.ReduceFoodSickness);
      }

      if (this.Poison) {
         var4.addFlags(65536);
      }

      if (this.UseForPoison != 0) {
         var4.addFlags(131072);
         var1.putShort((short)this.UseForPoison);
      }

      if (this.freezingTime != 0.0F) {
         var4.addFlags(262144);
         var1.putFloat(this.freezingTime);
      }

      if (this.isFrozen()) {
         var4.addFlags(524288);
      }

      if (this.LastFrozenUpdate != 0.0F) {
         var4.addFlags(1048576);
         var1.putFloat(this.LastFrozenUpdate);
      }

      if (this.rottenTime != 0.0F) {
         var4.addFlags(2097152);
         var1.putFloat(this.rottenTime);
      }

      if (this.compostTime != 0.0F) {
         var4.addFlags(4194304);
         var1.putFloat(this.compostTime);
      }

      if (this.cookedInMicrowave) {
         var4.addFlags(8388608);
      }

      if (this.fatigueChange != 0.0F) {
         var4.addFlags(16777216);
         var1.putFloat(this.fatigueChange);
      }

      if (this.endChange != 0.0F) {
         var4.addFlags(33554432);
         var1.putFloat(this.endChange);
      }

      if (this.milkQty > 0) {
         var4.addFlags(67108864);
         var1.putInt(this.milkQty);
         GameWindow.WriteString(var1, this.milkType);
      }

      if (this.isFertilized()) {
         var4.addFlags(134217728);
         var1.putInt(this.timeToHatch);
         var1.putInt(this.fertilizedTime);
         GameWindow.WriteString(var1, this.animalHatch);
         GameWindow.WriteString(var1, this.animalHatchBreed);
         ArrayList var8 = new ArrayList(this.eggGenome.keySet());
         var1.putInt(this.eggGenome.size());

         for(int var9 = 0; var9 < var8.size(); ++var9) {
            String var7 = (String)var8.get(var9);
            ((AnimalGene)this.eggGenome.get(var7)).save(var1, false);
         }

         var1.putInt(this.motherID);
      }

      if (!var4.equals(0)) {
         var3.addFlags(64);
         var4.write();
      } else {
         var1.position(var4.getStartPosition());
      }

      var3.write();
      var3.release();
      var4.release();
   }

   public void load(ByteBuffer var1, int var2) throws IOException {
      super.load(var1, var2);
      this.calories = 0.0F;
      this.proteins = 0.0F;
      this.lipids = 0.0F;
      this.carbohydrates = 0.0F;
      this.hungChange = 0.0F;
      this.baseHunger = 0.0F;
      this.unhappyChange = 0.0F;
      this.boredomChange = 0.0F;
      this.thirstChange = 0.0F;
      this.Heat = 1.0F;
      this.LastCookMinute = 0;
      this.CookingTime = 0.0F;
      this.Cooked = false;
      this.Burnt = false;
      this.IsCookable = false;
      this.bDangerousUncooked = false;
      this.poisonDetectionLevel = -1;
      this.spices = null;
      this.PoisonPower = 0;
      this.Chef = null;
      this.OffAge = 1000000000;
      this.OffAgeMax = 1000000000;
      this.painReduction = 0.0F;
      this.fluReduction = 0;
      this.ReduceFoodSickness = 0;
      this.Poison = false;
      this.UseForPoison = 0;
      this.freezingTime = 0.0F;
      this.frozen = false;
      this.LastFrozenUpdate = 0.0F;
      this.rottenTime = 0.0F;
      this.compostTime = 0.0F;
      this.cookedInMicrowave = false;
      this.fatigueChange = 0.0F;
      this.endChange = 0.0F;
      this.Age = var1.getFloat();
      this.LastAged = var1.getFloat();
      BitHeaderRead var3 = BitHeader.allocRead(BitHeader.HeaderSize.Byte, var1);
      if (!var3.equals(0)) {
         if (var3.hasFlags(1)) {
            this.calories = var1.getFloat();
            this.proteins = var1.getFloat();
            this.lipids = var1.getFloat();
            this.carbohydrates = var1.getFloat();
         }

         if (var3.hasFlags(2)) {
            this.hungChange = var1.getFloat();
         }

         if (var3.hasFlags(4)) {
            this.baseHunger = var1.getFloat();
         }

         if (var3.hasFlags(8)) {
            this.unhappyChange = var1.getFloat();
         }

         if (var3.hasFlags(16)) {
            this.boredomChange = var1.getFloat();
         }

         if (var3.hasFlags(32)) {
            this.thirstChange = var1.getFloat();
         }

         if (var3.hasFlags(64)) {
            BitHeaderRead var4 = BitHeader.allocRead(BitHeader.HeaderSize.Integer, var1);
            if (var4.hasFlags(1)) {
               this.Heat = var1.getFloat();
            }

            if (var4.hasFlags(2)) {
               this.LastCookMinute = var1.getInt();
            }

            if (var4.hasFlags(4)) {
               this.CookingTime = var1.getFloat();
            }

            this.Cooked = var4.hasFlags(8);
            this.Burnt = var4.hasFlags(16);
            this.IsCookable = var4.hasFlags(32);
            this.bDangerousUncooked = var4.hasFlags(64);
            if (var4.hasFlags(128)) {
               this.poisonDetectionLevel = var1.get();
            }

            int var5;
            int var6;
            if (var4.hasFlags(256)) {
               this.spices = new ArrayList();
               var5 = var1.get();

               for(var6 = 0; var6 < var5; ++var6) {
                  String var7 = GameWindow.ReadString(var1);
                  this.spices.add(var7);
               }
            }

            if (var4.hasFlags(512)) {
               this.PoisonPower = var1.get();
            }

            if (var4.hasFlags(1024)) {
               this.Chef = GameWindow.ReadString(var1);
            }

            if (var4.hasFlags(2048)) {
               this.OffAge = var1.getInt();
            }

            if (var4.hasFlags(4096)) {
               this.OffAgeMax = var1.getInt();
            }

            if (var4.hasFlags(8192)) {
               this.painReduction = var1.getFloat();
            }

            if (var4.hasFlags(16384)) {
               this.fluReduction = var1.getInt();
            }

            if (var4.hasFlags(32768)) {
               this.ReduceFoodSickness = var1.getInt();
            }

            this.Poison = var4.hasFlags(65536);
            if (var4.hasFlags(131072)) {
               this.UseForPoison = var1.getShort();
            }

            if (var4.hasFlags(262144)) {
               this.freezingTime = var1.getFloat();
            }

            this.setFrozen(var4.hasFlags(524288));
            if (var4.hasFlags(1048576)) {
               this.LastFrozenUpdate = var1.getFloat();
            }

            if (var4.hasFlags(2097152)) {
               this.rottenTime = var1.getFloat();
            }

            if (var4.hasFlags(4194304)) {
               this.compostTime = var1.getFloat();
            }

            this.cookedInMicrowave = var4.hasFlags(8388608);
            if (var4.hasFlags(16777216)) {
               this.fatigueChange = var1.getFloat();
            }

            if (var4.hasFlags(33554432)) {
               this.endChange = var1.getFloat();
            }

            if (var4.hasFlags(67108864)) {
               this.milkQty = var1.getInt();
               this.milkType = GameWindow.ReadString(var1);
            }

            if (var4.hasFlags(134217728)) {
               this.timeToHatch = var1.getInt();
               this.fertilizedTime = var1.getInt();
               this.animalHatch = GameWindow.ReadString(var1);
               this.animalHatchBreed = GameWindow.ReadString(var1);
               var5 = var1.getInt();
               this.eggGenome = new HashMap();

               for(var6 = 0; var6 < var5; ++var6) {
                  AnimalGene var8 = new AnimalGene();
                  var8.load(var1, var2, false);
                  this.eggGenome.put(var8.name, var8);
               }

               this.motherID = var1.getInt();
               this.setFertilized(true);
            }

            var4.release();
         }
      }

      var3.release();
      if (GameServer.bServer && this.LastAged == -1.0F) {
         this.LastAged = (float)GameTime.getInstance().getWorldAgeHours();
      }

   }

   public boolean finishupdate() {
      if (this.container == null && (this.getWorldItem() == null || this.getWorldItem().getSquare() == null)) {
         return true;
      } else if (this.IsCookable) {
         return false;
      } else if (this.container != null && (this.Heat != this.container.getTemprature() || this.container.isTemperatureChanging())) {
         return false;
      } else if (this.isTainted && this.container != null && this.container.getTemprature() > 1.0F) {
         return false;
      } else {
         if ((!GameClient.bClient || this.isInLocalPlayerInventory()) && (double)this.OffAgeMax != 1.0E9) {
            if (this.replaceOnRotten != null && !this.replaceOnRotten.isEmpty()) {
               return false;
            }

            if (SandboxOptions.instance.DaysForRottenFoodRemoval.getValue() != -1) {
               return false;
            }
         }

         return true;
      }
   }

   public boolean shouldUpdateInWorld() {
      if (!GameClient.bClient && (double)this.OffAgeMax != 1.0E9) {
         if (this.replaceOnRotten != null && !this.replaceOnRotten.isEmpty()) {
            return true;
         }

         if (SandboxOptions.instance.DaysForRottenFoodRemoval.getValue() != -1) {
            return true;
         }
      }

      if (this.getHeat() != 1.0F) {
         return true;
      } else {
         return !GameClient.bClient && this.isFertilized();
      }
   }

   public String getName() {
      String var1 = "";
      if (this.Burnt) {
         var1 = var1 + this.BurntString + ", ";
      } else if (this.OffAge < 1000000000 && this.Age < (float)this.OffAge && !this.hasTag("HideFresh")) {
         var1 = var1 + this.FreshString + ", ";
      } else if (this.OffAgeMax < 1000000000 && this.Age >= (float)this.OffAgeMax) {
         var1 = var1 + this.OffString + ", ";
      } else if (this.OffAgeMax < 1000000000 && this.Age >= (float)this.OffAge) {
         var1 = var1 + this.StaleString + ", ";
      }

      if (this.isCooked() && !this.Burnt && !this.hasTag("HideCooked")) {
         if (this.hasTag("Grilled")) {
            var1 = var1 + this.GrilledString + ", ";
         } else if (this.hasTag("Toastable")) {
            var1 = var1 + this.ToastedString + ", ";
         } else {
            var1 = var1 + this.CookedString + ", ";
         }
      } else if (this.IsCookable && !this.Burnt && !this.hasTag("HideCooked") && !this.hasTag("HideUncooked")) {
         var1 = var1 + this.UnCookedString + ", ";
      }

      if (this.isFrozen()) {
         var1 = var1 + this.FrozenString + ", ";
      }

      if (var1.length() > 2) {
         var1 = var1.substring(0, var1.length() - 2);
      }

      var1 = var1.trim();
      return var1.isEmpty() ? this.name : Translator.getText("IGUI_FoodNaming", var1, this.name);
   }

   public void DoTooltip(ObjectTooltip var1, ObjectTooltip.Layout var2) {
      ColorInfo var4 = new ColorInfo();
      ColorInfo var5 = Core.getInstance().getGoodHighlitedColor();
      ColorInfo var6 = Core.getInstance().getBadHighlitedColor();
      float var7 = var5.getR();
      float var8 = var5.getG();
      float var9 = var5.getB();
      float var10 = var6.getR();
      float var11 = var6.getG();
      float var12 = var6.getB();
      ObjectTooltip.LayoutItem var3;
      int var13;
      if (this.getHungerChange() != 0.0F && !this.hasTag("HideHungerChange")) {
         var3 = var2.addItem();
         var3.setLabel(Translator.getText("Tooltip_food_Hunger") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
         var13 = (int)(this.getHungerChange() * 100.0F);
         var3.setValueRight(var13, false);
         Core.getInstance().getBadHighlitedColor().interp(Core.getInstance().getGoodHighlitedColor(), (float)var13, var4);
      }

      float var26;
      if (this.getThirstChange() != 0.0F) {
         var3 = var2.addItem();
         var3.setLabel(Translator.getText("Tooltip_food_Thirst") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
         var26 = this.getThirstChange() * -2.0F;
         if (var26 > 0.0F) {
            var3.setProgress(var26, var7, var8, var9, 1.0F);
         } else {
            var3.setProgress(var26 * -1.0F, var10, var11, var12, 1.0F);
         }
      }

      if (this.getEnduranceChange() != 0.0F) {
         var3 = var2.addItem();
         var13 = (int)(this.getEnduranceChange() * 100.0F);
         var3.setLabel(Translator.getText("Tooltip_food_Endurance") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
         var3.setValueRight(var13, true);
      }

      if (this.getStressChange() != 0.0F) {
         var3 = var2.addItem();
         var13 = (int)(this.getStressChange() * 100.0F);
         var3.setLabel(Translator.getText("Tooltip_food_Stress") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
         var3.setValueRight(var13, false);
      }

      if (this.getBoredomChange() != 0.0F) {
         var3 = var2.addItem();
         var26 = this.getBoredomChange() * -0.02F;
         var3.setLabel(Translator.getText("Tooltip_food_Boredom") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
         if (var26 > 0.0F) {
            var3.setProgress(var26, var7, var8, var9, 1.0F);
         } else {
            var3.setProgress(var26 * -1.0F, var10, var11, var12, 1.0F);
         }
      }

      if (this.getUnhappyChange() != 0.0F) {
         var3 = var2.addItem();
         var26 = this.getUnhappyChange() * -0.02F;
         var3.setLabel(Translator.getText("Tooltip_food_Unhappiness") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
         if (var26 > 0.0F) {
            var3.setProgress(var26, var7, var8, var9, 1.0F);
         } else {
            var3.setProgress(var26 * -1.0F, var10, var11, var12, 1.0F);
         }
      }

      float var14;
      float var15;
      float var16;
      float var18;
      float var19;
      float var20;
      if (this.isIsCookable() && !this.isFrozen() && !this.Burnt && (double)this.getHeat() > 1.6) {
         var26 = this.getCookingTime();
         var14 = this.getMinutesToCook();
         var15 = this.getMinutesToBurn();
         var16 = var26 / var14;
         ColorInfo var17 = Core.getInstance().getGoodHighlitedColor();
         var18 = var17.getR();
         var19 = var17.getG();
         var20 = var17.getB();
         float var21 = 1.0F;
         float var22 = var17.getR();
         float var23 = var17.getG();
         float var24 = var17.getB();
         String var25 = Translator.getText("IGUI_invpanel_Cooking");
         if (var26 > var14) {
            var17 = Core.getInstance().getBadHighlitedColor();
            var25 = Translator.getText("IGUI_invpanel_Burning");
            var22 = var17.getR();
            var23 = var17.getG();
            var24 = var17.getB();
            var16 = (var26 - var14) / (var15 - var14);
            var18 = var17.getR();
            var19 = var17.getG();
            var20 = var17.getB();
         }

         var3 = var2.addItem();
         var3.setLabel(var25 + ": ", var22, var23, var24, 1.0F);
         var3.setProgress(var16, var18, var19, var20, var21);
      }

      if (this.getFreezingTime() < 100.0F && this.getFreezingTime() > 0.0F) {
         var26 = this.getFreezingTime() / 100.0F;
         var14 = 0.0F;
         var15 = 0.6F;
         var16 = 0.0F;
         float var31 = 0.7F;
         var18 = 1.0F;
         var19 = 1.0F;
         var20 = 0.8F;
         var3 = var2.addItem();
         var3.setLabel(Translator.getText("IGUI_invpanel_FreezingTime") + ": ", var18, var19, var20, 1.0F);
         var3.setProgress(var26, var14, var15, var16, var31);
      }

      if (Core.bDebug && this.isFertilized()) {
         var3 = var2.addItem();
         var3.setLabel("Fertilized :", 1.0F, 1.0F, 0.8F, 1.0F);
         var3.setValue(this.fertilizedTime + "/" + this.timeToHatch, 1.0F, 1.0F, 1.0F, 1.0F);
      } else if (this.isFertilized() && Double.valueOf((double)this.timeToHatch) / Double.valueOf((double)this.fertilizedTime) < 4.0) {
         var3 = var2.addItem();
         var3.setLabel(Translator.getText("Tooltip_food_fertilized"), 0.5F, 1.0F, 0.4F, 1.0F);
      }

      IsoGameCharacter var28 = var1.getCharacter();
      IsoPlayer var27 = (IsoPlayer)Type.tryCastTo(var28, IsoPlayer.class);
      boolean var29 = var28 != null && var28.Traits.Illiterate.isSet();
      boolean var30 = var27 != null && var27.tooDarkToRead();
      boolean var32 = this.getModData().rawget("NoLabel") != null;
      boolean var33 = this.isPackaged() && var28 != null && !var29 && !var30 && !var32;
      if (Core.bDebug && DebugOptions.instance.TooltipInfo.getValue() || var33 || var28 != null && (var28.Traits.Nutritionist.isSet() || var28.Traits.Nutritionist2.isSet())) {
         var3 = var2.addItem();
         var3.setLabel(Translator.getText("Tooltip_food_Nutrition") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
         var3 = var2.addItem();
         var3.setLabel(Translator.getText("Tooltip_food_Calories") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
         var3.setValueRightNoPlus(this.getCalories());
         var3 = var2.addItem();
         var3.setLabel(Translator.getText("Tooltip_food_Carbs") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
         var3.setValueRightNoPlus(this.getCarbohydrates());
         var3 = var2.addItem();
         var3.setLabel(Translator.getText("Tooltip_food_Prots") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
         var3.setValueRightNoPlus(this.getProteins());
         var3 = var2.addItem();
         var3.setLabel(Translator.getText("Tooltip_food_Fat") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
         var3.setValueRightNoPlus(this.getLipids());
      } else if (this.isPackaged() && var29) {
         var3 = var2.addItem();
         var3.setLabel(Translator.getText("Tooltip_food_Nutrition") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
         var3 = var2.addItem();
         var3.setLabel(Translator.getText("ContextMenu_Illiterate"), 1.0F, 1.0F, 0.8F, 1.0F);
      } else if (this.isPackaged() && var30) {
         var3 = var2.addItem();
         var3.setLabel(Translator.getText("Tooltip_food_Nutrition") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
         var3 = var2.addItem();
         var3.setLabel(Translator.getText("ContextMenu_TooDark"), 1.0F, 1.0F, 0.8F, 1.0F);
      }

      if (this.isbDangerousUncooked() && !this.isCooked() && !this.isBurnt()) {
         var3 = var2.addItem();
         var3.setLabel(Translator.getText("Tooltip_food_Dangerous_uncooked"), Core.getInstance().getBadHighlitedColor().getR(), Core.getInstance().getBadHighlitedColor().getG(), Core.getInstance().getBadHighlitedColor().getB(), 1.0F);
         if (this.hasTag("Egg")) {
            var3.setLabel(Translator.getText("Tooltip_food_SlightDanger_uncooked"), 1.0F, 0.0F, 0.0F, 1.0F);
         }
      }

      if (this.getScriptItem().RemoveUnhappinessWhenCooked && !this.isCooked() && !this.isBurnt()) {
         var3 = var2.addItem();
         var3.setLabel(Translator.getText("Tooltip_food_CookToRemoveUnhappiness"), Core.getInstance().getBadHighlitedColor().getR(), Core.getInstance().getBadHighlitedColor().getG(), Core.getInstance().getBadHighlitedColor().getB(), 1.0F);
      }

      if ((this.isGoodHot() || this.isBadCold()) && this.Heat < 1.3F) {
         var3 = var2.addItem();
         var3.setLabel(Translator.getText("Tooltip_food_BetterHot"), 1.0F, 0.9F, 0.9F, 1.0F);
      }

      if (this.cookedInMicrowave) {
         var3 = var2.addItem();
         var3.setLabel(Translator.getText("Tooltip_food_CookedInMicrowave"), 1.0F, 0.9F, 0.9F, 1.0F);
      }

      if (!StringUtils.isNullOrEmpty(this.getMilkType())) {
         var3 = var2.addItem();
         var3.setLabel(Translator.getText("Tooltip_food_MilkType") + ": ", 1.0F, 0.9F, 0.9F, 1.0F);
         var3.setValue(Translator.getText("Tooltip_food_" + this.getMilkType()), 1.0F, 0.9F, 0.9F, 1.0F);
      }

      if (Core.bDebug && DebugOptions.instance.TooltipInfo.getValue()) {
         var3 = var2.addItem();
         var3.setLabel("DBG: BaseHunger", 0.0F, 1.0F, 0.0F, 1.0F);
         var3.setValueRight((int)(this.getBaseHunger() * 100.0F), false);
         var3 = var2.addItem();
         var3.setLabel("DBG: Age", 0.0F, 1.0F, 0.0F, 1.0F);
         var3.setValueRightNoPlus(this.getAge() * 24.0F);
         if ((double)this.getOffAgeMax() != 1.0E9) {
            var3 = var2.addItem();
            var3.setLabel("DBG: Age Fresh", 0.0F, 1.0F, 0.0F, 1.0F);
            var3.setValueRightNoPlus((float)this.getOffAge() * 24.0F);
            var3 = var2.addItem();
            var3.setLabel("DBG: Age Rotten", 0.0F, 1.0F, 0.0F, 1.0F);
            var3.setValueRightNoPlus(this.getOffAgeMax() * 24);
         }

         var3 = var2.addItem();
         var3.setLabel("DBG: Heat", 0.0F, 1.0F, 0.0F, 1.0F);
         var3.setValueRightNoPlus(this.getHeat());
         var3 = var2.addItem();
         var3.setLabel("DBG: Freeze Time", 0.0F, 1.0F, 0.0F, 1.0F);
         var3.setValueRightNoPlus(this.getFreezingTime());
         var3 = var2.addItem();
         var3.setLabel("DBG: Compost Time", 0.0F, 1.0F, 0.0F, 1.0F);
         var3.setValueRightNoPlus(this.getCompostTime());
      }

   }

   public float getEnduranceChange() {
      if (this.Burnt) {
         return this.endChange / 3.0F;
      } else if (this.Age >= (float)this.OffAge && this.Age < (float)this.OffAgeMax) {
         return this.endChange / 2.0F;
      } else {
         return this.isCooked() ? this.endChange * 2.0F : this.endChange;
      }
   }

   public void setEnduranceChange(float var1) {
      this.endChange = var1;
   }

   public float getUnhappyChange() {
      float var1 = this.unhappyChange;
      Boolean var2 = "Icecream".equals(this.getType()) || this.hasTag("GoodFrozen");
      if (this.isFrozen() && !var2) {
         var1 += 30.0F;
      }

      if (this.Burnt) {
         var1 += 20.0F;
      }

      if (this.Age >= (float)this.OffAge && this.Age < (float)this.OffAgeMax) {
         var1 += 10.0F;
      }

      if (this.Age >= (float)this.OffAgeMax) {
         var1 += 20.0F;
      }

      if (this.isBadCold() && this.IsCookable && this.isCooked() && this.Heat < 1.3F) {
         var1 += 2.0F;
      }

      if (this.isGoodHot() && this.IsCookable && this.isCooked() && this.Heat > 1.3F) {
         var1 -= 2.0F;
      }

      return var1;
   }

   public float getBoredomChange() {
      float var1 = this.boredomChange;
      Boolean var2 = "Icecream".equals(this.getType()) || this.hasTag("GoodFrozen");
      if (this.isFrozen() && !var2) {
         var1 += 30.0F;
      }

      if (this.Burnt) {
         var1 += 20.0F;
      }

      if (this.Age >= (float)this.OffAge && this.Age < (float)this.OffAgeMax) {
         var1 += 10.0F;
      }

      if (this.Age >= (float)this.OffAgeMax) {
         var1 += 20.0F;
      }

      return var1;
   }

   public float getHungerChange() {
      float var1 = this.hungChange;
      if (var1 != 0.0F) {
         if (this.isCooked()) {
            return var1 * 1.3F;
         }

         float var2 = var1 < 0.0F ? -1.0F : 1.0F;
         float var3 = Math.abs(var1);
         if (this.Burnt) {
            return Math.max(var3 / 3.0F, 0.01F) * var2;
         }

         if (this.Age >= (float)this.OffAge && this.Age < (float)this.OffAgeMax) {
            return Math.max(var3 / 1.3F, 0.01F) * var2;
         }

         if (this.Age >= (float)this.OffAgeMax) {
            return Math.max(var3 / 2.2F, 0.01F) * var2;
         }
      }

      return var1;
   }

   public float getStressChange() {
      if (this.Burnt) {
         return this.stressChange / 4.0F;
      } else if (this.Age >= (float)this.OffAge && this.Age < (float)this.OffAgeMax) {
         return this.stressChange / 1.3F;
      } else if (this.Age >= (float)this.OffAgeMax) {
         return this.stressChange / 2.0F;
      } else {
         return this.isCooked() ? this.stressChange * 1.3F : this.stressChange;
      }
   }

   public float getBoredomChangeUnmodified() {
      return this.boredomChange;
   }

   public float getEnduranceChangeUnmodified() {
      return this.endChange;
   }

   public float getStressChangeUnmodified() {
      return this.stressChange;
   }

   public float getThirstChangeUnmodified() {
      return this.thirstChange;
   }

   public float getUnhappyChangeUnmodified() {
      return this.unhappyChange;
   }

   public float getScore(SurvivorDesc var1) {
      float var2 = 0.0F;
      var2 -= this.getHungerChange() * 100.0F;
      return var2;
   }

   public boolean isBadCold() {
      return this.bBadCold;
   }

   public void setBadCold(boolean var1) {
      this.bBadCold = var1;
   }

   public boolean isGoodHot() {
      return this.bGoodHot;
   }

   public void setGoodHot(boolean var1) {
      this.bGoodHot = var1;
   }

   public boolean isCookedInMicrowave() {
      return this.cookedInMicrowave;
   }

   public void setCookedInMicrowave(boolean var1) {
      this.cookedInMicrowave = var1;
   }

   public float getHeat() {
      return this.Heat;
   }

   public float getInvHeat() {
      return this.Heat > 1.0F ? (this.Heat - 1.0F) / 2.0F : 1.0F - (this.Heat - 0.2F) / 0.8F;
   }

   public void setHeat(float var1) {
      this.Heat = var1;
   }

   public float getEndChange() {
      return this.endChange;
   }

   public void setEndChange(float var1) {
      this.endChange = var1;
   }

   /** @deprecated */
   @Deprecated
   public float getBaseHungChange() {
      return this.getHungChange();
   }

   public float getHungChange() {
      return this.hungChange;
   }

   public void setHungChange(float var1) {
      this.hungChange = var1;
   }

   public String getUseOnConsume() {
      return this.useOnConsume;
   }

   public void setUseOnConsume(String var1) {
      this.useOnConsume = var1;
   }

   public boolean isRotten() {
      if (this.isFertilized()) {
         return false;
      } else {
         return this.Age >= (float)this.OffAgeMax;
      }
   }

   public boolean isFresh() {
      if (this.isFertilized()) {
         return true;
      } else {
         return this.Age < (float)this.OffAge;
      }
   }

   public void setRotten(boolean var1) {
      this.rotten = var1;
   }

   public boolean isbDangerousUncooked() {
      return this.bDangerousUncooked;
   }

   public void setbDangerousUncooked(boolean var1) {
      this.bDangerousUncooked = var1;
   }

   public int getLastCookMinute() {
      return this.LastCookMinute;
   }

   public void setLastCookMinute(int var1) {
      this.LastCookMinute = var1;
   }

   public float getThirstChange() {
      float var1 = this.thirstChange;
      if (this.Burnt) {
         return var1 / 5.0F;
      } else {
         return this.isCooked() ? var1 / 2.0F : var1;
      }
   }

   public void setThirstChange(float var1) {
      this.thirstChange = var1;
   }

   public void setReplaceOnCooked(List<String> var1) {
      this.ReplaceOnCooked = var1;
   }

   public List<String> getReplaceOnCooked() {
      return this.ReplaceOnCooked;
   }

   public float getBaseHunger() {
      return this.baseHunger;
   }

   public void setBaseHunger(float var1) {
      this.baseHunger = var1;
   }

   public boolean isSpice() {
      return this.isSpice;
   }

   public void setSpice(boolean var1) {
      this.isSpice = var1;
   }

   public boolean isPoison() {
      return this.Poison;
   }

   public int getPoisonDetectionLevel() {
      return this.poisonDetectionLevel;
   }

   public void setPoisonDetectionLevel(int var1) {
      this.poisonDetectionLevel = var1;
   }

   public Integer getPoisonLevelForRecipe() {
      return this.PoisonLevelForRecipe;
   }

   public void setPoisonLevelForRecipe(Integer var1) {
      this.PoisonLevelForRecipe = var1;
   }

   public int getUseForPoison() {
      return this.UseForPoison;
   }

   public void setUseForPoison(int var1) {
      this.UseForPoison = var1;
   }

   public int getPoisonPower() {
      return this.PoisonPower;
   }

   public void setPoisonPower(int var1) {
      this.PoisonPower = var1;
   }

   public String getFoodType() {
      return this.FoodType;
   }

   public void setFoodType(String var1) {
      this.FoodType = var1;
   }

   public boolean isRemoveNegativeEffectOnCooked() {
      return this.RemoveNegativeEffectOnCooked;
   }

   public void setRemoveNegativeEffectOnCooked(boolean var1) {
      this.RemoveNegativeEffectOnCooked = var1;
   }

   public String getCookingSound() {
      return this.getScriptItem().getCookingSound();
   }

   public String getCustomEatSound() {
      return this.CustomEatSound;
   }

   public void setCustomEatSound(String var1) {
      this.CustomEatSound = var1;
   }

   public String getChef() {
      return this.Chef;
   }

   public void setChef(String var1) {
      this.Chef = var1;
   }

   public String getOnCooked() {
      return this.OnCooked;
   }

   public void setOnCooked(String var1) {
      this.OnCooked = var1;
   }

   public String getHerbalistType() {
      return this.HerbalistType;
   }

   public void setHerbalistType(String var1) {
      this.HerbalistType = var1;
   }

   public ArrayList<String> getSpices() {
      return this.spices;
   }

   public void setSpices(ArrayList<String> var1) {
      if (var1 != null && !var1.isEmpty()) {
         if (this.spices == null) {
            this.spices = new ArrayList(var1);
         } else {
            this.spices.clear();
            this.spices.addAll(var1);
         }

      } else {
         if (this.spices != null) {
            this.spices.clear();
         }

      }
   }

   public Texture getTex() {
      if (this.Burnt) {
         return this.textureBurnt;
      } else if (this.Age >= (float)this.OffAgeMax) {
         return this.texturerotten;
      } else {
         return this.isCooked() ? this.textureCooked : super.getTex();
      }
   }

   public String getWorldTexture() {
      if (this.Burnt) {
         return this.WorldTextureOverdone;
      } else if (this.Age >= (float)this.OffAgeMax) {
         return this.WorldTextureRotten;
      } else {
         return this.isCooked() ? this.WorldTextureCooked : this.WorldTexture;
      }
   }

   public String getStaticModel() {
      ModelScript var1;
      if (this.isBurnt()) {
         var1 = ScriptManager.instance.getModelScript(super.getStaticModel() + "Burnt");
         if (var1 != null) {
            return var1.getName();
         }
      }

      if (this.isRotten()) {
         var1 = ScriptManager.instance.getModelScript(super.getStaticModel() + "Rotten");
         if (var1 != null) {
            return var1.getName();
         }
      }

      if (this.isCooked()) {
         var1 = ScriptManager.instance.getModelScript(super.getStaticModel() + "Cooked");
         if (var1 != null) {
            return var1.getName();
         }
      }

      return super.getStaticModel();
   }

   public int getReduceFoodSickness() {
      if (this.Burnt) {
         return (int)((float)this.ReduceFoodSickness / 3.0F);
      } else if (this.Age >= (float)this.OffAge && this.Age < (float)this.OffAgeMax) {
         return (int)((float)this.ReduceFoodSickness / 1.3F);
      } else if (this.Age >= (float)this.OffAgeMax) {
         return (int)((float)this.ReduceFoodSickness / 2.2F);
      } else {
         return this.isCooked() ? (int)((float)this.ReduceFoodSickness * 1.3F) : this.ReduceFoodSickness;
      }
   }

   public void setReduceFoodSickness(int var1) {
      this.ReduceFoodSickness = var1;
   }

   public int getFluReduction() {
      return this.fluReduction;
   }

   public void setFluReduction(int var1) {
      this.fluReduction = var1;
   }

   public float getPainReduction() {
      return this.painReduction;
   }

   public void setPainReduction(float var1) {
      this.painReduction = var1;
   }

   public float getCarbohydrates() {
      return this.carbohydrates;
   }

   public void setCarbohydrates(float var1) {
      this.carbohydrates = var1;
   }

   public float getLipids() {
      return this.lipids;
   }

   public void setLipids(float var1) {
      this.lipids = var1;
   }

   public float getProteins() {
      return this.proteins;
   }

   public void setProteins(float var1) {
      this.proteins = var1;
   }

   public float getCalories() {
      return this.calories;
   }

   public void setCalories(float var1) {
      this.calories = var1;
   }

   public boolean isPackaged() {
      return this.packaged;
   }

   public void setPackaged(boolean var1) {
      this.packaged = var1;
   }

   public float getFreezingTime() {
      return this.freezingTime;
   }

   public void setFreezingTime(float var1) {
      if (var1 >= 100.0F) {
         this.setFrozen(true);
         var1 = 100.0F;
      } else if (var1 <= 0.0F) {
         var1 = 0.0F;
         this.setFrozen(false);
      }

      this.freezingTime = var1;
   }

   public void freeze() {
      this.setFreezingTime(100.0F);
   }

   public boolean isFrozen() {
      return this.frozen;
   }

   public void setFrozen(boolean var1) {
      this.frozen = var1;
   }

   public boolean canBeFrozen() {
      return this.canBeFrozen;
   }

   public void setCanBeFrozen(boolean var1) {
      this.canBeFrozen = var1;
   }

   public boolean isFreezing() {
      return this.canBeFrozen() && !(this.getFreezingTime() >= 100.0F) && this.getOutermostContainer() != null && "freezer".equals(this.getOutermostContainer().getType()) ? this.getOutermostContainer().isPowered() : false;
   }

   public boolean isThawing() {
      if (this.canBeFrozen() && !(this.getFreezingTime() <= 0.0F)) {
         if (this.getOutermostContainer() != null && "freezer".equals(this.getOutermostContainer().getType())) {
            return !this.getOutermostContainer().isPowered();
         } else {
            return true;
         }
      } else {
         return false;
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

   public String getReplaceOnRotten() {
      return this.replaceOnRotten;
   }

   public void setReplaceOnRotten(String var1) {
      this.replaceOnRotten = var1;
   }

   public void multiplyFoodValues(float var1) {
      this.setBoredomChange(this.getBoredomChangeUnmodified() * var1);
      this.setUnhappyChange(this.getUnhappyChangeUnmodified() * var1);
      this.setHungChange(this.getHungChange() * var1);
      this.setFluReduction((int)((float)this.getFluReduction() * var1));
      this.setThirstChange(this.getThirstChangeUnmodified() * var1);
      this.setPainReduction(this.getPainReduction() * var1);
      this.setReduceFoodSickness((int)((float)this.getReduceFoodSickness() * var1));
      this.setEndChange(this.getEnduranceChangeUnmodified() * var1);
      this.setStressChange(this.getStressChangeUnmodified() * var1);
      this.setFatigueChange(this.getFatigueChange() * var1);
      this.setCalories(this.getCalories() * var1);
      this.setCarbohydrates(this.getCarbohydrates() * var1);
      this.setProteins(this.getProteins() * var1);
      this.setLipids(this.getLipids() * var1);
      this.setPoisonPower((int)((float)this.getPoisonPower() * var1));
   }

   public float getRottenTime() {
      return this.rottenTime;
   }

   public void setRottenTime(float var1) {
      this.rottenTime = var1;
   }

   public float getCompostTime() {
      return this.compostTime;
   }

   public void setCompostTime(float var1) {
      this.compostTime = var1;
   }

   public String getOnEat() {
      return this.onEat;
   }

   public void setOnEat(String var1) {
      this.onEat = var1;
   }

   public boolean isBadInMicrowave() {
      return this.badInMicrowave;
   }

   public void setBadInMicrowave(boolean var1) {
      this.badInMicrowave = var1;
   }

   public boolean isTainted() {
      return this.isTainted;
   }

   public void setTainted(boolean var1) {
      this.isTainted = var1;
   }

   private void destroyThisItem() {
      IsoWorldInventoryObject var1 = this.getWorldItem();
      if (var1 != null && var1.getSquare() != null) {
         if (GameServer.bServer) {
            GameServer.RemoveItemFromMap(var1);
         } else {
            var1.removeFromWorld();
            var1.removeFromSquare();
         }

         this.setWorldItem((IsoWorldInventoryObject)null);
      } else if (this.container != null) {
         IsoObject var2 = this.container.getParent();
         if (GameServer.bServer) {
            GameServer.sendRemoveItemFromContainer(this.container, this);
            this.container.Remove((InventoryItem)this);
         } else {
            this.container.Remove((InventoryItem)this);
         }

         IsoWorld.instance.CurrentCell.addToProcessItemsRemove((InventoryItem)this);
         LuaManager.updateOverlaySprite(var2);
      }

      if (!GameServer.bServer) {
         LuaEventManager.triggerEvent("OnContainerUpdate");
      }

   }

   public void setMilkQty(int var1) {
      this.milkQty = var1;
   }

   public int getMilkQty() {
      return this.milkQty;
   }

   public void setMilkType(String var1) {
      this.milkType = var1;
   }

   public String getMilkType() {
      return this.milkType;
   }

   public boolean isFertilized() {
      return this.fertilized;
   }

   public void setFertilized(boolean var1) {
      this.fertilized = var1;
   }

   public String getAnimalHatch() {
      return this.animalHatch;
   }

   public void setAnimalHatch(String var1) {
      this.animalHatch = var1;
   }

   public String getAnimalHatchBreed() {
      return this.animalHatchBreed;
   }

   public void setAnimalHatchBreed(String var1) {
      this.animalHatchBreed = var1;
   }

   public int getTimeToHatch() {
      return this.timeToHatch;
   }

   public void setTimeToHatch(int var1) {
      float var2 = 1.0F;
      switch (SandboxOptions.instance.AnimalEggHatch.getValue()) {
         case 1:
            var2 = 0.1F;
            break;
         case 2:
            var2 = 0.5F;
            break;
         case 3:
            var2 = 0.7F;
         case 4:
         default:
            break;
         case 5:
            var2 = 2.5F;
            break;
         case 6:
            var2 = 10.0F;
      }

      this.timeToHatch = (int)((float)var1 * var2);
   }

   public boolean isNormalAndFullFood() {
      if (!this.isTainted() && !this.isRotten() && !this.isFertilized()) {
         if (this.getSpices() != null) {
            return false;
         } else if (this.getCompostTime() != 0.0F) {
            return false;
         } else {
            Item var1 = this.getScriptItem();
            if (var1 == null) {
               return false;
            } else if ((float)this.getPoisonPower() != var1.getPoisonPower()) {
               return false;
            } else if (this.getPoisonDetectionLevel() != var1.getPoisonDetectionLevel()) {
               return false;
            } else {
               return this.isWholeFoodItem() && this.isUncooked();
            }
         }
      } else {
         return false;
      }
   }

   public boolean isWholeFoodItem() {
      Item var1 = this.getScriptItem();
      if (var1 == null) {
         return false;
      } else if (this.getHungerChange() != var1.getHungerChange()) {
         return false;
      } else if (this.getUnhappyChange() != var1.getUnhappyChange()) {
         return false;
      } else if (this.getBoredomChange() != var1.getBoredomChange()) {
         return false;
      } else if (this.getThirstChange() != var1.getThirstChange()) {
         return false;
      } else {
         Food var2 = (Food)InventoryItemFactory.CreateItem(this.getFullType());
         if (var2 == null) {
            return false;
         } else if (this.getCalories() != var2.getCalories()) {
            return false;
         } else if (this.getProteins() != var2.getProteins()) {
            return false;
         } else if (this.getLipids() != var2.getLipids()) {
            return false;
         } else if (this.getCarbohydrates() != var2.getCarbohydrates()) {
            return false;
         } else if (this.getPainReduction() != var2.getPainReduction()) {
            return false;
         } else if (this.getFluReduction() != var2.getFluReduction()) {
            return false;
         } else if (this.getReduceFoodSickness() != var2.getReduceFoodSickness()) {
            return false;
         } else {
            return this.getFatigueChange() == var2.getFatigueChange();
         }
      }
   }

   public boolean isUncooked() {
      return !this.isCooked() && !this.isBurnt();
   }

   public void OnAddedToContainer(ItemContainer var1) {
      if (GameServer.bServer) {
         this.updateAge();
      }

   }

   public void OnBeforeRemoveFromContainer(ItemContainer var1) {
      if (GameServer.bServer) {
         this.updateAge();
      }

   }

   public void setFertilizedTime(int var1) {
      this.fertilizedTime = var1;
   }

   public void inheritFoodAgeFrom(InventoryItem var1) {
      if (var1.isFood()) {
         Food var2 = (Food)var1;
         if (var2.canAge() && this.canAge()) {
            float var3 = var2.getAge() / (float)var2.getOffAgeMax();
            this.setAge((float)this.getOffAgeMax() * var3);
         }

      }
   }

   public void inheritOlderFoodAge(InventoryItem var1) {
      if (var1.isFood()) {
         Food var2 = (Food)var1;
         if (var2.canAge() && this.canAge()) {
            float var3 = var2.getAge() / (float)var2.getOffAgeMax();
            if (var3 > (float)this.getOffAgeMax() * var3) {
               this.inheritFoodAgeFrom(var2);
            }
         }

      }
   }

   public boolean hasAnimalParts() {
      return this.getModData().rawget("parts") != null ? (Boolean)this.getModData().rawget("parts") : false;
   }

   public boolean isAnimalSkeleton() {
      return "true".equals(this.getModData().rawget("skeleton"));
   }

   public boolean canAge() {
      return this.getOffAgeMax() != 1000000000;
   }

   public boolean isFood() {
      return true;
   }
}
