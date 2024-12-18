package zombie.scripting.entity.components.crafting;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.components.crafting.FluidMatchMode;
import zombie.entity.components.crafting.ItemApplyMode;
import zombie.entity.components.crafting.OutputFlag;
import zombie.entity.components.crafting.recipe.CraftRecipeData;
import zombie.entity.components.crafting.recipe.OutputMapper;
import zombie.entity.components.fluids.Fluid;
import zombie.entity.components.fluids.FluidContainer;
import zombie.entity.components.resources.ResourceType;
import zombie.entity.energy.Energy;
import zombie.inventory.types.DrainableComboItem;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptParser;
import zombie.scripting.objects.Item;
import zombie.util.StringUtils;

@DebugClassFields
public class OutputScript extends CraftRecipe.IOScript {
   private static ArrayList<Item> _emptyItems = new ArrayList();
   private final ResourceType type;
   private String loadedFluid;
   private String loadedEnergy;
   private Fluid fluid;
   private Energy energy;
   private float amount;
   private float chance = 1.0F;
   private boolean applyOnTick = false;
   /** @deprecated */
   @Deprecated
   private int shapedIndex = -1;
   private ItemApplyMode itemApplyMode;
   private FluidMatchMode fluidMatchMode;
   private String originalLine;
   protected OutputScript createToItemScript;
   private final EnumSet<OutputFlag> flags;
   private OutputMapper outputMapper;
   private final ArrayList<Fluid> possibleFluids;
   private final ArrayList<Energy> possiblyEnergies;

   private OutputScript(CraftRecipe var1, ResourceType var2) {
      super(var1);
      this.itemApplyMode = ItemApplyMode.Normal;
      this.fluidMatchMode = FluidMatchMode.Exact;
      this.originalLine = "";
      this.createToItemScript = null;
      this.flags = EnumSet.noneOf(OutputFlag.class);
      this.possibleFluids = new ArrayList();
      this.possiblyEnergies = new ArrayList();
      this.type = var2;
   }

   private boolean typeCheck(ResourceType var1) {
      return this.type == var1;
   }

   protected boolean isValid() {
      if (this.type == ResourceType.Item) {
         return this.outputMapper.getResultItems().size() > 0;
      } else if (this.type == ResourceType.Fluid) {
         return this.fluid != null;
      } else if (this.type == ResourceType.Energy) {
         return this.energy != null;
      } else {
         return false;
      }
   }

   public boolean hasCreateToItem() {
      return this.createToItemScript != null;
   }

   public OutputScript getCreateToItemScript() {
      return this.createToItemScript;
   }

   public boolean hasFlag(OutputFlag var1) {
      return this.flags.contains(var1);
   }

   /** @deprecated */
   @Deprecated
   public boolean isReplaceInput() {
      return false;
   }

   public String getOriginalLine() {
      return this.originalLine;
   }

   public ResourceType getResourceType() {
      return this.type;
   }

   public float getChance() {
      return this.chance;
   }

   public int getIntAmount() {
      return (int)this.amount;
   }

   public float getAmount() {
      return this.amount;
   }

   /** @deprecated */
   @Deprecated
   public int getShapedIndex() {
      return this.shapedIndex;
   }

   public boolean isApplyOnTick() {
      return this.applyOnTick;
   }

   public boolean isHandcraftOnly() {
      return this.flags.contains(OutputFlag.HandcraftOnly);
   }

   public boolean isAutomationOnly() {
      return this.flags.contains(OutputFlag.AutomationOnly);
   }

   public ArrayList<Item> getPossibleResultItems() {
      if (this.outputMapper == null) {
         DebugLog.General.warn("This output does not have items! returning empty list.");
         return _emptyItems;
      } else {
         return this.outputMapper.getResultItems();
      }
   }

   public ArrayList<Fluid> getPossibleResultFluids() {
      if (this.type == ResourceType.Fluid && this.possibleFluids.size() == 0 && this.fluid != null) {
         this.possibleFluids.add(this.fluid);
      }

      return this.possibleFluids;
   }

   public ArrayList<Energy> getPossibleResultEnergies() {
      if (this.type == ResourceType.Energy && this.possiblyEnergies.size() == 0 && this.energy != null) {
         this.possiblyEnergies.add(this.energy);
      }

      return this.possiblyEnergies;
   }

   public OutputMapper getOutputMapper() {
      return this.outputMapper;
   }

   public Item getItem(CraftRecipeData var1) {
      if (this.outputMapper == null) {
         DebugLog.General.warn("This output does not have items! returning null.");
         return null;
      } else {
         return this.outputMapper.getOutputItem(var1);
      }
   }

   public Fluid getFluid() {
      return this.fluid;
   }

   public Energy getEnergy() {
      return this.energy;
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
      return this.typeCheck(ResourceType.Fluid) && this.fluidMatchMode == FluidMatchMode.Primary;
   }

   public boolean isFluidAnything() {
      return this.typeCheck(ResourceType.Fluid) && this.fluidMatchMode == FluidMatchMode.Anything;
   }

   /** @deprecated */
   @Deprecated
   public boolean isCreateUses() {
      return this.typeCheck(ResourceType.Item);
   }

   public boolean containsItem(Item var1) {
      return true;
   }

   public boolean containsFluid(Fluid var1) {
      return this.typeCheck(ResourceType.Fluid) && this.fluid != null && this.fluid.equals(var1);
   }

   public boolean containsEnergy(Energy var1) {
      return this.typeCheck(ResourceType.Energy) && this.energy != null && this.energy.equals(var1);
   }

   public boolean isFluidMatch(FluidContainer var1) {
      if (this.typeCheck(ResourceType.Fluid) && var1 != null) {
         if (var1.isEmpty()) {
            return true;
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
      return this.typeCheck(ResourceType.Energy) && var1 != null && var1.isEnergy() && this.isEnergyMatch(var1.getEnergy());
   }

   public boolean isEnergyMatch(Energy var1) {
      return !this.typeCheck(ResourceType.Energy) ? false : this.containsEnergy(var1);
   }

   protected static OutputScript LoadBlock(CraftRecipe var0, ScriptParser.Block var1) throws Exception {
      OutputScript var2 = null;
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
         DebugLog.General.warn("Cannot load output block. " + var0.getScriptObjectFullType());
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

   protected static OutputScript Load(CraftRecipe var0, String var1) throws Exception {
      return Load(var0, var1, false);
   }

   protected static OutputScript Load(CraftRecipe var0, String var1, boolean var2) throws Exception {
      if (StringUtils.isNullOrWhitespace(var1)) {
         return null;
      } else {
         String[] var3 = var1.trim().split("\\s+");
         StringUtils.trimArray(var3);
         boolean var4 = false;
         ResourceType var5 = ResourceType.Any;
         if (var3[0].equalsIgnoreCase("item")) {
            var5 = ResourceType.Item;
         } else if (var3[0].equalsIgnoreCase("fluid")) {
            var5 = ResourceType.Fluid;
         } else {
            if (!var3[0].equalsIgnoreCase("energy")) {
               throw new Exception("unknown type in craftrecipe: " + var3[0]);
            }

            var5 = ResourceType.Energy;
         }

         OutputScript var6 = new OutputScript(var0, var5);
         var6.originalLine = var1.trim();
         var6.amount = PZMath.max(0.0F, Float.parseFloat(var3[1]));
         String var7 = var3[2];
         if (var5 == ResourceType.Item) {
            if (!var2) {
               if (var7.startsWith("mapper:")) {
                  var7 = var7.substring(var7.indexOf(":") + 1, var7.length());
                  var6.outputMapper = var0.getOutputMapper(var7);
                  if (var6.outputMapper == null) {
                     throw new Exception("Could not find output mapper: " + var7);
                  }
               } else {
                  var6.outputMapper = new OutputMapper(var7);
                  var6.outputMapper.setDefaultOutputEntree(var7);
               }
            } else {
               if (!"Uses".equalsIgnoreCase(var7) && Core.bDebug) {
                  throw new Exception("Parameter with index=2 should be 'Uses'.");
               }

               var4 = true;
            }
         } else if (var5 == ResourceType.Fluid) {
            var6.loadedFluid = var7;
         } else if (var5 == ResourceType.Energy) {
            var6.loadedEnergy = var7;
         }

         for(int var8 = 3; var8 < var3.length; ++var8) {
            String var9 = var3[var8];
            if (var9.startsWith("chance:")) {
               var9 = var9.substring(var9.indexOf(":") + 1, var9.length());
               var6.chance = PZMath.clamp(Float.parseFloat(var9), 0.0F, 1.0F);
            } else if (var9.startsWith("shapedIndex:")) {
               var9 = var9.substring(var9.indexOf(":") + 1, var9.length());
               var6.shapedIndex = Integer.parseInt(var9);
            } else if (var9.startsWith("apply:")) {
               if (var2) {
                  throw new Exception("Cannot apply 'onTick' on 'itemCreate' ('+' lines).");
               }

               var9 = var9.substring(var9.indexOf(":") + 1, var9.length());
               if (!var9.equalsIgnoreCase("onTick")) {
                  throw new Exception("Apply Error");
               }

               if (var5 == ResourceType.Item) {
                  throw new Exception("Cannot apply 'onTick' on item.");
               }

               if (var5 == ResourceType.Fluid) {
                  throw new Exception("Cannot apply 'onTick' on fluid.");
               }

               var6.applyOnTick = true;
            } else if (var9.startsWith("mode:")) {
               var9 = var9.substring(var9.indexOf(":") + 1, var9.length());
               switch (var5) {
                  case Item:
                     break;
                  case Fluid:
                     if (var9.equalsIgnoreCase("exact")) {
                        var6.fluidMatchMode = FluidMatchMode.Exact;
                     } else if (var9.equalsIgnoreCase("primary")) {
                        var6.fluidMatchMode = FluidMatchMode.Primary;
                     } else {
                        if (!var9.equalsIgnoreCase("anything")) {
                           throw new Exception("Invalid fluid mode Error");
                        }

                        var6.fluidMatchMode = FluidMatchMode.Anything;
                     }
                     break;
                  default:
                     DebugLog.General.warn("Cannot set mode for type = " + var5);
                     if (Core.bDebug) {
                        throw new Exception("Mode Error");
                     }
               }
            } else {
               if (!var9.startsWith("flags")) {
                  throw new Exception("unknown recipe param: " + var9);
               }

               var9 = var9.substring(var9.indexOf("[") + 1, var9.indexOf("]"));
               String[] var10 = var9.split(";");

               for(int var11 = 0; var11 < var10.length; ++var11) {
                  String var12 = var10[var11];
                  OutputFlag var13 = OutputFlag.valueOf(var12);
                  var6.flags.add(var13);
               }
            }
         }

         return var6;
      }
   }

   public void OnScriptsLoaded(ScriptLoadMode var1) throws Exception {
   }

   protected void OnPostWorldDictionaryInit() throws Exception {
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

         this.itemApplyMode = ItemApplyMode.Normal;
      }

      if (this.type == ResourceType.Item) {
         if (this.outputMapper == null) {
            throw new Exception("No outputMapper set. line: " + this.originalLine);
         }

         this.outputMapper.OnPostWorldDictionaryInit();
      } else if (this.type == ResourceType.Fluid) {
         Fluid var1 = Fluid.Get(this.loadedFluid);
         if (var1 == null) {
            throw new Exception("Fluid not found: " + this.loadedFluid + ", line: " + this.originalLine);
         }

         this.fluid = var1;
      } else if (this.type == ResourceType.Energy) {
         Energy var2 = Energy.Get(this.loadedEnergy);
         if (var2 == null) {
            throw new Exception("Energy not found: " + this.loadedEnergy + ", line: " + this.originalLine);
         }

         this.energy = var2;
      }

      if (!this.isValid()) {
         throw new Exception("Invalid output. line: " + this.originalLine);
      }
   }
}
