package zombie.core.textures;

public final class APNGFrame {
   static final int APNG_DISPOSE_OP_NONE = 0;
   static final int APNG_DISPOSE_OP_BACKGROUND = 1;
   static final int APNG_DISPOSE_OP_PREVIOUS = 2;
   static final int APNG_BLEND_OP_SOURCE = 0;
   static final int APNG_BLEND_OP_OVER = 1;
   int sequence_number;
   int width;
   int height;
   int x_offset;
   int y_offset;
   short delay_num;
   short delay_den;
   byte dispose_op;
   byte blend_op;

   public APNGFrame() {
   }

   APNGFrame set(APNGFrame var1) {
      this.sequence_number = var1.sequence_number;
      this.width = var1.width;
      this.height = var1.height;
      this.x_offset = var1.x_offset;
      this.y_offset = var1.y_offset;
      this.delay_num = var1.delay_num;
      this.delay_den = var1.delay_den;
      this.dispose_op = var1.dispose_op;
      this.blend_op = var1.blend_op;
      return this;
   }
}
