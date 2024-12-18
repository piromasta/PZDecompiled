package zombie.world;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.util.io.BitHeader;
import zombie.util.io.BitHeaderRead;
import zombie.util.io.BitHeaderWrite;

public class DictionaryStringInfo {
   protected String string;
   protected short registryID;
   protected boolean isLoaded = false;

   public DictionaryStringInfo() {
   }

   public String getString() {
      return this.string;
   }

   public short getRegistryID() {
      return this.registryID;
   }

   public boolean isLoaded() {
      return this.isLoaded;
   }

   protected DictionaryStringInfo copy() {
      DictionaryStringInfo var1 = new DictionaryStringInfo();
      var1.string = this.string;
      var1.registryID = this.registryID;
      var1.isLoaded = this.isLoaded;
      return var1;
   }

   protected void saveAsText(FileWriter var1, String var2) throws IOException {
      var1.write(var2 + "registryID = " + this.registryID + "," + System.lineSeparator());
      var1.write(var2 + "string = \"" + this.string + "\"," + System.lineSeparator());
      var1.write(var2 + "isLoaded = " + this.isLoaded + "," + System.lineSeparator());
   }

   protected void save(ByteBuffer var1) {
      BitHeaderWrite var2 = BitHeader.allocWrite(BitHeader.HeaderSize.Byte, var1);
      var1.putShort(this.registryID);
      if (this.string.startsWith("Base.") && this.string.length() > "Base.".length()) {
         var2.addFlags(1);
         GameWindow.WriteString(var1, this.string.substring("Base.".length()));
      } else {
         GameWindow.WriteString(var1, this.string);
      }

      if (this.isLoaded) {
         var2.addFlags(2);
      }

      var2.write();
      var2.release();
   }

   protected void load(ByteBuffer var1, int var2) {
      BitHeaderRead var3 = BitHeader.allocRead(BitHeader.HeaderSize.Byte, var1);
      this.registryID = var1.getShort();
      if (var3.hasFlags(1)) {
         this.string = "Base." + GameWindow.ReadString(var1);
      } else {
         this.string = GameWindow.ReadString(var1);
      }

      this.isLoaded = var3.hasFlags(2);
      var3.release();
   }
}
