package zombie.util;

import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import zombie.util.lambda.Comparators;
import zombie.util.lambda.Consumers;
import zombie.util.lambda.IntSupplierFunction;
import zombie.util.lambda.Invokers;
import zombie.util.lambda.Predicates;
import zombie.util.lambda.ReturnValueContainer;
import zombie.util.lambda.ReturnValueContainerPrimitives;
import zombie.util.lambda.Stacks;

public final class Lambda {
   public Lambda() {
   }

   public static <E, T1> Predicate<E> predicate(T1 var0, Predicates.Params1.ICallback<E, T1> var1) {
      return Predicates.Params1.CallbackStackItem.alloc(var0, var1);
   }

   public static <E, T1, T2> Predicate<E> predicate(T1 var0, T2 var1, Predicates.Params2.ICallback<E, T1, T2> var2) {
      return Predicates.Params2.CallbackStackItem.alloc(var0, var1, var2);
   }

   public static <E, T1, T2, T3> Predicate<E> predicate(T1 var0, T2 var1, T3 var2, Predicates.Params3.ICallback<E, T1, T2, T3> var3) {
      return Predicates.Params3.CallbackStackItem.alloc(var0, var1, var2, var3);
   }

   public static <E, T1> Comparator<E> comparator(T1 var0, Comparators.Params1.ICallback<E, T1> var1) {
      return Comparators.Params1.CallbackStackItem.alloc(var0, var1);
   }

   public static <E, T1, T2> Comparator<E> comparator(T1 var0, T2 var1, Comparators.Params2.ICallback<E, T1, T2> var2) {
      return Comparators.Params2.CallbackStackItem.alloc(var0, var1, var2);
   }

   public static <E, T1> Consumer<E> consumer(T1 var0, Consumers.Params1.ICallback<E, T1> var1) {
      return Consumers.Params1.CallbackStackItem.alloc(var0, var1);
   }

   public static <E, T1, T2> Consumer<E> consumer(T1 var0, T2 var1, Consumers.Params2.ICallback<E, T1, T2> var2) {
      return Consumers.Params2.CallbackStackItem.alloc(var0, var1, var2);
   }

   public static <E, T1, T2, T3> Consumer<E> consumer(T1 var0, T2 var1, T3 var2, Consumers.Params3.ICallback<E, T1, T2, T3> var3) {
      return Consumers.Params3.CallbackStackItem.alloc(var0, var1, var2, var3);
   }

   public static <E, T1, T2, T3, T4> Consumer<E> consumer(T1 var0, T2 var1, T3 var2, T4 var3, Consumers.Params4.ICallback<E, T1, T2, T3, T4> var4) {
      return Consumers.Params4.CallbackStackItem.alloc(var0, var1, var2, var3, var4);
   }

   public static <E, T1, T2, T3, T4, T5> Consumer<E> consumer(T1 var0, T2 var1, T3 var2, T4 var3, T5 var4, Consumers.Params5.ICallback<E, T1, T2, T3, T4, T5> var5) {
      return Consumers.Params5.CallbackStackItem.alloc(var0, var1, var2, var3, var4, var5);
   }

   public static <T1> Runnable invoker(T1 var0, Invokers.Params1.ICallback<T1> var1) {
      return Invokers.Params1.CallbackStackItem.alloc(var0, var1);
   }

   public static <T1, T2> Runnable invoker(T1 var0, T2 var1, Invokers.Params2.ICallback<T1, T2> var2) {
      return Invokers.Params2.CallbackStackItem.alloc(var0, var1, var2);
   }

   public static <T1, T2, T3> Runnable invoker(T1 var0, T2 var1, T3 var2, Invokers.Params3.ICallback<T1, T2, T3> var3) {
      return Invokers.Params3.CallbackStackItem.alloc(var0, var1, var2, var3);
   }

   public static <T1, T2, T3, T4> Runnable invoker(T1 var0, T2 var1, T3 var2, T4 var3, Invokers.Params4.ICallback<T1, T2, T3, T4> var4) {
      return Invokers.Params4.CallbackStackItem.alloc(var0, var1, var2, var3, var4);
   }

   public static <T1> void capture(T1 var0, Stacks.Params1.ICallback<T1> var1) {
      Stacks.Params1.CallbackStackItem var2 = Stacks.Params1.CallbackStackItem.alloc(var0, var1);
      var2.invokeAndRelease();
   }

   public static <T1, T2> void capture(T1 var0, T2 var1, Stacks.Params2.ICallback<T1, T2> var2) {
      Stacks.Params2.CallbackStackItem var3 = Stacks.Params2.CallbackStackItem.alloc(var0, var1, var2);
      var3.invokeAndRelease();
   }

   public static <T1, T2, T3> void capture(T1 var0, T2 var1, T3 var2, Stacks.Params3.ICallback<T1, T2, T3> var3) {
      Stacks.Params3.CallbackStackItem var4 = Stacks.Params3.CallbackStackItem.alloc(var0, var1, var2, var3);
      var4.invokeAndRelease();
   }

   public static <T1, T2, T3, T4> void capture(T1 var0, T2 var1, T3 var2, T4 var3, Stacks.Params4.ICallback<T1, T2, T3, T4> var4) {
      Stacks.Params4.CallbackStackItem var5 = Stacks.Params4.CallbackStackItem.alloc(var0, var1, var2, var3, var4);
      var5.invokeAndRelease();
   }

   public static <T1, T2, T3, T4, T5> void capture(T1 var0, T2 var1, T3 var2, T4 var3, T5 var4, Stacks.Params5.ICallback<T1, T2, T3, T4, T5> var5) {
      Stacks.Params5.CallbackStackItem var6 = Stacks.Params5.CallbackStackItem.alloc(var0, var1, var2, var3, var4, var5);
      var6.invokeAndRelease();
   }

   public static <T1, T2, T3, T4, T5, T6> void capture(T1 var0, T2 var1, T3 var2, T4 var3, T5 var4, T6 var5, Stacks.Params6.ICallback<T1, T2, T3, T4, T5, T6> var6) {
      Stacks.Params6.CallbackStackItem var7 = Stacks.Params6.CallbackStackItem.alloc(var0, var1, var2, var3, var4, var5, var6);
      var7.invokeAndRelease();
   }

   public static <E, T1> void forEach(Consumer<Consumer<E>> var0, T1 var1, Consumers.Params1.ICallback<E, T1> var2) {
      capture(var0, var1, var2, (var0x, var1x, var2x, var3) -> {
         var1x.accept(var0x.consumer(var2x, var3));
      });
   }

   public static <E, T1, T2> void forEach(Consumer<Consumer<E>> var0, T1 var1, T2 var2, Consumers.Params2.ICallback<E, T1, T2> var3) {
      capture(var0, var1, var2, var3, (var0x, var1x, var2x, var3x, var4) -> {
         var1x.accept(var0x.consumer(var2x, var3x, var4));
      });
   }

   public static <E, T1> void forEachFrom(BiConsumer<List<E>, Consumer<E>> var0, List<E> var1, T1 var2, Consumers.Params1.ICallback<E, T1> var3) {
      capture(var0, var1, var2, var3, (var0x, var1x, var2x, var3x, var4) -> {
         var1x.accept(var2x, var0x.consumer(var3x, var4));
      });
   }

   public static <E, T1, T2> void forEachFrom(BiConsumer<List<E>, Consumer<E>> var0, List<E> var1, T1 var2, T2 var3, Consumers.Params2.ICallback<E, T1, T2> var4) {
      capture(var0, var1, var2, var3, var4, (var0x, var1x, var2x, var3x, var4x, var5) -> {
         var1x.accept(var2x, var0x.consumer(var3x, var4x, var5));
      });
   }

   public static <E, F, T1> void forEachFrom(BiConsumer<F, Consumer<E>> var0, F var1, T1 var2, Consumers.Params1.ICallback<E, T1> var3) {
      capture(var0, var1, var2, var3, (var0x, var1x, var2x, var3x, var4) -> {
         var1x.accept(var2x, var0x.consumer(var3x, var4));
      });
   }

   public static <E, F, T1, T2> void forEachFrom(BiConsumer<F, Consumer<E>> var0, F var1, T1 var2, T2 var3, Consumers.Params2.ICallback<E, T1, T2> var4) {
      capture(var0, var1, var2, var3, var4, (var0x, var1x, var2x, var3x, var4x, var5) -> {
         var1x.accept(var2x, var0x.consumer(var3x, var4x, var5));
      });
   }

   public static <E, T1, R> R find(Function<Predicate<E>, R> var0, T1 var1, Predicates.Params1.ICallback<E, T1> var2) {
      ReturnValueContainer var3 = ReturnValueContainer.alloc();
      capture(var0, var1, var2, var3, (var0x, var1x, var2x, var3x, var4x) -> {
         var4x.ReturnVal = var1x.apply(var0x.predicate(var2x, var3x));
      });
      Object var4 = var3.ReturnVal;
      var3.release();
      return var4;
   }

   public static <E, T1> int indexOf(IntSupplierFunction<Predicate<E>> var0, T1 var1, Predicates.Params1.ICallback<E, T1> var2) {
      ReturnValueContainerPrimitives.RVInt var3 = ReturnValueContainerPrimitives.RVInt.alloc();
      capture(var0, var1, var2, var3, (var0x, var1x, var2x, var3x, var4x) -> {
         var4x.ReturnVal = var1x.getInt(var0x.predicate(var2x, var3x));
      });
      int var4 = var3.ReturnVal;
      var3.release();
      return var4;
   }

   public static <E, T1> boolean contains(Predicate<Predicate<E>> var0, T1 var1, Predicates.Params1.ICallback<E, T1> var2) {
      ReturnValueContainerPrimitives.RVBoolean var3 = ReturnValueContainerPrimitives.RVBoolean.alloc();
      capture(var0, var1, var2, var3, (var0x, var1x, var2x, var3x, var4x) -> {
         var4x.ReturnVal = var1x.test(var0x.predicate(var2x, var3x));
      });
      Boolean var4 = var3.ReturnVal;
      var3.release();
      return var4;
   }

   public static <E, F extends Iterable<E>, T1> boolean containsFrom(BiPredicate<F, Predicate<E>> var0, F var1, T1 var2, Predicates.Params1.ICallback<E, T1> var3) {
      ReturnValueContainerPrimitives.RVBoolean var4 = ReturnValueContainerPrimitives.RVBoolean.alloc();
      capture(var0, var1, var2, var3, var4, (var0x, var1x, var2x, var3x, var4x, var5x) -> {
         var5x.ReturnVal = var1x.test(var2x, var0x.predicate(var3x, var4x));
      });
      Boolean var5 = var4.ReturnVal;
      var4.release();
      return var5;
   }

   public static <T1> void invoke(Consumer<Runnable> var0, T1 var1, Invokers.Params1.ICallback<T1> var2) {
      capture(var0, var1, var2, (var0x, var1x, var2x, var3) -> {
         var1x.accept(var0x.invoker(var2x, var3));
      });
   }

   public static <T1, T2> void invoke(Consumer<Runnable> var0, T1 var1, T2 var2, Invokers.Params2.ICallback<T1, T2> var3) {
      capture(var0, var1, var2, var3, (var0x, var1x, var2x, var3x, var4) -> {
         var1x.accept(var0x.invoker(var2x, var3x, var4));
      });
   }
}
