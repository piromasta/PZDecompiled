package zombie.core.profiling;

import zombie.util.lambda.Invokers;

public final class PerformanceProbes {
   public PerformanceProbes() {
   }

   public static Invokable.Params0.IProbe create(String var0, Invokers.Params0.ICallback var1) {
      return new Invokable.Params0.Probe(var0, var1);
   }

   public static <T1> Invokable.Params1.IProbe<T1> create(String var0, Invokers.Params1.ICallback<T1> var1) {
      return new Invokable.Params1.Probe(var0, var1);
   }

   public static <T1> Invokable.Params0.IProbe create(String var0, T1 var1, Invokers.Params1.ICallback<T1> var2) {
      return new Invokable.Params1.Probe_Cached(var0, var1, var2);
   }

   public static <T1> Invokable.Params0.Boolean.IProbe create(String var0, T1 var1, Invokers.Params1.Boolean.ICallback<T1> var2) {
      return new Invokable.Params1.Boolean.Probe_Cached(var0, var1, var2);
   }

   public static <T1, T2> Invokable.Params1.IProbe<T2> create(String var0, T1 var1, Invokers.Params2.ICallback<T1, T2> var2) {
      return new Invokable.Params2.Probe_Cached(var0, var1, var2);
   }

   public static <T1, T2> Invokable.Params1.Boolean.IProbe<T2> create(String var0, T1 var1, Invokers.Params2.Boolean.ICallback<T1, T2> var2) {
      return new Invokable.Params2.Boolean.Probe_Cached(var0, var1, var2);
   }

   public static <T1, T2, T3> Invokable.Params2.IProbe<T2, T3> create(String var0, T1 var1, Invokers.Params3.ICallback<T1, T2, T3> var2) {
      return new Invokable.Params3.Probe_Cached(var0, var1, var2);
   }

   public static <T1, T2, T3, T4> Invokable.Params3.IProbe<T2, T3, T4> create(String var0, T1 var1, Invokers.Params4.ICallback<T1, T2, T3, T4> var2) {
      return new Invokable.Params4.Probe_Cached(var0, var1, var2);
   }

   public static <T1, T2, T3, T4, T5> Invokable.Params4.IProbe<T2, T3, T4, T5> create(String var0, T1 var1, Invokers.Params5.ICallback<T1, T2, T3, T4, T5> var2) {
      return new Invokable.Params5.Probe_Cached(var0, var1, var2);
   }

   public static final class Invokable {
      public Invokable() {
      }

      public static final class Params5 {
         public Params5() {
         }

         public static class Probe_Cached<T1, T2, T3, T4, T5> extends PerformanceProfileProbe implements Params4.IProbe<T2, T3, T4, T5> {
            final T1 m_val1;
            final Invokers.Params5.ICallback<T1, T2, T3, T4, T5> m_invoker;

            public Probe_Cached(String var1, T1 var2, Invokers.Params5.ICallback<T1, T2, T3, T4, T5> var3) {
               super(var1);
               this.m_invoker = var3;
               this.m_val1 = var2;
            }

            public void invoke(T2 var1, T3 var2, T4 var3, T5 var4) {
               if (!this.isProbeEnabled()) {
                  this.m_invoker.accept(this.m_val1, var1, var2, var3, var4);
               } else {
                  this.invokeAndMeasure(this.m_val1, var1, var2, var3, var4, this.m_invoker);
               }

            }
         }

         public interface IProbe<T1, T2, T3, T4, T5> extends IPerformanceProbe {
            void invoke(T1 var1, T2 var2, T3 var3, T4 var4, T5 var5);
         }
      }

      public static final class Params4 {
         public Params4() {
         }

         public static class Probe_Cached<T1, T2, T3, T4> extends PerformanceProfileProbe implements Params3.IProbe<T2, T3, T4> {
            final T1 m_val1;
            final Invokers.Params4.ICallback<T1, T2, T3, T4> m_invoker;

            public Probe_Cached(String var1, T1 var2, Invokers.Params4.ICallback<T1, T2, T3, T4> var3) {
               super(var1);
               this.m_invoker = var3;
               this.m_val1 = var2;
            }

            public void invoke(T2 var1, T3 var2, T4 var3) {
               if (!this.isProbeEnabled()) {
                  this.m_invoker.accept(this.m_val1, var1, var2, var3);
               } else {
                  this.invokeAndMeasure(this.m_val1, var1, var2, var3, this.m_invoker);
               }

            }
         }

         public interface IProbe<T1, T2, T3, T4> extends IPerformanceProbe {
            void invoke(T1 var1, T2 var2, T3 var3, T4 var4);
         }
      }

      public static final class Params3 {
         public Params3() {
         }

         public static class Probe_Cached<T1, T2, T3> extends PerformanceProfileProbe implements Params2.IProbe<T2, T3> {
            final T1 m_val1;
            final Invokers.Params3.ICallback<T1, T2, T3> m_invoker;

            public Probe_Cached(String var1, T1 var2, Invokers.Params3.ICallback<T1, T2, T3> var3) {
               super(var1);
               this.m_invoker = var3;
               this.m_val1 = var2;
            }

            public void invoke(T2 var1, T3 var2) {
               if (!this.isProbeEnabled()) {
                  this.m_invoker.accept(this.m_val1, var1, var2);
               } else {
                  this.invokeAndMeasure(this.m_val1, var1, var2, this.m_invoker);
               }

            }
         }

         public interface IProbe<T1, T2, T3> extends IPerformanceProbe {
            void invoke(T1 var1, T2 var2, T3 var3);
         }
      }

      public static final class Params2 {
         public Params2() {
         }

         public static class Boolean {
            public Boolean() {
            }

            public static class Probe_Cached<T1, T2> extends PerformanceProfileProbe implements Params1.IProbe<T2> {
               final T1 m_val1;
               final Invokers.Params2.Boolean.ICallback<T1, T2> m_invoker;

               public Probe_Cached(String var1, T1 var2, Invokers.Params2.Boolean.ICallback<T1, T2> var3) {
                  super(var1);
                  this.m_invoker = var3;
                  this.m_val1 = var2;
               }

               public boolean invoke(T2 var1) {
                  return !this.isProbeEnabled() ? this.m_invoker.accept(this.m_val1, var1) : this.invokeAndMeasure(this.m_val1, var1, this.m_invoker);
               }
            }

            public interface IProbe<T1, T2> extends IPerformanceProbe {
               boolean invoke(T1 var1, T2 var2);
            }
         }

         public static class Probe_Cached<T1, T2> extends PerformanceProfileProbe implements Params1.IProbe<T2> {
            final T1 m_val1;
            final Invokers.Params2.ICallback<T1, T2> m_invoker;

            public Probe_Cached(String var1, T1 var2, Invokers.Params2.ICallback<T1, T2> var3) {
               super(var1);
               this.m_invoker = var3;
               this.m_val1 = var2;
            }

            public void invoke(T2 var1) {
               if (!this.isProbeEnabled()) {
                  this.m_invoker.accept(this.m_val1, var1);
               } else {
                  this.invokeAndMeasure(this.m_val1, var1, this.m_invoker);
               }

            }
         }

         public interface IProbe<T1, T2> extends IPerformanceProbe {
            void invoke(T1 var1, T2 var2);
         }
      }

      public static final class Params1 {
         public Params1() {
         }

         public static final class Boolean {
            public Boolean() {
            }

            public static class Probe_Cached<T1> extends PerformanceProfileProbe implements Params0.IProbe {
               final T1 m_val1;
               final Invokers.Params1.Boolean.ICallback<T1> m_invoker;

               public Probe_Cached(String var1, T1 var2, Invokers.Params1.Boolean.ICallback<T1> var3) {
                  super(var1);
                  this.m_invoker = var3;
                  this.m_val1 = var2;
               }

               public boolean invoke() {
                  return !this.isProbeEnabled() ? this.m_invoker.accept(this.m_val1) : this.invokeAndMeasure(this.m_val1, this.m_invoker);
               }
            }

            public static class Probe<T1> extends PerformanceProfileProbe implements IProbe<T1> {
               final Invokers.Params1.Boolean.ICallback<T1> m_invoker;

               public Probe(String var1, Invokers.Params1.Boolean.ICallback<T1> var2) {
                  super(var1);
                  this.m_invoker = var2;
               }

               public boolean invoke(T1 var1) {
                  return !this.isProbeEnabled() ? this.m_invoker.accept(var1) : this.invokeAndMeasure(var1, this.m_invoker);
               }
            }

            public interface IProbe<T1> extends IPerformanceProbe {
               boolean invoke(T1 var1);
            }
         }

         public static class Probe_Cached<T1> extends PerformanceProfileProbe implements Params0.IProbe {
            final T1 m_val1;
            final Invokers.Params1.ICallback<T1> m_invoker;

            public Probe_Cached(String var1, T1 var2, Invokers.Params1.ICallback<T1> var3) {
               super(var1);
               this.m_invoker = var3;
               this.m_val1 = var2;
            }

            public void invoke() {
               if (!this.isProbeEnabled()) {
                  this.m_invoker.accept(this.m_val1);
               } else {
                  this.invokeAndMeasure(this.m_val1, this.m_invoker);
               }

            }
         }

         public static class Probe<T1> extends PerformanceProfileProbe implements IProbe<T1> {
            final Invokers.Params1.ICallback<T1> m_invoker;

            public Probe(String var1, Invokers.Params1.ICallback<T1> var2) {
               super(var1);
               this.m_invoker = var2;
            }

            public void invoke(T1 var1) {
               if (!this.isProbeEnabled()) {
                  this.m_invoker.accept(var1);
               } else {
                  this.invokeAndMeasure(var1, this.m_invoker);
               }

            }
         }

         public interface IProbe<T1> extends IPerformanceProbe {
            void invoke(T1 var1);
         }
      }

      public static final class Params0 {
         public Params0() {
         }

         public static final class Boolean {
            public Boolean() {
            }

            public static class Probe extends PerformanceProfileProbe implements IProbe {
               private final Invokers.Params0.Boolean.ICallback m_callback;

               public Probe(String var1, Invokers.Params0.Boolean.ICallback var2) {
                  super(var1);
                  this.m_callback = var2;
               }

               public boolean invoke() {
                  return !this.isProbeEnabled() ? this.m_callback.accept() : this.invokeAndMeasure(this.m_callback);
               }
            }

            public interface IProbe extends IPerformanceProbe {
               boolean invoke();
            }
         }

         public static class Probe extends PerformanceProfileProbe implements IProbe {
            private final Invokers.Params0.ICallback m_callback;

            public Probe(String var1, Invokers.Params0.ICallback var2) {
               super(var1);
               this.m_callback = var2;
            }

            public void invoke() {
               if (!this.isProbeEnabled()) {
                  this.m_callback.accept();
               } else {
                  this.invokeAndMeasure(this.m_callback);
               }

            }
         }

         public interface IProbe extends IPerformanceProbe {
            void invoke();
         }
      }
   }
}
