package zombie.debug.debugWindows;

import imgui.extension.implot.ImPlot;
import java.lang.reflect.Field;
import java.util.ArrayList;
import zombie.GameTime;
import zombie.debug.BaseDebugWindow;

public class Plotter extends BaseDebugWindow {
   ArrayList<PlottedVar> vars = new ArrayList();
   ArrayList<Double> valuesX = new ArrayList();
   ArrayList<Double> valuesY = new ArrayList();
   Float tick = 0.0F;
   float lastTime = -0.1F;

   public String getTitle() {
      return "Plotter";
   }

   public Plotter(Object var1, Field var2) {
      this.vars.add(new PlottedVar(var1, var2));
      this.lastTime = GameTime.getInstance().TimeOfDay - 0.01F;
   }

   public void addVariable(Object var1, Field var2) {
      this.vars.add(new PlottedVar(var1, var2));
   }

   protected void doWindowContents() {
      float var1 = GameTime.getInstance().TimeOfDay - this.lastTime;
      int var2;
      PlottedVar var3;
      if (var1 > 0.01F || var1 < 0.0F) {
         if (var1 < 0.0F) {
            var1 += 24.0F;
         }

         this.tick = this.tick + var1 / 0.01F;

         for(var2 = 0; var2 < this.vars.size(); ++var2) {
            var3 = (PlottedVar)this.vars.get(var2);
            var3.field.setAccessible(true);

            try {
               var3.valuesY.add(((Float)var3.field.get(var3.obj)).doubleValue());
            } catch (IllegalAccessException var12) {
            }
         }

         this.valuesX.add(this.tick.doubleValue());
         this.lastTime = GameTime.getInstance().TimeOfDay;
      }

      for(var2 = 0; var2 < this.vars.size(); ++var2) {
         var3 = (PlottedVar)this.vars.get(var2);
         var3.normalize();
      }

      double var13 = -1.0E8;
      double var4 = -1.0E8;
      double var6 = 1.0E8;
      double var8 = 1.0E8;

      int var10;
      for(var10 = 0; var10 < this.valuesX.size(); ++var10) {
         var6 = Math.min(var6, (Double)this.valuesX.get(var10));
         var13 = Math.max(var13, (Double)this.valuesX.get(var10));
      }

      PlottedVar var11;
      for(var10 = 0; var10 < this.vars.size(); ++var10) {
         var11 = (PlottedVar)this.vars.get(var10);
         var8 = Math.min(var8, var11.minY);
         var4 = Math.max(var4, var11.maxY);
      }

      if (this.valuesX.size() > 0) {
         ImPlot.setNextPlotLimits(var6, var13, var8, var4, 1);
      }

      if (ImPlot.beginPlot("plot", "time", "")) {
         for(var10 = 0; var10 < this.vars.size(); ++var10) {
            var11 = (PlottedVar)this.vars.get(var10);
            var11.plot(this.valuesX);
         }
      }

      ImPlot.endPlot();
   }

   public class PlottedVar {
      public Object obj;
      public Field field;
      ArrayList<Double> valuesY = new ArrayList();
      public double minY;
      public double maxY;

      public PlottedVar(Object var2, Field var3) {
         this.field = var3;
         this.obj = var2;
      }

      public void normalize() {
         double var1 = -1.0E8;
         double var3 = 1.0E8;

         for(int var5 = 0; var5 < this.valuesY.size(); ++var5) {
            var3 = Math.min(var3, (Double)this.valuesY.get(var5));
            var1 = Math.max(var1, (Double)this.valuesY.get(var5));
         }

         if (var3 >= 0.0 && var1 <= 1.0) {
            var3 = 0.0;
            var1 = 1.0;
         }

         this.minY = var3;
         this.maxY = var1;
      }

      public void plot(ArrayList<Double> var1) {
         ImPlot.plotLine(this.obj.getClass().getSimpleName() + "." + this.field.getName(), (Double[])var1.toArray(new Double[0]), (Double[])this.valuesY.toArray(new Double[0]));
      }
   }
}
