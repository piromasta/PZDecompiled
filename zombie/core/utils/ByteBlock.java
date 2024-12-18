package zombie.core.utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedDeque;
import zombie.core.Core;

public class ByteBlock {
   private static final ConcurrentLinkedDeque<ByteBlock> pool_data_block = new ConcurrentLinkedDeque();
   private Mode mode;
   private int start_pos;
   private int length;
   private boolean safelyForceSkipOnEnd = false;

   public static ByteBlock Start(ByteBuffer var0, Mode var1) throws IOException {
      ByteBlock var2 = (ByteBlock)pool_data_block.poll();
      if (var2 == null) {
         var2 = new ByteBlock();
      }

      var2.mode = var1;
      if (var1 == ByteBlock.Mode.Save) {
         var2.start_save(var0);
      } else {
         var2.load(var0);
      }

      return var2;
   }

   public static void SkipAndEnd(ByteBuffer var0, ByteBlock var1) throws IOException {
      if (var1.mode == ByteBlock.Mode.Load) {
         var1.skipBytes(var0);
         End(var0, var1);
      } else {
         throw new IOException("Cannot skip on block of type input.");
      }
   }

   public static void End(ByteBuffer var0, ByteBlock var1) throws IOException {
      assert !Core.bDebug || !pool_data_block.contains(var1) : "Object already in pool.";

      if (var1.mode == ByteBlock.Mode.Save) {
         var1.end_save(var0);
      } else {
         if (var1.safelyForceSkipOnEnd) {
            var1.skipBytes(var0);
         }

         if (!var1.verify(var0)) {
            throw new IOException("DataBlock size mismatch during load.");
         }
      }

      var1.reset();
      pool_data_block.offer(var1);
   }

   private ByteBlock() {
      this.reset();
   }

   private void reset() {
      this.start_pos = -1;
      this.length = -1;
      this.safelyForceSkipOnEnd = false;
   }

   public void safelyForceSkipOnEnd() {
      this.safelyForceSkipOnEnd(true);
   }

   public void safelyForceSkipOnEnd(boolean var1) {
      this.safelyForceSkipOnEnd = var1;
   }

   private void validate(Mode var1) throws IOException {
      if (this.mode != var1) {
         throw new IOException("DataBlock mode mismatch.");
      }
   }

   private void start_save(ByteBuffer var1) throws IOException {
      this.validate(ByteBlock.Mode.Save);
      var1.putInt(0);
      this.start_pos = var1.position();
   }

   private void end_save(ByteBuffer var1) throws IOException {
      this.validate(ByteBlock.Mode.Save);
      this.length = var1.position() - this.start_pos;
      var1.position(this.start_pos - 4);
      var1.putInt(this.length);
      var1.position(this.start_pos + this.length);
   }

   public int length() throws IOException {
      return this.length;
   }

   private void load(ByteBuffer var1) throws IOException {
      this.validate(ByteBlock.Mode.Load);
      this.length = var1.getInt();
      this.start_pos = var1.position();
      if (this.start_pos < 0 || this.length < 0) {
         throw new IOException("DataBlock possible corruption.");
      }
   }

   public boolean verify(ByteBuffer var1) throws IOException {
      this.validate(ByteBlock.Mode.Load);
      return var1.position() == this.start_pos + this.length;
   }

   private void skipBytes(ByteBuffer var1) throws IOException {
      this.validate(ByteBlock.Mode.Load);
      var1.position(this.start_pos + this.length);
   }

   public static enum Mode {
      Save,
      Load;

      private Mode() {
      }
   }
}
