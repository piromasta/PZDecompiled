package zombie.scripting.objects;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;
import zombie.characters.IsoGameCharacter;
import zombie.characters.skills.PerkFactory;
import zombie.core.Translator;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.Food;
import zombie.inventory.types.HandWeapon;
import zombie.scripting.ScriptManager;
import zombie.util.StringUtils;

public final class EvolvedRecipe extends BaseScriptObject {
   private static final DecimalFormat DECIMAL_FORMAT;
   public String name = null;
   public String DisplayName = null;
   private String originalname;
   public int maxItems = 0;
   public final Map<String, ItemRecipe> itemsList = new HashMap();
   public String resultItem = null;
   public String baseItem = null;
   public boolean cookable = false;
   public boolean addIngredientIfCooked = false;
   public boolean canAddSpicesEmpty = false;
   public String addIngredientSound = null;
   public boolean hidden = false;
   public boolean allowFrozenItem = false;

   public EvolvedRecipe(String var1) {
      this.name = var1;
   }

   public void Load(String var1, String[] var2) {
      this.DisplayName = Translator.getRecipeName(var1);
      this.originalname = var1;

      for(int var3 = 0; var3 < var2.length; ++var3) {
         if (!var2[var3].trim().isEmpty() && var2[var3].contains(":")) {
            String[] var4 = var2[var3].split(":");
            String var5 = var4[0].trim();
            String var6 = var4[1].trim();
            if (var5.equals("BaseItem")) {
               this.baseItem = var6;
            } else if (var5.equals("Name")) {
               this.DisplayName = Translator.getRecipeName(var6);
               this.originalname = var6;
            } else if (var5.equals("ResultItem")) {
               this.resultItem = var6;
               if (!var6.contains(".")) {
                  this.resultItem = var6;
               }
            } else if (var5.equals("Cookable")) {
               this.cookable = true;
            } else if (var5.equals("MaxItems")) {
               this.maxItems = Integer.parseInt(var6);
            } else if (var5.equals("AddIngredientIfCooked")) {
               this.addIngredientIfCooked = Boolean.parseBoolean(var6);
            } else if (var5.equals("AddIngredientSound")) {
               this.addIngredientSound = StringUtils.discardNullOrWhitespace(var6);
            } else if (var5.equals("CanAddSpicesEmpty")) {
               this.canAddSpicesEmpty = Boolean.parseBoolean(var6);
            } else if (var5.equals("IsHidden")) {
               this.hidden = Boolean.parseBoolean(var6);
            } else if (var5.equals("AllowFrozenItem")) {
               this.allowFrozenItem = Boolean.parseBoolean(var6);
            }
         }
      }

   }

   public boolean needToBeCooked(InventoryItem var1) {
      ItemRecipe var2 = this.getItemRecipe(var1);
      if (var2 == null) {
         return true;
      } else {
         return var2.cooked == var1.isCooked() || var2.cooked == var1.isBurnt() || !var2.cooked;
      }
   }

   public ArrayList<InventoryItem> getItemsCanBeUse(IsoGameCharacter var1, InventoryItem var2, ArrayList<ItemContainer> var3) {
      int var4 = var1.getPerkLevel(PerkFactory.Perks.Cooking);
      if (var3 == null) {
         var3 = new ArrayList();
      }

      ArrayList var5 = new ArrayList();
      Iterator var6 = this.itemsList.keySet().iterator();
      if (!var3.contains(var1.getInventory())) {
         var3.add(var1.getInventory());
      }

      while(var6.hasNext()) {
         String var7 = (String)var6.next();
         Iterator var8 = var3.iterator();

         while(var8.hasNext()) {
            ItemContainer var9 = (ItemContainer)var8.next();
            this.checkItemCanBeUse(var9, var7, var2, var4, var5);
         }
      }

      if (var2.haveExtraItems() && var2.getExtraItems().size() >= 3) {
         for(int var11 = 0; var11 < var3.size(); ++var11) {
            ItemContainer var12 = (ItemContainer)var3.get(var11);

            for(int var13 = 0; var13 < var12.getItems().size(); ++var13) {
               InventoryItem var10 = (InventoryItem)var12.getItems().get(var13);
               if (var10 instanceof Food && ((Food)var10).getPoisonLevelForRecipe() != null && var1.isKnownPoison(var10) && !var5.contains(var10)) {
                  var5.add(var10);
               }
            }
         }
      }

      return var5;
   }

   private void checkItemCanBeUse(ItemContainer var1, String var2, InventoryItem var3, int var4, ArrayList<InventoryItem> var5) {
      ArrayList var6 = var1.getItemsFromType(var2);

      for(int var7 = 0; var7 < var6.size(); ++var7) {
         InventoryItem var8 = (InventoryItem)var6.get(var7);
         boolean var9 = false;
         if (var8 instanceof Food var10 && ((ItemRecipe)this.itemsList.get(var2)).use != -1) {
            if (var10.isSpice()) {
               if (this.isResultItem(var3)) {
                  var9 = !this.isSpiceAdded(var3, var10);
               } else if (this.canAddSpicesEmpty) {
                  var9 = true;
               }

               if (var10.isRotten() && var4 < 7) {
                  var9 = false;
               }
            } else if ((!var3.haveExtraItems() || var3.extraItems.size() < this.maxItems) && (!var10.isRotten() || var4 >= 7)) {
               var9 = true;
            }

            if (var10.isFrozen() && !this.allowFrozenItem) {
               var9 = false;
            }
         } else {
            var9 = true;
         }

         this.getItemRecipe(var8);
         if (var9) {
            var5.add(var8);
         }
      }

   }

   public InventoryItem addItem(InventoryItem var1, InventoryItem var2, IsoGameCharacter var3) {
      int var4 = var3.getPerkLevel(PerkFactory.Perks.Cooking);
      if (!this.isResultItem(var1)) {
         InventoryItem var5 = var1 instanceof Food ? var1 : null;
         InventoryItem var6 = InventoryItemFactory.CreateItem(this.resultItem);
         if (var6 != null) {
            if (var1 instanceof HandWeapon) {
               var6.getModData().rawset("condition:" + var1.getType(), (double)var1.getCondition() / (double)var1.getConditionMax());
            }

            var3.getInventory().Remove(var1);
            var3.getInventory().AddItem(var6);
            InventoryItem var7 = var1;
            var1 = var6;
            if (var6 instanceof Food) {
               ((Food)var6).setCalories(0.0F);
               ((Food)var6).setCarbohydrates(0.0F);
               ((Food)var6).setProteins(0.0F);
               ((Food)var6).setLipids(0.0F);
               if (var2 instanceof Food && ((Food)var2).getPoisonLevelForRecipe() != null) {
                  this.addPoison(var2, var6, var3);
               }

               ((Food)var6).setIsCookable(this.cookable);
               if (var5 != null) {
                  ((Food)var6).setHungChange(((Food)var5).getHungChange());
                  ((Food)var6).setBaseHunger(((Food)var5).getBaseHunger());
               } else {
                  ((Food)var6).setHungChange(0.0F);
                  ((Food)var6).setBaseHunger(0.0F);
               }

               if (var7.isTaintedWater()) {
                  var6.setTaintedWater(true);
               }

               if (var7 instanceof Food && var7.getOffAgeMax() != 1000000000 && var6.getOffAgeMax() != 1000000000) {
                  float var8 = var7.getAge() / (float)var7.getOffAgeMax();
                  var6.setAge((float)var6.getOffAgeMax() * var8);
               }

               if (var5 instanceof Food) {
                  ((Food)var6).setCalories(((Food)var5).getCalories());
                  ((Food)var6).setProteins(((Food)var5).getProteins());
                  ((Food)var6).setLipids(((Food)var5).getLipids());
                  ((Food)var6).setCarbohydrates(((Food)var5).getCarbohydrates());
                  ((Food)var6).setThirstChange(((Food)var5).getThirstChange());
               }
            }

            var6.setUnhappyChange(0.0F);
            var6.setBoredomChange(0.0F);
         }
      }

      if (this.itemsList.get(var2.getType()) != null && ((ItemRecipe)this.itemsList.get(var2.getType())).use > -1) {
         if (!(var2 instanceof Food)) {
            var2.Use();
         } else {
            float var15 = (float)((ItemRecipe)this.itemsList.get(var2.getType())).use / 100.0F;
            Food var16 = (Food)var2;
            Food var17 = (Food)var1;
            boolean var18 = var17.hasTag("HerbalTea") && var16.hasTag("HerbalTea");
            if (var16.isSpice() && var1 instanceof Food) {
               if (var1 instanceof Food && var18) {
                  var17.setReduceFoodSickness(var17.getReduceFoodSickness() + var16.getReduceFoodSickness());
                  var17.setPainReduction(var17.getPainReduction() + var16.getPainReduction());
                  var17.setFluReduction(var17.getFluReduction() + var16.getFluReduction());
                  if (var16.getEnduranceChange() > 0.0F) {
                     var17.setEnduranceChange(var17.getEnduranceChange() + var16.getEnduranceChange());
                  }

                  if (var17.getReduceFoodSickness() > 12) {
                     var17.setReduceFoodSickness(12);
                  }
               }

               this.useSpice(var16, (Food)var1, var15, var4);
               return var1;
            }

            boolean var9 = false;
            DecimalFormat var10;
            if (var16.isRotten()) {
               var10 = DECIMAL_FORMAT;
               var10.setRoundingMode(RoundingMode.HALF_EVEN);
               if (var4 != 7 && var4 != 8) {
                  if (var4 == 9 || var4 == 10) {
                     var15 = Float.parseFloat(var10.format((double)Math.abs(var16.getBaseHunger() - (var16.getBaseHunger() - 0.1F * var16.getBaseHunger()))).replace(",", "."));
                  }
               } else {
                  var15 = Float.parseFloat(var10.format((double)Math.abs(var16.getBaseHunger() - (var16.getBaseHunger() - 0.05F * var16.getBaseHunger()))).replace(",", "."));
               }

               var9 = true;
            }

            if (Math.abs(var16.getHungerChange()) < var15) {
               var10 = DECIMAL_FORMAT;
               var10.setRoundingMode(RoundingMode.DOWN);
               var15 = Math.abs(Float.parseFloat(var10.format((double)var16.getHungerChange()).replace(",", ".")));
               var9 = true;
            }

            if (var1 instanceof Food) {
               if (var2 instanceof Food && ((Food)var2).getPoisonLevelForRecipe() != null) {
                  this.addPoison(var2, var1, var3);
               }

               var17.setHungChange(var17.getHungChange() - var15);
               var17.setBaseHunger(var17.getBaseHunger() - var15);
               if (var16.isbDangerousUncooked() && !var16.isCooked()) {
                  var17.setbDangerousUncooked(true);
               }

               int var19 = 0;
               if (var1.extraItems != null) {
                  for(int var11 = 0; var11 < var1.extraItems.size(); ++var11) {
                     if (((String)var1.extraItems.get(var11)).equals(var2.getFullType())) {
                        ++var19;
                     }
                  }
               }

               if (var1.extraItems != null && var1.extraItems.size() - 2 > var4) {
                  var19 += var1.extraItems.size() - 2 - var4 * 3;
               }

               float var20 = var15 - (float)(3 * var4) / 100.0F * var15;
               float var12 = Math.abs(var20 / var16.getHungChange());
               if (var12 > 1.0F) {
                  var12 = 1.0F;
               }

               var1.setUnhappyChange(((Food)var1).getUnhappyChangeUnmodified() - (float)(5 - var19 * 5));
               if (var1.getUnhappyChange() > 25.0F) {
                  var1.setUnhappyChange(25.0F);
               }

               float var13 = (float)var4 / 15.0F + 1.0F;
               var17.setCalories(var17.getCalories() + var16.getCalories() * var13 * var12);
               var17.setProteins(var17.getProteins() + var16.getProteins() * var13 * var12);
               var17.setCarbohydrates(var17.getCarbohydrates() + var16.getCarbohydrates() * var13 * var12);
               var17.setLipids(var17.getLipids() + var16.getLipids() * var13 * var12);
               float var14 = var16.getThirstChangeUnmodified() * var13 * var12;
               if (!var16.hasTag("DriedFood")) {
                  var17.setThirstChange(var17.getThirstChangeUnmodified() + var14);
               }

               if (var16.isCooked()) {
                  var20 = (float)((double)var20 / 1.3);
               }

               var16.setHungChange(var16.getHungChange() + var20);
               var16.setBaseHunger(var16.getBaseHunger() + var20);
               var16.setThirstChange(var16.getThirstChange() - var14);
               var16.setCalories(var16.getCalories() - var16.getCalories() * var12);
               var16.setProteins(var16.getProteins() - var16.getProteins() * var12);
               var16.setCarbohydrates(var16.getCarbohydrates() - var16.getCarbohydrates() * var12);
               var16.setLipids(var16.getLipids() - var16.getLipids() * var12);
               if (var17.hasTag("AlcoholicBeverage") && var16.isAlcoholic()) {
                  var17.setAlcoholic(true);
               }

               if (var18) {
                  var17.setReduceFoodSickness(var17.getReduceFoodSickness() + var16.getReduceFoodSickness());
                  var17.setPainReduction(var17.getPainReduction() + var16.getPainReduction());
                  var17.setFluReduction(var17.getFluReduction() + var16.getFluReduction());
                  if (var17.getReduceFoodSickness() > 12) {
                     var17.setReduceFoodSickness(12);
                  }
               }

               if ((double)var16.getHungerChange() >= -0.02 || var9) {
                  var2.Use();
               }

               if (var16.getFatigueChange() < 0.0F) {
                  var1.setFatigueChange(var16.getFatigueChange() * var12);
                  var16.setFatigueChange(var16.getFatigueChange() - var16.getFatigueChange() * var12);
               }

               if (var16.getPoisonPower() > 0) {
                  var16.setPoisonPower((int)((double)((float)var16.getPoisonPower() - (float)var16.getPoisonPower() * var12) + 0.999));
                  ((Food)var1).setPoisonPower((int)((double)((float)var16.getPoisonPower() * var12) + 0.999));
               }
            }
         }

         var1.addExtraItem(var2.getFullType());
      } else if (var2 instanceof Food && ((Food)var2).getPoisonLevelForRecipe() != null) {
         this.addPoison(var2, var1, var3);
      }

      this.checkUniqueRecipe(var1);
      var3.getXp().AddXP(PerkFactory.Perks.Cooking, 3.0F);
      return var1;
   }

   private void checkUniqueRecipe(InventoryItem var1) {
      if (var1 instanceof Food var2) {
         Stack var3 = ScriptManager.instance.getAllUniqueRecipes();

         for(int var4 = 0; var4 < var3.size(); ++var4) {
            ArrayList var5 = new ArrayList();
            UniqueRecipe var6 = (UniqueRecipe)var3.get(var4);
            if (var6.getBaseRecipe().equals(var1.getType())) {
               boolean var7 = true;

               for(int var8 = 0; var8 < var6.getItems().size(); ++var8) {
                  boolean var9 = false;

                  for(int var10 = 0; var10 < var2.getExtraItems().size(); ++var10) {
                     if (!var5.contains(var10) && ((String)var2.getExtraItems().get(var10)).equals(var6.getItems().get(var8))) {
                        var9 = true;
                        var5.add(var10);
                        break;
                     }
                  }

                  if (!var9) {
                     var7 = false;
                     break;
                  }
               }

               if (var2.getExtraItems().size() == var6.getItems().size() && var7) {
                  var2.setName(var6.getName());
                  if (var2.hasTag("Beer")) {
                     var2.setName("Beer");
                  }

                  var2.setBaseHunger(var2.getBaseHunger() - (float)var6.getHungerBonus() / 100.0F);
                  var2.setHungChange(var2.getBaseHunger());
                  var2.setBoredomChange(var2.getBoredomChangeUnmodified() - (float)var6.getBoredomBonus());
                  var2.setUnhappyChange(var2.getUnhappyChangeUnmodified() - (float)var6.getHapinessBonus());
                  var2.setCustomName(true);
               }
            }
         }
      }

   }

   private void addPoison(InventoryItem var1, InventoryItem var2, IsoGameCharacter var3) {
      Food var4 = (Food)var1;
      if (var2 instanceof Food var5) {
         int var6 = var4.getPoisonLevelForRecipe() - var3.getPerkLevel(PerkFactory.Perks.Cooking);
         if (var6 < 1) {
            var6 = 1;
         }

         Float var7 = 0.0F;
         float var8;
         if (var4.getThirstChange() <= -0.01F) {
            var8 = (float)var4.getUseForPoison() / 100.0F;
            if (Math.abs(var4.getThirstChange()) < var8) {
               var8 = Math.abs(var4.getThirstChange());
            }

            var7 = Math.abs(var8 / var4.getThirstChange());
            var7 = new Float((double)Math.round(var7.doubleValue() * 100.0) / 100.0);
            var4.setThirstChange(var4.getThirstChange() + var8);
            if ((double)var4.getThirstChange() > -0.01) {
               var4.Use();
            }
         } else if (var4.getBaseHunger() <= -0.01F) {
            var8 = (float)var4.getUseForPoison() / 100.0F;
            if (Math.abs(var4.getBaseHunger()) < var8) {
               var8 = Math.abs(var4.getThirstChange());
            }

            var7 = Math.abs(var8 / var4.getBaseHunger());
            var7 = new Float((double)Math.round(var7.doubleValue() * 100.0) / 100.0);
         }

         if (var5.getPoisonDetectionLevel() == -1) {
            var5.setPoisonDetectionLevel(0);
         }

         var5.setPoisonDetectionLevel(var5.getPoisonDetectionLevel() + var6);
         if (var5.getPoisonDetectionLevel() > 10) {
            var5.setPoisonDetectionLevel(10);
         }

         int var9 = (new Float(var7 * ((float)var4.getPoisonPower() / 100.0F) * 100.0F)).intValue();
         var5.setPoisonPower(var5.getPoisonPower() + var9);
         var4.setPoisonPower(var4.getPoisonPower() - var9);
      }

   }

   private void useSpice(Food var1, Food var2, float var3, int var4) {
      if (!this.isSpiceAdded(var2, var1)) {
         if (var2.spices == null) {
            var2.spices = new ArrayList();
         }

         var2.spices.add(var1.getFullType());
         float var5 = var3;
         if (var1.isRotten()) {
            DecimalFormat var6 = DECIMAL_FORMAT;
            var6.setRoundingMode(RoundingMode.HALF_EVEN);
            if (var4 != 7 && var4 != 8) {
               if (var4 == 9 || var4 == 10) {
                  var3 = Float.parseFloat(var6.format((double)Math.abs(var1.getBaseHunger() - (var1.getBaseHunger() - 0.1F * var1.getBaseHunger()))).replace(",", "."));
               }
            } else {
               var3 = Float.parseFloat(var6.format((double)Math.abs(var1.getBaseHunger() - (var1.getBaseHunger() - 0.05F * var1.getBaseHunger()))).replace(",", "."));
            }
         }

         float var8 = Math.abs(var3 / var1.getHungChange());
         if (var8 > 1.0F) {
            var8 = 1.0F;
         }

         float var7 = (float)var4 / 15.0F + 1.0F;
         var2.setUnhappyChange(var2.getUnhappyChangeUnmodified() - var3 * 200.0F);
         var2.setBoredomChange(var2.getBoredomChangeUnmodified() - var3 * 200.0F);
         var2.setCalories(var2.getCalories() + var1.getCalories() * var7 * var8);
         var2.setProteins(var2.getProteins() + var1.getProteins() * var7 * var8);
         var2.setCarbohydrates(var2.getCarbohydrates() + var1.getCarbohydrates() * var7 * var8);
         var2.setLipids(var2.getLipids() + var1.getLipids() * var7 * var8);
         var8 = Math.abs(var5 / var1.getHungChange());
         if (var8 > 1.0F) {
            var8 = 1.0F;
         }

         var1.setCalories(var1.getCalories() - var1.getCalories() * var8);
         var1.setProteins(var1.getProteins() - var1.getProteins() * var8);
         var1.setCarbohydrates(var1.getCarbohydrates() - var1.getCarbohydrates() * var8);
         var1.setLipids(var1.getLipids() - var1.getLipids() * var8);
         var1.setHungChange(var1.getHungChange() + var5);
         if ((double)var1.getHungerChange() > -0.01) {
            var1.Use();
         }
      }

   }

   public ItemRecipe getItemRecipe(InventoryItem var1) {
      return (ItemRecipe)this.itemsList.get(var1.getType());
   }

   public String getName() {
      return this.DisplayName;
   }

   public String getOriginalname() {
      return this.originalname;
   }

   public String getUntranslatedName() {
      return this.name;
   }

   public String getBaseItem() {
      return this.baseItem;
   }

   public Map<String, ItemRecipe> getItemsList() {
      return this.itemsList;
   }

   public ArrayList<ItemRecipe> getPossibleItems() {
      ArrayList var1 = new ArrayList();
      Iterator var2 = this.itemsList.values().iterator();

      while(var2.hasNext()) {
         ItemRecipe var3 = (ItemRecipe)var2.next();
         var1.add(var3);
      }

      return var1;
   }

   public String getResultItem() {
      return !this.resultItem.contains(".") ? this.resultItem : this.resultItem.split("\\.")[1];
   }

   public String getFullResultItem() {
      return this.resultItem;
   }

   public boolean isCookable() {
      return this.cookable;
   }

   public int getMaxItems() {
      return this.maxItems;
   }

   public boolean isResultItem(InventoryItem var1) {
      return var1 == null ? false : this.getResultItem().equals(var1.getType());
   }

   public boolean isSpiceAdded(InventoryItem var1, InventoryItem var2) {
      if (!this.isResultItem(var1)) {
         return false;
      } else if (var1 instanceof Food && var2 instanceof Food) {
         if (!((Food)var2).isSpice()) {
            return false;
         } else {
            ArrayList var3 = ((Food)var1).getSpices();
            return var3 == null ? false : var3.contains(var2.getFullType());
         }
      } else {
         return false;
      }
   }

   public String getAddIngredientSound() {
      return this.addIngredientSound;
   }

   public void setIsHidden(boolean var1) {
      this.hidden = var1;
   }

   public boolean isHidden() {
      return this.hidden;
   }

   public boolean isAllowFrozenItem() {
      return this.allowFrozenItem;
   }

   public void setAllowFrozenItem(boolean var1) {
      this.allowFrozenItem = var1;
   }

   static {
      DECIMAL_FORMAT = (DecimalFormat)NumberFormat.getInstance(Locale.US);
      DECIMAL_FORMAT.applyPattern("#.##");
   }
}
