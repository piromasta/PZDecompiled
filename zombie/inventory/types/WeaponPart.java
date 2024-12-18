package zombie.inventory.types;

import java.util.ArrayList;
import zombie.Lua.LuaManager;
import zombie.characters.IsoGameCharacter;
import zombie.core.Translator;
import zombie.debug.DebugLogStream;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemType;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Item;
import zombie.ui.ObjectTooltip;
import zombie.util.StringUtils;

public final class WeaponPart extends InventoryItem {
   public static final String TYPE_CANON = "Canon";
   public static final String TYPE_CLIP = "Clip";
   public static final String TYPE_RECOILPAD = "RecoilPad";
   public static final String TYPE_SCOPE = "Scope";
   public static final String TYPE_SLING = "Sling";
   public static final String TYPE_STOCK = "Stock";
   private float maxRange = 0.0F;
   private float minSightRange = 0.0F;
   private float maxSightRange = 0.0F;
   private float lowLightBonus = 0.0F;
   private float minRangeRanged = 0.0F;
   private float damage = 0.0F;
   private float recoilDelay = 0.0F;
   private int clipSize = 0;
   private int reloadTime = 0;
   private int aimingTime = 0;
   private int hitChance = 0;
   private float angle = 0.0F;
   private float spreadModifier = 0.0F;
   private float weightModifier = 0.0F;
   private final ArrayList<String> mountOn = new ArrayList();
   private final ArrayList<String> mountOnDisplayName = new ArrayList();
   private String partType = null;
   String canAttachCallback = null;
   String onAttachCallback = null;
   String onDetachCallback = null;

   public WeaponPart(String var1, String var2, String var3, String var4) {
      super(var1, var2, var3, var4);
      this.cat = ItemType.Weapon;
   }

   public int getSaveType() {
      return Item.Type.WeaponPart.ordinal();
   }

   public String getCategory() {
      return this.mainCategory != null ? this.mainCategory : "WeaponPart";
   }

   public void DoTooltip(ObjectTooltip var1, ObjectTooltip.Layout var2) {
      ObjectTooltip.LayoutItem var3 = var2.addItem();
      var3.setLabel(Translator.getText("Tooltip_weapon_Type") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
      var3.setValue(Translator.getText("Tooltip_weapon_" + this.partType), 1.0F, 1.0F, 0.8F, 1.0F);
      var3 = var2.addItem();
      String var10000 = Translator.getText("Tooltip_weapon_CanBeMountOn");
      String var4 = var10000 + this.mountOnDisplayName.toString().replaceAll("\\[", "").replaceAll("\\]", "");
      var3.setLabel(var4, 1.0F, 1.0F, 0.8F, 1.0F);
   }

   public float getMinSightRange() {
      return this.minSightRange;
   }

   public void setMinSightRange(float var1) {
      this.minSightRange = var1;
   }

   public float getMaxSightRange() {
      return this.maxSightRange;
   }

   public void setLowLightBonus(float var1) {
      this.lowLightBonus = var1;
   }

   public float getLowLightBonus() {
      return this.lowLightBonus;
   }

   public void setMaxSightRange(float var1) {
      this.maxSightRange = var1;
   }

   public float getMinRangeRanged() {
      return this.minRangeRanged;
   }

   public void setMinRangeRanged(float var1) {
      this.minRangeRanged = var1;
   }

   public float getMaxRange() {
      return this.maxRange;
   }

   public void setMaxRange(float var1) {
      this.maxRange = var1;
   }

   public float getRecoilDelay() {
      return this.recoilDelay;
   }

   public void setRecoilDelay(float var1) {
      this.recoilDelay = var1;
   }

   public int getClipSize() {
      return this.clipSize;
   }

   public void setClipSize(int var1) {
      this.clipSize = var1;
   }

   public float getDamage() {
      return this.damage;
   }

   public void setDamage(float var1) {
      this.damage = var1;
   }

   public ArrayList<String> getMountOn() {
      return this.mountOn;
   }

   public void setMountOn(ArrayList<String> var1) {
      this.mountOn.clear();
      this.mountOnDisplayName.clear();

      for(int var2 = 0; var2 < var1.size(); ++var2) {
         String var3 = (String)var1.get(var2);
         if (!var3.contains(".")) {
            String var10000 = this.getModule();
            var3 = var10000 + "." + var3;
         }

         Item var4 = ScriptManager.instance.getItem(var3);
         if (var4 != null) {
            this.mountOn.add(var4.getFullName());
            this.mountOnDisplayName.add(var4.getDisplayName());
         }
      }

   }

   public String getPartType() {
      return this.partType;
   }

   public void setPartType(String var1) {
      this.partType = var1;
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

   public int getHitChance() {
      return this.hitChance;
   }

   public void setHitChance(int var1) {
      this.hitChance = var1;
   }

   public float getAngle() {
      return this.angle;
   }

   public void setAngle(float var1) {
      this.angle = var1;
   }

   public float getSpreadModifier() {
      return this.spreadModifier;
   }

   public void setSpreadModifier(float var1) {
      this.spreadModifier = var1;
   }

   public float getWeightModifier() {
      return this.weightModifier;
   }

   public void setWeightModifier(float var1) {
      this.weightModifier = var1;
   }

   public void setCanAttachCallback(String var1) {
      this.canAttachCallback = var1;
   }

   public boolean canAttach(IsoGameCharacter var1, HandWeapon var2) {
      if (!this.mountOn.isEmpty() && !this.mountOn.contains(var2.getFullType())) {
         return false;
      } else if (StringUtils.isNullOrEmpty(this.canAttachCallback)) {
         return true;
      } else {
         Object var3 = LuaManager.getFunctionObject(this.canAttachCallback, (DebugLogStream)null);
         if (var3 == null) {
            return var2.getWeaponPart(this) == null;
         } else {
            Boolean var4 = LuaManager.caller.protectedCallBoolean(LuaManager.thread, var3, var1, var2, this);
            return var4 != null && var4;
         }
      }
   }

   public void setOnAttachCallback(String var1) {
      this.onAttachCallback = var1;
   }

   public void onAttach(IsoGameCharacter var1, HandWeapon var2) {
      if (!StringUtils.isNullOrEmpty(this.onAttachCallback)) {
         Object var3 = LuaManager.getFunctionObject(this.onAttachCallback, (DebugLogStream)null);
         if (var3 != null) {
            LuaManager.caller.protectedCallVoid(LuaManager.thread, var3, var1, var2, this);
         }

      }
   }

   public void setOnDetachCallback(String var1) {
      this.onDetachCallback = var1;
   }

   public void onDetach(IsoGameCharacter var1, HandWeapon var2) {
      if (!StringUtils.isNullOrEmpty(this.onDetachCallback)) {
         Object var3 = LuaManager.getFunctionObject(this.onDetachCallback, (DebugLogStream)null);
         if (var3 != null) {
            LuaManager.caller.protectedCallVoid(LuaManager.thread, var3, var1, var2, this);
         }

      }
   }
}
