package zombie.util.lambda;

import java.util.Objects;

@FunctionalInterface
public interface QuadConsumer<T, U, V, W> {
   void accept(T var1, U var2, V var3, W var4);

   default QuadConsumer<T, U, V, W> andThen(QuadConsumer<? super T, ? super U, ? super V, ? super W> var1) {
      Objects.requireNonNull(var1);
      return (var2, var3, var4, var5) -> {
         this.accept(var2, var3, var4, var5);
         var1.accept(var2, var3, var4, var5);
      };
   }
}
