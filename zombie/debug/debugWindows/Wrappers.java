package zombie.debug.debugWindows;

import imgui.ImGui;
import imgui.type.ImBoolean;
import imgui.type.ImString;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import zombie.core.math.PZMath;

abstract class Wrappers {
   private static final int[] valueInt = new int[1];
   private static final float[] valueFloat = new float[1];
   protected static final ImBoolean valueBoolean = new ImBoolean();
   protected static final ImString valueString = new ImString();

   Wrappers() {
   }

   public static int sliderInt(String var0, int var1, int var2, int var3) {
      valueInt[0] = var1;
      ImGui.sliderInt(var0, valueInt, var2, var3);
      return valueInt[0];
   }

   public static int sliderInt(String var0, int var1, int var2, Supplier<Integer> var3, Consumer<Integer> var4) {
      valueInt[0] = (Integer)var3.get();
      ImGui.sliderInt(var0, valueInt, var1, var2);
      if (valueInt[0] != (Integer)var3.get()) {
         var4.accept(valueInt[0]);
      }

      return valueInt[0];
   }

   public static <T> int sliderInt(String var0, int var1, int var2, SupplyConsumer<T, Integer> var3, BiConsumer<T, Integer> var4, T var5) {
      valueInt[0] = (Integer)var3.get(var5);
      ImGui.sliderInt(var0, valueInt, var1, var2);
      if (valueInt[0] != (Integer)var3.get(var5)) {
         var4.accept(var5, valueInt[0]);
      }

      return valueInt[0];
   }

   public static float sliderFloat(String var0, float var1, float var2, float var3) {
      valueFloat[0] = var1;
      ImGui.sliderFloat(var0, valueFloat, var2, var3, "%.3f");
      return valueFloat[0];
   }

   public static float sliderFloat(String var0, float var1, float var2, Supplier<Float> var3, Consumer<Float> var4) {
      valueFloat[0] = (Float)var3.get();
      ImGui.sliderFloat(var0, valueFloat, var1, var2, "%.3f");
      if (PZMath.roundFloat(valueFloat[0], 3) != PZMath.roundFloat((Float)var3.get(), 3)) {
         var4.accept(valueFloat[0]);
      }

      return valueFloat[0];
   }

   public static float sliderFloat(String var0, float var1, float var2, int var3, Supplier<Float> var4, Consumer<Float> var5) {
      valueFloat[0] = (Float)var4.get();
      ImGui.sliderFloat(var0, valueFloat, var1, var2, "%." + var3 + "f");
      if (PZMath.roundFloat(valueFloat[0], var3) != PZMath.roundFloat((Float)var4.get(), var3)) {
         var5.accept(valueFloat[0]);
      }

      return valueFloat[0];
   }

   public static void sliderDouble(String var0, double var1, double var3, Supplier<Double> var5, Consumer<Double> var6) {
      valueFloat[0] = ((Double)var5.get()).floatValue();
      ImGui.sliderFloat(var0, valueFloat, (float)var1, (float)var3, "%.3f");
      if (PZMath.roundFloat(valueFloat[0], 3) != PZMath.roundFloat(((Double)var5.get()).floatValue(), 3)) {
         var6.accept((double)valueFloat[0]);
      }

   }

   public static float dragFloat(String var0, float var1, float var2, float var3, Supplier<Float> var4, Consumer<Float> var5) {
      valueFloat[0] = (Float)var4.get();
      ImGui.dragFloat(var0, valueFloat, var3, var1, var2);
      if (PZMath.roundFloat(valueFloat[0], 3) != PZMath.roundFloat((Float)var4.get(), 3)) {
         var5.accept(valueFloat[0]);
      }

      return valueFloat[0];
   }

   public static float dragFloat(String var0, float var1, float var2, float var3, float var4) {
      valueFloat[0] = var1;
      ImGui.dragFloat(var0, valueFloat, var4, var2, var3);
      return valueFloat[0];
   }

   public static boolean checkbox(String var0, boolean var1) {
      valueBoolean.set(var1);
      ImGui.checkbox(var0, valueBoolean);
      return valueBoolean.get();
   }

   public static boolean checkbox(String var0, Supplier<Boolean> var1, Consumer<Boolean> var2) {
      valueBoolean.set((Boolean)var1.get());
      ImGui.checkbox(var0, valueBoolean);
      if (valueBoolean.get() != (Boolean)var1.get()) {
         var2.accept(valueBoolean.get());
      }

      return valueBoolean.get();
   }

   public static <T> boolean checkbox(String var0, SupplyConsumer<T, Boolean> var1, BiConsumer<T, Boolean> var2, T var3) {
      valueBoolean.set((Boolean)var1.get(var3));
      ImGui.checkbox(var0, valueBoolean);
      if (valueBoolean.get() != (Boolean)var1.get(var3)) {
         var2.accept(var3, valueBoolean.get());
      }

      return valueBoolean.get();
   }

   public static boolean selectable(String var0, boolean var1) {
      valueBoolean.set(var1);
      ImGui.selectable(var0, valueBoolean);
      return valueBoolean.get();
   }

   public interface SupplyConsumer<T, U> {
      U get(T var1);
   }
}
