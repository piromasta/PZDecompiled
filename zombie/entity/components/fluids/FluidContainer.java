package zombie.entity.components.fluids;

import com.google.common.collect.UnmodifiableIterator;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import zombie.GameWindow;
import zombie.SandboxOptions;
import zombie.characters.IsoGameCharacter;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.math.PZMath;
import zombie.core.raknet.UdpConnection;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.Component;
import zombie.entity.ComponentType;
import zombie.entity.network.EntityPacketType;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.scripting.entity.ComponentScript;
import zombie.scripting.entity.components.fluids.FluidContainerScript;
import zombie.ui.ObjectTooltip;
import zombie.util.StringUtils;
import zombie.util.io.BitHeader;
import zombie.util.io.BitHeaderRead;
import zombie.util.io.BitHeaderWrite;

@DebugClassFields
public class FluidContainer extends Component {
   public static final int MAX_FLUIDS = 8;
   public static final String DEF_CONTAINER_NAME = "FluidContainer";
   private static final Color colorDef;
   private float capacity = 1.0F;
   private FluidFilter whitelist = null;
   private FluidFilter blacklist = null;
   private float temperature = 22.0F;
   private final ArrayList<FluidInstance> fluids = new ArrayList(8);
   private final SealedFluidProperties propertiesCache = new SealedFluidProperties();
   private float amountCache = 0.0F;
   private final Color color;
   private String containerName;
   private String translatedContainerName;
   private String nameCache;
   private String customDrinkSound;
   private boolean cacheInvalidated;
   private boolean inputLocked;
   private boolean canPlayerEmpty;
   private boolean hiddenAmount;
   private float rainCatcher;
   private static final ArrayList<FluidInstance> tempFluidUI;
   private static final DecimalFormat df;

   public static FluidContainer CreateContainer() {
      return (FluidContainer)ComponentType.FluidContainer.CreateComponent();
   }

   public static void DisposeContainer(FluidContainer var0) {
      ComponentType.ReleaseComponent(var0);
   }

   private FluidContainer() {
      super(ComponentType.FluidContainer);
      this.color = (new Color()).set(colorDef);
      this.containerName = "FluidContainer";
      this.translatedContainerName = null;
      this.nameCache = null;
      this.customDrinkSound = null;
      this.cacheInvalidated = false;
      this.inputLocked = false;
      this.canPlayerEmpty = true;
      this.hiddenAmount = false;
      this.rainCatcher = 0.0F;
   }

   protected void readFromScript(ComponentScript var1) {
      super.readFromScript(var1);
      FluidContainerScript var2 = (FluidContainerScript)var1;
      this.whitelist = var2.getWhitelistCopy();
      this.blacklist = var2.getBlacklistCopy();
      this.containerName = var2.getContainerName();
      this.customDrinkSound = var2.getCustomDrinkSound();
      this.rainCatcher = var2.getRainCatcher();
      this.inputLocked = false;
      this.canPlayerEmpty = true;
      this.setCapacity(var2.getCapacity());
      if (var2.getInitialFluids() != null && !var2.getInitialFluids().isEmpty() && var2.getInitialAmount() > 0.0F) {
         float var4 = var2.getInitialAmount();
         if (var2.getInitialFluids().size() == 1) {
            this.addInitialFluid(var4, (FluidContainerScript.FluidScript)var2.getInitialFluids().get(0), var2.getName());
         } else {
            int var5;
            if (var2.isInitialFluidsIsRandom()) {
               var5 = Rand.Next(var2.getInitialFluids().size());
               this.addInitialFluid(var4, (FluidContainerScript.FluidScript)var2.getInitialFluids().get(var5), var2.getName());
            } else {
               for(var5 = 0; var5 < var2.getInitialFluids().size(); ++var5) {
                  FluidContainerScript.FluidScript var3 = (FluidContainerScript.FluidScript)var2.getInitialFluids().get(var5);
                  this.addInitialFluid(var4, var3, var2.getName());
               }
            }
         }
      }

      this.inputLocked = var2.getInputLocked();
      this.canPlayerEmpty = var2.getCanEmpty();
      this.hiddenAmount = var2.isHiddenAmount();
   }

   private void addInitialFluid(float var1, FluidContainerScript.FluidScript var2, String var3) {
      Fluid var4 = var2.getFluid();
      if (var4 != null) {
         float var5 = var1 * var2.getPercentage();
         FluidInstance var6 = FluidInstance.Alloc(var4);
         if (var2.getCustomColor() != null) {
            var6.setColor(var2.getCustomColor());
         }

         this.addFluid(var6, var5);
         FluidInstance.Release(var6);
      } else {
         String var10000 = var2.getFluidType();
         DebugLog.log("FluidContainer -> initial Fluid '" + var10000 + "' not found! [" + var3 + "]");
      }

   }

   protected void reset() {
      super.reset();
      this.Empty();
      this.capacity = 1.0F;
      this.whitelist = null;
      this.blacklist = null;
      this.temperature = 22.0F;
      this.propertiesCache.clear();
      this.amountCache = 0.0F;
      this.color.set(colorDef);
      this.containerName = "FluidContainer";
      this.translatedContainerName = null;
      this.nameCache = null;
      this.cacheInvalidated = false;
      this.inputLocked = false;
      this.canPlayerEmpty = true;
      this.hiddenAmount = false;
      this.rainCatcher = 0.0F;
      this.customDrinkSound = null;
   }

   public FluidContainer copy() {
      FluidContainer var1 = CreateContainer();
      var1.capacity = this.capacity;
      Iterator var2 = this.fluids.iterator();

      while(var2.hasNext()) {
         FluidInstance var3 = (FluidInstance)var2.next();
         var1.addFluid(var3, var3.getAmount());
      }

      if (this.whitelist != null) {
         var1.whitelist = this.whitelist.copy();
      }

      if (this.blacklist != null) {
         var1.blacklist = this.blacklist.copy();
      }

      var1.containerName = this.containerName;
      var1.recalculateCaches(true);
      return var1;
   }

   public void copyFluidsFrom(FluidContainer var1) {
      FluidInstance var2;
      int var3;
      for(var3 = this.fluids.size() - 1; var3 >= 0; --var3) {
         var2 = (FluidInstance)this.fluids.get(var3);
         FluidInstance.Release(var2);
      }

      this.fluids.clear();
      this.recalculateCaches(true);

      for(var3 = 0; var3 < var1.fluids.size(); ++var3) {
         var2 = (FluidInstance)var1.fluids.get(var3);
         this.addFluid(var2, var2.getAmount());
      }

      this.recalculateCaches(true);
   }

   public String getCustomDrinkSound() {
      return this.customDrinkSound;
   }

   public void setInputLocked(boolean var1) {
      this.inputLocked = var1;
   }

   public boolean isInputLocked() {
      return this.inputLocked;
   }

   public boolean canPlayerEmpty() {
      return this.canPlayerEmpty;
   }

   public void setCanPlayerEmpty(boolean var1) {
      this.canPlayerEmpty = var1;
   }

   public float getRainCatcher() {
      return this.rainCatcher;
   }

   public boolean isHiddenAmount() {
      return this.hiddenAmount;
   }

   public void DoTooltip(ObjectTooltip var1) {
      ObjectTooltip.Layout var2 = var1.beginLayout();
      var2.setMinLabelWidth(80);
      int var3 = var1.padTop;
      this.DoTooltip(var1, var2);
      var3 = var2.render(var1.padLeft, var3, var1);
      var1.endLayout(var2);
      var3 += var1.padBottom;
      var1.setHeight((double)var3);
      if (var1.getWidth() < 150.0) {
         var1.setWidth(150.0);
      }

   }

   public void DoTooltip(ObjectTooltip var1, ObjectTooltip.Layout var2) {
      if (var2 != null) {
         IsoGameCharacter var3 = var1.getCharacter();
         ObjectTooltip.LayoutItem var4 = var2.addItem();
         var4.setLabel(Translator.getFluidText("Fluid_Amount") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
         int var10001 = (int)(this.getAmount() * 1000.0F);
         var4.setValue("" + var10001 + "/" + (int)(this.getCapacity() * 1000.0F) + " mL", 1.0F, 1.0F, 1.0F, 1.0F);
         if (!this.isEmpty()) {
            FluidInstance var5;
            float var6;
            int var7;
            FluidInstance var14;
            if (this.fluids.size() == 1) {
               var5 = (FluidInstance)this.fluids.get(0);
               var4 = var2.addItem();
               var4.setLabel(var5.getFluid().getTranslatedName() + ":", 1.0F, 1.0F, 0.8F, 1.0F);
               var6 = var5.getAmount() / this.getCapacity();
               var4.setProgress(var6, var5.getColor().r, var5.getColor().g, var5.getColor().b, 1.0F);
            } else if (this.fluids.size() == 2 && ((FluidInstance)this.fluids.get(0)).getTranslatedName().equals(((FluidInstance)this.fluids.get(1)).getTranslatedName())) {
               var5 = (FluidInstance)this.fluids.get(0);
               var4 = var2.addItem();
               var4.setLabel(var5.getFluid().getTranslatedName() + ":", 1.0F, 1.0F, 0.8F, 1.0F);
               var6 = (var5.getAmount() + ((FluidInstance)this.fluids.get(1)).getAmount()) / this.getCapacity();
               var4.setProgress(var6, var5.getColor().r, var5.getColor().g, var5.getColor().b, 1.0F);
            } else {
               var4 = var2.addItem();
               var4.setLabel(Translator.getFluidText("Fluid_Mixture") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
               float var10 = this.getAmount() / this.getCapacity();
               var4.setProgress(var10, this.getColor().r, this.getColor().g, this.getColor().b, 1.0F);
               var4 = var2.addItem();
               var4.setLabel(Translator.getFluidText("Fluid_Fluids") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
               tempFluidUI.clear();

               for(int var11 = 0; var11 < this.fluids.size(); ++var11) {
                  tempFluidUI.add((FluidInstance)this.fluids.get(var11));
               }

               tempFluidUI.sort(Comparator.comparing(FluidInstance::getTranslatedName));

               for(var7 = 0; var7 < tempFluidUI.size(); ++var7) {
                  var14 = (FluidInstance)tempFluidUI.get(var7);
                  var4 = var2.addItem();
                  var4.setLabel(var14.getFluid().getTranslatedName() + ":", 0.7F, 0.7F, 0.4F, 1.0F);
                  var10 = var14.getAmount() / this.getAmount();
                  var4.setProgress(var10, var14.getColor().r, var14.getColor().g, var14.getColor().b, 1.0F);
               }
            }

            if (Core.bDebug) {
               if (this.propertiesCache.hasProperties()) {
                  var4 = var2.addItem();
                  var4.setLabel(Translator.getFluidText("Fluid_Properties_Per") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
                  this.doToolTipProp(var2, "Thirst", this.propertiesCache.getThirstChange());
                  this.doToolTipProp(var2, "Hunger", this.propertiesCache.getHungerChange());
                  this.doToolTipProp(var2, "Stress", this.propertiesCache.getStressChange());
                  this.doToolTipProp(var2, "Unhappy", this.propertiesCache.getUnhappyChange());
                  this.doToolTipProp(var2, "Fatigue", this.propertiesCache.getFatigueChange());
                  this.doToolTipProp(var2, "Calories", this.propertiesCache.getCalories());
                  this.doToolTipProp(var2, "Carbohydrates", this.propertiesCache.getCarbohydrates());
                  this.doToolTipProp(var2, "Lipids", this.propertiesCache.getLipids());
                  this.doToolTipProp(var2, "Proteins", this.propertiesCache.getProteins());
                  this.doToolTipProp(var2, "Alcohol", this.propertiesCache.getAlcohol());
               }

               if (this.isPoisonous()) {
                  String var10000 = Translator.getFluidText("Fluid_Poison");
                  String var12 = var10000 + " " + Translator.getFluidText("Fluid_Effect") + " " + Translator.getFluidText("Fluid_Per");
                  var12 = var12 + " " + (int)(this.getAmount() * 1000.0F) + " mL";
                  var4 = var2.addItem();
                  var4.setLabel(var12 + ":", 1.0F, 1.0F, 0.8F, 1.0F);
                  PoisonEffect var15 = this.getPoisonEffect();
                  var4 = var2.addItem();
                  var4.setLabel(Translator.getFluidText("Fluid_Effect") + ":", 0.7F, 0.7F, 0.4F, 1.0F);
                  var4.setValue(Translator.getFluidText("Fluid_Poison_" + var15), 0.85F, 0.85F, 0.85F, 1.0F);
               }

               var4 = var2.addItem();
               var4.setLabel("Categories:", 1.0F, 1.0F, 0.8F, 1.0F);
               ArrayList var13 = new ArrayList();

               for(var7 = 0; var7 < this.fluids.size(); ++var7) {
                  var14 = (FluidInstance)this.fluids.get(var7);
                  UnmodifiableIterator var8 = var14.getFluid().getCategories().iterator();

                  while(var8.hasNext()) {
                     FluidCategory var9 = (FluidCategory)var8.next();
                     if (!var13.contains(var9.toString())) {
                        var13.add(var9.toString());
                     }
                  }
               }

               Collections.sort(var13);
               Iterator var16 = var13.iterator();

               while(var16.hasNext()) {
                  String var17 = (String)var16.next();
                  var4 = var2.addItem();
                  var4.setLabel(var17, 0.7F, 0.7F, 0.4F, 1.0F);
               }
            }

         }
      }
   }

   private void doToolTipProp(ObjectTooltip.Layout var1, String var2, float var3) {
      if (var3 != 0.0F && this.getAmount() != 0.0F) {
         var3 /= this.getAmount();
         ObjectTooltip.LayoutItem var4 = var1.addItem();
         var4.setLabel(Translator.getFluidText("Fluid_Prop_" + var2) + ":", 0.7F, 0.7F, 0.4F, 1.0F);
         if (var2.equals("Alcohol")) {
            var3 = (float)((int)(var3 * 100.0F));
            var4.setValue(df.format((double)var3) + "%", 0.85F, 0.85F, 0.85F, 1.0F);
         } else {
            var4.setValue(df.format((double)var3), 0.85F, 0.85F, 0.85F, 1.0F);
         }

      }
   }

   public String getContainerName() {
      return this.containerName;
   }

   public void setContainerName(String var1) {
      if (StringUtils.containsWhitespace(var1)) {
         DebugLog.General.error("Sanitizing container name '" + var1 + "', name may not contain whitespaces.");
         var1 = StringUtils.removeWhitespace(var1);
      }

      this.containerName = var1;
   }

   public String getTranslatedContainerName() {
      if (this.translatedContainerName == null) {
         this.translatedContainerName = Translator.getFluidText("Fluid_Container_" + this.containerName);
      }

      return this.translatedContainerName;
   }

   public String getUiName() {
      if (this.cacheInvalidated || this.nameCache == null) {
         this.recalculateCaches();
         String var1 = this.getTranslatedContainerName();
         if (this.fluids.size() == 0) {
            this.nameCache = Translator.getText("Fluid_HoldingNone", var1);
            if (((InventoryItem)this.getOwner()).hasTag("OmitEmptyFromName")) {
               this.nameCache = var1;
            }
         } else {
            String var10001;
            if (this.fluids.size() == 1) {
               this.nameCache = Translator.getText("Fluid_HoldingOneType", var1, ((FluidInstance)this.fluids.get(0)).getFluid().getTranslatedName());
               if (this.getPrimaryFluid() == Fluid.TaintedWater && SandboxOptions.instance.EnableTaintedWaterText.getValue()) {
                  var10001 = this.nameCache;
                  this.nameCache = var10001 + " " + Translator.getFluidText("Fluid_Tainted");
               }

               if (!this.canPlayerEmpty) {
                  var10001 = this.nameCache;
                  this.nameCache = var10001 + " " + Translator.getFluidText("Fluid_Sealed");
               }
            } else {
               FluidInstance var6;
               FluidInstance var7;
               if (this.fluids.size() == 2 && this.isPoisonous()) {
                  this.nameCache = var1 + " " + Translator.getFluidText("Fluid_Of") + " ";
                  var6 = (FluidInstance)this.fluids.get(0);
                  var7 = (FluidInstance)this.fluids.get(1);
                  if (!var6.getFluid().isCategory(FluidCategory.Poisons)) {
                     this.nameCache = Translator.getText("Fluid_HoldingOneType", var1, var6.getFluid().getTranslatedName());
                  } else {
                     this.nameCache = Translator.getText("Fluid_HoldingOneType", var1, var7.getFluid().getTranslatedName());
                  }

                  if (this.contains(Fluid.TaintedWater) && this.getPoisonEffect() != PoisonEffect.None) {
                     var10001 = this.nameCache;
                     this.nameCache = var10001 + " " + Translator.getFluidText("Fluid_Tainted");
                  }
               } else {
                  FluidInstance var4;
                  if (this.fluids.size() == 2 && !this.isPoisonous()) {
                     var6 = (FluidInstance)this.fluids.get(0);
                     var7 = (FluidInstance)this.fluids.get(1);
                     var4 = var6.getAmount() < var7.getAmount() ? var6 : var7;
                     this.nameCache = Translator.getText("Fluid_HoldingOneType", var1, var4.getFluid().getTranslatedName());
                     if (!var6.getTranslatedName().equals(var7.getTranslatedName())) {
                        FluidInstance var8 = var6.getAmount() < var7.getAmount() ? var7 : var6;
                        this.nameCache = Translator.getText("Fluid_HoldingTwoTypes", var1, var4.getFluid().getTranslatedName(), var8.getFluid().getTranslatedName());
                     }

                     if (this.contains(Fluid.TaintedWater) && this.getPoisonEffect() != PoisonEffect.None) {
                        var10001 = this.nameCache;
                        this.nameCache = var10001 + " " + Translator.getFluidText("Fluid_Tainted");
                     }
                  } else {
                     boolean var2 = true;
                     int var3 = 0;

                     for(int var5 = 0; var5 < this.fluids.size(); ++var5) {
                        var4 = (FluidInstance)this.fluids.get(var5);
                        if (!var4.getFluid().isCategory(FluidCategory.Beverage)) {
                           var2 = var4.getFluid().isCategory(FluidCategory.Poisons);
                        }

                        if (var4.getFluid().isCategory(FluidCategory.Beverage) && var4.getFluid().isCategory(FluidCategory.Alcoholic)) {
                           ++var3;
                        }
                     }

                     if (var2) {
                        if (var3 >= 2) {
                           this.nameCache = Translator.getText("Fluid_HoldingOneType", var1, Translator.getFluidText("Fluid_Cocktail"));
                        } else {
                           this.nameCache = Translator.getText("Fluid_HoldingOneType", var1, Translator.getFluidText("Fluid_Mixed_Beverages"));
                        }
                     } else {
                        this.nameCache = Translator.getText("Fluid_HoldingOneType", var1, Translator.getFluidText("Fluid_Mixed_Fluids"));
                     }
                  }
               }
            }
         }
      }

      return this.nameCache;
   }

   public SealedFluidProperties getProperties() {
      this.recalculateCaches();
      return this.propertiesCache;
   }

   public boolean isEmpty() {
      return this.getAmount() == 0.0F;
   }

   public boolean isFull() {
      return PZMath.equal(this.getAmount(), this.capacity, 1.0E-4F);
   }

   public float getCapacity() {
      return this.capacity;
   }

   public float getFreeCapacity() {
      float var1 = this.capacity - this.getAmount();
      return PZMath.equal(var1, 0.0F, 1.0E-4F) ? 0.0F : var1;
   }

   public float getFilledRatio() {
      if (this.isFull()) {
         return 1.0F;
      } else if (this.isEmpty()) {
         return 0.0F;
      } else {
         return this.capacity != 0.0F ? this.getAmount() / this.capacity : 0.0F;
      }
   }

   protected void invalidateColor() {
      this.cacheInvalidated = true;
   }

   public Color getColor() {
      this.recalculateCaches();
      return this.color;
   }

   public float getAmount() {
      this.recalculateCaches();
      return this.amountCache;
   }

   private void recalculateCaches() {
      this.recalculateCaches(false, false);
   }

   private void recalculateCaches(boolean var1) {
      this.recalculateCaches(var1, false);
   }

   private void recalculateCaches(boolean var1, boolean var2) {
      if (this.cacheInvalidated || var1) {
         this.amountCache = 0.0F;
         float var3 = 0.0F;
         float var4 = 0.0F;
         float var5 = 0.0F;

         FluidInstance var6;
         int var7;
         for(var7 = this.fluids.size() - 1; var7 >= 0; --var7) {
            var6 = (FluidInstance)this.fluids.get(var7);
            if (!var2 || !this.removeFluidInstanceIfEmpty(var6)) {
               this.amountCache += var6.getAmount();
            }
         }

         this.propertiesCache.clear();
         if (this.amountCache > 0.0F) {
            for(var7 = 0; var7 < this.fluids.size(); ++var7) {
               var6 = (FluidInstance)this.fluids.get(var7);
               var6.setPercentage(var6.getAmount() / this.amountCache);
               var3 += var6.getColor().r * var6.getPercentage();
               var4 += var6.getColor().g * var6.getPercentage();
               var5 += var6.getColor().b * var6.getPercentage();
               if (var6.getFluid().getProperties() != null) {
                  float var8 = this.amountCache * var6.getPercentage();
                  this.propertiesCache.addFromMultiplied(var6.getFluid().getProperties(), var8);
               }
            }
         }

         if (this.amountCache > 0.0F) {
            this.color.set(var3, var4, var5);
         } else {
            this.color.set(colorDef);
         }

         this.nameCache = null;
         this.cacheInvalidated = false;
         if (this.owner != null && this.owner instanceof InventoryItem) {
            InventoryItem var9 = (InventoryItem)this.owner;
            if (var9.isInPlayerInventory()) {
               ItemContainer var10 = var9.getContainer();
               if (var10 != null) {
                  var10.setDrawDirty(true);
                  var10.setDirty(true);
               }
            }
         }
      }

   }

   private float getPoisonAmount() {
      float var2 = 0.0F;

      for(int var3 = 0; var3 < this.fluids.size(); ++var3) {
         FluidInstance var1 = (FluidInstance)this.fluids.get(var3);
         if (var1.getFluid().isPoisonous()) {
            var2 += var1.getAmount();
         }
      }

      return var2;
   }

   public float getPoisonRatio() {
      float var1 = this.getPoisonAmount();
      if (var1 > 0.0F) {
         float var2 = this.getAmount();
         return var2 > 0.0F && var2 >= var1 ? var1 / var2 : 1.0F;
      } else {
         return 0.0F;
      }
   }

   public boolean isPoisonous() {
      for(int var1 = 0; var1 < this.fluids.size(); ++var1) {
         if (((FluidInstance)this.fluids.get(var1)).getFluid().isPoisonous()) {
            return true;
         }
      }

      return false;
   }

   public PoisonEffect getPoisonEffect() {
      PoisonEffect var1 = PoisonEffect.None;

      for(int var3 = 0; var3 < this.fluids.size(); ++var3) {
         FluidInstance var2 = (FluidInstance)this.fluids.get(var3);
         if (var2.getFluid().getPoisonInfo() != null) {
            PoisonInfo var4 = var2.getFluid().getPoisonInfo();
            PoisonEffect var5 = var4.getPoisonEffect(this.getAmount() * var2.getPercentage(), var2.getPercentage());
            if (var5.getLevel() > var1.getLevel()) {
               var1 = var5;
            }
         }
      }

      return var1;
   }

   public void setCapacity(float var1) {
      float var2 = this.capacity;
      this.capacity = PZMath.max(var1, 0.05F);
      if (this.getAmount() > this.capacity) {
         for(int var3 = this.fluids.size() - 1; var3 >= 0; --var3) {
            FluidInstance var4 = (FluidInstance)this.fluids.get(var3);
            float var5 = var4.getAmount() / var2;
            var4.setAmount(this.capacity * var5);
            this.removeFluidInstanceIfEmpty(var4);
         }
      }

      this.cacheInvalidated = true;
   }

   public void adjustAmount(float var1) {
      if (!this.isEmpty()) {
         var1 = PZMath.clamp(var1, 0.0F, this.getCapacity());
         float var2 = var1 / this.getAmount();

         for(int var3 = this.fluids.size() - 1; var3 >= 0; --var3) {
            FluidInstance var4 = (FluidInstance)this.fluids.get(var3);
            float var5 = var4.getAmount() * var2;
            var4.setAmount(var5);
            this.removeFluidInstanceIfEmpty(var4);
         }

         this.cacheInvalidated = true;
      }
   }

   public void adjustSpecificFluidAmount(Fluid var1, float var2) {
      if (!this.isEmpty()) {
         var2 = PZMath.clamp(var2, 0.0F, this.getCapacity());

         for(int var3 = this.fluids.size() - 1; var3 >= 0; --var3) {
            FluidInstance var4 = (FluidInstance)this.fluids.get(var3);
            if (var4.getFluid() == var1) {
               var4.setAmount(var2);
               this.removeFluidInstanceIfEmpty(var4);
            }
         }

         this.cacheInvalidated = true;
      }
   }

   public float getSpecificFluidAmount(Fluid var1) {
      if (this.isEmpty()) {
         return 0.0F;
      } else {
         FluidInstance var2 = null;

         for(int var3 = 0; var3 < this.fluids.size(); ++var3) {
            if (((FluidInstance)this.fluids.get(var3)).getFluid() == var1) {
               var2 = (FluidInstance)this.fluids.get(var3);
            }
         }

         if (var2 == null) {
            return 0.0F;
         } else {
            return var2.getAmount();
         }
      }
   }

   public FluidSample createFluidSample() {
      return this.createFluidSample(this.getAmount());
   }

   public FluidSample createFluidSample(float var1) {
      FluidSample var2 = FluidSample.Alloc();

      for(int var3 = 0; var3 < this.fluids.size(); ++var3) {
         FluidInstance var4 = (FluidInstance)this.fluids.get(var3);
         var2.addFluid(var4);
      }

      if (var1 != this.getAmount()) {
         var2.scaleToAmount(var1);
      }

      return var2.seal();
   }

   public FluidSample createFluidSample(FluidSample var1, float var2) {
      var1.clear();

      for(int var3 = 0; var3 < this.fluids.size(); ++var3) {
         FluidInstance var4 = (FluidInstance)this.fluids.get(var3);
         var1.addFluid(var4);
      }

      if (var2 != this.getAmount()) {
         var1.scaleToAmount(var2);
      }

      return var1.seal();
   }

   public boolean isPureFluid(Fluid var1) {
      return this.fluids.size() == 1 && ((FluidInstance)this.fluids.get(0)).getFluid() == var1;
   }

   public Fluid getPrimaryFluid() {
      if (this.isEmpty()) {
         return null;
      } else if (this.fluids.size() == 1) {
         return ((FluidInstance)this.fluids.get(0)).getFluid();
      } else {
         FluidInstance var1 = null;

         for(int var2 = 0; var2 < this.fluids.size(); ++var2) {
            FluidInstance var3 = (FluidInstance)this.fluids.get(var2);
            if (var1 == null || var3.getAmount() > var1.getAmount()) {
               var1 = var3;
            }
         }

         return var1.getFluid();
      }
   }

   public float getPrimaryFluidAmount() {
      if (this.isEmpty()) {
         return 0.0F;
      } else if (this.fluids.size() == 1) {
         return ((FluidInstance)this.fluids.get(0)).getAmount();
      } else {
         FluidInstance var1 = null;

         for(int var2 = 0; var2 < this.fluids.size(); ++var2) {
            FluidInstance var3 = (FluidInstance)this.fluids.get(var2);
            if (var1 == null || var3.getAmount() > var1.getAmount()) {
               var1 = var3;
            }
         }

         return var1.getAmount();
      }
   }

   public boolean isPerceivedFluidToPlayer(Fluid var1, IsoGameCharacter var2) {
      return !this.isEmpty();
   }

   public boolean isMixture() {
      return this.fluids.size() > 1;
   }

   public FluidFilter getWhitelist() {
      return this.whitelist;
   }

   public FluidFilter getBlacklist() {
      return this.blacklist;
   }

   public void Empty() {
      this.Empty(true);
   }

   public void Empty(boolean var1) {
      for(int var2 = 0; var2 < this.fluids.size(); ++var2) {
         FluidInstance.Release((FluidInstance)this.fluids.get(var2));
      }

      this.fluids.clear();
      this.propertiesCache.clear();
      this.amountCache = 0.0F;
      this.color.set(colorDef);
      if (var1) {
         this.recalculateCaches(true);
      }

   }

   private FluidInstance getFluidInstance(Fluid var1) {
      if (this.fluids.size() == 0) {
         return null;
      } else {
         for(int var3 = 0; var3 < this.fluids.size(); ++var3) {
            FluidInstance var2 = (FluidInstance)this.fluids.get(var3);
            if (var2.getFluid() == var1) {
               return var2;
            }
         }

         return null;
      }
   }

   public boolean canAddFluid(Fluid var1) {
      if ((this.fluids.size() < 8 || this.contains(var1)) && !this.inputLocked && this.canPlayerEmpty) {
         for(int var3 = 0; var3 < this.fluids.size(); ++var3) {
            FluidInstance var2 = (FluidInstance)this.fluids.get(var3);
            if (!var2.getFluid().canBlendWith(var1) || !var1.canBlendWith(var2.getFluid())) {
               return false;
            }
         }

         return (this.whitelist == null || this.whitelist.allows(var1)) && (this.blacklist == null || this.blacklist.allows(var1));
      } else {
         return false;
      }
   }

   public void addFluid(String var1, float var2) {
      this.addFluid(Fluid.Get(var1), var2);
   }

   public void addFluid(FluidType var1, float var2) {
      this.addFluid(Fluid.Get(var1), var2);
   }

   public void addFluid(Fluid var1, float var2) {
      if (var1 != null) {
         FluidInstance var3 = var1.getInstance();
         this.addFluid(var3, var2);
         FluidInstance.Release(var3);
      }

   }

   private void addFluid(FluidInstance var1, float var2) {
      var2 = PZMath.max(0.0F, var2);
      if (var2 > 0.0F && !this.isFull() && this.canAddFluid(var1.getFluid())) {
         float var3 = this.capacity - this.getAmount();
         var2 = PZMath.min(var2, var3);
         FluidInstance var4 = this.getFluidInstance(var1.getFluid());
         if (var4 == null) {
            var4 = var1.getFluid().getInstance();
            var4.setParent(this);
            var4.setColor(var1.getColor());
            this.fluids.add(var4);
         } else if (var4.getFluid().isCategory(FluidCategory.Colors) && !var1.getColor().equals(var4.getColor())) {
            var4.mixColor(var1.getColor(), var2);
         }

         var4.setAmount(var4.getAmount() + var2);
         this.cacheInvalidated = true;
      }

   }

   public void removeFluid() {
      this.removeFluid(this.getAmount(), false);
   }

   public FluidConsume removeFluid(boolean var1) {
      return this.removeFluid(this.getAmount(), var1);
   }

   public void removeFluid(float var1) {
      this.removeFluid(var1, false);
   }

   public FluidConsume removeFluid(float var1, boolean var2) {
      return this.removeFluid(var1, var2, (FluidConsume)null);
   }

   public FluidConsume removeFluid(float var1, boolean var2, FluidConsume var3) {
      FluidConsume var4 = var3;
      if (var3 != null) {
         var3.clear();
      } else if (var2) {
         var4 = FluidConsume.Alloc();
      }

      var1 = PZMath.max(0.0F, var1);
      if (var1 > 0.0F && !this.isEmpty()) {
         var1 = PZMath.min(var1, this.getAmount());
         if (var4 != null) {
            var4.setAmount(var1);
         }

         for(int var6 = this.fluids.size() - 1; var6 >= 0; --var6) {
            FluidInstance var5 = (FluidInstance)this.fluids.get(var6);
            float var7 = var1 * var5.getPercentage();
            if (var4 != null) {
               if (var5.getFluid().getPoisonInfo() != null) {
                  PoisonInfo var8 = var5.getFluid().getPoisonInfo();
                  PoisonEffect var9 = var8.getPoisonEffect(var7, var5.getPercentage());
                  var4.setPoisonEffect(var9);
               }

               if (var5.getFluid().getProperties() != null) {
                  SealedFluidProperties var10 = var5.getFluid().getProperties();
                  var4.addFromMultiplied(var10, var7);
               }
            }

            var5.setAmount(var5.getAmount() - var7);
            this.removeFluidInstanceIfEmpty(var5);
         }

         this.cacheInvalidated = true;
      }

      return var4;
   }

   private boolean removeFluidInstanceIfEmpty(FluidInstance var1) {
      if (PZMath.equal(var1.getAmount(), 0.0F, 1.0E-4F)) {
         this.fluids.remove(var1);
         FluidInstance.Release(var1);
         return true;
      } else {
         return false;
      }
   }

   public boolean contains(Fluid var1) {
      for(int var2 = 0; var2 < this.fluids.size(); ++var2) {
         if (((FluidInstance)this.fluids.get(var2)).getFluid() == var1) {
            return true;
         }
      }

      return false;
   }

   public float getRatioForFluid(Fluid var1) {
      for(int var2 = 0; var2 < this.fluids.size(); ++var2) {
         if (((FluidInstance)this.fluids.get(var2)).getFluid() == var1) {
            return ((FluidInstance)this.fluids.get(var2)).getPercentage();
         }
      }

      return 0.0F;
   }

   public boolean isCategory(FluidCategory var1) {
      for(int var3 = 0; var3 < this.fluids.size(); ++var3) {
         FluidInstance var2 = (FluidInstance)this.fluids.get(var3);
         if (var2.getFluid().isCategory(var1)) {
            return true;
         }
      }

      return false;
   }

   public boolean isAllCategory(FluidCategory var1) {
      for(int var3 = 0; var3 < this.fluids.size(); ++var3) {
         FluidInstance var2 = (FluidInstance)this.fluids.get(var3);
         if (!var2.getFluid().isCategory(var1)) {
            return false;
         }
      }

      return true;
   }

   public void transferTo(FluidContainer var1) {
      this.transferTo(var1, this.getAmount());
   }

   public void transferTo(FluidContainer var1, float var2) {
      Transfer(this, var1, var2);
   }

   public void transferFrom(FluidContainer var1) {
      this.transferFrom(var1, var1.getAmount());
   }

   public void transferFrom(FluidContainer var1, float var2) {
      Transfer(var1, this, var2);
   }

   public static String GetTransferReason(FluidContainer var0, FluidContainer var1) {
      return GetTransferReason(var0, var1, false);
   }

   public static String GetTransferReason(FluidContainer var0, FluidContainer var1, boolean var2) {
      if (var2 && CanTransfer(var0, var1)) {
         return Translator.getFluidText("Fluid_Reason_Allowed");
      } else if (var0 == null) {
         return Translator.getFluidText("Fluid_Reason_Source_Null");
      } else if (var1 == null) {
         return Translator.getFluidText("Fluid_Reason_Target_Null");
      } else if (var0 == var1) {
         return Translator.getFluidText("Fluid_Reason_Equal");
      } else if (var0.isEmpty()) {
         return Translator.getFluidText("Fluid_Reason_Source_Empty");
      } else if (var1.isFull()) {
         return Translator.getFluidText("Fluid_Reason_Target_Full");
      } else if (var1.isInputLocked()) {
         return Translator.getFluidText("Fluid_Reason_Target_Locked");
      } else {
         if (var1.whitelist != null || var1.blacklist != null) {
            boolean var3 = true;

            for(int var5 = 0; var5 < var0.fluids.size(); ++var5) {
               FluidInstance var4 = (FluidInstance)var0.fluids.get(var5);
               if (var1.whitelist != null && !var1.whitelist.allows(var4.getFluid())) {
                  var3 = false;
                  break;
               }

               if (var1.blacklist != null && !var1.blacklist.allows(var4.getFluid())) {
                  var3 = false;
                  break;
               }
            }

            if (!var3) {
               return Translator.getFluidText("Fluid_Reason_Target_Filter");
            }
         }

         return Translator.getFluidText("Fluid_Reason_Mixing_Locked");
      }
   }

   public static boolean CanTransfer(FluidContainer var0, FluidContainer var1) {
      if (var0 != null && var1 != null) {
         if (var0 == var1) {
            return false;
         } else if (!var0.isEmpty() && !var1.isFull() && !var1.inputLocked) {
            int var2 = 0;

            int var4;
            for(var4 = 0; var4 < var0.fluids.size(); ++var4) {
               FluidInstance var3 = (FluidInstance)var0.fluids.get(var4);
               if (!var1.canAddFluid(var3.getFluid())) {
                  return false;
               }

               if (var1.contains(var3.getFluid())) {
                  ++var2;
               }
            }

            var4 = var2 + (var0.fluids.size() - var2) + (var1.fluids.size() - var2);
            if (var4 > 8) {
               return false;
            } else {
               return true;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static void Transfer(FluidContainer var0, FluidContainer var1) {
      Transfer(var0, var1, var0.getAmount());
   }

   public static void Transfer(FluidContainer var0, FluidContainer var1, float var2) {
      Transfer(var0, var1, var2, false);
   }

   public static void Transfer(FluidContainer var0, FluidContainer var1, float var2, boolean var3) {
      if (CanTransfer(var0, var1)) {
         float var4 = PZMath.min(var2, var0.getAmount());
         float var5 = var1.getCapacity() - var1.getAmount();
         var4 = PZMath.min(var4, var5);

         for(int var7 = var0.fluids.size() - 1; var7 >= 0; --var7) {
            FluidInstance var6 = (FluidInstance)var0.fluids.get(var7);
            float var8 = var4 * var6.getPercentage();
            var1.addFluid(var6, var8);
            if (!var3) {
               var6.setAmount(var6.getAmount() - var8);
               var0.removeFluidInstanceIfEmpty(var6);
            }
         }

         if (!var3) {
            var0.recalculateCaches(true);
         }

         var1.recalculateCaches(true);
      }

   }

   protected boolean onReceivePacket(ByteBuffer var1, EntityPacketType var2, UdpConnection var3) throws IOException {
      switch (var2) {
         default:
            return false;
      }
   }

   protected void saveSyncData(ByteBuffer var1) throws IOException {
      this.save(var1);
   }

   protected void loadSyncData(ByteBuffer var1) throws IOException {
      this.load(var1, 219);
   }

   public void save(ByteBuffer var1) throws IOException {
      super.save(var1);
      BitHeaderWrite var2 = BitHeader.allocWrite(BitHeader.HeaderSize.Short, var1);
      if (this.capacity != 1.0F) {
         var2.addFlags(1);
         var1.putFloat(this.capacity);
      }

      if (this.fluids.size() > 0) {
         var2.addFlags(2);
         if (this.fluids.size() == 1) {
            var2.addFlags(4);
            FluidInstance.save((FluidInstance)this.fluids.get(0), var1);
         } else {
            var1.put((byte)this.fluids.size());

            for(int var4 = 0; var4 < this.fluids.size(); ++var4) {
               FluidInstance var3 = (FluidInstance)this.fluids.get(var4);
               FluidInstance.save(var3, var1);
            }
         }
      }

      if (this.whitelist != null) {
         var2.addFlags(8);
         this.whitelist.save(var1);
      }

      if (this.blacklist != null) {
         var2.addFlags(16);
         this.blacklist.save(var1);
      }

      if (this.inputLocked) {
         var2.addFlags(32);
      }

      if (this.canPlayerEmpty) {
         var2.addFlags(64);
      }

      if (!this.containerName.equals("FluidContainer")) {
         var2.addFlags(128);
         GameWindow.WriteString(var1, this.containerName);
      }

      if (this.hiddenAmount) {
         var2.addFlags(256);
      }

      if (this.rainCatcher > 0.0F) {
         var2.addFlags(512);
         var1.putFloat(this.rainCatcher);
      }

      var2.write();
      var2.release();
   }

   public void load(ByteBuffer var1, int var2) throws IOException {
      super.load(var1, var2);
      this.amountCache = 0.0F;
      this.capacity = 1.0F;
      this.whitelist = null;
      this.blacklist = null;
      this.inputLocked = false;
      this.canPlayerEmpty = false;
      this.hiddenAmount = false;
      this.Empty(false);
      BitHeaderRead var3 = BitHeader.allocRead(BitHeader.HeaderSize.Short, var1);
      if (var3.hasFlags(1)) {
         this.capacity = var1.getFloat();
      }

      if (var3.hasFlags(2)) {
         if (var3.hasFlags(4)) {
            FluidInstance var4 = FluidInstance.load(var1, var2);
            if (var4.getFluid() != null) {
               var4.setParent(this);
               this.fluids.add(var4);
            }
         } else {
            byte var7 = var1.get();

            for(int var6 = 0; var6 < var7; ++var6) {
               FluidInstance var5 = FluidInstance.load(var1, var2);
               if (var5.getFluid() != null) {
                  var5.setParent(this);
                  this.fluids.add(var5);
               }
            }
         }
      }

      if (var3.hasFlags(8)) {
         this.whitelist = new FluidFilter();
         this.whitelist.load(var1, var2);
      }

      if (var3.hasFlags(16)) {
         this.blacklist = new FluidFilter();
         this.blacklist.load(var1, var2);
      }

      this.inputLocked = var3.hasFlags(32);
      this.canPlayerEmpty = var3.hasFlags(64);
      if (var3.hasFlags(128)) {
         this.containerName = GameWindow.ReadString(var1);
      } else {
         this.containerName = "FluidContainer";
      }

      this.hiddenAmount = var3.hasFlags(256);
      if (var3.hasFlags(512)) {
         this.rainCatcher = var1.getFloat();
      }

      var3.release();
      this.recalculateCaches(true);
   }

   static {
      colorDef = Color.white;
      tempFluidUI = new ArrayList();
      df = new DecimalFormat("#.##");
   }
}
