package zombie.debug.objects;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.debug.DebugLogStream;
import zombie.entity.util.assoc.AssocArray;

public class ObjectDebugger {
   private static final ConcurrentLinkedDeque<ArrayList<String>> array_list_pool = new ConcurrentLinkedDeque();
   private static final String tab_str = "  ";
   private static final ThreadLocal<Parser> localParser = new ThreadLocal<Parser>() {
      protected Parser initialValue() {
         return new Parser();
      }
   };
   private static final ConcurrentLinkedDeque<LogObject> pool_object = new ConcurrentLinkedDeque();
   private static final Comparator<LogField> fieldComparator = (var0, var1) -> {
      if (var0.value instanceof LogEntry && !(var1.value instanceof LogEntry)) {
         return 1;
      } else if (!(var0.value instanceof LogEntry) && var1.value instanceof LogEntry) {
         return -1;
      } else if (var0.isFunction && !var1.isFunction) {
         return 1;
      } else {
         return !var0.isFunction && var1.isFunction ? -1 : var0.field.compareTo(var1.field);
      }
   };
   private static final ConcurrentLinkedDeque<LogField> pool_field = new ConcurrentLinkedDeque();
   private static final ConcurrentLinkedDeque<LogList> pool_list = new ConcurrentLinkedDeque();
   private static final ConcurrentLinkedDeque<LogMap> pool_map = new ConcurrentLinkedDeque();

   public ObjectDebugger() {
   }

   public static ArrayList<String> AllocList() {
      ArrayList var0 = (ArrayList)array_list_pool.poll();
      if (var0 == null) {
         var0 = new ArrayList();
      }

      return var0;
   }

   public static void ReleaseList(ArrayList<String> var0) {
      var0.clear();
      array_list_pool.offer(var0);
   }

   public static void Log(Object var0) {
      Log(var0, 2147483647, true, true, 2147483647);
   }

   public static void Log(Object var0, boolean var1) {
      Log(var0, 2147483647, var1, true, 2147483647);
   }

   public static void Log(Object var0, boolean var1, boolean var2) {
      Log(var0, 2147483647, var1, var2, 2147483647);
   }

   public static void Log(Object var0, int var1) {
      Log(var0, var1, true, true, 2147483647);
   }

   public static void Log(Object var0, int var1, boolean var2, boolean var3, int var4) {
      Log(DebugLog.General, var0, var1, var2, var3, var4);
   }

   public static void Log(DebugLogStream var0, Object var1) {
      Log(var0, var1, 2147483647, true, true, 2147483647);
   }

   public static void Log(DebugLogStream var0, Object var1, boolean var2) {
      Log(var0, var1, 2147483647, var2, true, 2147483647);
   }

   public static void Log(DebugLogStream var0, Object var1, boolean var2, boolean var3) {
      Log(var0, var1, 2147483647, var2, var3, 2147483647);
   }

   public static void Log(DebugLogStream var0, Object var1, int var2) {
      Log(var0, var1, var2, true, true, 2147483647);
   }

   public static void Log(DebugLogStream var0, Object var1, int var2, boolean var3, boolean var4, int var5) {
      if (var0 == null) {
         var0 = DebugLog.General;
      }

      if (var1 == null) {
         var0.println("[null]");
      } else if (!Core.bDebug) {
         var0.println("ObjectDebugger can only run in debug mode.");
      } else {
         try {
            Parser var6 = (Parser)localParser.get();
            var6.parse(var1, var6.lines, var2, var3, var4, var5);

            for(int var7 = 0; var7 < var6.lines.size(); ++var7) {
               var0.println((String)var6.lines.get(var7));
            }
         } catch (Exception var11) {
            var11.printStackTrace();
         } finally {
            ((Parser)localParser.get()).reset();
         }

      }
   }

   public static void GetLines(Object var0, ArrayList<String> var1) {
      GetLines(var0, var1, 2147483647, true, false, 2147483647);
   }

   public static void GetLines(Object var0, ArrayList<String> var1, boolean var2) {
      GetLines(var0, var1, 2147483647, var2, false, 2147483647);
   }

   public static void GetLines(Object var0, ArrayList<String> var1, boolean var2, boolean var3) {
      GetLines(var0, var1, 2147483647, var2, var3, 2147483647);
   }

   public static void GetLines(Object var0, ArrayList<String> var1, int var2) {
      GetLines(var0, var1, var2, true, false, 2147483647);
   }

   public static void GetLines(Object var0, ArrayList<String> var1, int var2, boolean var3, boolean var4, int var5) {
      if (var0 == null) {
         var1.add("[null]");
      } else if (!Core.bDebug) {
         var1.add("ObjectDebugger can only run in debug mode.");
      } else {
         try {
            ((Parser)localParser.get()).parse(var0, var1, var2, var3, var4, var5);
         } catch (Exception var10) {
            var10.printStackTrace();
         } finally {
            ((Parser)localParser.get()).reset();
         }

      }
   }

   protected static LogObject alloc_log_object(Object var0) {
      LogObject var1 = (LogObject)pool_object.poll();
      if (var1 == null) {
         var1 = new LogObject();
      }

      var1.object = var0;
      return var1;
   }

   protected static LogField alloc_log_field(String var0, Object var1, boolean var2) {
      LogField var3 = (LogField)pool_field.poll();
      if (var3 == null) {
         var3 = new LogField();
      }

      var3.field = var0;
      var3.value = var1;
      var3.isFunction = var2;
      return var3;
   }

   protected static LogList alloc_log_list() {
      LogList var0 = (LogList)pool_list.poll();
      if (var0 == null) {
         var0 = new LogList();
      }

      return var0;
   }

   protected static LogMap alloc_log_map() {
      LogMap var0 = (LogMap)pool_map.poll();
      if (var0 == null) {
         var0 = new LogMap();
      }

      return var0;
   }

   private static class Parser {
      private final ArrayList<String> lines = new ArrayList();
      private final Set<Object> parsedObjects = new HashSet();
      private int originalInheritanceDepth = 2147483647;
      private boolean useClassAnnotations;
      private boolean forceAccessFields;
      private LogObject root;

      private Parser() {
      }

      private void reset() {
         this.lines.clear();
         this.parsedObjects.clear();
         this.originalInheritanceDepth = 2147483647;
         this.useClassAnnotations = false;
         this.forceAccessFields = false;
         if (this.root != null) {
            this.root.release();
         }

         this.root = null;
      }

      private boolean inheritsAnnotations(Class<?> var1, int var2) {
         if (var1.getAnnotation(DebugClass.class) == null && var1.getAnnotation(DebugClassFields.class) == null) {
            return var1.getSuperclass() != null && var2 >= 0 && this.inheritsAnnotations(var1.getSuperclass(), var2 - 1);
         } else {
            return true;
         }
      }

      private boolean validClass(Class<?> var1, int var2) {
         if (var1 == null) {
            return false;
         } else if (this.useClassAnnotations) {
            return this.inheritsAnnotations(var1, var2);
         } else if (String.class.isAssignableFrom(var1)) {
            return false;
         } else if (Boolean.class.isAssignableFrom(var1)) {
            return false;
         } else if (Byte.class.isAssignableFrom(var1)) {
            return false;
         } else if (Short.class.isAssignableFrom(var1)) {
            return false;
         } else if (Integer.class.isAssignableFrom(var1)) {
            return false;
         } else if (Long.class.isAssignableFrom(var1)) {
            return false;
         } else if (Float.class.isAssignableFrom(var1)) {
            return false;
         } else if (Double.class.isAssignableFrom(var1)) {
            return false;
         } else if (Enum.class.isAssignableFrom(var1)) {
            return false;
         } else if (Collection.class.isAssignableFrom(var1)) {
            return false;
         } else {
            return !Iterable.class.isAssignableFrom(var1);
         }
      }

      private void parse(Object var1, ArrayList<String> var2, int var3, boolean var4, boolean var5, int var6) throws Exception {
         this.originalInheritanceDepth = var3;
         this.useClassAnnotations = var5;
         this.forceAccessFields = var4;
         this.root = ObjectDebugger.alloc_log_object(var1);
         this.parseInternal(this.root, var3, var6);
         this.root.sort();
         this.root.build(var2, 0);
      }

      private void parseInternal(LogObject var1, int var2, int var3) throws Exception {
         Class var4 = var1.object.getClass();
         if (this.validClass(var4, var2)) {
            if (this.parsedObjects.contains(var1.object)) {
            }

            this.parsedObjects.add(var1.object);
            this.parseClass(var1, var4, var2, var3);
         }
      }

      private void parseClass(LogObject var1, Class<?> var2, int var3, int var4) throws Exception {
         this.parseClassMembers(var1, var2, var4);
         if (var2.getSuperclass() != null && var3 >= 0) {
            this.parseClass(var1, var2.getSuperclass(), var3 - 1, var4);
         }

      }

      private void parseClassMembers(LogObject var1, Class<?> var2, int var3) throws Exception {
         if (!this.useClassAnnotations || var2.getAnnotation(DebugClass.class) != null || var2.getAnnotation(DebugClassFields.class) != null) {
            boolean var4 = !this.useClassAnnotations || var2.getAnnotation(DebugClassFields.class) != null;
            Field[] var5 = var2.getDeclaredFields();
            Field[] var6 = var5;
            int var7 = var5.length;

            int var8;
            for(var8 = 0; var8 < var7; ++var8) {
               Field var9 = var6[var8];
               if ((var4 || var9.getAnnotation(DebugField.class) != null) && !Modifier.isStatic(var9.getModifiers()) && var9.getAnnotation(DebugIgnoreField.class) == null) {
                  boolean var10 = var9.canAccess(var1.object);
                  if (!var10) {
                     if (!this.forceAccessFields) {
                        continue;
                     }

                     if (!var9.trySetAccessible()) {
                        DebugLog.log("Cannot debug field: failed accessibility. field = " + var9.getName());
                        continue;
                     }
                  }

                  if (var1 == this.root) {
                     this.parsedObjects.clear();
                     this.parsedObjects.add(this.root.object);
                  }

                  this.parseMember(var1, var9.getName(), var9.get(var1.object), var9.getAnnotation(DebugNonRecursive.class) != null ? 0 : var3, false);
                  if (!var10) {
                     var9.setAccessible(var10);
                  }
               }
            }

            Method[] var11 = var2.getDeclaredMethods();
            Method[] var12 = var11;
            var8 = var11.length;

            for(int var13 = 0; var13 < var8; ++var13) {
               Method var14 = var12[var13];
               if (var14.getAnnotation(DebugMethod.class) != null) {
                  if (Modifier.isStatic(var14.getModifiers())) {
                     DebugLog.log("Cannot debug method: is static. method = " + var14.getName());
                  } else if (!Modifier.isPublic(var14.getModifiers())) {
                     DebugLog.log("Cannot debug method: not public. method = " + var14.getName());
                  } else if (var14.getParameterCount() > 0) {
                     DebugLog.log("Cannot debug method: has parameters. method = " + var14.getName());
                  } else if (!var1.containsMember(var14.getName())) {
                     if (var1 == this.root) {
                        this.parsedObjects.clear();
                        this.parsedObjects.add(this.root.object);
                     }

                     this.parseMember(var1, var14.getName(), var14.invoke(var1.object), var14.getAnnotation(DebugNonRecursive.class) != null ? 0 : var3, true);
                  }
               }
            }

         }
      }

      private void parseMember(LogObject var1, String var2, Object var3, int var4, boolean var5) throws Exception {
         boolean var6 = false;
         if (var3 != null) {
            Class var7 = var3.getClass();
            int var10;
            Object var11;
            LogField var22;
            if (var4 > 0 && var3 instanceof List) {
               LogList var18 = ObjectDebugger.alloc_log_list();
               List var21 = (List)var3;
               if (var21.size() > 0) {
                  for(var10 = 0; var10 < var21.size(); ++var10) {
                     var11 = var21.get(var10);
                     Class var25 = var11.getClass();
                     if (this.validClass(var25, this.originalInheritanceDepth)) {
                        LogObject var27 = ObjectDebugger.alloc_log_object(var11);
                        this.parseInternal(var27, this.originalInheritanceDepth, var4 - 1);
                        var18.addElement(var27);
                     } else {
                        var18.addElement(var11);
                     }
                  }
               }

               var22 = ObjectDebugger.alloc_log_field(var2, var18, var5);
               var1.addMember(var22);
               var6 = true;
            } else {
               Object var12;
               LogMap var17;
               if (var4 > 0 && var3 instanceof Map) {
                  var17 = ObjectDebugger.alloc_log_map();
                  Map var20 = (Map)var3;
                  Iterator var23 = var20.entrySet().iterator();

                  while(var23.hasNext()) {
                     Map.Entry var24 = (Map.Entry)var23.next();
                     var12 = var24.getKey();
                     Object var26 = var24.getValue();
                     Class var28 = var26.getClass();
                     if (this.validClass(var28, this.originalInheritanceDepth)) {
                        LogObject var15 = ObjectDebugger.alloc_log_object(var26);
                        this.parseInternal(var15, this.originalInheritanceDepth, var4 - 1);
                        var17.putElement(var12, var15);
                     } else {
                        var17.putElement(var12, var26);
                     }
                  }

                  var22 = ObjectDebugger.alloc_log_field(var2, var17, var5);
                  var1.addMember(var22);
                  var6 = true;
               } else if (var4 > 0 && var3 instanceof AssocArray) {
                  var17 = ObjectDebugger.alloc_log_map();
                  AssocArray var19 = (AssocArray)var3;

                  for(var10 = 0; var10 < var19.size(); ++var10) {
                     var11 = var19.getKey(var10);
                     var12 = var19.getValue(var10);
                     Class var13 = var12.getClass();
                     if (this.validClass(var13, this.originalInheritanceDepth)) {
                        LogObject var14 = ObjectDebugger.alloc_log_object(var12);
                        this.parseInternal(var14, this.originalInheritanceDepth, var4 - 1);
                        var17.putElement(var11, var14);
                     } else {
                        var17.putElement(var11, var12);
                     }
                  }

                  var22 = ObjectDebugger.alloc_log_field(var2, var17, var5);
                  var1.addMember(var22);
                  var6 = true;
               } else if (var4 > 0 && this.validClass(var7, this.originalInheritanceDepth)) {
                  LogObject var8 = ObjectDebugger.alloc_log_object(var3);
                  this.parseInternal(var8, this.originalInheritanceDepth, var4 - 1);
                  LogField var9 = ObjectDebugger.alloc_log_field(var2, var8, var5);
                  var1.addMember(var9);
                  var6 = true;
               }
            }
         }

         if (!var6) {
            LogField var16 = ObjectDebugger.alloc_log_field(var2, var3, var5);
            var1.addMember(var16);
         }

      }
   }

   private static class LogObject extends LogEntry {
      private Object object;
      protected final List<LogField> members = new ArrayList();

      private LogObject() {
      }

      protected boolean containsMember(String var1) {
         for(int var2 = 0; var2 < this.members.size(); ++var2) {
            if (((LogField)this.members.get(var2)).field.equals(var1)) {
               return true;
            }
         }

         return false;
      }

      protected void addMember(LogField var1) {
         if (!this.members.contains(var1)) {
            this.members.add(var1);
         }

      }

      protected String getHeader() {
         return "[" + this.object.getClass().getCanonicalName() + "]";
      }

      protected void build(ArrayList<String> var1, int var2) {
         String var10001 = "  ".repeat(var2);
         var1.add(var10001 + this.getHeader());
         var1.add("  ".repeat(var2) + "{");
         this.buildMembers(var1, var2 + 1);
         var1.add("  ".repeat(var2) + "}");
      }

      protected void buildMembers(ArrayList<String> var1, int var2) {
         for(int var3 = 0; var3 < this.members.size(); ++var3) {
            LogField var4 = (LogField)this.members.get(var3);
            var4.build(var1, var2);
         }

      }

      protected void reset() {
         this.object = null;

         for(int var1 = 0; var1 < this.members.size(); ++var1) {
            LogField var2 = (LogField)this.members.get(var1);
            var2.release();
         }

         this.members.clear();
      }

      protected void release() {
         this.reset();
         ObjectDebugger.pool_object.offer(this);
      }

      protected void sort() {
         for(int var1 = 0; var1 < this.members.size(); ++var1) {
            LogField var2 = (LogField)this.members.get(var1);
            var2.sort();
         }

         this.members.sort(ObjectDebugger.fieldComparator);
      }
   }

   private static class LogField extends LogEntry {
      protected String field;
      protected Object value;
      protected boolean isFunction = false;

      private LogField() {
      }

      protected void build(ArrayList<String> var1, int var2) {
         String var10001;
         if (this.value instanceof LogEntry) {
            if (this.value instanceof LogCollection && ((LogCollection)this.value).size() == 0) {
               var10001 = "  ".repeat(var2);
               var1.add(var10001 + this.getPrintName() + " = <EMPTY> " + this.getValueType());
            } else {
               var10001 = "  ".repeat(var2);
               var1.add(var10001 + this.getPrintName() + " = " + this.getValueType());
               var1.add("  ".repeat(var2) + "{");
               if (this.value instanceof LogObject) {
                  ((LogObject)this.value).buildMembers(var1, var2 + 1);
               } else {
                  ((LogEntry)this.value).build(var1, var2 + 1);
               }

               var1.add("  ".repeat(var2) + "}");
            }
         } else {
            var10001 = "  ".repeat(var2);
            var1.add(var10001 + this.getPrintName() + " = " + this.print(this.value));
         }

      }

      private String getPrintName() {
         return this.isFunction ? "func:" + this.field + "()" : this.field;
      }

      private String getValueType() {
         if (this.value instanceof LogObject) {
            return ((LogObject)this.value).getHeader();
         } else if (this.value instanceof LogList) {
            return "(List<T>)";
         } else {
            return this.value instanceof LogMap ? "(Map<K,V>)" : "";
         }
      }

      protected void reset() {
         this.field = null;
         if (this.value instanceof LogEntry) {
            ((LogEntry)this.value).release();
         }

         this.value = null;
         this.isFunction = false;
      }

      protected void release() {
         this.reset();
         ObjectDebugger.pool_field.offer(this);
      }

      protected void sort() {
         if (this.value instanceof LogEntry) {
            ((LogEntry)this.value).sort();
         }

      }
   }

   private static class LogList extends LogCollection {
      private final List<Object> elements = new ArrayList();

      private LogList() {
      }

      protected void addElement(Object var1) {
         this.elements.add(var1);
      }

      protected int size() {
         return this.elements.size();
      }

      protected void build(ArrayList<String> var1, int var2) {
         for(int var3 = 0; var3 < this.elements.size(); ++var3) {
            Object var4 = this.elements.get(var3);
            if (var4 instanceof LogObject) {
               var1.add("  ".repeat(var2) + "[" + var3 + "] = " + ((LogObject)var4).getHeader());
               var1.add("  ".repeat(var2) + "{");
               ((LogObject)var4).buildMembers(var1, var2 + 1);
               var1.add("  ".repeat(var2) + "}");
            } else {
               var1.add("  ".repeat(var2) + "[" + var3 + "] = " + this.print(var4));
            }
         }

      }

      protected void reset() {
         for(int var1 = 0; var1 < this.elements.size(); ++var1) {
            Object var2 = this.elements.get(var1);
            if (var2 instanceof LogEntry) {
               ((LogEntry)var2).release();
            }
         }

         this.elements.clear();
      }

      protected void release() {
         this.reset();
         ObjectDebugger.pool_list.offer(this);
      }

      protected void sort() {
         for(int var1 = 0; var1 < this.elements.size(); ++var1) {
            Object var2 = this.elements.get(var1);
            if (var2 instanceof LogEntry) {
               ((LogEntry)var2).sort();
            }
         }

      }
   }

   private static class LogMap extends LogCollection {
      private final Map<Object, Object> elements = new HashMap();

      private LogMap() {
      }

      protected void putElement(Object var1, Object var2) {
         this.elements.put(var1, var2);
      }

      protected int size() {
         return this.elements.size();
      }

      protected void build(ArrayList<String> var1, int var2) {
         Iterator var3 = this.elements.entrySet().iterator();

         while(var3.hasNext()) {
            Map.Entry var4 = (Map.Entry)var3.next();
            Object var5 = var4.getKey();
            Object var6 = var4.getValue();
            if (var6 instanceof LogObject) {
               var1.add("  ".repeat(var2) + "[" + var5 + "] = " + ((LogObject)var6).getHeader());
               var1.add("  ".repeat(var2) + "{");
               ((LogObject)var6).buildMembers(var1, var2 + 1);
               var1.add("  ".repeat(var2) + "}");
            } else {
               var1.add("  ".repeat(var2) + "[" + var5 + "] = " + this.print(var6));
            }
         }

      }

      protected void reset() {
         Iterator var1 = this.elements.entrySet().iterator();

         while(var1.hasNext()) {
            Map.Entry var2 = (Map.Entry)var1.next();
            Object var3 = var2.getValue();
            if (var3 instanceof LogEntry) {
               ((LogEntry)var3).release();
            }
         }

         this.elements.clear();
      }

      protected void release() {
         this.reset();
         ObjectDebugger.pool_map.offer(this);
      }

      protected void sort() {
         Iterator var1 = this.elements.entrySet().iterator();

         while(var1.hasNext()) {
            Map.Entry var2 = (Map.Entry)var1.next();
            Object var3 = var2.getValue();
            if (var3 instanceof LogEntry) {
               ((LogEntry)var3).sort();
            }
         }

      }
   }

   private abstract static class LogEntry {
      private LogEntry() {
      }

      protected abstract void build(ArrayList<String> var1, int var2);

      protected abstract void reset();

      protected abstract void release();

      protected abstract void sort();

      protected String print(Object var1) {
         if (var1 instanceof String) {
            return "\"" + var1 + "\"";
         } else if (var1 instanceof Byte) {
            return "(byte) " + var1;
         } else if (var1 instanceof Short) {
            return "(short) " + var1;
         } else if (var1 instanceof Integer) {
            return "(int) " + var1;
         } else if (var1 instanceof Float) {
            return "(float) " + var1;
         } else if (var1 instanceof Double) {
            return "(double) " + var1;
         } else if (var1 instanceof Long) {
            return "(long) " + var1;
         } else if (var1 instanceof Enum) {
            String var10000 = var1.getClass().getSimpleName();
            return var10000 + "." + var1;
         } else {
            return Objects.toString(var1);
         }
      }
   }

   private abstract static class LogCollection extends LogEntry {
      private LogCollection() {
      }

      protected abstract int size();
   }
}
