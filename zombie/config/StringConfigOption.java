package zombie.config;

import java.util.ArrayList;
import java.util.List;
import zombie.debug.DebugLog;

public class StringConfigOption extends ConfigOption {
   protected String value;
   protected String defaultValue;
   protected int maxLength;
   protected String[] values;

   public StringConfigOption(String var1, String var2, int var3) {
      super(var1);
      if (var2 == null) {
         var2 = "";
      }

      this.value = var2;
      this.defaultValue = var2;
      this.maxLength = var3;
   }

   public StringConfigOption(String var1, String var2, String[] var3) {
      super(var1);

      for(int var4 = 0; var4 < var3.length; ++var4) {
         if (var3[var4].equals(var2)) {
            this.value = var2;
            break;
         }
      }

      if (this.value == null) {
         this.value = var2 = var3[0];
      }

      this.defaultValue = var2;
      this.values = var3;
   }

   public String getType() {
      return "string";
   }

   public void resetToDefault() {
      this.value = this.defaultValue;
   }

   public void setDefaultToCurrentValue() {
      this.defaultValue = this.value;
   }

   public void parse(String var1) {
      this.setValueFromObject(var1);
   }

   public String getValueAsString() {
      return this.value;
   }

   public String getValueAsLuaString() {
      return String.format("\"%s\"", this.value.replace("\\", "\\\\").replace("\"", "\\\""));
   }

   public void setValueFromObject(Object var1) {
      if (this.values != null) {
         for(int var2 = 0; var2 < this.values.length; ++var2) {
            if (this.values[var2].equals(var1)) {
               this.value = this.values[var2];
               return;
            }
         }

         DebugLog.General.println("ERROR: StringConfigOption.setValueFromObject() \"%s\" value \"%s\" is unknown", this.getName(), var1);
      } else {
         if (var1 == null) {
            this.value = "";
         } else if (var1 instanceof String) {
            this.value = (String)var1;
         } else {
            this.value = var1.toString();
         }

      }
   }

   public Object getValueAsObject() {
      return this.value;
   }

   public boolean isValidString(String var1) {
      if (this.values != null) {
         for(int var2 = 0; var2 < this.values.length; ++var2) {
            if (this.values[var2].equals(var1)) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   public void setValue(String var1) {
      if (this.values != null) {
         if (this.isValidString(var1)) {
            this.value = var1;
         } else {
            DebugLog.General.println("ERROR StringConfigOption.setValue() \"%s\" string=\"%s\"", this.getName(), var1);
         }

      } else {
         if (var1 == null) {
            var1 = "";
         }

         if (this.maxLength > 0 && var1.length() > this.maxLength) {
            var1 = var1.substring(0, this.maxLength);
         }

         this.value = var1;
      }
   }

   public String getValue() {
      return this.value;
   }

   public String getDefaultValue() {
      return this.defaultValue;
   }

   public String getTooltip() {
      return this.value;
   }

   public ConfigOption makeCopy() {
      StringConfigOption var1;
      if (this.values == null) {
         var1 = new StringConfigOption(this.name, this.defaultValue, this.maxLength);
         var1.value = this.value;
         return var1;
      } else {
         var1 = new StringConfigOption(this.name, this.defaultValue, this.values);
         var1.value = this.value;
         return var1;
      }
   }

   public ArrayList<String> getSplitCSVList() {
      return new ArrayList(List.of(this.value.replaceAll("\\s+", "").split(",")));
   }
}
