package zombie.scripting.objects;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameWindow;
import zombie.Lua.LuaManager;
import zombie.characterTextures.BloodClothingType;
import zombie.core.Color;
import zombie.core.Translator;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.population.ClothingItem;
import zombie.core.skinnedmodel.population.OutfitRNG;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
import zombie.debug.DebugLogStream;
import zombie.entity.GameEntityFactory;
import zombie.entity.components.attributes.Attribute;
import zombie.entity.components.attributes.AttributeType;
import zombie.gameStates.GameLoadingState;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemConfigurator;
import zombie.inventory.ItemPickerJava;
import zombie.inventory.types.AlarmClock;
import zombie.inventory.types.AlarmClockClothing;
import zombie.inventory.types.AnimalInventoryItem;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.ComboItem;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.Food;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.InventoryContainer;
import zombie.inventory.types.Key;
import zombie.inventory.types.Literature;
import zombie.inventory.types.MapItem;
import zombie.inventory.types.Moveable;
import zombie.inventory.types.Radio;
import zombie.inventory.types.WeaponPart;
import zombie.iso.objects.IsoBulletTracerEffects;
import zombie.network.GameServer;
import zombie.radio.devices.DeviceData;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptManager;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.scripting.entity.GameEntityScript;
import zombie.scripting.itemConfig.ItemConfig;
import zombie.util.StringUtils;
import zombie.worldMap.MapDefinitions;

public final class Item extends GameEntityScript {
   public String clothingExtraSubmenu = null;
   public String DisplayName = null;
   public boolean Hidden = false;
   public boolean CantEat = false;
   public String Icon = "None";
   public boolean Medical = false;
   public boolean CannedFood = false;
   public boolean SurvivalGear = false;
   public boolean MechanicsItem = false;
   public boolean UseWorldItem = false;
   public float ScaleWorldIcon = 1.0F;
   public String CloseKillMove = null;
   public float WeaponLength = 0.4F;
   public float ActualWeight = 1.0F;
   public float WeightWet = 0.0F;
   public float WeightEmpty = 0.0F;
   public float HungerChange = 0.0F;
   public float CriticalChance = 20.0F;
   public int Count = 1;
   public int DaysFresh = 1000000000;
   public int DaysTotallyRotten = 1000000000;
   public int MinutesToCook = 60;
   public int MinutesToBurn = 120;
   public boolean IsCookable = false;
   private String CookingSound = null;
   public float StressChange = 0.0F;
   public float BoredomChange = 0.0F;
   public float UnhappyChange = 0.0F;
   public boolean AlwaysWelcomeGift = false;
   public String ReplaceOnDeplete = null;
   public boolean Ranged = false;
   public boolean CanStoreWater = false;
   public float MaxRange = 1.0F;
   public float MinRange = 0.0F;
   public float ThirstChange = 0.0F;
   public float FatigueChange = 0.0F;
   public float MinAngle = 1.0F;
   public boolean RequiresEquippedBothHands = false;
   public float MaxDamage = 1.5F;
   public float MinDamage = 0.0F;
   public float MinimumSwingTime = 0.0F;
   public String SwingSound = "BaseballBatSwing";
   public String WeaponSprite;
   public boolean AngleFalloff = false;
   public int SoundVolume = 0;
   public float ToHitModifier = 1.0F;
   public int SoundRadius = 0;
   public float OtherCharacterVolumeBoost;
   public final ArrayList<String> Categories = new ArrayList();
   public final ArrayList<String> Tags = new ArrayList();
   public String ImpactSound = "BaseballBatHit";
   public float SwingTime = 1.0F;
   public boolean KnockBackOnNoDeath = true;
   public boolean SplatBloodOnNoDeath = false;
   public float SwingAmountBeforeImpact = 0.0F;
   public String AmmoType = null;
   public int maxAmmo = 0;
   public String GunType = null;
   public int DoorDamage = 1;
   public int ConditionLowerChance = 10;
   public float HeadConditionLowerChanceMultiplier = 1.0F;
   public int ConditionMax = 10;
   public boolean CanBandage = false;
   public String name;
   public String moduleDotType;
   public int MaxHitCount = 1000;
   public boolean UseSelf = false;
   public boolean OtherHandUse = false;
   public String OtherHandRequire;
   public String PhysicsObject;
   public String SwingAnim = "Rifle";
   public float WeaponWeight = 1.0F;
   public float EnduranceChange = 0.0F;
   public String IdleAnim = "Idle";
   public String RunAnim = "Run";
   public String attachmentType = null;
   public String makeUpType = null;
   public String consolidateOption = null;
   public ArrayList<String> RequireInHandOrInventory = null;
   public String DoorHitSound = "BaseballBatHit";
   public String ReplaceOnUse = null;
   public boolean DangerousUncooked = false;
   public boolean Alcoholic = false;
   public float PushBackMod = 1.0F;
   public int SplatNumber = 2;
   public float NPCSoundBoost = 1.0F;
   public boolean RangeFalloff = false;
   public boolean UseEndurance = true;
   public boolean MultipleHitConditionAffected = true;
   public boolean ShareDamage = true;
   public boolean ShareEndurance = false;
   public boolean CanBarricade = false;
   public boolean UseWhileEquipped = true;
   public boolean UseWhileUnequipped = false;
   public int TicksPerEquipUse = 30;
   public boolean DisappearOnUse = true;
   public float UseDelta = 0.03125F;
   public boolean AlwaysKnockdown = false;
   public float EnduranceMod = 1.0F;
   public float KnockdownMod = 1.0F;
   public boolean CantAttackWithLowestEndurance = false;
   public String ReplaceOnUseOn = null;
   private String ReplaceTypes = null;
   private HashMap<String, String> ReplaceTypesMap = null;
   public ArrayList<String> attachmentsProvided = null;
   public String FoodType = null;
   public boolean Poison = false;
   public Integer PoisonDetectionLevel = null;
   public int PoisonPower = 0;
   public KahluaTable DefaultModData = null;
   public boolean IsAimedFirearm = false;
   public boolean IsAimedHandWeapon = false;
   public boolean CanStack = true;
   public float AimingMod = 1.0F;
   private int ProjectileCount = 1;
   private float projectileSpread = 0.0F;
   private float projectileWeightCenter = 1.0F;
   public float HitAngleMod = 0.0F;
   private float SplatSize = 1.0F;
   private float Temperature = 0.0F;
   public int NumberOfPages = -1;
   public int LvlSkillTrained = -1;
   public int NumLevelsTrained = 1;
   public String SkillTrained = "";
   public int Capacity = 0;
   public float maxItemSize = 0.0F;
   public int WeightReduction = 0;
   public String SubCategory = "";
   public boolean ActivatedItem = false;
   public float LightStrength = 0.0F;
   public boolean TorchCone = false;
   public int LightDistance = 0;
   public String CanBeEquipped = "";
   public boolean TwoHandWeapon = false;
   public String CustomContextMenu = null;
   public String Tooltip = null;
   public List<String> ReplaceOnCooked = null;
   public String DisplayCategory = null;
   public Boolean Trap = false;
   public boolean OBSOLETE = false;
   public boolean FishingLure = false;
   public boolean canBeWrite = false;
   public int AimingPerkCritModifier = 0;
   public float AimingPerkRangeModifier = 0.0F;
   public float AimingPerkHitChanceModifier = 0.0F;
   public int HitChance = 0;
   public float AimingPerkMinAngleModifier = 0.0F;
   public int RecoilDelay = 0;
   public boolean PiercingBullets = false;
   public float SoundGain = 1.0F;
   public boolean ProtectFromRainWhenEquipped = false;
   private float maxRangeModifier = 0.0F;
   public float minSightRange = 0.0F;
   public float maxSightRange = 0.0F;
   public float lowLightBonus = 0.0F;
   private float minRangeRangedModifier = 0.0F;
   private float damageModifier = 0.0F;
   private float recoilDelayModifier = 0.0F;
   private int clipSizeModifier = 0;
   private ArrayList<String> mountOn = null;
   private String partType = null;
   private String canAttachCallback = null;
   private String onAttachCallback = null;
   private String onDetachCallback = null;
   private int ClipSize = 0;
   private int reloadTime = 0;
   private int reloadTimeModifier = 0;
   private int aimingTime = 0;
   private int aimingTimeModifier = 0;
   private int hitChanceModifier = 0;
   private float angleModifier = 0.0F;
   private float projectileSpreadModifier = 0.0F;
   private float weightModifier = 0.0F;
   private int PageToWrite = 0;
   private boolean RemoveNegativeEffectOnCooked = false;
   private int treeDamage = 0;
   private float alcoholPower = 0.0F;
   private String PutInSound = null;
   private String PlaceOneSound = null;
   private String PlaceMultipleSound = null;
   private String OpenSound = null;
   private String CloseSound = null;
   private String breakSound = null;
   private String customEatSound = null;
   private String fillFromDispenserSound = null;
   private String fillFromLakeSound = null;
   private String fillFromTapSound = null;
   private String fillFromToiletSound = null;
   private String bulletOutSound = null;
   private String ShellFallSound = null;
   private String DropSound = null;
   private HashMap<String, String> SoundMap = null;
   private float bandagePower = 0.0F;
   private float ReduceInfectionPower = 0.0F;
   private String OnCooked = null;
   private String OnlyAcceptCategory = null;
   private String AcceptItemFunction = null;
   private boolean padlock = false;
   private boolean digitalPadlock = false;
   private List<String> teachedRecipes = null;
   private int triggerExplosionTimer = 0;
   private boolean canBePlaced = false;
   private int explosionRange = 0;
   private int explosionPower = 0;
   private int fireRange = 0;
   private int firePower = 0;
   private int smokeRange = 0;
   private int noiseRange = 0;
   private int noiseDuration = 0;
   private float extraDamage = 0.0F;
   private int explosionTimer = 0;
   private int explosionDuration = 0;
   private String PlacedSprite = null;
   private boolean canBeReused = false;
   private int sensorRange = 0;
   private boolean canBeRemote = false;
   private boolean remoteController = false;
   private int remoteRange = 0;
   private String countDownSound = null;
   private String explosionSound = null;
   private int fluReduction = 0;
   private int ReduceFoodSickness = 0;
   private int painReduction = 0;
   public float torchDot = 0.96F;
   public int colorRed = 255;
   public int colorGreen = 255;
   public int colorBlue = 255;
   public boolean twoWay = false;
   public int transmitRange = 0;
   public int micRange = 0;
   public float baseVolumeRange = 0.0F;
   public boolean isPortable = false;
   public boolean isTelevision = false;
   public int minChannel = 88000;
   public int maxChannel = 108000;
   public boolean usesBattery = false;
   public boolean isHighTier = false;
   public String HerbalistType;
   private float carbohydrates = 0.0F;
   private float lipids = 0.0F;
   private float proteins = 0.0F;
   private float calories = 0.0F;
   private boolean packaged = false;
   private boolean cantBeFrozen = false;
   public String evolvedRecipeName = null;
   private String ReplaceOnRotten = null;
   private float metalValue = 0.0F;
   private String AlarmSound = null;
   private String itemWhenDry = null;
   private float wetCooldown = 0.0F;
   private boolean isWet = false;
   private String onEat = null;
   private boolean cantBeConsolided = false;
   private boolean BadInMicrowave = false;
   private boolean GoodHot = false;
   private boolean BadCold = false;
   public String map = null;
   public int vehicleType = 0;
   private ArrayList<VehiclePartModel> vehiclePartModels = null;
   private int maxCapacity = -1;
   private int itemCapacity = -1;
   private boolean ConditionAffectsCapacity = false;
   private float brakeForce = 0.0F;
   private float durability = 0.0F;
   private int chanceToSpawnDamaged = 0;
   private float conditionLowerNormal = 0.0F;
   private float conditionLowerOffroad = 0.0F;
   private float wheelFriction = 0.0F;
   private float suspensionDamping = 0.0F;
   private float suspensionCompression = 0.0F;
   private float engineLoudness = 0.0F;
   public String ClothingItem = null;
   private ClothingItem clothingItemAsset = null;
   private String staticModel = null;
   public String primaryAnimMask = null;
   public String secondaryAnimMask = null;
   public String primaryAnimMaskAttachment = null;
   public String secondaryAnimMaskAttachment = null;
   public String replaceInSecondHand = null;
   public String replaceInPrimaryHand = null;
   public String replaceWhenUnequip = null;
   public ItemReplacement replacePrimaryHand = null;
   public ItemReplacement replaceSecondHand = null;
   public String worldObjectSprite = null;
   public String ItemName;
   public Texture NormalTexture;
   public List<Texture> SpecialTextures = new ArrayList();
   public List<String> SpecialWorldTextureNames = new ArrayList();
   public String WorldTextureName;
   public Texture WorldTexture;
   public String eatType;
   public String pourType;
   public String readType;
   public String digType;
   private ArrayList<String> IconsForTexture;
   private float baseSpeed = 1.0F;
   private ArrayList<BloodClothingType> bloodClothingType;
   private float stompPower = 1.0F;
   public float runSpeedModifier = 1.0F;
   public float combatSpeedModifier = 1.0F;
   public ArrayList<String> clothingItemExtra;
   public ArrayList<String> clothingItemExtraOption;
   private Boolean removeOnBroken = false;
   public Boolean canHaveHoles = true;
   private boolean cosmetic = false;
   private String ammoBox = null;
   private String insertAmmoStartSound = null;
   private String insertAmmoSound = null;
   private String insertAmmoStopSound = null;
   private String ejectAmmoStartSound = null;
   private String ejectAmmoSound = null;
   private String ejectAmmoStopSound = null;
   private String rackSound = null;
   private String clickSound = "Stormy9mmClick";
   private String equipSound = null;
   private String unequipSound = null;
   private String bringToBearSound = null;
   private String magazineType = null;
   private String weaponReloadType = null;
   private boolean rackAfterShoot = false;
   private float jamGunChance = 1.0F;
   private ArrayList<ModelWeaponPart> modelWeaponPart = null;
   private boolean haveChamber = true;
   private boolean manuallyRemoveSpentRounds = false;
   private float biteDefense = 0.0F;
   private float scratchDefense = 0.0F;
   private float corpseSicknessDefense = 0.0F;
   private float bulletDefense = 0.0F;
   private String damageCategory = null;
   private boolean damageMakeHole = false;
   public float neckProtectionModifier = 1.0F;
   private String attachmentReplacement = null;
   private boolean insertAllBulletsReload = false;
   private int chanceToFall = 0;
   public String fabricType = null;
   public boolean equippedNoSprint = false;
   public String worldStaticModel = null;
   private float critDmgMultiplier = 0.0F;
   public boolean isDung = false;
   private float insulation = 0.0F;
   private float windresist = 0.0F;
   private float waterresist = 0.0F;
   private String fireMode = null;
   private ArrayList<String> fireModePossibilities = null;
   public boolean RemoveUnhappinessWhenCooked = false;
   public float stopPower = 5.0F;
   private String recordedMediaCat;
   private byte acceptMediaType = -1;
   private boolean noTransmit = false;
   private boolean worldRender = true;
   private String LuaCreate = null;
   private HashMap<String, String> soundParameterMap = null;
   public String MilkReplaceItem = null;
   public int MaxMilk = 0;
   public String AnimalFeedType = null;
   public final ArrayList<String> evolvedRecipe = new ArrayList();
   private String itemConfigKey = null;
   private ItemConfig itemConfig = null;
   private String iconColorMask;
   private String iconFluidMask;
   public String withDrainable = null;
   public String withoutDrainable = null;
   private ArrayList<String> staticModelsByIndex = null;
   private ArrayList<String> worldStaticModelsByIndex = null;
   private ArrayList<String> weaponSpritesByIndex = null;
   public String spawnWith = null;
   public float visionModifier = 1.0F;
   public float hearingModifier = 1.0F;
   public float strainModifier = 1.0F;
   private String onBreak = null;
   public String damagedSound = null;
   private String shoutType = null;
   public float shoutMultiplier = 1.0F;
   public int eatTime = 0;
   public boolean visualAid = false;
   public float discomfortModifier = 0.0F;
   public float fireFuelRatio = 0.0F;
   public String HitSound = "BaseballBatHit";
   public String hitFloorSound = "BatOnFloor";
   public String BodyLocation = "";
   public Stack<String> PaletteChoices = new Stack();
   public String SpriteName = null;
   public String PalettesStart = "";
   public static HashMap<Integer, String> NetIDToItem = new HashMap();
   public static HashMap<String, Integer> NetItemToID = new HashMap();
   static int IDMax = 0;
   public Type type;
   private boolean Spice;
   private int UseForPoison;
   private final HashMap<String, ItemRecipe> itemRecipeMap;

   public Item() {
      super(ScriptType.Item);
      this.type = Item.Type.Normal;
      this.Spice = false;
      this.itemRecipeMap = new HashMap();
   }

   public String getDisplayName() {
      return this.DisplayName;
   }

   public void setDisplayName(String var1) {
      this.DisplayName = var1;
   }

   public boolean isHidden() {
      return this.Hidden;
   }

   public String getDisplayCategory() {
      return this.DisplayCategory;
   }

   public String getIcon() {
      return this.Icon;
   }

   public void setIcon(String var1) {
      this.Icon = var1;
   }

   public int getNoiseDuration() {
      return this.noiseDuration;
   }

   public Texture getNormalTexture() {
      return this.NormalTexture;
   }

   public int getNumberOfPages() {
      return this.NumberOfPages;
   }

   public float getActualWeight() {
      return this.ActualWeight < 0.0F ? 0.0F : this.ActualWeight;
   }

   public void setActualWeight(float var1) {
      if (var1 < 0.0F) {
         var1 = 0.0F;
      }

      this.ActualWeight = var1;
   }

   public float getWeightWet() {
      return this.WeightWet;
   }

   public void setWeightWet(float var1) {
      this.WeightWet = var1;
   }

   public float getWeightEmpty() {
      return this.WeightEmpty;
   }

   public void setWeightEmpty(float var1) {
      this.WeightEmpty = var1;
   }

   public float getHungerChange() {
      return this.HungerChange;
   }

   public void setHungerChange(float var1) {
      this.HungerChange = var1;
   }

   public float getThirstChange() {
      return this.ThirstChange;
   }

   public void setThirstChange(float var1) {
      this.ThirstChange = var1;
   }

   /** @deprecated */
   @Deprecated
   public int getCount() {
      return this.Count;
   }

   public void setCount(int var1) {
      this.Count = var1;
   }

   public int getDaysFresh() {
      return this.DaysFresh;
   }

   public void setDaysFresh(int var1) {
      this.DaysFresh = var1;
   }

   public int getDaysTotallyRotten() {
      return this.DaysTotallyRotten;
   }

   public void setDaysTotallyRotten(int var1) {
      this.DaysTotallyRotten = var1;
   }

   public int getMinutesToCook() {
      return this.MinutesToCook;
   }

   public void setMinutesToCook(int var1) {
      this.MinutesToCook = var1;
   }

   public int getMinutesToBurn() {
      return this.MinutesToBurn;
   }

   public void setMinutesToBurn(int var1) {
      this.MinutesToBurn = var1;
   }

   public boolean isIsCookable() {
      return this.IsCookable;
   }

   public void setIsCookable(boolean var1) {
      this.IsCookable = var1;
   }

   public String getCookingSound() {
      return this.CookingSound;
   }

   public float getStressChange() {
      return this.StressChange;
   }

   public void setStressChange(float var1) {
      this.StressChange = var1;
   }

   public float getBoredomChange() {
      return this.BoredomChange;
   }

   public void setBoredomChange(float var1) {
      this.BoredomChange = var1;
   }

   public float getUnhappyChange() {
      return this.UnhappyChange;
   }

   public void setUnhappyChange(float var1) {
      this.UnhappyChange = var1;
   }

   public boolean isAlwaysWelcomeGift() {
      return this.AlwaysWelcomeGift;
   }

   public void setAlwaysWelcomeGift(boolean var1) {
      this.AlwaysWelcomeGift = var1;
   }

   public boolean isRanged() {
      return this.Ranged;
   }

   public void setRanged(boolean var1) {
      this.Ranged = var1;
   }

   public float getMaxRange() {
      return this.MaxRange;
   }

   public void setMaxRange(float var1) {
      this.MaxRange = var1;
   }

   public float getMinAngle() {
      return this.MinAngle;
   }

   public void setMinAngle(float var1) {
      this.MinAngle = var1;
   }

   public float getMaxDamage() {
      return this.MaxDamage;
   }

   public void setMaxDamage(float var1) {
      this.MaxDamage = var1;
   }

   public float getMinDamage() {
      return this.MinDamage;
   }

   public void setMinDamage(float var1) {
      this.MinDamage = var1;
   }

   public float getMinimumSwingTime() {
      return this.MinimumSwingTime;
   }

   public void setMinimumSwingTime(float var1) {
      this.MinimumSwingTime = var1;
   }

   public String getSwingSound() {
      return this.SwingSound;
   }

   public void setSwingSound(String var1) {
      this.SwingSound = var1;
   }

   public String getWeaponSprite() {
      return this.WeaponSprite;
   }

   public void setWeaponSprite(String var1) {
      this.WeaponSprite = var1;
   }

   public boolean isAngleFalloff() {
      return this.AngleFalloff;
   }

   public void setAngleFalloff(boolean var1) {
      this.AngleFalloff = var1;
   }

   public int getSoundVolume() {
      return this.SoundVolume;
   }

   public void setSoundVolume(int var1) {
      this.SoundVolume = var1;
   }

   public float getToHitModifier() {
      return this.ToHitModifier;
   }

   public void setToHitModifier(float var1) {
      this.ToHitModifier = var1;
   }

   public int getSoundRadius() {
      return this.SoundRadius;
   }

   public void setSoundRadius(int var1) {
      this.SoundRadius = var1;
   }

   public float getOtherCharacterVolumeBoost() {
      return this.OtherCharacterVolumeBoost;
   }

   public void setOtherCharacterVolumeBoost(float var1) {
      this.OtherCharacterVolumeBoost = var1;
   }

   public ArrayList<String> getCategories() {
      return this.Categories;
   }

   public void setCategories(ArrayList<String> var1) {
      this.Categories.clear();
      this.Categories.addAll(var1);
   }

   public ArrayList<String> getTags() {
      return this.Tags;
   }

   public String getImpactSound() {
      return this.ImpactSound;
   }

   public void setImpactSound(String var1) {
      this.ImpactSound = var1;
   }

   public float getSwingTime() {
      return this.SwingTime;
   }

   public void setSwingTime(float var1) {
      this.SwingTime = var1;
   }

   public boolean isKnockBackOnNoDeath() {
      return this.KnockBackOnNoDeath;
   }

   public void setKnockBackOnNoDeath(boolean var1) {
      this.KnockBackOnNoDeath = var1;
   }

   public boolean isSplatBloodOnNoDeath() {
      return this.SplatBloodOnNoDeath;
   }

   public void setSplatBloodOnNoDeath(boolean var1) {
      this.SplatBloodOnNoDeath = var1;
   }

   public float getSwingAmountBeforeImpact() {
      return this.SwingAmountBeforeImpact;
   }

   public void setSwingAmountBeforeImpact(float var1) {
      this.SwingAmountBeforeImpact = var1;
   }

   public String getAmmoType() {
      return this.AmmoType;
   }

   public void setAmmoType(String var1) {
      this.AmmoType = var1;
   }

   public int getDoorDamage() {
      return this.DoorDamage;
   }

   public void setDoorDamage(int var1) {
      this.DoorDamage = var1;
   }

   public int getConditionLowerChance() {
      return this.ConditionLowerChance;
   }

   public void setConditionLowerChance(int var1) {
      this.ConditionLowerChance = var1;
   }

   public int getConditionMax() {
      return this.ConditionMax;
   }

   public void setConditionMax(int var1) {
      this.ConditionMax = var1;
   }

   public boolean isCanBandage() {
      return this.CanBandage;
   }

   public void setCanBandage(boolean var1) {
      this.CanBandage = var1;
   }

   public boolean isCosmetic() {
      return this.cosmetic;
   }

   public String getName() {
      return this.name;
   }

   public String getModuleName() {
      return this.getModule().name;
   }

   public String getFullName() {
      return this.moduleDotType;
   }

   public void setName(String var1) {
      this.name = var1;
      this.moduleDotType = this.getModule().name + "." + var1;
   }

   public int getMaxHitCount() {
      return this.MaxHitCount;
   }

   public void setMaxHitCount(int var1) {
      this.MaxHitCount = var1;
   }

   public boolean isUseSelf() {
      return this.UseSelf;
   }

   public void setUseSelf(boolean var1) {
      this.UseSelf = var1;
   }

   public boolean isOtherHandUse() {
      return this.OtherHandUse;
   }

   public void setOtherHandUse(boolean var1) {
      this.OtherHandUse = var1;
   }

   public String getOtherHandRequire() {
      return this.OtherHandRequire;
   }

   public void setOtherHandRequire(String var1) {
      this.OtherHandRequire = var1;
   }

   public String getPhysicsObject() {
      return this.PhysicsObject;
   }

   public void setPhysicsObject(String var1) {
      this.PhysicsObject = var1;
   }

   public String getSwingAnim() {
      return this.SwingAnim;
   }

   public void setSwingAnim(String var1) {
      this.SwingAnim = var1;
   }

   public float getWeaponWeight() {
      return this.WeaponWeight;
   }

   public void setWeaponWeight(float var1) {
      this.WeaponWeight = var1;
   }

   public float getEnduranceChange() {
      return this.EnduranceChange;
   }

   public void setEnduranceChange(float var1) {
      this.EnduranceChange = var1;
   }

   public String getBreakSound() {
      return this.breakSound;
   }

   public String getBulletOutSound() {
      return this.bulletOutSound;
   }

   public String getCloseSound() {
      return this.CloseSound;
   }

   public String getClothingItem() {
      return this.ClothingItem;
   }

   public void setClothingItemAsset(ClothingItem var1) {
      this.clothingItemAsset = var1;
   }

   public ClothingItem getClothingItemAsset() {
      return this.clothingItemAsset;
   }

   public ArrayList<String> getClothingItemExtra() {
      return this.clothingItemExtra;
   }

   public ArrayList<String> getClothingItemExtraOption() {
      return this.clothingItemExtraOption;
   }

   public String getFabricType() {
      return this.fabricType;
   }

   public ArrayList<String> getIconsForTexture() {
      return this.IconsForTexture;
   }

   public String getCustomEatSound() {
      return this.customEatSound;
   }

   public String getFillFromDispenserSound() {
      return this.fillFromDispenserSound;
   }

   public String getFillFromLakeSound() {
      return this.fillFromLakeSound;
   }

   public String getFillFromTapSound() {
      return this.fillFromTapSound;
   }

   public String getFillFromToiletSound() {
      return this.fillFromToiletSound;
   }

   public String getEatType() {
      return this.eatType;
   }

   public String getPourType() {
      return this.pourType;
   }

   public String getReadType() {
      return this.readType;
   }

   public String getDigType() {
      return this.digType;
   }

   public String getCountDownSound() {
      return this.countDownSound;
   }

   public String getBringToBearSound() {
      return this.bringToBearSound;
   }

   public String getEjectAmmoStartSound() {
      return this.ejectAmmoStartSound;
   }

   public String getEjectAmmoSound() {
      return this.ejectAmmoSound;
   }

   public String getEjectAmmoStopSound() {
      return this.ejectAmmoStopSound;
   }

   public String getInsertAmmoStartSound() {
      return this.insertAmmoStartSound;
   }

   public String getInsertAmmoSound() {
      return this.insertAmmoSound;
   }

   public String getInsertAmmoStopSound() {
      return this.insertAmmoStopSound;
   }

   public String getEquipSound() {
      return this.equipSound;
   }

   public String getUnequipSound() {
      return this.unequipSound;
   }

   public String getExplosionSound() {
      return this.explosionSound;
   }

   public String getStaticModel() {
      return this.staticModel;
   }

   public String getWorldStaticModel() {
      return this.worldStaticModel;
   }

   public String getStaticModelException() {
      return this.getTags().contains("UseWorldStaticModel") ? this.worldStaticModel : this.staticModel;
   }

   public String getOpenSound() {
      return this.OpenSound;
   }

   public String getPutInSound() {
      return this.PutInSound;
   }

   public String getPlaceOneSound() {
      return this.PlaceOneSound;
   }

   public String getPlaceMultipleSound() {
      return this.PlaceMultipleSound;
   }

   public String getShellFallSound() {
      return this.ShellFallSound;
   }

   public String getDropSound() {
      return this.DropSound;
   }

   public String getSoundByID(String var1) {
      return this.SoundMap == null ? null : (String)this.SoundMap.getOrDefault(var1, (Object)null);
   }

   public String getSkillTrained() {
      return this.SkillTrained;
   }

   public String getDoorHitSound() {
      return this.DoorHitSound;
   }

   public void setDoorHitSound(String var1) {
      this.DoorHitSound = var1;
   }

   public boolean isManuallyRemoveSpentRounds() {
      return this.manuallyRemoveSpentRounds;
   }

   public String getReplaceOnUse() {
      return this.ReplaceOnUse;
   }

   public void setReplaceOnUse(String var1) {
      this.ReplaceOnUse = var1;
   }

   public String getReplaceOnDeplete() {
      return this.ReplaceOnDeplete;
   }

   public void setReplaceOnDeplete(String var1) {
      this.ReplaceOnDeplete = var1;
   }

   public String getReplaceTypes() {
      return this.ReplaceTypes;
   }

   public HashMap<String, String> getReplaceTypesMap() {
      return this.ReplaceTypesMap;
   }

   public String getReplaceType(String var1) {
      return this.ReplaceTypesMap == null ? null : (String)this.ReplaceTypesMap.get(var1);
   }

   public boolean hasReplaceType(String var1) {
      return this.getReplaceType(var1) != null;
   }

   public boolean isDangerousUncooked() {
      return this.DangerousUncooked;
   }

   public void setDangerousUncooked(boolean var1) {
      this.DangerousUncooked = var1;
   }

   public boolean isAlcoholic() {
      return this.Alcoholic;
   }

   public void setAlcoholic(boolean var1) {
      this.Alcoholic = var1;
   }

   public float getPushBackMod() {
      return this.PushBackMod;
   }

   public void setPushBackMod(float var1) {
      this.PushBackMod = var1;
   }

   public int getSplatNumber() {
      return this.SplatNumber;
   }

   public void setSplatNumber(int var1) {
      this.SplatNumber = var1;
   }

   public float getNPCSoundBoost() {
      return this.NPCSoundBoost;
   }

   public void setNPCSoundBoost(float var1) {
      this.NPCSoundBoost = var1;
   }

   public boolean isRangeFalloff() {
      return this.RangeFalloff;
   }

   public void setRangeFalloff(boolean var1) {
      this.RangeFalloff = var1;
   }

   public boolean isUseEndurance() {
      return this.UseEndurance;
   }

   public void setUseEndurance(boolean var1) {
      this.UseEndurance = var1;
   }

   public boolean isMultipleHitConditionAffected() {
      return this.MultipleHitConditionAffected;
   }

   public void setMultipleHitConditionAffected(boolean var1) {
      this.MultipleHitConditionAffected = var1;
   }

   public boolean isShareDamage() {
      return this.ShareDamage;
   }

   public void setShareDamage(boolean var1) {
      this.ShareDamage = var1;
   }

   public boolean isShareEndurance() {
      return this.ShareEndurance;
   }

   public void setShareEndurance(boolean var1) {
      this.ShareEndurance = var1;
   }

   public boolean isCanBarricade() {
      return this.CanBarricade;
   }

   public void setCanBarricade(boolean var1) {
      this.CanBarricade = var1;
   }

   public boolean isUseWhileEquipped() {
      return this.UseWhileEquipped;
   }

   public void setUseWhileEquipped(boolean var1) {
      this.UseWhileEquipped = var1;
   }

   public boolean isUseWhileUnequipped() {
      return this.UseWhileUnequipped;
   }

   public void setUseWhileUnequipped(boolean var1) {
      this.UseWhileUnequipped = var1;
   }

   public void setTicksPerEquipUse(int var1) {
      this.TicksPerEquipUse = var1;
   }

   public float getTicksPerEquipUse() {
      return (float)this.TicksPerEquipUse;
   }

   public boolean isDisappearOnUse() {
      return this.DisappearOnUse;
   }

   public boolean isKeepOnDeplete() {
      return !this.DisappearOnUse;
   }

   public void setDisappearOnUse(boolean var1) {
      this.DisappearOnUse = var1;
   }

   public void setKeepOnDeplete(boolean var1) {
      this.DisappearOnUse = !var1;
   }

   public float getUseDelta() {
      return this.UseDelta;
   }

   public void setUseDelta(float var1) {
      this.UseDelta = var1;
   }

   public boolean isAlwaysKnockdown() {
      return this.AlwaysKnockdown;
   }

   public void setAlwaysKnockdown(boolean var1) {
      this.AlwaysKnockdown = var1;
   }

   public float getEnduranceMod() {
      return this.EnduranceMod;
   }

   public void setEnduranceMod(float var1) {
      this.EnduranceMod = var1;
   }

   public float getKnockdownMod() {
      return this.KnockdownMod;
   }

   public void setKnockdownMod(float var1) {
      this.KnockdownMod = var1;
   }

   public boolean isCantAttackWithLowestEndurance() {
      return this.CantAttackWithLowestEndurance;
   }

   public void setCantAttackWithLowestEndurance(boolean var1) {
      this.CantAttackWithLowestEndurance = var1;
   }

   public String getBodyLocation() {
      return this.BodyLocation;
   }

   public void setBodyLocation(String var1) {
      this.BodyLocation = var1;
   }

   public Stack<String> getPaletteChoices() {
      return this.PaletteChoices;
   }

   public void setPaletteChoices(Stack<String> var1) {
      this.PaletteChoices = var1;
   }

   public String getSpriteName() {
      return this.SpriteName;
   }

   public void setSpriteName(String var1) {
      this.SpriteName = var1;
   }

   public String getPalettesStart() {
      return this.PalettesStart;
   }

   public void setPalettesStart(String var1) {
      this.PalettesStart = var1;
   }

   public Type getType() {
      return this.type;
   }

   public void setType(Type var1) {
      this.type = var1;
   }

   public String getTypeString() {
      return this.type.name();
   }

   public String getMapID() {
      return this.map;
   }

   public ArrayList<String> getEvolvedRecipe() {
      return this.evolvedRecipe;
   }

   public void InitLoadPP(String var1) {
      this.name = var1;
      this.moduleDotType = this.getModule().name + "." + var1;
      super.InitLoadPP(var1);
      int var2 = IDMax++;
      NetIDToItem.put(var2, this.moduleDotType);
      NetItemToID.put(this.moduleDotType, var2);
   }

   public void Load(String var1, String var2) throws Exception {
      this.name = var1;
      this.moduleDotType = this.getModule().name + "." + var1;
      ScriptParser.Block var3 = ScriptParser.parse(var2);
      var3 = (ScriptParser.Block)var3.children.get(0);
      super.LoadCommonBlock(var3);
      Iterator var4 = var3.elements.iterator();

      while(var4.hasNext()) {
         ScriptParser.BlockElement var5 = (ScriptParser.BlockElement)var4.next();
         if (var5.asValue() != null) {
            try {
               String var6 = var5.asValue().string;
               if (!var6.trim().isEmpty()) {
                  String[] var7 = var6.split("=");
                  String var8 = var7[0].trim();
                  String var9 = var7[1].trim();
                  this.DoParam(var8, var9);
               }
            } catch (Exception var10) {
               var10.printStackTrace();
               throw new InvalidParameterException(var10.getMessage());
            }
         } else {
            ScriptParser.Block var12 = var5.asBlock();
            if (var12.type != null && var12.type.equalsIgnoreCase("component")) {
               this.LoadComponentBlock(var12);
            }
         }
      }

      if (this.DisplayName == null) {
         this.DisplayName = this.getFullName();
         this.Hidden = true;
      }

      String[] var11;
      if (!StringUtils.isNullOrWhitespace(this.replaceInPrimaryHand)) {
         var11 = this.replaceInPrimaryHand.trim().split("\\s+");
         if (var11.length == 2) {
            this.replacePrimaryHand = new ItemReplacement();
            this.replacePrimaryHand.clothingItemName = var11[0].trim();
            this.replacePrimaryHand.maskVariableValue = var11[1].trim();
            this.replacePrimaryHand.maskVariableName = "RightHandMask";
         }
      }

      if (!StringUtils.isNullOrWhitespace(this.replaceInSecondHand)) {
         var11 = this.replaceInSecondHand.trim().split("\\s+");
         if (var11.length == 2) {
            this.replaceSecondHand = new ItemReplacement();
            this.replaceSecondHand.clothingItemName = var11[0].trim();
            this.replaceSecondHand.maskVariableValue = var11[1].trim();
            this.replaceSecondHand.maskVariableName = "LeftHandMask";
         }
      }

      if (!StringUtils.isNullOrWhitespace(this.primaryAnimMask)) {
         this.replacePrimaryHand = new ItemReplacement();
         this.replacePrimaryHand.maskVariableValue = this.primaryAnimMask;
         this.replacePrimaryHand.maskVariableName = "RightHandMask";
         this.replacePrimaryHand.attachment = this.primaryAnimMaskAttachment;
      }

      if (!StringUtils.isNullOrWhitespace(this.secondaryAnimMask)) {
         this.replaceSecondHand = new ItemReplacement();
         this.replaceSecondHand.maskVariableValue = this.secondaryAnimMask;
         this.replaceSecondHand.maskVariableName = "LeftHandMask";
         this.replaceSecondHand.attachment = this.secondaryAnimMaskAttachment;
      }

      if (this.NormalTexture == null && this.IconsForTexture != null && !this.IconsForTexture.isEmpty()) {
         this.NormalTexture = Texture.trygetTexture("Item_" + (String)this.IconsForTexture.get(0));
      }

   }

   public InventoryItem InstanceItem(String var1) {
      return this.InstanceItem(var1, true);
   }

   public InventoryItem InstanceItem(String var1, boolean var2) {
      Object var3 = null;
      if (this.type == Item.Type.Key) {
         var3 = new Key(this.getModule().name, this.DisplayName, this.name, "Item_" + this.Icon);
         ((Key)var3).setDigitalPadlock(this.digitalPadlock);
         ((Key)var3).setPadlock(this.padlock);
         if (((Key)var3).isPadlock()) {
            ((Key)var3).setNumberOfKey(2);
            ((Key)var3).setKeyId(Rand.Next(10000000));
         }
      } else if (this.type == Item.Type.WeaponPart) {
         var3 = new WeaponPart(this.getModule().name, this.DisplayName, this.name, "Item_" + this.Icon);
         WeaponPart var4 = (WeaponPart)var3;
         var4.setDamage(this.damageModifier);
         var4.setClipSize(this.clipSizeModifier);
         var4.setMaxRange(this.maxRangeModifier);
         var4.setMinSightRange(this.minSightRange);
         var4.setMaxSightRange(this.maxSightRange);
         var4.setRecoilDelay(this.recoilDelayModifier);
         var4.setMountOn(this.mountOn);
         var4.setPartType(this.partType);
         var4.setCanAttachCallback(this.canAttachCallback);
         var4.setOnAttachCallback(this.onAttachCallback);
         var4.setOnDetachCallback(this.onDetachCallback);
         var4.setReloadTime(this.reloadTimeModifier);
         var4.setAimingTime(this.aimingTimeModifier);
         var4.setHitChance(this.hitChanceModifier);
         var4.setSpreadModifier(this.projectileSpreadModifier);
         var4.setWeightModifier(this.weightModifier);
      } else if (this.type == Item.Type.Container) {
         var3 = new InventoryContainer(this.getModule().name, this.DisplayName, this.name, "Item_" + this.Icon);
         InventoryContainer var7 = (InventoryContainer)var3;
         var7.setItemCapacity((float)this.Capacity);
         var7.setCapacity(this.Capacity);
         var7.setWeightReduction(this.WeightReduction);
         var7.setCanBeEquipped(this.CanBeEquipped);
         var7.getInventory().setPutSound(this.PutInSound);
         var7.getInventory().setCloseSound(this.CloseSound);
         var7.getInventory().setOpenSound(this.OpenSound);
         var7.getInventory().setOnlyAcceptCategory(this.OnlyAcceptCategory);
         var7.getInventory().setAcceptItemFunction(this.AcceptItemFunction);
      } else if (this.type == Item.Type.Food) {
         var3 = new Food(this.getModule().name, this.DisplayName, this.name, this);
         Food var8 = (Food)var3;
         var8.Poison = this.Poison;
         var8.setPoisonLevelForRecipe(this.PoisonDetectionLevel);
         var8.setFoodType(this.FoodType);
         var8.setPoisonPower(this.PoisonPower);
         var8.setUseForPoison(this.UseForPoison);
         var8.setThirstChange(this.ThirstChange / 100.0F);
         var8.setHungChange(this.HungerChange / 100.0F);
         var8.setBaseHunger(this.HungerChange / 100.0F);
         var8.setEndChange(this.EnduranceChange / 100.0F);
         var8.setOffAge(this.DaysFresh);
         var8.setOffAgeMax(this.DaysTotallyRotten);
         var8.setIsCookable(this.IsCookable);
         var8.setMinutesToCook((float)this.MinutesToCook);
         var8.setMinutesToBurn((float)this.MinutesToBurn);
         var8.setbDangerousUncooked(this.DangerousUncooked);
         var8.setReplaceOnUse(this.ReplaceOnUse);
         var8.setReplaceOnCooked(this.ReplaceOnCooked);
         var8.setSpice(this.Spice);
         var8.setRemoveNegativeEffectOnCooked(this.RemoveNegativeEffectOnCooked);
         var8.setCustomEatSound(this.customEatSound);
         var8.setOnCooked(this.OnCooked);
         var8.setFluReduction(this.fluReduction);
         var8.setReduceFoodSickness(this.ReduceFoodSickness);
         var8.setPainReduction((float)this.painReduction);
         var8.setHerbalistType(this.HerbalistType);
         var8.setCarbohydrates(this.carbohydrates);
         var8.setLipids(this.lipids);
         var8.setProteins(this.proteins);
         var8.setCalories(this.calories);
         var8.setPackaged(this.packaged);
         var8.setCanBeFrozen(!this.cantBeFrozen);
         var8.setReplaceOnRotten(this.ReplaceOnRotten);
         var8.setOnEat(this.onEat);
         var8.setBadInMicrowave(this.BadInMicrowave);
         var8.setGoodHot(this.GoodHot);
         var8.setBadCold(this.BadCold);
      } else if (this.type == Item.Type.Literature) {
         var3 = new Literature(this.getModule().name, this.DisplayName, this.name, this);
         Literature var9 = (Literature)var3;
         var9.setReplaceOnUse(this.ReplaceOnUse);
         var9.setNumberOfPages(this.NumberOfPages);
         var9.setAlreadyReadPages(0);
         var9.setSkillTrained(this.SkillTrained);
         var9.setLvlSkillTrained(this.LvlSkillTrained);
         var9.setNumLevelsTrained(this.NumLevelsTrained);
         var9.setCanBeWrite(this.canBeWrite);
         var9.setPageToWrite(this.PageToWrite);
         var9.setTeachedRecipes(this.teachedRecipes);
      } else if (this.type == Item.Type.AlarmClock) {
         var3 = new AlarmClock(this.getModule().name, this.DisplayName, this.name, this);
         AlarmClock var10 = (AlarmClock)var3;
         var10.setAlarmSound(this.AlarmSound);
         var10.setSoundRadius(this.SoundRadius);
      } else {
         String var5;
         int var6;
         String var11;
         String var10000;
         if (this.type == Item.Type.AlarmClockClothing) {
            var11 = "";
            var5 = null;
            if (!this.PaletteChoices.isEmpty() || var1 != null) {
               var6 = Rand.Next(this.PaletteChoices.size());
               var5 = (String)this.PaletteChoices.get(var6);
               if (var1 != null) {
                  var5 = var1;
               }

               var10000 = var5.replace(this.PalettesStart, "");
               var11 = "_" + var10000;
            }

            var3 = new AlarmClockClothing(this.getModule().name, this.DisplayName, this.name, "Item_" + this.Icon.replace(".png", "") + var11, var5, this.SpriteName);
            AlarmClockClothing var12 = (AlarmClockClothing)var3;
            var12.setTemperature(this.Temperature);
            var12.setInsulation(this.insulation);
            var12.setConditionLowerChance(this.ConditionLowerChance);
            var12.setStompPower(this.stompPower);
            var12.setRunSpeedModifier(this.runSpeedModifier);
            var12.setCombatSpeedModifier(this.combatSpeedModifier);
            var12.setRemoveOnBroken(this.removeOnBroken);
            var12.setCanHaveHoles(this.canHaveHoles);
            var12.setWeightWet(this.WeightWet);
            var12.setBiteDefense(this.biteDefense);
            var12.setBulletDefense(this.bulletDefense);
            var12.setNeckProtectionModifier(this.neckProtectionModifier);
            var12.setScratchDefense(this.scratchDefense);
            var12.setChanceToFall(this.chanceToFall);
            var12.setWindresistance(this.windresist);
            var12.setWaterResistance(this.waterresist);
            var12.setAlarmSound(this.AlarmSound);
            var12.setSoundRadius(this.SoundRadius);
         } else if (this.type == Item.Type.Weapon) {
            var3 = new HandWeapon(this.getModule().name, this.DisplayName, this.name, this);
            HandWeapon var13 = (HandWeapon)var3;
            var13.setMultipleHitConditionAffected(this.MultipleHitConditionAffected);
            var13.setConditionLowerChance(this.ConditionLowerChance);
            var13.SplatSize = this.SplatSize;
            this.AimingMod = var13.getAimingMod();
            var13.setMinDamage(this.MinDamage);
            var13.setMaxDamage(this.MaxDamage);
            var13.setBaseSpeed(this.baseSpeed);
            var13.setPhysicsObject(this.PhysicsObject);
            var13.setOtherHandRequire(this.OtherHandRequire);
            var13.setOtherHandUse(this.OtherHandUse);
            var13.setMaxRange(this.MaxRange);
            var13.setMinRange(this.MinRange);
            var13.setMinSightRange(this.minSightRange);
            var13.setMaxSightRange(this.maxSightRange);
            var13.setShareEndurance(this.ShareEndurance);
            var13.setKnockdownMod(this.KnockdownMod);
            var13.bIsAimedFirearm = this.IsAimedFirearm;
            var13.RunAnim = this.RunAnim;
            var13.IdleAnim = this.IdleAnim;
            var13.HitAngleMod = (float)Math.toRadians((double)this.HitAngleMod);
            var13.bIsAimedHandWeapon = this.IsAimedHandWeapon;
            var13.setCantAttackWithLowestEndurance(this.CantAttackWithLowestEndurance);
            var13.setAlwaysKnockdown(this.AlwaysKnockdown);
            var13.setEnduranceMod(this.EnduranceMod);
            var13.setUseSelf(this.UseSelf);
            var13.setMaxHitCount(this.MaxHitCount);
            var13.setMinimumSwingTime(this.MinimumSwingTime);
            var13.setSwingTime(this.SwingTime);
            var13.setDoSwingBeforeImpact(this.SwingAmountBeforeImpact);
            var13.setMinAngle(this.MinAngle);
            var13.setDoorDamage(this.DoorDamage);
            var13.setTreeDamage(this.treeDamage);
            var13.setDoorHitSound(this.DoorHitSound);
            var13.setHitFloorSound(this.hitFloorSound);
            var13.setZombieHitSound(this.HitSound);
            var13.setPushBackMod(this.PushBackMod);
            var13.setWeight(this.WeaponWeight);
            var13.setImpactSound(this.ImpactSound);
            var13.setSplatNumber(this.SplatNumber);
            var13.setKnockBackOnNoDeath(this.KnockBackOnNoDeath);
            var13.setSplatBloodOnNoDeath(this.SplatBloodOnNoDeath);
            var13.setSwingSound(this.SwingSound);
            var13.setBulletOutSound(this.bulletOutSound);
            var13.setShellFallSound(this.ShellFallSound);
            var13.setAngleFalloff(this.AngleFalloff);
            var13.setSoundVolume(this.SoundVolume);
            var13.setSoundRadius(this.SoundRadius);
            var13.setToHitModifier(this.ToHitModifier);
            var13.setOtherBoost(this.NPCSoundBoost);
            var13.setRanged(this.Ranged);
            var13.setRangeFalloff(this.RangeFalloff);
            var13.setUseEndurance(this.UseEndurance);
            var13.setCriticalChance(this.CriticalChance);
            var13.setCritDmgMultiplier(this.critDmgMultiplier);
            var13.setShareDamage(this.ShareDamage);
            var13.setCanBarracade(this.CanBarricade);
            var13.setWeaponSprite(this.WeaponSprite);
            var13.setOriginalWeaponSprite(this.WeaponSprite);
            var13.setSubCategory(this.SubCategory);
            var13.setCategories(this.Categories);
            var13.setSoundGain(this.SoundGain);
            var13.setAimingPerkCritModifier(this.AimingPerkCritModifier);
            var13.setAimingPerkRangeModifier(this.AimingPerkRangeModifier);
            var13.setAimingPerkHitChanceModifier(this.AimingPerkHitChanceModifier);
            var13.setHitChance(this.HitChance);
            var13.setRecoilDelay(this.RecoilDelay);
            var13.setAimingPerkMinAngleModifier(this.AimingPerkMinAngleModifier);
            var13.setPiercingBullets(this.PiercingBullets);
            var13.setClipSize(this.ClipSize);
            var13.setReloadTime(this.reloadTime);
            var13.setAimingTime(this.aimingTime);
            var13.setTriggerExplosionTimer(this.triggerExplosionTimer);
            var13.setSensorRange(this.sensorRange);
            var13.setWeaponLength(this.WeaponLength);
            var13.setPlacedSprite(this.PlacedSprite);
            var13.setExplosionTimer(this.explosionTimer);
            var13.setExplosionDuration(this.explosionDuration);
            var13.setCanBePlaced(this.canBePlaced);
            var13.setCanBeReused(this.canBeReused);
            var13.setExplosionRange(this.explosionRange);
            var13.setExplosionPower(this.explosionPower);
            var13.setFireRange(this.fireRange);
            var13.setFirePower(this.firePower);
            var13.setSmokeRange(this.smokeRange);
            var13.setNoiseRange(this.noiseRange);
            var13.setExtraDamage(this.extraDamage);
            var13.setAmmoBox(this.ammoBox);
            var13.setRackSound(this.rackSound);
            var13.setClickSound(this.clickSound);
            var13.setMagazineType(this.magazineType);
            var13.setWeaponReloadType(this.weaponReloadType);
            var13.setInsertAllBulletsReload(this.insertAllBulletsReload);
            var13.setRackAfterShoot(this.rackAfterShoot);
            var13.setJamGunChance(this.jamGunChance);
            var13.setModelWeaponPart(this.modelWeaponPart);
            var13.setHaveChamber(this.haveChamber);
            var13.setDamageCategory(this.damageCategory);
            var13.setDamageMakeHole(this.damageMakeHole);
            var13.setFireMode(this.fireMode);
            var13.setFireModePossibilities(this.fireModePossibilities);
            var13.setWeaponSpritesByIndex(this.weaponSpritesByIndex);
            var13.setProjectileCount(this.ProjectileCount);
            var13.setProjectileSpread(this.projectileSpread);
            var13.setProjectileWeightCenter(this.projectileWeightCenter);
         } else if (this.type == Item.Type.Normal) {
            var3 = new ComboItem(this.getModule().name, this.DisplayName, this.name, this);
         } else if (this.type == Item.Type.Clothing) {
            var11 = "";
            var5 = null;
            if (!this.PaletteChoices.isEmpty() || var1 != null) {
               var6 = Rand.Next(this.PaletteChoices.size());
               var5 = (String)this.PaletteChoices.get(var6);
               if (var1 != null) {
                  var5 = var1;
               }

               var10000 = var5.replace(this.PalettesStart, "");
               var11 = "_" + var10000;
            }

            var3 = new Clothing(this.getModule().name, this.DisplayName, this.name, "Item_" + this.Icon.replace(".png", "") + var11, var5, this.SpriteName);
            Clothing var14 = (Clothing)var3;
            var14.setTemperature(this.Temperature);
            var14.setInsulation(this.insulation);
            var14.setConditionLowerChance(this.ConditionLowerChance);
            var14.setStompPower(this.stompPower);
            var14.setRunSpeedModifier(this.runSpeedModifier);
            var14.setCombatSpeedModifier(this.combatSpeedModifier);
            var14.setRemoveOnBroken(this.removeOnBroken);
            var14.setCanHaveHoles(this.canHaveHoles);
            var14.setWeightWet(this.WeightWet);
            var14.setBiteDefense(this.biteDefense);
            var14.setBulletDefense(this.bulletDefense);
            var14.setNeckProtectionModifier(this.neckProtectionModifier);
            var14.setScratchDefense(this.scratchDefense);
            var14.setChanceToFall(this.chanceToFall);
            var14.setWindresistance(this.windresist);
            var14.setWaterResistance(this.waterresist);
         } else if (this.type == Item.Type.Drainable) {
            var3 = new DrainableComboItem(this.getModule().name, this.DisplayName, this.name, this);
            DrainableComboItem var16 = (DrainableComboItem)var3;
            var16.setUseWhileEquiped(this.UseWhileEquipped);
            var16.setUseWhileUnequiped(this.UseWhileUnequipped);
            var16.setTicksPerEquipUse(this.TicksPerEquipUse);
            var16.setUseDelta(this.UseDelta);
            var16.setReplaceOnDeplete(this.ReplaceOnDeplete);
            var16.setIsCookable(this.IsCookable);
            var16.setReplaceOnCooked(this.ReplaceOnCooked);
            var16.setMinutesToCook((float)this.MinutesToCook);
            var16.setOnCooked(this.OnCooked);
            var16.setCanConsolidate(!this.cantBeConsolided);
            var16.setWeightEmpty(this.WeightEmpty);
            var16.setOnEat(this.onEat);
         } else if (this.type == Item.Type.Radio) {
            var3 = new Radio(this.getModule().name, this.DisplayName, this.name, "Item_" + this.Icon);
            Radio var17 = (Radio)var3;
            var17.setCanBeEquipped(this.CanBeEquipped);
            DeviceData var18 = var17.getDeviceData();
            if (var18 != null) {
               if (this.DisplayName != null) {
                  var18.setDeviceName(this.DisplayName);
               }

               var18.setIsTwoWay(this.twoWay);
               var18.setTransmitRange(this.transmitRange);
               var18.setMicRange(this.micRange);
               var18.setBaseVolumeRange(this.baseVolumeRange);
               var18.setIsPortable(this.isPortable);
               var18.setIsTelevision(this.isTelevision);
               var18.setMinChannelRange(this.minChannel);
               var18.setMaxChannelRange(this.maxChannel);
               var18.setIsBatteryPowered(this.usesBattery);
               var18.setIsHighTier(this.isHighTier);
               var18.setUseDelta(this.UseDelta);
               var18.setMediaType(this.acceptMediaType);
               var18.setNoTransmit(this.noTransmit);
               var18.generatePresets();
               var18.setRandomChannel();
            }

            if (!StringUtils.isNullOrWhitespace(this.worldObjectSprite) && !var17.ReadFromWorldSprite(this.worldObjectSprite)) {
               var10000 = this.moduleDotType != null ? this.moduleDotType : "unknown";
               DebugLog.log("Item -> Radio item = " + var10000);
            }
         } else if (this.type == Item.Type.Moveable) {
            var3 = new Moveable(this.getModule().name, this.DisplayName, this.name, this);
            Moveable var19 = (Moveable)var3;
            var19.ReadFromWorldSprite(this.worldObjectSprite);
            this.ActualWeight = var19.getActualWeight();
         } else if (this.type == Item.Type.Map) {
            MapItem var20 = new MapItem(this.getModule().name, this.DisplayName, this.name, this);
            if (StringUtils.isNullOrWhitespace(this.map)) {
               var20.setMapID(MapDefinitions.getInstance().pickRandom());
            } else {
               var20.setMapID(this.map);
            }

            var3 = var20;
         } else if (this.type == Item.Type.Animal) {
            var3 = new AnimalInventoryItem(this.getModule().name, this.DisplayName, this.name, this);
         }
      }

      if (this.colorRed < 255 || this.colorGreen < 255 || this.colorBlue < 255) {
         ((InventoryItem)var3).setColor(new Color((float)this.colorRed / 255.0F, (float)this.colorGreen / 255.0F, (float)this.colorBlue / 255.0F));
      }

      ((InventoryItem)var3).setAlcoholPower(this.alcoholPower);
      ((InventoryItem)var3).setConditionMax(this.ConditionMax);
      ((InventoryItem)var3).setCondition(this.ConditionMax, false);
      ((InventoryItem)var3).setCanBeActivated(this.ActivatedItem);
      ((InventoryItem)var3).setLightStrength(this.LightStrength);
      ((InventoryItem)var3).setTorchCone(this.TorchCone);
      ((InventoryItem)var3).setLightDistance(this.LightDistance);
      ((InventoryItem)var3).setActualWeight(this.ActualWeight);
      ((InventoryItem)var3).setWeight(this.ActualWeight);
      ((InventoryItem)var3).setScriptItem(this);
      ((InventoryItem)var3).setBoredomChange(this.BoredomChange);
      ((InventoryItem)var3).setStressChange(this.StressChange / 100.0F);
      ((InventoryItem)var3).setUnhappyChange(this.UnhappyChange);
      ((InventoryItem)var3).setReplaceOnUseOn(this.ReplaceOnUseOn);
      ((InventoryItem)var3).setRequireInHandOrInventory(this.RequireInHandOrInventory);
      ((InventoryItem)var3).setAttachmentsProvided(this.attachmentsProvided);
      ((InventoryItem)var3).setAttachmentReplacement(this.attachmentReplacement);
      ((InventoryItem)var3).CanStack = this.CanStack;
      ((InventoryItem)var3).copyModData(this.DefaultModData);
      ((InventoryItem)var3).setCount(this.Count);
      ((InventoryItem)var3).setFatigueChange(this.FatigueChange / 100.0F);
      ((InventoryItem)var3).setTooltip(this.Tooltip);
      ((InventoryItem)var3).setDisplayCategory(this.DisplayCategory);
      ((InventoryItem)var3).setAlcoholic(this.Alcoholic);
      ((InventoryItem)var3).RequiresEquippedBothHands = this.RequiresEquippedBothHands;
      ((InventoryItem)var3).setBreakSound(this.breakSound);
      ((InventoryItem)var3).setReplaceOnUse(this.ReplaceOnUse);
      ((InventoryItem)var3).setBandagePower(this.bandagePower);
      ((InventoryItem)var3).setReduceInfectionPower(this.ReduceInfectionPower);
      ((InventoryItem)var3).setCanBeRemote(this.canBeRemote);
      ((InventoryItem)var3).setRemoteController(this.remoteController);
      ((InventoryItem)var3).setRemoteRange(this.remoteRange);
      ((InventoryItem)var3).setCountDownSound(this.countDownSound);
      ((InventoryItem)var3).setExplosionSound(this.explosionSound);
      ((InventoryItem)var3).setColorRed((float)this.colorRed / 255.0F);
      ((InventoryItem)var3).setColorGreen((float)this.colorGreen / 255.0F);
      ((InventoryItem)var3).setColorBlue((float)this.colorBlue / 255.0F);
      ((InventoryItem)var3).setEvolvedRecipeName(this.evolvedRecipeName);
      ((InventoryItem)var3).setMetalValue(this.metalValue);
      ((InventoryItem)var3).setWet(this.isWet);
      ((InventoryItem)var3).setWetCooldown(this.wetCooldown);
      ((InventoryItem)var3).setItemWhenDry(this.itemWhenDry);
      ((InventoryItem)var3).setItemCapacity((float)this.itemCapacity);
      ((InventoryItem)var3).setMaxCapacity(this.maxCapacity);
      ((InventoryItem)var3).setBrakeForce(this.brakeForce);
      ((InventoryItem)var3).setDurability(this.durability);
      ((InventoryItem)var3).setChanceToSpawnDamaged(this.chanceToSpawnDamaged);
      ((InventoryItem)var3).setConditionLowerNormal(this.conditionLowerNormal);
      ((InventoryItem)var3).setConditionLowerOffroad(this.conditionLowerOffroad);
      ((InventoryItem)var3).setWheelFriction(this.wheelFriction);
      ((InventoryItem)var3).setSuspensionCompression(this.suspensionCompression);
      ((InventoryItem)var3).setEngineLoudness(this.engineLoudness);
      ((InventoryItem)var3).setSuspensionDamping(this.suspensionDamping);
      if (this.CustomContextMenu != null) {
         ((InventoryItem)var3).setCustomMenuOption(Translator.getText("ContextMenu_" + this.CustomContextMenu));
      }

      if (this.IconsForTexture != null && !this.IconsForTexture.isEmpty()) {
         ((InventoryItem)var3).setIconsForTexture(this.IconsForTexture);
      }

      ((InventoryItem)var3).setBloodClothingType(this.bloodClothingType);
      ((InventoryItem)var3).CloseKillMove = this.CloseKillMove;
      ((InventoryItem)var3).setAmmoType(this.AmmoType);
      ((InventoryItem)var3).setMaxAmmo(this.maxAmmo);
      ((InventoryItem)var3).setGunType(this.GunType);
      ((InventoryItem)var3).setAttachmentType(this.attachmentType);
      if (this.iconColorMask != null) {
         ((InventoryItem)var3).setTextureColorMask("Item_" + this.iconColorMask);
      }

      if (this.iconFluidMask != null) {
         ((InventoryItem)var3).setTextureFluidMask("Item_" + this.iconFluidMask);
      }

      if (this.staticModelsByIndex != null && !this.staticModelsByIndex.isEmpty()) {
         ((InventoryItem)var3).setStaticModelsByIndex(this.staticModelsByIndex);
      }

      if (this.worldStaticModelsByIndex != null && !this.worldStaticModelsByIndex.isEmpty()) {
         ((InventoryItem)var3).setWorldStaticModelsByIndex(this.worldStaticModelsByIndex);
      }

      GameEntityFactory.CreateInventoryItemEntity((InventoryItem)var3, this, var2);
      long var21 = OutfitRNG.getSeed();
      OutfitRNG.setSeed((long)Rand.Next(2147483647));
      ((InventoryItem)var3).synchWithVisual();
      OutfitRNG.setSeed(var21);
      ((InventoryItem)var3).setRegistry_id(this);
      ItemConfigurator.ConfigureItemOnCreate((InventoryItem)var3);
      Thread var15 = Thread.currentThread();
      if ((var15 == GameWindow.GameThread || var15 == GameLoadingState.loader || var15 == GameServer.MainThread) && !((InventoryItem)var3).isInitialised()) {
         ((InventoryItem)var3).initialiseItem();
      }

      return (InventoryItem)var3;
   }

   public void DoParam(String var1) {
      if (var1.trim().length() != 0) {
         try {
            String[] var2 = var1.split("=");
            String var3 = var2[0].trim();
            String var4 = var2[1].trim();
            this.DoParam(var3, var4);
         } catch (Exception var5) {
            var5.printStackTrace();
            throw new InvalidParameterException(var5.getMessage());
         }
      }
   }

   public void DoParam(String var1, String var2) {
      try {
         AttributeType var3 = Attribute.TypeFromName(var1.trim());
         if (var3 != null) {
            this.LoadAttribute(var1.trim(), var2.trim());
            if (var3 == Attribute.HeadCondition) {
               this.LoadAttribute("TimesHeadRepaired", "0");
            }
         } else if (var1.trim().equalsIgnoreCase("BodyLocation")) {
            this.BodyLocation = var2.trim();
         } else {
            String[] var4;
            int var5;
            if (var1.trim().equalsIgnoreCase("Palettes")) {
               var4 = var2.split("/");

               for(var5 = 0; var5 < var4.length; ++var5) {
                  this.PaletteChoices.add(var4[var5].trim());
               }
            } else if (var1.trim().equalsIgnoreCase("HitSound")) {
               this.HitSound = var2.trim();
               if (this.HitSound.equals("null")) {
                  this.HitSound = null;
               }
            } else if (var1.trim().equalsIgnoreCase("HitFloorSound")) {
               this.hitFloorSound = var2.trim();
            } else if (var1.trim().equalsIgnoreCase("PalettesStart")) {
               this.PalettesStart = var2.trim();
            } else if (var1.trim().equalsIgnoreCase("DisplayName")) {
               this.DisplayName = var2.trim();
            } else if (var1.trim().equalsIgnoreCase("MetalValue")) {
               this.metalValue = Float.valueOf(var2.trim());
            } else if (var1.trim().equalsIgnoreCase("SpriteName")) {
               this.SpriteName = var2.trim();
            } else if (var1.trim().equalsIgnoreCase("Type")) {
               this.type = Item.Type.valueOf(var2.trim());
            } else if (var1.trim().equalsIgnoreCase("SplatSize")) {
               this.SplatSize = Float.parseFloat(var2);
            } else if (var1.trim().equalsIgnoreCase("CanStoreWater")) {
               this.CanStoreWater = var2.equalsIgnoreCase("true");
            } else if (var1.trim().equalsIgnoreCase("Poison")) {
               this.Poison = var2.equalsIgnoreCase("true");
            } else if (var1.trim().equalsIgnoreCase("FoodType")) {
               this.FoodType = var2.trim();
            } else if (var1.trim().equalsIgnoreCase("PoisonDetectionLevel")) {
               this.PoisonDetectionLevel = Integer.parseInt(var2);
            } else if (var1.trim().equalsIgnoreCase("PoisonPower")) {
               this.PoisonPower = Integer.parseInt(var2);
            } else if (var1.trim().equalsIgnoreCase("UseForPoison")) {
               this.UseForPoison = Integer.parseInt(var2);
            } else if (var1.trim().equalsIgnoreCase("SwingAnim")) {
               this.SwingAnim = var2;
            } else {
               String var7;
               String var17;
               if (var1.trim().equalsIgnoreCase("Icon")) {
                  this.Icon = var2;
                  this.ItemName = "Item_" + this.Icon;
                  this.NormalTexture = Texture.trygetTexture(this.ItemName);
                  if (this.NormalTexture == null) {
                     this.NormalTexture = Texture.getSharedTexture("media/inventory/Question_On.png");
                  }

                  this.WorldTextureName = this.ItemName.replace("Item_", "media/inventory/world/WItem_");
                  this.WorldTextureName = this.WorldTextureName + ".png";
                  this.WorldTexture = Texture.getSharedTexture(this.WorldTextureName);
                  if (this.type == Item.Type.Food) {
                     Texture var16 = Texture.trygetTexture(this.ItemName + "Rotten");
                     var17 = this.WorldTextureName.replace(".png", "Rotten.png");
                     if (var16 == null) {
                        var16 = Texture.trygetTexture(this.ItemName + "Spoiled");
                        var17 = var17.replace("Rotten.png", "Spoiled.png");
                     }

                     if (var16 == null) {
                        var16 = Texture.trygetTexture(this.ItemName + "_Rotten");
                        var17 = var17.replace("Rotten.png", "_Rotten.png");
                     }

                     this.SpecialWorldTextureNames.add(var17);
                     this.SpecialTextures.add(var16);
                     this.SpecialTextures.add(Texture.trygetTexture(this.ItemName + "Cooked"));
                     this.SpecialWorldTextureNames.add(this.WorldTextureName.replace(".png", "Cooked.png"));
                     Texture var6 = Texture.trygetTexture(this.ItemName + "Overdone");
                     var7 = this.WorldTextureName.replace(".png", "Overdone.png");
                     if (var6 == null) {
                        var6 = Texture.trygetTexture(this.ItemName + "Burnt");
                        var7 = var7.replace("Overdone.png", "Burnt.png");
                     }

                     if (var6 == null) {
                        var6 = Texture.trygetTexture(this.ItemName + "_Burnt");
                        var7 = var7.replace("Overdone.png", "_Burnt.png");
                     }

                     this.SpecialTextures.add(var6);
                     this.SpecialWorldTextureNames.add(var7);
                  }
               } else if (var1.trim().equalsIgnoreCase("UseWorldItem")) {
                  this.UseWorldItem = Boolean.parseBoolean(var2);
               } else if (var1.trim().equalsIgnoreCase("Medical")) {
                  this.Medical = Boolean.parseBoolean(var2);
               } else if (var1.trim().equalsIgnoreCase("CannedFood")) {
                  this.CannedFood = Boolean.parseBoolean(var2);
               } else if (var1.trim().equalsIgnoreCase("MechanicsItem")) {
                  this.MechanicsItem = Boolean.parseBoolean(var2);
               } else if (var1.trim().equalsIgnoreCase("SurvivalGear")) {
                  this.SurvivalGear = Boolean.parseBoolean(var2);
               } else if (var1.trim().equalsIgnoreCase("ScaleWorldIcon")) {
                  this.ScaleWorldIcon = Float.parseFloat(var2);
               } else if (var1.trim().equalsIgnoreCase("DoorHitSound")) {
                  this.DoorHitSound = var2;
               } else if (var1.trim().equalsIgnoreCase("Weight")) {
                  this.ActualWeight = Float.parseFloat(var2);
                  if (this.ActualWeight < 0.0F) {
                     this.ActualWeight = 0.0F;
                  }
               } else if (var1.trim().equalsIgnoreCase("WeightWet")) {
                  this.WeightWet = Float.parseFloat(var2);
               } else if (var1.trim().equalsIgnoreCase("WeightEmpty")) {
                  this.WeightEmpty = Float.parseFloat(var2);
               } else if (var1.trim().equalsIgnoreCase("HungerChange")) {
                  this.HungerChange = Float.parseFloat(var2);
               } else if (var1.trim().equalsIgnoreCase("ThirstChange")) {
                  this.ThirstChange = Float.parseFloat(var2);
               } else if (var1.trim().equalsIgnoreCase("FatigueChange")) {
                  this.FatigueChange = Float.parseFloat(var2);
               } else if (var1.trim().equalsIgnoreCase("EnduranceChange")) {
                  this.EnduranceChange = Float.parseFloat(var2);
               } else if (var1.trim().equalsIgnoreCase("CriticalChance")) {
                  this.CriticalChance = Float.parseFloat(var2);
               } else if (var1.trim().equalsIgnoreCase("critDmgMultiplier")) {
                  this.critDmgMultiplier = Float.parseFloat(var2);
               } else if (var1.trim().equalsIgnoreCase("DaysFresh")) {
                  this.DaysFresh = Integer.parseInt(var2);
               } else if (var1.trim().equalsIgnoreCase("MilkReplaceItem")) {
                  this.MilkReplaceItem = var2;
               } else if (var1.trim().equalsIgnoreCase("MaxMilk")) {
                  this.MaxMilk = Integer.parseInt(var2);
               } else if (var1.trim().equalsIgnoreCase("AnimalFeedType")) {
                  this.AnimalFeedType = var2;
               } else if (var1.trim().equalsIgnoreCase("DaysTotallyRotten")) {
                  this.DaysTotallyRotten = Integer.parseInt(var2);
               } else if (var1.trim().equalsIgnoreCase("IsCookable")) {
                  this.IsCookable = var2.equalsIgnoreCase("true");
               } else if (var1.trim().equalsIgnoreCase("CookingSound")) {
                  this.CookingSound = var2;
               } else if (var1.trim().equalsIgnoreCase("MinutesToCook")) {
                  this.MinutesToCook = Integer.parseInt(var2);
               } else if (var1.trim().equalsIgnoreCase("MinutesToBurn")) {
                  this.MinutesToBurn = Integer.parseInt(var2);
               } else if (var1.trim().equalsIgnoreCase("BoredomChange")) {
                  this.BoredomChange = (float)Integer.parseInt(var2);
               } else if (var1.trim().equalsIgnoreCase("StressChange")) {
                  this.StressChange = (float)Integer.parseInt(var2);
               } else if (var1.trim().equalsIgnoreCase("UnhappyChange")) {
                  this.UnhappyChange = (float)Integer.parseInt(var2);
               } else if (var1.trim().equalsIgnoreCase("RemoveUnhappinessWhenCooked")) {
                  this.RemoveUnhappinessWhenCooked = Boolean.parseBoolean(var2);
               } else if (var1.trim().equalsIgnoreCase("ReplaceOnDeplete")) {
                  this.ReplaceOnDeplete = var2;
               } else {
                  String var19;
                  if (var1.trim().equalsIgnoreCase("ReplaceOnUseOn")) {
                     this.ReplaceOnUseOn = var2;
                     if (var2.contains("-")) {
                        var4 = var2.split("-");
                        var17 = var4[0].trim();
                        var19 = var4[1].trim();
                        if (!var17.isEmpty() && !var19.isEmpty()) {
                           if (this.ReplaceTypesMap == null) {
                              this.ReplaceTypesMap = new HashMap();
                           }

                           this.ReplaceTypesMap.put(var17, var19);
                        }
                     }
                  } else {
                     String var8;
                     String var11;
                     String[] var18;
                     int var20;
                     if (var1.trim().equalsIgnoreCase("ReplaceTypes")) {
                        this.ReplaceTypes = var2;
                        var4 = var2.split(";");
                        var18 = var4;
                        var20 = var4.length;

                        for(int var21 = 0; var21 < var20; ++var21) {
                           var8 = var18[var21];
                           String[] var9 = var8.trim().split("\\s+");
                           if (var9.length == 2) {
                              String var10 = var9[0].trim();
                              var11 = var9[1].trim();
                              if (!var10.isEmpty() && !var11.isEmpty()) {
                                 if (this.ReplaceTypesMap == null) {
                                    this.ReplaceTypesMap = new HashMap();
                                 }

                                 this.ReplaceTypesMap.put(var10, var11);
                              }
                           }
                        }
                     } else if (var1.trim().equalsIgnoreCase("Ranged")) {
                        this.Ranged = var2.equalsIgnoreCase("true");
                     } else if (var1.trim().equalsIgnoreCase("UseSelf")) {
                        this.UseSelf = var2.equalsIgnoreCase("true");
                     } else if (var1.trim().equalsIgnoreCase("OtherHandUse")) {
                        this.OtherHandUse = var2.equalsIgnoreCase("true");
                     } else if (var1.trim().equalsIgnoreCase("DangerousUncooked")) {
                        this.DangerousUncooked = var2.equalsIgnoreCase("true");
                     } else if (var1.trim().equalsIgnoreCase("MaxRange")) {
                        this.MaxRange = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("MinRange")) {
                        this.MinRange = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("MinAngle")) {
                        this.MinAngle = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("MaxDamage")) {
                        this.MaxDamage = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("BaseSpeed")) {
                        this.baseSpeed = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("stompPower")) {
                        this.stompPower = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("combatSpeedModifier")) {
                        this.combatSpeedModifier = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("runSpeedModifier")) {
                        this.runSpeedModifier = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("clothingItemExtra")) {
                        this.clothingItemExtra = new ArrayList();
                        var4 = var2.split(";");

                        for(var5 = 0; var5 < var4.length; ++var5) {
                           this.clothingItemExtra.add(var4[var5]);
                        }
                     } else if (var1.trim().equalsIgnoreCase("clothingExtraSubmenu")) {
                        this.clothingExtraSubmenu = var2;
                     } else if (var1.trim().equalsIgnoreCase("removeOnBroken")) {
                        this.removeOnBroken = Boolean.parseBoolean(var2);
                     } else if (var1.trim().equalsIgnoreCase("canHaveHoles")) {
                        this.canHaveHoles = Boolean.parseBoolean(var2);
                     } else if (var1.trim().equalsIgnoreCase("Cosmetic")) {
                        this.cosmetic = Boolean.parseBoolean(var2);
                     } else if (var1.trim().equalsIgnoreCase("ammoBox")) {
                        this.ammoBox = var2;
                     } else if (var1.trim().equalsIgnoreCase("InsertAmmoStartSound")) {
                        this.insertAmmoStartSound = StringUtils.discardNullOrWhitespace(var2);
                     } else if (var1.trim().equalsIgnoreCase("InsertAmmoSound")) {
                        this.insertAmmoSound = StringUtils.discardNullOrWhitespace(var2);
                     } else if (var1.trim().equalsIgnoreCase("InsertAmmoStopSound")) {
                        this.insertAmmoStopSound = StringUtils.discardNullOrWhitespace(var2);
                     } else if (var1.trim().equalsIgnoreCase("EjectAmmoStartSound")) {
                        this.ejectAmmoStartSound = StringUtils.discardNullOrWhitespace(var2);
                     } else if (var1.trim().equalsIgnoreCase("EjectAmmoSound")) {
                        this.ejectAmmoSound = StringUtils.discardNullOrWhitespace(var2);
                     } else if (var1.trim().equalsIgnoreCase("EjectAmmoStopSound")) {
                        this.ejectAmmoStopSound = StringUtils.discardNullOrWhitespace(var2);
                     } else if (var1.trim().equalsIgnoreCase("rackSound")) {
                        this.rackSound = var2;
                     } else if (var1.trim().equalsIgnoreCase("clickSound")) {
                        this.clickSound = var2;
                     } else if (var1.equalsIgnoreCase("BringToBearSound")) {
                        this.bringToBearSound = StringUtils.discardNullOrWhitespace(var2);
                     } else if (var1.equalsIgnoreCase("EquipSound")) {
                        this.equipSound = StringUtils.discardNullOrWhitespace(var2);
                     } else if (var1.equalsIgnoreCase("UnequipSound")) {
                        this.unequipSound = StringUtils.discardNullOrWhitespace(var2);
                     } else if (var1.trim().equalsIgnoreCase("magazineType")) {
                        this.magazineType = var2;
                     } else if (var1.trim().equalsIgnoreCase("jamGunChance")) {
                        this.jamGunChance = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("modelWeaponPart")) {
                        if (this.modelWeaponPart == null) {
                           this.modelWeaponPart = new ArrayList();
                        }

                        var4 = var2.split("\\s+");
                        if (var4.length >= 2 && var4.length <= 4) {
                           ModelWeaponPart var22 = null;

                           for(var20 = 0; var20 < this.modelWeaponPart.size(); ++var20) {
                              ModelWeaponPart var24 = (ModelWeaponPart)this.modelWeaponPart.get(var20);
                              if (var24.partType.equals(var4[0])) {
                                 var22 = var24;
                                 break;
                              }
                           }

                           if (var22 == null) {
                              var22 = new ModelWeaponPart();
                           }

                           var22.partType = var4[0];
                           var22.modelName = var4[1];
                           var22.attachmentNameSelf = var4.length > 2 ? var4[2] : null;
                           var22.attachmentParent = var4.length > 3 ? var4[3] : null;
                           if (!var22.partType.contains(".")) {
                              var22.partType = this.getModule().name + "." + var22.partType;
                           }

                           if (!var22.modelName.contains(".")) {
                              var22.modelName = this.getModule().name + "." + var22.modelName;
                           }

                           if ("none".equalsIgnoreCase(var22.attachmentNameSelf)) {
                              var22.attachmentNameSelf = null;
                           }

                           if ("none".equalsIgnoreCase(var22.attachmentParent)) {
                              var22.attachmentParent = null;
                           }

                           this.modelWeaponPart.add(var22);
                        }
                     } else if (var1.trim().equalsIgnoreCase("rackAfterShoot")) {
                        this.rackAfterShoot = Boolean.parseBoolean(var2);
                     } else if (var1.trim().equalsIgnoreCase("haveChamber")) {
                        this.haveChamber = Boolean.parseBoolean(var2);
                     } else if (var1.trim().equalsIgnoreCase("isDung")) {
                        this.isDung = Boolean.parseBoolean(var2);
                     } else if (var1.equalsIgnoreCase("ManuallyRemoveSpentRounds")) {
                        this.manuallyRemoveSpentRounds = Boolean.parseBoolean(var2);
                     } else if (var1.trim().equalsIgnoreCase("biteDefense")) {
                        this.biteDefense = Float.parseFloat(var2);
                        this.biteDefense = Math.min(this.biteDefense, 100.0F);
                     } else if (var1.trim().equalsIgnoreCase("bulletDefense")) {
                        this.bulletDefense = Float.parseFloat(var2);
                        this.bulletDefense = Math.min(this.bulletDefense, 100.0F);
                     } else if (var1.trim().equalsIgnoreCase("scratchDefense")) {
                        this.scratchDefense = Float.parseFloat(var2);
                        this.scratchDefense = Math.min(this.scratchDefense, 100.0F);
                     } else if (var1.trim().equalsIgnoreCase("neckProtectionModifier")) {
                        this.neckProtectionModifier = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("damageCategory")) {
                        this.damageCategory = var2;
                     } else if (var1.trim().equalsIgnoreCase("fireMode")) {
                        this.fireMode = var2;
                     } else if (var1.trim().equalsIgnoreCase("damageMakeHole")) {
                        this.damageMakeHole = Boolean.parseBoolean(var2);
                     } else if (var1.trim().equalsIgnoreCase("equippedNoSprint")) {
                        this.equippedNoSprint = Boolean.parseBoolean(var2);
                     } else if (var1.trim().equalsIgnoreCase("corpseSicknessDefense")) {
                        this.corpseSicknessDefense = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("weaponReloadType")) {
                        this.weaponReloadType = var2;
                     } else if (var1.trim().equalsIgnoreCase("insertAllBulletsReload")) {
                        this.insertAllBulletsReload = Boolean.parseBoolean(var2);
                     } else if (var1.trim().equalsIgnoreCase("clothingItemExtraOption")) {
                        this.clothingItemExtraOption = new ArrayList();
                        var4 = var2.split(";");

                        for(var5 = 0; var5 < var4.length; ++var5) {
                           this.clothingItemExtraOption.add(var4[var5]);
                        }
                     } else if (var1.trim().equalsIgnoreCase("ConditionLowerChanceOneIn")) {
                        this.ConditionLowerChance = Integer.parseInt(var2);
                     } else if (var1.trim().equalsIgnoreCase("HeadConditionLowerChanceMultiplier")) {
                        this.HeadConditionLowerChanceMultiplier = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("MultipleHitConditionAffected")) {
                        this.MultipleHitConditionAffected = var2.equalsIgnoreCase("true");
                     } else if (var1.trim().equalsIgnoreCase("CanBandage")) {
                        this.CanBandage = var2.equalsIgnoreCase("true");
                     } else if (var1.trim().equalsIgnoreCase("ConditionMax")) {
                        this.ConditionMax = Integer.parseInt(var2);
                     } else if (var1.trim().equalsIgnoreCase("SoundGain")) {
                        this.SoundGain = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("MinDamage")) {
                        this.MinDamage = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("MinimumSwingTime")) {
                        this.MinimumSwingTime = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("SwingSound")) {
                        this.SwingSound = var2;
                     } else if (var1.trim().equalsIgnoreCase("ReplaceOnUse")) {
                        this.ReplaceOnUse = var2;
                     } else if (var1.trim().equalsIgnoreCase("WeaponSprite")) {
                        this.WeaponSprite = var2;
                     } else if (var1.trim().equalsIgnoreCase("weaponSpritesByIndex")) {
                        this.weaponSpritesByIndex = new ArrayList();
                        var4 = var2.split(";");

                        for(var5 = 0; var5 < var4.length; ++var5) {
                           this.weaponSpritesByIndex.add(var4[var5].trim());
                        }
                     } else if (var1.trim().equalsIgnoreCase("AimingPerkCritModifier")) {
                        this.AimingPerkCritModifier = Integer.parseInt(var2);
                     } else if (var1.trim().equalsIgnoreCase("AimingPerkRangeModifier")) {
                        this.AimingPerkRangeModifier = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("AimingPerkHitChanceModifier")) {
                        this.AimingPerkHitChanceModifier = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("ProjectileSpreadModifier")) {
                        this.projectileSpreadModifier = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("WeightModifier")) {
                        this.weightModifier = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("AimingPerkMinAngleModifier")) {
                        this.AimingPerkMinAngleModifier = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("HitChance")) {
                        this.HitChance = Integer.parseInt(var2);
                     } else if (var1.trim().equalsIgnoreCase("RecoilDelay")) {
                        this.RecoilDelay = Integer.parseInt(var2);
                     } else if (var1.trim().equalsIgnoreCase("StopPower")) {
                        this.stopPower = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("PiercingBullets")) {
                        this.PiercingBullets = var2.equalsIgnoreCase("true");
                     } else if (var1.trim().equalsIgnoreCase("AngleFalloff")) {
                        this.AngleFalloff = var2.equalsIgnoreCase("true");
                     } else if (var1.trim().equalsIgnoreCase("SoundVolume")) {
                        this.SoundVolume = Integer.parseInt(var2);
                     } else if (var1.trim().equalsIgnoreCase("ToHitModifier")) {
                        this.ToHitModifier = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("SoundRadius")) {
                        this.SoundRadius = Integer.parseInt(var2);
                     } else if (var1.trim().equalsIgnoreCase("Categories")) {
                        var4 = var2.split(";");

                        for(var5 = 0; var5 < var4.length; ++var5) {
                           this.Categories.add(var4[var5].trim());
                        }
                     } else if (var1.trim().equalsIgnoreCase("Tags")) {
                        var4 = var2.split(";");

                        for(var5 = 0; var5 < var4.length; ++var5) {
                           this.Tags.add(var4[var5].trim());
                        }
                     } else if (var1.trim().equalsIgnoreCase("OtherCharacterVolumeBoost")) {
                        this.OtherCharacterVolumeBoost = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("ImpactSound")) {
                        this.ImpactSound = var2;
                        if (this.ImpactSound.equals("null")) {
                           this.ImpactSound = null;
                        }
                     } else if (var1.trim().equalsIgnoreCase("SwingTime")) {
                        this.SwingTime = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("KnockBackOnNoDeath")) {
                        this.KnockBackOnNoDeath = var2.equalsIgnoreCase("true");
                     } else if (var1.trim().equalsIgnoreCase("Alcoholic")) {
                        this.Alcoholic = var2.equalsIgnoreCase("true");
                     } else if (var1.trim().equalsIgnoreCase("SplatBloodOnNoDeath")) {
                        this.SplatBloodOnNoDeath = var2.equalsIgnoreCase("true");
                     } else if (var1.trim().equalsIgnoreCase("SwingAmountBeforeImpact")) {
                        this.SwingAmountBeforeImpact = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("AmmoType")) {
                        this.AmmoType = var2;
                     } else if (var1.trim().equalsIgnoreCase("maxAmmo")) {
                        this.maxAmmo = Integer.parseInt(var2);
                     } else if (var1.trim().equalsIgnoreCase("GunType")) {
                        this.GunType = var2;
                     } else if (var1.trim().equalsIgnoreCase("HitAngleMod")) {
                        this.HitAngleMod = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("OtherHandRequire")) {
                        this.OtherHandRequire = var2;
                     } else if (var1.trim().equalsIgnoreCase("AlwaysWelcomeGift")) {
                        this.AlwaysWelcomeGift = var2.equalsIgnoreCase("true");
                     } else if (var1.trim().equalsIgnoreCase("CantAttackWithLowestEndurance")) {
                        this.CantAttackWithLowestEndurance = var2.equalsIgnoreCase("true");
                     } else if (var1.trim().equalsIgnoreCase("EnduranceMod")) {
                        this.EnduranceMod = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("KnockdownMod")) {
                        this.KnockdownMod = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("DoorDamage")) {
                        this.DoorDamage = Integer.parseInt(var2);
                     } else if (var1.trim().equalsIgnoreCase("MaxHitCount")) {
                        this.MaxHitCount = Integer.parseInt(var2);
                     } else if (var1.trim().equalsIgnoreCase("PhysicsObject")) {
                        this.PhysicsObject = var2;
                     } else if (var1.trim().equalsIgnoreCase("Count")) {
                        this.Count = Integer.parseInt(var2);
                     } else if (var1.trim().equalsIgnoreCase("SwingAnim")) {
                        this.SwingAnim = var2;
                     } else if (var1.trim().equalsIgnoreCase("WeaponWeight")) {
                        this.WeaponWeight = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("IdleAnim")) {
                        this.IdleAnim = var2;
                     } else if (var1.trim().equalsIgnoreCase("RunAnim")) {
                        this.RunAnim = var2;
                     } else if (var1.trim().equalsIgnoreCase("RequireInHandOrInventory")) {
                        this.RequireInHandOrInventory = new ArrayList(Arrays.asList(var2.split("/")));
                     } else if (var1.trim().equalsIgnoreCase("fireModePossibilities")) {
                        this.fireModePossibilities = new ArrayList(Arrays.asList(var2.split("/")));
                     } else if (var1.trim().equalsIgnoreCase("attachmentsProvided")) {
                        this.attachmentsProvided = new ArrayList(Arrays.asList(var2.split(";")));
                     } else if (var1.trim().equalsIgnoreCase("attachmentReplacement")) {
                        this.attachmentReplacement = var2.trim();
                     } else if (var1.trim().equalsIgnoreCase("PushBackMod")) {
                        this.PushBackMod = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("NPCSoundBoost")) {
                        this.NPCSoundBoost = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("SplatNumber")) {
                        this.SplatNumber = Integer.parseInt(var2);
                     } else if (var1.trim().equalsIgnoreCase("RangeFalloff")) {
                        this.RangeFalloff = var2.equalsIgnoreCase("true");
                     } else if (var1.trim().equalsIgnoreCase("UseEndurance")) {
                        this.UseEndurance = var2.equalsIgnoreCase("true");
                     } else if (var1.trim().equalsIgnoreCase("ShareDamage")) {
                        this.ShareDamage = var2.equalsIgnoreCase("true");
                     } else if (var1.trim().equalsIgnoreCase("ShareEndurance")) {
                        this.ShareEndurance = var2.equalsIgnoreCase("true");
                     } else if (var1.trim().equalsIgnoreCase("AlwaysKnockdown")) {
                        this.AlwaysKnockdown = var2.equalsIgnoreCase("true");
                     } else if (var1.trim().equalsIgnoreCase("IsAimedFirearm")) {
                        this.IsAimedFirearm = var2.equalsIgnoreCase("true");
                     } else if (var1.trim().equalsIgnoreCase("bulletOutSound")) {
                        this.bulletOutSound = var2.trim();
                     } else if (var1.trim().equalsIgnoreCase("ShellFallSound")) {
                        this.ShellFallSound = var2.trim();
                     } else if (var1.equalsIgnoreCase("DropSound")) {
                        this.DropSound = StringUtils.discardNullOrWhitespace(var2);
                     } else if (var1.trim().equalsIgnoreCase("SoundMap")) {
                        var4 = var2.split("\\s+");
                        if (var4.length == 2 && !var4[0].trim().isEmpty()) {
                           if (this.SoundMap == null) {
                              this.SoundMap = new HashMap();
                           }

                           this.SoundMap.put(var4[0].trim(), var4[1].trim());
                        }
                     } else if (var1.trim().equalsIgnoreCase("IsAimedHandWeapon")) {
                        this.IsAimedHandWeapon = var2.equalsIgnoreCase("true");
                     } else if (var1.trim().equalsIgnoreCase("AimingMod")) {
                        this.AimingMod = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("ProjectileCount")) {
                        this.ProjectileCount = Integer.parseInt(var2);
                     } else if (var1.trim().equalsIgnoreCase("ProjectileSpread")) {
                        this.projectileSpread = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("ProjectileWeightCenter")) {
                        this.projectileWeightCenter = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("CanStack")) {
                        this.CanStack = var2.equalsIgnoreCase("true");
                     } else if (var1.trim().equalsIgnoreCase("HerbalistType")) {
                        this.HerbalistType = var2.trim();
                     } else if (var1.trim().equalsIgnoreCase("CanBarricade")) {
                        this.CanBarricade = var2.equalsIgnoreCase("true");
                     } else if (var1.trim().equalsIgnoreCase("UseWhileEquipped")) {
                        this.UseWhileEquipped = var2.equalsIgnoreCase("true");
                     } else if (var1.trim().equalsIgnoreCase("UseWhileUnequipped")) {
                        this.UseWhileUnequipped = var2.equalsIgnoreCase("true");
                     } else if (var1.trim().equalsIgnoreCase("TicksPerEquipUse")) {
                        this.TicksPerEquipUse = Integer.parseInt(var2.trim());
                     } else if (var1.trim().equalsIgnoreCase("DisappearOnUse")) {
                        this.DisappearOnUse = var2.equalsIgnoreCase("true");
                     } else if (var1.trim().equalsIgnoreCase("Temperature")) {
                        this.Temperature = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("Insulation")) {
                        this.insulation = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("WindResistance")) {
                        this.windresist = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("WaterResistance")) {
                        this.waterresist = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("CloseKillMove")) {
                        this.CloseKillMove = var2.trim();
                     } else if (var1.trim().equalsIgnoreCase("UseDelta")) {
                        this.UseDelta = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("TorchDot")) {
                        this.torchDot = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("NumberOfPages")) {
                        this.NumberOfPages = Integer.parseInt(var2.trim());
                     } else if (var1.trim().equalsIgnoreCase("SkillTrained")) {
                        this.SkillTrained = var2.trim();
                     } else if (var1.trim().equalsIgnoreCase("LvlSkillTrained")) {
                        this.LvlSkillTrained = Integer.parseInt(var2.trim());
                     } else if (var1.trim().equalsIgnoreCase("NumLevelsTrained")) {
                        this.NumLevelsTrained = Integer.parseInt(var2.trim());
                     } else if (var1.trim().equalsIgnoreCase("Capacity")) {
                        int var23 = Integer.parseInt(var2.trim());
                        if ((float)var23 > 50.0F - this.ActualWeight) {
                           var23 = (int)(50.0F - this.ActualWeight);
                        }

                        this.Capacity = var23;
                     } else if (var1.trim().equalsIgnoreCase("MaxItemSize")) {
                        this.maxItemSize = Float.parseFloat(var2.trim());
                     } else if (var1.trim().equalsIgnoreCase("MaxCapacity")) {
                        this.maxCapacity = Integer.parseInt(var2.trim());
                     } else if (var1.trim().equalsIgnoreCase("ItemCapacity")) {
                        this.itemCapacity = Integer.parseInt(var2.trim());
                     } else if (var1.trim().equalsIgnoreCase("ConditionAffectsCapacity")) {
                        this.ConditionAffectsCapacity = Boolean.parseBoolean(var2.trim());
                     } else if (var1.trim().equalsIgnoreCase("BrakeForce")) {
                        this.brakeForce = (float)Integer.parseInt(var2.trim());
                     } else if (var1.trim().equalsIgnoreCase("Durability")) {
                        this.durability = Float.parseFloat(var2.trim());
                     } else if (var1.trim().equalsIgnoreCase("ChanceToSpawnDamaged")) {
                        this.chanceToSpawnDamaged = Integer.parseInt(var2.trim());
                     } else if (var1.trim().equalsIgnoreCase("WeaponLength")) {
                        this.WeaponLength = Float.valueOf(var2.trim());
                     } else if (var1.trim().equalsIgnoreCase("ClipSize")) {
                        this.ClipSize = Integer.parseInt(var2.trim());
                     } else if (var1.trim().equalsIgnoreCase("ReloadTime")) {
                        this.reloadTime = Integer.parseInt(var2.trim());
                     } else if (var1.trim().equalsIgnoreCase("AimingTime")) {
                        this.aimingTime = Integer.parseInt(var2.trim());
                     } else if (var1.trim().equalsIgnoreCase("AimingTimeModifier")) {
                        this.aimingTimeModifier = Integer.parseInt(var2.trim());
                     } else if (var1.trim().equalsIgnoreCase("ReloadTimeModifier")) {
                        this.reloadTimeModifier = Integer.parseInt(var2.trim());
                     } else if (var1.trim().equalsIgnoreCase("HitChanceModifier")) {
                        this.hitChanceModifier = Integer.parseInt(var2.trim());
                     } else if (var1.trim().equalsIgnoreCase("WeightReduction")) {
                        this.WeightReduction = Integer.parseInt(var2.trim());
                     } else if (var1.trim().equalsIgnoreCase("CanBeEquipped")) {
                        this.CanBeEquipped = var2.trim();
                     } else if (var1.trim().equalsIgnoreCase("SubCategory")) {
                        this.SubCategory = var2.trim();
                     } else if (var1.trim().equalsIgnoreCase("ActivatedItem")) {
                        this.ActivatedItem = var2.equalsIgnoreCase("true");
                     } else if (var1.trim().equalsIgnoreCase("ProtectFromRainWhenEquipped")) {
                        this.ProtectFromRainWhenEquipped = var2.equalsIgnoreCase("true");
                     } else if (var1.trim().equalsIgnoreCase("LightStrength")) {
                        this.LightStrength = Float.valueOf(var2.trim());
                     } else if (var1.trim().equalsIgnoreCase("TorchCone")) {
                        this.TorchCone = var2.equalsIgnoreCase("true");
                     } else if (var1.trim().equalsIgnoreCase("LightDistance")) {
                        this.LightDistance = Integer.parseInt(var2.trim());
                     } else if (var1.trim().equalsIgnoreCase("TwoHandWeapon")) {
                        this.TwoHandWeapon = var2.equalsIgnoreCase("true");
                     } else if (var1.trim().equalsIgnoreCase("Tooltip")) {
                        this.Tooltip = var2.trim();
                     } else if (var1.trim().equalsIgnoreCase("DisplayCategory")) {
                        this.DisplayCategory = var2.trim();
                     } else if (var1.trim().equalsIgnoreCase("BadInMicrowave")) {
                        this.BadInMicrowave = var2.equalsIgnoreCase("true");
                     } else if (var1.trim().equalsIgnoreCase("GoodHot")) {
                        this.GoodHot = var2.equalsIgnoreCase("true");
                     } else if (var1.trim().equalsIgnoreCase("BadCold")) {
                        this.BadCold = var2.equalsIgnoreCase("true");
                     } else if (var1.trim().equalsIgnoreCase("AlarmSound")) {
                        this.AlarmSound = var2.trim();
                     } else if (var1.trim().equalsIgnoreCase("RequiresEquippedBothHands")) {
                        this.RequiresEquippedBothHands = var2.equalsIgnoreCase("true");
                     } else if (var1.trim().equalsIgnoreCase("ReplaceOnCooked")) {
                        this.ReplaceOnCooked = Arrays.asList(var2.trim().split(";"));
                     } else if (var1.trim().equalsIgnoreCase("CustomContextMenu")) {
                        this.CustomContextMenu = var2.trim();
                     } else if (var1.trim().equalsIgnoreCase("Trap")) {
                        this.Trap = Boolean.parseBoolean(var2.trim());
                     } else if (var1.trim().equalsIgnoreCase("Wet")) {
                        this.isWet = var2.equalsIgnoreCase("true");
                     } else if (var1.trim().equalsIgnoreCase("WetCooldown")) {
                        this.wetCooldown = Float.parseFloat(var2.trim());
                     } else if (var1.trim().equalsIgnoreCase("ItemWhenDry")) {
                        this.itemWhenDry = var2.trim();
                     } else if (var1.trim().equalsIgnoreCase("FishingLure")) {
                        this.FishingLure = Boolean.parseBoolean(var2.trim());
                     } else if (var1.trim().equalsIgnoreCase("CanBeWrite")) {
                        this.canBeWrite = Boolean.parseBoolean(var2.trim());
                     } else if (var1.trim().equalsIgnoreCase("PageToWrite")) {
                        this.PageToWrite = Integer.parseInt(var2.trim());
                     } else if (var1.trim().equalsIgnoreCase("Spice")) {
                        this.Spice = var2.trim().equalsIgnoreCase("true");
                     } else if (var1.trim().equalsIgnoreCase("RemoveNegativeEffectOnCooked")) {
                        this.RemoveNegativeEffectOnCooked = var2.trim().equalsIgnoreCase("true");
                     } else if (var1.trim().equalsIgnoreCase("ClipSizeModifier")) {
                        this.clipSizeModifier = Integer.parseInt(var2);
                     } else if (var1.trim().equalsIgnoreCase("RecoilDelayModifier")) {
                        this.recoilDelayModifier = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("MaxRangeModifier")) {
                        this.maxRangeModifier = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("MinSightRange")) {
                        this.minSightRange = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("MaxSightRange")) {
                        this.maxSightRange = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("LowLightBonus")) {
                        this.lowLightBonus = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("DamageModifier")) {
                        this.damageModifier = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("Map")) {
                        this.map = var2.trim();
                     } else if (var1.trim().equalsIgnoreCase("PutInSound")) {
                        this.PutInSound = StringUtils.discardNullOrWhitespace(var2.trim());
                     } else if (var1.trim().equalsIgnoreCase("PlaceOneSound")) {
                        this.PlaceOneSound = StringUtils.discardNullOrWhitespace(var2.trim());
                     } else if (var1.trim().equalsIgnoreCase("PlaceMultipleSound")) {
                        this.PlaceMultipleSound = StringUtils.discardNullOrWhitespace(var2.trim());
                     } else if (var1.trim().equalsIgnoreCase("CloseSound")) {
                        this.CloseSound = var2.trim();
                     } else if (var1.trim().equalsIgnoreCase("OpenSound")) {
                        this.OpenSound = var2.trim();
                     } else if (var1.trim().equalsIgnoreCase("BreakSound")) {
                        this.breakSound = var2.trim();
                     } else if (var1.trim().equalsIgnoreCase("TreeDamage")) {
                        this.treeDamage = Integer.parseInt(var2);
                     } else if (var1.trim().equalsIgnoreCase("CustomEatSound")) {
                        this.customEatSound = var2.trim();
                     } else if (var1.trim().equalsIgnoreCase("FillFromDispenserSound")) {
                        this.fillFromDispenserSound = StringUtils.discardNullOrWhitespace(var2.trim());
                     } else if (var1.trim().equalsIgnoreCase("FillFromLakeSound")) {
                        this.fillFromLakeSound = StringUtils.discardNullOrWhitespace(var2.trim());
                     } else if (var1.trim().equalsIgnoreCase("FillFromTapSound")) {
                        this.fillFromTapSound = StringUtils.discardNullOrWhitespace(var2.trim());
                     } else if (var1.trim().equalsIgnoreCase("FillFromToiletSound")) {
                        this.fillFromToiletSound = StringUtils.discardNullOrWhitespace(var2.trim());
                     } else if (var1.trim().equalsIgnoreCase("AlcoholPower")) {
                        this.alcoholPower = Float.parseFloat(var2.trim());
                     } else if (var1.trim().equalsIgnoreCase("BandagePower")) {
                        this.bandagePower = Float.parseFloat(var2.trim());
                     } else if (var1.trim().equalsIgnoreCase("ReduceInfectionPower")) {
                        this.ReduceInfectionPower = Float.parseFloat(var2.trim());
                     } else if (var1.trim().equalsIgnoreCase("OnCooked")) {
                        this.OnCooked = var2.trim();
                     } else if (var1.trim().equalsIgnoreCase("OnlyAcceptCategory")) {
                        this.OnlyAcceptCategory = StringUtils.discardNullOrWhitespace(var2);
                     } else if (var1.trim().equalsIgnoreCase("AcceptItemFunction")) {
                        this.AcceptItemFunction = StringUtils.discardNullOrWhitespace(var2);
                     } else if (var1.trim().equalsIgnoreCase("Padlock")) {
                        this.padlock = var2.trim().equalsIgnoreCase("true");
                     } else if (var1.trim().equalsIgnoreCase("DigitalPadlock")) {
                        this.digitalPadlock = var2.trim().equalsIgnoreCase("true");
                     } else if (var1.trim().equalsIgnoreCase("triggerExplosionTimer")) {
                        this.triggerExplosionTimer = Integer.parseInt(var2);
                     } else if (var1.trim().equalsIgnoreCase("sensorRange")) {
                        this.sensorRange = Integer.parseInt(var2);
                     } else if (var1.trim().equalsIgnoreCase("remoteRange")) {
                        this.remoteRange = Integer.parseInt(var2);
                     } else if (var1.trim().equalsIgnoreCase("CountDownSound")) {
                        this.countDownSound = var2.trim();
                     } else if (var1.trim().equalsIgnoreCase("explosionSound")) {
                        this.explosionSound = var2.trim();
                     } else if (var1.trim().equalsIgnoreCase("PlacedSprite")) {
                        this.PlacedSprite = var2.trim();
                     } else if (var1.trim().equalsIgnoreCase("explosionTimer")) {
                        this.explosionTimer = Integer.parseInt(var2);
                     } else if (var1.trim().equalsIgnoreCase("explosionDuration")) {
                        this.explosionDuration = Integer.parseInt(var2);
                     } else if (var1.trim().equalsIgnoreCase("explosionRange")) {
                        this.explosionRange = Integer.parseInt(var2);
                     } else if (var1.trim().equalsIgnoreCase("explosionPower")) {
                        this.explosionPower = Integer.parseInt(var2);
                     } else if (var1.trim().equalsIgnoreCase("fireRange")) {
                        this.fireRange = Integer.parseInt(var2);
                     } else if (var1.trim().equalsIgnoreCase("firePower")) {
                        this.firePower = Integer.parseInt(var2);
                     } else if (var1.trim().equalsIgnoreCase("canBePlaced")) {
                        this.canBePlaced = var2.trim().equalsIgnoreCase("true");
                     } else if (var1.trim().equalsIgnoreCase("CanBeReused")) {
                        this.canBeReused = var2.trim().equalsIgnoreCase("true");
                     } else if (var1.trim().equalsIgnoreCase("canBeRemote")) {
                        this.canBeRemote = var2.trim().equalsIgnoreCase("true");
                     } else if (var1.trim().equalsIgnoreCase("remoteController")) {
                        this.remoteController = var2.trim().equalsIgnoreCase("true");
                     } else if (var1.trim().equalsIgnoreCase("smokeRange")) {
                        this.smokeRange = Integer.parseInt(var2);
                     } else if (var1.trim().equalsIgnoreCase("noiseRange")) {
                        this.noiseRange = Integer.parseInt(var2);
                     } else if (var1.trim().equalsIgnoreCase("noiseDuration")) {
                        this.noiseDuration = Integer.parseInt(var2);
                     } else if (var1.trim().equalsIgnoreCase("extraDamage")) {
                        this.extraDamage = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("TwoWay")) {
                        this.twoWay = Boolean.parseBoolean(var2.trim());
                     } else if (var1.trim().equalsIgnoreCase("TransmitRange")) {
                        this.transmitRange = Integer.parseInt(var2);
                     } else if (var1.trim().equalsIgnoreCase("MicRange")) {
                        this.micRange = Integer.parseInt(var2);
                     } else if (var1.trim().equalsIgnoreCase("BaseVolumeRange")) {
                        this.baseVolumeRange = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("IsPortable")) {
                        this.isPortable = Boolean.parseBoolean(var2.trim());
                     } else if (var1.trim().equalsIgnoreCase("IsTelevision")) {
                        this.isTelevision = Boolean.parseBoolean(var2.trim());
                     } else if (var1.trim().equalsIgnoreCase("MinChannel")) {
                        this.minChannel = Integer.parseInt(var2);
                     } else if (var1.trim().equalsIgnoreCase("MaxChannel")) {
                        this.maxChannel = Integer.parseInt(var2);
                     } else if (var1.trim().equalsIgnoreCase("UsesBattery")) {
                        this.usesBattery = Boolean.parseBoolean(var2.trim());
                     } else if (var1.trim().equalsIgnoreCase("IsHighTier")) {
                        this.isHighTier = Boolean.parseBoolean(var2.trim());
                     } else if (var1.trim().equalsIgnoreCase("WorldObjectSprite")) {
                        this.worldObjectSprite = var2.trim();
                     } else if (var1.trim().equalsIgnoreCase("fluReduction")) {
                        this.fluReduction = Integer.parseInt(var2);
                     } else if (var1.trim().equalsIgnoreCase("ReduceFoodSickness")) {
                        this.ReduceFoodSickness = Integer.parseInt(var2);
                     } else if (var1.trim().equalsIgnoreCase("painReduction")) {
                        this.painReduction = Integer.parseInt(var2);
                     } else if (var1.trim().equalsIgnoreCase("ColorRed")) {
                        this.colorRed = Integer.parseInt(var2);
                     } else if (var1.trim().equalsIgnoreCase("ColorGreen")) {
                        this.colorGreen = Integer.parseInt(var2);
                     } else if (var1.trim().equalsIgnoreCase("ColorBlue")) {
                        this.colorBlue = Integer.parseInt(var2);
                     } else if (var1.trim().equalsIgnoreCase("calories")) {
                        this.calories = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("carbohydrates")) {
                        this.carbohydrates = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("lipids")) {
                        this.lipids = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("proteins")) {
                        this.proteins = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("Packaged")) {
                        this.packaged = var2.trim().equalsIgnoreCase("true");
                     } else if (var1.trim().equalsIgnoreCase("CantBeFrozen")) {
                        this.cantBeFrozen = var2.trim().equalsIgnoreCase("true");
                     } else if (var1.trim().equalsIgnoreCase("EvolvedRecipeName")) {
                        Translator.setDefaultItemEvolvedRecipeName(this.getFullName(), var2);
                        this.evolvedRecipeName = Translator.getItemEvolvedRecipeName(this.getFullName());
                     } else if (var1.trim().equalsIgnoreCase("ReplaceOnRotten")) {
                        this.ReplaceOnRotten = var2.trim();
                     } else if (var1.trim().equalsIgnoreCase("CantBeConsolided")) {
                        this.cantBeConsolided = var2.equalsIgnoreCase("true");
                     } else if (var1.trim().equalsIgnoreCase("OnEat")) {
                        this.onEat = var2.trim();
                     } else if (var1.trim().equalsIgnoreCase("KeepOnDeplete")) {
                        this.DisappearOnUse = !var2.equalsIgnoreCase("true");
                     } else if (var1.trim().equalsIgnoreCase("VehicleType")) {
                        this.vehicleType = Integer.parseInt(var2);
                     } else if (var1.trim().equalsIgnoreCase("ChanceToFall")) {
                        this.chanceToFall = Integer.parseInt(var2);
                     } else if (var1.trim().equalsIgnoreCase("conditionLowerOffroad")) {
                        this.conditionLowerOffroad = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("ConditionLowerStandard")) {
                        this.conditionLowerNormal = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("wheelFriction")) {
                        this.wheelFriction = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("suspensionDamping")) {
                        this.suspensionDamping = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("suspensionCompression")) {
                        this.suspensionCompression = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("engineLoudness")) {
                        this.engineLoudness = Float.parseFloat(var2);
                     } else if (var1.trim().equalsIgnoreCase("attachmentType")) {
                        this.attachmentType = var2.trim();
                     } else if (var1.trim().equalsIgnoreCase("makeUpType")) {
                        this.makeUpType = var2.trim();
                     } else if (var1.trim().equalsIgnoreCase("consolidateOption")) {
                        this.consolidateOption = var2.trim();
                     } else if (var1.trim().equalsIgnoreCase("fabricType")) {
                        this.fabricType = var2.trim();
                     } else if (var1.trim().equalsIgnoreCase("TeachedRecipes")) {
                        this.teachedRecipes = new ArrayList();
                        var4 = var2.split(";");

                        for(var5 = 0; var5 < var4.length; ++var5) {
                           var19 = var4[var5].trim();
                           this.teachedRecipes.add(var19);
                           if (Translator.debug) {
                              Translator.getRecipeName(var19);
                           }
                        }
                     } else if (var1.trim().equalsIgnoreCase("MountOn")) {
                        this.mountOn = new ArrayList();
                        var4 = var2.split(";");

                        for(var5 = 0; var5 < var4.length; ++var5) {
                           this.mountOn.add(var4[var5].trim());
                        }
                     } else if (var1.trim().equalsIgnoreCase("PartType")) {
                        this.partType = var2;
                     } else if (var1.trim().equalsIgnoreCase("CanAttach")) {
                        this.canAttachCallback = var2;
                     } else if (var1.trim().equalsIgnoreCase("OnAttach")) {
                        this.onAttachCallback = var2;
                     } else if (var1.trim().equalsIgnoreCase("OnDetach")) {
                        this.onDetachCallback = var2;
                     } else if (var1.trim().equalsIgnoreCase("ClothingItem")) {
                        this.ClothingItem = var2;
                     } else if (var1.trim().equalsIgnoreCase("EvolvedRecipe")) {
                        var4 = var2.split(";");
                        var18 = var2.split(";");

                        for(var20 = 0; var20 < var18.length; ++var20) {
                           this.evolvedRecipe.add(var4[var20].trim());
                        }

                        for(var20 = 0; var20 < var4.length; ++var20) {
                           var7 = var4[var20];
                           var8 = null;
                           int var25 = 0;
                           boolean var26 = false;
                           if (!var7.contains(":")) {
                              var8 = var7;
                           } else {
                              var8 = var7.split(":")[0];
                              var11 = var7.split(":")[1];
                              if (!var11.contains("|")) {
                                 var25 = Integer.parseInt(var7.split(":")[1]);
                              } else {
                                 String[] var12 = var11.split("\\|");

                                 for(int var13 = 0; var13 < var12.length; ++var13) {
                                    if ("Cooked".equals(var12[var13])) {
                                       var26 = true;
                                    }
                                 }

                                 var25 = Integer.parseInt(var12[0]);
                              }
                           }

                           if (var8.equals("RicePot") || var8.equals("RicePan")) {
                              var8 = "Rice";
                           }

                           if (var8.equals("PastaPot") || var8.equals("PastaPan")) {
                              var8 = "Pasta";
                           }

                           if (var8.equals("Roasted Vegetables")) {
                              var8 = "Stir fry";
                           }

                           ItemRecipe var27 = new ItemRecipe(this.name, this.getModule().getName(), var25);
                           var27.cooked = var26;
                           this.itemRecipeMap.put(var8, var27);
                        }
                     } else if (var1.trim().equalsIgnoreCase("StaticModel")) {
                        this.staticModel = var2.trim();
                     } else if (var1.trim().equalsIgnoreCase("worldStaticModel")) {
                        this.worldStaticModel = var2.trim();
                     } else if (var1.trim().equalsIgnoreCase("primaryAnimMask")) {
                        this.primaryAnimMask = var2.trim();
                     } else if (var1.trim().equalsIgnoreCase("secondaryAnimMask")) {
                        this.secondaryAnimMask = var2.trim();
                     } else if (var1.trim().equalsIgnoreCase("primaryAnimMaskAttachment")) {
                        this.primaryAnimMaskAttachment = var2.trim();
                     } else if (var1.trim().equalsIgnoreCase("secondaryAnimMaskAttachment")) {
                        this.secondaryAnimMaskAttachment = var2.trim();
                     } else if (var1.trim().equalsIgnoreCase("replaceInSecondHand")) {
                        this.replaceInSecondHand = var2.trim();
                     } else if (var1.trim().equalsIgnoreCase("replaceInPrimaryHand")) {
                        this.replaceInPrimaryHand = var2.trim();
                     } else if (var1.trim().equalsIgnoreCase("replaceWhenUnequip")) {
                        this.replaceWhenUnequip = var2.trim();
                     } else if (var1.trim().equalsIgnoreCase("EatType")) {
                        this.eatType = var2.trim();
                     } else if (var1.trim().equalsIgnoreCase("PourType")) {
                        this.pourType = var2.trim();
                     } else if (var1.trim().equalsIgnoreCase("ReadType")) {
                        this.readType = var2.trim();
                     } else if (var1.trim().equalsIgnoreCase("DigType")) {
                        this.digType = var2.trim();
                     } else if (var1.trim().equalsIgnoreCase("IconsForTexture")) {
                        this.IconsForTexture = new ArrayList();
                        var4 = var2.split(";");

                        for(var5 = 0; var5 < var4.length; ++var5) {
                           this.IconsForTexture.add(var4[var5].trim());
                        }
                     } else if (var1.trim().equalsIgnoreCase("BloodLocation")) {
                        this.bloodClothingType = new ArrayList();
                        var4 = var2.split(";");

                        for(var5 = 0; var5 < var4.length; ++var5) {
                           this.bloodClothingType.add(BloodClothingType.fromString(var4[var5].trim()));
                        }
                     } else if (var1.trim().equalsIgnoreCase("MediaCategory")) {
                        this.recordedMediaCat = var2.trim();
                     } else if (var1.trim().equalsIgnoreCase("AcceptMediaType")) {
                        this.acceptMediaType = Byte.parseByte(var2.trim());
                     } else if (var1.trim().equalsIgnoreCase("NoTransmit")) {
                        this.noTransmit = Boolean.parseBoolean(var2.trim());
                     } else if (var1.trim().equalsIgnoreCase("WorldRender")) {
                        this.worldRender = Boolean.parseBoolean(var2.trim());
                     } else if (var1.trim().equalsIgnoreCase("CantEat")) {
                        this.CantEat = Boolean.parseBoolean(var2.trim());
                     } else if (var1.trim().equalsIgnoreCase("OBSOLETE")) {
                        this.OBSOLETE = var2.trim().toLowerCase().equals("true");
                     } else if (var1.trim().equalsIgnoreCase("OnCreate")) {
                        this.LuaCreate = var2.trim();
                     } else if (var1.trim().equalsIgnoreCase("ItemConfig")) {
                        this.itemConfigKey = var2.trim();
                     } else if (var1.trim().equalsIgnoreCase("IconColorMask")) {
                        this.iconColorMask = var2.trim();
                     } else if (var1.trim().equalsIgnoreCase("IconFluidMask")) {
                        this.iconFluidMask = var2.trim();
                     } else if (!var1.trim().equalsIgnoreCase("GameEntityScript")) {
                        if (var1.trim().equalsIgnoreCase("SoundParameter")) {
                           var4 = var2.split("\\s+");
                           if (this.soundParameterMap == null) {
                              this.soundParameterMap = new HashMap();
                           }

                           this.soundParameterMap.put(var4[0].trim(), var4[1].trim());
                        } else if (var1.trim().equalsIgnoreCase("VehiclePartModel")) {
                           this.DoParam_VehiclePartModel(var2);
                        } else if (var1.trim().equalsIgnoreCase("withDrainable")) {
                           this.withDrainable = var2;
                        } else if (var1.trim().equalsIgnoreCase("withoutDrainable")) {
                           this.withoutDrainable = var2;
                        } else if (var1.trim().equalsIgnoreCase("staticModelsByIndex")) {
                           this.staticModelsByIndex = new ArrayList();
                           var4 = var2.split(";");

                           for(var5 = 0; var5 < var4.length; ++var5) {
                              this.staticModelsByIndex.add(var4[var5].trim());
                           }
                        } else if (var1.trim().equalsIgnoreCase("worldStaticModelsByIndex")) {
                           this.worldStaticModelsByIndex = new ArrayList();
                           var4 = var2.split(";");

                           for(var5 = 0; var5 < var4.length; ++var5) {
                              this.worldStaticModelsByIndex.add(var4[var5].trim());
                           }
                        } else if (var1.trim().equalsIgnoreCase("spawnWith")) {
                           this.spawnWith = var2;
                        } else if (var1.trim().equalsIgnoreCase("visionModifier")) {
                           this.visionModifier = Float.parseFloat(var2);
                        } else if (var1.trim().equalsIgnoreCase("hearingModifier")) {
                           this.hearingModifier = Float.parseFloat(var2);
                        } else if (var1.trim().equalsIgnoreCase("strainModifier")) {
                           this.strainModifier = Float.parseFloat(var2);
                        } else if (var1.trim().equalsIgnoreCase("OnBreak")) {
                           this.onBreak = var2.trim();
                        } else if (var1.trim().equalsIgnoreCase("DamagedSound")) {
                           this.damagedSound = var2.trim();
                        } else if (var1.trim().equalsIgnoreCase("ShoutType")) {
                           this.shoutType = var2.trim();
                        } else if (var1.trim().equalsIgnoreCase("ShoutMultiplier")) {
                           this.shoutMultiplier = Float.parseFloat(var2);
                        } else if (var1.trim().equalsIgnoreCase("EatTime")) {
                           this.eatTime = Integer.parseInt(var2);
                        } else if (var1.trim().equalsIgnoreCase("VisualAid")) {
                           this.visualAid = Boolean.parseBoolean(var2.trim());
                        } else if (var1.trim().equalsIgnoreCase("DiscomfortModifier")) {
                           this.discomfortModifier = Float.parseFloat(var2);
                        } else if (var1.trim().equalsIgnoreCase("fireFuelRatio")) {
                           this.fireFuelRatio = Float.parseFloat(var2);
                        } else {
                           DebugLogStream var10000 = DebugLog.DetailedInfo;
                           String var10001 = var1.trim();
                           var10000.trace("adding unknown item param \"" + var10001 + "\" = \"" + var2.trim() + "\", script: " + this.getScriptObjectFullType() + ", path: " + this.getFileAbsPath());
                           if (this.DefaultModData == null) {
                              this.DefaultModData = LuaManager.platform.newTable();
                           }

                           try {
                              Double var28 = Double.parseDouble(var2.trim());
                              this.DefaultModData.rawset(var1.trim(), var28);
                           } catch (Exception var14) {
                              this.DefaultModData.rawset(var1.trim(), var2);
                           }
                        }
                     }
                  }
               }
            }
         }

      } catch (Exception var15) {
         String var10002 = var1.trim();
         throw new InvalidParameterException("Error: " + var10002 + " is not a valid parameter in item: " + this.name);
      }
   }

   private void DoParam_VehiclePartModel(String var1) {
      if (this.vehiclePartModels == null) {
         this.vehiclePartModels = new ArrayList();
      }

      String[] var2 = var1.split("\\s+");
      if (var2.length == 3) {
         VehiclePartModel var3 = new VehiclePartModel();
         var3.partId = var2[0];
         var3.partModelId = var2[1];
         var3.modelId = var2[2];
         this.vehiclePartModels.add(var3);
      }
   }

   public void reset() {
      super.reset();
   }

   public void PreReload() {
      super.PreReload();
   }

   public void OnScriptsLoaded(ScriptLoadMode var1) throws Exception {
      ArrayList var2 = ScriptManager.instance.getAllEvolvedRecipesList();
      Iterator var3 = this.itemRecipeMap.entrySet().iterator();

      Map.Entry var4;
      boolean var5;
      String var10002;
      do {
         if (!var3.hasNext()) {
            if (this.DisplayName != null) {
               this.DisplayName = Translator.getDisplayItemName(this.DisplayName);
               this.DisplayName = Translator.getItemNameFromFullType(this.getFullName());
            }

            if (this.itemConfigKey != null) {
               ItemConfig var9 = ScriptManager.instance.getItemConfig(this.itemConfigKey);
               if (var9 == null) {
                  var10002 = this.getItemConfigKey();
                  throw new Exception("Cannot set item config '" + var10002 + "' to item: " + this.getFullName());
               }

               this.setItemConfig(var9);
            }

            super.OnScriptsLoaded(var1);
            return;
         }

         var4 = (Map.Entry)var3.next();
         var5 = false;
         EvolvedRecipe var6 = ScriptManager.instance.getEvolvedRecipe((String)var4.getKey());
         if (var6 != null) {
            var6.itemsList.put(this.name, (ItemRecipe)var4.getValue());
            var5 = true;
         }

         Iterator var7 = var2.iterator();

         while(var7.hasNext()) {
            EvolvedRecipe var8 = (EvolvedRecipe)var7.next();
            if (var8.template.equalsIgnoreCase((String)var4.getKey())) {
               var8.itemsList.put(this.name, (ItemRecipe)var4.getValue());
               var5 = true;
            }
         }
      } while(var5);

      DebugLogStream var10000 = DebugLog.General;
      String var10001 = (String)var4.getKey();
      var10000.error("Could not find evolved recipe or template: '" + var10001 + "' in item = " + this.getFullName());
      var10002 = (String)var4.getKey();
      throw new InvalidParameterException("Could not find evolved recipe or template: '" + var10002 + "' in item: " + this.getFullName());
   }

   public void OnLoadedAfterLua() throws Exception {
      super.OnLoadedAfterLua();
   }

   public void OnPostWorldDictionaryInit() throws Exception {
      super.OnPostWorldDictionaryInit();
   }

   public int getLevelSkillTrained() {
      return this.LvlSkillTrained;
   }

   public int getNumLevelsTrained() {
      return this.NumLevelsTrained;
   }

   public int getMaxLevelTrained() {
      return this.LvlSkillTrained == -1 ? -1 : this.LvlSkillTrained + this.NumLevelsTrained;
   }

   public List<String> getTeachedRecipes() {
      return this.teachedRecipes;
   }

   public float getTemperature() {
      return this.Temperature;
   }

   public void setTemperature(float var1) {
      this.Temperature = var1;
   }

   public boolean isConditionAffectsCapacity() {
      return this.ConditionAffectsCapacity;
   }

   public int getChanceToFall() {
      return this.chanceToFall;
   }

   public float getInsulation() {
      return this.insulation;
   }

   public void setInsulation(float var1) {
      this.insulation = var1;
   }

   public float getWindresist() {
      return this.windresist;
   }

   public void setWindresist(float var1) {
      this.windresist = var1;
   }

   public float getWaterresist() {
      return this.waterresist;
   }

   public void setWaterresist(float var1) {
      this.waterresist = var1;
   }

   public boolean getObsolete() {
      return this.OBSOLETE;
   }

   public String getAcceptItemFunction() {
      return this.AcceptItemFunction;
   }

   public ArrayList<BloodClothingType> getBloodClothingType() {
      return this.bloodClothingType;
   }

   public String toString() {
      String var10000 = this.getClass().getSimpleName();
      return var10000 + "{Module: " + (this.getModule() != null ? this.getModule().name : "null") + ", Name:" + this.name + ", Type:" + this.type + "}";
   }

   public String getReplaceWhenUnequip() {
      return this.replaceWhenUnequip;
   }

   public void resolveItemTypes() {
      this.AmmoType = ScriptManager.instance.resolveItemType(this.getModule(), this.AmmoType);
      this.magazineType = ScriptManager.instance.resolveItemType(this.getModule(), this.magazineType);
      if (this.AmmoType != null && !this.AmmoType.isEmpty()) {
         IsoBulletTracerEffects.getInstance().load(this.AmmoType);
      }

      String var2;
      if (this.RequireInHandOrInventory != null) {
         for(int var1 = 0; var1 < this.RequireInHandOrInventory.size(); ++var1) {
            var2 = (String)this.RequireInHandOrInventory.get(var1);
            var2 = ScriptManager.instance.resolveItemType(this.getModule(), var2);
            this.RequireInHandOrInventory.set(var1, var2);
         }
      }

      if (this.ReplaceTypesMap != null) {
         Iterator var4 = this.ReplaceTypesMap.keySet().iterator();

         while(var4.hasNext()) {
            var2 = (String)var4.next();
            String var3 = (String)this.ReplaceTypesMap.get(var2);
            this.ReplaceTypesMap.replace(var2, ScriptManager.instance.resolveItemType(this.getModule(), var3));
         }
      }

   }

   public void resolveModelScripts() {
      this.staticModel = ScriptManager.instance.resolveModelScript(this.getModule(), this.staticModel);
      this.worldStaticModel = ScriptManager.instance.resolveModelScript(this.getModule(), this.worldStaticModel);
   }

   public String getRecordedMediaCat() {
      return this.recordedMediaCat;
   }

   public Boolean isWorldRender() {
      return this.worldRender;
   }

   public Boolean isCantEat() {
      return this.CantEat;
   }

   public String getLuaCreate() {
      return this.LuaCreate;
   }

   public void setLuaCreate(String var1) {
      this.LuaCreate = var1;
   }

   public String getSoundParameter(String var1) {
      return this.soundParameterMap == null ? null : (String)this.soundParameterMap.get(var1);
   }

   public ArrayList<VehiclePartModel> getVehiclePartModels() {
      return this.vehiclePartModels;
   }

   public String getItemConfigKey() {
      return this.itemConfigKey;
   }

   public float getR() {
      return (float)this.colorRed / 255.0F;
   }

   public float getColorRed() {
      return this.getR();
   }

   public float getG() {
      return (float)this.colorGreen / 255.0F;
   }

   public float getColorGreen() {
      return this.getG();
   }

   public float getB() {
      return (float)this.colorBlue / 255.0F;
   }

   public float getColorBlue() {
      return this.getB();
   }

   public void setItemConfig(ItemConfig var1) {
      this.itemConfig = var1;
   }

   public ItemConfig getItemConfig() {
      return this.itemConfig;
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

   public float getCorpseSicknessDefense() {
      return this.corpseSicknessDefense;
   }

   public String getWithDrainable() {
      return this.withDrainable;
   }

   public String getWithoutDrainable() {
      return this.withoutDrainable;
   }

   public String getSpawnWith() {
      return this.spawnWith;
   }

   public ArrayList<String> getStaticModelsByIndex() {
      return this.staticModelsByIndex;
   }

   public ArrayList<String> getWorldStaticModelsByIndex() {
      return this.worldStaticModelsByIndex;
   }

   public ArrayList<String> getWeaponSpritesByIndex() {
      return this.weaponSpritesByIndex;
   }

   public float getVisionModifier() {
      return this.visionModifier;
   }

   public float getHearingModifier() {
      return this.hearingModifier;
   }

   public String getWorldObjectSprite() {
      return this.worldObjectSprite;
   }

   public float getStrainModifier() {
      return this.strainModifier;
   }

   public float getMaxItemSize() {
      return this.maxItemSize;
   }

   public String getOnBreak() {
      return this.onBreak;
   }

   public float getHeadConditionLowerChanceMultiplier() {
      return this.HeadConditionLowerChanceMultiplier;
   }

   public String getDamagedSound() {
      return this.damagedSound;
   }

   public String getShoutType() {
      return this.shoutType;
   }

   public float getShoutMultiplier() {
      return this.shoutMultiplier;
   }

   public int getEatTime() {
      return this.eatTime;
   }

   public boolean isVisualAid() {
      return this.visualAid;
   }

   public float getDiscomfortModifier() {
      return this.discomfortModifier;
   }

   public float getPoisonPower() {
      return (float)this.PoisonPower;
   }

   public Integer getPoisonDetectionLevel() {
      return this.PoisonDetectionLevel;
   }

   public float getFireFuelRatio() {
      return this.fireFuelRatio;
   }

   public boolean isMementoLoot() {
      return this.hasTag("IsMemento") || Objects.equals(this.getDisplayCategory(), "Memento");
   }

   public boolean isCookwareLoot() {
      return Objects.equals(this.getDisplayCategory(), "CookingWeapon") || Objects.equals(this.getDisplayCategory(), "Cooking");
   }

   public boolean isMaterialLoot() {
      return Objects.equals(this.getDisplayCategory(), "MaterialWeapon") || Objects.equals(this.getDisplayCategory(), "Material");
   }

   public boolean isFarmingLoot() {
      return Objects.equals(this.getDisplayCategory(), "GardeningWeapon") || Objects.equals(this.getDisplayCategory(), "Gardening") || this.hasTag("FarmingLoot");
   }

   public boolean isToolLoot() {
      return Objects.equals(this.getDisplayCategory(), "ToolWeapon") || Objects.equals(this.getDisplayCategory(), "Tool");
   }

   public boolean isSurvivalGearLoot() {
      return this.SurvivalGear || Objects.equals(this.getDisplayCategory(), "FishingWeapon") || Objects.equals(this.getDisplayCategory(), "Fishing") || Objects.equals(this.getDisplayCategory(), "Trapping") || Objects.equals(this.getDisplayCategory(), "Camping") || Objects.equals(this.getDisplayCategory(), "FireSource");
   }

   public boolean isMedicalLoot() {
      return this.Medical || Objects.equals(this.getDisplayCategory(), "FirstAid") || Objects.equals(this.getDisplayCategory(), "FirstAidWeapon");
   }

   public boolean isMechanicsLoot() {
      return this.MechanicsItem || Objects.equals(this.getDisplayCategory(), "VehicleMaintenance") || Objects.equals(this.getDisplayCategory(), "VehicleMaintenanceWeapon");
   }

   public String getLootType() {
      return ItemPickerJava.getLootType(this);
   }

   public boolean ignoreZombieDensity() {
      return this.getType() == Item.Type.Food || this.hasTag("IgnoreZombieDensity") || this.isMementoLoot();
   }

   public static enum Type {
      Normal,
      Weapon,
      Food,
      Literature,
      Drainable,
      Clothing,
      Container,
      WeaponPart,
      Key,
      KeyRing,
      Moveable,
      Radio,
      AlarmClock,
      AlarmClockClothing,
      Map,
      Animal;

      private Type() {
      }
   }
}
