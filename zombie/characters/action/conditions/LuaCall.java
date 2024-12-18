package zombie.characters.action.conditions;

import org.w3c.dom.Element;
import zombie.characters.action.ActionContext;
import zombie.characters.action.IActionCondition;

public final class LuaCall implements IActionCondition {
   public LuaCall() {
   }

   public String getDescription() {
      return "<luaCheck>";
   }

   public boolean passes(ActionContext var1, int var2) {
      return false;
   }

   public IActionCondition clone() {
      return new LuaCall();
   }

   public String toString() {
      return this.toString("");
   }

   public String toString(String var1) {
      return var1 + this.getClass().getName();
   }

   public static class Factory implements IActionCondition.IFactory {
      public Factory() {
      }

      public IActionCondition create(Element var1) {
         return new LuaCall();
      }
   }
}
