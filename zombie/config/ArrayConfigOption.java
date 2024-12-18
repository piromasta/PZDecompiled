package zombie.config;

import java.util.ArrayList;
import zombie.core.math.PZMath;
import zombie.core.textures.ColorInfo;
import zombie.debug.DebugLog;

public class ArrayConfigOption extends ConfigOption {
   protected final ArrayList<ConfigOption> value = new ArrayList();
   protected final ArrayList<ConfigOption> defaultValue = new ArrayList();
   protected final String separator;
   protected final ConfigOption elementHandler;
   protected int fixedSize = -1;
   protected boolean bMultiLine = false;

   public ArrayConfigOption(String var1, ConfigOption var2, String var3, String var4) {
      super(var1);
      this.elementHandler = var2;
      this.separator = var3 == null ? "," : var3;
      this.parse(var4);
      this.setDefaultToCurrentValue();
   }

   public ArrayConfigOption setFixedSize(int var1) {
      if (var1 > 0) {
         this.resize(this.value, var1);
         this.resize(this.defaultValue, var1);
      } else {
         this.fixedSize = -1;
      }

      return this;
   }

   public ArrayConfigOption setMultiLine(boolean var1) {
      this.bMultiLine = var1;
      return this;
   }

   public boolean isMultiLine() {
      return this.bMultiLine;
   }

   public void clear() {
      this.value.clear();
   }

   public String getType() {
      return "array";
   }

   public void resetToDefault() {
      this.copyValue(this.defaultValue, this.value);
   }

   public void setDefaultToCurrentValue() {
      this.copyValue(this.value, this.defaultValue);
   }

   public void parse(String var1) {
      this.setValueFromObject(var1);
   }

   public String getValueAsString() {
      return this.getValueAsString(this.value);
   }

   public void setValueFromObject(Object var1) {
      String var10000;
      if (!(var1 instanceof String var2)) {
         var10000 = this.getName();
         DebugLog.log("ERROR ArrayConfigOption.setValueFromObject() \"" + var10000 + "\" string=" + var1 + "\"");
      } else if (!this.isValidString(var2)) {
         var10000 = this.getName();
         DebugLog.log("ERROR ArrayConfigOption.setValueFromObject() \"" + var10000 + "\" string=" + var2 + "\"");
      } else {
         if (!this.isMultiLine()) {
            this.value.clear();
         }

         if (!var2.trim().isEmpty()) {
            String[] var3 = var2.split(this.separator);

            for(int var4 = 0; var4 < var3.length; ++var4) {
               this.elementHandler.resetToDefault();
               this.elementHandler.parse(var3[var4]);
               this.value.add(this.elementHandler.makeCopy());
            }
         }

         if (this.fixedSize > 0) {
            this.resize(this.value, this.fixedSize);
         }

      }
   }

   public Object getValueAsObject() {
      return this.getValueAsString();
   }

   public boolean isValidString(String var1) {
      if (var1.trim().isEmpty()) {
         return true;
      } else {
         String[] var2 = var1.split(this.separator);

         for(int var3 = 0; var3 < var2.length; ++var3) {
            if (!this.elementHandler.isValidString(var2[var3])) {
               return false;
            }
         }

         return true;
      }
   }

   public String getTooltip() {
      return this.getValueAsString();
   }

   public ConfigOption makeCopy() {
      String var1 = this.getValueAsString(this.defaultValue);
      ArrayConfigOption var2 = new ArrayConfigOption(this.name, this.elementHandler, this.separator, var1);
      this.copyValue(this.value, var2.value);
      return var2;
   }

   public int size() {
      return this.value.size();
   }

   public ConfigOption getElement(int var1) {
      return (ConfigOption)this.value.get(var1);
   }

   public ArrayConfigOption setValueVarArgs(Object... var1) {
      int var2 = var1.length;
      if (this.fixedSize > 0) {
         var2 = PZMath.min(var2, this.fixedSize);
      } else {
         this.resize(this.value, var1.length);
      }

      for(int var3 = 0; var3 < var2; ++var3) {
         ((ConfigOption)this.value.get(var3)).setValueFromObject(var1[var3]);
      }

      return this;
   }

   private void copyValue(ArrayList<ConfigOption> var1, ArrayList<ConfigOption> var2) {
      var2.clear();

      for(int var3 = 0; var3 < var1.size(); ++var3) {
         ConfigOption var4 = ((ConfigOption)var1.get(var3)).makeCopy();
         var2.add(var4);
      }

   }

   private String getValueAsString(ArrayList<ConfigOption> var1) {
      StringBuilder var2 = new StringBuilder();

      for(int var3 = 0; var3 < var1.size(); ++var3) {
         ConfigOption var4 = (ConfigOption)var1.get(var3);
         var2.append(var4.getValueAsString());
         if (var3 < var1.size() - 1) {
            var2.append(this.separator);
         }
      }

      return var2.toString();
   }

   private void resize(ArrayList<ConfigOption> var1, int var2) {
      this.fixedSize = var2;
      this.elementHandler.resetToDefault();

      for(int var3 = var1.size(); var3 < this.fixedSize; ++var3) {
         ConfigOption var4 = this.elementHandler.makeCopy();
         var1.add(var4);
      }

      while(var1.size() > this.fixedSize) {
         var1.remove(var1.size() - 1);
      }

   }

   public ColorInfo getValueAsColorInfo(ColorInfo var1) {
      return var1.set((float)this.getElementAsDouble(0, 1.0F), (float)this.getElementAsDouble(1, 1.0F), (float)this.getElementAsDouble(2, 1.0F), (float)this.getElementAsDouble(3, 1.0F));
   }

   public double getElementAsDouble(int var1, float var2) {
      return var1 >= 0 && var1 < this.value.size() && this.elementHandler instanceof DoubleConfigOption ? ((DoubleConfigOption)this.getElement(var1)).getValue() : (double)var2;
   }
}
