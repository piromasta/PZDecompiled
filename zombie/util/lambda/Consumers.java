package zombie.util.lambda;

import java.util.function.Consumer;
import zombie.util.Pool;
import zombie.util.PooledObject;

public final class Consumers {
   public Consumers() {
   }

   public static final class Params5 {
      public Params5() {
      }

      public static final class CallbackStackItem<E, T1, T2, T3, T4, T5> extends StackItem<T1, T2, T3, T4, T5> implements Consumer<E> {
         private ICallback<E, T1, T2, T3, T4, T5> consumer;
         private static final Pool<CallbackStackItem<Object, Object, Object, Object, Object, Object>> s_pool = new Pool(CallbackStackItem::new);

         public CallbackStackItem() {
         }

         public void accept(E var1) {
            this.consumer.accept(var1, this.val1, this.val2, this.val3, this.val4, this.val5);
         }

         public static <E, T1, T2, T3, T4, T5> CallbackStackItem<E, T1, T2, T3, T4, T5> alloc(T1 var0, T2 var1, T3 var2, T4 var3, T5 var4, ICallback<E, T1, T2, T3, T4, T5> var5) {
            CallbackStackItem var6 = (CallbackStackItem)s_pool.alloc();
            var6.val1 = var0;
            var6.val2 = var1;
            var6.val3 = var2;
            var6.val4 = var3;
            var6.val5 = var4;
            var6.consumer = var5;
            return var6;
         }

         public void onReleased() {
            this.val1 = null;
            this.val2 = null;
            this.val3 = null;
            this.val4 = null;
            this.val5 = null;
            this.consumer = null;
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

      public interface ICallback<E, T1, T2, T3, T4, T5> {
         void accept(E var1, T1 var2, T2 var3, T3 var4, T4 var5, T5 var6);
      }
   }

   public static final class Params4 {
      public Params4() {
      }

      public static final class CallbackStackItem<E, T1, T2, T3, T4> extends StackItem<T1, T2, T3, T4> implements Consumer<E> {
         private ICallback<E, T1, T2, T3, T4> consumer;
         private static final Pool<CallbackStackItem<Object, Object, Object, Object, Object>> s_pool = new Pool(CallbackStackItem::new);

         public CallbackStackItem() {
         }

         public void accept(E var1) {
            this.consumer.accept(var1, this.val1, this.val2, this.val3, this.val4);
         }

         public static <E, T1, T2, T3, T4> CallbackStackItem<E, T1, T2, T3, T4> alloc(T1 var0, T2 var1, T3 var2, T4 var3, ICallback<E, T1, T2, T3, T4> var4) {
            CallbackStackItem var5 = (CallbackStackItem)s_pool.alloc();
            var5.val1 = var0;
            var5.val2 = var1;
            var5.val3 = var2;
            var5.val4 = var3;
            var5.consumer = var4;
            return var5;
         }

         public void onReleased() {
            this.val1 = null;
            this.val2 = null;
            this.val3 = null;
            this.val4 = null;
            this.consumer = null;
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

      public interface ICallback<E, T1, T2, T3, T4> {
         void accept(E var1, T1 var2, T2 var3, T3 var4, T4 var5);
      }
   }

   public static final class Params3 {
      public Params3() {
      }

      public static final class CallbackStackItem<E, T1, T2, T3> extends StackItem<T1, T2, T3> implements Consumer<E> {
         private ICallback<E, T1, T2, T3> consumer;
         private static final Pool<CallbackStackItem<Object, Object, Object, Object>> s_pool = new Pool(CallbackStackItem::new);

         public CallbackStackItem() {
         }

         public void accept(E var1) {
            this.consumer.accept(var1, this.val1, this.val2, this.val3);
         }

         public static <E, T1, T2, T3> CallbackStackItem<E, T1, T2, T3> alloc(T1 var0, T2 var1, T3 var2, ICallback<E, T1, T2, T3> var3) {
            CallbackStackItem var4 = (CallbackStackItem)s_pool.alloc();
            var4.val1 = var0;
            var4.val2 = var1;
            var4.val3 = var2;
            var4.consumer = var3;
            return var4;
         }

         public void onReleased() {
            this.val1 = null;
            this.val2 = null;
            this.val3 = null;
            this.consumer = null;
         }
      }

      private static class StackItem<T1, T2, T3> extends PooledObject {
         T1 val1;
         T2 val2;
         T3 val3;

         private StackItem() {
         }
      }

      public interface ICallback<E, T1, T2, T3> {
         void accept(E var1, T1 var2, T2 var3, T3 var4);
      }
   }

   public static class Params2 {
      public Params2() {
      }

      public static final class CallbackStackItem<E, T1, T2> extends StackItem<T1, T2> implements Consumer<E> {
         private ICallback<E, T1, T2> consumer;
         private static final Pool<CallbackStackItem<Object, Object, Object>> s_pool = new Pool(CallbackStackItem::new);

         public CallbackStackItem() {
         }

         public void accept(E var1) {
            this.consumer.accept(var1, this.val1, this.val2);
         }

         public static <E, T1, T2> CallbackStackItem<E, T1, T2> alloc(T1 var0, T2 var1, ICallback<E, T1, T2> var2) {
            CallbackStackItem var3 = (CallbackStackItem)s_pool.alloc();
            var3.val1 = var0;
            var3.val2 = var1;
            var3.consumer = var2;
            return var3;
         }

         public void onReleased() {
            this.val1 = null;
            this.val2 = null;
            this.consumer = null;
         }
      }

      private static class StackItem<T1, T2> extends PooledObject {
         T1 val1;
         T2 val2;

         private StackItem() {
         }
      }

      public interface ICallback<E, T1, T2> {
         void accept(E var1, T1 var2, T2 var3);
      }
   }

   public static final class Params1 {
      public Params1() {
      }

      public static final class CallbackStackItem<E, T1> extends StackItem<T1> implements Consumer<E> {
         private ICallback<E, T1> consumer;
         private static final Pool<CallbackStackItem<Object, Object>> s_pool = new Pool(CallbackStackItem::new);

         public CallbackStackItem() {
         }

         public void accept(E var1) {
            this.consumer.accept(var1, this.val1);
         }

         public static <E, T1> CallbackStackItem<E, T1> alloc(T1 var0, ICallback<E, T1> var1) {
            CallbackStackItem var2 = (CallbackStackItem)s_pool.alloc();
            var2.val1 = var0;
            var2.consumer = var1;
            return var2;
         }

         public void onReleased() {
            this.val1 = null;
            this.consumer = null;
         }
      }

      private static class StackItem<T1> extends PooledObject {
         T1 val1;

         private StackItem() {
         }
      }

      public interface ICallback<E, T1> {
         void accept(E var1, T1 var2);
      }
   }
}
