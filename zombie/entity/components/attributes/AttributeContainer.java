package zombie.entity.components.attributes;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.function.BiConsumer;
import zombie.core.Color;
import zombie.core.Colors;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.Component;
import zombie.entity.ComponentType;
import zombie.entity.network.EntityPacketType;
import zombie.entity.util.assoc.AssocArray;
import zombie.entity.util.enums.IOEnum;
import zombie.iso.IsoObject;
import zombie.scripting.entity.ComponentScript;
import zombie.scripting.entity.components.attributes.AttributesScript;
import zombie.ui.ObjectTooltip;

@DebugClassFields
public class AttributeContainer extends Component {
   private short max_attribute_id = -1;
   private final AssocArray<AttributeType, AttributeInstance<?, ?>> attributes = new AssocArray();
   public static final short STORAGE_SIZE = 64;
   private static final byte SAVE_EMPTY = 0;
   private static final byte SAVE_COMPRESSED = 1;
   private static final byte SAVE_UNCOMPRESSED_8 = 8;
   private static final byte SAVE_UNCOMPRESSED_16 = 16;

   private AttributeContainer() {
      super(ComponentType.Attributes);
   }

   protected void readFromScript(ComponentScript var1) {
      super.readFromScript(var1);
      AttributesScript var2 = (AttributesScript)var1;
      if (var2.getTemplateContainer() != null) {
         Copy(var2.getTemplateContainer(), this);
      } else {
         DebugLog.General.error("Unable to create AttributeContainer from script: " + var1.getName());
      }

   }

   public String toString() {
      boolean var1 = this.owner != null && this.owner instanceof IsoObject;
      String var2 = "";

      for(int var3 = 0; var3 < this.attributes.size(); ++var3) {
         var2 = var2 + ((AttributeType)this.attributes.getKey(var3)).toString() + ";";
      }

      return "AttributeContainer [owner = " + (this.owner != null ? this.owner.toString() : "null") + ", iso = " + var1 + ", attributes = " + var2 + "]";
   }

   public int size() {
      return this.attributes.size();
   }

   public void forEach(BiConsumer<AttributeType, AttributeInstance> var1) {
      this.attributes.forEach(var1);
   }

   public boolean contains(AttributeType var1) {
      return this.attributes.containsKey(var1);
   }

   public void remove(AttributeType var1) {
      this.removeAndRelease(var1);
   }

   private AttributeInstance removeAndRelease(AttributeType var1) {
      AttributeInstance var2 = (AttributeInstance)this.attributes.remove(var1);
      if (var2 != null) {
         var2.release();
      }

      return var2;
   }

   protected AttributeInstance<?, ?> getOrAdd(AttributeType var1) {
      AttributeInstance var2 = (AttributeInstance)this.attributes.get(var1);
      if (var2 == null) {
         var2 = AttributeFactory.Create(var1);
         this.attributes.put(var1, var2);
         if (var2.getType().id() > this.max_attribute_id) {
            this.max_attribute_id = var2.getType().id();
         }
      }

      return var2;
   }

   public boolean add(AttributeType var1) {
      if (!this.contains(var1)) {
         AttributeInstance var2 = AttributeFactory.Create(var1);
         this.attributes.put(var1, var2);
         if (var2.getType().id() > this.max_attribute_id) {
            this.max_attribute_id = var2.getType().id();
         }
      }

      return false;
   }

   public final boolean putFromScript(AttributeType var1, String var2) {
      AttributeInstance var3 = this.getOrAdd(var1);
      return var3.setValueFromScriptString(var2);
   }

   public final <E extends Enum<E> & IOEnum> void put(AttributeType.Enum<E> var1, E var2) {
      AttributeInstance var3 = this.getOrAdd(var1);
      ((AttributeInstance.Enum)var3).setValue(var2);
   }

   public final <E extends Enum<E> & IOEnum> void set(AttributeType.Enum<E> var1, E var2) {
      AttributeInstance var3 = (AttributeInstance)this.attributes.get(var1);
      if (var3 == null) {
         throw new UnsupportedOperationException("Container does not contain attribute '" + var1 + "'.");
      } else {
         ((AttributeInstance.Enum)var3).setValue(var2);
      }
   }

   public final <E extends Enum<E> & IOEnum> E get(AttributeType.Enum<E> var1) {
      AttributeInstance var2 = (AttributeInstance)this.attributes.get(var1);
      if (var2 == null) {
         throw new UnsupportedOperationException("Container does not contain attribute '" + var1 + "'.");
      } else {
         return ((AttributeInstance.Enum)var2).getValue();
      }
   }

   public final <E extends Enum<E> & IOEnum> E get(AttributeType.Enum<E> var1, E var2) {
      AttributeInstance var3 = (AttributeInstance)this.attributes.get(var1);
      return var3 != null ? ((AttributeInstance.Enum)var3).getValue() : var2;
   }

   public final <E extends Enum<E> & IOEnum> void put(AttributeType.EnumSet<E> var1, EnumSet<E> var2) {
      AttributeInstance var3 = this.getOrAdd(var1);
      ((AttributeInstance.EnumSet)var3).setValue(var2);
   }

   public final <E extends Enum<E> & IOEnum> void set(AttributeType.EnumSet<E> var1, EnumSet<E> var2) {
      AttributeInstance var3 = (AttributeInstance)this.attributes.get(var1);
      if (var3 == null) {
         throw new UnsupportedOperationException("Container does not contain attribute '" + var1 + "'.");
      } else {
         ((AttributeInstance.EnumSet)var3).setValue(var2);
      }
   }

   public final <E extends Enum<E> & IOEnum> EnumSet<E> get(AttributeType.EnumSet<E> var1) {
      AttributeInstance var2 = (AttributeInstance)this.attributes.get(var1);
      if (var2 == null) {
         throw new UnsupportedOperationException("Container does not contain attribute '" + var1 + "'.");
      } else {
         return ((AttributeInstance.EnumSet)var2).getValue();
      }
   }

   public final <E extends Enum<E> & IOEnum> void put(AttributeType.EnumStringSet<E> var1, EnumStringObj<E> var2) {
      AttributeInstance var3 = this.getOrAdd(var1);
      ((AttributeInstance.EnumStringSet)var3).setValue(var2);
   }

   public final <E extends Enum<E> & IOEnum> void set(AttributeType.EnumStringSet<E> var1, EnumStringObj<E> var2) {
      AttributeInstance var3 = (AttributeInstance)this.attributes.get(var1);
      if (var3 == null) {
         throw new UnsupportedOperationException("Container does not contain attribute '" + var1 + "'.");
      } else {
         ((AttributeInstance.EnumStringSet)var3).setValue(var2);
      }
   }

   public final <E extends Enum<E> & IOEnum> EnumStringObj<E> get(AttributeType.EnumStringSet<E> var1) {
      AttributeInstance var2 = (AttributeInstance)this.attributes.get(var1);
      if (var2 == null) {
         throw new UnsupportedOperationException("Container does not contain attribute '" + var1 + "'.");
      } else {
         return ((AttributeInstance.EnumStringSet)var2).getValue();
      }
   }

   public final void put(AttributeType.String var1, String var2) {
      AttributeInstance var3 = this.getOrAdd(var1);
      ((AttributeInstance.String)var3).setValue(var2);
   }

   public final void set(AttributeType.String var1, String var2) {
      AttributeInstance var3 = (AttributeInstance)this.attributes.get(var1);
      if (var3 == null) {
         throw new UnsupportedOperationException("Container does not contain attribute '" + var1 + "'.");
      } else {
         ((AttributeInstance.String)var3).setValue(var2);
      }
   }

   public final String get(AttributeType.String var1) {
      AttributeInstance var2 = (AttributeInstance)this.attributes.get(var1);
      if (var2 == null) {
         throw new UnsupportedOperationException("Container does not contain attribute '" + var1 + "'.");
      } else {
         return ((AttributeInstance.String)var2).getValue();
      }
   }

   public final String get(AttributeType.String var1, String var2) {
      AttributeInstance var3 = (AttributeInstance)this.attributes.get(var1);
      return var3 != null ? ((AttributeInstance.String)var3).getValue() : var2;
   }

   public final void put(AttributeType.Bool var1, boolean var2) {
      AttributeInstance var3 = this.getOrAdd(var1);
      ((AttributeInstance.Bool)var3).setValue(var2);
   }

   public final void set(AttributeType.Bool var1, boolean var2) {
      AttributeInstance var3 = (AttributeInstance)this.attributes.get(var1);
      if (var3 == null) {
         throw new UnsupportedOperationException("Container does not contain attribute '" + var1 + "'.");
      } else {
         ((AttributeInstance.Bool)var3).setValue(var2);
      }
   }

   public final boolean get(AttributeType.Bool var1) {
      AttributeInstance var2 = (AttributeInstance)this.attributes.get(var1);
      if (var2 == null) {
         throw new UnsupportedOperationException("Container does not contain attribute '" + var1 + "'.");
      } else {
         return ((AttributeInstance.Bool)var2).getValue();
      }
   }

   public final boolean get(AttributeType.Bool var1, boolean var2) {
      AttributeInstance var3 = (AttributeInstance)this.attributes.get(var1);
      return var3 != null ? ((AttributeInstance.Bool)var3).getValue() : var2;
   }

   public final void putFloatValue(AttributeType.Numeric var1, float var2) {
      AttributeInstance var3 = this.getOrAdd(var1);
      if (var3 == null) {
         throw new UnsupportedOperationException("Container does not contain attribute '" + var1 + "'.");
      } else {
         ((AttributeInstance.Numeric)var3).fromFloat(var2);
      }
   }

   public final void setFloatValue(AttributeType.Numeric var1, float var2) {
      AttributeInstance var3 = (AttributeInstance)this.attributes.get(var1);
      if (var3 == null) {
         throw new UnsupportedOperationException("Container does not contain attribute '" + var1 + "'.");
      } else {
         ((AttributeInstance.Numeric)var3).fromFloat(var2);
      }
   }

   public final float getFloatValue(AttributeType.Numeric var1) {
      AttributeInstance var2 = (AttributeInstance)this.attributes.get(var1);
      if (var2 == null) {
         throw new UnsupportedOperationException("Container does not contain attribute '" + var1 + "'.");
      } else {
         return ((AttributeInstance.Numeric)var2).floatValue();
      }
   }

   public final float getFloatValue(AttributeType.Numeric var1, float var2) {
      AttributeInstance var3 = (AttributeInstance)this.attributes.get(var1);
      return var3 != null ? ((AttributeInstance.Numeric)var3).floatValue() : var2;
   }

   public final void put(AttributeType.Float var1, float var2) {
      AttributeInstance var3 = this.getOrAdd(var1);
      ((AttributeInstance.Float)var3).setValue(var2);
   }

   public final void set(AttributeType.Float var1, float var2) {
      AttributeInstance var3 = (AttributeInstance)this.attributes.get(var1);
      if (var3 == null) {
         throw new UnsupportedOperationException("Container does not contain attribute '" + var1 + "'.");
      } else {
         ((AttributeInstance.Float)var3).setValue(var2);
      }
   }

   public final float get(AttributeType.Float var1) {
      AttributeInstance var2 = (AttributeInstance)this.attributes.get(var1);
      if (var2 == null) {
         throw new UnsupportedOperationException("Container does not contain attribute '" + var1 + "'.");
      } else {
         return ((AttributeInstance.Float)var2).getValue();
      }
   }

   public final float get(AttributeType.Float var1, float var2) {
      AttributeInstance var3 = (AttributeInstance)this.attributes.get(var1);
      return var3 != null ? ((AttributeInstance.Float)var3).getValue() : var2;
   }

   public final void put(AttributeType.Double var1, double var2) {
      AttributeInstance var4 = this.getOrAdd(var1);
      ((AttributeInstance.Double)var4).setValue(var2);
   }

   public final void set(AttributeType.Double var1, double var2) {
      AttributeInstance var4 = (AttributeInstance)this.attributes.get(var1);
      if (var4 == null) {
         throw new UnsupportedOperationException("Container does not contain attribute '" + var1 + "'.");
      } else {
         ((AttributeInstance.Double)var4).setValue(var2);
      }
   }

   public final double get(AttributeType.Double var1) {
      AttributeInstance var2 = (AttributeInstance)this.attributes.get(var1);
      if (var2 == null) {
         throw new UnsupportedOperationException("Container does not contain attribute '" + var1 + "'.");
      } else {
         return ((AttributeInstance.Double)var2).getValue();
      }
   }

   public final double get(AttributeType.Double var1, double var2) {
      AttributeInstance var4 = (AttributeInstance)this.attributes.get(var1);
      return var4 != null ? ((AttributeInstance.Double)var4).getValue() : var2;
   }

   public final void put(AttributeType.Byte var1, byte var2) {
      AttributeInstance var3 = this.getOrAdd(var1);
      ((AttributeInstance.Byte)var3).setValue(var2);
   }

   public final void set(AttributeType.Byte var1, byte var2) {
      AttributeInstance var3 = (AttributeInstance)this.attributes.get(var1);
      if (var3 == null) {
         throw new UnsupportedOperationException("Container does not contain attribute '" + var1 + "'.");
      } else {
         ((AttributeInstance.Byte)var3).setValue(var2);
      }
   }

   public final byte get(AttributeType.Byte var1) {
      AttributeInstance var2 = (AttributeInstance)this.attributes.get(var1);
      if (var2 == null) {
         throw new UnsupportedOperationException("Container does not contain attribute '" + var1 + "'.");
      } else {
         return ((AttributeInstance.Byte)var2).getValue();
      }
   }

   public final byte get(AttributeType.Byte var1, byte var2) {
      AttributeInstance var3 = (AttributeInstance)this.attributes.get(var1);
      return var3 != null ? ((AttributeInstance.Byte)var3).getValue() : var2;
   }

   public final void put(AttributeType.Short var1, short var2) {
      AttributeInstance var3 = this.getOrAdd(var1);
      ((AttributeInstance.Short)var3).setValue(var2);
   }

   public final void set(AttributeType.Short var1, short var2) {
      AttributeInstance var3 = (AttributeInstance)this.attributes.get(var1);
      if (var3 == null) {
         throw new UnsupportedOperationException("Container does not contain attribute '" + var1 + "'.");
      } else {
         ((AttributeInstance.Short)var3).setValue(var2);
      }
   }

   public final short get(AttributeType.Short var1) {
      AttributeInstance var2 = (AttributeInstance)this.attributes.get(var1);
      if (var2 == null) {
         throw new UnsupportedOperationException("Container does not contain attribute '" + var1 + "'.");
      } else {
         return ((AttributeInstance.Short)var2).getValue();
      }
   }

   public final short get(AttributeType.Short var1, short var2) {
      AttributeInstance var3 = (AttributeInstance)this.attributes.get(var1);
      return var3 != null ? ((AttributeInstance.Short)var3).getValue() : var2;
   }

   public final void put(AttributeType.Int var1, int var2) {
      AttributeInstance var3 = this.getOrAdd(var1);
      ((AttributeInstance.Int)var3).setValue(var2);
   }

   public final void set(AttributeType.Int var1, int var2) {
      AttributeInstance var3 = (AttributeInstance)this.attributes.get(var1);
      if (var3 == null) {
         throw new UnsupportedOperationException("Container does not contain attribute '" + var1 + "'.");
      } else {
         ((AttributeInstance.Int)var3).setValue(var2);
      }
   }

   public final int get(AttributeType.Int var1) {
      AttributeInstance var2 = (AttributeInstance)this.attributes.get(var1);
      if (var2 == null) {
         throw new UnsupportedOperationException("Container does not contain attribute '" + var1 + "'.");
      } else {
         return ((AttributeInstance.Int)var2).getValue();
      }
   }

   public final int get(AttributeType.Int var1, int var2) {
      AttributeInstance var3 = (AttributeInstance)this.attributes.get(var1);
      return var3 != null ? ((AttributeInstance.Int)var3).getValue() : var2;
   }

   public final void put(AttributeType.Long var1, long var2) {
      AttributeInstance var4 = this.getOrAdd(var1);
      ((AttributeInstance.Long)var4).setValue(var2);
   }

   public final void set(AttributeType.Long var1, long var2) {
      AttributeInstance var4 = (AttributeInstance)this.attributes.get(var1);
      if (var4 == null) {
         throw new UnsupportedOperationException("Container does not contain attribute '" + var1 + "'.");
      } else {
         ((AttributeInstance.Long)var4).setValue(var2);
      }
   }

   public final long get(AttributeType.Long var1) {
      AttributeInstance var2 = (AttributeInstance)this.attributes.get(var1);
      if (var2 == null) {
         throw new UnsupportedOperationException("Container does not contain attribute '" + var1 + "'.");
      } else {
         return ((AttributeInstance.Long)var2).getValue();
      }
   }

   public final long get(AttributeType.Long var1, long var2) {
      AttributeInstance var4 = (AttributeInstance)this.attributes.get(var1);
      return var4 == null ? ((AttributeInstance.Long)var4).getValue() : var2;
   }

   public AttributeType getKey(int var1) {
      return (AttributeType)this.attributes.getKey(var1);
   }

   public AttributeInstance getAttribute(int var1) {
      return (AttributeInstance)this.attributes.getValue(var1);
   }

   public AttributeInstance getAttribute(AttributeType var1) {
      return (AttributeInstance)this.attributes.get(var1);
   }

   private void recalculateMaxId() {
      this.max_attribute_id = -1;
      if (this.attributes.size() > 0) {
         for(int var2 = 0; var2 < this.attributes.size(); ++var2) {
            AttributeType var1 = (AttributeType)this.attributes.getKey(var2);
            if (var1.id() > this.max_attribute_id) {
               this.max_attribute_id = var1.id();
            }
         }

      }
   }

   protected void reset() {
      super.reset();
      this.clear();
   }

   public void clear() {
      if (this.attributes.size() > 0) {
         for(int var1 = 0; var1 < this.attributes.size(); ++var1) {
            ((AttributeInstance)this.attributes.getValue(var1)).release();
         }
      }

      this.attributes.clear();
      this.max_attribute_id = -1;
   }

   public static void Copy(AttributeContainer var0, AttributeContainer var1) {
      var1.clear();

      for(int var3 = 0; var3 < var0.attributes.size(); ++var3) {
         AttributeInstance var2 = (AttributeInstance)var0.attributes.getValue(var3);
         var1.attributes.put(var2.getType(), var2.copy());
      }

      var1.max_attribute_id = var0.max_attribute_id;
   }

   public static void Merge(AttributeContainer var0, AttributeContainer var1) {
      for(int var3 = 0; var3 < var0.attributes.size(); ++var3) {
         AttributeInstance var2 = (AttributeInstance)var0.attributes.getValue(var3);
         if (var1.attributes.containsKey(var2.getType())) {
            AttributeInstance var4 = (AttributeInstance)var1.attributes.remove(var2.getType());
            var4.release();
         }

         var1.attributes.put(var2.getType(), var2.copy());
      }

      var1.max_attribute_id = (short)Math.max(var1.max_attribute_id, var0.max_attribute_id);
   }

   public AttributeContainer copy() {
      AttributeContainer var1 = (AttributeContainer)ComponentType.Attributes.CreateComponent();
      Copy(this, var1);
      return var1;
   }

   public boolean isIdenticalTo(AttributeContainer var1) {
      if (this.size() == 0 && var1.size() == 0) {
         return true;
      } else if (this.size() != var1.size()) {
         return false;
      } else {
         for(int var4 = 0; var4 < this.attributes.size(); ++var4) {
            AttributeInstance var2 = (AttributeInstance)this.attributes.getValue(var4);
            AttributeInstance var3 = (AttributeInstance)var1.attributes.get(var2.getType());
            if (var3 == null || !var2.equalTo(var3)) {
               return false;
            }
         }

         return true;
      }
   }

   protected boolean onReceivePacket(ByteBuffer var1, EntityPacketType var2, UdpConnection var3) throws IOException {
      switch (var2) {
         default:
            return false;
      }
   }

   protected void saveSyncData(ByteBuffer var1) throws IOException {
      this.save(var1);
   }

   protected void loadSyncData(ByteBuffer var1) throws IOException {
      this.load(var1, 219);
   }

   public void save(ByteBuffer var1) {
      if (this.max_attribute_id == -1) {
         var1.put((byte)0);
      } else {
         int var2 = this.max_attribute_id > 127 ? 16 : 8;
         int var3 = 1 + this.max_attribute_id / 64;
         int var4 = this.attributes.size();
         int var6;
         if (var4 * var2 + 16 > var3 * 64) {
            var1.put((byte)1);
            var1.put((byte)var3);
            int var5 = var1.position();

            for(var6 = 0; var6 < var3; ++var6) {
               var1.putLong(0L);
            }

            for(int var12 = 0; var12 < this.attributes.size(); ++var12) {
               AttributeInstance var11 = (AttributeInstance)this.attributes.getValue(var12);
               short var10 = var11.getType().id();
               int var13 = var5 + 8 * (var10 / 64);
               int var14 = var10 % 64;
               long var17 = 1L << var14;
               int var15 = var1.position();
               var1.position(var13);
               long var8 = var1.getLong();
               var8 |= var17;
               var1.position(var13);
               var1.putLong(var8);
               var1.position(var15);
               var11.save(var1);
            }
         } else {
            var1.put((byte)var2);
            var1.putShort((short)var4);

            for(var6 = 0; var6 < this.attributes.size(); ++var6) {
               AttributeInstance var16 = (AttributeInstance)this.attributes.getValue(var6);
               if (var2 == 8) {
                  var1.put((byte)var16.getType().id());
               } else {
                  var1.putShort(var16.getType().id());
               }

               var16.save(var1);
            }
         }

      }
   }

   public void load(ByteBuffer var1, int var2) throws IOException {
      this.clear();
      byte var3 = var1.get();
      if (var3 != 0) {
         short var4;
         int var7;
         if (var3 == 1) {
            var4 = var1.get();
            if (var4 == 0) {
               return;
            }

            int var5 = var1.position();
            int var6 = var5 + var4 * 8;

            for(short var12 = 0; var12 < var4; ++var12) {
               var1.position(var5 + var12 * 8);
               long var8 = var1.getLong();
               var1.position(var6);
               long var10 = 1L;

               for(short var13 = 0; var13 < 64; ++var13) {
                  if ((var8 & var10) == var10) {
                     var7 = (short)(var12 * 64 + var13);
                     AttributeType var14 = Attribute.TypeFromId((short)var7);
                     if (var14 == null) {
                        throw new IOException("Unable to read attribute type.");
                     }

                     AttributeInstance var15 = this.getOrAdd(var14);
                     if (var15 != null) {
                        var15.load(var1);
                     }
                  }

                  var10 <<= 1;
               }

               var6 = var1.position();
            }
         } else {
            var4 = var1.getShort();

            for(var7 = 0; var7 < var4; ++var7) {
               AttributeType var16;
               if (var3 == 8) {
                  var16 = Attribute.TypeFromId((short)var1.get());
               } else {
                  var16 = Attribute.TypeFromId(var1.getShort());
               }

               if (var16 == null) {
                  throw new IOException("Unable to read attribute type.");
               }

               AttributeInstance var17 = this.getOrAdd(var16);
               if (var17 != null) {
                  var17.load(var1);
               }
            }
         }

      }
   }

   public void DoTooltip(ObjectTooltip var1, ObjectTooltip.Layout var2) {
      if (var2 != null) {
         if (this.size() > 0) {
            ArrayList var4 = new ArrayList();

            AttributeInstance var5;
            int var6;
            for(var6 = 0; var6 < this.size(); ++var6) {
               var5 = this.getAttribute(var6);
               if (!var5.isHiddenUI()) {
                  var4.add(var5);
               }
            }

            ObjectTooltip.LayoutItem var3;
            Color var10;
            if (DebugOptions.instance.TooltipAttributes.getValue()) {
               var3 = var2.addItem();
               var10 = Colors.CornFlowerBlue;
               var3.setLabel("[Debug Begin Attributes]", var10.r, var10.g, var10.b, 1.0F);
            }

            var4.sort(Comparator.comparing(AttributeInstance::getNameUI));
            Iterator var11 = var4.iterator();

            while(var11.hasNext()) {
               AttributeInstance var7 = (AttributeInstance)var11.next();
               var3 = var2.addItem();
               var3.setLabel(var7.getNameUI() + ":", 1.0F, 1.0F, 0.8F, 1.0F);
               if (var7.isDisplayAsBar()) {
                  float var8 = var7.getDisplayAsBarUnit();
                  var3.setProgress(var8, 0.0F, 0.6F, 0.0F, 0.7F);
                  if (DebugOptions.instance.TooltipAttributes.getValue()) {
                     var3 = var2.addItem();
                     var3.setLabel("*" + var7.getNameUI() + ":", 0.5F, 0.5F, 0.5F, 1.0F);
                     var3.setValue(var7.stringValue(), 0.5F, 0.5F, 0.5F, 1.0F);
                  }
               } else {
                  var3.setValue(var7.stringValue(), 1.0F, 1.0F, 1.0F, 1.0F);
               }
            }

            if (DebugOptions.instance.TooltipAttributes.getValue()) {
               var4.clear();

               for(var6 = 0; var6 < this.size(); ++var6) {
                  var5 = this.getAttribute(var6);
                  if (var5.isHiddenUI()) {
                     var4.add(var5);
                  }
               }

               if (var4.size() > 0) {
                  var3 = var2.addItem();
                  var10 = Colors.CornFlowerBlue;
                  var3.setLabel("[Debug Hidden Attributes]", var10.r, var10.g, var10.b, 1.0F);
                  Iterator var12 = var4.iterator();

                  while(var12.hasNext()) {
                     AttributeInstance var13 = (AttributeInstance)var12.next();
                     var3 = var2.addItem();
                     var3.setLabel(var13.getNameUI() + ":", 1.0F, 1.0F, 0.8F, 1.0F);
                     if (var13.isDisplayAsBar()) {
                        float var9 = var13.getDisplayAsBarUnit();
                        var3.setProgress(var9, 0.0F, 0.6F, 0.0F, 0.7F);
                        if (DebugOptions.instance.TooltipAttributes.getValue()) {
                           var3 = var2.addItem();
                           var3.setLabel("*" + var13.getNameUI() + ":", 0.5F, 0.5F, 0.5F, 1.0F);
                           var3.setValue(var13.stringValue(), 0.5F, 0.5F, 0.5F, 1.0F);
                        }
                     } else {
                        var3.setValue(var13.stringValue(), 1.0F, 1.0F, 1.0F, 1.0F);
                     }
                  }
               }

               var3 = var2.addItem();
               var10 = Colors.CornFlowerBlue;
               var3.setLabel("[Debug End Attributes]", var10.r, var10.g, var10.b, 1.0F);
            }
         }

      }
   }
}
