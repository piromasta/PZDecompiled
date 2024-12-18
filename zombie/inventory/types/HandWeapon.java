package zombie.inventory.types;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import zombie.GameWindow;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.SurvivorDesc;
import zombie.characters.Moodles.MoodleType;
import zombie.characters.skills.PerkFactory;
import zombie.core.BoxedStaticValues;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.core.textures.ColorInfo;
import zombie.debug.DebugLog;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemType;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ModelWeaponPart;
import zombie.ui.ObjectTooltip;
import zombie.util.StringUtils;
import zombie.util.io.BitHeader;
import zombie.util.io.BitHeaderRead;
import zombie.util.io.BitHeaderWrite;

public final class HandWeapon extends InventoryItem {
   public float WeaponLength;
   public float SplatSize = 1.0F;
   private int ammoPerShoot = 1;
   private String magazineType = null;
   protected boolean angleFalloff = false;
   protected boolean bCanBarracade = false;
   protected float doSwingBeforeImpact = 0.0F;
   protected String impactSound = "BaseballBatHit";
   protected boolean knockBackOnNoDeath = true;
   protected float maxAngle = 1.0F;
   protected float maxDamage = 1.5F;
   protected int maxHitCount = 1000;
   protected float maxRange = 1.0F;
   protected boolean ranged = false;
   protected float minAngle = 0.5F;
   protected float minDamage = 0.4F;
   protected float minimumSwingTime = 0.5F;
   protected float minRange = 0.0F;
   protected float noiseFactor = 0.0F;
   protected String otherHandRequire = null;
   protected boolean otherHandUse = false;
   protected String physicsObject = null;
   protected float pushBackMod = 1.0F;
   protected boolean rangeFalloff = false;
   protected boolean shareDamage = true;
   protected int soundRadius = 0;
   protected int soundVolume = 0;
   protected boolean splatBloodOnNoDeath = false;
   protected int splatNumber = 2;
   protected String swingSound = "BaseballBatSwing";
   protected float swingTime = 1.0F;
   protected float toHitModifier = 1.0F;
   protected boolean useEndurance = true;
   protected boolean useSelf = false;
   protected String weaponSprite = null;
   private String originalWeaponSprite = null;
   protected float otherBoost = 1.0F;
   protected int DoorDamage = 1;
   protected String doorHitSound = "BaseballBatHit";
   protected int ConditionLowerChance = 10;
   protected boolean MultipleHitConditionAffected = true;
   protected boolean shareEndurance = true;
   protected boolean AlwaysKnockdown = false;
   protected float EnduranceMod = 1.0F;
   protected float KnockdownMod = 1.0F;
   protected boolean CantAttackWithLowestEndurance = false;
   public boolean bIsAimedFirearm = false;
   public boolean bIsAimedHandWeapon = false;
   public String RunAnim = "Run";
   public String IdleAnim = "Idle";
   public float HitAngleMod = 0.0F;
   private String SubCategory = "";
   private ArrayList<String> Categories = null;
   private int AimingPerkCritModifier = 0;
   private float AimingPerkRangeModifier = 0.0F;
   private float AimingPerkHitChanceModifier = 0.0F;
   private int HitChance = 0;
   private float AimingPerkMinAngleModifier = 0.0F;
   private int RecoilDelay = 0;
   private boolean PiercingBullets = false;
   private float soundGain = 1.0F;
   private final HashMap<String, WeaponPart> attachments = new HashMap();
   public WeaponPart activeSight = null;
   private WeaponPart activeLight = null;
   private int ClipSize = 0;
   private int reloadTime = 0;
   private int aimingTime = 0;
   private float minRangeRanged = 0.0F;
   private float minSightRange = 2.0F;
   private float maxSightRange = 6.0F;
   private int treeDamage = 0;
   private String bulletOutSound = null;
   private String shellFallSound = null;
   private int triggerExplosionTimer = 0;
   private boolean canBePlaced = false;
   private int explosionRange = 0;
   private int explosionPower = 0;
   private int fireRange = 0;
   private int firePower = 0;
   private int smokeRange = 0;
   private int noiseRange = 0;
   private float extraDamage = 0.0F;
   private int explosionTimer = 0;
   private int explosionDuration = 0;
   private String placedSprite = null;
   private boolean canBeReused = false;
   private int sensorRange = 0;
   private float critDmgMultiplier = 2.0F;
   private float baseSpeed = 1.0F;
   private float bloodLevel = 0.0F;
   private String ammoBox = null;
   private String insertAmmoStartSound = null;
   private String insertAmmoSound = null;
   private String insertAmmoStopSound = null;
   private String ejectAmmoStartSound = null;
   private String ejectAmmoSound = null;
   private String ejectAmmoStopSound = null;
   private String rackSound = null;
   private String clickSound = "Stormy9mmClick";
   private boolean containsClip = false;
   private String weaponReloadType = "handgun";
   private boolean rackAfterShoot = false;
   private boolean roundChambered = false;
   private boolean bSpentRoundChambered = false;
   private int spentRoundCount = 0;
   private float jamGunChance = 5.0F;
   private int ProjectileCount = 1;
   private float projectileSpread = 0.0F;
   private float projectileWeightCenter = 1.0F;
   private float aimingMod = 1.0F;
   private float CriticalChance = 20.0F;
   private String hitSound = "BaseballBatHit";
   private boolean isJammed = false;
   private ArrayList<ModelWeaponPart> modelWeaponPart = null;
   private boolean haveChamber = true;
   private String bulletName = null;
   private String damageCategory = null;
   private boolean damageMakeHole = false;
   private String hitFloorSound = "BatOnFloor";
   private boolean insertAllBulletsReload = false;
   private String fireMode = null;
   private ArrayList<String> fireModePossibilities = null;
   private ArrayList<String> weaponSpritesByIndex = null;
   private static final Comparator<InventoryItem> magazineComparator = Comparator.comparingInt(InventoryItem::getCurrentAmmoCount);

   public float getSplatSize() {
      return this.SplatSize;
   }

   public boolean CanStack(InventoryItem var1) {
      return false;
   }

   public String getCategory() {
      return this.mainCategory != null ? this.mainCategory : "Weapon";
   }

   public HandWeapon(String var1, String var2, String var3, String var4) {
      super(var1, var2, var3, var4);
      this.cat = ItemType.Weapon;
   }

   public HandWeapon(String var1, String var2, String var3, Item var4) {
      super(var1, var2, var3, var4);
      this.cat = ItemType.Weapon;
   }

   public boolean IsWeapon() {
      return true;
   }

   public int getSaveType() {
      return Item.Type.Weapon.ordinal();
   }

   public float getScore(SurvivorDesc var1) {
      float var2 = 0.0F;
      if (this.getAmmoType() != null && !this.getAmmoType().equals("none") && !this.container.contains(this.getAmmoType())) {
         var2 -= 100000.0F;
      }

      if (this.Condition == 0) {
         var2 -= 100000.0F;
      }

      var2 += this.maxDamage * 10.0F;
      var2 += this.maxAngle * 5.0F;
      var2 -= this.minimumSwingTime * 0.1F;
      var2 -= this.swingTime;
      if (var1 != null && var1.getInstance().getThreatLevel() <= 2 && this.soundRadius > 5) {
         if (var2 > 0.0F && (float)this.soundRadius > var2) {
            var2 = 1.0F;
         }

         var2 -= (float)this.soundRadius;
      }

      return var2;
   }

   public float getActualWeight() {
      float var1 = this.getScriptItem().getActualWeight();

      WeaponPart var3;
      for(Iterator var2 = this.attachments.values().iterator(); var2.hasNext(); var1 += this.getWeaponPartWeightModifier(var3)) {
         var3 = (WeaponPart)var2.next();
      }

      return var1;
   }

   public float getWeight() {
      return this.getActualWeight();
   }

   public float getContentsWeight() {
      float var1 = 0.0F;
      Item var2;
      if (this.haveChamber() && this.isRoundChambered() && !StringUtils.isNullOrWhitespace(this.getAmmoType())) {
         var2 = ScriptManager.instance.FindItem(this.getAmmoType());
         if (var2 != null) {
            var1 += var2.getActualWeight();
         }
      }

      if (this.isContainsClip() && !StringUtils.isNullOrWhitespace(this.getMagazineType())) {
         var2 = ScriptManager.instance.FindItem(this.getMagazineType());
         if (var2 != null) {
            var1 += var2.getActualWeight();
         }
      }

      return var1 + super.getContentsWeight();
   }

   public void DoTooltip(ObjectTooltip var1, ObjectTooltip.Layout var2) {
      ColorInfo var4 = new ColorInfo();
      ObjectTooltip.LayoutItem var3;
      float var5;
      if (this.hasSharpness()) {
         var3 = var2.addItem();
         var3.setLabel(Translator.getText("Tooltip_weapon_Sharpness") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
         var5 = this.getSharpness();
         Core.getInstance().getBadHighlitedColor().interp(Core.getInstance().getGoodHighlitedColor(), var5, var4);
         var3.setProgress(var5, var4.getR(), var4.getG(), var4.getB(), 1.0F);
      }

      var3 = var2.addItem();
      String var10 = "Tooltip_weapon_Condition";
      if (this.hasHeadCondition()) {
         var10 = "Tooltip_weapon_HandleCondition";
      }

      var3.setLabel(Translator.getText(var10) + ":", 1.0F, 1.0F, 0.8F, 1.0F);
      float var6 = (float)this.getCondition() / (float)this.getConditionMax();
      Core.getInstance().getBadHighlitedColor().interp(Core.getInstance().getGoodHighlitedColor(), var6, var4);
      var3.setProgress(var6, var4.getR(), var4.getG(), var4.getB(), 1.0F);
      if (this.hasHeadCondition()) {
         var3 = var2.addItem();
         var3.setLabel(Translator.getText("Tooltip_weapon_HeadCondition") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
         var5 = (float)this.getHeadCondition() / (float)this.getConditionMax();
         Core.getInstance().getBadHighlitedColor().interp(Core.getInstance().getGoodHighlitedColor(), var5, var4);
         var3.setProgress(var5, var4.getR(), var4.getG(), var4.getB(), 1.0F);
      }

      float var7;
      if (this.getMaxDamage() > 0.0F) {
         var3 = var2.addItem();
         var3.setLabel(Translator.getText("Tooltip_weapon_Damage") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
         var5 = this.getMaxDamage() + this.getMinDamage();
         var6 = 5.0F;
         var7 = var5 / var6;
         Core.getInstance().getBadHighlitedColor().interp(Core.getInstance().getGoodHighlitedColor(), var7, var4);
         var3.setProgress(var7, var4.getR(), var4.getG(), var4.getB(), 1.0F);
      }

      if (this.bloodLevel != 0.0F) {
         ColorInfo var11 = new ColorInfo();
         var3 = var2.addItem();
         var3.setLabel(Translator.getText("Tooltip_clothing_bloody") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
         var6 = this.bloodLevel;
         Core.getInstance().getGoodHighlitedColor().interp(Core.getInstance().getBadHighlitedColor(), var6, var11);
         var3.setProgress(var6, var11.getR(), var11.getG(), var11.getB(), 1.0F);
      }

      if (this.hasTag("FishingRod")) {
         var3 = var2.addItem();
         var3.setLabel(Translator.getText("Tooltip_fishing_line_Condition") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
         Object var12 = this.getModData().rawget("fishing_LineCondition");
         var6 = 1.0F;
         if (var12 == null) {
            this.getModData().rawset("fishing_LineCondition", 1.0);
         } else {
            var6 = (float)(Double)var12;
         }

         Core.getInstance().getBadHighlitedColor().interp(Core.getInstance().getGoodHighlitedColor(), var6, var4);
         var3.setProgress(var6, var4.getR(), var4.getG(), var4.getB(), 1.0F);
         var3 = var2.addItem();
         var3.setLabel(Translator.getText("Tooltip_fishing_line") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
         Object var13 = this.getModData().rawget("fishing_LineType");
         if (var13 == null) {
            this.getModData().rawset("fishing_LineType", "FishingLine");
            var3.setValue(Translator.getText(ScriptManager.instance.FindItem("Base.FishingLine").getDisplayName()), 1.0F, 1.0F, 1.0F, 1.0F);
         } else if (ScriptManager.instance.FindItem("FishingLine") != null && ScriptManager.instance.FindItem((String)var13) != null) {
            var3.setValue(Translator.getText(ScriptManager.instance.FindItem((String)var13).getDisplayName()), 1.0F, 1.0F, 1.0F, 1.0F);
         } else {
            var3.setValue(Translator.getText("Tooltip_fishing_line"), 1.0F, 1.0F, 1.0F, 1.0F);
         }

         var3 = var2.addItem();
         var3.setLabel(Translator.getText("Tooltip_fishing_hook") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
         Object var8 = this.getModData().rawget("fishing_HookType");
         if (var8 == null) {
            this.getModData().rawset("fishing_HookType", "FishingHook");
            var3.setValue(Translator.getText(ScriptManager.instance.FindItem("FishingHook").getDisplayName()), 1.0F, 1.0F, 1.0F, 1.0F);
         } else if (ScriptManager.instance.FindItem((String)var8) != null && ScriptManager.instance.FindItem("FishingHook") != null) {
            var3.setValue(Translator.getText(ScriptManager.instance.FindItem((String)var8).getDisplayName()), 1.0F, 1.0F, 1.0F, 1.0F);
         } else {
            var3.setValue(Translator.getText("Tooltip_fishing_hook"), 1.0F, 1.0F, 1.0F, 1.0F);
         }

         var3 = var2.addItem();
         var3.setLabel(Translator.getText("Tooltip_fishing_bait") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
         Object var9 = this.getModData().rawget("fishing_Lure");
         if (var9 == null) {
            var3.setValue(Translator.getText("UI_None"), 1.0F, 0.0F, 0.0F, 1.0F);
         } else if (ScriptManager.instance.FindItem((String)var9) != null && Translator.getText("UI_None") != null) {
            var3.setValue(Translator.getText(ScriptManager.instance.FindItem((String)var9).getDisplayName()), 1.0F, 1.0F, 1.0F, 1.0F);
         } else {
            var3.setValue(Translator.getText("Tooltip_fishing_bait"), 1.0F, 1.0F, 1.0F, 1.0F);
         }
      }

      if (this.isRanged()) {
         var3 = var2.addItem();
         var3.setLabel(Translator.getText("Tooltip_weapon_Range") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
         var5 = this.getMaxRange(IsoPlayer.getInstance());
         var6 = 40.0F;
         var7 = var5 / var6;
         Core.getInstance().getBadHighlitedColor().interp(Core.getInstance().getGoodHighlitedColor(), var7, var4);
         var3.setProgress(var7, var4.getR(), var4.getG(), var4.getB(), 1.0F);
      }

      if (this.isTwoHandWeapon() && !this.isRequiresEquippedBothHands()) {
         var3 = var2.addItem();
         var3.setLabel(Translator.getText("Tooltip_item_TwoHandWeapon"), 1.0F, 1.0F, 0.8F, 1.0F);
      }

      if (!StringUtils.isNullOrEmpty(this.getFireMode())) {
         var3 = var2.addItem();
         var3.setLabel(Translator.getText("Tooltip_item_FireMode") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
         var3.setValue(Translator.getText("ContextMenu_FireMode_" + this.getFireMode()), 1.0F, 1.0F, 1.0F, 1.0F);
      }

      if (this.CantAttackWithLowestEndurance) {
         var3 = var2.addItem();
         var3.setLabel(Translator.getText("Tooltip_weapon_Unusable_at_max_exertion"), Core.getInstance().getBadHighlitedColor().getR(), Core.getInstance().getBadHighlitedColor().getG(), Core.getInstance().getBadHighlitedColor().getB(), 1.0F);
      }

      if (this.getMaxAmmo() > 0) {
         var10 = String.valueOf(this.getCurrentAmmoCount());
         if (this.isRoundChambered()) {
            var10 = var10 + "+1";
         }

         var3 = var2.addItem();
         if (this.bulletName == null) {
            if (this.getMagazineType() != null) {
               this.bulletName = InventoryItemFactory.CreateItem(this.getMagazineType()).getDisplayName();
            } else {
               this.bulletName = InventoryItemFactory.CreateItem(this.getAmmoType()).getDisplayName();
            }
         }

         var3.setLabel(this.bulletName + ":", 1.0F, 1.0F, 0.8F, 1.0F);
         var3.setValue(var10 + " / " + this.getMaxAmmo(), 1.0F, 1.0F, 1.0F, 1.0F);
      }

      if (this.isJammed()) {
         var3 = var2.addItem();
         var3.setLabel(Translator.getText("Tooltip_weapon_Jammed"), Core.getInstance().getBadHighlitedColor().getR(), Core.getInstance().getBadHighlitedColor().getG(), Core.getInstance().getBadHighlitedColor().getB(), 1.0F);
      } else if (this.haveChamber() && !this.isRoundChambered() && this.getCurrentAmmoCount() > 0) {
         var3 = var2.addItem();
         var10 = this.isSpentRoundChambered() ? "Tooltip_weapon_SpentRoundChambered" : "Tooltip_weapon_NoRoundChambered";
         var3.setLabel(Translator.getText(var10), Core.getInstance().getBadHighlitedColor().getR(), Core.getInstance().getBadHighlitedColor().getG(), Core.getInstance().getBadHighlitedColor().getB(), 1.0F);
      } else if (this.getSpentRoundCount() > 0) {
         var3 = var2.addItem();
         var3.setLabel(Translator.getText("Tooltip_weapon_SpentRounds") + ":", Core.getInstance().getBadHighlitedColor().getR(), Core.getInstance().getBadHighlitedColor().getG(), Core.getInstance().getBadHighlitedColor().getB(), 1.0F);
         var3.setValue(this.getSpentRoundCount() + " / " + this.getMaxAmmo(), 1.0F, 1.0F, 1.0F, 1.0F);
      }

      if (!StringUtils.isNullOrEmpty(this.getMagazineType())) {
         if (this.isContainsClip()) {
            var3 = var2.addItem();
            var3.setLabel(Translator.getText("Tooltip_weapon_ContainsClip"), 1.0F, 1.0F, 0.8F, 1.0F);
         } else {
            var3 = var2.addItem();
            var3.setLabel(Translator.getText("Tooltip_weapon_NoClip"), 1.0F, 1.0F, 0.8F, 1.0F);
         }
      }

      ObjectTooltip.Layout var14 = var1.beginLayout();
      this.attachments.forEach((var1x, var2x) -> {
         ObjectTooltip.LayoutItem var3 = var14.addItem();
         var3.setLabel(Translator.getText("Tooltip_weapon_" + var1x) + ":", 1.0F, 1.0F, 0.8F, 1.0F);
         var3.setValue(var2x.getName(), 1.0F, 1.0F, 1.0F, 1.0F);
      });
      if (!var14.items.isEmpty()) {
         var2.next = var14;
         var14.nextPadY = var1.getLineSpacing();
      } else {
         var1.endLayout(var14);
      }

   }

   public float getDamageMod(IsoGameCharacter var1) {
      int var2 = var1.getPerkLevel(PerkFactory.Perks.Blunt);
      if (this.ScriptItem.Categories.contains("Blunt")) {
         if (var2 >= 3 && var2 <= 6) {
            return 1.1F;
         }

         if (var2 >= 7) {
            return 1.2F;
         }
      }

      int var3 = var1.getPerkLevel(PerkFactory.Perks.Axe);
      if (this.ScriptItem.Categories.contains("Axe")) {
         if (var3 >= 3 && var3 <= 6) {
            return 1.1F;
         }

         if (var3 >= 7) {
            return 1.2F;
         }
      }

      int var4 = var1.getPerkLevel(PerkFactory.Perks.Spear);
      if (this.ScriptItem.Categories.contains("Spear")) {
         if (var4 >= 3 && var4 <= 6) {
            return 1.1F;
         }

         if (var4 >= 7) {
            return 1.2F;
         }
      }

      return 1.0F;
   }

   public float getRangeMod(IsoGameCharacter var1) {
      int var2 = var1.getPerkLevel(PerkFactory.Perks.Blunt);
      if (this.ScriptItem.Categories.contains("Blunt") && var2 >= 7) {
         return 1.2F;
      } else {
         int var3 = var1.getPerkLevel(PerkFactory.Perks.Axe);
         if (this.ScriptItem.Categories.contains("Axe") && var3 >= 7) {
            return 1.2F;
         } else {
            int var4 = var1.getPerkLevel(PerkFactory.Perks.Spear);
            return this.ScriptItem.Categories.contains("Spear") && var4 >= 7 ? 1.2F : 1.0F;
         }
      }
   }

   public float getFatigueMod(IsoGameCharacter var1) {
      int var2 = var1.getPerkLevel(PerkFactory.Perks.Blunt);
      if (this.ScriptItem.Categories.contains("Blunt") && var2 >= 8) {
         return 0.8F;
      } else {
         int var3 = var1.getPerkLevel(PerkFactory.Perks.Axe);
         if (this.ScriptItem.Categories.contains("Axe") && var3 >= 8) {
            return 0.8F;
         } else {
            int var4 = var1.getPerkLevel(PerkFactory.Perks.Spear);
            return this.ScriptItem.Categories.contains("Spear") && var4 >= 8 ? 0.8F : 1.0F;
         }
      }
   }

   public float getKnockbackMod(IsoGameCharacter var1) {
      int var2 = var1.getPerkLevel(PerkFactory.Perks.Axe);
      return this.ScriptItem.Categories.contains("Axe") && var2 >= 6 ? 2.0F : 1.0F;
   }

   public float getSpeedMod(IsoGameCharacter var1) {
      int var2;
      if (this.ScriptItem.Categories.contains("Blunt")) {
         var2 = var1.getPerkLevel(PerkFactory.Perks.Blunt);
         if (var2 >= 10) {
            return 0.65F;
         }

         if (var2 >= 9) {
            return 0.68F;
         }

         if (var2 >= 8) {
            return 0.71F;
         }

         if (var2 >= 7) {
            return 0.74F;
         }

         if (var2 >= 6) {
            return 0.77F;
         }

         if (var2 >= 5) {
            return 0.8F;
         }

         if (var2 >= 4) {
            return 0.83F;
         }

         if (var2 >= 3) {
            return 0.86F;
         }

         if (var2 >= 2) {
            return 0.9F;
         }

         if (var2 >= 1) {
            return 0.95F;
         }
      }

      if (this.ScriptItem.Categories.contains("Axe")) {
         var2 = var1.getPerkLevel(PerkFactory.Perks.Axe);
         float var3 = 1.0F;
         if (var1.Traits.Axeman.isSet()) {
            var3 = 0.95F;
         }

         if (var2 >= 10) {
            return 0.65F * var3;
         } else if (var2 >= 9) {
            return 0.68F * var3;
         } else if (var2 >= 8) {
            return 0.71F * var3;
         } else if (var2 >= 7) {
            return 0.74F * var3;
         } else if (var2 >= 6) {
            return 0.77F * var3;
         } else if (var2 >= 5) {
            return 0.8F * var3;
         } else if (var2 >= 4) {
            return 0.83F * var3;
         } else if (var2 >= 3) {
            return 0.86F * var3;
         } else if (var2 >= 2) {
            return 0.9F * var3;
         } else {
            return var2 >= 1 ? 0.95F * var3 : 1.0F * var3;
         }
      } else {
         if (this.ScriptItem.Categories.contains("Spear")) {
            var2 = var1.getPerkLevel(PerkFactory.Perks.Spear);
            if (var2 >= 10) {
               return 0.65F;
            }

            if (var2 >= 9) {
               return 0.68F;
            }

            if (var2 >= 8) {
               return 0.71F;
            }

            if (var2 >= 7) {
               return 0.74F;
            }

            if (var2 >= 6) {
               return 0.77F;
            }

            if (var2 >= 5) {
               return 0.8F;
            }

            if (var2 >= 4) {
               return 0.83F;
            }

            if (var2 >= 3) {
               return 0.86F;
            }

            if (var2 >= 2) {
               return 0.9F;
            }

            if (var2 >= 1) {
               return 0.95F;
            }
         }

         return 1.0F;
      }
   }

   public float getToHitMod(IsoGameCharacter var1) {
      int var2 = var1.getPerkLevel(PerkFactory.Perks.Blunt);
      if (this.ScriptItem.Categories.contains("Blunt")) {
         if (var2 == 1) {
            return 1.2F;
         }

         if (var2 == 2) {
            return 1.3F;
         }

         if (var2 == 3) {
            return 1.4F;
         }

         if (var2 == 4) {
            return 1.5F;
         }

         if (var2 == 5) {
            return 1.6F;
         }

         if (var2 == 6) {
            return 1.7F;
         }

         if (var2 == 7) {
            return 1.8F;
         }

         if (var2 == 8) {
            return 1.9F;
         }

         if (var2 == 9) {
            return 2.0F;
         }

         if (var2 == 10) {
            return 100.0F;
         }
      }

      int var3 = var1.getPerkLevel(PerkFactory.Perks.Axe);
      if (this.ScriptItem.Categories.contains("Axe")) {
         if (var3 == 1) {
            return 1.2F;
         }

         if (var3 == 2) {
            return 1.3F;
         }

         if (var3 == 3) {
            return 1.4F;
         }

         if (var3 == 4) {
            return 1.5F;
         }

         if (var3 == 5) {
            return 1.6F;
         }

         if (var3 == 6) {
            return 1.7F;
         }

         if (var3 == 7) {
            return 1.8F;
         }

         if (var3 == 8) {
            return 1.9F;
         }

         if (var3 == 9) {
            return 2.0F;
         }

         if (var3 == 10) {
            return 100.0F;
         }
      }

      int var4 = var1.getPerkLevel(PerkFactory.Perks.Spear);
      if (this.ScriptItem.Categories.contains("Spear")) {
         if (var4 == 1) {
            return 1.2F;
         }

         if (var4 == 2) {
            return 1.3F;
         }

         if (var4 == 3) {
            return 1.4F;
         }

         if (var4 == 4) {
            return 1.5F;
         }

         if (var4 == 5) {
            return 1.6F;
         }

         if (var4 == 6) {
            return 1.7F;
         }

         if (var4 == 7) {
            return 1.8F;
         }

         if (var4 == 8) {
            return 1.9F;
         }

         if (var4 == 9) {
            return 2.0F;
         }

         if (var4 == 10) {
            return 100.0F;
         }
      }

      return 1.0F;
   }

   public PerkFactory.Perk getPerk() {
      if (this.getCategories().contains("Axe")) {
         return PerkFactory.Perks.Axe;
      } else if (this.getCategories().contains("LongBlade")) {
         return PerkFactory.Perks.LongBlade;
      } else if (this.getCategories().contains("Spear")) {
         return PerkFactory.Perks.Spear;
      } else if (this.getCategories().contains("SmallBlade")) {
         return PerkFactory.Perks.SmallBlade;
      } else if (this.getCategories().contains("SmallBlunt")) {
         return PerkFactory.Perks.SmallBlunt;
      } else {
         return WeaponType.getWeaponType(this).isRanged ? PerkFactory.Perks.Aiming : PerkFactory.Perks.Blunt;
      }
   }

   public float muscleStrainMod(IsoGameCharacter var1) {
      float var2 = 1.0F;
      int var3 = this.getWeaponSkill(var1);
      var2 -= (float)var3 * 0.075F;
      var2 *= this.getStrainModifier();
      return var2;
   }

   public int getWeaponSkill(IsoGameCharacter var1) {
      return var1.getPerkLevel(this.getPerk());
   }

   public boolean isAngleFalloff() {
      return this.angleFalloff;
   }

   public void setAngleFalloff(boolean var1) {
      this.angleFalloff = var1;
   }

   public boolean isCanBarracade() {
      return this.bCanBarracade;
   }

   public void setCanBarracade(boolean var1) {
      this.bCanBarracade = var1;
   }

   public float getDoSwingBeforeImpact() {
      return this.doSwingBeforeImpact;
   }

   public void setDoSwingBeforeImpact(float var1) {
      this.doSwingBeforeImpact = var1;
   }

   public String getImpactSound() {
      return this.impactSound;
   }

   public void setImpactSound(String var1) {
      this.impactSound = var1;
   }

   public boolean isKnockBackOnNoDeath() {
      return this.knockBackOnNoDeath;
   }

   public void setKnockBackOnNoDeath(boolean var1) {
      this.knockBackOnNoDeath = var1;
   }

   public float getMaxAngle() {
      return this.maxAngle;
   }

   public void setMaxAngle(float var1) {
      this.maxAngle = var1;
   }

   public float getMaxDamage() {
      float var1 = this.maxDamage;
      if (this.hasSharpness() && this.maxDamage > this.getMinDamage()) {
         float var2 = this.maxDamage - this.getMinDamage();
         var2 *= this.getSharpnessMultiplier();
         var1 = var2 + this.getMinDamage();
      }

      return var1;
   }

   public void setMaxDamage(float var1) {
      this.maxDamage = var1;
   }

   public int getMaxHitCount() {
      return this.maxHitCount;
   }

   public void setMaxHitCount(int var1) {
      this.maxHitCount = var1;
   }

   public float getMaxRange() {
      return this.maxRange;
   }

   public float getMaxRange(IsoGameCharacter var1) {
      return this.isRanged() ? this.maxRange + this.getAimingPerkRangeModifier() * ((float)var1.getPerkLevel(PerkFactory.Perks.Aiming) / 2.0F) : this.maxRange;
   }

   public void setMaxRange(float var1) {
      this.maxRange = var1;
   }

   public boolean isRanged() {
      return this.ranged;
   }

   public void setRanged(boolean var1) {
      this.ranged = var1;
   }

   public float getMinAngle() {
      return this.minAngle;
   }

   public void setMinAngle(float var1) {
      this.minAngle = var1;
   }

   public float getMinDamage() {
      return this.minDamage;
   }

   public void setMinDamage(float var1) {
      this.minDamage = var1;
   }

   public float getMinimumSwingTime() {
      return this.minimumSwingTime;
   }

   public void setMinimumSwingTime(float var1) {
      this.minimumSwingTime = var1;
   }

   public float getMinRange() {
      return this.minRange;
   }

   public void setMinRange(float var1) {
      this.minRange = var1;
   }

   public float getNoiseFactor() {
      return this.noiseFactor;
   }

   public void setNoiseFactor(float var1) {
      this.noiseFactor = var1;
   }

   public String getOtherHandRequire() {
      return this.otherHandRequire;
   }

   public void setOtherHandRequire(String var1) {
      this.otherHandRequire = var1;
   }

   public boolean isOtherHandUse() {
      return this.otherHandUse;
   }

   public void setOtherHandUse(boolean var1) {
      this.otherHandUse = var1;
   }

   public String getPhysicsObject() {
      return this.physicsObject;
   }

   public void setPhysicsObject(String var1) {
      this.physicsObject = var1;
   }

   public float getPushBackMod() {
      return this.pushBackMod;
   }

   public void setPushBackMod(float var1) {
      this.pushBackMod = var1;
   }

   public boolean isRangeFalloff() {
      return this.rangeFalloff;
   }

   public void setRangeFalloff(boolean var1) {
      this.rangeFalloff = var1;
   }

   public boolean isShareDamage() {
      return this.shareDamage;
   }

   public void setShareDamage(boolean var1) {
      this.shareDamage = var1;
   }

   public int getSoundRadius() {
      return this.soundRadius;
   }

   public void setSoundRadius(int var1) {
      this.soundRadius = var1;
   }

   public int getSoundVolume() {
      return this.soundVolume;
   }

   public void setSoundVolume(int var1) {
      this.soundVolume = var1;
   }

   public boolean isSplatBloodOnNoDeath() {
      return this.splatBloodOnNoDeath;
   }

   public void setSplatBloodOnNoDeath(boolean var1) {
      this.splatBloodOnNoDeath = var1;
   }

   public int getSplatNumber() {
      return this.splatNumber;
   }

   public void setSplatNumber(int var1) {
      this.splatNumber = var1;
   }

   public String getSwingSound() {
      return this.swingSound;
   }

   public void setSwingSound(String var1) {
      this.swingSound = var1;
   }

   public float getSwingTime() {
      return this.swingTime;
   }

   public void setSwingTime(float var1) {
      this.swingTime = var1;
   }

   public float getToHitModifier() {
      return this.toHitModifier;
   }

   public void setToHitModifier(float var1) {
      this.toHitModifier = var1;
   }

   public boolean isUseEndurance() {
      return this.useEndurance;
   }

   public void setUseEndurance(boolean var1) {
      this.useEndurance = var1;
   }

   public boolean isUseSelf() {
      return this.useSelf;
   }

   public void setUseSelf(boolean var1) {
      this.useSelf = var1;
   }

   public String getWeaponSprite() {
      return this.getModelIndex() != -1 && this.getWeaponSpritesByIndex() != null ? (String)this.getWeaponSpritesByIndex().get(this.getModelIndex()) : this.weaponSprite;
   }

   public void setWeaponSprite(String var1) {
      this.weaponSprite = var1;
   }

   public float getOtherBoost() {
      return this.otherBoost;
   }

   public void setOtherBoost(float var1) {
      this.otherBoost = var1;
   }

   public int getDoorDamage() {
      return (int)((float)this.DoorDamage * this.getSharpnessMultiplier());
   }

   public void setDoorDamage(int var1) {
      this.DoorDamage = var1;
   }

   public String getDoorHitSound() {
      return this.doorHitSound;
   }

   public void setDoorHitSound(String var1) {
      this.doorHitSound = var1;
   }

   public int getConditionLowerChance() {
      return this.ConditionLowerChance;
   }

   public void setConditionLowerChance(int var1) {
      this.ConditionLowerChance = var1;
   }

   public boolean isMultipleHitConditionAffected() {
      return this.MultipleHitConditionAffected;
   }

   public void setMultipleHitConditionAffected(boolean var1) {
      this.MultipleHitConditionAffected = var1;
   }

   public boolean isShareEndurance() {
      return this.shareEndurance;
   }

   public void setShareEndurance(boolean var1) {
      this.shareEndurance = var1;
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

   public boolean isAimedFirearm() {
      return this.bIsAimedFirearm;
   }

   public boolean isAimedHandWeapon() {
      return this.bIsAimedHandWeapon;
   }

   public int getProjectileCount() {
      return this.ProjectileCount;
   }

   public void setProjectileCount(int var1) {
      this.ProjectileCount = var1;
   }

   public float getProjectileSpread() {
      return this.projectileSpread;
   }

   public void setProjectileSpread(float var1) {
      this.projectileSpread = var1;
   }

   public float getProjectileWeightCenter() {
      return this.projectileWeightCenter;
   }

   public void setProjectileWeightCenter(float var1) {
      this.projectileWeightCenter = var1;
   }

   public float getAimingMod() {
      return this.aimingMod;
   }

   public boolean isAimed() {
      return this.bIsAimedFirearm || this.bIsAimedHandWeapon;
   }

   public void setCriticalChance(float var1) {
      this.CriticalChance = var1;
   }

   public float getCriticalChance() {
      return this.hasSharpness() ? this.CriticalChance * this.getSharpness() : this.CriticalChance;
   }

   public void setSubCategory(String var1) {
      this.SubCategory = var1;
   }

   public String getSubCategory() {
      return this.SubCategory;
   }

   public void setZombieHitSound(String var1) {
      this.hitSound = var1;
   }

   public String getZombieHitSound() {
      return this.hitSound;
   }

   public ArrayList<String> getCategories() {
      return this.Categories;
   }

   public void setCategories(ArrayList<String> var1) {
      this.Categories = var1;
   }

   public int getAimingPerkCritModifier() {
      return this.AimingPerkCritModifier;
   }

   public void setAimingPerkCritModifier(int var1) {
      this.AimingPerkCritModifier = var1;
   }

   public float getAimingPerkRangeModifier() {
      return this.AimingPerkRangeModifier;
   }

   public void setAimingPerkRangeModifier(float var1) {
      this.AimingPerkRangeModifier = var1;
   }

   public int getHitChance() {
      return this.HitChance;
   }

   public void setHitChance(int var1) {
      this.HitChance = var1;
   }

   public float getAimingPerkHitChanceModifier() {
      return this.AimingPerkHitChanceModifier;
   }

   public void setAimingPerkHitChanceModifier(float var1) {
      this.AimingPerkHitChanceModifier = var1;
   }

   public float getAimingPerkMinAngleModifier() {
      return this.AimingPerkMinAngleModifier;
   }

   public void setAimingPerkMinAngleModifier(float var1) {
      this.AimingPerkMinAngleModifier = var1;
   }

   public int getRecoilDelay() {
      return this.RecoilDelay;
   }

   public int getRecoilDelay(IsoGameCharacter var1) {
      return PZMath.max(0, (int)((float)this.RecoilDelay * (1.0F - (float)var1.getPerkLevel(PerkFactory.Perks.Aiming) / 40.0F) * (1.0F - (-10.0F + (float)var1.getPerkLevel(PerkFactory.Perks.Strength) * 2.0F) / 40.0F) * (var1.getPrimaryHandItem() == this && var1.getSecondaryHandItem() != this && var1.getSecondaryHandItem() != null ? 1.3F : 1.0F)));
   }

   public void setRecoilDelay(int var1) {
      this.RecoilDelay = var1;
   }

   public boolean isPiercingBullets() {
      return this.PiercingBullets;
   }

   public void setPiercingBullets(boolean var1) {
      this.PiercingBullets = var1;
   }

   public float getSoundGain() {
      return this.soundGain;
   }

   public void setSoundGain(float var1) {
      this.soundGain = var1;
   }

   public int getClipSize() {
      return this.ClipSize;
   }

   public void setClipSize(int var1) {
      this.ClipSize = var1;
      this.getModData().rawset("maxCapacity", BoxedStaticValues.toDouble((double)var1));
   }

   public void save(ByteBuffer var1, boolean var2) throws IOException {
      super.save(var1, var2);
      BitHeaderWrite var3 = BitHeader.allocWrite(BitHeader.HeaderSize.Integer, var1);
      if (this.maxRange != 1.0F) {
         var3.addFlags(1);
         var1.putFloat(this.maxRange);
      }

      if (this.minRangeRanged != 0.0F) {
         var3.addFlags(2);
         var1.putFloat(this.minRangeRanged);
      }

      if (this.ClipSize != 0) {
         var3.addFlags(4);
         var1.putInt(this.ClipSize);
      }

      if (this.minDamage != 0.4F) {
         var3.addFlags(8);
         var1.putFloat(this.minDamage);
      }

      if (this.maxDamage != 1.5F) {
         var3.addFlags(16);
         var1.putFloat(this.maxDamage);
      }

      if (this.RecoilDelay != 0) {
         var3.addFlags(32);
         var1.putInt(this.RecoilDelay);
      }

      if (this.aimingTime != 0) {
         var3.addFlags(64);
         var1.putInt(this.aimingTime);
      }

      if (this.reloadTime != 0) {
         var3.addFlags(128);
         var1.putInt(this.reloadTime);
      }

      if (this.HitChance != 0) {
         var3.addFlags(256);
         var1.putInt(this.HitChance);
      }

      if (this.minAngle != 0.5F) {
         var3.addFlags(512);
         var1.putFloat(this.minAngle);
      }

      if (this.getExplosionTimer() != 0) {
         var3.addFlags(65536);
         var1.putInt(this.getExplosionTimer());
      }

      if (this.maxAngle != 1.0F) {
         var3.addFlags(131072);
         var1.putFloat(this.maxAngle);
      }

      if (this.bloodLevel != 0.0F) {
         var3.addFlags(262144);
         var1.putFloat(this.bloodLevel);
      }

      if (this.containsClip) {
         var3.addFlags(524288);
      }

      if (this.roundChambered) {
         var3.addFlags(1048576);
      }

      if (this.isJammed) {
         var3.addFlags(2097152);
      }

      if (!StringUtils.equals(this.weaponSprite, this.getScriptItem().getWeaponSprite())) {
         var3.addFlags(4194304);
         GameWindow.WriteString(var1, this.weaponSprite);
      }

      if (this.minSightRange != 2.0F) {
         var3.addFlags(8388608);
         var1.putFloat(this.minSightRange);
      }

      if (this.maxSightRange != 6.0F) {
         var3.addFlags(16777216);
         var1.putFloat(this.maxSightRange);
      }

      if (!this.attachments.isEmpty()) {
         var3.addFlags(33554432);
         ArrayList var4 = this.getAllWeaponParts();
         var1.put((byte)var4.size());

         for(int var5 = 0; var5 < var4.size(); ++var5) {
            var1.putShort(((WeaponPart)var4.get(var5)).getRegistry_id());
         }
      }

      var3.write();
      var3.release();
   }

   public void load(ByteBuffer var1, int var2) throws IOException {
      super.load(var1, var2);
      this.attachments.clear();
      this.maxRange = 1.0F;
      this.minRangeRanged = 0.0F;
      this.ClipSize = 0;
      this.minDamage = 0.4F;
      this.maxDamage = 1.5F;
      this.RecoilDelay = 0;
      this.aimingTime = 0;
      this.reloadTime = 0;
      this.HitChance = 0;
      this.minAngle = 0.5F;
      this.explosionTimer = 0;
      this.maxAngle = 1.0F;
      this.bloodLevel = 0.0F;
      this.containsClip = false;
      this.roundChambered = false;
      this.isJammed = false;
      this.weaponSprite = this.getScriptItem().getWeaponSprite();
      BitHeaderRead var3 = BitHeader.allocRead(BitHeader.HeaderSize.Integer, var1);
      if (!var3.equals(0)) {
         if (var3.hasFlags(1)) {
            this.setMaxRange(var1.getFloat());
         }

         if (var3.hasFlags(2)) {
            this.setMinRangeRanged(var1.getFloat());
         }

         if (var3.hasFlags(4)) {
            this.setClipSize(var1.getInt());
         }

         if (var3.hasFlags(8)) {
            this.setMinDamage(var1.getFloat());
         }

         if (var3.hasFlags(16)) {
            this.setMaxDamage(var1.getFloat());
         }

         if (var3.hasFlags(32)) {
            this.setRecoilDelay(var1.getInt());
         }

         if (var3.hasFlags(64)) {
            this.setAimingTime(var1.getInt());
         }

         if (var3.hasFlags(128)) {
            this.setReloadTime(var1.getInt());
         }

         if (var3.hasFlags(256)) {
            this.setHitChance(var1.getInt());
         }

         if (var3.hasFlags(512)) {
            this.setMinAngle(var1.getFloat());
         }

         if (var3.hasFlags(65536)) {
            this.setExplosionTimer(var1.getInt());
         }

         if (var3.hasFlags(131072)) {
            this.setMaxAngle(var1.getFloat());
         }

         if (var3.hasFlags(262144)) {
            this.setBloodLevel(var1.getFloat());
         }

         this.setContainsClip(var3.hasFlags(524288));
         if (StringUtils.isNullOrWhitespace(this.magazineType)) {
            this.setContainsClip(false);
         }

         this.setRoundChambered(var3.hasFlags(1048576));
         this.setJammed(var3.hasFlags(2097152));
         if (var3.hasFlags(4194304)) {
            this.setWeaponSprite(GameWindow.ReadStringUTF(var1));
         }

         if (var3.hasFlags(8388608)) {
            this.setMinSightRange(var1.getFloat());
         }

         if (var3.hasFlags(16777216)) {
            this.setMaxSightRange(var1.getFloat());
         }

         if (var3.hasFlags(33554432)) {
            byte var4 = var1.get();

            for(byte var5 = 0; var5 < var4; ++var5) {
               InventoryItem var6 = InventoryItemFactory.CreateItem(var1.getShort());
               if (var6 instanceof WeaponPart) {
                  this.attachWeaponPart((IsoGameCharacter)null, (WeaponPart)var6, false);
               }
            }
         }
      }

      var3.release();
   }

   public WeaponPart getActiveLight() {
      return this.activeLight;
   }

   public void setActiveLight(WeaponPart var1) {
      this.activeLight = var1;
   }

   public WeaponPart getActiveSight() {
      return this.activeSight;
   }

   public void setActiveSight(WeaponPart var1) {
      this.activeSight = var1;
   }

   public void setMinSightRange(float var1) {
      this.minSightRange = var1;
   }

   public float getMinSightRange() {
      return this.minSightRange;
   }

   public float getMinSightRange(IsoGameCharacter var1) {
      return (this.activeSight != null ? this.activeSight.getMinSightRange() : this.minSightRange) * (1.0F - (float)var1.getPerkLevel(PerkFactory.Perks.Aiming) / 30.0F);
   }

   public void setMaxSightRange(float var1) {
      this.maxSightRange = var1;
   }

   public float getMaxSightRange() {
      return this.maxSightRange;
   }

   public float getMaxSightRange(IsoGameCharacter var1) {
      return var1.Traits.ShortSighted.isSet() && !var1.isWearingGlasses() ? this.getMinSightRange(var1) : (this.activeSight != null ? this.activeSight.getMaxSightRange() : this.maxSightRange) * (1.0F + (float)var1.getPerkLevel(PerkFactory.Perks.Aiming) / 30.0F) * (var1.Traits.EagleEyed.isSet() ? 1.2F : 1.0F);
   }

   public float getLowLightBonus() {
      return this.activeSight != null ? this.activeSight.getLowLightBonus() : 0.0F;
   }

   public float getMinRangeRanged() {
      return this.minRangeRanged;
   }

   public void setMinRangeRanged(float var1) {
      this.minRangeRanged = var1;
   }

   public int getReloadTime() {
      return this.reloadTime;
   }

   public void setReloadTime(int var1) {
      this.reloadTime = var1;
   }

   public int getAimingTime() {
      return this.aimingTime;
   }

   public void setAimingTime(int var1) {
      this.aimingTime = var1;
   }

   public int getTreeDamage() {
      return (int)((float)this.treeDamage * this.getSharpnessMultiplier());
   }

   public void setTreeDamage(int var1) {
      this.treeDamage = var1;
   }

   public String getBulletOutSound() {
      return this.bulletOutSound;
   }

   public void setBulletOutSound(String var1) {
      this.bulletOutSound = var1;
   }

   public String getShellFallSound() {
      return this.shellFallSound;
   }

   public void setShellFallSound(String var1) {
      this.shellFallSound = var1;
   }

   private void addPartToList(String var1, ArrayList<WeaponPart> var2) {
      WeaponPart var3 = this.getWeaponPart(var1);
      if (var3 != null) {
         var2.add(var3);
      }

   }

   public ArrayList<WeaponPart> getAllWeaponParts() {
      return this.getAllWeaponParts(new ArrayList());
   }

   public ArrayList<WeaponPart> getAllWeaponParts(ArrayList<WeaponPart> var1) {
      var1.clear();
      var1.addAll(this.attachments.values());
      return var1;
   }

   public void clearAllWeaponParts() {
      this.activeLight = null;
      this.activeSight = null;
      this.attachments.clear();
   }

   public void clearWeaponPart(WeaponPart var1) {
      if (var1 == this.activeLight) {
         this.activeLight = null;
      }

      if (var1 == this.activeSight) {
         this.activeSight = null;
      }

      this.attachments.remove(var1.getPartType());
   }

   public void clearWeaponPart(String var1) {
      this.clearWeaponPart((WeaponPart)this.attachments.get(var1));
   }

   public void setWeaponPart(WeaponPart var1) {
      this.setWeaponPart(var1.getPartType(), var1);
   }

   public void setWeaponPart(String var1, WeaponPart var2) {
      if (!StringUtils.isNullOrEmpty(var1)) {
         if (var2 == null) {
            this.clearWeaponPart(var1);
         } else {
            if (var2.hasTag("optics") && this.activeSight == null) {
               this.activeSight = var2;
            }

            if (var2.isTorchCone() && this.activeLight == null) {
               this.activeLight = var2;
            }

            this.attachments.put(var1, var2);
         }

      }
   }

   public WeaponPart getWeaponPart(WeaponPart var1) {
      return (WeaponPart)this.attachments.get(var1.getPartType());
   }

   public WeaponPart getWeaponPart(String var1) {
      return (WeaponPart)this.attachments.get(var1);
   }

   public float getWeaponPartWeightModifier(String var1) {
      return this.getWeaponPartWeightModifier(this.getWeaponPart(var1));
   }

   public float getWeaponPartWeightModifier(WeaponPart var1) {
      return var1 == null ? 0.0F : var1.getWeightModifier();
   }

   public void attachWeaponPart(WeaponPart var1) {
      this.attachWeaponPart((IsoGameCharacter)null, var1, true);
   }

   public void attachWeaponPart(WeaponPart var1, boolean var2) {
      this.attachWeaponPart((IsoGameCharacter)null, var1, var2);
   }

   public void attachWeaponPart(IsoGameCharacter var1, WeaponPart var2) {
      this.attachWeaponPart(var1, var2, true);
   }

   public void attachWeaponPart(IsoGameCharacter var1, WeaponPart var2, boolean var3) {
      if (var2 != null) {
         if (this.attachments.containsKey(var2.getPartType())) {
            this.detachWeaponPart(var1, (WeaponPart)this.attachments.get(var2.getPartType()), var3);
         }

         this.setWeaponPart(var2);
         if (var3) {
            this.setMaxRange(this.getMaxRange() + var2.getMaxRange());
            this.setReloadTime(this.getReloadTime() + var2.getReloadTime());
            this.setRecoilDelay((int)((float)this.getRecoilDelay() + var2.getRecoilDelay()));
            this.setAimingTime(this.getAimingTime() + var2.getAimingTime());
            this.setHitChance(this.getHitChance() + var2.getHitChance());
            this.setProjectileSpread(this.getProjectileSpread() + var2.getSpreadModifier());
            this.setMinDamage(this.getMinDamage() + var2.getDamage());
            this.setMaxDamage(this.getMaxDamage() + var2.getDamage());
            var2.onAttach(var1, this);
         }
      }
   }

   public void detachAllWeaponParts() {
      Iterator var1 = this.getAllWeaponParts().iterator();

      while(var1.hasNext()) {
         WeaponPart var2 = (WeaponPart)var1.next();
         this.detachWeaponPart((IsoGameCharacter)null, var2, true);
      }

   }

   public void detachWeaponPart(WeaponPart var1) {
      this.detachWeaponPart((IsoGameCharacter)null, var1, true);
   }

   public void detachWeaponPart(String var1) {
      this.detachWeaponPart((IsoGameCharacter)null, this.getWeaponPart(var1), true);
   }

   public void detachWeaponPart(IsoGameCharacter var1, WeaponPart var2) {
      this.detachWeaponPart(var1, var2, true);
   }

   public void detachWeaponPart(IsoGameCharacter var1, WeaponPart var2, boolean var3) {
      if (var2 != null && this.getAllWeaponParts().contains(var2)) {
         WeaponPart var4 = this.getWeaponPart(var2.getPartType());
         if (var4 == var2) {
            this.clearWeaponPart(var2);
            if (var3) {
               this.setMaxRange(this.getMaxRange() - var2.getMaxRange());
               this.setClipSize(this.getClipSize() - var2.getClipSize());
               this.setReloadTime(this.getReloadTime() - var2.getReloadTime());
               this.setRecoilDelay((int)((float)this.getRecoilDelay() - var2.getRecoilDelay()));
               this.setAimingTime(this.getAimingTime() - var2.getAimingTime());
               this.setHitChance(this.getHitChance() - var2.getHitChance());
               this.setProjectileSpread(this.getProjectileSpread() - var2.getSpreadModifier());
               this.setMinDamage(this.getMinDamage() - var2.getDamage());
               this.setMaxDamage(this.getMaxDamage() - var2.getDamage());
               var2.onDetach(var1, this);
            }
         }
      }
   }

   public int getTriggerExplosionTimer() {
      return this.triggerExplosionTimer;
   }

   public void setTriggerExplosionTimer(int var1) {
      this.triggerExplosionTimer = var1;
   }

   public boolean canBePlaced() {
      return this.canBePlaced;
   }

   public void setCanBePlaced(boolean var1) {
      this.canBePlaced = var1;
   }

   public int getExplosionRange() {
      return this.explosionRange;
   }

   public void setExplosionRange(int var1) {
      this.explosionRange = var1;
   }

   public int getExplosionPower() {
      return this.explosionPower;
   }

   public void setExplosionPower(int var1) {
      this.explosionPower = var1;
   }

   public int getFireRange() {
      return this.fireRange;
   }

   public void setFireRange(int var1) {
      this.fireRange = var1;
   }

   public int getSmokeRange() {
      return this.smokeRange;
   }

   public void setSmokeRange(int var1) {
      this.smokeRange = var1;
   }

   public int getFirePower() {
      return this.firePower;
   }

   public void setFirePower(int var1) {
      this.firePower = var1;
   }

   public int getNoiseRange() {
      return this.noiseRange;
   }

   public void setNoiseRange(int var1) {
      this.noiseRange = var1;
   }

   public int getNoiseDuration() {
      return this.getScriptItem().getNoiseDuration();
   }

   public float getExtraDamage() {
      return this.extraDamage;
   }

   public void setExtraDamage(float var1) {
      this.extraDamage = var1;
   }

   public int getExplosionTimer() {
      return this.explosionTimer;
   }

   public void setExplosionTimer(int var1) {
      this.explosionTimer = var1;
   }

   public int getExplosionDuration() {
      return this.explosionDuration;
   }

   public void setExplosionDuration(int var1) {
      this.explosionDuration = var1;
   }

   public String getPlacedSprite() {
      return this.placedSprite;
   }

   public void setPlacedSprite(String var1) {
      this.placedSprite = var1;
   }

   public boolean canBeReused() {
      return this.canBeReused;
   }

   public void setCanBeReused(boolean var1) {
      this.canBeReused = var1;
   }

   public int getSensorRange() {
      return this.sensorRange;
   }

   public void setSensorRange(int var1) {
      this.sensorRange = var1;
   }

   public String getRunAnim() {
      return this.RunAnim;
   }

   public float getCritDmgMultiplier() {
      return this.hasSharpness() ? this.critDmgMultiplier * this.getSharpnessMultiplier() : this.critDmgMultiplier;
   }

   public void setCritDmgMultiplier(float var1) {
      this.critDmgMultiplier = var1;
   }

   public String getStaticModel() {
      if (this.getModelIndex() != -1 && this.getWeaponSpritesByIndex() != null) {
         return (String)this.getWeaponSpritesByIndex().get(this.getModelIndex());
      } else {
         return this.staticModel != null ? this.staticModel : this.weaponSprite;
      }
   }

   public String getStaticModelException() {
      return this.hasTag("UseWorldStaticModel") ? this.getWorldStaticModel() : this.getStaticModel();
   }

   public float getBaseSpeed() {
      return this.baseSpeed;
   }

   public void setBaseSpeed(float var1) {
      this.baseSpeed = var1;
   }

   public float getBloodLevel() {
      return this.bloodLevel;
   }

   public void setBloodLevel(float var1) {
      this.bloodLevel = Math.max(0.0F, Math.min(1.0F, var1));
   }

   public void setWeaponLength(float var1) {
      this.WeaponLength = var1;
   }

   public String getAmmoBox() {
      return this.ammoBox;
   }

   public void setAmmoBox(String var1) {
      this.ammoBox = var1;
   }

   public String getMagazineType() {
      return this.magazineType;
   }

   public void setMagazineType(String var1) {
      this.magazineType = var1;
   }

   public String getEjectAmmoStartSound() {
      return this.getScriptItem().getEjectAmmoStartSound();
   }

   public String getEjectAmmoSound() {
      return this.getScriptItem().getEjectAmmoSound();
   }

   public String getEjectAmmoStopSound() {
      return this.getScriptItem().getEjectAmmoStopSound();
   }

   public String getInsertAmmoStartSound() {
      return this.getScriptItem().getInsertAmmoStartSound();
   }

   public String getInsertAmmoSound() {
      return this.getScriptItem().getInsertAmmoSound();
   }

   public String getInsertAmmoStopSound() {
      return this.getScriptItem().getInsertAmmoStopSound();
   }

   public String getRackSound() {
      return this.rackSound;
   }

   public void setRackSound(String var1) {
      this.rackSound = var1;
   }

   public boolean isReloadable(IsoGameCharacter var1) {
      return this.isRanged();
   }

   public boolean isContainsClip() {
      return this.containsClip;
   }

   public void setContainsClip(boolean var1) {
      this.containsClip = this.usesExternalMagazine() && var1;
   }

   public InventoryItem getBestMagazine(IsoGameCharacter var1) {
      return StringUtils.isNullOrEmpty(this.getMagazineType()) ? null : var1.getInventory().getBestTypeRecurse(this.getMagazineType(), magazineComparator);
   }

   public String getWeaponReloadType() {
      return this.weaponReloadType;
   }

   public void setWeaponReloadType(String var1) {
      this.weaponReloadType = var1;
   }

   public boolean isRackAfterShoot() {
      return this.rackAfterShoot;
   }

   public void setRackAfterShoot(boolean var1) {
      this.rackAfterShoot = var1;
   }

   public boolean isRoundChambered() {
      return this.roundChambered;
   }

   public void setRoundChambered(boolean var1) {
      this.roundChambered = this.haveChamber && var1;
   }

   public boolean isSpentRoundChambered() {
      return this.bSpentRoundChambered;
   }

   public void setSpentRoundChambered(boolean var1) {
      this.bSpentRoundChambered = var1;
   }

   public int getSpentRoundCount() {
      return this.spentRoundCount;
   }

   public void setSpentRoundCount(int var1) {
      this.spentRoundCount = PZMath.clamp(var1, 0, this.getMaxAmmo());
   }

   public boolean isManuallyRemoveSpentRounds() {
      return this.getScriptItem().isManuallyRemoveSpentRounds();
   }

   public int getAmmoPerShoot() {
      return this.ammoPerShoot;
   }

   public void setAmmoPerShoot(int var1) {
      this.ammoPerShoot = var1;
   }

   public float getJamGunChance() {
      return this.jamGunChance;
   }

   public void setJamGunChance(float var1) {
      this.jamGunChance = var1;
   }

   public boolean isJammed() {
      return this.isJammed;
   }

   public void setJammed(boolean var1) {
      this.isJammed = var1;
   }

   public boolean checkJam(IsoPlayer var1, boolean var2) {
      boolean var3 = !var2 && !this.isManuallyRemoveSpentRounds() && (var1.getPerkLevel(PerkFactory.Perks.Aiming) < 3 && var1.getPerkLevel(PerkFactory.Perks.Strength) < 6 || var1.getPerkLevel(PerkFactory.Perks.Aiming) < 6 && var1.getPerkLevel(PerkFactory.Perks.Strength) < 3);
      float var4 = 8.0F * ((float)this.getCondition() / (float)this.getConditionMax());
      float var5 = (this.jamGunChance + (float)(var3 ? 1 : 0) + (float)(this.getConditionMax() - this.getCondition()) / var4) * 0.01F;
      float var6 = Rand.Next(0.0F, 1.0F);
      DebugLog.Combat.debugln("Jam chance: " + var5 + ", roll: " + var6 + ", jammed: " + (var6 < var5));
      if (var6 < var5) {
         this.setJammed(true);
      }

      return this.isJammed;
   }

   public boolean checkUnJam(IsoPlayer var1) {
      float var2 = 8.0F * ((float)this.getCondition() / (float)this.getConditionMax());
      float var3 = 8.0F - (float)var1.getPerkLevel(PerkFactory.Perks.Aiming) * 0.5F + (float)((var1.getMoodleLevel(MoodleType.Panic) + var1.getMoodleLevel(MoodleType.Stress) + var1.getMoodleLevel(MoodleType.Drunk)) * 3);
      var3 -= var1.HasTrait("Dextrous") ? 2.0F : 0.0F;
      var3 += var1.HasTrait("AllThumbs") ? 2.0F : 0.0F;
      var3 = PZMath.max(var3, 1.0F);
      var3 = 1.0F - (var3 + (float)(this.getConditionMax() - this.getCondition()) / var2) * 0.01F;
      float var4 = Rand.Next(0.0F, 1.0F);
      DebugLog.Combat.debugln("UnJam chance: " + var3 + ", roll: " + var4 + ", unjammed: " + (var4 < var3));
      if (var4 < var3) {
         this.setJammed(false);
         if (this.shellFallSound != null) {
            var1.getEmitter().playSound(this.shellFallSound);
         }
      }

      return this.isJammed;
   }

   public String getClickSound() {
      return this.clickSound;
   }

   public void setClickSound(String var1) {
      this.clickSound = var1;
   }

   public ArrayList<ModelWeaponPart> getModelWeaponPart() {
      return this.modelWeaponPart;
   }

   public void setModelWeaponPart(ArrayList<ModelWeaponPart> var1) {
      this.modelWeaponPart = var1;
   }

   public String getOriginalWeaponSprite() {
      return this.originalWeaponSprite;
   }

   public void setOriginalWeaponSprite(String var1) {
      this.originalWeaponSprite = var1;
   }

   public boolean haveChamber() {
      return this.haveChamber;
   }

   public void setHaveChamber(boolean var1) {
      this.haveChamber = var1;
   }

   public String getDamageCategory() {
      return this.damageCategory;
   }

   public void setDamageCategory(String var1) {
      this.damageCategory = var1;
   }

   public boolean isDamageMakeHole() {
      return this.damageMakeHole;
   }

   public void setDamageMakeHole(boolean var1) {
      this.damageMakeHole = var1;
   }

   public String getHitFloorSound() {
      return this.hitFloorSound;
   }

   public void setHitFloorSound(String var1) {
      this.hitFloorSound = var1;
   }

   public boolean isInsertAllBulletsReload() {
      return this.insertAllBulletsReload;
   }

   public void setInsertAllBulletsReload(boolean var1) {
      this.insertAllBulletsReload = var1;
   }

   public String getFireMode() {
      return this.fireMode;
   }

   public void setFireMode(String var1) {
      this.fireMode = var1;
   }

   public ArrayList<String> getFireModePossibilities() {
      return this.fireModePossibilities;
   }

   public void setFireModePossibilities(ArrayList<String> var1) {
      this.fireModePossibilities = var1;
   }

   public void randomizeBullets() {
      if (this.isRanged() && !Rand.NextBool(4)) {
         this.setCurrentAmmoCount(Rand.Next(this.getMaxAmmo() - 2, this.getMaxAmmo()));
         if (!StringUtils.isNullOrEmpty(this.getMagazineType())) {
            this.setContainsClip(true);
         }

         if (this.haveChamber()) {
            this.setRoundChambered(true);
         }

      }
   }

   public boolean canEmitLight() {
      return this.activeLight != null && this.activeLight.canEmitLight() || super.canEmitLight();
   }

   public float getLightStrength() {
      return this.activeLight != null ? this.activeLight.getLightStrength() : super.getLightStrength();
   }

   public boolean isTorchCone() {
      return this.activeLight != null && this.activeLight.isTorchCone() || super.isTorchCone();
   }

   public float getTorchDot() {
      return this.activeLight != null ? this.activeLight.getTorchDot() : super.getTorchDot();
   }

   public int getLightDistance() {
      return this.activeLight != null ? this.activeLight.getLightDistance() : super.getLightDistance();
   }

   public boolean canBeActivated() {
      return this.activeLight != null ? this.activeLight.canBeActivated() : super.canBeActivated();
   }

   public float getStopPower() {
      return this.getScriptItem().stopPower;
   }

   public boolean isInstantExplosion() {
      return this.explosionTimer <= 0 && this.sensorRange <= 0 && this.getRemoteControlID() == -1 && !this.canBeRemote();
   }

   public void setWeaponSpritesByIndex(ArrayList<String> var1) {
      this.weaponSpritesByIndex = var1;
   }

   public ArrayList<String> getWeaponSpritesByIndex() {
      return this.weaponSpritesByIndex;
   }

   public boolean usesExternalMagazine() {
      return this.getMagazineType() != null;
   }

   public void inheritAmmunition(HandWeapon var1) {
      this.setJammed(var1.isJammed());
      if (var1.haveChamber() && this.haveChamber()) {
         this.setRoundChambered(var1.isRoundChambered());
         this.setSpentRoundChambered(var1.isSpentRoundChambered());
      }

      if (var1.usesExternalMagazine() && this.usesExternalMagazine()) {
         this.setContainsClip(var1.isContainsClip());
      }

      this.setCurrentAmmoCount(var1.getCurrentAmmoCount());
      this.setFireMode(var1.getFireMode());
   }
}
