package zombie.characters.action.conditions;

import org.w3c.dom.Element;
import zombie.characters.action.ActionContext;
import zombie.characters.action.IActionCondition;

public final class EventOccurred implements IActionCondition {
   public String eventName;

   public EventOccurred() {
   }

   public String getDescription() {
      return "EventOccurred(" + this.eventName + ")";
   }

   private boolean load(Element var1) {
      this.eventName = var1.getTextContent().toLowerCase();
      return true;
   }

   public boolean passes(ActionContext var1, int var2) {
      return var1.hasEventOccurred(this.eventName, var2);
   }

   public IActionCondition clone() {
      return null;
   }

   public String toString() {
      return this.toString("");
   }

   public String toString(String var1) {
      return var1 + this.getClass().getName() + "{ " + this.eventName + " }";
   }

   public static class Factory implements IActionCondition.IFactory {
      public Factory() {
      }

      public IActionCondition create(Element var1) {
         EventOccurred var2 = new EventOccurred();
         return var2.load(var1) ? var2 : null;
      }
   }
}
