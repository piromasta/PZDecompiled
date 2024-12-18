package zombie.inventory.recipemanager;

import java.util.ArrayDeque;
import zombie.debug.DebugLog;
import zombie.entity.components.fluids.Fluid;
import zombie.util.StringUtils;

public class SourceType {
   private static final ArrayDeque<SourceType> pool = new ArrayDeque();
   private String itemType;
   private Fluid sourceFluid;
   private boolean usesFluid;

   protected static SourceType alloc(String var0) {
      return pool.isEmpty() ? (new SourceType()).init(var0) : ((SourceType)pool.pop()).init(var0);
   }

   protected static void release(SourceType var0) {
      assert !pool.contains(var0);

      pool.push(var0.reset());
   }

   private SourceType() {
   }

   private SourceType init(String var1) {
      this.itemType = var1;
      if (StringUtils.startsWithIgnoreCase(this.itemType, "Fluid.".toLowerCase())) {
         this.usesFluid = true;
         String var2 = this.itemType.substring("Fluid.".length());
         Fluid var3 = Fluid.Get(var2);
         if (var3 != null) {
            this.sourceFluid = var3;
         } else {
            this.sourceFluid = null;
            DebugLog.General.error("Could not find fluid type '" + this.itemType + "'");
         }
      } else {
         this.usesFluid = false;
         this.sourceFluid = null;
      }

      return this;
   }

   private SourceType reset() {
      this.itemType = null;
      this.sourceFluid = null;
      this.usesFluid = false;
      return this;
   }

   protected boolean isUsesFluid() {
      return this.usesFluid;
   }

   protected Fluid getSourceFluid() {
      return this.sourceFluid;
   }

   protected String getItemType() {
      return this.itemType;
   }

   public String toString() {
      return "SourceType [itemType=" + this.itemType + ", sourceFluid=" + this.sourceFluid + ", usesFluid=" + this.usesFluid + "]";
   }
}
