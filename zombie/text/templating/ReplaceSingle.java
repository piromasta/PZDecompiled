package zombie.text.templating;

public class ReplaceSingle implements IReplace {
   private String value = "";

   public ReplaceSingle() {
   }

   public ReplaceSingle(String var1) {
      this.value = var1;
   }

   protected String getValue() {
      return this.value;
   }

   protected void setValue(String var1) {
      this.value = var1;
   }

   public String getString() {
      return this.value;
   }
}
