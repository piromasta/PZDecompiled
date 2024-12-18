package zombie.entity.components.fluids;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import zombie.GameWindow;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.FluidFilterScript;

@DebugClassFields
public class FluidFilter {
   private FluidFilterScript filterScript;
   private final EnumSet<FluidCategory> categories = EnumSet.noneOf(FluidCategory.class);
   private final EnumSet<FluidType> fluidEnums = EnumSet.noneOf(FluidType.class);
   private final HashSet<String> fluidStrings = new HashSet();
   private FilterType filterType;
   private boolean isSealed;

   public FluidFilter() {
      this.filterType = FluidFilter.FilterType.Whitelist;
      this.isSealed = false;
   }

   public void setFilterScript(String var1) {
      if (var1 == null) {
         this.filterScript = null;
      } else if (this.filterScript == null || !var1.equalsIgnoreCase(this.filterScript.getScriptObjectFullType())) {
         FluidFilterScript var2 = ScriptManager.instance.getFluidFilter(var1);
         if (var2 == null) {
            DebugLog.General.warn("FluidFilter '" + var1 + "' not found.");
         }

         this.filterScript = var2;
      }

   }

   public String toString() {
      String var1 = super.toString();
      if (this.categories.isEmpty() && this.fluidEnums.isEmpty() && this.fluidStrings.isEmpty()) {
         var1 = var1 + "{EMPTY_FILTER}";
         return var1;
      } else {
         var1 = var1 + "{";
         var1 = var1 + "<cats=" + this.categories + ">";
         var1 = var1 + "<fluids=" + this.fluidEnums + ">";
         var1 = var1 + "<strings=" + this.fluidStrings.size() + ">";
         var1 = var1 + "}";
         return var1;
      }
   }

   protected void seal() {
      this.isSealed = true;
   }

   public FluidFilter copy() {
      FluidFilter var1 = new FluidFilter();
      var1.filterType = this.filterType;
      var1.categories.addAll(this.categories);
      var1.fluidEnums.addAll(this.fluidEnums);
      var1.fluidStrings.addAll(this.fluidStrings);
      return var1;
   }

   public FilterType getFilterType() {
      return this.filterType;
   }

   public FluidFilter setFilterType(FilterType var1) {
      if (this.isSealed) {
         DebugLog.log("FluidFilter -> attempting setFilterType on sealed filter.");
         return this;
      } else {
         this.filterType = var1;
         return this;
      }
   }

   public FluidFilter add(FluidCategory var1) {
      if (this.isSealed) {
         DebugLog.log("FluidFilter -> attempting add on sealed filter.");
         return this;
      } else {
         this.categories.add(var1);
         return this;
      }
   }

   public FluidFilter remove(FluidCategory var1) {
      if (this.isSealed) {
         DebugLog.log("FluidFilter -> attempting remove on sealed filter.");
         return this;
      } else {
         this.categories.remove(var1);
         return this;
      }
   }

   public boolean contains(FluidCategory var1) {
      return this.categories.contains(var1);
   }

   public FluidFilter add(FluidType var1) {
      if (this.isSealed) {
         DebugLog.log("FluidFilter -> attempting add on sealed filter.");
         return this;
      } else if (var1 == FluidType.Modded) {
         DebugLog.log("Cannot add enum FluidType.Modded to fluid filter, add the fluid string (getFluidTypeString) instead.");
         return this;
      } else {
         if (!this.fluidEnums.contains(var1)) {
            this.fluidEnums.add(var1);
            this.fluidStrings.add(var1.toString());
         }

         return this;
      }
   }

   public FluidFilter add(Fluid var1) {
      return var1.getFluidType() != FluidType.Modded ? this.add(var1.getFluidType()) : this.add(var1.getFluidTypeString());
   }

   public FluidFilter add(String var1) {
      if (this.isSealed) {
         DebugLog.log("FluidFilter -> attempting add on sealed filter.");
         return this;
      } else {
         if (!this.fluidStrings.contains(var1)) {
            if (Fluid.Get(var1) != null) {
               this.fluidStrings.add(var1);
            } else {
               DebugLog.log("FluidFilter.add -> fluid '" + var1 + "' is not a valid registered fluid.");
            }
         }

         return this;
      }
   }

   public FluidFilter remove(FluidType var1) {
      if (this.isSealed) {
         DebugLog.log("FluidFilter -> attempting remove on sealed filter.");
         return this;
      } else {
         this.fluidEnums.remove(var1);
         this.fluidStrings.remove(var1.toString());
         return this;
      }
   }

   public FluidFilter remove(Fluid var1) {
      return var1.getFluidType() != FluidType.Modded ? this.remove(var1.getFluidType()) : this.remove(var1.getFluidTypeString());
   }

   public FluidFilter remove(String var1) {
      if (this.isSealed) {
         DebugLog.log("FluidFilter -> attempting remove on sealed filter.");
         return this;
      } else {
         this.fluidStrings.remove(var1);
         return this;
      }
   }

   public boolean contains(FluidType var1) {
      return this.fluidEnums.contains(var1);
   }

   public boolean contains(Fluid var1) {
      return var1.getFluidType() != FluidType.Modded ? this.contains(var1.getFluidType()) : this.contains(var1.getFluidTypeString());
   }

   public boolean contains(String var1) {
      return this.fluidStrings.contains(var1);
   }

   public boolean allows(FluidType var1) {
      Fluid var2 = Fluid.Get(var1);
      return this.allows(var2);
   }

   public boolean allows(Fluid var1) {
      if (var1 == null) {
         return false;
      } else if (var1.getFluidType() != FluidType.Modded && this.fluidEnums.contains(var1.getFluidType()) || var1.getFluidType() == FluidType.Modded && this.fluidStrings.contains(var1.getFluidTypeString())) {
         return this.filterType == FluidFilter.FilterType.Whitelist;
      } else {
         if (this.categories.size() > 0) {
            Iterator var2 = this.categories.iterator();

            while(var2.hasNext()) {
               FluidCategory var3 = (FluidCategory)var2.next();
               if (var1.isCategory(var3)) {
                  return this.filterType == FluidFilter.FilterType.Whitelist;
               }
            }
         }

         return this.filterType != FluidFilter.FilterType.Whitelist;
      }
   }

   public boolean allows(String var1) {
      Fluid var2 = Fluid.Get(var1);
      return this.allows(var2);
   }

   public void save(ByteBuffer var1) throws IOException {
      var1.put((byte)(this.filterType == FluidFilter.FilterType.Whitelist ? 1 : 0));
      var1.put((byte)this.fluidEnums.size());
      this.fluidEnums.forEach((var1x) -> {
         var1.put(var1x.getId());
      });
      var1.put((byte)this.fluidStrings.size());
      this.fluidStrings.forEach((var1x) -> {
         GameWindow.WriteString(var1, var1x);
      });
      var1.put((byte)this.categories.size());
      this.categories.forEach((var1x) -> {
         var1.put(var1x.getId());
      });
   }

   public void load(ByteBuffer var1, int var2) throws IOException {
      if (this.isSealed) {
         DebugLog.log("FluidFilter -> Warning loading on a sealed fluid filter!");
         if (Core.bDebug) {
            throw new RuntimeException("Loading on a sealed fluid filter!");
         }
      }

      this.fluidEnums.clear();
      this.fluidStrings.clear();
      this.categories.clear();
      this.filterType = var1.get() == 1 ? FluidFilter.FilterType.Whitelist : FluidFilter.FilterType.Blacklist;
      byte var3 = var1.get();
      int var4;
      byte var5;
      if (var3 > 0) {
         for(var4 = 0; var4 < var3; ++var4) {
            var5 = var1.get();
            FluidType var6 = FluidType.FromId(var5);
            if (var6 != null) {
               this.fluidEnums.add(var6);
            }
         }
      }

      var3 = var1.get();
      if (var3 > 0) {
         for(var4 = 0; var4 < var3; ++var4) {
            String var7 = GameWindow.ReadString(var1);
            if (Fluid.Get(var7) != null) {
               this.fluidStrings.add(var7);
            }
         }
      }

      var3 = var1.get();
      if (var3 > 0) {
         for(var4 = 0; var4 < var3; ++var4) {
            var5 = var1.get();
            FluidCategory var8 = FluidCategory.FromId(var5);
            if (var8 != null) {
               this.categories.add(var8);
            }
         }
      }

   }

   public static enum FilterType {
      Whitelist,
      Blacklist;

      private FilterType() {
      }
   }
}
