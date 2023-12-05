package zombie.util.lambda;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import zombie.util.IPooledObject;
import zombie.util.Lambda;
import zombie.util.Pool;
import zombie.util.PooledObject;

public final class Stacks {
   public Stacks() {
   }

   public static final class Params6 {
      public Params6() {
      }

      public static final class CallbackStackItem<T1, T2, T3, T4, T5, T6> extends StackItem<T1, T2, T3, T4, T5, T6> {
         private ICallback<T1, T2, T3, T4, T5, T6> callback;
         private static final Pool<CallbackStackItem<Object, Object, Object, Object, Object, Object>> s_pool = new Pool(CallbackStackItem::new);

         public CallbackStackItem() {
         }

         public void invoke() {
            this.callback.accept(this, this.val1, this.val2, this.val3, this.val4, this.val5, this.val6);
         }

         public static <T1, T2, T3, T4, T5, T6> CallbackStackItem<T1, T2, T3, T4, T5, T6> alloc(T1 var0, T2 var1, T3 var2, T4 var3, T5 var4, T6 var5, ICallback<T1, T2, T3, T4, T5, T6> var6) {
            CallbackStackItem var7 = (CallbackStackItem)s_pool.alloc();
            var7.val1 = var0;
            var7.val2 = var1;
            var7.val3 = var2;
            var7.val4 = var3;
            var7.val5 = var4;
            var7.val6 = var5;
            var7.callback = var6;
            return var7;
         }

         public void onReleased() {
            this.val1 = null;
            this.val2 = null;
            this.val3 = null;
            this.val4 = null;
            this.val5 = null;
            this.val6 = null;
            this.callback = null;
            super.onReleased();
         }
      }

      private abstract static class StackItem<T1, T2, T3, T4, T5, T6> extends GenericStack {
         T1 val1;
         T2 val2;
         T3 val3;
         T4 val4;
         T5 val5;
         T6 val6;

         private StackItem() {
         }
      }

      public interface ICallback<T1, T2, T3, T4, T5, T6> {
         void accept(GenericStack var1, T1 var2, T2 var3, T3 var4, T4 var5, T5 var6, T6 var7);
      }
   }

   public static final class Params5 {
      public Params5() {
      }

      public static final class CallbackStackItem<T1, T2, T3, T4, T5> extends StackItem<T1, T2, T3, T4, T5> {
         private ICallback<T1, T2, T3, T4, T5> callback;
         private static final Pool<CallbackStackItem<Object, Object, Object, Object, Object>> s_pool = new Pool(CallbackStackItem::new);

         public CallbackStackItem() {
         }

         public void invoke() {
            this.callback.accept(this, this.val1, this.val2, this.val3, this.val4, this.val5);
         }

         public static <T1, T2, T3, T4, T5> CallbackStackItem<T1, T2, T3, T4, T5> alloc(T1 var0, T2 var1, T3 var2, T4 var3, T5 var4, ICallback<T1, T2, T3, T4, T5> var5) {
            CallbackStackItem var6 = (CallbackStackItem)s_pool.alloc();
            var6.val1 = var0;
            var6.val2 = var1;
            var6.val3 = var2;
            var6.val4 = var3;
            var6.val5 = var4;
            var6.callback = var5;
            return var6;
         }

         public void onReleased() {
            this.val1 = null;
            this.val2 = null;
            this.val3 = null;
            this.val4 = null;
            this.val5 = null;
            this.callback = null;
            super.onReleased();
         }
      }

      private abstract static class StackItem<T1, T2, T3, T4, T5> extends GenericStack {
         T1 val1;
         T2 val2;
         T3 val3;
         T4 val4;
         T5 val5;

         private StackItem() {
         }
      }

      public interface ICallback<T1, T2, T3, T4, T5> {
         void accept(GenericStack var1, T1 var2, T2 var3, T3 var4, T4 var5, T5 var6);
      }
   }

   public static final class Params4 {
      public Params4() {
      }

      public static final class CallbackStackItem<T1, T2, T3, T4> extends StackItem<T1, T2, T3, T4> {
         private ICallback<T1, T2, T3, T4> callback;
         private static final Pool<CallbackStackItem<Object, Object, Object, Object>> s_pool = new Pool(CallbackStackItem::new);

         public CallbackStackItem() {
         }

         public void invoke() {
            this.callback.accept(this, this.val1, this.val2, this.val3, this.val4);
         }

         public static <T1, T2, T3, T4> CallbackStackItem<T1, T2, T3, T4> alloc(T1 var0, T2 var1, T3 var2, T4 var3, ICallback<T1, T2, T3, T4> var4) {
            CallbackStackItem var5 = (CallbackStackItem)s_pool.alloc();
            var5.val1 = var0;
            var5.val2 = var1;
            var5.val3 = var2;
            var5.val4 = var3;
            var5.callback = var4;
            return var5;
         }

         public void onReleased() {
            this.val1 = null;
            this.val2 = null;
            this.val3 = null;
            this.val4 = null;
            this.callback = null;
            super.onReleased();
         }
      }

      private abstract static class StackItem<T1, T2, T3, T4> extends GenericStack {
         T1 val1;
         T2 val2;
         T3 val3;
         T4 val4;

         private StackItem() {
         }
      }

      public interface ICallback<T1, T2, T3, T4> {
         void accept(GenericStack var1, T1 var2, T2 var3, T3 var4, T4 var5);
      }
   }

   public static final class Params3 {
      public Params3() {
      }

      public static final class CallbackStackItem<T1, T2, T3> extends StackItem<T1, T2, T3> {
         private ICallback<T1, T2, T3> callback;
         private static final Pool<CallbackStackItem<Object, Object, Object>> s_pool = new Pool(CallbackStackItem::new);

         public CallbackStackItem() {
         }

         public void invoke() {
            this.callback.accept(this, this.val1, this.val2, this.val3);
         }

         public static <T1, T2, T3> CallbackStackItem<T1, T2, T3> alloc(T1 var0, T2 var1, T3 var2, ICallback<T1, T2, T3> var3) {
            CallbackStackItem var4 = (CallbackStackItem)s_pool.alloc();
            var4.val1 = var0;
            var4.val2 = var1;
            var4.val3 = var2;
            var4.callback = var3;
            return var4;
         }

         public void onReleased() {
            this.val1 = null;
            this.val2 = null;
            this.val3 = null;
            this.callback = null;
            super.onReleased();
         }
      }

      private abstract static class StackItem<T1, T2, T3> extends GenericStack {
         T1 val1;
         T2 val2;
         T3 val3;

         private StackItem() {
         }
      }

      public interface ICallback<T1, T2, T3> {
         void accept(GenericStack var1, T1 var2, T2 var3, T3 var4);
      }
   }

   public static final class Params2 {
      public Params2() {
      }

      public static final class CallbackStackItem<T1, T2> extends StackItem<T1, T2> {
         private ICallback<T1, T2> callback;
         private static final Pool<CallbackStackItem<Object, Object>> s_pool = new Pool(CallbackStackItem::new);

         public CallbackStackItem() {
         }

         public void invoke() {
            this.callback.accept(this, this.val1, this.val2);
         }

         public static <T1, T2> CallbackStackItem<T1, T2> alloc(T1 var0, T2 var1, ICallback<T1, T2> var2) {
            CallbackStackItem var3 = (CallbackStackItem)s_pool.alloc();
            var3.val1 = var0;
            var3.val2 = var1;
            var3.callback = var2;
            return var3;
         }

         public void onReleased() {
            this.val1 = null;
            this.val2 = null;
            this.callback = null;
            super.onReleased();
         }
      }

      private abstract static class StackItem<T1, T2> extends GenericStack {
         T1 val1;
         T2 val2;

         private StackItem() {
         }
      }

      public interface ICallback<T1, T2> {
         void accept(GenericStack var1, T1 var2, T2 var3);
      }
   }

   public static final class Params1 {
      public Params1() {
      }

      public static final class CallbackStackItem<T1> extends StackItem<T1> {
         private ICallback<T1> callback;
         private static final Pool<CallbackStackItem<Object>> s_pool = new Pool(CallbackStackItem::new);

         public CallbackStackItem() {
         }

         public void invoke() {
            this.callback.accept(this, this.val1);
         }

         public static <T1> CallbackStackItem<T1> alloc(T1 var0, ICallback<T1> var1) {
            CallbackStackItem var2 = (CallbackStackItem)s_pool.alloc();
            var2.val1 = var0;
            var2.callback = var1;
            return var2;
         }

         public void onReleased() {
            this.val1 = null;
            this.callback = null;
            super.onReleased();
         }
      }

      private abstract static class StackItem<T1> extends GenericStack {
         T1 val1;

         private StackItem() {
         }
      }

      public interface ICallback<T1> {
         void accept(GenericStack var1, T1 var2);
      }
   }

   public abstract static class GenericStack extends PooledObject {
      private final List<IPooledObject> m_stackItems = new ArrayList();

      public GenericStack() {
      }

      public abstract void invoke();

      public void invokeAndRelease() {
         try {
            this.invoke();
         } finally {
            this.release();
         }

      }

      private <E> E push(E var1) {
         this.m_stackItems.add((IPooledObject)var1);
         return var1;
      }

      public void onReleased() {
         this.m_stackItems.forEach(Pool::tryRelease);
         this.m_stackItems.clear();
      }

      public <E, T1> Predicate<E> predicate(T1 var1, Predicates.Params1.ICallback<E, T1> var2) {
         return (Predicate)this.push(Lambda.predicate(var1, var2));
      }

      public <E, T1, T2> Predicate<E> predicate(T1 var1, T2 var2, Predicates.Params2.ICallback<E, T1, T2> var3) {
         return (Predicate)this.push(Lambda.predicate(var1, var2, var3));
      }

      public <E, T1, T2, T3> Predicate<E> predicate(T1 var1, T2 var2, T3 var3, Predicates.Params3.ICallback<E, T1, T2, T3> var4) {
         return (Predicate)this.push(Lambda.predicate(var1, var2, var3, var4));
      }

      public <E, T1> Comparator<E> comparator(T1 var1, Comparators.Params1.ICallback<E, T1> var2) {
         return (Comparator)this.push(Lambda.comparator(var1, var2));
      }

      public <E, T1, T2> Comparator<E> comparator(T1 var1, T2 var2, Comparators.Params2.ICallback<E, T1, T2> var3) {
         return (Comparator)this.push(Lambda.comparator(var1, var2, var3));
      }

      public <E, T1> Consumer<E> consumer(T1 var1, Consumers.Params1.ICallback<E, T1> var2) {
         return (Consumer)this.push(Lambda.consumer(var1, var2));
      }

      public <E, T1, T2> Consumer<E> consumer(T1 var1, T2 var2, Consumers.Params2.ICallback<E, T1, T2> var3) {
         return (Consumer)this.push(Lambda.consumer(var1, var2, var3));
      }

      public <T1> Runnable invoker(T1 var1, Invokers.Params1.ICallback<T1> var2) {
         return (Runnable)this.push(Lambda.invoker(var1, var2));
      }

      public <T1, T2> Runnable invoker(T1 var1, T2 var2, Invokers.Params2.ICallback<T1, T2> var3) {
         return (Runnable)this.push(Lambda.invoker(var1, var2, var3));
      }

      public <T1, T2, T3> Runnable invoker(T1 var1, T2 var2, T3 var3, Invokers.Params3.ICallback<T1, T2, T3> var4) {
         return (Runnable)this.push(Lambda.invoker(var1, var2, var3, var4));
      }

      public <T1, T2, T3, T4> Runnable invoker(T1 var1, T2 var2, T3 var3, T4 var4, Invokers.Params4.ICallback<T1, T2, T3, T4> var5) {
         return (Runnable)this.push(Lambda.invoker(var1, var2, var3, var4, var5));
      }
   }
}
