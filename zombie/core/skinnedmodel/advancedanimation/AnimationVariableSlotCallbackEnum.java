package zombie.core.skinnedmodel.advancedanimation;

import java.util.function.Consumer;
import java.util.function.Supplier;
import zombie.debug.DebugLog;

public class AnimationVariableSlotCallbackEnum<EnumType extends Enum<EnumType>> extends AnimationVariableSlotEnum<EnumType> {
   private final Supplier<EnumType> m_callbackGet;
   private final Consumer<EnumType> m_callbackSet;

   protected AnimationVariableSlotCallbackEnum(Class<EnumType> var1, String var2, EnumType var3, Supplier<EnumType> var4) {
      this(var1, var2, var3, var4, (Consumer)null);
   }

   protected AnimationVariableSlotCallbackEnum(Class<EnumType> var1, String var2, EnumType var3, Supplier<EnumType> var4, Consumer<EnumType> var5) {
      super(var1, var2, var3);
      this.m_callbackGet = var4;
      this.m_callbackSet = var5;
   }

   public EnumType getValue() {
      return (Enum)this.m_callbackGet.get();
   }

   public void setValue(EnumType var1) {
      this.trySetValue(var1);
   }

   public boolean trySetValue(EnumType var1) {
      if (this.isReadOnly()) {
         DebugLog.General.warn("Trying to set read-only variable \"%s\"", super.getKey());
         return false;
      } else {
         this.m_callbackSet.accept(var1);
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
}
