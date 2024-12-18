package zombie.core.skinnedmodel.advancedanimation;

import javax.xml.bind.annotation.XmlTransient;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.util.StringUtils;

public final class AnimCondition {
   public String m_Name = "";
   public Type m_Type;
   public String m_Value;
   public float m_FloatValue;
   public boolean m_BoolValue;
   public String m_StringValue;
   @XmlTransient
   AnimationVariableReference m_variableReference;

   public AnimCondition() {
      this.m_Type = AnimCondition.Type.STRING;
      this.m_Value = "";
      this.m_FloatValue = 0.0F;
      this.m_BoolValue = false;
      this.m_StringValue = "";
      this.m_variableReference = null;
   }

   public void parse(AnimNode var1, AnimNode var2) {
      this.parseValue();
      this.m_variableReference = AnimationVariableReference.fromRawVariableName(this.m_Name);
      if (this.isTypeString()) {
         if (this.m_StringValue.contains("$this")) {
            this.m_StringValue = this.m_StringValue.replaceAll("\\$this", var1.m_Name);
         }

         if (this.m_StringValue.contains("$source")) {
            this.m_StringValue = this.m_StringValue.replaceAll("\\$source", var1.m_Name);
         }

         if (this.m_StringValue.contains("$target")) {
            if (var2 != null) {
               this.m_StringValue = this.m_StringValue.replaceAll("\\$target", var2.m_Name);
            } else {
               DebugLog.Animation.error("$target not supported in conditions that have no toNode specified. Only allowed in AnimTransition. FromNode: %s, ToNode: %s", var1, var2);
            }
         }
      }

   }

   public void parseValue() {
      if (!this.m_Value.isEmpty()) {
         Type var1 = this.m_Type;
         switch (var1) {
            case STRING:
            case STRNEQ:
               this.m_StringValue = this.m_Value;
               break;
            case BOOL:
               this.m_BoolValue = StringUtils.tryParseBoolean(this.m_Value);
               break;
            case EQU:
            case NEQ:
            case LESS:
            case GTR:
            case ABSLESS:
            case ABSGTR:
               this.m_FloatValue = StringUtils.tryParseFloat(this.m_Value);
            case OR:
         }

      }
   }

   public String toString() {
      return String.format("AnimCondition{name:%s type:%s value:%s }", this.m_Name, this.m_Type.toString(), this.getValueString());
   }

   public String getConditionString() {
      return this.m_Type == AnimCondition.Type.OR ? "OR" : String.format("( %s %s %s )", this.m_Name, this.m_Type.toString(), this.getValueString());
   }

   public String getValueString() {
      switch (this.m_Type) {
         case STRING:
         case STRNEQ:
            return this.m_StringValue;
         case BOOL:
            return this.m_BoolValue ? "true" : "false";
         case EQU:
         case NEQ:
         case LESS:
         case GTR:
         case ABSLESS:
         case ABSGTR:
            return String.valueOf(this.m_FloatValue);
         case OR:
            return " -- OR -- ";
         default:
            throw new RuntimeException("Unexpected internal type:" + this.m_Type);
      }
   }

   public boolean isTypeString() {
      return this.m_Type == AnimCondition.Type.STRING || this.m_Type == AnimCondition.Type.STRNEQ;
   }

   public boolean check(IAnimationVariableSource var1) {
      Type var2 = this.m_Type;
      if (var2 == AnimCondition.Type.OR) {
         return false;
      } else {
         IAnimationVariableSlot var3 = this.m_variableReference.getVariable(var1);
         if (var3 == null) {
            switch (var2) {
               case STRING:
                  return this.m_StringValue.equalsIgnoreCase("");
               case STRNEQ:
                  return !this.m_StringValue.equalsIgnoreCase("");
               case BOOL:
                  return !this.m_BoolValue;
               case EQU:
               case NEQ:
               case LESS:
               case GTR:
               case ABSLESS:
               case ABSGTR:
                  DebugLog.Animation.warnOnce("Variable \"%s\" not found in %s", this.m_variableReference, var1);
                  return false;
               case OR:
                  return false;
            }
         }

         switch (var2) {
            case STRING:
               return this.m_StringValue.equalsIgnoreCase(var3.getValueString());
            case STRNEQ:
               return !this.m_StringValue.equalsIgnoreCase(var3.getValueString());
            case BOOL:
               return var3.getValueBool() == this.m_BoolValue;
            case EQU:
               return this.m_FloatValue == var3.getValueFloat();
            case NEQ:
               return this.m_FloatValue != var3.getValueFloat();
            case LESS:
               return var3.getValueFloat() < this.m_FloatValue;
            case GTR:
               return var3.getValueFloat() > this.m_FloatValue;
            case ABSLESS:
               return PZMath.abs(var3.getValueFloat()) < this.m_FloatValue;
            case ABSGTR:
               return PZMath.abs(var3.getValueFloat()) > this.m_FloatValue;
            case OR:
               return false;
            default:
               throw new RuntimeException("Unexpected internal type:" + this.m_Type);
         }
      }
   }

   public static boolean pass(IAnimationVariableSource var0, AnimCondition[] var1) {
      boolean var2 = true;
      AnimCondition[] var3 = var1;
      int var4 = var1.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         AnimCondition var6 = var3[var5];
         if (var6.m_Type == AnimCondition.Type.OR) {
            if (var2) {
               break;
            }

            var2 = true;
         } else {
            var2 = var2 && var6.check(var0);
         }
      }

      return var2;
   }

   public static enum Type {
      STRING,
      STRNEQ,
      BOOL,
      EQU,
      NEQ,
      LESS,
      GTR,
      ABSLESS,
      ABSGTR,
      OR;

      private Type() {
      }
   }
}
