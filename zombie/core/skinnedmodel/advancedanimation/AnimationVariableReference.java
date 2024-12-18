package zombie.core.skinnedmodel.advancedanimation;

import zombie.core.Core;
import zombie.core.skinnedmodel.advancedanimation.debug.AnimatorDebugMonitor;
import zombie.debug.DebugLog;
import zombie.util.StringUtils;

public class AnimationVariableReference {
   private String m_SubVariableSourceName;
   private String m_Name;
   private AnimationVariableHandle m_variableHandle;

   private AnimationVariableReference() {
   }

   public boolean equals(AnimationVariableReference var1) {
      return var1 != null && StringUtils.equalsIgnoreCase(this.m_SubVariableSourceName, var1.m_SubVariableSourceName) && StringUtils.equalsIgnoreCase(this.m_Name, var1.m_Name) && AnimationVariableHandle.equals(this.getVariableHandle(), var1.getVariableHandle());
   }

   public String getName() {
      return this.m_Name;
   }

   public String getSubVariableSourceName() {
      return this.m_SubVariableSourceName;
   }

   private void parse() {
      String var1 = this.m_Name;
      if (var1.startsWith("$")) {
         if (var1.indexOf(46) > 1) {
            int var2 = var1.indexOf(46);
            String var3 = var1.substring(1, var2);
            String var4 = var1.substring(var2 + 1);
            if (StringUtils.isNullOrWhitespace(var4)) {
               DebugLog.Animation.warn("Error parsing: %s", var1);
               DebugLog.Animation.warn("  SubVariableName not specified.");
               DebugLog.Animation.warn("  Expected: $<subVariableSource>.<subVariableName>");
            } else if (!StringUtils.isValidVariableName(var3)) {
               DebugLog.Animation.warn("Error parsing: %s", var1);
               DebugLog.Animation.warn("  SubVariableSource name not valid. Only AlphaNumeric or underscores '_' allowed.");
            } else {
               this.m_SubVariableSourceName = var3;
               this.m_Name = var4;
            }
         }
      }
   }

   public static AnimationVariableReference fromRawVariableName(String var0) {
      if (Core.bDebug) {
         AnimatorDebugMonitor.registerVariable(var0);
      }

      AnimationVariableReference var1 = new AnimationVariableReference();
      var1.m_Name = var0;
      var1.parse();
      return var1;
   }

   public IAnimationVariableSlot getVariable(IAnimationVariableSource var1) {
      if (this.getName().isBlank()) {
         return null;
      } else {
         AnimationVariableHandle var2 = this.getVariableHandle();
         IAnimationVariableSource var3 = this.getAnimationVariableSource(var1);
         if (var3 == null) {
            return null;
         } else {
            IAnimationVariableSlot var4 = var3.getVariable(var2);
            return var4;
         }
      }
   }

   private AnimationVariableHandle getVariableHandle() {
      if (this.m_variableHandle == null) {
         this.m_variableHandle = AnimationVariableHandle.alloc(this.m_Name);
      }

      return this.m_variableHandle;
   }

   private IAnimationVariableSource getAnimationVariableSource(IAnimationVariableSource var1) {
      if (this.m_SubVariableSourceName != null) {
         IAnimationVariableSource var2 = var1.getSubVariableSource(this.m_SubVariableSourceName);
         if (var2 == null) {
            DebugLog.Animation.warnOnce("SubVariableSource name \"%s\" does not exist in %s", this.m_SubVariableSourceName, var1);
         }

         return var2;
      } else {
         return var1;
      }
   }

   public boolean isSubVariableSourceReference() {
      return this.m_SubVariableSourceName != null;
   }

   public void setVariable(IAnimationVariableSource var1, String var2) {
      if (this.getName().isBlank()) {
         DebugLog.General.warnOnce("Variable name is blank. Cannot set.");
      } else {
         IAnimationVariableSlot var3 = this.getVariable(var1);
         if (var3 != null) {
            var3.setValue(var2);
         } else {
            String var4 = this.getName();
            IAnimationVariableSource var5 = this.getAnimationVariableSource(var1);
            if (var5 instanceof IAnimationVariableMap) {
               ((IAnimationVariableMap)var5).setVariable(var4, var2);
            } else {
               DebugLog.General.warnOnce("Destination VariableSource is read-only. Cannot set %s.%s=%s", var5, var4, var2);
            }

         }
      }
   }

   public void setVariable(IAnimationVariableSource var1, boolean var2) {
      if (this.getName().isBlank()) {
         DebugLog.General.warnOnce("Variable name is blank. Cannot set.");
      } else {
         IAnimationVariableSlot var3 = this.getVariable(var1);
         if (var3 != null) {
            var3.setValue(var2);
         } else {
            String var4 = this.getName();
            IAnimationVariableSource var5 = this.getAnimationVariableSource(var1);
            if (var5 instanceof IAnimationVariableMap) {
               ((IAnimationVariableMap)var5).setVariable(var4, var2);
            } else {
               DebugLog.General.warnOnce("Destination VariableSource is read-only. Cannot set %s.%s=%s", var5, var4, var2);
            }

         }
      }
   }

   public void clearVariable(IAnimationVariableSource var1) {
      if (this.getName().isBlank()) {
         DebugLog.General.warnOnce("Variable name is blank. Cannot set.");
      } else {
         IAnimationVariableSlot var2 = this.getVariable(var1);
         if (var2 != null) {
            var2.clear();
         } else {
            String var3 = this.getName();
            IAnimationVariableSource var4 = this.getAnimationVariableSource(var1);
            if (var4 instanceof IAnimationVariableMap) {
               ((IAnimationVariableMap)var4).clearVariable(var3);
            } else {
               DebugLog.General.warnOnce("Destination VariableSource is read-only. Cannot clear variable %s.%s", var4, var3);
            }

         }
      }
   }

   public String toString() {
      String var10000 = this.getClass().getSimpleName();
      return var10000 + "{" + (this.isSubVariableSourceReference() ? " sourceName:" + this.getSubVariableSourceName() + ", " : "") + " variableName:" + this.getName() + " }";
   }
}
