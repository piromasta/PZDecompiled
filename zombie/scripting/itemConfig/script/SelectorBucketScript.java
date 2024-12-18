package zombie.scripting.itemConfig.script;

import java.util.ArrayList;
import java.util.Iterator;
import zombie.scripting.itemConfig.enums.SelectorType;
import zombie.scripting.itemConfig.enums.SituatedType;

public class SelectorBucketScript {
   protected final ArrayList<SelectorBucketScript> children = new ArrayList();
   private final SelectorType selectorType;
   protected String selectorString;
   protected SituatedType selectorSituated;
   protected int selectorWorldAge;
   protected final ArrayList<String> randomizers = new ArrayList();

   public SelectorType getSelectorType() {
      return this.selectorType;
   }

   public String getSelectorString() {
      return this.selectorString;
   }

   public SituatedType getSelectorSituated() {
      return this.selectorSituated;
   }

   public int getSelectorWorldAge() {
      return this.selectorWorldAge;
   }

   public ArrayList<String> getRandomizers() {
      return this.randomizers;
   }

   public ArrayList<SelectorBucketScript> getChildren() {
      return this.children;
   }

   protected SelectorBucketScript(SelectorType var1) {
      this.selectorType = var1;
   }

   protected SelectorBucketScript copy() {
      SelectorBucketScript var1 = new SelectorBucketScript(this.selectorType);
      var1.selectorString = this.selectorString;
      var1.selectorSituated = this.selectorSituated;
      var1.selectorWorldAge = this.selectorWorldAge;
      Iterator var2 = this.children.iterator();

      while(var2.hasNext()) {
         SelectorBucketScript var3 = (SelectorBucketScript)var2.next();
         var1.children.add(var3.copy());
      }

      var2 = this.randomizers.iterator();

      while(var2.hasNext()) {
         String var4 = (String)var2.next();
         var1.randomizers.add(var4);
      }

      return var1;
   }
}
