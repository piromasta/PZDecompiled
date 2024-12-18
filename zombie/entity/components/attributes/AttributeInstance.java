package zombie.entity.components.attributes;

import java.nio.ByteBuffer;
import java.util.Iterator;
import zombie.GameWindow;
import zombie.debug.DebugLog;
import zombie.entity.util.enums.IOEnum;

public abstract class AttributeInstance<C extends AttributeInstance<C, T>, T extends AttributeType> {
   protected T type;

   protected AttributeInstance() {
   }

   protected abstract void setType(T var1);

   public final T getType() {
      return this.type;
   }

   public final AttributeValueType getValueType() {
      return this.type.getValueType();
   }

   public final java.lang.String getNameUI() {
      return this.type.getNameUI();
   }

   public final boolean isHiddenUI() {
      return this.type.isHiddenUI();
   }

   public boolean isRequiresValidation() {
      return false;
   }

   public final boolean isReadOnly() {
      return this.type.isReadOnly();
   }

   protected boolean canSetValue() {
      if (this.isReadOnly()) {
         DebugLog.General.error("Trying to set value on a read-only attribute [" + this.toString() + "]");
         return false;
      } else {
         return true;
      }
   }

   public abstract java.lang.String stringValue();

   public abstract boolean setValueFromScriptString(java.lang.String var1);

   public abstract boolean equalTo(C var1);

   public abstract C copy();

   public boolean isDisplayAsBar() {
      return false;
   }

   public float getDisplayAsBarUnit() {
      return 0.0F;
   }

   public float getFloatValue() {
      return 0.0F;
   }

   public int getIntValue() {
      return 0;
   }

   protected void reset() {
      this.type = null;
   }

   protected abstract void release();

   public abstract void save(ByteBuffer var1);

   public abstract void load(ByteBuffer var1);

   public java.lang.String toString() {
      Object var10000 = this.type != null ? this.type : "NOT_SET";
      return "Attribute." + var10000 + " [value = " + this.stringValue() + ", valueType = " + (this.type != null ? this.type.getValueType() : "NOT_SET") + ", hidden = " + this.isHiddenUI() + ", req_val = " + this.isRequiresValidation() + ", read-only = " + this.isReadOnly() + "]";
   }

   public static class Long extends Numeric<Long, AttributeType.Long> {
      private long value = 0L;

      public Long() {
      }

      protected void setType(AttributeType.Long var1) {
         this.type = var1;
         this.value = (java.lang.Long)var1.getInitialValue();
      }

      public long getValue() {
         return this.value;
      }

      public void setValue(long var1) {
         if (this.canSetValue()) {
            this.value = ((AttributeType.Long)this.type).validate(var1);
         }

      }

      public float floatValue() {
         return (float)this.value;
      }

      public void fromFloat(float var1) {
         this.setValue((long)var1);
      }

      public java.lang.String stringValue() {
         return java.lang.Long.toString(this.value);
      }

      public boolean setValueFromScriptString(java.lang.String var1) {
         try {
            this.value = ((AttributeType.Long)this.type).validate(java.lang.Long.parseLong(var1));
            return true;
         } catch (Exception var3) {
            var3.printStackTrace();
            this.value = (java.lang.Long)((AttributeType.Long)this.type).getInitialValue();
            return false;
         }
      }

      public boolean equalTo(Long var1) {
         if (this.type == var1.type) {
            return this.value == var1.value;
         } else {
            return true;
         }
      }

      public Long copy() {
         Long var1 = AttributeFactory.AllocAttributeLong();
         var1.setType((AttributeType.Long)this.type);
         var1.value = this.value;
         return var1;
      }

      protected void release() {
         AttributeFactory.Release(this);
      }

      public void save(ByteBuffer var1) {
         var1.putLong(this.value);
      }

      public void load(ByteBuffer var1) {
         this.value = var1.getLong();
      }
   }

   public static class Int extends Numeric<Int, AttributeType.Int> {
      private int value = 0;

      public Int() {
      }

      protected void setType(AttributeType.Int var1) {
         this.type = var1;
         this.value = (Integer)var1.getInitialValue();
      }

      public int getValue() {
         return this.value;
      }

      public void setValue(int var1) {
         if (this.canSetValue()) {
            this.value = ((AttributeType.Int)this.type).validate(var1);
         }

      }

      public float floatValue() {
         return (float)this.value;
      }

      public void fromFloat(float var1) {
         this.setValue((int)var1);
      }

      public java.lang.String stringValue() {
         return Integer.toString(this.value);
      }

      public boolean setValueFromScriptString(java.lang.String var1) {
         try {
            this.value = ((AttributeType.Int)this.type).validate(Integer.parseInt(var1));
            return true;
         } catch (Exception var3) {
            var3.printStackTrace();
            this.value = (Integer)((AttributeType.Int)this.type).getInitialValue();
            return false;
         }
      }

      public boolean equalTo(Int var1) {
         if (this.type == var1.type) {
            return this.value == var1.value;
         } else {
            return true;
         }
      }

      public Int copy() {
         Int var1 = AttributeFactory.AllocAttributeInt();
         var1.setType((AttributeType.Int)this.type);
         var1.value = this.value;
         return var1;
      }

      protected void release() {
         AttributeFactory.Release(this);
      }

      public void save(ByteBuffer var1) {
         var1.putInt(this.value);
      }

      public void load(ByteBuffer var1) {
         this.value = var1.getInt();
      }
   }

   public static class Short extends Numeric<Short, AttributeType.Short> {
      private short value = 0;

      public Short() {
      }

      protected void setType(AttributeType.Short var1) {
         this.type = var1;
         this.value = (java.lang.Short)var1.getInitialValue();
      }

      public short getValue() {
         return this.value;
      }

      public void setValue(short var1) {
         if (this.canSetValue()) {
            this.value = ((AttributeType.Short)this.type).validate(var1);
         }

      }

      public float floatValue() {
         return (float)this.value;
      }

      public void fromFloat(float var1) {
         this.setValue((short)((int)var1));
      }

      public java.lang.String stringValue() {
         return java.lang.Short.toString(this.value);
      }

      public boolean setValueFromScriptString(java.lang.String var1) {
         try {
            this.value = ((AttributeType.Short)this.type).validate(java.lang.Short.parseShort(var1));
            return true;
         } catch (Exception var3) {
            var3.printStackTrace();
            this.value = (java.lang.Short)((AttributeType.Short)this.type).getInitialValue();
            return false;
         }
      }

      public boolean equalTo(Short var1) {
         if (this.type == var1.type) {
            return this.value == var1.value;
         } else {
            return true;
         }
      }

      public Short copy() {
         Short var1 = AttributeFactory.AllocAttributeShort();
         var1.setType((AttributeType.Short)this.type);
         var1.value = this.value;
         return var1;
      }

      protected void release() {
         AttributeFactory.Release(this);
      }

      public void save(ByteBuffer var1) {
         var1.putShort(this.value);
      }

      public void load(ByteBuffer var1) {
         this.value = var1.getShort();
      }
   }

   public static class Byte extends Numeric<Byte, AttributeType.Byte> {
      private byte value = 0;

      public Byte() {
      }

      protected void setType(AttributeType.Byte var1) {
         this.type = var1;
         this.value = (java.lang.Byte)var1.getInitialValue();
      }

      public byte getValue() {
         return this.value;
      }

      public void setValue(byte var1) {
         if (this.canSetValue()) {
            this.value = ((AttributeType.Byte)this.type).validate(var1);
         }

      }

      public float floatValue() {
         return (float)this.value;
      }

      public void fromFloat(float var1) {
         this.setValue((byte)((int)var1));
      }

      public java.lang.String stringValue() {
         return java.lang.Byte.toString(this.value);
      }

      public boolean setValueFromScriptString(java.lang.String var1) {
         try {
            this.value = ((AttributeType.Byte)this.type).validate(java.lang.Byte.parseByte(var1));
            return true;
         } catch (Exception var3) {
            var3.printStackTrace();
            this.value = (java.lang.Byte)((AttributeType.Byte)this.type).getInitialValue();
            return false;
         }
      }

      public boolean equalTo(Byte var1) {
         if (this.type == var1.type) {
            return this.value == var1.value;
         } else {
            return true;
         }
      }

      public Byte copy() {
         Byte var1 = AttributeFactory.AllocAttributeByte();
         var1.setType((AttributeType.Byte)this.type);
         var1.value = this.value;
         return var1;
      }

      protected void release() {
         AttributeFactory.Release(this);
      }

      public void save(ByteBuffer var1) {
         var1.put(this.value);
      }

      public void load(ByteBuffer var1) {
         this.value = var1.get();
      }
   }

   public static class Double extends Numeric<Double, AttributeType.Double> {
      private double value = 0.0;

      public Double() {
      }

      protected void setType(AttributeType.Double var1) {
         this.type = var1;
         this.value = (java.lang.Double)var1.getInitialValue();
      }

      public double getValue() {
         return this.value;
      }

      public void setValue(double var1) {
         if (this.canSetValue()) {
            this.value = ((AttributeType.Double)this.type).validate(var1);
         }

      }

      public float floatValue() {
         if (this.value < -3.4028234663852886E38 || this.value > 3.4028234663852886E38) {
            DebugLog.General.error("Attribute '" + this.type + "' double value exceeds float bounds.");
         }

         return (float)this.value;
      }

      public void fromFloat(float var1) {
         this.setValue((double)var1);
      }

      public java.lang.String stringValue() {
         return java.lang.Double.toString(this.value);
      }

      public boolean setValueFromScriptString(java.lang.String var1) {
         try {
            this.value = ((AttributeType.Double)this.type).validate(java.lang.Double.parseDouble(var1));
            return true;
         } catch (Exception var3) {
            var3.printStackTrace();
            this.value = (java.lang.Double)((AttributeType.Double)this.type).getInitialValue();
            return false;
         }
      }

      public boolean equalTo(Double var1) {
         if (this.type == var1.type) {
            return this.value == var1.value;
         } else {
            return true;
         }
      }

      public Double copy() {
         Double var1 = AttributeFactory.AllocAttributeDouble();
         var1.setType((AttributeType.Double)this.type);
         var1.value = this.value;
         return var1;
      }

      protected void release() {
         AttributeFactory.Release(this);
      }

      public void save(ByteBuffer var1) {
         var1.putDouble(this.value);
      }

      public void load(ByteBuffer var1) {
         this.value = var1.getDouble();
      }
   }

   public static class Float extends Numeric<Float, AttributeType.Float> {
      private float value = 0.0F;

      public Float() {
      }

      protected void setType(AttributeType.Float var1) {
         this.type = var1;
         this.value = (java.lang.Float)var1.getInitialValue();
      }

      public float getValue() {
         return this.value;
      }

      public void setValue(float var1) {
         if (this.canSetValue()) {
            this.value = ((AttributeType.Float)this.type).validate(var1);
         }

      }

      public float floatValue() {
         return this.value;
      }

      public void fromFloat(float var1) {
         this.setValue(var1);
      }

      public java.lang.String stringValue() {
         return java.lang.Float.toString(this.value);
      }

      public boolean setValueFromScriptString(java.lang.String var1) {
         try {
            this.value = ((AttributeType.Float)this.type).validate(java.lang.Float.parseFloat(var1));
            return true;
         } catch (Exception var3) {
            var3.printStackTrace();
            this.value = (java.lang.Float)((AttributeType.Float)this.type).getInitialValue();
            return false;
         }
      }

      public boolean equalTo(Float var1) {
         if (this.type == var1.type) {
            return this.value == var1.value;
         } else {
            return true;
         }
      }

      public Float copy() {
         Float var1 = AttributeFactory.AllocAttributeFloat();
         var1.setType((AttributeType.Float)this.type);
         var1.value = this.value;
         return var1;
      }

      protected void release() {
         AttributeFactory.Release(this);
      }

      public void save(ByteBuffer var1) {
         var1.putFloat(this.value);
      }

      public void load(ByteBuffer var1) {
         this.value = var1.getFloat();
      }
   }

   public abstract static class Numeric<C extends Numeric<C, T>, T extends AttributeType.Numeric<T, ?>> extends AttributeInstance<C, T> {
      public Numeric() {
      }

      public abstract float floatValue();

      public abstract void fromFloat(float var1);

      public boolean isRequiresValidation() {
         return ((AttributeType.Numeric)this.type).isRequiresValidation();
      }

      public boolean isDisplayAsBar() {
         if (((AttributeType.Numeric)this.type).getDisplayAsBar() != Attribute.UI.DisplayAsBar.Never) {
            return ((AttributeType.Numeric)this.type).getVars() != null;
         } else {
            return false;
         }
      }

      public float getDisplayAsBarUnit() {
         if (((AttributeType.Numeric)this.type).getVars() != null) {
            float var1 = ((Number)((AttributeType.Numeric)this.type).getVars().min).floatValue();
            float var2 = ((Number)((AttributeType.Numeric)this.type).getVars().max).floatValue();
            float var3 = this.floatValue();
            return (var3 - var1) / (var2 - var1);
         } else {
            return 0.0F;
         }
      }

      public float getFloatValue() {
         return this.floatValue();
      }

      public int getIntValue() {
         return (int)this.floatValue();
      }
   }

   public static class String extends AttributeInstance<String, AttributeType.String> {
      private java.lang.String value;

      public String() {
      }

      protected void setType(AttributeType.String var1) {
         this.type = var1;
         this.value = var1.getInitialValue();
      }

      public java.lang.String getValue() {
         return this.value;
      }

      public void setValue(java.lang.String var1) {
         if (this.canSetValue()) {
            this.value = var1;
         }

      }

      public java.lang.String stringValue() {
         return this.value;
      }

      public boolean setValueFromScriptString(java.lang.String var1) {
         try {
            this.value = var1;
            return true;
         } catch (Exception var3) {
            var3.printStackTrace();
            this.value = ((AttributeType.String)this.type).getInitialValue();
            return false;
         }
      }

      public boolean equalTo(String var1) {
         return this.type == var1.type ? this.value.equals(var1.value) : true;
      }

      public String copy() {
         String var1 = AttributeFactory.AllocAttributeString();
         var1.setType((AttributeType.String)this.type);
         var1.value = this.value;
         return var1;
      }

      protected void release() {
         AttributeFactory.Release(this);
      }

      public void save(ByteBuffer var1) {
         GameWindow.WriteString(var1, this.value);
      }

      public void load(ByteBuffer var1) {
         this.value = GameWindow.ReadString(var1);
      }
   }

   public static class Bool extends AttributeInstance<Bool, AttributeType.Bool> {
      private boolean value;

      public Bool() {
      }

      protected void setType(AttributeType.Bool var1) {
         this.type = var1;
         this.value = var1.getInitialValue();
      }

      public boolean getValue() {
         return this.value;
      }

      public void setValue(boolean var1) {
         if (this.canSetValue()) {
            this.value = var1;
         }

      }

      public java.lang.String stringValue() {
         return Boolean.toString(this.value);
      }

      public boolean setValueFromScriptString(java.lang.String var1) {
         try {
            this.value = Boolean.parseBoolean(var1);
            return true;
         } catch (Exception var3) {
            var3.printStackTrace();
            this.value = ((AttributeType.Bool)this.type).getInitialValue();
            return false;
         }
      }

      public boolean equalTo(Bool var1) {
         if (this.type == var1.type) {
            return this.value == var1.value;
         } else {
            return true;
         }
      }

      public Bool copy() {
         Bool var1 = AttributeFactory.AllocAttributeBool();
         var1.setType((AttributeType.Bool)this.type);
         var1.value = this.value;
         return var1;
      }

      protected void release() {
         AttributeFactory.Release(this);
      }

      public void save(ByteBuffer var1) {
         var1.put((byte)(this.value ? 1 : 0));
      }

      public void load(ByteBuffer var1) {
         this.value = var1.get() == 1;
      }
   }

   public static class EnumStringSet<E extends java.lang.Enum<E> & IOEnum> extends AttributeInstance<EnumStringSet<E>, AttributeType.EnumStringSet<E>> {
      private final EnumStringObj<E> value = new EnumStringObj();

      public EnumStringSet() {
      }

      protected void setType(AttributeType.EnumStringSet<E> var1) {
         this.type = var1;
         this.value.initialize(var1.getEnumClass());
         this.value.addAll(true, var1.getInitialValue());
      }

      public EnumStringObj<E> getValue() {
         return this.value;
      }

      public void setValue(EnumStringObj<E> var1) {
         if (this.canSetValue()) {
            this.value.addAll(true, var1);
         }

      }

      public java.lang.String stringValue() {
         return this.value.toString();
      }

      public boolean setValueFromScriptString(java.lang.String var1) {
         try {
            this.value.clear();
            if (var1.contains(";")) {
               java.lang.String[] var2 = var1.split(";");
               java.lang.String[] var3 = var2;
               int var4 = var2.length;

               for(int var5 = 0; var5 < var4; ++var5) {
                  java.lang.String var6 = var3[var5];
                  if (AttributeUtil.isEnumString(var6)) {
                     this.addEnumValueFromString(var6);
                  } else {
                     this.addStringValue(var6);
                  }
               }
            } else if (AttributeUtil.isEnumString(var1)) {
               this.addEnumValueFromString(var1);
            } else {
               this.addStringValue(var1);
            }

            return true;
         } catch (Exception var7) {
            var7.printStackTrace();
            return false;
         }
      }

      public void addEnumValueFromString(java.lang.String var1) {
         java.lang.Enum var2 = ((AttributeType.EnumStringSet)this.type).enumValueFromString(var1);
         if (var2 != null) {
            this.value.add(var2);
         } else {
            throw new NullPointerException("Attribute.EnumSet Cannot read Enum script value '" + var1 + "'.");
         }
      }

      public boolean removeEnumValueFromString(java.lang.String var1) {
         java.lang.Enum var2 = ((AttributeType.EnumStringSet)this.type).enumValueFromString(var1);
         if (var2 != null) {
            return this.value.remove(var2);
         } else {
            throw new NullPointerException("Attribute.EnumSet Cannot read Enum script value '" + var1 + "'.");
         }
      }

      public void addStringValue(java.lang.String var1) {
         this.value.add(var1);
      }

      public boolean removeStringValue(java.lang.String var1) {
         return this.value.remove(var1);
      }

      public void clear() {
         this.value.clear();
      }

      public boolean equalTo(EnumStringSet<E> var1) {
         return this.type == var1.type ? this.value.equals(var1.value) : true;
      }

      public EnumStringSet<E> copy() {
         EnumStringSet var1 = AttributeFactory.AllocAttributeEnumStringSet();
         var1.setType((AttributeType.EnumStringSet)this.type);
         var1.value.initialize(((AttributeType.EnumStringSet)this.type).getEnumClass());
         var1.value.addAll(this.value);
         return var1;
      }

      protected void release() {
         AttributeFactory.Release(this);
      }

      protected void reset() {
         super.reset();
         this.value.reset();
      }

      public void save(ByteBuffer var1) {
         var1.put((byte)this.value.getEnumValues().size());
         Iterator var2 = this.value.getEnumValues().iterator();

         while(var2.hasNext()) {
            java.lang.Enum var3 = (java.lang.Enum)var2.next();
            var1.put(((IOEnum)var3).getByteId());
         }

         var1.put((byte)this.value.getStringValues().size());

         for(int var4 = 0; var4 < this.value.getStringValues().size(); ++var4) {
            GameWindow.WriteString(var1, (java.lang.String)this.value.getStringValues().get(var4));
         }

      }

      public void load(ByteBuffer var1) {
         if (this.value.size() > 0) {
            this.value.clear();
         }

         byte var2 = var1.get();

         int var4;
         for(var4 = 0; var4 < var2; ++var4) {
            byte var3 = var1.get();
            java.lang.Enum var5 = ((AttributeType.EnumStringSet)this.type).enumValueFromByteID(var3);
            if (var5 != null) {
               this.value.add(((AttributeType.EnumStringSet)this.type).enumValueFromByteID(var3));
            } else {
               DebugLog.General.error("Could not load value for EnumStringSet attribute '" + this.type + "'.");
            }
         }

         var2 = var1.get();

         for(var4 = 0; var4 < var2; ++var4) {
            java.lang.String var6 = GameWindow.ReadString(var1);
            this.value.add(var6);
         }

      }
   }

   public static class EnumSet<E extends java.lang.Enum<E> & IOEnum> extends AttributeInstance<EnumSet<E>, AttributeType.EnumSet<E>> {
      private java.util.EnumSet<E> value;

      public EnumSet() {
      }

      protected void setType(AttributeType.EnumSet<E> var1) {
         this.type = var1;
         this.value = java.util.EnumSet.copyOf(var1.getInitialValue());
      }

      public java.util.EnumSet<E> getValue() {
         return this.value;
      }

      public void setValue(java.util.EnumSet<E> var1) {
         if (this.canSetValue()) {
            this.value = var1;
         }

      }

      public java.lang.String stringValue() {
         return this.value.toString();
      }

      public boolean setValueFromScriptString(java.lang.String var1) {
         try {
            if (this.value.size() > 0) {
               this.value.clear();
            }

            if (var1.contains(";")) {
               java.lang.String[] var2 = var1.split(";");
               java.lang.String[] var3 = var2;
               int var4 = var2.length;

               for(int var5 = 0; var5 < var4; ++var5) {
                  java.lang.String var6 = var3[var5];
                  this.addValueFromString(var6);
               }
            } else {
               this.addValueFromString(var1);
            }

            return true;
         } catch (Exception var7) {
            DebugLog.General.error("Error in script string '" + var1 + "'");
            var7.printStackTrace();
            return false;
         }
      }

      public void addValueFromString(java.lang.String var1) {
         java.lang.Enum var2 = ((AttributeType.EnumSet)this.type).enumValueFromString(var1);
         if (var2 != null) {
            this.value.add(var2);
         } else {
            throw new NullPointerException("Attribute.EnumSet Cannot read script value '" + var1 + "'.");
         }
      }

      public boolean removeValueFromString(java.lang.String var1) {
         java.lang.Enum var2 = ((AttributeType.EnumSet)this.type).enumValueFromString(var1);
         if (var2 != null) {
            return this.value.remove(var2);
         } else {
            throw new NullPointerException("Attribute.EnumSet Cannot read script value '" + var1 + "'.");
         }
      }

      public void clear() {
         this.value.clear();
      }

      public boolean equalTo(EnumSet<E> var1) {
         return this.type == var1.type ? this.value.equals(var1.value) : true;
      }

      public EnumSet<E> copy() {
         EnumSet var1 = AttributeFactory.AllocAttributeEnumSet();
         var1.setType((AttributeType.EnumSet)this.type);
         var1.value.addAll(this.value);
         return var1;
      }

      protected void release() {
         AttributeFactory.Release(this);
      }

      protected void reset() {
         super.reset();
         this.value = null;
      }

      public void save(ByteBuffer var1) {
         var1.put((byte)this.value.size());
         Iterator var2 = this.value.iterator();

         while(var2.hasNext()) {
            java.lang.Enum var3 = (java.lang.Enum)var2.next();
            var1.put(((IOEnum)var3).getByteId());
         }

      }

      public void load(ByteBuffer var1) {
         if (this.value.size() > 0) {
            this.value.clear();
         }

         byte var2 = var1.get();

         for(int var4 = 0; var4 < var2; ++var4) {
            byte var3 = var1.get();
            java.lang.Enum var5 = ((AttributeType.EnumSet)this.type).enumValueFromByteID(var3);
            if (var5 != null) {
               this.value.add(((AttributeType.EnumSet)this.type).enumValueFromByteID(var3));
            } else {
               DebugLog.General.error("Could not load value for EnumSet attribute '" + this.type + "'.");
            }
         }

      }
   }

   public static class Enum<E extends java.lang.Enum<E> & IOEnum> extends AttributeInstance<Enum<E>, AttributeType.Enum<E>> {
      private E value;

      public Enum() {
      }

      protected void setType(AttributeType.Enum<E> var1) {
         this.type = var1;
         this.value = var1.getInitialValue();
      }

      public E getValue() {
         return this.value;
      }

      public void setValue(E var1) {
         if (this.canSetValue()) {
            this.value = var1;
         }

      }

      public java.lang.String stringValue() {
         return this.value.toString();
      }

      public boolean setValueFromScriptString(java.lang.String var1) {
         this.value = ((AttributeType.Enum)this.type).enumValueFromString(var1);
         if (this.value == null) {
            this.value = ((AttributeType.Enum)this.type).getInitialValue();
         }

         return true;
      }

      public boolean equalTo(Enum<E> var1) {
         if (this.type == var1.type) {
            return this.value == var1.value;
         } else {
            return true;
         }
      }

      public Enum<E> copy() {
         Enum var1 = AttributeFactory.AllocAttributeEnum();
         var1.setType((AttributeType.Enum)this.type);
         var1.value = this.value;
         return var1;
      }

      protected void release() {
         AttributeFactory.Release(this);
      }

      protected void reset() {
         super.reset();
         this.value = null;
      }

      public void save(ByteBuffer var1) {
         var1.put(((IOEnum)this.value).getByteId());
      }

      public void load(ByteBuffer var1) {
         byte var2 = var1.get();
         this.value = ((AttributeType.Enum)this.type).enumValueFromByteID(var2);
         if (this.value == null) {
            DebugLog.General.error("Could not load value for Enum attribute '" + this.type + "', setting default.");
            this.value = ((AttributeType.Enum)this.type).getInitialValue();
         }

      }
   }
}
