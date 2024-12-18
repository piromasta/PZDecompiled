package zombie.core.profiling;

import zombie.GameProfiler;
import zombie.util.lambda.Invokers;

public abstract class AbstractPerformanceProfileProbe implements IPerformanceProbe {
   public final String Name;
   private boolean m_isEnabled = true;
   private boolean m_isRunning = false;
   private boolean m_isProfilerRunning = false;

   protected AbstractPerformanceProfileProbe(String var1) {
      this.Name = var1;
   }

   protected abstract void onStart();

   protected abstract void onEnd();

   public void start() {
      if (GameProfiler.isValidThread()) {
         if (this.m_isRunning) {
            throw new RuntimeException("start() already called. " + this.getClass().getSimpleName() + " is Non-reentrant. Please call end() first.");
         } else {
            this.m_isProfilerRunning = this.isEnabled() && GameProfiler.isRunning();
            if (this.m_isProfilerRunning) {
               this.m_isRunning = true;
               this.onStart();
            }
         }
      }
   }

   public boolean isEnabled() {
      return this.m_isEnabled;
   }

   public void setEnabled(boolean var1) {
      this.m_isEnabled = var1;
   }

   public void end() {
      if (GameProfiler.isValidThread()) {
         if (this.m_isProfilerRunning) {
            if (!this.m_isRunning) {
               throw new RuntimeException("end() called without calling start().");
            } else {
               this.onEnd();
               this.m_isRunning = false;
            }
         }
      }
   }

   public void invokeAndMeasure(Invokers.Params0.ICallback var1) {
      try {
         this.start();
         var1.accept();
      } finally {
         this.end();
      }

   }

   public boolean invokeAndMeasure(Invokers.Params0.Boolean.ICallback var1) {
      boolean var2;
      try {
         this.start();
         var2 = var1.accept();
      } finally {
         this.end();
      }

      return var2;
   }

   public <T1> void invokeAndMeasure(T1 var1, Invokers.Params1.ICallback<T1> var2) {
      try {
         this.start();
         var2.accept(var1);
      } finally {
         this.end();
      }

   }

   public <T1> boolean invokeAndMeasure(T1 var1, Invokers.Params1.Boolean.ICallback<T1> var2) {
      boolean var3;
      try {
         this.start();
         var3 = var2.accept(var1);
      } finally {
         this.end();
      }

      return var3;
   }

   public <T1, T2> void invokeAndMeasure(T1 var1, T2 var2, Invokers.Params2.ICallback<T1, T2> var3) {
      try {
         this.start();
         var3.accept(var1, var2);
      } finally {
         this.end();
      }

   }

   public <T1, T2> boolean invokeAndMeasure(T1 var1, T2 var2, Invokers.Params2.Boolean.ICallback<T1, T2> var3) {
      boolean var4;
      try {
         this.start();
         var4 = var3.accept(var1, var2);
      } finally {
         this.end();
      }

      return var4;
   }

   public <T1, T2, T3> void invokeAndMeasure(T1 var1, T2 var2, T3 var3, Invokers.Params3.ICallback<T1, T2, T3> var4) {
      try {
         this.start();
         var4.accept(var1, var2, var3);
      } finally {
         this.end();
      }

   }

   public <T1, T2, T3, T4> void invokeAndMeasure(T1 var1, T2 var2, T3 var3, T4 var4, Invokers.Params4.ICallback<T1, T2, T3, T4> var5) {
      try {
         this.start();
         var5.accept(var1, var2, var3, var4);
      } finally {
         this.end();
      }

   }

   public <T1, T2, T3, T4, T5> void invokeAndMeasure(T1 var1, T2 var2, T3 var3, T4 var4, T5 var5, Invokers.Params5.ICallback<T1, T2, T3, T4, T5> var6) {
      try {
         this.start();
         var6.accept(var1, var2, var3, var4, var5);
      } finally {
         this.end();
      }

   }
}
