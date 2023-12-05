package zombie.core.skinnedmodel.advancedanimation;

import zombie.debug.DebugLog;

public abstract class AnimationVariableSlotCallback<VariableType> extends AnimationVariableSlot {
   private final CallbackGet<VariableType> m_callbackGet;
   private final CallbackSet<VariableType> m_callbackSet;

   protected AnimationVariableSlotCallback(String var1, CallbackGet<VariableType> var2) {
      this(var1, var2, (CallbackSet)null);
   }

   protected AnimationVariableSlotCallback(String var1, CallbackGet<VariableType> var2, CallbackSet<VariableType> var3) {
      super(var1);
      this.m_callbackGet = var2;
      this.m_callbackSet = var3;
   }

   public VariableType getValue() {
      return this.m_callbackGet.call();
   }

   public abstract VariableType getDefaultValue();

   public boolean trySetValue(VariableType var1) {
      if (this.isReadOnly()) {
         DebugLog.General.warn("Trying to set read-only variable \"%s\"", super.getKey());
         return false;
      } else {
         this.m_callbackSet.call(var1);
         return true;
      }
   }

   public boolean isReadOnly() {
      return this.m_callbackSet == null;
   }

   public void clear() {
      if (!this.isReadOnly()) {
         this.trySetValue(this.getDefaultValue());
      }

   }

   public interface CallbackGet<VariableType> {
      VariableType call();
   }

   public interface CallbackSet<VariableType> {
      void call(VariableType var1);
   }
}
