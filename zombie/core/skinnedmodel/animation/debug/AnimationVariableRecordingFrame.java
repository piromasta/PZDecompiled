package zombie.core.skinnedmodel.animation.debug;

import java.util.Iterator;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableType;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSlot;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSource;
import zombie.debug.DebugLog;
import zombie.iso.Vector2;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

public final class AnimationVariableRecordingFrame extends GenericNameValueRecordingFrame {
   private String[] m_variableValues = new String[0];
   private AnimationVariableType[] m_variableTypes = new AnimationVariableType[0];
   private final Vector2 m_deferredMovement = new Vector2();
   private final Vector2 m_deferredMovementFromRagdoll = new Vector2();

   public AnimationVariableRecordingFrame(String var1) {
      super(var1, "_values");
   }

   public void logVariables(IAnimationVariableSource var1) {
      Iterator var2 = var1.getGameVariables().iterator();

      while(var2.hasNext()) {
         IAnimationVariableSlot var3 = (IAnimationVariableSlot)var2.next();
         this.logVariable(var3);
      }

   }

   protected void onColumnAdded() {
      this.m_variableValues = (String[])PZArrayUtil.add(this.m_variableValues, (Object)null);
      this.m_variableTypes = (AnimationVariableType[])PZArrayUtil.add(this.m_variableTypes, AnimationVariableType.Void);
   }

   public void logVariable(IAnimationVariableSlot var1) {
      String var2 = var1.getKey();
      AnimationVariableType var3 = var1.getType();
      switch (var3) {
         case Void:
            this.logVariable(var2, var1.getValueString());
            break;
         case String:
            this.logVariable(var2, var1.getValueString());
            break;
         case Float:
            this.logVariable(var2, var1.getValueFloat());
            break;
         case Boolean:
            this.logVariable(var2, var1.getValueBool());
      }

   }

   public void logVariable(String var1, String var2) {
      int var3 = this.getOrCreateColumn(var1);
      if (this.m_variableValues[var3] != null) {
         DebugLog.General.error("Value for %s already set: %s, new value: %s", var1, this.m_variableValues[var3], var2);
      }

      this.m_variableValues[var3] = var2;
      AnimationVariableType var4 = this.m_variableTypes[var3];
      AnimationVariableType var5 = this.checkCellType(var4, var2);
      this.setCellType(var3, var5);
   }

   public void logVariable(String var1, float var2) {
      int var3 = this.getOrCreateColumn(var1);
      if (this.m_variableValues[var3] != null) {
         DebugLog.General.error("Value for %s already set: %s, new value: %f", var1, this.m_variableValues[var3], var2);
      }

      this.m_variableValues[var3] = String.valueOf(var2);
      this.setCellType(var3, AnimationVariableType.Float);
   }

   public void logVariable(String var1, boolean var2) {
      int var3 = this.getOrCreateColumn(var1);
      if (this.m_variableValues[var3] != null) {
         DebugLog.General.error("Value for %s already set: %s, new value: %s", var1, this.m_variableValues[var3], var2 ? "1" : "0");
      }

      this.m_variableValues[var3] = var2 ? "1" : "0";
      this.setCellType(var3, AnimationVariableType.Boolean);
   }

   private void setCellType(int var1, AnimationVariableType var2) {
      AnimationVariableType var3 = this.m_variableTypes[var1];
      if (var3 != var2) {
         this.m_variableTypes[var1] = var2;
         this.m_headerDirty = true;
      }

   }

   private AnimationVariableType checkCellType(AnimationVariableType var1, String var2) {
      AnimationVariableType var3 = var1;
      if (var1 != null && var1 != AnimationVariableType.Void) {
         if (var1 == AnimationVariableType.String) {
            return var1;
         } else {
            boolean var4;
            if (var1 == AnimationVariableType.Float) {
               var4 = StringUtils.isNullOrWhitespace(var2) || StringUtils.isFloat(var2);
               if (!var4) {
                  var3 = AnimationVariableType.String;
               }

               return var3;
            } else if (var1 != AnimationVariableType.Boolean) {
               return var1;
            } else {
               var4 = StringUtils.isNullOrWhitespace(var2) || StringUtils.isBoolean(var2);
               if (!var4) {
                  var3 = AnimationVariableType.String;
               }

               return var3;
            }
         }
      } else if (StringUtils.isNullOrWhitespace(var2)) {
         return var1;
      } else {
         if (StringUtils.isFloat(var2)) {
            var3 = AnimationVariableType.Float;
         } else if (StringUtils.isBoolean(var2)) {
            var3 = AnimationVariableType.Boolean;
         } else {
            var3 = AnimationVariableType.String;
         }

         return var3;
      }
   }

   public String getValueAt(int var1) {
      return this.m_variableValues[var1];
   }

   public void reset() {
      int var1 = 0;

      for(int var2 = this.m_variableValues.length; var1 < var2; ++var1) {
         this.m_variableValues[var1] = null;
      }

      this.m_deferredMovement.set(0.0F, 0.0F);
      this.m_deferredMovementFromRagdoll.set(0.0F, 0.0F);
   }

   protected void writeHeaderToMemory() {
      super.writeHeaderToMemory();
      StringBuilder var1 = new StringBuilder();

      for(int var2 = 0; var2 < this.m_variableTypes.length; ++var2) {
         if (var2 > 0) {
            var1.append(",");
         }

         if (this.m_variableTypes[var2] == null) {
            var1.append("String");
         } else {
            var1.append(this.m_variableTypes[var2]);
         }
      }

      this.m_outHeader.println(var1);
   }

   public void logDeferredMovement(Vector2 var1, Vector2 var2) {
      this.m_deferredMovement.set(var1);
      this.m_deferredMovementFromRagdoll.set(var2);
      this.logVariable("dm.x", this.m_deferredMovement.x);
      this.logVariable("dm.y", this.m_deferredMovement.y);
      this.logVariable("dmrd.x", this.m_deferredMovementFromRagdoll.x);
      this.logVariable("dmrd.y", this.m_deferredMovementFromRagdoll.y);
   }
}
