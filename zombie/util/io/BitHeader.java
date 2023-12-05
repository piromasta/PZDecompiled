package zombie.util.io;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedDeque;
import zombie.core.utils.Bits;
import zombie.debug.DebugLog;

public final class BitHeader {
   private static final ConcurrentLinkedDeque<BitHeaderByte> pool_byte = new ConcurrentLinkedDeque();
   private static final ConcurrentLinkedDeque<BitHeaderShort> pool_short = new ConcurrentLinkedDeque();
   private static final ConcurrentLinkedDeque<BitHeaderInt> pool_int = new ConcurrentLinkedDeque();
   private static final ConcurrentLinkedDeque<BitHeaderLong> pool_long = new ConcurrentLinkedDeque();
   public static boolean DEBUG = true;

   private static BitHeaderBase getHeader(HeaderSize var0, ByteBuffer var1, boolean var2) {
      if (var0 == BitHeader.HeaderSize.Byte) {
         BitHeaderByte var5 = (BitHeaderByte)pool_byte.poll();
         if (var5 == null) {
            var5 = new BitHeaderByte();
         }

         var5.setBuffer(var1);
         var5.setWrite(var2);
         return var5;
      } else if (var0 == BitHeader.HeaderSize.Short) {
         BitHeaderShort var4 = (BitHeaderShort)pool_short.poll();
         if (var4 == null) {
            var4 = new BitHeaderShort();
         }

         var4.setBuffer(var1);
         var4.setWrite(var2);
         return var4;
      } else if (var0 == BitHeader.HeaderSize.Integer) {
         BitHeaderInt var6 = (BitHeaderInt)pool_int.poll();
         if (var6 == null) {
            var6 = new BitHeaderInt();
         }

         var6.setBuffer(var1);
         var6.setWrite(var2);
         return var6;
      } else if (var0 == BitHeader.HeaderSize.Long) {
         BitHeaderLong var3 = (BitHeaderLong)pool_long.poll();
         if (var3 == null) {
            var3 = new BitHeaderLong();
         }

         var3.setBuffer(var1);
         var3.setWrite(var2);
         return var3;
      } else {
         return null;
      }
   }

   private BitHeader() {
   }

   public static void debug_print() {
      if (DEBUG) {
         DebugLog.log("*********************************************");
         DebugLog.log("ByteHeader = " + pool_byte.size());
         DebugLog.log("ShortHeader = " + pool_short.size());
         DebugLog.log("IntHeader = " + pool_int.size());
         DebugLog.log("LongHeader = " + pool_long.size());
      }

   }

   public static BitHeaderWrite allocWrite(HeaderSize var0, ByteBuffer var1) {
      return allocWrite(var0, var1, false);
   }

   public static BitHeaderWrite allocWrite(HeaderSize var0, ByteBuffer var1, boolean var2) {
      BitHeaderBase var3 = getHeader(var0, var1, true);
      if (!var2) {
         var3.create();
      }

      return var3;
   }

   public static BitHeaderRead allocRead(HeaderSize var0, ByteBuffer var1) {
      return allocRead(var0, var1, false);
   }

   public static BitHeaderRead allocRead(HeaderSize var0, ByteBuffer var1, boolean var2) {
      BitHeaderBase var3 = getHeader(var0, var1, false);
      if (!var2) {
         var3.read();
      }

      return var3;
   }

   public static enum HeaderSize {
      Byte,
      Short,
      Integer,
      Long;

      private HeaderSize() {
      }
   }

   public static class BitHeaderByte extends BitHeaderBase {
      private ConcurrentLinkedDeque<BitHeaderByte> pool;
      private byte header;

      private BitHeaderByte() {
      }

      public void release() {
         this.reset();
         BitHeader.pool_byte.offer(this);
      }

      public int getLen() {
         return Bits.getLen(this.header);
      }

      protected void reset_header() {
         this.header = 0;
      }

      protected void write_header() {
         this.buffer.put(this.header);
      }

      protected void read_header() {
         this.header = this.buffer.get();
      }

      protected void addflags_header(int var1) {
         this.header = Bits.addFlags(this.header, var1);
      }

      protected void addflags_header(long var1) {
         this.header = Bits.addFlags(this.header, var1);
      }

      protected boolean hasflags_header(int var1) {
         return Bits.hasFlags(this.header, var1);
      }

      protected boolean hasflags_header(long var1) {
         return Bits.hasFlags(this.header, var1);
      }

      protected boolean equals_header(int var1) {
         return this.header == var1;
      }

      protected boolean equals_header(long var1) {
         return (long)this.header == var1;
      }
   }

   public static class BitHeaderShort extends BitHeaderBase {
      private ConcurrentLinkedDeque<BitHeaderShort> pool;
      private short header;

      private BitHeaderShort() {
      }

      public void release() {
         this.reset();
         BitHeader.pool_short.offer(this);
      }

      public int getLen() {
         return Bits.getLen(this.header);
      }

      protected void reset_header() {
         this.header = 0;
      }

      protected void write_header() {
         this.buffer.putShort(this.header);
      }

      protected void read_header() {
         this.header = this.buffer.getShort();
      }

      protected void addflags_header(int var1) {
         this.header = Bits.addFlags(this.header, var1);
      }

      protected void addflags_header(long var1) {
         this.header = Bits.addFlags(this.header, var1);
      }

      protected boolean hasflags_header(int var1) {
         return Bits.hasFlags(this.header, var1);
      }

      protected boolean hasflags_header(long var1) {
         return Bits.hasFlags(this.header, var1);
      }

      protected boolean equals_header(int var1) {
         return this.header == var1;
      }

      protected boolean equals_header(long var1) {
         return (long)this.header == var1;
      }
   }

   public static class BitHeaderInt extends BitHeaderBase {
      private ConcurrentLinkedDeque<BitHeaderInt> pool;
      private int header;

      private BitHeaderInt() {
      }

      public void release() {
         this.reset();
         BitHeader.pool_int.offer(this);
      }

      public int getLen() {
         return Bits.getLen(this.header);
      }

      protected void reset_header() {
         this.header = 0;
      }

      protected void write_header() {
         this.buffer.putInt(this.header);
      }

      protected void read_header() {
         this.header = this.buffer.getInt();
      }

      protected void addflags_header(int var1) {
         this.header = Bits.addFlags(this.header, var1);
      }

      protected void addflags_header(long var1) {
         this.header = Bits.addFlags(this.header, var1);
      }

      protected boolean hasflags_header(int var1) {
         return Bits.hasFlags(this.header, var1);
      }

      protected boolean hasflags_header(long var1) {
         return Bits.hasFlags(this.header, var1);
      }

      protected boolean equals_header(int var1) {
         return this.header == var1;
      }

      protected boolean equals_header(long var1) {
         return (long)this.header == var1;
      }
   }

   public static class BitHeaderLong extends BitHeaderBase {
      private ConcurrentLinkedDeque<BitHeaderLong> pool;
      private long header;

      private BitHeaderLong() {
      }

      public void release() {
         this.reset();
         BitHeader.pool_long.offer(this);
      }

      public int getLen() {
         return Bits.getLen(this.header);
      }

      protected void reset_header() {
         this.header = 0L;
      }

      protected void write_header() {
         this.buffer.putLong(this.header);
      }

      protected void read_header() {
         this.header = this.buffer.getLong();
      }

      protected void addflags_header(int var1) {
         this.header = Bits.addFlags(this.header, var1);
      }

      protected void addflags_header(long var1) {
         this.header = Bits.addFlags(this.header, var1);
      }

      protected boolean hasflags_header(int var1) {
         return Bits.hasFlags(this.header, var1);
      }

      protected boolean hasflags_header(long var1) {
         return Bits.hasFlags(this.header, var1);
      }

      protected boolean equals_header(int var1) {
         return this.header == (long)var1;
      }

      protected boolean equals_header(long var1) {
         return this.header == var1;
      }
   }

   public abstract static class BitHeaderBase implements BitHeaderRead, BitHeaderWrite {
      protected boolean isWrite;
      protected ByteBuffer buffer;
      protected int start_pos = -1;

      public BitHeaderBase() {
      }

      protected void setBuffer(ByteBuffer var1) {
         this.buffer = var1;
      }

      protected void setWrite(boolean var1) {
         this.isWrite = var1;
      }

      public int getStartPosition() {
         return this.start_pos;
      }

      protected void reset() {
         this.buffer = null;
         this.isWrite = false;
         this.start_pos = -1;
         this.reset_header();
      }

      public abstract int getLen();

      public abstract void release();

      protected abstract void reset_header();

      protected abstract void write_header();

      protected abstract void read_header();

      protected abstract void addflags_header(int var1);

      protected abstract void addflags_header(long var1);

      protected abstract boolean hasflags_header(int var1);

      protected abstract boolean hasflags_header(long var1);

      protected abstract boolean equals_header(int var1);

      protected abstract boolean equals_header(long var1);

      public void create() {
         if (this.isWrite) {
            this.start_pos = this.buffer.position();
            this.reset_header();
            this.write_header();
         } else {
            throw new RuntimeException("BitHeader -> Cannot write to a non write Header.");
         }
      }

      public void write() {
         if (this.isWrite) {
            int var1 = this.buffer.position();
            this.buffer.position(this.start_pos);
            this.write_header();
            this.buffer.position(var1);
         } else {
            throw new RuntimeException("BitHeader -> Cannot write to a non write Header.");
         }
      }

      public void read() {
         if (!this.isWrite) {
            this.start_pos = this.buffer.position();
            this.read_header();
         } else {
            throw new RuntimeException("BitHeader -> Cannot read from a non read Header.");
         }
      }

      public void addFlags(int var1) {
         if (this.isWrite) {
            this.addflags_header(var1);
         } else {
            throw new RuntimeException("BitHeader -> Cannot set bits on a non write Header.");
         }
      }

      public void addFlags(long var1) {
         if (this.isWrite) {
            this.addflags_header(var1);
         } else {
            throw new RuntimeException("BitHeader -> Cannot set bits on a non write Header.");
         }
      }

      public boolean hasFlags(int var1) {
         return this.hasflags_header(var1);
      }

      public boolean hasFlags(long var1) {
         return this.hasflags_header(var1);
      }

      public boolean equals(int var1) {
         return this.equals_header(var1);
      }

      public boolean equals(long var1) {
         return this.equals_header(var1);
      }
   }
}
