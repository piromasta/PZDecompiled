package zombie.core.skinnedmodel.advancedanimation;

import zombie.util.StringUtils;

public class AnimationVariableHandle {
   private String m_name = null;
   private int m_varIndex = -1;

   AnimationVariableHandle() {
   }

   public static boolean equals(AnimationVariableHandle var0, AnimationVariableHandle var1) {
      if (var0 == var1) {
         return true;
      } else {
         return var0 != null && var0.equals(var1);
      }
   }

   public boolean equals(AnimationVariableHandle var1) {
      return var1 != null && StringUtils.equalsIgnoreCase(this.m_name, var1.m_name) && this.m_varIndex == var1.m_varIndex;
   }

   public static AnimationVariableHandle alloc(String var0) {
      return AnimationVariableHandlePool.getOrCreate(var0);
   }

   public String getVariableName() {
      return this.m_name;
   }

   public int getVariableIndex() {
      return this.m_varIndex;
   }

   void setVariableName(String var1) {
      this.m_name = var1;
   }

   void setVariableIndex(int var1) {
      this.m_varIndex = var1;
   }

   public String toString() {
      String var10000 = this.getClass().getSimpleName();
      return var10000 + "{  variableName:" + this.m_name + ",  variableIndex:" + this.m_varIndex + " }";
   }
}
