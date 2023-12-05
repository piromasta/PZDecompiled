package zombie.text.templating;

import java.util.ArrayList;

public class ReplaceList implements IReplace {
   private final ArrayList<String> replacements;

   public ReplaceList() {
      this.replacements = new ArrayList();
   }

   public ReplaceList(ArrayList<String> var1) {
      this.replacements = var1;
   }

   protected ArrayList<String> getReplacements() {
      return this.replacements;
   }

   public String getString() {
      return this.replacements.size() == 0 ? "!ERROR_EMPTY_LIST!" : (String)this.replacements.get(TemplateText.RandNext(this.replacements.size()));
   }
}
