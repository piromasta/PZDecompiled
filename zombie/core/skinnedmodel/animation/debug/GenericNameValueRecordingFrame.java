package zombie.core.skinnedmodel.animation.debug;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import zombie.ZomboidFileSystem;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.util.list.PZArrayUtil;

public abstract class GenericNameValueRecordingFrame {
   protected String[] m_columnNames = new String[0];
   protected final HashMap<String, Integer> m_nameIndices = new HashMap();
   protected boolean m_headerDirty = false;
   protected final String m_fileKey;
   protected PrintStream m_outHeader = null;
   private ByteArrayOutputStream m_outHeaderByteArrayStream;
   protected PrintStream m_outHeaderFile;
   protected PrintStream m_outValues = null;
   private String m_headerFilePath = null;
   private String m_valuesFilePath = null;
   protected int m_firstFrameNumber = -1;
   protected int m_frameNumber = -1;
   protected static final String delim = ",";
   protected final String m_valuesFileNameSuffix;
   private String m_previousLine = null;
   private int m_previousFrameNo = -1;
   protected final StringBuilder m_lineBuffer = new StringBuilder();

   public GenericNameValueRecordingFrame(String var1, String var2) {
      this.m_fileKey = var1;
      this.m_valuesFileNameSuffix = var2;
   }

   protected int addColumnInternal(String var1) {
      int var2 = this.m_columnNames.length;
      this.m_columnNames = (String[])PZArrayUtil.add(this.m_columnNames, var1);
      this.m_nameIndices.put(var1, var2);
      this.m_headerDirty = true;
      this.onColumnAdded();
      return var2;
   }

   public int getOrCreateColumn(String var1) {
      return this.m_nameIndices.containsKey(var1) ? (Integer)this.m_nameIndices.get(var1) : this.addColumnInternal(var1);
   }

   public void setFrameNumber(int var1) {
      if (this.m_frameNumber != var1) {
         this.m_frameNumber = var1;
         if (this.m_firstFrameNumber == -1) {
            this.m_firstFrameNumber = this.m_frameNumber;
         }

         this.m_headerDirty = true;
      }

   }

   public int getColumnCount() {
      return this.m_columnNames.length;
   }

   public String getNameAt(int var1) {
      return this.m_columnNames[var1];
   }

   public abstract String getValueAt(int var1);

   protected void openHeader() {
      if (this.m_outHeader != null) {
         this.m_outHeader.close();
         this.m_outHeader = null;
      }

      this.m_outHeaderByteArrayStream = new ByteArrayOutputStream();
      this.m_outHeader = new PrintStream(this.m_outHeaderByteArrayStream, true, StandardCharsets.UTF_8);
   }

   protected void flushHeaderToFile() {
      this.m_outHeaderFile = AnimationPlayerRecorder.openFileStream(this.m_fileKey + "_header", false, (var1x) -> {
         this.m_headerFilePath = var1x;
      });
      byte[] var1 = this.m_outHeaderByteArrayStream.toByteArray();

      try {
         this.m_outHeaderFile.write(var1);
      } catch (IOException var3) {
         DebugType.General.printException(var3, "Exception thrown trying to write recording header file.", LogSeverity.Error);
      }

      this.m_outHeaderByteArrayStream.reset();
   }

   protected void openValuesFile(boolean var1) {
      if (this.m_outValues != null) {
         this.m_outValues.close();
         this.m_outValues = null;
      }

      this.m_outValues = AnimationPlayerRecorder.openFileStream(this.m_fileKey + this.m_valuesFileNameSuffix, var1, (var1x) -> {
         this.m_valuesFilePath = var1x;
      });
   }

   public void writeLine() {
      if (this.m_headerDirty || this.m_outHeader == null) {
         this.m_headerDirty = false;
         this.writeHeaderToMemory();
         this.flushHeaderToFile();
      }

      this.writeData();
   }

   public void close() {
      if (this.m_outHeader != null) {
         this.m_outHeader.close();
         this.m_outHeader = null;
      }

      if (this.m_outValues != null) {
         this.m_outValues.close();
         this.m_outValues = null;
      }

   }

   public void closeAndDiscard() {
      this.close();
      ZomboidFileSystem.instance.tryDeleteFile(this.m_headerFilePath);
      this.m_headerFilePath = null;
      ZomboidFileSystem.instance.tryDeleteFile(this.m_valuesFilePath);
      this.m_valuesFilePath = null;
      this.m_previousLine = null;
      this.m_previousFrameNo = -1;
   }

   protected abstract void onColumnAdded();

   public abstract void reset();

   protected void writeHeaderToMemory() {
      StringBuilder var1 = new StringBuilder();
      var1.append("frameNo");
      this.buildHeader(var1);
      this.openHeader();
      this.m_outHeader.println(var1);
      this.m_outHeader.println(this.m_firstFrameNumber + "," + this.m_frameNumber);
   }

   protected void buildHeader(StringBuilder var1) {
      int var2 = 0;

      for(int var3 = this.getColumnCount(); var2 < var3; ++var2) {
         appendCell(var1, this.getNameAt(var2));
      }

   }

   protected void writeData() {
      if (this.m_outValues == null) {
         this.openValuesFile(false);
      }

      StringBuilder var1 = this.m_lineBuffer;
      var1.setLength(0);
      this.writeData(var1);
      if (this.m_previousLine == null || !this.m_previousLine.contentEquals(var1)) {
         this.m_outValues.print(this.m_frameNumber);
         this.m_outValues.println(var1);
         this.m_previousLine = var1.toString();
         this.m_previousFrameNo = this.m_frameNumber;
      }
   }

   protected void writeData(StringBuilder var1) {
      int var2 = 0;

      for(int var3 = this.getColumnCount(); var2 < var3; ++var2) {
         appendCell(var1, this.getValueAt(var2));
      }

   }

   public static StringBuilder appendCell(StringBuilder var0) {
      return var0.append(",");
   }

   public static StringBuilder appendCell(StringBuilder var0, String var1) {
      return var0.append(",").append(var1);
   }

   public static StringBuilder appendCell(StringBuilder var0, float var1) {
      return var0.append(",").append(var1);
   }

   public static StringBuilder appendCell(StringBuilder var0, int var1) {
      return var0.append(",").append(var1);
   }

   public static StringBuilder appendCell(StringBuilder var0, long var1) {
      return var0.append(",").append(var1);
   }

   public static StringBuilder appendCellQuot(StringBuilder var0, String var1) {
      return var0.append(",").append('"').append(var1).append('"');
   }
}
