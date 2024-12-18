package zombie.scripting.entity.components.fluids;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import zombie.core.Color;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.ComponentType;
import zombie.entity.components.fluids.Fluid;
import zombie.entity.components.fluids.FluidFilter;
import zombie.entity.components.fluids.FluidUtil;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptParser;
import zombie.scripting.entity.ComponentScript;
import zombie.scripting.objects.FluidFilterScript;
import zombie.util.StringUtils;

@DebugClassFields
public class FluidContainerScript extends ComponentScript {
   private static final String TAG_OVERRIDE = "override";
   private FluidFilterScript whitelist = null;
   private FluidFilterScript blacklist = null;
   private String containerName = "FluidContainer";
   private float capacity = 1.0F;
   private boolean initialAmountSet = false;
   private float initialAmountMin = 0.0F;
   private float initialAmountMax = 1.0F;
   private ArrayList<FluidScript> initialFluids = null;
   private boolean initialFluidsIsRandom = false;
   private boolean inputLocked = false;
   private boolean canEmpty = true;
   private boolean hiddenAmount = false;
   private float rainCatcher = 0.0F;
   private String customDrinkSound = "DrinkingFromGeneric";

   private FluidContainerScript() {
      super(ComponentType.FluidContainer);
   }

   public void PreReload() {
      this.whitelist = null;
      this.blacklist = null;
      this.containerName = "FluidContainer";
      this.capacity = 1.0F;
      this.initialAmountSet = false;
      this.initialAmountMin = 0.0F;
      this.initialAmountMax = 1.0F;
      this.initialFluids = null;
      this.initialFluidsIsRandom = false;
      this.inputLocked = false;
      this.canEmpty = true;
      this.hiddenAmount = false;
      this.rainCatcher = 0.0F;
      this.customDrinkSound = "DrinkingFromGeneric";
   }

   public void OnScriptsLoaded(ScriptLoadMode var1) throws Exception {
      super.OnScriptsLoaded(var1);
      this.capacity = PZMath.max(this.capacity, FluidUtil.getMinContainerCapacity());
      if (this.initialAmountMin > this.initialAmountMax) {
         float var2 = this.initialAmountMax;
         this.initialAmountMax = this.initialAmountMin;
         this.initialAmountMin = var2;
      }

      this.initialAmountMin = PZMath.clamp(this.initialAmountMin, 0.0F, 1.0F);
      this.initialAmountMax = PZMath.clamp(this.initialAmountMax, 0.0F, 1.0F);
      this.initialAmountMin *= this.capacity;
      this.initialAmountMax *= this.capacity;
      if (!this.initialFluidsIsRandom && this.initialFluids != null && this.initialFluids.size() > 0) {
         PZMath.normalize((List)this.initialFluids, FluidScript::getPercentage, FluidScript::setPercentage);
      }

   }

   protected void copyFrom(ComponentScript var1) {
      FluidContainerScript var2 = (FluidContainerScript)var1;
      this.containerName = var2.containerName;
      this.capacity = var2.capacity;
      this.initialFluidsIsRandom = var2.initialFluidsIsRandom;
      this.initialAmountSet = var2.initialAmountSet;
      this.initialAmountMin = var2.initialAmountMin;
      this.initialAmountMax = var2.initialAmountMax;
      this.inputLocked = var2.inputLocked;
      this.canEmpty = var2.canEmpty;
      this.rainCatcher = var2.rainCatcher;
      this.hiddenAmount = var2.hiddenAmount;
      this.customDrinkSound = var2.customDrinkSound;
      if (var2.initialFluids != null) {
         this.initialFluids = new ArrayList();
         Iterator var3 = var2.initialFluids.iterator();

         while(var3.hasNext()) {
            FluidScript var4 = (FluidScript)var3.next();
            FluidScript var5 = this.getOrCreateFluidScript(var4.fluidType);
            var5.percentage = var4.percentage;
            if (var4.customColor != null) {
               var5.customColor = new Color();
               var5.customColor.set(var4.customColor);
            }
         }
      }

      if (var2.whitelist != null) {
         this.whitelist = var2.whitelist.copy();
      }

      if (var2.blacklist != null) {
         this.blacklist = var2.blacklist.copy();
      }

   }

   protected void load(ScriptParser.Block var1) throws Exception {
      super.load(var1);
      Iterator var2 = var1.elements.iterator();

      while(true) {
         while(var2.hasNext()) {
            ScriptParser.BlockElement var3 = (ScriptParser.BlockElement)var2.next();
            if (var3.asValue() != null) {
               String var8 = var3.asValue().string;
               if (!var8.trim().isEmpty() && var8.contains("=")) {
                  String[] var9 = var8.split("=");
                  String var10 = var9[0].trim();
                  String var11 = var9[1].trim();
                  if (var10.equalsIgnoreCase("Capacity")) {
                     this.capacity = Float.parseFloat(var11);
                  } else if (var10.equalsIgnoreCase("ContainerName")) {
                     if (StringUtils.containsWhitespace(var11)) {
                        DebugLog.General.error("Sanitizing container name '" + var11 + "', name may not contain whitespaces.");
                        var11 = StringUtils.removeWhitespace(var11);
                     }

                     this.containerName = var11;
                  } else if (var10.equalsIgnoreCase("InitialPercent")) {
                     this.initialAmountMin = Float.parseFloat(var11);
                     this.initialAmountMax = this.initialAmountMin;
                     this.initialAmountSet = true;
                  } else if (var10.equalsIgnoreCase("InitialPercentMin")) {
                     this.initialAmountMin = Float.parseFloat(var11);
                     this.initialAmountSet = true;
                  } else if (var10.equalsIgnoreCase("InitialPercentMax")) {
                     this.initialAmountMax = Float.parseFloat(var11);
                     this.initialAmountSet = true;
                  } else if (var10.equalsIgnoreCase("PickRandomFluid")) {
                     this.initialFluidsIsRandom = var11.equalsIgnoreCase("true");
                  } else if (var10.equalsIgnoreCase("InputLocked")) {
                     this.inputLocked = var11.equalsIgnoreCase("true");
                  } else if (var10.equalsIgnoreCase("Opened")) {
                     this.canEmpty = var11.equalsIgnoreCase("true");
                  } else if (var10.equalsIgnoreCase("HiddenAmount")) {
                     this.hiddenAmount = var11.equalsIgnoreCase("true");
                  } else if (var10.equalsIgnoreCase("RainFactor")) {
                     this.rainCatcher = Float.parseFloat(var11);
                  } else if (var10.equalsIgnoreCase("CustomDrinkSound")) {
                     this.customDrinkSound = var11;
                  }
               }
            } else {
               ScriptParser.Block var4 = var3.asBlock();
               boolean var5 = var1.id != null && var1.id.equalsIgnoreCase("override");
               if (var4.type.equalsIgnoreCase("fluids")) {
                  this.loadBlockFluids(var4, var5);
               } else if (var4.type.equalsIgnoreCase("whitelist") || var4.type.equalsIgnoreCase("blacklist")) {
                  boolean var6 = var4.type.equalsIgnoreCase("whitelist");
                  FluidFilterScript var7;
                  if (!var5 && var6 && this.whitelist != null) {
                     var7 = this.whitelist;
                  } else if (!var5 && !var6 && this.blacklist != null) {
                     var7 = this.blacklist;
                  } else {
                     var7 = FluidFilterScript.GetAnonymous(var6);
                  }

                  var7.LoadAnonymousFromBlock(var4);
                  if (var6) {
                     this.whitelist = var7;
                  } else {
                     this.blacklist = var7;
                  }
               }
            }
         }

         return;
      }
   }

   private void loadBlockFluids(ScriptParser.Block var1, boolean var2) {
      if (var2 && this.initialFluids != null) {
         this.initialFluids.clear();
      }

      Iterator var3 = var1.values.iterator();

      while(var3.hasNext()) {
         ScriptParser.Value var4 = (ScriptParser.Value)var3.next();
         String var5 = var4.getKey().trim();
         String var6 = var4.getValue().trim();
         if (var5.equalsIgnoreCase("Fluid")) {
            FluidScript var7 = this.readFluid(var6);
            if (this.initialFluids == null) {
               this.initialFluids = new ArrayList();
            }

            this.initialFluids.add(var7);
         }
      }

      var3 = var1.children.iterator();

      while(var3.hasNext()) {
         ScriptParser.Block var8 = (ScriptParser.Block)var3.next();
         if (var8.type.equalsIgnoreCase("Fluid")) {
            FluidScript var9 = this.readFluidAsBlock(var8);
            if (var9 != null) {
               if (this.initialFluids == null) {
                  this.initialFluids = new ArrayList();
               }

               this.initialFluids.add(var9);
            } else {
               DebugLog.General.warn("Unable to read fluid block.");
            }
         }
      }

   }

   private FluidScript getOrCreateFluidScript(String var1) {
      return new FluidScript(var1);
   }

   private FluidScript readFluidAsBlock(ScriptParser.Block var1) {
      FluidScript var2 = null;
      Iterator var3 = var1.values.iterator();

      ScriptParser.Value var4;
      String var5;
      String var6;
      while(var3.hasNext()) {
         var4 = (ScriptParser.Value)var3.next();
         var5 = var4.getKey().trim();
         var6 = var4.getValue().trim();
         if ("type".equalsIgnoreCase(var5)) {
            var2 = this.getOrCreateFluidScript(var6);
         }
      }

      if (var2 == null) {
         return null;
      } else {
         var3 = var1.values.iterator();

         while(var3.hasNext()) {
            var4 = (ScriptParser.Value)var3.next();
            var5 = var4.getKey().trim();
            var6 = var4.getValue().trim();
            if ("percentage".equalsIgnoreCase(var5)) {
               var2.percentage = Float.parseFloat(var6);
            } else if ("r".equalsIgnoreCase(var5)) {
               if (var2.customColor == null) {
                  var2.customColor = new Color(1.0F, 1.0F, 1.0F);
               }

               var2.customColor.r = Float.parseFloat(var6);
            } else if ("g".equalsIgnoreCase(var5)) {
               if (var2.customColor == null) {
                  var2.customColor = new Color(1.0F, 1.0F, 1.0F);
               }

               var2.customColor.g = Float.parseFloat(var6);
            } else if ("b".equalsIgnoreCase(var5)) {
               if (var2.customColor == null) {
                  var2.customColor = new Color(1.0F, 1.0F, 1.0F);
               }

               var2.customColor.b = Float.parseFloat(var6);
            }
         }

         return var2;
      }
   }

   private FluidScript readFluid(String var1) {
      String[] var2 = var1.split(":");
      FluidScript var3 = this.getOrCreateFluidScript(var2[0]);
      if (var2.length > 1) {
         var3.percentage = Float.parseFloat(var2[1]);
      }

      if (var2.length == 5) {
         float var4 = Float.parseFloat(var2[2]);
         float var5 = Float.parseFloat(var2[3]);
         float var6 = Float.parseFloat(var2[4]);
         Color var7 = new Color(var4, var5, var6);
         var3.customColor = var7;
      }

      return var3;
   }

   public FluidFilter getWhitelistCopy() {
      return this.whitelist != null ? this.whitelist.getFilter().copy() : null;
   }

   public FluidFilter getBlacklistCopy() {
      return this.blacklist != null ? this.blacklist.getFilter().copy() : null;
   }

   public String getContainerName() {
      return this.containerName;
   }

   public String getCustomDrinkSound() {
      return this.customDrinkSound;
   }

   public float getCapacity() {
      return this.capacity;
   }

   public float getInitialAmount() {
      if (!this.initialAmountSet) {
         return this.capacity;
      } else {
         return this.initialAmountMin == this.initialAmountMax ? this.initialAmountMin : Rand.Next(this.initialAmountMin, this.initialAmountMax);
      }
   }

   public ArrayList<FluidScript> getInitialFluids() {
      return this.initialFluids;
   }

   public boolean isInitialFluidsIsRandom() {
      return this.initialFluidsIsRandom;
   }

   public boolean getInputLocked() {
      return this.inputLocked;
   }

   public boolean getCanEmpty() {
      return this.canEmpty;
   }

   public boolean isHiddenAmount() {
      return this.hiddenAmount;
   }

   public float getRainCatcher() {
      return this.rainCatcher;
   }

   @DebugClassFields
   public static class FluidScript {
      private final String fluidType;
      private float percentage = 1.0F;
      private Color customColor;
      private Fluid fluid;

      private FluidScript(String var1) {
         this.fluidType = var1;
      }

      public String getFluidType() {
         return this.fluidType;
      }

      public Fluid getFluid() {
         if (this.fluid == null) {
            this.fluid = Fluid.Get(this.fluidType);
            if (this.fluid == null) {
               DebugLog.General.warn("Cannot find fluid '" + this.fluidType + "' in fluid script.");
            }
         }

         return this.fluid;
      }

      protected void setPercentage(float var1) {
         this.percentage = var1;
      }

      public float getPercentage() {
         return this.percentage;
      }

      public Color getCustomColor() {
         return this.customColor;
      }
   }
}
