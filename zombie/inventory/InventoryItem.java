package zombie.inventory;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.SandboxOptions;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.audio.BaseSoundEmitter;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characterTextures.BloodClothingType;
import zombie.characters.BaseCharacterSoundEmitter;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.SurvivorDesc;
import zombie.characters.UnderwearDefinition;
import zombie.characters.ZombiesZoneDefinition;
import zombie.characters.animals.AnimalDefinitions;
import zombie.characters.animals.AnimalTracks;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.animals.datas.AnimalBreed;
import zombie.characters.skills.PerkFactory;
import zombie.core.Color;
import zombie.core.Colors;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.model.WorldItemAtlas;
import zombie.core.skinnedmodel.population.ClothingItem;
import zombie.core.skinnedmodel.population.Outfit;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.stash.StashSystem;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.core.utils.Bits;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.entity.ComponentType;
import zombie.entity.GameEntity;
import zombie.entity.GameEntityType;
import zombie.entity.components.attributes.Attribute;
import zombie.entity.components.fluids.Fluid;
import zombie.entity.components.fluids.FluidContainer;
import zombie.entity.components.fluids.FluidType;
import zombie.inventory.types.AnimalInventoryItem;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.Drainable;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.Food;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.InventoryContainer;
import zombie.inventory.types.Key;
import zombie.inventory.types.WeaponType;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoFireManager;
import zombie.iso.objects.IsoFireplace;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.iso.objects.RainManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.radio.ZomboidRadio;
import zombie.radio.media.MediaData;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemReplacement;
import zombie.ui.ObjectTooltip;
import zombie.ui.TextManager;
import zombie.ui.UIFont;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.util.io.BitHeader;
import zombie.util.io.BitHeaderRead;
import zombie.util.io.BitHeaderWrite;
import zombie.util.list.PZArrayUtil;
import zombie.vehicles.VehiclePart;
import zombie.world.ItemInfo;
import zombie.world.WorldDictionary;

public class InventoryItem extends GameEntity {
   protected IsoGameCharacter previousOwner = null;
   protected Item ScriptItem = null;
   protected ItemType cat;
   protected ItemContainer container;
   protected int containerX;
   protected int containerY;
   protected String name;
   protected String replaceOnUse;
   protected String replaceOnUseFullType;
   protected int ConditionMax;
   protected ItemContainer rightClickContainer;
   protected Texture texture;
   protected Texture texturerotten;
   protected Texture textureCooked;
   protected Texture textureBurnt;
   protected String type;
   protected String fullType;
   protected int uses;
   protected float Age;
   protected float LastAged;
   protected boolean IsCookable;
   protected float CookingTime;
   protected float MinutesToCook;
   protected float MinutesToBurn;
   public boolean Cooked;
   protected boolean Burnt;
   protected int OffAge;
   protected int OffAgeMax;
   protected float Weight;
   protected float ActualWeight;
   protected String WorldTexture;
   protected String Description;
   protected int Condition;
   protected String OffString;
   protected String FreshString;
   protected String StaleString;
   protected String CookedString;
   protected String ToastedString;
   protected String GrilledString;
   protected String UnCookedString;
   protected String FrozenString;
   protected String BurntString;
   protected String EmptyString;
   private String brokenString;
   private String bluntString;
   private String dullString;
   private final String wornString;
   private final String bloodyString;
   protected String module;
   protected float boredomChange;
   protected float unhappyChange;
   protected float stressChange;
   protected ArrayList<IsoObject> Taken;
   protected IsoDirections placeDir;
   protected IsoDirections newPlaceDir;
   private KahluaTable table;
   public String ReplaceOnUseOn;
   public Color col;
   public boolean CanStack;
   private boolean activated;
   private boolean isTorchCone;
   private int lightDistance;
   private int Count;
   public float fatigueChange;
   public IsoWorldInventoryObject worldItem;
   public IsoDeadBody deadBodyObject;
   private String customMenuOption;
   private String tooltip;
   private String displayCategory;
   private int haveBeenRepaired;
   private boolean broken;
   private String originalName;
   public int id;
   public boolean RequiresEquippedBothHands;
   public ByteBuffer byteData;
   public ArrayList<String> extraItems;
   private boolean customName;
   private String breakSound;
   protected boolean alcoholic;
   private float alcoholPower;
   private float bandagePower;
   private float ReduceInfectionPower;
   private boolean customWeight;
   private boolean customColor;
   private int keyId;
   private boolean remoteController;
   private boolean canBeRemote;
   private int remoteControlID;
   private int remoteRange;
   private float colorRed;
   private float colorGreen;
   private float colorBlue;
   private String countDownSound;
   private String explosionSound;
   private IsoGameCharacter equipParent;
   private String evolvedRecipeName;
   private float metalValue;
   private float itemHeat;
   private float meltingTime;
   private String worker;
   private boolean isWet;
   private float wetCooldown;
   private String itemWhenDry;
   private boolean favorite;
   protected ArrayList<String> requireInHandOrInventory;
   private String map;
   private String stashMap;
   private boolean zombieInfected;
   private float itemCapacity;
   private int maxCapacity;
   private float brakeForce;
   private float durability;
   private int chanceToSpawnDamaged;
   private float conditionLowerNormal;
   private float conditionLowerOffroad;
   private float wheelFriction;
   private float suspensionDamping;
   private float suspensionCompression;
   private float engineLoudness;
   protected ItemVisual visual;
   protected String staticModel;
   private ArrayList<String> iconsForTexture;
   private ArrayList<BloodClothingType> bloodClothingType;
   private int stashChance;
   private String ammoType;
   private int maxAmmo;
   private int currentAmmoCount;
   private String gunType;
   private String attachmentType;
   private ArrayList<String> attachmentsProvided;
   private int attachedSlot;
   private String attachedSlotType;
   private String attachmentReplacement;
   private String attachedToModel;
   private String m_alternateModelName;
   private short registry_id;
   public int worldZRotation;
   public float worldScale;
   private short recordedMediaIndex;
   private byte mediaType;
   private boolean isInitialised;
   public WorldItemAtlas.ItemTexture atlasTexture;
   protected Texture textureColorMask;
   protected Texture textureFluidMask;
   private AnimalTracks animalTracks;
   private ArrayList<String> staticModelsByIndex;
   private ArrayList<String> worldStaticModelsByIndex;
   private int modelIndex;
   private final int maxTextLength;
   private IsoPlayer equippedAndActivatedPlayer;
   private long equippedAndActivatedSound;
   private boolean isCraftingConsumed;
   public float jobDelta;
   public String jobType;
   static ByteBuffer tempBuffer = ByteBuffer.allocate(20000);
   public String mainCategory;
   private boolean canBeActivated;
   private float lightStrength;
   public String CloseKillMove;
   private boolean beingFilled;

   public int getSaveType() {
      throw new RuntimeException("InventoryItem.getSaveType() not implemented for " + this.getClass().getName());
   }

   public IsoWorldInventoryObject getWorldItem() {
      return this.worldItem;
   }

   public void setEquipParent(IsoGameCharacter var1) {
      this.setEquipParent(var1, true);
   }

   public void setEquipParent(IsoGameCharacter var1, boolean var2) {
      this.equipParent = var1;
      if (this.equipParent == null) {
         this.onUnEquip();
      } else {
         this.onEquip(var2);
      }

   }

   public IsoGameCharacter getEquipParent() {
      return this.equipParent == null || this.equipParent.getPrimaryHandItem() != this && this.equipParent.getSecondaryHandItem() != this ? null : this.equipParent;
   }

   public String getBringToBearSound() {
      return this.getScriptItem().getBringToBearSound();
   }

   public String getEquipSound() {
      return this.getScriptItem().getEquipSound();
   }

   public String getUnequipSound() {
      return this.getScriptItem().getUnequipSound();
   }

   public String getDropSound() {
      if (StringUtils.equalsIgnoreCase(this.getType(), "CorpseAnimal")) {
         IsoDeadBody var1 = this.loadCorpseFromByteData((IsoGridSquare)null);
         if (var1 != null && var1.isAnimal()) {
            AnimalDefinitions var2 = AnimalDefinitions.getDef(var1.getAnimalType());
            if (var2 == null) {
               return this.getScriptItem().getDropSound();
            } else {
               AnimalBreed var3 = var2.getBreedByName(var1.getBreed());
               if (var3 == null) {
                  return this.getScriptItem().getDropSound();
               } else {
                  AnimalBreed.Sound var4 = var3.getSound("put_down_corpse");
                  return var4 == null ? this.getScriptItem().getDropSound() : var4.soundName;
               }
            }
         } else {
            return this.getScriptItem().getDropSound();
         }
      } else {
         return this.getScriptItem().getDropSound();
      }
   }

   public void setWorldItem(IsoWorldInventoryObject var1) {
      this.worldItem = var1;
   }

   public void setJobDelta(float var1) {
      this.jobDelta = var1;
   }

   public float getJobDelta() {
      return this.jobDelta;
   }

   public void setJobType(String var1) {
      this.jobType = var1;
   }

   public String getJobType() {
      return this.jobType;
   }

   public boolean hasModData() {
      return this.table != null && !this.table.isEmpty();
   }

   public KahluaTable getModData() {
      if (this.table == null) {
         this.table = LuaManager.platform.newTable();
      }

      return this.table;
   }

   public void storeInByteData(IsoObject var1) {
      tempBuffer.clear();

      try {
         var1.save(tempBuffer, false);
      } catch (IOException var3) {
         var3.printStackTrace();
      }

      tempBuffer.flip();
      if (this.byteData == null || this.byteData.capacity() < tempBuffer.limit() - 2 + 8) {
         this.byteData = ByteBuffer.allocate(tempBuffer.limit() - 2 + 8);
      }

      tempBuffer.get();
      tempBuffer.get();
      this.byteData.clear();
      this.byteData.put((byte)87);
      this.byteData.put((byte)86);
      this.byteData.put((byte)69);
      this.byteData.put((byte)82);
      this.byteData.putInt(219);
      this.byteData.put(tempBuffer);
      this.byteData.flip();
   }

   public ByteBuffer getByteData() {
      return this.byteData;
   }

   public IsoDeadBody loadCorpseFromByteData(IsoGridSquare var1) {
      if (this.getByteData() == null) {
         return this.createAndStoreDefaultDeadBody(var1);
      } else {
         Object var4;
         try {
            IsoDeadBody var2 = this.tryLoadCorpseFromByteData(var1);
            return var2;
         } catch (IOException var10) {
            ExceptionLogger.logException(var10);

            try {
               IsoDeadBody var3 = this.createDefaultDeadBody(var1);
               return var3;
            } catch (Throwable var9) {
               ExceptionLogger.logException(var9);
               var4 = null;
            }
         } finally {
            this.getByteData().rewind();
         }

         return (IsoDeadBody)var4;
      }
   }

   private IsoDeadBody tryLoadCorpseFromByteData(IsoGridSquare var1) throws IOException {
      this.getByteData().rewind();
      byte var2 = this.getByteData().get();
      byte var3 = this.getByteData().get();
      byte var4 = this.getByteData().get();
      byte var5 = this.getByteData().get();
      if (var2 == 87 && var3 == 86 && var4 == 69 && var5 == 82) {
         int var6 = this.getByteData().getInt();
         IsoDeadBody var7 = new IsoDeadBody(IsoWorld.instance.CurrentCell);
         var7.load(this.getByteData(), var6);
         if ("CorpseAnimal".equalsIgnoreCase(this.getType())) {
            Object var8 = this.hasModData() ? this.getModData().rawget("skeleton") : null;
            if (var8 != null && "true".equalsIgnoreCase(var8.toString())) {
               var7.getModData().rawset("skeleton", "true");
            }
         }

         if (var1 != null) {
            var7.setSquare(var1);
            var7.setCurrent(var1);
         }

         return var7;
      } else {
         throw new IOException("expected 'WVER' signature in byteData");
      }
   }

   private IsoDeadBody createDefaultDeadBody(IsoGridSquare var1) throws Throwable {
      if (!"CorpseFemale".equalsIgnoreCase(this.getType()) && !"CorpseMale".equalsIgnoreCase(this.getType())) {
         if ("CorpseAnimal".equalsIgnoreCase(this.getType())) {
            AnimalDefinitions var6 = (AnimalDefinitions)PZArrayUtil.pickRandom((List)AnimalDefinitions.getAnimalDefsArray());
            if (var6 == null) {
               return null;
            } else {
               AnimalBreed var7 = var6.getRandomBreed();
               if (var7 == null) {
                  return null;
               } else {
                  IsoAnimal var8 = new IsoAnimal(IsoWorld.instance.CurrentCell, 0, 0, 0, var6.getAnimalType(), var7.getName());
                  var8.setDir(IsoDirections.fromIndex(Rand.Next(8)));
                  var8.getForwardDirection().set(var8.getDir().ToVector());
                  var8.setHealth(0.0F);
                  if (var1 != null) {
                     var8.setSquare(var1);
                     var8.setCurrent(var1);
                  }

                  IsoDeadBody var5 = new IsoDeadBody(var8, true, var1 != null);
                  this.copyModData(var5.getModData());
                  this.setIcon(Texture.getSharedTexture(var5.invIcon));
                  if (var5.isAnimalSkeleton()) {
                     this.setName(Translator.getText("IGUI_Item_AnimalSkeleton", var5.customName));
                  } else {
                     this.setName(Translator.getText("IGUI_Item_AnimalCorpse", var5.customName));
                  }

                  this.setCustomName(true);
                  this.setActualWeight(var5.weight);
                  this.setWeight(var5.weight);
                  this.setCustomWeight(true);
                  return var5;
               }
            }
         } else {
            return null;
         }
      } else {
         IsoZombie var2 = new IsoZombie(IsoWorld.instance.CurrentCell);
         var2.setDir(IsoDirections.fromIndex(Rand.Next(8)));
         var2.getForwardDirection().set(var2.dir.ToVector());
         var2.setFakeDead(false);
         var2.setHealth(0.0F);
         var2.upKillCount = false;
         IsoDeadBody var3;
         if (var1 != null) {
            var2.dressInRandomOutfit();
         } else if (!var2.isSkeleton()) {
            var3 = null;
            Outfit var4 = ZombiesZoneDefinition.getRandomDefaultOutfit(var2.isFemale(), var3);
            UnderwearDefinition.addRandomUnderwear(var2);
            var2.dressInPersistentOutfit(var4.m_Name);
         }

         var2.DoZombieInventory();
         if (var1 != null) {
            var2.setSquare(var1);
            var2.setCurrent(var1);
         }

         var3 = new IsoDeadBody(var2, true, var1 != null);
         return var3;
      }
   }

   public IsoDeadBody createAndStoreDefaultDeadBody(IsoGridSquare var1) {
      try {
         IsoDeadBody var2 = this.createDefaultDeadBody(var1);
         if (var2 != null) {
            this.storeInByteData(var2);
         }

         return var2;
      } catch (Throwable var3) {
         ExceptionLogger.logException(var3);
         return null;
      }
   }

   public boolean isRequiresEquippedBothHands() {
      return this.RequiresEquippedBothHands;
   }

   public float getA() {
      return this.col.a;
   }

   public float getR() {
      return this.col.r;
   }

   public float getG() {
      return this.col.g;
   }

   public float getB() {
      return this.col.b;
   }

   public InventoryItem(String var1, String var2, String var3, String var4) {
      this.cat = ItemType.None;
      this.containerX = 0;
      this.containerY = 0;
      this.replaceOnUse = null;
      this.replaceOnUseFullType = null;
      this.ConditionMax = 10;
      this.rightClickContainer = null;
      this.uses = 1;
      this.Age = 0.0F;
      this.LastAged = -1.0F;
      this.IsCookable = false;
      this.CookingTime = 0.0F;
      this.MinutesToCook = 60.0F;
      this.MinutesToBurn = 120.0F;
      this.Cooked = false;
      this.Burnt = false;
      this.OffAge = 1000000000;
      this.OffAgeMax = 1000000000;
      this.Weight = 1.0F;
      this.ActualWeight = 1.0F;
      this.Condition = 10;
      this.OffString = Translator.getText("Tooltip_food_Rotten");
      this.FreshString = Translator.getText("Tooltip_food_Fresh");
      this.StaleString = Translator.getText("Tooltip_food_Stale");
      this.CookedString = Translator.getText("Tooltip_food_Cooked");
      this.ToastedString = Translator.getText("Tooltip_food_Toasted");
      this.GrilledString = Translator.getText("Tooltip_food_Grilled");
      this.UnCookedString = Translator.getText("Tooltip_food_Uncooked");
      this.FrozenString = Translator.getText("Tooltip_food_Frozen");
      this.BurntString = Translator.getText("Tooltip_food_Burnt");
      this.EmptyString = Translator.getText("ContextMenu_Empty");
      this.brokenString = Translator.getText("Tooltip_broken");
      this.bluntString = Translator.getText("Tooltip_blunt");
      this.dullString = Translator.getText("Tooltip_dull");
      this.wornString = Translator.getText("IGUI_ClothingName_Worn");
      this.bloodyString = Translator.getText("IGUI_ClothingName_Bloody");
      this.module = "Base";
      this.boredomChange = 0.0F;
      this.unhappyChange = 0.0F;
      this.stressChange = 0.0F;
      this.Taken = new ArrayList();
      this.placeDir = IsoDirections.Max;
      this.newPlaceDir = IsoDirections.Max;
      this.table = null;
      this.ReplaceOnUseOn = null;
      this.col = Color.white;
      this.CanStack = false;
      this.activated = false;
      this.isTorchCone = false;
      this.lightDistance = 0;
      this.Count = 1;
      this.fatigueChange = 0.0F;
      this.worldItem = null;
      this.deadBodyObject = null;
      this.customMenuOption = null;
      this.tooltip = null;
      this.displayCategory = null;
      this.haveBeenRepaired = 0;
      this.broken = false;
      this.originalName = null;
      this.id = 0;
      this.extraItems = null;
      this.customName = false;
      this.breakSound = null;
      this.alcoholic = false;
      this.alcoholPower = 0.0F;
      this.bandagePower = 0.0F;
      this.ReduceInfectionPower = 0.0F;
      this.customWeight = false;
      this.customColor = false;
      this.keyId = -1;
      this.remoteController = false;
      this.canBeRemote = false;
      this.remoteControlID = -1;
      this.remoteRange = 0;
      this.colorRed = 1.0F;
      this.colorGreen = 1.0F;
      this.colorBlue = 1.0F;
      this.countDownSound = null;
      this.explosionSound = null;
      this.equipParent = null;
      this.evolvedRecipeName = null;
      this.metalValue = 0.0F;
      this.itemHeat = 1.0F;
      this.meltingTime = 0.0F;
      this.isWet = false;
      this.wetCooldown = -1.0F;
      this.itemWhenDry = null;
      this.favorite = false;
      this.requireInHandOrInventory = null;
      this.map = null;
      this.stashMap = null;
      this.zombieInfected = false;
      this.itemCapacity = -1.0F;
      this.maxCapacity = -1;
      this.brakeForce = 0.0F;
      this.durability = 0.0F;
      this.chanceToSpawnDamaged = 0;
      this.conditionLowerNormal = 0.0F;
      this.conditionLowerOffroad = 0.0F;
      this.wheelFriction = 0.0F;
      this.suspensionDamping = 0.0F;
      this.suspensionCompression = 0.0F;
      this.engineLoudness = 0.0F;
      this.visual = null;
      this.staticModel = null;
      this.iconsForTexture = null;
      this.bloodClothingType = new ArrayList();
      this.stashChance = 80;
      this.ammoType = null;
      this.maxAmmo = 0;
      this.currentAmmoCount = 0;
      this.gunType = null;
      this.attachmentType = null;
      this.attachmentsProvided = null;
      this.attachedSlot = -1;
      this.attachedSlotType = null;
      this.attachmentReplacement = null;
      this.attachedToModel = null;
      this.m_alternateModelName = null;
      this.registry_id = -1;
      this.worldZRotation = -1;
      this.worldScale = 1.0F;
      this.recordedMediaIndex = -1;
      this.mediaType = -1;
      this.isInitialised = false;
      this.atlasTexture = null;
      this.staticModelsByIndex = null;
      this.worldStaticModelsByIndex = null;
      this.modelIndex = -1;
      this.maxTextLength = 256;
      this.equippedAndActivatedPlayer = null;
      this.equippedAndActivatedSound = 0L;
      this.isCraftingConsumed = false;
      this.jobDelta = 0.0F;
      this.jobType = null;
      this.mainCategory = null;
      this.CloseKillMove = null;
      this.beingFilled = false;
      this.col = Color.white;
      this.texture = Texture.trygetTexture(var4);
      if (this.texture == null) {
         this.texture = Texture.getSharedTexture("media/inventory/Question_On.png");
      }

      this.module = var1;
      this.name = var2;
      this.originalName = var2;
      this.type = var3;
      this.fullType = var1 + "." + var3;
      this.WorldTexture = var4.replace("Item_", "media/inventory/world/WItem_");
      this.WorldTexture = this.WorldTexture + ".png";
   }

   public InventoryItem(String var1, String var2, String var3, Item var4) {
      this.cat = ItemType.None;
      this.containerX = 0;
      this.containerY = 0;
      this.replaceOnUse = null;
      this.replaceOnUseFullType = null;
      this.ConditionMax = 10;
      this.rightClickContainer = null;
      this.uses = 1;
      this.Age = 0.0F;
      this.LastAged = -1.0F;
      this.IsCookable = false;
      this.CookingTime = 0.0F;
      this.MinutesToCook = 60.0F;
      this.MinutesToBurn = 120.0F;
      this.Cooked = false;
      this.Burnt = false;
      this.OffAge = 1000000000;
      this.OffAgeMax = 1000000000;
      this.Weight = 1.0F;
      this.ActualWeight = 1.0F;
      this.Condition = 10;
      this.OffString = Translator.getText("Tooltip_food_Rotten");
      this.FreshString = Translator.getText("Tooltip_food_Fresh");
      this.StaleString = Translator.getText("Tooltip_food_Stale");
      this.CookedString = Translator.getText("Tooltip_food_Cooked");
      this.ToastedString = Translator.getText("Tooltip_food_Toasted");
      this.GrilledString = Translator.getText("Tooltip_food_Grilled");
      this.UnCookedString = Translator.getText("Tooltip_food_Uncooked");
      this.FrozenString = Translator.getText("Tooltip_food_Frozen");
      this.BurntString = Translator.getText("Tooltip_food_Burnt");
      this.EmptyString = Translator.getText("ContextMenu_Empty");
      this.brokenString = Translator.getText("Tooltip_broken");
      this.bluntString = Translator.getText("Tooltip_blunt");
      this.dullString = Translator.getText("Tooltip_dull");
      this.wornString = Translator.getText("IGUI_ClothingName_Worn");
      this.bloodyString = Translator.getText("IGUI_ClothingName_Bloody");
      this.module = "Base";
      this.boredomChange = 0.0F;
      this.unhappyChange = 0.0F;
      this.stressChange = 0.0F;
      this.Taken = new ArrayList();
      this.placeDir = IsoDirections.Max;
      this.newPlaceDir = IsoDirections.Max;
      this.table = null;
      this.ReplaceOnUseOn = null;
      this.col = Color.white;
      this.CanStack = false;
      this.activated = false;
      this.isTorchCone = false;
      this.lightDistance = 0;
      this.Count = 1;
      this.fatigueChange = 0.0F;
      this.worldItem = null;
      this.deadBodyObject = null;
      this.customMenuOption = null;
      this.tooltip = null;
      this.displayCategory = null;
      this.haveBeenRepaired = 0;
      this.broken = false;
      this.originalName = null;
      this.id = 0;
      this.extraItems = null;
      this.customName = false;
      this.breakSound = null;
      this.alcoholic = false;
      this.alcoholPower = 0.0F;
      this.bandagePower = 0.0F;
      this.ReduceInfectionPower = 0.0F;
      this.customWeight = false;
      this.customColor = false;
      this.keyId = -1;
      this.remoteController = false;
      this.canBeRemote = false;
      this.remoteControlID = -1;
      this.remoteRange = 0;
      this.colorRed = 1.0F;
      this.colorGreen = 1.0F;
      this.colorBlue = 1.0F;
      this.countDownSound = null;
      this.explosionSound = null;
      this.equipParent = null;
      this.evolvedRecipeName = null;
      this.metalValue = 0.0F;
      this.itemHeat = 1.0F;
      this.meltingTime = 0.0F;
      this.isWet = false;
      this.wetCooldown = -1.0F;
      this.itemWhenDry = null;
      this.favorite = false;
      this.requireInHandOrInventory = null;
      this.map = null;
      this.stashMap = null;
      this.zombieInfected = false;
      this.itemCapacity = -1.0F;
      this.maxCapacity = -1;
      this.brakeForce = 0.0F;
      this.durability = 0.0F;
      this.chanceToSpawnDamaged = 0;
      this.conditionLowerNormal = 0.0F;
      this.conditionLowerOffroad = 0.0F;
      this.wheelFriction = 0.0F;
      this.suspensionDamping = 0.0F;
      this.suspensionCompression = 0.0F;
      this.engineLoudness = 0.0F;
      this.visual = null;
      this.staticModel = null;
      this.iconsForTexture = null;
      this.bloodClothingType = new ArrayList();
      this.stashChance = 80;
      this.ammoType = null;
      this.maxAmmo = 0;
      this.currentAmmoCount = 0;
      this.gunType = null;
      this.attachmentType = null;
      this.attachmentsProvided = null;
      this.attachedSlot = -1;
      this.attachedSlotType = null;
      this.attachmentReplacement = null;
      this.attachedToModel = null;
      this.m_alternateModelName = null;
      this.registry_id = -1;
      this.worldZRotation = -1;
      this.worldScale = 1.0F;
      this.recordedMediaIndex = -1;
      this.mediaType = -1;
      this.isInitialised = false;
      this.atlasTexture = null;
      this.staticModelsByIndex = null;
      this.worldStaticModelsByIndex = null;
      this.modelIndex = -1;
      this.maxTextLength = 256;
      this.equippedAndActivatedPlayer = null;
      this.equippedAndActivatedSound = 0L;
      this.isCraftingConsumed = false;
      this.jobDelta = 0.0F;
      this.jobType = null;
      this.mainCategory = null;
      this.CloseKillMove = null;
      this.beingFilled = false;
      this.col = Color.white;
      this.texture = var4.NormalTexture;
      this.module = var1;
      this.name = var2;
      this.originalName = var2;
      this.type = var3;
      this.fullType = var1 + "." + var3;
      this.WorldTexture = var4.WorldTextureName;
   }

   public String getType() {
      return this.type;
   }

   public Texture getTex() {
      return this.texture;
   }

   public String getCategory() {
      return this.mainCategory != null ? this.mainCategory : "Item";
   }

   public boolean UseForCrafting(int var1) {
      return false;
   }

   public boolean IsRotten() {
      return this.Age > (float)this.OffAge;
   }

   public float HowRotten() {
      if (this.OffAgeMax - this.OffAge == 0) {
         return this.Age > (float)this.OffAge ? 1.0F : 0.0F;
      } else {
         return (this.Age - (float)this.OffAge) / (float)(this.OffAgeMax - this.OffAge);
      }
   }

   public boolean CanStack(InventoryItem var1) {
      return false;
   }

   public boolean ModDataMatches(InventoryItem var1) {
      KahluaTable var2 = var1.getModData();
      KahluaTable var3 = var1.getModData();
      if (var2 == null && var3 == null) {
         return true;
      } else if (var2 == null) {
         return false;
      } else if (var3 == null) {
         return false;
      } else if (var2.len() != var3.len()) {
         return false;
      } else {
         KahluaTableIterator var4 = var2.iterator();

         Object var5;
         Object var6;
         do {
            if (!var4.advance()) {
               return true;
            }

            var5 = var3.rawget(var4.getKey());
            var6 = var4.getValue();
         } while(var5.equals(var6));

         return false;
      }
   }

   public void DoTooltip(ObjectTooltip var1) {
      this.DoTooltipEmbedded(var1, (ObjectTooltip.Layout)null, 0);
   }

   public void DoTooltipEmbedded(ObjectTooltip var1, ObjectTooltip.Layout var2, int var3) {
      var1.render();
      UIFont var4 = var1.getFont();
      int var5 = var1.getLineSpacing();
      int var6 = var1.padTop + var3;
      String var7 = this.getName();
      var1.DrawText(var4, var7, (double)var1.padLeft, (double)var6, 1.0, 1.0, 0.800000011920929, 1.0);
      var1.adjustWidth(var1.padLeft, var7);
      var6 += var5 + 5;
      int var8;
      int var9;
      int var10;
      InventoryItem var11;
      if (this.extraItems != null) {
         var1.DrawText(var4, Translator.getText("Tooltip_item_Contains"), (double)var1.padLeft, (double)var6, 1.0, 1.0, 0.800000011920929, 1.0);
         var8 = var1.padLeft + TextManager.instance.MeasureStringX(var4, Translator.getText("Tooltip_item_Contains")) + 4;
         var9 = (var5 - 10) / 2;

         for(var10 = 0; var10 < this.extraItems.size(); ++var10) {
            var11 = InventoryItemFactory.CreateItem((String)this.extraItems.get(var10));
            if (!this.IsCookable && var11.IsCookable) {
               var11.setCooked(true);
            }

            if (this.isCooked() && var11.IsCookable) {
               var11.setCooked(true);
            }

            var1.DrawTextureScaled(var11.getTex(), (double)var8, (double)(var6 + var9), 10.0, 10.0, 1.0);
            var8 += 11;
         }

         var6 = var6 + var5 + 5;
      }

      if (this instanceof Food && ((Food)this).spices != null) {
         var1.DrawText(var4, Translator.getText("Tooltip_item_Spices"), (double)var1.padLeft, (double)var6, 1.0, 1.0, 0.800000011920929, 1.0);
         var8 = var1.padLeft + TextManager.instance.MeasureStringX(var4, Translator.getText("Tooltip_item_Spices")) + 4;
         var9 = (var5 - 10) / 2;

         for(var10 = 0; var10 < ((Food)this).spices.size(); ++var10) {
            var11 = InventoryItemFactory.CreateItem((String)((Food)this).spices.get(var10));
            var1.DrawTextureScaled(var11.getTex(), (double)var8, (double)(var6 + var9), 10.0, 10.0, 1.0);
            var8 += 11;
         }

         var6 = var6 + var5 + 5;
      }

      ObjectTooltip.Layout var18;
      if (var2 != null) {
         var18 = var2;
         var2.offsetY = var6;
      } else {
         var18 = var1.beginLayout();
         var18.setMinLabelWidth(80);
      }

      ObjectTooltip.LayoutItem var19;
      if (SandboxOptions.instance.isUnstableScriptNameSpam()) {
         var19 = var18.addItem();
         var19.setLabel(Translator.getText("(DEBUG) Script Name") + ":", 1.0F, 0.4F, 0.7F, 1.0F);
         var19.setValue(this.getFullType(), 1.0F, 1.0F, 0.8F, 1.0F);
      }

      var19 = var18.addItem();
      var19.setLabel(Translator.getText("Tooltip_item_Weight") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
      boolean var20 = this.isEquipped();
      String var10001;
      float var21;
      if (!(this instanceof HandWeapon) && !(this instanceof Clothing) && !(this instanceof DrainableComboItem) && !this.getFullType().contains("Walkie")) {
         if (this instanceof AnimalInventoryItem) {
            var19.setValueRightNoPlus(this.getWeight());
         } else {
            var21 = this.getUnequippedWeight();
            if (var21 > 0.0F && var21 < 0.01F) {
               var21 = 0.01F;
            }

            if (this.getAttachedSlot() > -1) {
               var10001 = this.getCleanString(this.getHotbarEquippedWeight());
               var19.setValue(var10001 + "    (" + this.getCleanString(this.getUnequippedWeight()) + " " + Translator.getText("Tooltip_item_Unattached") + ")", 1.0F, 1.0F, 1.0F, 1.0F);
            } else {
               var19.setValueRightNoPlus(var21);
            }
         }
      } else if (var20) {
         var10001 = this.getCleanString(this.getEquippedWeight());
         var19.setValue(var10001 + "    (" + this.getCleanString(this.getUnequippedWeight()) + " " + Translator.getText("Tooltip_item_Unequipped") + ")", 1.0F, 1.0F, 1.0F, 1.0F);
      } else if (this.getAttachedSlot() > -1) {
         var10001 = this.getCleanString(this.getHotbarEquippedWeight());
         var19.setValue(var10001 + "    (" + this.getCleanString(this.getUnequippedWeight()) + " " + Translator.getText("Tooltip_item_Unattached") + ")", 1.0F, 1.0F, 1.0F, 1.0F);
      } else {
         var10001 = this.getCleanString(this.getUnequippedWeight());
         var19.setValue(var10001 + "    (" + this.getCleanString(this.getEquippedWeight()) + " " + Translator.getText("Tooltip_item_Equipped") + ")", 1.0F, 1.0F, 1.0F, 1.0F);
      }

      if (var1.getWeightOfStack() > 0.0F) {
         var19 = var18.addItem();
         var19.setLabel(Translator.getText("Tooltip_item_StackWeight") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
         var21 = var1.getWeightOfStack();
         if (var21 > 0.0F && var21 < 0.01F) {
            var21 = 0.01F;
         }

         var19.setValueRightNoPlus(var21);
      }

      if (this.getMaxAmmo() > 0 && !(this instanceof HandWeapon)) {
         var19 = var18.addItem();
         var19.setLabel(Translator.getText("Tooltip_weapon_AmmoCount") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
         var19.setValue(this.getCurrentAmmoCount() + " / " + this.getMaxAmmo(), 1.0F, 1.0F, 1.0F, 1.0F);
      }

      String var25;
      if (!(this instanceof HandWeapon) && this.getAmmoType() != null) {
         var19 = var18.addItem();
         var19.setLabel(Translator.getText("ContextMenu_AmmoType") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
         var25 = InventoryItemFactory.CreateItem(this.getAmmoType()).getDisplayName();
         var19.setValue(Translator.getText(var25), 1.0F, 1.0F, 1.0F, 1.0F);
      }

      if (this.gunType != null) {
         Item var26 = ScriptManager.instance.FindItem(this.getGunType());
         if (var26 == null) {
            ScriptManager var10000 = ScriptManager.instance;
            var10001 = this.getModule();
            var26 = var10000.FindItem(var10001 + "." + this.ammoType);
         }

         if (var26 != null) {
            var19 = var18.addItem();
            var19.setLabel(Translator.getText("ContextMenu_GunType") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
            var19.setValue(var26.getDisplayName(), 1.0F, 1.0F, 1.0F, 1.0F);
         }
      }

      if (Core.bDebug && DebugOptions.instance.TooltipInfo.getValue()) {
         var19 = var18.addItem();
         var19.setLabel("getActualWeight()", 1.0F, 1.0F, 0.8F, 1.0F);
         var19.setValueRightNoPlus(this.getActualWeight());
         var19 = var18.addItem();
         var19.setLabel("getWeight()", 1.0F, 1.0F, 0.8F, 1.0F);
         var19.setValueRightNoPlus(this.getWeight());
         var19 = var18.addItem();
         var19.setLabel("getEquippedWeight()", 1.0F, 1.0F, 0.8F, 1.0F);
         var19.setValueRightNoPlus(this.getEquippedWeight());
         var19 = var18.addItem();
         var19.setLabel("getUnequippedWeight()", 1.0F, 1.0F, 0.8F, 1.0F);
         var19.setValueRightNoPlus(this.getUnequippedWeight());
         var19 = var18.addItem();
         var19.setLabel("getContentsWeight()", 1.0F, 1.0F, 0.8F, 1.0F);
         var19.setValueRightNoPlus(this.getContentsWeight());
         if (this instanceof Key || "Doorknob".equals(this.type)) {
            var19 = var18.addItem();
            var19.setLabel("DBG: keyId", 1.0F, 1.0F, 0.8F, 1.0F);
            var19.setValueRightNoPlus(this.getKeyId());
         }

         var19 = var18.addItem();
         var19.setLabel("ID", 1.0F, 1.0F, 0.8F, 1.0F);
         var19.setValueRightNoPlus(this.id);
         var19 = var18.addItem();
         var19.setLabel("DictionaryID", 1.0F, 1.0F, 0.8F, 1.0F);
         var19.setValueRightNoPlus(this.getRegistry_id());
         ClothingItem var28 = this.getClothingItem();
         if (var28 != null) {
            var19 = var18.addItem();
            var19.setLabel("ClothingItem", 1.0F, 1.0F, 1.0F, 1.0F);
            var19.setValue(this.getClothingItem().m_Name, 1.0F, 1.0F, 1.0F, 1.0F);
         }
      }

      if (Core.bDebug && DebugOptions.instance.TooltipInfo.getValue() || LuaManager.GlobalObject.isAdmin()) {
         var19 = var18.addItem();
         var25 = "Loot Category";
         String var12 = Translator.getText("Sandbox_" + this.getLootType() + "LootNew");
         var19.setLabel(var25 + ":", 1.0F, 1.0F, 0.8F, 1.0F);
         var19.setValue(var12, 1.0F, 1.0F, 1.0F, 1.0F);
      }

      if (this.getFatigueChange() != 0.0F) {
         var19 = var18.addItem();
         var19.setLabel(Translator.getText("Tooltip_item_Fatigue") + ": ", 1.0F, 1.0F, 0.8F, 1.0F);
         var19.setValueRight((int)(this.getFatigueChange() * 100.0F), false);
      }

      ColorInfo var22;
      if (this instanceof DrainableComboItem) {
         var19 = var18.addItem();
         var19.setLabel(Translator.getText("IGUI_invpanel_Remaining") + ": ", 1.0F, 1.0F, 0.8F, 1.0F);
         var21 = this.getCurrentUsesFloat();
         var22 = new ColorInfo();
         Core.getInstance().getBadHighlitedColor().interp(Core.getInstance().getGoodHighlitedColor(), var21, var22);
         var19.setProgress(var21, var22.getR(), var22.getG(), var22.getB(), 1.0F);
      }

      if (this instanceof Food && ((Food)this).isTainted() && SandboxOptions.instance.EnableTaintedWaterText.getValue()) {
         var19 = var18.addItem();
         if (!this.hasMetal()) {
            var19.setLabel(Translator.getText("Tooltip_item_TaintedWater"), 1.0F, 0.5F, 0.5F, 1.0F);
         } else {
            var19.setLabel(Translator.getText("Tooltip_item_TaintedWater_Plastic"), 1.0F, 0.5F, 0.5F, 1.0F);
         }
      }

      this.DoTooltip(var1, var18);
      if (this.getRemoteControlID() != -1) {
         var19 = var18.addItem();
         var19.setLabel(Translator.getText("Tooltip_TrapControllerID"), 1.0F, 1.0F, 0.8F, 1.0F);
         var19.setValue(Integer.toString(this.getRemoteControlID()), 1.0F, 1.0F, 0.8F, 1.0F);
      }

      if (this.getHaveBeenRepaired() > 0) {
         var19 = var18.addItem();
         var19.setLabel(Translator.getText("Tooltip_weapon_Repaired") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
         if (this.hasTimesHeadRepaired()) {
            var19.setLabel(Translator.getText("Tooltip_handle_Repaired") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
         }

         var19.setValue(this.getHaveBeenRepaired() + "x", 1.0F, 1.0F, 1.0F, 1.0F);
      }

      if (this.hasTimesHeadRepaired() && this.getTimesHeadRepaired() > 0) {
         var19 = var18.addItem();
         var19.setLabel(Translator.getText("Tooltip_head_Repaired") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
         var19.setValue(this.getTimesHeadRepaired() + "x", 1.0F, 1.0F, 1.0F, 1.0F);
      }

      if (this.isEquippedNoSprint()) {
         var19 = var18.addItem();
         var19.setLabel(Translator.getText("Tooltip_CantSprintEquipped"), 1.0F, 0.1F, 0.1F, 1.0F);
      }

      if (this.isWet()) {
         var19 = var18.addItem();
         var19.setLabel(Translator.getText("Tooltip_Wetness") + ": ", 1.0F, 1.0F, 0.8F, 1.0F);
         var21 = this.getWetCooldown() / 10000.0F;
         var22 = new ColorInfo();
         Core.getInstance().getGoodHighlitedColor().interp(Core.getInstance().getBadHighlitedColor(), var21, var22);
         var19.setProgress(var21, var22.getR(), var22.getG(), var22.getB(), 1.0F);
      }

      if (this.getMaxCapacity() > 0) {
         var19 = var18.addItem();
         var19.setLabel(Translator.getText("Tooltip_container_Capacity") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
         var21 = (float)this.getMaxCapacity();
         if (this.isConditionAffectsCapacity()) {
            var21 = VehiclePart.getNumberByCondition((float)this.getMaxCapacity(), (float)this.getCondition(), 5.0F);
         }

         if (this.getItemCapacity() > -1.0F) {
            var19.setValue(this.getItemCapacity() + " / " + var21, 1.0F, 1.0F, 0.8F, 1.0F);
         } else {
            var19.setValue("0 / " + var21, 1.0F, 1.0F, 0.8F, 1.0F);
         }
      }

      float var13;
      ColorInfo var29;
      if (!(this instanceof HandWeapon) && !(this instanceof Clothing) && this.getConditionMax() > 0 && (this.getMechanicType() > 0 || this.hasTag("ShowCondition") || this.getConditionMax() > this.getCondition())) {
         var29 = new ColorInfo();
         float var23 = 1.0F;
         var13 = 1.0F;
         float var14 = 0.8F;
         float var15 = 1.0F;
         var19 = var18.addItem();
         String var16 = "Tooltip_weapon_Condition";
         if (this.hasHeadCondition()) {
            var16 = "Tooltip_weapon_HandleCondition";
         }

         var19.setLabel(Translator.getText(var16) + ":", var23, var13, var14, var15);
         float var17 = (float)this.getCondition() / (float)this.getConditionMax();
         Core.getInstance().getBadHighlitedColor().interp(Core.getInstance().getGoodHighlitedColor(), var17, var29);
         var19.setProgress(var17, var29.getR(), var29.getG(), var29.getB(), 1.0F);
      }

      if (this.isRecordedMedia()) {
         MediaData var30 = this.getMediaData();
         if (var30 != null) {
            if (var30.getTranslatedTitle() != null) {
               var19 = var18.addItem();
               var19.setLabel(Translator.getText("Tooltip_media_title") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
               var19.setValue(var30.getTranslatedTitle(), 1.0F, 1.0F, 1.0F, 1.0F);
               if (var30.getTranslatedSubTitle() != null) {
                  var19 = var18.addItem();
                  var19.setLabel("", 1.0F, 1.0F, 0.8F, 1.0F);
                  var19.setValue(var30.getTranslatedSubTitle(), 1.0F, 1.0F, 1.0F, 1.0F);
               }
            }

            if (var30.getTranslatedAuthor() != null) {
               var19 = var18.addItem();
               var19.setLabel(Translator.getText("Tooltip_media_author") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
               var19.setValue(var30.getTranslatedAuthor(), 1.0F, 1.0F, 1.0F, 1.0F);
            }
         }
      }

      if (this.hasTag("Compass") && this.isInPlayerInventory()) {
         IsoDirections var31 = this.getOutermostContainer().getParent().getDir();
         var19 = var18.addItem();
         var19.setLabel(Translator.getText("Tooltip_compass_" + var31.toCompassString()), 1.0F, 1.0F, 0.8F, 1.0F);
      }

      if (this.isFishingLure()) {
         var19 = var18.addItem();
         var19.setLabel(Translator.getText("Tooltip_IsFishingLure"), 1.0F, 1.0F, 0.8F, 1.0F);
      }

      if (this.getAttributes() != null) {
         this.getAttributes().DoTooltip(var1, var18);
      }

      if (this.getFluidContainer() != null) {
         this.getFluidContainer().DoTooltip(var1, var18);
      }

      if (this.getVisionModifier() != 1.0F) {
         var29 = new ColorInfo();
         var19 = var18.addItem();
         var19.setLabel(Translator.getText("Tooltip_item_VisionImpariment") + ": ", 1.0F, 1.0F, 0.8F, 1.0F);
         Core.getInstance().getGoodHighlitedColor().interp(Core.getInstance().getBadHighlitedColor(), 1.0F - this.getVisionModifier(), var29);
         var19.setProgress(1.0F - this.getVisionModifier(), var29.getR(), var29.getG(), var29.getB(), 1.0F);
      }

      if (this.getHearingModifier() != 1.0F) {
         var29 = new ColorInfo();
         var19 = var18.addItem();
         var19.setLabel(Translator.getText("Tooltip_item_HearingImpariment") + ": ", 1.0F, 1.0F, 0.8F, 1.0F);
         Core.getInstance().getGoodHighlitedColor().interp(Core.getInstance().getBadHighlitedColor(), 1.0F - this.getHearingModifier(), var29);
         var19.setProgress(1.0F - this.getHearingModifier(), var29.getR(), var29.getG(), var29.getB(), 1.0F);
      }

      if (this.getDiscomfortModifier() != 0.0F) {
         var29 = new ColorInfo();
         var19 = var18.addItem();
         var19.setLabel(Translator.getText("Tooltip_item_Discomfort") + ": ", 1.0F, 1.0F, 0.8F, 1.0F);
         Core.getInstance().getGoodHighlitedColor().interp(Core.getInstance().getBadHighlitedColor(), this.getDiscomfortModifier(), var29);
         var19.setProgress(this.getDiscomfortModifier(), var29.getR(), var29.getG(), var29.getB(), 1.0F);
      }

      if (Core.getInstance().getOptionShowItemModInfo() && !this.isVanilla()) {
         var19 = var18.addItem();
         Color var32 = Colors.CornFlowerBlue;
         var19.setLabel("Mod: " + this.getModName(), var32.r, var32.g, var32.b, 1.0F);
         ItemInfo var24 = WorldDictionary.getItemInfoFromID(this.getRegistry_id());
         if (var24 != null && var24.getModOverrides() != null) {
            var19 = var18.addItem();
            var13 = 0.5F;
            if (var24.getModOverrides().size() == 1) {
               var19.setLabel("This item overrides: " + WorldDictionary.getModNameFromID((String)var24.getModOverrides().get(0)), var13, var13, var13, 1.0F);
            } else {
               var19.setLabel("This item overrides:", var13, var13, var13, 1.0F);

               for(int var27 = 0; var27 < var24.getModOverrides().size(); ++var27) {
                  var19 = var18.addItem();
                  var19.setLabel(" - " + WorldDictionary.getModNameFromID((String)var24.getModOverrides().get(var27)), var13, var13, var13, 1.0F);
               }
            }
         }
      }

      if (this.getTooltip() != null) {
         var19 = var18.addItem();
         var19.setLabel(Translator.getText(this.getTooltip()), 1.0F, 1.0F, 0.8F, 1.0F);
      }

      if (var2 == null) {
         var6 = var18.render(var1.padLeft, var6, var1);
         var1.endLayout(var18);
         var6 += var1.padBottom;
         var1.setHeight((double)var6);
         if (var1.getWidth() < 150.0) {
            var1.setWidth(150.0);
         }
      }

   }

   public String getCleanString(float var1) {
      float var2 = (float)((int)(((double)var1 + 0.005) * 100.0)) / 100.0F;
      return Float.toString(var2);
   }

   public void DoTooltip(ObjectTooltip var1, ObjectTooltip.Layout var2) {
   }

   public void SetContainerPosition(int var1, int var2) {
      this.containerX = var1;
      this.containerY = var2;
   }

   public void Use() {
      this.Use(false);
   }

   public void UseAndSync() {
      this.Use(false, false, GameServer.bServer);
   }

   public void UseItem() {
      this.Use(false);
   }

   public void Use(boolean var1) {
      this.Use(var1, false, false);
   }

   public void Use(boolean var1, boolean var2, boolean var3) {
      if (this.isDisappearOnUse() || var1) {
         this.setCurrentUses(this.getCurrentUses() - 1);
         if (this.replaceOnUse != null && !var2 && !var1 && this.container != null) {
            String var4 = this.replaceOnUse;
            if (!this.replaceOnUse.contains(".")) {
               var4 = this.module + "." + var4;
            }

            InventoryItem var5 = this.container.AddItem(var4);
            if (var5 != null) {
               var5.setConditionFromModData(this);
               var5.setColorRed(this.colorRed);
               var5.setColorGreen(this.colorGreen);
               var5.setColorBlue(this.colorBlue);
               var5.setColor(new Color(this.colorRed, this.colorGreen, this.colorBlue));
               var5.setCustomColor(true);
               this.container.setDrawDirty(true);
               this.container.setDirty(true);
               var5.setCondition(this.getCondition());
               var5.setFavorite(this.isFavorite());
               if (GameServer.bServer && var3) {
                  GameServer.sendAddItemToContainer(this.container, var5);
               }
            }
         }

         if (this.getCurrentUses() <= 0) {
            if (this.isKeepOnDeplete()) {
               return;
            }

            if (this.container != null) {
               if (this.container.parent instanceof IsoGameCharacter && !(this instanceof HandWeapon)) {
                  IsoGameCharacter var6 = (IsoGameCharacter)this.container.parent;
                  var6.removeFromHands(this);
               }

               this.container.Items.remove(this);
               this.container.setDirty(true);
               this.container.setDrawDirty(true);
               if (GameServer.bServer && var3) {
                  GameServer.sendRemoveItemFromContainer(this.container, this);
               }

               this.container = null;
            }
         } else if (var3) {
            this.syncItemFields();
         }

      }
   }

   public boolean shouldUpdateInWorld() {
      if (!GameServer.bServer && this.hasComponent(ComponentType.FluidContainer) && this.itemHeat != 1.0F) {
         return true;
      } else if (GameClient.bClient || !this.hasComponent(ComponentType.FluidContainer) && !(this instanceof Food)) {
         return false;
      } else {
         IsoGridSquare var1 = this.getWorldItem().getSquare();
         return var1 != null && var1.isOutside();
      }
   }

   public void update() {
      if (this.isWet()) {
         this.wetCooldown -= 1.0F * GameTime.instance.getMultiplier();
         if (this.wetCooldown <= 0.0F) {
            InventoryItem var1 = InventoryItemFactory.CreateItem(this.itemWhenDry);
            if (this.isFavorite()) {
               var1.setFavorite(true);
            }

            IsoWorldInventoryObject var2 = this.getWorldItem();
            if (var2 != null) {
               IsoGridSquare var3 = var2.getSquare();
               var3.AddWorldInventoryItem(var1, var2.getX() % 1.0F, var2.getY() % 1.0F, var2.getZ() % 1.0F);
               var3.transmitRemoveItemFromSquare(var2);
               if (this.getContainer() != null) {
                  this.getContainer().setDirty(true);
                  this.getContainer().setDrawDirty(true);
               }

               var3.chunk.recalcHashCodeObjects();
               this.setWorldItem((IsoWorldInventoryObject)null);
            } else if (this.getContainer() != null) {
               this.getContainer().addItem(var1);
               this.getContainer().Remove(this);
            }

            this.setWet(false);
            IsoWorld.instance.CurrentCell.addToProcessItemsRemove(this);
            LuaEventManager.triggerEvent("OnContainerUpdate");
         }
      }

      FluidContainer var8;
      if (this.hasComponent(ComponentType.FluidContainer)) {
         ItemContainer var6 = this.getOutermostContainer();
         var8 = this.getFluidContainer();
         float var9;
         if (var6 != null) {
            var9 = var6.getTemprature();
            if (this.itemHeat > var9) {
               this.itemHeat -= 0.001F * GameTime.instance.getMultiplier();
               if (this.itemHeat < Math.max(0.2F, var9)) {
                  this.itemHeat = Math.max(0.2F, var9);
               }
            }

            if (this.itemHeat < var9 && (this.hasTag("Cookable") || this.hasTag("CookableMicrowave") && var6.getType().equals("microwave"))) {
               this.itemHeat += var9 / 1000.0F * GameTime.instance.getMultiplier();
               if (this.itemHeat > Math.min(3.0F, var9)) {
                  this.itemHeat = Math.min(3.0F, var9);
               }
            }

            if (this.itemHeat > 1.6F && !var8.isEmpty()) {
               if (var8.contains(Fluid.TaintedWater)) {
                  Float var4 = var8.getSpecificFluidAmount(Fluid.TaintedWater);
                  Float var5 = PZMath.min(var4, 0.001F);
                  var8.adjustSpecificFluidAmount(Fluid.TaintedWater, var4 - var5);
                  var8.addFluid(Fluid.Water, var5);
               }

               if (var8.contains(Fluid.Petrol)) {
                  var8.removeFluid();
                  boolean var10 = this.container != null && this.container.getParent() != null && this.container.getParent().getName() != null && this.container.getParent().getName().equals("Campfire");
                  if (!var10 && this.container != null && this.container.getParent() != null && this.container.getParent() instanceof IsoFireplace) {
                     var10 = true;
                  }

                  if (this.container != null && this.container.SourceGrid != null && !var10) {
                     IsoFireManager.StartFire(this.container.SourceGrid.getCell(), this.container.SourceGrid, true, 500000);
                  }
               }
            }
         }

         if ((this.container == null || this.getWorldItem() != null) && this.itemHeat != 1.0F) {
            var9 = 1.0F;
            if (this.itemHeat > var9) {
               this.itemHeat -= 0.001F * GameTime.instance.getMultiplier();
               if (this.itemHeat < var9) {
                  this.itemHeat = var9;
               }
            }

            if (this.itemHeat < var9) {
               this.itemHeat += var9 / 1000.0F * GameTime.instance.getMultiplier();
               if (this.itemHeat > var9) {
                  this.itemHeat = var9;
               }
            }
         }
      }

      if (!GameServer.bServer && this.getWorldItem() != null && RainManager.isRaining()) {
         IsoGridSquare var7 = this.getWorldItem().getSquare();
         if (this.hasComponent(ComponentType.FluidContainer)) {
            var8 = this.getFluidContainer();
            if (var7 != null && var7.isOutside() && var8.canPlayerEmpty() && var8.getRainCatcher() > 0.0F) {
               var8.addFluid(FluidType.TaintedWater, 0.001F * RainManager.getRainIntensity() * GameTime.instance.getMultiplier() * var8.getRainCatcher());
            }
         }

         if (this instanceof Food && var7 != null && var7.isOutside() && LuaManager.GlobalObject.ZombRandFloat(0.0F, 1.0F) < RainManager.getRainIntensity()) {
            ((Food)this).setTainted(true);
         }
      }

   }

   public boolean finishupdate() {
      if (!GameClient.bClient && this.getWorldItem() != null && this.getWorldItem().getObjectIndex() != -1 && this instanceof Food && !((Food)this).isTainted()) {
         return false;
      } else {
         if (this.hasComponent(ComponentType.FluidContainer)) {
            FluidContainer var1 = this.getFluidContainer();
            if (this.getWorldItem() != null && this.getWorldItem().getObjectIndex() != -1 && var1.canPlayerEmpty()) {
               return false;
            }

            if (this.getWorldItem() != null && this.itemHeat != 1.0F) {
               return false;
            }

            if (this.container != null && (this.itemHeat != 1.0F || this.itemHeat != this.container.getTemprature() || this.container.isTemperatureChanging())) {
               return false;
            }
         }

         return !this.isWet();
      }
   }

   public void updateSound(BaseSoundEmitter var1) {
      this.updateEquippedAndActivatedSound(var1);
   }

   public void updateEquippedAndActivatedSound(BaseSoundEmitter var1) {
      String var2 = this.getScriptItem().getSoundByID("EquippedAndActivated");
      if (var2 != null) {
         IsoPlayer var3 = this.getOwnerPlayer(this.getContainer());
         if (var3 == null) {
            this.stopEquippedAndActivatedSound();
            ItemSoundManager.removeItem(this);
         } else if (this.isEquipped() && this.isActivated()) {
            BaseCharacterSoundEmitter var4 = var3.getEmitter();
            if (!var4.isPlaying(this.equippedAndActivatedSound)) {
               this.stopEquippedAndActivatedSound();
               this.equippedAndActivatedPlayer = var3;
               this.equippedAndActivatedSound = var4.playSoundImpl(var2, var3);
            }

         } else {
            this.stopEquippedAndActivatedSound();
            ItemSoundManager.removeItem(this);
         }
      }
   }

   public void updateEquippedAndActivatedSound() {
      String var1 = this.getScriptItem().getSoundByID("EquippedAndActivated");
      if (var1 != null) {
         if (this.isActivated() && this instanceof DrainableComboItem && this.getCurrentUses() <= 0) {
            this.setActivated(false);
         }

         if (this.isEquipped() && this.isActivated()) {
            ItemSoundManager.addItem(this);
         } else {
            this.stopEquippedAndActivatedSound();
            ItemSoundManager.removeItem(this);
         }

      }
   }

   protected void stopEquippedAndActivatedSound() {
      if (this.equippedAndActivatedPlayer != null && this.equippedAndActivatedSound != 0L) {
         this.equippedAndActivatedPlayer.getEmitter().stopOrTriggerSound(this.equippedAndActivatedSound);
         this.equippedAndActivatedPlayer = null;
         this.equippedAndActivatedSound = 0L;
      }

   }

   public void playActivateSound() {
      String var1 = this.getScriptItem().getSoundByID("Activate");
      if (var1 != null) {
         this.playSoundOnPlayer(var1);
      }
   }

   public void playDeactivateSound() {
      String var1 = this.getScriptItem().getSoundByID("Deactivate");
      if (var1 != null) {
         this.playSoundOnPlayer(var1);
      }
   }

   public void playActivateDeactivateSound() {
      if (this.isActivated()) {
         this.playActivateSound();
      } else {
         this.playDeactivateSound();
      }

   }

   protected void playSoundOnPlayer(String var1) {
      IsoPlayer var2 = this.getOwnerPlayer(this.getContainer());
      if (var2 != null && var2.isLocalPlayer()) {
         var2.getEmitter().playSound(var1);
      }
   }

   protected IsoPlayer getOwnerPlayer(ItemContainer var1) {
      if (var1 == null) {
         return null;
      } else {
         IsoObject var2 = var1.getParent();
         return var2 instanceof IsoPlayer ? (IsoPlayer)var2 : null;
      }
   }

   public String getFullType() {
      assert this.fullType != null && this.fullType.equals(this.module + "." + this.type);

      return this.fullType;
   }

   public void save(ByteBuffer var1, boolean var2) throws IOException {
      var2 = false;
      if (GameWindow.DEBUG_SAVE) {
         DebugLog.log(this.getFullType());
      }

      var1.putShort(this.getRegistry_id());
      var1.put((byte)this.getSaveType());
      var1.putInt(this.id);
      BitHeaderWrite var3 = BitHeader.allocWrite(BitHeader.HeaderSize.Byte, var1);
      if (this.getCurrentUses() != 1) {
         var3.addFlags(1);
         if (this.getCurrentUses() > 32767) {
            var1.putShort((short)32767);
         } else {
            var1.putShort((short)this.getCurrentUses());
         }
      }

      int var5;
      if (this.IsDrainable() && this.getCurrentUsesFloat() < 1.0F) {
         var3.addFlags(2);
         float var4 = this.getCurrentUsesFloat();
         var5 = (byte)((byte)((int)(var4 * 255.0F)) + -128);
         var1.put((byte)var5);
      }

      if (this.Condition != this.ConditionMax) {
         var3.addFlags(4);
         var1.put((byte)this.getCondition());
      }

      if (this.visual != null) {
         var3.addFlags(8);
         this.visual.save(var1);
      }

      if (this.isCustomColor() && (this.col.r != 1.0F || this.col.g != 1.0F || this.col.b != 1.0F || this.col.a != 1.0F)) {
         var3.addFlags(16);
         var1.put(Bits.packFloatUnitToByte(this.getColor().r));
         var1.put(Bits.packFloatUnitToByte(this.getColor().g));
         var1.put(Bits.packFloatUnitToByte(this.getColor().b));
         var1.put(Bits.packFloatUnitToByte(this.getColor().a));
      }

      if (this.itemCapacity != -1.0F) {
         var3.addFlags(32);
         var1.putFloat(this.itemCapacity);
      }

      BitHeaderWrite var7 = BitHeader.allocWrite(BitHeader.HeaderSize.Integer, var1);
      if (this.table != null && !this.table.isEmpty()) {
         var7.addFlags(1);
         this.table.save(var1);
      }

      if (this.isActivated()) {
         var7.addFlags(2);
      }

      if (this.haveBeenRepaired != 0) {
         var7.addFlags(4);
         var1.putShort((short)this.getHaveBeenRepaired());
      }

      if (this.name != null && !this.name.equals(this.originalName)) {
         var7.addFlags(8);
         GameWindow.WriteString(var1, this.name);
      }

      if (this.byteData != null) {
         var7.addFlags(16);
         this.byteData.rewind();
         var1.putInt(this.byteData.limit());
         var1.put(this.byteData);
         this.byteData.flip();
      }

      if (this.extraItems != null && this.extraItems.size() > 0) {
         var7.addFlags(32);
         var1.putInt(this.extraItems.size());

         for(var5 = 0; var5 < this.extraItems.size(); ++var5) {
            var1.putShort(WorldDictionary.getItemRegistryID((String)this.extraItems.get(var5)));
         }
      }

      if (this.isCustomName()) {
         var7.addFlags(64);
      }

      if (this.isCustomWeight()) {
         var7.addFlags(128);
         var1.putFloat(this.isCustomWeight() ? this.getActualWeight() : -1.0F);
      }

      if (this.keyId != -1) {
         var7.addFlags(256);
         var1.putInt(this.getKeyId());
      }

      if (this.remoteControlID != -1 || this.remoteRange != 0) {
         var7.addFlags(1024);
         var1.putInt(this.getRemoteControlID());
         var1.putInt(this.getRemoteRange());
      }

      if (this.colorRed != 1.0F || this.colorGreen != 1.0F || this.colorBlue != 1.0F) {
         var7.addFlags(2048);
         var1.put(Bits.packFloatUnitToByte(this.colorRed));
         var1.put(Bits.packFloatUnitToByte(this.colorGreen));
         var1.put(Bits.packFloatUnitToByte(this.colorBlue));
      }

      if (this.worker != null) {
         var7.addFlags(4096);
         GameWindow.WriteString(var1, this.getWorker());
      }

      if (this.wetCooldown != -1.0F) {
         var7.addFlags(8192);
         var1.putFloat(this.wetCooldown);
      }

      if (this.isFavorite()) {
         var7.addFlags(16384);
      }

      if (this.stashMap != null) {
         var7.addFlags(32768);
         GameWindow.WriteString(var1, this.stashMap);
      }

      if (this.isInfected()) {
         var7.addFlags(65536);
      }

      if (this.currentAmmoCount != 0) {
         var7.addFlags(131072);
         var1.putInt(this.currentAmmoCount);
      }

      if (this.attachedSlot != -1) {
         var7.addFlags(262144);
         var1.putInt(this.attachedSlot);
      }

      if (this.attachedSlotType != null) {
         var7.addFlags(524288);
         GameWindow.WriteString(var1, this.attachedSlotType);
      }

      if (this.attachedToModel != null) {
         var7.addFlags(1048576);
         GameWindow.WriteString(var1, this.attachedToModel);
      }

      if (this.maxCapacity != -1) {
         var7.addFlags(2097152);
         var1.putInt(this.maxCapacity);
      }

      if (this.isRecordedMedia()) {
         var7.addFlags(4194304);
         var1.putShort(this.recordedMediaIndex);
      }

      if (this.worldZRotation > -1) {
         var7.addFlags(8388608);
         var1.putInt(this.worldZRotation);
      }

      if (this.worldScale != 1.0F) {
         var7.addFlags(16777216);
         var1.putFloat(this.worldScale);
      }

      if (this.isInitialised) {
         var7.addFlags(33554432);
      }

      if (this.requiresEntitySave()) {
         var7.addFlags(67108864);
         this.saveEntity(var1);
      }

      if (this.animalTracks != null) {
         var7.addFlags(134217728);
         this.animalTracks.save(var1);
      }

      if (this.texture != null && this.texture.getName() != null && this.texture != Texture.getSharedTexture("media/inventory/Question_On.png") && this.getScriptItem().getIcon() != null && !Objects.equals(this.getScriptItem().getIcon(), "None") && !Objects.equals(this.getScriptItem().getIcon(), "default")) {
         String var8 = this.texture.getName();
         String var6 = "Item_" + this.getScriptItem().getIcon();
         if (!Objects.equals(var8, var6)) {
            var7.addFlags(268435456);
            GameWindow.WriteString(var1, this.texture.getName());
         }
      }

      if (this.modelIndex > -1) {
         var7.addFlags(536870912);
         var1.putInt(this.modelIndex);
      }

      if (!var7.equals(0)) {
         var3.addFlags(64);
         var7.write();
      } else {
         var1.position(var7.getStartPosition());
      }

      var3.write();
      var3.release();
      var7.release();
   }

   public static InventoryItem loadItem(ByteBuffer var0, int var1) throws IOException {
      return loadItem(var0, var1, true);
   }

   public static InventoryItem loadItem(ByteBuffer var0, int var1, boolean var2) throws IOException {
      return loadItem(var0, var1, var2, (InventoryItem)null);
   }

   public static InventoryItem loadItem(ByteBuffer var0, int var1, boolean var2, InventoryItem var3) throws IOException {
      int var4 = var0.getInt();
      if (var4 <= 0) {
         throw new IOException("InventoryItem.loadItem() invalid item data length: " + var4);
      } else {
         int var5 = var0.position();
         short var6 = var0.getShort();
         boolean var7 = true;
         byte var11 = var0.get();
         if (var11 < 0) {
            DebugLog.log("InventoryItem.loadItem() invalid item save-type " + var11 + ", itemtype: " + WorldDictionary.getItemTypeDebugString(var6));
            return null;
         } else {
            InventoryItem var8 = var3;
            if (var3 == null) {
               var8 = InventoryItemFactory.CreateItem(var6);
            }

            if (var2 && var11 != -1 && var8 != null && var8.getSaveType() != var11) {
               DebugLog.log("InventoryItem.loadItem() ignoring \"" + var8.getFullType() + "\" because type changed from " + var11 + " to " + var8.getSaveType());
               var8 = null;
            }

            if (var8 != null) {
               try {
                  var8.load(var0, var1);
               } catch (Exception var10) {
                  ExceptionLogger.logException(var10);
                  var8 = null;
               }
            }

            if (var8 != null) {
               if (var4 != -1 && var0.position() != var5 + var4) {
                  var0.position(var5 + var4);
                  DebugLog.log("InventoryItem.loadItem() data length not matching, resetting buffer position to '" + (var5 + var4) + "'. itemtype: " + WorldDictionary.getItemTypeDebugString(var6));
                  if (Core.bDebug) {
                     throw new IOException("InventoryItem.loadItem() read more data than save() wrote (" + WorldDictionary.getItemTypeDebugString(var6) + ")");
                  }
               }

               return var8;
            } else {
               if (var0.position() >= var5 + var4) {
                  if (var0.position() >= var5 + var4) {
                     var0.position(var5 + var4);
                     DebugLog.log("InventoryItem.loadItem() item == null, resetting buffer position to '" + (var5 + var4) + "'. itemtype: " + WorldDictionary.getItemTypeDebugString(var6));
                  }
               } else {
                  while(var0.position() < var5 + var4) {
                     var0.get();
                  }

                  DebugLog.log("InventoryItem.loadItem() item == null, skipped bytes. itemtype: " + WorldDictionary.getItemTypeDebugString(var6));
               }

               return null;
            }
         }
      }
   }

   public void load(ByteBuffer var1, int var2) throws IOException {
      this.id = var1.getInt();
      BitHeaderRead var3 = BitHeader.allocRead(BitHeader.HeaderSize.Byte, var1);
      this.setCurrentUses(this.getMaxUses());
      this.Condition = this.ConditionMax;
      this.customColor = false;
      this.col = Color.white;
      this.itemCapacity = -1.0F;
      this.activated = false;
      this.haveBeenRepaired = 0;
      this.customName = false;
      this.customWeight = false;
      this.keyId = -1;
      this.remoteControlID = -1;
      this.remoteRange = 0;
      this.colorRed = this.colorGreen = this.colorBlue = 1.0F;
      this.worker = null;
      this.wetCooldown = -1.0F;
      this.favorite = false;
      this.stashMap = null;
      this.zombieInfected = false;
      this.currentAmmoCount = 0;
      this.attachedSlot = -1;
      this.attachedSlotType = null;
      this.attachedToModel = null;
      this.maxCapacity = -1;
      this.recordedMediaIndex = -1;
      this.worldZRotation = -1;
      this.worldScale = 1.0F;
      this.isInitialised = false;
      if (!var3.equals(0)) {
         if (var3.hasFlags(1)) {
            short var4 = var1.getShort();
            this.setCurrentUses(var4);
         }

         float var5;
         int var6;
         if (var3.hasFlags(2)) {
            byte var9 = var1.get();
            var5 = PZMath.clamp((float)(var9 - -128) / 255.0F, 0.0F, 1.0F);
            var6 = (int)(var5 / ((DrainableComboItem)this).getUseDelta());
            this.setCurrentUses(var6);
         }

         if (var3.hasFlags(4)) {
            this.setConditionNoSound(var1.get());
         }

         if (var3.hasFlags(8)) {
            this.visual = new ItemVisual();
            this.visual.load(var1, var2);
         }

         float var7;
         float var13;
         if (var3.hasFlags(16)) {
            float var10 = Bits.unpackByteToFloatUnit(var1.get());
            var5 = Bits.unpackByteToFloatUnit(var1.get());
            var13 = Bits.unpackByteToFloatUnit(var1.get());
            var7 = Bits.unpackByteToFloatUnit(var1.get());
            this.setColor(new Color(var10, var5, var13, var7));
         }

         if (var3.hasFlags(32)) {
            this.itemCapacity = var1.getFloat();
         }

         if (var3.hasFlags(64)) {
            BitHeaderRead var11 = BitHeader.allocRead(BitHeader.HeaderSize.Integer, var1);
            if (var11.hasFlags(1)) {
               if (this.table == null) {
                  this.table = LuaManager.platform.newTable();
               }

               this.table.load(var1, var2);
            }

            this.activated = var11.hasFlags(2);
            if (var11.hasFlags(4)) {
               this.setHaveBeenRepaired(var1.getShort());
            }

            if (var11.hasFlags(8)) {
               this.name = GameWindow.ReadString(var1);
            }

            int var12;
            if (var11.hasFlags(16)) {
               var12 = var1.getInt();
               this.byteData = ByteBuffer.allocate(var12);

               for(var6 = 0; var6 < var12; ++var6) {
                  this.byteData.put(var1.get());
               }

               this.byteData.flip();
            }

            if (var11.hasFlags(32)) {
               var12 = var1.getInt();
               if (var12 > 0) {
                  this.extraItems = new ArrayList();

                  for(var6 = 0; var6 < var12; ++var6) {
                     short var14 = var1.getShort();
                     String var8 = WorldDictionary.getItemTypeFromID(var14);
                     this.extraItems.add(var8);
                  }
               }
            }

            this.setCustomName(var11.hasFlags(64));
            if (var11.hasFlags(128)) {
               var5 = var1.getFloat();
               if (var5 >= 0.0F) {
                  this.setActualWeight(var5);
                  this.setWeight(var5);
                  this.setCustomWeight(true);
               }
            }

            if (var11.hasFlags(256)) {
               this.setKeyId(var1.getInt());
            }

            if (var11.hasFlags(1024)) {
               this.setRemoteControlID(var1.getInt());
               this.setRemoteRange(var1.getInt());
            }

            if (var11.hasFlags(2048)) {
               var5 = Bits.unpackByteToFloatUnit(var1.get());
               var13 = Bits.unpackByteToFloatUnit(var1.get());
               var7 = Bits.unpackByteToFloatUnit(var1.get());
               this.setColorRed(var5);
               this.setColorGreen(var13);
               this.setColorBlue(var7);
               this.setColor(new Color(this.colorRed, this.colorGreen, this.colorBlue));
            }

            if (var11.hasFlags(4096)) {
               this.setWorker(GameWindow.ReadString(var1));
            }

            if (var11.hasFlags(8192)) {
               this.setWetCooldown(var1.getFloat());
            }

            this.setFavorite(var11.hasFlags(16384));
            if (var11.hasFlags(32768)) {
               this.stashMap = GameWindow.ReadString(var1);
            }

            this.setInfected(var11.hasFlags(65536));
            if (var11.hasFlags(131072)) {
               this.setCurrentAmmoCount(var1.getInt());
            }

            if (var11.hasFlags(262144)) {
               this.attachedSlot = var1.getInt();
            }

            if (var11.hasFlags(524288)) {
               this.attachedSlotType = GameWindow.ReadString(var1);
            }

            if (var11.hasFlags(1048576)) {
               this.attachedToModel = GameWindow.ReadString(var1);
            }

            if (var11.hasFlags(2097152)) {
               this.maxCapacity = var1.getInt();
            }

            if (var11.hasFlags(4194304)) {
               this.setRecordedMediaIndex(var1.getShort());
            }

            if (var11.hasFlags(8388608)) {
               this.setWorldZRotation(var1.getInt());
            }

            if (var11.hasFlags(16777216)) {
               this.worldScale = var1.getFloat();
            }

            this.setInitialised(var11.hasFlags(33554432));
            if (var11.hasFlags(67108864)) {
               this.loadEntity(var1, var2);
            }

            if (var11.hasFlags(134217728)) {
               this.animalTracks = new AnimalTracks();
               this.animalTracks.load(var1, var2);
            }

            if (var11.hasFlags(268435456)) {
               this.setTexture(Texture.getSharedTexture(GameWindow.ReadStringUTF(var1)));
            }

            if (var11.hasFlags(536870912)) {
               this.modelIndex = var1.getInt();
            }

            var11.release();
         }
      }

      this.synchWithVisual();
      var3.release();
   }

   public boolean IsFood() {
      return false;
   }

   public boolean IsWeapon() {
      return false;
   }

   public boolean IsDrainable() {
      return false;
   }

   public boolean IsLiterature() {
      return false;
   }

   public boolean IsClothing() {
      return false;
   }

   public boolean IsInventoryContainer() {
      return false;
   }

   public boolean IsMap() {
      return false;
   }

   static InventoryItem LoadFromFile(DataInputStream var0) throws IOException {
      GameWindow.ReadString(var0);
      return null;
   }

   public ItemContainer getOutermostContainer() {
      if (this.container != null && !"floor".equals(this.container.type)) {
         ItemContainer var1;
         for(var1 = this.container; var1.getContainingItem() != null && var1.getContainingItem().getContainer() != null && !"floor".equals(var1.getContainingItem().getContainer().type); var1 = var1.getContainingItem().getContainer()) {
         }

         return var1;
      } else {
         return null;
      }
   }

   public boolean isInLocalPlayerInventory() {
      if (!GameClient.bClient) {
         return false;
      } else {
         ItemContainer var1 = this.getOutermostContainer();
         if (var1 == null) {
            return false;
         } else {
            return var1.getParent() instanceof IsoPlayer ? ((IsoPlayer)var1.getParent()).isLocalPlayer() : false;
         }
      }
   }

   public boolean isInPlayerInventory() {
      ItemContainer var1 = this.getOutermostContainer();
      return var1 == null ? false : var1.getParent() instanceof IsoPlayer;
   }

   public ItemReplacement getItemReplacementPrimaryHand() {
      return this.ScriptItem.replacePrimaryHand;
   }

   public ItemReplacement getItemReplacementSecondHand() {
      return this.ScriptItem.replaceSecondHand;
   }

   public ClothingItem getClothingItem() {
      if ("RightHand".equalsIgnoreCase(this.getAlternateModelName())) {
         return this.getItemReplacementPrimaryHand().clothingItem;
      } else {
         return "LeftHand".equalsIgnoreCase(this.getAlternateModelName()) ? this.getItemReplacementSecondHand().clothingItem : this.ScriptItem.getClothingItemAsset();
      }
   }

   public String getAlternateModelName() {
      if (this.getContainer() != null && this.getContainer().getParent() instanceof IsoGameCharacter) {
         IsoGameCharacter var1 = (IsoGameCharacter)this.getContainer().getParent();
         if (var1.getPrimaryHandItem() == this && this.getItemReplacementPrimaryHand() != null) {
            return "RightHand";
         }

         if (var1.getSecondaryHandItem() == this && this.getItemReplacementSecondHand() != null) {
            return "LeftHand";
         }
      }

      return this.m_alternateModelName;
   }

   public ItemVisual getVisual() {
      ClothingItem var1 = this.getClothingItem();
      if (var1 != null && var1.isReady()) {
         if (this.visual == null) {
            this.visual = new ItemVisual();
            this.visual.setItemType(this.getFullType());
            this.visual.pickUninitializedValues(var1);
         }

         this.visual.setClothingItemName(var1.m_Name);
         this.visual.setAlternateModelName(this.getAlternateModelName());
         return this.visual;
      } else {
         this.visual = null;
         return null;
      }
   }

   public boolean allowRandomTint() {
      ClothingItem var1 = this.getClothingItem();
      return var1 != null ? var1.m_AllowRandomTint : false;
   }

   public void synchWithVisual() {
      int var4;
      if (this instanceof HandWeapon && ((HandWeapon)this).getWeaponSpritesByIndex() != null && this.modelIndex == -1) {
         var4 = ((HandWeapon)this).getWeaponSpritesByIndex().size();
         this.modelIndex = Rand.Next(var4);
      } else if ((this.getStaticModelsByIndex() != null || this.getWorldStaticModelsByIndex() != null) && this.modelIndex == -1) {
         boolean var1 = true;
         if (this.getStaticModelsByIndex() != null && this.getWorldStaticModelsByIndex() != null) {
            var4 = Math.max(this.getStaticModelsByIndex().size(), this.getWorldStaticModelsByIndex().size());
         } else if (this.getStaticModelsByIndex() != null && this.getWorldStaticModelsByIndex() == null) {
            var4 = this.getStaticModelsByIndex().size();
         } else {
            var4 = this.getWorldStaticModelsByIndex().size();
         }

         this.modelIndex = Rand.Next(var4);
      }

      if (this.modelIndex != -1 && this.getIconsForTexture() != null && this.getIconsForTexture().get(this.modelIndex) != null) {
         String var5 = (String)this.getIconsForTexture().get(this.modelIndex);
         if (!StringUtils.isNullOrWhitespace(var5)) {
            this.texture = Texture.trygetTexture("Item_" + var5);
            if (this.texture == null) {
               this.texture = Texture.getSharedTexture("media/inventory/Question_On.png");
            }
         }
      }

      if (this instanceof Clothing || this instanceof InventoryContainer) {
         ItemVisual var6 = this.getVisual();
         if (var6 != null) {
            if (this instanceof Clothing && this.getBloodClothingType() != null) {
               BloodClothingType.calcTotalBloodLevel((Clothing)this);
               BloodClothingType.calcTotalDirtLevel((Clothing)this);
            }

            ClothingItem var2 = this.getClothingItem();
            if (var2.m_AllowRandomTint) {
               this.setColor(new Color(var6.m_Tint.r, var6.m_Tint.g, var6.m_Tint.b));
            } else {
               this.setColor(new Color(this.getColorRed(), this.getColorGreen(), this.getColorBlue()));
            }

            if ((var2.m_BaseTextures.size() > 1 || var6.m_TextureChoice > -1) && this.getIconsForTexture() != null) {
               String var3 = null;
               if (var6.m_BaseTexture > -1 && this.getIconsForTexture().size() > var6.m_BaseTexture) {
                  var3 = (String)this.getIconsForTexture().get(var6.m_BaseTexture);
               } else if (var6.m_TextureChoice > -1 && this.getIconsForTexture().size() > var6.m_TextureChoice) {
                  var3 = (String)this.getIconsForTexture().get(var6.m_TextureChoice);
               }

               if (!StringUtils.isNullOrWhitespace(var3)) {
                  this.texture = Texture.trygetTexture("Item_" + var3);
                  if (this.texture == null) {
                     this.texture = Texture.getSharedTexture("media/inventory/Question_On.png");
                  }

               }
            }
         }
      }
   }

   public int getContainerX() {
      return this.containerX;
   }

   public void setContainerX(int var1) {
      this.containerX = var1;
   }

   public int getContainerY() {
      return this.containerY;
   }

   public void setContainerY(int var1) {
      this.containerY = var1;
   }

   public boolean isDisappearOnUse() {
      return this.getScriptItem().isDisappearOnUse();
   }

   public boolean isKeepOnDeplete() {
      return !this.getScriptItem().isDisappearOnUse();
   }

   public String getName() {
      if (this.getFluidContainer() != null) {
         return this.getFluidContainer().getUiName();
      } else if (this.getRemoteControlID() != -1) {
         return Translator.getText("IGUI_ItemNameControllerLinked", this.name);
      } else {
         String var1 = this.name;
         if (this.getMechanicType() > 0) {
            var1 = Translator.getText("IGUI_ItemNameMechanicalType", var1, Translator.getText("IGUI_VehicleType_" + this.getMechanicType()));
         }

         String var2 = "";
         if (this.isBloody()) {
            var2 = var2 + this.bloodyString + ", ";
         }

         if (this.isBroken()) {
            var2 = var2 + this.brokenString + ", ";
         } else if ((float)this.getCondition() < (float)this.getConditionMax() / 3.0F) {
            var2 = var2 + this.wornString + ", ";
         }

         if (!this.isBroken() && this.hasSharpness() && this.getSharpness() < 0.33333334F) {
            if (this.getSharpness() <= 0.0F) {
               var2 = var2 + this.bluntString + ", ";
            } else {
               var2 = var2 + this.dullString + ", ";
            }
         }

         if (this instanceof DrainableComboItem && this.getCurrentUsesFloat() <= 0.0F) {
            var2 = var2 + this.EmptyString + ", ";
         }

         if (var2.length() > 2) {
            var2 = var2.substring(0, var2.length() - 2);
         }

         var2 = var2.trim();
         return var2.isEmpty() ? var1 : Translator.getText("IGUI_ClothingNaming", var2, var1);
      }
   }

   public void setName(String var1) {
      if (var1.length() > 256) {
         var1 = var1.substring(0, Math.min(var1.length(), 256));
      }

      this.name = var1;
   }

   public String getReplaceOnUse() {
      return this.replaceOnUse;
   }

   public void setReplaceOnUse(String var1) {
      this.replaceOnUse = var1;
      this.replaceOnUseFullType = StringUtils.moduleDotType(this.getModule(), var1);
   }

   public String getReplaceOnUseFullType() {
      return this.replaceOnUseFullType;
   }

   public int getConditionMax() {
      return this.ConditionMax;
   }

   public void setConditionMax(int var1) {
      this.ConditionMax = var1;
   }

   public ItemContainer getRightClickContainer() {
      return this.rightClickContainer;
   }

   public void setRightClickContainer(ItemContainer var1) {
      this.rightClickContainer = var1;
   }

   public String getSwingAnim() {
      return this.getScriptItem().SwingAnim;
   }

   public Texture getTexture() {
      return this.texture;
   }

   public Texture getIcon() {
      return this.getTexture();
   }

   public void setTexture(Texture var1) {
      this.texture = var1;
   }

   public void setIcon(Texture var1) {
      this.setTexture(var1);
   }

   public Texture getTexturerotten() {
      return this.texturerotten;
   }

   public void setTexturerotten(Texture var1) {
      this.texturerotten = var1;
   }

   public Texture getTextureCooked() {
      return this.textureCooked;
   }

   public void setTextureCooked(Texture var1) {
      this.textureCooked = var1;
   }

   public Texture getTextureBurnt() {
      return this.textureBurnt;
   }

   public void setTextureBurnt(Texture var1) {
      this.textureBurnt = var1;
   }

   public void setType(String var1) {
      this.type = var1;
      this.fullType = this.module + "." + var1;
   }

   public void setCurrentUses(int var1) {
      this.uses = var1;
   }

   public int getCurrentUses() {
      return this.uses;
   }

   public void setCurrentUsesFrom(InventoryItem var1) {
      this.setCurrentUses(var1.getCurrentUses());
   }

   public int getMaxUses() {
      return 1;
   }

   public float getCurrentUsesFloat() {
      return 1.0F;
   }

   /** @deprecated */
   @Deprecated
   public int getUses() {
      return this.uses;
   }

   /** @deprecated */
   @Deprecated
   public void setUses(int var1) {
      this.uses = var1;
   }

   public void setUsesFrom(InventoryItem var1) {
      this.setUses(var1.getUses());
   }

   public float getAge() {
      return this.Age;
   }

   public void setAge(float var1) {
      this.Age = var1;
   }

   public float getLastAged() {
      return this.LastAged;
   }

   public void setLastAged(float var1) {
      this.LastAged = var1;
   }

   public void updateAge() {
   }

   public void setAutoAge() {
   }

   public boolean isIsCookable() {
      return this.IsCookable;
   }

   public boolean isCookable() {
      return this.IsCookable;
   }

   public void setIsCookable(boolean var1) {
      this.IsCookable = var1;
   }

   public float getCookingTime() {
      return this.CookingTime;
   }

   public void setCookingTime(float var1) {
      this.CookingTime = var1;
   }

   public float getMinutesToCook() {
      return this.MinutesToCook;
   }

   public void setMinutesToCook(float var1) {
      this.MinutesToCook = var1;
   }

   public float getMinutesToBurn() {
      return this.MinutesToBurn;
   }

   public void setMinutesToBurn(float var1) {
      this.MinutesToBurn = var1;
   }

   public boolean isCooked() {
      return this.Cooked;
   }

   public void setCooked(boolean var1) {
      this.Cooked = var1;
   }

   public boolean isBurnt() {
      return this.Burnt;
   }

   public void setBurnt(boolean var1) {
      this.Burnt = var1;
   }

   public int getOffAge() {
      return this.OffAge;
   }

   public void setOffAge(int var1) {
      this.OffAge = var1;
   }

   public int getOffAgeMax() {
      return this.OffAgeMax;
   }

   public void setOffAgeMax(int var1) {
      this.OffAgeMax = var1;
   }

   public float getWeight() {
      return this.Weight < 0.0F ? 0.0F : this.Weight;
   }

   public void setWeight(float var1) {
      if (var1 < 0.0F) {
         var1 = 0.0F;
      }

      this.Weight = var1;
   }

   public float getActualWeight() {
      if (this.getDisplayName().equals(this.getFullType())) {
         return 0.0F;
      } else {
         return this.ActualWeight < 0.0F ? 0.0F : this.ActualWeight;
      }
   }

   public void setActualWeight(float var1) {
      if (var1 < 0.0F) {
         var1 = 0.0F;
      }

      this.ActualWeight = var1;
   }

   public String getWorldTexture() {
      return this.WorldTexture;
   }

   public void setWorldTexture(String var1) {
      this.WorldTexture = var1;
   }

   public String getDescription() {
      return this.Description;
   }

   public void setDescription(String var1) {
      this.Description = var1;
   }

   public int getCondition() {
      return this.Condition;
   }

   public void setCondition(int var1, boolean var2) {
      if (!Core.bDebug || !DebugOptions.instance.Cheat.Player.UnlimitedCondition.getValue() || this.Condition <= var1) {
         var1 = Math.max(0, var1);
         var1 = Math.min(this.getConditionMax(), var1);
         if (var2 && this.Condition > 0 && var1 <= 0) {
            this.doBreakSound();
         } else if (var2 && this.Condition > 0 && var1 < this.Condition) {
            this.doDamagedSound();
         }

         this.Condition = var1;
         this.setBroken(var1 <= 0);
      }
   }

   public void doBreakSound() {
      if (this.getBreakSound() != null && !this.getBreakSound().isEmpty() && IsoPlayer.getInstance() != null) {
         IsoPlayer.getInstance().playSound(this.getBreakSound());
      } else if (this.getDamagedSound() != null) {
         this.doDamagedSound();
      }

   }

   public void doDamagedSound() {
      if (this.getDamagedSound() != null && !this.getDamagedSound().isEmpty() && IsoPlayer.getInstance() != null) {
         IsoPlayer.getInstance().playSound(this.getDamagedSound());
      }

   }

   public void setCondition(int var1) {
      this.setCondition(var1, true);
   }

   public void setConditionNoSound(int var1) {
      this.setCondition(var1, false);
   }

   public String getOffString() {
      return this.OffString;
   }

   public void setOffString(String var1) {
      this.OffString = var1;
   }

   public String getCookedString() {
      return this.CookedString;
   }

   public void setCookedString(String var1) {
      this.CookedString = var1;
   }

   public String getUnCookedString() {
      return this.UnCookedString;
   }

   public void setUnCookedString(String var1) {
      this.UnCookedString = var1;
   }

   public String getBurntString() {
      return this.BurntString;
   }

   public void setBurntString(String var1) {
      this.BurntString = var1;
   }

   public String getModule() {
      return this.module;
   }

   public void setModule(String var1) {
      this.module = var1;
      this.fullType = var1 + "." + this.type;
   }

   public boolean isAlwaysWelcomeGift() {
      return this.getScriptItem().isAlwaysWelcomeGift();
   }

   public boolean isCanBandage() {
      return this.getScriptItem().isCanBandage();
   }

   public float getBoredomChange() {
      return this.boredomChange;
   }

   public void setBoredomChange(float var1) {
      this.boredomChange = var1;
   }

   public float getUnhappyChange() {
      return this.unhappyChange;
   }

   public void setUnhappyChange(float var1) {
      this.unhappyChange = var1;
   }

   public float getStressChange() {
      return this.stressChange;
   }

   public void setStressChange(float var1) {
      this.stressChange = var1;
   }

   public ArrayList<String> getTags() {
      return this.ScriptItem.getTags();
   }

   public boolean hasTag(String var1) {
      ArrayList var2 = this.getTags();

      for(int var3 = 0; var3 < var2.size(); ++var3) {
         if (((String)var2.get(var3)).equalsIgnoreCase(var1)) {
            return true;
         }
      }

      return false;
   }

   public ArrayList<IsoObject> getTaken() {
      return this.Taken;
   }

   public void setTaken(ArrayList<IsoObject> var1) {
      this.Taken = var1;
   }

   public IsoDirections getPlaceDir() {
      return this.placeDir;
   }

   public void setPlaceDir(IsoDirections var1) {
      this.placeDir = var1;
   }

   public IsoDirections getNewPlaceDir() {
      return this.newPlaceDir;
   }

   public void setNewPlaceDir(IsoDirections var1) {
      this.newPlaceDir = var1;
   }

   public void setReplaceOnUseOn(String var1) {
      this.ReplaceOnUseOn = var1;
   }

   public String getReplaceOnUseOn() {
      return this.ReplaceOnUseOn;
   }

   public String getReplaceOnUseOnString() {
      String var1 = this.getReplaceOnUseOn();
      if (var1.split("-")[0].trim().contains("WaterSource")) {
         var1 = var1.split("-")[1];
         if (!var1.contains(".")) {
            String var10000 = this.getModule();
            var1 = var10000 + "." + var1;
         }
      }

      return var1;
   }

   public String getReplaceTypes() {
      return this.getScriptItem().getReplaceTypes();
   }

   public HashMap<String, String> getReplaceTypesMap() {
      return this.getScriptItem().getReplaceTypesMap();
   }

   public String getReplaceType(String var1) {
      return this.getScriptItem().getReplaceType(var1);
   }

   public boolean hasReplaceType(String var1) {
      return this.getScriptItem().hasReplaceType(var1);
   }

   public boolean isWaterSource() {
      return this.hasComponent(ComponentType.FluidContainer) && !this.getFluidContainer().isEmpty() && (this.getFluidContainer().getPrimaryFluid() == Fluid.Water || this.getFluidContainer().getPrimaryFluid() == Fluid.TaintedWater);
   }

   boolean CanStackNoTemp(InventoryItem var1) {
      return false;
   }

   public void CopyModData(KahluaTable var1) {
      this.copyModData(var1);
   }

   public void copyModData(KahluaTable var1) {
      if (this.table != null) {
         this.table.wipe();
      }

      if (var1 != null) {
         LuaManager.copyTable(this.getModData(), var1);
      }
   }

   public int getCount() {
      return this.Count;
   }

   public void setCount(int var1) {
      this.Count = var1;
   }

   public boolean isActivated() {
      return this.activated;
   }

   public void setActivated(boolean var1) {
      this.activated = var1;
      if (this.canEmitLight() && GameClient.bClient && this.getEquipParent() != null) {
         if (this.getEquipParent().getPrimaryHandItem() == this) {
            this.getEquipParent().reportEvent("EventSetActivatedPrimary");
         } else if (this.getEquipParent().getSecondaryHandItem() == this) {
            this.getEquipParent().reportEvent("EventSetActivatedSecondary");
         }
      }

   }

   public void setActivatedRemote(boolean var1) {
      this.activated = var1;
   }

   public void setCanBeActivated(boolean var1) {
      this.canBeActivated = var1;
   }

   public boolean canBeActivated() {
      return this.canBeActivated;
   }

   public void setLightStrength(float var1) {
      this.lightStrength = var1;
   }

   public float getLightStrength() {
      return this.lightStrength;
   }

   public boolean isTorchCone() {
      return this.isTorchCone;
   }

   public void setTorchCone(boolean var1) {
      this.isTorchCone = var1;
   }

   public float getTorchDot() {
      return this.getScriptItem().torchDot;
   }

   public int getLightDistance() {
      return this.lightDistance;
   }

   public void setLightDistance(int var1) {
      this.lightDistance = var1;
   }

   public boolean canEmitLight() {
      if (this.getLightStrength() <= 0.0F) {
         return false;
      } else {
         Drainable var1 = (Drainable)Type.tryCastTo(this, Drainable.class);
         return var1 == null || this.getCurrentUses() > 0;
      }
   }

   public boolean isEmittingLight() {
      if (!this.canEmitLight()) {
         return false;
      } else {
         return !this.canBeActivated() || this.isActivated();
      }
   }

   public boolean canStoreWater() {
      return this.hasComponent(ComponentType.FluidContainer);
   }

   public float getFatigueChange() {
      return this.fatigueChange;
   }

   public void setFatigueChange(float var1) {
      this.fatigueChange = var1;
   }

   public float getCurrentCondition() {
      Float var1 = (float)this.Condition / (float)this.ConditionMax;
      return Float.valueOf(var1 * 100.0F);
   }

   public void setColor(Color var1) {
      this.col = var1;
   }

   public Color getColor() {
      return this.col;
   }

   public ColorInfo getColorInfo() {
      return new ColorInfo(this.col.getRedFloat(), this.col.getGreenFloat(), this.col.getBlueFloat(), this.col.getAlphaFloat());
   }

   public boolean isTwoHandWeapon() {
      return this.getScriptItem().TwoHandWeapon;
   }

   public String getCustomMenuOption() {
      return this.customMenuOption;
   }

   public void setCustomMenuOption(String var1) {
      this.customMenuOption = var1;
   }

   public void setTooltip(String var1) {
      this.getModData().rawset("Tooltip", var1);
      this.tooltip = var1;
   }

   public String getTooltip() {
      return this.getModData().rawget("Tooltip") != null ? (String)this.getModData().rawget("Tooltip") : this.tooltip;
   }

   public String getDisplayCategory() {
      return this.displayCategory;
   }

   public void setDisplayCategory(String var1) {
      this.displayCategory = var1;
   }

   public int getHaveBeenRepaired() {
      return this.haveBeenRepaired;
   }

   public void setHaveBeenRepaired(int var1) {
      this.haveBeenRepaired = var1;
   }

   public int getTimesRepaired() {
      return this.haveBeenRepaired;
   }

   public void setTimesRepaired(int var1) {
      this.haveBeenRepaired = var1;
   }

   public void copyTimesRepairedFrom(InventoryItem var1) {
      this.setTimesRepaired(var1.getTimesRepaired());
   }

   public void copyTimesRepairedTo(InventoryItem var1) {
      var1.setTimesRepaired(this.getTimesRepaired());
   }

   public int getTimesHeadRepaired() {
      return this.attrib() != null && this.attrib().contains(Attribute.TimesHeadRepaired) ? this.attrib().get(Attribute.TimesHeadRepaired) : this.haveBeenRepaired;
   }

   public void setTimesHeadRepaired(int var1) {
      if (this.attrib() != null && this.attrib().contains(Attribute.TimesHeadRepaired)) {
         this.attrib().set(Attribute.TimesHeadRepaired, var1);
      } else {
         this.haveBeenRepaired = var1;
      }
   }

   public boolean hasTimesHeadRepaired() {
      return this.attrib() != null && this.attrib().contains(Attribute.TimesHeadRepaired);
   }

   public void copyTimesHeadRepairedFrom(InventoryItem var1) {
      this.setTimesHeadRepaired(var1.getTimesHeadRepaired());
   }

   public void copyTimesHeadRepairedTo(InventoryItem var1) {
      var1.setTimesHeadRepaired(this.getTimesHeadRepaired());
   }

   public boolean isBroken() {
      return this.broken;
   }

   public void setBroken(boolean var1) {
      this.broken = var1;
      if (var1) {
         this.onBreak();
      }

   }

   public String getDisplayName() {
      return this.name;
   }

   public boolean isTrap() {
      return this.getScriptItem().Trap;
   }

   public void addExtraItem(String var1) {
      if (this.extraItems == null) {
         this.extraItems = new ArrayList();
      }

      this.extraItems.add(var1);
   }

   public boolean haveExtraItems() {
      return this.extraItems != null;
   }

   public ArrayList<String> getExtraItems() {
      return this.extraItems;
   }

   public float getExtraItemsWeight() {
      if (!this.haveExtraItems()) {
         return 0.0F;
      } else {
         float var1 = 0.0F;

         for(int var2 = 0; var2 < this.extraItems.size(); ++var2) {
            InventoryItem var3 = InventoryItemFactory.CreateItem((String)this.extraItems.get(var2));
            if (var3 != null && var3.getActualWeight() > 0.0F) {
               var1 += var3.getActualWeight();
            }
         }

         var1 *= 0.6F;
         return var1;
      }
   }

   public boolean isCustomName() {
      return this.customName;
   }

   public void setCustomName(boolean var1) {
      this.customName = var1;
   }

   public boolean isFishingLure() {
      return this.getScriptItem().FishingLure;
   }

   public void copyConditionModData(InventoryItem var1) {
      if (var1.hasModData()) {
         KahluaTableIterator var2 = var1.getModData().iterator();

         while(var2.advance()) {
            if (var2.getKey() instanceof String && ((String)var2.getKey()).startsWith("condition:")) {
               this.getModData().rawset(var2.getKey(), var2.getValue());
            }
         }
      }

   }

   public void setConditionFromModData(InventoryItem var1) {
      if (var1.hasModData()) {
         Object var2 = var1.getModData().rawget("condition:" + this.getType());
         if (var2 != null && var2 instanceof Double) {
            this.setConditionNoSound((int)Math.round((Double)var2 * (double)this.getConditionMax()));
         }
      } else if (!this.hasTag("DontInheritCondition")) {
         this.setConditionFrom(var1);
      }

   }

   public String getBreakSound() {
      return this.breakSound;
   }

   public void setBreakSound(String var1) {
      this.breakSound = var1;
   }

   public String getPlaceOneSound() {
      return this.getScriptItem().getPlaceOneSound();
   }

   public String getPlaceMultipleSound() {
      return this.getScriptItem().getPlaceMultipleSound();
   }

   public String getSoundByID(String var1) {
      return this.getScriptItem().getSoundByID(var1);
   }

   public void setBeingFilled(boolean var1) {
      this.beingFilled = var1;
   }

   public boolean isBeingFilled() {
      return this.beingFilled;
   }

   public String getFillFromDispenserSound() {
      return this.getScriptItem().getFillFromDispenserSound();
   }

   public String getFillFromLakeSound() {
      return this.getScriptItem().getFillFromLakeSound();
   }

   public String getFillFromTapSound() {
      return this.getScriptItem().getFillFromTapSound();
   }

   public String getFillFromToiletSound() {
      return this.getScriptItem().getFillFromToiletSound();
   }

   public String getPourLiquidOnGroundSound() {
      if (StringUtils.equalsIgnoreCase(this.getPourType(), "Bucket") && this.hasTag("HasMetal")) {
         return "PourLiquidOnGroundMetal";
      } else {
         return StringUtils.equalsIgnoreCase(this.getPourType(), "Pot") ? "PourLiquidOnGroundMetal" : "PourLiquidOnGround";
      }
   }

   public boolean isAlcoholic() {
      return this.alcoholic;
   }

   public void setAlcoholic(boolean var1) {
      this.alcoholic = var1;
   }

   public float getAlcoholPower() {
      return this.alcoholPower;
   }

   public void setAlcoholPower(float var1) {
      this.alcoholPower = var1;
   }

   public float getBandagePower() {
      return this.bandagePower;
   }

   public void setBandagePower(float var1) {
      this.bandagePower = var1;
   }

   public float getReduceInfectionPower() {
      if (this.Burnt) {
         return (float)((int)(this.ReduceInfectionPower / 3.0F));
      } else if (this.Age >= (float)this.OffAge && this.Age < (float)this.OffAgeMax) {
         return (float)((int)(this.ReduceInfectionPower / 1.3F));
      } else if (this.Age >= (float)this.OffAgeMax) {
         return (float)((int)(this.ReduceInfectionPower / 2.2F));
      } else {
         return this.isCooked() ? this.ReduceInfectionPower * 1.3F : this.ReduceInfectionPower;
      }
   }

   public void setReduceInfectionPower(float var1) {
      this.ReduceInfectionPower = var1;
   }

   public final void saveWithSize(ByteBuffer var1, boolean var2) throws IOException {
      int var3 = var1.position();
      var1.putInt(0);
      int var4 = var1.position();
      this.save(var1, var2);
      int var5 = var1.position();
      var1.position(var3);
      var1.putInt(var5 - var4);
      var1.position(var5);
   }

   public boolean isCustomWeight() {
      return this.customWeight;
   }

   public void setCustomWeight(boolean var1) {
      this.customWeight = var1;
   }

   public float getContentsWeight() {
      if (!StringUtils.isNullOrEmpty(this.getAmmoType())) {
         Item var1 = ScriptManager.instance.FindItem(this.getAmmoType());
         if (var1 != null) {
            return var1.getActualWeight() * (float)this.getCurrentAmmoCount();
         }
      }

      return this.getFluidContainer() != null ? this.getFluidContainer().getAmount() : 0.0F;
   }

   public float getHotbarEquippedWeight() {
      return this.hasTag("LightWhenAttached") ? (this.getActualWeight() + this.getContentsWeight()) * 0.3F : (this.getActualWeight() + this.getContentsWeight()) * 0.7F;
   }

   public float getEquippedWeight() {
      return (this.getActualWeight() + this.getContentsWeight()) * 0.3F;
   }

   public float getUnequippedWeight() {
      return this.getActualWeight() + this.getContentsWeight();
   }

   public boolean isEquipped() {
      if (this.getContainer() == null) {
         return false;
      } else {
         IsoObject var2 = this.getContainer().getParent();
         if (var2 instanceof IsoGameCharacter) {
            IsoGameCharacter var3 = (IsoGameCharacter)var2;
            return var3.isEquipped(this);
         } else {
            var2 = this.getContainer().getParent();
            if (var2 instanceof IsoDeadBody) {
               IsoDeadBody var1 = (IsoDeadBody)var2;
               return var1.isEquipped(this);
            } else {
               return false;
            }
         }
      }
   }

   public IsoGameCharacter getUser() {
      return this.getContainer() != null && this.getContainer().getParent() instanceof IsoGameCharacter && ((IsoGameCharacter)this.getContainer().getParent()).isEquipped(this) ? (IsoGameCharacter)this.getContainer().getParent() : null;
   }

   public IsoGameCharacter getOwner() {
      return this.getContainer() != null && this.getContainer().getParent() instanceof IsoGameCharacter ? (IsoGameCharacter)this.getContainer().getParent() : null;
   }

   public int getKeyId() {
      return this.keyId;
   }

   public void setKeyId(int var1) {
      this.keyId = var1;
   }

   public boolean isRemoteController() {
      return this.remoteController;
   }

   public void setRemoteController(boolean var1) {
      this.remoteController = var1;
   }

   public boolean canBeRemote() {
      return this.canBeRemote;
   }

   public void setCanBeRemote(boolean var1) {
      this.canBeRemote = var1;
   }

   public int getRemoteControlID() {
      return this.remoteControlID;
   }

   public void setRemoteControlID(int var1) {
      this.remoteControlID = var1;
   }

   public int getRemoteRange() {
      return this.remoteRange;
   }

   public void setRemoteRange(int var1) {
      this.remoteRange = var1;
   }

   public String getExplosionSound() {
      return this.explosionSound;
   }

   public void setExplosionSound(String var1) {
      this.explosionSound = var1;
   }

   public String getCountDownSound() {
      return this.countDownSound;
   }

   public void setCountDownSound(String var1) {
      this.countDownSound = var1;
   }

   public float getColorRed() {
      return this.colorRed;
   }

   public void setColorRed(float var1) {
      this.colorRed = var1;
   }

   public float getColorGreen() {
      return this.colorGreen;
   }

   public void setColorGreen(float var1) {
      this.colorGreen = var1;
   }

   public float getColorBlue() {
      return this.colorBlue;
   }

   public void setColorBlue(float var1) {
      this.colorBlue = var1;
   }

   public String getEvolvedRecipeName() {
      return this.evolvedRecipeName;
   }

   public void setEvolvedRecipeName(String var1) {
      this.evolvedRecipeName = var1;
   }

   public float getMetalValue() {
      return this.metalValue;
   }

   public void setMetalValue(float var1) {
      this.metalValue = var1;
   }

   public float getItemHeat() {
      return this.itemHeat;
   }

   public void setItemHeat(float var1) {
      if (var1 > 3.0F) {
         var1 = 3.0F;
      }

      if (var1 < 0.0F) {
         var1 = 0.0F;
      }

      this.itemHeat = var1;
   }

   public float getInvHeat() {
      return 1.0F - this.itemHeat;
   }

   public float getMeltingTime() {
      return this.meltingTime;
   }

   public void setMeltingTime(float var1) {
      if (var1 > 100.0F) {
         var1 = 100.0F;
      }

      if (var1 < 0.0F) {
         var1 = 0.0F;
      }

      this.meltingTime = var1;
   }

   public String getWorker() {
      return this.worker;
   }

   public void setWorker(String var1) {
      this.worker = var1;
   }

   public int getID() {
      return this.id;
   }

   public void setID(int var1) {
      this.id = var1;
   }

   public boolean isWet() {
      return this.isWet;
   }

   public void setWet(boolean var1) {
      this.isWet = var1;
   }

   public float getWetCooldown() {
      return this.wetCooldown;
   }

   public void setWetCooldown(float var1) {
      this.wetCooldown = var1;
   }

   public String getItemWhenDry() {
      return this.itemWhenDry;
   }

   public void setItemWhenDry(String var1) {
      this.itemWhenDry = var1;
   }

   public boolean isFavorite() {
      return this.favorite;
   }

   public void setFavorite(boolean var1) {
      this.favorite = var1;
   }

   public ArrayList<String> getRequireInHandOrInventory() {
      return this.requireInHandOrInventory;
   }

   public void setRequireInHandOrInventory(ArrayList<String> var1) {
      this.requireInHandOrInventory = var1;
   }

   public boolean isCustomColor() {
      return this.customColor;
   }

   public void setCustomColor(boolean var1) {
      this.customColor = var1;
   }

   public void doBuildingStash() {
      if (this.stashMap != null) {
         if (GameClient.bClient) {
            INetworkPacket.send(PacketTypes.PacketType.ReadAnnotedMap, this.stashMap);
         } else {
            StashSystem.prepareBuildingStash(this.stashMap);
         }
      }

   }

   public void setStashMap(String var1) {
      this.stashMap = var1;
   }

   public String getStashMap() {
      return this.stashMap;
   }

   public int getMechanicType() {
      return this.getScriptItem().vehicleType;
   }

   public float getItemCapacity() {
      return this.itemCapacity;
   }

   public void setItemCapacity(float var1) {
      this.itemCapacity = var1;
   }

   public int getMaxCapacity() {
      return this.maxCapacity;
   }

   public void setMaxCapacity(int var1) {
      this.maxCapacity = var1;
   }

   public boolean isConditionAffectsCapacity() {
      return this.ScriptItem != null && this.ScriptItem.isConditionAffectsCapacity();
   }

   public float getBrakeForce() {
      return this.brakeForce;
   }

   public void setBrakeForce(float var1) {
      this.brakeForce = var1;
   }

   public float getDurability() {
      return this.durability;
   }

   public void setDurability(float var1) {
      this.durability = var1;
   }

   public int getChanceToSpawnDamaged() {
      return this.chanceToSpawnDamaged;
   }

   public void setChanceToSpawnDamaged(int var1) {
      this.chanceToSpawnDamaged = var1;
   }

   public float getConditionLowerNormal() {
      return this.conditionLowerNormal;
   }

   public void setConditionLowerNormal(float var1) {
      this.conditionLowerNormal = var1;
   }

   public float getConditionLowerOffroad() {
      return this.conditionLowerOffroad;
   }

   public void setConditionLowerOffroad(float var1) {
      this.conditionLowerOffroad = var1;
   }

   public float getWheelFriction() {
      return this.wheelFriction;
   }

   public void setWheelFriction(float var1) {
      this.wheelFriction = var1;
   }

   public float getSuspensionDamping() {
      return this.suspensionDamping;
   }

   public void setSuspensionDamping(float var1) {
      this.suspensionDamping = var1;
   }

   public float getSuspensionCompression() {
      return this.suspensionCompression;
   }

   public void setSuspensionCompression(float var1) {
      this.suspensionCompression = var1;
   }

   public void setInfected(boolean var1) {
      this.zombieInfected = var1;
   }

   public boolean isInfected() {
      return this.zombieInfected;
   }

   public float getEngineLoudness() {
      return this.engineLoudness;
   }

   public void setEngineLoudness(float var1) {
      this.engineLoudness = var1;
   }

   public String getStaticModel() {
      if (this.getModData().rawget("staticModel") != null) {
         return (String)this.getModData().rawget("staticModel");
      } else {
         return this.modelIndex != -1 && this.getStaticModelsByIndex() != null ? (String)this.getStaticModelsByIndex().get(this.modelIndex) : this.getScriptItem().getStaticModel();
      }
   }

   public void setStaticModel(String var1) {
      this.getModData().rawset("staticModel", var1);
   }

   public String getStaticModelException() {
      return this.hasTag("UseWorldStaticModel") ? this.getWorldStaticModel() : this.getStaticModel();
   }

   public ArrayList<String> getIconsForTexture() {
      return this.iconsForTexture;
   }

   public void setIconsForTexture(ArrayList<String> var1) {
      this.iconsForTexture = var1;
   }

   public float getScore(SurvivorDesc var1) {
      return 0.0F;
   }

   public IsoGameCharacter getPreviousOwner() {
      return this.previousOwner;
   }

   public void setPreviousOwner(IsoGameCharacter var1) {
      this.previousOwner = var1;
   }

   public Item getScriptItem() {
      return this.ScriptItem;
   }

   public void setScriptItem(Item var1) {
      this.ScriptItem = var1;
   }

   public ItemType getCat() {
      return this.cat;
   }

   public void setCat(ItemType var1) {
      this.cat = var1;
   }

   public ItemContainer getContainer() {
      return this.container;
   }

   public void setContainer(ItemContainer var1) {
      this.container = var1;
   }

   public ArrayList<BloodClothingType> getBloodClothingType() {
      return this.bloodClothingType;
   }

   public void setBloodClothingType(ArrayList<BloodClothingType> var1) {
      this.bloodClothingType = var1;
   }

   public void setBlood(BloodBodyPartType var1, float var2) {
      ItemVisual var3 = this.getVisual();
      if (var3 != null) {
         var3.setBlood(var1, var2);
      }

   }

   public float getBlood(BloodBodyPartType var1) {
      ItemVisual var2 = this.getVisual();
      return var2 != null ? var2.getBlood(var1) : 0.0F;
   }

   public void setDirt(BloodBodyPartType var1, float var2) {
      ItemVisual var3 = this.getVisual();
      if (var3 != null) {
         var3.setDirt(var1, var2);
      }

   }

   public float getDirt(BloodBodyPartType var1) {
      ItemVisual var2 = this.getVisual();
      return var2 != null ? var2.getDirt(var1) : 0.0F;
   }

   public String getClothingItemName() {
      return this.getScriptItem().ClothingItem;
   }

   public int getStashChance() {
      return this.stashChance;
   }

   public void setStashChance(int var1) {
      this.stashChance = var1;
   }

   public String getEatType() {
      return this.getScriptItem().eatType;
   }

   public String getPourType() {
      return this.getScriptItem().pourType;
   }

   public boolean isUseWorldItem() {
      return this.getScriptItem().UseWorldItem;
   }

   public String getAmmoType() {
      return this.ammoType;
   }

   public void setAmmoType(String var1) {
      this.ammoType = var1;
   }

   public int getMaxAmmo() {
      return this.maxAmmo;
   }

   public void setMaxAmmo(int var1) {
      this.maxAmmo = var1;
   }

   public int getCurrentAmmoCount() {
      return this.currentAmmoCount;
   }

   public void setCurrentAmmoCount(int var1) {
      this.currentAmmoCount = var1;
   }

   public String getGunType() {
      return this.gunType;
   }

   public void setGunType(String var1) {
      this.gunType = var1;
   }

   public boolean hasBlood() {
      if (this instanceof Clothing) {
         if (this.getBloodClothingType() == null || this.getBloodClothingType().isEmpty()) {
            return false;
         }

         ArrayList var1 = BloodClothingType.getCoveredParts(this.getBloodClothingType());
         if (var1 == null) {
            return false;
         }

         for(int var2 = 0; var2 < var1.size(); ++var2) {
            if (this.getBlood((BloodBodyPartType)var1.get(var2)) > 0.0F) {
               return true;
            }
         }
      } else {
         if (this instanceof HandWeapon) {
            return ((HandWeapon)this).getBloodLevel() > 0.0F;
         }

         if (this instanceof InventoryContainer) {
            return ((InventoryContainer)this).getBloodLevel() > 0.0F;
         }
      }

      return false;
   }

   public boolean hasDirt() {
      if (this instanceof Clothing) {
         if (this.getBloodClothingType() == null || this.getBloodClothingType().isEmpty()) {
            return false;
         }

         ArrayList var1 = BloodClothingType.getCoveredParts(this.getBloodClothingType());
         if (var1 == null) {
            return false;
         }

         for(int var2 = 0; var2 < var1.size(); ++var2) {
            if (this.getDirt((BloodBodyPartType)var1.get(var2)) > 0.0F) {
               return true;
            }
         }
      }

      return false;
   }

   public String getAttachmentType() {
      return this.attachmentType;
   }

   public void setAttachmentType(String var1) {
      this.attachmentType = var1;
   }

   public int getAttachedSlot() {
      return this.attachedSlot;
   }

   public void setAttachedSlot(int var1) {
      this.attachedSlot = var1;
   }

   public ArrayList<String> getAttachmentsProvided() {
      return this.attachmentsProvided;
   }

   public void setAttachmentsProvided(ArrayList<String> var1) {
      this.attachmentsProvided = var1;
   }

   public String getAttachedSlotType() {
      return this.attachedSlotType;
   }

   public void setAttachedSlotType(String var1) {
      this.attachedSlotType = var1;
   }

   public String getAttachmentReplacement() {
      return this.attachmentReplacement;
   }

   public void setAttachmentReplacement(String var1) {
      this.attachmentReplacement = var1;
   }

   public String getAttachedToModel() {
      return this.attachedToModel;
   }

   public void setAttachedToModel(String var1) {
      this.attachedToModel = var1;
   }

   public String getFabricType() {
      return this.getScriptItem().fabricType;
   }

   public String getStringItemType() {
      Item var1 = ScriptManager.instance.FindItem(this.getFullType());
      if (var1 != null && var1.getType() != null) {
         if (var1.getType() == Item.Type.Food) {
            return var1.CannedFood ? "CannedFood" : "Food";
         } else if ("Ammo".equals(var1.getDisplayCategory())) {
            return "Ammo";
         } else if (var1.getType() == Item.Type.Weapon && !var1.isRanged()) {
            return "MeleeWeapon";
         } else if (var1.getType() != Item.Type.WeaponPart && (var1.getType() != Item.Type.Weapon || !var1.isRanged()) && (var1.getType() != Item.Type.Normal || StringUtils.isNullOrEmpty(var1.getAmmoType()))) {
            if (var1.getType() == Item.Type.Literature) {
               return "Literature";
            } else if (var1.Medical) {
               return "Medical";
            } else if (var1.SurvivalGear) {
               return "SurvivalGear";
            } else {
               return var1.MechanicsItem ? "Mechanic" : "Other";
            }
         } else {
            return "RangedWeapon";
         }
      } else {
         return "Other";
      }
   }

   public boolean isProtectFromRainWhileEquipped() {
      return this.getScriptItem().ProtectFromRainWhenEquipped;
   }

   public boolean isEquippedNoSprint() {
      return this.getScriptItem().equippedNoSprint;
   }

   public String getBodyLocation() {
      return this.getScriptItem().BodyLocation;
   }

   public String getMakeUpType() {
      return this.getScriptItem().makeUpType;
   }

   public boolean isHidden() {
      return this.getScriptItem().isHidden();
   }

   public String getConsolidateOption() {
      return this.getScriptItem().consolidateOption;
   }

   public ArrayList<String> getClothingItemExtra() {
      return this.getScriptItem().clothingItemExtra;
   }

   public ArrayList<String> getClothingItemExtraOption() {
      return this.getScriptItem().clothingItemExtraOption;
   }

   public String getWorldStaticItem() {
      if (this.getModData().rawget("Flatpack") == "true") {
         return "Flatpack";
      } else if (this.getModData().rawget("worldStaticModel") != null) {
         return (String)this.getModData().rawget("worldStaticModel");
      } else {
         String var1 = this.tryGetWorldStaticModelByIndex(this.getModelIndex());
         return var1 != null ? var1 : this.getScriptItem().worldStaticModel;
      }
   }

   public String getWorldStaticModel() {
      return this.getWorldStaticItem();
   }

   public void setWorldStaticItem(String var1) {
      this.getModData().rawset("worldStaticModel", var1);
   }

   public void setWorldStaticModel(String var1) {
      this.setWorldStaticItem(var1);
   }

   public void setRegistry_id(Item var1) {
      if (var1.getFullName().equals(this.getFullType())) {
         this.registry_id = var1.getRegistry_id();
      } else if (Core.bDebug) {
         WorldDictionary.DebugPrintItem(var1);
         throw new RuntimeException("These types should always match");
      }

   }

   public short getRegistry_id() {
      return this.registry_id;
   }

   public String getModID() {
      return this.ScriptItem != null && this.ScriptItem.getModID() != null ? this.ScriptItem.getModID() : WorldDictionary.getItemModID(this.registry_id);
   }

   public String getModName() {
      return WorldDictionary.getModNameFromID(this.getModID());
   }

   public boolean isVanilla() {
      if (this.getModID() != null) {
         return this.getModID().equals("pz-vanilla");
      } else if (Core.bDebug) {
         WorldDictionary.DebugPrintItem(this);
         throw new RuntimeException("Item has no modID?");
      } else {
         return true;
      }
   }

   public short getRecordedMediaIndex() {
      return this.recordedMediaIndex;
   }

   public void setRecordedMediaIndex(short var1) {
      this.recordedMediaIndex = var1;
      if (this.recordedMediaIndex >= 0) {
         MediaData var2 = ZomboidRadio.getInstance().getRecordedMedia().getMediaDataFromIndex(this.recordedMediaIndex);
         this.mediaType = -1;
         if (var2 != null) {
            this.name = var2.getTranslatedItemDisplayName();
            this.mediaType = var2.getMediaType();
         } else {
            this.recordedMediaIndex = -1;
         }
      } else {
         this.mediaType = -1;
         this.name = this.getScriptItem().getDisplayName();
      }

   }

   public void setRecordedMediaIndexInteger(int var1) {
      this.setRecordedMediaIndex((short)var1);
   }

   public boolean isRecordedMedia() {
      return this.recordedMediaIndex >= 0;
   }

   public MediaData getMediaData() {
      return this.isRecordedMedia() ? ZomboidRadio.getInstance().getRecordedMedia().getMediaDataFromIndex(this.recordedMediaIndex) : null;
   }

   public byte getMediaType() {
      return this.mediaType;
   }

   public void setMediaType(byte var1) {
      this.mediaType = var1;
   }

   public void setRecordedMediaData(MediaData var1) {
      if (var1 != null && var1.getIndex() >= 0) {
         this.setRecordedMediaIndex(var1.getIndex());
      }

   }

   public void setWorldZRotation(int var1) {
      this.worldZRotation = var1;
   }

   public void randomizeWorldZRotation() {
      this.worldZRotation = Rand.Next(360);
   }

   public void setWorldScale(float var1) {
      this.worldScale = var1;
   }

   public String getLuaCreate() {
      return this.getScriptItem().getLuaCreate();
   }

   public boolean isInitialised() {
      return this.isInitialised;
   }

   public void setInitialised(boolean var1) {
      this.isInitialised = var1;
   }

   public void initialiseItem() {
      this.setInitialised(true);
      if (this.getLuaCreate() != null) {
         Object var1 = LuaManager.getFunctionObject(this.getLuaCreate());
         if (var1 != null) {
            LuaManager.caller.protectedCallVoid(LuaManager.thread, var1, this);
         }
      }

   }

   public String getMilkReplaceItem() {
      return this.getScriptItem().MilkReplaceItem;
   }

   public int getMaxMilk() {
      return this.getScriptItem().MaxMilk;
   }

   public boolean isAnimalFeed() {
      return !StringUtils.isNullOrEmpty(this.getScriptItem().AnimalFeedType);
   }

   public String getAnimalFeedType() {
      return this.getScriptItem().AnimalFeedType;
   }

   public String getDigType() {
      return this.getScriptItem().digType;
   }

   public String getSoundParameter(String var1) {
      return this.getScriptItem().getSoundParameter(var1);
   }

   public boolean isWorn() {
      return this.IsClothing() && this.isWorn();
   }

   public void reset() {
      super.reset();
   }

   public String toString() {
      String var10000 = this.getFullType();
      return var10000 + ":" + super.toString();
   }

   public Texture getTextureColorMask() {
      return this.textureColorMask;
   }

   public Texture getTextureFluidMask() {
      return this.textureFluidMask;
   }

   public void setTextureColorMask(String var1) {
      this.textureColorMask = Texture.trygetTexture(var1);
      if (this.textureColorMask == null) {
         this.textureColorMask = Texture.getSharedTexture("media/inventory/Question_On.png");
      }

   }

   public void setTextureFluidMask(String var1) {
      this.textureFluidMask = Texture.trygetTexture(var1);
      if (this.textureFluidMask == null) {
         this.textureFluidMask = Texture.getSharedTexture("media/inventory/Question_On.png");
      }

   }

   public IsoGridSquare getSquare() {
      return this.equipParent != null ? this.equipParent.getSquare() : null;
   }

   public GameEntityType getGameEntityType() {
      return GameEntityType.InventoryItem;
   }

   public long getEntityNetID() {
      return (long)this.id;
   }

   public float getX() {
      return this.equipParent != null ? this.equipParent.getX() : 3.4028235E38F;
   }

   public float getY() {
      return this.equipParent != null ? this.equipParent.getY() : 3.4028235E38F;
   }

   public float getZ() {
      return this.equipParent != null ? this.equipParent.getZ() : 3.4028235E38F;
   }

   public boolean isEntityValid() {
      return this.getEquipParent() != null;
   }

   public static boolean RemoveFromContainer(InventoryItem var0) {
      ItemContainer var1 = var0.getContainer();
      if (var1 != null) {
         if (var1.getType().equals("floor") && var0.getWorldItem() != null && var0.getWorldItem().getSquare() != null) {
            var0.getWorldItem().getSquare().transmitRemoveItemFromSquare(var0.getWorldItem());
            var0.getWorldItem().getSquare().getWorldObjects().remove(var0.getWorldItem());
            var0.getWorldItem().getSquare().getObjects().remove(var0.getWorldItem());
            var0.setWorldItem((IsoWorldInventoryObject)null);
         }

         var1.DoRemoveItem(var0);
         return true;
      } else {
         return false;
      }
   }

   public AnimalTracks getAnimalTracks() {
      return this.animalTracks;
   }

   public void setAnimalTracks(AnimalTracks var1) {
      this.animalTracks = var1;
   }

   public void syncItemFields() {
      ItemContainer var1 = this.getOutermostContainer();
      if (var1 != null && var1.getParent() instanceof IsoPlayer) {
         if (GameClient.bClient) {
            INetworkPacket.send(PacketTypes.PacketType.SyncItemFields, var1.getParent(), this);
         } else if (GameServer.bServer) {
            INetworkPacket.send((IsoPlayer)var1.getParent(), PacketTypes.PacketType.SyncItemFields, var1.getParent(), this);
         }
      }

   }

   public String getWithDrainable() {
      return this.getScriptItem().getWithDrainable();
   }

   public String getWithoutDrainable() {
      return this.getScriptItem().getWithoutDrainable();
   }

   public ArrayList<String> getStaticModelsByIndex() {
      return this.staticModelsByIndex;
   }

   public void setStaticModelsByIndex(ArrayList<String> var1) {
      this.staticModelsByIndex = var1;
   }

   public ArrayList<String> getWorldStaticModelsByIndex() {
      return this.worldStaticModelsByIndex;
   }

   public void setWorldStaticModelsByIndex(ArrayList<String> var1) {
      this.worldStaticModelsByIndex = var1;
   }

   public String tryGetWorldStaticModelByIndex(int var1) {
      ArrayList var2 = this.getWorldStaticModelsByIndex();
      return var2 != null && var1 >= 0 && var1 < var2.size() ? (String)var2.get(var1) : null;
   }

   public int getModelIndex() {
      return this.modelIndex;
   }

   public void setModelIndex(int var1) {
      this.modelIndex = var1;
   }

   public float getVisionModifier() {
      return this.getScriptItem().getVisionModifier();
   }

   public float getHearingModifier() {
      return this.getScriptItem().getHearingModifier();
   }

   public String getWorldObjectSprite() {
      return this.getScriptItem().getWorldObjectSprite();
   }

   public float getStrainModifier() {
      return this.getScriptItem().getStrainModifier();
   }

   public int getConditionLowerChance() {
      return this.getScriptItem().getConditionLowerChance();
   }

   public void setConditionFrom(InventoryItem var1) {
      if (var1 != null) {
         if (this.hasSharpness() && var1.hasSharpness()) {
            this.setSharpness(var1.getSharpness());
         }

         if (this.getConditionMax() == var1.getConditionMax()) {
            this.setConditionNoSound(var1.getCondition());
         } else {
            float var2 = (float)var1.getCondition() / (float)var1.getConditionMax();
            this.setConditionNoSound((int)((float)this.getConditionMax() * var2));
         }
      }
   }

   public void setConditionTo(InventoryItem var1) {
      if (var1 != null) {
         var1.setConditionFrom(this);
      }
   }

   public void reduceCondition() {
      this.setCondition(this.getCondition() - 1);
   }

   public boolean damageCheck() {
      return this.damageCheck(0, 1.0F);
   }

   public boolean damageCheck(int var1) {
      return this.damageCheck(var1, 1.0F);
   }

   public boolean damageCheck(int var1, float var2) {
      return this.damageCheck(var1, var2, true);
   }

   public boolean damageCheck(int var1, float var2, boolean var3) {
      return this.damageCheck(var1, var2, var3, true);
   }

   public boolean damageCheck(int var1, float var2, boolean var3, boolean var4) {
      return this.damageCheck(var1, var2, var3, var4, (IsoGameCharacter)null);
   }

   public boolean damageCheck(int var1, float var2, boolean var3, boolean var4, IsoGameCharacter var5) {
      var2 = Math.max(var2, 0.0F);
      if (var3) {
         var1 += this.getMaintenanceMod(var4, var5);
      }

      boolean var6 = this.sharpnessCheck(var1 / 2, var2 / 2.0F, false, var4);
      if (this.headConditionCheck(var1, var2, false, var4)) {
         var6 = true;
      }

      if (Rand.NextBool((int)((float)this.getConditionLowerChance() * var2 + (float)var1))) {
         this.reduceCondition();
         return true;
      } else {
         return var6;
      }
   }

   public boolean sharpnessCheck() {
      return this.sharpnessCheck(0);
   }

   public boolean sharpnessCheck(int var1) {
      return this.sharpnessCheck(var1, 1.0F);
   }

   public boolean sharpnessCheck(int var1, float var2) {
      return this.sharpnessCheck(var1, var2, true);
   }

   public boolean sharpnessCheck(int var1, float var2, boolean var3) {
      return this.sharpnessCheck(var1, var2, var3, true);
   }

   public boolean sharpnessCheck(int var1, float var2, boolean var3, boolean var4) {
      return this.sharpnessCheck(var1, var2, var3, var4, (IsoGameCharacter)null);
   }

   public boolean sharpnessCheck(int var1, float var2, boolean var3, boolean var4, IsoGameCharacter var5) {
      if (!this.hasSharpness()) {
         return false;
      } else {
         var2 = Math.max(var2, 0.0F);
         int var6 = 0;
         if (var3) {
            var6 += this.getMaintenanceMod(var4, var5);
         }

         if (Rand.NextBool((int)((float)this.getConditionLowerChance() * var2 + (float)(var1 + var6)))) {
            this.reduceSharpness();
            return true;
         } else {
            return false;
         }
      }
   }

   public void reduceSharpness() {
      if (this.hasSharpness()) {
         if (this.getSharpness() <= 0.0F) {
            if (this.hasHeadCondition()) {
               this.reduceHeadCondition();
            } else {
               this.reduceCondition();
            }

         } else {
            this.setSharpness(this.getSharpness() - this.getSharpnessIncrement());
         }
      }
   }

   public boolean hasSharpness() {
      return this.attrib() != null && this.attrib().getAttribute(Attribute.Sharpness) != null;
   }

   public float getSharpness() {
      if (!this.hasSharpness()) {
         return 0.0F;
      } else {
         if (this.attrib().getAttribute(Attribute.Sharpness).getFloatValue() > this.getMaxSharpness()) {
            this.applyMaxSharpness();
         }

         return this.attrib().getAttribute(Attribute.Sharpness).getFloatValue();
      }
   }

   public float getMaxSharpness() {
      if (!this.hasSharpness()) {
         return 1.0F;
      } else {
         return this.hasHeadCondition() ? (float)this.getHeadCondition() / (float)this.getHeadConditionMax() : (float)this.getCondition() / (float)this.getConditionMax();
      }
   }

   public void applyMaxSharpness() {
      if (this.hasSharpness()) {
         this.setSharpness(this.getMaxSharpness());
      }
   }

   public float getSharpnessMultiplier() {
      return !this.hasSharpness() ? 1.0F : (this.attrib().getAttribute(Attribute.Sharpness).getFloatValue() + 1.0F) / 2.0F;
   }

   public void setSharpness(float var1) {
      if (this.hasSharpness()) {
         float var2 = this.getMaxSharpness();
         if (var1 > var2) {
            var1 = var2;
         }

         if (var1 < 0.0F) {
            var1 = 0.0F;
         }

         if (var1 > 1.0F) {
            var1 = 1.0F;
         }

         String var3 = String.valueOf(var1);
         this.attrib().getAttribute(Attribute.Sharpness).setValueFromScriptString(var3);
      }
   }

   public void setSharpnessFrom(InventoryItem var1) {
      if (this.hasSharpness() && var1.hasSharpness()) {
         this.setSharpness(var1.getSharpness());
      }
   }

   public float getSharpnessIncrement() {
      return !this.hasSharpness() ? 0.0F : 1.0F / (float)this.getConditionMax();
   }

   public boolean isDamaged() {
      return this.getCondition() < this.getConditionMax();
   }

   public boolean isDull() {
      return this.hasSharpness() && this.getSharpness() <= this.getMaxSharpness() / 3.0F;
   }

   public int getMaintenanceMod() {
      return this.getMaintenanceMod(true);
   }

   public int getMaintenanceMod(boolean var1) {
      return this.getMaintenanceMod(var1, (IsoGameCharacter)null);
   }

   public int getMaintenanceMod(IsoGameCharacter var1) {
      return this.getMaintenanceMod(false, var1);
   }

   public int getMaintenanceMod(boolean var1, IsoGameCharacter var2) {
      if (var1 && !this.isEquipped()) {
         return 0;
      } else {
         if (var1 && var2 == null) {
            var2 = this.getUser();
         } else if (var2 == null) {
            var2 = this.getOwner();
         }

         if (var2 == null) {
            return 0;
         } else {
            int var3 = var2.getPerkLevel(PerkFactory.Perks.Maintenance);
            if (this instanceof HandWeapon) {
               var3 += var2.getWeaponLevel((HandWeapon)this) / 2;
            }

            return var3;
         }
      }
   }

   public int getWeaponLevel() {
      if (this.isEquipped() && this instanceof HandWeapon) {
         WeaponType var1 = WeaponType.getWeaponType((HandWeapon)this);
         int var2 = -1;
         HandWeapon var3 = (HandWeapon)this;
         if (var1 != null && var1 != WeaponType.barehand) {
            if (var3.getCategories().contains("Axe")) {
               var2 = this.getUser().getPerkLevel(PerkFactory.Perks.Axe);
            }

            if (var3.getCategories().contains("Spear")) {
               var2 += this.getUser().getPerkLevel(PerkFactory.Perks.Spear);
            }

            if (var3.getCategories().contains("SmallBlade")) {
               var2 += this.getUser().getPerkLevel(PerkFactory.Perks.SmallBlade);
            }

            if (var3.getCategories().contains("LongBlade")) {
               var2 += this.getUser().getPerkLevel(PerkFactory.Perks.LongBlade);
            }

            if (var3.getCategories().contains("Blunt")) {
               var2 += this.getUser().getPerkLevel(PerkFactory.Perks.Blunt);
            }

            if (var3.getCategories().contains("SmallBlunt")) {
               var2 += this.getUser().getPerkLevel(PerkFactory.Perks.SmallBlunt);
            }
         }

         if (var2 > 10) {
            var2 = 10;
         }

         return var2 == -1 ? 0 : var2;
      } else {
         return 0;
      }
   }

   public boolean headConditionCheck() {
      return this.headConditionCheck(0, 1.0F);
   }

   public boolean headConditionCheck(int var1) {
      return this.headConditionCheck(var1, 1.0F);
   }

   public boolean headConditionCheck(int var1, float var2) {
      return this.headConditionCheck(var1, var2, true);
   }

   public boolean headConditionCheck(int var1, float var2, boolean var3) {
      return this.headConditionCheck(var1, var2, var3, true);
   }

   public boolean headConditionCheck(int var1, float var2, boolean var3, boolean var4) {
      return this.headConditionCheck(var1, var2, var3, var4, (IsoGameCharacter)null);
   }

   public boolean headConditionCheck(int var1, float var2, boolean var3, boolean var4, IsoGameCharacter var5) {
      if (!this.hasHeadCondition()) {
         return false;
      } else {
         var2 = Math.max(var2, 0.0F);
         int var6 = 0;
         if (var3) {
            var6 += this.getMaintenanceMod(var4, var5);
         }

         if (Rand.NextBool((int)((float)this.getHeadConditionLowerChance() * var2 + (float)(var1 + var6)))) {
            this.reduceHeadCondition();
            return true;
         } else {
            return false;
         }
      }
   }

   public int getHeadConditionLowerChance() {
      return (int)((float)this.getConditionLowerChance() * this.getHeadConditionLowerChanceMultiplier());
   }

   public float getHeadConditionLowerChanceMultiplier() {
      return this.getScriptItem().getHeadConditionLowerChanceMultiplier();
   }

   public void reduceHeadCondition() {
      if (this.hasHeadCondition()) {
         DebugLog.log("Reduce Head Condition from " + this.getHeadCondition());
         this.setHeadCondition(this.getHeadCondition() - 1);
      }
   }

   public boolean hasHeadCondition() {
      return this.attrib() != null && this.attrib().getAttribute(Attribute.HeadCondition) != null;
   }

   public int getHeadCondition() {
      return !this.hasHeadCondition() ? 0 : this.attrib().getAttribute(Attribute.HeadCondition).getIntValue();
   }

   public int getHeadConditionMax() {
      if (!this.hasHeadCondition()) {
         return 0;
      } else {
         return this.attrib() != null && this.attrib().getAttribute(Attribute.HeadConditionMax) != null ? this.attrib().getAttribute(Attribute.HeadConditionMax).getIntValue() : this.getConditionMax();
      }
   }

   public void setHeadCondition(int var1) {
      if (this.hasHeadCondition()) {
         if (var1 < 0) {
            var1 = 0;
         }

         int var2 = this.getHeadConditionMax();
         if (var1 > var2) {
            var1 = var2;
         }

         String var3 = String.valueOf(var1);
         this.attrib().getAttribute(Attribute.HeadCondition).setValueFromScriptString(var3);
         if (this.getHeadCondition() <= 0) {
            this.setCondition(0);
         }

      }
   }

   public void setHeadConditionFromCondition(InventoryItem var1) {
      if (var1 != null) {
         if (this.hasHeadCondition()) {
            if (this.getHeadConditionMax() == var1.getConditionMax()) {
               this.setHeadCondition(var1.getCondition());
               if (this.hasSharpness() && var1.hasSharpness()) {
                  this.setSharpness(var1.getSharpness());
               }

            } else {
               float var2 = (float)var1.getCondition() / (float)var1.getConditionMax();
               this.setHeadCondition((int)((float)this.getConditionMax() * var2));
               if (this.hasSharpness() && var1.hasSharpness()) {
                  this.setSharpness(var1.getSharpness());
               }

            }
         }
      }
   }

   public void setConditionFromHeadCondition(InventoryItem var1) {
      if (var1 != null) {
         if (var1.hasHeadCondition()) {
            if (this.getConditionMax() == var1.getHeadConditionMax()) {
               this.setConditionNoSound(var1.getHeadCondition());
               if (this.hasSharpness() && var1.hasSharpness()) {
                  this.setSharpness(var1.getSharpness());
               }

            } else {
               float var2 = (float)var1.getHeadCondition() / (float)var1.getHeadConditionMax();
               this.setConditionNoSound((int)((float)this.getConditionMax() * var2));
               if (this.hasSharpness() && var1.hasSharpness()) {
                  this.setSharpness(var1.getSharpness());
               }

            }
         }
      }
   }

   public boolean hasQuality() {
      return this.attrib() != null && this.attrib().getAttribute(Attribute.Quality) != null;
   }

   public int getQuality() {
      return !this.hasQuality() ? 0 : this.attrib().getAttribute(Attribute.Quality).getIntValue();
   }

   public void setQuality(int var1) {
      if (this.hasQuality()) {
         if (var1 < -50) {
            var1 = -50;
         }

         if (var1 > 50) {
            var1 = 50;
         }

         String var2 = String.valueOf(var1);
         this.attrib().getAttribute(Attribute.Quality).setValueFromScriptString(var2);
      }
   }

   public String getOnBreak() {
      return this.getScriptItem().getOnBreak();
   }

   public void onBreak() {
      Object var1 = LuaManager.getFunctionObject(this.getOnBreak());
      IsoGameCharacter var2 = null;
      if (this.container != null && this.container.parent instanceof IsoGameCharacter) {
         var2 = (IsoGameCharacter)this.container.parent;
      }

      if (var1 != null) {
         LuaManager.caller.pcallvoid(LuaManager.thread, var1, this, var2);
      }

   }

   public float getBloodLevelAdjustedLow() {
      return !(this instanceof Clothing) && !(this instanceof InventoryContainer) ? this.getBloodLevel() : this.getBloodLevel() / 100.0F;
   }

   public float getBloodLevelAdjustedHigh() {
      return !(this instanceof Clothing) && !(this instanceof InventoryContainer) ? this.getBloodLevel() * 100.0F : this.getBloodLevel();
   }

   public float getBloodLevel() {
      return 0.0F;
   }

   public void setBloodLevel(float var1) {
   }

   public void copyBloodLevelFrom(InventoryItem var1) {
      this.setBloodLevel(var1.getBloodLevel());
   }

   public boolean isBloody() {
      return this.getBloodLevel() > 0.25F;
   }

   public String getDamagedSound() {
      return this.getScriptItem() == null ? null : this.getScriptItem().getDamagedSound();
   }

   public String getShoutType() {
      return this.getScriptItem() == null ? null : this.getScriptItem().getShoutType();
   }

   public float getShoutMultiplier() {
      return this.getScriptItem() == null ? 1.0F : this.getScriptItem().getShoutMultiplier();
   }

   public int getEatTime() {
      return this.getScriptItem() == null ? 0 : this.getScriptItem().getEatTime();
   }

   public boolean isVisualAid() {
      return this.getScriptItem().isVisualAid();
   }

   public float getDiscomfortModifier() {
      return this.getScriptItem().getDiscomfortModifier();
   }

   public boolean hasMetal() {
      return this.getMetalValue() > 0.0F || this.hasTag("HasMetal");
   }

   public float getFireFuelRatio() {
      return this.getScriptItem().getFireFuelRatio();
   }

   public float getWetness() {
      return 0.0F;
   }

   public boolean isMemento() {
      return this.hasTag("IsMemento") || Objects.equals(this.getDisplayCategory(), "Memento");
   }

   public void nameAfterDescriptor(SurvivorDesc var1) {
      if (var1 != null) {
         String var2 = this.getScriptItem().getDisplayName();
         var2 = Translator.getText(var2);
         this.setName(var2 + ": " + var1.getForename() + " " + var1.getSurname());
      }
   }

   public void monogramAfterDescriptor(SurvivorDesc var1) {
      if (var1 != null) {
         String var2 = this.getScriptItem().getDisplayName();
         var2 = Translator.getText(var2);
         this.setName(var2 + ": " + var1.getForename().charAt(0) + var1.getSurname().charAt(0));
      }
   }

   public String getLootType() {
      return ItemPickerJava.getLootType(this.getScriptItem());
   }

   public boolean getIsCraftingConsumed() {
      return this.isCraftingConsumed;
   }

   public void setIsCraftingConsumed(boolean var1) {
      this.isCraftingConsumed = var1;
   }

   public void OnAddedToContainer(ItemContainer var1) {
   }

   public void OnBeforeRemoveFromContainer(ItemContainer var1) {
   }

   public IsoDeadBody getDeadBodyObject() {
      return this.deadBodyObject;
   }

   public boolean isPureWater(boolean var1) {
      if (this.getFluidContainer() != null && !this.getFluidContainer().isEmpty()) {
         if (this.getFluidContainer().isPureFluid(Fluid.Water)) {
            return true;
         }

         if (var1 && this.getFluidContainer().isPureFluid(Fluid.TaintedWater)) {
            return true;
         }
      }

      return false;
   }

   public void copyClothing(InventoryItem var1) {
      if (this.getClothingItem() != null && var1.getClothingItem() != null) {
         Object var2 = LuaManager.getFunctionObject("copyClothingItem");
         if (var2 != null) {
            LuaManager.caller.pcallvoid(LuaManager.thread, var2, var1, this);
         }

      }
   }

   public void inheritFoodAgeFrom(InventoryItem var1) {
   }

   public void inheritOlderFoodAge(InventoryItem var1) {
   }

   public boolean isFood() {
      return false;
   }
}
