package zombie.world;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.scripting.objects.BaseScriptObject;
import zombie.util.io.BitHeader;
import zombie.util.io.BitHeaderRead;
import zombie.util.io.BitHeaderWrite;

public class DictionaryScriptInfo<T extends BaseScriptObject> {
   protected T script;
   protected String name;
   protected short registryID;
   protected long version;
   protected boolean isLoaded = false;

   public DictionaryScriptInfo() {
   }

   public T getScript() {
      return this.script;
   }

   public String getName() {
      return this.name;
   }

   public short getRegistryID() {
      return this.registryID;
   }

   public long getVersion() {
      return this.version;
   }

   public boolean isLoaded() {
      return this.isLoaded;
   }

   protected DictionaryScriptInfo<T> copy() {
      DictionaryScriptInfo var1 = new DictionaryScriptInfo();
      var1.name = this.name;
      var1.registryID = this.registryID;
      var1.version = this.version;
      var1.isLoaded = this.isLoaded;
      var1.script = this.script;
      return var1;
   }

   protected void saveAsText(FileWriter var1, String var2) throws IOException {
      var1.write(var2 + "registryID = " + this.registryID + "," + System.lineSeparator());
      var1.write(var2 + "name = \"" + this.name + "\"," + System.lineSeparator());
      var1.write(var2 + "version = \"" + this.version + "\"," + System.lineSeparator());
      var1.write(var2 + "isLoaded = " + this.isLoaded + "," + System.lineSeparator());
   }

   protected void save(ByteBuffer var1) {
      BitHeaderWrite var2 = BitHeader.allocWrite(BitHeader.HeaderSize.Byte, var1);
      var1.putShort(this.registryID);
      var1.putLong(this.version);
      if (this.name.startsWith("Base.") && this.name.length() > "Base.".length()) {
         var2.addFlags(1);
         GameWindow.WriteString(var1, this.name.substring("Base.".length()));
      } else {
         GameWindow.WriteString(var1, this.name);
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
      this.version = var1.getLong();
      if (var3.hasFlags(1)) {
         this.name = "Base." + GameWindow.ReadString(var1);
      } else {
         this.name = GameWindow.ReadString(var1);
      }

      this.isLoaded = var3.hasFlags(2);
      var3.release();
   }
}
