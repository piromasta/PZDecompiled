package zombie.debug.objects;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedDeque;
import zombie.debug.DebugLog;

public class ObjectDebuggerLua {
   private static final ConcurrentLinkedDeque<ArrayList<String>> array_list_pool = new ConcurrentLinkedDeque();

   public ObjectDebuggerLua() {
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
      Log(var0, 2147483647, 2147483647);
   }

   public static void Log(Object var0, int var1) {
      Log(var0, var1, 2147483647);
   }

   public static void Log(Object var0, int var1, int var2) {
      ObjectDebugger.Log(DebugLog.General, var0, var1, true, true, var2);
   }

   public static void GetLines(Object var0, ArrayList<String> var1) {
      GetLines(var0, var1, 2147483647, 2147483647);
   }

   public static void GetLines(Object var0, ArrayList<String> var1, int var2) {
      GetLines(var0, var1, var2, 2147483647);
   }

   public static void GetLines(Object var0, ArrayList<String> var1, int var2, int var3) {
      ObjectDebugger.GetLines(var0, var1, var2, true, true, var3);
   }
}
