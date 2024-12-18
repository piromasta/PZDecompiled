package zombie.entity.components.crafting;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import zombie.GameWindow;
import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.entity.Component;
import zombie.entity.ComponentType;
import zombie.entity.GameEntityNetwork;
import zombie.entity.components.crafting.recipe.CraftRecipeData;
import zombie.entity.components.crafting.recipe.CraftRecipeManager;
import zombie.entity.components.resources.Resource;
import zombie.entity.components.resources.ResourceGroup;
import zombie.entity.components.resources.ResourceItem;
import zombie.entity.components.resources.Resources;
import zombie.entity.network.EntityPacketData;
import zombie.entity.network.EntityPacketType;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.fields.PlayerID;
import zombie.scripting.ScriptManager;
import zombie.scripting.entity.ComponentScript;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.entity.components.crafting.FurnaceLogicScript;
import zombie.util.StringUtils;
import zombie.util.io.BitHeader;
import zombie.util.io.BitHeaderRead;
import zombie.util.io.BitHeaderWrite;
import zombie.util.list.PZUnmodifiableList;

public class FurnaceLogic extends Component {
   private static final ConcurrentLinkedDeque<FurnaceSlot> SLOT_POOL = new ConcurrentLinkedDeque();
   private static final List<CraftRecipe> _emptyRecipeList = PZUnmodifiableList.wrap(new ArrayList());
   private static final List<Resource> _emptyResourceList = PZUnmodifiableList.wrap(new ArrayList());
   private String furnaceRecipeTagQuery;
   private String fuelRecipeTagQuery;
   private List<CraftRecipe> furnaceRecipes;
   private List<CraftRecipe> fuelRecipes;
   private StartMode startMode;
   private CraftRecipe currentRecipe;
   private int elapsedTime;
   private boolean doAutomaticCraftCheck;
   private boolean startRequested;
   private boolean stopRequested;
   private IsoPlayer requestingPlayer;
   private final CraftRecipeData craftData;
   private final CraftRecipeData craftTestData;
   private String furnaceInputsGroupName;
   private String furnaceOutputsGroupName;
   private String fuelInputsGroupName;
   private String fuelOutputsGroupName;
   private final FurnaceSlot[] furnaceSlots;
   private int furnaceSlotSize;

   private static FurnaceSlot allocFurnaceSlot(int var0) {
      FurnaceSlot var1 = (FurnaceSlot)SLOT_POOL.poll();
      if (var1 == null) {
         var1 = new FurnaceSlot();
      }

      var1.index = var0;
      return var1;
   }

   private static void releaseFurnaceSlot(FurnaceSlot var0) {
      try {
         if (var0 == null) {
            throw new IllegalArgumentException("Object cannot be null.");
         }

         assert !SLOT_POOL.contains(var0);

         var0.reset();
         SLOT_POOL.offer(var0);
      } catch (Exception var2) {
         var2.printStackTrace();
      }

   }

   private FurnaceLogic() {
      super(ComponentType.FurnaceLogic);
      this.startMode = StartMode.Manual;
      this.elapsedTime = 0;
      this.doAutomaticCraftCheck = true;
      this.startRequested = false;
      this.stopRequested = false;
      this.requestingPlayer = null;
      this.furnaceSlots = new FurnaceSlot[16];
      this.furnaceSlotSize = 0;
      this.craftData = CraftRecipeData.Alloc(CraftMode.Automation, true, false, true, false);
      this.craftTestData = CraftRecipeData.Alloc(CraftMode.Automation, true, false, true, false);
   }

   protected void readFromScript(ComponentScript var1) {
      super.readFromScript(var1);
      FurnaceLogicScript var2 = (FurnaceLogicScript)var1;
      this.startMode = var2.getStartMode();
      this.furnaceRecipeTagQuery = null;
      this.setFurnaceRecipeTagQuery(var2.getFurnaceRecipeTagQuery());
      this.fuelRecipeTagQuery = null;
      this.setFuelRecipeTagQuery(var2.getFuelRecipeTagQuery());
      this.furnaceInputsGroupName = var2.getInputsGroupName();
      this.furnaceOutputsGroupName = var2.getOutputsGroupName();
      this.fuelInputsGroupName = var2.getFuelInputsGroupName();
      this.fuelOutputsGroupName = var2.getFuelOutputsGroupName();
      this.doAutomaticCraftCheck = this.startMode == StartMode.Automatic;
   }

   public boolean isValid() {
      return super.isValid() && this.owner.hasComponent(ComponentType.Resources);
   }

   protected void reset() {
      super.reset();
      this.furnaceRecipeTagQuery = null;
      this.fuelRecipeTagQuery = null;
      this.furnaceRecipes = null;
      this.fuelRecipes = null;
      this.craftData.reset();
      this.craftTestData.reset();
      this.startMode = StartMode.Manual;
      this.doAutomaticCraftCheck = true;
      this.startRequested = false;
      this.stopRequested = false;
      this.requestingPlayer = null;
      this.furnaceInputsGroupName = null;
      this.furnaceOutputsGroupName = null;
      this.clearSlots();
      this.clearRecipe();
   }

   private void clearRecipe() {
      this.elapsedTime = 0;
      this.currentRecipe = null;
   }

   protected void clearSlots() {
      if (this.getSlotSize() != 0) {
         for(int var1 = 0; var1 < this.furnaceSlotSize; ++var1) {
            FurnaceSlot var2 = this.furnaceSlots[var1];
            if (var2 != null) {
               releaseFurnaceSlot(var2);
               this.furnaceSlots[var1] = null;
            }
         }

         this.furnaceSlotSize = 0;
      }
   }

   public int getSlotSize() {
      return this.furnaceSlotSize;
   }

   public FurnaceSlot getSlot(int var1) {
      return var1 >= 0 && var1 < this.furnaceSlotSize ? this.furnaceSlots[var1] : null;
   }

   protected FurnaceSlot createSlot(int var1) {
      if (var1 >= 0 && var1 < this.furnaceSlots.length) {
         FurnaceSlot var2 = allocFurnaceSlot(var1);
         if (this.furnaceSlots[var1] != null) {
            releaseFurnaceSlot(this.furnaceSlots[var1]);
         }

         this.furnaceSlots[var1] = var2;
         this.furnaceSlotSize = PZMath.max(var1 + 1, this.furnaceSlotSize);
         return var2;
      } else {
         return null;
      }
   }

   public ResourceItem getInputSlotResource(int var1) {
      Resources var2 = (Resources)this.getComponent(ComponentType.Resources);
      FurnaceSlot var3 = this.getSlot(var1);
      if (var2 != null && var3 != null) {
         ResourceItem var4 = (ResourceItem)var2.getResource(var3.inputResourceID);
         if (var4 != null) {
            return var4;
         }
      }

      return null;
   }

   public ResourceItem getOutputSlotResource(int var1) {
      Resources var2 = (Resources)this.getComponent(ComponentType.Resources);
      FurnaceSlot var3 = this.getSlot(var1);
      if (var2 != null && var3 != null) {
         ResourceItem var4 = (ResourceItem)var2.getResource(var3.outputResourceID);
         if (var4 != null) {
            return var4;
         }
      }

      return null;
   }

   public StartMode getStartMode() {
      return this.startMode;
   }

   public int getElapsedTime() {
      return this.elapsedTime;
   }

   void setElapsedTime(int var1) {
      this.elapsedTime = var1;
   }

   public boolean isStartRequested() {
      return this.startRequested;
   }

   void setStartRequested(boolean var1) {
      this.startRequested = var1;
   }

   public boolean isStopRequested() {
      return this.stopRequested;
   }

   void setStopRequested(boolean var1) {
      this.stopRequested = var1;
   }

   public IsoPlayer getRequestingPlayer() {
      return this.requestingPlayer;
   }

   void setRequestingPlayer(IsoPlayer var1) {
      this.requestingPlayer = var1;
   }

   public boolean isDoAutomaticCraftCheck() {
      return this.doAutomaticCraftCheck;
   }

   void setDoAutomaticCraftCheck(boolean var1) {
      this.doAutomaticCraftCheck = var1;
   }

   CraftRecipeData getCraftData() {
      return this.craftData;
   }

   CraftRecipeData getCraftTestData() {
      return this.craftTestData;
   }

   public String getFurnaceInputsGroupName() {
      return this.furnaceInputsGroupName;
   }

   public String getFurnaceOutputsGroupName() {
      return this.furnaceOutputsGroupName;
   }

   public String getFuelInputsGroupName() {
      return this.fuelInputsGroupName;
   }

   public String getFuelOutputsGroupName() {
      return this.fuelOutputsGroupName;
   }

   public String getFurnaceRecipeTagQuery() {
      return this.furnaceRecipeTagQuery;
   }

   public void setFurnaceRecipeTagQuery(String var1) {
      if (this.furnaceRecipeTagQuery == null || !this.furnaceRecipeTagQuery.equalsIgnoreCase(var1)) {
         this.furnaceRecipeTagQuery = var1;
         this.furnaceRecipes = null;
         if (!StringUtils.isNullOrWhitespace(this.furnaceRecipeTagQuery)) {
            this.furnaceRecipes = CraftRecipeManager.queryRecipes(var1);
         }
      }

   }

   public String getFuelRecipeTagQuery() {
      return this.fuelRecipeTagQuery;
   }

   public void setFuelRecipeTagQuery(String var1) {
      if (this.fuelRecipeTagQuery == null || !this.fuelRecipeTagQuery.equalsIgnoreCase(var1)) {
         this.fuelRecipeTagQuery = var1;
         this.fuelRecipes = null;
         if (!StringUtils.isNullOrWhitespace(this.fuelRecipeTagQuery)) {
            this.fuelRecipes = CraftRecipeManager.queryRecipes(var1);
         }
      }

   }

   public ArrayList<CraftRecipe> getFurnaceRecipes(ArrayList<CraftRecipe> var1) {
      var1.clear();
      if (this.furnaceRecipes != null) {
         var1.addAll(this.furnaceRecipes);
      }

      return var1;
   }

   protected List<CraftRecipe> getFurnaceRecipes() {
      return this.furnaceRecipes != null ? this.furnaceRecipes : _emptyRecipeList;
   }

   public ArrayList<CraftRecipe> getFuelRecipes(ArrayList<CraftRecipe> var1) {
      var1.clear();
      if (this.fuelRecipes != null) {
         var1.addAll(this.fuelRecipes);
      }

      return var1;
   }

   protected List<CraftRecipe> getFuelRecipes() {
      return this.fuelRecipes != null ? this.fuelRecipes : _emptyRecipeList;
   }

   public List<Resource> getFurnaceInputResources() {
      Resources var1 = (Resources)this.getComponent(ComponentType.Resources);
      if (var1 != null) {
         ResourceGroup var2 = var1.getResourceGroup(this.furnaceInputsGroupName);
         if (var2 != null) {
            return var2.getResources();
         }
      }

      return _emptyResourceList;
   }

   public List<Resource> getFurnaceOutputResources() {
      Resources var1 = (Resources)this.getComponent(ComponentType.Resources);
      if (var1 != null) {
         ResourceGroup var2 = var1.getResourceGroup(this.furnaceOutputsGroupName);
         if (var2 != null) {
            return var2.getResources();
         }
      }

      return _emptyResourceList;
   }

   public List<Resource> getFuelInputResources() {
      Resources var1 = (Resources)this.getComponent(ComponentType.Resources);
      if (var1 != null) {
         ResourceGroup var2 = var1.getResourceGroup(this.fuelInputsGroupName);
         if (var2 != null) {
            return var2.getResources();
         }
      }

      return _emptyResourceList;
   }

   public List<Resource> getFuelOutputResources() {
      Resources var1 = (Resources)this.getComponent(ComponentType.Resources);
      if (var1 != null) {
         ResourceGroup var2 = var1.getResourceGroup(this.fuelOutputsGroupName);
         if (var2 != null) {
            return var2.getResources();
         }
      }

      return _emptyResourceList;
   }

   public boolean isRunning() {
      return this.currentRecipe != null;
   }

   public boolean isFinished() {
      if (this.isRunning()) {
         return this.elapsedTime >= this.currentRecipe.getTime();
      } else {
         return false;
      }
   }

   public CraftRecipe getCurrentRecipe() {
      return this.currentRecipe;
   }

   public double getProgress() {
      if (this.isRunning() && this.elapsedTime != 0) {
         return this.elapsedTime >= this.currentRecipe.getTime() ? 1.0 : (double)this.elapsedTime / (double)this.currentRecipe.getTime();
      } else {
         return 0.0;
      }
   }

   protected void setRecipe(CraftRecipe var1) {
      if (var1 == null) {
         this.clearRecipe();
      } else if (this.currentRecipe != var1) {
         this.clearRecipe();
         this.currentRecipe = var1;
         this.doAutomaticCraftCheck = this.startMode == StartMode.Automatic;
         this.craftData.setRecipe(this.currentRecipe);
         this.craftTestData.setRecipe(this.currentRecipe);
      }

   }

   public CraftRecipe getPossibleRecipe() {
      List var1 = this.getFuelInputResources();
      List var2 = this.getFuelOutputResources();
      return var1 != null && !var1.isEmpty() ? CraftUtil.getPossibleRecipe(this.craftTestData, this.fuelRecipes, var1, var2) : null;
   }

   protected void onRemovedFromOwner() {
   }

   protected void onConnectComponents() {
   }

   public CraftRecipeMonitor debugCanStart(IsoPlayer var1) {
      CraftRecipeMonitor var3;
      try {
         DebugLog.General.println("=== Starting debug canStart test ===");
         CraftRecipeMonitor var2 = CraftRecipeMonitor.Create();
         if (this.isValid()) {
            if (this.startMode == StartMode.Manual && !this.owner.isUsingPlayer(var1)) {
               var2.warn("Player is not the using player.");
               var2.close();
               var3 = var2.seal();
               return var3;
            }

            var2.logFurnaceLogic(this);
            List var11 = this.getFuelInputResources();
            List var4 = this.getFuelOutputResources();
            var2.logResources(var11, var4);
            CraftRecipeMonitor var5 = CraftUtil.debugCanStart(var1, this.craftTestData, this.fuelRecipes, var11, var4, var2);
            return var5;
         }

         var2.warn("Unable to start (not valid).");
         var2.close();
         var3 = var2.seal();
      } catch (Exception var9) {
         var9.printStackTrace();
         return null;
      } finally {
         this.craftData.setMonitor((CraftRecipeMonitor)null);
         this.craftTestData.setMonitor((CraftRecipeMonitor)null);
      }

      return var3;
   }

   public boolean canStart(IsoPlayer var1) {
      return this.canStart(StartMode.Manual, var1);
   }

   protected boolean canStart(StartMode var1, IsoPlayer var2) {
      if (!this.isValid()) {
         return false;
      } else if (!this.isRunning() && this.startMode != StartMode.Passive) {
         if (this.startMode != var1) {
            return false;
         } else if (this.startMode == StartMode.Manual && !this.owner.isUsingPlayer(var2)) {
            return false;
         } else {
            List var3 = this.getFuelInputResources();
            List var4 = this.getFuelOutputResources();
            if (var3 != null && !var3.isEmpty()) {
               return CraftUtil.canStart(this.craftTestData, this.fuelRecipes, var3, var4);
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   public void start(IsoPlayer var1) {
      if (this.startMode != StartMode.Manual || this.owner.isUsingPlayer(var1)) {
         if (GameClient.bClient) {
            if (this.canStart(StartMode.Manual, var1)) {
               this.sendStartRequest(var1);
            }
         } else {
            this.startRequested = true;
            this.requestingPlayer = var1;
         }

      }
   }

   public void stop(IsoPlayer var1) {
      this.stop(var1, false);
   }

   public void stop(IsoPlayer var1, boolean var2) {
      if (this.isValid()) {
         if (this.startMode != StartMode.Manual || var2 || this.owner.isUsingPlayer(var1)) {
            if (GameClient.bClient) {
               this.sendStopRequest(var1);
            } else {
               this.stopRequested = true;
               this.requestingPlayer = var1;
            }

         }
      }
   }

   protected boolean onReceivePacket(ByteBuffer var1, EntityPacketType var2, UdpConnection var3) throws IOException {
      switch (var2) {
         case CraftLogicStartRequest:
            this.receiveStartRequest(var1, var3);
            return true;
         case CraftLogicStopRequest:
            this.receiveStopRequest(var1, var3);
            return true;
         default:
            return false;
      }
   }

   public void sendStartRequest(IsoPlayer var1) {
      if (GameClient.bClient) {
         EntityPacketData var2 = GameEntityNetwork.createPacketData(EntityPacketType.CraftLogicStartRequest);
         var2.bb.put((byte)(var1 != null ? 1 : 0));
         if (var1 != null) {
            PlayerID var3 = new PlayerID();
            var3.set(var1);
            var3.write(var2.bb);
         }

         this.sendClientPacket(var2);
      }
   }

   protected void receiveStartRequest(ByteBuffer var1, UdpConnection var2) throws IOException {
      if (GameServer.bServer) {
         IsoPlayer var3 = null;
         if (var1.get() == 1) {
            PlayerID var4 = new PlayerID();
            var4.parse(var1, var2);
            var4.parsePlayer(var2);
            var3 = var4.getPlayer();
            if (var3 == null) {
               throw new IOException("Player not found.");
            }
         }

         this.start(var3);
      }
   }

   public void sendStopRequest(IsoPlayer var1) {
      if (GameClient.bClient) {
         EntityPacketData var2 = GameEntityNetwork.createPacketData(EntityPacketType.CraftLogicStopRequest);
         var2.bb.put((byte)(var1 != null ? 1 : 0));
         if (var1 != null) {
            PlayerID var3 = new PlayerID();
            var3.set(var1);
            var3.write(var2.bb);
         }

         this.sendClientPacket(var2);
      }
   }

   protected void receiveStopRequest(ByteBuffer var1, UdpConnection var2) throws IOException {
      if (GameServer.bServer) {
         IsoPlayer var3 = null;
         if (var1.get() == 1) {
            PlayerID var4 = new PlayerID();
            var4.parse(var1, var2);
            var4.parsePlayer(var2);
            var3 = var4.getPlayer();
            if (var3 == null) {
               throw new IOException("Player not found.");
            }
         }

         this.stop(var3);
      }
   }

   protected void saveSyncData(ByteBuffer var1) throws IOException {
      this.save(var1);
   }

   protected void loadSyncData(ByteBuffer var1) throws IOException {
      this.load(var1, 219);
   }

   protected void save(ByteBuffer var1) throws IOException {
      super.save(var1);
      BitHeaderWrite var2 = BitHeader.allocWrite(BitHeader.HeaderSize.Short, var1);
      if (this.furnaceRecipeTagQuery != null) {
         var2.addFlags(1);
         GameWindow.WriteString(var1, this.furnaceRecipeTagQuery);
      }

      if (this.fuelRecipeTagQuery != null) {
         var2.addFlags(2);
         GameWindow.WriteString(var1, this.fuelRecipeTagQuery);
      }

      if (this.startMode != StartMode.Manual) {
         var2.addFlags(4);
         var1.put(this.startMode.getByteId());
      }

      if (this.fuelInputsGroupName != null) {
         var2.addFlags(8);
         GameWindow.WriteString(var1, this.fuelInputsGroupName);
      }

      if (this.fuelOutputsGroupName != null) {
         var2.addFlags(16);
         GameWindow.WriteString(var1, this.fuelOutputsGroupName);
      }

      if (this.furnaceInputsGroupName != null) {
         var2.addFlags(32);
         GameWindow.WriteString(var1, this.furnaceInputsGroupName);
      }

      if (this.furnaceOutputsGroupName != null) {
         var2.addFlags(64);
         GameWindow.WriteString(var1, this.furnaceOutputsGroupName);
      }

      if (this.currentRecipe != null) {
         var2.addFlags(128);
         GameWindow.WriteString(var1, this.currentRecipe.getScriptObjectFullType());
         var1.putLong(this.currentRecipe.getScriptVersion());
         var1.putInt(this.elapsedTime);
         this.craftData.save(var1);
      }

      var2.write();
      var2.release();
   }

   protected void load(ByteBuffer var1, int var2) throws IOException {
      super.load(var1, var2);
      BitHeaderRead var3 = BitHeader.allocRead(BitHeader.HeaderSize.Short, var1);
      this.furnaceRecipeTagQuery = null;
      this.fuelRecipeTagQuery = null;
      this.startMode = StartMode.Manual;
      this.fuelInputsGroupName = null;
      this.fuelOutputsGroupName = null;
      this.furnaceInputsGroupName = null;
      this.furnaceOutputsGroupName = null;
      boolean var4 = false;
      String var5;
      if (var3.hasFlags(1)) {
         var5 = GameWindow.ReadString(var1);
         this.setFurnaceRecipeTagQuery(var5);
      }

      if (var3.hasFlags(2)) {
         var5 = GameWindow.ReadString(var1);
         this.setFuelRecipeTagQuery(var5);
      }

      if (var3.hasFlags(4)) {
         this.startMode = StartMode.fromByteId(var1.get());
      }

      if (var3.hasFlags(8)) {
         this.fuelInputsGroupName = GameWindow.ReadString(var1);
      }

      if (var3.hasFlags(16)) {
         this.fuelOutputsGroupName = GameWindow.ReadString(var1);
      }

      if (var3.hasFlags(32)) {
         this.furnaceInputsGroupName = GameWindow.ReadString(var1);
      }

      if (var3.hasFlags(64)) {
         this.furnaceOutputsGroupName = GameWindow.ReadString(var1);
      }

      if (var3.hasFlags(128)) {
         var5 = GameWindow.ReadString(var1);
         CraftRecipe var6 = ScriptManager.instance.getCraftRecipe(var5);
         this.setRecipe(var6);
         long var7 = var1.getLong();
         if (var6 == null || var7 != var6.getScriptVersion()) {
            var4 = true;
            DebugLog.General.warn("CraftRecipe '" + var5 + "' is null (" + (var6 == null) + ", or has script version mismatch. Cancelling current craft.");
         }

         this.elapsedTime = var1.getInt();
         if (this.currentRecipe == null) {
            this.elapsedTime = 0;
         }

         if (!this.craftData.load(var1, var2, var6, var4)) {
            var4 = true;
            this.craftData.setRecipe((CraftRecipe)null);
         }
      } else {
         this.setRecipe((CraftRecipe)null);
         this.elapsedTime = 0;
      }

      var3.release();
      if (var4) {
         this.clearRecipe();
         this.clearSlots();
      }

      this.doAutomaticCraftCheck = true;
   }

   public static class FurnaceSlot {
      private int index = -1;
      private CraftRecipe currentRecipe;
      private int elapsedTime = 0;
      private String inputResourceID;
      private String outputResourceID;

      public FurnaceSlot() {
      }

      public int getIndex() {
         return this.index;
      }

      public CraftRecipe getCurrentRecipe() {
         return this.currentRecipe;
      }

      public int getElapsedTime() {
         return this.elapsedTime;
      }

      protected void setElapsedTime(int var1) {
         this.elapsedTime = var1;
      }

      public String getInputResourceID() {
         return this.inputResourceID;
      }

      public String getOutputResourceID() {
         return this.outputResourceID;
      }

      private void reset() {
         this.index = -1;
         this.inputResourceID = null;
         this.outputResourceID = null;
         this.clearRecipe();
      }

      protected void setRecipe(CraftRecipe var1) {
         if (this.currentRecipe != null) {
            this.clearRecipe();
         }

         this.currentRecipe = var1;
      }

      protected void clearRecipe() {
         this.currentRecipe = null;
         this.elapsedTime = 0;
      }

      protected void initialize(String var1, String var2) {
         if (this.inputResourceID == null || !this.inputResourceID.equals(var1) || this.outputResourceID == null || !this.outputResourceID.equals(var2)) {
            this.inputResourceID = var1;
            this.outputResourceID = var2;
            this.elapsedTime = 0;
            this.currentRecipe = null;
         }

      }

      private void save(ByteBuffer var1) throws IOException {
         if (this.currentRecipe != null) {
            var1.put((byte)1);
            GameWindow.WriteString(var1, this.currentRecipe.getScriptObjectFullType());
            var1.putLong(this.currentRecipe.getScriptVersion());
            var1.putInt(this.elapsedTime);
         } else {
            var1.put((byte)0);
         }

         GameWindow.WriteString(var1, this.inputResourceID);
         GameWindow.WriteString(var1, this.outputResourceID);
      }

      private void load(ByteBuffer var1, int var2) throws IOException {
         if (var1.get() == 1) {
            String var3 = GameWindow.ReadString(var1);
            long var4 = var1.getLong();
            this.elapsedTime = var1.getInt();
            this.currentRecipe = ScriptManager.instance.getCraftRecipe(var3);
            if (this.currentRecipe == null || var4 != this.currentRecipe.getScriptVersion()) {
               this.currentRecipe = null;
               this.elapsedTime = 0;
               DebugLog.General.warn("FurnaceSlot[" + this.index + "] CraftRecipe '" + var3 + "' is null (" + (this.currentRecipe == null) + ", or has script version mismatch. Cancelling current craft.");
            }
         }

         this.inputResourceID = GameWindow.ReadString(var1);
         this.outputResourceID = GameWindow.ReadString(var1);
      }
   }
}
