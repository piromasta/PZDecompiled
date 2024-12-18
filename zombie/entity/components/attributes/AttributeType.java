package zombie.entity.components.attributes;

import java.util.Objects;
import zombie.core.Translator;
import zombie.debug.DebugLog;
import zombie.entity.util.enums.IOEnum;
import zombie.util.StringUtils;

public abstract class AttributeType {
   private static final int MAX_ID = 8128;
   private final short id;
   private final java.lang.String name;
   private final java.lang.String translateKey;
   private final java.lang.String tooltipOverride;
   private final Attribute.UI.Display optionDisplay;
   private final Attribute.UI.DisplayAsBar optionDisplayAsBar;
   private final boolean readOnly;

   protected AttributeType(short var1, java.lang.String var2, boolean var3, Attribute.UI.Display var4, java.lang.String var5) {
      this(var1, var2, var3, var4, Attribute.UI.DisplayAsBar.Default, var5);
   }

   protected AttributeType(short var1, java.lang.String var2, boolean var3, Attribute.UI.Display var4, Attribute.UI.DisplayAsBar var5, java.lang.String var6) {
      if (var1 >= 0 && var1 <= 8128) {
         if (var2 == null) {
            throw new RuntimeException("AttributeType name cannot be null.");
         } else {
            if (StringUtils.containsWhitespace(var2)) {
               DebugLog.General.error("Sanitizing AttributeType name '" + var2 + "', name may not contain whitespaces.");
               var2 = StringUtils.removeWhitespace(var2);
            }

            this.id = var1;
            this.name = var2;
            this.optionDisplay = var4;
            this.optionDisplayAsBar = var5;
            this.tooltipOverride = var6;
            this.translateKey = "Attribute_Type_" + this.name;
            this.readOnly = var3;
         }
      } else {
         throw new RuntimeException("AttributeType Id may not exceed '8128' or be less than zero.");
      }
   }

   public short id() {
      return this.id;
   }

   public boolean isReadOnly() {
      return this.readOnly;
   }

   public java.lang.String toString() {
      return this.getName();
   }

   public java.lang.String getName() {
      return this.name;
   }

   public abstract AttributeValueType getValueType();

   public boolean isNumeric() {
      return AttributeValueType.IsNumeric(this.getValueType());
   }

   public boolean isDecimal() {
      return AttributeValueType.IsDecimal(this.getValueType());
   }

   public boolean isHiddenUI() {
      return this.optionDisplay == Attribute.UI.Display.Hidden;
   }

   protected Attribute.UI.DisplayAsBar getDisplayAsBar() {
      return this.optionDisplayAsBar;
   }

   public java.lang.String getTranslateKey() {
      return this.translateKey;
   }

   private java.lang.String getTranslatedName() {
      java.lang.String var1 = Translator.getAttributeTextOrNull(this.translateKey);
      return var1 != null ? var1 : this.getName();
   }

   public java.lang.String getNameUI() {
      if (this.tooltipOverride != null) {
         java.lang.String var1 = Translator.getAttributeTextOrNull(this.tooltipOverride);
         if (var1 != null) {
            return var1;
         }
      }

      return this.getTranslatedName();
   }

   public static class Long extends Numeric<Long, java.lang.Long> {
      protected Long(short var1, java.lang.String var2, long var3) {
         super(var1, var2, var3, false, Attribute.UI.Display.Visible, Attribute.UI.DisplayAsBar.Default, (java.lang.String)null);
      }

      protected Long(short var1, java.lang.String var2, long var3, boolean var5, Attribute.UI.Display var6, Attribute.UI.DisplayAsBar var7, java.lang.String var8) {
         super(var1, var2, var3, var5, var6, var7, var8);
      }

      public AttributeValueType getValueType() {
         return AttributeValueType.Long;
      }

      public java.lang.Long validate(java.lang.Long var1) {
         if (this.isRequiresValidation() && this.getVars() != null) {
            var1 = Math.min(var1, (java.lang.Long)this.getVars().max);
            var1 = Math.max(var1, (java.lang.Long)this.getVars().min);
         }

         return var1;
      }

      public java.lang.Long getMin() {
         return this.getVars() != null ? (java.lang.Long)this.getVars().min : -9223372036854775808L;
      }

      public java.lang.Long getMax() {
         return this.getVars() != null ? (java.lang.Long)this.getVars().max : 9223372036854775807L;
      }

      protected boolean withinBounds(java.lang.Long var1) {
         if (this.getVars() == null) {
            return true;
         } else {
            return var1 >= (java.lang.Long)this.getVars().min && var1 <= (java.lang.Long)this.getVars().max;
         }
      }
   }

   public static class Int extends Numeric<Int, Integer> {
      protected Int(short var1, java.lang.String var2, int var3) {
         super(var1, var2, var3, false, Attribute.UI.Display.Visible, Attribute.UI.DisplayAsBar.Default, (java.lang.String)null);
      }

      protected Int(short var1, java.lang.String var2, int var3, boolean var4, Attribute.UI.Display var5, Attribute.UI.DisplayAsBar var6, java.lang.String var7) {
         super(var1, var2, var3, var4, var5, var6, var7);
      }

      public AttributeValueType getValueType() {
         return AttributeValueType.Int;
      }

      public Integer validate(Integer var1) {
         if (this.isRequiresValidation() && this.getVars() != null) {
            var1 = Math.min(var1, (Integer)this.getVars().max);
            var1 = Math.max(var1, (Integer)this.getVars().min);
         }

         return var1;
      }

      public Integer getMin() {
         return this.getVars() != null ? (Integer)this.getVars().min : -2147483648;
      }

      public Integer getMax() {
         return this.getVars() != null ? (Integer)this.getVars().max : 2147483647;
      }

      protected boolean withinBounds(Integer var1) {
         if (this.getVars() == null) {
            return true;
         } else {
            return var1 >= (Integer)this.getVars().min && var1 <= (Integer)this.getVars().max;
         }
      }
   }

   public static class Short extends Numeric<Short, java.lang.Short> {
      protected Short(short var1, java.lang.String var2, short var3) {
         super(var1, var2, var3, false, Attribute.UI.Display.Visible, Attribute.UI.DisplayAsBar.Default, (java.lang.String)null);
      }

      protected Short(short var1, java.lang.String var2, short var3, boolean var4, Attribute.UI.Display var5, Attribute.UI.DisplayAsBar var6, java.lang.String var7) {
         super(var1, var2, var3, var4, var5, var6, var7);
      }

      public AttributeValueType getValueType() {
         return AttributeValueType.Short;
      }

      public java.lang.Short validate(java.lang.Short var1) {
         if (this.isRequiresValidation() && this.getVars() != null) {
            var1 = (short)Math.min(var1, (java.lang.Short)this.getVars().max);
            var1 = (short)Math.max(var1, (java.lang.Short)this.getVars().min);
         }

         return var1;
      }

      public java.lang.Short getMin() {
         return this.getVars() != null ? (java.lang.Short)this.getVars().min : -32768;
      }

      public java.lang.Short getMax() {
         return this.getVars() != null ? (java.lang.Short)this.getVars().max : 32767;
      }

      protected boolean withinBounds(java.lang.Short var1) {
         if (this.getVars() == null) {
            return true;
         } else {
            return var1 >= (java.lang.Short)this.getVars().min && var1 <= (java.lang.Short)this.getVars().max;
         }
      }
   }

   public static class Byte extends Numeric<Byte, java.lang.Byte> {
      protected Byte(short var1, java.lang.String var2, byte var3) {
         super(var1, var2, var3, false, Attribute.UI.Display.Visible, Attribute.UI.DisplayAsBar.Default, (java.lang.String)null);
      }

      protected Byte(short var1, java.lang.String var2, byte var3, boolean var4, Attribute.UI.Display var5, Attribute.UI.DisplayAsBar var6, java.lang.String var7) {
         super(var1, var2, var3, var4, var5, var6, var7);
      }

      public AttributeValueType getValueType() {
         return AttributeValueType.Byte;
      }

      public java.lang.Byte validate(java.lang.Byte var1) {
         if (this.isRequiresValidation() && this.getVars() != null) {
            var1 = (byte)Math.min(var1, (java.lang.Byte)this.getVars().max);
            var1 = (byte)Math.max(var1, (java.lang.Byte)this.getVars().min);
         }

         return var1;
      }

      public java.lang.Byte getMin() {
         return this.getVars() != null ? (java.lang.Byte)this.getVars().min : -128;
      }

      public java.lang.Byte getMax() {
         return this.getVars() != null ? (java.lang.Byte)this.getVars().max : 127;
      }

      protected boolean withinBounds(java.lang.Byte var1) {
         if (this.getVars() == null) {
            return true;
         } else {
            return var1 >= (java.lang.Byte)this.getVars().min && var1 <= (java.lang.Byte)this.getVars().max;
         }
      }
   }

   public static class Double extends Numeric<Double, java.lang.Double> {
      protected Double(short var1, java.lang.String var2, double var3) {
         super(var1, var2, var3, false, Attribute.UI.Display.Visible, Attribute.UI.DisplayAsBar.Default, (java.lang.String)null);
      }

      protected Double(short var1, java.lang.String var2, double var3, boolean var5, Attribute.UI.Display var6, Attribute.UI.DisplayAsBar var7, java.lang.String var8) {
         super(var1, var2, var3, var5, var6, var7, var8);
      }

      public AttributeValueType getValueType() {
         return AttributeValueType.Double;
      }

      public java.lang.Double validate(java.lang.Double var1) {
         if (this.isRequiresValidation() && this.getVars() != null) {
            var1 = Math.min(var1, (java.lang.Double)this.getVars().max);
            var1 = Math.max(var1, (java.lang.Double)this.getVars().min);
         }

         return var1;
      }

      public java.lang.Double getMin() {
         return this.getVars() != null ? (java.lang.Double)this.getVars().min : 4.9E-324;
      }

      public java.lang.Double getMax() {
         return this.getVars() != null ? (java.lang.Double)this.getVars().max : 1.7976931348623157E308;
      }

      protected boolean withinBounds(java.lang.Double var1) {
         if (this.getVars() == null) {
            return true;
         } else {
            return var1 >= (java.lang.Double)this.getVars().min && var1 <= (java.lang.Double)this.getVars().max;
         }
      }
   }

   public static class Float extends Numeric<Float, java.lang.Float> {
      protected Float(short var1, java.lang.String var2, float var3) {
         super(var1, var2, var3, false, Attribute.UI.Display.Visible, Attribute.UI.DisplayAsBar.Default, (java.lang.String)null);
      }

      protected Float(short var1, java.lang.String var2, float var3, boolean var4, Attribute.UI.Display var5, Attribute.UI.DisplayAsBar var6, java.lang.String var7) {
         super(var1, var2, var3, var4, var5, var6, var7);
      }

      public AttributeValueType getValueType() {
         return AttributeValueType.Float;
      }

      public java.lang.Float validate(java.lang.Float var1) {
         if (this.isRequiresValidation() && this.getVars() != null) {
            var1 = Math.min(var1, (java.lang.Float)this.getVars().max);
            var1 = Math.max(var1, (java.lang.Float)this.getVars().min);
         }

         return var1;
      }

      public java.lang.Float getMin() {
         return this.getVars() != null ? (java.lang.Float)this.getVars().min : 1.4E-45F;
      }

      public java.lang.Float getMax() {
         return this.getVars() != null ? (java.lang.Float)this.getVars().max : 3.4028235E38F;
      }

      protected boolean withinBounds(java.lang.Float var1) {
         if (this.getVars() == null) {
            return true;
         } else {
            return var1 >= (java.lang.Float)this.getVars().min && var1 <= (java.lang.Float)this.getVars().max;
         }
      }
   }

   public abstract static class Numeric<C extends Numeric<C, T>, T extends Number> extends AttributeType {
      private final T initialValue;
      private NumericVars<T> vars = null;
      private boolean requiresValidation = false;

      protected Numeric(short var1, java.lang.String var2, T var3, boolean var4, Attribute.UI.Display var5, Attribute.UI.DisplayAsBar var6, java.lang.String var7) {
         super(var1, var2, var4, var5, var6, var7);
         this.initialValue = (Number)Objects.requireNonNull(var3);
      }

      public AttributeValueType getValueType() {
         return AttributeValueType.Float;
      }

      protected boolean isRequiresValidation() {
         return this.requiresValidation;
      }

      protected NumericVars<T> getVars() {
         return this.vars;
      }

      public T getInitialValue() {
         return this.initialValue;
      }

      protected final Numeric<C, T> setBounds(T var1, T var2) {
         if (!(var1.doubleValue() < 0.0) && !(var1.doubleValue() >= var2.doubleValue()) && !(var2.doubleValue() <= 0.0)) {
            this.requiresValidation = true;
            if (this.vars == null) {
               this.vars = new NumericVars(var1, var2);
            }

            if (!this.withinBounds(this.initialValue)) {
               throw new IllegalArgumentException("Initialvalue outside set bounds.");
            } else {
               return this;
            }
         } else {
            throw new IllegalArgumentException("Illegal 'Bounds' on Attribute [" + this.toString() + "]");
         }
      }

      public boolean hasBounds() {
         return this.requiresValidation;
      }

      public abstract T validate(T var1);

      public abstract T getMin();

      public abstract T getMax();

      protected abstract boolean withinBounds(T var1);

      protected static class NumericVars<T> {
         protected final T min;
         protected final T max;

         protected NumericVars(T var1, T var2) {
            this.min = var1;
            this.max = var2;
         }
      }
   }

   public static class String extends AttributeType {
      private final java.lang.String initialValue;

      protected String(short var1, java.lang.String var2, java.lang.String var3) {
         super(var1, var2, false, Attribute.UI.Display.Visible, Attribute.UI.DisplayAsBar.Never, (java.lang.String)null);
         this.initialValue = (java.lang.String)Objects.requireNonNull(var3);
      }

      protected String(short var1, java.lang.String var2, java.lang.String var3, boolean var4, Attribute.UI.Display var5, java.lang.String var6) {
         super(var1, var2, var4, var5, Attribute.UI.DisplayAsBar.Never, var6);
         this.initialValue = (java.lang.String)Objects.requireNonNull(var3);
      }

      public AttributeValueType getValueType() {
         return AttributeValueType.String;
      }

      public java.lang.String getInitialValue() {
         return this.initialValue;
      }
   }

   public static class Bool extends AttributeType {
      private final boolean initialValue;

      protected Bool(short var1, java.lang.String var2, boolean var3) {
         super(var1, var2, false, Attribute.UI.Display.Visible, Attribute.UI.DisplayAsBar.Never, (java.lang.String)null);
         this.initialValue = var3;
      }

      protected Bool(short var1, java.lang.String var2, boolean var3, boolean var4, Attribute.UI.Display var5, java.lang.String var6) {
         super(var1, var2, var4, var5, Attribute.UI.DisplayAsBar.Never, var6);
         this.initialValue = var3;
      }

      public AttributeValueType getValueType() {
         return AttributeValueType.Boolean;
      }

      public boolean getInitialValue() {
         return this.initialValue;
      }
   }

   public static class EnumStringSet<E extends java.lang.Enum<E> & IOEnum> extends AttributeType {
      private final Class<E> enumClass;
      private final EnumStringObj<E> initialValue;

      protected EnumStringSet(short var1, java.lang.String var2, Class<E> var3) {
         super(var1, var2, false, Attribute.UI.Display.Visible, Attribute.UI.DisplayAsBar.Never, (java.lang.String)null);
         this.enumClass = (Class)Objects.requireNonNull(var3);
         this.initialValue = new EnumStringObj();
         this.initialValue.initialize(this.enumClass);
      }

      protected EnumStringSet(short var1, java.lang.String var2, Class<E> var3, boolean var4, Attribute.UI.Display var5, java.lang.String var6) {
         super(var1, var2, var4, var5, Attribute.UI.DisplayAsBar.Never, var6);
         this.enumClass = (Class)Objects.requireNonNull(var3);
         this.initialValue = new EnumStringObj();
         this.initialValue.initialize(this.enumClass);
      }

      public AttributeValueType getValueType() {
         return AttributeValueType.EnumStringSet;
      }

      public EnumStringObj<E> getInitialValue() {
         return this.initialValue;
      }

      public E enumValueFromString(java.lang.String var1) {
         return AttributeUtil.enumValueFromScriptString(this.enumClass, var1);
      }

      public E enumValueFromByteID(byte var1) {
         java.lang.Enum[] var2 = (java.lang.Enum[])this.enumClass.getEnumConstants();
         int var3 = var2.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            java.lang.Enum var5 = var2[var4];
            if (((IOEnum)var5).getByteId() == var1) {
               return var5;
            }
         }

         return null;
      }

      protected Class<E> getEnumClass() {
         return this.enumClass;
      }
   }

   public static class EnumSet<E extends java.lang.Enum<E> & IOEnum> extends AttributeType {
      private final Class<E> enumClass;
      private final java.util.EnumSet<E> initialValue;

      protected EnumSet(short var1, java.lang.String var2, Class<E> var3) {
         super(var1, var2, false, Attribute.UI.Display.Visible, Attribute.UI.DisplayAsBar.Never, (java.lang.String)null);
         this.enumClass = (Class)Objects.requireNonNull(var3);
         this.initialValue = java.util.EnumSet.noneOf(this.enumClass);
      }

      protected EnumSet(short var1, java.lang.String var2, Class<E> var3, boolean var4, Attribute.UI.Display var5, java.lang.String var6) {
         super(var1, var2, var4, var5, Attribute.UI.DisplayAsBar.Never, var6);
         this.enumClass = (Class)Objects.requireNonNull(var3);
         this.initialValue = java.util.EnumSet.noneOf(this.enumClass);
      }

      public AttributeValueType getValueType() {
         return AttributeValueType.EnumSet;
      }

      public java.util.EnumSet<E> getInitialValue() {
         return this.initialValue;
      }

      public E enumValueFromString(java.lang.String var1) {
         return AttributeUtil.enumValueFromScriptString(this.enumClass, var1);
      }

      public E enumValueFromByteID(byte var1) {
         java.lang.Enum[] var2 = (java.lang.Enum[])this.enumClass.getEnumConstants();
         int var3 = var2.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            java.lang.Enum var5 = var2[var4];
            if (((IOEnum)var5).getByteId() == var1) {
               return var5;
            }
         }

         return null;
      }

      protected Class<E> getEnumClass() {
         return this.enumClass;
      }
   }

   public static class Enum<E extends java.lang.Enum<E> & IOEnum> extends AttributeType {
      private final Class<E> enumClass;
      private final E initialValue;

      protected Enum(short var1, java.lang.String var2, E var3) {
         super(var1, var2, false, Attribute.UI.Display.Visible, Attribute.UI.DisplayAsBar.Never, (java.lang.String)null);
         this.initialValue = (java.lang.Enum)Objects.requireNonNull(var3);
         this.enumClass = this.initialValue.getDeclaringClass();
      }

      protected Enum(short var1, java.lang.String var2, E var3, boolean var4, Attribute.UI.Display var5, java.lang.String var6) {
         super(var1, var2, var4, var5, Attribute.UI.DisplayAsBar.Never, var6);
         this.initialValue = (java.lang.Enum)Objects.requireNonNull(var3);
         this.enumClass = this.initialValue.getDeclaringClass();
      }

      public AttributeValueType getValueType() {
         return AttributeValueType.Enum;
      }

      public E getInitialValue() {
         return this.initialValue;
      }

      public E enumValueFromString(java.lang.String var1) {
         return AttributeUtil.enumValueFromScriptString(this.enumClass, var1);
      }

      public E enumValueFromByteID(byte var1) {
         Class var2 = this.initialValue.getDeclaringClass();
         java.lang.Enum[] var3 = (java.lang.Enum[])var2.getEnumConstants();
         int var4 = var3.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            java.lang.Enum var6 = var3[var5];
            if (((IOEnum)var6).getByteId() == var1) {
               return var6;
            }
         }

         return null;
      }
   }
}
