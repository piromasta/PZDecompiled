package zombie.characters.action.conditions;

import org.w3c.dom.Element;
import zombie.characters.action.ActionContext;
import zombie.characters.action.IActionCondition;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableReference;
import zombie.core.skinnedmodel.advancedanimation.IAnimatable;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSlot;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSource;
import zombie.util.StringUtils;

public final class CharacterVariableCondition implements IActionCondition {
   private Operator op;
   private Object lhsValue;
   private Object rhsValue;

   public CharacterVariableCondition() {
   }

   private static Object parseValue(String var0, boolean var1) {
      if (var0.length() <= 0) {
         return var0;
      } else {
         char var2 = var0.charAt(0);
         int var4;
         char var5;
         if (var2 != '-' && var2 != '+' && (var2 < '0' || var2 > '9')) {
            if (!var0.equalsIgnoreCase("true") && !var0.equalsIgnoreCase("yes")) {
               if (!var0.equalsIgnoreCase("false") && !var0.equalsIgnoreCase("no")) {
                  if (var1) {
                     if (var2 != '\'' && var2 != '"') {
                        return new CharacterVariableLookup(var0);
                     } else {
                        StringBuilder var8 = new StringBuilder(var0.length() - 2);

                        for(var4 = 1; var4 < var0.length(); ++var4) {
                           var5 = var0.charAt(var4);
                           switch (var5) {
                              case '"':
                              case '\'':
                                 if (var5 == var2) {
                                    return var8.toString();
                                 }
                              default:
                                 var8.append(var5);
                                 break;
                              case '\\':
                                 var8.append(var0.charAt(var4));
                           }
                        }

                        return var8.toString();
                     }
                  } else {
                     return var0;
                  }
               } else {
                  return false;
               }
            } else {
               return true;
            }
         } else {
            int var3 = 0;
            if (var2 >= '0' && var2 <= '9') {
               var3 = var2 - 48;
            }

            for(var4 = 1; var4 < var0.length(); ++var4) {
               var5 = var0.charAt(var4);
               if (var5 >= '0' && var5 <= '9') {
                  var3 = var3 * 10 + (var5 - 48);
               } else if (var5 != ',') {
                  if (var5 != '.') {
                     return var0;
                  }

                  ++var4;
                  break;
               }
            }

            if (var4 == var0.length()) {
               return var3;
            } else {
               float var9 = (float)var3;

               for(float var6 = 10.0F; var4 < var0.length(); ++var4) {
                  char var7 = var0.charAt(var4);
                  if (var7 >= '0' && var7 <= '9') {
                     var9 += (float)(var7 - 48) / var6;
                     var6 *= 10.0F;
                  } else if (var7 != ',') {
                     return var0;
                  }
               }

               if (var2 == '-') {
                  var9 *= -1.0F;
               }

               return var9;
            }
         }
      }
   }

   private boolean load(Element var1) {
      switch (var1.getNodeName()) {
         case "isTrue":
            this.op = CharacterVariableCondition.Operator.Equal;
            this.lhsValue = new CharacterVariableLookup(var1.getTextContent().trim());
            this.rhsValue = true;
            return true;
         case "isFalse":
            this.op = CharacterVariableCondition.Operator.Equal;
            this.lhsValue = new CharacterVariableLookup(var1.getTextContent().trim());
            this.rhsValue = false;
            return true;
         case "compare":
            switch (var1.getAttribute("op").trim()) {
               case "=":
               case "==":
                  this.op = CharacterVariableCondition.Operator.Equal;
                  break;
               case "!=":
               case "<>":
                  this.op = CharacterVariableCondition.Operator.NotEqual;
                  break;
               case "<":
                  this.op = CharacterVariableCondition.Operator.Less;
                  break;
               case ">":
                  this.op = CharacterVariableCondition.Operator.Greater;
                  break;
               case "<=":
                  this.op = CharacterVariableCondition.Operator.LessEqual;
                  break;
               case ">=":
                  this.op = CharacterVariableCondition.Operator.GreaterEqual;
                  break;
               default:
                  return false;
            }

            this.loadCompareValues(var1);
            return true;
         case "gtr":
            this.op = CharacterVariableCondition.Operator.Greater;
            this.loadCompareValues(var1);
            return true;
         case "less":
            this.op = CharacterVariableCondition.Operator.Less;
            this.loadCompareValues(var1);
            return true;
         case "equals":
            this.op = CharacterVariableCondition.Operator.Equal;
            this.loadCompareValues(var1);
            return true;
         case "notEquals":
            this.op = CharacterVariableCondition.Operator.NotEqual;
            this.loadCompareValues(var1);
            return true;
         case "lessEqual":
            this.op = CharacterVariableCondition.Operator.LessEqual;
            this.loadCompareValues(var1);
            return true;
         case "gtrEqual":
            this.op = CharacterVariableCondition.Operator.GreaterEqual;
            this.loadCompareValues(var1);
            return true;
         default:
            return false;
      }
   }

   private void loadCompareValues(Element var1) {
      String var2 = var1.getAttribute("a").trim();
      String var3 = var1.getAttribute("b").trim();
      this.lhsValue = parseValue(var2, true);
      this.rhsValue = parseValue(var3, false);
   }

   private static Object resolveValue(Object var0, IAnimationVariableSource var1) {
      if (var0 instanceof CharacterVariableLookup var2) {
         String var3 = var2.getValueString(var1);
         return var3 != null ? parseValue(var3, false) : null;
      } else {
         return var0;
      }
   }

   private boolean resolveCompareTo(int var1) {
      switch (this.op) {
         case Equal:
            return var1 == 0;
         case NotEqual:
            return var1 != 0;
         case Less:
            return var1 < 0;
         case LessEqual:
            return var1 <= 0;
         case Greater:
            return var1 > 0;
         case GreaterEqual:
            return var1 >= 0;
         default:
            return false;
      }
   }

   public boolean passes(ActionContext var1, int var2) {
      IAnimatable var3 = var1.getOwner();
      Object var4 = resolveValue(this.lhsValue, var3);
      Object var5 = resolveValue(this.rhsValue, var3);
      boolean var6;
      if (var4 == null && var5 instanceof String && StringUtils.isNullOrEmpty((String)var5)) {
         if (this.op == CharacterVariableCondition.Operator.Equal) {
            return true;
         }

         if (this.op == CharacterVariableCondition.Operator.NotEqual) {
            return false;
         }

         var6 = true;
      }

      if (var4 != null && var5 != null) {
         if (var4.getClass().equals(var5.getClass())) {
            if (var4 instanceof String) {
               return this.resolveCompareTo(((String)var4).compareTo((String)var5));
            }

            if (var4 instanceof Integer) {
               return this.resolveCompareTo(((Integer)var4).compareTo((Integer)var5));
            }

            if (var4 instanceof Float) {
               return this.resolveCompareTo(((Float)var4).compareTo((Float)var5));
            }

            if (var4 instanceof Boolean) {
               return this.resolveCompareTo(((Boolean)var4).compareTo((Boolean)var5));
            }
         }

         var6 = var4 instanceof Integer;
         boolean var7 = var4 instanceof Float;
         boolean var8 = var5 instanceof Integer;
         boolean var9 = var5 instanceof Float;
         if ((var6 || var7) && (var8 || var9)) {
            boolean var10 = this.lhsValue instanceof CharacterVariableLookup;
            boolean var11 = this.rhsValue instanceof CharacterVariableLookup;
            float var14;
            float var15;
            if (var10 == var11) {
               var14 = var7 ? (Float)var4 : (float)(Integer)var4;
               var15 = var9 ? (Float)var5 : (float)(Integer)var5;
               return this.resolveCompareTo(Float.compare(var14, var15));
            } else {
               int var12;
               int var13;
               if (var10) {
                  if (var9) {
                     var14 = var7 ? (Float)var4 : (float)(Integer)var4;
                     var15 = (Float)var5;
                     return this.resolveCompareTo(Float.compare(var14, var15));
                  } else {
                     var12 = var7 ? (int)(Float)var4 : (Integer)var4;
                     var13 = (Integer)var5;
                     return this.resolveCompareTo(Integer.compare(var12, var13));
                  }
               } else if (var7) {
                  var14 = (Float)var4;
                  var15 = var9 ? (Float)var5 : (float)(Integer)var5;
                  return this.resolveCompareTo(Float.compare(var14, var15));
               } else {
                  var12 = (Integer)var4;
                  var13 = var9 ? (int)(Float)var5 : (Integer)var5;
                  return this.resolveCompareTo(Integer.compare(var12, var13));
               }
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public IActionCondition clone() {
      return this;
   }

   private static String getOpString(Operator var0) {
      switch (var0) {
         case Equal:
            return " == ";
         case NotEqual:
            return " != ";
         case Less:
            return " < ";
         case LessEqual:
            return " <= ";
         case Greater:
            return " > ";
         case GreaterEqual:
            return " >=";
         default:
            return " ?? ";
      }
   }

   private static String valueToString(Object var0) {
      return var0 instanceof String ? "\"" + (String)var0 + "\"" : var0.toString();
   }

   public String getDescription() {
      String var10000 = valueToString(this.lhsValue);
      return var10000 + getOpString(this.op) + valueToString(this.rhsValue);
   }

   public String toString() {
      return this.toString("");
   }

   public String toString(String var1) {
      return var1 + this.getClass().getName() + "{ " + this.getDescription() + " }";
   }

   private static class CharacterVariableLookup {
      private final AnimationVariableReference m_variableReference;

      public CharacterVariableLookup(String var1) {
         this.m_variableReference = AnimationVariableReference.fromRawVariableName(var1);
      }

      public String getValueString(IAnimationVariableSource var1) {
         IAnimationVariableSlot var2 = this.m_variableReference.getVariable(var1);
         if (var2 == null) {
            return null;
         } else {
            String var3 = var2.getValueString();
            return var3;
         }
      }

      public String toString() {
         return this.m_variableReference.toString();
      }
   }

   static enum Operator {
      Equal,
      NotEqual,
      Less,
      Greater,
      LessEqual,
      GreaterEqual;

      private Operator() {
      }
   }

   public static class Factory implements IActionCondition.IFactory {
      public Factory() {
      }

      public IActionCondition create(Element var1) {
         CharacterVariableCondition var2 = new CharacterVariableCondition();
         return var2.load(var1) ? var2 : null;
      }
   }
}
