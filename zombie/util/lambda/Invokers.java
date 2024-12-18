package zombie.util.lambda;

import zombie.util.Pool;
import zombie.util.PooledObject;

public class Invokers {
   public Invokers() {
   }

   public static final class Params5 {
      public Params5() {
      }

      public static final class CallbackStackItem<T1, T2, T3, T4, T5> extends StackItem<T1, T2, T3, T4, T5> implements Runnable {
         private ICallback<T1, T2, T3, T4, T5> invoker;
         private static final Pool<CallbackStackItem<Object, Object, Object, Object, Object>> s_pool = new Pool(CallbackStackItem::new);

         public CallbackStackItem() {
         }

         public void run() {
            this.invoker.accept(this.val1, this.val2, this.val3, this.val4, this.val5);
         }

         public static <T1, T2, T3, T4, T5> CallbackStackItem<T1, T2, T3, T4, T5> alloc(T1 var0, T2 var1, T3 var2, T4 var3, T5 var4, ICallback<T1, T2, T3, T4, T5> var5) {
            CallbackStackItem var6 = (CallbackStackItem)s_pool.alloc();
            var6.val1 = var0;
            var6.val2 = var1;
            var6.val3 = var2;
            var6.val4 = var3;
            var6.val5 = var4;
            var6.invoker = var5;
            return var6;
         }

         public void onReleased() {
            this.val1 = null;
            this.val2 = null;
            this.val3 = null;
            this.val4 = null;
            this.val5 = null;
            this.invoker = null;
         }
      }

      private static class StackItem<T1, T2, T3, T4, T5> extends PooledObject {
         T1 val1;
         T2 val2;
         T3 val3;
         T4 val4;
         T5 val5;

         private StackItem() {
         }
      }

      public interface ICallback<T1, T2, T3, T4, T5> {
         void accept(T1 var1, T2 var2, T3 var3, T4 var4, T5 var5);
      }
   }

   public static final class Params4 {
      public Params4() {
      }

      public static final class CallbackStackItem<T1, T2, T3, T4> extends StackItem<T1, T2, T3, T4> implements Runnable {
         private ICallback<T1, T2, T3, T4> invoker;
         private static final Pool<CallbackStackItem<Object, Object, Object, Object>> s_pool = new Pool(CallbackStackItem::new);

         public CallbackStackItem() {
         }

         public void run() {
            this.invoker.accept(this.val1, this.val2, this.val3, this.val4);
         }

         public static <T1, T2, T3, T4> CallbackStackItem<T1, T2, T3, T4> alloc(T1 var0, T2 var1, T3 var2, T4 var3, ICallback<T1, T2, T3, T4> var4) {
            CallbackStackItem var5 = (CallbackStackItem)s_pool.alloc();
            var5.val1 = var0;
            var5.val2 = var1;
            var5.val3 = var2;
            var5.val4 = var3;
            var5.invoker = var4;
            return var5;
         }

         public void onReleased() {
            this.val1 = null;
            this.val2 = null;
            this.val3 = null;
            this.val4 = null;
            this.invoker = null;
         }
      }

      private static class StackItem<T1, T2, T3, T4> extends PooledObject {
         T1 val1;
         T2 val2;
         T3 val3;
         T4 val4;

         private StackItem() {
         }
      }

      public interface ICallback<T1, T2, T3, T4> {
         void accept(T1 var1, T2 var2, T3 var3, T4 var4);
      }
   }

   public static final class Params3 {
      public Params3() {
      }

      public static final class CallbackStackItem<T1, T2, T3> extends StackItem<T1, T2, T3> implements Runnable {
         private ICallback<T1, T2, T3> invoker;
         private static final Pool<CallbackStackItem<Object, Object, Object>> s_pool = new Pool(CallbackStackItem::new);

         public CallbackStackItem() {
         }

         public void run() {
            this.invoker.accept(this.val1, this.val2, this.val3);
         }

         public static <T1, T2, T3> CallbackStackItem<T1, T2, T3> alloc(T1 var0, T2 var1, T3 var2, ICallback<T1, T2, T3> var3) {
            CallbackStackItem var4 = (CallbackStackItem)s_pool.alloc();
            var4.val1 = var0;
            var4.val2 = var1;
            var4.val3 = var2;
            var4.invoker = var3;
            return var4;
         }

         public void onReleased() {
            this.val1 = null;
            this.val2 = null;
            this.val3 = null;
            this.invoker = null;
         }
      }

      private static class StackItem<T1, T2, T3> extends PooledObject {
         T1 val1;
         T2 val2;
         T3 val3;

         private StackItem() {
         }
      }

      public interface ICallback<T1, T2, T3> {
         void accept(T1 var1, T2 var2, T3 var3);
      }
   }

   public static final class Params2 {
      public Params2() {
      }

      public static final class Boolean {
         public Boolean() {
         }

         public interface ICallback<T1, T2> {
            boolean accept(T1 var1, T2 var2);
         }
      }

      public static final class CallbackStackItem<T1, T2> extends StackItem<T1, T2> implements Runnable {
         private ICallback<T1, T2> invoker;
         private static final Pool<CallbackStackItem<Object, Object>> s_pool = new Pool(CallbackStackItem::new);

         public CallbackStackItem() {
         }

         public void run() {
            this.invoker.accept(this.val1, this.val2);
         }

         public static <T1, T2> CallbackStackItem<T1, T2> alloc(T1 var0, T2 var1, ICallback<T1, T2> var2) {
            CallbackStackItem var3 = (CallbackStackItem)s_pool.alloc();
            var3.val1 = var0;
            var3.val2 = var1;
            var3.invoker = var2;
            return var3;
         }

         public void onReleased() {
            this.val1 = null;
            this.val2 = null;
            this.invoker = null;
         }
      }

      private static class StackItem<T1, T2> extends PooledObject {
         T1 val1;
         T2 val2;

         private StackItem() {
         }
      }

      public interface ICallback<T1, T2> {
         void accept(T1 var1, T2 var2);
      }
   }

   public static final class Params1 {
      public Params1() {
      }

      public static final class Boolean {
         public Boolean() {
         }

         public interface ICallback<T1> {
            boolean accept(T1 var1);
         }
      }

      public static final class CallbackStackItem<T1> extends StackItem<T1> implements Runnable {
         private ICallback<T1> invoker;
         private static final Pool<CallbackStackItem<Object>> s_pool = new Pool(CallbackStackItem::new);

         public CallbackStackItem() {
         }

         public void run() {
            this.invoker.accept(this.val1);
         }

         public static <T1> CallbackStackItem<T1> alloc(T1 var0, ICallback<T1> var1) {
            CallbackStackItem var2 = (CallbackStackItem)s_pool.alloc();
            var2.val1 = var0;
            var2.invoker = var1;
            return var2;
         }

         public void onReleased() {
            this.val1 = null;
            this.invoker = null;
         }
      }

      private static class StackItem<T1> extends PooledObject {
         T1 val1;

         private StackItem() {
         }
      }

      public interface ICallback<T1> {
         void accept(T1 var1);
      }
   }

   public static final class Params0 {
      public Params0() {
      }

      public static final class Boolean {
         public Boolean() {
         }

         public interface ICallback {
            boolean accept();
         }
      }

      public interface ICallback {
         void accept();
      }
   }
}
