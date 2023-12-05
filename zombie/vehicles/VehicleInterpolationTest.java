package zombie.vehicles;

import java.nio.ByteBuffer;
import java.util.Iterator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

public class VehicleInterpolationTest extends Assert {
   final VehicleInterpolation interpolation = new VehicleInterpolation();
   final float[] physics = new float[27];
   final float[] engineSound = new float[2];
   final ByteBuffer bb = ByteBuffer.allocateDirect(255);
   final int tick = 100;
   final int delay = 300;
   final int history = 200;
   final int bufferingIterations = 4;
   @Rule
   public TestRule watchman = new TestWatcher() {
      protected void failed(Throwable var1, Description var2) {
         System.out.println("interpolation.buffer:");
         System.out.print("TIME: ");
         Iterator var3 = VehicleInterpolationTest.this.interpolation.buffer.iterator();

         VehicleInterpolationData var4;
         while(var3.hasNext()) {
            var4 = (VehicleInterpolationData)var3.next();
            System.out.print(String.format(" %5d", var4.time));
         }

         System.out.println();
         System.out.print("   X: ");
         var3 = VehicleInterpolationTest.this.interpolation.buffer.iterator();

         while(var3.hasNext()) {
            var4 = (VehicleInterpolationData)var3.next();
            System.out.print(String.format(" %5.0f", var4.x));
         }

      }
   };

   public VehicleInterpolationTest() {
   }

   @Before
   public void setup() {
      this.interpolation.clear();
      this.interpolation.delay = 300;
      this.interpolation.history = 500;
      this.interpolation.reset();
   }

   @Test
   public void normalTest() {
      long var1 = 9223372036853775807L;

      for(int var3 = 1; var3 < 30; ++var3) {
         this.bb.position(0);
         this.interpolation.interpolationDataAdd(this.bb, var1, (float)(var3 * 2), (float)(var3 * 2), 0.0F, var1);
         boolean var4 = this.interpolation.interpolationDataGet(this.physics, this.engineSound, var1 - 298L);
         if (var3 < 4) {
            assertFalse(var4);
         } else {
            assertTrue(var4);
            assertEquals((float)(var3 - 4 + 1) * 2.0F, this.physics[0], 0.2F);
         }

         this.interpolation.interpolationDataGet(this.physics, this.engineSound, var1 - 298L + 50L);
         if (var3 < 4) {
            assertFalse(var4);
         } else {
            assertTrue(var4);
            assertEquals((float)(var3 - 4 + 1) * 2.0F + 1.0F, this.physics[0], 0.2F);
         }

         var1 += 100L;
      }

   }

   @Test
   public void interpolationTest() {
      int var1 = 0;

      for(int var2 = 1; var2 < 30; ++var2) {
         this.bb.position(0);
         if (var2 % 2 == 1) {
            this.interpolation.interpolationDataAdd(this.bb, (long)var1, (float)var2, (float)var2, 0.0F, (long)var1);
         }

         boolean var3 = this.interpolation.interpolationDataGet(this.physics, this.engineSound, (long)(var1 - 298));
         if (var2 < 4) {
            assertFalse(var3);
         } else {
            assertTrue(var3);
            assertEquals((float)(var2 - 4 + 1), this.physics[0], 0.2F);
         }

         var1 += 100;
      }

   }

   @Test
   public void interpolationMicroStepTest() {
      int var1 = 0;

      int var2;
      for(var2 = 1; var2 < 30; ++var2) {
         this.bb.position(0);
         this.interpolation.interpolationDataAdd(this.bb, (long)var1, (float)var2, (float)var2, 0.0F, (long)var1);
         boolean var3 = this.interpolation.interpolationDataGet(this.physics, this.engineSound, (long)(var1 - 298));
         if (var2 < 4) {
            assertFalse(var3);
         } else {
            assertTrue(var3);
            assertEquals((float)(var2 - 4 + 1), this.physics[0], 0.2F);
         }

         var1 += 100;
      }

      for(var2 = 30; var2 < 35; ++var2) {
         this.interpolation.interpolationDataAdd(this.bb, (long)var1, (float)var2, (float)var2, 0.0F, (long)var1);

         for(int var5 = 0; var5 < 100; ++var5) {
            boolean var4 = this.interpolation.interpolationDataGet(this.physics, this.engineSound, (long)(var1 - 300 + 100 * var5 / 100));
            assertTrue(var4);
            assertEquals((float)(var2 - 4 + 1) + (float)var5 / 100.0F, this.physics[0], 0.001F);
         }

         var1 += 100;
      }

   }

   @Test
   public void interpolationMicroStepTest2() {
      long var1 = 0L;
      byte var3 = 50;

      int var4;
      for(var4 = 1; var4 < 30; ++var4) {
         this.bb.position(0);
         this.interpolation.interpolationDataAdd(this.bb, var1, (float)var4, (float)var4, 0.0F, var1);
         boolean var5 = this.interpolation.interpolationDataGet(this.physics, this.engineSound, var1 - 298L);
         System.out.println("" + var4 + "   " + var1 + " " + var5 + " " + this.physics[0]);
         var1 += (long)var3;
      }

      for(var4 = 30; var4 < 35; ++var4) {
         this.interpolation.interpolationDataAdd(this.bb, var1, (float)var4, (float)var4, 0.0F, var1);

         for(int var7 = 0; var7 < 10; ++var7) {
            boolean var6 = this.interpolation.interpolationDataGet(this.physics, this.engineSound, var1 - 300L + (long)(var3 * var7 / 10));
            System.out.println("" + var4 + "." + var7 + " " + (var1 + (long)(var3 * var7 / 10)) + " " + var6 + " " + this.physics[0] + " " + ((float)var4 - 6.0F + (float)var7 / 10.0F));
            assertTrue(var6);
            assertEquals((float)var4 - 6.0F + (float)var7 / 10.0F, this.physics[0], 0.001F);
         }

         var1 += (long)var3;
      }

   }

   @Test
   public void testBufferRestoring() {
      int var1 = 0;

      for(int var2 = 1; var2 < 30; ++var2) {
         this.bb.position(0);
         this.interpolation.interpolationDataAdd(this.bb, (long)var1, (float)var2, (float)var2, 0.0F, (long)var1);
         boolean var3 = this.interpolation.interpolationDataGet(this.physics, this.engineSound, (long)(var1 - 298));
         System.out.println("" + var2 + " " + var1 + " " + var3 + " " + this.physics[0]);
         if (var2 >= 4 && (var2 <= 10 || var2 >= 14)) {
            assertTrue(var3);
            assertEquals((float)(var2 - 4 + 1), this.physics[0], 0.2F);
         }

         if (var2 == 10) {
            var1 += 500;
         }

         var1 += 100;
      }

   }

   @Test
   public void normalTestBufferRestoring2() {
      int var1 = 0;

      for(int var2 = 1; var2 < 100; ++var2) {
         this.bb.position(0);
         boolean var3 = var2 < 15 || var2 > 21;
         if (var3) {
            this.interpolation.interpolationDataAdd(this.bb, (long)var1, (float)var2, 0.0F, 0.0F, (long)var1);
         }

         boolean var4 = this.interpolation.interpolationDataGet(this.physics, this.engineSound, (long)(var1 - 298));
         System.out.println("" + var2 + " " + var4 + " " + this.physics[0]);
         if (var2 < 4 || var2 > 17 && var2 < 25) {
            assertFalse(var4);
         } else {
            assertTrue(var4);
            if (var2 >= 17 && var2 <= 21) {
               assertEquals(14.0F, this.physics[0], 0.1F);
            } else {
               assertEquals((float)(var2 - 4 + 1), this.physics[0], 0.1F);
            }
         }

         var1 += 100;
      }

   }

   @Test
   public void normalTestBufferRestoring3() {
      int var1 = 0;

      for(int var2 = 1; var2 < 40; ++var2) {
         this.bb.position(0);
         if (var2 != 10 && var2 != 12 && var2 != 13 && var2 != 15 && var2 != 16) {
            this.interpolation.interpolationDataAdd(this.bb, (long)var1, (float)var2, 0.0F, 0.0F, (long)var1);
         }

         if (var2 > 26 && var2 < 33) {
            this.interpolation.interpolationDataAdd(this.bb, (long)(var1 + 50), (float)var2 + 0.5F, 0.0F, 0.0F, (long)var1);
         }

         boolean var3 = this.interpolation.interpolationDataGet(this.physics, this.engineSound, (long)(var1 - 298));
         System.out.println("" + var2 + " " + var3 + " " + this.physics[0]);
         if (var2 < 4) {
            assertFalse(var3);
         } else {
            assertTrue(var3);
            assertEquals((float)(var2 - 4 + 1), this.physics[0], 0.1F);
         }

         var1 += 100;
      }

   }
}
