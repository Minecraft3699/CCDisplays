package com.mc3699.ccdisplays.holoprojector;

import com.mc3699.ccdisplays.holoprojector.rendering.HoloTextElement;
import com.mc3699.ccdisplays.util.ModBlockEntities;
import com.mc3699.ccdisplays.util.ModCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.SpringFeature;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class HoloProjectorBlockEntity extends BlockEntity {

    private final List<HoloTextElement> elementList = new ArrayList<>();


    private final HoloProjectorPeripheral peripheral;
    private final LazyOptional peripheralLazyOptional;

    public int addText(String text, float x, float y, float z, float rotation, float scale, int color)
    {
        this.elementList.add(new HoloTextElement(x,y,z,rotation,scale,color,text));
        setChanged();
        level.sendBlockUpdated(getBlockPos(),getBlockState(),getBlockState(),3);
        return this.elementList.size() - 1;
    }

    public boolean replaceText(int index, String text, float x, float y, float z, float rotation, float scale, int color){
        if(index >= 0 && index < elementList.size()) {
            elementList.set(index, new HoloTextElement(x,y,z,rotation,scale,color,text));
            setChanged();
            level.sendBlockUpdated(getBlockPos(),getBlockState(),getBlockState(),3);
            return true;
        }
        return false;
    }

    public void clearElements()
    {
        this.elementList.clear();
        setChanged();
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
    }

    public List<HoloTextElement> getElementList() {
        return this.elementList;
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        super.saveAdditional(pTag);

        ListTag elementListTag = new ListTag();

        for(HoloTextElement element : elementList)
        {
            CompoundTag elementTag = new CompoundTag();
            elementTag.putString("text",element.getText());
            elementTag.putFloat("x",element.getXPos());
            elementTag.putFloat("y",element.getYPos());
            elementTag.putFloat("z",element.getZPos());
            elementTag.putFloat("rotation",element.getRotation());
            elementTag.putFloat("scale",element.getScale());
            elementTag.putInt("color",element.getColor());
            elementListTag.add(elementTag);
        }
        pTag.put("text", elementListTag);

    }

    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        this.elementList.clear();

        if(pTag.contains("text", ListTag.TAG_LIST))
        {
            ListTag textListTag = pTag.getList("text", ListTag.TAG_COMPOUND);
            for(int i = 0; i < textListTag.size(); i++)
            {
                CompoundTag elementTag = textListTag.getCompound(i);
                HoloTextElement element = new HoloTextElement(
                        elementTag.getFloat("x"),
                        elementTag.getFloat("y"),
                        elementTag.getFloat("z"),
                        elementTag.getFloat("rotation"),
                        elementTag.getFloat("scale"),
                        elementTag.getInt("color"),
                        elementTag.getString("text"));
                this.elementList.add(element);
            }
        }
    }

    public HoloProjectorBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.HOLOGRAM_PROJECTOR.get(), pPos, pBlockState);
        this.peripheral = new HoloProjectorPeripheral(this);
        this.peripheralLazyOptional = LazyOptional.of(() -> this.peripheral);
    }

    public HoloProjectorPeripheral getPeripheral() {
        return this.peripheral;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if(cap == ModCapabilities.PERIPHERAL_CAPABILITY)
        {
            return peripheralLazyOptional.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        peripheralLazyOptional.invalidate();
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.offset(-128, -128, -128), worldPosition.offset(128, 128, 128));
    }
}
