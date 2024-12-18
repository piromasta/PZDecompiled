package zombie.network.packets;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Iterator;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.network.JSONField;
import zombie.network.fields.INetworkPacketField;

public interface IDescriptor {
   default void getClassDescription(StringBuilder var1, Class<?> var2, HashSet<Object> var3) {
      boolean var4 = false;
      Field[] var5 = var2.getDeclaredFields();
      int var6 = var5.length;

      for(int var7 = 0; var7 < var6; ++var7) {
         Field var8 = var5[var7];
         Annotation[] var9 = var8.getAnnotationsByType(JSONField.class);
         if (var9.length > 0) {
            String var10 = var8.getName();

            try {
               var8.setAccessible(true);
               Object var11 = var8.get(this);
               if (var4) {
                  var1.append(", ");
               } else {
                  var4 = true;
               }

               var1.append("\"").append(var10).append("\" : ").append(this.toJSON(var11, var3));
            } catch (IllegalAccessException var12) {
               DebugLog.Multiplayer.printException(var12, "INetworkPacketField.getDescription: can't get the value of the " + var10 + " field", LogSeverity.Error);
            }
         }
      }

   }

   default String getDescription(HashSet<Object> var1) {
      StringBuilder var2 = new StringBuilder("{ ");

      for(Class var3 = this.getClass(); var3 != null && var3 != Object.class; var3 = var3.getSuperclass()) {
         if (var3 != this.getClass()) {
            var2.append(" , ");
         }

         var2.append("\"").append(var3.getSimpleName()).append("\": { ");
         this.getClassDescription(var2, var3, var1);
         var2.append(" } ");
      }

      var2.append(" }");
      return var2.toString();
   }

   default String getDescription() {
      return this.getDescription(new HashSet());
   }

   private String toJSON(Object var1, HashSet<Object> var2) {
      if (var1 == null) {
         return "null";
      } else if (var1 instanceof INetworkPacketField) {
         if (var2.contains(var1)) {
            return "\"Previously described " + var1.getClass().getSimpleName() + "\"";
         } else {
            var2.add(var1);
            return ((INetworkPacketField)var1).getDescription(var2);
         }
      } else if (var1 instanceof String) {
         return "\"" + var1 + "\"";
      } else if (!(var1 instanceof Boolean) && !(var1 instanceof Byte) && !(var1 instanceof Short) && !(var1 instanceof Integer) && !(var1 instanceof Long) && !(var1 instanceof Float) && !(var1 instanceof Double)) {
         if (var1 instanceof Iterable) {
            if (var2.contains(var1)) {
               return "\"Previously described " + var1.getClass().getSimpleName() + "\"";
            } else {
               var2.add(var1);
               StringBuilder var7 = new StringBuilder("[");
               boolean var8 = true;

               for(Iterator var9 = ((Iterable)var1).iterator(); var9.hasNext(); var8 = false) {
                  if (!var8) {
                     var7.append(", ");
                  }

                  var7.append(this.toJSON(var9.next(), var2));
               }

               return "" + var7 + "]";
            }
         } else if (var1 instanceof KahluaTable) {
            if (var2.contains(var1)) {
               return "\"Previously described " + var1.getClass().getSimpleName() + "\"";
            } else {
               var2.add(var1);
               KahluaTableIterator var3 = ((KahluaTable)var1).iterator();
               StringBuilder var4 = new StringBuilder("{");
               boolean var5 = true;

               while(var3.advance()) {
                  if (!var5) {
                     var4.append(", ");
                  }

                  Object var6 = var3.getKey();
                  if (!"netAction".equals(var6)) {
                     var4.append(this.toJSON(var3.getKey(), var2));
                     var4.append(": ");
                     var4.append(this.toJSON(var3.getValue(), var2));
                     var5 = false;
                  }
               }

               var4.append("}");
               return var4.toString();
            }
         } else if (var2.contains(var1)) {
            return "\"Previously described " + var1.getClass().getSimpleName() + "\"";
         } else {
            var2.add(var1);
            return "\"" + var1 + "\"";
         }
      } else {
         return var1.toString();
      }
   }
}
