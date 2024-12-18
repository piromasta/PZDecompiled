package zombie.scripting.entity.components.crafting;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.ComponentType;
import zombie.entity.components.crafting.FluidMatchMode;
import zombie.entity.components.crafting.InputFlag;
import zombie.entity.components.crafting.ItemApplyMode;
import zombie.entity.components.crafting.recipe.CraftRecipeManager;
import zombie.entity.components.crafting.recipe.OutputMapper;
import zombie.entity.components.fluids.Fluid;
import zombie.entity.components.fluids.FluidContainer;
import zombie.entity.components.resources.ResourceType;
import zombie.entity.energy.Energy;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemTags;
import zombie.inventory.types.DrainableComboItem;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptManager;
import zombie.scripting.ScriptParser;
import zombie.scripting.objects.Item;
import zombie.util.StringUtils;
import zombie.util.list.PZUnmodifiableList;

@DebugClassFields
public class InputScript extends CraftRecipe.IOScript {
   private final ArrayList<String> loadedItems = new ArrayList();
   private final ArrayList<String> loadedFluids = new ArrayList();
   private final ArrayList<String> loadedEnergies = new ArrayList();
   private final ArrayList<String> items = new ArrayList();
   private final ArrayList<String> tags = new ArrayList();
   private final ArrayList<Fluid> fluids = new ArrayList();
   private final ArrayList<Energy> energies = new ArrayList();
   private boolean acceptsAnyItem = false;
   private boolean acceptsAnyFluid = false;
   private boolean acceptsAnyEnergy = false;
   private final ResourceType type;
   private List<Item> itemScriptCache = new ArrayList();
   private ItemApplyMode itemApplyMode;
   private FluidMatchMode fluidMatchMode;
   private float amount;
   private final ArrayList<Float> amounts;
   private boolean applyOnTick;
   private int shapedIndex;
   private String originalLine;
   protected OutputScript createToItemScript;
   protected InputScript consumeFromItemScript;
   private final EnumSet<InputFlag> flags;
   boolean bIsExclusive;
   boolean bIsItemCount;

   private InputScript(CraftRecipe var1, ResourceType var2) {
      super(var1);
      this.itemApplyMode = ItemApplyMode.Normal;
      this.fluidMatchMode = FluidMatchMode.Exact;
      this.amounts = new ArrayList();
      this.applyOnTick = false;
      this.shapedIndex = -1;
      this.originalLine = "";
      this.createToItemScript = null;
      this.consumeFromItemScript = null;
      this.flags = EnumSet.noneOf(InputFlag.class);
      this.bIsExclusive = false;
      this.bIsItemCount = false;
      this.type = var2;
   }

   private boolean typeCheck(ResourceType var1) {
      return this.type == var1;
   }

   protected boolean isValid() {
      if (this.type == ResourceType.Item) {
         return this.acceptsAnyItem || this.items.size() > 0;
      } else if (this.type == ResourceType.Fluid) {
         return this.acceptsAnyFluid || this.fluids.size() > 0;
      } else if (this.type != ResourceType.Energy) {
         return false;
      } else {
         return this.acceptsAnyEnergy || this.energies.size() > 0;
      }
   }

   public List<Item> getPossibleInputItems() {
      if (this.acceptsAnyItem) {
      }

      return this.itemScriptCache;
   }

   public ArrayList<Fluid> getPossibleInputFluids() {
      return this.acceptsAnyFluid ? Fluid.getAllFluids() : this.fluids;
   }

   public ArrayList<Energy> getPossibleInputEnergies() {
      return this.acceptsAnyEnergy ? Energy.getAllEnergies() : this.energies;
   }

   public boolean hasCreateToItem() {
      return this.createToItemScript != null;
   }

   public OutputScript getCreateToItemScript() {
      return this.createToItemScript;
   }

   public boolean hasConsumeFromItem() {
      return this.consumeFromItemScript != null;
   }

   public InputScript getConsumeFromItemScript() {
      return this.consumeFromItemScript;
   }

   public boolean hasFlag(InputFlag var1) {
      return this.flags.contains(var1);
   }

   public String getOriginalLine() {
      return this.originalLine;
   }

   public ResourceType getResourceType() {
      return this.type;
   }

   /** @deprecated */
   @Deprecated
   public boolean isUse() {
      return true;
   }

   public boolean isExclusive() {
      return true;
   }

   public boolean isItemCount() {
      return this.bIsItemCount;
   }

   public boolean isDestroy() {
      return this.itemApplyMode == ItemApplyMode.Destroy;
   }

   public boolean isKeep() {
      return this.itemApplyMode == ItemApplyMode.Keep;
   }

   public boolean isTool() {
      return this.hasFlag(InputFlag.ToolRight) || this.hasFlag(InputFlag.ToolLeft);
   }

   public boolean isToolLeft() {
      return this.hasFlag(InputFlag.ToolLeft);
   }

   public boolean isToolRight() {
      return this.hasFlag(InputFlag.ToolRight);
   }

   public boolean isWorn() {
      return this.hasFlag(InputFlag.IsWorn);
   }

   public boolean isNotWorn() {
      return this.hasFlag(InputFlag.IsNotWorn);
   }

   public boolean isFull() {
      return this.hasFlag(InputFlag.IsFull);
   }

   public boolean isEmpty() {
      return this.hasFlag(InputFlag.IsEmpty);
   }

   public boolean notFull() {
      return this.hasFlag(InputFlag.NotFull);
   }

   public boolean notEmpty() {
      return this.hasFlag(InputFlag.NotEmpty);
   }

   public boolean isDamaged() {
      return this.hasFlag(InputFlag.IsDamaged);
   }

   public boolean isUndamaged() {
      return this.hasFlag(InputFlag.IsUndamaged);
   }

   public boolean allowFrozenItem() {
      return this.hasFlag(InputFlag.AllowFrozenItem);
   }

   public boolean allowRottenItem() {
      return this.hasFlag(InputFlag.AllowRottenItem);
   }

   public boolean allowDestroyedItem() {
      return this.hasFlag(InputFlag.AllowDestroyedItem);
   }

   public boolean isEmptyContainer() {
      return this.hasFlag(InputFlag.IsEmptyContainer);
   }

   public boolean isWholeFoodItem() {
      return this.hasFlag(InputFlag.IsWholeFoodItem);
   }

   public boolean isUncookedFoodItem() {
      return this.hasFlag(InputFlag.IsUncookedFoodItem);
   }

   public boolean isCookedFoodItem() {
      return this.hasFlag(InputFlag.IsCookedFoodItem);
   }

   public boolean isHeadPart() {
      return this.hasFlag(InputFlag.IsHeadPart);
   }

   public boolean isSharpenable() {
      return this.hasFlag(InputFlag.IsSharpenable);
   }

   public boolean dontPutBack() {
      return this.hasFlag(InputFlag.DontPutBack);
   }

   public boolean inheritColor() {
      return this.hasFlag(InputFlag.InheritColor);
   }

   public boolean inheritCondition() {
      return this.hasFlag(InputFlag.InheritCondition);
   }

   public boolean inheritHeadCondition() {
      return this.hasFlag(InputFlag.InheritHeadCondition);
   }

   public boolean inheritSharpness() {
      return this.hasFlag(InputFlag.InheritSharpness);
   }

   public boolean inheritUses() {
      return this.hasFlag(InputFlag.InheritUses);
   }

   public boolean isNotDull() {
      return this.hasFlag(InputFlag.IsNotDull);
   }

   public boolean mayDegrade() {
      return this.hasFlag(InputFlag.MayDegrade);
   }

   public boolean mayDegradeLight() {
      return this.hasFlag(InputFlag.MayDegradeLight);
   }

   public boolean mayDegradeHeavy() {
      return this.hasFlag(InputFlag.MayDegradeHeavy);
   }

   public boolean sharpnessCheck() {
      return this.hasFlag(InputFlag.SharpnessCheck);
   }

   /** @deprecated */
   @Deprecated
   public int getShapedIndex() {
      return this.shapedIndex;
   }

   public ItemApplyMode getItemApplyMode() {
      return this.itemApplyMode;
   }

   public FluidMatchMode getFluidMatchMode() {
      return this.fluidMatchMode;
   }

   public boolean isFluidExact() {
      return this.typeCheck(ResourceType.Fluid) && this.fluidMatchMode == FluidMatchMode.Exact;
   }

   public boolean isFluidPrimary() {
      return this.fluidMatchMode == FluidMatchMode.Primary;
   }

   public boolean isFluidAnything() {
      return this.fluidMatchMode == FluidMatchMode.Anything;
   }

   public int getIntAmount() {
      return (int)this.amount;
   }

   public float getAmount() {
      return this.amount;
   }

   public int getIntAmount(int var1) {
      return (int)this.getAmount(var1);
   }

   public float getAmount(int var1) {
      return var1 < this.amounts.size() ? (Float)this.amounts.get(var1) : this.amount;
   }

   public int getIntAmount(String var1) {
      return (int)this.getAmount(var1);
   }

   public float getAmount(String var1) {
      for(int var2 = 0; var2 < this.items.size(); ++var2) {
         String var3 = (String)this.items.get(var2);
         if (var1.equalsIgnoreCase(var3)) {
            return this.getAmount(var2);
         }
      }

      return 1.0F;
   }

   public float getRelativeScale(String var1) {
      float var2 = this.getAmount(var1);
      return this.amount != 0.0F && var2 != 0.0F ? var2 / this.amount : 1.0F;
   }

   public boolean isProp1() {
      return this.flags.contains(InputFlag.Prop1);
   }

   public boolean isProp2() {
      return this.flags.contains(InputFlag.Prop2);
   }

   public boolean isApplyOnTick() {
      return this.applyOnTick;
   }

   public boolean isAcceptsAnyItem() {
      return this.acceptsAnyItem;
   }

   public boolean isAcceptsAnyFluid() {
      return this.acceptsAnyFluid;
   }

   public boolean isAcceptsAnyEnergy() {
      return this.acceptsAnyEnergy;
   }

   public boolean isHandcraftOnly() {
      return this.flags.contains(InputFlag.HandcraftOnly);
   }

   public boolean isAutomationOnly() {
      return this.flags.contains(InputFlag.AutomationOnly);
   }

   /** @deprecated */
   @Deprecated
   public boolean isReplace() {
      return false;
   }

   /** @deprecated */
   @Deprecated
   public OutputScript getReplaceOutputScript() {
      return null;
   }

   public boolean containsItem(Item var1) {
      return var1 != null && (this.acceptsAnyItem || this.itemScriptCache.contains(var1));
   }

   public boolean containsFluid(Fluid var1) {
      return var1 != null && (this.acceptsAnyFluid || this.fluids.contains(var1));
   }

   public boolean containsEnergy(Energy var1) {
      return var1 != null && (this.acceptsAnyEnergy || this.energies.contains(var1));
   }

   public boolean isFluidMatch(FluidContainer var1) {
      if (this.type == ResourceType.Fluid && var1 != null) {
         if (var1.isEmpty()) {
            return false;
         } else {
            boolean var2;
            if (this.isFluidExact()) {
               var2 = !var1.isMixture() && this.containsFluid(var1.getPrimaryFluid());
            } else if (this.isFluidPrimary()) {
               var2 = this.containsFluid(var1.getPrimaryFluid());
            } else {
               var2 = true;
            }

            return var2;
         }
      } else {
         return false;
      }
   }

   public boolean isEnergyMatch(DrainableComboItem var1) {
      return var1 != null && var1.isEnergy() && this.isEnergyMatch(var1.getEnergy());
   }

   public boolean isEnergyMatch(Energy var1) {
      return this.type != ResourceType.Energy ? false : this.containsEnergy(var1);
   }

   protected static InputScript LoadBlock(CraftRecipe var0, ScriptParser.Block var1) throws Exception {
      InputScript var2 = null;
      Iterator var3 = var1.values.iterator();

      ScriptParser.Value var4;
      String var5;
      while(var3.hasNext()) {
         var4 = (ScriptParser.Value)var3.next();
         if (!StringUtils.isNullOrWhitespace(var4.string) && !var4.string.contains("=") && StringUtils.containsWhitespace(var4.string)) {
            String var10001 = var4.string;
            DebugLog.General.warn("Cannot load: " + var10001 + ", recipe:" + var0.getScriptObjectFullType());
            var5 = var4.string.trim();
            var2 = Load(var0, var5);
         }
      }

      if (var2 == null) {
         DebugLog.General.warn("Cannot load input block. " + var0.getScriptObjectFullType());
      }

      var3 = var1.values.iterator();

      while(var3.hasNext()) {
         var4 = (ScriptParser.Value)var3.next();
         var5 = var4.getKey().trim();
         String var6 = var4.getValue().trim();
         if (!var5.isEmpty() && !var6.isEmpty() && var5.equalsIgnoreCase("something")) {
         }
      }

      return var2;
   }

   protected static InputScript Load(CraftRecipe var0, String var1) throws Exception {
      return Load(var0, var1, false);
   }

   protected static InputScript Load(CraftRecipe var0, String var1, boolean var2) throws Exception {
      if (StringUtils.isNullOrWhitespace(var1)) {
         return null;
      } else {
         String[] var3 = var1.trim().split("\\s+");
         StringUtils.trimArray(var3);
         String var4 = var0.getScriptObjectFullType();
         boolean var5 = false;
         ResourceType var6 = ResourceType.Any;
         if (var3[0].equalsIgnoreCase("item")) {
            var6 = ResourceType.Item;
         } else if (var2 && var3[0].equalsIgnoreCase("uses")) {
            var6 = ResourceType.Item;
            var5 = true;
         } else if (var3[0].equalsIgnoreCase("fluid")) {
            var6 = ResourceType.Fluid;
         } else {
            if (!var3[0].equalsIgnoreCase("energy")) {
               throw new Exception("unknown type in craftrecipe: " + var3[0]);
            }

            var6 = ResourceType.Energy;
         }

         InputScript var7 = new InputScript(var0, var6);
         var7.originalLine = var1.trim();
         float var8 = PZMath.max(0.0F, Float.parseFloat(var3[1]));
         var7.amount = PZMath.max(0.0F, Float.parseFloat(var3[1]));
         if (var2) {
            if (var6 == ResourceType.Item) {
               var7.acceptsAnyItem = true;
            }

            if (var6 == ResourceType.Fluid) {
               var7.acceptsAnyItem = true;
            }

            if (var6 == ResourceType.Energy) {
               var7.acceptsAnyItem = true;
            }
         }

         for(int var9 = 2; var9 < var3.length; ++var9) {
            String var10 = var3[var9];
            String[] var11;
            int var12;
            String var13;
            if (var10.startsWith("[")) {
               var10 = var10.substring(var10.indexOf("[") + 1, var10.indexOf("]"));
               var11 = var10.split(";");

               for(var12 = 0; var12 < var11.length; ++var12) {
                  var13 = var11[var12];
                  if (var6 == ResourceType.Item) {
                     if ("*".equals(var13)) {
                        var7.acceptsAnyItem = true;
                     } else if (var13.contains(":")) {
                        String[] var16 = var13.split(":");
                        var7.loadedItems.add(var16[1]);
                        var7.amounts.add(PZMath.max(0.0F, Float.parseFloat(var16[0])));
                     } else {
                        var7.loadedItems.add(var13);
                        var7.amounts.add(var8);
                     }
                  } else if (var6 == ResourceType.Fluid) {
                     if ("*".equals(var13)) {
                        var7.acceptsAnyFluid = true;
                     } else {
                        var7.loadedFluids.add(var13);
                        var7.amounts.add(var8);
                     }
                  } else if (var6 == ResourceType.Energy) {
                     if ("*".equals(var13)) {
                        var7.acceptsAnyEnergy = true;
                     } else {
                        var7.loadedEnergies.add(var13);
                        var7.amounts.add(var8);
                     }
                  }
               }
            } else if (var10.startsWith("shapedIndex:")) {
               var10 = var10.substring(var10.indexOf(":") + 1, var10.length());
               var7.shapedIndex = Integer.parseInt(var10);
            } else {
               if (var10.startsWith("apply:")) {
                  var10 = var10.substring(var10.indexOf(":") + 1, var10.length());
                  throw new Exception("OnTick currently disabled for inputs.");
               }

               if (var10.startsWith("mode:")) {
                  var10 = var10.substring(var10.indexOf(":") + 1, var10.length());
                  switch (var6) {
                     case Item:
                        if (!var10.equalsIgnoreCase("use")) {
                           if (var10.equalsIgnoreCase("keep")) {
                              var7.itemApplyMode = ItemApplyMode.Keep;
                           } else if (var10.equalsIgnoreCase("destroy")) {
                              var7.itemApplyMode = ItemApplyMode.Destroy;
                           } else if (var10.equalsIgnoreCase("useprop1")) {
                              DebugLog.General.error("Deprecated parameter in inputscript! for recipe " + var4);
                           } else if (var10.equalsIgnoreCase("useprop2")) {
                              DebugLog.General.error("Deprecated parameter in inputscript! for recipe " + var4);
                           } else if (var10.equalsIgnoreCase("keepprop1")) {
                              DebugLog.General.error("Deprecated parameter in inputscript! for recipe " + var4);
                           } else if (var10.equalsIgnoreCase("keepprop2")) {
                              DebugLog.General.error("Deprecated parameter in inputscript! for recipe " + var4);
                           } else if (var10.equalsIgnoreCase("prop1")) {
                              DebugLog.General.error("Deprecated parameter in inputscript! for recipe " + var4);
                           } else {
                              if (!var10.equalsIgnoreCase("prop2")) {
                                 throw new Exception("Invalid item mode Error - " + var10);
                              }

                              DebugLog.General.error("Deprecated parameter in inputscript! for recipe " + var4);
                           }
                        }
                        break;
                     case Fluid:
                        if (var10.equalsIgnoreCase("exact")) {
                           var7.fluidMatchMode = FluidMatchMode.Exact;
                        } else if (var10.equalsIgnoreCase("primary")) {
                           var7.fluidMatchMode = FluidMatchMode.Primary;
                        } else {
                           if (!var10.equalsIgnoreCase("anything")) {
                              throw new Exception("Invalid fluid mode Error");
                           }

                           var7.fluidMatchMode = FluidMatchMode.Anything;
                        }
                        break;
                     case Energy:
                        if (!var10.equalsIgnoreCase("keep")) {
                           throw new Exception("Invalid energy mode Error");
                        }

                        var7.itemApplyMode = ItemApplyMode.Keep;
                        break;
                     default:
                        DebugLog.General.warn("Cannot set mode for type = " + var6 + " for recipe " + var4);
                        if (Core.bDebug) {
                           throw new Exception("Mode Error");
                        }
                  }
               } else if (var10.startsWith("tags")) {
                  if (var6 != ResourceType.Item) {
                     throw new Exception("cannot set tags on non-item: " + var10);
                  }

                  var10 = var10.substring(var10.indexOf("[") + 1, var10.indexOf("]"));
                  var11 = var10.split(";");

                  for(var12 = 0; var12 < var11.length; ++var12) {
                     var13 = var11[var12];
                     var7.tags.add(var13);
                  }
               } else if (var10.startsWith("flags")) {
                  var10 = var10.substring(var10.indexOf("[") + 1, var10.indexOf("]"));
                  var11 = var10.split(";");

                  for(var12 = 0; var12 < var11.length; ++var12) {
                     var13 = var11[var12];
                     InputFlag var15 = InputFlag.valueOf(var13);
                     var7.flags.add(var15);
                  }
               } else if (var10.startsWith("mappers")) {
                  var10 = var10.substring(var10.indexOf("[") + 1, var10.indexOf("]"));
                  var11 = var10.split(";");

                  for(var12 = 0; var12 < var11.length; ++var12) {
                     var13 = var11[var12];
                     OutputMapper var14 = var0.getOrCreateOutputMapper(var13);
                     var14.registerInputScript(var7);
                  }
               } else if (var10.startsWith("exclusive")) {
                  var7.bIsExclusive = true;
               } else {
                  if (!var10.startsWith("itemcount")) {
                     throw new Exception("unknown recipe param: " + var10);
                  }

                  var7.bIsItemCount = true;
               }
            }
         }

         if (var7.acceptsAnyItem) {
            var7.loadedItems.clear();
            var7.tags.clear();
         }

         if (var7.acceptsAnyFluid) {
            var7.loadedFluids.clear();
         }

         if (var7.acceptsAnyEnergy) {
            var7.loadedEnergies.clear();
         }

         return var7;
      }
   }

   public void OnScriptsLoaded(ScriptLoadMode var1) throws Exception {
      if (this.isToolLeft() && this.isToolRight()) {
         throw new Exception("ToolLeft and ToolRight both set. line: " + this.originalLine);
      } else if (this.isProp1() && this.isProp2()) {
         throw new Exception("Prop1 and Prop2 both set. line: " + this.originalLine);
      }
   }

   protected void OnPostWorldDictionaryInit() throws Exception {
      if (this.createToItemScript != null && this.consumeFromItemScript != null && this.createToItemScript.getResourceType() == this.consumeFromItemScript.getResourceType()) {
         throw new Exception("Input line cannot have a '-' and '+' line of the same ResourceType. line: " + this.originalLine);
      } else {
         if (this.createToItemScript != null) {
            this.createToItemScript.OnPostWorldDictionaryInit();
            if (this.getIntAmount() != 1) {
               throw new Exception("Lines prior to a '+' line should have 1 item amount. line: " + this.originalLine);
            }

            if (this.type != ResourceType.Item) {
               throw new Exception("Lines prior to a '+' line should be of resource type Item. line: " + this.originalLine);
            }

            if (this.applyOnTick) {
               throw new Exception("Lines prior to a '+' line should not be apply on tick. line: " + this.originalLine);
            }

            this.itemApplyMode = ItemApplyMode.Keep;
         }

         if (this.consumeFromItemScript != null) {
            this.consumeFromItemScript.OnPostWorldDictionaryInit();
            if (this.getIntAmount() != 1) {
               throw new Exception("Lines prior to a '-' line should have 1 item amount. line: " + this.originalLine);
            }

            if (this.type != ResourceType.Item) {
               throw new Exception("Lines prior to a '-' line should be of resource type Item. line: " + this.originalLine);
            }

            if (this.applyOnTick) {
               throw new Exception("Lines prior to a '-' line should not be apply on tick. line: " + this.originalLine);
            }

            this.itemApplyMode = ItemApplyMode.Keep;
         }

         Iterator var1;
         String var2;
         if (this.acceptsAnyItem) {
            this.itemScriptCache.clear();
            Item var6;
            ArrayList var7;
            int var9;
            if (!this.flags.contains(InputFlag.ItemIsFluid) && (this.consumeFromItemScript == null || this.consumeFromItemScript.getResourceType() != ResourceType.Fluid) && (this.createToItemScript == null || this.createToItemScript.getResourceType() != ResourceType.Fluid)) {
               if (!this.flags.contains(InputFlag.ItemIsEnergy) && (this.consumeFromItemScript == null || this.consumeFromItemScript.getResourceType() != ResourceType.Energy) && (this.createToItemScript == null || this.createToItemScript.getResourceType() != ResourceType.Energy)) {
                  this.itemScriptCache = PZUnmodifiableList.wrap(ScriptManager.instance.getAllItems());
               } else {
                  var7 = ScriptManager.instance.getAllItems();

                  for(var9 = 0; var9 < var7.size(); ++var9) {
                     var6 = (Item)var7.get(var9);
                     if (var6.getType() == Item.Type.Drainable) {
                        this.itemScriptCache.add(var6);
                     }
                  }
               }
            } else {
               var7 = ScriptManager.instance.getAllItems();

               for(var9 = 0; var9 < var7.size(); ++var9) {
                  var6 = (Item)var7.get(var9);
                  if (var6.containsComponent(ComponentType.FluidContainer)) {
                     this.itemScriptCache.add(var6);
                  }
               }
            }
         } else {
            var1 = this.loadedItems.iterator();

            while(var1.hasNext()) {
               var2 = (String)var1.next();
               Item var3 = ScriptManager.instance.getItem(var2);
               if (var3 == null) {
                  throw new Exception("Item not found: " + var2 + ". line: " + this.originalLine);
               }

               if (!this.items.contains(var2)) {
                  this.items.add(var2);
                  this.itemScriptCache.add(var3);
               }
            }

            var1 = this.tags.iterator();

            while(var1.hasNext()) {
               var2 = (String)var1.next();
               ArrayList var8 = ItemTags.getItemsForTag(var2);
               if (var8 == null || var8.size() <= 0) {
                  throw new Exception("Tag has no items: " + var2 + ". line: " + this.originalLine);
               }

               Iterator var4 = var8.iterator();

               while(var4.hasNext()) {
                  Item var5 = (Item)var4.next();
                  if (!this.items.contains(var5.getFullName())) {
                     this.items.add(var5.getFullName());
                     this.itemScriptCache.add(var5);
                  }
               }
            }
         }

         var1 = this.loadedFluids.iterator();

         while(var1.hasNext()) {
            var2 = (String)var1.next();
            Fluid var10 = Fluid.Get(var2);
            if (var10 == null) {
               throw new Exception("Fluid not found: " + var2 + ". line: " + this.originalLine);
            }

            if (!this.fluids.contains(var10)) {
               this.fluids.add(var10);
            }
         }

         var1 = this.loadedEnergies.iterator();

         while(var1.hasNext()) {
            var2 = (String)var1.next();
            Energy var11 = Energy.Get(var2);
            if (var11 == null) {
               throw new Exception("Energy not found: " + var2 + ". line: " + this.originalLine);
            }

            if (!this.energies.contains(var11)) {
               this.energies.add(var11);
            }
         }

         if (!this.isValid()) {
            throw new Exception("Invalid input. line: " + this.originalLine);
         }
      }
   }

   public boolean canUseItem(InventoryItem var1) {
      return CraftRecipeManager.isItemValidForInputScript(this, var1);
   }

   public boolean canUseItem(String var1) {
      if (this.getResourceType() != ResourceType.Item) {
         return false;
      } else {
         List var2 = this.getPossibleInputItems();

         for(int var3 = 0; var3 < var2.size(); ++var3) {
            Item var4 = (Item)this.getPossibleInputItems().get(var3);
            if (Objects.equals(var4.getName(), var1) || Objects.equals(var4.getFullName(), var1)) {
               return true;
            }
         }

         return false;
      }
   }
}
