package zombie.util;

import java.util.ArrayList;
import java.util.Stack;

public class TimingHelper {
   public static TimingHelper instance = new TimingHelper();
   public boolean enabled = true;
   Timing root = null;
   Timing current = null;
   Stack<Timing> stack = new Stack();

   public TimingHelper() {
   }

   public void Start(String var1) {
      if (this.enabled) {
         Timing var2 = new Timing();
         var2.name = var1;
         if (this.root == null) {
            this.root = this.current = var2;
         } else {
            this.current.children.add(var2);
            this.stack.push(this.current);
            this.current = var2;
         }

         this.current.startTime = System.nanoTime();
      }
   }

   public void End() {
      if (this.enabled) {
         this.current.selfTime = System.nanoTime() - this.current.startTime;
         if (this.stack.isEmpty()) {
            this.current = null;
            this.Report();
            this.root = null;
         } else if (this.current != null) {
            this.current = (Timing)this.stack.pop();
         }
      }
   }

   private void Report() {
      if (this.root != null) {
         this.Report(this.root, 0);
      }
   }

   private void Report(Timing var1, int var2) {
      double var3 = (double)var1.selfTime / 1000000.0;
      String var5 = var1.name + " - " + (float)((int)(var3 * 100.0)) / 100.0F + " ms";

      for(int var6 = 0; var6 < var2; ++var6) {
         var5 = "   " + var5;
      }

      System.out.println(var5);
      ArrayList var11 = var1.children;
      double var7 = 0.0;

      for(int var9 = 0; var9 < var11.size(); ++var9) {
         Timing var10 = (Timing)var11.get(var9);
         this.Report(var10, var2 + 1);
         var7 += (double)var10.selfTime / 1000000.0;
      }

      var1.mschildren = (float)((int)(var7 * 100.0)) / 100.0F;
      var1.ms = (float)var3;
      if (var3 > 100.0) {
         boolean var12 = false;
      }

   }

   public class Timing {
      public long startTime;
      public String name;
      public ArrayList<Timing> children = new ArrayList();
      public long selfTime;
      public float mschildren;
      public float ms;

      public Timing() {
      }
   }
}
